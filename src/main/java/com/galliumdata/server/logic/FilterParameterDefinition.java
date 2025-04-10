// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import java.util.HashMap;
import java.util.Map;

public class FilterParameterDefinition
{
    private String name;
    private Map<String, FilterParameter> parameterDefinitions;
    
    public FilterParameterDefinition() {
        this.parameterDefinitions = new HashMap<String, FilterParameter>();
    }
    
    public Map<String, FilterParameter> getParameterDefinitions() {
        return this.parameterDefinitions;
    }
    
    public void addParameterDefinition(final FilterParameter p) {
        this.parameterDefinitions.put(p.name, p);
    }
}
