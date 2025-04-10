// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class TokenAttribute extends XMLToken
{
    private int qname;
    
    public TokenAttribute(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        this.qname = this.doc.readVarLengthInt();
        this.doc.write(" " + this.doc.getQName(this.qname));
        while (this.doc.readValue() != null) {}
    }
    
    @Override
    public byte getType() {
        return -10;
    }
}
