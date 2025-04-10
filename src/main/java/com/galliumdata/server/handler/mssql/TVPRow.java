// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class TVPRow
{
    private final TVPTypeInfo typeInfo;
    private final List<TVPRowValue> values;
    
    public TVPRow(final TVPTypeInfo typeInfo) {
        this.values = new ArrayList<TVPRowValue>();
        this.typeInfo = typeInfo;
    }
    
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final List<TVPTypeInfoColumnMetadata> metas = this.typeInfo.getColumnMetas();
        for (final TVPTypeInfoColumnMetadata meta : metas) {
            final TVPRowValue value = new TVPRowValue(meta.getTypeInfo());
            idx += value.readFromBytes(bytes, idx);
            this.values.add(value);
        }
        return idx - offset;
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeByte((byte)1);
        for (final TVPRowValue value : this.values) {
            value.write(writer);
        }
    }
    
    @Override
    public String toString() {
        String s = "TVPRow: ";
        for (int i = 0; i < this.typeInfo.getColumnMetas().size(); ++i) {
            final TVPTypeInfoColumnMetadata meta = this.typeInfo.getColumnMetas().get(i);
            final TVPRowValue value = this.values.get(i);
            String name = meta.getColumnName();
            if (name == null) {
                name = meta.getTypeInfo().getTypeName();
            }
            s = s + name + "=" + String.valueOf(value.value) + ", ";
        }
        return s;
    }
}
