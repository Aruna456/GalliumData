// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.Arrays;
import com.galliumdata.server.js.JSListWrapper;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import java.util.List;

public class TokenColInfo extends MessageToken
{
    private final List<ColInfo> colInfos;
    
    public TokenColInfo(final ConnectionState connectionState) {
        super(connectionState);
        this.colInfos = new ArrayList<ColInfo>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        throw new RuntimeException("SHOULD NOT BE CALLED");
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readTwoByteIntLow();
        reader.resetMarker();
        while (reader.getMarker() < length) {
            final ColInfo colInfo = new ColInfo();
            colInfo.read(reader);
            this.colInfos.add(colInfo);
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        for (final ColInfo colInfo : this.colInfos) {
            size += colInfo.getSerializedSize();
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        int colInfoSize = 0;
        for (final ColInfo colInfo : this.colInfos) {
            colInfoSize += colInfo.getSerializedSize();
        }
        writer.writeTwoByteIntegerLow(colInfoSize);
        for (final ColInfo colInfo : this.colInfos) {
            colInfo.write(writer);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -91;
    }
    
    @Override
    public String getTokenTypeName() {
        return "ColInfo";
    }
    
    public List<ColInfo> getColInfos() {
        return this.colInfos;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "colInfos": {
                return new JSListWrapper(this.colInfos, () -> {});
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
        keys.add("colInfos");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "colInfos": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        super.putMember(key, value);
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "ColInfo token");
    }
}
