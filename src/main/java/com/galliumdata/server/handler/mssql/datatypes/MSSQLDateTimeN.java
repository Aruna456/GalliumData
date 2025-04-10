// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.time.LocalDateTime;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLDateTimeN extends MSSQLDataType
{
    public MSSQLDateTimeN(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int valueLen = bytes[idx];
        ++idx;
        if (valueLen == 0) {
            this.value = MSSQLDateTimeN.NULL_VALUE;
            return 0;
        }
        if (valueLen == 4) {
            int numDays = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            numDays = Short.toUnsignedInt((short)numDays);
            idx += 2;
            final int numMinutes = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
            final LocalDateTime ldt = MSSQLDateTimeN.DATETIME1900.plusDays(numDays);
            this.value = ldt.plusMinutes(numMinutes);
            return 1 + valueLen;
        }
        if (valueLen != 8) {
            throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateTimeN", valueLen, this.meta.getColumnName() });
        }
        int numDays = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        final int numSecFragments = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        final LocalDateTime ldt = MSSQLDateTimeN.DATETIME1900.plusDays(numDays);
        this.value = ldt.plusNanos(numSecFragments * 3333333L / 1000000L * 1000000L);
        return 1 + valueLen;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int valueLen = reader.readByte();
        if (valueLen == 0) {
            this.value = MSSQLDateTimeN.NULL_VALUE;
            return;
        }
        if (valueLen == 4) {
            int numDays = reader.readTwoByteIntLow();
            numDays = Short.toUnsignedInt((short)numDays);
            final int numMinutes = reader.readTwoByteIntLow();
            final LocalDateTime ldt = MSSQLDateTimeN.DATETIME1900.plusDays(numDays);
            this.value = ldt.plusMinutes(numMinutes);
            return;
        }
        if (valueLen != 8) {
            throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateTimeN", valueLen, this.meta.getColumnName() });
        }
        int numDays = reader.readFourByteIntLow();
        final int numSecFragments = reader.readFourByteIntLow();
        final LocalDateTime ldt = MSSQLDateTimeN.DATETIME1900.plusDays(numDays);
        this.value = ldt.plusNanos(numSecFragments * 3333333L / 1000000L * 1000000L);
    }
    
    @Override
    public int getSerializedSize() {
        return 1 + this.meta.getTypeInfo().getVarLen();
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLDateTimeN.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        final LocalDateTime ldt = (LocalDateTime)this.value;
        final LocalDateTime dayStart = ldt.minusHours(ldt.getHour()).minusMinutes(ldt.getMinute()).minusSeconds(ldt.getSecond()).minusNanos(ldt.getNano());
        if (this.meta.getTypeInfo().getVarLen() == 4) {
            writer.writeByte((byte)4);
            final long numDays = MSSQLDateTimeN.DATETIME1900.until(ldt, ChronoUnit.DAYS);
            final long numMinutes = dayStart.until(ldt, ChronoUnit.MINUTES);
            writer.writeTwoByteIntegerLow((short)numDays);
            writer.writeTwoByteIntegerLow((short)numMinutes);
        }
        else {
            if (this.meta.getTypeInfo().getVarLen() != 8) {
                throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateTimeN", this.meta.getTypeInfo().getVarLen(), this.meta.getColumnName() });
            }
            writer.writeByte((byte)8);
            final long numDays = MSSQLDateTimeN.DATETIME1900.until(ldt, ChronoUnit.DAYS);
            long numSecFragments = dayStart.until(ldt, ChronoUnit.NANOS) / 3333333L;
            if (dayStart.until(ldt, ChronoUnit.NANOS) % 3333333L > 0L) {
                ++numSecFragments;
            }
            writer.writeFourByteIntegerLow((int)numDays);
            writer.writeFourByteIntegerLow((int)numSecFragments);
        }
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLDateTimeN.NULL_VALUE;
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
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "DateTimeN", "2021-08-19T23:59:59.012", str });
        }
    }
}
