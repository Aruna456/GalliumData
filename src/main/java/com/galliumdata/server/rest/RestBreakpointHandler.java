// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.net.URI;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.debug.DebugManager;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.repository.Breakpoint;
import com.galliumdata.server.repository.RepositoryManager;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.handler.ProtocolData;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;

public class RestBreakpointHandler
{
    private static final Logger log;
    
    public static ProtocolData handleRequest(final HttpExchange exchange, final JsonNode payload) {
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 4) {
            throw new RuntimeException("Invalid URL for breakpoints: " + path);
        }
        final String projectName = pathParts[3];
        final Project project = RepositoryManager.getMainRepository().getProjects().get(projectName);
        if (project == null) {
            throw new RuntimeException("No such project for breakpoints: " + projectName);
        }
        final String method = exchange.getRequestMethod();
        if ("GET".equals(method)) {
            if (pathParts.length != 4) {
                throw new RuntimeException("Invalid URL for breakpoints GET: " + path);
            }
            return project.getBreakpointsAsJson();
        }
        else if ("POST".equals(method)) {
            if (pathParts.length != 4) {
                throw new RuntimeException("Invalid URL for breakpoints POST: " + path);
            }
            final JsonNode bpsNode = payload.get("breakpoints");
            if (bpsNode == null) {
                throw new RuntimeException("Ignoring POST to breakpoints because it does not have a breakpoints attribute");
            }
            if (!bpsNode.isArray()) {
                throw new RuntimeException("Ignoring POST to breakpoints because the breakpoints attribute is not an array");
            }
            for (int i = 0; i < bpsNode.size(); ++i) {
                final JsonNode bp = bpsNode.get(i);
                if (!bp.isObject()) {
                    throw new RuntimeException("Ignoring POST to breakpoints because a breakpoint is not an object");
                }
                final Breakpoint bPoint = new Breakpoint(bp.get("file").asText(), bp.get("line").asInt());
                if ("POST".equals(method)) {
                    RestBreakpointHandler.log.trace(Markers.REPO, "Adding new breakpoint to project " + projectName + " in " + bPoint.filename + " line " + bPoint.linenum);
                    project.addBreakpoint(bPoint);
                    if (DebugManager.debugIsActive()) {
                        ScriptExecutor.addBreakpoint(project, bPoint);
                    }
                }
                else {
                    RestBreakpointHandler.log.trace(Markers.REPO, "Removing breakpoint from project " + projectName + " in " + bPoint.filename + " line " + bPoint.linenum);
                    project.removeBreakpoint(bPoint);
                }
            }
            return project.getBreakpointsAsJson();
        }
        else {
            if (!"DELETE".equals(method)) {
                throw new RuntimeException("Unsupported method for breakpoints: " + method);
            }
            if (pathParts.length != 8) {
                throw new RuntimeException("Invalid URL for breakpoints DELETE: " + path);
            }
            final String codeType = pathParts[4];
            final String codeName = pathParts[5];
            final String fileName = pathParts[6];
            final int line = Integer.parseInt(pathParts[7]);
            final Breakpoint bp2 = new Breakpoint(codeType + "/" + codeName + "/" + fileName, line);
            project.removeBreakpoint(bp2);
            if (DebugManager.debugIsActive()) {
                ScriptExecutor.removeBreakpoint(project, bp2);
            }
            return null;
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
