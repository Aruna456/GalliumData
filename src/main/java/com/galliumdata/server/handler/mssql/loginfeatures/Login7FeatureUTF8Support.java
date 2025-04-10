// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.loginfeatures;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;

public class Login7FeatureUTF8Support extends Login7Feature
{
    private Byte value;
    
    @Override
    public String getFeatureType() {
        return "UTF8Support";
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final int len = DataTypeReader.readFourByteIntegerLow(bytes, offset);
        if (len == 0) {
            return 4;
        }
        if (len == 1) {
            this.value = bytes[offset];
            return 5;
        }
        throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { this.getFeatureType(), 0, len });
    }
    
    @Override
    public int getSerializedSize() {
        if (this.value == null) {
            return 5;
        }
        return 6;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeByte((byte)10);
        if (this.value == null) {
            writer.writeFourByteIntegerLow(0);
        }
        else {
            writer.writeFourByteIntegerLow(1);
            writer.writeByte(this.value);
        }
    }
}
