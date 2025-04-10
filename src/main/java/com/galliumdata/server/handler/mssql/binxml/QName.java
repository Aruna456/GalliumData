// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class QName
{
    protected String namespace;
    protected String prefix;
    protected String localName;
    private BinXMLDocument doc;
    
    protected QName(final BinXMLDocument doc) {
        this.doc = doc;
    }
}
