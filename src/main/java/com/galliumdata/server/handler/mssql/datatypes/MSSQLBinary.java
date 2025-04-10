// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLBinary extends MSSQLDataType
{
    public MSSQLBinary(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final short dataSize = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (dataSize == -1) {
            this.value = MSSQLBinary.NULL_VALUE;
        }
        else {
            System.arraycopy(bytes, idx, this.value = new byte[dataSize], 0, dataSize);
            idx += dataSize;
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final short dataSize = reader.readTwoByteIntLow();
        if (dataSize == -1) {
            this.value = MSSQLBinary.NULL_VALUE;
            return;
        }
        this.value = reader.readBytes(dataSize);
    }
    
    @Override
    public int getSerializedSize() {
        if (this.value == MSSQLBinary.NULL_VALUE) {
            return 0;
        }
        return 2 + ((byte[])this.value).length;
    }
    
    @Override
    public int getVariantSize() {
        return ((byte[])this.value).length;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == MSSQLBinary.NULL_VALUE) {
            writer.writeTwoByteIntegerLow(-1);
            return;
        }
        writer.writeTwoByteIntegerLow((short)((byte[])this.value).length);
        final byte[] strBytes = (byte[])this.value;
        writer.writeBytes(strBytes, 0, strBytes.length);
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        writer.writeTwoByteIntegerLow((short)this.meta.getTypeInfo().getVarLen());
        final byte[] strBytes = (byte[])this.value;
        writer.writeBytes(strBytes, 0, strBytes.length);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        if (val == null || val.isNull()) {
            this.value = MSSQLBinary.NULL_VALUE;
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
        this.changed = true;
    }
    
    @Override
    public String toString() {
        if (this.value == null || this.value == MSSQLBinary.NULL_VALUE) {
            return "byte[] (null)";
        }
        final byte[] bytes = (byte[])this.value;
        return "byte[" + bytes.length;
    }
}
