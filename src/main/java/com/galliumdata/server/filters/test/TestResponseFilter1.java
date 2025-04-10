// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.test;

import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.handler.GenericPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.ResponseFilter;

public class TestResponseFilter1 implements ResponseFilter
{
    @Override
    public FilterResult filterResponse(final Variables variables) {
        final GenericPacket pkt = (GenericPacket)variables.get("packet");
        System.out.println("TestResponseFilter1 has received a request packet: " + String.valueOf(pkt));
        return new FilterResult();
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[0];
    }
    
    @Override
    public void configure(final FilterUse filterUse) {
    }
    
    @Override
    public String getName() {
        return "TestResponseFilter1";
    }
}
