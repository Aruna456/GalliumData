// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import com.fasterxml.jackson.databind.JsonNode;

public class Library extends RepositoryObject
{
    @Persisted(JSONName = "type")
    protected String type;
    @Persisted(JSONName = "orgId")
    protected String orgId;
    @Persisted(JSONName = "artifactId")
    protected String artifactId;
    @Persisted(JSONName = "version")
    protected String version;
    
    public Library(final Repository repo, final JsonNode node) {
        super(repo);
        if (!node.has("type")) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "libraries", "type (missing)", "repository" });
        }
        if (!node.has("orgId")) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "libraries", "orgId (missing)", "repository" });
        }
        if (!node.has("artifactId")) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "libraries", "artifactId (missing)", "repository" });
        }
        if (!node.has("version")) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "libraries", "version (missing)", "repository" });
        }
        this.type = node.get("type").asText();
        this.orgId = node.get("orgId").asText();
        this.artifactId = node.get("artifactId").asText();
        this.version = node.get("version").asText();
        if (this.type == null || this.type.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "libraries", "type", "repository" });
        }
        if (this.orgId == null || this.orgId.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "libraries", "orgId", "repository" });
        }
        if (this.artifactId == null || this.artifactId.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "libraries", "artifactId", "repository" });
        }
        if (this.version == null || this.version.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "libraries", "version", "repository" });
        }
    }
    
    @Override
    public int hashCode() {
        return this.orgId.hashCode() + this.artifactId.hashCode() + this.version.hashCode();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Library)) {
            return false;
        }
        final Library lib = (Library)obj;
        return this.orgId.equals(lib.orgId) && this.artifactId.equals(lib.artifactId) && this.version.equals(lib.version);
    }
    
    @Override
    public String toString() {
        return "Library " + this.orgId + "/" + this.artifactId + "/" + this.version;
    }
}
