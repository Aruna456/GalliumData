// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.time.LocalDateTime;
import java.math.BigInteger;
import com.galliumdata.server.ServerException;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLDateTime2N extends MSSQLDataType
{
    public MSSQLDateTime2N(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int valueLen = bytes[idx];
        idx = ++idx + this.readVariantBytes(bytes, idx, valueLen);
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int valueLen = reader.readByte();
        this.readVariantBytes(reader, valueLen);
    }
    
    @Override
    public int readVariantBytes(final byte[] bytes, final int offset, final int valueLen) {
        int idx = offset;
        if (valueLen == 0) {
            this.value = MSSQLDateTime2N.NULL_VALUE;
            return 0;
        }
        long timeVal;
        long numDays;
        if (valueLen == this.getLongTimeSize()) {
            timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, bytes[idx + 4], bytes[idx + 3], bytes[idx + 2], bytes[idx + 1], bytes[idx + 0]);
            numDays = Ints.fromBytes((byte)0, bytes[idx + 7], bytes[idx + 6], bytes[idx + 5]);
            idx += 8;
        }
        else if (valueLen == this.getLongTimeSize() - 1) {
            timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, bytes[idx + 3], bytes[idx + 2], bytes[idx + 1], bytes[idx + 0]);
            numDays = Ints.fromBytes((byte)0, bytes[idx + 6], bytes[idx + 5], bytes[idx + 4]);
            idx += 7;
        }
        else {
            if (valueLen != this.getLongTimeSize() - 2) {
                throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateTime2N", valueLen, this.meta.getColumnName() });
            }
            timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, bytes[idx + 3], bytes[idx + 2], bytes[idx + 1], bytes[idx + 0]);
            numDays = Ints.fromBytes((byte)0, bytes[idx + 6], bytes[idx + 5], bytes[idx + 4]);
            idx += 6;
        }
        timeVal *= BigInteger.TEN.pow(9 - this.meta.getTypeInfo().getScale()).intValue();
        final LocalDateTime ldt = MSSQLDateTime2N.DATETIME1.plusDays(numDays);
        this.value = ldt.plusNanos(timeVal);
        return idx - offset;
    }
    
    @Override
    public void readVariantBytes(final RawPacketReader reader, final int valueLen) {
        if (valueLen == 0) {
            this.value = MSSQLDateTime2N.NULL_VALUE;
            return;
        }
        long timeVal;
        long numDays;
        if (valueLen == this.getLongTimeSize()) {
            final byte[] bytes = reader.readBytes(8);
            timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
            numDays = Ints.fromBytes((byte)0, bytes[7], bytes[6], bytes[5]);
        }
        else if (valueLen == this.getLongTimeSize() - 1) {
            final byte[] bytes = reader.readBytes(7);
            timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, bytes[3], bytes[2], bytes[1], bytes[0]);
            numDays = Ints.fromBytes((byte)0, bytes[6], bytes[5], bytes[4]);
        }
        else {
            if (valueLen != this.getLongTimeSize() - 2) {
                throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateTime2N", valueLen, this.meta.getColumnName() });
            }
            final byte[] bytes = reader.readBytes(6);
            timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, bytes[3], bytes[2], bytes[1], bytes[0]);
            numDays = Ints.fromBytes((byte)0, bytes[6], bytes[5], bytes[4]);
        }
        timeVal *= BigInteger.TEN.pow(9 - this.meta.getTypeInfo().getScale()).intValue();
        final LocalDateTime ldt = MSSQLDateTime2N.DATETIME1.plusDays(numDays);
        this.value = ldt.plusNanos(timeVal);
    }
    
    @Override
    public int getSerializedSize() {
        final int scale = this.meta.getTypeInfo().getScale();
        if (scale <= 2) {
            return 7;
        }
        if (scale <= 4) {
            return 8;
        }
        if (scale <= 7) {
            return 9;
        }
        throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateTime2N", scale, this.meta.getColumnName() });
    }
    
    @Override
    public int getVariantSize() {
        return 8;
    }
    
    protected int getLongTimeSize() {
        return 8;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLDateTime2N.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        final int scale = this.meta.getTypeInfo().getScale();
        final LocalDateTime ldt = (LocalDateTime)this.value;
        long numTime = ldt.getHour() * 3600L * 1000000000L + ldt.getMinute() * 60L * 1000000000L + ldt.getSecond() * 1000000000L + ldt.getNano();
        final int divider = BigInteger.TEN.pow(9 - this.meta.getTypeInfo().getScale()).intValue();
        numTime /= divider;
        if (scale <= 2) {
            writer.writeByte((byte)(this.getLongTimeSize() - 2));
            writer.writeByte((byte)(numTime & 0xFFL));
            writer.writeByte((byte)(numTime >> 8 & 0xFFL));
            writer.writeByte((byte)(numTime >> 16 & 0xFFL));
        }
        else if (scale <= 4) {
            writer.writeByte((byte)(this.getLongTimeSize() - 1));
            writer.writeByte((byte)(numTime & 0xFFL));
            writer.writeByte((byte)(numTime >> 8 & 0xFFL));
            writer.writeByte((byte)(numTime >> 16 & 0xFFL));
            writer.writeByte((byte)(numTime >> 24 & 0xFFL));
        }
        else {
            if (scale > 7) {
                throw new ServerException("db.mssql.logic.ScaleNotSupported", new Object[] { "DateTime2N", scale, this.meta.getColumnName() });
            }
            writer.writeByte((byte)this.getLongTimeSize());
            writer.writeByte((byte)(numTime & 0xFFL));
            writer.writeByte((byte)(numTime >> 8 & 0xFFL));
            writer.writeByte((byte)(numTime >> 16 & 0xFFL));
            writer.writeByte((byte)(numTime >> 24 & 0xFFL));
            writer.writeByte((byte)(numTime >> 32 & 0xFFL));
        }
        final long numDays = MSSQLDateTime2N.DATETIME1.until(ldt, ChronoUnit.DAYS);
        writer.writeByte((byte)(numDays & 0xFFL));
        writer.writeByte((byte)(numDays >> 8 & 0xFFL));
        writer.writeByte((byte)(numDays >> 16 & 0xFFL));
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        writer.writeByte((byte)this.meta.getTypeInfo().getScale());
        final LocalDateTime ldt = (LocalDateTime)this.value;
        long numTime = ldt.getHour() * 3600L * 1000000000L + ldt.getMinute() * 60L * 1000000000L + ldt.getSecond() * 1000000000L + ldt.getNano();
        final int divider = BigInteger.TEN.pow(9 - this.meta.getTypeInfo().getScale()).intValue();
        numTime /= divider;
        writer.writeByte((byte)(numTime & 0xFFL));
        writer.writeByte((byte)(numTime >> 8 & 0xFFL));
        writer.writeByte((byte)(numTime >> 16 & 0xFFL));
        writer.writeByte((byte)(numTime >> 24 & 0xFFL));
        writer.writeByte((byte)(numTime >> 32 & 0xFFL));
        final long numDays = MSSQLDateTime2N.DATETIME1.until(ldt, ChronoUnit.DAYS);
        writer.writeByte((byte)(numDays & 0xFFL));
        writer.writeByte((byte)(numDays >> 8 & 0xFFL));
        writer.writeByte((byte)(numDays >> 16 & 0xFFL));
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLDateTime2N.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string date/time", val });
        }
        final String str = val.asString();
        try {
            this.value = LocalDateTime.parse(str);
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "DateTime2N", "2021-08-19T23:59:59.012", str });
        }
    }
}
