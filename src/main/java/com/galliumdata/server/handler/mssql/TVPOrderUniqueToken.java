// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class TVPOrderUniqueToken
{
    private List<OrderUnique> orderUniques;
    
    public TVPOrderUniqueToken() {
        this.orderUniques = new ArrayList<OrderUnique>();
    }
    
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int totalCount = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
        idx += 2;
        for (int i = 0; i < totalCount; ++i) {
            final OrderUnique orderUnique = new OrderUnique();
            orderUnique.colNum = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
            idx += 2;
            final byte flags = bytes[idx];
            ++idx;
            orderUnique.fOrderAsc = ((flags & 0x1) != 0x0);
            orderUnique.fOrderDesc = ((flags & 0x2) != 0x0);
            orderUnique.fUnique = ((flags & 0x4) != 0x0);
            this.orderUniques.add(orderUnique);
        }
        return idx - offset;
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeTwoByteIntegerLow(this.orderUniques.size());
        for (final OrderUnique ou : this.orderUniques) {
            writer.writeTwoByteIntegerLow(ou.colNum);
            byte flags = 0;
            if (ou.fOrderAsc) {
                flags |= 0x1;
            }
            if (ou.fOrderDesc) {
                flags |= 0x2;
            }
            if (ou.fUnique) {
                flags |= 0x4;
            }
            writer.writeByte(flags);
        }
    }
    
    public static class OrderUnique
    {
        public int colNum;
        public boolean fOrderAsc;
        public boolean fOrderDesc;
        public boolean fUnique;
    }
}
