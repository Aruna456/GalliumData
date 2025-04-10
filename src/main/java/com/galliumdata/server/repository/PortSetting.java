// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

public class PortSetting extends RepositoryObject
{
    @Persisted(JSONName = "PortNumber")
    private int portNumber;
    @Persisted(JSONName = "Active")
    private boolean active;
    
    public PortSetting(final Repository repo) {
        super(repo);
    }
}
