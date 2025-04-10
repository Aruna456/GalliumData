// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.response;

import com.galliumdata.server.handler.http.RawPacketWriter;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.ServerException;
import com.google.common.primitives.Bytes;
import com.galliumdata.server.handler.http.RawPacket;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse
{
    private String protocol;
    private int statusCode;
    private String statusMsg;
    private Map<String, String> headers;
    
    public HttpResponse() {
        this.headers = new LinkedHashMap<String, String>();
    }
    
    public static HttpResponse createHttpResponse(final RawPacket pkt) {
        return new HttpResponse();
    }
    
    public void readPacket(final RawPacket pkt) {
        final byte[] bytes = pkt.getBuffer();
        int crlfIdx = Bytes.indexOf(bytes, new byte[] { 13, 10 });
        if (crlfIdx == -1) {
            throw new ServerException("db.http.protocol.UnknownResponseType", new Object[] { new String(bytes, 0, 7) });
        }
        final String line0 = new String(bytes, 0, crlfIdx, StandardCharsets.UTF_8);
        final String[] line0bits = line0.split(" ");
        if (line0bits.length != 3) {
            throw new ServerException("db.http.protocol.ProtocolError", new Object[] { "Line 0 of response does not have 3 parts but " + line0bits.length });
        }
        int idx = crlfIdx + 2;
        while (true) {
            crlfIdx = Bytes.indexOf(bytes, new byte[] { 13, 10 });
            if (crlfIdx == -1 || crlfIdx == 0) {
                return;
            }
            final String line2 = new String(bytes, idx, crlfIdx, StandardCharsets.UTF_8);
            idx = crlfIdx + 2;
            if (line2.isEmpty()) {
                continue;
            }
            final int colonIdx = line2.indexOf(58);
            if (colonIdx == -1) {
                throw new ServerException("db.http.protocol.ProtocolError", new Object[] { "Response has incomplete header - no colon" });
            }
            final String name = line2.substring(0, colonIdx);
            final String value = line2.substring(colonIdx + 2);
            this.headers.put(name, value);
        }
    }
    
    public String getResponseType() {
        return "Response";
    }
    
    public void writePacket(final RawPacketWriter writer) {
    }
}
