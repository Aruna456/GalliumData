// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;
import java.util.List;

public class RawPacketGroup
{
    private final List<RawPacket> packets;
    
    public RawPacketGroup() {
        this.packets = new Vector<RawPacket>();
    }
    
    public RawPacketGroup(final RawPacket pkt) {
        (this.packets = new Vector<RawPacket>()).add(pkt);
    }
    
    public RawPacketGroup(final RawPacketGroup other) {
        (this.packets = new Vector<RawPacket>()).addAll(other.packets);
    }
    
    public List<RawPacket> getPackets() {
        return new ArrayList<RawPacket>(this.packets);
    }
    
    public RawPacket getPacketAt(final int idx) {
        return this.packets.get(idx);
    }
    
    public int getSize() {
        return this.packets.size();
    }
    
    public void addRawPacket(final RawPacket pkt) {
        final RawPacket copy = new RawPacket(pkt);
        this.packets.add(copy);
    }
    
    public void addRawPacketNoCopy(final RawPacket pkt) {
        this.packets.add(pkt);
    }
    
    public void addRawPacketGroup(final RawPacketGroup grp) {
        for (int i = 0; i < grp.getSize(); ++i) {
            this.addRawPacket(grp.getPackets().get(i));
        }
    }
    
    public void addPacket(final MSSQLPacket pkt) {
        final RawPacketWriter writer = new RawPacketWriter(pkt.connectionState, pkt, null);
        pkt.write(writer);
        writer.finalizePacket();
        this.addRawPacket(writer.getPacket());
    }
    
    public void clear() {
        this.packets.clear();
    }
}
