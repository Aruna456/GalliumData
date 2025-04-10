// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.UUID;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLUUID extends MSSQLDataType
{
    public MSSQLUUID(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final byte len = bytes[idx];
        ++idx;
        if (len != 16) {
            throw new ServerException("db.mssql.protocol.BadDataTypeLength", new Object[] { "UUID", len, this.meta.getColumnName() });
        }
        idx += this.readVariantBytes(bytes, idx, 16);
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final byte len = reader.readByte();
        if (len == 0) {
            this.value = MSSQLUUID.NULL_VALUE;
            return;
        }
        if (len != 16) {
            throw new ServerException("db.mssql.protocol.BadDataTypeLength", new Object[] { "UUID", len, this.meta.getColumnName() });
        }
        this.readVariantBytes(reader, len);
    }
    
    @Override
    public int readVariantBytes(final byte[] bytes, final int offset, final int valueLen) {
        int idx = offset;
        long bits1 = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        bits1 &= 0xFFFFFFFFL;
        idx += 4;
        long bits2 = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        bits2 &= 0xFFFFL;
        idx += 2;
        long bits3 = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
        bits3 &= 0xFFFFL;
        idx += 2;
        long bits4 = DataTypeReader.readFourByteInteger(bytes, idx);
        bits4 &= 0xFFFFFFFFL;
        idx += 4;
        long bits5 = DataTypeReader.readFourByteInteger(bytes, idx);
        bits5 &= 0xFFFFFFFFL;
        idx += 4;
        final long highBits = bits1 << 32 | bits2 << 16 | bits3;
        final long lowBits = (bits4 << 32) + bits5;
        this.value = new UUID(highBits, lowBits);
        return idx - offset;
    }
    
    @Override
    public void readVariantBytes(final RawPacketReader reader, final int valueLen) {
        long bits1 = reader.readFourByteIntLow();
        bits1 &= 0xFFFFFFFFL;
        long bits2 = reader.readTwoByteIntLow();
        bits2 &= 0xFFFFL;
        long bits3 = reader.readTwoByteIntLow();
        bits3 &= 0xFFFFL;
        long bits4 = reader.readFourByteInt();
        bits4 &= 0xFFFFFFFFL;
        long bits5 = reader.readFourByteInt();
        bits5 &= 0xFFFFFFFFL;
        final long highBits = bits1 << 32 | bits2 << 16 | bits3;
        final long lowBits = (bits4 << 32) + bits5;
        this.value = new UUID(highBits, lowBits);
    }
    
    @Override
    public int getSerializedSize() {
        return 17;
    }
    
    @Override
    public int getVariantSize() {
        return 16;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        final byte[] bytes = new byte[17];
        if (this.value == null || this.value == MSSQLUUID.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        final UUID uuid = (UUID)this.value;
        bytes[0] = 16;
        final long highBits = uuid.getMostSignificantBits();
        final long bits1 = highBits >> 32 & 0xFFFFFFFFL;
        DataTypeWriter.encodeFourByteIntegerLow(bytes, 1, (int)bits1);
        final long bits2 = highBits >> 16 & 0xFFFFL;
        DataTypeWriter.encodeTwoByteIntegerLow(bytes, 5, (short)bits2);
        final long bits3 = highBits & 0xFFFFL;
        DataTypeWriter.encodeTwoByteIntegerLow(bytes, 7, (short)bits3);
        final long lowBits = uuid.getLeastSignificantBits();
        final long bits4 = lowBits >> 32 & 0xFFFFFFFFL;
        DataTypeWriter.encodeFourByteInteger(bytes, 9, (int)bits4);
        final long bits5 = lowBits & 0xFFFFFFFFL;
        DataTypeWriter.encodeFourByteInteger(bytes, 13, (int)bits5);
        writer.writeBytes(bytes, 0, 17);
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        if (this.value == MSSQLUUID.NULL_VALUE) {
            writer.writeEightByteIntegerLow(0L);
            writer.writeEightByteIntegerLow(0L);
            return;
        }
        final byte[] bytes = new byte[16];
        final UUID uuid = (UUID)this.value;
        final long highBits = uuid.getMostSignificantBits();
        final long bits1 = highBits >> 32 & 0xFFFFFFFFL;
        DataTypeWriter.encodeFourByteIntegerLow(bytes, 0, (int)bits1);
        final long bits2 = highBits >> 16 & 0xFFFFL;
        DataTypeWriter.encodeTwoByteIntegerLow(bytes, 4, (short)bits2);
        final long bits3 = highBits & 0xFFFFL;
        DataTypeWriter.encodeTwoByteIntegerLow(bytes, 6, (short)bits3);
        final long lowBits = uuid.getLeastSignificantBits();
        final long bits4 = lowBits >> 32 & 0xFFFFFFFFL;
        DataTypeWriter.encodeFourByteInteger(bytes, 8, (int)bits4);
        final long bits5 = lowBits & 0xFFFFFFFFL;
        DataTypeWriter.encodeFourByteInteger(bytes, 12, (int)bits5);
        writer.writeBytes(bytes, 0, 16);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLUUID.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string date/time", val });
        }
        final String str = val.asString();
        try {
            this.value = UUID.fromString(str);
        }
        catch (final Exception ex) {
            throw new ServerException("db.mssql.logic.ValueHasWrongFormat", new Object[] { "UUID", "12345678-FEDC-ABCD-8765-BA9876543210", str });
        }
    }
}
