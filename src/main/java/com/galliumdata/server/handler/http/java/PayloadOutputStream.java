// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java;

import java.io.IOException;
import java.util.function.Consumer;
import java.io.ByteArrayOutputStream;

public class PayloadOutputStream extends ByteArrayOutputStream
{
    private final Consumer<byte[]> callback;
    
    public PayloadOutputStream(final Consumer<byte[]> cb) {
        this.callback = cb;
    }
    
    @Override
    public void flush() throws IOException {
        super.flush();
        this.callback.accept(this.toByteArray());
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        this.callback.accept(this.toByteArray());
    }
}
