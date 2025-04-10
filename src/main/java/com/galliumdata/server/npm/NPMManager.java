// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.npm;

import org.apache.logging.log4j.LogManager;
import org.orienteer.jnpm.IInstallationStrategy;
import org.orienteer.jnpm.InstallationStrategy;
import org.orienteer.jnpm.traversal.TraversalTree;
import org.orienteer.jnpm.JNPMSettings;
import com.galliumdata.server.log.Markers;
import java.io.File;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import java.nio.file.Path;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraverseDirection;
import java.nio.file.Paths;
import org.orienteer.jnpm.dm.PackageInfo;
import com.vdurmont.semver4j.Semver;
import org.orienteer.jnpm.dm.VersionInfo;
import java.util.Map;
import java.util.Iterator;
import org.orienteer.jnpm.dm.search.SearchResults;
import org.orienteer.jnpm.dm.search.SearchResultItem;
import org.orienteer.jnpm.JNPMService;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class NPMManager
{
    private static boolean npmAvailable;
    private static final Logger log;
    
    public static List<NPMPackage> searchForModule(final String searchString) {
        initJNPM();
        if (!NPMManager.npmAvailable) {
            return new ArrayList<NPMPackage>();
        }
        final SearchResults searchResults = JNPMService.instance().search(searchString, Integer.valueOf(50));
        final List<SearchResultItem> items = searchResults.getObjects();
        final List<NPMPackage> result = new ArrayList<NPMPackage>();
        for (final SearchResultItem item : items) {
            final NPMPackage pkg = new NPMPackage();
            pkg.setName(item.getSearchPackage().getName());
            pkg.setDescription(item.getSearchPackage().getDescription());
            pkg.setNpmUrl(item.getSearchPackage().getNpmUrl());
            pkg.setHomePage(item.getSearchPackage().getHomepage());
            result.add(pkg);
        }
        return result;
    }
    
    public static List<NPMPackage> getVersions(final String moduleName) {
        initJNPM();
        if (!NPMManager.npmAvailable) {
            return new ArrayList<NPMPackage>();
        }
        final List<NPMPackage> versions = new ArrayList<NPMPackage>();
        final PackageInfo packageInfo = JNPMService.instance().getPackageInfo(moduleName);
        final Map<String, VersionInfo> vers = packageInfo.getVersions();
        for (final Map.Entry<String, VersionInfo> entry : vers.entrySet()) {
            final VersionInfo version = entry.getValue();
            final NPMPackage pkg = new NPMPackage();
            pkg.setName(version.getName());
            pkg.setVersion(version.getVersionAsString());
            pkg.setHomePage(version.getHomepage());
            pkg.setNpmUrl(version.getJsdelivr());
            pkg.setDescription(version.getDescription());
            versions.add(pkg);
        }
        versions.sort((a, b) -> {
            final Semver ver1 = new Semver(a.getVersion());
            final Semver ver2 = new Semver(b.getVersion());
            return ver2.compareTo(ver1);
        });
        return versions;
    }
    
    public static boolean moduleIsInstalled(final String moduleName, final String version) {
        initJNPM();
        if (!NPMManager.npmAvailable) {
            return false;
        }
        final VersionInfo versionInfo = JNPMService.instance().getVersionInfo(moduleName, version);
        return versionInfo.getLocalTarball().exists();
    }
    
    public static void installLibrary(final String libName, final String libVersion) {
        initJNPM();
        if (!NPMManager.npmAvailable) {
            NPMManager.log.error("Unable to install NPM library: " + libName + " " + libVersion + " - NPM system is not available.");
            return;
        }
        NPMManager.log.debug("Installing NPM library: " + libName + " " + libVersion);
        final VersionInfo version = JNPMService.instance().getVersionInfo(libName, libVersion);
        final String npmHome = getNPMHome();
        if (npmHome == null) {
            return;
        }
        final Path extractPath = Paths.get(npmHome, new String[0]);
        JNPMService.instance().getRxService().traverse(TraverseDirection.WIDER, ITraversalRule.TraversalRule.DEPENDENCIES, new VersionInfo[] { version }).blockingSubscribe(t -> {
            NPMManager.log.trace(Markers.SYSTEM, "Installing NPM: " + String.valueOf(t));
            t.install(extractPath, (IInstallationStrategy)InstallationStrategy.NPM).blockingAwait();
        });
        NPMManager.log.debug("Installed NPM library: " + libName + " " + libVersion);
    }
    
    public static void initJNPM() {
        if (!JNPMService.isConfigured()) {
            final String npmHome = getNPMHome();
            if (npmHome == null) {
                return;
            }
            String npmDownloadDir = SettingsManager.getInstance().getStringSetting(SettingName.NODE_MODULES_DOWNLOAD_DIR);
            if (npmDownloadDir == null) {
                npmDownloadDir = System.getProperty("java.io.tmpdir");
            }
            else {
                final File downloadDir = new File(npmDownloadDir);
                if (!downloadDir.exists() || !downloadDir.isDirectory()) {
                    NPMManager.log.error(Markers.SYSTEM, "The directory specified for the download of NPM libraries does not exist: " + npmDownloadDir + ". JavaScript libraries will not be available.");
                    NPMManager.npmAvailable = false;
                    return;
                }
            }
            final String registry = SettingsManager.getInstance().getStringSetting(SettingName.NPM_REGISTRY_URL);
            try {
                final JNPMSettings.JNPMSettingsBuilder builder = JNPMSettings.builder();
                builder.homeDirectory(Paths.get(npmHome, new String[0])).downloadDirectory(Paths.get(npmDownloadDir, new String[0]));
                if (registry != null && !registry.isBlank()) {
                    builder.registryUrl(registry);
                }
                final JNPMSettings settings = builder.build();
                JNPMService.configure(settings);
                settings.createAllDirectories();
            }
            catch (final Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private static String getNPMHome() {
        String npmHome = SettingsManager.getInstance().getStringSetting(SettingName.NODE_MODULES_DIR);
        if (npmHome == null) {
            npmHome = System.getProperty("user.home");
        }
        final String term = System.getProperty("file.separator");
        if (npmHome.endsWith(term)) {
            npmHome = npmHome.substring(0, npmHome.length() - 1);
        }
        if (npmHome.endsWith("node_modules")) {
            npmHome = npmHome.substring(0, npmHome.length() - "node_modules".length() - 1);
        }
        final File npmDir = new File(npmHome);
        if (!npmDir.exists() || !npmDir.isDirectory()) {
            NPMManager.log.error(Markers.SYSTEM, "The directory for NPM libraries does not exist: " + npmHome + ". JavaScript libraries will not be available.");
            NPMManager.npmAvailable = false;
            return null;
        }
        return npmHome;
    }
    
    static {
        NPMManager.npmAvailable = true;
        log = LogManager.getLogger("galliumdata.core");
    }
}
