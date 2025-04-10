// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

public class PcapPacket extends PcapBlock
{
    private final PcapFile pcapFile;
    private int localInterfaceId;
    private int remoteInterfaceId;
    private final long ts;
    private byte[] bytes;
    
    public PcapPacket(final PcapFile pcapFile) {
        this.ts = System.nanoTime() / 1000L;
        this.pcapFile = pcapFile;
    }
    
    public void setInterfaceIds(final int localId, final int remoteId) {
        this.localInterfaceId = localId;
        this.remoteInterfaceId = remoteId;
    }
    
    public void setData(final byte[] bytes) {
        this.bytes = bytes;
    }
    
    public void addComment(final String s) {
        this.addOption(1, s);
    }
    
    @Override
    public byte[] serialize() {
        final int valueLen = this.bytes.length;
        final int numPaddingBytes = (4 - valueLen % 4) % 4;
        final byte[] ipHeader = this.getIpHeader(this.bytes.length);
        (this.writer = new ByteWriter()).writeBigEndianInt(6);
        this.writer.writeBigEndianInt(0);
        this.writer.writeBigEndianInt(this.localInterfaceId);
        this.writer.writeBigEndianLong(this.ts);
        this.writer.writeBigEndianInt(ipHeader.length + this.bytes.length);
        this.writer.writeBigEndianInt(ipHeader.length + this.bytes.length);
        this.writer.writeBytes(ipHeader);
        this.writer.writeBytes(this.bytes);
        if (numPaddingBytes > 0) {
            this.writer.writeBytes(new byte[numPaddingBytes]);
        }
        this.writeOptions();
        this.writer.writeBigEndianInt(0);
        final int blockLen = this.writer.getBytes().length;
        (this.writer = new ByteWriter()).writeBigEndianInt(6);
        this.writer.writeBigEndianInt(blockLen);
        this.writer.writeBigEndianInt(this.localInterfaceId);
        this.writer.writeBigEndianLong(System.nanoTime() / 1000L);
        this.writer.writeBigEndianInt(ipHeader.length + this.bytes.length);
        this.writer.writeBigEndianInt(ipHeader.length + this.bytes.length);
        this.writer.writeBytes(ipHeader);
        this.writer.writeBytes(this.bytes);
        if (numPaddingBytes > 0) {
            this.writer.writeBytes(new byte[numPaddingBytes]);
        }
        this.writeOptions();
        this.writer.writeBigEndianInt(blockLen);
        return this.writer.getBytes();
    }
    
    private byte[] getIpHeader(final int payloadLength) {
        final PcapInterfaceDescription localInterface = this.pcapFile.getInterface(this.localInterfaceId);
        final PcapInterfaceDescription remoteInterface = this.pcapFile.getInterface(this.remoteInterfaceId);
        final ByteWriter out = new ByteWriter();
        out.writeByte(69);
        out.writeByte(0);
        out.writeBigEndianShort(payloadLength + 52);
        out.writeBigEndianShort(0);
        out.writeByte(64);
        out.writeByte(0);
        out.writeByte(64);
        out.writeByte(6);
        out.writeBigEndianShort(0);
        out.writeBigEndianInt(localInterface.getAddress());
        out.writeBigEndianInt(remoteInterface.getAddress());
        out.writeBigEndianShort(localInterface.getPort());
        out.writeBigEndianShort(remoteInterface.getPort());
        out.writeBigEndianInt(this.pcapFile.getNextSequenceId(this.localInterfaceId, payloadLength));
        out.writeBigEndianInt(0);
        out.writeByte(128);
        out.writeByte(24);
        out.writeBigEndianShort(6333);
        out.writeBigEndianShort(0);
        out.writeBigEndianShort(0);
        out.writeBigEndianShort(257);
        out.writeByte(8);
        out.writeByte(10);
        out.writeInt((int)System.currentTimeMillis());
        out.writeInt((int)System.currentTimeMillis());
        return out.getBytes();
    }
}
