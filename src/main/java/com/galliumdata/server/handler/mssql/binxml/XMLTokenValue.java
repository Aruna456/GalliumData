// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public abstract class XMLTokenValue extends XMLToken
{
    public XMLTokenValue(final BinXMLDocument doc) {
        super(doc);
    }
    
    public abstract String stringValue();
    
    @Override
    public boolean isValue() {
        return true;
    }
}
