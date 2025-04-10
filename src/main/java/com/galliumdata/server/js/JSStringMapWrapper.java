// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.js;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.Map;
import org.graalvm.polyglot.proxy.ProxyObject;

public class JSStringMapWrapper implements ProxyObject
{
    private Map<String, String> map;
    private Runnable cb;
    
    public JSStringMapWrapper(final Map<String, String> map, final Runnable cb) {
        this.map = map;
        this.cb = cb;
    }
    
    public Object getMember(final String key) {
        return this.map.get(key);
    }
    
    public Object getMemberKeys() {
        return this.map.keySet().toArray();
    }
    
    public boolean hasMember(final String key) {
        return this.map.containsKey(key);
    }
    
    public void putMember(final String key, final Value value) {
        if (value == null || value.isNull()) {
            this.map.remove(key);
        }
        else if (!value.isString()) {
            throw new ServerException("db.mysql.logic.WrongTypeForMap", new Object[] { "string", value.getMetaSimpleName() });
        }
        this.map.put(key, value.asString());
        if (this.cb != null) {
            this.cb.run();
        }
    }
    
    public boolean removeMember(final String key) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.map.remove(key) != null;
    }
}
