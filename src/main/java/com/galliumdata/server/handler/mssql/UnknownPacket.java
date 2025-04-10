// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.List;
import java.util.Arrays;

public class UnknownPacket extends MSSQLPacket
{
    private byte[] data;
    
    public UnknownPacket(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        final int dataLen = this.length - 8;
        System.arraycopy(bytes, idx, this.data = new byte[dataLen], 0, dataLen);
        return this.length;
    }
    
    @Override
    public int getSerializedSize() {
        return 8 + this.data.length;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeBytes(this.data, 0, this.data.length);
    }
    
    @Override
    public String getPacketType() {
        return "Unknown";
    }
    
    @Override
    public String toString() {
        return "Unknown message: " + this.data.length + " bytes";
    }
    
    public byte[] getData() {
        return this.data;
    }
    
    public void setData(final byte[] data) {
        this.data = data;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "data": {
                return this.data;
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
        keys.add("data");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "data": {
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
            case "data": {
                if (!value.hasArrayElements()) {
                    throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "byte array", value });
                }
                final int arraySize = (int)value.getArraySize();
                final byte[] bytes = new byte[arraySize];
                for (int i = 0; i < arraySize; ++i) {
                    final Value elem = value.getArrayElement((long)i);
                    if (!elem.fitsInByte()) {
                        throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "byte", elem });
                    }
                    bytes[i] = elem.asByte();
                }
                this.setData(bytes);
                return;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "Unknown packet" });
    }
}
