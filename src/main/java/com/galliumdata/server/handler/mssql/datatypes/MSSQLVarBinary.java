// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLVarBinary extends MSSQLDataType
{
    private long size;
    
    public MSSQLVarBinary(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            this.size = DataTypeReader.readEightByteIntegerLow(bytes, idx);
            idx += 8;
            if (this.size == -1L) {
                this.value = null;
                return idx - offset;
            }
            if (this.size == -2L) {
                this.size = -2L;
            }
            int chunkSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
            if (chunkSize < this.size) {
                final byte[] fullBytes = new byte[(int)this.size];
                for (int numDechunked = 0; numDechunked < this.size; numDechunked += chunkSize, chunkSize = DataTypeReader.readFourByteIntegerLow(bytes, idx), idx += 4) {
                    System.arraycopy(bytes, idx, fullBytes, numDechunked, chunkSize);
                    idx += chunkSize;
                }
                this.value = fullBytes;
                if (chunkSize != 0) {
                    throw new ServerException("db.mssql.protocol.BadTerminator", new Object[] { "VarBinary", this.meta.getColumnName() });
                }
                return idx - offset;
            }
            else {
                this.value = new byte[chunkSize];
                System.arraycopy(bytes, idx, this.value, 0, chunkSize);
                idx += chunkSize;
                final int terminator = DataTypeReader.readFourByteIntegerLow(bytes, idx);
                if (terminator != 0) {
                    throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "varbinary terminator != 0" });
                }
                idx += 4;
            }
        }
        else {
            final int dataLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
            if (dataLen == -1) {
                this.value = MSSQLVarBinary.NULL_VALUE;
            }
            else {
                this.value = new byte[dataLen];
                System.arraycopy(bytes, idx, this.value, 0, dataLen);
                idx += dataLen;
            }
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            final RawPacketReader.ByteArray varBytes = reader.readVarBytes();
            if (varBytes == null) {
                this.value = MSSQLVarBinary.NULL_VALUE;
                return;
            }
            this.size = varBytes.size;
            this.value = varBytes.bytes;
        }
        else {
            final int dataLen = reader.readTwoByteIntLow();
            if (dataLen == -1) {
                this.value = MSSQLVarBinary.NULL_VALUE;
            }
            else {
                this.value = reader.readBytes(dataLen);
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            int size = 8;
            size += 4;
            size += ((byte[])this.value).length;
            size += 4;
            return size;
        }
        if (this.value == MSSQLVarBinary.NULL_VALUE) {
            return 2;
        }
        return 2 + ((byte[])this.value).length;
    }
    
    @Override
    public int getVariantSize() {
        if (this.value == MSSQLVarBinary.NULL_VALUE) {
            return 0;
        }
        return ((byte[])this.value).length;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        byte[] data;
        if (this.value == null || this.value == MSSQLVarBinary.NULL_VALUE) {
            data = new byte[0];
        }
        else {
            data = (byte[])this.value;
        }
        final int bytesLen = data.length;
        if (!this.meta.getTypeInfo().varLenIsUnlimited()) {
            if (this.value == null || this.value == MSSQLVarBinary.NULL_VALUE) {
                writer.writeTwoByteIntegerLow(-1);
            }
            else {
                writer.writeTwoByteIntegerLow((short)data.length);
                writer.writeBytes(data, 0, data.length);
            }
            return;
        }
        if (this.value == null || this.value == MSSQLVarBinary.NULL_VALUE) {
            writer.writeEightByteIntegerLow(-1L);
            return;
        }
        if (this.size >= 0L) {
            writer.writeEightByteIntegerLow(bytesLen);
        }
        else {
            writer.writeEightByteIntegerLow(-2L);
        }
        int numInPacket = bytesLen;
        if (numInPacket > 0 && writer.getPacket().getRemainingBytesToWrite() < 6) {
            writer.addPacket();
        }
        if (writer.getPacket().getRemainingBytesToWrite() - 4 < numInPacket) {
            numInPacket = writer.getPacket().getRemainingBytesToWrite() - 4;
        }
        writer.writeFourByteIntegerLowNoBreak(numInPacket);
        int chunkSize;
        for (int numWritten = 0; numWritten < bytesLen; numWritten += chunkSize) {
            chunkSize = writer.writeBytesUpToSplit(data, numWritten, bytesLen - numWritten);
            if (chunkSize < bytesLen - numWritten) {
                int nextWriteSize = bytesLen - numWritten - chunkSize;
                if (nextWriteSize > writer.getPacketSize() - 12) {
                    nextWriteSize = writer.getPacketSize() - 12;
                }
                writer.writeFourByteIntegerLowNoBreak(nextWriteSize);
            }
        }
        if (bytesLen > 0) {
            if (writer.getPacket().getRemainingBytesToWrite() <= 4) {
                writer.addPacket();
            }
            writer.writeFourByteIntegerLowNoBreak(0);
        }
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        writer.writeTwoByteIntegerLow((short)this.meta.getTypeInfo().getVarLen());
        final byte[] data = (byte[])this.value;
        writer.writeBytes(data, 0, data.length);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLVarBinary.NULL_VALUE;
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
