// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLNText extends MSSQLDataType
{
    private byte[] dummyTxt;
    
    public MSSQLNText(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            final byte dummyTxtPtrSize = bytes[idx];
            if (dummyTxtPtrSize != 16) {
                throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), "dummy textptr != 16" });
            }
            ++idx;
            System.arraycopy(bytes, idx, this.dummyTxt = new byte[16], 0, 16);
            idx += 16;
            final String dummyTS = new String(bytes, idx, 7);
            if (!"dummyTS".equals(dummyTS)) {
                throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), dummyTS });
            }
            idx += 8;
        }
        final int textSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (textSize == -1) {
            this.value = MSSQLNText.NULL_VALUE;
        }
        else {
            this.value = new String(bytes, idx, textSize, StandardCharsets.UTF_16LE);
            idx += textSize;
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            final byte dummyTxtPtrSize = reader.readByte();
            if (dummyTxtPtrSize == 0) {
                this.value = MSSQLNText.NULL_VALUE;
                return;
            }
            if (dummyTxtPtrSize != 16) {
                throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), "dummy textptr != 16" });
            }
            this.dummyTxt = reader.readBytes(16);
            final String dummyTS = reader.readAsciiString(7);
            if (!"dummyTS".equals(dummyTS)) {
                throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), dummyTS });
            }
            reader.readByte();
        }
        final int textSize = reader.readFourByteIntLow();
        if (textSize == -1) {
            this.value = MSSQLNText.NULL_VALUE;
        }
        else {
            this.value = reader.readString(textSize);
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = 0;
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            ++size;
            size += 16;
            size += 8;
        }
        size += 4;
        size += ((String)this.value).length() * 2;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            if (this.value == MSSQLNText.NULL_VALUE) {
                writer.writeByte((byte)0);
                return;
            }
            writer.writeByte((byte)16);
            writer.writeBytes(this.dummyTxt, 0, this.dummyTxt.length);
            writer.writeBytes("dummyTS".getBytes(StandardCharsets.US_ASCII), 0, 7);
            writer.writeByte((byte)0);
        }
        if (this.value == MSSQLNText.NULL_VALUE) {
            writer.writeFourByteIntegerLow(-1);
            return;
        }
        final byte[] strBytes = ((String)this.value).getBytes(StandardCharsets.UTF_16LE);
        writer.writeFourByteIntegerLow(strBytes.length);
        writer.writeBytes(strBytes, 0, strBytes.length);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLNText.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string", val });
        }
        this.value = val.asString();
    }
    
    @Override
    public String toString() {
        if (this.value == null || this.value == MSSQLNText.NULL_VALUE) {
            return "ntext (null)";
        }
        String s = (String)this.value;
        if (s.length() > 100) {
            s = s.substring(0, 100) + "...(" + (s.length() - 100) + " more)";
        }
        return "ntext: " + s;
    }
}
