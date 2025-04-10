// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import java.net.Socket;
import java.net.InetSocketAddress;
import com.galliumdata.server.ServerException;
import java.net.InetAddress;
import java.time.ZonedDateTime;
import com.galliumdata.server.log.Markers;
import java.util.HashSet;
import javax.net.ssl.SSLContext;
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

public class MSSQLAdapter implements AdapterInterface
{
    private Project project;
    private Connection connection;
    private Map<String, Object> parameters;
    private AdapterCallback callback;
    private final Set<MSSQLConnectionHandler> liveHandlers;
    private final AdapterStatus status;
    private ServerSocket serverSocket;
    private boolean stopRequested;
    private final Variables adapterContext;
    protected int batchSizeRows;
    protected int batchSizeBytes;
    private static final Logger log;
    private static final int MAX_LIVE_HANDLERS = 1000;
    protected static final String PARAM_LOCAL_ADDRESS = "Local address";
    protected static final String PARAM_LOCAL_PORT = "Local port";
    protected static final String PARAM_SERVER_HOST = "Server host";
    protected static final String PARAM_SERVER_PORT = "Server port";
    protected static final String PARAM_TIMEOUT_TO_SERVER = "Timeout to server";
    protected static final String PARAM_TRUST_SERVER_CERT = "Trust server certificate";
    protected static final String PARAM_BATCH_SIZE_ROWS = "Result set batch size (rows)";
    protected static final String PARAM_BATCH_SIZE_BYTES = "Result set batch size (bytes)";
    protected SSLContext clientSSLContext;
    protected SSLContext serverSSLContext;
    public static final String CTXT_CONNECTION_CONTEXT = "connectionContext";
    
    public MSSQLAdapter() {
        this.liveHandlers = new HashSet<MSSQLConnectionHandler>();
        this.status = new AdapterStatus();
        this.stopRequested = false;
        this.adapterContext = new Variables();
        this.batchSizeRows = -1;
        this.batchSizeBytes = -1;
    }
    
    @Override
    public void initialize() {
    }
    
    @Override
    public boolean configure(final Project project, final Connection conn, final AdapterCallback callback) {
        MSSQLAdapter.log.trace(Markers.MSSQL, "MSSQL connection " + conn.getName() + " is being configured");
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
            MSSQLAdapter.log.trace(Markers.MSSQL, "Exception when closing serverSocket: " + ex.getMessage());
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
                MSSQLAdapter.log.warn(Markers.MSSQL, "Exception while shutting down MSSQL adapter: " + ex.getMessage());
            }
        }
    }
    
    @Override
    public AdapterStatus getStatus() {
        return this.status;
    }
    
    @Override
    public String getName() {
        return "MSSQL adapter - " + this.connection.getName();
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
            final Object obj = this.parameters.get("Result set batch size (rows)");
            if (obj == null || obj.toString().isBlank()) {
                this.batchSizeRows = -1;
            }
            else {
                if (!(obj instanceof Integer)) {
                    throw new ServerException("repo.BadProperty", new Object[] { "Result set batch size (rows)", "Result set batch size (rows) must be an integer" });
                }
                this.batchSizeRows = (int)obj;
            }
        }
        catch (final Exception ex2) {
            throw new ServerException("repo.BadProperty", new Object[] { "Result set batch size (rows)", ex2.getMessage() });
        }
        try {
            final Object obj = this.parameters.get("Result set batch size (bytes)");
            if (obj == null || obj.toString().isBlank()) {
                this.batchSizeBytes = -1;
            }
            else {
                if (!(obj instanceof Integer)) {
                    throw new ServerException("repo.BadProperty", new Object[] { "Result set batch size (bytes)", "Result set batch size (bytes) must be an integer" });
                }
                this.batchSizeBytes = (int)obj;
            }
        }
        catch (final Exception ex2) {
            throw new ServerException("repo.BadProperty", new Object[] { "Result set batch size (bytes)", ex2.getMessage() });
        }
        try {
            this.serverSocket = new ServerSocket(localPort, 1000, localAddress);
        }
        catch (final Exception ex2) {
            throw new ServerException("core.PortAlreadyTaken", new Object[] { localPort, this.project.getName() });
        }
        MSSQLAdapter.log.trace(Markers.MSSQL, "MSSQL adapter is now starting on local port " + localPort);
        while (!this.stopRequested) {
            Socket socket = null;
            try {
                socket = this.serverSocket.accept();
            }
            catch (final Exception ex3) {
                if (!"Socket closed".equals(ex3.getMessage())) {
                    MSSQLAdapter.log.error("MSSQL local socket got exception: " + ex3.getMessage());
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
            final InetSocketAddress remoteAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
            connectionContext.put("clientIP", remoteAddress.getHostString());
            connectionContext.put("clientPort", remoteAddress.getPort());
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
                final MSSQLConnectionHandler handler = new MSSQLConnectionHandler(socket, this, context, "MSSQLConnectionHandler");
                if (this.liveHandlers.size() > 1000) {
                    throw new ServerException("db.mssql.server.TooManyRequests", new Object[] { this.liveHandlers.size(), 1000 });
                }
                this.liveHandlers.add(handler);
                handler.start();
            }
        }
    }
    
    @Override
    public String testConnection(final Project project, final Connection conn) {
        final MSSQLConnectionHandler handler = new MSSQLConnectionHandler(this);
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
    
    public void handlerIsDone(final MSSQLConnectionHandler handler) {
        if (!this.liveHandlers.contains(handler)) {
            MSSQLAdapter.log.warn(Markers.MSSQL, "Unknown handler terminating");
            return;
        }
        this.getCallbackAdapter().connectionClosing(handler.connectionContext);
        this.liveHandlers.remove(handler);
    }
    
    protected synchronized void initializeServerSSLContext(final int serverMajorVersion) {
        if (this.serverSSLContext != null) {
            return;
        }
        try {
            if (serverMajorVersion == 11) {
                System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.0,TLSv1.1,TLSv1.2");
                this.serverSSLContext = SSLContext.getInstance("TLSv1.1");
            }
            else {
                this.serverSSLContext = SSLContext.getInstance("TLSv1.2");
            }
            final Object trustParam = this.getParameter("Trust server certificate");
            TrustManager[] trustManagers;
            if (trustParam == null || trustParam.equals(Boolean.FALSE)) {
                trustManagers = this.getProject().getTrustManagers();
            }
            else {
                trustManagers = Project.getCredulousTrustManagers();
            }
            this.serverSSLContext.init(this.getProject().getKeyManagers(), trustManagers, new SecureRandom());
        }
        catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    protected synchronized void intializeClientSSLContext() {
        if (this.clientSSLContext != null) {
            return;
        }
        try {
            (this.clientSSLContext = SSLContext.getInstance("TLSv1.2")).init(this.getProject().getKeyManagers(), this.getProject().getTrustManagers(), new SecureRandom());
        }
        catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
