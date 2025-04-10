// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import java.time.ZonedDateTime;

public class AdapterStatus
{
    public ZonedDateTime startTime;
    public long numRequests;
    public long numRequestBytes;
    public long numResponses;
    public long numResponseBytes;
    public long numErrors;
    
    public synchronized void incrementNumRequests(final long num) {
        this.numRequests += num;
    }
    
    public synchronized void incrementNumRequestBytes(final long num) {
        this.numRequestBytes += num;
    }
    
    public synchronized void incrementNumResponses(final long num) {
        this.numResponses += num;
    }
    
    public synchronized void incrementNumResponseBytes(final long num) {
        this.numResponseBytes += num;
    }
    
    public synchronized void incrementNumErrors(final long num) {
        this.numErrors += num;
    }
}
