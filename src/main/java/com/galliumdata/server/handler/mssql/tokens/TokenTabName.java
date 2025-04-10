// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import org.graalvm.polyglot.Value;
import java.util.Arrays;
import com.galliumdata.server.js.JSListWrapper;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import java.util.List;

public class TokenTabName extends MessageToken
{
    private final List<List<String>> tableNames;
    
    public TokenTabName(final ConnectionState connectionState) {
        super(connectionState);
        this.tableNames = new ArrayList<List<String>>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (this.connectionState.tdsVersion71andHigher()) {
            final byte numParts = bytes[idx];
            ++idx;
            final List<String> strs = new ArrayList<String>();
            for (int i = 0; i < numParts; ++i) {
                final short partLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
                idx += 2;
                final String part = new String(bytes, idx, partLen * 2, StandardCharsets.UTF_16LE);
                idx += partLen * 2;
                strs.add(part);
            }
            this.tableNames.add(strs);
        }
        else {
            final List<String> strs2 = new ArrayList<String>();
            final short partLen2 = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
            final String part2 = new String(bytes, idx, partLen2 * 2, StandardCharsets.UTF_16LE);
            idx += partLen2 * 2;
            strs2.add(part2);
            this.tableNames.add(strs2);
        }
        if (idx - offset - 3 != length) {
            throw new ServerException("db.mssql.protocol.UnexpectedTokenSize", this.getTokenTypeName(), length, idx - offset - 3);
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readTwoByteIntLow();
        if (this.connectionState.tdsVersion71Revision1andHigher()) {
            int numRead = 0;
            while (numRead < length) {
                final List<String> strs = new ArrayList<String>();
                final byte numParts = reader.readByte();
                ++numRead;
                for (int i = 0; i < numParts; ++i) {
                    final short partLen = reader.readTwoByteIntLow();
                    numRead += 2;
                    final String part = reader.readString(partLen * 2);
                    numRead += partLen * 2;
                    strs.add(part);
                }
                this.tableNames.add(strs);
            }
        }
        else {
            final List<String> strs2 = new ArrayList<String>();
            final short partLen2 = reader.readTwoByteIntLow();
            final String part2 = reader.readString(partLen2 * 2);
            strs2.add(part2);
            this.tableNames.add(strs2);
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        if (this.connectionState.tdsVersion71Revision1andHigher()) {
            for (final List<String> strs : this.tableNames) {
                ++size;
                for (final String s : strs) {
                    size += 2;
                    size += s.length() * 2;
                }
            }
        }
        else {
            size += 2;
            size += this.tableNames.get(0).get(0).length() * 2;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow(this.getSerializedSize() - 3);
        if (this.connectionState.tdsVersion71Revision1andHigher()) {
            for (final List<String> strs : this.tableNames) {
                writer.writeByte((byte)strs.size());
                for (final String part : strs) {
                    writer.writeTwoByteIntegerLow(part.length());
                    final byte[] bytes = part.getBytes(StandardCharsets.UTF_16LE);
                    writer.writeBytes(bytes, 0, bytes.length);
                }
            }
        }
        else {
            writer.writeTwoByteIntegerLow(this.tableNames.get(0).get(0).length());
            final byte[] bytes2 = this.tableNames.get(0).get(0).getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(bytes2, 0, bytes2.length);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -92;
    }
    
    @Override
    public String getTokenTypeName() {
        return "TabName";
    }
    
    public List<List<String>> getTableNames() {
        return this.tableNames;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "tableName": {
                return new JSListWrapper(this.tableNames, () -> {});
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        final String[] parentKeys = (String[])super.getMemberKeys();
        final List<String> keys = Arrays.asList(parentKeys);
        keys.add("tableName");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "tableName": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "tableName": {
                if (value.isString()) {
                    this.tableNames.get(0).clear();
                    this.tableNames.get(0).add(value.asString());
                    break;
                }
                if (value.hasArrayElements()) {
                    this.tableNames.get(0).clear();
                    for (int i = 0; i < value.getArraySize(); ++i) {
                        final Value part = value.getArrayElement(i);
                        if (!part.isString()) {
                            throw new ServerException("db.mssql.logic.ValueHasWrongType", "string", part);
                        }
                        this.tableNames.get(0).add(part.asString());
                    }
                    break;
                }
                break;
            }
            default: {
                super.putMember(key, value);
                break;
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "TabName token");
    }
}
