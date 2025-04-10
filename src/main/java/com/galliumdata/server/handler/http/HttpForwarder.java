// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.handler.http.response.HttpResponse;
import com.galliumdata.server.repository.FilterStage;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import com.galliumdata.server.handler.http.requests.HttpRequest;
import com.galliumdata.server.util.BinaryDump;
import com.galliumdata.server.log.Markers;
import java.io.IOException;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;
import java.io.OutputStream;
import java.net.Socket;

public abstract class HttpForwarder implements Runnable
{
    private HttpForwarder otherForwarder;
    protected Socket inSock;
    protected Socket outSock;
    protected OutputStream out;
    protected HttpAdapter adapter;
    protected NetworkPacketReader reader;
    protected RawPacketReader pktReader;
    protected NetworkPacketSender sender;
    protected RawPacketWriter writer;
    protected boolean stopRequested;
    protected Variables connectionContext;
    protected static ThreadLocal<Variables> threadContext;
    protected Thread thread;
    protected ConnectionState connectionState;
    private static final Logger log;
    private static final Logger sslLog;
    
    public HttpForwarder(final Socket inSock, final Socket outSock, final Variables connectionContext, final ConnectionState connectionState) {
        this.stopRequested = false;
        this.connectionContext = connectionContext;
        this.connectionState = connectionState;
        this.inSock = inSock;
        try {
            this.reader = new NetworkPacketReader(inSock, this.toString(), this);
            this.pktReader = new RawPacketReader(connectionState, this.reader);
            if (outSock != null) {
                this.out = outSock.getOutputStream();
                this.outSock = outSock;
                this.sender = new NetworkPacketSender(this.out);
                this.writer = new RawPacketWriter(this.sender, this.connectionState);
            }
        }
        catch (final IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }
    
    @Override
    public void run() {
        while (!this.stopRequested) {
            RawPacket rawPacket = null;
            try {
                rawPacket = this.reader.readNextPacket();
            }
            catch (final Exception ex) {
                HttpForwarder.log.debug(Markers.HTTP, "Exception while reading packet: {}", (Object)ex.getMessage());
            }
            if (rawPacket == null) {
                try {
                    this.out.close();
                }
                catch (final Exception ex) {
                    ex.printStackTrace();
                }
                this.otherForwarder.requestStop();
                return;
            }
            try {
                this.processPacket(rawPacket, this.pktReader, this.writer);
                this.writer.send();
            }
            catch (final Exception ex) {
                HttpForwarder.log.debug(Markers.HTTP, "Exception while processing packet, closing connection, error was: {}", (Object)ex.getMessage());
                try {
                    this.out.close();
                }
                catch (final Exception ex2) {
                    ex2.printStackTrace();
                }
                this.getOtherForwarder().requestStop();
                return;
            }
        }
        this.cleanup();
    }
    
    protected abstract void processPacket(final RawPacket p0, final RawPacketReader p1, final RawPacketWriter p2);
    
    public HttpForwarder getOtherForwarder() {
        return this.otherForwarder;
    }
    
    public void setOtherForwarder(final HttpForwarder otherForwarder) {
        this.otherForwarder = otherForwarder;
    }
    
    public void requestStop() {
        this.stopRequested = true;
        try {
            this.inSock.close();
        }
        catch (final Exception ex) {
            HttpForwarder.log.trace(Markers.HTTP, "Exception caught when closing inSock in requestStop for {}: {}", (Object)this, (Object)ex.getMessage());
        }
        this.getOtherForwarder().reader.close();
    }
    
    protected void writeOut(final byte[] buffer, final int offset, final int length) throws IOException {
        if (HttpForwarder.sslLog.isTraceEnabled()) {
            HttpForwarder.sslLog.trace(Markers.HTTP, this.toString() + " is about to encrypt and send:\n" + BinaryDump.getBinaryDump(buffer, offset, length));
        }
        try {
            this.out.write(buffer, offset, length);
        }
        catch (final IOException ioex) {
            throw ioex;
        }
        this.out.flush();
    }
    
    protected void cleanup() {
    }
    
    public ConnectionState getConnectionState() {
        return this.connectionState;
    }
    
    public static Variables getThreadContext() {
        Variables var = HttpForwarder.threadContext.get();
        if (var == null) {
            var = new Variables();
            HttpForwarder.threadContext.set(var);
        }
        return var;
    }
    
    protected AdapterCallbackResponse callRequestFilters(final HttpRequest req) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.REQUEST, req.getRequestType())) {
            return new AdapterCallbackResponse();
        }
        final Variables context = new Variables();
        context.put("request", req);
        context.put("clientAddress", this.inSock.getInetAddress());
        context.put("connectionContext", this.connectionContext);
        context.put("threadContext", getThreadContext());
        final AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeRequestFilters(req.getRequestType(), context);
        if (response.reject) {
            HttpForwarder.log.trace(Markers.HTTP, "Request has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        return response;
    }
    
    protected AdapterCallbackResponse callResponseFilters(final HttpResponse resp) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.RESPONSE, resp.getResponseType())) {
            return new AdapterCallbackResponse();
        }
        final Variables context = new Variables();
        context.put("response", resp);
        context.put("clientAddress", this.outSock.getInetAddress());
        context.put("connectionContext", this.connectionContext);
        context.put("threadContext", getThreadContext());
        final AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeResponseFilters(resp.getResponseType(), context);
        if (response.reject) {
            HttpForwarder.log.trace(Markers.HTTP, "Response has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        return response;
    }
    
    protected HttpAdapter getAdapter() {
        return this.adapter;
    }
    
    protected void setAdapter(final HttpAdapter adapter) {
        this.adapter = adapter;
    }
    
    protected abstract void incrementBytesReceived(final long p0);
    
    @Override
    public String toString() {
        return "Generic HTTP forwarder";
    }
    
    static {
        HttpForwarder.threadContext = new ThreadLocal<Variables>();
        log = LogManager.getLogger("galliumdata.dbproto");
        sslLog = LogManager.getLogger("galliumdata.ssl");
    }
}
