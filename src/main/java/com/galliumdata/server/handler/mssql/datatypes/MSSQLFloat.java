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

public class MSSQLFloat extends MSSQLDataType
{
    public MSSQLFloat(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final long num = DataTypeReader.readEightByteIntegerLow(bytes, offset);
        this.value = Double.longBitsToDouble(num);
        return 8;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final long num = reader.readEightByteIntLow();
        this.value = Double.longBitsToDouble(num);
    }
    
    @Override
    public int readVariantBytes(final byte[] bytes, final int offset, final int valueLen) {
        this.meta.getTypeInfo().setVarLen(8);
        final long num = DataTypeReader.readEightByteIntegerLow(bytes, offset);
        this.value = Double.longBitsToDouble(num);
        return 8;
    }
    
    @Override
    public void readVariantBytes(final RawPacketReader reader, final int valueLen) {
        this.meta.getTypeInfo().setVarLen(8);
        final long num = reader.readEightByteIntLow();
        this.value = Double.longBitsToDouble(num);
    }
    
    @Override
    public int getSerializedSize() {
        return 8;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLFloat.NULL_VALUE) {
            writer.writeEightByteIntegerLow(Double.doubleToLongBits(Double.NaN));
            return;
        }
        final double doubleValue = ((Number)this.value).doubleValue();
        writer.writeEightByteIntegerLow(Double.doubleToLongBits(doubleValue));
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLFloat.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        this.value = val.asDouble();
    }
}
