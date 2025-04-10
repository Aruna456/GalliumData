// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;

public class DataStreamHeaderTraceActivity extends DataStreamHeader
{
    private byte[] activityId;
    private long activitySequence;
    
    public DataStreamHeaderTraceActivity() {
        this.activityId = new byte[16];
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int hdrSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (hdrSize != 24) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", new Object[] { "TransactionDescriptor header size != 0x18" });
        }
        final int hdrType = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (hdrType != 3) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", new Object[] { "TraceActivity type != 3" });
        }
        System.arraycopy(bytes, idx, this.activityId, 0, 16);
        idx += 16;
        this.activitySequence = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        idx += 8;
        if (idx - offset != hdrSize) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", new Object[] { "wrong header size for TraceActivity: " + (idx - offset) + ", expected " + hdrSize });
        }
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        return 30;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeFourByteIntegerLow(24);
        writer.writeTwoByteIntegerLow(3);
        writer.writeBytes(this.activityId, 0, 16);
        writer.writeEightByteIntegerLow(this.activitySequence);
    }
    
    @Override
    public String getHeaderType() {
        return "TraceActivity";
    }
    
    public byte[] getActivityId() {
        return this.activityId;
    }
    
    public void setActivityId(final byte[] activityId) {
        this.activityId = activityId;
    }
    
    public long getActivitySequence() {
        return this.activitySequence;
    }
    
    public void setActivitySequence(final long activitySequence) {
        this.activitySequence = activitySequence;
    }
}
