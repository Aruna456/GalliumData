// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http.response;

import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.handler.http.java.HttpExchange;
import com.galliumdata.server.handler.http.java.HttpResponse;
import java.util.Collection;
import com.galliumdata.server.logic.FilterUtils;
import com.galliumdata.server.handler.http.java.HttpRequest;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.filters.http.HttpResponseFilter;

public class GenericResponseFilter extends HttpResponseFilter implements ResponseFilter
{
    @Override
    public void configure(final FilterUse def) {
        super.configure(def);
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        final HttpRequest request = (HttpRequest)context.get("request");
        final String reqMethod = request.getMethod();
        if (this.method != null && !this.method.equals("<all>") && !this.method.equals(reqMethod)) {
            return new FilterResult();
        }
        if (!this.urlMatches(request.getUrl())) {
            return new FilterResult();
        }
        final String clientIP = request.getClientAddress().getHostAddress();
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        final HttpResponse response = (HttpResponse)context.get("response");
        if (!this.headerPatternsMatch(this.headerPatterns, response)) {
            return new FilterResult();
        }
        if (!this.statusCodeMatches(response)) {
            return new FilterResult();
        }
        if (!this.contentPatternMatches(response)) {
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
        return "HTTP Generic Response Filter";
    }
    
    @Override
    public String[] getPacketTypes() {
        if (this.method == null || "<all>".equals(this.method)) {
            return null;
        }
        return new String[] { this.method };
    }
}
