// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import java.util.Arrays;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.dns.DNSPacket;
import org.graalvm.polyglot.proxy.ProxyObject;

public class DNSOption implements ProxyObject
{
    protected DNSAnswerOPT answer;
    protected short optionCode;
    protected short optionLength;
    protected byte[] data;
    
    public static DNSOption createOption(final DNSAnswerOPT answer, final byte[] bytes, final int offset) {
        final int code = DNSPacket.readShort(bytes, offset);
        DNSOption option = null;
        switch (code) {
            case 11: {
                option = new DNSOptionTCPKeepAlive(answer);
                break;
            }
            default: {
                option = new DNSOption(answer);
                break;
            }
        }
        return option;
    }
    
    public DNSOption(final DNSAnswerOPT answer) {
        this.answer = answer;
    }
    
    public int read(final byte[] bytes, final int offset) {
        int idx = offset;
        this.optionCode = DNSPacket.readShort(bytes, idx);
        idx += 2;
        this.optionLength = DNSPacket.readShort(bytes, idx);
        idx += 2;
        if (this.getCodeName().startsWith("Unknown ")) {
            System.arraycopy(bytes, idx, this.data = new byte[this.optionLength], 0, this.optionLength);
            idx += this.optionLength;
        }
        return idx - offset;
    }
    
    public int getSerializedSize() {
        int size = 2;
        size += 2;
        if (this.getCodeName().startsWith("Unknown ")) {
            size += this.data.length;
        }
        return size;
    }
    
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        DNSPacket.writeShort(this.optionCode, buffer, idx);
        idx += 2;
        DNSPacket.writeShort((short)this.getSerializedSize(), buffer, idx);
        idx += 2;
        if (this.getCodeName().startsWith("Unknown ")) {
            System.arraycopy(this.data, 0, buffer, idx, this.data.length);
            idx += this.data.length;
        }
        return idx - offset;
    }
    
    public String getCodeName() {
        return "Unknown " + this.optionCode;
    }
    
    public short getOptionCode() {
        return this.optionCode;
    }
    
    public void setOptionCode(final short optionCode) {
        this.optionCode = optionCode;
    }
    
    public byte[] getData() {
        return this.data;
    }
    
    public void setData(final byte[] data) {
        this.data = data;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "optionCode": {
                return this.getOptionCode();
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
        return new String[] { "optionCode", "data" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "optionCode":
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
            case "optionCode": {
                this.setOptionCode(value.asShort());
                return;
            }
            default: {
                throw new ServerException("db.dns.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    @Override
    public int hashCode() {
        int hash = this.optionCode * 17;
        if (this.getCodeName().startsWith("Unknown ")) {
            hash += Arrays.hashCode(this.data) * 11;
        }
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSOption)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        final DNSOption opt = (DNSOption)obj;
        return this.optionCode == opt.optionCode && (!this.getCodeName().startsWith("Unknown ") || Arrays.equals(this.data, opt.data));
    }
    
    @Override
    public String toString() {
        return "OPT type " + this.optionCode;
    }
}
