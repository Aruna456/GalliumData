// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLVarChar extends MSSQLDataType
{
    private int size;
    
    public MSSQLVarChar(final ColumnMetadata meta) {
        super(meta);
        this.size = -1;
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final VarString varStr = readString(bytes, offset, StandardCharsets.ISO_8859_1, this.meta);
        this.size = (int)varStr.size;
        this.value = varStr.str;
        return varStr.numBytesRead;
    }
    
    protected static VarString readString(final byte[] bytes, final int offset, final Charset charset, final ColumnMetadata meta) {
        final VarString result = new VarString();
        int idx = offset;
        String value = null;
        if (!meta.getTypeInfo().varLenIsUnlimited()) {
            result.size = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
            if (result.size == -1L) {
                result.str = null;
            }
            else {
                value = new String(bytes, idx, (int)result.size, charset);
                idx += (int)result.size;
            }
            result.str = value;
            result.numBytesRead = idx - offset;
            return result;
        }
        result.size = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        idx += 8;
        if (result.size == -1L) {
            result.str = null;
            result.numBytesRead = idx - offset;
            return result;
        }
        int chunkSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (chunkSize < result.size) {
            final byte[] fullBytes = new byte[(int)result.size];
            for (int numDechunked = 0; numDechunked < result.size; numDechunked += chunkSize, chunkSize = DataTypeReader.readFourByteIntegerLow(bytes, idx), idx += 4) {
                System.arraycopy(bytes, idx, fullBytes, numDechunked, chunkSize);
                idx += chunkSize;
            }
            result.str = new String(fullBytes, 0, (int)result.size, charset);
        }
        else {
            result.str = new String(bytes, idx, chunkSize, charset);
            idx += chunkSize;
        }
        if (chunkSize > 0) {
            final int term = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            if (term != 0) {
                throw new ServerException("db.mssql.protocol.BadTVPMetadata", new Object[] { "Bad terminator for TVP string" });
            }
            idx += 4;
        }
        result.numBytesRead = idx - offset;
        return result;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            final RawPacketReader.ByteArray varBytes = reader.readVarBytes();
            if (varBytes == null) {
                this.value = MSSQLVarChar.NULL_VALUE;
            }
            else {
                this.size = varBytes.size;
                final byte[] bytes = varBytes.bytes;
                this.value = new String(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1);
            }
        }
        else {
            final int strLen = reader.readTwoByteIntLow();
            if (strLen == -1) {
                this.value = MSSQLVarChar.NULL_VALUE;
            }
            else {
                this.value = reader.readStringWithEncoding(strLen, this.meta.getTypeInfo().getCollationLCID());
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        if (this.meta.getTypeInfo().varLenIsUnlimited()) {
            int size = 8;
            size += 4;
            if (this.value != null) {
                if (this.value != MSSQLVarChar.NULL_VALUE) {
                    final byte[] strBytes = ((String)this.value).getBytes(StandardCharsets.ISO_8859_1);
                    size += strBytes.length;
                }
            }
            size += 4;
            return size;
        }
        if (MSSQLVarChar.NULL_VALUE == this.value) {
            return 2;
        }
        return 2 + ((String)this.value).length();
    }
    
    @Override
    public int getVariantSize() {
        return ((String)this.value).length();
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writeString(writer, this.meta, this.value, this.size, StandardCharsets.ISO_8859_1);
    }
    
    protected static void writeString(final RawPacketWriter writer, final ColumnMetadata colMeta, final Object value, final int size, final Charset charset) {
        if (value == null || value == MSSQLVarChar.NULL_VALUE) {
            if (colMeta.getTypeInfo().varLenIsUnlimited()) {
                writer.writeEightByteIntegerLow(-1L);
            }
            else {
                writer.writeTwoByteIntegerLow(-1);
            }
            return;
        }
        final byte[] strBytes = ((String)value).getBytes(charset);
        final int bytesLen = strBytes.length;
        if (colMeta.getTypeInfo().varLenIsUnlimited()) {
            if (size >= 0) {
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
                chunkSize = writer.writeBytesUpToSplit(strBytes, numWritten, bytesLen - numWritten);
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
            return;
        }
        if (value == null || value == MSSQLVarChar.NULL_VALUE) {
            writer.writeTwoByteIntegerLow(-1);
        }
        else {
            writer.writeTwoByteIntegerLow((short)strBytes.length);
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
    }
    
    @Override
    public void writeVariant(final RawPacketWriter writer) {
        this.meta.getTypeInfo().writeCollation(writer);
        final byte[] strBytes = ((String)this.value).getBytes(StandardCharsets.ISO_8859_1);
        writer.writeTwoByteIntegerLow((short)this.meta.getTypeInfo().getVarLen());
        writer.writeBytes(strBytes, 0, strBytes.length);
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLVarChar.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string", val });
        }
        final String s = val.asString();
        final int numBytes = s.getBytes(StandardCharsets.ISO_8859_1).length;
        final int varLen = this.getMeta().getTypeInfo().getVarLen();
        if (varLen != -1 && numBytes > varLen) {
            if (!this.resizable) {
                throw new ServerException("db.mssql.logic.ValueDoesNotFit", new Object[] { s, "varchar(" + varLen });
            }
            this.getMeta().getTypeInfo().setVarLen(numBytes);
        }
        this.value = s;
    }
    
    public static class VarString
    {
        public long size;
        public String str;
        public int numBytesRead;
    }
}
