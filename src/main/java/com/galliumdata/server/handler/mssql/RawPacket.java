// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import java.util.concurrent.atomic.AtomicLong;

public class RawPacket
{
    public byte[] buffer;
    public long id;
    private static final AtomicLong idGen;
    private final long ts;
    private int readIndex;
    private int writeIndex;
    private boolean finalized;
    private int smpSessionId;
    protected int realLength;
    
    public RawPacket(final byte[] buffer) {
        this.id = RawPacket.idGen.incrementAndGet();
        this.ts = System.nanoTime();
        this.readIndex = 0;
        this.writeIndex = 0;
        this.smpSessionId = -1;
        this.buffer = buffer;
    }
    
    public RawPacket(final RawPacket orig) {
        this.id = RawPacket.idGen.incrementAndGet();
        this.ts = System.nanoTime();
        this.readIndex = 0;
        this.writeIndex = 0;
        this.smpSessionId = -1;
        this.buffer = new byte[orig.getBuffer().length];
        this.id = orig.id;
        this.smpSessionId = orig.smpSessionId;
        System.arraycopy(orig.getBuffer(), 0, this.buffer, 0, orig.getBuffer().length);
    }
    
    public byte[] getBuffer() {
        return this.buffer;
    }
    
    public byte[] getWrittenBuffer() {
        if (this.writeIndex == this.buffer.length) {
            return this.buffer;
        }
        final byte[] buf = new byte[this.writeIndex];
        System.arraycopy(this.buffer, 0, buf, 0, this.writeIndex);
        return buf;
    }
    
    public void addBytes(final byte[] addBytes, final int offset) {
        final byte[] newBuffer = new byte[this.buffer.length + addBytes.length - offset];
        System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.length);
        System.arraycopy(addBytes, offset, newBuffer, this.buffer.length, addBytes.length - offset);
        this.buffer = newBuffer;
    }
    
    public byte getTypeCode() {
        if (this.buffer == null || this.buffer.length == 0) {
            throw new ServerException("db.mssql.protocol.EmptyPacket", new Object[0]);
        }
        return this.buffer[0];
    }
    
    public int getReadIndex() {
        return this.readIndex;
    }
    
    public void setReadIndex(final int idx) {
        this.readIndex = idx;
    }
    
    public void increaseReadIndex(final int delta) {
        this.readIndex += delta;
        if (this.readIndex > this.buffer.length) {
            throw new RuntimeException("Internal error: RawPacket readIndex > buffer length");
        }
    }
    
    public int getWriteIndex() {
        return this.writeIndex;
    }
    
    public void setWriteIndex(final int idx) {
        this.writeIndex = idx;
    }
    
    public int getNumReadableBytes() {
        return this.buffer.length - this.readIndex;
    }
    
    public int getRemainingBytesToWrite() {
        return this.buffer.length - this.writeIndex;
    }
    
    public byte readByte() {
        if (this.getNumReadableBytes() < 1) {
            throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "Underflow in RawPacket.readByte" });
        }
        final byte b = this.buffer[this.readIndex];
        ++this.readIndex;
        if (this.readIndex > this.buffer.length) {
            throw new RuntimeException("Internal error: RawPacket readIndex > buffer length");
        }
        return b;
    }
    
    public void readBytes(final byte[] buf, final int idx, final int numBytes) {
        if (this.getNumReadableBytes() < numBytes) {
            throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "Underflow in RawPacket.readBytes" });
        }
        if (numBytes < 0) {
            throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "Negative numBytes in RawPacket.readBytes" });
        }
        System.arraycopy(this.buffer, this.readIndex, buf, idx, numBytes);
        this.readIndex += numBytes;
        if (this.readIndex > this.buffer.length) {
            throw new RuntimeException("Internal error: RawPacket readIndex > buffer length");
        }
    }
    
    public void writeBytes(final byte[] bytes, final int idx, final int numBytes) {
        if (numBytes > this.getRemainingBytesToWrite()) {
            throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "Overflow in RawPacket" });
        }
        System.arraycopy(bytes, idx, this.buffer, this.writeIndex, numBytes);
        this.writeIndex += numBytes;
    }
    
    public byte getStatus() {
        return this.buffer[1];
    }
    
    public void setStatus(final byte status) {
        this.buffer[1] = status;
    }
    
    public boolean isEndOfMessage() {
        return (this.buffer[1] & 0x1) != 0x0;
    }
    
    public void setEndOfMessage(final boolean b) {
        this.buffer[1] = (byte)(b ? 1 : 0);
    }
    
    public short getSpid() {
        return DataTypeReader.readTwoByteInteger(this.buffer, 4);
    }
    
    public void setSpid(final short spid) {
        DataTypeWriter.encodeTwoByteInteger(this.buffer, 4, spid);
    }
    
    public byte getPacketId() {
        return this.buffer[6];
    }
    
    public void setPacketId(final int id) {
        this.buffer[6] = (byte)id;
    }
    
    public byte getWindow() {
        return this.buffer[7];
    }
    
    public void setWindow(final byte window) {
        this.buffer[7] = window;
    }
    
    public void finalizeLength() {
        if (this.isWrappedInSMP()) {
            DataTypeWriter.encodeFourByteInteger(this.buffer, 4, (short)this.writeIndex);
            DataTypeWriter.encodeTwoByteInteger(this.buffer, 18, (short)(this.writeIndex - 16));
        }
        else {
            DataTypeWriter.encodeTwoByteInteger(this.buffer, 2, (short)this.writeIndex);
        }
        if (this.buffer.length > this.writeIndex) {
            final byte[] newBuffer = new byte[this.writeIndex];
            System.arraycopy(this.buffer, 0, newBuffer, 0, this.writeIndex);
            this.buffer = newBuffer;
        }
        this.finalized = true;
    }
    
    public boolean isFinalized() {
        return this.finalized;
    }
    
    public boolean isWrappedInSMP() {
        return this.smpSessionId != -1;
    }
    
    public int getSMPSessionId() {
        return this.smpSessionId;
    }
    
    public void setSMPSessionId(final int i) {
        this.smpSessionId = i;
    }
    
    static {
        idGen = new AtomicLong();
    }
}
