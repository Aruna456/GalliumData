// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

public class ConnectionState
{
    private int protocolVersion;
    private String serverName;
    private String currentURL;
    
    public ConnectionState() {
        this.protocolVersion = 1;
    }
    
    public int getProtocolVersion() {
        return this.protocolVersion;
    }
    
    public void setProtocolVersion(final int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public void setServerName(final String name) {
        this.serverName = name;
    }
    
    public String getServerName() {
        return this.serverName;
    }
    
    public String getCurrentURL() {
        return this.currentURL;
    }
    
    public void setCurrentURL(final String currentURL) {
        this.currentURL = currentURL;
    }
}
