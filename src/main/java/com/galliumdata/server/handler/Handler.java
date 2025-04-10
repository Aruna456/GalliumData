// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

public interface Handler
{
    void init();
    
    void openConnection();
    
    void handleRequest();
    
    void closeConnection();
}
