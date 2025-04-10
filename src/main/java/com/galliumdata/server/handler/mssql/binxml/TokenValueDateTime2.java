// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.binxml;

import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class TokenValueDateTime2 extends XMLTokenValue
{
    private int daysTicks;
    private int timeTicks;
    public static final LocalDate DATE1;
    public static final LocalDateTime DATETIME1;
    public static final LocalDateTime DATETIME1900;
    public static final DateTimeFormatter formatter;
    
    public TokenValueDateTime2(final BinXMLDocument doc) {
        super(doc);
    }
    
    @Override
    public void read() {
        this.daysTicks = this.doc.readInt32();
        this.timeTicks = this.doc.readInt32();
    }
    
    @Override
    public String stringValue() {
        LocalDateTime ldt = TokenValueDateTime2.DATETIME1.plusDays(this.daysTicks);
        ldt = ldt.plusNanos(this.timeTicks * 3333333L);
        return TokenValueDateTime2.formatter.format(ldt);
    }
    
    @Override
    public byte getType() {
        return 127;
    }
    
    static {
        DATE1 = LocalDate.of(1, 1, 1);
        DATETIME1 = LocalDateTime.of(1, 1, 1, 0, 0, 0);
        DATETIME1900 = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
}
