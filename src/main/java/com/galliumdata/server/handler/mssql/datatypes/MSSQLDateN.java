// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.google.common.primitives.Ints;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLDateN extends MSSQLDataType
{
    public MSSQLDateN(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int valueLen = bytes[idx];
        ++idx;
        if (valueLen == 0) {
            this.value = MSSQLDateN.NULL_VALUE;
            return 1;
        }
        if (valueLen != 3) {
            throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateN", valueLen, this.meta.getColumnName() });
        }
        final int numDays = Ints.fromBytes((byte)0, bytes[idx + 2], bytes[idx + 1], bytes[idx]);
        this.value = MSSQLDateN.DATE1.plusDays(numDays);
        return 1 + valueLen;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int valueLen = reader.readByte();
        if (valueLen == 0) {
            this.value = MSSQLDateN.NULL_VALUE;
            return;
        }
        if (valueLen != 3) {
            throw new ServerException("db.mssql.logic.VarLenNotSupported", new Object[] { "DateN", valueLen, this.meta.getColumnName() });
        }
        final byte[] bytes = reader.readBytes(3);
        final int numDays = Ints.fromBytes((byte)0, bytes[2], bytes[1], bytes[0]);
        this.value = MSSQLDateN.DATE1.plusDays(numDays);
    }
    
    @Override
    public int readVariantBytes(final byte[] bytes, final int offset, final int valueLen) {
        if (valueLen != 3) {
            throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 3, valueLen });
        }
        final int numDays = Ints.fromBytes((byte)0, bytes[offset + 2], bytes[offset + 1], bytes[offset]);
        this.value = MSSQLDateN.DATE1.plusDays(numDays);
        return 3;
    }
    
    @Override
    public void readVariantBytes(final RawPacketReader reader, final int valueLen) {
        if (valueLen != 3) {
            throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 3, valueLen });
        }
        final byte[] bytes = reader.readBytes(3);
        final int numDays = Ints.fromBytes((byte)0, bytes[2], bytes[1], bytes[0]);
        this.value = MSSQLDateN.DATE1.plusDays(numDays);
    }
    
    @Override
    public int getSerializedSize() {
        if (this.value == null || this.value == MSSQLDateN.NULL_VALUE) {
            return 1;
        }
        return 4;
    }
    
    @Override
    public int getVariantSize() {
        return 3;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLDateN.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        writer.writeByte((byte)3);
        this.writeVariant(writer);
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        final long numDays = MSSQLDateN.DATE1.until((Temporal)this.value, ChronoUnit.DAYS);
        writer.writeByte((byte)(numDays & 0xFFL));
        writer.writeByte((byte)(numDays >> 8 & 0xFFL));
        writer.writeByte((byte)(numDays >> 16 & 0xFFL));
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        if (val == null || val.isNull()) {
            this.value = MSSQLDateN.NULL_VALUE;
            this.changed = true;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string date", val });
        }
        final String str = val.asString();
        try {
            this.value = LocalDate.parse(str);
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "DateN", "2007-12-03", str });
        }
        this.changed = true;
    }
}
