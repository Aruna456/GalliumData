// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.response;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;

import java.util.*;

import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.handler.mssql.tokens.TokenRowBatch;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import java.util.regex.Pattern;
import com.galliumdata.server.logic.FilterUtils;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;

public class ResultSetBatchFilter implements ResponseFilter
{
    private FilterUse def;
    private Object queryPattern;
    private Set<Object> clientIps;
    private Set<Object> users;
    private Set<Object> columnNames;
    private static final Logger log;
    
    public ResultSetBatchFilter() {
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        this.columnNames = new HashSet<Object>();
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.queryPattern = null;
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        this.columnNames = new HashSet<Object>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.queryPattern = init.get("queryPattern");
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = Collections.singleton(init.get("users"));
            this.columnNames = (Set<Object>) init.get("schemaTableColumns");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
        final String usersStr = (String) def.getParameters().get("Users");
        init.put("users", this.users = FilterUtils.readCommaSeparatedNamesOrRegexes(usersStr));
        String qryPatStr = (String) def.getParameters().get("Query pattern");
        if (qryPatStr != null && qryPatStr.trim().length() > 0) {
            qryPatStr = qryPatStr.trim();
            if (qryPatStr.startsWith("regex:")) {
                this.queryPattern = Pattern.compile(qryPatStr.substring("regex:".length()), 42);
            }
            else if (qryPatStr.startsWith("REGEX:")) {
                this.queryPattern = Pattern.compile(qryPatStr, 40);
            }
            else {
                this.queryPattern = qryPatStr;
            }
        }
        init.put("queryPattern", this.queryPattern);
        final String columnsRaw = (String) def.getParameters().get("Columns");
        if (columnsRaw != null && !columnsRaw.isEmpty()) {
            this.columnNames = FilterUtils.readCommaSeparatedNamesOrRegexes(columnsRaw);
        }
        init.put("Columns", this.columnNames);
    }
    
    @Override
    public String getName() {
        return "MSSQL result set batch filter";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "RowBatch" };
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        final FilterResult result = new FilterResult();
        final Variables connectionContext = (Variables)context.get("connectionContext");
        if (this.users != null && !this.users.isEmpty()) {
            final String username = (String)connectionContext.get("userName");
            if (!FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
                return new FilterResult();
            }
        }
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        if (this.queryPattern != null) {
            final String curSql = (String)connectionContext.get("lastSQL");
            if (this.queryPattern instanceof String) {
                final String queryStr = (String)this.queryPattern;
                if (!queryStr.equals(curSql)) {
                    return new FilterResult();
                }
            }
            else {
                final Pattern pat = (Pattern)this.queryPattern;
                if (curSql != null && !pat.asMatchPredicate().test(curSql)) {
                    return new FilterResult();
                }
            }
        }
        if (this.columnNames != null && !this.columnNames.isEmpty()) {
            final TokenRowBatch batch = (TokenRowBatch)context.get("packet");
            final TokenRow row = batch.getRows().get(0);
            final List<ColumnMetadata> metas = row.getColumnMetadata();
            boolean match = false;
            for (final ColumnMetadata meta : metas) {
                if (FilterUtils.stringMatchesNamesOrRegexes(meta.getColumnName(), this.columnNames)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return new FilterResult();
            }
        }
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        context.put("result", result);
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
