// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import org.apache.logging.log4j.LogManager;
import java.net.SocketTimeoutException;
import java.net.BindException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import com.galliumdata.server.ServerException;
import java.net.InetAddress;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;
import java.net.Socket;

public class HttpConnectionHandler extends Thread
{
    private final Socket clientSocket;
    protected Socket httpSocket;
    protected HttpAdapter adapter;
    protected Variables connectionContext;
    protected ConnectionState connectionState;
    private static final Logger log;
    
    public HttpConnectionHandler(final Socket clientSocket, final HttpAdapter adapter, final Variables ctxt, final String name) {
        super(name);
        this.connectionContext = new Variables();
        this.connectionState = new ConnectionState();
        this.clientSocket = clientSocket;
        this.adapter = adapter;
        this.connectionContext = (Variables)ctxt.get("connectionContext");
    }
    
    HttpConnectionHandler(final HttpAdapter adapter) {
        super("HTTP connection test for " + adapter.getConnection().getName());
        this.connectionContext = new Variables();
        this.connectionState = new ConnectionState();
        this.adapter = adapter;
        this.clientSocket = null;
    }
    
    @Override
    public void run() {
        HttpConnectionHandler.log.trace(Markers.HTTP, "HttpConnectionHandler is running");
        this.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        this.connectionState.setServerName((String)this.adapter.getParameter("Server host"));
        try {
            this.connectToServer();
        }
        catch (final Exception ex) {
            HttpConnectionHandler.log.error(Markers.HTTP, "Unable to connect to HTTP server: " + ex.getMessage());
            return;
        }
        ClientForwarder forwardFromClient;
        ServerForwarder forwardFromServer;
        try {
            forwardFromClient = new ClientForwarder(this.clientSocket, this.httpSocket, this.connectionContext, this.connectionState);
            forwardFromServer = new ServerForwarder(this.httpSocket, this.clientSocket, this.connectionContext, this.connectionState);
        }
        catch (final Exception ex2) {
            HttpConnectionHandler.log.error(Markers.HTTP, "Unable to start connection handler for {}/{} because of exception while launching forwarders: {}", (Object)this.adapter.getProject().getName(), (Object)this.adapter.getConnection().getName(), (Object)ex2.getMessage());
            this.cleanup();
            return;
        }
        forwardFromClient.setOtherForwarder(forwardFromServer);
        forwardFromClient.setAdapter(this.adapter);
        forwardFromServer.setOtherForwarder(forwardFromClient);
        forwardFromServer.setAdapter(this.adapter);
        final Thread forwardFromClientThread = new Thread(forwardFromClient, "forwardFromClientThread");
        forwardFromClientThread.start();
        forwardFromServer.thread = Thread.currentThread();
        try {
            Thread.sleep(1000L);
        }
        catch (final Exception ex3) {
            ex3.printStackTrace();
        }
        forwardFromServer.run();
        this.cleanup();
        forwardFromClient.requestStop();
        if (HttpConnectionHandler.log.isTraceEnabled()) {
            HttpConnectionHandler.log.trace(Markers.HTTP, "HttpConnectionHandler is done with this connection: client " + this.clientSocket.toString() + ", server " + this.httpSocket.toString() + ", connection " + this.adapter.getConnection().getName());
        }
    }
    
    public void connectToServer() {
        final String serverName = (String)this.adapter.getParameter("Server host");
        if (null == serverName || serverName.trim().length() == 0) {
            throw new RepositoryException("repo.MissingProperty", new Object[] { "Server host" });
        }
        InetAddress remoteAddress;
        try {
            remoteAddress = InetAddress.getByName(serverName);
        }
        catch (final Exception ex) {
            HttpConnectionHandler.log.error(Markers.HTTP, "Bad property 'Server host' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadHost", new Object[] { ex.getMessage() });
        }
        int remotePort;
        try {
            remotePort = (int)this.adapter.getParameter("Server port");
        }
        catch (final Exception ex2) {
            HttpConnectionHandler.log.error(Markers.HTTP, "Bad property 'Server port' in repository: " + ex2.getMessage());
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
            HttpConnectionHandler.log.error(Markers.HTTP, "Bad property 'Timeout to server' in repository, assuming default of 15000: " + ex3.getMessage());
            timeout = 15000;
        }
        try {
            this.httpSocket = new Socket();
            final InetSocketAddress sockAddr = new InetSocketAddress(remoteAddress, remotePort);
            this.httpSocket.connect(sockAddr, timeout);
        }
        catch (final BindException bex) {
            HttpConnectionHandler.log.error(Markers.HTTP, "Unable to bind to port " + remotePort + " : " + bex.getMessage());
            this.cleanup();
            throw new ServerException("db.http.server.CannotConnectToServer", new Object[] { serverName, remotePort, bex.getMessage() });
        }
        catch (final SocketTimeoutException ste) {
            HttpConnectionHandler.log.error(Markers.HTTP, "Timeout while trying to connect to HTTP port " + remotePort + " : " + ste.getMessage());
            this.cleanup();
            throw new ServerException("db.http.server.CannotConnectToServer", new Object[] { serverName, remotePort, ste.getMessage() });
        }
        catch (final Exception ex3) {
            ex3.printStackTrace();
            this.cleanup();
            throw new ServerException("db.http.server.CannotConnectToServer", new Object[] { serverName, remotePort, ex3.getMessage() });
        }
    }
    
    protected void cleanup() {
        try {
            this.clientSocket.close();
        }
        catch (final Exception ex) {
            HttpConnectionHandler.log.warn(Markers.HTTP, "Exception closing socket from HTTP client: " + ex.getMessage());
        }
        try {
            this.httpSocket.close();
        }
        catch (final Exception ex) {
            HttpConnectionHandler.log.warn(Markers.HTTP, "Exception closing socket to HTTP: " + ex.getMessage());
        }
        this.adapter.handlerIsDone(this);
    }
    
    protected void cleanupTestHandler() {
        try {
            this.httpSocket.close();
        }
        catch (final Exception ex) {
            HttpConnectionHandler.log.trace(Markers.HTTP, "Exception closing socket to HTTP server for connection test: " + ex.getMessage());
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
