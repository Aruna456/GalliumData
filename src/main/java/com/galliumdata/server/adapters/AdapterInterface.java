// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.Project;

public interface AdapterInterface extends Runnable
{
    void initialize();
    
    boolean configure(final Project p0, final Connection p1, final AdapterCallback p2);
    
    void stopProcessing();
    
    void switchProject(final Project p0, final Connection p1);
    
    void shutdown();
    
    AdapterStatus getStatus();
    
    String getName();
    
    String testConnection(final Project p0, final Connection p1);
}
