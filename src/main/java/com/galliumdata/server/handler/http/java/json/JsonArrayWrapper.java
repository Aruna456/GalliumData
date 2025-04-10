// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java.json;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.graalvm.polyglot.proxy.ProxyArray;

public class JsonArrayWrapper extends JsonWrapper implements ProxyArray
{
    private final ArrayNode topNode;
    
    public JsonArrayWrapper(final JsonWrapper parent, final JsonNode topNode) {
        super(parent);
        if (!topNode.isArray()) {
            throw new ServerException("db.http.server.InternalError", new Object[] { "JsonNode is not an array" });
        }
        this.topNode = (ArrayNode)topNode;
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
    
    public Object get(final long index) {
        JsonNode childNode;
        try {
            childNode = this.topNode.get((int)index);
        }
        catch (final Exception ex) {
            throw new ServerException("db.http.logic.ArrayIndexOutOfBound", new Object[] { index, this.topNode.size() });
        }
        return JsonWrapper.wrap(this, childNode);
    }
    
    public void set(final long index, final Value value) {
        this.topNode.set((int)index, JsonWrapper.convertJS(value));
        this.markAsChanged();
    }
    
    public boolean remove(final long index) {
        final JsonNode removed = this.topNode.remove((int)index);
        this.markAsChanged();
        return removed != null && !removed.isNull();
    }
    
    public long getSize() {
        return this.topNode.size();
    }
    
    public Object getIterator() {
        return new JsonArrayIterator(this, this.topNode);
    }
}
