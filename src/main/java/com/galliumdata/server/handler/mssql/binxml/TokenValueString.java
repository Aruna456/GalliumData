// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class TokenValueString extends XMLTokenValue
{
    private String s;
    
    public TokenValueString(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        final int len = this.doc.readVarLengthInt();
        this.s = this.doc.readString(len);
    }
    
    @Override
    public String stringValue() {
        return this.s;
    }
    
    @Override
    public byte getType() {
        return 17;
    }
}
