// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.util.BinaryUtil;
import java.math.RoundingMode;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.math.BigInteger;
import com.galliumdata.server.ServerException;
import java.math.BigDecimal;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLDecimal extends MSSQLDataType
{
    private int readLength;
    
    public MSSQLDecimal(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int size = bytes[idx];
        this.readLength = size;
        ++idx;
        int scale = this.meta.getTypeInfo().getScale();
        if (this.meta.getTypeInfo().getVariantScale() > 0) {
            scale = this.meta.getTypeInfo().getVariantScale();
        }
        if (size == 0) {
            this.value = null;
        }
        else if (size == 5) {
            final byte sign = bytes[idx];
            ++idx;
            final long rawValue = DataTypeReader.readUnsignedFourByteIntegerLow(bytes, idx);
            BigDecimal bd = new BigDecimal(rawValue);
            if (scale > 0) {
                final BigDecimal scaleBd = new BigDecimal(10).pow(scale);
                bd = bd.divide(scaleBd);
            }
            if (sign == 0) {
                bd = bd.negate();
            }
            this.value = bd;
        }
        else {
            if (size != 9 && size != 13 && size != 17) {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "Decimal precision not supported: " + this.meta.getTypeInfo().getPrecision() });
            }
            final byte sign = bytes[idx];
            ++idx;
            final long rawLong = DataTypeReader.readEightByteIntegerLow(bytes, idx);
            final BigInteger rawValue2 = BigInteger.valueOf(rawLong);
            BigDecimal bd2 = new BigDecimal(rawValue2, this.meta.getTypeInfo().getScale());
            if (scale > 0) {
                final BigDecimal scaleBd2 = new BigDecimal(10).pow(scale);
                bd2 = bd2.divide(scaleBd2);
            }
            if (sign == 0) {
                bd2 = bd2.negate();
            }
            this.value = bd2;
        }
        return 1 + size;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        int scale = this.meta.getTypeInfo().getScale();
        if (this.meta.getTypeInfo().getVariantScale() > 0) {
            scale = this.meta.getTypeInfo().getVariantScale();
        }
        final int size = reader.readByte();
        if ((this.readLength = size) == 0) {
            this.value = null;
            return;
        }
        if (size == 5) {
            final byte sign = reader.readByte();
            final long rawValue = reader.readUsignedFourByteIntLow();
            BigDecimal bd = new BigDecimal(rawValue);
            if (scale > 0) {
                final BigDecimal scaleBd = new BigDecimal(10).pow(scale);
                bd = bd.divide(scaleBd);
            }
            if (sign == 0) {
                bd = bd.negate();
            }
            this.value = bd;
        }
        else {
            if (size != 9 && size != 13 && size != 17) {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "Decimal precision not supported: " + this.meta.getTypeInfo().getPrecision() });
            }
            final byte sign = reader.readByte();
            final byte[] numBytes = reader.readBytes(size - 1);
            for (int i = 0; i < (size - 1) / 2; ++i) {
                final byte b = numBytes[i];
                numBytes[i] = numBytes[size - 2 - i];
                numBytes[size - 2 - i] = b;
            }
            final BigInteger rawValue2 = new BigInteger(numBytes);
            BigDecimal bd = new BigDecimal(rawValue2, scale);
            if (sign == 0) {
                bd = bd.negate();
            }
            this.value = bd;
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = 1;
        if (this.isNull()) {
            return size;
        }
        BigDecimal bd = ((BigDecimal)this.value).abs();
        if (this.meta.getTypeInfo().getScale() > 0) {
            bd = bd.multiply(BigDecimal.TEN.pow(this.meta.getTypeInfo().getScale()));
        }
        if (bd.compareTo(new BigDecimal(Integer.MAX_VALUE)) < 0) {
            size += 5;
        }
        else if (bd.compareTo(new BigDecimal(Long.MAX_VALUE)) < 0) {
            size += 9;
        }
        else if (bd.compareTo(new BigDecimal("1099511627776")) < 0) {
            size += 13;
        }
        else {
            size += 17;
        }
        return size;
    }
    
    @Override
    public int getVariantSize() {
        return 17;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLDecimal.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        int scale = this.meta.getTypeInfo().getScale();
        BigDecimal bd = (BigDecimal)this.value;
        byte sign = 1;
        if (bd.signum() == -1) {
            sign = 0;
            bd = bd.negate();
        }
        writer.writeByte((byte)this.readLength);
        writer.writeByte(sign);
        final byte[] bytes = new byte[16];
        if (bd.scale() > scale) {
            scale = bd.scale();
            bd = bd.setScale(scale, RoundingMode.DOWN);
        }
        bd = bd.multiply(BigDecimal.TEN.pow(scale));
        final BigInteger biVal = bd.toBigInteger();
        final byte[] byteArray = biVal.toByteArray();
        if (byteArray.length > 16) {
            throw new ServerException("db.mssql.protocol.ValueOutOfRange", new Object[] { "Numeric larger than 16 bytes" });
        }
        BinaryUtil.reverseByteArray(byteArray);
        System.arraycopy(byteArray, 0, bytes, 0, byteArray.length);
        writer.writeBytes(bytes, 0, this.readLength - 1);
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        writer.writeByte((byte)this.meta.getTypeInfo().getPrecision());
        writer.writeByte((byte)this.meta.getTypeInfo().getScale());
        final BigDecimal rawBd = (BigDecimal)this.value;
        writer.writeByte((byte)((rawBd.signum() != -1) ? 1 : 0));
        int scale = this.meta.getTypeInfo().getScale();
        if (this.meta.getTypeInfo().getVariantScale() > 0) {
            scale = this.meta.getTypeInfo().getVariantScale();
        }
        final BigDecimal bd = rawBd.abs().multiply(BigDecimal.TEN.pow(scale));
        final BigInteger bi = bd.toBigInteger();
        final byte[] byteArray = bi.toByteArray();
        if (byteArray.length > 16) {
            throw new ServerException("db.mssql.protocol.ValueOutOfRange", new Object[] { "Decimal larger than 16 bytes" });
        }
        BinaryUtil.reverseByteArray(byteArray);
        final byte[] numBytes = new byte[16];
        System.arraycopy(byteArray, 0, numBytes, 0, byteArray.length);
        writer.writeBytes(numBytes, 0, numBytes.length);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLDecimal.NULL_VALUE;
            return;
        }
        if (!val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "number", val });
        }
        final String str = val.toString();
        try {
            final BigDecimal bd = new BigDecimal(str);
            final int scale = bd.scale();
            if (scale > this.meta.getTypeInfo().getScale()) {
                this.meta.getTypeInfo().setScale(scale);
            }
            this.value = bd;
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "Decimal", "123.456", str });
        }
    }
}
