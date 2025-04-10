// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import java.math.BigInteger;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import java.math.BigDecimal;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLMoneyN extends MSSQLDataType
{
    public MSSQLMoneyN(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int valueLen = bytes[idx];
        ++idx;
        if (valueLen == 0) {
            this.value = null;
        }
        else if (valueLen == 4) {
            final int rawValue = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            BigDecimal bd = new BigDecimal(rawValue);
            bd = bd.divide(new BigDecimal(10000));
            this.value = bd;
        }
        else {
            if (valueLen != 8) {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "MoneyN length not supported: " + valueLen });
            }
            final long rawValue2 = DataTypeReader.readEightByteDecimal(bytes, idx);
            BigDecimal bd2 = new BigDecimal(rawValue2);
            bd2 = bd2.divide(new BigDecimal(10000));
            this.value = bd2;
        }
        return 1 + valueLen;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int valueLen = reader.readByte();
        if (valueLen == 0) {
            this.value = null;
        }
        else if (valueLen == 4) {
            final int rawValue = reader.readFourByteIntLow();
            BigDecimal bd = new BigDecimal(rawValue);
            bd = bd.divide(new BigDecimal(10000));
            this.value = bd;
        }
        else {
            if (valueLen != 8) {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "MoneyN length not supported: " + valueLen });
            }
            final long rawValue2 = reader.readEightByteDecimal();
            BigDecimal bd2 = new BigDecimal(rawValue2);
            bd2 = bd2.divide(new BigDecimal(10000));
            this.value = bd2;
        }
    }
    
    @Override
    public int getSerializedSize() {
        return 1 + this.meta.getTypeInfo().getVarLen();
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLMoneyN.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        final byte[] bytes = new byte[9];
        BigDecimal bd;
        if (this.value instanceof Byte || this.value instanceof Short || this.value instanceof Integer || this.value instanceof Long) {
            bd = BigDecimal.valueOf(((Number)this.value).longValue());
        }
        else if (this.value instanceof Float || this.value instanceof Double) {
            bd = BigDecimal.valueOf(((Number)this.value).doubleValue());
        }
        else if (this.value instanceof BigInteger) {
            bd = new BigDecimal((BigInteger)this.value);
        }
        else {
            if (!(this.value instanceof BigDecimal)) {
                throw new ServerException("db.mssql.logic.InvalidType", new Object[] { this.meta.getColumnName(), this.value.getClass() });
            }
            bd = (BigDecimal)this.value;
        }
        bd = bd.multiply(BigDecimal.TEN.pow(4));
        int size;
        if (this.meta.getTypeInfo().getVarLen() == 4) {
            final int intVal = bd.intValue();
            DataTypeWriter.encodeFourByteIntegerLow(bytes, 1, intVal);
            size = 4;
        }
        else {
            if (this.meta.getTypeInfo().getVarLen() != 8) {
                throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "money", this.meta.getTypeInfo().getVarLen(), this.meta.getColumnName() });
            }
            final long longVal = bd.longValue();
            DataTypeWriter.encodeEightByteDecimal(bytes, 1, longVal);
            size = 8;
        }
        bytes[0] = (byte)size;
        writer.writeBytes(bytes, 0, size + 1);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLMoneyN.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        final String str = val.toString();
        BigDecimal bd;
        try {
            bd = new BigDecimal(str);
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "MoneyN", "123.456", str });
        }
        if (this.meta.getTypeInfo().getVarLen() == 4) {
            if (bd.compareTo(MSSQLSmallMoney.MIN_SMALL_MONEY) == -1) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { str, "SmallMoney" });
            }
            if (bd.compareTo(MSSQLSmallMoney.MAX_SMALL_MONEY) == 1) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { str, "SmallMoney" });
            }
        }
        this.value = bd;
    }
}
