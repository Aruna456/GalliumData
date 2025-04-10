// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.io.IOException;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.galliumdata.server.debug.DebuggerCallback;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.ProtocolData;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;

public class RestDebugValueHandler
{
    private static final Logger log;
    
    public static ProtocolData handleRequest(final HttpExchange exchange) throws IOException {
        if (RestDebugValueHandler.log.isTraceEnabled()) {
            RestDebugValueHandler.log.trace(Markers.WEB, "Received REST request for debug value: " + exchange.getRequestMethod() + " : " + String.valueOf(exchange.getRequestURI()));
        }
        if (!RestManager.checkAddress(exchange)) {
            return new ProtocolDataObject();
        }
        final String method = exchange.getRequestMethod();
        final JsonNode node = null;
        if (!"GET".equals(method)) {
            throw new RuntimeException("Invalid debug value request method: " + method);
        }
        if (!DebuggerCallback.isSuspended()) {
            throw new RuntimeException("Cannot get debug value if not suspended");
        }
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 4) {
            throw new RuntimeException("Invalid URL for debug value: " + path);
        }
        final String varPath = pathParts[3];
        final String[] nameParts = varPath.split("\\.");
        final ProtocolDataObject debugVar = DebuggerCallback.getDebugVariable(nameParts);
        final ProtocolDataObject children = (ProtocolDataObject)debugVar.get("children");
        if (children != null) {
            children.sort();
        }
        return debugVar;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
