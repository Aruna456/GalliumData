// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLNumeric extends MSSQLDecimal
{
    public MSSQLNumeric(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        super.writeVariant(writer);
    }
}
