// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.js.JSListWrapper;
import java.util.Iterator;
import com.galliumdata.server.ServerException;
import java.util.ArrayList;
import com.galliumdata.server.handler.dns.DNSPacket;
import java.util.List;

public class DNSAnswerOPT extends DNSAnswer
{
    private List<DNSOption> options;
    
    public DNSAnswerOPT(final DNSPacket packet) {
        super(packet);
        this.options = new ArrayList<DNSOption>();
        this.type = 41;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for OPT");
        }
        int idx = offset + super.read(bytes, offset);
        if (this.name.length() > 0) {
            throw new ServerException("db.dns.proto.NotEmptyNameInOPT", new Object[] { this.name });
        }
        while (idx - offset < this.rdLength) {
            final DNSOption option = DNSOption.createOption(this, bytes, idx);
            idx += option.read(bytes, idx);
            this.options.add(option);
        }
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        int size = super.getSerializedSize(offset);
        for (final DNSOption option : this.options) {
            size += option.getSerializedSize();
        }
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        short optionsSize = 0;
        for (final DNSOption option : this.options) {
            optionsSize += (short)option.getSerializedSize();
        }
        DNSPacket.writeShort(optionsSize, buffer, idx);
        idx += 2;
        for (final DNSOption option : this.options) {
            idx += option.writeToBytes(buffer, idx);
        }
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "OPT";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType", new Object[0]);
    }
    
    public short getUDPPayloadSize() {
        return this.cls;
    }
    
    @Override
    public short getCls() {
        throw new ServerException("db.dns.logic.CannotGetClsForOPT", new Object[0]);
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "options": {
                return new JSListWrapper(this.options, () -> this.packet.setModified());
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), (Object[])new String[] { "options" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "options": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        for (final DNSOption option : this.options) {
            hash += option.hashCode();
        }
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerOPT)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerOPT answer = (DNSAnswerOPT)obj;
        final Set<DNSOption> myOptions = new HashSet<DNSOption>(this.options);
        final Set<DNSOption> theirOptions = new HashSet<DNSOption>(answer.options);
        return myOptions.equals(theirOptions);
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s += " options: ";
        for (DNSOption opt : this.options) {
            s += opt.toString();
        }
        return s;
    }
}
