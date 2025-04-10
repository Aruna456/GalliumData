// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLSmallInt extends MSSQLDataType
{
    protected static final int MIN_SMALL_INT = -32768;
    protected static final int MAX_SMALL_INT = 32767;
    
    public MSSQLSmallInt(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        this.value = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
        return 2;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        this.value = reader.readTwoByteIntLow();
    }
    
    @Override
    public int getSerializedSize() {
        return 2;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLSmallInt.NULL_VALUE) {
            writer.writeTwoByteIntegerLow(0);
            return;
        }
        writer.writeTwoByteIntegerLow(((Number)this.value).shortValue());
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLSmallInt.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        this.value = val.asShort();
    }
}
