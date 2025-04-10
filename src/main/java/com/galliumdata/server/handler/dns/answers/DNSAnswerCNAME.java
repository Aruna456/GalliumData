// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import org.graalvm.polyglot.Value;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.dns.DNSPacket;

public class DNSAnswerCNAME extends DNSAnswer
{
    private String aliasName;
    
    public DNSAnswerCNAME(final DNSPacket packet) {
        super(packet);
        this.type = 5;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for CNAME");
        }
        int idx = offset + super.read(bytes, offset);
        final StringBuffer sb = new StringBuffer();
        idx += this.packet.readPossiblyCompressedString(bytes, idx, sb);
        this.aliasName = sb.toString();
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        int size = super.getSerializedSize(offset);
        size += this.packet.getSizeOfString(this.aliasName, offset + size, true);
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        final int dataLength = this.packet.getSizeOfString(this.aliasName, idx + 2, false);
        DNSPacket.writeShort((short)dataLength, buffer, idx);
        idx += 2;
        idx += this.packet.writeString(this.aliasName, buffer, idx);
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "CNAME";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType", new Object[0]);
    }
    
    public String getAliasName() {
        return this.aliasName;
    }
    
    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
        this.packet.setModified();
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "aliasName": {
                return this.getAliasName();
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), (Object[])new String[] { "aliasName" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "aliasName": {
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
            case "aliasName": {
                this.setAliasName(value.asString());
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
        hash += this.aliasName.hashCode();
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerCNAME)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerCNAME answer = (DNSAnswerCNAME)obj;
        return this.aliasName.equals(answer.aliasName);
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s = s + " alias: " + this.aliasName;
        return s;
    }
}
