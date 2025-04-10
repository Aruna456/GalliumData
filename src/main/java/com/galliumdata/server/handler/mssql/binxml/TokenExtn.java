// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class TokenExtn extends XMLToken
{
    public TokenExtn(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        final int len = this.doc.readVarLengthInt();
        this.doc.recordExtension(this.doc.readBytes(len));
    }
    
    @Override
    public byte getType() {
        return -22;
    }
}
