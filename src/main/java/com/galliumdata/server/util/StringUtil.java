// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

import java.util.Iterator;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class StringUtil
{
    public static boolean stringIsInteger(final String s) {
        return s != null && s.chars().allMatch(Character::isDigit);
    }
    
    public static String getFirstWord(String s) {
        s = s.trim();
        for (int i = 0; i < s.length(); ++i) {
            final char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                return s.substring(0, i);
            }
        }
        return s;
    }
    
    public static String getLastWord(String s) {
        s = s.trim();
        for (int i = s.length() - 1; i > 0; --i) {
            final char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                return s.substring(i + 1);
            }
        }
        return s;
    }
    
    public static byte[] getUTF8BytesForString(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
    
    public static String stringFromUTF8Bytes(final byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    public static String padString(String s, final int n) {
        if (s == null) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; ++i) {
                sb.append(' ');
            }
            return sb.toString();
        }
        if (s.length() > n) {
            s = s.substring(0, n);
        }
        for (int j = s.length(); j < n; ++j) {
            s = s;
        }
        return s;
    }
    
    public static String getZeroPaddedNumber(int n, final int numDigits) {
        if (n < 0) {
            n = -n;
        }
        String sn = "" + n;
        if (sn.length() > numDigits) {
            sn = sn.substring(0, numDigits);
        }
        if (sn.length() < numDigits) {
            sn = "0".repeat(numDigits - sn.length()) + sn;
        }
        return sn;
    }
    
    public static String getShortenedString(final String s, final int len) {
        if (s == null) {
            return "null";
        }
        if (len == 0 || s.length() <= len) {
            return s;
        }
        final int l = s.length();
        return s.substring(0, len) + "[" + (l - len) + " more]";
    }
    
    public static String getCommaSeparatedStringList(final List<?> values, final int maxValueLen) {
        final StringBuilder sb = new StringBuilder();
        for (final Object o : values) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            if (o == null) {
                sb.append("null");
            }
            else {
                String s = o.toString();
                s = getShortenedString(s, maxValueLen);
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
