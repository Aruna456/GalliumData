// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.proxy.ProxyObject;

public class DNSQuestion implements ProxyObject
{
    private final DNSPacket packet;
    private String name;
    private short type;
    private short cls;
    
    public DNSQuestion(final DNSPacket pkt) {
        this.name = "";
        this.type = 1;
        this.cls = 1;
        this.packet = pkt;
    }
    
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 7) {
            throw new RuntimeException("DNS question: not enough bytes");
        }
        int idx = offset;
        final StringBuffer nameSb = new StringBuffer();
        idx += this.packet.readPossiblyCompressedString(bytes, idx, nameSb);
        this.name = nameSb.toString();
        this.type = DNSPacket.readShort(bytes, idx);
        idx += 2;
        this.cls = DNSPacket.readShort(bytes, idx);
        idx += 2;
        return idx - offset;
    }
    
    public int getSerializedSize(final int offset) {
        return this.packet.getSizeOfString(this.name, offset, true) + 2 + 2;
    }
    
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += this.packet.writeString(this.name, buffer, idx);
        DNSPacket.writeShort(this.type, buffer, idx);
        idx += 2;
        DNSPacket.writeShort(this.cls, buffer, idx);
        idx += 2;
        return idx - offset;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
        if (name == null) {
            throw new ServerException("db.dns.NameCannotBeNull", new Object[0]);
        }
        final String[] split;
        final String[] nameParts = split = name.split("\\.");
        for (final String namePart : split) {
            if (namePart.length() > 64) {
                throw new ServerException("db.dns.NamePartTooLong", new Object[] { name, namePart });
            }
            if (namePart.length() == 0) {
                throw new ServerException("db.dns.NamePartEmpty", new Object[] { name });
            }
        }
        this.name = name;
        this.packet.setModified();
    }
    
    public short getType() {
        return this.type;
    }
    
    public void setType(final short type) {
        this.type = type;
        this.packet.setModified();
    }
    
    public String getTypeName() {
        switch (this.type) {
            case 1: {
                return "A";
            }
            case 2: {
                return "NS";
            }
            case 5: {
                return "CNAME";
            }
            case 6: {
                return "SOA";
            }
            case 11: {
                return "WKS";
            }
            case 12: {
                return "PTR";
            }
            case 13: {
                return "HINFO";
            }
            case 15: {
                return "MX";
            }
            case 16: {
                return "TXT";
            }
            case 28: {
                return "AAAA";
            }
            case 33: {
                return "SRV";
            }
            case 37: {
                return "CERT";
            }
            case 41: {
                return "OPT";
            }
            case 44: {
                return "SSHFP";
            }
            case 65: {
                return "HTTPS";
            }
            case 255: {
                return "*";
            }
            case 257: {
                return "CAA";
            }
            default: {
                return "Unknown " + this.type;
            }
        }
    }
    
    public void setTypeName(final String s) {
        switch (s) {
            case "A": {
                this.setType((short)1);
                break;
            }
            case "NS": {
                this.setType((short)2);
                break;
            }
            case "CNAME": {
                this.setType((short)5);
                break;
            }
            case "SOA": {
                this.setType((short)6);
                break;
            }
            case "WKS": {
                this.setType((short)11);
                break;
            }
            case "PTR": {
                this.setType((short)12);
                break;
            }
            case "HINFO": {
                this.setType((short)13);
                break;
            }
            case "MX": {
                this.setType((short)15);
                break;
            }
            case "TXT": {
                this.setType((short)16);
                break;
            }
            case "AAAA": {
                this.setType((short)28);
                break;
            }
            case "SRV": {
                this.setType((short)33);
                break;
            }
            case "CERT": {
                this.setType((short)37);
                break;
            }
            case "OPT": {
                this.setType((short)41);
                break;
            }
            case "SSHFP": {
                this.setType((short)44);
                break;
            }
            case "HTTPS": {
                this.setType((short)65);
                break;
            }
            case "*": {
                this.setType((short)255);
                break;
            }
            case "CAA": {
                this.setType((short)257);
                break;
            }
            default: {
                throw new ServerException("db.dns.UnknownTypeName", new Object[] { s });
            }
        }
    }
    
    public short getCls() {
        return this.cls;
    }
    
    public String getClsName() {
        switch (this.cls) {
            case 1: {
                return "IN";
            }
            case 3: {
                return "CH";
            }
            case 4: {
                return "HS";
            }
            default: {
                return "" + this.cls;
            }
        }
    }
    
    public void setCls(final short cls) {
        this.cls = cls;
        this.packet.setModified();
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "name": {
                return this.name;
            }
            case "type": {
                return this.type;
            }
            case "typeName": {
                return this.getTypeName();
            }
            case "cls": {
                return this.cls;
            }
            case "clsName": {
                return this.getClsName();
            }
            default: {
                throw new ServerException("db.dns.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "name", "type", "typeName", "cls", "clsName" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "name":
            case "type":
            case "typeName":
            case "cls":
            case "clsName": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "name": {
                this.setName(value.asString());
                break;
            }
            case "type": {
                this.setType(value.asShort());
                break;
            }
            case "typeName": {
                this.setTypeName(value.asString());
                break;
            }
            case "cls": {
                this.setCls(value.asShort());
                break;
            }
            default: {
                throw new ServerException("db.dns.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.dns.logic.CannotRemoveMember", new Object[] { key, "DNSPacket" });
    }
    
    @Override
    public int hashCode() {
        return this.name.hashCode() + this.type * 16 + this.cls;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSQuestion)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final DNSQuestion quest = (DNSQuestion)obj;
        return this.name.equals(quest.name) && this.type == quest.type && this.cls == quest.cls;
    }
    
    @Override
    public String toString() {
        return "    Q - type: " + this.getTypeName() + " - name: " + this.name;
    }
}
