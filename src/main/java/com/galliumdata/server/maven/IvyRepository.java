// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.maven;

import org.apache.logging.log4j.LogManager;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.Reader;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import com.galliumdata.server.handler.ProtocolDataValue;
import com.galliumdata.server.handler.ProtocolData;
import com.galliumdata.server.handler.ProtocolDataArray;
import com.galliumdata.server.handler.ProtocolDataObject;
import java.nio.file.Paths;
import java.nio.file.Path;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import com.galliumdata.server.log.Markers;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.module.id.ModuleId;
import java.net.URL;
import com.vdurmont.semver4j.Semver;
import java.util.Iterator;
import java.util.Set;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.SearchEngine;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.ivy.core.search.OrganisationEntry;
import com.galliumdata.server.ServerException;
import java.io.File;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import org.apache.ivy.util.MessageLogger;
import org.apache.ivy.util.Message;
import org.apache.logging.log4j.Logger;
import org.apache.ivy.Ivy;

public class IvyRepository
{
    private final Ivy ivy;
    private static final IvyRepository instance;
    private static final Logger log;
    
    private IvyRepository() {
        Message.setDefaultLogger((MessageLogger)new IvyLogger());
        this.ivy = Ivy.newInstance();
        this.ivy.getLoggerEngine().setDefaultLogger((MessageLogger)new IvyLogger());
        final String settingsLoc = SettingsManager.getInstance().getStringSetting(SettingName.IVY_SETTINGS);
        if (settingsLoc != null && settingsLoc.trim().length() > 0) {
            final File settingsFile = new File(settingsLoc);
            if (!settingsFile.exists() || !settingsFile.isFile() || !settingsFile.canRead()) {
                throw new ServerException("settings.BadIvySettings", new Object[] { settingsLoc });
            }
            try {
                this.ivy.configure(settingsFile);
            }
            catch (final Exception ex) {
                throw new ServerException("core.IvyConfigError", new Object[] { ex.getMessage() });
            }
        }
        else {
            try {
                this.ivy.configureDefault();
            }
            catch (final Exception ex2) {
                throw new ServerException("core.IvyConfigError", new Object[] { ex2.getMessage() });
            }
        }
    }
    
    public static IvyRepository getInstance() {
        return IvyRepository.instance;
    }
    
    public String[] findArtifacts(final String orgName) {
        final SearchEngine search = this.ivy.getSearchEngine();
        final OrganisationEntry orgEntry = new OrganisationEntry(this.ivy.getSettings().getDefaultResolver(), orgName);
        final ModuleEntry[] modules = search.listModuleEntries(orgEntry);
        final Set<String> uniqueModules = new HashSet<String>();
        for (final ModuleEntry module : modules) {
            uniqueModules.add(module.getModule());
        }
        final String[] uniqueArray = new String[uniqueModules.size()];
        int i = 0;
        for (final String moduleName : uniqueModules) {
            uniqueArray[i++] = moduleName;
        }
        Arrays.sort(uniqueArray);
        return uniqueArray;
    }
    
    public String[] findVersions(final String orgId, final String artifactId) {
        final String[] versions = this.ivy.listRevisions(orgId, artifactId);
        Arrays.sort(versions, (a, b) -> {
            final Semver ver1 = new Semver(a, Semver.SemverType.LOOSE);
            final Semver ver2 = new Semver(b, Semver.SemverType.LOOSE);
            return ver2.compareTo(ver1);
        });
        return versions;
    }
    
    public URL[] getLibraryWithDependencies(final String orgId, final String artifactId, final String version) {
        final URL[] cachedUrls = this.readCachedDependencies(orgId, artifactId, version);
        if (cachedUrls != null) {
            return cachedUrls;
        }
        final Set<File> allFiles = new HashSet<File>();
        this.downloadModule(orgId, artifactId, version, allFiles);
        final URL[] urls = new URL[allFiles.size()];
        int i = 0;
        for (final File f : allFiles) {
            if (f == null) {
                continue;
            }
            try {
                urls[i] = f.toURI().toURL();
            }
            catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
            ++i;
        }
        this.cacheDependencies(orgId, artifactId, version, urls);
        return urls;
    }
    
    private void downloadModule(final String orgId, final String artifactId, final String version, final Set<File> allFiles) {
        final ModuleId moduleId = new ModuleId(orgId, artifactId);
        final ModuleRevisionId modRev = new ModuleRevisionId(moduleId, version);
        final ResolvedModuleRevision rev = this.ivy.findModule(modRev);
        if (rev == null) {
            throw new ServerException("repo.UnableToLoadLibrary", new Object[] { orgId, artifactId, version, "Library not found" });
        }
        final DependencyResolver depRes = rev.getArtifactResolver();
        final Artifact[] allArtifacts;
        final Artifact[] artifacts = allArtifacts = rev.getDescriptor().getAllArtifacts();
        for (int length = allArtifacts.length, i = 0; i < length; ++i) {
            final Artifact artifact = allArtifacts[i];
            if (artifact.getExt().equals("jar") && (artifact.getType().equals("bundle") || artifact.getType().equals("jar"))) {
                final DownloadReport report = depRes.download(new Artifact[] { artifact }, (DownloadOptions)new DownloadOptions() {});
                final ArtifactDownloadReport[] reports = report.getArtifactsReports();
                final ArtifactDownloadReport repEntry = reports[0];
                if (repEntry.getDownloadStatus() == DownloadStatus.FAILED) {
                    IvyRepository.log.warn(Markers.SYSTEM, "Unable to load library " + String.valueOf(artifact) + " : " + repEntry.getDownloadDetails());
                }
                else {
                    final File localFile = reports[0].getLocalFile();
                    if (localFile == null) {
                        throw new ServerException("repo.UnableToLoadLibrary", new Object[] { orgId, artifactId, version, "File not found" });
                    }
                    allFiles.add(reports[0].getLocalFile());
                }
            }
        }
        final DependencyDescriptor[] dependencies;
        final DependencyDescriptor[] deps = dependencies = rev.getDescriptor().getDependencies();
        for (final DependencyDescriptor dep : dependencies) {
            final String[] cfgs = dep.getDependencyConfigurations("runtime");
            if (cfgs != null && cfgs.length > 0) {
                final ModuleRevisionId depRev = dep.getDependencyRevisionId();
                this.downloadModule(depRev.getOrganisation(), depRev.getName(), depRev.getRevision(), allFiles);
            }
        }
    }
    
    private Path getCacheFileName(final String orgId, final String artifactId, final String version) {
        final String tmpDirName = System.getProperty("java.io.tmpdir");
        final Path tmpDirPath = Paths.get(tmpDirName, new String[0]);
        return tmpDirPath.resolve("GalliumData_cache_" + orgId + "_" + artifactId + "_" + version + ".json");
    }
    
    private void cacheDependencies(final String orgId, final String artifactId, final String version, final URL[] urls) {
        final Path filePath = this.getCacheFileName(orgId, artifactId, version);
        final File file = filePath.toFile();
        if (file.exists() && !file.delete()) {
            IvyRepository.log.warn(Markers.SYSTEM, "Unable to delete library dependencies file " + file.getAbsolutePath());
            return;
        }
        final ProtocolDataObject topObj = new ProtocolDataObject();
        topObj.putString("orgId", orgId);
        topObj.putString("artifactId", artifactId);
        topObj.putString("version", version);
        final ProtocolDataArray deps = new ProtocolDataArray();
        topObj.put("dependencies", deps);
        for (final URL url : urls) {
            deps.add(new ProtocolDataValue(url.toString()));
        }
        IvyRepository.log.trace(Markers.SYSTEM, "Writing dependencies for " + orgId + "." + artifactId + "." + version + " to " + file.getAbsolutePath());
        try {
            final FileWriter writer = new FileWriter(file);
            writer.write(topObj.toPrettyJSON(2));
            writer.close();
        }
        catch (final Exception ex) {
            IvyRepository.log.warn(Markers.SYSTEM, "Unable to cache library dependencies for " + orgId + "/" + artifactId + "/" + version + " : " + ex.getMessage());
        }
    }
    
    private URL[] readCachedDependencies(final String orgId, final String artifactId, final String version) {
        final Path filePath = this.getCacheFileName(orgId, artifactId, version);
        final File file = filePath.toFile();
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode topNode;
        try (final BufferedReader reader = Files.newBufferedReader(filePath)) {
            topNode = mapper.readTree((Reader)reader);
        }
        catch (final Exception ex) {
            IvyRepository.log.warn(Markers.SYSTEM, "Unable to read library dependency cache " + file.getAbsolutePath() + " : " + ex.getMessage());
            return null;
        }
        final JsonNode orgIdNode = topNode.get("orgId");
        if (orgIdNode == null || !orgIdNode.isTextual()) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : orgId missing or invalid");
            return null;
        }
        final String cacheOrgId = orgIdNode.textValue();
        if (cacheOrgId == null || cacheOrgId.trim().length() == 0) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : orgId missing or invalid");
            return null;
        }
        final JsonNode artifactIdNode = topNode.get("artifactId");
        if (artifactIdNode == null || !artifactIdNode.isTextual()) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : artifactId missing or invalid");
            return null;
        }
        final String cacheArtifactId = artifactIdNode.textValue();
        if (cacheArtifactId == null || cacheArtifactId.trim().length() == 0) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : artifactId missing or invalid");
            return null;
        }
        final JsonNode versionNode = topNode.get("version");
        if (versionNode == null || !versionNode.isTextual()) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : version missing or invalid");
            return null;
        }
        final String cacheVersion = versionNode.textValue();
        if (cacheVersion == null || cacheVersion.trim().length() == 0) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : version missing or invalid");
            return null;
        }
        if (!cacheOrgId.equals(orgId)) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : orgId is " + cacheOrgId + ", not the expected " + orgId);
            return null;
        }
        if (!cacheArtifactId.equals(artifactId)) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : artifactId is " + cacheArtifactId + ", not the expected " + artifactId);
            return null;
        }
        if (!cacheVersion.equals(version)) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : version is " + cacheVersion + ", not the expected " + version);
            return null;
        }
        final JsonNode deps = topNode.get("dependencies");
        if (deps == null || !deps.isArray()) {
            IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : dependencies are missing or have the wrong format");
            return null;
        }
        final Set<URL> urls = new HashSet<URL>();
        for (int i = 0; i < deps.size(); ++i) {
            final JsonNode dep = deps.get(i);
            if (dep == null || !dep.isTextual()) {
                IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : invalid dependency");
                return null;
            }
            URL url;
            File urlFile;
            try {
                url = new URL(dep.asText());
                urlFile = Paths.get(url.toURI()).toFile();
            }
            catch (final Exception ex2) {
                IvyRepository.log.warn(Markers.SYSTEM, "Invalid library dependency cache " + file.getAbsolutePath() + " : invalid dependency: " + dep.asText());
                return null;
            }
            if (!urlFile.exists() || !urlFile.canRead()) {
                IvyRepository.log.debug(Markers.SYSTEM, "Library dependency cache " + file.getAbsolutePath() + " points to file " + urlFile.getAbsolutePath() + ", which does not exist, so the cache will be ignored.");
                return null;
            }
            urls.add(url);
        }
        return urls.toArray(new URL[0]);
    }
    
    static {
        instance = new IvyRepository();
        log = LogManager.getLogger("galliumdata.core");
    }
}
