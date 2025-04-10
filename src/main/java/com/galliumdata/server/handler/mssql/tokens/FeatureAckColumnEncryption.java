// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class FeatureAckColumnEncryption extends FeatureAck
{
    private byte version;
    private String enclaveType;
    
    public FeatureAckColumnEncryption(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        this.version = bytes[idx];
        ++idx;
        if (this.length >= 2) {
            final byte len = bytes[idx];
            ++idx;
            this.enclaveType = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
            idx += len * 2;
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        super.read(reader);
        this.version = reader.readByte();
        if (this.length >= 2) {
            final byte len = reader.readByte();
            this.enclaveType = reader.readString(len);
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        ++size;
        if (this.enclaveType != null) {
            size += 1 + this.enclaveType.length() * 2;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeByte(this.version);
        if (this.enclaveType != null) {
            final byte[] enclaveBytes = this.enclaveType.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(enclaveBytes.length / 2));
            writer.writeBytes(enclaveBytes, 0, enclaveBytes.length);
        }
    }
    
    @Override
    public byte getFeatureType() {
        return 4;
    }
    
    @Override
    public String getFeatureTypeName() {
        return "ColumnEncryption";
    }
    
    public byte getVersion() {
        return this.version;
    }
    
    public void setVersion(final byte version) {
        this.version = version;
    }
    
    public String getEnclaveType() {
        return this.enclaveType;
    }
    
    public void setEnclaveType(final String enclaveType) {
        this.enclaveType = enclaveType;
    }
}
