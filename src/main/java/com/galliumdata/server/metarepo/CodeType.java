// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.metarepo;

public enum CodeType
{
    JAVA("java"), 
    JAVASCRIPT("javascript");
    
    private String name;
    
    private CodeType(final String s) {
        this.name = s;
    }
    
    public String getName() {
        return this.name;
    }
    
    public static CodeType forString(final String s) {
        switch (s) {
            case "java": {
                return CodeType.JAVA;
            }
            case "javascript": {
                return CodeType.JAVASCRIPT;
            }
            default: {
                return null;
            }
        }
    }
}
