// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public abstract class ProtocolData implements ProxyObject
{
    protected boolean modified;
    protected ProtocolData parent;
    
    public ProtocolData() {
        this.modified = false;
    }
    
    public ProtocolData getParent() {
        return this.parent;
    }
    
    public void setParent(final ProtocolData parent) {
        this.parent = parent;
    }
    
    public abstract boolean isObject();
    
    public abstract boolean isArray();
    
    public abstract boolean isValue();
    
    public abstract int getSize();
    
    public abstract ProtocolData get(final String p0);
    
    public ProtocolData get(final int n) {
        throw new ProtocolException("get not implemented in ProtocolData", new Object[0]);
    }
    
    public abstract void put(final String p0, final ProtocolData p1);
    
    public void putString(final String name, final String value) {
        throw new ProtocolException("Not implemented", new Object[0]);
    }
    
    public void putNumber(final String name, final Number value) {
        throw new ProtocolException("Not implemented", new Object[0]);
    }
    
    public void putBoolean(final String name, final Boolean b) {
        throw new ProtocolException("Not implemented", new Object[0]);
    }
    
    public abstract ProtocolData remove(final String p0);
    
    public abstract boolean hasProperty(final String p0);
    
    public abstract String toJSON();
    
    public String toPrettyJSON() {
        return this.toPrettyJSON(0);
    }
    
    public abstract String toPrettyJSON(final int p0);
    
    public abstract String toJSONDebug(final int p0, final int p1);
    
    public int asInt() {
        if (!this.isValue()) {
            throw new ProtocolException("", new Object[0]);
        }
        final ProtocolDataValue value = (ProtocolDataValue)this;
        return value.asNumber().intValue();
    }
    
    public String asString() {
        if (!this.isValue()) {
            throw new ProtocolException("", new Object[0]);
        }
        final ProtocolDataValue value = (ProtocolDataValue)this;
        return value.asString();
    }
    
    public boolean asBoolean() {
        if (!this.isValue()) {
            throw new ProtocolException("", new Object[0]);
        }
        final ProtocolDataValue value = (ProtocolDataValue)this;
        return value.asBoolean();
    }
    
    public byte[] asByteArray() {
        if (!this.isArray()) {
            throw new ProtocolException("", new Object[0]);
        }
        final ProtocolDataArray value = (ProtocolDataArray)this;
        return value.getByteArray();
    }
    
    public boolean isModified() {
        return this.modified;
    }
    
    public void setModified() {
        this.modified = true;
        if (this.parent != null) {
            this.parent.setModified();
        }
    }
    
    public void resetModified() {
        this.modified = false;
    }
    
    public Value toPolyglotValue() {
        return null;
    }
}
