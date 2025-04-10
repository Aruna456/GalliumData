// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.loginfeatures;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.DataTypeReader;

public class Login7FeatureUnknown extends Login7Feature
{
    private byte featureTypeCode;
    private byte[] data;
    
    @Override
    public String getFeatureType() {
        return "Unknown";
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        this.featureTypeCode = bytes[idx];
        ++idx;
        final int featureSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        System.arraycopy(bytes, idx, this.data = new byte[featureSize], 0, featureSize);
        idx += featureSize;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        return 5 + this.data.length;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeByte((byte)11);
        writer.writeFourByteIntegerLow(this.data.length);
        writer.writeBytes(this.data, 0, this.data.length);
    }
    
    public byte getFeatureTypeCode() {
        return this.featureTypeCode;
    }
    
    public void setFeatureTypeCode(final byte featureType) {
        this.featureTypeCode = featureType;
    }
    
    public byte[] getData() {
        return this.data;
    }
    
    public void setData(final byte[] data) {
        this.data = data;
    }
}
