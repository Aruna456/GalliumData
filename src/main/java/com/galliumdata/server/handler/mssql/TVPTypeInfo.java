// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.Iterator;
import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TVPTypeInfo extends TypeInfo
{
    private String tvpDbName;
    private String tvpOwningSchema;
    private String tvpTypeName;
    private List<TVPTypeInfoColumnMetadata> columnMetas;
    private TVPOrderUniqueToken orderUniqueToken;
    private TVPColumnOrderingToken columnOrderToken;
    private final List<TVPRow> tvpRows;
    
    public TVPTypeInfo() {
        this.columnMetas = new ArrayList<TVPTypeInfoColumnMetadata>();
        this.tvpRows = new ArrayList<TVPRow>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset);
        byte len = bytes[idx];
        ++idx;
        if (len > 0) {
            this.tvpDbName = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
            idx += len * 2;
        }
        len = bytes[idx];
        ++idx;
        if (len > 0) {
            this.tvpOwningSchema = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
            idx += len * 2;
        }
        len = bytes[idx];
        ++idx;
        if (len > 0) {
            this.tvpTypeName = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
            idx += len * 2;
        }
        short metaCnt = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (metaCnt == -1) {
            metaCnt = 0;
        }
        for (int i = 0; i < metaCnt; ++i) {
            final TVPTypeInfoColumnMetadata meta = new TVPTypeInfoColumnMetadata();
            idx += meta.readFromBytes(bytes, idx, bytes.length);
            this.columnMetas.add(meta);
        }
        byte tokenType = bytes[idx];
        ++idx;
        while (tokenType != 0) {
            if (tokenType == 16) {
                this.orderUniqueToken = new TVPOrderUniqueToken();
                idx += this.orderUniqueToken.readFromBytes(bytes, idx);
            }
            if (tokenType == 17) {
                this.columnOrderToken = new TVPColumnOrderingToken();
                idx += this.columnOrderToken.readFromBytes(bytes, idx);
            }
            tokenType = bytes[idx];
            ++idx;
        }
        tokenType = bytes[idx];
        ++idx;
        while (tokenType != 0) {
            if (tokenType != 1) {
                throw new ServerException("db.mssql.protocol.BadTVPMetadata", new Object[] { "unknown token type (expected 1): " + tokenType });
            }
            final TVPRow row = new TVPRow(this);
            idx += row.readFromBytes(bytes, idx);
            this.tvpRows.add(row);
            tokenType = bytes[idx];
            ++idx;
        }
        return idx - offset;
    }
    
    @Override
    protected int readDataTypeLength(final byte type, final byte[] bytes, final int idx) {
        return super.readDataTypeLength(type, bytes, idx);
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        if (this.tvpDbName != null) {
            final byte[] nameBytes = this.tvpDbName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(nameBytes.length / 2));
            writer.writeBytes(nameBytes, 0, nameBytes.length);
        }
        else {
            writer.writeByte((byte)0);
        }
        if (this.tvpOwningSchema != null) {
            final byte[] nameBytes = this.tvpOwningSchema.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(nameBytes.length / 2));
            writer.writeBytes(nameBytes, 0, nameBytes.length);
        }
        else {
            writer.writeByte((byte)0);
        }
        if (this.tvpTypeName != null) {
            final byte[] nameBytes = this.tvpTypeName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(nameBytes.length / 2));
            writer.writeBytes(nameBytes, 0, nameBytes.length);
        }
        else {
            writer.writeByte((byte)0);
        }
        writer.writeTwoByteIntegerLow(this.columnMetas.size());
        for (final TVPTypeInfoColumnMetadata meta : this.columnMetas) {
            meta.write(writer);
        }
        if (this.orderUniqueToken != null) {
            this.orderUniqueToken.write(writer);
        }
        if (this.columnOrderToken != null) {
            this.columnOrderToken.write(writer);
        }
        writer.writeByte((byte)0);
        for (final TVPRow row : this.tvpRows) {
            row.write(writer);
        }
        writer.writeByte((byte)0);
    }
    
    @Override
    public String toString() {
        return "TVPTypeInfo: " + this.tvpTypeName;
    }
    
    public List<TVPTypeInfoColumnMetadata> getColumnMetas() {
        return this.columnMetas;
    }
    
    public void setColumnMetas(final List<TVPTypeInfoColumnMetadata> columnMetas) {
        this.columnMetas = columnMetas;
    }
    
    public String getTvpDbName() {
        return this.tvpDbName;
    }
    
    public void setTvpDbName(final String tvpDbName) {
        this.tvpDbName = tvpDbName;
    }
    
    public String getTvpOwningSchema() {
        return this.tvpOwningSchema;
    }
    
    public void setTvpOwningSchema(final String tvpOwningSchema) {
        this.tvpOwningSchema = tvpOwningSchema;
    }
    
    public String getTvpTypeName() {
        return this.tvpTypeName;
    }
    
    public void setTvpTypeName(final String tvpTypeName) {
        this.tvpTypeName = tvpTypeName;
    }
    
    public TVPOrderUniqueToken getOrderUniqueToken() {
        return this.orderUniqueToken;
    }
    
    public void setOrderUniqueToken(final TVPOrderUniqueToken orderUniqueToken) {
        this.orderUniqueToken = orderUniqueToken;
    }
    
    public TVPColumnOrderingToken getColumnOrderToken() {
        return this.columnOrderToken;
    }
    
    public void setColumnOrderToken(final TVPColumnOrderingToken columnOrderToken) {
        this.columnOrderToken = columnOrderToken;
    }
    
    public List<TVPRow> getTvpRows() {
        return this.tvpRows;
    }
}
