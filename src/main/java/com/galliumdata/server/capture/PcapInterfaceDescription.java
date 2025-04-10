// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

import java.net.InetSocketAddress;

public class PcapInterfaceDescription extends PcapBlock
{
    private InetSocketAddress sockAddr;
    
    public void setAddress(final InetSocketAddress sockAddr) {
        this.sockAddr = sockAddr;
        final String remoteName = sockAddr.getHostString();
        final int remotePort = sockAddr.getPort();
        final PcapOption option = new PcapOption(3, remoteName + ":" + remotePort);
        this.options.add(option);
    }
    
    public int getAddress() {
        final byte[] bytes = this.sockAddr.getAddress().getAddress();
        return (bytes[0] << 24) + (bytes[1] << 16) + (bytes[1] << 8) + bytes[3];
    }
    
    public int getPort() {
        return this.sockAddr.getPort();
    }
    
    @Override
    public byte[] serialize() {
        (this.writer = new ByteWriter()).writeBigEndianInt(1);
        this.writer.writeBigEndianInt(0);
        this.writer.writeBigEndianShort(1);
        this.writer.writeBigEndianShort(0);
        this.writer.writeBigEndianInt(0);
        this.writeOptions();
        this.writer.writeBigEndianInt(0);
        final int blockLen = this.writer.getBytes().length;
        (this.writer = new ByteWriter()).writeBigEndianInt(1);
        this.writer.writeBigEndianInt(blockLen);
        this.writer.writeBigEndianShort(101);
        this.writer.writeBigEndianShort(0);
        this.writer.writeBigEndianInt(0);
        this.writeOptions();
        this.writer.writeBigEndianInt(blockLen);
        return this.writer.getBytes();
    }
}
