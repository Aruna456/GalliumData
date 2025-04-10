// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.js;

import java.util.NoSuchElementException;
import org.graalvm.polyglot.proxy.ProxyIterator;
import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;

public class JSByteArray implements ProxyArray
{
    private final byte[] array;
    
    public JSByteArray(final byte[] arr) {
        this.array = arr;
    }
    
    public Object get(final long index) {
        return this.array[(int)index];
    }
    
    public void set(final long index, final Value value) {
        if (index < 0L || index >= this.array.length) {
            throw new ServerException("logic.ArrayIndexInvalid", new Object[] { index, this.array.length });
        }
        this.array[(int)index] = value.asByte();
    }
    
    public boolean remove(final long index) {
        if (index < 0L || index >= this.array.length) {
            throw new ServerException("logic.ArrayIndexInvalid", new Object[] { index, this.array.length });
        }
        this.array[(int)index] = 0;
        return true;
    }
    
    public long getSize() {
        return this.array.length;
    }
    
    public Object getIterator() {
        return new ProxyIterator() {
            private int idx = 0;
            
            public boolean hasNext() {
                return this.idx < JSByteArray.this.array.length;
            }
            
            public Object getNext() throws NoSuchElementException, UnsupportedOperationException {
                return JSByteArray.this.array[this.idx++];
            }
        };
    }
}
