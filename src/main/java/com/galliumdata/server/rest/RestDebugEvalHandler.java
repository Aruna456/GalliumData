// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.debug.DebuggerCallback;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.ProtocolData;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;

public class RestDebugEvalHandler
{
    private static final Logger log;
    
    public static ProtocolData handleRequest(final HttpExchange exchange, final JsonNode payload) {
        if (RestDebugEvalHandler.log.isTraceEnabled()) {
            RestDebugEvalHandler.log.trace(Markers.WEB, "Received REST request for debug eval: " + String.valueOf(payload));
        }
        if (!RestManager.checkAddress(exchange)) {
            return new ProtocolDataObject();
        }
        final JsonNode exprNode = payload.get("expression");
        if (exprNode == null || exprNode.isNull() || !exprNode.isTextual()) {
            return new ProtocolDataObject();
        }
        if (!DebuggerCallback.isSuspended()) {
            throw new RuntimeException("Cannot get debug eval if not suspended");
        }
        final String expr = exprNode.asText();
        return DebuggerCallback.evalExpression(expr);
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
