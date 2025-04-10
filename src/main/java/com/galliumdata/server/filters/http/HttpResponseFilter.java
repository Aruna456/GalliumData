// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http;

import java.util.Iterator;
import com.galliumdata.server.handler.http.java.HttpResponse;
import com.galliumdata.server.logic.FilterUtils;
import java.util.HashSet;
import com.galliumdata.server.repository.FilterUse;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class HttpResponseFilter extends HttpFilter
{
    protected Pattern contentPattern;
    protected Set<Object> statusCodes;
    
    @Override
    public void configure(final FilterUse def) {
        super.configure(def);
        this.contentPattern = null;
        this.statusCodes = new HashSet<Object>();
        final String contentPatternStr = (String) def.getParameters().get("Content pattern");
        if (contentPatternStr != null && !contentPatternStr.isBlank()) {
            this.contentPattern = Pattern.compile(contentPatternStr, 40);
        }
        final String statusCodesStr = (String) def.getParameters().get("Status code patterns");
        if (statusCodesStr != null && !statusCodesStr.isBlank()) {
            this.statusCodes = FilterUtils.readCommaSeparatedNamesOrRegexes(statusCodesStr);
        }
    }
    
    protected boolean statusCodeMatches(final HttpResponse response) {
        boolean statusCodeMatch = false;
        if (this.statusCodes.isEmpty()) {
            statusCodeMatch = true;
        }
        else {
            final String statusCode = "" + response.getResponseCode();
            for (final Object statusCodePatternObj : this.statusCodes) {
                if (statusCodePatternObj instanceof String) {
                    final String statusCodePatternStr = (String)statusCodePatternObj;
                    if (statusCodePatternStr.equals(statusCode)) {
                        statusCodeMatch = true;
                        break;
                    }
                    continue;
                }
                else {
                    final Pattern pat = (Pattern)statusCodePatternObj;
                    if (pat.matcher(statusCode).matches()) {
                        statusCodeMatch = true;
                        break;
                    }
                    continue;
                }
            }
        }
        return statusCodeMatch;
    }
    
    protected boolean contentPatternMatches(final HttpResponse response) {
        if (this.contentPattern == null) {
            return true;
        }
        final String body = response.getPayloadString();
        return this.contentPattern.asMatchPredicate().test(body);
    }
}
