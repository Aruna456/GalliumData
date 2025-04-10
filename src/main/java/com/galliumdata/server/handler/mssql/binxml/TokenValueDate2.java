// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

public class TokenValueDate2 extends XMLTokenValue
{
    private int daysTicks;
    public static final LocalDate DATETIME0001;
    public static final DateTimeFormatter formatter;
    
    public TokenValueDate2(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        this.daysTicks = this.doc.readInt24() - 3;
    }
    
    @Override
    public String stringValue() {
        final LocalDate ld = TokenValueDate2.DATETIME0001.plusDays(this.daysTicks);
        return TokenValueDate2.formatter.format(ld);
    }
    
    @Override
    public byte getType() {
        return 126;
    }
    
    static {
        DATETIME0001 = LocalDate.of(1, 1, 1);
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }
}
