// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import com.galliumdata.server.ServerException;
import java.io.ByteArrayOutputStream;

public class RawPacketWriter
{
    protected final PacketSender sender;
    protected final ConnectionState connectionState;
    protected ByteArrayOutputStream out;
    private static final byte[] crlfBytes;
    
    public RawPacketWriter(final PacketSender sender, final ConnectionState connectionState) {
        this.out = new ByteArrayOutputStream();
        this.sender = sender;
        this.connectionState = connectionState;
    }
    
    public void send() {
        final byte[] bytes = this.out.toByteArray();
        final int bytesIdx = 0;
        final int numInPacket = bytes.length;
        final byte[] packetBytes = new byte[numInPacket];
        System.arraycopy(bytes, bytesIdx, packetBytes, 0, numInPacket);
        this.sender.sendPacket(packetBytes, 0, packetBytes.length);
    }
    
    public int getNumBytesWritten() {
        return this.out.size();
    }
    
    public void writeByte(final int b) {
        this.out.write(b);
    }
    
    public void writeBytes(final byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        try {
            this.out.write(bytes);
        }
        catch (final Exception ex) {
            throw new ServerException("db.oracle.protocol.WritingError", new Object[] { ex });
        }
    }
    
    public void writeBytes(final byte[] bytes, final int offset, final int len) {
        try {
            this.out.write(bytes, offset, len);
        }
        catch (final Exception ex) {
            throw new ServerException("db.oracle.protocol.WritingError", new Object[] { ex });
        }
    }
    
    public void writeCRLF() {
        this.writeBytes(RawPacketWriter.crlfBytes);
    }
    
    static {
        crlfBytes = new byte[] { 13, 10 };
    }
}
