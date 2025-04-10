// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import org.apache.logging.log4j.LogManager;
import java.util.Arrays;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.handler.dns.DNSPacket;
import org.graalvm.polyglot.proxy.ProxyObject;

public class DNSAnswer implements ProxyObject
{
    protected final DNSPacket packet;
    protected String name;
    protected short type;
    protected short cls;
    protected int ttl;
    protected short rdLength;
    private byte[] data;
    protected static final Logger userLog;
    
    public DNSAnswer(final DNSPacket packet) {
        this.name = "";
        this.cls = 1;
        this.ttl = 600;
        this.packet = packet;
    }
    
    public static DNSAnswer readAnswer(final byte[] bytes, final int offset, final DNSPacket pkt) {
        int idx = offset;
        final StringBuffer nameSb = new StringBuffer();
        idx += pkt.readPossiblyCompressedString(bytes, idx, nameSb);
        final short type = DNSPacket.readShort(bytes, idx);
        idx += 2;
        DNSAnswer answer = null;
        switch (type) {
            case 1: {
                answer = new DNSAnswerA(pkt);
                break;
            }
            case 2: {
                answer = new DNSAnswerNS(pkt);
                break;
            }
            case 5: {
                answer = new DNSAnswerCNAME(pkt);
                break;
            }
            case 6: {
                answer = new DNSAnswerSOA(pkt);
                break;
            }
            case 12: {
                answer = new DNSAnswerPTR(pkt);
                break;
            }
            case 13: {
                answer = new DNSAnswerHINFO(pkt);
                break;
            }
            case 15: {
                answer = new DNSAnswerMX(pkt);
                break;
            }
            case 16: {
                answer = new DNSAnswerTXT(pkt);
                break;
            }
            case 28: {
                answer = new DNSAnswerAAAA(pkt);
                break;
            }
            case 41: {
                answer = new DNSAnswerOPT(pkt);
                break;
            }
            default: {
                answer = new DNSAnswer(pkt);
                answer.type = type;
                break;
            }
        }
        idx += answer.read(bytes, offset);
        if (answer.getTypeName().equals("UNKNOWN")) {
            System.arraycopy(bytes, idx, answer.data = new byte[answer.rdLength], 0, answer.rdLength);
            idx += answer.rdLength;
        }
        return answer;
    }
    
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes");
        }
        int idx = offset;
        final StringBuffer nameSb = new StringBuffer();
        idx += this.packet.readPossiblyCompressedString(bytes, idx, nameSb);
        this.name = nameSb.toString();
        final int t = DNSPacket.readShort(bytes, idx);
        if (t != this.type) {
            throw new ServerException("db.dns.proto.AnswerTypeMismatch", new Object[] { t, this.type });
        }
        idx += 2;
        this.cls = DNSPacket.readShort(bytes, idx);
        idx += 2;
        this.ttl = DNSPacket.readInt(bytes, idx);
        idx += 4;
        this.rdLength = DNSPacket.readShort(bytes, idx);
        idx += 2;
        if (this.getTypeName().startsWith("UNKNOWN")) {
            System.arraycopy(bytes, idx, this.data = new byte[this.rdLength], 0, this.rdLength);
        }
        return idx - offset;
    }
    
    public int getSerializedSize(final int offset) {
        int size = this.packet.getSizeOfString(this.name, offset, true);
        size += 10;
        if (this.getTypeName().startsWith("UNKNOWN")) {
            size += this.data.length;
        }
        return size;
    }
    
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += this.packet.writeString(this.name, buffer, idx);
        DNSPacket.writeShort(this.type, buffer, idx);
        idx += 2;
        DNSPacket.writeShort(this.cls, buffer, idx);
        idx += 2;
        DNSPacket.writeInt(this.ttl, buffer, idx);
        idx += 4;
        if (this.getTypeName().startsWith("UNKNOWN") && this.data != null) {
            DNSPacket.writeShort((short)this.data.length, buffer, idx);
            idx += 2;
            System.arraycopy(this.data, 0, buffer, idx, this.data.length);
            idx += this.data.length;
        }
        return idx - offset;
    }
    
    public String getTypeName() {
        return "UNKNOWN " + this.type;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
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
    
    public int getTtl() {
        return this.ttl;
    }
    
    public void setTtl(final int ttl) {
        this.ttl = ttl;
        this.packet.setModified();
    }
    
    public byte[] getData() {
        return this.data;
    }
    
    public void setData(final byte[] data) {
        this.data = data;
        this.packet.setModified();
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "name": {
                return this.getName();
            }
            case "type": {
                return this.getType();
            }
            case "typeName": {
                return this.getTypeName();
            }
            case "cls": {
                return this.getCls();
            }
            case "clsName": {
                return this.getClsName();
            }
            case "ttl": {
                return this.getTtl();
            }
            case "data": {
                return this.getData();
            }
            default: {
                throw new ServerException("db.dns.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "name", "type", "typeName", "cls", "clsName", "ttl", "data" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "name":
            case "type":
            case "typeName":
            case "cls":
            case "clsName":
            case "ttl":
            case "data": {
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
            case "cls": {
                this.setCls(value.asShort());
                break;
            }
            case "ttl": {
                this.setTtl(value.asShort());
                break;
            }
            case "data": {
                if (value.isHostObject()) {
                    this.setData((byte[])value.asHostObject());
                    break;
                }
                if (value.hasArrayElements()) {
                    final byte[] bytes = new byte[(int)value.getArraySize()];
                    for (int i = 0; i < value.getArraySize(); ++i) {
                        final Value v = value.getArrayElement((long)i);
                        if (v.isNumber()) {
                            bytes[i] = value.getArrayElement((long)i).asByte();
                        }
                        else if (v.isString()) {
                            bytes[i] = (byte)v.asString().charAt(0);
                        }
                    }
                    this.setData(bytes);
                    break;
                }
                throw new ServerException("db.dns.logic.InvalidArgumentType", new Object[] { "data", "byte array", value.toString() });
            }
            default: {
                throw new ServerException("db.dns.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.dns.logic.CannotRemoveMember", new Object[] { key, "DNSAnswer" + this.getTypeName() });
    }
    
    @Override
    public int hashCode() {
        int hash = this.name.hashCode();
        hash += this.type * 256 * 256;
        hash += this.cls * 256;
        hash += this.ttl;
        if (this.data != null) {
            hash += Arrays.hashCode(this.data);
        }
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswer)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final DNSAnswer answer = (DNSAnswer)obj;
        return this.name.equals(answer.name) && this.type == answer.type && this.cls == answer.cls && this.ttl == answer.ttl && Arrays.equals(this.data, answer.data);
    }
    
    @Override
    public String toString() {
        return "type " + this.getTypeName() + " - name " + this.name;
    }
    
    static {
        userLog = LogManager.getLogger("galliumdata.uselog");
    }
}
