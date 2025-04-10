// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

public class RegexTest
{
    public static void main(final String[] args) throws Exception {
        String s = "This is a \"test\"";
        s = s.replaceAll("\"", "\\\\\"");
        System.out.println("Result: " + s);
    }
}
