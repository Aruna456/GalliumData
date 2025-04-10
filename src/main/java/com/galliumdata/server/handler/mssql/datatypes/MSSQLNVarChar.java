// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLNVarChar extends MSSQLDataType
{
    private int size;
    
    public MSSQLNVarChar(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final MSSQLVarChar.VarString varStr = MSSQLVarChar.readString(bytes, offset, StandardCharsets.UTF_16LE, this.meta);
        this.size = (int)varStr.size;
        this.value = varStr.str;
        return varStr.numBytesRead;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            final RawPacketReader.ByteArray varBytes = reader.readVarBytes();
            if (varBytes == null) {
                this.value = MSSQLNVarChar.NULL_VALUE;
            }
            else {
                this.size = varBytes.size;
                this.value = new String(varBytes.bytes, 0, varBytes.bytes.length, StandardCharsets.UTF_16LE);
            }
        }
        else {
            final int strLen = reader.readTwoByteIntLow();
            if (strLen == -1) {
                this.value = MSSQLNVarChar.NULL_VALUE;
            }
            else {
                this.value = reader.readString(strLen);
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            int size = 8;
            size += 4;
            final String stringVal = (String)this.value;
            size += stringVal.length() * 2;
            size += 4;
            return size;
        }
        if (this.value == MSSQLNVarChar.NULL_VALUE) {
            return 2;
        }
        return 2 + ((String)this.value).length() * 2;
    }
    
    @Override
    public int getVariantSize() {
        if (this.value == MSSQLNVarChar.NULL_VALUE) {
            return 0;
        }
        return ((String)this.value).length() * 2;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        MSSQLVarChar.writeString(writer, this.meta, this.value, this.size, StandardCharsets.UTF_16LE);
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        this.meta.getTypeInfo().writeCollation(writer);
        if (this.value == MSSQLNVarChar.NULL_VALUE) {
            writer.writeTwoByteIntegerLow(-1);
        }
        else {
            final byte[] strBytes = ((String)this.value).getBytes(StandardCharsets.UTF_16LE);
            writer.writeTwoByteIntegerLow((short)this.meta.getTypeInfo().getVarLen());
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLNVarChar.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string", val });
        }
        final String s = val.asString();
        final int numBytes = s.getBytes(StandardCharsets.UTF_16LE).length;
        final int varLen = this.getMeta().getTypeInfo().getVarLen();
        if (varLen != -1 && numBytes > varLen) {
            if (!this.resizable) {
                throw new ServerException("db.mssql.logic.ValueDoesNotFit", new Object[] { s, "nvarchar(" + varLen });
            }
            this.getMeta().getTypeInfo().setVarLen(numBytes);
        }
        this.value = s;
    }
}
