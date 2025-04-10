// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class MemoryPacketSender implements PacketSender
{
    private final List<byte[]> packets;
    
    public MemoryPacketSender() {
        this.packets = new ArrayList<byte[]>();
    }
    
    @Override
    public void sendPacket(final byte[] bytes, final int offset, final int len) {
        if (offset == 0 && len == bytes.length) {
            this.packets.add(bytes);
            return;
        }
        final byte[] pkt = Arrays.copyOfRange(bytes, offset, len);
        this.packets.add(pkt);
    }
    
    public List<byte[]> getPackets() {
        return this.packets;
    }
}
