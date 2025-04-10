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

public class MSSQLIntN extends MSSQLDataType
{
    public MSSQLIntN(final ColumnMetadata meta) {
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
            case 1: {
                this.value = Byte.toUnsignedInt(bytes[idx]);
                break;
            }
            case 2: {
                this.value = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
                break;
            }
            case 4: {
                this.value = DataTypeReader.readFourByteIntegerLow(bytes, idx);
                break;
            }
            case 8: {
                this.value = DataTypeReader.readEightByteIntegerLow(bytes, idx);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "IntN length not supported: " + valueLen });
            }
        }
        return 1 + valueLen;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int valueLen = reader.readByte();
        switch (valueLen) {
            case 0: {
                this.value = null;
                break;
            }
            case 1: {
                this.value = Byte.toUnsignedInt(reader.readByte());
                break;
            }
            case 2: {
                this.value = reader.readTwoByteIntLow();
                break;
            }
            case 4: {
                this.value = reader.readFourByteIntLow();
                break;
            }
            case 8: {
                this.value = reader.readEightByteIntLow();
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "IntN length not supported: " + valueLen });
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        return 1 + this.meta.getTypeInfo().getVarLen();
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLIntN.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        final byte[] bytes = new byte[9];
        final int varLen = this.meta.getTypeInfo().getVarLen();
        final long longVal = ((Number)this.value).longValue();
        int size = 0;
        switch (varLen) {
            case 1: {
                bytes[1] = (byte)longVal;
                size = 1;
                break;
            }
            case 2: {
                DataTypeWriter.encodeTwoByteIntegerLow(bytes, 1, (short)longVal);
                size = 2;
                break;
            }
            case 4: {
                DataTypeWriter.encodeFourByteIntegerLow(bytes, 1, (int)longVal);
                size = 4;
                break;
            }
            case 8: {
                DataTypeWriter.encodeEightByteIntegerLow(bytes, 1, longVal);
                size = 8;
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "IntN", varLen, this.meta.getColumnName() });
            }
        }
        bytes[0] = (byte)size;
        writer.writeBytes(bytes, 0, size + 1);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLIntN.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        final long newVal = val.asLong();
        final int varLen = this.meta.getTypeInfo().getVarLen();
        if (varLen == 1) {
            if (newVal < 0L) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { newVal, "TinyInt" });
            }
            if (newVal > 255L) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { newVal, "TinyInt" });
            }
        }
        else if (varLen == 2) {
            if (newVal < -32768L) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { newVal, "SmallInt" });
            }
            if (newVal > 32767L) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { newVal, "SmallInt" });
            }
        }
        else if (varLen == 4) {
            if (newVal < -2147483648L) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { newVal, "Int" });
            }
            if (newVal > 2147483647L) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { newVal, "Int" });
            }
        }
        this.value = val.asLong();
    }
}
