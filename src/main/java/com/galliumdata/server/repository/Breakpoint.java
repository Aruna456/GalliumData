// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

public class Breakpoint
{
    public String filename;
    public int linenum;
    
    public Breakpoint(final String file, final int line) {
        this.filename = file;
        this.linenum = line;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Breakpoint)) {
            return false;
        }
        final Breakpoint bp = (Breakpoint)o;
        return this.filename.equals(bp.filename) && this.linenum == bp.linenum;
    }
    
    @Override
    public int hashCode() {
        return this.filename.hashCode() + this.linenum;
    }
}
