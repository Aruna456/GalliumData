// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.request;

import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import java.util.Collection;
import com.galliumdata.server.handler.GenericPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.log.Markers;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.HashSet;
import com.galliumdata.server.repository.FilterUse;
import java.util.regex.Pattern;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import com.galliumdata.server.filters.mssql.GeneralFilter;

public class RequestLoggerFilter extends GeneralFilter
{
    private static final DateTimeFormatter logTsFormat;
    private PrintStream out;
    private Logger logger;
    private String format;
    protected Set<String> packetTypes;
    protected Set<Object> clientIps;
    protected Set<Object> users;
    private Boolean verbose;
    protected Pattern filter;
    
    @Override
    public void configure(final FilterUse def) {
        this.out = null;
        this.logger = null;
        this.format = null;
        this.packetTypes = new HashSet<String>();
        this.clientIps = new HashSet<Object>();
        this.users = new HashSet<Object>();
        this.verbose = false;
        this.filter = null;
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.out = (PrintStream) init.get("out");
            this.logger = (Logger) init.get("logger");
            this.format = init.get("format").toString();
            this.packetTypes = (Set<String>) init.get("packetTypes");
            this.clientIps = (Set<Object>) init.get("clientIps");
            this.users = (Set<Object>) init.get("users");
            this.verbose = (Boolean) init.get("verbose");
            this.filter = (Pattern) init.get("filter");
            return;
        }
        super.configure(def);
        init = (Map)filterContext.get("_initialized");
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
            init.put("logger", this.logger = LogManager.getLogger(loggerName));
        }
        else if (this.out == null) {
            this.logger = LogManager.getLogger("galliumdata.uselog");
        }
        this.format = def.getParameters().get("Format").toString();
        if (this.format != null && this.format.trim().length() > 0) {
            init.put("format", this.format);
        }
        final String packetTypesStr = (String) def.getParameters().get("Packet types");
        init.put("packetTypes", this.packetTypes = FilterUtils.readCommaSeparatedNames(packetTypesStr));
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
        final String usersStr = (String) def.getParameters().get("Users");
        init.put("users", this.users = FilterUtils.readCommaSeparatedNamesOrRegexes(usersStr));
        this.verbose = (Boolean) def.getParameters().get("Verbose");
        if (this.verbose != null) {
            init.put("verbose", this.verbose);
        }
        final String filterStr = (String) def.getParameters().get("Filter");
        if (filterStr != null && !filterStr.isBlank()) {
            init.put("filter", this.filter = Pattern.compile(filterStr, 40));
        }
        filterContext.put("_initialized", init);
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        if (super.skipInvocation(context)) {
            return new FilterResult();
        }
        if (this.out == null) {
            return new FilterResult();
        }
        final GenericPacket pkt = (GenericPacket)context.get("packet");
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return new FilterResult();
        }
        final String username = (String)connectionContext.get("userName");
        if (username != null && this.users != null && this.users.size() > 0 && !FilterUtils.stringMatchesNamesOrRegexes(username, this.users)) {
            return new FilterResult();
        }
        final String pktType = pkt.getPacketType();
        if (this.packetTypes != null && !this.packetTypes.contains(pktType)) {
            return new FilterResult();
        }
        String msg = null;
        if (this.verbose != null && this.verbose) {
            if (pkt instanceof MSSQLPacket) {
                final MSSQLPacket sqlPkt = (MSSQLPacket)pkt;
                msg = sqlPkt.toLongString();
            }
            else if (pkt instanceof MessageToken) {
                final MessageToken msgToken = (MessageToken)pkt;
                msg = msgToken.toLongString();
            }
            else {
                msg = pkt.toString();
            }
        }
        else {
            msg = pkt.toString();
        }
        this.logText(msg, context);
        return new FilterResult();
    }
    
    @Override
    public String getName() {
        return "Request logger - MSSQL";
    }
    
    @Override
    public String[] getPacketTypes() {
        if (this.packetTypes == null || this.packetTypes.size() == 0) {
            return null;
        }
        return this.packetTypes.toArray(new String[0]);
    }
    
    private void logText(final String s, final Variables context) {
        String formatStr = "$ts [$user] $message";
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
        if (formatStr.contains("$user")) {
            final Variables connCtxt = (Variables)context.get("connectionContext");
            String username = (String)connCtxt.get("userName");
            if (username == null) {
                username = "<none>";
            }
            formatStr = formatStr.replace("$user", username);
        }
        if (formatStr.contains("$ip")) {
            final Variables connCtxt = (Variables)context.get("connectionContext");
            final String ip = (String)connCtxt.get("userIP");
            formatStr = formatStr.replace("$ip", ip);
        }
        if (formatStr.contains("$thread")) {
            final String threadStr = "" + Thread.currentThread().getId();
            formatStr = formatStr.replace("$thread", threadStr);
        }
        if (formatStr.contains("$message")) {
            formatStr = formatStr.replace("$message", s);
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
    }
}
