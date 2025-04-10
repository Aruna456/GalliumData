// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

import org.apache.commons.codec.binary.Hex;

public class HexUtil
{
    public static byte[] decodeContinuousBytes(String s) {
        s = s.replaceAll(" ", "");
        if (s.length() % 2 == 1) {
            throw new RuntimeException("Byte string has odd number of characters");
        }
        try {
            return Hex.decodeHex(s);
        }
        catch (final Exception ex) {
            throw new RuntimeException("Unable to decode hex data: " + String.valueOf(ex));
        }
    }
}
