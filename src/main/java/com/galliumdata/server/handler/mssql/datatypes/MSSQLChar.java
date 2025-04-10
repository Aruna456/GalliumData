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

public class MSSQLChar extends MSSQLDataType
{
    public MSSQLChar(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int strLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (strLen == -1) {
            this.value = MSSQLChar.NULL_VALUE;
        }
        else {
            this.value = new String(bytes, idx, strLen, StandardCharsets.UTF_8);
            idx += strLen;
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final short strLen = reader.readTwoByteIntLow();
        if (strLen == -1) {
            this.value = MSSQLChar.NULL_VALUE;
            return;
        }
        this.value = reader.readStringWithEncoding(strLen, this.meta.getTypeInfo().getCollationLCID());
    }
    
    @Override
    public int getSerializedSize() {
        if (this.value == MSSQLChar.NULL_VALUE) {
            return 2;
        }
        return 2 + ((String)this.value).length();
    }
    
    @Override
    public int getVariantSize() {
        if (this.value == MSSQLChar.NULL_VALUE) {
            return 0;
        }
        return ((String)this.value).length();
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLChar.NULL_VALUE) {
            writer.writeTwoByteIntegerLow(-1);
            return;
        }
        writer.writeTwoByteIntegerLow((short)((String)this.value).length());
        writer.writeStringWithEncoding((String)this.value, this.meta.getTypeInfo().getCollationLCID());
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        this.meta.getTypeInfo().writeCollation(writer);
        final String s = (String)this.value;
        writer.writeTwoByteIntegerLow((short)s.length());
        writer.writeStringWithEncoding(s, this.meta.getTypeInfo().getCollationLCID());
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        if (val == null || val.isNull()) {
            this.value = MSSQLChar.NULL_VALUE;
            this.changed = true;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string", val });
        }
        final String s = val.asString();
        final int numBytes = s.getBytes(StandardCharsets.UTF_8).length;
        final int varLen = this.getMeta().getTypeInfo().getVarLen();
        if (numBytes > varLen) {
            if (!this.resizable) {
                throw new ServerException("db.mssql.logic.ValueDoesNotFit", new Object[] { s, "char(" + varLen });
            }
            this.getMeta().getTypeInfo().setVarLen(numBytes);
        }
        this.value = s;
        this.changed = true;
    }
}
