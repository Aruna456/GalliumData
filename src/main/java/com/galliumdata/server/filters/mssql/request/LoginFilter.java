// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.request;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.handler.mssql.Login7Packet;
import com.galliumdata.server.handler.GenericPacket;
import java.util.Collection;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.RequestFilter;

public class LoginFilter implements RequestFilter
{
    private FilterUse def;
    protected Set<Object> clientIps;
    protected Set<Object> users;
    private static final Logger log;
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = (Set<Object>) init.get("users");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
        final String usersStr = (String) def.getParameters().get("Users");
        init.put("users", this.users = FilterUtils.readCommaSeparatedNamesOrRegexes(usersStr));
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        final GenericPacket pkt = (GenericPacket)context.get("packet");
        final Login7Packet loginPacket = (Login7Packet)pkt;
        final String loginName = loginPacket.getUsername();
        if (loginName != null && loginName.trim().length() > 0) {
            final String username = (String)connectionContext.get("userName");
            if (!FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
                return new FilterResult();
            }
        }
        final FilterResult result = new FilterResult();
        context.put("result", result);
        final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    @Override
    public String getName() {
        return "Login filter - MSSQL";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "Login7" };
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
