// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;

public class StreamHeaderTxDescriptor extends StreamHeader
{
    private long txDescriptor;
    private int outstandingRequestCount;
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        if (this.length != 18) {
            throw new ServerException("db.mssql.protocol.StreamHeaderWrongSize", new Object[] { this.getTypeName(), 18, this.length });
        }
        this.txDescriptor = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        idx += 8;
        this.outstandingRequestCount = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 8;
        size += 4;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeEightByteIntegerLow(this.txDescriptor);
        writer.writeFourByteIntegerLow(this.outstandingRequestCount);
    }
    
    @Override
    public short getType() {
        return 2;
    }
    
    @Override
    public String getTypeName() {
        return "TransactionDescriptor";
    }
    
    public long getTxDescriptor() {
        return this.txDescriptor;
    }
    
    public void setTxDescriptor(final long txDescriptor) {
        this.txDescriptor = txDescriptor;
    }
    
    public int getOutstandingRequestCount() {
        return this.outstandingRequestCount;
    }
    
    public void setOutstandingRequestCount(final int outstandingRequestCount) {
        this.outstandingRequestCount = outstandingRequestCount;
    }
}
