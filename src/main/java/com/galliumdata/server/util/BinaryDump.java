// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

public class BinaryDump
{
    private static final char[] hexChars;
    
    public static String getBinaryDump(final byte[] bytes) {
        return getBinaryDump(bytes, 0, bytes.length);
    }
    
    public static String getBinaryDump(final byte[] bytes, final int offset, final int length) {
        if (length == 0) {
            return "[empty array]";
        }
        int idx = offset;
        final StringBuffer sb = new StringBuffer();
        while (idx - offset < length) {
            final StringBuffer asciiSb = new StringBuffer();
            int rowIdx;
            for (rowIdx = 0; rowIdx < 16 && idx + rowIdx - offset < length; ++rowIdx) {
                addByte(sb, asciiSb, bytes[idx + rowIdx]);
            }
            idx += rowIdx;
            sb.append("   ".repeat(Math.max(0, 16 - rowIdx)));
            sb.append("  ");
            sb.append(asciiSb);
            sb.append('\n');
        }
        return sb.toString();
    }
    
    private static void addByte(final StringBuffer sb, final StringBuffer sb2, final byte b) {
        int idx = b;
        if (idx < 0) {
            idx += 256;
        }
        sb.append(BinaryDump.hexChars[idx / 16]);
        sb.append(BinaryDump.hexChars[idx % 16]);
        sb.append(' ');
        if (idx >= 32 && idx <= 126) {
            sb2.append((char)idx);
        }
        else {
            sb2.append('.');
        }
    }
    
    static {
        hexChars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    }
}
