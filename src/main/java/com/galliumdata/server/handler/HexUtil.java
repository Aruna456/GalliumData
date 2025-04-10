// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

public class HexUtil
{
    private static final char[] HEX_DIGITS;
    
    public static String formatHex(final byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            final int highBits = bytes[i] & 0xF0;
            sb.append(HexUtil.HEX_DIGITS[highBits >> 4]);
            final int lowBits = bytes[i] & 0xF;
            sb.append(HexUtil.HEX_DIGITS[lowBits]);
        }
        return sb.toString();
    }
    
    public static String formatHex(final byte[] bytes, final int offset, final int length) {
        if (bytes == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length; ++i) {
            final int highBits = bytes[i] & 0xF0;
            sb.append(HexUtil.HEX_DIGITS[highBits >> 4]);
            final int lowBits = bytes[i] & 0xF;
            sb.append(HexUtil.HEX_DIGITS[lowBits]);
        }
        return sb.toString();
    }
    
    public static byte[] readHexString(String s) {
        s = s.trim().toUpperCase();
        final byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            bytes[i / 2] = (byte)(getHexAsByte(s.charAt(i)) << 4);
            final byte[] array = bytes;
            final int n = i / 2;
            array[n] += getHexAsByte(s.charAt(i + 1));
        }
        return bytes;
    }
    
    public static int getIntForByte(final byte b) {
        if (b < 0) {
            return b + 256;
        }
        return b;
    }
    
    public static String getIntAsHex(final int i) {
        String s = "";
        s += HexUtil.HEX_DIGITS[(i & 0xF000) >> 12];
        s += HexUtil.HEX_DIGITS[(i & 0xF00) >> 8];
        s += HexUtil.HEX_DIGITS[(i & 0xF0) >> 4];
        s += HexUtil.HEX_DIGITS[i & 0xF];
        return s;
    }
    
    public static byte getHexAsByte(final char c) {
        if (c >= 'A') {
            return (byte)(c - 'A' + 10);
        }
        return (byte)(c - '0');
    }
    
    public static int getArraySize(final byte[] bytes) {
        if (bytes == null) {
            return 0;
        }
        return bytes.length;
    }
    
    static {
        HEX_DIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    }
}
