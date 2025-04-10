// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.Arrays;
import com.galliumdata.server.js.JSListWrapper;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import java.util.List;

public class TokenOrder extends MessageToken
{
    private List<Integer> colNums;
    
    public TokenOrder(final ConnectionState connectionState) {
        super(connectionState);
        this.colNums = new ArrayList<Integer>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        for (int i = 0; i < len; i += 2) {
            final int colNum = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
            this.colNums.add(colNum);
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        for (int len = reader.readTwoByteIntLow(), i = 0; i < len; i += 2) {
            final int colNum = reader.readTwoByteIntLow();
            this.colNums.add(colNum);
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        size += this.colNums.size() * 2;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow((short)(this.colNums.size() * 2));
        for (final int colNum : this.colNums) {
            writer.writeTwoByteIntegerLow((short)colNum);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -87;
    }
    
    @Override
    public String getTokenTypeName() {
        return "Order";
    }
    
    @Override
    public String toString() {
        String s = "Order by ";
        for (int i : this.colNums) {
            s += i;
        }
        return s;
    }
    
    public List<Integer> getColNums() {
        return this.colNums;
    }
    
    public void setColNums(final List<Integer> colNums) {
        this.colNums = colNums;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "colNums": {
                return new JSListWrapper(this.colNums, () -> {});
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
        keys.add("colNums");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "colNums": {
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
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "Order token");
    }
}
