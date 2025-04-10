// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.loginfeatures;

import com.galliumdata.server.handler.mssql.RawPacketWriter;

public abstract class Login7Feature
{
    public abstract String getFeatureType();
    
    public abstract int readFromBytes(final byte[] p0, final int p1);
    
    public abstract int getSerializedSize();
    
    public abstract void write(final RawPacketWriter p0);
}
