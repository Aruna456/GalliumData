// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class MapWithCallback<K, V> extends HashMap<K, V>
{
    private final Map<K, V> myMap;
    private final Runnable cb;
    
    public MapWithCallback(final Map<K, V> map, final Runnable cb) {
        this.myMap = map;
        this.cb = cb;
    }
    
    @Override
    public int size() {
        return this.myMap.size();
    }
    
    @Override
    public boolean isEmpty() {
        return this.myMap.isEmpty();
    }
    
    @Override
    public V get(final Object key) {
        return this.myMap.get(key);
    }
    
    @Override
    public boolean containsKey(final Object key) {
        return this.myMap.containsKey(key);
    }
    
    @Override
    public V put(final K key, final V value) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.put(key, value);
    }
    
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        if (this.cb != null) {
            this.cb.run();
        }
        this.myMap.putAll(m);
    }
    
    @Override
    public V remove(final Object key) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.remove(key);
    }
    
    @Override
    public void clear() {
        if (this.cb != null) {
            this.cb.run();
        }
        this.myMap.clear();
    }
    
    @Override
    public boolean containsValue(final Object value) {
        return this.myMap.containsValue(value);
    }
    
    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet((Set<? extends K>)this.myMap.keySet());
    }
    
    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection((Collection<? extends V>)this.myMap.values());
    }
    
    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet((Set<? extends Entry<K, V>>)this.myMap.entrySet());
    }
    
    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        return this.myMap.getOrDefault(key, defaultValue);
    }
    
    @Override
    public V putIfAbsent(final K key, final V value) {
        final V result = this.myMap.putIfAbsent(key, value);
        if (result != null && this.cb != null) {
            this.cb.run();
        }
        return result;
    }
    
    @Override
    public boolean remove(final Object key, final Object value) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.remove(key, value);
    }
    
    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.replace(key, oldValue, newValue);
    }
    
    @Override
    public V replace(final K key, final V value) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.replace(key, value);
    }
}
