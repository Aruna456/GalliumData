// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

import java.util.HashMap;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.logic.LogicException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.util.StringUtil;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class PacketGroup<T extends GenericPacket> implements ProxyObject, Iterable<T>
{
    private final List<T> packets;
    private boolean modified;
    private Map<String, Object> members;
    
    public PacketGroup() {
        this.packets = new ArrayList<T>(100);
        this.modified = false;
    }
    
    public boolean isModified() {
        for (final T pkt : this.packets) {
            if (pkt.isModified()) {
                return true;
            }
        }
        return this.modified;
    }
    
    public void setModified(final boolean b) {
        this.modified = b;
    }
    
    public int getSize() {
        return this.packets.size();
    }
    
    public T get(final int idx) {
        return this.packets.get(idx);
    }
    
    public List<T> getPackets() {
        return this.packets;
    }
    
    public void addPacket(final T newPkt) {
        this.packets.add(newPkt);
        this.modified = true;
    }
    
    public void addPacket(final int idx, final T newPkt) {
        this.packets.add(idx, newPkt);
        this.modified = true;
    }
    
    public void addPacketNoModify(final T newPkt) {
        this.packets.add(newPkt);
    }
    
    public void removePacket(final T pkt) {
        int pktIdx = -1;
        for (int i = 0; i < this.packets.size(); ++i) {
            if (this.packets.get(i) == pkt) {
                pktIdx = i;
                break;
            }
        }
        if (pktIdx >= 0) {
            this.packets.remove(pktIdx);
            this.modified = true;
        }
    }
    
    public void clear() {
        this.packets.clear();
        this.modified = true;
    }
    
    public void replacePacket(final T oldPkt, final T newPkt) {
        final int idx = this.packets.indexOf(oldPkt);
        if (idx == -1) {
            throw new RuntimeException("No such packet in PakcetGroup");
        }
        this.packets.remove(idx);
        this.packets.add(idx, newPkt);
        this.modified = true;
    }
    
    public Iterator<T> iterator() {
        return new PacketIterator<T>(this);
    }
    
    public Object getMember(final String key) {
        this.initializeMembers();
        if (!StringUtil.stringIsInteger(key)) {
            final Object val = this.members.get(key);
            return val;
        }
        final int idx = Integer.parseInt(key);
        if (idx >= this.packets.size()) {
            return null;
        }
        return this.packets.get(idx);
    }
    
    public Object getMemberKeys() {
        this.initializeMembers();
        return this.members.keySet().toArray();
    }
    
    public boolean hasMember(final String key) {
        this.initializeMembers();
        return this.members.containsKey(key);
    }
    
    public void putMember(final String key, final Value value) {
        this.initializeMembers();
        if (StringUtil.stringIsInteger(key)) {
            final int idx = Integer.parseInt(key);
            this.packets.add(idx, (T)value.asHostObject());
            return;
        }
        throw new LogicException("Cannot set value for " + key + " in a packet group");
    }
    
    public boolean removeMember(final String key) {
        this.initializeMembers();
        if (!StringUtil.stringIsInteger(key)) {
            throw new ServerException("db.mssql.logic.InvalidIndex", new Object[] { key });
        }
        if (!this.members.containsKey(key)) {
            throw new ServerException("db.mssql.logic.UnknownIndex", new Object[] { key });
        }
        final int idx = Integer.parseInt(key);
        this.packets.remove(idx);
        this.members = null;
        return true;
    }
    
    private void initializeMembers() {
        if (this.members != null) {
            return;
        }
        (this.members = new HashMap<String, Object>()).put("addPacket", (Function<Value[],Object>) arguments -> {
            if (arguments.length == 2) {
                final int idx = arguments[0].asInt();
                final Value val = arguments[1];
                if (val.isProxyObject()) {
                    this.addPacket(idx, (T) val.asProxyObject());
                }
            }
            else {
                final Value val2 = arguments[0];
                if (val2.isProxyObject()) {
                    this.addPacket((T) arguments[0].asProxyObject());
                }
            }
            return null;
        });
        for (int i = 0; i < this.packets.size(); ++i) {
            this.members.put("" + i, this.members.get(i));
        }
        this.members.put("remove", (Function<Value[],Object>) arguments -> this.packets.remove(arguments[0].asHostObject()));
    }
}
