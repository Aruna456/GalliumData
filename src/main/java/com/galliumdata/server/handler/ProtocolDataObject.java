// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

import org.graalvm.polyglot.Value;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import com.galliumdata.server.ServerException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProtocolDataObject extends ProtocolData
{
    private final Map<String, ProtocolData> contents;
    
    public ProtocolDataObject() {
        this.contents = new LinkedHashMap<String, ProtocolData>();
    }
    
    @Override
    public boolean isObject() {
        return true;
    }
    
    @Override
    public boolean isArray() {
        return false;
    }
    
    @Override
    public boolean isValue() {
        return false;
    }
    
    @Override
    public int getSize() {
        return this.contents.size();
    }
    
    @Override
    public ProtocolData get(final String name) {
        return this.contents.get(name);
    }
    
    public String getString(final String name) {
        final ProtocolData val = this.get(name);
        if (val == null) {
            return null;
        }
        if (!val.isValue()) {
            throw new ServerException("core.InternalError", new Object[] { "ProtocolData is not a value" });
        }
        return val.asString();
    }
    
    @Override
    public void put(final String name, final ProtocolData value) {
        this.contents.put(name, value);
        if (value != null) {
            value.parent = this;
        }
        this.setModified();
    }
    
    @Override
    public void putString(final String name, final String value) {
        if (value == null) {
            this.contents.put(name, null);
        }
        else {
            final ProtocolData data = new ProtocolDataValue(value);
            data.setParent(this);
            this.put(name, data);
        }
        this.setModified();
    }
    
    @Override
    public void putNumber(final String name, final Number value) {
        final ProtocolData data = new ProtocolDataValue(value);
        data.setParent(this);
        this.put(name, data);
        this.setModified();
    }
    
    @Override
    public void putBoolean(final String name, final Boolean b) {
        final ProtocolData data = new ProtocolDataValue(b);
        data.setParent(this);
        this.put(name, data);
        this.setModified();
    }
    
    @Override
    public ProtocolData remove(final String name) {
        final ProtocolData oldValue = this.contents.remove(name);
        this.setModified();
        return oldValue;
    }
    
    @Override
    public boolean hasProperty(final String name) {
        return this.contents.containsKey(name);
    }
    
    public Map<String, ProtocolData> getProperties() {
        return this.contents;
    }
    
    @Override
    public String toJSON() {
        String s = "{";
        boolean moreThanOne = false;
        for (Map.Entry<String, ProtocolData> entry : this.contents.entrySet()) {
            if (moreThanOne) {
                s = s;
            }
            s = s;
            s += (String)entry.getKey();
            s += "\":";
            final ProtocolData val = entry.getValue();
            if (null == val) {
                s += "null";
            }
            else {
                s += entry.getValue().toJSON();
            }
            moreThanOne = true;
        }
        s = s;
        return s;
    }
    
    @Override
    public String toPrettyJSON(final int padding) {
        String s = "{\n";
        boolean moreThanOne = false;
        for (Map.Entry<String, ProtocolData> entry : this.contents.entrySet()) {
            if (moreThanOne) {
                s += ",\n";
            }
            s += " ".repeat(padding + 4);
            s += (String)entry.getKey();
            s += "\": ";
            final ProtocolData val = entry.getValue();
            if (null == val) {
                s += "null";
            }
            else {
                s += entry.getValue().toPrettyJSON(padding + 4);
            }
            moreThanOne = true;
        }
        s = s + "\n" + " ".repeat(padding);
        return s;
    }
    
    @Override
    public String toJSONDebug(final int maxValueLen, final int maxMembers) {
        String s = "{";
        int memberNum = 0;
        for (Map.Entry<String, ProtocolData> entry : this.contents.entrySet()) {
            if (memberNum > 0) {
                s = s;
            }
            s = s;
            s += (String)entry.getKey();
            s += "\":";
            final ProtocolData val = entry.getValue();
            if (null == val) {
                s += "null";
            }
            else {
                s += entry.getValue().toJSONDebug(maxValueLen, maxMembers);
            }
            if (++memberNum > maxMembers) {
                final String etc = "(" + (this.contents.size() - maxMembers) + " more items)";
                s = s + ", \"_hidden_\": {\"type\":\"string\",\"asString\":\"" + etc + "\",\"value\":\"" + etc + "\"}";
                break;
            }
        }
        s = s;
        return s;
    }
    
    public void sort() {
        final Map<String, ProtocolData> old = new LinkedHashMap<String, ProtocolData>(this.contents);
        final Object[] keys = old.keySet().toArray();
        Arrays.sort(keys, new Comparator<Object>() {
            @Override
            public int compare(final Object key1, final Object key2) {
                final String key1str = key1.toString();
                final String key2str = key2.toString();
                if (key1str.chars().allMatch(c -> c >= 48 && c <= 57) && key2str.chars().allMatch(c -> c >= 48 && c <= 57)) {
                    final int key1int = Integer.parseInt(key1str);
                    final int key2int = Integer.parseInt(key2str);
                    return Integer.compare(key1int, key2int);
                }
                return key1str.compareTo(key2str);
            }
        });
        this.contents.clear();
        for (final Object key : keys) {
            final String keyStr = key.toString();
            if (keyStr.chars().allMatch(c -> c >= 48 && c <= 9)) {}
            this.contents.put(key.toString(), old.get(key));
        }
    }
    
    public Object getMember(final String key) {
        return this.contents.get(key);
    }
    
    public Object getMemberKeys() {
        return this.contents.keySet().toArray();
    }
    
    public boolean hasMember(final String key) {
        return this.contents.containsKey(key);
    }
    
    public void putMember(final String key, final Value value) {
        this.contents.put(key, (ProtocolData)value.asHostObject());
    }
    
    public boolean removeMember(final String key) {
        return this.contents.remove(key) != null;
    }
}
