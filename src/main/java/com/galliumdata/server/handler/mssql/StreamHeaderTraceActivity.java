// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.UUID;

public class StreamHeaderTraceActivity extends StreamHeader
{
    private UUID activityId;
    private int activitySequence;
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        final long highBites = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        idx += 8;
        final long lowBits = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        idx += 8;
        this.activityId = new UUID(highBites, lowBits);
        this.activitySequence = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 16;
        size += 4;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeEightByteIntegerLow(this.activityId.getMostSignificantBits());
        writer.writeEightByteIntegerLow(this.activityId.getLeastSignificantBits());
        writer.writeFourByteIntegerLow(this.activitySequence);
    }
    
    @Override
    public short getType() {
        return 3;
    }
    
    @Override
    public String getTypeName() {
        return "TraceActivity";
    }
}
