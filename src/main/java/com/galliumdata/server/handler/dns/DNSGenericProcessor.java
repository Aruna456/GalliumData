// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import org.apache.logging.log4j.LogManager;
import java.util.Iterator;
import java.util.Set;
import com.galliumdata.server.log.Markers;
import java.util.Collection;
import com.galliumdata.server.repository.FilterStage;
import java.util.HashSet;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;

public class DNSGenericProcessor extends Thread
{
    protected DNSAdapter adapter;
    protected static ThreadLocal<Variables> threadContext;
    protected static final Logger log;
    
    protected AdapterCallbackResponse callRequestFilters(final DNSClientInfo clientInfo, final DNSPacket packet) {
        final Set<String> requestTypes = new HashSet<String>();
        for (final DNSQuestion question : packet.getQuestions()) {
            requestTypes.add(question.getTypeName());
        }
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketTypes(FilterStage.REQUEST, requestTypes)) {
            return new AdapterCallbackResponse();
        }
        final Variables context = new Variables();
        context.put("packet", packet);
        context.put("clientAddress", clientInfo.address);
        context.put("clientPort", clientInfo.port);
        context.put("connectionContext", this.adapter.getAdapterContext());
        context.put("threadContext", this.getThreadContext());
        final short reqId = packet.getTransactionId();
        final DNSRequest req = this.getRequest(reqId);
        if (req == null) {
            DNSGenericProcessor.log.debug(Markers.DNS, "Request has been forgotten - rejecting it");
            final AdapterCallbackResponse response = new AdapterCallbackResponse();
            response.reject = true;
            return response;
        }
        context.put("requestContext", req.getRequestContext());
        final AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeRequestFilters("query", context);
        if (response.reject && DNSGenericProcessor.log.isTraceEnabled()) {
            DNSGenericProcessor.log.trace(Markers.DNS, "Request has been rejected by user logic: {}", (Object)response.errorMessage);
        }
        return response;
    }
    
    protected AdapterCallbackResponse callResponseFilters(final DNSClientInfo clientInfo, final DNSPacket packet) {
        final Set<String> requestTypes = new HashSet<String>();
        for (final DNSQuestion question : packet.getQuestions()) {
            requestTypes.add(question.getTypeName());
        }
        final short reqId = packet.getTransactionId();
        final DNSRequest req = this.getRequest(reqId);
        if (req == null) {
            DNSGenericProcessor.log.debug(Markers.DNS, "Response to forgotten request - ignoring");
            final AdapterCallbackResponse response = new AdapterCallbackResponse();
            response.reject = true;
            return response;
        }
        packet.setTransactionIdNoChange(req.transactionId);
        if (this.adapter.getCallbackAdapter().hasFiltersForPacketTypes(FilterStage.RESPONSE, requestTypes)) {
            final Variables context = new Variables();
            context.put("packet", packet);
            context.put("clientAddress", clientInfo.address);
            context.put("clientPort", clientInfo.port);
            context.put("connectionContext", this.adapter.getAdapterContext());
            context.put("threadContext", this.getThreadContext());
            context.put("requestContext", req.getRequestContext());
            final AdapterCallbackResponse response2 = this.adapter.getCallbackAdapter().invokeResponseFilters("answer", context);
            if (response2.reject && DNSGenericProcessor.log.isTraceEnabled()) {
                DNSGenericProcessor.log.trace(Markers.DNS, "Response has been rejected by user logic: {}", (Object)response2.errorMessage);
            }
            return response2;
        }
        return new AdapterCallbackResponse();
    }
    
    protected Variables getThreadContext() {
        Variables var = DNSGenericProcessor.threadContext.get();
        if (var == null) {
            var = new Variables();
            DNSGenericProcessor.threadContext.set(var);
        }
        return var;
    }
    
    protected DNSRequest getRequest(final short id) {
        return this.adapter.getRequestByProxyId(id);
    }
    
    static {
        DNSGenericProcessor.threadContext = new ThreadLocal<Variables>();
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
