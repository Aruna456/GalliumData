// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngine;
import java.nio.ByteBuffer;
import com.galliumdata.server.ServerException;

public class RawPacketReader
{
    private final ConnectionState connectionState;
    private final PacketReader reader;
    private RawPacket curPkt;
    private int marker;
    
    public RawPacketReader(final ConnectionState connectionState, final PacketReader reader) {
        this.connectionState = connectionState;
        this.reader = reader;
    }
    
    public boolean isDone() {
        return this.curPkt != null && this.curPkt.isEndOfMessage() && this.curPkt.getNumReadableBytes() == 0;
    }
    
    public int getNumUnreadBytes() {
        return this.curPkt.getNumReadableBytes();
    }
    
    public void resetReadIndex() {
        if (this.curPkt == null) {
            this.readNextPacket();
        }
        this.curPkt.setReadIndex(0);
    }
    
    public void setReadIndex(final int idx) {
        this.curPkt.setReadIndex(idx);
    }
    
    public void resetMarker() {
        this.marker = 0;
    }
    
    public int getMarker() {
        return this.marker;
    }
    
    public RawPacket getCurrentPacket() {
        return this.curPkt;
    }
    
    public void setCurrentPacket(final RawPacket pkt, final int readIdx) {
        (this.curPkt = pkt).setReadIndex(readIdx);
    }
    
    public void readNextPacket() {
        if (this.curPkt != null && this.curPkt.isEndOfMessage()) {
            throw new ServerException("db.mssql.protocol.NoMoreDataToRead", new Object[0]);
        }
        this.curPkt = this.reader.readNextPacket();
        if (this.curPkt == null) {
            throw new ServerException("db.mssql.protocol.ReadTerminated", new Object[0]);
        }
        if (this.curPkt.buffer[0] == 23) {
            final ByteBuffer encryptedBuffer = ByteBuffer.wrap(this.curPkt.buffer);
            final byte[] decryptedBytes = new byte[(int)(this.connectionState.getPacketSize() * 1.2)];
            final ByteBuffer decryptBuffer = ByteBuffer.wrap(decryptedBytes);
            SSLEngineResult sslResult;
            try {
                final SSLEngine sslEngine = this.reader.getSslEngine();
                sslResult = sslEngine.unwrap(encryptedBuffer, decryptBuffer);
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
            final byte[] finalBytes = new byte[sslResult.bytesProduced()];
            System.arraycopy(decryptedBytes, 0, finalBytes, 0, sslResult.bytesProduced());
            this.curPkt = new RawPacket(finalBytes);
        }
        if (this.curPkt.buffer[0] == 83) {
            final SMPPacket smpPacket = new SMPPacket(this.connectionState);
            smpPacket.readFromBytes(this.curPkt.buffer, 0, this.curPkt.buffer.length);
            if (smpPacket.getSMPPacketType() != SMPPacket.SMPPacketType.DATA) {
                throw new ServerException("db.mssql.protocol.UnexpectedSMPPacketType", new Object[] { smpPacket.getSMPPacketType() });
            }
            this.curPkt = new RawPacket(smpPacket.getPayload());
        }
        this.curPkt.setReadIndex(8);
    }
    
    public byte[] readBytes(final int numBytes) {
        if (numBytes == 0) {
            return new byte[0];
        }
        if (numBytes < 0) {
            throw new RuntimeException("Unexpected internal error: negative size for raw packet readBytes");
        }
        final byte[] buffer = new byte[numBytes];
        final int bufferIdx = 0;
        if (this.curPkt.getNumReadableBytes() >= numBytes) {
            this.curPkt.readBytes(buffer, 0, numBytes);
            this.marker += numBytes;
            return buffer;
        }
        int numToRead = this.curPkt.getNumReadableBytes() - bufferIdx;
        int numRead = bufferIdx;
        while (numRead < numBytes - bufferIdx) {
            this.curPkt.readBytes(buffer, numRead, numToRead);
            numRead += numToRead;
            this.marker += numToRead;
            if (this.curPkt.getNumReadableBytes() == 0 && numRead < numBytes - bufferIdx) {
                this.readNextPacket();
            }
            numToRead = Math.min(this.curPkt.getNumReadableBytes(), numBytes - numRead);
            if (numToRead == 0) {
                break;
            }
        }
        return buffer;
    }
    
    public byte readByte() {
        if (this.curPkt == null || this.curPkt.getNumReadableBytes() < 1) {
            this.readNextPacket();
        }
        ++this.marker;
        return this.curPkt.readByte();
    }
    
    public short readTwoByteInt() {
        final byte[] buffer = this.readBytes(2);
        return Shorts.fromBytes(buffer[0], buffer[1]);
    }
    
    public short readTwoByteIntLow() {
        final byte[] buffer = this.readBytes(2);
        return Shorts.fromBytes(buffer[1], buffer[0]);
    }
    
    public int readFourByteInt() {
        final byte[] bytes = this.readBytes(4);
        return Ints.fromBytes(bytes[0], bytes[1], bytes[2], bytes[3]);
    }
    
    public int readFourByteIntLow() {
        final byte[] buffer = this.readBytes(4);
        return Ints.fromBytes(buffer[3], buffer[2], buffer[1], buffer[0]);
    }
    
    public long readUsignedFourByteIntLow() {
        final byte[] bytes = this.readBytes(4);
        return Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, bytes[3], bytes[2], bytes[1], bytes[0]);
    }
    
    public long readEightByteInt() {
        final byte[] bytes = this.readBytes(8);
        return Longs.fromBytes(bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]);
    }
    
    public long readEightByteIntLow() {
        final byte[] bytes = this.readBytes(8);
        return Longs.fromBytes(bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
    }
    
    public long readEightByteDecimal() {
        final byte[] bytes = this.readBytes(8);
        return Longs.fromBytes(bytes[3], bytes[2], bytes[1], bytes[0], bytes[7], bytes[6], bytes[5], bytes[4]);
    }
    
    public String readString(final int numBytes) {
        if (numBytes == 0) {
            return "";
        }
        final byte[] bytes = this.readBytes(numBytes);
        return new String(bytes, 0, numBytes, StandardCharsets.UTF_16LE);
    }
    
    public String readAsciiString(final int numBytes) {
        if (numBytes == 0) {
            return "";
        }
        final byte[] bytes = this.readBytes(numBytes);
        return new String(bytes, 0, numBytes, StandardCharsets.US_ASCII);
    }
    
    public String readStringWithEncoding(final int numBytes, final int collation) {
        if (numBytes == 0) {
            return "";
        }
        Charset charset = null;
        switch (collation) {
            case 1033: {
                charset = StandardCharsets.ISO_8859_1;
                break;
            }
            default: {
                charset = StandardCharsets.ISO_8859_1;
                break;
            }
        }
        final byte[] bytes = this.readBytes(numBytes);
        return new String(bytes, 0, numBytes, charset);
    }
    
    public ByteArray readVarBytes() {
        final ByteArray result = new ByteArray();
        result.size = (int)this.readEightByteIntLow();
        if (result.size == -1) {
            return null;
        }
        if (result.size > 500000000 || result.size < 0) {
            throw new ServerException("db.mssql.protocol.ValueOutOfRange", new Object[] { "readVarBytes too large or negative: " + result.size });
        }
        result.bytes = new byte[result.size];
        if (this.curPkt.getNumReadableBytes() > 0 && this.curPkt.getNumReadableBytes() < 4) {
            throw new ServerException("db.mssql.protocol.BadVarLength", new Object[] { "chunk size is broken up" });
        }
        int chunkSize = this.readFourByteIntLow();
        int numRead = 0;
        while (chunkSize != 0) {
            System.arraycopy(this.curPkt.getBuffer(), this.curPkt.getReadIndex(), result.bytes, numRead, chunkSize);
            this.curPkt.increaseReadIndex(chunkSize);
            numRead += chunkSize;
            chunkSize = this.readFourByteIntLow();
            if (numRead == result.size && chunkSize != 0) {
                throw new ServerException("db.mssql.protocol.BadVarLength", new Object[] { "no terminator" });
            }
        }
        this.marker += numRead;
        return result;
    }
    
    public static class ByteArray
    {
        public int size;
        public byte[] bytes;
    }
}
