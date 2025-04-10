// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import org.graalvm.polyglot.Value;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.dns.DNSPacket;

public class DNSAnswerPTR extends DNSAnswer
{
    private String pointerDomainName;
    
    public DNSAnswerPTR(final DNSPacket packet) {
        super(packet);
        this.type = 12;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for PTR");
        }
        int idx = offset + super.read(bytes, offset);
        final StringBuffer sb = new StringBuffer();
        idx += this.packet.readPossiblyCompressedString(bytes, idx, sb);
        this.pointerDomainName = sb.toString();
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        int size = super.getSerializedSize(offset);
        size += this.packet.getSizeOfString(this.pointerDomainName, offset + size, true);
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        final int dataLength = this.packet.getSizeOfString(this.pointerDomainName, idx + 2, false);
        DNSPacket.writeShort((short)dataLength, buffer, idx);
        idx += 2;
        idx += this.packet.writeString(this.pointerDomainName, buffer, idx);
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "PTR";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType", new Object[0]);
    }
    
    public String getPointerDomainName() {
        return this.pointerDomainName;
    }
    
    public void setPointerDomainName(final String pointerDomainName) {
        this.pointerDomainName = pointerDomainName;
        this.packet.setModified();
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "pointerDomainName": {
                return this.getPointerDomainName();
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), (Object[])new String[] { "pointerDomainName" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "pointerDomainName": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "pointerDomainName": {
                this.setPointerDomainName(value.asString());
                break;
            }
            default: {
                super.putMember(key, value);
                break;
            }
        }
    }
    
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash += this.pointerDomainName.hashCode();
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerPTR)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerPTR answer = (DNSAnswerPTR)obj;
        return this.pointerDomainName.equals(answer.pointerDomainName);
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s = s + " pointer: " + this.pointerDomainName;
        return s;
    }
}
