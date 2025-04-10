// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.javadoc;

import jdk.javadoc.doclet.DocletEnvironment;
import javax.lang.model.SourceVersion;
import java.util.Set;
import jdk.javadoc.doclet.Reporter;
import java.util.Locale;
import jdk.javadoc.doclet.Doclet;

public class PubDoclet implements Doclet
{
    @Override
    public void init(final Locale locale, final Reporter reporter) {
        System.out.println("init called");
    }
    
    @Override
    public String getName() {
        System.out.println("getName called");
        return "MyName";
    }
    
    @Override
    public Set<? extends Option> getSupportedOptions() {
        System.out.println("getSupportedOptions called");
        return null;
    }
    
    @Override
    public SourceVersion getSupportedSourceVersion() {
        System.out.println("getSupportedSourceVersion called");
        return null;
    }
    
    @Override
    public boolean run(final DocletEnvironment environment) {
        System.out.println("run called");
        return false;
    }
}
