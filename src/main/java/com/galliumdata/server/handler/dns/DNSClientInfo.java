// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import java.net.InetAddress;

public class DNSClientInfo
{
    public InetAddress address;
    public int port;
    
    public DNSClientInfo(final InetAddress address, final int port) {
        this.address = address;
        this.port = port;
    }
}
