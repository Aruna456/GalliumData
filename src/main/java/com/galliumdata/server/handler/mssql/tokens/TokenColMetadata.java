// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.Collection;
import java.util.Arrays;
import com.galliumdata.server.js.JSListWrapper;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class TokenColMetadata extends MessageToken implements ProxyObject
{
    private List<EncryptionKeyInfo> keyInfos;
    private List<ColumnMetadata> columns;
    
    public TokenColMetadata(final ConnectionState connectionState) {
        super(connectionState);
        this.keyInfos = new ArrayList<EncryptionKeyInfo>();
        this.columns = new ArrayList<ColumnMetadata>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int count = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (this.connectionState.isColumnEncryptionInUse()) {
            final int ckCount = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
            for (int i = 0; i < ckCount; ++i) {
                final EncryptionKeyInfo eki = new EncryptionKeyInfo();
                idx += eki.readFromBytes(bytes, idx, numBytes - (idx - offset));
                this.keyInfos.add(eki);
            }
        }
        if (bytes[idx] == -1 && bytes[idx + 1] == -1) {
            return idx - offset;
        }
        for (int j = 0; j < count; ++j) {
            final ColumnMetadata col = new ColumnMetadata(this.connectionState);
            idx += col.readFromBytes(bytes, idx, numBytes - (idx - offset));
            this.columns.add(col);
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final short count = reader.readTwoByteIntLow();
        if (count == -1) {
            return;
        }
        if (this.connectionState.isColumnEncryptionInUse()) {
            for (int ckCount = reader.readTwoByteIntLow(), i = 0; i < ckCount; ++i) {
                final EncryptionKeyInfo eki = new EncryptionKeyInfo();
                eki.read(reader);
                this.keyInfos.add(eki);
            }
        }
        for (int j = 0; j < count; ++j) {
            final ColumnMetadata col = new ColumnMetadata(this.connectionState);
            col.read(reader);
            this.columns.add(col);
        }
        this.connectionState.setLastMetadata(this);
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        if (this.connectionState.isColumnEncryptionInUse()) {
            size += 2;
            for (final EncryptionKeyInfo eki : this.keyInfos) {
                size += eki.getSerializedSize();
            }
        }
        if (this.columns.isEmpty()) {
            size += 2;
        }
        for (final ColumnMetadata meta : this.columns) {
            size += meta.getSerializedSize();
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        final List<ColumnMetadata> cols = this.columns;
        if (cols.isEmpty()) {
            writer.writeTwoByteIntegerLow(-1);
            return;
        }
        writer.writeTwoByteIntegerLow((short)this.columns.size());
        if (this.connectionState.isColumnEncryptionInUse()) {
            writer.writeTwoByteIntegerLow((short)this.keyInfos.size());
            for (final EncryptionKeyInfo eki : this.keyInfos) {
                eki.write(writer);
            }
        }
        if (this.columns.isEmpty()) {
            writer.writeByte((byte)(-1));
            writer.writeByte((byte)(-1));
        }
        for (final ColumnMetadata meta : this.columns) {
            meta.write(writer);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -127;
    }
    
    @Override
    public String getTokenTypeName() {
        return "ColMetadata";
    }
    
    @Override
    public String toString() {
        String s = "Column metadata: ";
        for (ColumnMetadata col : this.columns) {
            String name = col.getColumnName();
            if (name == null || name.isBlank()) {
                name = "<unnamed>";
            }
            s += name;
            if (s.length() > 100) {
                s = s.substring(0, 100) + "...";
            }
        }
        return s;
    }
    
    @Override
    public String toLongString() {
        String s = "Column metadata: ";
        for (ColumnMetadata col : this.columns) {
            String name = col.getColumnName();
            if (name == null || name.isBlank()) {
                name = "<unnamed>";
            }
            s += name;
        }
        return s;
    }
    
    public List<EncryptionKeyInfo> getKeyInfos() {
        return this.keyInfos;
    }
    
    public void setKeyInfos(final List<EncryptionKeyInfo> keyInfos) {
        this.keyInfos = keyInfos;
    }
    
    public List<ColumnMetadata> getColumns() {
        return this.columns;
    }
    
    public void setColumns(final List<ColumnMetadata> columns) {
        this.columns = columns;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "keyInfos": {
                return new JSListWrapper(this.keyInfos, () -> {});
            }
            case "columns": {
                return new JSListWrapper(this.columns, () -> {});
            }
            case "remove": {
                return (Function<Value[],Object>) arguments -> {
                    this.remove();
                    return null;
                };
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        final String[] parentKeys = (String[])super.getMemberKeys();
        final List<String> keys = new ArrayList<String>(Arrays.asList(parentKeys));
        keys.add("keyInfos");
        keys.add("columns");
        keys.add("remove");
        keys.add("toString");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "keyInfos":
            case "columns":
            case "remove":
            case "toString": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        throw new ServerException("db.mssql.logic.NoSuchMember", key);
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "Error token");
    }
}
