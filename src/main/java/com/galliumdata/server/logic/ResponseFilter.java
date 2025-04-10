// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import com.galliumdata.server.adapters.Variables;

public interface ResponseFilter extends Filter
{
    FilterResult filterResponse(final Variables p0);
    
    String[] getPacketTypes();
}
