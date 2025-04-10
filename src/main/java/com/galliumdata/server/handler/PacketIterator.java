// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

import java.util.Iterator;

public class PacketIterator<T extends GenericPacket> implements Iterator<T>
{
    private int idx;
    private PacketGroup<T> packetGroup;
    
    protected PacketIterator(final PacketGroup<T> packetGroup) {
        this.packetGroup = packetGroup;
    }
    
    @Override
    public boolean hasNext() {
        return this.idx < this.packetGroup.getSize();
    }
    
    @Override
    public T next() {
        return this.packetGroup.get(this.idx++);
    }
}
