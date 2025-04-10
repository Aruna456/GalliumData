// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.dns.duplex;

import org.apache.logging.log4j.LogManager;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Iterator;
import com.galliumdata.server.handler.dns.answers.DNSAnswer;
import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.HashMap;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.handler.dns.DNSPacket;
import java.util.Map;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.logic.RequestFilter;

public class CachingFilter implements RequestFilter, ResponseFilter
{
    private FilterUse filterUse;
    private Set<String> questionTypes;
    private Set<Object> questionNames;
    private Map<DNSPacket, DNSPacket> cache;
    private int cacheSize;
    private static final Logger log;
    
    @Override
    public synchronized void configure(final FilterUse filterUse) {
        this.filterUse = filterUse;
        this.questionTypes = null;
        this.questionNames = null;
        this.cache = null;
        this.cacheSize = 100;
        final Variables filterContext = filterUse.getFilterContext();
        Map<String, Object> init = (Map<String, Object>)filterContext.get("_initialized");
        if (init != null) {
            this.questionTypes = (Set<String>) init.get("questionTypes");
            this.questionNames = (Set<Object>) init.get("questionNames");
            this.cache = (Map<DNSPacket, DNSPacket>) init.get("cache");
            final Integer sizeInt = (Integer) init.get("cacheSize");
            if (sizeInt != null) {
                this.cacheSize = sizeInt;
            }
            return;
        }
        init = new HashMap<String, Object>();
        filterContext.put("_initialized", init);
        final String typesStr = (String) filterUse.getParameters().get("Question types");
        if (typesStr != null && typesStr.trim().length() > 0) {
            final String[] split;
            final String[] typesParts = split = typesStr.split("\\.");
            for (final String typePart : split) {
                if (this.questionTypes == null) {
                    init.put("questionTypes", this.questionTypes = new HashSet<String>());
                }
                this.questionTypes.add(typePart.toUpperCase());
            }
        }
        final String questNamesStr = (String) filterUse.getParameters().get("Question names");
        if (questNamesStr != null && questNamesStr.trim().length() > 0) {
            final String[] split2;
            final String[] questNameParts = split2 = questNamesStr.split("\\.");
            for (final String questNamePart : split2) {
                if (this.questionNames == null) {
                    init.put("questionNames", this.questionNames = new HashSet<Object>());
                }
                if (questNamePart.startsWith("regex:")) {
                    final Pattern pattern = Pattern.compile(questNamePart.substring("regex:".length()), 106);
                    this.questionNames.add(pattern);
                }
                else if (questNamePart.startsWith("REGEX:")) {
                    final Pattern pattern = Pattern.compile(questNamePart.substring("regex:".length()));
                    this.questionNames.add(pattern);
                }
                else {
                    this.questionNames.add(questNamePart);
                }
            }
        }
        init.put("cache", this.cache = new ConcurrentHashMap<DNSPacket, DNSPacket>());
        final Integer sizeInt2 = (Integer) filterUse.getParameters().get("Cache size");
        if (sizeInt2 != null) {
            this.cacheSize = sizeInt2;
            if (this.cacheSize < 10) {
                this.cacheSize = 10;
            }
            if (this.cacheSize > 100000) {
                this.cacheSize = 100000;
            }
        }
        init.put("cacheSize", this.cacheSize);
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        final DNSPacket pkt = (DNSPacket)context.get("packet");
        if (!this.packetMatches(pkt)) {
            return new FilterResult();
        }
        final FilterResult result = new FilterResult();
        context.put("result", result);
        if (this.cache != null && this.cache.containsKey(pkt)) {
            final DNSPacket resp = this.cache.get(pkt);
            if (resp.hasExpired()) {
                this.cache.remove(pkt);
            }
            else {
                result.setResponse(resp);
                resp.setTransactionIdNoChange(pkt.getClientTransactionId());
                CachingFilter.log.trace(Markers.DNS, "Cache hit for: " + pkt.getQuestion(0).getName());
            }
        }
        if (result.getResponse() == null) {
            final Variables requestContext = (Variables)context.get("requestContext");
            requestContext.put("request", pkt);
        }
        final Source src = ScriptManager.getInstance().getSource(this.filterUse.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        return result;
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        DNSPacket pkt = (DNSPacket)context.get("packet");
        if (!this.packetMatches(pkt)) {
            return new FilterResult();
        }
        final FilterResult result = new FilterResult();
        context.put("result", result);
        final Source src = ScriptManager.getInstance().getSource(this.filterUse.getPath().toString());
        ScriptExecutor.executeFilterScript(src, result, context);
        if (result.isSuccess() && !result.isSkip()) {
            pkt = (DNSPacket)context.get("packet");
            final Variables requestContext = (Variables)context.get("requestContext");
            final DNSPacket request = (DNSPacket)requestContext.get("request");
            if (this.cache != null && this.cache.size() > this.cacheSize) {
                synchronized (this.cache) {
                    this.compressCache();
                }
            }
            if (request != null) {
                this.cache.put(request, pkt);
            }
        }
        return result;
    }
    
    @Override
    public String[] getPacketTypes() {
        return null;
    }
    
    @Override
    public String getName() {
        return "Caching duplex filter - DNS";
    }
    
    private boolean packetMatches(final DNSPacket pkt) {
        if (this.questionTypes != null) {
            boolean match = false;
            for (final String type : this.questionTypes) {
                for (final DNSAnswer answer : pkt.getAnswers()) {
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
        if (this.questionNames != null) {
            boolean match = false;
            for (final Object name : this.questionNames) {
                if (name instanceof String) {
                    for (final DNSAnswer answer : pkt.getAnswers()) {
                        if (name.equals(answer.getName())) {
                            match = true;
                            break;
                        }
                    }
                }
                else {
                    final Pattern pattern = (Pattern)name;
                    for (final DNSAnswer answer2 : pkt.getAnswers()) {
                        if (pattern.matcher(answer2.getName()).matches()) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) {
                    break;
                }
            }
            return match;
        }
        return true;
    }
    
    private void compressCache() {
        final int fullSize = this.cache.size();
        if (fullSize < this.cacheSize * 0.7) {
            return;
        }
        final Set<DNSPacket> orderedCache = new TreeSet<DNSPacket>((o1, o2) -> (int)(o1.getTimestamp() - o2.getTimestamp()));
        orderedCache.addAll(this.cache.keySet());
        int numRemoved = 0;
        final Set<DNSPacket> toRemove = new HashSet<DNSPacket>();
        for (final Map.Entry<DNSPacket, DNSPacket> p : this.cache.entrySet()) {
            if (p.getValue().hasExpired()) {
                toRemove.add(p.getKey());
                ++numRemoved;
            }
        }
        if (toRemove.size() > 0) {
            CachingFilter.log.trace(Markers.DNS, "DNS caching filter is removing " + toRemove.size() + " expired entries");
        }
        for (final DNSPacket p2 : toRemove) {
            this.cache.remove(p2);
        }
        if (this.cache.size() > this.cacheSize * 0.7) {
            for (final DNSPacket p2 : orderedCache) {
                this.cache.remove(p2);
                ++numRemoved;
                if (this.cache.size() < this.cacheSize / 2) {
                    break;
                }
            }
        }
        if (CachingFilter.log.isTraceEnabled()) {
            CachingFilter.log.trace(Markers.DNS, "DNS caching filter is purging the cache - size " + fullSize + ", purged: " + numRemoved);
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.uselog");
    }
}
