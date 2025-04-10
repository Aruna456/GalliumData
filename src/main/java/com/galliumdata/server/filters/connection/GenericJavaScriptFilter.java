// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.connection;

import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import java.net.Socket;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ConnectionFilter;

public class GenericJavaScriptFilter implements ConnectionFilter
{
    private FilterUse filterUse;
    
    @Override
    public void configure(final FilterUse def) {
        this.filterUse = def;
    }
    
    @Override
    public FilterResult acceptConnection(final Socket socket, final Variables context) {
        final FilterResult result = new FilterResult();
        final Variables ctxt = new Variables();
        ctxt.put("socket", socket);
        ctxt.put("result", result);
        String clientAddress = socket.getRemoteSocketAddress().toString();
        if (clientAddress.startsWith("/")) {
            clientAddress = clientAddress.substring(1);
        }
        ctxt.put("clientIP", clientAddress);
        final Source src = ScriptManager.getInstance().getSource(this.filterUse.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, ctxt);
        return result;
    }
    
    @Override
    public String getName() {
        return "GenericJavaScriptFilter - connection";
    }
}
