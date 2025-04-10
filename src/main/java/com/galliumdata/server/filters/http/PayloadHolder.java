// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http;

import org.graalvm.polyglot.Value;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.ServerException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class PayloadHolder implements ProxyObject
{
    private final InputStream jsonStream;
    private byte[] payloadBytes;
    private String payloadString;
    private boolean modified;
    
    public PayloadHolder(final InputStream jsonStream) {
        this.jsonStream = jsonStream;
    }
    
    public byte[] getPayload() {
        if (this.payloadBytes == null) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
            try {
                this.jsonStream.transferTo(baos);
                this.payloadBytes = baos.toByteArray();
                this.jsonStream.close();
            }
            catch (final Exception ex) {
                throw new ServerException("db.http.server.ErrorReadingResponseBody", new Object[] { ex });
            }
        }
        return this.payloadBytes;
    }
    
    public String getPayloadAsString() {
        if (this.payloadString == null) {
            this.payloadString = new String(this.getPayload(), StandardCharsets.UTF_8);
        }
        return this.payloadString;
    }
    
    public void setPayload(final byte[] bytes) {
        this.payloadBytes = bytes;
        this.modified = true;
    }
    
    public void setPayloadAsString(final String s) {
        this.payloadString = s;
        this.payloadBytes = s.getBytes(StandardCharsets.UTF_8);
        this.modified = true;
    }
    
    public boolean payloadHasBeenRead() {
        return this.payloadBytes != null;
    }
    
    public boolean isModified() {
        return this.modified;
    }
    
    @Override
    public String toString() {
        return "PayloadHolder - " + this.getPayload().length + " bytes";
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "payload": {
                return this.getPayload();
            }
            case "payloadAsString": {
                return this.getPayloadAsString();
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key, "PayloadHolder" });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "payload", "payloadAsString", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "payload":
            case "payloadAsString":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value val) {
        switch (key) {
            case "payloadAsString": {
                this.setPayloadAsString(val.asString());
                return;
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key, "PayloadHolder" });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.http.logic.CannotRemoveMember", new Object[] { key, "PayloadHolder" });
    }
}
