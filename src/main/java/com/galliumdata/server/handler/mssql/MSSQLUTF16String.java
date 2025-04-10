// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.ProtocolException;
import com.galliumdata.server.util.BinaryDump;
import java.nio.charset.StandardCharsets;

public class MSSQLUTF16String
{
    private String value;
    
    public MSSQLUTF16String() {
    }
    
    public MSSQLUTF16String(final String s) {
        this.value = s;
    }
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        try {
            this.value = new String(bytes, offset, numBytes, StandardCharsets.UTF_16);
        }
        catch (final Exception ex) {
            throw new ProtocolException("db.mssql.protocol.BadString", new Object[] { numBytes, BinaryDump.getBinaryDump(bytes, offset, Math.min(numBytes, 16)) });
        }
        return this.value.getBytes(StandardCharsets.UTF_16).length;
    }
    
    public int writeToBytes(final byte[] buffer, final int offset) {
        final byte[] strBytes = this.value.getBytes(StandardCharsets.UTF_16);
        System.arraycopy(strBytes, 0, buffer, offset, strBytes.length);
        return strBytes.length;
    }
    
    public String getString() {
        return this.value;
    }
}
