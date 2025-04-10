// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.request;

import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.regex.Pattern;
import com.galliumdata.server.handler.dns.DNSQuestion;
import com.galliumdata.server.handler.dns.DNSPacket;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.RequestFilter;

public abstract class DNSRequestFilter implements RequestFilter
{
    protected FilterUse def;
    protected Set<String> questionTypes;
    protected Set<Object> questionNames;
    protected Set<Object> clientIps;
    protected static final Logger log;
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.questionTypes = null;
        this.questionNames = null;
        this.clientIps = null;
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.questionTypes = (Set<String>) init.get("questionTypes");
            this.questionNames = (Set<Object>) init.get("questionNames");
            this.clientIps = Collections.singleton(init.get("clientIps"));
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String typesStr = (String) def.getParameters().get("Question types");
        init.put("questionTypes", this.questionTypes = FilterUtils.readCommaSeparatedUpperCaseNames(typesStr));
        final String questNamesStr = (String) def.getParameters().get("Question names");
        init.put("questionNames", this.questionNames = FilterUtils.readCommaSeparatedNamesOrRegexes(questNamesStr));
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
    }
    
    protected void setQuestionTypes(final String types) {
        this.questionTypes = FilterUtils.readCommaSeparatedUpperCaseNames(types);
        final Map<String, Object> init = (Map<String, Object>)this.def.getFilterContext().get("_initialized");
        init.put("questionTypes", this.questionTypes);
    }
    
    protected boolean requestPacketIsRelevant(final Variables context) {
        final DNSPacket pkt = (DNSPacket)context.get("packet");
        if (this.questionTypes != null) {
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
                return false;
            }
        }
        if (this.questionNames != null) {
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
