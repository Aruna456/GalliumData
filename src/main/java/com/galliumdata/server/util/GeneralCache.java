// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.util;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class GeneralCache
{
    private final Map<String, Entry> map;
    private final int maxSize;
    private final long maxIdleTime;
    private long lastMaintenanceTime;
    private static final long MAINTENANCE_INTERVAL = 5000L;
    
    public GeneralCache(final int maxSize, final long maxIdleTimeInSecs) {
        this.map = new ConcurrentHashMap<String, Entry>();
        this.lastMaintenanceTime = System.currentTimeMillis();
        this.maxSize = maxSize;
        this.maxIdleTime = maxIdleTimeInSecs * 1000L;
    }
    
    public int getSize() {
        this.maintainCache();
        return this.map.size();
    }
    
    public boolean isEmpty() {
        this.maintainCache();
        return this.map.isEmpty();
    }
    
    public boolean containsKey(final String key) {
        return this.map.containsKey(key);
    }
    
    public Collection<String> getKeys() {
        return this.map.keySet();
    }
    
    public void put(final String key, final Object value) {
        this.maintainCache();
        if (this.map.size() >= this.maxSize) {
            this.shrinkCache(this.maxSize / 10);
        }
        final Entry entry = new Entry();
        entry.key = key;
        entry.value = value;
        this.map.put(key, entry);
    }
    
    public Object get(final String key) {
        this.maintainCache();
        final Entry entry = this.map.get(key);
        if (entry == null) {
            return null;
        }
        entry.lastAccessTime = System.currentTimeMillis();
        return entry.value;
    }
    
    public Object remove(final String key) {
        this.maintainCache();
        final Entry entry = this.map.remove(key);
        if (entry == null) {
            return null;
        }
        return entry.value;
    }
    
    private synchronized void maintainCache() {
        if (System.currentTimeMillis() - this.lastMaintenanceTime < 5000L) {
            return;
        }
        final long minAccessTime = System.currentTimeMillis() - this.maxIdleTime;
        final Set<String> toRemove = new HashSet<String>();
        for (final Map.Entry<String, Entry> entry : this.map.entrySet()) {
            final Entry ent = entry.getValue();
            if (ent.lastAccessTime < minAccessTime) {
                toRemove.add(entry.getKey());
            }
        }
        for (final String rem : toRemove) {
            this.map.remove(rem);
        }
        this.lastMaintenanceTime = System.currentTimeMillis();
    }
    
    private synchronized void shrinkCache(final int numToShrink) {
        final List<Entry> entriesByTime = new ArrayList<Entry>(this.map.values());
        entriesByTime.sort(Comparator.comparingLong(e -> e.lastAccessTime));
        for (int i = 0; i < numToShrink; ++i) {
            this.map.remove(entriesByTime.get(i).key);
        }
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, Entry> entry : this.map.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue().value.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
    
    private static class Entry
    {
        private long lastAccessTime;
        private String key;
        private Object value;
        
        private Entry() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
