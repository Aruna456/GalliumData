// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import com.galliumdata.server.repository.FilterUse;

public interface Filter
{
    public static final String REGEX_PREFIX = "regex:";
    public static final String REGEX_PREFIX_CASE = "REGEX:";
    public static final String RANGE_PREFIX = "range:";
    
    void configure(final FilterUse p0);
    
    String getName();
}
