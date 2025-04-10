// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class TokenNameDef extends XMLToken
{
    public TokenNameDef(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        final int len = this.doc.readVarLengthInt();
        final String s = this.doc.readString(len);
        this.doc.recordName(s);
    }
    
    @Override
    public byte getType() {
        return -16;
    }
}
