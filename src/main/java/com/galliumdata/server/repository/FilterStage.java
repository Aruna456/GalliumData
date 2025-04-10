// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

public enum FilterStage
{
    CONNECTION("connection"), 
    REQUEST("request"), 
    RESPONSE("response"), 
    DUPLEX("duplex");
    
    private String name;
    
    private FilterStage(final String s) {
        this.name = s;
    }
}
