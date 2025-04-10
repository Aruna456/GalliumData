// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import java.util.ArrayList;
import java.util.List;

public class AdapterCallbackResponse
{
    public boolean reject;
    public Object response;
    public boolean skip;
    public String errorMessage;
    public byte[] errorResponse;
    public long errorCode;
    public List<String> errorParameters;
    public int sqlStatus;
    public boolean closeConnection;
    public String connectionName;
    public String logicName;
    public boolean doNotCall;
    
    public AdapterCallbackResponse() {
        this.reject = false;
        this.errorCode = 0L;
        this.errorParameters = new ArrayList<String>();
        this.sqlStatus = 0;
        this.closeConnection = false;
    }
}
