// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLBit extends MSSQLDataType
{
    public MSSQLBit(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        this.value = bytes[offset];
        return 1;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        this.value = reader.readByte();
    }
    
    @Override
    public int getSerializedSize() {
        return 1;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLBit.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        writer.writeByte((byte)this.value);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        if (val == null || val.isNull()) {
            this.value = MSSQLBit.NULL_VALUE;
            this.changed = true;
            return;
        }
        if (!val.isBoolean() && !val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "boolean", val });
        }
        if (val.isNumber()) {
            this.value = (byte)((val.asInt() != 0) ? 1 : 0);
        }
        else {
            this.value = (byte)(val.asBoolean() ? 1 : 0);
        }
        this.changed = true;
    }
}
