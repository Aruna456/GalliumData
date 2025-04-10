// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import com.fasterxml.jackson.databind.JsonNode;

public class LogSetting extends RepositoryObject
{
    @Persisted(JSONName = "loggerName")
    protected String loggerName;
    @Persisted(JSONName = "level")
    protected String level;
    
    public LogSetting(final Repository repo, final JsonNode node) {
        super(repo);
        if (!node.has("loggerName")) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "logSettings", "loggerName (missing)", "repository" });
        }
        if (!node.has("level")) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "logSettings", "level (missing)", "repository" });
        }
        this.loggerName = node.get("loggerName").asText();
        this.level = node.get("level").asText();
        if (this.loggerName == null || this.loggerName.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "logSettings", "loggerName", "repository" });
        }
        if (this.level == null || this.level.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { "logSettings", "level", "repository" });
        }
    }
    
    public String getLoggerName() {
        return this.loggerName;
    }
    
    public String getLevel() {
        return this.level;
    }
    
    @Override
    public int hashCode() {
        return this.loggerName.hashCode() + this.level.hashCode();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof LogSetting)) {
            return false;
        }
        final LogSetting lib = (LogSetting)obj;
        return this.loggerName.equals(lib.loggerName) && this.level.equals(lib.level);
    }
    
    @Override
    public String toString() {
        return "LogSetting " + this.loggerName + ":" + this.level;
    }
}
