// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public abstract class PcapBlock
{
    protected ByteWriter writer;
    protected List<PcapOption> options;
    
    public PcapBlock() {
        this.writer = new ByteWriter();
        this.options = new ArrayList<PcapOption>();
    }
    
    protected void addOption(final int type, final String value) {
        final PcapOption option = new PcapOption(type, value);
        this.options.add(option);
    }
    
    protected void writeOptions() {
        for (final PcapOption option : this.options) {
            final byte[] bytes = option.serialize();
            this.writer.writeBytes(bytes);
        }
        this.writer.writeBigEndianInt(0);
    }
    
    public byte[] serialize() {
        return this.writer.getBytes();
    }
}
