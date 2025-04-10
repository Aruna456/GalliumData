// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.ConnectionState;

public abstract class FeatureAck
{
    protected ConnectionState connectionState;
    protected boolean removed;
    protected int length;
    
    public FeatureAck(final ConnectionState connectionState) {
        this.connectionState = connectionState;
    }
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        final byte type = bytes[idx];
        ++idx;
        if (type != -1 && this.getFeatureType() != 0 && type != this.getFeatureType()) {
            throw new ServerException("db.mssql.protocol.WrongFeatureAck", new Object[] { type, this.getFeatureType() });
        }
        this.length = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        return idx - offset;
    }
    
    public void read(final RawPacketReader reader) {
        this.length = reader.readFourByteIntLow();
    }
    
    public int getSerializedSize() {
        return 5;
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeByte(this.getFeatureType());
        writer.writeFourByteIntegerLow(this.getSerializedSize() - 5);
    }
    
    public abstract byte getFeatureType();
    
    public abstract String getFeatureTypeName();
    
    public void remove() {
        this.removed = true;
    }
}
