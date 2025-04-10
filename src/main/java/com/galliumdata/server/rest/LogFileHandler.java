// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.io.IOException;
import java.util.Iterator;
import java.util.Deque;
import java.time.ZonedDateTime;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.LogEvent;
import com.galliumdata.server.log.LogStreamAppender;
import java.util.zip.ZipEntry;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Instant;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import com.galliumdata.server.log.Markers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;
import com.sun.net.httpserver.HttpHandler;

public class LogFileHandler implements HttpHandler
{
    private static final Logger log;
    private static final String BASE_PATH_KEYWORD = "logfile";
    public static final String BASE_PATH = "/logfile/";
    
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        if (!RestManager.checkAddress(exchange)) {
            return;
        }
        final String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            handleOptions(exchange);
            return;
        }
        if (!"GET".equals(method)) {
            final String errMsg = "Method not supported: " + exchange.getRequestMethod();
            final OutputStream out = exchange.getResponseBody();
            out.write(errMsg.getBytes());
            exchange.sendResponseHeaders(405, errMsg.getBytes().length);
            out.close();
            return;
        }
        if (LogFileHandler.log.isTraceEnabled()) {
            LogFileHandler.log.trace(Markers.REPO, "Retrieving logfile: {}", (Object)exchange.getRequestURI().toString());
        }
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length != 2 || !pathParts[1].equals("logfile")) {
            LogFileHandler.log.debug(Markers.REPO, "Request for invalid logfile: " + String.valueOf(uri));
            sendErrorMessage(exchange, "Request for invalid logfile: " + String.valueOf(uri));
            return;
        }
        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        final ZipOutputStream zipOut = new ZipOutputStream(fos);
        final ZonedDateTime now = Instant.now().atZone(ZoneOffset.UTC);
        final String fileName = "GalliumData_" + DateTimeFormatter.ofPattern("yyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(now);
        final ZipEntry zipEntry = new ZipEntry(fileName + ".log");
        zipOut.putNextEntry(zipEntry);
        final DateTimeFormatter logDateFormat = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
        final Deque<LogEvent> events = LogStreamAppender.getInstance().getEventQueue();
        for (final LogEvent evt : events) {
            final StringBuilder sb = new StringBuilder();
            sb.append(logDateFormat.format(Instant.ofEpochMilli(evt.getInstant().getEpochMillisecond())));
            sb.append(" ");
            sb.append(evt.getMessage().getFormattedMessage());
            sb.append("\n");
            zipOut.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        zipOut.close();
        fos.close();
        final byte[] zipBytes = fos.toByteArray();
        exchange.getResponseHeaders().add("Content-Type", "application/zip");
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + fileName + ".zip\"");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(200, zipBytes.length);
        final OutputStream out2 = exchange.getResponseBody();
        out2.write(zipBytes);
        out2.close();
        if (LogFileHandler.log.isTraceEnabled()) {
            LogFileHandler.log.trace(Markers.REPO, "Retrieved log file for: {}, size was: {}", (Object)exchange.getRequestURI(), (Object)zipBytes.length);
        }
    }
    
    public static void sendErrorMessage(final HttpExchange exchange, final String msg) {
        try {
            final byte[] errMsgBytes = msg.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(404, errMsgBytes.length);
            final OutputStream out = exchange.getResponseBody();
            out.write(errMsgBytes);
            out.close();
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void handleOptions(final HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(200, 0L);
        final OutputStream out = exchange.getResponseBody();
        out.close();
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
