// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.javafilters;

import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.logic.FilterResult;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ResponseFilter;

public class TestJavaResponseFilter implements ResponseFilter
{
    @Override
    public void configure(final FilterUse def) {
        System.out.println("TestJavaResponseFilter.configure has been called");
    }
    
    @Override
    public String getName() {
        return "TestJavaResponseFilter for MSSQL";
    }
    
    @Override
    public FilterResult filterResponse(final Variables context) {
        System.out.println("TestJavaResponseFilter.filterResponse has been called");
        final FilterResult result = new FilterResult();
        final TokenRow row = (TokenRow)context.get("packet");
        final ColumnMetadata meta = row.getColumnMetadata().get(0);
        final MSSQLDataType val = row.getColumnValue(meta);
        if (val != null && val.toString().contains("REJECT")) {
            System.out.println("TestJavaResponseFilter.filterResponse is rejecting the response");
            result.setSuccess(false);
            result.setErrorMessage("Response rejected because REJECT!");
            result.setErrorCode(999);
            result.setCloseConnection(true);
        }
        return result;
    }
    
    @Override
    public String[] getPacketTypes() {
        return new String[] { "Row", "NBCRow" };
    }
}
