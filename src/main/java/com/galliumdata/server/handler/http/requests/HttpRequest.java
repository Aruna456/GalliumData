// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.requests;

import com.galliumdata.server.handler.http.RawPacketWriter;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.http.RawPacket;

public abstract class HttpRequest
{
    protected String url;
    protected String protocol;
    
    public static HttpRequest createHttpRequest(final RawPacket pkt) {
        if (pkt.getBuffer().length > 4 && "GET ".equals(new String(pkt.getBuffer(), 0, 4))) {
            return new GetRequest();
        }
        throw new ServerException("db.http.protocol.UnknownRequestType", new Object[] { new String(pkt.getBuffer(), 0, 7) });
    }
    
    public abstract String getRequestType();
    
    public abstract void readPacket(final RawPacket p0);
    
    public abstract void writePacket(final RawPacketWriter p0);
    
    public String getUrl() {
        return this.url;
    }
    
    public void setUrl(final String url) {
        this.url = url;
    }
    
    public String getProtocol() {
        return this.protocol;
    }
    
    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }
}
