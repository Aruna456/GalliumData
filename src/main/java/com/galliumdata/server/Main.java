// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManager;
import com.galliumdata.server.security.KeyLoaderManager;
import com.galliumdata.server.adapters.AdapterCallback;
import com.galliumdata.server.adapters.AdapterManager;
import com.galliumdata.server.repository.Library;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import com.galliumdata.server.settings.SettingName;
import java.util.Iterator;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.telemetry.TelemetryNewRelicService;
import com.galliumdata.server.repository.Project;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import com.galliumdata.server.repository.LogSetting;
import com.galliumdata.server.repository.RepositoryManager;
import com.galliumdata.server.rest.RestManager;
import com.galliumdata.server.settings.SettingsManager;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.adapters.AdapterInterface;
import com.galliumdata.server.repository.Connection;
import java.util.Map;
import org.apache.logging.log4j.Logger;

public class Main
{
    public static final String VERSION_NUMBER = "1.9.3";
    public static final String BUILD_NUMBER = "2245";
    public static final String EDITION_NAME = "Community";
    private static final Logger log;
    private static final Logger logDb;
    private static final Logger logLogic;
    private static final Logger logRest;
    private static final Logger logSsl;
    private static final Logger logNetwork;
    private static final Logger logMaven;
    private static final Map<Connection, AdapterInterface> runningAdapters;
    private static final Map<Connection, Thread> adapterThreads;
    public static String uuid;
    private static final int MAX_RUNNING_ADAPTERS = 1000;
    public static final String INSTANCE_START_TIMESTAMP;
    
    public static void main(final String[] args) {
        try{
            Main.log.info(Markers.SYSTEM, "Gallium Data v.1.9.3 (build 2245) is now starting");
            System.setProperty("java.awt.headless", "true");

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    Main.log.info("Gallium Data is shutting down, cleaning up...");
                    stopServer();
                }));

            SettingsManager.initialize(args);
            RestManager.startService();
            startServer();
            Main.log.info(Markers.SYSTEM, "Gallium Data is now running");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public static void startServer() {
        final Repository mainRepo = RepositoryManager.getMainRepository();
        final Map<String, Project> projects = mainRepo.getProjects();
        RepositoryManager.getLibrariesClassLoader();
        for (LogSetting setting : mainRepo.getLogSettings()) {
            final LoggerContext logContext = (LoggerContext)LogManager.getContext(false);
            final String loggerName = "galliumdata." + setting.getLoggerName();
            try {
                logContext.getConfiguration().getLoggerConfig(loggerName).setLevel(Level.getLevel(setting.getLevel()));
            }
            catch (final Exception ex) {
                Main.log.warn(Markers.SYSTEM, "Unable to set logging level specified in repository: " + String.valueOf(setting) + ", error is:" + ex.getMessage());
            }
        }
        loadKeys(projects.values());
        for (final Project project : projects.values()) {
            if (project.isActive()) {
                startProject(project);
            }
        }
        getSystemUUID();
        TelemetryNewRelicService.sendEventAsync("serverStart", "buildNumber", "2245");
        TelemetryNewRelicService.startUptimeTelemetry();
    }
    
    private static void getSystemUUID() {
        final String settingUUID = SettingsManager.getInstance().getStringSetting(SettingName.SYSTEM_UUID);
        if (settingUUID != null && !settingUUID.isBlank()) {
            Main.uuid = settingUUID;
            return;
        }
        final String tmpBase = System.getProperty("java.io.tmpdir");
        final String sep = System.getProperty("file.separator");
        final Path uuidPath = Paths.get(tmpBase + sep + "galliumdata.uuid", new String[0]);
        if (Files.exists(uuidPath, new LinkOption[0]) && Files.isRegularFile(uuidPath, new LinkOption[0]) && Files.isReadable(uuidPath)) {
            try {
                final byte[] bytes = Files.readAllBytes(uuidPath);
                Main.uuid = UUID.fromString(new String(bytes)).toString();
            }
            catch (final Exception ex2) {
                ex2.printStackTrace();
            }
        }
        else {
            Main.uuid = UUID.randomUUID().toString();
            try {
                Files.write(uuidPath, Main.uuid.getBytes(), new OpenOption[0]);
            }
            catch (final Exception ex) {
                Main.log.debug(Markers.SYSTEM, "Unable to write UUID to " + String.valueOf(uuidPath) + ": " + ex.getMessage());
            }
        }
    }
    
    public static void switchRepository(final Repository oldRepo) {
        Main.log.debug(Markers.REPO, "Switching repository");
        final Repository mainRepo = RepositoryManager.getMainRepository();
        final Set<Library> newLibs = mainRepo.getLibraries();
        final Set<Library> oldLibs = oldRepo.getLibraries();
        if ((newLibs == null && oldLibs != null) || (newLibs != null && oldLibs == null)) {
            RepositoryManager.resetLibrariesClassLoader();
        }
        if (oldLibs != null && newLibs != null && !oldLibs.equals(newLibs)) {
            RepositoryManager.resetLibrariesClassLoader();
        }
        final Set<Project> deletedProjects = new HashSet<Project>();
        final Set<Project> newProjects = new HashSet<Project>();
        for (final Map.Entry<String, Project> entry : mainRepo.getProjects().entrySet()) {
            final String projectName = entry.getKey();
            if (!oldRepo.getProjects().containsKey(projectName)) {
                newProjects.add(entry.getValue());
            }
        }
        for (final Map.Entry<String, Project> entry : oldRepo.getProjects().entrySet()) {
            final String projectName = entry.getKey();
            if (!mainRepo.getProjects().containsKey(projectName)) {
                deletedProjects.add(entry.getValue());
            }
        }
        final Map<Connection, AdapterInterface> adaptersToRemove = new HashMap<Connection, AdapterInterface>();
        for (Project deletedProject : deletedProjects) {
            if (deletedProject.isActive()) {
                Main.log.trace(Markers.SYSTEM, "Stopping deleted project: " + deletedProject.getName());
                for (final Map.Entry<String, Connection> conEntry : deletedProject.getConnections().entrySet()) {
                    final Connection conn = conEntry.getValue();
                    if (conn.isActive()) {
                        final AdapterInterface adapter = Main.runningAdapters.get(conn);
                        if (adapter == null) {
                            throw new RuntimeException("UNEXPECTED: AdapterInterface has disappeared after repo switch???");
                        }
                        adapter.shutdown();
                        adaptersToRemove.put(conn, adapter);
                    }
                }
            }
        }
        for (final Map.Entry<Connection, AdapterInterface> entry2 : adaptersToRemove.entrySet()) {
            final Connection conn2 = entry2.getKey();
            final AdapterInterface adapter2 = Main.runningAdapters.get(conn2);
            if (adapter2 == null) {
                throw new RuntimeException("UNEXPECTED: Project was deleted, but can't find its connection to close");
            }
            if (adapter2 != entry2.getValue()) {
                throw new RuntimeException("UNEXPECTED: Project was deleted, but its adapter was not as expected");
            }
            Main.runningAdapters.remove(conn2);
            Main.adapterThreads.remove(conn2);
        }
        for (Map.Entry<String, Project> projEntry : oldRepo.getProjects().entrySet()) {
            final String oldProjectName = projEntry.getKey();
            final Project oldProject = projEntry.getValue();
            if (deletedProjects.contains(oldProject)) {
                continue;
            }
            final Project newProject = mainRepo.getProjects().get(oldProjectName);
            if (newProject == null) {
                throw new RuntimeException("UNEXPECTED: Project has disappeared???");
            }
            newProject.setProjectContext(oldProject.getProjectContext());
            final Set<String> deletedConnections = new HashSet<String>();
            final Set<String> changedConnections = new HashSet<String>();
            final Set<String> unchangedConnections = new HashSet<String>();
            final Set<String> newConnections = new HashSet<String>();
            for (final Map.Entry<String, Connection> conEntry2 : oldProject.getConnections().entrySet()) {
                final String oldConnectionName = conEntry2.getKey();
                final Connection oldConnection = conEntry2.getValue();
                final Connection newConn = newProject.getConnections().get(oldConnectionName);
                if (newConn == null) {
                    if (!oldConnection.isActive()) {
                        continue;
                    }
                    deletedConnections.add(oldConnectionName);
                }
                else if (!newConn.getParameters().equals(oldConnection.getParameters()) || newConn.isActive() != oldConnection.isActive()) {
                    changedConnections.add(oldConnectionName);
                }
                else {
                    if (!oldConnection.isActive()) {
                        continue;
                    }
                    unchangedConnections.add(oldConnectionName);
                }
            }
            for (final Map.Entry<String, Connection> conEntry2 : newProject.getConnections().entrySet()) {
                final String newConnectionName = conEntry2.getKey();
                if (!oldProject.getConnections().containsKey(newConnectionName)) {
                    newConnections.add(newConnectionName);
                }
            }
            for (final String connName : deletedConnections) {
                final Connection conn3 = oldProject.getConnections().get(connName);
                final AdapterInterface adapter3 = Main.runningAdapters.get(conn3);
                if (adapter3 == null) {
                    continue;
                }
                adapter3.shutdown();
                Main.runningAdapters.remove(conn3);
                Main.adapterThreads.remove(conn3);
            }
            if (oldProject.getCryptoHash() != newProject.getCryptoHash()) {
                changedConnections.addAll(newProject.getConnections().keySet());
            }
            for (String connName : changedConnections) {
                final Connection conn3 = oldProject.getConnections().get(connName);
                final AdapterInterface adapter3 = Main.runningAdapters.get(conn3);
                if (conn3.isActive()) {
                    if (adapter3 == null) {
                        Main.log.debug(Markers.SYSTEM, "Adapter not found, probably not running");
                    }
                    else {
                        Main.log.debug(Markers.REPO, "Shutting down [" + adapter3.getName());
                        adapter3.shutdown();
                        Main.runningAdapters.remove(conn3);
                        Main.adapterThreads.remove(conn3);
                    }
                }
                final Connection newConn = newProject.getConnections().get(connName);
                startConnection(newProject, newConn);
            }
            for (String connName : unchangedConnections) {
                final Connection oldConn = oldProject.getConnections().get(connName);
                final Connection newConn2 = newProject.getConnections().get(connName);
                final AdapterInterface adapter4 = Main.runningAdapters.get(oldConn);
                if (adapter4 == null) {
                    Main.log.debug("Running adapter for " + oldConn.getName() + " was not found during deployment, probably a secondary connection, ignoring");
                }
                else {
                    Main.runningAdapters.remove(oldConn);
                    Main.runningAdapters.put(newConn2, adapter4);
                    final Thread thread = Main.adapterThreads.get(oldConn);
                    Main.adapterThreads.remove(oldConn);
                    Main.adapterThreads.put(newConn2, thread);
                    adapter4.switchProject(newProject, newConn2);
                }
            }
            for (final String connName : newConnections) {
                final Connection conn3 = newProject.getConnections().get(connName);
                startConnection(newProject, conn3);
            }
            newProject.forgetEverything();
        }
        loadKeys(newProjects);
        for (final Project project : newProjects) {
            startProject(project);
        }
    }
    
    public static void stopServer() {
        Main.log.info("Stopping Gallium Data server");
        final Set<Map.Entry<Connection, AdapterInterface>> adapterEntries = new HashSet<Map.Entry<Connection, AdapterInterface>>(Main.runningAdapters.entrySet());
        for (Map.Entry<Connection, AdapterInterface> entry : adapterEntries) {
            Main.log.info(Markers.SYSTEM, "Stopping connection: " + entry.getKey().getName());
            entry.getValue().stopProcessing();
            entry.getValue().shutdown();
            Main.runningAdapters.remove(entry.getKey());
        }
        if (Main.runningAdapters.size() > 0) {
            Main.log.warn(Markers.SYSTEM, "There are running adapters after stopServer ???");
            throw new RuntimeException("There are running adapters after stopServer ???");
        }
        Main.log.info("Gallium Data is now stopped");
    }
    
    protected static void startProject(final Project project) {
        for (final Connection conn : project.getConnections().values()) {
            startConnection(project, conn);
        }
    }
    
    protected static void startConnection(final Project project, final Connection conn) {
        if (!conn.isActive()) {
            return;
        }
        if (Main.runningAdapters.size() > 1000) {
            throw new ServerException("core.TooManyAdapters", new Object[] { Main.runningAdapters.size(), 1000 });
        }
        final String adapterType = conn.getAdapterType();
        final AdapterInterface adapter = AdapterManager.getInstance().instantiateAdapter(adapterType);
        final AdapterCallback callback = new AdapterCallback(project);
        if (adapter.configure(project, conn, callback)) {
            Main.runningAdapters.put(conn, adapter);
            final Thread thread = new Thread(adapter);
            thread.setName("Listener for " + conn.getName());
            Main.adapterThreads.put(conn, thread);
            thread.start();
        }
    }
    
    public static Map<Connection, AdapterInterface> getRunningAdapters() {
        return Main.runningAdapters;
    }
    
    private static void loadKeys(final Collection<Project> projects) {
        final KeyLoaderManager keyLoaderMgr = new KeyLoaderManager();
        keyLoaderMgr.loadKeyLoaders();
        final KeyManager[] keyManagers = keyLoaderMgr.getKeyManagers();
        final TrustManager[] trustManagers = keyLoaderMgr.getTrustManagers();
        if (keyManagers != null || trustManagers != null) {
            for (final Project project : projects) {
                if (project.isActive()) {
                    if (keyManagers != null) {
                        project.addKeyManagers(keyManagers);
                    }
                    if (trustManagers == null) {
                        continue;
                    }
                    project.addTrustManagers(trustManagers);
                }
            }
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
        logDb = LogManager.getLogger("galliumdata.dbproto");
        logLogic = LogManager.getLogger("galliumdata.uselog");
        logRest = LogManager.getLogger("galliumdata.rest");
        logSsl = LogManager.getLogger("galliumdata.ssl");
        logNetwork = LogManager.getLogger("galliumdata.network");
        logMaven = LogManager.getLogger("galliumdata.maven");
        runningAdapters = new ConcurrentHashMap<Connection, AdapterInterface>();
        adapterThreads = new ConcurrentHashMap<Connection, Thread>();
        Main.uuid = "";
        INSTANCE_START_TIMESTAMP = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }
}
