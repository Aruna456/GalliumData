// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.metarepo;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.ServerException;
import com.fasterxml.jackson.databind.JsonNode;
import com.galliumdata.server.repository.RepositoryException;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Path;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.logic.ResponseFilter;
import com.galliumdata.server.logic.RequestFilter;
import com.galliumdata.server.repository.FilterUse;
import java.util.Map;
import com.galliumdata.server.logic.ConnectionFilter;
import com.galliumdata.server.repository.FilterStage;

public class FilterImplementation extends MetaRepositoryObject
{
    private final FilterStage filterStage;
    private final String name;
    private final CodeType codeType;
    private final String connectionType;
    private final boolean canHaveCode;
    private final String editions;
    private final String implementation;
    private Class<? extends ConnectionFilter> connectionFilterClass;
    private final Map<FilterUse, ConnectionFilter> connectionFilters;
    private Class<? extends RequestFilter> requestFilterClass;
    private final Map<FilterUse, RequestFilter> requestFilters;
    private Class<? extends ResponseFilter> responseFilterClass;
    private final Map<FilterUse, ResponseFilter> responseFilters;
    protected static final Logger log;
    
    public FilterImplementation(final Path path, final FilterStage filterStage, final String name, final String codeType, final String implementation, final boolean canHaveCode, final String connectionType, final String editions) {
        super(path);
        this.connectionFilters = new ConcurrentHashMap<FilterUse, ConnectionFilter>();
        this.requestFilters = new ConcurrentHashMap<FilterUse, RequestFilter>();
        this.responseFilters = new ConcurrentHashMap<FilterUse, ResponseFilter>();
        this.filterStage = filterStage;
        this.name = name;
        this.codeType = CodeType.forString(codeType);
        if (this.codeType == null) {
            throw new RepositoryException("metarepo.InvalidCodeType", new Object[] { codeType, path.toAbsolutePath() });
        }
        this.implementation = implementation;
        this.canHaveCode = canHaveCode;
        this.connectionType = connectionType;
        this.editions = editions;
    }
    
    public FilterStage getFilterStage() {
        return this.filterStage;
    }
    
    public Path getPath() {
        return this.path;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    public CodeType getCodeType() {
        return this.codeType;
    }
    
    public String getImplementation() {
        return this.implementation;
    }
    
    public boolean canHaveCode() {
        return this.canHaveCode;
    }
    
    public String getEditions() {
        return this.editions;
    }
    
    @Override
    protected String getJsonFileName() {
        return "filter.json";
    }
    
    @Override
    protected void processJson(final JsonNode node) {
    }
    
    public ConnectionFilter getConnectionFilter(final FilterUse use) {
        synchronized (this) {
            if (this.connectionFilterClass == null) {
                try {
                    this.connectionFilterClass = (Class<? extends ConnectionFilter>)Class.forName(this.implementation);
                }
                catch (final Exception ex) {
                    throw new ServerException("logic.ErrorInstantiatingJavaFilter", new Object[] { this.implementation, ex.getMessage() });
                }
            }
        }
        synchronized (this.connectionFilters) {
            if (this.connectionFilters.get(use) == null) {
                ConnectionFilter filter;
                try {
                    filter = (ConnectionFilter)this.connectionFilterClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
                    this.connectionFilters.put(use, filter);
                }
                catch (final Exception ex2) {
                    throw new ServerException("logic.ErrorInstantiatingJavaFilter", new Object[] { this.implementation, ex2.getMessage() });
                }
                try {
                    filter.configure(use);
                }
                catch (final Exception ex2) {
                    FilterImplementation.log.debug(Markers.USER_LOGIC, "Error configuring filter " + use.getName() + ": " + ex2.getMessage());
                    throw new ServerException("logic.ErrorConfiguringJavaFilter", new Object[] { this.implementation, ex2.getMessage() });
                }
            }
        }
        return this.connectionFilters.get(use);
    }
    
    public RequestFilter getRequestFilter(final FilterUse use) {
        synchronized (this) {
            if (this.requestFilterClass == null) {
                try {
                    this.requestFilterClass = (Class<? extends RequestFilter>)Class.forName(this.implementation);
                }
                catch (final Exception ex) {
                    throw new ServerException("logic.ErrorInstantiatingJavaFilter", new Object[] { this.implementation, ex.getMessage() });
                }
            }
        }
        synchronized (this.requestFilters) {
            if (this.requestFilters.get(use) == null) {
                RequestFilter filter;
                try {
                    filter = (RequestFilter)this.requestFilterClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
                    this.requestFilters.put(use, filter);
                }
                catch (final Exception ex2) {
                    throw new ServerException("logic.ErrorInstantiatingJavaFilter", new Object[] { this.implementation, ex2.getMessage() });
                }
                try {
                    filter.configure(use);
                }
                catch (final Exception ex2) {
                    FilterImplementation.log.debug(Markers.USER_LOGIC, "Error configuring filter " + use.getName() + ": " + ex2.getMessage());
                    throw new ServerException("logic.ErrorConfiguringJavaFilter", new Object[] { this.implementation, ex2.getMessage() });
                }
            }
        }
        return this.requestFilters.get(use);
    }
    
    public ResponseFilter getResponseFilter(final FilterUse use) {
        if (this.responseFilterClass == null) {
            synchronized (this) {
                if (this.responseFilterClass == null) {
                    try {
                        this.responseFilterClass = (Class<? extends ResponseFilter>)Class.forName(this.implementation);
                    }
                    catch (final Exception ex) {
                        throw new ServerException("logic.ErrorInstantiatingJavaFilter", new Object[] { this.implementation, ex.getMessage() });
                    }
                }
            }
        }
        ResponseFilter result = this.responseFilters.get(use);
        if (result == null) {
            synchronized (this.responseFilters) {
                result = this.responseFilters.get(use);
                if (result == null) {
                    try {
                        result = (ResponseFilter)this.responseFilterClass.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
                        this.responseFilters.put(use, result);
                    }
                    catch (final Exception ex2) {
                        FilterImplementation.log.error(Markers.USER_LOGIC, "Error instantiating filter " + use.getName() + ": " + ex2.getMessage());
                        throw new ServerException("logic.ErrorInstantiatingJavaFilter", new Object[] { this.implementation, ex2.getMessage() });
                    }
                    try {
                        result.configure(use);
                    }
                    catch (final Exception ex2) {
                        FilterImplementation.log.error(Markers.USER_LOGIC, "Error configuring filter " + use.getName() + ": " + ex2.getMessage());
                        throw new ServerException("logic.ErrorConfiguringJavaFilter", new Object[] { this.implementation, ex2.getMessage() });
                    }
                }
            }
        }
        return result;
    }
    
    public void forgetAllFilters() {
        this.connectionFilters.clear();
        this.requestFilters.clear();
        this.responseFilters.clear();
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
