// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class TokenEndElement extends XMLToken
{
    public TokenEndElement(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
    }
    
    @Override
    public byte getType() {
        return -9;
    }
}
