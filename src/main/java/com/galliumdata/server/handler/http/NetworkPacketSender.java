// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import com.galliumdata.server.ServerException;
import java.io.OutputStream;

public class NetworkPacketSender implements PacketSender
{
    private final OutputStream sockOut;
    
    public NetworkPacketSender(final OutputStream sockOut) {
        this.sockOut = sockOut;
    }
    
    @Override
    public void sendPacket(final byte[] bytes, final int offset, final int len) {
        try {
            this.sockOut.write(bytes, offset, len);
        }
        catch (final Exception ex) {
            throw new ServerException("db.oracle.protocol.PacketWriteError", new Object[] { ex });
        }
    }
}
