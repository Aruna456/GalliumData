// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class FeatureAckDataClassification extends FeatureAck
{
    private byte version;
    private byte enabled;
    private byte[] data;
    
    public FeatureAckDataClassification(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        this.version = bytes[idx];
        ++idx;
        this.enabled = bytes[idx];
        ++idx;
        if (this.length > 2) {
            System.arraycopy(bytes, idx, this.data = new byte[this.length - 2], 0, this.length - 2);
            idx += this.length - 2;
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        super.read(reader);
        this.version = reader.readByte();
        this.enabled = reader.readByte();
        if (this.length >= 2) {
            this.data = reader.readBytes(this.length - 2);
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        ++size;
        ++size;
        if (this.data != null) {
            size += this.data.length;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeByte(this.version);
        writer.writeByte(this.enabled);
        if (this.data != null) {
            writer.writeBytes(this.data, 0, this.data.length);
        }
    }
    
    @Override
    public byte getFeatureType() {
        return 9;
    }
    
    @Override
    public String getFeatureTypeName() {
        return "DataClassification";
    }
    
    public byte getVersion() {
        return this.version;
    }
    
    public void setVersion(final byte version) {
        this.version = version;
    }
    
    public byte getEnabled() {
        return this.enabled;
    }
    
    public void setEnabled(final byte enabled) {
        this.enabled = enabled;
    }
    
    public byte[] getData() {
        return this.data;
    }
    
    public void setData(final byte[] data) {
        this.data = data;
    }
}
