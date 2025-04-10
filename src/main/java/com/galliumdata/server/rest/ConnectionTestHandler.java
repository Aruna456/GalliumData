// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.net.URI;
import com.galliumdata.server.adapters.AdapterCallback;
import com.galliumdata.server.adapters.AdapterManager;
import com.galliumdata.server.Main;
import com.galliumdata.server.adapters.AdapterInterface;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.RepositoryManager;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.ProtocolData;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;

public class ConnectionTestHandler
{
    private static final Logger log;
    
    public static ProtocolData handleRequest(final HttpExchange exchange) {
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 6) {
            throw new RuntimeException("Invalid URL for connections: " + path);
        }
        final String command = pathParts[3];
        if (!"test".equals(command)) {
            ConnectionTestHandler.log.debug("Unknown command for connection test: " + command);
            throw new ServerException("connection.NoSuchCommand", new Object[] { command });
        }
        final String projectName = pathParts[4];
        final Project project = RepositoryManager.getMainRepository().getProjects().get(projectName);
        if (project == null) {
            ConnectionTestHandler.log.debug("Request for connection test for non-existent project: " + projectName);
            throw new ServerException("connection.NoSuchProject", new Object[] { projectName });
        }
        final String connectionName = pathParts[5];
        final Connection conn = project.getConnections().get(connectionName);
        if (conn == null) {
            ConnectionTestHandler.log.debug("Request for connection test for non-existent connection {} in project {}", (Object)connectionName, (Object)projectName);
            throw new ServerException("connection.NoSuchConnection", new Object[] { connectionName, projectName });
        }
        final ProtocolData topNode = new ProtocolDataObject();
        String msg;
        if (conn.isActive()) {
            final AdapterInterface adapter = Main.getRunningAdapters().get(conn);
            if (adapter == null) {
                msg = "Unable to find active connection";
            }
            else {
                msg = adapter.testConnection(project, conn);
            }
        }
        else {
            final AdapterInterface adapter = AdapterManager.getInstance().instantiateAdapter(conn.getAdapterType());
            try {
                adapter.configure(project, conn, null);
                msg = adapter.testConnection(project, conn);
            }
            catch (final Exception ex) {
                msg = ex.getMessage();
            }
        }
        if (msg == null) {
            topNode.putString("status", "OK");
        }
        else {
            topNode.putString("status", "Error");
            topNode.putString("message", msg);
        }
        return topNode;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
