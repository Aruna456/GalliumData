// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.debug;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.rest.ServerSentEventHandler;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.logic.ScriptExecutor;
import org.apache.logging.log4j.Logger;

public class DebugManager
{
    private static boolean debugActive;
    private static final Logger log;
    
    public static boolean debugIsActive() {
        return DebugManager.debugActive;
    }
    
    public static void startDebug() {
        if (DebugManager.debugActive) {
            return;
        }
        DebugManager.debugActive = true;
        ScriptExecutor.enterDebugSession();
        DebugManager.log.debug(Markers.SYSTEM, "Entering debug mode");
    }
    
    public static void stopDebug() {
        if (!DebugManager.debugActive) {
            return;
        }
        DebugManager.debugActive = false;
        ScriptExecutor.exitDebugSession();
        ServerSentEventHandler.stopEventQueue();
        DebuggerCallback.unblock();
        DebugManager.log.debug(Markers.SYSTEM, "Exiting debug mode");
    }
    
    public static void step() {
        DebugManager.log.trace(Markers.SYSTEM, "DebugManager: step");
        ScriptExecutor.step();
    }
    
    public static void go() {
        DebugManager.log.trace(Markers.SYSTEM, "DebugManager: go");
        ScriptExecutor.go();
    }
    
    public static void stepOut() {
        DebugManager.log.trace(Markers.SYSTEM, "DebugManager: stepOut");
        ScriptExecutor.stepOut();
    }
    
    public static void stepIn() {
        DebugManager.log.trace(Markers.SYSTEM, "DebugManager: stepIn");
        ScriptExecutor.stepIn();
    }
    
    static {
        DebugManager.debugActive = false;
        log = LogManager.getLogger("galliumdata.rest");
    }
}
