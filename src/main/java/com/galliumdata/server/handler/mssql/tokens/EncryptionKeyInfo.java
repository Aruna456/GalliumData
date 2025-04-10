// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import java.util.ArrayList;
import java.util.List;

public class EncryptionKeyInfo
{
    private int databaseId;
    private int cekId;
    private int cekVersion;
    private long cekMetadataVersion;
    private List<EncryptionKeyValue> keyValues;
    
    public EncryptionKeyInfo() {
        this.keyValues = new ArrayList<EncryptionKeyValue>();
    }
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        this.databaseId = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.cekId = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.cekVersion = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.cekMetadataVersion = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        idx += 8;
        final byte numValues = bytes[idx];
        ++idx;
        for (int i = 0; i < numValues; ++i) {
            final EncryptionKeyValue keyValue = new EncryptionKeyValue();
            idx += keyValue.readFromBytes(bytes, idx, numBytes - (idx - offset));
            this.keyValues.add(keyValue);
        }
        return idx - offset;
    }
    
    public void read(final RawPacketReader reader) {
        this.databaseId = reader.readFourByteIntLow();
        this.cekId = reader.readFourByteIntLow();
        this.cekVersion = reader.readFourByteIntLow();
        this.cekMetadataVersion = reader.readEightByteIntLow();
        final byte numValues = reader.readByte();
        for (int i = 0; i < numValues; ++i) {
            final EncryptionKeyValue keyValue = new EncryptionKeyValue();
            keyValue.read(reader);
            this.keyValues.add(keyValue);
        }
    }
    
    public int getSerializedSize() {
        final int size = 0;
        return size;
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeFourByteIntegerLow(this.databaseId);
        writer.writeFourByteIntegerLow(this.cekId);
        writer.writeFourByteIntegerLow(this.cekVersion);
        writer.writeEightByteIntegerLow(this.cekMetadataVersion);
        writer.writeByte((byte)this.keyValues.size());
        for (final EncryptionKeyValue keyValue : this.keyValues) {
            keyValue.write(writer);
        }
    }
    
    public int getDatabaseId() {
        return this.databaseId;
    }
    
    public void setDatabaseId(final int databaseId) {
        this.databaseId = databaseId;
    }
    
    public int getCekId() {
        return this.cekId;
    }
    
    public void setCekId(final int cekId) {
        this.cekId = cekId;
    }
    
    public int getCekVersion() {
        return this.cekVersion;
    }
    
    public void setCekVersion(final int cekVersion) {
        this.cekVersion = cekVersion;
    }
    
    public long getCekMetadataVersion() {
        return this.cekMetadataVersion;
    }
    
    public void setCekMetadataVersion(final long cekMetadataVersion) {
        this.cekMetadataVersion = cekMetadataVersion;
    }
    
    public List<EncryptionKeyValue> getKeyValues() {
        return this.keyValues;
    }
    
    public void setKeyValues(final List<EncryptionKeyValue> keyValues) {
        this.keyValues = keyValues;
    }
}
