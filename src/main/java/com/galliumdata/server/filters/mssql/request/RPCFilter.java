// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.request;

import org.graalvm.polyglot.Source;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;

import java.util.*;

import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.handler.mssql.RPCParameter;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.util.StringUtil;
import com.galliumdata.server.handler.mssql.RPCPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.logic.FilterUtils;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.filters.mssql.GeneralFilter;

public class RPCFilter extends GeneralFilter
{
    protected Set<Object> rpcPatterns;
    protected Set<Object> clientIps;
    protected Set<Object> users;
    private Map<String, List<Object>> paramPatterns;
    private String andOr;
    
    public RPCFilter() {
        this.paramPatterns = new HashMap<String, List<Object>>();
        this.andOr = "and";
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.rpcPatterns = new HashSet<Object>();
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.rpcPatterns = Collections.singleton(init.get("rpcPatterns"));
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = (Set<Object>) init.get("users");
            if (init.containsKey("paramPatterns")) {
                this.paramPatterns = (Map<String, List<Object>>) init.get("paramPatterns");
            }
            if (init.containsKey("andOr")) {
                this.andOr = (String) init.get("andOr");
            }
            return;
        }
        super.configure(def);
        init = (Map)filterContext.get("_initialized");
        final String questPatternStr = (String) def.getParameters().get("Stored procedure name");
        init.put("rpcPatterns", this.rpcPatterns = FilterUtils.readCommaSeparatedNamesOrRegexes(questPatternStr));
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
        final String usersStr = (String) def.getParameters().get("Users");
        init.put("users", this.users = FilterUtils.readCommaSeparatedNamesOrRegexes(usersStr));
        final String paramPatStr = (String) def.getParameters().get("Parameter patterns");
        if (paramPatStr != null && paramPatStr.trim().length() > 0) {
            final String[] split;
            final String[] paramPats = split = paramPatStr.split("\\v");
            for (String pat : split) {
                pat = pat.replaceAll("\\\\=", "GA989796");
                final String[] nameVal = pat.split("=");
                if (nameVal.length != 2) {
                    RPCFilter.log.error(Markers.MSSQL, "Invalid value for Parameter patterns in filter " + def.getName() + ": not in the form name=value");
                    throw new RepositoryException("repo.BadProperty", new Object[] { "RPC filter - parameter patterns", pat });
                }
                nameVal[1] = nameVal[1].replaceAll("GA989796", "=");
                this.paramPatterns.put(nameVal[0], FilterUtils.readListOfNamesOrRegexes(nameVal[1]));
            }
        }
        init.put("paramPatterns", this.paramPatterns);
        if (def.getParameters().containsKey("And/or")) {
            this.andOr = def.getParameters().get("And/or").toString();
            if (!"and".equals(this.andOr) && !"or".equals(this.andOr)) {
                throw new RepositoryException("repo.BadProperty", new Object[] { "RPC filter - paramsAndOr", this.andOr });
            }
            init.put("andOr", this.andOr);
        }
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        if (super.skipInvocation(context)) {
            return new FilterResult();
        }
        final RPCPacket rpcPacket = (RPCPacket)context.get("packet");
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        final String spName = rpcPacket.getProcName();
        if (spName != null && this.rpcPatterns != null && this.rpcPatterns.size() == 1 && this.rpcPatterns.iterator().next() instanceof String) {
            final String s = (String) this.rpcPatterns.iterator().next();
            if (!spName.equalsIgnoreCase(s)) {
                return new FilterResult();
            }
        }
        else if (!FilterUtils.stringMatchesNamesOrRegexes(spName, this.rpcPatterns)) {
            return new FilterResult();
        }
        final String username = (String)connectionContext.get("userName");
        if (!FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
            return new FilterResult();
        }
        final List<RPCParameter> params = rpcPacket.getParameters();
        boolean matches = true;
    Label_0683:
        for (Map.Entry<String, List<Object>> entry : this.paramPatterns.entrySet()) {
            final String userParamName = entry.getKey();
            if (StringUtil.stringIsInteger(userParamName)) {
                final int paramNum = Integer.parseInt(userParamName);
                if (paramNum > params.size()) {
                    throw new ServerException("db.mssql.logic.InvalidRPCParamNum", new Object[] { paramNum, params.size(), spName });
                }
                final RPCParameter param = params.get(paramNum - 1);
                final MSSQLDataType paramValue = param.getValue();
                if (paramValue == null || paramValue.isNull()) {
                    matches = "null".equalsIgnoreCase(entry.getValue().toString());
                }
                else {
                    matches = FilterUtils.stringMatchesNamesOrRegexes(paramValue.getValue().toString(), entry.getValue());
                }
                if (!matches && "and".equals(this.andOr)) {
                    return new FilterResult();
                }
                if (matches && "or".equals(this.andOr)) {
                    break;
                }
                continue;
            }
            else {
                int paramNum = 1;
                boolean paramFound = false;
                for (RPCParameter param2 : params) {
                    String paramName = param2.getParamName();
                    if (paramName == null || paramName.isEmpty()) {
                        paramName = "" + paramNum;
                    }
                    if (!paramName.equals(userParamName)) {
                        continue;
                    }
                    paramFound = true;
                    final MSSQLDataType paramValue2 = param2.getValue();
                    if (paramValue2 == null || paramValue2.isNull()) {
                        matches = "null".equalsIgnoreCase(entry.getValue().toString());
                    }
                    else {
                        matches = FilterUtils.stringMatchesNamesOrRegexes(paramValue2.getValue().toString(), entry.getValue());
                    }
                    if (!matches && "and".equals(this.andOr)) {
                        return new FilterResult();
                    }
                    if (matches && "or".equals(this.andOr)) {
                        break Label_0683;
                    }
                    ++paramNum;
                }
                if (!paramFound) {
                    return new FilterResult();
                }
                continue;
            }
        }
        if (!matches) {
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
        return "RPC filter - MSSQL";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "RPC" };
    }
}
