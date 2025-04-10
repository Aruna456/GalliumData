// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.loginfeatures;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.DataTypeReader;

public class Login7FeatureSessionRecovery extends Login7Feature
{
    private SessionRecoveryData initSessionRecoveryData;
    private SessionRecoveryData sessionRecoveryDataToBe;
    
    @Override
    public String getFeatureType() {
        return "SessionRecovery";
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int totalLength = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (totalLength == 0) {
            return idx - offset;
        }
        this.initSessionRecoveryData = new SessionRecoveryData();
        idx += this.initSessionRecoveryData.readFromBytes(bytes, idx);
        this.sessionRecoveryDataToBe = new SessionRecoveryData();
        idx += this.sessionRecoveryDataToBe.readFromBytes(bytes, idx);
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        if (this.initSessionRecoveryData == null) {
            return 5;
        }
        return 5 + this.initSessionRecoveryData.getSerializedSize() + this.sessionRecoveryDataToBe.getSerializedSize();
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeByte((byte)1);
        if (this.initSessionRecoveryData == null) {
            writer.writeFourByteIntegerLow(0);
            return;
        }
        writer.writeFourByteIntegerLow(this.initSessionRecoveryData.getSerializedSize() + this.sessionRecoveryDataToBe.getSerializedSize());
        this.initSessionRecoveryData.write(writer);
        this.sessionRecoveryDataToBe.write(writer);
    }
    
    public static class SessionRecoveryData
    {
        private byte[] data;
        
        public int readFromBytes(final byte[] bytes, final int offset) {
            int idx = offset;
            final int length = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
            System.arraycopy(bytes, idx, this.data = new byte[length], 0, length);
            idx += length;
            return idx - offset;
        }
        
        public int getSerializedSize() {
            int size = 4;
            size += this.data.length;
            return size;
        }
        
        public void write(final RawPacketWriter writer) {
            writer.writeFourByteIntegerLow(this.data.length);
            writer.writeBytes(this.data, 0, this.data.length);
        }
    }
}
