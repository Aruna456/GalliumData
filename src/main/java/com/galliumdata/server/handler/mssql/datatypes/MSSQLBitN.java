// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLBitN extends MSSQLDataType
{
    public MSSQLBitN(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final byte valueLen = bytes[offset];
        if (valueLen == 0) {
            this.value = null;
        }
        else {
            if (valueLen != 1) {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "bit length > 1" });
            }
            this.value = bytes[offset + 1];
        }
        return 1 + valueLen;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final byte valueLen = reader.readByte();
        if (valueLen == 0) {
            this.value = null;
        }
        else {
            if (valueLen != 1) {
                throw new ServerException("db.mssql.protocol.ValueError", new Object[] { this.meta.getColumnName(), "bit length > 1" });
            }
            this.value = reader.readByte();
        }
    }
    
    @Override
    public int getSerializedSize() {
        return 2;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLBitN.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        writer.writeByte((byte)1);
        if (((Number)this.value).byteValue() != 0) {
            writer.writeByte((byte)1);
        }
        else {
            writer.writeByte((byte)0);
        }
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        if (val == null || val.isNull()) {
            this.value = MSSQLBitN.NULL_VALUE;
            this.changed = true;
            return;
        }
        if (!val.isBoolean() && !val.isNumber()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "boolean", val });
        }
        if (val.isNumber()) {
            this.value = (byte)((val.asInt() != 0) ? 1 : 0);
        }
        else {
            this.value = (byte)(val.asBoolean() ? 1 : 0);
        }
        this.changed = true;
    }
}
