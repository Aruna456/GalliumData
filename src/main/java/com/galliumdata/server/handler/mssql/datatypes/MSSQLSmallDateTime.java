// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLSmallDateTime extends MSSQLDataType
{
    public MSSQLSmallDateTime(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int numDays = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
        idx += 2;
        final int numMinutes = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
        idx += 2;
        this.value = MSSQLSmallDateTime.DATETIME1900.plusDays(numDays).plusMinutes(numMinutes);
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int numDays = reader.readTwoByteIntLow() & 0xFFFF;
        final int numMinutes = reader.readTwoByteIntLow() & 0xFFFF;
        this.value = MSSQLSmallDateTime.DATETIME1900.plusDays(numDays).plusMinutes(numMinutes);
    }
    
    @Override
    public int getSerializedSize() {
        return 4;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLSmallDateTime.NULL_VALUE) {
            writer.writeFourByteIntegerLow(0);
            return;
        }
        final LocalDateTime ldt = (LocalDateTime)this.value;
        final long numDays = MSSQLSmallDateTime.DATETIME1900.until(ldt, ChronoUnit.DAYS);
        writer.writeTwoByteIntegerLow((short)numDays);
        final long numMinutes = ldt.getHour() * 60 + ldt.getMinute();
        writer.writeTwoByteIntegerLow((short)numMinutes);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLSmallDateTime.NULL_VALUE;
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
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "SmallDateTime", "2021-08-19T23:59:59.012", str });
        }
    }
}
