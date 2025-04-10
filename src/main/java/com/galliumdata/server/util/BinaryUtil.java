// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

//import com.galliumdata.server.handler.db2.packets.DataTypeWriter;
import com.google.common.io.BaseEncoding;

public class BinaryUtil
{
    public static void reverseByteArray(final byte[] bytes) {
        for (int len = bytes.length, i = 0; i < len / 2; ++i) {
            final byte b = bytes[i];
            bytes[i] = bytes[len - 1 - i];
            bytes[len - 1 - i] = b;
        }
    }
    
    public static String getHexRepresentationOfBytes(final byte[] bytes) {
        return BaseEncoding.base16().lowerCase().encode(bytes);
    }
    
    public static String getHexRepresentationOfByte(final byte b) {
        return "0x" + BaseEncoding.base16().lowerCase().encode(new byte[] { b });
    }
    
//    public static String getHexRepresentationOfShort(final short i) {
//        final byte[] bytes = new byte[2];
//        DataTypeWriter.encodeTwoByteInteger(bytes, 0, i);
//        return "0x" + BaseEncoding.base16().lowerCase().encode(bytes);
//    }
//
//    public static String getHexRepresentationOfInt(final int i) {
//        final byte[] bytes = new byte[4];
//        DataTypeWriter.encodeFourByteInteger(bytes, 0, i);
//        return "0x" + BaseEncoding.base16().lowerCase().encode(bytes);
//    }
    
    public static String getHexRepresentationOfBytes(final byte[] bytes, final int offset, final int len) {
        final byte[] buf = new byte[len];
        System.arraycopy(bytes, offset, buf, 0, len);
        return getHexRepresentationOfBytes(buf);
    }
    
    public static byte[] getBytesFromHexString(final String s) {
        return BaseEncoding.base16().lowerCase().decode((CharSequence)s);
    }
}
