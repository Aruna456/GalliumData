// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.request;

import org.graalvm.polyglot.Source;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;
import java.util.Iterator;
import java.util.List;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.RPCParameter;
import java.util.ArrayList;
import java.util.Collection;
import com.galliumdata.server.handler.mssql.RPCPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.logic.FilterUtils;
import java.util.HashSet;
import com.galliumdata.server.repository.FilterUse;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;
import com.galliumdata.server.filters.mssql.GeneralFilter;

public class PreparedStatementFilter extends GeneralFilter
{
    protected Set<Object> sqlPatterns;
    protected Set<Object> clientIps;
    protected Set<Object> users;
    private Map<String, Pattern> paramPatterns;
    private String andOr;
    
    public PreparedStatementFilter() {
        this.paramPatterns = new HashMap<String, Pattern>();
        this.andOr = "and";
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.sqlPatterns = new HashSet<Object>();
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        this.paramPatterns = new HashMap<String, Pattern>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.sqlPatterns = (Set<Object>) init.get("sqlPatterns");
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = (Set<Object>) init.get("users");
            if (init.containsKey("paramPatterns")) {
                this.paramPatterns = (Map<String, Pattern>) init.get("paramPatterns");
            }
            if (init.containsKey("andOr")) {
                this.andOr = init.get("andOr").toString();
            }
            return;
        }
        super.configure(def);
        init = (Map)filterContext.get("_initialized");
        final String questPatternStr = (String) def.getParameters().get("SQL patterns");
        init.put("sqlPatterns", this.sqlPatterns = FilterUtils.readCommaSeparatedNamesOrRegexes(questPatternStr));
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
                    PreparedStatementFilter.log.error(Markers.MSSQL, "Invalid value for Parameter patterns in filter " + def.getName() + ": not in the form name=value");
                    throw new RepositoryException("repo.BadProperty", new Object[] { "RPC filter - parameter patterns", pat });
                }
                nameVal[1] = nameVal[1].replaceAll("GA989796", "=");
                this.paramPatterns.put(nameVal[0], Pattern.compile(nameVal[1], 42));
            }
        }
        init.put("paramPatterns", this.paramPatterns);
        if (def.getParameters().containsKey("And/or")) {
            this.andOr = (String) def.getParameters().get("And/or");
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
        final String procName = rpcPacket.getProcName();
        if (!"Sp_PrepExec".equalsIgnoreCase(procName) && !"Sp_Execute".equalsIgnoreCase(procName)) {
            return new FilterResult();
        }
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        String sql = (String)connectionContext.get("lastSQL");
        sql = sql.replaceAll("@P\\d+", "?");
        if (!FilterUtils.stringMatchesNamesOrRegexes(sql, this.sqlPatterns)) {
            return new FilterResult();
        }
        final String username = (String)connectionContext.get("userName");
        if (!FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
            return new FilterResult();
        }
        final List<RPCParameter> params = new ArrayList<RPCParameter>(rpcPacket.getParameters());
        params.remove(0);
        if ("Sp_PrepExec".equalsIgnoreCase(procName)) {
            params.remove(0);
            params.remove(0);
        }
        boolean matches = true;
    Label_0723:
        for (Map.Entry<String, Pattern> entry : this.paramPatterns.entrySet()) {
            boolean userParamNameIsNumber = true;
            final String userParamName = entry.getKey();
            for (int i = 0; i < userParamName.length(); ++i) {
                final Character c = userParamName.charAt(i);
                if (!Character.isDigit(c)) {
                    userParamNameIsNumber = false;
                    break;
                }
            }
            if (userParamNameIsNumber) {
                final int paramNum = Integer.parseInt(userParamName);
                if (paramNum > params.size()) {
                    throw new ServerException("db.mssql.logic.InvalidRPCParamNum", new Object[] { paramNum, params.size(), this.getName() });
                }
                final RPCParameter param = params.get(paramNum - 1);
                final MSSQLDataType paramValue = param.getValue();
                if (paramValue == null || paramValue.isNull()) {
                    matches = "null".equalsIgnoreCase(entry.getValue().toString());
                }
                else {
                    matches = entry.getValue().matcher(paramValue.getValue().toString()).matches();
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
                for (RPCParameter param2 : params) {
                    String paramName = param2.getParamName();
                    if (paramName == null || paramName.isEmpty()) {
                        paramName = "" + paramNum;
                    }
                    if (paramName.equals(userParamName)) {
                        final MSSQLDataType paramValue2 = param2.getValue();
                        if (paramValue2 == null || paramValue2.isNull()) {
                            matches = "null".equalsIgnoreCase(entry.getValue().toString());
                        }
                        else {
                            matches = entry.getValue().matcher(paramValue2.getValue().toString()).matches();
                        }
                    }
                    if (!matches && "and".equals(this.andOr)) {
                        return new FilterResult();
                    }
                    if (matches && "or".equals(this.andOr)) {
                        break Label_0723;
                    }
                    ++paramNum;
                }
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
        return "Prepared statement filter - MSSQL";
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "RPC" };
    }
}
