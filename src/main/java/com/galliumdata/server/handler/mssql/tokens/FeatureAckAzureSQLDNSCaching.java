// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class FeatureAckAzureSQLDNSCaching extends FeatureAck
{
    private byte isSupported;
    
    public FeatureAckAzureSQLDNSCaching(final ConnectionState connectionState) {
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
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { "AzureSQLDNSCaching", 1, length });
        }
        this.isSupported = bytes[idx];
        return ++idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readFourByteIntLow();
        if (length == 0) {
            return;
        }
        if (length != 1) {
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { "AzureSQLDNSCaching", 1, length });
        }
        this.isSupported = reader.readByte();
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        return ++size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeByte(this.isSupported);
    }
    
    @Override
    public byte getFeatureType() {
        return 11;
    }
    
    @Override
    public String getFeatureTypeName() {
        return "AzureSQLDNSCaching";
    }
    
    public byte getIsSupported() {
        return this.isSupported;
    }
    
    public void setIsSupported(final byte b) {
        this.isSupported = b;
    }
}
