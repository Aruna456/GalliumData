// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http.duplex;

import com.galliumdata.server.handler.http.java.HttpResponse;
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
import java.util.HashMap;
import com.galliumdata.server.repository.FilterUse;
import java.util.regex.Pattern;
import java.util.Map;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.logic.RequestFilter;
import com.galliumdata.server.filters.http.HttpFilter;

public class GenericDuplexFilter extends HttpFilter implements RequestFilter, ResponseFilter
{
    protected Map<String, Object> responseHeaderPatterns;
    protected Pattern requestContentPattern;
    protected Pattern responseContentPattern;
    
    @Override
    public void configure(final FilterUse def) {
        this.responseHeaderPatterns = new HashMap<String, Object>();
        this.requestContentPattern = null;
        this.responseContentPattern = null;
        super.configure(def);
        final String headerPatternsStr = (String) def.getParameters().get("Response header patterns");
        if (headerPatternsStr != null && !headerPatternsStr.isBlank()) {
            this.readHeaderPatterns(headerPatternsStr, this.responseHeaderPatterns);
        }
        final String contentPatternStr = (String) def.getParameters().get("Content pattern");
        if (contentPatternStr != null && !contentPatternStr.isBlank()) {
            this.requestContentPattern = Pattern.compile(contentPatternStr, 40);
        }
        final String respContentPatternStr = (String) def.getParameters().get("Response content pattern");
        if (respContentPatternStr != null && !respContentPatternStr.isBlank()) {
            this.responseContentPattern = Pattern.compile(respContentPatternStr, 40);
        }
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final HttpRequest request = (HttpRequest)context.get("request");
        if (GenericDuplexFilter.log.isTraceEnabled()) {
            GenericDuplexFilter.log.trace(Markers.HTTP, "Executing filter " + this.def.getName() + " in " + this.def.getParentObject().getName());
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
        if (this.requestContentPattern != null && !this.requestContentPattern.asMatchPredicate().test(request.getPayloadString())) {
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
        if (!this.headerPatternsMatch(this.headerPatterns, request)) {
            return new FilterResult();
        }
        if (!this.headerPatternsMatch(this.responseHeaderPatterns, response)) {
            return new FilterResult();
        }
        if (this.requestContentPattern != null && !this.requestContentPattern.asMatchPredicate().test(request.getPayloadString())) {
            return new FilterResult();
        }
        if (this.responseContentPattern != null) {
            final String body = response.getResponseMessage();
            if (!this.responseContentPattern.asMatchPredicate().test(body)) {
                return new FilterResult();
            }
        }
        final FilterResult result = new FilterResult();
        context.put("result", result);
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    @Override
    public String getName() {
        return "JavaScript duplex filter - HTTP";
    }
    
    @Override
    public String[] getPacketTypes() {
        if (this.method == null || "<all>".equals(this.method)) {
            return null;
        }
        return new String[] { this.method };
    }
    
    @Override
    protected String getHeaderPatternsName() {
        return "Request header patterns";
    }
}
