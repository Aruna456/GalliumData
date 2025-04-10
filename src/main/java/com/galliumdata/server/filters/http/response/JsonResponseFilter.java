// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http.response;

import org.graalvm.polyglot.Source;
import java.util.Iterator;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.handler.http.java.HttpExchange;
import com.galliumdata.server.handler.http.java.HttpResponse;
import java.util.Collection;
import com.galliumdata.server.logic.FilterUtils;
import com.galliumdata.server.handler.http.java.HttpRequest;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.jayway.jsonpath.Predicate;
import com.galliumdata.server.filters.http.JsonHolder;
import java.util.ArrayList;
import com.galliumdata.server.repository.FilterUse;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.filters.http.HttpResponseFilter;

public class JsonResponseFilter extends HttpResponseFilter implements ResponseFilter
{
    protected List<JsonPath> jsonPaths;
    
    @Override
    public void configure(final FilterUse def) {
        this.jsonPaths = new ArrayList<JsonPath>();
        super.configure(def);
        final String jsonPathStr = (String) def.getParameters().get("JSON path");
        if (jsonPathStr != null && !jsonPathStr.isBlank()) {
            final String[] split;
            final String[] pathStrs = split = jsonPathStr.split("\\R");
            for (final String pathStr : split) {
                JsonHolder.init();
                this.jsonPaths.add(JsonPath.compile(pathStr, new Predicate[0]));
            }
        }
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
        Object jsonPathResult = null;
        final String body = response.getPayloadString();
        if (body != null && !body.isBlank()) {
            for (final JsonPath path : this.jsonPaths) {
                try {
                    jsonPathResult = path.read(body);
                }
                catch (final Exception ex) {
                    ex.printStackTrace();
                }
                if (jsonPathResult == null) {
                    return new FilterResult();
                }
                if (!(jsonPathResult instanceof List)) {
                    continue;
                }
                final List<?> list = (List<?>)jsonPathResult;
                if (list.isEmpty()) {
                    return new FilterResult();
                }
            }
        }
        final FilterResult result = new FilterResult();
        context.put("result", result);
        if (jsonPathResult != null) {
            context.put("jsonPathResult", jsonPathResult);
        }
        final JsonHolder jsonHolder = new JsonHolder(response.getPayloadStream());
        context.put("jsonHolder", jsonHolder);
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        try {
            ScriptExecutor.executeFilterScript(src, result, context);
        }
        catch (final Exception ex2) {
            ex2.printStackTrace();
        }
        if (jsonHolder.jsonHasBeenRead()) {
            response.setPayloadString(jsonHolder.getJson());
        }
        return result;
    }
    
    @Override
    public String getName() {
        return "HTTP JSON Response Filter";
    }
    
    @Override
    public String[] getPacketTypes() {
        if (this.method == null || "<all>".equals(this.method)) {
            return null;
        }
        return new String[] { this.method };
    }
}
