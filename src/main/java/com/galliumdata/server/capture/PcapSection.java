// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class PcapSection extends PcapBlock
{
    private final PcapSectionHeader header;
    private final List<PcapInterfaceDescription> interfaces;
    private final List<PcapBlock> blocks;
    
    public PcapSection() {
        this.header = new PcapSectionHeader();
        this.interfaces = new ArrayList<PcapInterfaceDescription>();
        this.blocks = new ArrayList<PcapBlock>();
    }
    
    public PcapInterfaceDescription addInterfaceDescription() {
        final PcapInterfaceDescription iDesc = new PcapInterfaceDescription();
        this.blocks.add(iDesc);
        this.interfaces.add(iDesc);
        return iDesc;
    }
    
    public PcapInterfaceDescription getInterfaceDescription(final int id) {
        return this.interfaces.get(id);
    }
    
    public void addHeaderOption(final int type, final String value) {
        this.header.addOption(type, value);
    }
    
    public PcapPacket addPacket(final PcapFile pcapFile) {
        final PcapPacket pkt = new PcapPacket(pcapFile);
        this.blocks.add(pkt);
        return pkt;
    }
    
    @Override
    public byte[] serialize() {
        this.writer = new ByteWriter();
        for (final PcapBlock block : this.blocks) {
            this.writer.writeBytes(block.serialize());
        }
        this.writeOptions();
        this.header.setSectionLength(this.writer.getBytes().length);
        (this.writer = new ByteWriter()).writeBytes(this.header.serialize());
        for (final PcapBlock block : this.blocks) {
            this.writer.writeBytes(block.serialize());
        }
        return this.writer.getBytes();
    }
}
