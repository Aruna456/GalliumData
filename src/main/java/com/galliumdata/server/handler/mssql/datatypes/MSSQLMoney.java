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

public class MSSQLMoney extends MSSQLDataType
{
    private static final BigDecimal TEN_THOUSAND;
    
    public MSSQLMoney(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final long rawValue = DataTypeReader.readEightByteDecimal(bytes, offset);
        final BigDecimal bd = new BigDecimal(rawValue);
        this.value = bd.divide(MSSQLMoney.TEN_THOUSAND, 4, RoundingMode.UNNECESSARY);
        return 8;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final long rawValue = reader.readEightByteDecimal();
        final BigDecimal bd = new BigDecimal(rawValue);
        this.value = bd.divide(MSSQLMoney.TEN_THOUSAND, 4, RoundingMode.UNNECESSARY);
    }
    
    @Override
    public int getSerializedSize() {
        return 8;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLMoney.NULL_VALUE) {
            writer.writeEightByteDecimal(0L);
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
        bd = bd.multiply(MSSQLMoney.TEN_THOUSAND);
        final long longVal = bd.longValue();
        writer.writeEightByteDecimal(longVal);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLMoney.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        final String str = val.toString();
        try {
            this.value = new BigDecimal(str);
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "Money", "123.456", str });
        }
    }
    
    static {
        TEN_THOUSAND = BigDecimal.TEN.pow(4);
    }
}
