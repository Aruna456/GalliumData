// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.loginfeatures;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;

public class Login7FeatureGlobalTransactions extends Login7Feature
{
    @Override
    public String getFeatureType() {
        return "GlobalTransactions";
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final int length = DataTypeReader.readFourByteIntegerLow(bytes, offset);
        if (length != 0) {
            throw new ServerException("db.mssql.protocol.IncorrectLoginFeature", new Object[] { "GlobalTransactions", "invalid length: " + length + ", expected 0" });
        }
        return 4;
    }
    
    @Override
    public int getSerializedSize() {
        return 5;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeByte((byte)5);
        writer.writeFourByteIntegerLow(0);
    }
}
