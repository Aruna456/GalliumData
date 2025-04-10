// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

public class NullClass extends RepositoryObject
{
    public NullClass(final Repository repo) {
        super(repo);
        throw new RuntimeException("This should never happen");
    }
}
