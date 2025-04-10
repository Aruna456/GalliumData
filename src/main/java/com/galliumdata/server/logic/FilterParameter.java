// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

public class FilterParameter
{
    public String name;
    public ParameterType type;
    public String description;
    public boolean required;
    public String defaultValue;
    public String[] allowedValues;
    
    @Override
    public String toString() {
        return "FilterParameter " + this.name;
    }
    
    public enum ParameterType
    {
        STRING, 
        INTEGER, 
        BOOLEAN;
    }
}
