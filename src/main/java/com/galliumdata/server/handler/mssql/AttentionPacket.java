// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

public class AttentionPacket extends MSSQLPacket
{
    public AttentionPacket(final ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = 6;
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        return super.readFromBytes(bytes, offset, numBytes);
    }
    
    @Override
    public int getSerializedSize() {
        return super.getSerializedSize();
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.getPacket().setWriteIndex(8);
    }
    
    @Override
    public String getPacketType() {
        return "Attention";
    }
}
