// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

public class ArrayUtil
{
    public static void reverseArray(final byte[] a) {
        if (a == null) {
            return;
        }
        byte tmpByte;
        for (int i = 0, j = a.length - 1; j > i; a[j--] = a[i], a[i++] = tmpByte) {
            tmpByte = a[j];
        }
    }
    
    public static boolean compareByteArrays(final byte[] a1, final byte[] a2) {
        if (a1 == null || a2 == null) {
            return false;
        }
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; ++i) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }
}
