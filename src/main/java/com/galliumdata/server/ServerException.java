// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server;

import com.galliumdata.server.locale.I18nManager;

public class ServerException extends RuntimeException
{
    protected String msgName;
    protected Object[] args;
    
    public ServerException(final String msgName, final Object... args) {
        super(msgName);
        this.msgName = msgName;
        this.args = args;
    }
    
    @Override
    public String getMessage() {
        return I18nManager.getString(this.msgName, this.args);
    }
    
    public Object[] getArgs() {
        return this.args;
    }
    
    public static void throwException(final String errName) {
        final String msg = I18nManager.getString(errName, new Object[0]);
        throw new ServerException(msg, new Object[0]);
    }
}
