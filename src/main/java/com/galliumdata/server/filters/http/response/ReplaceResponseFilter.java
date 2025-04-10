// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http.response;

import java.util.regex.Matcher;
import com.galliumdata.server.handler.http.java.HttpExchange;
import com.galliumdata.server.handler.http.java.HttpResponse;
import java.util.Collection;
import com.galliumdata.server.logic.FilterUtils;
import com.galliumdata.server.handler.http.java.HttpRequest;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import java.util.regex.Pattern;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.filters.http.HttpResponseFilter;

public class ReplaceResponseFilter extends HttpResponseFilter implements ResponseFilter
{
    private Object searchPattern;
    private String replacementString;
    
    @Override
    public void configure(final FilterUse def) {
        super.configure(def);
        final String searchPatternStr = (String) def.getParameters().get("Search pattern");
        if (searchPatternStr != null && !searchPatternStr.isBlank()) {
            if (searchPatternStr.startsWith("regex:")) {
                this.searchPattern = Pattern.compile(searchPatternStr.substring("regex:".length()), 42);
            }
            else if (searchPatternStr.startsWith("REGEX:")) {
                this.searchPattern = Pattern.compile(searchPatternStr.substring("REGEX:".length()), 40);
            }
            else {
                this.searchPattern = searchPatternStr;
            }
        }
        this.replacementString = (String) def.getParameters().get("Replacement string");
        if (this.replacementString == null) {
            this.replacementString = "";
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
        if (!this.contentPatternMatches(response)) {
            return new FilterResult();
        }
        this.replace(response);
        return new FilterResult();
    }
    
    private void replace(final HttpResponse response) {
        final String body = response.getPayloadString();
        if (body == null) {
            return;
        }
        if (this.searchPattern == null) {
            return;
        }
        if (this.searchPattern instanceof String) {
            response.setPayloadString(body.replaceAll((String)this.searchPattern, this.replacementString));
            return;
        }
        final Matcher matcher = ((Pattern)this.searchPattern).matcher(body);
        final StringBuilder sb = new StringBuilder();
        int sidx = 0;
        while (matcher.find()) {
            String expandedTemplate = this.replacementString;
            expandedTemplate = expandedTemplate.replaceAll("\\$\\{0}", matcher.group());
            for (int j = 1; j <= matcher.groupCount(); ++j) {
                expandedTemplate = expandedTemplate.replaceAll("\\$\\{" + j, matcher.group(j));
            }
            sb.append(body, sidx, matcher.start());
            sb.append(expandedTemplate);
            sidx = matcher.end();
        }
        sb.append(body, sidx, body.length());
        response.setPayloadString(sb.toString());
    }
    
    @Override
    public String getName() {
        return "HTTP Replace Response Filter";
    }
    
    @Override
    public String[] getPacketTypes() {
        if (this.method == null || "<all>".equals(this.method)) {
            return null;
        }
        return new String[] { this.method };
    }
}
