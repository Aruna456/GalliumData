// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import com.galliumdata.server.adapters.Variables;
import java.net.Socket;

public interface ConnectionFilter extends Filter
{
    FilterResult acceptConnection(final Socket p0, final Variables p1);
}
