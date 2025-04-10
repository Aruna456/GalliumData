// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import org.graalvm.polyglot.Value;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.dns.DNSPacket;

public class DNSAnswerHINFO extends DNSAnswer
{
    private String cpu;
    private String os;
    
    public DNSAnswerHINFO(final DNSPacket packet) {
        super(packet);
        this.type = 13;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for HINFO");
        }
        int idx = offset + super.read(bytes, offset);
        final byte cpuLength = bytes[idx];
        ++idx;
        this.cpu = new String(bytes, idx, cpuLength);
        idx += cpuLength;
        final byte osLength = bytes[idx];
        ++idx;
        this.os = new String(bytes, idx, osLength);
        idx += osLength;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        int size = super.getSerializedSize(offset);
        size = ++size + this.cpu.length();
        size = ++size + this.os.length();
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        final int cpuLength = this.cpu.getBytes().length;
        buffer[idx] = (byte)cpuLength;
        ++idx;
        System.arraycopy(this.cpu.getBytes(), 0, buffer, idx, cpuLength);
        idx += cpuLength;
        final int osLength = this.os.getBytes().length;
        buffer[idx] = (byte)osLength;
        ++idx;
        System.arraycopy(this.os.getBytes(), 0, buffer, idx, osLength);
        idx += osLength;
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "HINFO";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType", new Object[0]);
    }
    
    public String getCpu() {
        return this.cpu;
    }
    
    public void setCpu(final String cpu) {
        this.cpu = cpu;
        this.packet.setModified();
    }
    
    public String getOs() {
        return this.os;
    }
    
    public void setOs(final String os) {
        this.os = os;
        this.packet.setModified();
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "cpu": {
                return this.getCpu();
            }
            case "os": {
                return this.getOs();
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), (Object[])new String[] { "cpu", "os" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "cpu":
            case "os": {
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
            case "cpu": {
                this.setCpu(value.asString());
                break;
            }
            case "os": {
                this.setOs(value.asString());
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
        hash += this.cpu.hashCode();
        hash += this.os.hashCode();
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerHINFO)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerHINFO answer = (DNSAnswerHINFO)obj;
        return this.cpu.equals(answer.cpu) && this.os.equals(answer.os);
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s = s + " cpu: " + this.cpu + ", os: " + this.os;
        return s;
    }
}
