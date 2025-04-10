// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import java.net.Socket;
import com.galliumdata.server.ServerException;
import java.net.InetAddress;
import java.time.ZonedDateTime;
import com.galliumdata.server.log.Markers;
import java.util.HashSet;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;
import java.net.ServerSocket;
import com.galliumdata.server.adapters.AdapterStatus;
import java.util.Set;
import com.galliumdata.server.adapters.AdapterCallback;
import java.util.Map;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.adapters.AdapterInterface;

public class HttpAdapter implements AdapterInterface
{
    private Project project;
    private Connection connection;
    private Map<String, Object> parameters;
    private AdapterCallback callback;
    private final Set<HttpConnectionHandler> liveHandlers;
    private final AdapterStatus status;
    private ServerSocket serverSocket;
    private boolean stopRequested;
    private final Variables adapterContext;
    private static final int MAX_LIVE_HANDLERS = 1000;
    protected static final String PARAM_LOCAL_ADDRESS = "Local address";
    protected static final String PARAM_LOCAL_PORT = "Local port";
    protected static final String PARAM_SERVER_HOST = "Server host";
    protected static final String PARAM_SERVER_PORT = "Server port";
    protected static final String PARAM_TIMEOUT_TO_SERVER = "Timeout to server";
    protected static final String PARAM_TRUST_SERVER_CERT = "Trust server certificate";
    public static final String CTXT_CONNECTION_CONTEXT = "connectionContext";
    private static final Logger log;
    
    public HttpAdapter() {
        this.liveHandlers = new HashSet<HttpConnectionHandler>();
        this.status = new AdapterStatus();
        this.stopRequested = false;
        this.adapterContext = new Variables();
    }
    
    @Override
    public void initialize() {
    }
    
    @Override
    public boolean configure(final Project project, final Connection conn, final AdapterCallback callback) {
        HttpAdapter.log.trace(Markers.HTTP, "HTTP connection " + conn.getName() + " is being configured");
        this.project = project;
        this.connection = conn;
        this.parameters = conn.getParameters();
        this.callback = callback;
        return true;
    }
    
    @Override
    public void stopProcessing() {
        this.stopRequested = true;
        try {
            this.serverSocket.close();
        }
        catch (final Exception ex) {
            HttpAdapter.log.trace(Markers.HTTP, "Exception when closing serverSocket: " + ex.getMessage());
        }
    }
    
    @Override
    public void switchProject(final Project project, final Connection conn) {
        this.project = project;
        this.parameters = conn.getParameters();
        this.callback.resetCache();
        this.callback.setProject(project);
    }
    
    @Override
    public void shutdown() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            }
            catch (final Exception ex) {
                HttpAdapter.log.warn(Markers.HTTP, "Exception while shutting down HTTP adapter: " + ex.getMessage());
            }
        }
    }
    
    @Override
    public AdapterStatus getStatus() {
        return this.status;
    }
    
    @Override
    public String getName() {
        return "HTTP adapter - " + this.connection.getName();
    }
    
    @Override
    public String testConnection(final Project project, final Connection conn) {
        final HttpConnectionHandler handler = new HttpConnectionHandler(this);
        try {
            handler.connectToServer();
            return null;
        }
        catch (final Exception ex) {
            return ex.getMessage();
        }
        finally {
            handler.cleanupTestHandler();
        }
    }
    
    @Override
    public void run() {
        this.status.startTime = ZonedDateTime.now();
        String localAddressStr = (String) this.parameters.get("Local address");
        if (null == localAddressStr || localAddressStr.trim().length() == 0) {
            localAddressStr = "0.0.0.0";
        }
        InetAddress localAddress;
        try {
            localAddress = InetAddress.getByName(localAddressStr);
        }
        catch (final Exception ex) {
            throw new ServerException("repo.BadHost", new Object[] { ex.getMessage() });
        }
        int localPort;
        try {
            final Object obj = this.parameters.get("Local port");
            if (obj == null) {
                throw new ServerException("repo.BadProperty", new Object[] { "Local port", "Local port must be specified" });
            }
            if (!(obj instanceof Integer)) {
                throw new ServerException("repo.BadProperty", new Object[] { "Local port", "Local port must be an integer" });
            }
            localPort = (int)obj;
        }
        catch (final Exception ex2) {
            throw new ServerException("repo.BadProperty", new Object[] { "Local port", ex2.getMessage() });
        }
        try {
            this.serverSocket = new ServerSocket(localPort, 1000, localAddress);
        }
        catch (final Exception ex2) {
            throw new ServerException("core.PortAlreadyTaken", new Object[] { localPort, this.project.getName() });
        }
        HttpAdapter.log.trace(Markers.HTTP, "HTTP adapter is now starting on local port " + localPort);
        while (!this.stopRequested) {
            Socket socket = null;
            try {
                socket = this.serverSocket.accept();
            }
            catch (final Exception ex3) {
                if (!"Socket closed".equals(ex3.getMessage())) {
                    HttpAdapter.log.error("HTTP local socket got exception: " + ex3.getMessage());
                }
                try {
                    if (socket != null) {
                        socket.close();
                    }
                }
                catch (final Exception ex4) {
                    ex3.printStackTrace();
                }
                break;
            }
            Variables context = new Variables();
            context.put("connectionParameters", this.parameters);
            final Variables connectionContext = new Variables();
            context.put("connectionContext", connectionContext);
            final AdapterCallbackResponse callbackResponse = this.callback.connectionRequested(socket, context);
            if (callbackResponse.reject) {
                try {
                    socket.close();
                }
                catch (final Exception ex5) {
                    ex5.printStackTrace();
                }
            }
            else {
                context = new Variables();
                context.put("connectionContext", connectionContext);
                final HttpConnectionHandler handler = new HttpConnectionHandler(socket, this, context, "HttpConnectionHandler");
                if (this.liveHandlers.size() > 1000) {
                    throw new ServerException("db.http.server.TooManyRequests", new Object[] { this.liveHandlers.size(), 1000 });
                }
                this.liveHandlers.add(handler);
                handler.start();
            }
        }
    }
    
    public Object getParameter(final String name) {
        return this.parameters.get(name);
    }
    
    public AdapterCallback getCallbackAdapter() {
        return this.callback;
    }
    
    public Project getProject() {
        return this.project;
    }
    
    public Connection getConnection() {
        return this.connection;
    }
    
    public void handlerIsDone(final HttpConnectionHandler handler) {
        if (!this.liveHandlers.contains(handler)) {
            HttpAdapter.log.warn(Markers.HTTP, "Unknown handler terminating");
            return;
        }
        this.getCallbackAdapter().connectionClosing(handler.connectionContext);
        this.liveHandlers.remove(handler);
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
