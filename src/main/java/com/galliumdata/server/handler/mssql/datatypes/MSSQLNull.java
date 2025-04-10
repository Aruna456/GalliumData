// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLNull extends MSSQLDataType
{
    public static final MSSQLNull NULL;
    
    public MSSQLNull(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        return 0;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
    }
    
    @Override
    public int getSerializedSize() {
        return 0;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        throw new ServerException("db.mssql.logic.CannotSetNull", new Object[0]);
    }
    
    static {
        NULL = new MSSQLNull(null);
    }
}
