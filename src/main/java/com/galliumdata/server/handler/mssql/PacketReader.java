// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.ServerException;
import java.net.SocketException;
import com.galliumdata.server.handler.ProtocolException;
import com.galliumdata.server.util.BinaryDump;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.Level;
import javax.net.ssl.SSLEngineResult;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.BufferedInputStream;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.SSLEngine;
import java.io.InputStream;
import java.net.Socket;

public class PacketReader
{
    protected final Socket socket;
    private InputStream socketIn;
    private final MSSQLForwarder forwarder;
    private final String name;
    public static final int PKT_MAX = 32768;
    private byte[] buffer;
    private int readOffset;
    private boolean needToReadMore;
    private boolean bufferResized;
    public Object sideBandReadSignal;
    public RawPacket sideBandPacket;
    private SSLEngine sslEngine;
    private static final Logger log;
    protected static final Logger logNet;
    
    public PacketReader(final Socket socket, final String name, final MSSQLForwarder forwarder) {
        this.socketIn = null;
        this.buffer = new byte[32768];
        this.readOffset = 0;
        this.needToReadMore = true;
        this.bufferResized = false;
        this.socket = socket;
        this.name = name;
        this.forwarder = forwarder;
        try {
            if (socket != null) {
                this.socketIn = new BufferedInputStream(socket.getInputStream());
            }
        }
        catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public RawPacket readNextPacket() {
        RawPacket pkt = this.readNextPacketPlain(false);
        if (this.sideBandReadSignal != null) {
            if (pkt.getTypeCode() == 23) {
                final ByteBuffer byteBuffer = ByteBuffer.wrap(pkt.getBuffer());
                final ByteBuffer decryptOut = ByteBuffer.allocate(pkt.getBuffer().length * 2);
                SSLEngineResult sslResult = null;
                try {
                    sslResult = this.sslEngine.unwrap(byteBuffer, decryptOut);
                }
                catch (final Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
                final byte[] decryptedBytes = new byte[sslResult.bytesProduced()];
                System.arraycopy(decryptOut.array(), 0, decryptedBytes, 0, sslResult.bytesProduced());
                pkt = new RawPacket(decryptedBytes);
            }
            this.sideBandPacket = pkt;
            synchronized (this.sideBandReadSignal) {
                this.sideBandReadSignal.notify();
            }
            this.sideBandReadSignal = null;
            pkt = this.readNextPacketPlain(false);
        }
        return pkt;
    }
    
    public RawPacket readNextPacketPlain(final boolean expectSSL) {
        int packetLen = 0;
        while (true) {
            int readThisTime = 0;
            if (this.needToReadMore) {
                int numRead;
                try {
                    if (PacketReader.log.isEnabled(Level.TRACE)) {
                        PacketReader.log.trace(Markers.MSSQL, this.forwarder.toString() + " is reading from network...");
                    }
                    numRead = this.socketIn.read(this.buffer, this.readOffset, this.buffer.length - this.readOffset);
                    if (PacketReader.log.isEnabled(Level.TRACE)) {
                        PacketReader.log.trace(Markers.MSSQL, this.forwarder.toString() + " has read from network:\n" + BinaryDump.getBinaryDump(this.buffer, this.readOffset, numRead));
                    }
                    readThisTime = numRead;
                    this.forwarder.incrementBytesReceived(numRead);
                    this.logNetwork(this.buffer, this.readOffset, numRead);
                }
                catch (final SocketException sockex) {
                    if ("Socket closed".equals(sockex.getMessage())) {
                        PacketReader.log.trace(Markers.MSSQL, "Socket has been closed: port " + this.socket.getPort() + ", local port: " + this.socket.getLocalPort());
                        return null;
                    }
                    if ("Connection reset".equals(sockex.getMessage())) {
                        PacketReader.log.trace(Markers.MSSQL, "Socket has been reset: port " + this.socket.getPort() + ", local port: " + this.socket.getLocalPort());
                        return null;
                    }
                    throw new ProtocolException("db.mssql.protocol.ProtocolException", new Object[] { sockex.getMessage() });
                }
                catch (final Exception ex) {
                    throw new ProtocolException("db.mssql.protocol.ProtocolException", new Object[] { ex.getMessage() });
                }
                if (numRead == 0) {
                    PacketReader.log.warn(Markers.MSSQL, "Received 0 bytes???");
                }
                if (numRead == -1) {
                    return null;
                }
                this.readOffset += numRead;
                this.needToReadMore = false;
            }
            if (this.readOffset < 4) {
                this.needToReadMore = true;
            }
            else {
                if (expectSSL) {
                    if (this.readOffset > 4 && this.buffer[0] == 23 && this.buffer[1] == 3 && this.buffer[2] == 3) {
                        final byte[] packetBytes = new byte[this.readOffset];
                        System.arraycopy(this.buffer, 0, packetBytes, 0, this.readOffset);
                        final RawPacket pkt = new RawPacket(packetBytes);
                        this.readOffset = 0;
                        return pkt;
                    }
                }
                else if (this.buffer[0] == 23) {
                    if (PacketReader.log.isTraceEnabled()) {
                        PacketReader.log.trace("Read TLS packet (probably) from " + String.valueOf(this.forwarder) + ", size so far: " + this.readOffset + ", read this time: " + readThisTime);
                    }
                    int sslPktLen = DataTypeReader.readTwoByteInteger(this.buffer, 3);
                    if (this.readOffset < sslPktLen + 5) {
                        if (PacketReader.log.isTraceEnabled()) {
                            PacketReader.log.trace("DEBUG - SSL packet is " + sslPktLen + " but have only received " + this.readOffset + " of it, must read more");
                        }
                        this.needToReadMore = true;
                        continue;
                    }
                    final byte[] packetBytes2 = new byte[sslPktLen + 5];
                    System.arraycopy(this.buffer, 0, packetBytes2, 0, sslPktLen + 5);
                    final RawPacket pkt2 = new RawPacket(packetBytes2);
                    if (this.readOffset - (sslPktLen + 5) > 0) {
                        System.arraycopy(this.buffer, sslPktLen + 5, this.buffer, 0, this.readOffset - (sslPktLen + 5));
                    }
                    this.readOffset -= sslPktLen + 5;
                    if (this.readOffset > 0 && this.buffer[0] != 23) {
                        throw new RuntimeException("Not a TLS packet");
                    }
                    if (this.readOffset >= 5) {
                        sslPktLen = DataTypeReader.readTwoByteInteger(this.buffer, 3);
                        if (this.readOffset < sslPktLen) {
                            this.needToReadMore = true;
                        }
                    }
                    else {
                        this.needToReadMore = true;
                    }
                    return pkt2;
                }
                if (this.buffer[0] == 83) {
                    packetLen = DataTypeReader.readTwoByteIntegerLow(this.buffer, 4);
                }
                else {
                    packetLen = DataTypeReader.readTwoByteInteger(this.buffer, 2);
                }
                if (packetLen < 0) {
                    throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "Negative packet length" });
                }
                if (packetLen <= this.readOffset) {
                    final byte[] packetBytes = new byte[packetLen];
                    System.arraycopy(this.buffer, 0, packetBytes, 0, packetLen);
                    final RawPacket pkt = new RawPacket(packetBytes);
                    if (this.readOffset > packetLen) {
                        System.arraycopy(this.buffer, packetLen, this.buffer, 0, this.readOffset - packetLen);
                        this.readOffset -= packetLen;
                        this.needToReadMore = false;
                    }
                    else {
                        this.readOffset = 0;
                        this.needToReadMore = true;
                    }
                    return pkt;
                }
                this.needToReadMore = true;
                if (this.readOffset == this.buffer.length) {
                    throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "Unable to read packet -- too large" });
                }
                continue;
            }
        }
    }
    
    public void close() {
        try {
            if (PacketReader.log.isTraceEnabled()) {
                PacketReader.log.trace(Markers.MSSQL, "NetworkPacketReader is closing: {} on port {}, local port {}", (Object)this.forwarder.toString(), (Object)this.socket.getPort(), (Object)this.socket.getLocalPort());
            }
            this.socket.close();
        }
        catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    protected void logNetwork(final byte[] bytes, final int idx, final int len) {
        if (!PacketReader.logNet.isDebugEnabled()) {
            return;
        }
        if (PacketReader.logNet.isTraceEnabled()) {
            final String bytesStr = BinaryDump.getBinaryDump(bytes, idx, len);
            PacketReader.logNet.trace(Markers.MSSQL, "Connection " + this.forwarder.adapter.getConnection().getName() + " from " + this.forwarder.getReceiveFromName() + ":\n" + bytesStr);
        }
    }
    
    public SSLEngine getSslEngine() {
        return this.sslEngine;
    }
    
    public void setSslEngine(final SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
        logNet = LogManager.getLogger("galliumdata.network");
    }
}
