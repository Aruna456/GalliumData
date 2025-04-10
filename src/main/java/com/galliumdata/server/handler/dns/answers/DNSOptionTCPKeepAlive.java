// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import com.galliumdata.server.handler.dns.DNSPacket;

public class DNSOptionTCPKeepAlive extends DNSOption
{
    private short timeout;
    
    public DNSOptionTCPKeepAlive(final DNSAnswerOPT answer) {
        super(answer);
        this.optionCode = 11;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        int idx = offset;
        idx += super.read(bytes, offset);
        this.timeout = DNSPacket.readShort(bytes, idx);
        idx += 2;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        DNSPacket.writeShort(this.timeout, buffer, idx);
        idx += 2;
        return idx - offset;
    }
    
    @Override
    public String getCodeName() {
        return "edns-tcp-keepalive";
    }
    
    public short getTimeout() {
        return this.timeout;
    }
    
    public void setTimeout(final short timeout) {
        this.timeout = timeout;
    }
    
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash += this.timeout * 257;
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSOptionTCPKeepAlive)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final DNSOptionTCPKeepAlive opt = (DNSOptionTCPKeepAlive)obj;
        return super.equals(obj) && this.timeout == opt.timeout;
    }
    
    @Override
    public String toString() {
        return "OPT type edns-tcp-keepalive: " + this.timeout;
    }
}
