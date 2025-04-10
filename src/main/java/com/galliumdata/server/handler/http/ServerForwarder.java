// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import java.io.IOException;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.http.response.HttpResponse;
import com.galliumdata.server.adapters.Variables;
import java.net.Socket;
import org.apache.logging.log4j.Logger;

public class ServerForwarder extends HttpForwarder
{
    private static final Logger log;
    
    public ServerForwarder(final Socket inSock, final Socket outSock, final Variables connectionContext, final ConnectionState connectionState) {
        super(inSock, outSock, connectionContext, connectionState);
    }
    
    @Override
    protected void processPacket(final RawPacket rawPacket, final RawPacketReader reader, final RawPacketWriter writer) {
        this.adapter.getStatus().incrementNumResponses(1L);
        final HttpResponse resp = HttpResponse.createHttpResponse(rawPacket);
        final AdapterCallbackResponse cbResp = this.callResponseFilters(resp);
        if (cbResp.reject) {
            ServerForwarder.log.debug(Markers.HTTP, "Response filter '" + cbResp.logicName + "' has rejected response: " + cbResp.errorMessage);
            if (cbResp.errorResponse != null) {
                ServerForwarder.log.debug(Markers.HTTP, "Sending custom response provided by request filter '" + cbResp.logicName);
                try {
                    this.writeOut(cbResp.errorResponse, 0, cbResp.errorResponse.length);
                }
                catch (final IOException ex) {
                    ServerForwarder.log.error(Markers.HTTP, "Exception while sending back rejection packet: " + ex.getMessage());
                }
            }
            if (cbResp.closeConnection) {
                ServerForwarder.log.debug(Markers.HTTP, "Closing connection, as requested by filter '" + cbResp.logicName);
                this.requestStop();
            }
        }
        resp.writePacket(writer);
    }
    
    public void sendErrorMessage(final String errMsg, final int errNo) {
    }
    
    @Override
    protected void incrementBytesReceived(final long num) {
        this.adapter.getStatus().incrementNumResponseBytes(num);
    }
    
    @Override
    public String toString() {
        return "HTTP server forwarder";
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
