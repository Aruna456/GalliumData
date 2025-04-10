// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

import java.util.ListIterator;
import java.util.List;
import java.util.AbstractSequentialList;

public class ListWithCallback<T> extends AbstractSequentialList<T>
{
    private final List<T> myList;
    private final Runnable cb;
    
    public ListWithCallback(final List<T> list, final Runnable cb) {
        this.myList = list;
        this.cb = cb;
    }
    
    @Override
    public ListIterator<T> listIterator(final int index) {
        return new ListIterator<T>() {
            private int pos = -1;
            
            @Override
            public boolean hasNext() {
                return ListWithCallback.this.myList != null && this.pos < ListWithCallback.this.myList.size() - 1;
            }
            
            @Override
            public T next() {
                return ListWithCallback.this.myList.get(++this.pos);
            }
            
            @Override
            public boolean hasPrevious() {
                return this.pos > 0;
            }
            
            @Override
            public T previous() {
                return ListWithCallback.this.myList.get(this.pos - 1);
            }
            
            @Override
            public int nextIndex() {
                return this.pos + 1;
            }
            
            @Override
            public int previousIndex() {
                return this.pos - 1;
            }
            
            @Override
            public void remove() {
                ListWithCallback.this.myList.remove(this.pos);
                if (ListWithCallback.this.cb != null) {
                    ListWithCallback.this.cb.run();
                }
            }
            
            @Override
            public void set(final T o) {
                ListWithCallback.this.myList.set(this.pos, o);
                if (ListWithCallback.this.cb != null) {
                    ListWithCallback.this.cb.run();
                }
            }
            
            @Override
            public void add(final T o) {
                ListWithCallback.this.myList.add(o);
                if (ListWithCallback.this.cb != null) {
                    ListWithCallback.this.cb.run();
                }
            }
        };
    }
    
    @Override
    public int size() {
        if (this.myList == null) {
            return 0;
        }
        return this.myList.size();
    }
    
    @Override
    public T get(final int idx) {
        return this.myList.get(idx);
    }
    
    @Override
    public T set(final int idx, final T obj) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myList.set(idx, obj);
    }
    
    @Override
    public T remove(final int idx) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myList.remove(idx);
    }
    
    @Override
    public void add(final int idx, final T obj) {
        if (this.cb != null) {
            this.cb.run();
        }
        this.myList.add(idx, obj);
    }
    
    public void push(final T obj) {
        if (this.cb != null) {
            this.cb.run();
        }
        this.myList.add(obj);
    }
    
    @Override
    public int indexOf(final Object member) {
        return this.myList.indexOf(member);
    }
}
