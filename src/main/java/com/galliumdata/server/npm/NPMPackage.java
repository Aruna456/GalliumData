// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.npm;

public class NPMPackage
{
    private String name;
    private String description;
    private String npmUrl;
    private String homePage;
    private String version;
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(final String description) {
        this.description = description;
    }
    
    public String getNpmUrl() {
        return this.npmUrl;
    }
    
    public void setNpmUrl(final String npmUrl) {
        this.npmUrl = npmUrl;
    }
    
    public String getHomePage() {
        return this.homePage;
    }
    
    public void setHomePage(final String homePage) {
        this.homePage = homePage;
    }
    
    public String getVersion() {
        return this.version;
    }
    
    public void setVersion(final String version) {
        this.version = version;
    }
}
