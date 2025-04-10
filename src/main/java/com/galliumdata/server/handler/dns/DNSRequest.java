// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import com.galliumdata.server.adapters.Variables;
import java.net.InetAddress;

public class DNSRequest
{
    protected long ts;
    protected short transactionId;
    protected InetAddress clientAddress;
    protected int clientPort;
    protected short proxyTransactionId;
    private final Variables requestContext;
    
    public DNSRequest() {
        this.ts = System.currentTimeMillis();
        this.requestContext = new Variables();
    }
    
    public Variables getRequestContext() {
        return this.requestContext;
    }
}
