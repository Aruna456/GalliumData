// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLBigInt extends MSSQLDataType
{
    public MSSQLBigInt(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        this.value = DataTypeReader.readEightByteIntegerLow(bytes, offset);
        return 8;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        this.value = reader.readEightByteIntLow();
    }
    
    @Override
    public int getSerializedSize() {
        return 8;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLBigInt.NULL_VALUE) {
            writer.writeEightByteIntegerLow(0L);
            this.changed = true;
            return;
        }
        writer.writeEightByteIntegerLow(((Number)this.value).longValue());
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        if (val == null || val.isNull()) {
            this.value = MSSQLBigInt.NULL_VALUE;
            return;
        }
        this.value = val.asLong();
        this.changed = true;
    }
}
