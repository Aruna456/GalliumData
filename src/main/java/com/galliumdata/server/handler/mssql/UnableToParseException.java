// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

public class UnableToParseException extends RuntimeException
{
    private final String packetType;
    
    public UnableToParseException(final String packetType, final String message) {
        super(message);
        this.packetType = packetType;
    }
    
    public String getPacketType() {
        return this.packetType;
    }
}
