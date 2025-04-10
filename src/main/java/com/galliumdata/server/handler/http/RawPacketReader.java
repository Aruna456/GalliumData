// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import com.galliumdata.server.ServerException;

public class RawPacketReader
{
    private final ConnectionState connectionState;
    private final PacketReader reader;
    private RawPacket curPkt;
    protected long numBytesRead;
    
    public RawPacketReader(final ConnectionState connectionState, final PacketReader reader) {
        this.connectionState = connectionState;
        this.reader = reader;
    }
    
    public int getNumUnreadBytes() {
        return this.curPkt.getNumReadableBytes();
    }
    
    public long getNumBytesRead() {
        return this.numBytesRead;
    }
    
    public RawPacket getCurrentPacket() {
        return this.curPkt;
    }
    
    public void setCurrentPacket(final RawPacket pkt, final int readIdx) {
        (this.curPkt = pkt).setReadIndex(readIdx);
        this.numBytesRead = readIdx;
    }
    
    public void readNextPacket() {
        this.curPkt = this.reader.readNextPacket();
        this.numBytesRead += this.curPkt.getBuffer().length;
    }
    
    public byte[] readBytes(final int numBytes) {
        if (numBytes == 0) {
            return new byte[0];
        }
        if (numBytes < 0) {
            throw new RuntimeException("Unexpected internal error: negative size for raw packet readBytes");
        }
        this.numBytesRead += numBytes;
        final byte[] buffer = new byte[numBytes];
        final int bufferIdx = 0;
        if (this.curPkt.getNumReadableBytes() >= numBytes) {
            this.curPkt.readBytes(buffer, 0, numBytes);
            return buffer;
        }
        int numToRead = this.curPkt.getNumReadableBytes() - bufferIdx;
        int numRead = bufferIdx;
        while (numRead < numBytes - bufferIdx) {
            this.curPkt.readBytes(buffer, numRead, numToRead);
            numRead += numToRead;
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
        ++this.numBytesRead;
        return this.curPkt.readByte();
    }
    
    public int readUnsignedByte() {
        if (this.curPkt == null || this.curPkt.getNumReadableBytes() < 1) {
            this.readNextPacket();
        }
        ++this.numBytesRead;
        return Byte.toUnsignedInt(this.curPkt.readByte());
    }
    
    public void skipBytes(final int num) {
        this.readBytes(num);
    }
    
    public byte[] readRemainingBytes() {
        final int numBytes = this.curPkt.getNumReadableBytes();
        return this.readBytes(numBytes);
    }
    
    public byte peekAtByte(final int idx) {
        if (this.curPkt == null || this.curPkt.getNumReadableBytes() < 1) {
            this.readNextPacket();
        }
        if (idx > this.getNumUnreadBytes()) {
            throw new ServerException("db.http.protocol.InternalError", new Object[] { "peekAtByte beyond available data" });
        }
        return this.curPkt.getBuffer()[this.curPkt.getReadIndex() + idx];
    }
}
