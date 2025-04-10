// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.connection;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import java.util.Iterator;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import java.time.LocalDateTime;
import com.galliumdata.server.logic.FilterResult;
import java.net.Socket;
import com.galliumdata.server.adapters.Variables;
import java.util.HashSet;
import org.apache.logging.log4j.Logger;
import java.util.regex.Pattern;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ConnectionFilter;

public class AddressConnectionFilter implements ConnectionFilter
{
    private FilterUse filterUse;
    private Set<Pattern> addressPatterns;
    private Set<Pattern> deniedPatterns;
    private String deniedMessage;
    private boolean executeCode;
    private static final Logger log;
    
    public AddressConnectionFilter() {
        this.addressPatterns = new HashSet<Pattern>();
        this.deniedPatterns = new HashSet<Pattern>();
        this.executeCode = false;
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.filterUse = def;
        final Variables filterContext = def.getFilterContext();
        if (filterContext.containsKey("_initialized")) {
            this.addressPatterns = (Set)filterContext.get("addressPatterns");
            this.deniedPatterns = (Set)filterContext.get("deniedPatterns");
            this.deniedMessage = (String)filterContext.get("deniedMessage");
            this.executeCode = (boolean)filterContext.get("executeCode");
            return;
        }
        final String patternStr = (String) this.filterUse.getParameters().get("Addresses allowed");
        if (patternStr != null && patternStr.trim().length() > 0) {
            final String[] split;
            final String[] patternParts = split = patternStr.split(" ");
            for (final String patternPart : split) {
                this.addressPatterns.add(Pattern.compile(patternPart.trim()));
            }
            filterContext.put("addressPatterns", this.addressPatterns);
        }
        final String deniedPatternStr = (String) this.filterUse.getParameters().get("Addresses denied");
        if (deniedPatternStr != null && deniedPatternStr.trim().length() > 0) {
            final String[] split2;
            final String[] patternParts2 = split2 = deniedPatternStr.split(" ");
            for (final String patternPart2 : split2) {
                this.deniedPatterns.add(Pattern.compile(patternPart2.trim()));
            }
            filterContext.put("deniedPatterns", this.deniedPatterns);
        }
        this.deniedMessage = (String) this.filterUse.getParameters().get("Denied message");
        if (this.deniedMessage != null && this.deniedMessage.trim().length() > 0) {
            filterContext.put("deniedMessage", this.deniedMessage);
        }
        final Boolean runCode = (Boolean) this.filterUse.getParameters().get("Execute code");
        if (runCode == null) {
            this.executeCode = false;
        }
        else {
            this.executeCode = runCode;
        }
        filterContext.put("executeCode", this.executeCode);
        filterContext.put("_initialized", true);
    }
    
    @Override
    public FilterResult acceptConnection(final Socket socket, final Variables context) {
        context.getLog("log").debug("Filter " + this.filterUse.getName() + " is checking connection from " + socket.getRemoteSocketAddress().toString() + " for IP address");
        final FilterResult result = new FilterResult();
        final LocalDateTime now = LocalDateTime.now();
        final String dayOfWeek = now.getDayOfWeek().name();
        String clientAddress = socket.getRemoteSocketAddress().toString();
        if (clientAddress.startsWith("/")) {
            clientAddress = clientAddress.substring(1);
        }
        boolean allowed = false;
        for (final Pattern pat : this.addressPatterns) {
            if (pat.matcher(clientAddress).matches()) {
                allowed = true;
                break;
            }
        }
        if (this.addressPatterns.size() == 0) {
            allowed = true;
        }
        String msg = "Connection not allowed";
        if (!allowed) {
            if (this.deniedMessage != null) {
                msg = this.deniedMessage;
            }
            result.setSuccess(false);
            result.setErrorMessage(msg);
        }
        if (this.executeCode) {
            final Variables ctxt = new Variables();
            ctxt.put("socket", socket);
            ctxt.put("clientIP", clientAddress);
            ctxt.put("result", result);
            final Source src = ScriptManager.getInstance().getSource(this.filterUse.getPath().toString());
            ScriptExecutor.executeFilterScript(src, result, ctxt);
        }
        if (!result.isSuccess()) {
            context.getLog("log").debug("Filter " + this.filterUse.getName() + " is rejecting connection  from " + socket.getRemoteSocketAddress().toString() + " because: " + msg);
        }
        return result;
    }
    
    @Override
    public String getName() {
        return AddressConnectionFilter.class.getName();
    }
    
    static {
        log = LogManager.getLogger("galliumdata.uselog");
    }
}
