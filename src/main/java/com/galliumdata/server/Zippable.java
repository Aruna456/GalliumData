// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server;

import java.io.IOException;

public interface Zippable
{
    byte[] zip() throws IOException;
    
    String getName();
}
