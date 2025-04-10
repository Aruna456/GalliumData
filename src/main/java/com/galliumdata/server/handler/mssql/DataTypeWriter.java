// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

public class DataTypeWriter
{
    public static void encodeTwoByteInteger(final byte[] buffer, final int offset, final short num) {
        buffer[offset + 0] = (byte)(num >> 8 & 0xFF);
        buffer[offset + 1] = (byte)(num & 0xFF);
    }
    
    public static void encodeTwoByteIntegerLow(final byte[] buffer, final int offset, final short num) {
        buffer[offset + 0] = (byte)(num & 0xFF);
        buffer[offset + 1] = (byte)(num >> 8 & 0xFF);
    }
    
    public static void encodeFourByteInteger(final byte[] buffer, final int offset, final int num) {
        buffer[offset + 0] = (byte)(num >> 24 & 0xFF);
        buffer[offset + 1] = (byte)(num >> 16 & 0xFF);
        buffer[offset + 2] = (byte)(num >> 8 & 0xFF);
        buffer[offset + 3] = (byte)(num & 0xFF);
    }
    
    public static void encodeFourByteIntegerLow(final byte[] buffer, final int offset, final int num) {
        buffer[offset + 3] = (byte)(num >> 24 & 0xFF);
        buffer[offset + 2] = (byte)(num >> 16 & 0xFF);
        buffer[offset + 1] = (byte)(num >> 8 & 0xFF);
        buffer[offset + 0] = (byte)(num & 0xFF);
    }
    
    public static void encodeEightByteInteger(final byte[] buffer, final int offset, final long num) {
        buffer[offset + 0] = (byte)(num >> 56 & 0xFFL);
        buffer[offset + 1] = (byte)(num >> 48 & 0xFFL);
        buffer[offset + 2] = (byte)(num >> 40 & 0xFFL);
        buffer[offset + 3] = (byte)(num >> 32 & 0xFFL);
        buffer[offset + 4] = (byte)(num >> 24 & 0xFFL);
        buffer[offset + 5] = (byte)(num >> 16 & 0xFFL);
        buffer[offset + 6] = (byte)(num >> 8 & 0xFFL);
        buffer[offset + 7] = (byte)(num & 0xFFL);
    }
    
    public static void encodeEightByteIntegerLow(final byte[] buffer, final int offset, final long num) {
        buffer[offset + 7] = (byte)(num >> 56 & 0xFFL);
        buffer[offset + 6] = (byte)(num >> 48 & 0xFFL);
        buffer[offset + 5] = (byte)(num >> 40 & 0xFFL);
        buffer[offset + 4] = (byte)(num >> 32 & 0xFFL);
        buffer[offset + 3] = (byte)(num >> 24 & 0xFFL);
        buffer[offset + 2] = (byte)(num >> 16 & 0xFFL);
        buffer[offset + 1] = (byte)(num >> 8 & 0xFFL);
        buffer[offset + 0] = (byte)(num & 0xFFL);
    }
    
    public static void encodeEightByteDecimal(final byte[] buffer, final int offset, final long num) {
        buffer[offset + 7] = (byte)(num >> 24 & 0xFFL);
        buffer[offset + 6] = (byte)(num >> 16 & 0xFFL);
        buffer[offset + 5] = (byte)(num >> 8 & 0xFFL);
        buffer[offset + 4] = (byte)(num >> 0 & 0xFFL);
        buffer[offset + 3] = (byte)(num >> 56 & 0xFFL);
        buffer[offset + 2] = (byte)(num >> 48 & 0xFFL);
        buffer[offset + 1] = (byte)(num >> 40 & 0xFFL);
        buffer[offset + 0] = (byte)(num >> 32 & 0xFFL);
    }
    
    public static void encodeEightByteNumber(final byte[] buffer, final int offset, final long num) {
        buffer[offset + 0] = (byte)(num >> 0 & 0xFFL);
        buffer[offset + 1] = (byte)(num >> 8 & 0xFFL);
        buffer[offset + 2] = (byte)(num >> 16 & 0xFFL);
        buffer[offset + 3] = (byte)(num >> 24 & 0xFFL);
        buffer[offset + 4] = (byte)(num >> 32 & 0xFFL);
        buffer[offset + 5] = (byte)(num >> 40 & 0xFFL);
        buffer[offset + 6] = (byte)(num >> 48 & 0xFFL);
        buffer[offset + 7] = (byte)(num >> 56 & 0xFFL);
    }
}
