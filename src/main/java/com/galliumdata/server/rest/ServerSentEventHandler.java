// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import com.galliumdata.server.debug.DebugManager;
import com.galliumdata.server.log.Markers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Queue;
import com.sun.net.httpserver.HttpHandler;

public class ServerSentEventHandler implements HttpHandler
{
    private static final Queue<ServerEvent> messageQueue;
    private static ServerEvent currentEvent;
    private static final AtomicLong latestTimestamp;
    private static final Object sendFlag;
    private static final Logger log;
    
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        if (!RestManager.checkAddress(exchange)) {
            ServerSentEventHandler.log.debug(Markers.SYSTEM, "Rejecting SSE call from " + String.valueOf(exchange.getRemoteAddress()));
            return;
        }
        final long now = System.nanoTime();
        synchronized (ServerSentEventHandler.latestTimestamp) {
            if (now > ServerSentEventHandler.latestTimestamp.longValue()) {
                ServerSentEventHandler.latestTimestamp.set(now);
            }
        }
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            RestHandler.handleOptions(exchange);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            ServerSentEventHandler.log.debug(Markers.SYSTEM, "Rejecting SSE call (not OPTIONS or GET) from " + String.valueOf(exchange.getRemoteAddress()));
            return;
        }
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");
        exchange.getResponseHeaders().add("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0L);
        final OutputStream out = exchange.getResponseBody();
        if (!DebugManager.debugIsActive()) {
            out.write("event: closed\n\n".getBytes());
            out.flush();
            out.close();
            return;
        }
        while (true) {
            if (ServerSentEventHandler.messageQueue.isEmpty()) {
                try {
                    Thread.sleep(500L);
                }
                catch (final Exception ex) {
                    ServerSentEventHandler.log.debug(Markers.WEB, "SSE server interrupted during wait");
                }
            }
            else {
                synchronized (ServerSentEventHandler.latestTimestamp) {
                    if (ServerSentEventHandler.latestTimestamp.longValue() > now) {
                        ServerSentEventHandler.log.debug(Markers.WEB, "This SSE request has been superseded");
                        out.close();
                        return;
                    }
                }
                final StringBuilder resp = new StringBuilder();
                resp.append("retry: 1000\n");
                if (!ServerSentEventHandler.messageQueue.isEmpty()) {
                    final ServerEvent theEvent = ServerSentEventHandler.messageQueue.remove();
                    resp.append("event: ").append(theEvent.eventType).append("\n");
                    final String[] split;
                    final String[] lines = split = theEvent.message.split("\\n");
                    for (String line : split) {
                        line = line.replaceAll("\r", "\\r");
                        resp.append("data: ").append(line).append("\n");
                    }
                }
                resp.append("\n");
                final String respStr = resp.toString();
                if (ServerSentEventHandler.log.isTraceEnabled()) {
                    ServerSentEventHandler.log.trace(Markers.SYSTEM, "Sending an SSE now " + String.valueOf(new Date()) + " to " + String.valueOf(exchange.getRemoteAddress()) + ": " + respStr);
                }
                try {
                    out.write(respStr.getBytes());
                    out.flush();
                }
                catch (final Exception ex2) {
                    ServerSentEventHandler.log.trace(Markers.SYSTEM, "SSE stream broken by client");
                }
            }
        }
    }
    
    public static void queueServerEvent(final ServerEvent evt) {
        if (ServerSentEventHandler.messageQueue.size() > 10000) {
            throw new RuntimeException("Unexpected: server event queue has grown way too much");
        }
        ServerSentEventHandler.currentEvent = evt;
        ServerSentEventHandler.messageQueue.add(evt);
        synchronized (ServerSentEventHandler.sendFlag) {
            ServerSentEventHandler.sendFlag.notifyAll();
        }
    }
    
    public static void stopEventQueue() {
        ServerSentEventHandler.messageQueue.clear();
        synchronized (ServerSentEventHandler.sendFlag) {
            ServerSentEventHandler.sendFlag.notifyAll();
        }
    }
    
    public static ServerEvent getCurrentEvent() {
        return ServerSentEventHandler.currentEvent;
    }
    
    public static void deleteCurrentEvent() {
        ServerSentEventHandler.currentEvent = null;
    }
    
    static {
        messageQueue = new ConcurrentLinkedDeque<ServerEvent>();
        latestTimestamp = new AtomicLong();
        sendFlag = new Object();
        log = LogManager.getLogger("galliumdata.core");
    }
}
