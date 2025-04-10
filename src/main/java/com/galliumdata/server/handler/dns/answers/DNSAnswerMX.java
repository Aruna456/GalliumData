// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import org.graalvm.polyglot.Value;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.dns.DNSPacket;

public class DNSAnswerMX extends DNSAnswer
{
    private String exchange;
    private short preference;
    
    public DNSAnswerMX(final DNSPacket packet) {
        super(packet);
        this.type = 15;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for MX");
        }
        int idx = offset + super.read(bytes, offset);
        this.preference = DNSPacket.readShort(bytes, idx);
        idx += 2;
        final StringBuffer sb = new StringBuffer();
        idx += this.packet.readPossiblyCompressedString(bytes, idx, sb);
        this.exchange = sb.toString();
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        int size = super.getSerializedSize(offset);
        size += 2;
        size += this.packet.getSizeOfString(this.exchange, offset + size, true);
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        final int dataLength = this.packet.getSizeOfString(this.exchange, idx + 2 + 2, false);
        DNSPacket.writeShort((short)(dataLength + 2), buffer, idx);
        idx += 2;
        DNSPacket.writeShort(this.preference, buffer, idx);
        idx += 2;
        idx += this.packet.writeString(this.exchange, buffer, idx);
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "MX";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType", new Object[0]);
    }
    
    public String getExchange() {
        return this.exchange;
    }
    
    public void setExchange(final String exchange) {
        this.exchange = exchange;
        this.packet.setModified();
    }
    
    public short getPreference() {
        return this.preference;
    }
    
    public void setPreference(final short preference) {
        this.preference = preference;
        this.packet.setModified();
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "exchange": {
                return this.getExchange();
            }
            case "preference": {
                return this.getPreference();
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), (Object[])new String[] { "exchange", "preference" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "exchange":
            case "preference": {
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
            case "exchange": {
                this.setExchange(value.asString());
                break;
            }
            case "preference": {
                this.setPreference(value.asShort());
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
        hash += this.exchange.hashCode();
        hash += this.preference;
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerMX)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerMX answer = (DNSAnswerMX)obj;
        return this.exchange.equals(answer.exchange) && this.preference == answer.preference;
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s = s + " exchange: " + this.exchange;
        return s;
    }
}
