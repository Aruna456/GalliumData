// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.nio.charset.StandardCharsets;
import com.galliumdata.server.ServerException;

public class DataStreamHeaderQueryNotifications extends DataStreamHeader
{
    private String notifyId;
    private String ssbDeployment;
    private Integer notifyTimeout;
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int hdrSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        final int hdrType = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (hdrType != 1) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", new Object[] { "QueryNotifications type != 1" });
        }
        int len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.notifyId = new String(bytes, idx, len, StandardCharsets.UTF_16LE);
        idx += len;
        len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.ssbDeployment = new String(bytes, idx, len, StandardCharsets.UTF_16LE);
        idx += len;
        if (hdrSize == idx - offset + 4) {
            this.notifyTimeout = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
        }
        if (idx - offset != hdrSize) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", new Object[] { "wrong header size for QueryNotifications: " + (idx - offset) + ", expected " + hdrSize });
        }
        return hdrSize;
    }
    
    @Override
    public int getSerializedSize() {
        int size = 0;
        size += 4;
        size += 2;
        size += 2;
        size += this.notifyId.length() * 2;
        size += 2;
        size += this.ssbDeployment.length() * 2;
        if (this.notifyTimeout != null) {
            size += 4;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        final int hdrSize = this.getSerializedSize();
        writer.writeFourByteIntegerLow(hdrSize - 4);
        writer.writeTwoByteIntegerLow(1);
        writer.writeByte((byte)this.notifyId.length());
        byte[] strBytes = this.notifyId.getBytes(StandardCharsets.UTF_16LE);
        writer.writeBytes(strBytes, 0, strBytes.length);
        writer.writeByte((byte)this.ssbDeployment.length());
        strBytes = this.ssbDeployment.getBytes(StandardCharsets.UTF_16LE);
        writer.writeBytes(strBytes, 0, strBytes.length);
        if (this.notifyTimeout != null) {
            writer.writeFourByteIntegerLow(this.notifyTimeout);
        }
    }
    
    @Override
    public String getHeaderType() {
        return "TransactionDescriptor";
    }
    
    public String getNotifyId() {
        return this.notifyId;
    }
    
    public void setNotifyId(final String notifyId) {
        this.notifyId = notifyId;
    }
    
    public String getSsbDeployment() {
        return this.ssbDeployment;
    }
    
    public void setSsbDeployment(final String ssbDeployment) {
        this.ssbDeployment = ssbDeployment;
    }
    
    public int getNotifyTimeout() {
        return this.notifyTimeout;
    }
    
    public void setNotifyTimeout(final int notifyTimeout) {
        this.notifyTimeout = notifyTimeout;
    }
}
