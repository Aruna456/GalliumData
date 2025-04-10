// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.response;

import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;

public class GenericJavaScriptFilter extends DNSResponseFilter
{
    @Override
    public FilterResult filterResponse(final Variables context) {
        if (!this.responsePacketIsRelevant(context)) {
            return new FilterResult();
        }
        final FilterResult result = new FilterResult();
        context.put("result", result);
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    @Override
    public String getName() {
        return "JavaScript response filter - DNS";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[0];
    }
}
