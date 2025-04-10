// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.repository.RepositoryException;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import java.security.SecureRandom;
import org.eclipse.jetty.util.thread.ThreadPool;
import java.time.ZonedDateTime;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.net.ServerSocket;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.server.Server;
import com.galliumdata.server.adapters.AdapterStatus;
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
    private final AdapterStatus status;
    private Server jettyServer;
    private QueuedThreadPool jettyThreadPool;
    private ServerConnector serverConnector;
    private RequestHandlerJetty jettyHandler;
    private InetAddress serverAddress;
    private int serverPort;
    private boolean httpsWithServer;
    private boolean trustServerCertificate;
    private String localHostName;
    private InetAddress localAddress;
    private int localPort;
    private boolean httpsWithClients;
    private boolean stopRequested;
    protected SSLContext clientSSLContext;
    protected SSLContext serverSSLContext;
    private static final Logger log;
    protected static final String PARAM_SERVER_ADDRESS = "Server address";
    protected static final String PARAM_SERVER_PORT = "Server port";
    protected static final String PARAM_SERVER_HTTPS = "Use HTTPS with server";
    protected static final String PARAM_SERVER_TRUST_CERT = "Trust server certificate";
    protected static final String PARAM_LOCAL_ADDRESS = "Local address";
    protected static final String PARAM_LOCAL_PORT = "Local port";
    protected static final String PARAM_CLIENTS_HTTPS = "Use HTTPS with clients";
    public static final String CTXT_CONNECTION_CONTEXT = "connectionContext";
    public static final String ALL_METHODS = "<all>";
    
    public HttpAdapter() {
        this.status = new AdapterStatus();
        this.stopRequested = false;
    }
    
    @Override
    public void initialize() {
    }
    
    @Override
    public boolean configure(final Project project, final Connection conn, final AdapterCallback callback) {
        HttpAdapter.log.trace(Markers.HTTP, "HTTP connection \"" + conn.getName() + "\" is being configured");
        this.project = project;
        this.connection = conn;
        this.parameters = conn.getParameters();
        this.callback = callback;
        this.readConfiguration();
        this.waitForLocalPort();
        return true;
    }
    
    @Override
    public void stopProcessing() {
        this.stopRequested = true;
    }
    
    @Override
    public void switchProject(final Project project, final Connection conn) {
        this.project = project;
        this.parameters = conn.getParameters();
        this.callback.resetCache();
        this.callback.setProject(project);
        HttpAdapter.log.trace(Markers.HTTP, "Stopping HTTP connection \"" + this.connection.getName() + "\" for restart");
        try {
            this.serverConnector.setShutdownIdleTimeout(1L);
            this.serverConnector.stop();
            this.serverConnector.destroy();
            this.jettyServer.setStopTimeout(1L);
            this.jettyServer.stop();
            this.jettyServer.destroy();
            this.jettyThreadPool.setStopTimeout(1L);
            this.jettyThreadPool.stop();
            this.jettyThreadPool.destroy();
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            throw new ServerException("db.http.server.ErrorStoppingHTTPServer", new Object[] { this.connection.getName(), ex });
        }
        this.jettyServer = null;
        this.run();
        HttpAdapter.log.trace(Markers.HTTP, "HTTP connection \"" + this.connection.getName() + "\" has restarted");
    }
    
    @Override
    public void shutdown() {
        HttpAdapter.log.trace(Markers.HTTP, "HTTP connection \"" + this.connection.getName() + "\" is stopping");
        try {
            this.serverConnector.setShutdownIdleTimeout(1L);
            this.serverConnector.stop();
            this.serverConnector.destroy();
            this.jettyServer.setStopTimeout(1L);
            this.jettyServer.stop();
            this.jettyServer.destroy();
            this.jettyThreadPool.setStopTimeout(1L);
            this.jettyThreadPool.stop();
            this.jettyThreadPool.destroy();
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            throw new ServerException("db.http.server.ErrorStoppingHTTPServer", new Object[] { this.connection.getName(), ex });
        }
        HttpAdapter.log.trace(Markers.HTTP, "HTTP connection \"" + this.connection.getName() + "\" has stopped");
    }
    
    private void waitForLocalPort() {
        final int MAX_WAIT = 3000;
        final long startTime = System.currentTimeMillis();
        final Integer newPort = (Integer) this.parameters.get("Local port");
        while (true) {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(newPort);
                serverSocket.close();
            }
            catch (final IOException e) {
                HttpAdapter.log.debug(Markers.HTTP, "Local port " + newPort + " is busy, waiting for it for connection " + this.connection.getName());
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    }
                    catch (final Exception ex) {}
                }
                try {
                    Thread.sleep(500L);
                }
                catch (final Exception ex2) {}
                if (System.currentTimeMillis() - startTime > MAX_WAIT) {
                    HttpAdapter.log.error(Markers.HTTP, "Unable to obtain local port " + newPort + ", cannot restart Jetty server ");
                    throw new ServerException("db.http.server.ErrorStartingHTTPServer", new Object[] { this.connection.getName(), "Unable to use local port " + newPort + ", port is already taken" });
                }
                continue;
            }
            break;
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
        String url = "http://";
        if (this.isHttpsWithServer()) {
            url = "https://";
        }
        url = url + this.getServerAddress().getHostName() + ":" + this.getServerPort();
        try {
            final CloseableHttpClient httpClient = HttpClients.createDefault();
            final HttpGet get = new HttpGet(url);
            final CloseableHttpResponse resp = httpClient.execute((HttpUriRequest)get);
            return null;
        }
        catch (final Exception ex) {
            return ex.getMessage();
        }
    }
    
    @Override
    public void run() {
        HttpAdapter.log.debug(Markers.HTTP, "HTTP connection \"" + this.connection.getName() + "\" is opening on port " + this.localPort);
        this.status.startTime = ZonedDateTime.now();
        (this.jettyThreadPool = new QueuedThreadPool()).setName("HTTP server for " + this.connection.getName());
        this.jettyServer = new Server((ThreadPool)this.jettyThreadPool);
        if (this.httpsWithClients) {
            if (this.clientSSLContext == null) {
                try {
                    (this.clientSSLContext = SSLContext.getInstance("TLSv1.2")).init(this.getProject().getKeyManagers(), this.getProject().getTrustManagers(), new SecureRandom());
                }
                catch (final Exception ex) {
                    throw new ServerException("db.http.server.ErrorStartingSSL", new Object[] { "client side of " + this.connection.getName(), ex });
                }
            }
            final HttpConfiguration config = new HttpConfiguration();
            config.setSecureScheme("https");
            config.setSecurePort(this.localPort);
            config.setOutputBufferSize(32786);
            config.setRequestHeaderSize(8192);
            config.setResponseHeaderSize(8192);
            config.addCustomizer((HttpConfiguration.Customizer)new SecureRequestCustomizer());
            final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setSslContext(this.clientSSLContext);
            final HttpConfiguration https_config = new HttpConfiguration(config);
            final SecureRequestCustomizer src = new SecureRequestCustomizer();
            src.setStsMaxAge(2000L);
            src.setStsIncludeSubDomains(true);
            https_config.setOutputBufferSize(32786);
            https_config.setRequestHeaderSize(8192);
            https_config.setResponseHeaderSize(8192);
            https_config.setSendServerVersion(false);
            https_config.setSendDateHeader(false);
            https_config.addCustomizer((HttpConfiguration.Customizer)src);
            (this.serverConnector = new ServerConnector(this.jettyServer, new ConnectionFactory[] { (ConnectionFactory)new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), (ConnectionFactory)new HttpConnectionFactory(https_config) })).setPort(this.localPort);
            this.serverConnector.setHost((String)null);
            this.jettyServer.addConnector((Connector)this.serverConnector);
        }
        else {
            final HttpConfiguration config = new HttpConfiguration();
            config.setSecureScheme("http");
            config.setOutputBufferSize(32786);
            config.setRequestHeaderSize(8192);
            config.setResponseHeaderSize(8192);
            config.setSendServerVersion(false);
            config.setSendDateHeader(false);
            final HttpConnectionFactory httpConnFact = new HttpConnectionFactory(config);
            (this.serverConnector = new ServerConnector(this.jettyServer, -1, -1, new ConnectionFactory[] { (ConnectionFactory)httpConnFact })).setPort(this.localPort);
            this.serverConnector.setHost((String)null);
            this.jettyServer.addConnector((Connector)this.serverConnector);
        }
        this.jettyHandler = new RequestHandlerJetty(this);
        this.jettyServer.setHandler((Handler)this.jettyHandler);
        final GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setIncludedMethods(new String[] { "PUT", "POST", "GET", "PATCH" });
        gzipHandler.setInflateBufferSize(2048);
        gzipHandler.setHandler((Handler)this.jettyHandler);
        this.jettyServer.setHandler((Handler)gzipHandler);
        this.jettyServer.setErrorHandler((ErrorHandler)new HttpErrorHandler());
        try {
            this.waitForLocalPort();
            this.jettyServer.start();
        }
        catch (final ServerException sex) {
            throw sex;
        }
        catch (final Exception ex2) {
            ex2.printStackTrace();
            throw new ServerException("db.http.server.ErrorStartingHTTPServer", new Object[] { this.connection.getName(), ex2 });
        }
        HttpAdapter.log.debug(Markers.HTTP, "HTTP connection \"" + this.connection.getName() + "\" is now open on port " + this.localPort);
    }
    
    private void readConfiguration() {
        final String serverAddressStr = (String)this.connection.getParameterValue("Server address");
        if (null == serverAddressStr || serverAddressStr.trim().length() == 0) {
            throw new RepositoryException("repo.MissingProperty", new Object[] { "Server address" });
        }
        try {
            this.serverAddress = InetAddress.getByName(serverAddressStr);
        }
        catch (final Exception ex) {
            HttpAdapter.log.error(Markers.HTTP, "Bad property 'Server address' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadHost", new Object[] { ex.getMessage() });
        }
        try {
            this.serverPort = (int)this.connection.getParameterValue("Server port");
        }
        catch (final Exception ex) {
            HttpAdapter.log.error(Markers.HTTP, "Bad property 'Server port' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadProperty", new Object[] { "Server port", ex.getMessage() });
        }
        try {
            Boolean b = (Boolean)this.connection.getParameterValue("Use HTTPS with server");
            if (b == null) {
                b = false;
            }
            this.httpsWithServer = b;
        }
        catch (final Exception ex) {
            HttpAdapter.log.error(Markers.HTTP, "Bad property 'Use HTTPS with server' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadProperty", new Object[] { "Use HTTPS with server", ex.getMessage() });
        }
        try {
            Boolean b = (Boolean)this.connection.getParameterValue("Trust server certificate");
            if (b == null) {
                b = false;
            }
            this.trustServerCertificate = b;
        }
        catch (final Exception ex) {
            HttpAdapter.log.error(Markers.HTTP, "Bad property 'Trust server certificate' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadProperty", new Object[] { "Trust server certificate", ex.getMessage() });
        }
        this.localHostName = (String)this.connection.getParameterValue("Local address");
        Label_0501: {
            Label_0444: {
                if (null != this.localHostName) {
                    if (this.localHostName.trim().length() != 0) {
                        break Label_0444;
                    }
                }
                try {
                    this.localAddress = InetAddress.getLocalHost();
                    this.localHostName = this.localAddress.getHostName();
                    break Label_0501;
                }
                catch (final Exception ex) {
                    HttpAdapter.log.error(Markers.HTTP, "Unable to get local address: " + ex.getMessage());
                    throw new ServerException("repo.BadHost", new Object[] { ex.getMessage() });
                }
            }
            try {
                this.localPort = (int)this.connection.getParameterValue("Local port");
            }
            catch (final Exception ex) {
                HttpAdapter.log.error(Markers.HTTP, "Bad property 'Local port' in repository: " + ex.getMessage());
                throw new ServerException("repo.BadProperty", new Object[] { "Local port", ex.getMessage() });
            }
        }
        try {
            Boolean b = (Boolean)this.connection.getParameterValue("Use HTTPS with clients");
            if (b == null) {
                b = false;
            }
            this.httpsWithClients = b;
        }
        catch (final Exception ex) {
            HttpAdapter.log.error(Markers.HTTP, "Bad property 'Use HTTPS with clients' in repository: " + ex.getMessage());
            throw new ServerException("repo.BadProperty", new Object[] { "Use HTTPS with clients", ex.getMessage() });
        }
    }
    
    public Project getProject() {
        return this.project;
    }
    
    public Connection getConnection() {
        return this.connection;
    }
    
    public AdapterCallback getCallback() {
        return this.callback;
    }
    
    public InetAddress getServerAddress() {
        return this.serverAddress;
    }
    
    public int getServerPort() {
        return this.serverPort;
    }
    
    public boolean isHttpsWithServer() {
        return this.httpsWithServer;
    }
    
    public boolean isTrustServerCertificate() {
        return this.trustServerCertificate;
    }
    
    public InetAddress getLocalAddress() {
        return this.localAddress;
    }
    
    public String getLocalHostName() {
        return this.localHostName;
    }
    
    public int getLocalPort() {
        return this.localPort;
    }
    
    public boolean isHttpsWithClients() {
        return this.httpsWithClients;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
