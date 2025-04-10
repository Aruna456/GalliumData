// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class TokenElement extends XMLToken
{
    private int qname;
    
    public TokenElement(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        this.qname = this.doc.readVarLengthInt();
        this.doc.write("<" + this.doc.getQName(this.qname));
        final boolean attributesComplete = false;
        for (XMLToken token = this.doc.readToken(); token.getType() != -9; token = this.doc.readToken()) {}
        this.doc.write(">");
    }
    
    @Override
    public byte getType() {
        return -8;
    }
}
