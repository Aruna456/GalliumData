// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.loginfeatures;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;

public class Login7FeatureAzureSQLDNSCaching extends Login7Feature
{
    @Override
    public String getFeatureType() {
        return "AzureSQLDNSCaching";
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final int len = DataTypeReader.readFourByteIntegerLow(bytes, offset);
        if (len != 0) {
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { this.getFeatureType(), 0, len });
        }
        return 4;
    }
    
    @Override
    public int getSerializedSize() {
        return 5;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeByte((byte)11);
        writer.writeFourByteIntegerLow(0);
    }
}
