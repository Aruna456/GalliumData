// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.adapters;

import java.util.List;

public class AdapterInfo
{
    public String name;
    public String version;
    public String adapterFor;
    public String adapterForVersion;
    public List<ParameterDefinition> parameterDefinitions;
    
    public static class ParameterDefinition
    {
        public String name;
        public ParameterType type;
        public String description;
        public boolean required;
        public String defaultValue;
        public String[] allowedValues;
        
        public enum ParameterType
        {
            STRING, 
            INTEGER, 
            BOOLEAN, 
            MAP;
        }
    }
}
