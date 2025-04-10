// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns.answers;

import org.graalvm.polyglot.Value;
import com.google.common.collect.ObjectArrays;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.dns.DNSPacket;

public class DNSAnswerSOA extends DNSAnswer
{
    private String masterName;
    private String responsibleName;
    private int serialNumber;
    private int refreshInterval;
    private int retryInterval;
    private int expireInterval;
    private int negativeCachingTTL;
    
    public DNSAnswerSOA(final DNSPacket packet) {
        super(packet);
        this.type = 6;
    }
    
    @Override
    public int read(final byte[] bytes, final int offset) {
        if (bytes.length - offset < 11) {
            throw new RuntimeException("DNS answer: not enough bytes for SOA");
        }
        int idx = offset + super.read(bytes, offset);
        StringBuffer sb = new StringBuffer();
        idx += this.packet.readPossiblyCompressedString(bytes, idx, sb);
        this.masterName = sb.toString();
        sb = new StringBuffer();
        idx += this.packet.readPossiblyCompressedString(bytes, idx, sb);
        this.responsibleName = sb.toString();
        this.serialNumber = DNSPacket.readInt(bytes, idx);
        idx += 4;
        this.refreshInterval = DNSPacket.readInt(bytes, idx);
        idx += 4;
        this.retryInterval = DNSPacket.readInt(bytes, idx);
        idx += 4;
        this.expireInterval = DNSPacket.readInt(bytes, idx);
        idx += 4;
        this.negativeCachingTTL = DNSPacket.readInt(bytes, idx);
        idx += 4;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize(final int offset) {
        int size = super.getSerializedSize(offset);
        size += this.packet.getSizeOfString(this.masterName, offset + size, true);
        size += this.packet.getSizeOfString(this.responsibleName, offset + size, true);
        size += 20;
        return size;
    }
    
    @Override
    public int writeToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        idx += super.writeToBytes(buffer, idx);
        int dataLength = this.packet.getSizeOfString(this.masterName, idx + 2, false);
        dataLength += this.packet.getSizeOfString(this.responsibleName, idx + dataLength, false);
        DNSPacket.writeShort((short)(dataLength + 20), buffer, idx);
        idx += 2;
        idx += this.packet.writeString(this.masterName, buffer, idx);
        idx += this.packet.writeString(this.responsibleName, buffer, idx);
        DNSPacket.writeInt(this.serialNumber, buffer, idx);
        idx += 4;
        DNSPacket.writeInt(this.refreshInterval, buffer, idx);
        idx += 4;
        DNSPacket.writeInt(this.retryInterval, buffer, idx);
        idx += 4;
        DNSPacket.writeInt(this.expireInterval, buffer, idx);
        idx += 4;
        DNSPacket.writeInt(this.negativeCachingTTL, buffer, idx);
        idx += 4;
        return idx - offset;
    }
    
    @Override
    public String getTypeName() {
        return "SOA";
    }
    
    @Override
    public void setType(final short type) {
        throw new ServerException("db.dns.CannotSetAnswerType", new Object[0]);
    }
    
    public String getMasterName() {
        return this.masterName;
    }
    
    public void setMasterName(final String masterName) {
        this.masterName = masterName;
        this.packet.setModified();
    }
    
    public String getResponsibleName() {
        return this.responsibleName;
    }
    
    public void setResponsibleName(final String responsibleName) {
        this.responsibleName = responsibleName;
        this.packet.setModified();
    }
    
    public int getSerialNumber() {
        return this.serialNumber;
    }
    
    public void setSerialNumber(final int serialNumber) {
        this.serialNumber = serialNumber;
        this.packet.setModified();
    }
    
    public int getRefreshInterval() {
        return this.refreshInterval;
    }
    
    public void setRefreshInterval(final int refreshInterval) {
        this.refreshInterval = refreshInterval;
        this.packet.setModified();
    }
    
    public int getRetryInterval() {
        return this.retryInterval;
    }
    
    public void setRetryInterval(final int retryInterval) {
        this.retryInterval = retryInterval;
        this.packet.setModified();
    }
    
    public int getExpireInterval() {
        return this.expireInterval;
    }
    
    public void setExpireInterval(final int expireInterval) {
        this.expireInterval = expireInterval;
        this.packet.setModified();
    }
    
    public int getNegativeCachingTTL() {
        return this.negativeCachingTTL;
    }
    
    public void setNegativeCachingTTL(final int negativeCachingTTL) {
        this.negativeCachingTTL = negativeCachingTTL;
        this.packet.setModified();
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "masterName": {
                return this.getMasterName();
            }
            case "responsibleName": {
                return this.getResponsibleName();
            }
            case "serialNumber": {
                return this.getSerialNumber();
            }
            case "refreshInterval": {
                return this.getRefreshInterval();
            }
            case "retryInterval": {
                return this.getRetryInterval();
            }
            case "expireInterval": {
                return this.getExpireInterval();
            }
            case "negativeCachingTTL": {
                return this.getNegativeCachingTTL();
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return ObjectArrays.concat((Object[])super.getMemberKeys(), (Object[])new String[] { "masterName", "responsibleName", "serialNumber", "refreshInterval", "retryInterval", "expireInterval", "negativeCachingTTL" }, (Class)String.class);
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "masterName":
            case "responsibleName":
            case "serialNumber":
            case "refreshInterval":
            case "retryInterval":
            case "expireInterval":
            case "negativeCachingTTL": {
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
            case "masterName": {
                this.setMasterName(value.asString());
                break;
            }
            case "responsibleName": {
                this.setResponsibleName(value.asString());
                break;
            }
            case "serialNumber": {
                this.setSerialNumber(value.asInt());
                break;
            }
            case "refreshInterval": {
                this.setRefreshInterval(value.asInt());
                break;
            }
            case "retryInterval": {
                this.setRetryInterval(value.asInt());
                break;
            }
            case "expireInterval": {
                this.setExpireInterval(value.asInt());
                break;
            }
            case "negativeCachingTTL": {
                this.setNegativeCachingTTL(value.asInt());
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
        if (this.masterName != null) {
            hash += this.masterName.hashCode();
        }
        if (this.responsibleName != null) {
            hash += this.responsibleName.hashCode();
        }
        hash += this.serialNumber;
        hash += this.refreshInterval * 17;
        hash += this.retryInterval * 117;
        hash += this.expireInterval * 1713;
        hash += this.negativeCachingTTL * 171317;
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DNSAnswerSOA)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DNSAnswerSOA answer = (DNSAnswerSOA)obj;
        return (this.masterName == null || this.masterName.equals(answer.masterName)) && (answer.masterName == null || answer.masterName.equals(this.masterName)) && (this.responsibleName == null || this.responsibleName.equals(answer.responsibleName)) && (answer.responsibleName == null || answer.responsibleName.equals(this.responsibleName)) && this.serialNumber == answer.serialNumber && this.refreshInterval == answer.refreshInterval && this.retryInterval == answer.retryInterval && this.expireInterval == answer.expireInterval && this.negativeCachingTTL == answer.negativeCachingTTL;
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s = s + " master: " + this.masterName + ", responsible: " + this.responsibleName;
        return s;
    }
}
