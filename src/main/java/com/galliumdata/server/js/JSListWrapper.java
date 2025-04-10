// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.js;

import java.util.NoSuchElementException;
import org.graalvm.polyglot.proxy.ProxyIterator;
import org.graalvm.polyglot.Value;
import java.util.List;
import org.graalvm.polyglot.proxy.ProxyArray;

public class JSListWrapper<T> implements ProxyArray
{
    private List<T> coll;
    private Runnable cb;
    
    public JSListWrapper(final List<T> coll, final Runnable cb) {
        this.coll = coll;
        this.cb = cb;
    }
    
    public Object get(final long index) {
        return this.coll.get((int)index);
    }
    
    public void set(final long index, final Value value) {
        while (index >= this.coll.size()) {
            this.coll.add(null);
        }
        if (value.isNull()) {
            this.coll.remove((int)index);
        }
        else if (value.isHostObject()) {
            this.coll.set((int)index, (T)value.asHostObject());
        }
        else if (value.isString()) {
            this.coll.set((int)index, (T)value.asString());
        }
        this.cb.run();
    }
    
    public boolean remove(final long index) {
        final T removed = this.coll.remove((int)index);
        this.cb.run();
        return removed != null;
    }
    
    public long getSize() {
        return this.coll.size();
    }
    
    public Object getIterator() {
        return new JSListWrapperIterator();
    }
    
    public void filter(final Object run) {
    }
    
    private class JSListWrapperIterator implements ProxyIterator
    {
        private int idx;
        
        private JSListWrapperIterator() {
            this.idx = -1;
        }
        
        public boolean hasNext() {
            return JSListWrapper.this.coll.size() > 0 && this.idx < JSListWrapper.this.coll.size();
        }
        
        public Object getNext() throws NoSuchElementException, UnsupportedOperationException {
            ++this.idx;
            return JSListWrapper.this.coll.get(this.idx);
        }
    }
}
