// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import org.graalvm.polyglot.Value;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.js.JSListWrapper;
import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.ArrayList;
import com.galliumdata.server.handler.dns.DNSPacket;
import java.util.List;

public class DNSAnswerTXT extends DNSAnswer
{
    private final List<String> txts;
    
    public DNSAnswerTXT(final DNSPacket packet) {
        super(packet);
        this.txts = new ArrayList<String>();
        this.type = 16;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for TXT");
        }
        int idx;
        int strLen;
        for (idx = offset + super.read(bytes, offset); idx - offset < this.rdLength; idx += strLen) {
            strLen = (bytes[idx] & 0xFF);
            ++idx;
            final String txt = new String(bytes, idx, strLen);
            this.txts.add(txt);
        }
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        int size = super.getSerializedSize(offset);
        for (final String s : this.txts) {
            size = ++size + s.getBytes().length;
        }
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        short strSize = 0;
        for (final String s : this.txts) {
            ++strSize;
            strSize += (short)s.getBytes().length;
        }
        DNSPacket.writeShort(strSize, buffer, idx);
        idx += 2;
        for (final String s : this.txts) {
            final byte[] strBytes = s.getBytes(StandardCharsets.UTF_8);
            buffer[idx] = (byte)strBytes.length;
            ++idx;
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "TXT";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType");
    }
    
    public List<String> getTxts() {
        return this.txts;
    }
    
    public String getTxt(final int idx) {
        return this.txts.get(idx);
    }
    
    public void addTxt(String txt) {
        if (txt == null) {
            txt = "";
        }
        if (txt.getBytes().length > 255) {
            throw new ServerException("db.dns.logic.StringTooLong", txt.getBytes().length, 255, "TXT");
        }
        this.txts.add(txt);
        this.packet.setModified();
    }
    
    public void removeTxt(final String s) {
        this.txts.remove(s);
        this.packet.setModified();
    }
    
    public void removeTxt(final int idx) {
        this.txts.remove(idx);
        this.packet.setModified();
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "txts": {
                return new JSListWrapper(this.txts, () -> this.packet.setModified());
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), new String[] { "txts" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "txts": {
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
            case "txts": {
                if (value.hasArrayElements()) {
                    this.packet.setModified();
                    this.txts.clear();
                    for (int i = 0; i < value.getArraySize(); ++i) {
                        final Value val = value.getArrayElement(i);
                        if (!val.isString()) {
                            throw new ServerException("db.dns.logic.WrongTypeForCollection", "txts");
                        }
                        this.txts.add(val.asString());
                    }
                    break;
                }
                throw new ServerException("db.dns.logic.WrongTypeForCollection", "questions");
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
        for (final String s : this.txts) {
            hash += s.hashCode();
        }
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerTXT answer)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return this.txts.equals(answer.txts);
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s = s + " text: " + String.join(",", this.txts);
        return s;
    }
}
