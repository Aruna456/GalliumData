// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public abstract class XMLToken
{
    protected BinXMLDocument doc;
    
    public XMLToken(final BinXMLDocument doc) {
        this.doc = doc;
    }
    
    public abstract void read();
    
    public abstract byte getType();
    
    public boolean isValue() {
        return false;
    }
}
