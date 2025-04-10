// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.metarepo;

public class ParameterType
{
    public String name;
    public ParameterDataType type;
    public String description;
    public boolean required;
    public String defaultValue;
    public String[] allowedValues;
    
    public enum ParameterDataType
    {
        STRING, 
        INTEGER, 
        BOOLEAN, 
        MAP, 
        ARRAY, 
        TEXT;
    }
}
