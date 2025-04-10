// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.metarepo;

import java.io.BufferedReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.galliumdata.server.repository.RepositoryException;
import java.io.Reader;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;

public abstract class MetaRepositoryObject
{
    protected boolean isRead;
    protected String name;
    protected Path path;
    
    public MetaRepositoryObject(final Path path) {
        this.isRead = false;
        this.path = path;
    }
    
    public void readFromJson() {
        if (this.isRead) {
            return;
        }
        this.isRead = true;
        final Path jsonPath = this.path.resolve(this.getJsonFileName());
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode node = null;
        try (final BufferedReader reader = Files.newBufferedReader(jsonPath)) {
            node = mapper.readTree((Reader)reader);
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.BadFile", new Object[] { this.path.toString(), ex.getMessage() });
        }
        this.processJson(node);
    }
    
    public String getName() {
        return this.name;
    }
    
    protected abstract String getJsonFileName();
    
    protected abstract void processJson(final JsonNode p0);
}
