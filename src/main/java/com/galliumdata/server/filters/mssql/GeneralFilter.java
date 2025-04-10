// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql;

import org.apache.logging.log4j.LogManager;
import java.util.Iterator;
import com.galliumdata.server.adapters.Variables;
import java.util.regex.Pattern;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.log.Markers;
import java.util.HashMap;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.RequestFilter;

public abstract class GeneralFilter implements RequestFilter
{
    protected FilterUse def;
    protected Map<String, Object> contextPatterns;
    protected static final Logger log;
    
    public GeneralFilter() {
        this.contextPatterns = new HashMap<String, Object>();
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.contextPatterns = (Map<String, Object>) init.get("contextPatterns");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String contextPatStr = (String) def.getParameters().get("Context patterns");
        if (contextPatStr != null && contextPatStr.trim().length() > 0) {
            final String[] split;
            final String[] ctxtPats = split = contextPatStr.split("\\v");
            for (String ctxtPat : split) {
                final String[] nameVal = ctxtPat.split("=");
                if (nameVal.length != 2) {
                    GeneralFilter.log.error(Markers.MSSQL, "Invalid value for Context patterns in filter " + def.getName() + ": not in the form name=value");
                    throw new RepositoryException("repo.BadProperty", new Object[] { "General filter - context patterns", ctxtPat });
                }
                final String name = nameVal[0];
                final String rawVal = nameVal[1];
                if (rawVal.startsWith("regex:")) {
                    final Pattern pat = Pattern.compile(rawVal.substring("regex:".length()), 10);
                    this.contextPatterns.put(name, pat);
                }
                else if (rawVal.startsWith("REGEX:")) {
                    final Pattern pat = Pattern.compile(rawVal.substring("REGEX:".length()), 8);
                    this.contextPatterns.put(name, pat);
                }
                else {
                    this.contextPatterns.put(name, rawVal);
                }
            }
            init.put("contextPatterns", this.contextPatterns);
        }
    }
    
    protected boolean skipInvocation(final Variables context) {
        if (this.contextPatterns.size() > 0) {
            for (final Map.Entry<String, Object> entry : this.contextPatterns.entrySet()) {
                final String name = entry.getKey();
                final String[] nameParts = name.split("\\.");
                Object valueObj = null;
                Variables ctxt = context;
                for (final String namePart : nameParts) {
                    valueObj = ctxt.get(namePart);
                    if (valueObj == null) {
                        return true;
                    }
                    if (valueObj instanceof Variables) {
                        ctxt = (Variables)valueObj;
                    }
                }
                final String varStr = valueObj.toString();
                final Object value = entry.getValue();
                if (value instanceof String) {
                    if (!value.equals(varStr)) {
                        return true;
                    }
                    continue;
                }
                else {
                    if (!(value instanceof Pattern)) {
                        throw new RuntimeException("Unexpected: context pattern not a string or a pattern");
                    }
                    final Pattern pattern = (Pattern)value;
                    if (!pattern.matcher(varStr).matches()) {
                        return true;
                    }
                    continue;
                }
            }
        }
        return false;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
