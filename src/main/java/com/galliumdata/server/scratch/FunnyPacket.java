// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

public class FunnyPacket
{
    private byte[] bytes;
    private int offset;
    private int len;
    
    public FunnyPacket(final byte[] bytes, final int offset, final int len) {
        this.bytes = bytes;
        this.offset = offset;
        this.len = len;
        if (len == 0) {
            throw new RuntimeException("Packet cannot be length 0");
        }
        final byte lastByte = bytes[offset + len - 1];
        if (lastByte != -2 && lastByte != -1) {
            throw new RuntimeException("Packet does not end with expected marker");
        }
    }
    
    public boolean isFinalPacket() {
        return this.bytes[this.offset + this.len - 1] == -1;
    }
    
    @Override
    public String toString() {
        return " offset: " + this.offset + ", length: " + this.len;
    }
}
