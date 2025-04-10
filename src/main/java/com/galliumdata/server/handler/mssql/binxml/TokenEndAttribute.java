// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class TokenEndAttribute extends XMLToken
{
    public TokenEndAttribute(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        this.doc.write(">");
    }
    
    @Override
    public byte getType() {
        return -11;
    }
}
