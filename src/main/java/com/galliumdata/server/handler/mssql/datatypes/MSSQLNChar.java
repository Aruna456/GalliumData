// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLNChar extends MSSQLDataType
{
    public MSSQLNChar(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int strLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (strLen == -1) {
            this.value = MSSQLNChar.NULL_VALUE;
        }
        else {
            this.value = new String(bytes, idx, strLen, StandardCharsets.UTF_16LE);
            idx += strLen;
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int strLen = reader.readTwoByteIntLow();
        if (strLen == -1) {
            this.value = MSSQLNChar.NULL_VALUE;
            return;
        }
        this.value = reader.readString(strLen);
    }
    
    @Override
    public int getSerializedSize() {
        if (this.value == MSSQLNChar.NULL_VALUE) {
            return 2;
        }
        return 2 + ((String)this.value).length() * 2;
    }
    
    @Override
    public int getVariantSize() {
        if (this.value == MSSQLNChar.NULL_VALUE) {
            return 0;
        }
        return ((String)this.value).length() * 2;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLNChar.NULL_VALUE) {
            writer.writeTwoByteIntegerLow(-1);
            return;
        }
        final byte[] strBytes = ((String)this.value).getBytes(StandardCharsets.UTF_16LE);
        writer.writeTwoByteIntegerLow((short)strBytes.length);
        writer.writeBytes(strBytes, 0, strBytes.length);
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        this.meta.getTypeInfo().writeCollation(writer);
        this.write(writer);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLNChar.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string", val });
        }
        final String s = val.asString();
        final int numBytes = s.getBytes(StandardCharsets.UTF_16LE).length;
        final int varLen = this.getMeta().getTypeInfo().getVarLen();
        if (numBytes > varLen) {
            if (!this.resizable) {
                throw new ServerException("db.mssql.logic.ValueDoesNotFit", new Object[] { s, "nchar(" + varLen });
            }
            this.getMeta().getTypeInfo().setVarLen(numBytes);
        }
        this.value = s;
    }
}
