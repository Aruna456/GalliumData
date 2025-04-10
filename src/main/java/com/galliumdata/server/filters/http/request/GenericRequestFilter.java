// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http.request;

import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.filters.http.PayloadHolder;
import com.galliumdata.server.handler.http.java.HttpExchange;
import java.util.Collection;
import com.galliumdata.server.logic.FilterUtils;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.http.java.HttpRequest;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.FilterUse;
import java.util.regex.Pattern;
import com.galliumdata.server.logic.RequestFilter;
import com.galliumdata.server.filters.http.HttpFilter;

public class GenericRequestFilter extends HttpFilter implements RequestFilter
{
    protected Pattern contentPattern;
    
    @Override
    public void configure(final FilterUse def) {
        this.contentPattern = null;
        super.configure(def);
        final String contentPatternStr = (String) def.getParameters().get("Content pattern");
        if (contentPatternStr != null && !contentPatternStr.isBlank()) {
            this.contentPattern = Pattern.compile(contentPatternStr, 40);
        }
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final HttpRequest request = (HttpRequest)context.get("request");
        if (GenericRequestFilter.log.isTraceEnabled()) {
            GenericRequestFilter.log.trace(Markers.HTTP, "Executing filter " + this.def.getName() + " in " + this.def.getParentObject().getName());
        }
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
        if (!this.headerPatternsMatch(this.headerPatterns, request)) {
            return new FilterResult();
        }
        if (this.contentPattern != null && !this.contentPattern.asMatchPredicate().test(request.getPayloadString())) {
            return new FilterResult();
        }
        final FilterResult result = new FilterResult();
        context.put("result", result);
        PayloadHolder payloadHolder = null;
        if (request.getPayloadStream() != null) {
            payloadHolder = new PayloadHolder(request.getPayloadStream());
            context.put("payloadHolder", payloadHolder);
        }
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        if (payloadHolder != null && payloadHolder.payloadHasBeenRead()) {
            request.setPayload(payloadHolder.getPayload());
        }
        return result;
    }
    
    @Override
    public String getName() {
        return "JavaScript request filter - HTTP";
    }
    
    @Override
    public String[] getPacketTypes() {
        if (this.method == null || "<all>".equals(this.method)) {
            return null;
        }
        return new String[] { this.method };
    }
}
