// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.request;

import com.galliumdata.server.handler.dns.answers.DNSAnswerAAAA;
import com.galliumdata.server.handler.dns.answers.DNSAnswerA;
import com.galliumdata.server.handler.dns.DNSQuestion;
import com.galliumdata.server.handler.dns.DNSPacket;
import com.galliumdata.server.logic.FilterResult;
import java.util.Iterator;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.log.Markers;
import java.util.HashMap;
import java.util.Map;
import com.galliumdata.server.repository.FilterUse;

public class AddressLookupFilter extends DNSRequestFilter
{
    private Boolean skip;
    private Integer responseCode;
    private Boolean authoritative;
    private String answerIP4;
    private String answerIP6;
    private Integer ttl;
    
    @Override
    public void configure(final FilterUse def) {
        super.configure(def);
        if (this.questionTypes.size() == 0) {
            this.setQuestionTypes("A,AAAA");
        }
        this.skip = false;
        this.responseCode = 0;
        this.authoritative = false;
        this.answerIP4 = null;
        this.answerIP6 = null;
        this.ttl = 0;
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized2");
        if (init != null) {
            this.skip = (Boolean) init.get("skip");
            this.responseCode = (Integer) init.get("responseCode");
            this.authoritative = (Boolean) init.get("authoritative");
            this.answerIP4 = init.get("answerIP4").toString();
            this.answerIP6 = init.get("answerIP6").toString();
            this.ttl = (Integer) init.get("ttl");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized2", init);
        for (String qType : this.questionTypes) {
            if (!"A".equals(qType) && !"AAAA".equals(qType)) {
                AddressLookupFilter.log.debug(Markers.DNS, "Invalid value for Question types parameter: " + qType + ", valid values are A,AAAA, both or nothing");
                break;
            }
        }
        init.put("skip", this.skip = (Boolean) def.getParameters().get("Skip packet"));
        init.put("responseCode", this.responseCode = (Integer) def.getParameters().get("Response code"));
        init.put("authoritative", this.authoritative = (Boolean) def.getParameters().get("Set authoritative"));
        init.put("answerIP4", this.answerIP4 = (String) def.getParameters().get("Answer for IP4"));
        init.put("answerIP6", this.answerIP6 = (String) def.getParameters().get("Answer for IP6"));
        init.put("ttl", this.ttl = (Integer) def.getParameters().get("Time to live"));
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final DNSPacket pkt = (DNSPacket)context.get("packet");
        final FilterResult result = new FilterResult();
        if (!this.requestPacketIsRelevant(context)) {
            return result;
        }
        if (this.skip != null && this.skip) {
            result.setSkip(true);
            if (AddressLookupFilter.log.isTraceEnabled()) {
                AddressLookupFilter.log.trace(Markers.USER_LOGIC, "DNS address request skipped for " + String.valueOf(pkt.getQuestion(0)));
            }
            return result;
        }
        pkt.setQuery(false);
        pkt.removeAnswers();
        pkt.removeNameServers();
        pkt.removeAdditionalRecords();
        result.setResponse(pkt);
        pkt.setResponseCode((byte)((this.responseCode != null) ? this.responseCode.byteValue() : 0));
        pkt.setAuthoritative(this.authoritative != null && this.authoritative);
        if (this.responseCode != null && this.responseCode > 0) {
            AddressLookupFilter.log.trace(Markers.USER_LOGIC, "DNS address request: returning error code " + this.responseCode + " for " + String.valueOf(pkt.getQuestion(0)));
            return result;
        }
        if (this.answerIP4 != null && this.answerIP4.trim().length() > 0) {
            for (final DNSQuestion q : pkt.getQuestions()) {
                if (!"A".equals(q.getTypeName())) {
                    continue;
                }
                final DNSAnswerA ans = (DNSAnswerA)pkt.addAnswer("A");
                ans.setName(q.getName());
                ans.setIpAddress(this.answerIP4);
                if (this.ttl != null && this.ttl > 0) {
                    ans.setTtl(this.ttl);
                }
                else {
                    ans.setTtl(3600);
                }
            }
        }
        if (this.answerIP6 != null && this.answerIP6.trim().length() > 0) {
            for (final DNSQuestion q : pkt.getQuestions()) {
                if (!"AAAA".equals(q.getTypeName())) {
                    continue;
                }
                final DNSAnswerAAAA ans2 = (DNSAnswerAAAA)pkt.addAnswer("AAAA");
                ans2.setName(q.getName());
                ans2.setIpAddress(this.answerIP6);
                if (this.ttl != null && this.ttl > 0) {
                    ans2.setTtl(this.ttl);
                }
                else {
                    ans2.setTtl(3600);
                }
            }
        }
        return result;
    }
    
    @Override
    public String getName() {
        return "Address lookup filter - DNS";
    }
    
    @Override
    public String[] getPacketTypes() {
        return null;
    }
}
