// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.List;

public class PacketUtil
{
    public static byte[] joinPackets(final List<RawPacket> pkts) {
        int fullSize = 0;
        for (int i = 0; i < pkts.size(); ++i) {
            final RawPacket p = pkts.get(i);
            if (fullSize == 0) {
                fullSize += p.getBuffer().length;
            }
            else {
                fullSize += p.getBuffer().length - 8;
            }
        }
        final byte[] fullBytes = new byte[fullSize];
        int idx = 0;
        for (int j = 0; j < pkts.size(); ++j) {
            final byte[] bytes = pkts.get(j).getBuffer();
            if (idx == 0) {
                System.arraycopy(bytes, 0, fullBytes, 0, bytes.length);
                idx += bytes.length;
            }
            else {
                System.arraycopy(bytes, 8, fullBytes, idx, bytes.length - 8);
                idx += bytes.length - 8;
            }
        }
        DataTypeWriter.encodeTwoByteInteger(fullBytes, 2, (short)fullSize);
        return fullBytes;
    }
}
