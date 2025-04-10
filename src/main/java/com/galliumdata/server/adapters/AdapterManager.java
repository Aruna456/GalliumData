// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.metarepo.MetaRepositoryManager;
import com.galliumdata.server.metarepo.Adapter;

public class AdapterManager
{
    private static final AdapterManager instance;
    
    public static AdapterManager getInstance() {
        return AdapterManager.instance;
    }
    
    public AdapterInterface instantiateAdapter(final String name) {
        final Adapter repoAdapter = MetaRepositoryManager.getMainRepository().getAdapters().get(name);
        if (repoAdapter == null) {
            throw new ServerException("repo.UnknownAdapter", new Object[] { name });
        }
        final String adapterImplStr = repoAdapter.getImplementation();
        if (adapterImplStr.startsWith("java:")) {
            final String clsName = adapterImplStr.substring("java:".length());
            AdapterInterface newAdapter;
            try {
                final Class<? extends AdapterInterface> cls = (Class<? extends AdapterInterface>)Class.forName(clsName);
                newAdapter = (AdapterInterface)cls.getConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            }
            catch (final ClassNotFoundException cnfe) {
                throw new ServerException("repo.UnknownAdapterClass", new Object[] { name, clsName });
            }
            catch (final Exception ex) {
                throw new ServerException("repo.CannotInstantiateAdapter", new Object[] { clsName, ex.getMessage() });
            }
            return newAdapter;
        }
        throw new RepositoryException("repo.InvalidAdapter", new Object[] { repoAdapter.getName(), adapterImplStr });
    }
    
    static {
        instance = new AdapterManager();
    }
}
