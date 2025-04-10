// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.response;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;

import java.util.*;

import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.handler.GenericPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.logic.FilterUtils;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;

public class ResultSetFilter implements ResponseFilter
{
    private FilterUse def;
    private Object queryPattern;
    private Map<String, List<Pattern>> columnPatterns;
    private String andOr;
    private String action;
    private Set<Object> clientIps;
    private Set<Object> users;
    private static final Logger log;
    
    public ResultSetFilter() {
        this.andOr = "and";
        this.action = "code";
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.queryPattern = null;
        this.columnPatterns = null;
        this.andOr = "and";
        this.action = "code";
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.queryPattern = init.get("queryPattern");
            this.columnPatterns = (Map<String, List<Pattern>>) init.get("columnPatterns");
            if (init.containsKey("columnsAndOr")) {
                this.andOr = (String) init.get("columnsAndOr");
            }
            if (init.containsKey("action")) {
                this.action = (String) init.get("action");
            }
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = Collections.singleton(init.get("users"));
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
            init.put("queryPattern", this.queryPattern);
        }
        final String colPatStr = (String) def.getParameters().get("Column patterns");
        if (colPatStr != null && colPatStr.trim().length() > 0) {
            final String[] split;
            final String[] colPats = split = colPatStr.split("[,\\n\\r]");
            for (final String pat : split) {
                final String[] nameVal = pat.split("=");
                if (nameVal.length != 2) {
                    throw new RepositoryException("repo.BadProperty", new Object[] { "Result set filter - column patterns", pat });
                }
                if (this.columnPatterns == null) {
                    this.columnPatterns = new HashMap<String, List<Pattern>>();
                }
                List<Pattern> pats = this.columnPatterns.get(nameVal[0]);
                if (pats == null) {
                    pats = new ArrayList<Pattern>();
                    this.columnPatterns.put(nameVal[0], pats);
                }
                pats.add(Pattern.compile(nameVal[1]));
            }
        }
        else {
            this.columnPatterns = new HashMap<String, List<Pattern>>();
        }
        if (this.columnPatterns != null) {
            init.put("columnPatterns", this.columnPatterns);
        }
        if (def.getParameters().containsKey("And/or")) {
            this.andOr = (String) def.getParameters().get("And/or");
            if (!"and".equals(this.andOr) && !"or".equals(this.andOr)) {
                throw new RepositoryException("repo.BadProperty", new Object[] { "Result set filter - And/or", this.andOr });
            }
            init.put("andOr", this.andOr);
        }
        if (def.getParameters().containsKey("Action")) {
            this.action = (String) def.getParameters().get("Action");
            if (!"code".equals(this.action) && !"hide".equals(this.action)) {
                throw new RepositoryException("repo.BadProperty", new Object[] { "Result set filter - Action", this.action });
            }
            init.put("action", this.action);
        }
    }
    
    @Override
    public String getName() {
        return "MSSQL result set filter";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "Row", "NBCRow" };
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        final FilterResult result = new FilterResult();
        final GenericPacket packet = (GenericPacket)context.get("packet");
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
            if (curSql != null) {
                if (this.queryPattern instanceof Pattern) {
                    final Pattern pat = (Pattern)this.queryPattern;
                    if (!pat.asMatchPredicate().test(curSql)) {
                        return new FilterResult();
                    }
                }
                else {
                    final String q = (String)this.queryPattern;
                    if (!q.equals(curSql)) {
                        return new FilterResult();
                    }
                }
            }
        }
        final TokenRow dataRow = (TokenRow)packet;
        final List<ColumnMetadata> colDefs = dataRow.getColumnMetadata();
        boolean rowIsRelevant = false;
    Label_0487:
        for (final ColumnMetadata colDef : colDefs) {
            final String columnName = colDef.getColumnName();
            if (this.columnPatterns == null || this.columnPatterns.size() <= 0 || columnName == null) {
                rowIsRelevant = true;
                break;
            }
            if (!this.columnPatterns.containsKey(columnName)) {
                continue;
            }
            final List<Pattern> pats = this.columnPatterns.get(columnName);
            final MSSQLDataType colVal = dataRow.getColumnValue(colDef);
            for (final Pattern pat2 : pats) {
                if ((colVal == null || colVal.isNull()) && "null".equals(pat2.pattern())) {
                    rowIsRelevant = true;
                }
                else if (colVal != null && !colVal.isNull() && !"null".equals(pat2.pattern()) && pat2.asMatchPredicate().test(colVal.getValue().toString())) {
                    rowIsRelevant = true;
                }
                if (!rowIsRelevant) {
                    if ("and".equals(this.andOr)) {
                        return result;
                    }
                    continue;
                }
                else {
                    if ("or".equals(this.andOr)) {
                        break Label_0487;
                    }
                    continue;
                }
            }
        }
        if (!rowIsRelevant) {
            return result;
        }
        if (this.action != null && this.action.equals("hide")) {
            packet.remove();
            return result;
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
