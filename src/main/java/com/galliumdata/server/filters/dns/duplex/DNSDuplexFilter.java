// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.duplex;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.handler.dns.answers.DNSAnswer;
import java.util.Iterator;
import java.util.regex.Pattern;
import com.galliumdata.server.handler.dns.DNSQuestion;
import com.galliumdata.server.handler.dns.DNSPacket;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.logic.RequestFilter;

public abstract class DNSDuplexFilter implements RequestFilter, ResponseFilter
{
    protected FilterUse def;
    protected Set<String> questionTypes;
    protected Set<Object> questionNames;
    protected Set<String> answerTypes;
    protected Set<Object> answerNames;
    protected Set<Object> clientIps;
    protected static final Logger log;
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.questionTypes = null;
        this.questionNames = null;
        this.answerTypes = null;
        this.answerNames = null;
        this.clientIps = null;
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.questionTypes = (Set<String>) init.get("questionTypes");
            this.questionNames = (Set<Object>) init.get("questionNames");
            this.answerTypes = (Set<String>) init.get("answerTypes");
            this.answerNames = (Set<Object>) init.get("answerNames");
            this.clientIps = (Set<Object>) init.get("clientIps");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String typesStr = (String) def.getParameters().get("Question types");
        init.put("questionTypes", this.questionTypes = FilterUtils.readCommaSeparatedUpperCaseNames(typesStr));
        final String questNamesStr = (String) def.getParameters().get("Question names");
        init.put("questionNames", this.questionNames = FilterUtils.readCommaSeparatedNamesOrRegexes(questNamesStr));
        final String ansTypesStr = (String) def.getParameters().get("Answer types");
        init.put("answerTypes", this.answerTypes = FilterUtils.readCommaSeparatedUpperCaseNames(ansTypesStr));
        final String ansNamesStr = (String) def.getParameters().get("Answer names");
        init.put("answerNames", this.answerNames = FilterUtils.readCommaSeparatedNamesOrRegexes(ansNamesStr));
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
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
        return this.clientAddressMatches(context);
    }
    
    protected boolean responsePacketIsRelevant(final Variables context) {
        final DNSPacket pkt = (DNSPacket)context.get("packet");
        if (this.answerTypes != null) {
            boolean match = false;
            for (final String type : this.answerTypes) {
                for (final DNSAnswer answer : pkt.getAllAnswers()) {
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
                return false;
            }
        }
        if (this.answerNames != null) {
            boolean match = false;
            for (final Object name : this.answerNames) {
                if (name instanceof String) {
                    for (final DNSAnswer answer : pkt.getAllAnswers()) {
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
                    for (final DNSAnswer answer2 : pkt.getAllAnswers()) {
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
        return this.clientAddressMatches(context);
    }
    
    private boolean clientAddressMatches(final Variables context) {
        if (this.clientIps != null && !this.clientIps.isEmpty()) {
            String clientAddress = context.get("clientAddress").toString();
            if (clientAddress.startsWith("/")) {
                clientAddress = clientAddress.substring(1);
            }
            boolean match = false;
            for (final Object clientIp : this.clientIps) {
                if (clientIp instanceof String) {
                    if (clientIp.equals(clientAddress)) {
                        match = true;
                        break;
                    }
                    continue;
                }
                else {
                    final Pattern pattern = (Pattern)clientIp;
                    if (pattern.matcher(clientAddress).matches()) {
                        match = true;
                        break;
                    }
                    continue;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
