// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

import java.util.Arrays;
import org.graalvm.polyglot.Value;
import java.util.Iterator;
import com.galliumdata.server.logic.LogicException;
import com.galliumdata.server.util.StringUtil;
import java.util.Vector;
import java.util.List;

public class ProtocolDataArray extends ProtocolData
{
    private final List<ProtocolData> values;
    private byte[] bytes;
    private long[] longs;
    private boolean[] nulls;
    
    public ProtocolDataArray() {
        this.values = new Vector<ProtocolData>();
    }
    
    public ProtocolDataArray(final byte[] bytes) {
        this.values = new Vector<ProtocolData>();
        this.bytes = bytes;
        this.nulls = new boolean[bytes.length];
    }
    
    public ProtocolDataArray(final long[] longs) {
        this.values = new Vector<ProtocolData>();
        this.longs = longs;
        this.nulls = new boolean[longs.length];
    }
    
    @Override
    public boolean isObject() {
        return false;
    }
    
    @Override
    public boolean isArray() {
        return true;
    }
    
    @Override
    public boolean isValue() {
        return false;
    }
    
    @Override
    public int getSize() {
        return this.values.size();
    }
    
    @Override
    public ProtocolData get(final String name) {
        throw new ProtocolException("", new Object[0]);
    }
    
    public ProtocolData get(final Number n) {
        final int i = n.intValue();
        if (i < 0) {
            throw new ProtocolException("", new Object[0]);
        }
        if (i >= this.values.size()) {
            throw new ProtocolException("", new Object[0]);
        }
        return this.values.get(i);
    }
    
    @Override
    public void put(final String name, final ProtocolData value) {
        throw new ProtocolException("", new Object[0]);
    }
    
    public void put(final Number n, final ProtocolDataArray value) {
        this.values.add(n.intValue(), value);
        value.setParent(this);
        this.setModified();
    }
    
    public void add(final ProtocolData value) {
        this.values.add(value);
        if (value != null) {
            value.setParent(this);
        }
        this.setModified();
    }
    
    @Override
    public boolean hasProperty(final String name) {
        throw new ProtocolException("", new Object[0]);
    }
    
    @Override
    public ProtocolData remove(final String name) {
        throw new ProtocolException("", new Object[0]);
    }
    
    public ProtocolData remove(final Number n) {
        final ProtocolData oldValue = this.values.remove(n.intValue());
        this.setModified();
        return oldValue;
    }
    
    @Override
    public String toJSON() {
        String s = "[";
        if (this.bytes != null) {
            for (int i = 0; i < this.bytes.length; ++i) {
                if (i > 0) {
                    s = s;
                }
                s += HexUtil.getIntForByte(this.bytes[i]);
            }
        }
        else if (this.longs != null) {
            for (int i = 0; i < this.longs.length; ++i) {
                if (i > 0) {
                    s = s;
                }
                s += this.longs[i];
            }
        }
        else {
            for (int i = 0; i < this.values.size(); ++i) {
                if (i > 0) {
                    s = s;
                }
                final ProtocolData d = this.values.get(i);
                if (d == null) {
                    s += "null";
                }
                else {
                    s += d.toJSON();
                }
            }
        }
        s = s;
        return s;
    }
    
    @Override
    public String toPrettyJSON(final int padding) {
        String s = "[";
        if (this.bytes != null) {
            for (int i = 0; i < this.bytes.length; ++i) {
                if (i > 0) {
                    s = s;
                }
                s += HexUtil.getIntForByte(this.bytes[i]);
            }
            s = s;
        }
        else if (this.longs != null) {
            for (int i = 0; i < this.longs.length; ++i) {
                if (i > 0) {
                    s = s;
                }
                s += this.longs[i];
            }
            s = s;
        }
        else {
            s = "\n" + " ".repeat(padding) + "[\n";
            for (int i = 0; i < this.values.size(); ++i) {
                if (i > 0) {
                    s += ",\n";
                }
                s = s + " ".repeat(padding + 4) + this.values.get(i).toPrettyJSON(padding + 4);
            }
            s = s + "\n" + " ".repeat(padding);
        }
        return s;
    }
    
    @Override
    public String toJSONDebug(final int maxValueLen, final int maxMembers) {
        String s = "[";
        if (this.bytes != null) {
            for (int i = 0; i < this.bytes.length; ++i) {
                if (i > 0) {
                    s = s;
                }
                s += HexUtil.getIntForByte(this.bytes[i]);
                if (i >= maxMembers) {
                    final String etc = "(" + (this.bytes.length - maxMembers) + " more items)";
                    s = s + ", {\"type\":\"string\",\"asString\":\"" + etc + "\",\"value\":\"" + etc + "\"}";
                    break;
                }
            }
        }
        else if (this.longs != null) {
            for (int i = 0; i < this.longs.length; ++i) {
                if (i > 0) {
                    s = s;
                }
                s += this.longs[i];
                if (i >= maxMembers) {
                    final String etc = "(" + (this.longs.length - maxMembers) + " more items)";
                    s = s + ", {\"type\":\"string\",\"asString\":\"" + etc + "\",\"value\":\"" + etc + "\"}";
                    break;
                }
            }
        }
        else {
            for (int i = 0; i < this.values.size(); ++i) {
                if (i > 0) {
                    s = s;
                }
                final ProtocolData d = this.values.get(i);
                if (d == null) {
                    s += "null";
                }
                else {
                    s += d.toJSONDebug(maxValueLen, maxMembers);
                }
                if (i >= maxMembers) {
                    final String etc2 = "(" + (this.values.size() - maxMembers) + " more items)";
                    s = s + ", {\"type\":\"string\",\"asString\":\"" + etc2 + "\",\"value\":\"" + etc2 + "\"}";
                    break;
                }
            }
        }
        s = s;
        return s;
    }
    
    public byte[] getByteArray() {
        return this.bytes;
    }
    
    public long[] getLongArray() {
        return this.longs;
    }
    
    public List<ProtocolData> getValues() {
        return this.values;
    }
    
    @Override
    public ProtocolData get(final int n) {
        if (this.bytes != null) {
            if (this.nulls[n]) {
                return null;
            }
            return new ProtocolDataValue(this.bytes[n]);
        }
        else {
            if (this.longs == null) {
                return this.values.get(n);
            }
            if (this.nulls[n]) {
                return null;
            }
            return new ProtocolDataValue(this.longs[n]);
        }
    }
    
    public Object getMember(final String key) {
        if (!StringUtil.stringIsInteger(key)) {
            throw new LogicException("Invalid index (not a number) for JavaScript array: " + key);
        }
        final int idx = Integer.parseInt(key);
        if (this.values != null) {
            if (idx >= this.values.size()) {
                throw new LogicException("Index " + idx + " is larger than the number of objects in the array (" + this.values.size());
            }
            return this.values.get(idx);
        }
        else if (this.bytes != null) {
            if (idx >= this.bytes.length) {
                throw new LogicException("Index " + idx + " is larger than the number of objects in the array (" + this.bytes.length);
            }
            return this.bytes[idx];
        }
        else {
            if (this.longs == null) {
                throw new LogicException("Array is empty");
            }
            if (idx >= this.longs.length) {
                throw new LogicException("Index " + idx + " is larger than the number of objects in the array (" + this.longs.length);
            }
            return this.longs[idx];
        }
    }
    
    public Object getMemberKeys() {
        if (this.bytes != null) {
            final int[] keys = new int[this.bytes.length];
            for (int i = 0; i < this.bytes.length; ++i) {
                keys[i] = i;
            }
            return keys;
        }
        if (this.longs != null) {
            final int[] keys = new int[this.longs.length];
            for (int i = 0; i < this.longs.length; ++i) {
                keys[i] = i;
            }
            return keys;
        }
        int numKeys = 0;
        for (final ProtocolData value : this.values) {
            if (value != null) {
                ++numKeys;
            }
        }
        final int[] keys2 = new int[numKeys];
        int keyIdx = 0;
        for (int j = 0; j < this.values.size(); ++j) {
            if (this.values.get(j) != null) {
                keys2[keyIdx] = j;
                ++keyIdx;
            }
        }
        return keys2;
    }
    
    public boolean hasMember(final String key) {
        if (!StringUtil.stringIsInteger(key)) {
            throw new LogicException("Invalid index (not a number) for JavaScript array: " + key);
        }
        final int idx = Integer.parseInt(key);
        if (this.bytes != null) {
            return idx < this.bytes.length;
        }
        if (this.longs != null) {
            return idx < this.longs.length;
        }
        if (this.values.get(idx) != null) {
            return true;
        }
        throw new LogicException("Array is empty");
    }
    
    public void putMember(final String key, final Value value) {
        if (!StringUtil.stringIsInteger(key)) {
            throw new LogicException("Invalid index (not a number) for JavaScript array: " + key);
        }
        final int idx = Integer.parseInt(key);
        if (this.bytes != null) {
            if (idx >= this.bytes.length) {
                this.bytes = Arrays.copyOf(this.bytes, idx + 1);
                this.nulls = Arrays.copyOf(this.nulls, idx + 1);
            }
            this.nulls[idx] = (value == null || value.isNull());
            if (value != null) {
                this.bytes[idx] = value.asByte();
            }
        }
        else if (this.longs != null) {
            if (idx >= this.longs.length) {
                this.longs = Arrays.copyOf(this.longs, idx + 1);
                this.nulls = Arrays.copyOf(this.nulls, idx + 1);
            }
            this.nulls[idx] = (value == null || value.isNull());
            if (value != null) {
                this.longs[idx] = value.asLong();
            }
        }
        this.values.set(idx, (ProtocolData)value.asHostObject());
    }
    
    public boolean removeMember(final String key) {
        if (!StringUtil.stringIsInteger(key)) {
            throw new LogicException("Invalid index (not a number) for JavaScript array: " + key);
        }
        final int idx = Integer.parseInt(key);
        if (this.bytes != null) {
            if (idx >= this.bytes.length) {
                return false;
            }
            this.bytes[idx] = 0;
            this.nulls[idx] = true;
        }
        else if (this.longs != null) {
            if (idx >= this.longs.length) {
                throw new LogicException("Index " + idx + " is larger than the number of objects in the array (" + this.longs.length);
            }
            this.longs[idx] = 0L;
            this.nulls[idx] = true;
        }
        return idx < this.values.size() && this.values.remove(idx) != null;
    }
}
