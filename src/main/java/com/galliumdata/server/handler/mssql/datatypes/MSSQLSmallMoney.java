// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import java.math.BigInteger;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.math.RoundingMode;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import java.math.BigDecimal;

public class MSSQLSmallMoney extends MSSQLDataType
{
    private static final BigDecimal TEN_THOUSAND;
    protected static final BigDecimal MIN_SMALL_MONEY;
    protected static final BigDecimal MAX_SMALL_MONEY;
    
    public MSSQLSmallMoney(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final int rawValue = DataTypeReader.readFourByteIntegerLow(bytes, offset);
        BigDecimal bd = new BigDecimal(rawValue);
        bd = bd.divide(MSSQLSmallMoney.TEN_THOUSAND, 4, RoundingMode.UNNECESSARY);
        this.value = bd;
        return 4;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int rawValue = reader.readFourByteIntLow();
        BigDecimal bd = new BigDecimal(rawValue);
        bd = bd.divide(MSSQLSmallMoney.TEN_THOUSAND, 4, RoundingMode.UNNECESSARY);
        this.value = bd;
    }
    
    @Override
    public int getSerializedSize() {
        return 4;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLSmallMoney.NULL_VALUE) {
            writer.writeFourByteIntegerLow(0);
            return;
        }
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
        bd = bd.multiply(MSSQLSmallMoney.TEN_THOUSAND);
        final int intVal = bd.intValue();
        writer.writeFourByteIntegerLow(intVal);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLSmallMoney.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        final String str = val.toString();
        try {
            final BigDecimal bd = new BigDecimal(str);
            if (bd.compareTo(MSSQLSmallMoney.MIN_SMALL_MONEY) == -1) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { str, "SmallMoney" });
            }
            if (bd.compareTo(MSSQLSmallMoney.MAX_SMALL_MONEY) == 1) {
                throw new ServerException("db.mssql.logic.DataTypeOutOfRange", new Object[] { str, "SmallMoney" });
            }
            this.value = bd;
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "SmallMoney", "123.456", str });
        }
    }
    
    static {
        TEN_THOUSAND = BigDecimal.TEN.pow(4);
        MIN_SMALL_MONEY = new BigDecimal("-214748.3648");
        MAX_SMALL_MONEY = new BigDecimal("214748.3647");
    }
}
