// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import org.apache.logging.log4j.LogManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.io.FileWriter;
import com.galliumdata.server.ServerException;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Set;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import java.nio.file.Path;

public class Repository extends RepositoryObject
{
    private final Path rootDir;
    private static final String FILENAME = "repository.json";
    private static final Logger log;
    @Persisted(JSONName = "RepositoryVersion")
    protected String versionNum;
    @Persisted(directoryName = "projects", fileName = "project.json", memberClass = Project.class)
    protected Map<String, Project> projects;
    @Persisted(JSONName = "libraries", memberClass = Library.class)
    protected Set<Library> libraries;
    @Persisted(JSONName = "logSettings", memberClass = LogSetting.class)
    protected Set<LogSetting> logSettings;
    
    public Repository(final String rootDir) {
        super(null);
        this.setRepository(this);
        if (null == rootDir) {
            throw new RepositoryException("repo.BadLocation", new Object[] { "null", "null" });
        }
        this.rootDir = Paths.get(rootDir, new String[0]);
        if (!Files.exists(this.rootDir, new LinkOption[0])) {
            try {
                Files.createDirectories(this.rootDir, (FileAttribute<?>[])new FileAttribute[0]);
            }
            catch (final Exception ex) {
                throw new ServerException("repo.CannotCreateRepo", new Object[] { rootDir, ex.getMessage() });
            }
        }
        RepositoryUtil.checkDirectory(this.rootDir);
        this.path = this.rootDir.resolve("repository.json");
        if (!Files.exists(this.path, new LinkOption[0])) {
            Repository.log.info("Repository does not exist at " + rootDir + ", creating it now");
            try {
                final FileWriter fw = new FileWriter(this.path.toFile());
                fw.write("{\n  \"RepositoryVersion\": \"1.0\",\n  \"SystemSettings\": {},\n  \"libraries\": [],\n  \"logSettings\": [\n    {\n      \"loggerName\": \"core\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"uselog\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"rest\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"dbproto\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"ssl\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"maven\",\n      \"level\": \"INFO\"\n    }\n  ]\n}");
                fw.close();
                final Path projsPath = this.path.resolveSibling("projects");
                if (!Files.exists(projsPath, new LinkOption[0])) {
                    Files.createDirectory(projsPath, (FileAttribute<?>[])new FileAttribute[0]);
                }
            }
            catch (final Exception ex) {
                throw new ServerException("repo.CannotCreateRepo", new Object[] { this.path.toString(), ex.getMessage() });
            }
        }
        RepositoryUtil.checkFile(this.path);
        this.readFromJson();
    }
    
    @Override
    public String getName() {
        return "repository";
    }
    
    protected String getFileName() {
        return "repository.json";
    }
    
    public Path getRootDir() {
        return this.rootDir;
    }
    
    public String getVersionNum() {
        return this.versionNum;
    }
    
    public void setVersionNum(final String versionNum) {
        this.versionNum = versionNum;
    }
    
    public Map<String, Project> getProjects() {
        if (this.projects == null) {
            this.projects = new TreeMap<String, Project>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("projects"));
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        for (final Project project : this.projects.values()) {
            if (!project.isActive()) {
                continue;
            }
            for (final Connection conn : project.getConnections().values()) {
                if (!conn.isActive()) {
                    continue;
                }
            }
        }
        return this.projects;
    }
    
    public Set<Library> getLibraries() {
        return this.libraries;
    }
    
    public Set<LogSetting> getLogSettings() {
        if (this.logSettings == null) {
            return new HashSet<LogSetting>();
        }
        return this.logSettings;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
