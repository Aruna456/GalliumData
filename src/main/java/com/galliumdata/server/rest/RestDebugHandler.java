// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.net.URI;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.debug.DebugManager;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.ProtocolData;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;

public class RestDebugHandler
{
    private static final Logger log;
    
    public static ProtocolData handleRequest(final HttpExchange exchange) {
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 4) {
            throw new RuntimeException("Invalid URL for breakpoints: " + path);
        }
        final String command = pathParts[3];
        String status = "OK";
        final String method = exchange.getRequestMethod();
        if (!"GET".equals(method)) {
            RestDebugHandler.log.debug(Markers.SYSTEM, "debug command received, but was not GET: " + method);
            return null;
        }
        if ("startDebug".equals(command)) {
            if (DebugManager.debugIsActive()) {
                status = "startDebug command received, but already debugging";
                RestDebugHandler.log.debug(Markers.SYSTEM, status);
            }
            else {
                DebugManager.startDebug();
            }
        }
        else if ("stopDebug".equals(command)) {
            if (!DebugManager.debugIsActive()) {
                status = "stopDebug command received, but not currently debugging";
                RestDebugHandler.log.debug(Markers.SYSTEM, status);
            }
            else {
                DebugManager.stopDebug();
            }
        }
        else {
            if ("inDebugMode".equals(command)) {
                final ProtocolData node = new ProtocolDataObject();
                node.putBoolean("inDebugMode", DebugManager.debugIsActive());
                final ServerEvent currentEvent = ServerSentEventHandler.getCurrentEvent();
                if (currentEvent != null) {
                    node.putString("currentEventType", currentEvent.eventType);
                    node.putString("currentEvent", currentEvent.message);
                }
                return node;
            }
            if ("step".equals(command)) {
                if (!DebugManager.debugIsActive()) {
                    status = "step command received, but not currently debugging";
                    RestDebugHandler.log.debug(Markers.SYSTEM, status);
                }
                else {
                    DebugManager.step();
                }
            }
            else if ("go".equals(command)) {
                if (!DebugManager.debugIsActive()) {
                    status = "go command received, but not currently debugging";
                    RestDebugHandler.log.debug(Markers.SYSTEM, status);
                }
                else {
                    DebugManager.go();
                }
            }
            else if ("stepOut".equals(command)) {
                if (!DebugManager.debugIsActive()) {
                    status = "stepOut command received, but not currently debugging";
                    RestDebugHandler.log.debug(Markers.SYSTEM, status);
                }
                else {
                    DebugManager.stepOut();
                }
            }
            else if ("stepIn".equals(command)) {
                if (!DebugManager.debugIsActive()) {
                    status = "kill command received, but not currently debugging";
                    RestDebugHandler.log.debug(Markers.SYSTEM, status);
                }
                else {
                    DebugManager.stepIn();
                }
            }
            else {
                status = "Unknown debug command received: " + command;
                RestDebugHandler.log.debug(Markers.SYSTEM, status);
            }
        }
        final ProtocolData topNode = new ProtocolDataObject();
        topNode.putString("status", status);
        return topNode;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
