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

public class MSSQLReal extends MSSQLDataType
{
    public MSSQLReal(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final int num = DataTypeReader.readFourByteIntegerLow(bytes, offset);
        this.value = Float.intBitsToFloat(num);
        return 4;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int num = reader.readFourByteIntLow();
        this.value = Float.intBitsToFloat(num);
    }
    
    @Override
    public int getSerializedSize() {
        return 4;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLReal.NULL_VALUE) {
            writer.writeFourByteIntegerLow(0);
            return;
        }
        final float floatValue = ((Number)this.value).floatValue();
        writer.writeFourByteIntegerLow(Float.floatToIntBits(floatValue));
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLReal.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        this.value = val.asFloat();
    }
}
