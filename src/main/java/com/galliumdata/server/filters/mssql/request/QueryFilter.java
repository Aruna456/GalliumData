// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.request;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.handler.mssql.SQLBatchPacket;

import java.util.*;

import com.galliumdata.server.handler.GenericPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.RequestFilter;

public class QueryFilter implements RequestFilter
{
    private FilterUse def;
    protected Set<Object> queryPatterns;
    protected Set<Object> clientIps;
    protected Set<Object> users;
    private static final Logger log;
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.queryPatterns = new HashSet<Object>();
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.queryPatterns = (Set<Object>) init.get("queryPatterns");
            this.clientIps = Collections.singleton(init.get("clientIps"));
            this.users = (Set<Object>) init.get("users");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String questPatternStr = (String) def.getParameters().get("Query patterns");
        init.put("queryPatterns", this.queryPatterns = FilterUtils.readCommaSeparatedNamesOrRegexes(questPatternStr));
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
        final String usersStr = (String) def.getParameters().get("Users");
        init.put("users", this.users = FilterUtils.readCommaSeparatedNamesOrRegexes(usersStr));
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final GenericPacket pkt = (GenericPacket)context.get("packet");
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        final SQLBatchPacket commandPacket = (SQLBatchPacket)pkt;
        final String query = commandPacket.getSql();
        if (!FilterUtils.stringMatchesNamesOrRegexes(query, this.queryPatterns)) {
            return new FilterResult();
        }
        final String username = (String)connectionContext.get("userName");
        if (!FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
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
        return "Query filter - MSSQL";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "SQLBatch" };
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
