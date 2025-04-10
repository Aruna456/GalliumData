// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

import java.io.ByteArrayOutputStream;

public class ByteWriter
{
    private final ByteArrayOutputStream out;
    
    public ByteWriter() {
        this.out = new ByteArrayOutputStream();
    }
    
    public void writeBytes(final byte[] bytes) {
        this.out.writeBytes(bytes);
    }
    
    public void writeByte(final int b) {
        this.out.write(b);
    }
    
    public void writeShort(final int num) {
        final byte[] buffer = { (byte)(num & 0xFF), (byte)(num >> 8 & 0xFF) };
        this.writeBytes(buffer);
    }
    
    public void writeBigEndianShort(final int num) {
        final byte[] buffer = { (byte)(num >> 8 & 0xFF), (byte)(num & 0xFF) };
        this.writeBytes(buffer);
    }
    
    public void writeInt(final int num) {
        final byte[] buffer = { (byte)(num & 0xFF), (byte)(num >> 8 & 0xFF), (byte)(num >> 16 & 0xFF), (byte)(num >> 24 & 0xFF) };
        this.writeBytes(buffer);
    }
    
    public void writeBigEndianInt(final int num) {
        final byte[] buffer = { (byte)(num >> 24 & 0xFF), (byte)(num >> 16 & 0xFF), (byte)(num >> 8 & 0xFF), (byte)(num & 0xFF) };
        this.writeBytes(buffer);
    }
    
    public void writeLong(final long num) {
        final byte[] buffer = { (byte)(num >> 56 & 0xFFL), (byte)(num >> 48 & 0xFFL), (byte)(num >> 40 & 0xFFL), (byte)(num >> 32 & 0xFFL), (byte)(num >> 24 & 0xFFL), (byte)(num >> 16 & 0xFFL), (byte)(num >> 8 & 0xFFL), (byte)(num & 0xFFL) };
        this.writeBytes(buffer);
    }
    
    public void writeBigEndianLong(final long num) {
        final byte[] buffer = { (byte)(num & 0xFFL), (byte)(num >> 8 & 0xFFL), (byte)(num >> 16 & 0xFFL), (byte)(num >> 24 & 0xFFL), (byte)(num >> 32 & 0xFFL), (byte)(num >> 40 & 0xFFL), (byte)(num >> 48 & 0xFFL), (byte)(num >> 56 & 0xFFL) };
        this.writeBytes(buffer);
    }
    
    public byte[] getBytes() {
        return this.out.toByteArray();
    }
}
