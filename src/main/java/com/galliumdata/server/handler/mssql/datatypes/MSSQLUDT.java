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

public class MSSQLUDT extends MSSQLDataType
{
    private long totalSize;
    private byte[] extraBytes;
    
    public MSSQLUDT(final ColumnMetadata meta) {
        super(meta);
        this.extraBytes = new byte[4];
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        this.totalSize = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        idx += 8;
        int chunkSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (chunkSize < this.totalSize) {
            final byte[] fullBytes = new byte[(int)this.totalSize];
            for (int numDechunked = 0; numDechunked < this.totalSize; numDechunked += chunkSize, chunkSize = DataTypeReader.readFourByteIntegerLow(bytes, idx), idx += 4) {
                System.arraycopy(bytes, idx, fullBytes, numDechunked, chunkSize);
                idx += chunkSize;
            }
            this.value = fullBytes;
            if (chunkSize != 0) {
                throw new ServerException("db.mssql.protocol.BadTerminator", new Object[] { "UDT", this.meta.getColumnName() });
            }
            return idx - offset;
        }
        else {
            this.value = new byte[chunkSize];
            System.arraycopy(bytes, idx, this.value, 0, chunkSize);
            idx += chunkSize;
            final int terminator = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
            if (terminator != 0) {
                throw new ServerException("db.mssql.protocol.BadTerminator", new Object[] { "UDT", this.meta.getColumnName() });
            }
            return idx - offset;
        }
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final RawPacketReader.ByteArray varBytes = reader.readVarBytes();
        if (varBytes != null) {
            this.totalSize = varBytes.size;
            this.value = varBytes.bytes;
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = 8;
        size += 4;
        final byte[] bytes = (byte[])this.value;
        size += bytes.length;
        size += 4;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLUDT.NULL_VALUE) {
            writer.writeEightByteIntegerLow(0L);
            writer.writeFourByteIntegerLowNoBreak(0);
            return;
        }
        final byte[] valBytes = (byte[])this.value;
        final int bytesLen = valBytes.length;
        writer.writeEightByteIntegerLow(bytesLen);
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
            chunkSize = writer.writeBytesUpToSplit(valBytes, numWritten, bytesLen - numWritten);
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
    public String toString() {
        return "UDT " + this.meta.getTypeInfo().getUdtTypeName() + ":" + String.valueOf(this.value);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLUDT.NULL_VALUE;
            return;
        }
        throw new ServerException("db.mssql.logic.CannotSetUDT", new Object[0]);
    }
}
