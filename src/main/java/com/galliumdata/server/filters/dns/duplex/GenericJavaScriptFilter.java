// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.duplex;

import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;

public class GenericJavaScriptFilter extends DNSDuplexFilter
{
    @Override
    public FilterResult filterRequest(final Variables context) {
        final FilterResult result = new FilterResult();
        if (!this.requestPacketIsRelevant(context)) {
            return result;
        }
        context.put("result", result);
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        final FilterResult result = new FilterResult();
        if (!this.responsePacketIsRelevant(context)) {
            return result;
        }
        context.put("result", result);
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    @Override
    public String[] getPacketTypes() {
        return null;
    }
    
    @Override
    public String getName() {
        return "JavaScript duplex filter - DNS";
    }
}
