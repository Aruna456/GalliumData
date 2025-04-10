// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.response;

import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;

public class GenericJavaScriptFilter implements ResponseFilter
{
    protected FilterUse filterUse;
    protected String[] packetTypes;
    
    public GenericJavaScriptFilter() {
        this.packetTypes = new String[0];
    }
    
    @Override
    public void configure(final FilterUse filterUse) {
        this.filterUse = filterUse;
        final Variables filterContext = filterUse.getFilterContext();
        if (filterContext.get("_initialized") != null) {
            this.packetTypes = (String[])filterContext.get("packetTypes");
            return;
        }
        final String pktTypesStr = (String) filterUse.getParameters().get("Packet types");
        if (pktTypesStr != null && pktTypesStr.trim().length() > 0) {
            this.packetTypes = pktTypesStr.split(",");
            for (int i = 0; i < this.packetTypes.length; ++i) {
                this.packetTypes[i] = this.packetTypes[i].trim();
            }
            filterContext.put("packetTypes", this.packetTypes);
        }
    }
    
    @Override
    public String getName() {
        return "GenericJavaScriptFilter - response";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[0];
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        final FilterResult result = new FilterResult();
        return result;
    }
}
