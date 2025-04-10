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

public class DNSAnswerAAAA extends DNSAnswer
{
    private byte[] ipAddress;
    
    public DNSAnswerAAAA(final DNSPacket packet) {
        super(packet);
        this.type = 28;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 23) {
            throw new RuntimeException("DNS answer: not enough bytes for AAAA");
        }
        int idx = offset + super.read(bytes, offset);
        System.arraycopy(bytes, idx, this.ipAddress = new byte[16], 0, 16);
        idx += 16;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        return super.getSerializedSize(offset) + 16;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        DNSPacket.writeShort((short)16, buffer, idx);
        idx += 2;
        System.arraycopy(this.ipAddress, 0, buffer, idx, 16);
        idx += 16;
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "AAAA";
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
            final InetAddress addr = InetAddress.getByAddress(this.ipAddress);
            return addr.getHostAddress();
        }
        catch (final Exception ex) {
            DNSAnswerAAAA.userLog.debug(Markers.DNS, "Invalid IP6 address for AAAA packet: " + ex.getMessage());
            throw new ServerException("db.dns.InvalidIp6Address", new Object[0]);
        }
    }
    
    public void setIpAddress(final byte[] ipAddress) {
        if (ipAddress != null && ipAddress.length != 16) {
            throw new ServerException("db.dns.InvalidIp6Address", new Object[0]);
        }
        this.ipAddress = ipAddress;
        this.packet.setModified();
    }
    
    public void setIpAddress(final String str) {
        try {
            final InetAddress addr = InetAddress.getByName(str);
            final byte[] bytes = addr.getAddress();
            if (bytes.length != 16) {
                throw new ServerException("db.dns.InvalidIp6Address", new Object[0]);
            }
            this.ipAddress = bytes;
        }
        catch (final Exception ex) {
            DNSAnswerAAAA.userLog.debug(Markers.DNS, "Invalid IP address for setIpAddress: " + str + ", " + ex.getMessage());
            throw new ServerException("db.dns.InvalidIp6Address", new Object[0]);
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
        if (!(obj instanceof DNSAnswerAAAA)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerAAAA answer = (DNSAnswerAAAA)obj;
        return Arrays.equals(this.ipAddress, answer.ipAddress);
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        InetAddress addr = null;
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
