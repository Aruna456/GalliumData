// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

public interface PacketReader
{
    RawPacket readNextPacket();
    
    RawPacket readNextPacketPlain();
    
    void close();
}
