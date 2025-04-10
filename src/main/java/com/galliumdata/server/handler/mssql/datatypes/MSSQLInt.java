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

public class MSSQLInt extends MSSQLDataType
{
    protected static final int MIN_INT = Integer.MIN_VALUE;
    protected static final int MAX_INT = Integer.MAX_VALUE;
    
    public MSSQLInt(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        this.value = DataTypeReader.readFourByteIntegerLow(bytes, offset);
        return 4;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        this.value = reader.readFourByteIntLow();
    }
    
    @Override
    public int getSerializedSize() {
        return 4;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLInt.NULL_VALUE) {
            writer.writeFourByteIntegerLow(0);
            return;
        }
        writer.writeFourByteIntegerLow(((Number)this.value).intValue());
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLInt.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        this.value = val.asInt();
    }
}
