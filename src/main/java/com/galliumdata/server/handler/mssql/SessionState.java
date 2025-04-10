// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

public class SessionState
{
    private byte stateId;
    private byte[] stateValue;
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        this.stateId = bytes[idx];
        ++idx;
        int len = bytes[idx];
        ++idx;
        if (len == -1) {
            len = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 2;
        }
        System.arraycopy(bytes, idx, this.stateValue = new byte[len], 0, len);
        return idx - offset;
    }
    
    public byte getStateId() {
        return this.stateId;
    }
    
    public void setStateId(final byte stateId) {
        this.stateId = stateId;
    }
    
    public byte[] getStateValue() {
        return this.stateValue;
    }
    
    public void setStateValue(final byte[] stateValue) {
        this.stateValue = stateValue;
    }
}
