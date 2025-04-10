// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import org.graalvm.polyglot.Value;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.polyglot.proxy.ProxyObject;

public class Variables implements ProxyObject
{
    private final Map<String, Object> values;
    
    public Variables() {
        this.values = new HashMap<String, Object>(30);
    }
    
    public Variables(final Map<String, Object> model) {
        this.values = new HashMap<String, Object>(model);
    }
    
    public Logger getLog(final String logName) {
        return (Logger) this.values.get(logName);
    }
    
    public int size() {
        return this.values.size();
    }
    
    public boolean isEmpty() {
        return this.values.isEmpty();
    }
    
    public boolean containsKey(final Object key) {
        return this.values.containsKey(key);
    }
    
    public Object get(final Object key) {
        return this.values.get(key);
    }
    
    public Object put(final String name, final Object value) {
        if (value == null) {
            return this.values.remove(name);
        }
        return this.values.put(name, value);
    }
    
    public void putAll(final Variables vars) {
        for (final String key : vars.keySet()) {
            this.values.put(key, vars.get(key));
        }
    }
    
    public Object remove(final Object key) {
        return this.values.remove(key);
    }
    
    public void clear() {
        this.values.clear();
    }
    
    public Set<String> keySet() {
        return this.values.keySet();
    }
    
    public Collection<Object> values() {
        return this.values.values();
    }
    
    public void add(final Object value) {
        for (int i = 0; i < 1000000000; ++i) {
            final String key = "" + i;
            if (!this.values.containsKey(key)) {
                this.values.put(key, value);
                return;
            }
        }
        throw new RuntimeException("Array is full");
    }
    
    @Override
    public String toString() {
        String s = "";
        for (Map.Entry<String, Object> entry : this.values.entrySet()) {
            if (s.length() > 0) {
                s += ", ";
            }
            s += (String)entry.getKey();
            s = s;
            final Object value = entry.getValue();
            if (value == null) {
                s += "<null>";
            }
            else {
                String valueStr = value.toString();
                if (valueStr.length() > 50) {
                    valueStr = valueStr.substring(0, 50) + "...";
                }
                s += valueStr.replaceAll("\\s", " ");
            }
        }
        return s;
    }
    
    public Object getMember(final String key) {
        return this.values.get(key);
    }
    
    public Object getMemberKeys() {
        return this.values.keySet().toArray();
    }
    
    public boolean hasMember(final String key) {
        return this.values.containsKey(key);
    }
    
    public void putMember(final String key, final Value value) {
        this.values.put(key, value);
    }
    
    public boolean removeMember(final String key) {
        if (!this.values.containsKey(key)) {
            return false;
        }
        this.values.remove(key);
        return true;
    }
}
