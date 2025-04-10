// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class FeatureAckUTF8Support extends FeatureAck
{
    private byte data;
    
    public FeatureAckUTF8Support(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        if (this.length != 1) {
            throw new ServerException("db.mssql.protocol.InvalidFeatureAckLength", new Object[] { this.getFeatureTypeName(), 1, this.length });
        }
        this.data = bytes[idx];
        return ++idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        super.read(reader);
        if (this.length != 1) {
            throw new ServerException("db.mssql.protocol.InvalidFeatureAckLength", new Object[] { this.getFeatureTypeName(), 1, this.length });
        }
        this.data = reader.readByte();
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        return ++size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeByte(this.data);
    }
    
    @Override
    public byte getFeatureType() {
        return 10;
    }
    
    @Override
    public String getFeatureTypeName() {
        return "UTF8Support";
    }
    
    public byte getData() {
        return this.data;
    }
    
    public void setData(final byte data) {
        this.data = data;
    }
}
