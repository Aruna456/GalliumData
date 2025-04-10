// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

public interface GenericPacket
{
    boolean isModified();
    
    String getPacketType();
    
    void remove();
    
    boolean isRemoved();
}
