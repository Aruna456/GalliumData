// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java.json;

import java.util.NoSuchElementException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.graalvm.polyglot.proxy.ProxyIterator;

public class JsonArrayIterator implements ProxyIterator
{
    private final JsonWrapper parent;
    private final ArrayNode array;
    private long idx;
    
    public JsonArrayIterator(final JsonWrapper parent, final ArrayNode array) {
        this.idx = 0L;
        this.parent = parent;
        this.array = array;
    }
    
    public boolean hasNext() {
        return this.idx < this.array.size();
    }
    
    public Object getNext() throws NoSuchElementException, UnsupportedOperationException {
        final JsonNode member = this.array.get((int)this.idx);
        ++this.idx;
        return JsonWrapper.wrap(this.parent, member);
    }
}
