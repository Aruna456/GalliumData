// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenFedAuthInfo extends MessageToken
{
    private byte[] data;
    
    public TokenFedAuthInfo(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        System.arraycopy(bytes, idx, this.data = new byte[length], 0, length);
        idx += length;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readTwoByteIntLow();
        this.data = reader.readBytes(length);
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        size += this.data.length;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow(this.data.length);
        writer.writeBytes(this.data, 0, this.data.length);
    }
    
    @Override
    public byte getTokenType() {
        return -18;
    }
    
    @Override
    public String getTokenTypeName() {
        return "FedAuthInfo";
    }
    
    @Override
    public String toString() {
        return "FedAuthInfo: " + this.data.length + " bytes";
    }
    
    public byte[] getData() {
        return this.data;
    }
    
    public void setData(final byte[] data) {
        this.data = data;
    }
}
