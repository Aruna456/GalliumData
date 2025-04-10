// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

public class PcapSectionHeader extends PcapBlock
{
    private long sectionLength;
    
    public void setSectionLength(final long l) {
        this.sectionLength = l;
    }
    
    @Override
    public byte[] serialize() {
        (this.writer = new ByteWriter()).writeBigEndianInt(168627466);
        this.writer.writeBigEndianInt(0);
        this.writer.writeBigEndianInt(439041101);
        this.writer.writeBigEndianShort(1);
        this.writer.writeBigEndianShort(0);
        this.writer.writeLong(this.sectionLength);
        this.writeOptions();
        this.writer.writeBigEndianInt(0);
        final int length = this.writer.getBytes().length;
        (this.writer = new ByteWriter()).writeBigEndianInt(168627466);
        this.writer.writeBigEndianInt(length);
        this.writer.writeBigEndianInt(439041101);
        this.writer.writeBigEndianShort(1);
        this.writer.writeBigEndianShort(0);
        this.writer.writeLong(this.sectionLength);
        this.writeOptions();
        this.writer.writeBigEndianInt(length);
        return this.writer.getBytes();
    }
}
