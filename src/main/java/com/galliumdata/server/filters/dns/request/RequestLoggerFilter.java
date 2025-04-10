// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.request;

import com.galliumdata.server.handler.dns.DNSPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.log.Markers;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import com.galliumdata.server.repository.FilterUse;
import org.apache.logging.log4j.Logger;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;

public class RequestLoggerFilter extends DNSRequestFilter
{
    private static final DateTimeFormatter logTsFormat;
    private PrintStream out;
    private Logger logger;
    private String format;
    
    @Override
    public void configure(final FilterUse def) {
        super.configure(def);
        this.out = null;
        this.logger = null;
        this.format = null;
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized2");
        if (init != null) {
            this.out = (PrintStream) init.get("out");
            this.logger = (Logger) init.get("logger");
            this.format = init.get("format").toString();
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized2", init);
        final String baseFilename = (String) def.getParameters().get("File name");
        if (baseFilename != null && baseFilename.trim().length() > 0) {
            final Boolean useTimestamp = (Boolean) def.getParameters().get("Use timestamp in file name");
            String filename = baseFilename;
            final int dotIdx = filename.lastIndexOf(46);
            if (useTimestamp != null && useTimestamp) {
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
                RequestLoggerFilter.log.error(Markers.POSTGRES, "Unable to open log file for requests: " + filename + ", reason: " + ex.getMessage());
                this.out = null;
            }
        }
        else {
            this.out = System.out;
        }
        init.put("out", this.out);
        final String loggerName = (String) def.getParameters().get("Logger name");
        if (loggerName != null && loggerName.trim().length() > 0) {
            this.logger = LogManager.getLogger(loggerName);
        }
        else if (this.out == null) {
            this.logger = LogManager.getLogger("galliumdata.uselog");
        }
        init.put("logger", this.logger);
        this.format = def.getParameters().get("Format").toString();
        if (this.format != null && this.format.trim().length() > 0) {
            init.put("format", this.format);
        }
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        if (this.out == null) {
            return new FilterResult();
        }
        final DNSPacket pkt = (DNSPacket)context.get("packet");
        if (!this.requestPacketIsRelevant(context)) {
            return new FilterResult();
        }
        this.logText(pkt.toString(), context);
        return new FilterResult();
    }
    
    @Override
    public String getName() {
        return RequestLoggerFilter.class.getName();
    }
    
    @Override
    public String[] getPacketTypes() {
        return null;
    }
    
    private void logText(final String s, final Variables context) {
        String formatStr = "$ts [$clientIp-$thread] $message";
        if (this.format != null) {
            formatStr = this.format;
        }
        if (!formatStr.contains("$message")) {
            formatStr += " $message";
        }
        if (formatStr.contains("$ts")) {
            final String nowStr = ZonedDateTime.now().format(RequestLoggerFilter.logTsFormat);
            formatStr = formatStr.replace("$ts", nowStr);
        }
        if (formatStr.contains("$thread")) {
            final String threadStr = "" + Thread.currentThread().getId();
            formatStr = formatStr.replace("$thread", threadStr);
        }
        if (formatStr.contains("$clientIp")) {
            formatStr = formatStr.replace("$clientIp", context.get("clientAddress").toString());
        }
        formatStr = formatStr.replace("$message", s);
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
    }
}
