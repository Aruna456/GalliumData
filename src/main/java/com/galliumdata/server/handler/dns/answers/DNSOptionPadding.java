// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import java.util.Arrays;

public class DNSOptionPadding extends DNSOption
{
    private byte[] padding;
    
    public DNSOptionPadding(final DNSAnswerOPT answer) {
        super(answer);
        this.optionCode = 11;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        int idx = offset;
        idx += super.read(bytes, offset);
        System.arraycopy(bytes, idx, this.padding = new byte[this.optionLength], 0, this.optionLength);
        idx += this.optionLength;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += this.padding.length;
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        System.arraycopy(this.padding, 0, buffer, idx, this.padding.length);
        idx += this.padding.length;
        return idx - offset;
    }
    
    @Override
    public String getCodeName() {
        return "edns-tcp-keepalive";
    }
    
    public byte[] getPadding() {
        return this.padding;
    }
    
    public void setPadding(final byte[] bytes) {
        this.padding = this.padding;
    }
    
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash += Arrays.hashCode(this.padding);
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSOptionPadding)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final DNSOptionPadding opt = (DNSOptionPadding)obj;
        return super.equals(obj) && Arrays.equals(this.padding, opt.padding);
    }
    
    @Override
    public String toString() {
        return "OPT type padding: " + new String(this.padding);
    }
}
