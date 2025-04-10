// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import org.apache.logging.log4j.LogManager;
import java.net.SocketException;
import com.galliumdata.server.handler.ProtocolException;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.Level;
import java.io.IOException;
import org.apache.logging.log4j.Logger;
import java.io.InputStream;
import java.net.Socket;

public class NetworkPacketReader implements PacketReader
{
    protected final Socket socket;
    private InputStream socketIn;
    private final HttpForwarder forwarder;
    private final String name;
    public static final int PKT_MAX = 1000000;
    private byte[] buffer;
    private static final Logger log;
    
    public NetworkPacketReader(final Socket socket, final String name, final HttpForwarder forwarder) {
        this.socketIn = null;
        this.buffer = new byte[1000000];
        this.socket = socket;
        this.name = name;
        this.forwarder = forwarder;
        try {
            if (socket != null) {
                this.socketIn = socket.getInputStream();
            }
        }
        catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public RawPacket readNextPacket() {
        final RawPacket pkt = this.readNextPacketPlain();
        return pkt;
    }
    
    @Override
    public RawPacket readNextPacketPlain() {
        int numRead = 0;
        try {
            if (NetworkPacketReader.log.isEnabled(Level.TRACE)) {
                NetworkPacketReader.log.trace(Markers.HTTP, this.forwarder.toString() + " is reading from network...");
            }
            numRead += this.socketIn.read(this.buffer, 0, 1000000);
            if (numRead == -1) {
                return null;
            }
            this.forwarder.incrementBytesReceived(numRead);
        }
        catch (final SocketException sockex) {
            if ("Socket closed".equals(sockex.getMessage())) {
                NetworkPacketReader.log.trace(Markers.HTTP, "Socket has been closed: port " + this.socket.getPort() + ", local port: " + this.socket.getLocalPort());
                return null;
            }
            throw new ProtocolException("db.http.protocol.ProtocolException", new Object[] { sockex.getMessage() });
        }
        catch (final Exception ex) {
            throw new ProtocolException("db.http.protocol.ProtocolException", new Object[] { ex.getMessage() });
        }
        final byte[] packetBytes = new byte[numRead];
        System.arraycopy(this.buffer, 0, packetBytes, 0, numRead);
        final RawPacket pkt = new RawPacket(packetBytes);
        return pkt;
    }
    
    @Override
    public void close() {
        try {
            if (NetworkPacketReader.log.isTraceEnabled()) {
                NetworkPacketReader.log.trace(Markers.HTTP, "NetworkPacketReader is closing: {} on port {}, local port {}", (Object)this.forwarder.toString(), (Object)this.socket.getPort(), (Object)this.socket.getLocalPort());
            }
            this.socket.close();
        }
        catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
