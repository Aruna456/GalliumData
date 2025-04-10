// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.ServerException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLDateTimeOffsetN extends MSSQLDateTime2N
{
    public MSSQLDateTimeOffsetN(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx);
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        super.read(reader);
    }
    
    @Override
    public int readVariantBytes(final byte[] bytes, final int offset, final int varLen) {
        int idx = offset;
        idx += super.readVariantBytes(bytes, idx, varLen);
        final int offsetMinutes = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        LocalDateTime ldt = (LocalDateTime)this.value;
        ldt = ldt.plusNanos(offsetMinutes * 60 * 1000000000L);
        final ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(offsetMinutes / 60, offsetMinutes % 60);
        this.value = OffsetDateTime.of(ldt, zoneOffset);
        return idx - offset;
    }
    
    @Override
    public void readVariantBytes(final RawPacketReader reader, final int valueLen) {
        super.readVariantBytes(reader, valueLen);
        if (this.value == null || this.value == MSSQLDateTimeOffsetN.NULL_VALUE) {
            return;
        }
        final int offsetMinutes = reader.readTwoByteIntLow();
        LocalDateTime ldt = (LocalDateTime)this.value;
        ldt = ldt.plusNanos(offsetMinutes * 60 * 1000000000L);
        final ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(offsetMinutes / 60, offsetMinutes % 60);
        this.value = OffsetDateTime.of(ldt, zoneOffset);
    }
    
    @Override
    public int getSerializedSize() {
        final int scale = this.meta.getTypeInfo().getScale();
        if (scale <= 2) {
            return 9;
        }
        if (scale <= 4) {
            return 10;
        }
        if (scale <= 7) {
            return 11;
        }
        throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateTimeOffsetN", scale, this.meta.getColumnName() });
    }
    
    @Override
    public int getVariantSize() {
        return 10;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLDateTimeOffsetN.NULL_VALUE) {
            super.write(writer);
            return;
        }
        final OffsetDateTime odt = (OffsetDateTime)this.value;
        final int offsetMinutes = odt.getOffset().getTotalSeconds() / 60;
        this.value = odt.toLocalDateTime().minusMinutes(offsetMinutes);
        super.write(writer);
        this.value = odt;
        writer.writeTwoByteIntegerLow((short)offsetMinutes);
    }
    
    @Override
    protected int getLongTimeSize() {
        return 10;
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        final OffsetDateTime odt = (OffsetDateTime)this.value;
        final int offsetMinutes = odt.getOffset().getTotalSeconds() / 60;
        this.value = odt.toLocalDateTime().minusMinutes(offsetMinutes);
        super.writeVariant(writer);
        this.value = odt;
        writer.writeTwoByteIntegerLow((short)offsetMinutes);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLDateTimeOffsetN.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string date/time", val });
        }
        final String str = val.asString();
        try {
            this.value = OffsetDateTime.parse(str);
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "DateTimeOffsetN", "2022-08-19T23:59:59.012-08:00", str });
        }
    }
}
