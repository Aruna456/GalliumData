// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.google.common.primitives.Longs;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

public class DataTypeReader
{
    public static int getUnsignedByte(final byte b) {
        return b & 0xFF;
    }
    
    public static byte readByte(final byte[] bytes, final int offset) {
        return (byte)(bytes[offset] & 0xFF);
    }
    
    public static short readTwoByteInteger(final byte[] bytes, final int offset) {
        return Shorts.fromBytes(bytes[offset + 0], bytes[offset + 1]);
    }
    
    public static short readTwoByteIntegerLow(final byte[] bytes, final int offset) {
        return Shorts.fromBytes(bytes[offset + 1], bytes[offset + 0]);
    }
    
    public static int readFourByteInteger(final byte[] bytes, final int offset) {
        return Ints.fromBytes(bytes[offset + 0], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3]);
    }
    
    public static int readFourByteIntegerLow(final byte[] bytes, final int offset) {
        return Ints.fromBytes(bytes[offset + 3], bytes[offset + 2], bytes[offset + 1], bytes[offset + 0]);
    }
    
    public static long readUnsignedFourByteIntegerLow(final byte[] bytes, final int offset) {
        return Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, bytes[offset + 3], bytes[offset + 2], bytes[offset + 1], bytes[offset + 0]);
    }
    
    public static long readEightByteInteger(final byte[] bytes, final int offset) {
        return Longs.fromBytes(bytes[offset + 0], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4], bytes[offset + 5], bytes[offset + 6], bytes[offset + 7]);
    }
    
    public static long readEightByteIntegerLow(final byte[] bytes, final int offset) {
        return Longs.fromBytes(bytes[offset + 7], bytes[offset + 6], bytes[offset + 5], bytes[offset + 4], bytes[offset + 3], bytes[offset + 2], bytes[offset + 1], bytes[offset + 0]);
    }
    
    public static long readEightByteDecimal(final byte[] bytes, final int offset) {
        return Longs.fromBytes(bytes[offset + 3], bytes[offset + 2], bytes[offset + 1], bytes[offset + 0], bytes[offset + 7], bytes[offset + 6], bytes[offset + 5], bytes[offset + 4]);
    }
}
