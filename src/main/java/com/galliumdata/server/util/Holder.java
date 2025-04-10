// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

public class Holder<T>
{
    private T value;
    
    public Holder(final T value) {
        this.value = value;
    }
    
    public T getValue() {
        return this.value;
    }
    
    public void setValue(final T newValue) {
        this.value = newValue;
    }
}
