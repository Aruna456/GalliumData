// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.response;

import org.apache.logging.log4j.LogManager;
import java.util.Iterator;
import com.galliumdata.server.handler.dns.answers.DNSAnswer;
import com.galliumdata.server.handler.dns.DNSQuestion;
import com.galliumdata.server.handler.dns.DNSPacket;
import com.galliumdata.server.adapters.Variables;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;

public abstract class DNSResponseFilter implements ResponseFilter
{
    protected FilterUse def;
    protected Set<String> answerTypes;
    protected Set<Object> answerNames;
    protected Set<Object> clientIps;
    protected static final Logger log;
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.answerTypes = null;
        this.answerNames = null;
        this.clientIps = null;
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.answerTypes = (Set<String>) init.get("answerTypes");
            this.answerNames = (Set<Object>) init.get("answerNames");
            this.clientIps = (Set<Object>) init.get("clientIps");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String typesStr = (String) def.getParameters().get("Answer types");
        if (typesStr != null && typesStr.trim().length() > 0) {
            final String[] split;
            final String[] typesParts = split = typesStr.split(",");
            for (final String typePart : split) {
                if (this.answerTypes == null) {
                    init.put("answerTypes", this.answerTypes = new HashSet<String>());
                }
                this.answerTypes.add(typePart.toUpperCase());
            }
        }
        final String questNamesStr = (String) def.getParameters().get("Answer names");
        if (questNamesStr != null && questNamesStr.trim().length() > 0) {
            final String[] split2;
            final String[] questNameParts = split2 = questNamesStr.split(",");
            for (final String questNamePart : split2) {
                if (this.answerNames == null) {
                    init.put("answerNames", this.answerNames = new HashSet<Object>());
                }
                if (questNamePart.startsWith("regex:")) {
                    final Pattern pattern = Pattern.compile(questNamePart.substring("regex:".length()), 106);
                    this.answerNames.add(pattern);
                }
                else if (questNamePart.startsWith("REGEX:")) {
                    final Pattern pattern = Pattern.compile(questNamePart.substring("regex:".length()));
                    this.answerNames.add(pattern);
                }
                else {
                    this.answerNames.add(questNamePart);
                }
            }
        }
        String clientIpStr = (String) def.getParameters().get("Client IPs");
        if (clientIpStr != null && clientIpStr.trim().length() > 0) {
            clientIpStr = clientIpStr.trim();
            final String[] split3;
            final String[] clientIpParts = split3 = clientIpStr.split(",");
            for (final String clientIpPart : split3) {
                if (this.clientIps == null) {
                    init.put("clientIps", this.clientIps = new HashSet<Object>());
                }
                if (clientIpPart.startsWith("regex:")) {
                    final Pattern pattern2 = Pattern.compile(clientIpPart.substring("regex:".length()), 106);
                    this.clientIps.add(pattern2);
                }
                else if (clientIpPart.startsWith("REGEX:")) {
                    final Pattern pattern2 = Pattern.compile(clientIpPart.substring("regex:".length()));
                    this.clientIps.add(pattern2);
                }
                else {
                    this.clientIps.add(clientIpPart);
                }
            }
        }
    }
    
    protected boolean responsePacketIsRelevant(final Variables context) {
        final DNSPacket pkt = (DNSPacket)context.get("packet");
        if (this.answerTypes != null) {
            boolean match = false;
            for (final String type : this.answerTypes) {
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
                return false;
            }
        }
        if (this.answerNames != null) {
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
                return false;
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
                return false;
            }
        }
        return true;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
