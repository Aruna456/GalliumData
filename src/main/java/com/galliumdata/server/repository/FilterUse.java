// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import java.util.HashMap;
import com.galliumdata.server.adapters.Variables;
import java.util.Map;

public class FilterUse extends RepositoryObject
{
    @Persisted(JSONName = "filterType")
    protected String filterType;
    @Persisted(JSONName = "phase")
    protected String phase;
    @Persisted(JSONName = "priority")
    protected int priority;
    @Persisted(JSONName = "parameters")
    protected Map<String, Object> parameters;
    private final Variables filterContext;
    
    public FilterUse(final Repository repo) {
        super(repo);
        this.parameters = new HashMap<String, Object>();
        this.filterContext = new Variables();
        this.parameters = new HashMap<String, Object>();
    }
    
    public String getFilterType() {
        return this.filterType;
    }
    
    public String getPhase() {
        return this.phase;
    }
    
    public int getPriority() {
        return this.priority;
    }
    
    public Map<String, Object> getParameters() {
        return this.parameters;
    }
    
    public boolean getBooleanParameter(final String name) {
        final Object obj = this.parameters.get(name);
        return obj != null && (boolean)obj;
    }
    
    public Variables getFilterContext() {
        return this.filterContext;
    }
}
