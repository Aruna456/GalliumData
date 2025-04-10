// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import com.galliumdata.server.adapters.AdapterCallbackResponse;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.log.Markers;
import java.net.InetAddress;
import java.net.DatagramPacket;

public class DNSProcessor extends DNSGenericProcessor
{
    private final DNSForwarder forwarder;
    private final DatagramPacket inPacket;
    private final boolean isClientToServer;
    private final InetAddress toAddress;
    private final int toPort;
    private final short clientTransactionId;
    
    protected DNSProcessor(final DNSForwarder forwarder, final DatagramPacket inPacket, final boolean isClientToServer, final InetAddress toAddress, final int toPort, final short clientTransactionId) {
        this.forwarder = forwarder;
        this.adapter = forwarder.adapter;
        this.inPacket = inPacket;
        this.isClientToServer = isClientToServer;
        this.toAddress = toAddress;
        this.toPort = toPort;
        this.clientTransactionId = clientTransactionId;
        final String direction = isClientToServer ? "client-to-server" : "server-to-client";
        super.setName("DNS " + direction + " processor for " + DNSPacket.getShortFromBuffer(inPacket.getData(), inPacket.getOffset()));
    }
    
    @Override
    public void run() {
        try {
            final DNSPacket pkt = new DNSPacket();
            pkt.read(this.inPacket.getData());
            pkt.setClientTransactionId(this.clientTransactionId);
            if (DNSProcessor.log.isTraceEnabled()) {
                DNSProcessor.log.trace(Markers.DNS, "Received UDP packet from " + (this.isClientToServer ? "client" : "server") + ": " + String.valueOf(pkt));
            }
            AdapterCallbackResponse result;
            if (this.isClientToServer) {
                result = this.callRequestFilters(new DNSClientInfo(this.inPacket.getAddress(), this.inPacket.getPort()), pkt);
                if (result.response != null && result.response instanceof DNSPacket) {
                    final DNSPacket respPkt = (DNSPacket)result.response;
                    respPkt.setTransactionId(this.clientTransactionId);
                    this.forwarder.sendPacket(this.forwarder.fromSocket, respPkt, this.inPacket.getAddress(), this.inPacket.getPort());
                    return;
                }
            }
            else {
                final DNSRequest req = this.adapter.getRequestByProxyId(pkt.getTransactionId());
                if (req == null) {
                    DNSProcessor.log.debug(Markers.DNS, "Received response for forgotten query -- ignoring");
                    return;
                }
                pkt.setClientTransactionId(req.transactionId);
                result = this.callResponseFilters(new DNSClientInfo(req.clientAddress, req.clientPort), pkt);
                while (pkt.getSerializedSize() > 512) {
                    pkt.setTruncated(true);
                    for (int i = pkt.getAdditionalRecords().size() - 1; i >= 0; --i) {
                        pkt.removeAdditionalRecord(i);
                    }
                }
                while (pkt.getSerializedSize() > 512) {
                    for (int i = pkt.getNameServers().size() - 1; i >= 0; --i) {
                        pkt.removeNameServer(i);
                    }
                }
                while (pkt.getSerializedSize() > 512) {
                    for (int i = pkt.getAnswers().size() - 1; i >= 0; --i) {
                        pkt.removeAnswer(i);
                    }
                }
                while (pkt.getSerializedSize() > 512) {
                    for (int i = pkt.getQuestions().size() - 1; i >= 0; --i) {
                        pkt.removeQuestion(i);
                    }
                }
            }
            if (result.reject && this.isClientToServer) {
                DNSProcessor.log.trace(Markers.DNS, "Rejecting query");
                pkt.setQuery(false);
                pkt.setResponseCode((byte)5);
                pkt.removeAnswers();
                pkt.removeNameServers();
                pkt.removeAdditionalRecords();
                this.forwarder.sendPacket(this.forwarder.fromSocket, pkt, this.inPacket.getAddress(), this.inPacket.getPort());
                return;
            }
            if (result.reject) {
                DNSProcessor.log.trace(Markers.DNS, "Rejecting packet");
                return;
            }
            if (result.skip) {
                DNSProcessor.log.trace(Markers.DNS, "Skipping packet");
                return;
            }
            if (this.isClientToServer) {
                if (result.connectionName != null && result.connectionName.trim().length() > 0) {
                    final DNSAdapterConfiguration secondaryAdapter = this.adapter.secondaryConnections.get(result.connectionName);
                    if (secondaryAdapter == null) {
                        throw new ServerException("db.dns.logic.NoSuchConnection", new Object[] { result.connectionName });
                    }
                    DNSProcessor.log.trace(Markers.DNS, "Sending request to secondary connection " + result.connectionName);
                    this.forwarder.sendPacket(this.forwarder.toSocket, pkt, secondaryAdapter.serverAddress, secondaryAdapter.serverPort);
                }
                else {
                    this.forwarder.sendPacket(this.forwarder.toSocket, pkt, this.toAddress, this.toPort);
                }
            }
            else {
                this.forwarder.sendPacket(this.forwarder.toSocket, pkt, this.toAddress, this.toPort);
            }
        }
        finally {
            this.forwarder.processorIsDone(this);
        }
    }
}
