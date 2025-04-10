// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.metarepo;

import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import com.galliumdata.server.repository.RepositoryException;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.Map;

public class Adapter extends MetaRepositoryObject
{
    protected String version;
    protected String implementation;
    protected Map<String, ParameterType> parameters;
    
    public Adapter(final Path path, final String name) {
        super(path);
        this.name = name;
    }
    
    public String getVersion() {
        return this.version;
    }
    
    public String getImplementation() {
        return this.implementation;
    }
    
    @Override
    protected String getJsonFileName() {
        return "adapter.json";
    }
    
    @Override
    protected void processJson(final JsonNode node) {
        this.version = node.get("version").asText();
        this.implementation = node.get("implementation").asText();
        final JsonNode paramsNode = node.get("parameters");
        if (paramsNode == null) {
            throw new RepositoryException("repo.MissingPropInFile", new Object[] { "parameters", this.path });
        }
        this.parameters = new HashMap<String, ParameterType>();
        paramsNode.fields().forEachRemaining(e -> {
            final ParameterType param = new ParameterType();
            param.name = e.getKey();
            this.parameters.put(e.getKey(), param);
            final JsonNode paramJson = e.getValue();
            param.type = ParameterType.ParameterDataType.valueOf(paramJson.get("type").asText());
            param.description = paramJson.get("description").asText();
            param.required = paramJson.get("required").asBoolean(false);
            param.defaultValue = paramJson.get("defaultValue").asText((String)null);
            final JsonNode allowedJson = paramJson.get("allowedValues");
            if (allowedJson != null) {
                final Vector<String> avs = new Vector<String>();
                for (int i = 0; i < allowedJson.size(); ++i) {
                    avs.add(allowedJson.get(i).asText());
                }
                param.allowedValues = avs.toArray(new String[0]);
            }
        });
    }
}
