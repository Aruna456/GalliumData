// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;

public abstract class StreamHeader
{
    protected int length;
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        this.length = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        final int type = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (type != this.getType()) {
            throw new RuntimeException("Unexpected: StreamHeader has wrong type: " + type + ", expected " + this.getType());
        }
        return idx - offset;
    }
    
    public int getSerializedSize() {
        int size = 0;
        size += 4;
        size += 2;
        return size;
    }
    
    public void write(final RawPacketWriter writer) {
        final int len = this.getSerializedSize();
        writer.writeFourByteIntegerLow(len);
        writer.writeTwoByteIntegerLow(this.getType());
    }
    
    public abstract short getType();
    
    public abstract String getTypeName();
    
    public static StreamHeader createStreamHeader(final byte type) {
        switch (type) {
            case 1: {
                return new StreamHeaderQueryNotifications();
            }
            case 2: {
                return new StreamHeaderTxDescriptor();
            }
            case 3: {
                return new StreamHeaderTraceActivity();
            }
            default: {
                throw new ServerException("db.mssql.protocol.StreamHeaderError", new Object[] { type, "unsupported type" });
            }
        }
    }
}
