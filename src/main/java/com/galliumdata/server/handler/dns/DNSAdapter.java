// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import org.apache.logging.log4j.LogManager;
import java.net.DatagramPacket;
import java.util.Iterator;
import java.net.InetAddress;
import java.net.BindException;
import com.galliumdata.server.ServerException;
import java.net.DatagramSocket;
import java.time.ZonedDateTime;
import com.galliumdata.server.log.Markers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.adapters.AdapterStatus;
import com.galliumdata.server.adapters.AdapterCallback;
import java.util.Map;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.adapters.AdapterInterface;

public class DNSAdapter implements AdapterInterface
{
    private Project project;
    private Connection connection;
    private Map<String, Object> parameters;
    protected DNSAdapterConfiguration config;
    private AdapterCallback callback;
    private final AdapterStatus status;
    private final Variables adapterContext;
    private static final Logger log;
    private static final int MAX_LIVE_FORWARDERS = 100;
    private static final int MAX_FORWARDERS_AGE = 10000;
    private static long lastPruneTime;
    protected static final String PARAM_LOCAL_ADDRESS = "Local address";
    protected static final String PARAM_LOCAL_PORT = "Local port";
    protected static final String PARAM_SERVER_HOST = "Server host";
    protected static final String PARAM_SERVER_PORT = "Server port";
    protected static final String PARAM_DEFAULT_CONN = "Default connection";
    protected static final String PARAM_RUN_TCP = "Run on TCP";
    protected final AtomicInteger proxyTxId;
    private final Map<Short, DNSRequest> requests;
    private final List<DNSRequest> requestsInOrder;
    protected DNSForwarder clientToServerForwarder;
    protected DNSForwarder serverToClientForwarder;
    protected DNSTCPForwarder tcpForwarder;
    protected Map<String, DNSAdapterConfiguration> secondaryConnections;
    
    public DNSAdapter() {
        this.status = new AdapterStatus();
        this.adapterContext = new Variables();
        this.proxyTxId = new AtomicInteger();
        this.requests = Collections.synchronizedMap(new HashMap<Short, DNSRequest>());
        this.requestsInOrder = Collections.synchronizedList(new ArrayList<DNSRequest>());
        this.secondaryConnections = new HashMap<String, DNSAdapterConfiguration>();
    }
    
    @Override
    public void initialize() {
    }
    
    @Override
    public boolean configure(final Project project, final Connection conn, final AdapterCallback callback) {
        DNSAdapter.log.trace(Markers.DNS, "DNS connection is being configured");
        this.project = project;
        this.connection = conn;
        this.parameters = conn.getParameters();
        (this.config = new DNSAdapterConfiguration()).readParameters(this.parameters);
        this.callback = callback;
        return this.config.defaultConnection;
    }
    
    @Override
    public void stopProcessing() {
        if (this.clientToServerForwarder != null) {
            try {
                this.clientToServerForwarder.requestStop();
            }
            catch (final Exception ex) {
                DNSAdapter.log.debug(Markers.DNS, "Exception while stopping DNS request forwarder: " + ex.getMessage());
            }
        }
        this.clientToServerForwarder = null;
        if (this.serverToClientForwarder != null) {
            try {
                this.serverToClientForwarder.requestStop();
            }
            catch (final Exception ex) {
                DNSAdapter.log.debug(Markers.DNS, "Exception while stopping DNS response forwarder: " + ex.getMessage());
            }
        }
        if (this.tcpForwarder != null) {
            this.tcpForwarder.requestStop();
        }
        this.serverToClientForwarder = null;
        if (this.tcpForwarder != null) {
            try {
                this.tcpForwarder.requestStop();
            }
            catch (final Exception ex) {
                DNSAdapter.log.debug(Markers.DNS, "Exception while stopping DNS TCP forwarder: " + ex.getMessage());
            }
        }
        this.serverToClientForwarder = null;
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
        this.stopProcessing();
    }
    
    @Override
    public AdapterStatus getStatus() {
        return this.status;
    }
    
    @Override
    public String getName() {
        return "DNS adapter - " + this.connection.getName();
    }
    
    @Override
    public void run() {
        this.status.startTime = ZonedDateTime.now();
        if (!this.config.defaultConnection) {
            DNSAdapter.log.trace(Markers.DNS, "Non-default connection has been configured: " + this.connection.getName());
            return;
        }
        DNSAdapter.log.trace(Markers.DNS, "Starting DNS connection on port " + this.config.localPort + " for address " + String.valueOf(this.config.localAddress));
        DatagramSocket socketFromClient;
        try {
            socketFromClient = new DatagramSocket(this.config.localPort, this.config.localAddress);
        }
        catch (final BindException bex) {
            throw new ServerException("db.dns.CannotBindToPort", new Object[] { this.config.localPort, bex.getMessage() });
        }
        catch (final Exception ex) {
            throw new ServerException("db.dns.CannotUsePort", new Object[] { this.config.localPort, ex.getMessage() });
        }
        DatagramSocket socketToServer;
        try {
            socketToServer = new DatagramSocket();
        }
        catch (final Exception ex2) {
            throw new ServerException("db.dns.ConnectionError", new Object[] { ex2 });
        }
        for (final Map.Entry<String, Connection> connEntry : this.project.getConnections().entrySet()) {
            final Connection conn = connEntry.getValue();
            if (conn == this.connection) {
                continue;
            }
            if (!conn.isActive()) {
                continue;
            }
            final DNSAdapterConfiguration cfg = new DNSAdapterConfiguration();
            cfg.readParameters(conn.getParameters());
            if (cfg.defaultConnection) {
                throw new ServerException("db.dns.config.OnlyOneDefaultConnection", new Object[] { this.connection.getName(), conn.getName(), this.project.getName() });
            }
            this.secondaryConnections.put(conn.getName(), cfg);
        }
        this.clientToServerForwarder = new DNSForwarder(this, socketFromClient, socketToServer, this.config.serverAddress, this.config.serverPort, true);
        this.serverToClientForwarder = new DNSForwarder(this, socketToServer, socketFromClient, null, 0, false);
        DNSAdapter.log.trace(Markers.DNS, "DNS adapter is now starting on port " + this.config.localPort + " - " + this.connection.getName());
        if (this.config.runTCP) {
            (this.tcpForwarder = new DNSTCPForwarder(this, this.config.localAddress, this.config.localPort, this.config.serverAddress, this.config.serverPort)).start();
        }
        this.serverToClientForwarder.setName("DNS server-to-client forwarder - " + this.connection.getName());
        this.serverToClientForwarder.start();
        Thread.currentThread().setName("DNS client-to-server forwarder - " + this.connection.getName());
        try {
            this.clientToServerForwarder.run();
        }
        catch (final Exception ex2) {
            DNSAdapter.log.debug(Markers.DNS, "DNS client-to-server forwarder ended with an exception: " + ex2.getMessage());
        }
        DNSAdapter.log.trace(Markers.DNS, "DNS adapter is done - " + this.connection.getName());
    }
    
    public void recordRequest(final DNSRequest req) {
        req.proxyTransactionId = (short)this.proxyTxId.incrementAndGet();
        this.requests.put(req.proxyTransactionId, req);
        this.requestsInOrder.add(req);
        if (this.requestsInOrder.size() > 100 || System.currentTimeMillis() - DNSAdapter.lastPruneTime > 10000L) {
            this.pruneForwarders();
            DNSAdapter.lastPruneTime = System.currentTimeMillis();
        }
    }
    
    public DNSRequest getRequestByProxyId(final short id) {
        return this.requests.get(id);
    }
    
    private void pruneForwarders() {
        final long now = System.currentTimeMillis();
        int numPruned = 0;
        while (this.requestsInOrder.size() > 0) {
            final DNSRequest req = this.requestsInOrder.get(0);
            if (now - req.ts <= 10000L) {
                break;
            }
            this.requestsInOrder.remove(req);
            if (!this.requests.containsKey(req.proxyTransactionId)) {
                throw new RuntimeException("Unexpected: DNS request unknown");
            }
            this.requests.remove(req.proxyTransactionId);
            this.requestsInOrder.remove(req);
            ++numPruned;
        }
        if (numPruned > 0 && DNSAdapter.log.isTraceEnabled()) {
            DNSAdapter.log.trace("Pruned " + numPruned + " old DNS requests");
        }
        numPruned = 0;
        while (this.requestsInOrder.size() > 80) {
            final DNSRequest req = this.requestsInOrder.get(0);
            this.requestsInOrder.remove(req);
            this.requests.remove(req.proxyTransactionId);
            ++numPruned;
        }
        if (numPruned > 0 && DNSAdapter.log.isDebugEnabled()) {
            DNSAdapter.log.debug("Pruned " + numPruned + " DNS requests to make room");
        }
    }
    
    @Override
    public String testConnection(final Project project, final Connection conn) {
        final String serverAddressStr = (String) conn.getParameters().get("Server host");
        if (null == serverAddressStr || serverAddressStr.trim().length() == 0) {
            return "Invalid server name: empty";
        }
        InetAddress serverAddress;
        try {
            serverAddress = InetAddress.getByName(serverAddressStr);
        }
        catch (final Exception ex) {
            return "Invalid server name: " + ex.getMessage();
        }
        int serverPort;
        try {
            Object obj = conn.getParameters().get("Server port");
            if (obj == null) {
                obj = 53;
            }
            if (!(obj instanceof Integer)) {
                return "Invalid server port: not an integer";
            }
            serverPort = (int)obj;
        }
        catch (final Exception ex2) {
            return "Invalid server port: " + ex2.getMessage();
        }
        try {
            final DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(1000);
            final DNSPacket pkt = new DNSPacket();
            final DNSQuestion question = pkt.addQuestion();
            question.setName("www.galliumdata.com");
            final int pktSize = pkt.getSerializedSize();
            final byte[] buf = new byte[pktSize];
            pkt.writeToBytes(buf, 0);
            final DatagramPacket packetToServer = new DatagramPacket(buf, pktSize, serverAddress, serverPort);
            socket.send(packetToServer);
            final byte[] respBuf = new byte[512];
            final DatagramPacket packetFromServer = new DatagramPacket(respBuf, respBuf.length);
            socket.receive(packetFromServer);
            final DNSPacket respPkt = new DNSPacket();
            respPkt.read(respBuf);
        }
        catch (final Exception ex2) {
            return ex2.getMessage();
        }
        return null;
    }
    
    public Variables getAdapterContext() {
        return this.adapterContext;
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
    
    public boolean responseIsAcceptable(final InetAddress addr, final int port) {
        if (addr.equals(this.config.serverAddress) && port == this.config.serverPort) {
            return true;
        }
        for (final DNSAdapterConfiguration secCfg : this.secondaryConnections.values()) {
            if (addr.equals(secCfg.serverAddress) && port == secCfg.serverPort) {
                return true;
            }
        }
        return false;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
        DNSAdapter.lastPruneTime = 0L;
    }
}
