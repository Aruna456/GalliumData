// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.time.LocalDateTime;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLDateTime extends MSSQLDataType
{
    public MSSQLDateTime(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int numDays = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        final int numSecSegs = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        final LocalDateTime ldt = MSSQLDateTime.DATETIME1900.plusDays(numDays);
        this.value = ldt.plusNanos(numSecSegs * 3333333L / 1000000L * 1000000L);
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int numDays = reader.readFourByteIntLow();
        final int numSecSegs = reader.readFourByteIntLow();
        final LocalDateTime ldt = MSSQLDateTime.DATETIME1900.plusDays(numDays);
        this.value = ldt.plusNanos(numSecSegs * 3333333L / 1000000L * 1000000L);
    }
    
    @Override
    public int getSerializedSize() {
        return 8;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLDateTime.NULL_VALUE) {
            writer.writeFourByteIntegerLow(0);
            writer.writeFourByteIntegerLow(0);
            return;
        }
        final LocalDateTime ldt = (LocalDateTime)this.value;
        final LocalDateTime dayStart = ldt.minusHours(ldt.getHour()).minusMinutes(ldt.getMinute()).minusSeconds(ldt.getSecond()).minusNanos(ldt.getNano());
        final long numDays = MSSQLDateTime.DATETIME1900.until(ldt, ChronoUnit.DAYS);
        long numSecFragments = dayStart.until(ldt, ChronoUnit.NANOS) / 3333333L;
        if (dayStart.until(ldt, ChronoUnit.NANOS) % 3333333L > 0L) {
            ++numSecFragments;
        }
        writer.writeFourByteIntegerLow((int)numDays);
        writer.writeFourByteIntegerLow((int)numSecFragments);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        if (val == null || val.isNull()) {
            this.value = MSSQLDateTime.NULL_VALUE;
            this.changed = true;
            return;
        }
        if (!val.isString() && !val.isDate()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "date or string", val });
        }
        if (val.isDate()) {
            this.value = val.asDate().atStartOfDay();
        }
        else {
            final String str = val.asString();
            try {
                this.value = LocalDateTime.parse(str);
            }
            catch (final Exception ex) {
                throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "DateTime", "2021-08-19T23:59:59.012", str });
            }
        }
        this.changed = true;
    }
}
