// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class TVPColumnOrderingToken
{
    private List<Integer> colNums;
    
    public TVPColumnOrderingToken() {
        this.colNums = new ArrayList<Integer>();
    }
    
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int totalCount = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
        idx += 2;
        for (int i = 0; i < totalCount; ++i) {
            final int colNum = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
            idx += 2;
            this.colNums.add(colNum);
        }
        return idx - offset;
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeTwoByteIntegerLow(this.colNums.size());
        for (final Integer colNum : this.colNums) {
            writer.writeTwoByteIntegerLow(colNum);
        }
    }
}
