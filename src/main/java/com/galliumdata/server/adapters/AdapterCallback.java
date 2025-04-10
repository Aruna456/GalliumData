// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import com.google.common.cache.CacheBuilder;
import org.apache.logging.log4j.LogManager;
import java.util.Set;
import org.apache.logging.log4j.Marker;
import com.galliumdata.server.logic.RequestFilter;
import org.apache.logging.log4j.util.Supplier;
import java.util.stream.Stream;
import java.util.function.Predicate;
import java.util.Objects;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import org.graalvm.polyglot.Source;
import com.galliumdata.server.logic.ConnectionFilter;
import com.galliumdata.server.metarepo.FilterImplementation;
import java.util.Iterator;
import com.galliumdata.server.metarepo.MetaRepository;
import com.galliumdata.server.logic.ScriptExecutor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import com.galliumdata.server.handler.mssql.MSSQLForwarder;
import org.graalvm.polyglot.proxy.ProxyObject;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.util.Utils;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.repository.FilterStage;
import com.galliumdata.server.metarepo.MetaRepositoryManager;
import java.net.Socket;
import java.util.HashMap;
import com.google.common.cache.LoadingCache;
import com.galliumdata.server.logic.ResponseFilter;
import com.google.common.cache.CacheLoader;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import com.galliumdata.server.repository.Project;

public class AdapterCallback
{
    private Project project;
    private final Variables adapterContext;
    private final Map<String, Boolean> requestFilterAvailability;
    private final Map<String, Boolean> responseFilterAvailability;
    public static final String DO_NOT_CALL = "_doNotCallFilters";
    private static final Logger log;
    private static CacheLoader<String, Class<? extends ResponseFilter>> respFiltCacheLoader;
    private static final LoadingCache<String, Class<? extends ResponseFilter>> responseFilterClassCache;
    
    public AdapterCallback(final Project project) {
        this.adapterContext = new Variables();
        this.requestFilterAvailability = new HashMap<String, Boolean>(20);
        this.responseFilterAvailability = new HashMap<String, Boolean>(20);
        this.project = project;
    }
    
    public AdapterCallbackResponse connectionRequested(final Socket socket, final Variables context) {
        final AdapterCallbackResponse response = new AdapterCallbackResponse();
        response.reject = false;
        response.errorMessage = null;
        final MetaRepository metarepo = MetaRepositoryManager.getMainRepository();
        for (final FilterUse conFilter : this.project.getFilters(FilterStage.CONNECTION).values()) {
            if (!conFilter.isActive()) {
                continue;
            }
            if (conFilter.getFilterType().equals("ConnectionCloseFilter")) {
                continue;
            }
            final FilterImplementation filterImpl = metarepo.getFilterType(conFilter.getFilterType());
            if (filterImpl == null) {
                throw new RepositoryException("repo.UnknownFilter", new Object[] { conFilter.getName(), conFilter.getFilterType() });
            }
            switch (filterImpl.getCodeType()) {
                case JAVA: {
                    final ConnectionFilter filter = filterImpl.getConnectionFilter(conFilter);
                    context.put("filterContext", conFilter.getFilterContext());
                    context.put("projectContext", this.project.getProjectContext());
                    context.put("adapterContext", this.adapterContext);
                    context.put("utils", new Utils());
                    context.put("socket", socket);
                    context.put("log", AdapterCallback.log);
                    final FilterResult filterResult = filter.acceptConnection(socket, context);
                    if (filterResult.isSuccess()) {
                        break;
                    }
                    response.reject = true;
                    response.errorMessage = filterResult.getErrorMessage();
                    if (AdapterCallback.log.isTraceEnabled()) {
                        AdapterCallback.log.trace(Markers.USER_LOGIC, "Filter {} has rejected connection with error message: {}", (Object)conFilter.getName(), (Object)filterResult.getErrorMessage());
                        break;
                    }
                    break;
                }
                case JAVASCRIPT: {
                    final FilterResult result = new FilterResult();
                    result.setSuccess(true);
                    result.setFilterName(conFilter.getName());
                    final Source src = ScriptManager.getInstance().getSource(filterImpl.getPath().toString());
                    context.put("socket", socket);
                    final FilterResult filterResult = new FilterResult();
                    context.put("log", AdapterCallback.log);
                    context.put("result", filterResult);
                    context.put("utils", new Utils());
                    context.put("parameters", ProxyObject.fromMap((Map)conFilter.getParameters()));
                    context.put("adapterContext", this.adapterContext);
                    context.put("filterContext", conFilter.getFilterContext());
                    context.put("projectContext", this.project.getProjectContext());
                    context.put("threadContext", MSSQLForwarder.getThreadContext());
                    if (filterImpl.canHaveCode() && Files.exists(conFilter.getPath(), new LinkOption[0])) {
                        final Source userSrc = ScriptManager.getInstance().getSource(conFilter.getPath().toString());
                        ScriptExecutor.executeFilterScript(userSrc, filterResult, context);
                    }
                    ScriptExecutor.executeFilterScript(src, filterResult, context);
                    if (filterResult.isSuccess()) {
                        break;
                    }
                    response.reject = true;
                    response.errorMessage = filterResult.getErrorMessage();
                    if (AdapterCallback.log.isTraceEnabled()) {
                        AdapterCallback.log.trace(Markers.USER_LOGIC, "Filter implementation {} for filter {} has rejected connection with error message: {}", (Object)filterImpl.getName(), (Object)conFilter.getName(), (Object)filterResult.getErrorMessage());
                        break;
                    }
                    break;
                }
                default: {
                    throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "filterType", filterImpl.getCodeType(), filterImpl.getName() });
                }
            }
            if (response.reject) {
                return response;
            }
        }
        return response;
    }
    
    public void connectionClosing(final Variables context) {
        final MetaRepository metarepo = MetaRepositoryManager.getMainRepository();
        for (final FilterUse conFilter : this.project.getFilters(FilterStage.CONNECTION).values()) {
            if (!conFilter.isActive()) {
                continue;
            }
            if (!conFilter.getFilterType().equals("ConnectionCloseFilter")) {
                continue;
            }
            final FilterImplementation filterImpl = metarepo.getFilterType(conFilter.getFilterType());
            if (filterImpl == null) {
                throw new RepositoryException("repo.UnknownFilter", new Object[] { conFilter.getName(), conFilter.getFilterType() });
            }
            switch (filterImpl.getCodeType()) {
                case JAVA: {
                    final Variables ctxt = new Variables();
                    ctxt.put("connectionContext", context);
                    final ConnectionFilter filter = filterImpl.getConnectionFilter(conFilter);
                    ctxt.put("filterContext", conFilter.getFilterContext());
                    ctxt.put("projectContext", this.project.getProjectContext());
                    ctxt.put("utils", new Utils());
                    ctxt.put("log", AdapterCallback.log);
                    filter.acceptConnection(null, ctxt);
                    continue;
                }
                default: {
                    throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "filterType", filterImpl.getCodeType(), filterImpl.getName() });
                }
            }
        }
    }
    
    public boolean hasFiltersForPacketType(final FilterStage filterStage, final String packetType) {
        if (filterStage == FilterStage.REQUEST) {
            final Boolean avail = this.requestFilterAvailability.get(packetType);
            if (avail != null) {
                return avail;
            }
        }
        else if (filterStage == FilterStage.RESPONSE) {
            final Boolean avail = this.responseFilterAvailability.get(packetType);
            if (avail != null) {
                return avail;
            }
        }
        final List<String> packetTypes = new LinkedList<String>();
        packetTypes.add(packetType);
        final boolean avail2 = this.hasFiltersForPacketTypes(filterStage, packetTypes);
        if (filterStage == FilterStage.REQUEST) {
            this.requestFilterAvailability.put(packetType, avail2);
        }
        if (filterStage == FilterStage.RESPONSE) {
            this.responseFilterAvailability.put(packetType, avail2);
        }
        return avail2;
    }
    
    public void resetCache() {
        this.requestFilterAvailability.clear();
        this.responseFilterAvailability.clear();
    }
    
    public boolean hasFiltersForPacketTypes(final FilterStage filterStage, final Collection<String> packetTypes) {
        final Map<String, FilterUse> filters = this.project.getFilters(filterStage);
        if (filters.size() == 0) {
            return false;
        }
        if (filterStage == FilterStage.CONNECTION) {
            for (final FilterUse use : filters.values()) {
                if (use.isActive()) {
                    return true;
                }
            }
        }
        for (final FilterUse use : filters.values()) {
            if (!use.isActive()) {
                continue;
            }
            final FilterImplementation filterImpl = MetaRepositoryManager.getMainRepository().getFilterType(use.getFilterType());
            String[] pktTypes = null;
            if (filterStage == FilterStage.REQUEST || filterStage == FilterStage.DUPLEX) {
                pktTypes = filterImpl.getRequestFilter(use).getPacketTypes();
            }
            if (filterStage == FilterStage.RESPONSE || filterStage == FilterStage.DUPLEX) {
                pktTypes = filterImpl.getResponseFilter(use).getPacketTypes();
            }
            if (pktTypes != null && pktTypes.length != 0) {
                final Stream<String> stream = Arrays.stream(pktTypes);
                Objects.requireNonNull(packetTypes);
                if (!stream.anyMatch(packetTypes::contains)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
    
    public AdapterCallbackResponse invokeRequestFilters(final String packetType, final Variables context) {
        final AdapterCallbackResponse response = new AdapterCallbackResponse();
        context.put("projectContext", this.project.getProjectContext());
        context.put("utils", new Utils());
        context.put("log", AdapterCallback.log);
        for (FilterUse reqFilter : this.project.getFilters(FilterStage.REQUEST).values()) {
            if (!reqFilter.isActive()) {
                continue;
            }
            final FilterImplementation filterImpl = MetaRepositoryManager.getMainRepository().getFilterType(reqFilter.getFilterType());
            if (filterImpl == null) {
                throw new RepositoryException("repo.UnknownFilter", new Object[] { reqFilter.getName(), reqFilter.getFilterType() });
            }
            if (filterImpl.getEditions() != null && !filterImpl.getEditions().contains("Community")) {
                AdapterCallback.log.debug(Markers.DB2, "Filter \"" + reqFilter.getName() + "\" will not be executed because it is not allowed in this edition of Gallium Data.");
            }
            else {
                String[] pktTypes = filterImpl.getRequestFilter(reqFilter).getPacketTypes();
                if (pktTypes != null && pktTypes.length > 0) {
                    final Stream<String> stream = Arrays.stream(pktTypes);
                    Objects.requireNonNull(packetType);
                    if (stream.noneMatch(packetType::equals)) {
                        continue;
                    }
                }
                FilterResult result = null;
                switch (filterImpl.getCodeType()) {
                    case JAVA: {
                        context.put("filterContext", reqFilter.getFilterContext());
                        context.put("packetType", packetType);
                        final RequestFilter filter = filterImpl.getRequestFilter(reqFilter);
                        pktTypes = filterImpl.getRequestFilter(reqFilter).getPacketTypes();
                        if (pktTypes != null && pktTypes.length > 0) {
                            final Stream<String> stream2 = Arrays.stream(pktTypes);
                            Objects.requireNonNull(packetType);
                            if (stream2.noneMatch(packetType::equals)) {
                                continue;
                            }
                        }
                        result = filter.filterRequest(context);
                        break;
                    }
                    case JAVASCRIPT: {
                        final Logger log = AdapterCallback.log;
                        final Marker user_LOGIC = Markers.USER_LOGIC;
                        final String s = "JavaScript request filter called: {}";
                        final Supplier[] array = { null };
                        final int n = 0;
                        final FilterUse obj = reqFilter;
                        Objects.requireNonNull(obj);
                        array[n] = obj::getName;
                        log.trace(user_LOGIC, s, array);
                        result = new FilterResult();
                        result.setSuccess(true);
                        result.setFilterName(reqFilter.getName());
                        final Source src = ScriptManager.getInstance().getSource(reqFilter.getPath().toString());
                        result = new FilterResult();
                        context.put("result", result);
                        context.put("parameters", ProxyObject.fromMap((Map)reqFilter.getParameters()));
                        context.put("filterContext", reqFilter.getFilterContext());
                        ScriptExecutor.executeFilterScript(src, result, context);
                        break;
                    }
                    default: {
                        throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "filterType", filterImpl.getCodeType(), filterImpl.getName() });
                    }
                }
                if (response.response == null) {
                    response.response = result.getResponse();
                }
                else if (result.getResponse() != null) {
                    AdapterCallback.log.debug(Markers.USER_LOGIC, "Response has been set by more than one filter, the first one will be used");
                }
                if (result.isSkip()) {
                    response.skip = true;
                }
                if (!result.isSuccess() || result.getErrorMessage() != null || result.getErrorCode() != 0) {
                    response.reject = true;
                    response.errorMessage = result.getErrorMessage();
                    response.errorCode = result.getErrorCode();
                    response.errorParameters = result.getErrorParameters();
                    response.sqlStatus = result.getSqlStatus();
                    response.errorResponse = result.getErrorResponse();
                    response.closeConnection = result.isCloseConnection();
                    response.logicName = reqFilter.getName();
                }
                if (response.reject) {
                    return response;
                }
                continue;
            }
        }
        return response;
    }
    
    public AdapterCallbackResponse invokeResponseFilters(final String packetType, final Variables context) {
        final AdapterCallbackResponse response = new AdapterCallbackResponse();
        response.reject = false;
        response.errorMessage = null;
        final MetaRepository metarepo = MetaRepositoryManager.getMainRepository();
        context.put("projectContext", this.project.getProjectContext());
        context.put("utils", new Utils());
        context.put("log", AdapterCallback.log);
        context.put("packetType", packetType);
        final Variables connCtxt = (Variables)context.get("connectionContext");
        final Set<Integer> doNotCallRules = (Set<Integer>)connCtxt.get("_doNotCallFilters");
        for (FilterUse respFilter : this.project.getFilters(FilterStage.RESPONSE).values()) {
            if (!respFilter.isActive()) {
                continue;
            }
            if (doNotCallRules != null && doNotCallRules.contains(respFilter.hashCode())) {
                continue;
            }
            final FilterImplementation filterImpl = metarepo.getFilterType(respFilter.getFilterType());
            if (filterImpl == null) {
                throw new RepositoryException("repo.UnknownFilter", new Object[] { respFilter.getName(), respFilter.getFilterType() });
            }
            switch (filterImpl.getCodeType()) {
                case JAVA: {
                    final ResponseFilter filter = filterImpl.getResponseFilter(respFilter);
                    final String[] pktTypes = filterImpl.getResponseFilter(respFilter).getPacketTypes();
                    if (pktTypes != null && pktTypes.length > 0) {
                        int i;
                        for (i = 0; i < pktTypes.length && !pktTypes[i].equals(packetType); ++i) {}
                        if (i == pktTypes.length) {
                            continue;
                        }
                    }
                    context.put("filterContext", respFilter.getFilterContext());
                    try {
                        final FilterResult result = filter.filterResponse(context);
                        if (!result.isSuccess()) {
                            response.reject = true;
                            response.errorMessage = result.getErrorMessage();
                            response.errorCode = result.getErrorCode();
                            response.closeConnection = result.isCloseConnection();
                        }
                        if (result.isCloseConnection()) {
                            response.closeConnection = true;
                        }
                        if (result.isDoNotCall()) {
                            response.doNotCall = true;
                        }
                    }
                    catch (final Exception ex) {
                        if (AdapterCallback.log.isDebugEnabled()) {
                            AdapterCallback.log.debug(Markers.USER_LOGIC, "Exception while executing filter " + filter.getName() + ": " + ex.getMessage());
                        }
                    }
                    break;
                }
                case JAVASCRIPT: {
                    final Logger log = AdapterCallback.log;
                    final Marker user_LOGIC = Markers.USER_LOGIC;
                    final String s = "JavaScript request filter called: {}";
                    final Supplier[] array = { null };
                    final int n = 0;
                    final FilterUse obj = respFilter;
                    Objects.requireNonNull(obj);
                    array[n] = obj::getName;
                    log.trace(user_LOGIC, s, array);
                    final FilterResult result = new FilterResult();
                    result.setSuccess(true);
                    result.setFilterName(respFilter.getName());
                    final Source src = ScriptManager.getInstance().getSource(respFilter.getPath().toString());
                    context.putAll(context);
                    context.put("result", result);
                    context.put("parameters", ProxyObject.fromMap((Map)respFilter.getParameters()));
                    context.put("filterContext", respFilter.getFilterContext());
                    ScriptExecutor.executeFilterScript(src, result, context);
                    if (!result.isSuccess()) {
                        response.reject = true;
                        response.errorMessage = result.getErrorMessage();
                        break;
                    }
                    break;
                }
                default: {
                    throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "filterType", filterImpl.getCodeType(), filterImpl.getName() });
                }
            }
            if (!response.doNotCall || doNotCallRules == null) {
                continue;
            }
            doNotCallRules.add(respFilter.hashCode());
        }
        return response;
    }
    
    public void setProject(final Project project) {
        this.project = project;
    }
    
    public static void flushCaches() {
        AdapterCallback.responseFilterClassCache.invalidateAll();
    }
    
    private static Class<? extends ResponseFilter> getResponseFilterClass(final String name) {
        return (Class)AdapterCallback.responseFilterClassCache.getUnchecked((String) name);
    }
    
    static {
        log = LogManager.getLogger("galliumdata.uselog");
        AdapterCallback.respFiltCacheLoader = new CacheLoader<String, Class<? extends ResponseFilter>>() {
            public Class<? extends ResponseFilter> load(final String className) throws Exception {
                try {
                    return (Class<? extends ResponseFilter>)Class.forName(className);
                }
                catch (final Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        responseFilterClassCache = CacheBuilder.newBuilder().maximumSize(100L).build((CacheLoader)AdapterCallback.respFiltCacheLoader);
    }
}
