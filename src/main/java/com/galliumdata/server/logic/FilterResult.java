// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class FilterResult implements ProxyObject
{
    private boolean success;
    private boolean skip;
    private String filterName;
    private String connectionName;
    private Object response;
    private String errorMessage;
    private int errorCode;
    private List<String> errorParameters;
    private int sqlStatus;
    private byte[] errorResponse;
    private boolean closeConnection;
    private boolean doNotCall;
    private final String[] fields;
    
    public FilterResult() {
        this.success = true;
        this.skip = false;
        this.errorParameters = new ArrayList<String>();
        this.fields = new String[] { "success", "skip", "filterName", "connectionName", "response", "errorMessage", "errorCode", "addErrorParameter", "sqlStatus", "closeConnection", "doNotCall" };
    }
    
    public boolean isSuccess() {
        return this.success;
    }
    
    public void setSuccess(final boolean success) {
        this.success = success;
    }
    
    public boolean isSkip() {
        return this.skip;
    }
    
    public void setSkip(final boolean skip) {
        this.skip = skip;
    }
    
    public String getFilterName() {
        return this.filterName;
    }
    
    public void setFilterName(final String filterName) {
        this.filterName = filterName;
    }
    
    public String getConnectionName() {
        return this.connectionName;
    }
    
    public void setConnectionName(final String connectionName) {
        this.connectionName = connectionName;
    }
    
    public Object getResponse() {
        return this.response;
    }
    
    public void setResponse(final Object obj) {
        this.response = obj;
    }
    
    public String getErrorMessage() {
        return this.errorMessage;
    }
    
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getErrorCode() {
        return this.errorCode;
    }
    
    public void setErrorCode(final int code) {
        this.errorCode = code;
    }
    
    public List<String> getErrorParameters() {
        return this.errorParameters;
    }
    
    public int getSqlStatus() {
        return this.sqlStatus;
    }
    
    public void setSqlStatus(final int n) {
        this.sqlStatus = n;
    }
    
    public byte[] getErrorResponse() {
        return this.errorResponse;
    }
    
    public void setErrorResponse(final byte[] bytes) {
        this.errorResponse = bytes;
    }
    
    public boolean isCloseConnection() {
        return this.closeConnection;
    }
    
    public void setCloseConnection(final boolean closeConnection) {
        this.closeConnection = closeConnection;
    }
    
    public boolean isDoNotCall() {
        return this.doNotCall;
    }
    
    public void setDoNotCall(final boolean doNotCall) {
        this.doNotCall = doNotCall;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "success": {
                return this.success;
            }
            case "skip": {
                return this.skip;
            }
            case "filterName": {
                return this.filterName;
            }
            case "connectionName": {
                return this.connectionName;
            }
            case "response": {
                return this.response;
            }
            case "errorMessage": {
                return this.errorMessage;
            }
            case "errorCode": {
                return this.errorCode;
            }
            case "addErrorParameter": {
                return(Function<Value[],Object>) p -> {
                    this.errorParameters.add(p[0].asString());
                    return null;
                };
            }
            case "sqlStatus": {
                return this.sqlStatus;
            }
            case "closeConnection": {
                return this.closeConnection;
            }
            case "doNotCall": {
                return this.doNotCall;
            }
            default: {
                throw new LogicException("No such attribute in FilterResult: " + key);
            }
        }
    }
    
    public Object getMemberKeys() {
        return this.fields;
    }
    
    public boolean hasMember(final String key) {
        return Arrays.asList(this.fields).contains(key);
    }
    
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "success": {
                this.success = (value != null && !value.isNull() && value.asBoolean());
                break;
            }
            case "skip": {
                this.skip = (value != null && !value.isNull() && value.asBoolean());
                break;
            }
            case "filterName": {
                this.filterName = ((value == null || value.isNull()) ? null : value.asString());
                break;
            }
            case "connectionName": {
                this.connectionName = ((value == null || value.isNull()) ? null : value.asString());
                break;
            }
            case "response": {
                if (value.isProxyObject()) {
                    this.response = value.asProxyObject();
                    break;
                }
                if (value.isHostObject()) {
                    this.response = value.asHostObject();
                    break;
                }
                if (value.isString()) {
                    this.response = value.asString();
                    break;
                }
                throw new ServerException("logic.CannotSetResponse", new Object[] { value });
            }
            case "errorMessage": {
                this.errorMessage = ((value == null || value.isNull()) ? null : value.asString());
                break;
            }
            case "errorCode": {
                this.errorCode = ((value == null || value.isNull()) ? 0 : value.asInt());
                break;
            }
            case "sqlStatus": {
                this.sqlStatus = ((value == null || value.isNull()) ? 0 : value.asInt());
                break;
            }
            case "closeConnection": {
                this.closeConnection = (value != null && !value.isNull() && value.asBoolean());
                break;
            }
            case "doNotCall": {
                this.doNotCall = (value != null && !value.isNull() && value.asBoolean());
                break;
            }
            default: {
                throw new LogicException("No such attribute in FilterResult: " + key);
            }
        }
    }
    
    public boolean removeMember(final String key) {
        switch (key) {
            case "success": {
                this.success = false;
                return true;
            }
            case "skip": {
                this.skip = false;
                return true;
            }
            case "filterName": {
                this.filterName = null;
                return true;
            }
            case "response": {
                this.response = null;
                return true;
            }
            case "errorMessage": {
                this.errorMessage = null;
                return true;
            }
            case "errorCode": {
                this.errorCode = 0;
                return true;
            }
            case "sqlStatus": {
                this.sqlStatus = 0;
                return true;
            }
            case "closeConnection": {
                this.closeConnection = false;
                return true;
            }
            default: {
                return false;
            }
        }
    }
}
