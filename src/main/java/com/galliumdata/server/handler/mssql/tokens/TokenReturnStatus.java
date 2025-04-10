// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenReturnStatus extends MessageToken
{
    private int value;
    
    public TokenReturnStatus(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        this.value = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        this.value = reader.readFourByteIntLow();
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 4;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeFourByteIntegerLow(this.value);
    }
    
    @Override
    public byte getTokenType() {
        return 121;
    }
    
    @Override
    public String getTokenTypeName() {
        return "ReturnStatus";
    }
    
    @Override
    public String toString() {
        return "ReturnStatus: " + this.value;
    }
    
    public int getValue() {
        return this.value;
    }
    
    public void setValue(final int value) {
        this.value = value;
    }
}
