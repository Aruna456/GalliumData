// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

import com.galliumdata.server.locale.I18nManager;

public class ProtocolException extends RuntimeException
{
    private final String msgName;
    private final Object[] args;
    
    public ProtocolException(final String msgName, final Object... args) {
        super(msgName);
        if (msgName == null || msgName.trim().length() == 0) {
            throw new RuntimeException("Cannot throw ProtocolException with empty message");
        }
        this.msgName = msgName;
        this.args = args;
    }
    
    @Override
    public String getMessage() {
        return I18nManager.getString(this.msgName, this.args);
    }
}
