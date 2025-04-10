// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.loginfeatures;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;

public class Login7FeatureColumnEncryption extends Login7Feature
{
    private byte version;
    
    @Override
    public String getFeatureType() {
        return "ColumnEncryption";
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final int len = DataTypeReader.readFourByteIntegerLow(bytes, offset);
        if (len != 1) {
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { this.getFeatureType(), 1, len });
        }
        this.version = bytes[offset + 4];
        return 5;
    }
    
    @Override
    public int getSerializedSize() {
        return 6;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeByte((byte)4);
        writer.writeFourByteIntegerLow(1);
        writer.writeByte(this.version);
    }
    
    public byte getVersion() {
        return this.version;
    }
    
    public void setVersion(final byte version) {
        this.version = version;
    }
}
