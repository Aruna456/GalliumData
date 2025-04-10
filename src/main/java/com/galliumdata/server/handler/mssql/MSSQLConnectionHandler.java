// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import java.net.SocketTimeoutException;
import java.net.BindException;
import java.net.SocketAddress;
import com.galliumdata.server.ServerException;
import java.net.InetAddress;
import com.galliumdata.server.repository.RepositoryException;
import java.net.InetSocketAddress;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;
import java.net.Socket;

public class MSSQLConnectionHandler extends Thread
{
    private final Socket clientSocket;
    protected Socket mssqlSocket;
    protected MSSQLAdapter adapter;
    protected Variables connectionContext;
    protected ConnectionState connectionState;
    private static final Logger log;
    
    public MSSQLConnectionHandler(final Socket clientSocket, final MSSQLAdapter adapter, final Variables ctxt, final String name) {
        super(name);
        this.connectionContext = new Variables();
        this.connectionState = new ConnectionState();
        this.clientSocket = clientSocket;
        this.adapter = adapter;
        this.connectionContext = (Variables)ctxt.get("connectionContext");
        this.connectionState.connectionContext = this.connectionContext;
    }
    
    MSSQLConnectionHandler(final MSSQLAdapter adapter) {
        super("MSSQL connection test for " + adapter.getConnection().getName());
        this.connectionContext = new Variables();
        this.connectionState = new ConnectionState();
        this.adapter = adapter;
        this.clientSocket = null;
    }
    
    @Override
    public void run() {
        MSSQLConnectionHandler.log.trace(Markers.MSSQL, "MSSQLConnectionHandler is running");
        this.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        this.connectionState.setServerName((String)this.adapter.getParameter("Server host"));
        try {
            this.connectToServer();
        }
        catch (final Exception ex) {
            MSSQLConnectionHandler.log.error(Markers.MSSQL, "Unable to connect to MSSQL server: " + ex.getMessage());
            if (MSSQLConnectionHandler.log.isDebugEnabled()) {
                ex.printStackTrace();
            }
            return;
        }
        ClientForwarder forwardFromClient;
        ServerForwarder forwardFromServer;
        try {
            forwardFromClient = new ClientForwarder(this.clientSocket, this.mssqlSocket, this.connectionContext, this.connectionState);
            forwardFromServer = new ServerForwarder(this.mssqlSocket, this.clientSocket, this.connectionContext, this.connectionState);
        }
        catch (final Exception ex2) {
            MSSQLConnectionHandler.log.error(Markers.MSSQL, "Unable to start connection handler for {}/{} because of exception while launching forwarders: {}", (Object)this.adapter.getProject().getName(), (Object)this.adapter.getConnection().getName(), (Object)ex2.getMessage());
            this.cleanup();
            return;
        }
        forwardFromClient.setOtherForwarder(forwardFromServer);
        forwardFromClient.setAdapter(this.adapter);
        forwardFromServer.setOtherForwarder(forwardFromClient);
        forwardFromServer.setAdapter(this.adapter);
        this.connectionContext.put("userIP", ((InetSocketAddress)this.clientSocket.getRemoteSocketAddress()).getAddress().toString());
        this.connectionContext.put("clientForwarder", forwardFromClient);
        this.connectionContext.put("serverForwarder", forwardFromServer);
        final Thread forwardFromClientThread = new Thread(forwardFromClient, "forwardFromClientThread");
        forwardFromClientThread.start();
        forwardFromServer.thread = Thread.currentThread();
        forwardFromServer.run();
        this.cleanup();
        forwardFromClient.requestStop();
        if (MSSQLConnectionHandler.log.isTraceEnabled()) {
            MSSQLConnectionHandler.log.trace(Markers.MSSQL, "MSSQLConnectionHandler is done with this connection: client " + this.clientSocket.toString() + ", server " + this.mssqlSocket.toString() + ", connection " + this.adapter.getConnection().getName());
        }
    }
    
    public void connectToServer() {
        this.connectionState.setConnectionName(this.adapter.getName());
        final String serverName = (String)this.adapter.getParameter("Server host");
        if (null == serverName || serverName.trim().isEmpty()) {
            throw new RepositoryException("repo.MissingProperty", new Object[] { "Server host" });
        }
        try {
            final InetAddress remoteAddress = InetAddress.getByName(serverName);
        }
        catch (final Exception ex) {
            MSSQLConnectionHandler.log.error(Markers.MSSQL, "Bad property 'Server host' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadHost", new Object[] { ex.getMessage() });
        }
        int remotePort;
        try {
            remotePort = (int)this.adapter.getParameter("Server port");
        }
        catch (final Exception ex2) {
            MSSQLConnectionHandler.log.error(Markers.MSSQL, "Bad property 'Server port' in repository: " + ex2.getMessage());
            throw new ServerException("repo.BadProperty", new Object[] { "Server port", ex2.getMessage() });
        }
        Integer timeout;
        try {
            timeout = (Integer)this.adapter.getParameter("Timeout to server");
            if (timeout == null) {
                timeout = 15000;
            }
        }
        catch (final Exception ex3) {
            MSSQLConnectionHandler.log.error(Markers.MSSQL, "Bad property 'Timeout to server' in repository, assuming default of 15000: " + ex3.getMessage());
            timeout = 15000;
        }
        try {
            this.mssqlSocket = new Socket();
            final InetSocketAddress sockAddr = new InetSocketAddress(serverName, remotePort);
            this.mssqlSocket.connect(sockAddr, timeout);
        }
        catch (final BindException bex) {
            MSSQLConnectionHandler.log.error(Markers.MSSQL, "Unable to bind to port " + remotePort + " : " + bex.getMessage());
            this.cleanup();
            throw new ServerException("db.mssql.server.CannotConnectToServer", new Object[] { serverName, remotePort, bex.getMessage() });
        }
        catch (final SocketTimeoutException ste) {
            MSSQLConnectionHandler.log.error(Markers.MSSQL, "Timeout while trying to connect to MSSQL port " + remotePort + " : " + ste.getMessage());
            this.cleanup();
            throw new ServerException("db.mssql.server.CannotConnectToServer", new Object[] { serverName, remotePort, ste.getMessage() });
        }
        catch (final Exception ex3) {
            if (MSSQLConnectionHandler.log.isDebugEnabled()) {
                ex3.printStackTrace();
            }
            this.cleanup();
            throw new ServerException("db.mssql.server.CannotConnectToServer", new Object[] { serverName, remotePort, ex3.getMessage() });
        }
    }
    
    protected void cleanup() {
        try {
            this.clientSocket.close();
        }
        catch (final Exception ex) {
            MSSQLConnectionHandler.log.warn(Markers.MSSQL, "Exception closing socket from MSSQL client: " + ex.getMessage());
        }
        try {
            this.mssqlSocket.close();
        }
        catch (final Exception ex) {
            MSSQLConnectionHandler.log.warn(Markers.MSSQL, "Exception closing socket to MSSQL: " + ex.getMessage());
        }
        this.adapter.handlerIsDone(this);
    }
    
    protected void cleanupTestHandler() {
        try {
            this.mssqlSocket.close();
        }
        catch (final Exception ex) {
            MSSQLConnectionHandler.log.trace(Markers.MSSQL, "Exception closing socket to MSSQL for connection test: " + ex.getMessage());
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
