// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.maven;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Set;

public class Library
{
    public String groupId;
    public String artifactId;
    public String version;
    public Set<Library> dependencies;
    public Map<String, String> properties;
    private Library parent;
    public URL jarUrl;
    public File pomFile;
    
    public boolean isTestLibrary() {
        return "org.mockito".equals(this.groupId);
    }
    
    public String getProperty(final String name) {
        final String value = this.properties.get(name);
        if (value != null) {
            return value;
        }
        if (this.parent != null) {
            return this.parent.getProperty(name);
        }
        return null;
    }
    
    public String translateProperty(final String s) {
        if (!s.startsWith("${")) {
            return s;
        }
        final String propName = s.substring(2, s.length() - 1);
        return this.getProperty(propName);
    }
    
    @Override
    public String toString() {
        return this.groupId + "/" + this.artifactId + "/" + this.version;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Library)) {
            return false;
        }
        final Library lib = (Library)obj;
        return this.groupId.equals(lib.groupId) && this.artifactId.equals(lib.artifactId) && (this.version == null || lib.version != null) && (this.version != null || lib.version == null);
    }
    
    @Override
    public int hashCode() {
        int hash = this.groupId.hashCode() + this.artifactId.hashCode();
        if (this.version != null) {
            hash += this.version.hashCode();
        }
        return hash;
    }
}
