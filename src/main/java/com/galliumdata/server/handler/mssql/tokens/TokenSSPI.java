// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.List;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenSSPI extends MessageToken
{
    private byte[] sspiBuffer;
    
    public TokenSSPI(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        System.arraycopy(bytes, idx, this.sspiBuffer = new byte[length], 0, length);
        idx += length;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readTwoByteIntLow();
        this.sspiBuffer = reader.readBytes(length);
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        size += this.sspiBuffer.length;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow((short)this.sspiBuffer.length);
        writer.writeBytes(this.sspiBuffer, 0, this.sspiBuffer.length);
    }
    
    @Override
    public byte getTokenType() {
        return -19;
    }
    
    @Override
    public String getTokenTypeName() {
        return "SSPIToken";
    }
    
    public byte[] getSspiBuffer() {
        return this.sspiBuffer;
    }
    
    public void setSspiBuffer(final byte[] sspiBuffer) {
        this.sspiBuffer = sspiBuffer;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "sspiBuffer": {
                return this.sspiBuffer;
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        final String[] parentKeys = (String[])super.getMemberKeys();
        final List<String> keys = new ArrayList<String>();
        for (int i = 0; i < parentKeys.length; ++i) {
            keys.add(parentKeys[i]);
        }
        keys.add("sspiBuffer");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "sspiBuffer": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        if (!"sspiBuffer".equals(key)) {
            super.putMember(key, value);
            return;
        }
        if (value == null || value.isNull()) {
            this.sspiBuffer = null;
            return;
        }
        if (!value.hasArrayElements()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "byte array", value });
        }
        final byte[] newVal = new byte[(int)value.getArraySize()];
        for (int i = 0; i < value.getArraySize(); ++i) {
            newVal[i] = value.getArrayElement((long)i).asByte();
        }
        this.sspiBuffer = newVal;
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "SSPI token" });
    }
}
