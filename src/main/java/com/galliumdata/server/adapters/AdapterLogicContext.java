// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import java.util.HashMap;
import java.util.Map;

public class AdapterLogicContext
{
    private Map<String, Object> variables;
    
    public AdapterLogicContext() {
        this.variables = new HashMap<String, Object>();
    }
    
    public Object get(final String name) {
        return this.variables.get(name);
    }
    
    public Object put(final String name, final Object value) {
        return this.variables.put(name, value);
    }
}
