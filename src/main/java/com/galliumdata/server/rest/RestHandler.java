// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.adapters.AdapterInterface;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.adapters.AdapterStatus;
import java.awt.Font;
import java.util.Iterator;
import java.lang.management.MemoryUsage;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.Locale;
import java.awt.GraphicsEnvironment;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.polyglot.Language;
import com.galliumdata.server.logic.ScriptExecutor;
import java.util.Date;
import java.util.Calendar;
import java.lang.management.ManagementFactory;
import java.util.function.BiConsumer;
import java.util.Objects;
import java.util.Map;
import java.util.TreeMap;
import com.galliumdata.server.handler.ProtocolDataValue;
import java.time.format.DateTimeFormatter;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.Main;
import java.io.OutputStream;
import java.io.IOException;
import com.galliumdata.server.handler.ProtocolData;
import java.net.URI;
import java.io.InputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galliumdata.server.log.Markers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;
import com.sun.net.httpserver.HttpHandler;

public class RestHandler implements HttpHandler
{
    private static final Logger log;
    
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        if (RestHandler.log.isTraceEnabled()) {
            RestHandler.log.trace(Markers.WEB, "Received REST request: " + exchange.getRequestMethod() + " : " + String.valueOf(exchange.getRequestURI()));
        }
        if (!RestManager.checkAddress(exchange)) {
            return;
        }
        final String method = exchange.getRequestMethod();
        JsonNode node = null;
        if ("POST".equals(method)) {
            final InputStream in = exchange.getRequestBody();
            final ObjectMapper mapper = new ObjectMapper();
            node = mapper.readTree(in);
        }
        if ("OPTIONS".equals(method)) {
            handleOptions(exchange);
            return;
        }
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        final String[] pathParts = path.split("/");
        ProtocolData data = null;
        if (pathParts.length == 3 && pathParts[2].equals("state")) {
            data = this.getState();
        }
        else if (pathParts.length >= 3 && pathParts[2].equals("breakpoints")) {
            data = RestBreakpointHandler.handleRequest(exchange, node);
        }
        else if (pathParts.length >= 3 && pathParts[2].equals("debug")) {
            data = RestDebugHandler.handleRequest(exchange);
        }
        else if (pathParts.length >= 3 && pathParts[2].equals("debugvalue")) {
            data = RestDebugValueHandler.handleRequest(exchange);
        }
        else if (pathParts.length == 3 && pathParts[2].equals("debugeval")) {
            if (!"POST".equals(method)) {
                throw new RuntimeException("Cannot call debugeval with anything but POST");
            }
            data = RestDebugEvalHandler.handleRequest(exchange, node);
        }
        else if (pathParts.length >= 5 && pathParts[2].equals("connection")) {
            data = ConnectionTestHandler.handleRequest(exchange);
        }
        else if (pathParts.length == 3 && pathParts[2].equals("logs")) {
            data = RestLogHandler.getCurrentLogs(exchange);
        }
        else if (pathParts.length == 4 && pathParts[2].equals("logs") && pathParts[3].equals("clear")) {
            data = RestLogHandler.clearLogs(exchange);
        }
        else if (pathParts.length == 3 && pathParts[2].equals("logsettings")) {
            if ("GET".equals(method)) {
                data = RestLogHandler.getLogSettings(exchange);
            }
            else if ("POST".equals(method)) {
                data = RestLogHandler.postLogSettings(exchange, node);
            }
        }
        else if (pathParts.length >= 3 && pathParts[2].equals("libraries")) {
            data = LibrariesHandler.handle(exchange);
        }
        else if (pathParts.length >= 3 && pathParts[2].equals("npms")) {
            data = NPMHandler.handle(exchange);
        }
        else {
            data = new ProtocolDataObject();
            data.putString("errorMsg", "Unknown URL");
        }
        final String response = (data == null) ? "" : data.toJSON();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        int responseCode = 200;
        if ("DELETE".equals(method) && data == null) {
            responseCode = 204;
            exchange.sendResponseHeaders(responseCode, -1L);
        }
        if (!"DELETE".equals(method)) {
            exchange.sendResponseHeaders(responseCode, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
        }
        exchange.getResponseBody().flush();
        exchange.close();
    }
    
    public static void handleOptions(final HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(200, 0L);
        final OutputStream out = exchange.getResponseBody();
        out.close();
    }
    
    private ProtocolDataObject getState() {
        RestHandler.log.trace(Markers.WEB, "Getting REST - state");
        final ProtocolDataObject topObj = new ProtocolDataObject();
        final ProtocolDataObject projectsObj = new ProtocolDataObject();
        topObj.put("projects", projectsObj);
        Main.getRunningAdapters().forEach((conn, adapt) -> {
            if (!conn.isActive()) {
                return;
            }
            else {
                final Project project = (Project)conn.getParentObject();
                final ProtocolData projectEntry = projectsObj.get(project.getName());
                ProtocolData connectionsEntry;
                if (projectEntry == null) {
                    final ProtocolData projectEntry2 = new ProtocolDataObject();
                    projectsObj.put(project.getName(), projectEntry2);
                    connectionsEntry = new ProtocolDataObject();
                    projectEntry2.put("connections", connectionsEntry);
                }
                else {
                    connectionsEntry = projectEntry.get("connections");
                }
                final ProtocolData connectionEntry = new ProtocolDataObject();
                connectionsEntry.put(conn.getName(), connectionEntry);
                final AdapterStatus status = adapt.getStatus();
                connectionEntry.putString("startTime", status.startTime.format(DateTimeFormatter.ISO_INSTANT));
                connectionEntry.put("numRequests", new ProtocolDataValue(status.numRequests));
                connectionEntry.put("numRequestBytes", new ProtocolDataValue(status.numRequestBytes));
                connectionEntry.put("numResponses", new ProtocolDataValue(status.numResponses));
                connectionEntry.put("numResponseBytes", new ProtocolDataValue(status.numResponseBytes));
                return;
            }
        });
        final ProtocolDataObject envObj = new ProtocolDataObject();
        topObj.put("environmentVariables", envObj);
        final TreeMap<Object, String> treeMap= new TreeMap<Object, String>(System.getenv());
        final TreeMap<Object, String> sortedEnv = treeMap;
        final ProtocolDataObject obj = envObj;
        Objects.requireNonNull(obj);
        treeMap.forEach((k,v)->obj.putString((String) k,v));
        final ProtocolDataObject sysVarObj = new ProtocolDataObject();
        topObj.put("systemVariables", sysVarObj);
        final Map<String, String> sysVarMap = new TreeMap<String, String>();
        System.getProperties().entrySet().forEach(e -> sysVarMap.put(e.getKey().toString(), (e.getValue() == null) ? "null" : e.getValue().toString()));
        final Map<String, String> map = sysVarMap;
        final ProtocolDataObject obj2 = sysVarObj;
        Objects.requireNonNull(obj2);
        map.forEach(obj2::putString);
        final ProtocolDataObject jmxObj = new ProtocolDataObject();
        topObj.put("jmxRuntime", jmxObj);
        final RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        jmxObj.putString("Gallium Data version (server)", "1.9.3");
        jmxObj.putString("Gallium Data build (server)", "2245");
        jmxObj.putString("Gallium Data edition", "Community");
        jmxObj.putString("UUID of this Gallium Data instance", Main.uuid);
        jmxObj.putString("Java system time zone", Calendar.getInstance().getTimeZone().getDisplayName());
        jmxObj.putString("current server time", Calendar.getInstance().toInstant().atZone(Calendar.getInstance().getTimeZone().toZoneId()).toString());
        jmxObj.putString("classPath", mxbean.getClassPath());
        jmxObj.putString("libraryPath", mxbean.getLibraryPath());
        jmxObj.putString("managementSpecVersion", mxbean.getManagementSpecVersion());
        jmxObj.putString("specName", mxbean.getSpecName());
        jmxObj.putString("specVersion", mxbean.getSpecVersion());
        jmxObj.putString("vmName", mxbean.getVmName());
        jmxObj.putString("vmVendor", mxbean.getVmVendor());
        jmxObj.putString("vmVersion", mxbean.getVmVersion());
        jmxObj.putNumber("pid", mxbean.getPid());
        jmxObj.putString("startTime", new Date(mxbean.getStartTime()).toString());
        jmxObj.putNumber("upTime", mxbean.getUptime());
        try {
            jmxObj.putString("bootClassPath", mxbean.getBootClassPath());
        }
        catch (final Exception ex) {
            jmxObj.putString("bootClassPath", "Not supported");
        }
        final ProtocolDataObject osObj = new ProtocolDataObject();
        topObj.put("jmxOS", osObj);
        final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        osObj.putString("name", osBean.getName());
        osObj.putString("version", osBean.getVersion());
        osObj.putString("architecture", osBean.getArch());
        osObj.putNumber("availableProcessors", osBean.getAvailableProcessors());
        osObj.putNumber("systemLoadAverage", osBean.getSystemLoadAverage());
        final ProtocolDataObject threadObj = new ProtocolDataObject();
        topObj.put("jmxThreads", threadObj);
        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        threadObj.putNumber("threadCount", threadBean.getThreadCount());
        threadObj.putNumber("peakThreadCount", threadBean.getPeakThreadCount());
        threadObj.putNumber("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());
        final ProtocolDataObject memObj = new ProtocolDataObject();
        topObj.put("jmxMemory", memObj);
        final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        memObj.putBoolean("isVerbose", memBean.isVerbose());
        final MemoryUsage heapMem = memBean.getHeapMemoryUsage();
        memObj.putNumber("heapInitial", heapMem.getInit());
        memObj.putNumber("heapUsed", heapMem.getUsed());
        memObj.putNumber("heapCommitted", heapMem.getCommitted());
        memObj.putNumber("heapMax", heapMem.getMax());
        final MemoryUsage nonHeapMem = memBean.getNonHeapMemoryUsage();
        memObj.putNumber("nonHeapInitial", nonHeapMem.getInit());
        memObj.putNumber("nonHeapUsed", nonHeapMem.getUsed());
        memObj.putNumber("nonHeapCommitted", nonHeapMem.getCommitted());
        memObj.putNumber("nonHeapMax", nonHeapMem.getMax());
        if (ScriptExecutor.globalEngine != null) {
            final ProtocolDataObject jsObj = new ProtocolDataObject();
            topObj.put("scriptEngine", jsObj);
            jsObj.putString("implementation", ScriptExecutor.globalEngine.getImplementationName());
            jsObj.putString("version", ScriptExecutor.globalEngine.getVersion());
            jsObj.putString("instruments", ScriptExecutor.globalEngine.getInstruments().keySet().toString());
            jsObj.putString("languages", ScriptExecutor.globalEngine.getLanguages().keySet().toString());
            for (Map.Entry<String, Language> entry : ScriptExecutor.globalEngine.getLanguages().entrySet()) {
                final Language lang = entry.getValue();
                jsObj.putString((String)entry.getKey() + " version", lang.getVersion());
                jsObj.putString((String)entry.getKey() + " implementation", lang.getImplementationName());
                jsObj.putString((String)entry.getKey() + " MIME type", lang.getDefaultMimeType());
                String opts = "";
                for (OptionDescriptor optDesc : lang.getOptions()) {
                    if (opts.length() > 0) {
                        opts += "<br/>\n";
                    }
                    opts = opts + optDesc.getName() + ": " + optDesc.getHelp();
                }
                jsObj.putString((String)entry.getKey() + " options", opts);
            }
            final StringBuilder fontsNames = new StringBuilder();
            try {
                System.setProperty("java.awt.headless", "true");
                final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                final Font[] allFonts2;
                final Font[] allFonts = allFonts2 = ge.getAllFonts();
                for (final Font font : allFonts2) {
                    fontsNames.append(font.getFontName(Locale.US));
                    fontsNames.append("<br/>");
                }
            }
            catch (final Exception ex2) {
                fontsNames.append("Unable to retrieve font names: " + ex2.getMessage());
            }
            jsObj.putString("Available fonts", fontsNames.toString());
        }
        return topObj;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
