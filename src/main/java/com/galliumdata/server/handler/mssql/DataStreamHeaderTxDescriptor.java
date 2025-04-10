// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;

public class DataStreamHeaderTxDescriptor extends DataStreamHeader
{
    private long transactionDescription;
    private int outstandingRequestCount;
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int hdrSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (hdrSize != 18) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", new Object[] { "TransactionDescriptor header size != 0x12" });
        }
        final int hdrType = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (hdrType != 2) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", new Object[] { "TransactionDescriptor type != 2" });
        }
        this.transactionDescription = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        idx += 8;
        this.outstandingRequestCount = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (idx - offset != hdrSize) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", new Object[] { "wrong header size for TxDescriptor: " + (idx - offset) + ", expected " + hdrSize });
        }
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        return 18;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeFourByteIntegerLow(18);
        writer.writeTwoByteIntegerLow(2);
        writer.writeEightByteIntegerLow(this.transactionDescription);
        writer.writeFourByteIntegerLow(this.outstandingRequestCount);
    }
    
    @Override
    public String getHeaderType() {
        return "TransactionDescriptor";
    }
    
    public long getTransactionDescription() {
        return this.transactionDescription;
    }
    
    public void setTransactionDescription(final long transactionDescription) {
        this.transactionDescription = transactionDescription;
    }
    
    public int getOutstandingRequestCount() {
        return this.outstandingRequestCount;
    }
    
    public void setOutstandingRequestCount(final int outstandingRequestCount) {
        this.outstandingRequestCount = outstandingRequestCount;
    }
}
