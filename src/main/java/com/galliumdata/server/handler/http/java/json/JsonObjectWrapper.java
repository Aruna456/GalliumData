// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java.json;

import org.graalvm.polyglot.Value;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;

import com.galliumdata.server.ServerException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graalvm.polyglot.proxy.ProxyObject;

public class JsonObjectWrapper extends JsonWrapper implements ProxyObject
{
    private final ObjectNode topNode;
    
    public JsonObjectWrapper(final JsonWrapper parent, final JsonNode topNode) {
        super(parent);
        if (!topNode.isObject()) {
            throw new ServerException("db.http.logic.NotAnObject", new Object[0]);
        }
        this.topNode = (ObjectNode)topNode;
    }
    
    @Override
    public String getJson() {
        return this.topNode.toString();
    }
    
    @Override
    public Object at(final String ptr) {
        final JsonNode res = this.topNode.at(ptr);
        return JsonWrapper.wrap(this, res);
    }
    
    public String toString() {
        return this.topNode.toString();
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                return JsonWrapper.wrap(this, this.topNode.get(key));
            }
        }
    }
    
    public Object getMemberKeys() {
        final List<String> keys = new ArrayList<String>();
        final Iterator<String> iter = this.topNode.fieldNames();
        while (iter.hasNext()) {
            keys.add(iter.next());
        }
        keys.add("toString");
        return keys.toArray(new String[0]);
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "toString": {
                return true;
            }
            default: {
                return this.topNode.has(key);
            }
        }
    }
    
    public void putMember(final String key, final Value value) {
        this.topNode.set(key, JsonWrapper.convertJS(value));
        this.markAsChanged();
    }
    
    public boolean removeMember(final String key) {
        this.markAsChanged();
        final JsonNode n = this.topNode.remove(key);
        return n != null && !n.isNull();
    }
}
