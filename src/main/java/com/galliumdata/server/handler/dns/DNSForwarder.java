// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.log.Markers;
import java.net.DatagramPacket;
import java.util.HashSet;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import java.net.InetAddress;
import java.net.DatagramSocket;

public class DNSForwarder extends Thread
{
    private final boolean isClientToServer;
    protected final DNSAdapter adapter;
    protected final DatagramSocket fromSocket;
    protected final DatagramSocket toSocket;
    private final InetAddress toAddress;
    private final int toPort;
    private boolean running;
    private final Set<DNSProcessor> runningProcessors;
    private static final Logger log;
    private static final int MAX_CONCURRENT_PROCESSORS = 1000;
    
    public DNSForwarder(final DNSAdapter adapter, final DatagramSocket fromSocket, final DatagramSocket toSocket, final InetAddress toAddress, final int toPort, final boolean isClientToServer) {
        this.running = true;
        this.runningProcessors = new HashSet<DNSProcessor>();
        this.isClientToServer = isClientToServer;
        this.adapter = adapter;
        this.fromSocket = fromSocket;
        this.toSocket = toSocket;
        this.toAddress = toAddress;
        this.toPort = toPort;
    }
    
    @Override
    public void run() {
        while (this.running) {
            final byte[] buf = new byte[4096];
            final DatagramPacket inPacket = new DatagramPacket(buf, buf.length);
            try {
                this.fromSocket.receive(inPacket);
            }
            catch (final Exception ex) {
                if (!this.running && ex.getMessage().equals("Socket closed")) {
                    DNSForwarder.log.trace(Markers.DNS, "DNS forwarder is done (c-s:" + this.isClientToServer);
                    this.requestStop();
                    return;
                }
                DNSForwarder.log.debug(Markers.DNS, "Exception receiving from DNS socket: " + String.valueOf(ex));
                continue;
            }
            if (!this.isClientToServer && !this.adapter.responseIsAcceptable(inPacket.getAddress(), inPacket.getPort()) && DNSForwarder.log.isTraceEnabled()) {
                DNSForwarder.log.trace(Markers.DNS, "Ignoring response from unexpected source: " + String.valueOf(inPacket.getAddress()));
            }
            else {
                DNSProcessor processor;
                if (this.isClientToServer) {
                    this.adapter.getStatus().incrementNumRequests(1L);
                    this.adapter.getStatus().incrementNumRequestBytes(inPacket.getLength());
                    final DNSRequest req = new DNSRequest();
                    req.clientAddress = inPacket.getAddress();
                    req.clientPort = inPacket.getPort();
                    req.transactionId = DNSPacket.getShortFromBuffer(buf, 0);
                    this.adapter.recordRequest(req);
                    DNSPacket.setShortInBuffer(buf, 0, req.proxyTransactionId);
                    processor = new DNSProcessor(this, inPacket, true, this.toAddress, this.toPort, req.transactionId);
                }
                else {
                    this.adapter.getStatus().incrementNumResponses(1L);
                    this.adapter.getStatus().incrementNumResponseBytes(inPacket.getLength());
                    final short id = DNSPacket.getShortFromBuffer(inPacket.getData(), inPacket.getOffset());
                    final DNSRequest req2 = this.adapter.getRequestByProxyId(id);
                    if (req2 == null) {
                        DNSForwarder.log.warn(Markers.DNS, "Response to forgotten request -- ignoring");
                        continue;
                    }
                    processor = new DNSProcessor(this, inPacket, false, req2.clientAddress, req2.clientPort, req2.transactionId);
                }
                this.runningProcessors.add(processor);
                if (this.runningProcessors.size() > 1000) {
                    throw new ServerException("db.dns.TooManyConcurrentProcessors", new Object[] { this.runningProcessors.size() });
                }
                processor.start();
            }
        }
    }
    
    protected void processorIsDone(final DNSProcessor processor) {
        if (!this.runningProcessors.contains(processor)) {
            throw new ServerException("db.dns.UnknownProcessorReturning", new Object[0]);
        }
        this.runningProcessors.remove(processor);
    }
    
    protected void sendPacket(final DatagramSocket socket, final DNSPacket pkt, InetAddress addr, int port) {
        final int pktSize = pkt.getSerializedSize();
        final byte[] outBuf = new byte[pktSize];
        pkt.writeToBytes(outBuf, 0);
        if (addr == null) {
            addr = this.toAddress;
            port = this.toPort;
        }
        final DatagramPacket packetToServer = new DatagramPacket(outBuf, pktSize, addr, port);
        try {
            socket.send(packetToServer);
        }
        catch (final Exception ex) {
            DNSForwarder.log.trace(Markers.DNS, "Exception sending to DNS socket: " + String.valueOf(ex));
        }
    }
    
    public void requestStop() {
        this.running = false;
        DNSForwarder.log.trace(Markers.DNS, "DNS forwarder is closing socket " + String.valueOf(this.fromSocket));
        try {
            this.fromSocket.close();
        }
        catch (final Exception ex) {
            DNSForwarder.log.warn(Markers.DNS, "Exception while closing listen socket: " + String.valueOf(ex));
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
