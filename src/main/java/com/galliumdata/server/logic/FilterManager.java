// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import com.galliumdata.server.logic.builtin.ConnectionAddressFilter;
import java.util.HashMap;
import java.util.Map;

public class FilterManager
{
    private static FilterManager instance;
    private Map<String, Class<? extends Filter>> builtinFilters;
    
    public static FilterManager getInstance() {
        return FilterManager.instance;
    }
    
    private FilterManager() {
        (this.builtinFilters = new HashMap<String, Class<? extends Filter>>()).put("Connection address", ConnectionAddressFilter.class);
    }
    
    public Class<? extends Filter> getFilterByName(final String name) {
        return this.builtinFilters.get(name);
    }
    
    static {
        FilterManager.instance = new FilterManager();
    }
}
