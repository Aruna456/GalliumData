// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.math.BigInteger;
import com.galliumdata.server.ServerException;
import java.time.LocalTime;
import com.google.common.primitives.Longs;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLTimeN extends MSSQLDataType
{
    public MSSQLTimeN(final ColumnMetadata meta) {
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
        if (valueLen == 0) {
            this.value = MSSQLTimeN.NULL_VALUE;
            return 0;
        }
        int idx = offset;
        if (valueLen == 3) {
            long timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, (byte)0, bytes[idx + 2], bytes[idx + 1], bytes[idx + 0]);
            idx += 3;
            timeVal *= 1000000000L;
            final LocalTime lt = LocalTime.ofNanoOfDay(timeVal);
            this.value = lt;
        }
        else if (valueLen == 4) {
            long timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, bytes[idx + 4], bytes[idx + 3], bytes[idx + 2], bytes[idx + 1], bytes[idx + 0]);
            idx += 5;
            timeVal *= 100000000L;
            final LocalTime lt = LocalTime.ofNanoOfDay(timeVal);
            this.value = lt;
        }
        else {
            if (valueLen != 5) {
                throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 5, valueLen });
            }
            long timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, bytes[idx + 4], bytes[idx + 3], bytes[idx + 2], bytes[idx + 1], bytes[idx + 0]);
            idx += 5;
            this.meta.getTypeInfo().setScale(7);
            timeVal *= BigInteger.TEN.pow(9 - this.meta.getTypeInfo().getScale()).intValue();
            final LocalTime lt = LocalTime.ofNanoOfDay(timeVal);
            this.value = lt;
        }
        return idx - offset;
    }
    
    @Override
    public void readVariantBytes(final RawPacketReader reader, final int valueLen) {
        if (valueLen == 0) {
            this.value = MSSQLTimeN.NULL_VALUE;
            return;
        }
        if (valueLen != 5) {
            throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 5, valueLen });
        }
        final byte[] bytes = reader.readBytes(5);
        long timeVal = Longs.fromBytes((byte)0, (byte)0, (byte)0, bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
        this.meta.getTypeInfo().setScale(7);
        timeVal *= BigInteger.TEN.pow(9 - this.meta.getTypeInfo().getScale()).intValue();
        final LocalTime lt = LocalTime.ofNanoOfDay(timeVal);
        this.value = lt;
    }
    
    @Override
    public int getSerializedSize() {
        final int scale = this.meta.getTypeInfo().getScale();
        if (scale <= 2) {
            return 4;
        }
        if (scale <= 4) {
            return 5;
        }
        if (scale <= 7) {
            return 6;
        }
        throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "MSSQLTimeN", scale, this.meta.getColumnName() });
    }
    
    @Override
    public int getVariantSize() {
        return 5;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLTimeN.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        final LocalTime lt = (LocalTime)this.value;
        long timeNum = lt.toNanoOfDay();
        timeNum /= BigInteger.TEN.pow(9 - this.meta.getTypeInfo().getScale()).intValue();
        final int scale = this.meta.getTypeInfo().getScale();
        if (scale <= 2) {
            writer.writeByte((byte)3);
            writer.writeByte((byte)(timeNum & 0xFFL));
            writer.writeByte((byte)(timeNum >> 8 & 0xFFL));
            writer.writeByte((byte)(timeNum >> 16 & 0xFFL));
        }
        else if (scale <= 4) {
            writer.writeByte((byte)4);
            writer.writeByte((byte)(timeNum & 0xFFL));
            writer.writeByte((byte)(timeNum >> 8 & 0xFFL));
            writer.writeByte((byte)(timeNum >> 16 & 0xFFL));
            writer.writeByte((byte)(timeNum >> 24 & 0xFFL));
        }
        else {
            if (scale > 7) {
                throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "MSSQLTimeN", scale, this.meta.getColumnName() });
            }
            writer.writeByte((byte)5);
            writer.writeByte((byte)(timeNum & 0xFFL));
            writer.writeByte((byte)(timeNum >> 8 & 0xFFL));
            writer.writeByte((byte)(timeNum >> 16 & 0xFFL));
            writer.writeByte((byte)(timeNum >> 24 & 0xFFL));
            writer.writeByte((byte)(timeNum >> 32 & 0xFFL));
        }
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        final LocalTime lt = (LocalTime)this.value;
        long timeNum = lt.getHour() * 3600L * 1000000000L + lt.getMinute() * 60L * 1000000000L + lt.getSecond() * 1000000000L + lt.getNano();
        timeNum /= BigInteger.TEN.pow(9 - this.meta.getTypeInfo().getScale()).intValue();
        writer.writeByte((byte)this.meta.getTypeInfo().getScale());
        writer.writeByte((byte)(timeNum & 0xFFL));
        writer.writeByte((byte)(timeNum >> 8 & 0xFFL));
        writer.writeByte((byte)(timeNum >> 16 & 0xFFL));
        writer.writeByte((byte)(timeNum >> 24 & 0xFFL));
        writer.writeByte((byte)(timeNum >> 32 & 0xFFL));
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLTimeN.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string date/time", val });
        }
        final String str = val.asString();
        try {
            this.value = LocalTime.parse(str);
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "TimeN", "23:59:59.012", str });
        }
    }
}
