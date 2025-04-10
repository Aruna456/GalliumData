// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.request;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import java.util.Iterator;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.ServerException;
import java.util.Collection;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.PacketGroup;
import com.galliumdata.server.handler.GenericPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.logic.FilterUtils;
import java.util.HashSet;
import java.util.HashMap;
import org.apache.logging.log4j.Logger;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.RequestFilter;

public class BulkLoadFilter implements RequestFilter
{
    private FilterUse def;
    protected Set<Object> queryPatterns;
    protected Set<Object> clientIps;
    protected Set<Object> users;
    private Map<String, Pattern> columnPatterns;
    private String andOr;
    private static final Logger log;
    
    public BulkLoadFilter() {
        this.columnPatterns = new HashMap<String, Pattern>();
        this.andOr = "and";
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.queryPatterns = new HashSet<Object>();
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        this.columnPatterns = new HashMap<String, Pattern>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.queryPatterns = (Set<Object>) init.get("queryPatterns");
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = (Set<Object>) init.get("users");
            this.columnPatterns = (Map<String, Pattern>) init.get("columnPatterns");
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
        final String columnPatStr = (String) def.getParameters().get("Column patterns");
        if (columnPatStr != null && columnPatStr.trim().length() > 0) {
            final String[] split;
            final String[] columnPats = split = columnPatStr.split("\\v");
            for (String pat : split) {
                pat = pat.replaceAll("\\\\=", "GA989796YADDA");
                final String[] nameVal = pat.split("=");
                if (nameVal.length != 2) {
                    BulkLoadFilter.log.error(Markers.MSSQL, "Invalid value for Column patterns in filter " + def.getName() + ": not in the form name=value");
                    throw new RepositoryException("repo.BadProperty", new Object[] { "bulk load filter - parameter patterns", pat });
                }
                nameVal[1] = nameVal[1].replaceAll("GA989796YADDA", "=");
                this.columnPatterns.put(nameVal[0], Pattern.compile(nameVal[1], 42));
            }
        }
        init.put("columnPatterns", this.columnPatterns);
        if (def.getParameters().containsKey("And/or")) {
            this.andOr = def.getParameters().get("And/or").toString();
            if (!"and".equals(this.andOr) && !"or".equals(this.andOr)) {
                throw new RepositoryException("repo.BadProperty", new Object[] { "bulk load filter - paramsAndOr", this.andOr });
            }
            init.put("andOr", this.andOr);
        }
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final GenericPacket pkt = (GenericPacket)context.get("packet");
        final Object packets = context.get("packets");
        if (!(packets instanceof PacketGroup)) {
            return new FilterResult();
        }
        final PacketGroup<MessageToken> pktGrp = (PacketGroup<MessageToken>)packets;
        final MessageToken token = pktGrp.get(0);
        if (!(token instanceof TokenRow)) {
            return new FilterResult();
        }
        final TokenRow row = (TokenRow)token;
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        final String sql = (String)connectionContext.get("lastSQL");
        if (!FilterUtils.stringMatchesNamesOrRegexes(sql, this.queryPatterns)) {
            return new FilterResult();
        }
        final String username = (String)connectionContext.get("userName");
        if (!FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
            return new FilterResult();
        }
        if (!this.columnPatterns.isEmpty()) {
            boolean matches = true;
            for (final Map.Entry<String, Pattern> entry : this.columnPatterns.entrySet()) {
                final String columnName = entry.getKey();
                final int colIdx = row.getIndexOfColumn(columnName);
                if (colIdx == -1) {
                    throw new ServerException("db.mssql.logic.NoSuchColumn", new Object[] { columnName });
                }
                final Object value = row.getValue(columnName);
                if (value == null) {
                    matches = "null".equalsIgnoreCase(entry.getValue().toString());
                }
                else {
                    matches = entry.getValue().matcher(value.toString()).matches();
                }
                if (!matches && "and".equals(this.andOr)) {
                    return new FilterResult();
                }
                if (matches && "or".equals(this.andOr)) {
                    break;
                }
            }
            if (!matches) {
                return new FilterResult();
            }
        }
        final FilterResult result = new FilterResult();
        context.put("result", result);
        context.put("packet", row);
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    @Override
    public String getName() {
        return "Bulk load filter - MSSQL";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "BulkLoadBCP" };
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
