// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLTinyInt extends MSSQLDataType
{
    protected static final int MIN_TINY_INT = 0;
    protected static final int MAX_TINY_INT = 255;
    
    public MSSQLTinyInt(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        this.value = Byte.toUnsignedInt(bytes[offset]);
        return 1;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        this.value = Byte.toUnsignedInt(reader.readByte());
    }
    
    @Override
    public int getSerializedSize() {
        return 1;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLTinyInt.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        writer.writeByte(((Number)this.value).byteValue());
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLTinyInt.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        this.value = val.asByte();
    }
}
