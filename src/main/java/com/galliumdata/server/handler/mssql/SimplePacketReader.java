// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.net.Socket;

public class SimplePacketReader extends PacketReader
{
    private RawPacket rawPkt;
    
    public SimplePacketReader(final ConnectionState connState, final RawPacket pkt) {
        super(null, null, null);
        this.rawPkt = pkt;
    }
    
    @Override
    public RawPacket readNextPacket() {
        if (this.rawPkt == null) {
            return null;
        }
        final RawPacket pkt = this.rawPkt;
        this.rawPkt = null;
        return pkt;
    }
    
    @Override
    public void close() {
        this.rawPkt = null;
    }
}
