// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.request;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.RequestFilter;

public class GenericJavaScriptFilter implements RequestFilter
{
    protected FilterUse filterUse;
    protected String[] packetTypes;
    protected static final Logger log;
    
    public GenericJavaScriptFilter() {
        this.packetTypes = new String[0];
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.filterUse = def;
        final Variables filterContext = def.getFilterContext();
        if (filterContext.get("_initialized") != null) {
            this.packetTypes = (String[])filterContext.get("packetTypes");
            return;
        }
        final String pktTypesStr = (String) def.getParameters().get("Packet types");
        if (pktTypesStr != null && pktTypesStr.trim().length() > 0) {
            this.packetTypes = pktTypesStr.split(",");
            for (int i = 0; i < this.packetTypes.length; ++i) {
                this.packetTypes[i] = this.packetTypes[i].trim();
            }
            filterContext.put("packetTypes", this.packetTypes);
        }
        filterContext.put("_initialized", true);
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final FilterResult result = new FilterResult();
        result.setFilterName(this.getName());
        context.put("result", result);
        final Source src = ScriptManager.getInstance().getSource(this.filterUse.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    @Override
    public String getName() {
        return "GenericJavaScriptFilter - request";
    }
    
    @Override
    public String[] getPacketTypes() {
        return this.packetTypes;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.uselog");
    }
}
