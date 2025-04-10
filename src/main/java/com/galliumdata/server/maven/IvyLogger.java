// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.maven;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.ivy.util.MessageLogger;

public class IvyLogger implements MessageLogger
{
    private boolean showingProgress;
    private static final Logger log;
    
    public void log(final String s, final int i) {
        IvyLogger.log.log(getLevel(i), s);
    }
    
    public void rawlog(final String s, final int i) {
        IvyLogger.log.log(getLevel(i), s);
    }
    
    public void debug(final String s) {
        IvyLogger.log.trace(s);
    }
    
    public void verbose(final String s) {
        IvyLogger.log.debug(s);
    }
    
    public void deprecated(final String s) {
        IvyLogger.log.debug(s);
    }
    
    public void info(final String s) {
        if (s.startsWith("downloading")) {
            IvyLogger.log.info(s);
        }
        else {
            IvyLogger.log.debug(s);
        }
    }
    
    public void rawinfo(final String s) {
        IvyLogger.log.info(s);
    }
    
    public void warn(final String s) {
        IvyLogger.log.warn(s);
    }
    
    public void error(final String s) {
        IvyLogger.log.error(s);
    }
    
    public List<String> getProblems() {
        return null;
    }
    
    public List<String> getWarns() {
        return null;
    }
    
    public List<String> getErrors() {
        return null;
    }
    
    public void clearProblems() {
    }
    
    public void sumupProblems() {
    }
    
    public void progress() {
    }
    
    public void endProgress() {
        IvyLogger.log.debug("...Done");
    }
    
    public void endProgress(final String s) {
        if (" (0kB)".equals(s)) {
            return;
        }
        IvyLogger.log.info("...Done: " + s);
    }
    
    public boolean isShowProgress() {
        return this.showingProgress;
    }
    
    public void setShowProgress(final boolean b) {
        this.showingProgress = b;
    }
    
    private static Level getLevel(final int l) {
        switch (l) {
            case 4: {
                return Level.TRACE;
            }
            case 0: {
                return Level.ERROR;
            }
            case 2: {
                return Level.INFO;
            }
            case 1: {
                return Level.WARN;
            }
            default: {
                return Level.DEBUG;
            }
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.maven");
    }
}
