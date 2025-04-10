// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.security;

import java.util.Map;

public interface KeyLoader
{
    public static final String KEY_LOADER_RETURN_KEY_MANAGERS = "keyManagers";
    public static final String KEY_LOADER_RETURN_TRUST_MANAGERS = "trustManagers";
    
    Map<String, Object> loadKeys(final Map<String, Object> p0);
}
