// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

public class TokenQNameDef extends XMLToken
{
    public TokenQNameDef(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        final int[] qname = { this.doc.readVarLengthInt(), this.doc.readVarLengthInt(), this.doc.readVarLengthInt() };
        this.doc.recordQName(qname);
    }
    
    @Override
    public byte getType() {
        return -17;
    }
}
