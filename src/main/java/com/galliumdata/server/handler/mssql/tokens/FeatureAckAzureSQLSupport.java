// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class FeatureAckAzureSQLSupport extends FeatureAck
{
    private byte isEnabled;
    
    public FeatureAckAzureSQLSupport(final ConnectionState connectionState) {
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
        if (length != 1) {
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { "AzureSQLSupport", 1, length });
        }
        this.isEnabled = bytes[idx];
        return ++idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readFourByteIntLow();
        if (length == 0) {
            return;
        }
        if (length != 1) {
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { "AzureSQLSupport", 1, length });
        }
        this.isEnabled = reader.readByte();
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        return ++size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeByte(this.isEnabled);
    }
    
    @Override
    public byte getFeatureType() {
        return 8;
    }
    
    @Override
    public String getFeatureTypeName() {
        return "AzureSQLSupport";
    }
    
    public byte getIsEnabled() {
        return this.isEnabled;
    }
    
    public void setIsEnabled(final byte b) {
        this.isEnabled = b;
    }
}
