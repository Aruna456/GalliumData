// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http.request;

import java.util.stream.Stream;
import com.galliumdata.server.handler.http.java.HttpExchange;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.Objects;
import com.galliumdata.server.handler.http.java.HttpRequest;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.log.Markers;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.HashSet;
import com.galliumdata.server.repository.FilterUse;
import java.util.regex.Pattern;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import com.galliumdata.server.logic.RequestFilter;
import com.galliumdata.server.filters.http.HttpFilter;

public class RequestLoggingFilter extends HttpFilter implements RequestFilter
{
    private static final DateTimeFormatter logTsFormat;
    private PrintStream out;
    private Logger logger;
    private String format;
    protected Set<String> methods;
    protected Pattern contentPattern;
    protected Integer maxPayload;
    protected Pattern filter;
    private static final Logger log;
    
    @Override
    public void configure(final FilterUse def) {
        this.out = null;
        this.logger = null;
        this.format = null;
        this.methods = new HashSet<String>();
        this.contentPattern = null;
        this.maxPayload = 100;
        this.filter = null;
        super.configure(def);
        final String baseFilename = (String) def.getParameters().get("File name");
        if (baseFilename != null && baseFilename.trim().length() > 0) {
            final boolean useTimestamp = (boolean) def.getParameters().get("Use timestamp");
            String filename = baseFilename;
            final int dotIdx = filename.lastIndexOf(46);
            if (useTimestamp) {
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd-HHmmss");
                final String ts = ZonedDateTime.now().format(formatter);
                if (dotIdx == -1) {
                    filename = filename + "-" + ts + ".log";
                }
                else {
                    filename = filename.substring(0, dotIdx) + "-" + ts + filename.substring(dotIdx);
                }
            }
            else if (dotIdx == -1) {
                filename += ".log";
            }
            final File f = new File(filename);
            try {
                this.out = new PrintStream(f);
            }
            catch (final Exception ex) {
                RequestLoggingFilter.log.error(Markers.HTTP, "Unable to open log file for requests: " + filename + ", reason: " + ex.getMessage());
                this.out = null;
            }
        }
        else {
            this.out = System.out;
        }
        final String loggerName = (String) def.getParameters().get("Logger name");
        if (loggerName != null && loggerName.trim().length() > 0) {
            this.logger = LogManager.getLogger(loggerName);
        }
        else if (this.out == null) {
            this.logger = LogManager.getLogger("galliumdata.uselog");
        }
        this.format = def.getParameters().get("Format").toString();
        final String methodsStr = (String) def.getParameters().get("Methods");
        if (methodsStr != null && !methodsStr.isEmpty()) {
            this.methods = FilterUtils.readCommaSeparatedNames(methodsStr);
        }
        final String contentPatternStr = (String) def.getParameters().get("Content pattern");
        if (contentPatternStr != null && !contentPatternStr.isBlank()) {
            this.contentPattern = Pattern.compile(contentPatternStr, 40);
        }
        this.maxPayload = (Integer) def.getParameters().get("Max payload");
        final String filterStr = (String) def.getParameters().get("Filter");
        if (filterStr != null && !filterStr.isBlank()) {
            this.filter = Pattern.compile(filterStr, 40);
        }
    }
    
    @Override
    public String getName() {
        return "Request logging filter - HTTP";
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final HttpRequest request = (HttpRequest)context.get("request");
        final String reqMethod = request.getMethod();
        if (!this.methods.isEmpty()) {
            final Stream<String> stream = this.methods.stream();
            final String obj = reqMethod;
            Objects.requireNonNull(obj);
            if (stream.noneMatch((method->method.equalsIgnoreCase(obj))))
            {
                return new FilterResult();
            }
        }
        if (!this.urlMatches(request.getUrl())) {
            return new FilterResult();
        }
        final String clientIP = request.getClientAddress().getHostAddress();
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        if (!this.headerPatternsMatch(this.headerPatterns, request)) {
            return new FilterResult();
        }
        if (this.contentPattern != null && !this.contentPattern.asMatchPredicate().test(request.getPayloadString())) {
            return new FilterResult();
        }
        this.logText(request);
        return new FilterResult();
    }
    
    @Override
    public String[] getPacketTypes() {
        if (!this.methods.isEmpty()) {
            return this.methods.toArray(new String[0]);
        }
        return new String[0];
    }
    
    private void logText(final HttpRequest request) {
        String formatStr = "REQ $ts [$method $url] $payload";
        if (this.format != null && !this.format.isBlank()) {
            formatStr = this.format;
        }
        if (formatStr.contains("$ts")) {
            final String nowStr = ZonedDateTime.now().format(RequestLoggingFilter.logTsFormat);
            formatStr = formatStr.replace("$ts", nowStr);
        }
        if (formatStr.contains("$ip")) {
            formatStr = formatStr.replace("$ip", request.getClientAddress().getHostAddress());
        }
        if (formatStr.contains("$method")) {
            formatStr = formatStr.replace("$method", request.getMethod());
        }
        if (formatStr.contains("$url")) {
            formatStr = formatStr.replace("$url", request.getUrl());
        }
        if (formatStr.contains("$payload")) {
            String payload = request.getPayloadString();
            if (payload == null || payload.isBlank()) {
                payload = "[no payload]";
            }
            else {
                int maxLen = 100;
                if (this.maxPayload != null && this.maxPayload > 0) {
                    maxLen = this.maxPayload;
                }
                if (payload.length() > maxLen) {
                    payload = payload.substring(0, maxLen) + " [+" + (payload.length() - maxLen) + " chars]";
                }
            }
            formatStr = formatStr.replace("$payload", payload);
        }
        if (this.filter != null && !this.filter.matcher(formatStr).matches()) {
            return;
        }
        if (this.logger != null) {
            this.logger.info(formatStr);
        }
        else {
            this.out.println(formatStr);
            this.out.flush();
        }
    }
    
    static {
        logTsFormat = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss.SSS");
        log = LogManager.getLogger("galliumdata.core");
    }
}
