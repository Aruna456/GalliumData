// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import java.util.TreeMap;
import java.util.Map;

public class AdapterContext
{
    public Map<String, Object> parameterValues;
    public AdapterCallback callbacks;
    
    public AdapterContext() {
        this.parameterValues = new TreeMap<String, Object>();
    }
}
