// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import java.io.IOException;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.http.requests.HttpRequest;
import com.galliumdata.server.adapters.Variables;
import java.net.Socket;
import org.apache.logging.log4j.Logger;

public class ClientForwarder extends HttpForwarder
{
    private static final Logger log;
    
    public ClientForwarder(final Socket inSock, final Socket outSock, final Variables connectionContext, final ConnectionState connectionState) {
        super(inSock, outSock, connectionContext, connectionState);
    }
    
    @Override
    protected void processPacket(final RawPacket rawPacket, final RawPacketReader reader, final RawPacketWriter writer) {
        this.adapter.getStatus().incrementNumRequests(1L);
        final HttpRequest req = HttpRequest.createHttpRequest(rawPacket);
        req.readPacket(rawPacket);
        final AdapterCallbackResponse resp = this.callRequestFilters(req);
        if (resp.reject) {
            ClientForwarder.log.debug(Markers.HTTP, "Request filter '" + resp.logicName + "' has rejected request: " + resp.errorMessage);
            if (resp.errorResponse != null) {
                ClientForwarder.log.debug(Markers.HTTP, "Sending custom response provided by request filter '" + resp.logicName);
                try {
                    this.getOtherForwarder().writeOut(resp.errorResponse, 0, resp.errorResponse.length);
                }
                catch (final IOException ex) {
                    ClientForwarder.log.error(Markers.HTTP, "Exception while sending back rejection packet: " + ex.getMessage());
                }
            }
            if (resp.errorMessage != null) {
                return;
            }
            if (resp.closeConnection) {
                ClientForwarder.log.debug(Markers.HTTP, "Closing connection, as requested by filter '" + resp.logicName);
                this.requestStop();
            }
        }
        req.writePacket(writer);
    }
    
    @Override
    protected void incrementBytesReceived(final long num) {
        this.adapter.getStatus().incrementNumRequestBytes(num);
    }
    
    @Override
    public String toString() {
        return "HTTP client forwarder";
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
