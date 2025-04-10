// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;

public abstract class DataStreamHeader
{
    public static DataStreamHeader createDataStreamHeader(final byte[] bytes, final int offset) {
        final int type = DataTypeReader.readTwoByteIntegerLow(bytes, offset + 4);
        switch (type) {
            case 1: {
                return new DataStreamHeaderQueryNotifications();
            }
            case 2: {
                return new DataStreamHeaderTxDescriptor();
            }
            case 3: {
                return new DataStreamHeaderTraceActivity();
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownDataStreamHeader", new Object[] { type });
            }
        }
    }
    
    public abstract int readFromBytes(final byte[] p0, final int p1);
    
    public abstract int getSerializedSize();
    
    public abstract void write(final RawPacketWriter p0);
    
    public abstract String getHeaderType();
}
