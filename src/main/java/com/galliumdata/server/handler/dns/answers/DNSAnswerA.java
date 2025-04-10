// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import java.util.Arrays;
import org.graalvm.polyglot.Value;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.log.Markers;
import java.net.InetAddress;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.dns.DNSPacket;

public class DNSAnswerA extends DNSAnswer
{
    private byte[] ipAddress;
    
    public DNSAnswerA(final DNSPacket packet) {
        super(packet);
        this.type = 1;
        this.ipAddress = new byte[4];
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for A");
        }
        int idx = offset + super.read(bytes, offset);
        System.arraycopy(bytes, idx, this.ipAddress = new byte[4], 0, 4);
        idx += 4;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        return super.getSerializedSize(offset) + 4;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        DNSPacket.writeShort((short)4, buffer, idx);
        idx += 2;
        System.arraycopy(this.ipAddress, 0, buffer, idx, 4);
        idx += 4;
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "A";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType", new Object[0]);
    }
    
    public byte[] getIpAddressBytes() {
        return this.ipAddress;
    }
    
    public String getIpAddress() {
        try {
            return InetAddress.getByAddress(this.ipAddress).getHostAddress();
        }
        catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void setIpAddress(final String str) {
        try {
            final InetAddress addr = InetAddress.getByName(str);
            this.ipAddress = addr.getAddress();
        }
        catch (final Exception ex) {
            DNSAnswerA.userLog.debug(Markers.DNS, "Invalid IP address for setIpAddress: " + str + ", " + ex.getMessage());
            throw new ServerException("db.dns.InvalidIp4Address", new Object[0]);
        }
        this.packet.setModified();
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "ipAddress": {
                return this.getIpAddress();
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), (Object[])new String[] { "ipAddress" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "ipAddress": {
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
            case "ipAddress": {
                this.setIpAddress(value.asString());
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
        hash += Arrays.hashCode(this.ipAddress);
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerA)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerA answer = (DNSAnswerA)obj;
        return Arrays.equals(this.ipAddress, answer.ipAddress);
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        InetAddress addr;
        try {
            addr = InetAddress.getByAddress(this.ipAddress);
        }
        catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
        s = s + " address: " + String.valueOf(addr);
        return s;
    }
}
