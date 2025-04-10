// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLFloatN extends MSSQLDataType
{
    public MSSQLFloatN(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int valueLen = bytes[idx];
        ++idx;
        switch (valueLen) {
            case 0: {
                this.value = null;
                break;
            }
            case 4: {
                final int intVal = DataTypeReader.readFourByteIntegerLow(bytes, idx);
                this.value = Float.intBitsToFloat(intVal);
                break;
            }
            case 8: {
                final long longVal = DataTypeReader.readEightByteIntegerLow(bytes, idx);
                this.value = Double.longBitsToDouble(longVal);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "FloatN length not supported: " + valueLen });
            }
        }
        return 1 + valueLen;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int valueLen = reader.readByte();
        switch (valueLen) {
            case 0: {
                this.value = MSSQLFloatN.NULL_VALUE;
                break;
            }
            case 4: {
                final int intVal = reader.readFourByteIntLow();
                this.value = Float.intBitsToFloat(intVal);
                break;
            }
            case 8: {
                final long longVal = reader.readEightByteIntLow();
                this.value = Double.longBitsToDouble(longVal);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "FloatN length not supported: " + valueLen });
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = 1;
        if (this.isNull()) {
            return size;
        }
        if (this.value instanceof Float) {
            size += 4;
        }
        else if (this.value instanceof Double) {
            size += 8;
        }
        else {
            if (!(this.value instanceof Number)) {
                throw new ServerException("db.mssql.logic.InvalidType", new Object[] { this.meta.getColumnName(), this.value.getClass() });
            }
            size += 8;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLFloatN.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        final byte[] bytes = new byte[9];
        int size;
        if (this.meta.getTypeInfo().getVarLen() == 4) {
            final int flInt = Float.floatToIntBits(((Number)this.value).floatValue());
            DataTypeWriter.encodeFourByteIntegerLow(bytes, 1, flInt);
            size = 4;
        }
        else {
            if (this.meta.getTypeInfo().getVarLen() != 8) {
                throw new ServerException("db.mssql.logic.InvalidType", new Object[] { this.meta.getColumnName(), this.value.getClass() });
            }
            final long dblLong = Double.doubleToLongBits(((Number)this.value).doubleValue());
            DataTypeWriter.encodeEightByteIntegerLow(bytes, 1, dblLong);
            size = 8;
        }
        bytes[0] = (byte)size;
        writer.writeBytes(bytes, 0, size + 1);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLFloatN.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        this.value = val.asDouble();
    }
}
