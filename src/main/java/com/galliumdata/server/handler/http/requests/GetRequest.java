// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.requests;

import java.util.Iterator;
import com.galliumdata.server.handler.http.RawPacketWriter;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.http.RawPacket;
import java.util.LinkedHashMap;
import java.util.Map;

public class GetRequest extends HttpRequest
{
    private Map<String, String> headers;
    
    public GetRequest() {
        this.headers = new LinkedHashMap<String, String>();
    }
    
    @Override
    public String getRequestType() {
        return "GET";
    }
    
    @Override
    public void readPacket(final RawPacket pkt) {
        final byte[] buf = pkt.getBuffer();
        if (buf.length < 5) {
            throw new ServerException("db.http.protocol.RequestTooShort", new Object[] { buf.length });
        }
        final String type = new String(buf, 0, 4);
        if (!"GET ".equals(type)) {
            throw new ServerException("db.http.server.InternalError", new Object[] { "Request is not a GET: " + type });
        }
        final String reqStr = new String(buf, StandardCharsets.UTF_8);
        final String[] lines = reqStr.split("\r\n");
        if (lines.length == 0) {
            throw new ServerException("db.http.protocol.ProtocolException", new Object[] { "GET request does not contain a line" });
        }
        final String topLine = lines[0];
        if (!topLine.startsWith("GET ")) {
            throw new ServerException("db.http.server.InternalError", new Object[] { "Request is not a GET: " + type });
        }
        final String[] topLineBits = topLine.split(" ");
        if (topLineBits.length < 3) {
            throw new ServerException("db.http.protocol.ProtocolException", new Object[] { "GET request has incomplete top line" });
        }
        this.setUrl(topLineBits[1]);
        this.setProtocol(topLineBits[2]);
        for (int i = 1; i < lines.length; ++i) {
            final String line = lines[i];
            if (!line.isEmpty()) {
                final int colonIdx = line.indexOf(58);
                if (colonIdx == -1) {
                    throw new ServerException("db.http.protocol.ProtocolException", new Object[] { "GET request has incomplete header - no colon" });
                }
                final String name = line.substring(0, colonIdx);
                final String value = line.substring(colonIdx + 2);
                this.headers.put(name, value);
            }
        }
    }
    
    @Override
    public void writePacket(final RawPacketWriter writer) {
        writer.writeBytes("GET ".getBytes(StandardCharsets.UTF_8));
        writer.writeBytes(this.url.getBytes(StandardCharsets.UTF_8));
        writer.writeByte(32);
        writer.writeBytes(this.protocol.getBytes(StandardCharsets.UTF_8));
        writer.writeCRLF();
        for (final Map.Entry<String, String> entry : this.headers.entrySet()) {
            writer.writeBytes(entry.getKey().getBytes(StandardCharsets.UTF_8));
            writer.writeBytes(": ".getBytes(StandardCharsets.UTF_8));
            writer.writeBytes(entry.getValue().getBytes(StandardCharsets.UTF_8));
            writer.writeCRLF();
        }
        writer.writeCRLF();
    }
    
    public Map<String, String> getHeaders() {
        return this.headers;
    }
    
    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }
}
