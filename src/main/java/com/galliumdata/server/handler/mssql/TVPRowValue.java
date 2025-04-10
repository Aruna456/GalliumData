// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;

public class TVPRowValue
{
    private final TypeInfo typeInfo;
    protected int len;
    protected MSSQLDataType value;
    private byte extraVariantByte;
    
    public TVPRowValue(final TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }
    
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final ColumnMetadata colMeta = new ColumnMetadata(null);
        colMeta.setTypeInfo(this.typeInfo);
        (this.value = MSSQLDataType.createDataType(colMeta)).setResizable(true);
        if (this.typeInfo.getType() == 106 || this.typeInfo.getType() == 108) {
            this.typeInfo.setVariantScale(6);
        }
        idx += this.value.readFromBytes(bytes, idx);
        return idx - offset;
    }
    
    public void write(final RawPacketWriter writer) {
        this.value.write(writer);
    }
}
