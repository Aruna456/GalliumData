// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic.builtin;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.locale.I18nManager;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import java.net.Socket;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ConnectionFilter;

public class ConnectionAddressFilter implements ConnectionFilter
{
    private FilterUse def;
    public static final String PARAM_ACCEPT_ADDRESSES = "acceptAddresses";
    public static final String PARAM_REJECT_ADDRESSES = "rejectAddresses";
    public static final String PARAM_USE_SCRIPT = "useScript";
    private static final Logger log;
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
    }
    
    @Override
    public FilterResult acceptConnection(final Socket socket, final Variables context) {
        final FilterResult result = new FilterResult();
        result.setSuccess(true);
        result.setFilterName(this.def.getName());
        final String acceptAddresses = (String) this.def.getParameters().get("acceptAddresses");
        if (acceptAddresses != null && acceptAddresses.trim().length() > 0) {
            final InetAddress addr = ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress();
            InetAddress addrOk;
            try {
                addrOk = InetAddress.getByName(acceptAddresses);
            }
            catch (final Exception ex) {
                result.setSuccess(false);
                result.setErrorMessage(ex.getMessage());
                return result;
            }
            if (!addrOk.equals(addr)) {
                result.setSuccess(false);
                result.setErrorMessage(I18nManager.getString("logic.ConnectionNotAllowed", addr.getHostAddress()));
            }
        }
        final String rejectAddresses = (String) this.def.getParameters().get("rejectAddresses");
        if (rejectAddresses != null && rejectAddresses.trim().length() > 0) {
            final InetAddress addr2 = ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress();
            InetAddress badAddr;
            try {
                badAddr = InetAddress.getByName(rejectAddresses);
            }
            catch (final Exception ex2) {
                result.setSuccess(false);
                result.setErrorMessage(ex2.getMessage());
                return result;
            }
            if (badAddr.equals(addr2)) {
                result.setSuccess(false);
                result.setErrorMessage(I18nManager.getString("logic.ConnectionNotAllowed", addr2.getHostAddress()));
            }
        }
        final Boolean useScript = (Boolean) this.def.getParameters().get("useScript");
        if (useScript != null && useScript) {
            final Source src = ScriptManager.getInstance().getSource(this.def.getPath().toString());
            final Variables ctxt = new Variables();
            ctxt.put("socket", socket);
            ctxt.put("result", result);
            ScriptExecutor.executeFilterScript(src, result, ctxt);
            ConnectionAddressFilter.log.debug("Script has message: {}", (Object)result.getErrorMessage());
        }
        return result;
    }
    
    @Override
    public String getName() {
        return ConnectionAddressFilter.class.getName();
    }
    
    static {
        log = LogManager.getLogger("galliumdata.uselog");
    }
}
