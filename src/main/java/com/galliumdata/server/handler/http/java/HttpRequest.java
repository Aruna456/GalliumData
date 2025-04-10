// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java;

import org.graalvm.polyglot.Value;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.Arrays;
import com.galliumdata.server.ServerException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.jetty.server.Request;
import org.graalvm.polyglot.proxy.ProxyObject;

public class HttpRequest extends HttpExchange implements ProxyObject
{
    private final Request request;
    private String destinationProtocol;
    private String destinationHost;
    private int destinationPort;
    private String destinationUrl;
    private String destinationMethod;
    private String destinationFullUrl;
    private final Map<String, String> overrideHeaders;
    private byte[] body;
    private boolean modified;
    private static final String[] ALLOWED_METHODS;
    
    public HttpRequest(final Request request) {
        this.overrideHeaders = new HashMap<String, String>();
        this.request = request;
    }
    
    public String getProtocol() {
        if (this.destinationProtocol != null) {
            return this.destinationProtocol;
        }
        return this.request.getProtocol();
    }
    
    public String getDestinationProtocol() {
        return this.destinationProtocol;
    }
    
    public void setDestinationProtocol(final String s) {
        if (s != null && !s.equals("http") && !s.equals("https")) {
            throw new ServerException("db.http.logic.InvalidArgumentValue", new Object[] { "destinationProtocol", "HttpRequest", s, "http,https" });
        }
        this.destinationProtocol = s;
    }
    
    public String getAddress() {
        return this.request.getServerName();
    }
    
    public String getDestinationHost() {
        return this.destinationHost;
    }
    
    public void setDestinationHost(final String s) {
        this.destinationHost = s;
    }
    
    public int getPort() {
        return this.request.getServerPort();
    }
    
    public int getDestinationPort() {
        return this.destinationPort;
    }
    
    public void setDestinationPort(final int n) {
        if (n < 0 || n > 65536) {
            throw new ServerException("db.http.logic.InvalidArgumentValue", new Object[] { "destinationPort", "HttpRequest", n, "1-65536" });
        }
        this.destinationPort = n;
    }
    
    public String getMethod() {
        return this.request.getMethod();
    }
    
    public String getDestinationMethod() {
        return this.destinationMethod;
    }
    
    public void setDestinationMethod(final String s) {
        if (s != null) {
            final String s2 = s.toUpperCase();
            if (Arrays.stream(HttpRequest.ALLOWED_METHODS).noneMatch(m -> m.equals(s2))) {
                throw new ServerException("db.http.logic.InvalidArgumentValue", new Object[] { "destinationMethod", s, HttpRequest.ALLOWED_METHODS });
            }
            this.destinationMethod = s2;
        }
        else {
            this.destinationMethod = null;
        }
    }
    
    public String getUrl() {
        return this.request.getRequestURI();
    }
    
    public String getDestinationUrl() {
        return this.destinationUrl;
    }
    
    public void setDestinationUrl(final String s) {
        this.destinationUrl = s;
    }
    
    public String getDestinationFullUrl() {
        return this.destinationFullUrl;
    }
    
    public void setDestinationFullUrl(final String s) {
        this.destinationFullUrl = s;
    }
    
    public InetAddress getClientAddress() {
        return this.request.getRemoteInetSocketAddress().getAddress();
    }
    
    public String getClientAddressString() {
        return this.request.getRemoteInetSocketAddress().getAddress().getHostAddress();
    }
    
    public int getClientPort() {
        return this.request.getRemoteInetSocketAddress().getPort();
    }
    
    @Override
    public String getHeader(final String name) {
        return this.request.getHeader(name);
    }
    
    @Override
    public boolean hasHeader(final String name) {
        return this.request.getHeader(name) != null;
    }
    
    @Override
    public void setHeader(final String name, final String value) {
        this.overrideHeaders.put(name, value);
        this.modified = true;
    }
    
    public boolean hasOverrideHeader(final String name) {
        return this.overrideHeaders.containsKey(name);
    }
    
    public boolean hasPayload() {
        return this.request.getContentLength() > 0;
    }
    
    public byte[] getPayload() {
        if (this.body == null) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
            try {
                final InputStream is = (InputStream)this.request.getInputStream();
                if (is != null) {
                    is.transferTo(baos);
                    this.body = baos.toByteArray();
                    is.close();
                }
            }
            catch (final Exception ex) {
                throw new ServerException("db.http.server.ErrorReadingRequestBody", new Object[] { ex });
            }
        }
        return this.body;
    }
    
    public String getPayloadString() {
        final byte[] bodyBytes = this.getPayload();
        if (bodyBytes == null) {
            return null;
        }
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }
    
    public String getPayloadString(final String encoding) {
        try {
            final Charset charset = Charset.forName(encoding);
            final byte[] bodyBytes = this.getPayload();
            if (bodyBytes == null) {
                return null;
            }
            return new String(bodyBytes, charset);
        }
        catch (final Exception ex) {
            throw new ServerException("db.http.logic.EncodingError", new Object[] { ex, Charset.availableCharsets().keySet() });
        }
    }
    
    public InputStream getPayloadStream() {
        try {
            return (InputStream)this.request.getInputStream();
        }
        catch (final IOException ioex) {
            throw new ServerException("db.http.server.ErrorReadingRequestBody", new Object[] { ioex });
        }
    }
    
    public OutputStream getPayloadWriteStream() {
        return new PayloadOutputStream(b -> {
            this.body = b;
            this.modified = true;
        });
    }
    
    public void setPayload(final byte[] bytes) {
        this.body = bytes;
        this.modified = true;
    }
    
    public void setPayloadString(final String s) {
        if (s == null) {
            this.body = null;
        }
        else {
            this.body = s.getBytes(StandardCharsets.UTF_8);
        }
        this.modified = true;
    }
    
    public void setPayloadString(final String s, final String enc) {
        try {
            final Charset charset = Charset.forName(enc);
            final byte[] bodyBytes = this.getPayload();
            if (bodyBytes == null) {
                this.body = null;
            }
            else {
                this.body = s.getBytes(charset);
            }
        }
        catch (final Exception ex) {
            throw new ServerException("db.http.logic.EncodingError", new Object[] { ex, Charset.availableCharsets().keySet() });
        }
    }
    
    public Map<String, String> getOverrideHeaders() {
        return this.overrideHeaders;
    }
    
    public boolean isModified() {
        return this.modified;
    }
    
    public boolean bodyHasBeenRead() {
        return this.body != null;
    }
    
    public String toString() {
        return "HttpRequest: " + this.getMethod() + " " + this.getUrl();
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "protocol": {
                return this.getProtocol();
            }
            case "address": {
                return this.getAddress();
            }
            case "port": {
                return this.getPort();
            }
            case "method": {
                return this.getMethod();
            }
            case "url": {
                return this.getUrl();
            }
            case "destinationProtocol": {
                return this.getDestinationProtocol();
            }
            case "destinationHost": {
                return this.getDestinationHost();
            }
            case "destinationPort": {
                return this.getDestinationPort();
            }
            case "destinationMethod": {
                return this.getDestinationMethod();
            }
            case "destinationUrl": {
                return this.getDestinationUrl();
            }
            case "destinationFullUrl": {
                return this.getDestinationFullUrl();
            }
            case "clientAddress": {
                return this.getClientAddress();
            }
            case "clientAddressString": {
                return this.getClientAddressString();
            }
            case "payload": {
                return this.getPayload();
            }
            case "payloadString": {
                return this.getPayloadString();
            }
            case "payloadStream": {
                return this.getPayloadStream();
            }
            case "payloadWriteStream": {
                return this.getPayloadWriteStream();
            }
            case "getHeader": {
                return (Function<Value[],Object>) args -> this.getHeader(args[0].toString());
            }
            case "hasHeader": {
                return (Function<Value[],Object>) args -> this.hasHeader(args[0].toString());
            }
            case "setHeader": {
                return (Function<Value[],Object>) args -> {
                    this.setHeader(args[0].toString(), args[1].toString());
                    return null;
                };
            }
            case "getPayloadString": {
                return (Function<Value[],Object>) args -> this.getPayloadString(args[0].toString());
            }
            case "setPayloadString": {
                return (Function<Value[],Object>) args -> {
                    this.setPayloadString(args[0].toString(), args[1].toString());
                    return null;
                };
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key, "HTTP request" });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "protocol", "address", "port", "method", "url", "destinationProtocol", "destinationHost", "destinationPort", "destinationMethod", "destinationUrl", "destinationFullUrl", "clientAddress", "clientAddressString", "payload", "payloadString", "getPayloadString", "setPayloadString", "payloadStream", "payloadWriteStream", "getHeader", "setHeader", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "protocol":
            case "address":
            case "port":
            case "method":
            case "url":
            case "destinationProtocol":
            case "destinationHost":
            case "destinationPort":
            case "destinationMethod":
            case "destinationUrl":
            case "destinationFullUrl":
            case "clientAddress":
            case "clientAddressString":
            case "payload":
            case "payloadString":
            case "payloadStream":
            case "payloadWriteStream":
            case "getHeader":
            case "setHeader":
            case "getPayloadString":
            case "setPayloadString":
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
            case "destinationProtocol": {
                this.setDestinationProtocol(val.asString());
                break;
            }
            case "destinationHost": {
                this.setDestinationHost(val.asString());
                break;
            }
            case "destinationPort": {
                if (val == null || val.isNull()) {
                    this.setDestinationPort(0);
                    break;
                }
                this.setDestinationPort(val.asInt());
                break;
            }
            case "destinationMethod": {
                this.setDestinationMethod(val.asString());
                break;
            }
            case "destinationUrl": {
                this.setDestinationUrl(val.asString());
                break;
            }
            case "destinationFullUrl": {
                this.setDestinationFullUrl(val.asString());
                break;
            }
            case "payloadString": {
                this.setPayloadString(val.asString());
                break;
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key, "HTTP request" });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.http.logic.CannotRemoveMember", new Object[] { key, "HTTP request" });
    }
    
    static {
        ALLOWED_METHODS = new String[] { "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "TRACE", "HEAD", "CONNECT" };
    }
}
