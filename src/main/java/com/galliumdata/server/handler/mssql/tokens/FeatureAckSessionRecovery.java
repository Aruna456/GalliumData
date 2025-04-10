// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class FeatureAckSessionRecovery extends FeatureAck
{
    private byte[] initSessionStateData;
    
    public FeatureAckSessionRecovery(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (length == 0) {
            return idx - offset;
        }
        System.arraycopy(bytes, idx, this.initSessionStateData = new byte[length], 0, length);
        idx += length;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readFourByteIntLow();
        if (length == 0) {
            return;
        }
        this.initSessionStateData = reader.readBytes(length);
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.initSessionStateData != null) {
            size += this.initSessionStateData.length;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        if (this.initSessionStateData == null) {
            writer.writeFourByteIntegerLow(0);
            return;
        }
        writer.writeBytes(this.initSessionStateData, 0, this.initSessionStateData.length);
    }
    
    @Override
    public byte getFeatureType() {
        return 1;
    }
    
    @Override
    public String getFeatureTypeName() {
        return "SessionRecovery";
    }
    
    public byte[] getInitSessionStateData() {
        return this.initSessionStateData;
    }
    
    public void setInitSessionStateData(final byte[] initSessionStateData) {
        this.initSessionStateData = initSessionStateData;
    }
}
