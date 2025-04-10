// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;

public class StreamHeaderQueryNotifications extends StreamHeader
{
    private String notifyId;
    private String ssbDeployment;
    private int notifyTimeout;
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        int len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.notifyId = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
        idx += len * 2;
        len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.ssbDeployment = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
        idx += len * 2;
        final int sizeSoFar = idx - offset + 5;
        if (this.length == sizeSoFar) {
            return idx - offset;
        }
        if (this.length != sizeSoFar + 4) {
            throw new ServerException("db.mssql.protocol.StreamHeaderError", new Object[] { this.getTypeName(), "notifyTimeout: wrong size: " + (idx - offset + 5) });
        }
        this.notifyTimeout = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        final int size = 0;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
    }
    
    @Override
    public short getType() {
        return 1;
    }
    
    @Override
    public String getTypeName() {
        return "QueryNotification";
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
