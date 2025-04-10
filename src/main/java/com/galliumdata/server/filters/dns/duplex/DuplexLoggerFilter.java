// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.duplex;

import com.galliumdata.server.handler.dns.answers.DNSAnswer;
import java.util.Iterator;
import java.util.regex.Pattern;
import com.galliumdata.server.handler.dns.DNSQuestion;
import com.galliumdata.server.handler.dns.DNSPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.log.Markers;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.logic.RequestFilter;

public class DuplexLoggerFilter implements RequestFilter, ResponseFilter
{
    private FilterUse def;
    private static final DateTimeFormatter logTsFormat;
    private PrintStream out;
    private Logger logger;
    private String format;
    private Set<String> questionTypes;
    private Set<Object> questionNames;
    private Set<String> answerTypes;
    private Set<Object> answerNames;
    private Set<Object> clientIps;
    private static final Logger log;
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.out = null;
        this.logger = null;
        this.format = null;
        this.questionTypes = null;
        this.questionNames = null;
        this.answerTypes = null;
        this.answerNames = null;
        this.clientIps = null;
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.out = (PrintStream) init.get("out");
            this.logger = (Logger) init.get("logger");
            this.format = init.get("format").toString();
            this.questionTypes = (Set<String>) init.get("questionTypes");
            this.questionNames = (Set<Object>) init.get("questionNames");
            this.answerTypes = (Set<String>) init.get("answerTypes");
            this.answerNames = (Set<Object>) init.get("answerNames");
            this.clientIps = (Set<Object>) init.get("clientIps");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
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
                DuplexLoggerFilter.log.error(Markers.POSTGRES, "Unable to open log file for requests: " + filename + ", reason: " + ex.getMessage());
                this.out = null;
            }
        }
        else {
            this.out = System.out;
        }
        filterContext.put("out", this.out);
        init.put("out", this.out);
        final String loggerName = (String) def.getParameters().get("Logger name");
        if (loggerName != null && loggerName.trim().length() > 0) {
            this.logger = LogManager.getLogger(loggerName);
        }
        else if (this.out == null) {
            this.logger = LogManager.getLogger("galliumdata.uselog");
        }
        filterContext.put("logger", this.logger);
        this.format = (String) def.getParameters().get("Format");
        if (this.format != null && this.format.trim().length() > 0) {
            filterContext.put("format", this.format);
            init.put("format", this.format);
        }
        final String typesStr = (String) def.getParameters().get("Question types");
        init.put("questionTypes", this.questionTypes = FilterUtils.readCommaSeparatedUpperCaseNames(typesStr));
        final String questNamesStr = (String) def.getParameters().get("Question names");
        init.put("questionNames", this.questionNames = FilterUtils.readCommaSeparatedNamesOrRegexes(questNamesStr));
        final String answerTypesStr = (String) def.getParameters().get("Answer types");
        init.put("answerTypes", this.answerTypes = FilterUtils.readCommaSeparatedUpperCaseNames(answerTypesStr));
        final String ansNamesStr = (String) def.getParameters().get("Answer names");
        init.put("answerNames", this.answerNames = FilterUtils.readCommaSeparatedNamesOrRegexes(ansNamesStr));
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        if (this.out == null) {
            return new FilterResult();
        }
        final DNSPacket pkt = (DNSPacket)context.get("packet");
        if (this.questionTypes != null && !this.questionTypes.isEmpty()) {
            boolean match = false;
            for (final String type : this.questionTypes) {
                for (final DNSQuestion question : pkt.getQuestions()) {
                    if (type.equals(question.getTypeName())) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    break;
                }
            }
            if (!match) {
                return new FilterResult();
            }
        }
        if (this.questionNames != null && !this.questionNames.isEmpty()) {
            boolean match = false;
            for (final Object name : this.questionNames) {
                if (name instanceof String) {
                    for (final DNSQuestion question : pkt.getQuestions()) {
                        if (name.equals(question.getName())) {
                            match = true;
                            break;
                        }
                    }
                }
                else {
                    final Pattern pattern = (Pattern)name;
                    for (final DNSQuestion question2 : pkt.getQuestions()) {
                        if (pattern.matcher(question2.getName()).matches()) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) {
                    break;
                }
            }
            if (!match) {
                return new FilterResult();
            }
        }
        if (this.clientIps != null && !this.clientIps.isEmpty()) {
            String clientAddress = context.get("clientAddress").toString();
            if (clientAddress.startsWith("/")) {
                clientAddress = clientAddress.substring(1);
            }
            boolean match2 = false;
            for (final Object clientIp : this.clientIps) {
                if (clientIp instanceof String) {
                    if (clientIp.equals(clientAddress)) {
                        match2 = true;
                        break;
                    }
                    continue;
                }
                else {
                    final Pattern pattern2 = (Pattern)clientIp;
                    if (pattern2.matcher(clientAddress).matches()) {
                        match2 = true;
                        break;
                    }
                    continue;
                }
            }
            if (!match2) {
                return new FilterResult();
            }
        }
        this.logText("DNS request: " + pkt.toString(), context);
        return new FilterResult();
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        if (this.out == null) {
            return new FilterResult();
        }
        final DNSPacket pkt = (DNSPacket)context.get("packet");
        if (this.answerTypes != null && !this.answerTypes.isEmpty()) {
            boolean match = false;
            for (final String type : this.answerTypes) {
                for (final DNSAnswer answer : pkt.getAnswers()) {
                    if (type.equals(answer.getTypeName())) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    break;
                }
                for (final DNSAnswer answer : pkt.getNameServers()) {
                    if (type.equals(answer.getTypeName())) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    break;
                }
                for (final DNSAnswer answer : pkt.getAdditionalRecords()) {
                    if (type.equals(answer.getTypeName())) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    break;
                }
            }
            if (!match) {
                return new FilterResult();
            }
        }
        if (this.answerNames != null && !this.answerNames.isEmpty()) {
            boolean match = false;
            for (final Object name : this.answerNames) {
                if (name instanceof String) {
                    for (final DNSAnswer answer : pkt.getAnswers()) {
                        if (name.equals(answer.getName())) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    }
                    for (final DNSAnswer answer : pkt.getNameServers()) {
                        if (name.equals(answer.getName())) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    }
                    for (final DNSAnswer answer : pkt.getAdditionalRecords()) {
                        if (name.equals(answer.getName())) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    }
                    continue;
                }
                else {
                    final Pattern pattern = (Pattern)name;
                    for (final DNSAnswer answer2 : pkt.getAnswers()) {
                        if (pattern.matcher(answer2.getName()).matches()) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    }
                    for (final DNSAnswer answer2 : pkt.getNameServers()) {
                        if (pattern.matcher(answer2.getName()).matches()) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    }
                    for (final DNSAnswer answer2 : pkt.getAdditionalRecords()) {
                        if (pattern.matcher(answer2.getName()).matches()) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        break;
                    }
                    continue;
                }
            }
            if (!match) {
                return new FilterResult();
            }
        }
        if (this.clientIps != null && !this.clientIps.isEmpty()) {
            String clientAddress = context.get("clientAddress").toString();
            if (clientAddress.startsWith("/")) {
                clientAddress = clientAddress.substring(1);
            }
            boolean match2 = false;
            for (final Object clientIp : this.clientIps) {
                if (clientIp instanceof String) {
                    if (clientIp.equals(clientAddress)) {
                        match2 = true;
                        break;
                    }
                    continue;
                }
                else {
                    final Pattern pattern2 = (Pattern)clientIp;
                    if (pattern2.matcher(clientAddress).matches()) {
                        match2 = true;
                        break;
                    }
                    continue;
                }
            }
            if (!match2) {
                return new FilterResult();
            }
        }
        this.logText("DNS response: " + pkt.toString(), context);
        return new FilterResult();
    }
    
    @Override
    public String getName() {
        return DuplexLoggerFilter.class.getName();
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
            final String nowStr = ZonedDateTime.now().format(DuplexLoggerFilter.logTsFormat);
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
        log = LogManager.getLogger("galliumdata.core");
    }
}
