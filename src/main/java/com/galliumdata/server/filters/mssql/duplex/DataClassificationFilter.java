// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.mssql.duplex;

import com.galliumdata.server.handler.mssql.tokens.TokenDataClassification;
import com.galliumdata.server.handler.mssql.tokens.TokenFeatureExtAck;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7Feature;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureDataClassification;
import com.galliumdata.server.handler.mssql.Login7Packet;
import java.util.Collection;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.FilterUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.logic.RequestFilter;

public class DataClassificationFilter implements RequestFilter, ResponseFilter
{
    private FilterUse def;
    protected Set<Object> clientIps;
    private static final String FEATURE_ADDED_NAME = "duplex_filters.DataClassification.DataClassificationAddedToLogin";
    
    @Override
    public void configure(final FilterUse def) {
        this.def = def;
        this.clientIps = new HashSet<Object>();
        final Variables filterContext = def.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.clientIps = (Set<Object>) init.get ("clientIps");
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String clientIpStr = (String) def.getParameters().get("Client IPs");
        init.put("clientIps", this.clientIps = FilterUtils.readCommaSeparatedNamesOrRegexes(clientIpStr));
    }
    
    @Override
    public String getName() {
        return "Data classification filter";
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final FilterResult result = new FilterResult();
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return result;
        }
        final Object packet = context.get("packet");
        if (!(packet instanceof Login7Packet)) {
            return result;
        }
        final Login7Packet loginPkt = (Login7Packet)packet;
        final Login7Feature feature = loginPkt.getFeature("DataClassification");
        if (feature != null) {
            return result;
        }
        final Login7FeatureDataClassification dcFeature = (Login7FeatureDataClassification)loginPkt.addFeature("DataClassification");
        dcFeature.setVersion((byte)1);
        final Variables connCtxt = (Variables)context.get("connectionContext");
        connCtxt.put("duplex_filters.DataClassification.DataClassificationAddedToLogin", "true");
        return result;
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        final FilterResult result = new FilterResult();
        final Variables connectionContext = (Variables)context.get("connectionContext");
        final String clientIP = (String)connectionContext.get("userIP");
        if (!FilterUtils.stringMatchesNamesOrRegexes(clientIP, this.clientIps)) {
            return result;
        }
        final Variables connCtxt = (Variables)context.get("connectionContext");
        if (!"true".equals(connCtxt.get("duplex_filters.DataClassification.DataClassificationAddedToLogin"))) {
            return result;
        }
        final Object packet = context.get("packet");
        if (packet instanceof TokenFeatureExtAck) {
            final TokenFeatureExtAck feature = (TokenFeatureExtAck)packet;
            feature.removeFeatureAck("DataClassification");
        }
        else if (packet instanceof TokenDataClassification) {
            final TokenDataClassification token = (TokenDataClassification)packet;
            token.remove();
        }
        return result;
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "Login7", "FeatureExtAck", "DataClassification" };
    }
}
