// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class FeatureAckUnknown extends FeatureAck
{
    private byte type;
    private byte[] data;
    
    public FeatureAckUnknown(final ConnectionState connectionState, final byte type) {
        super(connectionState);
        this.type = type;
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        this.type = bytes[idx];
        idx += super.readFromBytes(bytes, idx, numBytes);
        System.arraycopy(bytes, idx, this.data = new byte[this.length], 0, this.length);
        idx += this.length;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        super.read(reader);
        this.data = reader.readBytes(this.length);
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += this.data.length;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeBytes(this.data, 0, this.data.length);
    }
    
    @Override
    public byte getFeatureType() {
        return this.type;
    }
    
    @Override
    public String getFeatureTypeName() {
        return "Unknown: " + this.type;
    }
    
    public byte getType() {
        return this.type;
    }
    
    public void setType(final byte type) {
        this.type = type;
    }
    
    public byte[] getData() {
        return this.data;
    }
    
    public void setData(final byte[] data) {
        this.data = data;
    }
}
