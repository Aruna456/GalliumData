// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.capture;

import java.nio.charset.StandardCharsets;

public class PcapOption extends PcapBlock
{
    private final int type;
    private final String value;
    public static final int TYPE_COMMENT = 1;
    public static final int TYPE_OS = 2;
    public static final int TYPE_USERAPPL = 3;
    
    public PcapOption(final int type, final String value) {
        this.type = type;
        this.value = value;
    }
    
    @Override
    public byte[] serialize() {
        this.writer = new ByteWriter();
        final byte[] valueBytes = this.value.getBytes(StandardCharsets.UTF_8);
        final int valueLen = valueBytes.length;
        final int numPaddingBytes = (4 - valueLen % 4) % 4;
        this.writer.writeBigEndianShort(this.type);
        this.writer.writeBigEndianShort(valueLen);
        this.writer.writeBytes(valueBytes);
        if (numPaddingBytes > 0) {
            this.writer.writeBytes(new byte[numPaddingBytes]);
        }
        return this.writer.getBytes();
    }
}
