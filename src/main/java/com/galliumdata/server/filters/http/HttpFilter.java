// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http;

import org.apache.logging.log4j.LogManager;
import java.util.Iterator;
import com.galliumdata.server.handler.http.java.HttpExchange;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.logic.FilterUtils;
import java.util.regex.Pattern;
import java.util.HashSet;
import com.galliumdata.server.log.Markers;
import java.util.HashMap;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;

public abstract class HttpFilter
{
    protected FilterUse def;
    protected String method;
    protected Set<Object> clientIps;
    protected Map<String, Object> headerPatterns;
    protected Object urlPattern;
    protected static final Logger log;
    
    public HttpFilter() {
        this.headerPatterns = new HashMap<String, Object>();
    }
    
    public void configure(final FilterUse def) {
        this.def = def;
        if (HttpFilter.log.isTraceEnabled()) {
            HttpFilter.log.trace(Markers.HTTP, "Configuring filter " + def.getName() + " in " + def.getParentObject().getName());
        }
        this.urlPattern = null;
        this.method = null;
        this.clientIps = new HashSet<Object>();
        this.headerPatterns = new HashMap<String, Object>();
        final String urlPatStr = (String) def.getParameters().get("URL pattern");
        if (urlPatStr != null && urlPatStr.trim().length() > 0) {
            if (urlPatStr.trim().startsWith("regex:")) {
                this.urlPattern = Pattern.compile(urlPatStr.substring("regex:".length()), 42);
            }
            else {
                this.urlPattern = urlPatStr;
            }
        }
        this.method = def.getParameters().get("Method").toString();
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr);
        final String headerPatternsStr = (String) def.getParameters().get(this.getHeaderPatternsName());
        if (headerPatternsStr != null && !headerPatternsStr.isBlank()) {
            this.readHeaderPatterns(headerPatternsStr, this.headerPatterns);
        }
    }
    
    protected void readHeaderPatterns(final String headerPatternsStr, final Map<String, Object> map) {
        final String[] split;
        final String[] headerParts = split = headerPatternsStr.split("\\R");
        for (final String part : split) {
            if (!part.isBlank()) {
                final int colonIdx = part.indexOf(58);
                if (colonIdx == -1) {
                    throw new ServerException("db.http.logic.BadParameterValue", new Object[] { this.def.getName(), "Header patterns", "No colon found in \"" + part });
                }
                final String name = part.substring(0, colonIdx).trim();
                String value = part.substring(colonIdx + 1).trim();
                if (value.startsWith("regex:")) {
                    value = value.substring("regex:".length()).trim();
                    final Pattern pat = Pattern.compile(value, 2);
                    map.put(name, pat);
                }
                else if (value.startsWith("REGEX:")) {
                    value = value.substring("regex:".length()).trim();
                    final Pattern pat = Pattern.compile(value);
                    map.put(name, pat);
                }
                else {
                    map.put(name, value);
                }
            }
        }
    }
    
    protected boolean urlMatches(final String url) {
        if (this.urlPattern == null) {
            return true;
        }
        if (this.urlPattern instanceof String) {
            final String urlPatternStr = (String)this.urlPattern;
            return urlPatternStr.equalsIgnoreCase(url);
        }
        final Pattern urlPatternPat = (Pattern)this.urlPattern;
        return urlPatternPat.asMatchPredicate().test(url);
    }
    
    protected boolean headerPatternsMatch(final Map<String, Object> headerPatterns, final HttpExchange exchange) {
        if (headerPatterns == null || headerPatterns.isEmpty()) {
            return true;
        }
        boolean isMatch = false;
        for (final Map.Entry<String, Object> entry : headerPatterns.entrySet()) {
            final String headerName = entry.getKey();
            if (!exchange.hasHeader(headerName)) {
                continue;
            }
            final String headerValue = exchange.getHeader(headerName);
            final Object pat = entry.getValue();
            if (pat instanceof String) {
                final String patStr = (String)pat;
                if (patStr.equalsIgnoreCase(headerValue)) {
                    isMatch = true;
                    break;
                }
                continue;
            }
            else {
                final Pattern patPat = (Pattern)pat;
                if (patPat.asMatchPredicate().test(headerValue)) {
                    isMatch = true;
                    break;
                }
                continue;
            }
        }
        return isMatch;
    }
    
    protected String getHeaderPatternsName() {
        return "Header patterns";
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
