// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import org.graalvm.polyglot.Value;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.proxy.ProxyArray;

public class VariableArray implements ProxyArray
{
    private final List<Object> objects;
    
    public VariableArray() {
        this.objects = new ArrayList<Object>();
    }
    
    public void add(final Object obj) {
        this.objects.add(obj);
    }
    
    public void clear() {
        this.objects.clear();
    }
    
    public Object get(final long index) {
        return this.objects.get((int)index);
    }
    
    public void set(final long index, final Value value) {
        if (value == null || value.isNull()) {
            this.objects.set((int)index, null);
        }
        else {
            this.objects.set((int)index, value);
        }
    }
    
    public boolean remove(final long index) {
        return this.objects.remove(index);
    }
    
    public long getSize() {
        return this.objects.size();
    }
}
