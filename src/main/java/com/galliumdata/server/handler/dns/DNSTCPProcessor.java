// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.Logger;
import java.net.Socket;

public class DNSTCPProcessor extends DNSGenericProcessor
{
    private final DNSTCPForwarder forwarder;
    private final Socket socket;
    private Socket socketToServer;
    private boolean running;
    private DNSRequest dnsReq;
    private static final int PKT_MAX = 65536;
    protected String finalError;
    private static final Logger log;
    
    protected DNSTCPProcessor(final DNSTCPForwarder forwarder, final Socket socket) {
        this.running = true;
        this.dnsReq = new DNSRequest();
        this.finalError = null;
        this.forwarder = forwarder;
        this.adapter = forwarder.getAdapter();
        this.socket = socket;
        this.dnsReq.clientAddress = socket.getInetAddress();
        this.dnsReq.clientPort = socket.getPort();
    }
    
    @Override
    public void run() {
        try {
            while (this.running) {
                try {
                    this.receiveFromClient();
                    continue;
                }
                catch (final Exception ex) {
                    this.finalError = ex.getMessage();
                    return;
                }
            }
        }
        finally {
            if (this.socketToServer != null) {
                try {
                    this.socketToServer.close();
                }
                catch (final Exception ex2) {
                    DNSTCPProcessor.log.trace(Markers.DNS, "Error while closing TCP socket to server: " + ex2.getMessage());
                }
            }
            this.forwarder.processorIsDone(this);
        }
    }
    
    private void receiveFromClient() {
        final byte[] buffer = new byte[65536];
        int numReceived;
        try {
            numReceived = this.socket.getInputStream().read(buffer);
        }
        catch (final Exception ex) {
            throw new ServerException("db.dns.ErrorReceivingFromClient", new Object[] { ex.getMessage() });
        }
        if (numReceived == -1) {
            this.running = false;
            return;
        }
        int totalSize = DNSPacket.getShortFromBuffer(buffer, 0);
        while (numReceived < totalSize) {
            try {
                numReceived += this.socket.getInputStream().read(buffer, numReceived, 65536 - numReceived);
                continue;
            }
            catch (final Exception ex2) {
                throw new ServerException("db.dns.ErrorReceivingFromClient", new Object[] { ex2.getMessage() });
            }

        }
        DNSPacket pkt = new DNSPacket();
        byte[] bufferNoSize = new byte[totalSize];
        System.arraycopy(buffer, 2, bufferNoSize, 0, totalSize);
        int numRead = pkt.read(bufferNoSize);
        if (numRead != totalSize) {
            throw new ServerException("db.dns.proto.PacketSizeMismatch", new Object[] { numRead, totalSize, "client" });
        }
        if (DNSTCPProcessor.log.isTraceEnabled()) {
            DNSTCPProcessor.log.trace(Markers.DNS, "Received TCP packet from client: " + String.valueOf(pkt));
        }
        this.adapter.getStatus().incrementNumRequests(1L);
        this.adapter.getStatus().incrementNumRequestBytes(numReceived);
        this.dnsReq.transactionId = pkt.getTransactionId();
        AdapterCallbackResponse result = this.callRequestFilters(new DNSClientInfo(this.socket.getInetAddress(), this.socket.getPort()), pkt);
        if (result.response != null && result.response instanceof DNSPacket) {
            final DNSPacket respPkt = (DNSPacket)result.response;
            this.sendPacketToClient(respPkt);
            return;
        }
        if (result.reject) {
            DNSTCPProcessor.log.debug(Markers.DNS, "Rejecting request: " + String.valueOf(pkt));
            final DNSPacket rejectPkt = new DNSPacket();
            rejectPkt.setResponseCode((byte)5);
            rejectPkt.addQuestions(pkt.getQuestions());
            this.sendPacketToClient(rejectPkt);
            return;
        }
        int pktSize = pkt.getSerializedSize();
        DNSPacket.setShortInBuffer(buffer, 0, (short)pktSize);
        pkt.writeToBytes(buffer, 2);
        try {
            this.socketToServer = this.forwarder.getSocketToServer();
            this.socketToServer.getOutputStream().write(buffer, 0, pktSize + 2);
            this.socketToServer.getOutputStream().flush();
        }
        catch (final Exception ex3) {
            DNSTCPProcessor.log.trace(Markers.DNS, "Error forwarding TCP request to server: " + ex3.getMessage());
            pkt.setQuery(true);
            pkt.setResponseCode((byte)2);
            this.sendPacketToClient(pkt);
            throw new ServerException("db.dns.ErrorSendingToServer", new Object[] { ex3.getMessage() });
        }
        int numReceivedFromServer;
        try {
            numReceivedFromServer = this.socketToServer.getInputStream().read(buffer);
        }
        catch (final Exception ex4) {
            DNSTCPProcessor.log.trace(Markers.DNS, "Error getting TCP response to server: " + ex4.getMessage());
            throw new ServerException("db.dns.ErrorReceivingFromServer", new Object[] { ex4.getMessage() });
        }
        pkt = new DNSPacket();
        totalSize = DNSPacket.getShortFromBuffer(buffer, 0);
        while (totalSize > numReceivedFromServer) {
            try {
                numReceivedFromServer += this.socketToServer.getInputStream().read(buffer, numReceivedFromServer, 65536 - numReceivedFromServer);
                continue;
            }
            catch (final Exception ex4) {
                DNSTCPProcessor.log.trace(Markers.DNS, "Error getting TCP response to server: " + ex4.getMessage());
                throw new ServerException("db.dns.ErrorReceivingFromServer", new Object[] { ex4.getMessage() });
            }
        }
        if (totalSize + 2 != numReceivedFromServer) {
            throw new ServerException("db.dns.proto.AnswerSizeError", new Object[] { totalSize, numReceivedFromServer });
        }
        bufferNoSize = new byte[totalSize];
        System.arraycopy(buffer, 2, bufferNoSize, 0, totalSize);
        numRead = pkt.read(bufferNoSize);
        if (DNSTCPProcessor.log.isTraceEnabled()) {
            DNSTCPProcessor.log.trace(Markers.DNS, "Received TCP packet from server: " + String.valueOf(pkt));
        }
        this.adapter.getStatus().incrementNumResponses(1L);
        this.adapter.getStatus().incrementNumResponseBytes(numReceivedFromServer);
        if (numRead != totalSize) {
            throw new ServerException("db.dns.proto.PacketSizeMismatch", new Object[] { numRead, totalSize, "server" });
        }
        result = this.callResponseFilters(new DNSClientInfo(this.socket.getInetAddress(), this.socket.getPort()), pkt);
        if (result.response != null && result.response instanceof DNSPacket) {
            final DNSPacket respPkt2 = (DNSPacket)result.response;
            this.sendPacketToClient(respPkt2);
            return;
        }
        if (result.reject) {
            DNSTCPProcessor.log.debug(Markers.DNS, "Rejecting response: " + String.valueOf(pkt));
            return;
        }
        pktSize = pkt.getSerializedSize();
        DNSPacket.setShortInBuffer(buffer, 0, (short)pktSize);
        pkt.writeToBytes(buffer, 2);
        try {
            this.socket.getOutputStream().write(buffer, 0, pktSize + 2);
        }
        catch (final Exception ex4) {
            throw new ServerException("db.dns.ErrorSendingToClientError", new Object[] { ex4.getMessage() });
        }
    }
    
    protected void close() {
        this.running = false;
        try {
            this.socket.getInputStream().close();
        }
        catch (final Exception ex) {
            DNSTCPProcessor.log.trace(Markers.DNS, "Exception closing TCP in: " + ex.getMessage());
        }
        try {
            this.socket.close();
        }
        catch (final Exception ex) {
            DNSTCPProcessor.log.trace(Markers.DNS, "Exception closing TCP socket: " + ex.getMessage());
        }
    }
    
    private void sendPacketToClient(final DNSPacket pkt) {
        final int size = pkt.getSerializedSize();
        final byte[] buffer = new byte[size + 2];
        DNSPacket.writeShort((short)size, buffer, 0);
        pkt.writeToBytes(buffer, 2);
        try {
            this.socket.getOutputStream().write(buffer, 0, size + 2);
            this.socket.getOutputStream().flush();
        }
        catch (final Exception ex) {
            DNSTCPProcessor.log.debug(Markers.DNS, "Error while sending packet to client: " + ex.getMessage());
        }
    }
    
    @Override
    protected DNSRequest getRequest(final short id) {
        return this.dnsReq;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
