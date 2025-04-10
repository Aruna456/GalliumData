// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java;

import org.graalvm.polyglot.Value;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.http.Header;

import java.util.function.Function;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import com.galliumdata.server.ServerException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.graalvm.polyglot.proxy.ProxyObject;

public class HttpResponse extends HttpExchange implements ProxyObject
{
    private final HttpRequest request;
    private final org.apache.http.HttpResponse resp;
    private boolean modified;
    private byte[] response;
    private String responseString;
    
    public HttpResponse(final HttpRequest request, final org.apache.http.HttpResponse resp) {
        this.request = request;
        this.resp = resp;
    }
    
    public InputStream getPayloadStream() {
        if (this.response != null) {
            return new ByteArrayInputStream(this.response);
        }
        try {
            return this.resp.getEntity().getContent();
        }
        catch (final IOException ioex) {
            throw new ServerException("db.http.server.ErrorReadingResponseBody", new Object[] { ioex });
        }
    }
    
    public OutputStream getPayloadWriteStream() {
        return new PayloadOutputStream(b -> {
            this.response = b;
            this.modified = true;
            this.responseString = null;
        });
    }
    
    public byte[] getPayload() {
        if (this.response == null) {
            if (this.resp.getEntity() == null) {
                throw new ServerException("db.http.server.ErrorReadingResponseBody", new Object[] { "Response has no payload: " + this.request.getUrl() });
            }
            final Header contentEncodingHdr = this.resp.getFirstHeader("Content-encoding");
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (contentEncodingHdr != null && contentEncodingHdr.getValue().equalsIgnoreCase("gzip")) {
                try {
                    final GZIPOutputStream zos = new GZIPOutputStream(baos);
                    this.resp.getEntity().writeTo((OutputStream)zos);
                    this.resp.getEntity().getContent().close();
                    this.response = baos.toByteArray();
                    zos.close();
                    baos.close();
                    return this.response;
                }
                catch (final Exception ex) {
                    throw new ServerException("db.http.server.ErrorReadingResponseBody", new Object[] { ex });
                }
            }
            try {
                this.resp.getEntity().writeTo((OutputStream)baos);
                this.resp.getEntity().getContent().close();
                this.response = baos.toByteArray();
                baos.close();
            }
            catch (final Exception ioex) {
                throw new ServerException("db.http.server.ErrorReadingResponseBody", new Object[] { ioex });
            }
        }
        return this.response;
    }
    
    public byte[] getPayloadNoRead() {
        return this.response;
    }
    
    public String getPayloadString() {
        if (this.responseString != null) {
            return this.responseString;
        }
        final byte[] responseBytes = this.getPayload();
        if (responseBytes == null) {
            return null;
        }
        return this.responseString = new String(responseBytes, StandardCharsets.UTF_8);
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
    
    public int getPayloadSize() {
        if (this.responseString != null) {
            return this.responseString.length();
        }
        if (this.response != null) {
            return this.response.length;
        }
        return 0;
    }
    
    public void setPayloadString(final String s) {
        if (s == null) {
            this.responseString = null;
            this.response = null;
        }
        else {
            this.responseString = s;
            this.response = s.getBytes(StandardCharsets.UTF_8);
        }
        this.modified = true;
    }
    
    public void setPayloadString(final String s, final String enc) {
        final byte[] bodyBytes = this.getPayload();
        if (bodyBytes == null) {
            this.responseString = null;
            this.response = null;
        }
        else {
            try {
                final Charset charset = Charset.forName(enc);
                this.responseString = s;
                this.response = s.getBytes(charset);
            }
            catch (final Exception ex) {
                throw new ServerException("db.http.logic.EncodingError", new Object[] { ex, Charset.availableCharsets().keySet() });
            }
        }
    }
    
    public int getResponseCode() {
        return this.resp.getStatusLine().getStatusCode();
    }
    
    public void setResponseCode(final int code) {
        this.resp.setStatusCode(code);
        this.modified = true;
    }
    
    public String getResponseMessage() {
        return this.resp.getStatusLine().getReasonPhrase();
    }
    
    public void setResponseMessage(final String s) {
        this.resp.setReasonPhrase(s);
        this.modified = true;
    }
    
    public Header[] getResponseHeaders() {
        return this.resp.getAllHeaders();
    }
    
    public Header[] getResponseHeaders(final String name) {
        return this.resp.getHeaders(name);
    }
    
    @Override
    public String getHeader(final String name) {
        final Header hdr = this.resp.getFirstHeader(name);
        if (hdr == null) {
            return null;
        }
        return hdr.getValue();
    }
    
    @Override
    public boolean hasHeader(final String name) {
        return this.resp.containsHeader(name);
    }
    
    @Override
    public void setHeader(final String name, final String value) {
        if (value == null) {
            this.resp.removeHeaders(name);
        }
        else if (this.resp.getHeaders(name) == null) {
            this.resp.addHeader(name, value);
        }
        else {
            this.resp.setHeader(name, value);
        }
        this.modified = true;
    }
    
    public void setModified() {
        this.modified = true;
    }
    
    public boolean isModified() {
        return this.modified;
    }
    
    public HttpRequest getRequest() {
        return this.request;
    }
    
    public String toString() {
        String size = "0 bytes";
        if (this.resp.getEntity() != null) {
            size = this.resp.getEntity().getContentLength() + " bytes";
        }
        String contentType = " [type unknown]";
        final Header header = this.resp.getFirstHeader("Content-type");
        if (header != null) {
            contentType = " [" + String.valueOf(header.getElements()[0]);
        }
        return "HttpResponse for " + this.request.getMethod() + " " + this.request.getUrl() + ": " + size + contentType;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "request": {
                return this.getRequest();
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
            case "getResponseHeader": {
                return (Function<Value[],Object>) args -> this.getHeader(args[0].toString());
            }
            case "hasResponseHeader": {
                return (Function<Value[],Object>) args -> this.hasHeader(args[0].toString());
            }
            case "setResponseHeader": {
                return (Function<Value[],Object>) args -> {
                    String val = args[1].toString();
                    if (args[1].isNull()) {
                        val = null;
                    }
                    this.setHeader(args[0].toString(), val);
                    return null;
                };
            }
            case "responseCode": {
                return this.getResponseCode();
            }
            case "responseMessage": {
                return this.getResponseMessage();
            }
            case "getPayloadString": {
                return (Function<Value[],Object>) args -> this.getPayloadString(args[0].toString());
            }
            case "setPayloadString": {
                return(Function<Value[],Object>)  args -> {
                    this.setPayloadString(args[0].toString(), args[1].toString());
                    return null;
                };
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key, "HTTP response" });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "request", "payload", "payloadString", "payloadStream", "payloadWriteStream", "getPayloadString", "setPayloadString", "getResponseHeader", "hasResponseHeader", "setResponseHeader", "responseCode", "responseMessage", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "request":
            case "payload":
            case "payloadString":
            case "payloadStream":
            case "payloadWriteStream":
            case "getResponseHeader":
            case "hasResponseHeader":
            case "setResponseHeader":
            case "getPayloadString":
            case "setPayloadString":
            case "responseCode":
            case "responseMessage":
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
            case "payloadString": {
                this.setPayloadString(val.asString());
                break;
            }
            case "responseCode": {
                this.setResponseCode(val.asInt());
                break;
            }
            case "responseMessage": {
                this.setResponseMessage(val.asString());
                break;
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key, "HTTP response" });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.http.logic.CannotRemoveMember", new Object[] { key, "HTTP response" });
    }
}
