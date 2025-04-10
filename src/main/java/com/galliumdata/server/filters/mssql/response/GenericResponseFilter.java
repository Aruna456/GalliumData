// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.response;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import java.util.Collection;
import com.galliumdata.server.handler.GenericPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.repository.FilterUse;
import java.util.Set;
import java.util.regex.Pattern;
import com.galliumdata.server.logic.ResponseFilter;

public class GenericResponseFilter implements ResponseFilter
{
    protected Pattern queryPattern;
    protected Set<String> packetTypes;
    protected Set<Object> clientIps;
    protected Set<Object> users;
    private FilterUse def;
    private static final Logger log;
    
    public GenericResponseFilter() {
        this.users = new HashSet<Object>();
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.queryPattern = null;
        this.packetTypes = new HashSet<String>();
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.queryPattern = (Pattern) init.get("queryPattern");
            this.packetTypes = (Set<String>) init.get("packetTypes");
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = (Set<Object>) init.get("users");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String qryPatStr = (String) def.getParameters().get("Query pattern");
        if (qryPatStr != null && qryPatStr.trim().length() > 0) {
            if (qryPatStr.trim().startsWith("regex:")) {
                this.queryPattern = Pattern.compile(qryPatStr.substring("regex:".length()), 42);
            }
            else {
                this.queryPattern = Pattern.compile(qryPatStr, 40);
            }
            init.put("queryPattern", this.queryPattern);
        }
        final String packetTypesStr = (String) def.getParameters().get("Packet types");
        init.put("packetTypes", this.packetTypes = FilterUtils.readCommaSeparatedNames(packetTypesStr));
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
        final String usersStr = (String) def.getParameters().get("Users");
        init.put("users", this.users = FilterUtils.readCommaSeparatedNamesOrRegexes(usersStr));
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        final GenericPacket pkt = (GenericPacket)context.get("packet");
        final String pktType = pkt.getPacketType();
        if (this.packetTypes != null && !this.packetTypes.isEmpty() && !this.packetTypes.contains(pktType)) {
            return new FilterResult();
        }
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String username = (String)connectionContext.get("userName");
        if (username != null && this.users != null && !this.users.isEmpty() && !FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
            return new FilterResult();
        }
        if (this.queryPattern != null) {
            final String curSql = (String)connectionContext.get("lastSQL");
            if (curSql != null && !this.queryPattern.asMatchPredicate().test(curSql)) {
                return new FilterResult();
            }
        }
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
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
        return "MSSQL Generic Response Filter";
    }
    
    @Override
    public String[] getPacketTypes() {
        if (this.packetTypes == null) {
            return null;
        }
        return this.packetTypes.toArray(new String[0]);
    }
    
    static {
        log = LogManager.getLogger("galliumdata.uselog");
    }
}
