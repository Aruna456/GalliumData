// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import org.apache.logging.log4j.LogManager;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.log.Markers;
import java.util.Collections;
import java.util.HashSet;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import java.net.ServerSocket;
import java.net.InetAddress;

public class DNSTCPForwarder extends Thread
{
    private final DNSAdapter adapter;
    private final InetAddress serviceAddress;
    private final int port;
    private final InetAddress serverAddress;
    private int serverPort;
    private boolean running;
    private ServerSocket serverSocket;
    private long lastErrorTime;
    private int errorWait;
    private final Set<DNSTCPProcessor> runningProcessors;
    private static final Logger log;
    private static final int MAX_CONCURRENT_PROCESSORS = 1000;
    
    public DNSTCPForwarder(final DNSAdapter adapter, final InetAddress serviceAddress, final int servicePort, final InetAddress serverAddress, final int serverPort) {
        this.running = true;
        this.lastErrorTime = Long.MAX_VALUE;
        this.errorWait = 10;
        this.runningProcessors = Collections.synchronizedSet(new HashSet<DNSTCPProcessor>());
        this.adapter = adapter;
        this.serviceAddress = serviceAddress;
        this.port = servicePort;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
    
    @Override
    public void run() {
        this.setName("DNS TCP forwarder - " + this.adapter.getConnection().getName());
        try {
            this.serverSocket = new ServerSocket(this.port, 1000, this.serviceAddress);
        }
        catch (final Exception ex) {
            DNSTCPForwarder.log.info(Markers.DNS, "Unable to start TCP connection for " + this.adapter.getConnection().getName() + " on port " + this.port + " : " + ex.getMessage());
            this.adapter.tcpForwarder = null;
            return;
        }
        while (this.running) {
            Socket socket = null;
            try {
                socket = this.serverSocket.accept();
            }
            catch (final SocketException sex) {
                if ("Socket closed".equals(sex.getMessage())) {
                    DNSTCPForwarder.log.trace(Markers.DNS, "TCP socket to server was closed");
                    return;
                }
                throw new ServerException("db.dns.ErrorReceivingTCP", new Object[] { this.port, sex.getMessage() });
            }
            catch (final Exception ex2) {
                throw new ServerException("db.dns.ErrorReceivingTCP", new Object[] { this.port, ex2.getMessage() });
            }
            if (this.runningProcessors.size() >= 1000) {
                DNSTCPForwarder.log.warn(Markers.DNS, "Too many TCP processors running -- ignoring request");
                try {
                    socket.close();
                }
                catch (final Exception ex2) {
                    DNSTCPForwarder.log.debug(Markers.DNS, "Exception while closing TCP socket: " + ex2.getMessage());
                }
            }
            else {
                final DNSTCPProcessor processor = new DNSTCPProcessor(this, socket);
                this.runningProcessors.add(processor);
                processor.start();
            }
        }
    }
    
    protected Socket getSocketToServer() {
        try {
            final Socket socketToServer = new Socket();
            final InetSocketAddress sockAddr = new InetSocketAddress(this.serverAddress, this.serverPort);
            socketToServer.connect(sockAddr, 1000);
            return socketToServer;
        }
        catch (final Exception ex) {
            throw new ServerException("db.dns.CannotConnectToServer", new Object[] { this.serverAddress, this.serverPort, ex.getMessage() });
        }
    }
    
    protected void processorIsDone(final DNSTCPProcessor processor) {
        if (processor.finalError != null) {
            if (System.currentTimeMillis() - this.lastErrorTime > this.errorWait) {
                try {
                    this.errorWait *= 2;
                    DNSTCPForwarder.log.trace(Markers.DNS, "TCP processor failed, waiting " + this.errorWait + " ms. before retrying");
                    Thread.sleep(this.errorWait);
                }
                catch (final InterruptedException ex) {}
            }
            this.lastErrorTime = System.currentTimeMillis();
        }
        else {
            this.errorWait = 10;
        }
        if (!this.runningProcessors.contains(processor)) {
            DNSTCPForwarder.log.debug(Markers.DNS, "Unknown TCP processor terminating");
            return;
        }
        this.runningProcessors.remove(processor);
    }
    
    protected void requestStop() {
        this.running = false;
        try {
            if (this.serverSocket != null) {
                DNSTCPForwarder.log.trace(Markers.DNS, "Closing DNS TCP server socket");
                this.serverSocket.close();
            }
        }
        catch (final Exception ex) {
            DNSTCPForwarder.log.debug(Markers.DNS, "Exception while closing TCP server socket: " + ex.getMessage());
        }
    }
    
    private void notifyAdapter() {
        this.adapter.stopProcessing();
    }
    
    protected DNSAdapter getAdapter() {
        return this.adapter;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
