// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import org.apache.logging.log4j.LogManager;
import java.io.File;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.repository.RepositoryManager;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.Debugger;
import java.util.Iterator;
import org.graalvm.polyglot.SourceSection;
import com.galliumdata.server.rest.ServerSentEventHandler;
import com.galliumdata.server.rest.ServerEvent;
import com.galliumdata.server.handler.ProtocolDataObject;
import org.graalvm.polyglot.PolyglotException;
import com.galliumdata.server.debug.DebugManager;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.adapters.Variables;
import org.graalvm.polyglot.Source;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.repository.Breakpoint;
import java.util.Map;
import com.galliumdata.server.debug.DebuggerCallback;
import com.oracle.truffle.api.debug.DebuggerSession;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

public class ScriptExecutor
{
    public static Engine globalEngine;
    private static ThreadLocal<Context> jsContexts;
    private static ThreadLocal<Value> jsBindings;
    private static DebuggerSession debuggerSession;
    private static DebuggerCallback debuggerCallback;
    private static Map<Breakpoint, com.oracle.truffle.api.debug.Breakpoint> breakpoints;
    private static String nodeModulesDir;
    private static final Logger log;
    private static final Logger userLog;
    
    public static void executeFilterScript(final Source src, final FilterResult result, final Variables context) {
        setContexts();
        final Context jsContext = ScriptExecutor.jsContexts.get();
        final Value val = ScriptExecutor.jsBindings.get();
        if (val == null) {
            ScriptExecutor.userLog.warn(Markers.SYSTEM, "Unable to find JavaScript binding for " + src.getName() + ", this filter will not get executed.");
            return;
        }
        val.putMember("context", (Object)context);
        val.putMember("log", (Object)ScriptExecutor.userLog);
        try {
            jsContext.eval(src);
        }
        catch (final PolyglotException pex) {
            if (DebugManager.debugIsActive()) {
                SourceSection section = pex.getSourceLocation();
                if (section == null) {
                    Iterator<PolyglotException.StackFrame> stackIter;
                    PolyglotException.StackFrame stackFrame;
                    for (stackIter = pex.getPolyglotStackTrace().iterator(), stackFrame = stackIter.next(), section = stackFrame.getSourceLocation(); section == null && stackIter.hasNext(); section = stackFrame.getSourceLocation()) {
                        stackFrame = stackIter.next();
                    }
                }
                final ProtocolDataObject errorNode = new ProtocolDataObject();
                if (section == null) {
                    ScriptExecutor.log.debug(Markers.SYSTEM, "Unable to determine source location for: " + String.valueOf(pex));
                    errorNode.putString("projectName", "<unknown>");
                    errorNode.putString("filterType", "<unknown>");
                    errorNode.putString("filterName", "<unknown>");
                    errorNode.putNumber("startLine", 0);
                    errorNode.putNumber("endLine", 0);
                    errorNode.putNumber("startCol", 0);
                    errorNode.putNumber("endCol", 0);
                }
                else {
                    final String[] paths = section.getSource().getName().split("/");
                    errorNode.putString("projectName", paths[paths.length - 4]);
                    errorNode.putString("filterType", paths[paths.length - 3]);
                    errorNode.putString("filterName", paths[paths.length - 2]);
                    errorNode.putNumber("startLine", section.getStartLine());
                    errorNode.putNumber("endLine", section.getEndLine());
                    errorNode.putNumber("startCol", section.getStartColumn());
                    errorNode.putNumber("endCol", section.getEndColumn());
                }
                errorNode.putString("message", pex.getMessage());
                errorNode.putNumber("ts", System.currentTimeMillis());
                final ServerEvent serverEvent = new ServerEvent();
                serverEvent.eventType = "error";
                serverEvent.message = errorNode.toJSON();
                ServerSentEventHandler.queueServerEvent(serverEvent);
            }
            jsContext.close(true);
            createJavaScriptContext();
            if (ScriptExecutor.userLog.isDebugEnabled()) {
                ScriptExecutor.userLog.debug(Markers.USER_LOGIC, "Exception while running filter: " + src.getName() + ": " + pex.getMessage());
                pex.printStackTrace();
                if (pex.getCause() != null) {
                    pex.getCause().printStackTrace();
                }
            }
        }
        catch (final Exception ex) {
            jsContext.close(true);
            createJavaScriptContext();
            if (ScriptExecutor.userLog.isDebugEnabled()) {
                ScriptExecutor.userLog.debug(Markers.USER_LOGIC, "Exception while running filter: " + src.getName() + ": " + ex.getMessage());
            }
        }
    }
    
    public static void forgetAllScripts() {
        ScriptExecutor.jsContexts = new ThreadLocal<Context>();
        ScriptExecutor.jsBindings = new ThreadLocal<Value>();
        exitDebugSession();
    }
    
    public static void go() {
        ScriptExecutor.debuggerCallback.setActionOnResume("go");
        ScriptExecutor.debuggerCallback.resumeExecution();
    }
    
    public static void step() {
        ScriptExecutor.debuggerCallback.setActionOnResume("step");
        ScriptExecutor.debuggerCallback.resumeExecution();
    }
    
    public static void stepOut() {
        ScriptExecutor.debuggerCallback.setActionOnResume("stepOut");
        ScriptExecutor.debuggerCallback.resumeExecution();
    }
    
    public static void stepIn() {
        ScriptExecutor.debuggerCallback.setActionOnResume("stepIn");
        ScriptExecutor.debuggerCallback.resumeExecution();
    }
    
    private static void setContexts() {
        if (ScriptExecutor.globalEngine == null) {
            ScriptExecutor.globalEngine = Engine.newBuilder().build();
        }
        Context jsContext = ScriptExecutor.jsContexts.get();
        if (jsContext == null) {
            createJavaScriptContext();
            jsContext = ScriptExecutor.jsContexts.get();
        }
        Value val = ScriptExecutor.jsBindings.get();
        if (val == null) {
            val = jsContext.getPolyglotBindings();
            val.putMember("polyglot.js.allowAllAccess", (Object)true);
            ScriptExecutor.jsBindings.set(val);
        }
    }
    
    public static void enterDebugSession() {
        if (ScriptExecutor.debuggerSession != null) {
            ScriptExecutor.log.debug(Markers.SYSTEM, "We are already debugging, only one at a time");
            return;
        }
        setContexts();
        if (ScriptExecutor.debuggerSession == null) {
            final Debugger debugger = Debugger.find(ScriptExecutor.globalEngine);
            ScriptExecutor.debuggerCallback = new DebuggerCallback();
            ScriptExecutor.debuggerSession = debugger.startSession((SuspendedCallback)ScriptExecutor.debuggerCallback);
            for (final Project project : RepositoryManager.getMainRepository().getProjects().values()) {
                for (final Breakpoint bp : project.getBreakpoints()) {
                    addBreakpoint(project, bp);
                }
            }
        }
    }
    
    public static void exitDebugSession() {
        if (ScriptExecutor.debuggerSession != null) {
            ScriptExecutor.debuggerSession.close();
            ScriptExecutor.debuggerSession = null;
        }
        ScriptExecutor.debuggerCallback = null;
        ScriptExecutor.breakpoints = new HashMap<Breakpoint, com.oracle.truffle.api.debug.Breakpoint>();
        ScriptExecutor.jsBindings = new ThreadLocal<Value>();
        ScriptExecutor.jsContexts = new ThreadLocal<Context>();
    }
    
    public static void addBreakpoint(final Project project, final Breakpoint bp) {
        URI srcUri;
        try {
            srcUri = new URI(URLEncoder.encode(project.getName() + "/" + bp.filename, StandardCharsets.UTF_8));
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            return;
        }
        final com.oracle.truffle.api.debug.Breakpoint breakp = com.oracle.truffle.api.debug.Breakpoint.newBuilder(srcUri).lineIs(bp.linenum + 1).build();
        ScriptExecutor.debuggerSession.install(breakp);
        ScriptExecutor.breakpoints.put(bp, breakp);
    }
    
    public static void removeBreakpoint(final Project project, final Breakpoint bPoint) {
        final com.oracle.truffle.api.debug.Breakpoint bp = ScriptExecutor.breakpoints.get(bPoint);
        if (bp == null) {
            ScriptExecutor.log.debug(Markers.SYSTEM, "Unable to remove non-existent breakpoint in " + bPoint.filename + " line " + bPoint.linenum);
            return;
        }
        bp.dispose();
        ScriptExecutor.breakpoints.remove(bPoint);
    }
    
    public static Value getCurrentBindings() {
        return ScriptExecutor.jsBindings.get();
    }
    
    private static void createJavaScriptContext() {
        final Map<String, String> options = new HashMap<String, String>();
        final String nodeModulesDir = getNodeModulesDir();
        if (nodeModulesDir != null && nodeModulesDir.trim().length() > 0) {
            options.put("js.commonjs-require", "true");
            options.put("js.commonjs-require-cwd", nodeModulesDir);
        }
        if (SettingsManager.getInstance().getStringSetting(SettingName.NODE_GLOBALS) != null) {
            options.put("js.commonjs-global-properties", SettingsManager.getInstance().getStringSetting(SettingName.NODE_GLOBALS));
        }
        if (SettingsManager.getInstance().getStringSetting(SettingName.NODE_MODULES_REPLACEMENTS) != null) {
            options.put("js.commonjs-core-modules-replacements", SettingsManager.getInstance().getStringSetting(SettingName.NODE_MODULES_REPLACEMENTS));
        }
        @SuppressWarnings("deprecation")
        final Context.Builder builder = Context.newBuilder(new String[0]).allowExperimentalOptions(true).allowIO(true).options((Map)options).allowAllAccess(true).engine(ScriptExecutor.globalEngine);
        final ClassLoader libsClsLoader = RepositoryManager.getLibrariesClassLoader();
        if (libsClsLoader != null) {
            builder.hostClassLoader(libsClsLoader);
        }
        final Context jsContext = builder.build();
        ScriptExecutor.jsContexts.set(jsContext);
        final Value val = jsContext.getPolyglotBindings();
        val.putMember("polyglot.js.allowAllAccess", (Object)true);
        ScriptExecutor.jsBindings.set(val);
    }
    
    private static String getNodeModulesDir() {
        if (ScriptExecutor.nodeModulesDir != null) {
            return ScriptExecutor.nodeModulesDir;
        }
        ScriptExecutor.nodeModulesDir = SettingsManager.getInstance().getStringSetting(SettingName.NODE_MODULES_DIR);
        if (ScriptExecutor.nodeModulesDir == null || ScriptExecutor.nodeModulesDir.trim().length() == 0) {
            ScriptExecutor.nodeModulesDir = "";
        }
        final File dir = new File(ScriptExecutor.nodeModulesDir);
        if (!dir.exists()) {
            ScriptExecutor.log.error(Markers.SYSTEM, "node-modules-dir was specified as " + ScriptExecutor.nodeModulesDir + " but does not exist. Node modules will not be available.");
            return null;
        }
        if (!dir.canRead()) {
            ScriptExecutor.log.error(Markers.SYSTEM, "node-modules-dir was specified as " + ScriptExecutor.nodeModulesDir + " but cannot be read. Node modules will not be available.");
            return null;
        }
        return ScriptExecutor.nodeModulesDir;
    }
    
    static {
        ScriptExecutor.jsContexts = new ThreadLocal<Context>();
        ScriptExecutor.jsBindings = new ThreadLocal<Value>();
        ScriptExecutor.breakpoints = new HashMap<Breakpoint, com.oracle.truffle.api.debug.Breakpoint>();
        ScriptExecutor.nodeModulesDir = null;
        log = LogManager.getLogger("galliumdata.core");
        userLog = LogManager.getLogger("galliumdata.uselog");
    }
}
