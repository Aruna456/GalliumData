// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLXML extends MSSQLDataType
{
    private long size;
    private boolean binaryEncoded;
    
    public MSSQLXML(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        throw new RuntimeException("Unexpected error: MSSQLXML.readFromBytes not implemented");
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        this.size = reader.readEightByteIntLow();
        if (this.size == -1L) {
            this.value = MSSQLXML.NULL_VALUE;
            return;
        }
        int xmlLen = reader.readFourByteIntLow();
        if (xmlLen == 0) {
            this.value = "";
            return;
        }
        byte[] xmlBytes = reader.readBytes(xmlLen);
        if (xmlLen >= 5 && xmlBytes[0] == -33 && xmlBytes[1] == -1) {
            this.binaryEncoded = true;
            this.value = xmlBytes;
            xmlLen = reader.readFourByteIntLow();
            while (xmlLen > 0) {
                xmlBytes = reader.readBytes(xmlLen);
                final byte[] oldValue = (byte[])this.value;
                final int oldLen = oldValue.length;
                final byte[] newValue = new byte[oldLen + xmlLen];
                System.arraycopy(oldValue, 0, newValue, 0, oldLen);
                System.arraycopy(xmlBytes, 0, newValue, oldLen, xmlLen);
                this.value = newValue;
                xmlLen = reader.readFourByteIntLow();
                if (xmlLen == 0) {
                    break;
                }
            }
            return;
        }
        this.value = new String(xmlBytes, 0, xmlLen, StandardCharsets.UTF_16LE);
        if (this.size == -2L) {
            for (xmlLen = reader.readFourByteIntLow(); xmlLen > 0; xmlLen = reader.readFourByteIntLow()) {
                this.value = String.valueOf(this.value) + reader.readString(xmlLen);
            }
        }
        else {
            for (long numRead = xmlLen; numRead < this.size; numRead += xmlLen) {
                xmlLen = reader.readFourByteIntLow();
                this.value = String.valueOf(this.value) + reader.readString(xmlLen);
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        throw new RuntimeException("Unexpected error: MSSQLXML.getSerializedSize not implemented");
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLXML.NULL_VALUE) {
            writer.writeEightByteIntegerLow(-1L);
            return;
        }
        byte[] xmlBytes;
        if (this.binaryEncoded) {
            xmlBytes = (byte[])this.value;
        }
        else {
            xmlBytes = ((String)this.value).getBytes(StandardCharsets.UTF_16LE);
        }
        if (this.size == -2L) {
            writer.writeEightByteIntegerLow(-2L);
            int numToWrite;
            for (int numWritten = 0; numWritten < xmlBytes.length; numWritten += writer.writeBytesUpToSplit(xmlBytes, numWritten, numToWrite)) {
                numToWrite = xmlBytes.length - numWritten;
                int roomInPacket = writer.getPacket().getRemainingBytesToWrite() - 4;
                if (roomInPacket < 2) {
                    writer.addPacket();
                    roomInPacket = writer.getPacket().getRemainingBytesToWrite();
                }
                if (roomInPacket < numToWrite) {
                    numToWrite = roomInPacket;
                }
                writer.writeFourByteIntegerLowNoBreak(numToWrite);
            }
            writer.writeFourByteIntegerLowNoBreak(0);
            return;
        }
        writer.writeEightByteIntegerLow(xmlBytes.length);
        int numToWrite;
        for (int numWritten = 0; numWritten < xmlBytes.length; numWritten += writer.writeBytesUpToSplit(xmlBytes, numWritten, numToWrite)) {
            numToWrite = xmlBytes.length - numWritten;
            int roomInPacket = writer.getPacket().getRemainingBytesToWrite() - 4;
            if (roomInPacket < 2) {
                writer.addPacket();
                roomInPacket = writer.getPacket().getRemainingBytesToWrite();
            }
            if (roomInPacket < numToWrite) {
                numToWrite = roomInPacket;
            }
            writer.writeFourByteIntegerLowNoBreak(numToWrite);
        }
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLXML.NULL_VALUE;
            return;
        }
        if (!val.isString()) {
            throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "string", val });
        }
        this.value = val.asString();
    }
}
