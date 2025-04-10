// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java;

public abstract class HttpExchange
{
    public abstract String getHeader(final String p0);
    
    public abstract boolean hasHeader(final String p0);
    
    public abstract void setHeader(final String p0, final String p1);
}
