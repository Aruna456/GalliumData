// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.KeyStore;
import java.util.Map;

public class Connection extends RepositoryObject
{
    @Persisted(JSONName = "adapterType")
    protected String adapterType;
    @Persisted(JSONName = "adapterVersion")
    protected String adapterVersion;
    @Persisted(JSONName = "parameters")
    protected Map<String, Object> parameters;
    @Persisted(JSONName = "keystorePassword")
    protected String keystorePassword;
    protected KeyStore keystore;
    
    public Connection(final Repository repo) {
        super(repo);
    }
    
    public Project getProject() {
        return (Project)this.parentObject;
    }
    
    public String getAdapterType() {
        return this.adapterType;
    }
    
    public String getAdapterVersion() {
        return this.adapterVersion;
    }
    
    public Map<String, Object> getParameters() {
        return this.parameters;
    }
    
    public Object getParameterValue(final String name) {
        if (null == this.parameters) {
            return null;
        }
        return this.parameters.get(name);
    }
    
    public KeyStore getKeystore() {
        if (this.keystore != null) {
            return this.keystore;
        }
        final Path ksPath = this.path.resolveSibling("keystore.jks");
        if (!Files.exists(ksPath, new LinkOption[0])) {
            return null;
        }
        if (!Files.isReadable(ksPath)) {
            throw new RepositoryException("repo.FileNotReadable", new Object[] { ksPath });
        }
        try {
            this.keystore = KeyStore.getInstance("JKS");
            char[] pwChars = null;
            if (this.keystorePassword != null) {
                pwChars = this.keystorePassword.toCharArray();
            }
            this.keystore.load(Files.newInputStream(ksPath, new OpenOption[0]), pwChars);
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.BadKeystore", new Object[] { ksPath, ex.getMessage() });
        }
        return this.keystore;
    }
    
    public String getKeystorePassword() {
        return this.keystorePassword;
    }
}
