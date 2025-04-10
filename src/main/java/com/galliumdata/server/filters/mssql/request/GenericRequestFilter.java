// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.request;

import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import java.util.Collection;
import com.galliumdata.server.handler.GenericPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import java.util.Map;
import java.util.HashSet;
import com.galliumdata.server.repository.FilterUse;
import java.util.Set;
import com.galliumdata.server.filters.mssql.GeneralFilter;

public class GenericRequestFilter extends GeneralFilter
{
    protected Set<String> packetTypes;
    protected Set<Object> clientIps;
    protected Set<Object> users;
    
    @Override
    public void configure(final FilterUse def) {
        this.packetTypes = new HashSet<String>();
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.packetTypes = (Set<String>) init.get("packetTypes");
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = (Set<Object>) init.get("users");
            return;
        }
        super.configure(def);
        init = (Map)filterContext.get("_initialized");
        final String packetTypesStr = (String) def.getParameters().get("Packet types");
        init.put("packetTypes", this.packetTypes = FilterUtils.readCommaSeparatedNames(packetTypesStr));
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
        final String usersStr = (String) def.getParameters().get("Users");
        init.put("users", this.users = FilterUtils.readCommaSeparatedNamesOrRegexes(usersStr));
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        if (super.skipInvocation(context)) {
            return new FilterResult();
        }
        final GenericPacket pkt = (GenericPacket)context.get("packet");
        final String pktType = pkt.getPacketType();
        if (this.packetTypes != null && !this.packetTypes.isEmpty() && !this.packetTypes.contains(pktType)) {
            return new FilterResult();
        }
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String username = (String)connectionContext.get("username");
        if (username != null && this.users != null && this.users.size() > 0 && !FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
            return new FilterResult();
        }
        final String clientIP = (String)connectionContext.get("clientIP");
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
        return "JavaScript request filter - MSSQL";
    }
    
    @Override
    public String[] getPacketTypes() {
        if (this.packetTypes == null) {
            return null;
        }
        return this.packetTypes.toArray(new String[0]);
    }
}
