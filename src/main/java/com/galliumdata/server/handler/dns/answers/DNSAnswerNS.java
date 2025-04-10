// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import org.graalvm.polyglot.Value;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.dns.DNSPacket;

public class DNSAnswerNS extends DNSAnswer
{
    private String serverName;
    
    public DNSAnswerNS(final DNSPacket packet) {
        super(packet);
        this.type = 2;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for NS");
        }
        int idx = offset + super.read(bytes, offset);
        final StringBuffer sb = new StringBuffer();
        idx += this.packet.readPossiblyCompressedString(bytes, idx, sb);
        this.serverName = sb.toString();
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        int size = super.getSerializedSize(offset);
        size += this.packet.getSizeOfString(this.serverName, offset + size, true);
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        final int dataLength = this.packet.getSizeOfString(this.serverName, idx + 2, false);
        DNSPacket.writeShort((short)dataLength, buffer, idx);
        idx += 2;
        idx += this.packet.writeString(this.serverName, buffer, idx);
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "NS";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType", new Object[0]);
    }
    
    public String getServerName() {
        return this.serverName;
    }
    
    public void setServerName(final String serverName) {
        this.serverName = serverName;
        this.packet.setModified();
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "serverName": {
                return this.getServerName();
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), (Object[])new String[] { "serverName" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "serverName": {
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
            case "serverName": {
                this.setServerName(value.asString());
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
        hash += this.serverName.hashCode();
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerNS)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerNS answer = (DNSAnswerNS)obj;
        return this.serverName.equals(answer.serverName);
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s = s + " server name: " + this.serverName;
        return s;
    }
}
