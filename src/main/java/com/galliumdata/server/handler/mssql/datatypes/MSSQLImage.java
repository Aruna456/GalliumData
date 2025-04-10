// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLImage extends MSSQLDataType
{
    private byte[] privateTextPtr;
    private byte[] extraBytes1;
    private byte extraByte2;
    
    public MSSQLImage(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final byte dummyTxtPtrSize = bytes[idx];
        if (dummyTxtPtrSize == 0) {
            this.value = MSSQLImage.NULL_VALUE;
            return 1;
        }
        if (dummyTxtPtrSize != 16) {
            throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), "dummy textptr != 16" });
        }
        ++idx;
        final String dummyTxtPtr = new String(bytes, idx, 13, StandardCharsets.US_ASCII);
        if (!"dummy textptr".equals(dummyTxtPtr)) {
            throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), dummyTxtPtr });
        }
        idx += dummyTxtPtrSize;
        final String dummyTS = new String(bytes, idx, 7);
        if (!"dummyTS".equals(dummyTS)) {
            throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), dummyTS });
        }
        idx += 8;
        final int binSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.value = new byte[binSize];
        System.arraycopy(bytes, idx, this.value, 0, binSize);
        idx += binSize;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final byte dummyTxtPtrSize = reader.readByte();
        if (dummyTxtPtrSize == 0) {
            this.value = MSSQLImage.NULL_VALUE;
            return;
        }
        if (dummyTxtPtrSize != 16) {
            throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), "dummy textptr != 16" });
        }
        this.privateTextPtr = reader.readBytes(13);
        this.extraBytes1 = reader.readBytes(3);
        final String dummyTS = reader.readAsciiString(7);
        if (!"dummyTS".equals(dummyTS)) {
            throw new ServerException("db.mssql.protocol.BadTextPtr", new Object[] { this.meta.getColumnName(), dummyTS });
        }
        this.extraByte2 = reader.readByte();
        final int binSize = reader.readFourByteIntLow();
        this.value = reader.readBytes(binSize);
    }
    
    @Override
    public int getSerializedSize() {
        if (this.value == MSSQLImage.NULL_VALUE) {
            return 1;
        }
        int size = 1;
        size += 16;
        size += 8;
        size += 4;
        size += ((byte[])this.value).length;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLImage.NULL_VALUE) {
            writer.writeByte((byte)0);
            return;
        }
        writer.writeByte((byte)16);
        writer.writeBytes(this.privateTextPtr, 0, this.privateTextPtr.length);
        writer.writeBytes(this.extraBytes1, 0, this.extraBytes1.length);
        writer.writeBytes("dummyTS".getBytes(StandardCharsets.US_ASCII), 0, 7);
        writer.writeByte(this.extraByte2);
        final byte[] strBytes = (byte[])this.value;
        writer.writeFourByteIntegerLow(strBytes.length);
        writer.writeBytes(strBytes, 0, strBytes.length);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLImage.NULL_VALUE;
            return;
        }
        if (!val.hasArrayElements()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "byte array", val });
        }
        final byte[] newVal = new byte[(int)val.getArraySize()];
        for (int i = 0; i < val.getArraySize(); ++i) {
            newVal[i] = val.getArrayElement((long)i).asByte();
        }
        this.value = newVal;
    }
}
