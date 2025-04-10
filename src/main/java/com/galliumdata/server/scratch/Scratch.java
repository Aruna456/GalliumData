// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.scratch;

import java.nio.charset.Charset;

public class Scratch
{
    public static void main(final String[] args) throws Exception {
        System.out.println("Parsed: " + String.valueOf(Charset.availableCharsets().keySet()));
    }
}
