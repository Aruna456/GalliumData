// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.javafilters;

import com.galliumdata.server.handler.mssql.SQLBatchPacket;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.RequestFilter;

public class TestJavaRequestFilter implements RequestFilter
{
    @Override
    public void configure(final FilterUse def) {
        System.out.println("TestJavaRequestFilter.configure has been called");
    }
    
    @Override
    public String getName() {
        return "TestJavaRequestFilter for MSSQL";
    }
    
    @Override
    public FilterResult filterRequest(final Variables context) {
        System.out.println("TestJavaRequestFilter.filterRequest has been called");
        final FilterResult result = new FilterResult();
        final SQLBatchPacket pkt = (SQLBatchPacket)context.get("packet");
        if (pkt.getSql().contains("REJECT")) {
            result.setSuccess(false);
            result.setErrorMessage("Query rejected because REJECT!");
            result.setErrorCode(999);
            result.setCloseConnection(true);
        }
        return result;
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "SQLBatch" };
    }
}
