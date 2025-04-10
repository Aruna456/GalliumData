// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import org.apache.logging.log4j.LogManager;
import java.io.File;
import java.util.zip.ZipEntry;
import java.io.FileOutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitor;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.FileSystems;
import java.util.Calendar;
import java.util.TimeZone;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.net.URLClassLoader;
import com.galliumdata.server.npm.NPMManager;
import java.util.Collection;
import java.util.Collections;
import com.galliumdata.server.maven.IvyRepository;
import com.galliumdata.server.log.Markers;
import java.net.URL;
import java.util.HashSet;
import java.nio.file.Path;
import com.galliumdata.telemetry.TelemetryNewRelicService;
import com.galliumdata.server.metarepo.MetaRepositoryManager;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.Main;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import org.apache.logging.log4j.Logger;

public class RepositoryManager
{
    private static Repository mainRepo;
    private static ClassLoader librariesClassLoader;
    private static boolean librariesClassLoaderInitialized;
    private static final Logger log;
    
    public static Repository getMainRepository() {
        if (RepositoryManager.mainRepo != null) {
            return RepositoryManager.mainRepo;
        }
        final String repoLoc = SettingsManager.getInstance().getStringSetting(SettingName.REPO_LOCATION);
        if (null == repoLoc) {
            throw new RepositoryException("repo.BadLocation", new Object[] { "null", "Must be specified" });
        }
        return RepositoryManager.mainRepo = new Repository(repoLoc);
    }
    
    public static void installNewRepository(final ZipInputStream zis) {
        final Repository oldRepo = getMainRepository();
        final Path backupDir = createBackupDirectory();
        if (backupDir != null) {
            copyRepositoryToBackup(backupDir);
        }
        deleteRepository();
        try {
            unzipRepository(zis);
        }
        catch (final IOException ioex) {
            throw new RepositoryException("repo.backup.ErrorInstallingRepository", new Object[] { ioex.getMessage() });
        }
        RepositoryManager.mainRepo = null;
        Main.switchRepository(oldRepo);
        ScriptManager.getInstance().forgetAllScripts();
        ScriptExecutor.forgetAllScripts();
        MetaRepositoryManager.getMainRepository().forgetFilterTypes();
        try {
            TelemetryNewRelicService.sendEventAsync("serverEvent", "repository", "publish");
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static ClassLoader getLibrariesClassLoader() {
        if (RepositoryManager.librariesClassLoaderInitialized) {
            return RepositoryManager.librariesClassLoader;
        }
        final Set<Library> libs = RepositoryManager.mainRepo.getLibraries();
        final Set<URL> libUrls = new HashSet<URL>();
        if (libs != null && libs.size() > 0) {
            RepositoryManager.log.debug(Markers.SYSTEM, "Loading libraries...");
            int numNPM = 0;
            for (final Library lib : libs) {
                if (lib.type.equals("java")) {
                    final URL[] urls = IvyRepository.getInstance().getLibraryWithDependencies(lib.orgId, lib.artifactId, lib.version);
                    Collections.addAll(libUrls, urls);
                }
                else {
                    if (!lib.type.equals("javascript") || NPMManager.moduleIsInstalled(lib.artifactId, lib.version)) {
                        continue;
                    }
                    NPMManager.installLibrary(lib.artifactId, lib.version);
                    ++numNPM;
                }
            }
            if (numNPM > 0) {
                RepositoryManager.log.info(Markers.SYSTEM, "JavaScript libraries installed: " + numNPM);
            }
            if (libUrls.size() > 0) {
                final URL[] allUrls = new URL[libUrls.size()];
                int i = 0;
                for (final URL url : libUrls) {
                    allUrls[i] = url;
                    ++i;
                }
                RepositoryManager.librariesClassLoader = new URLClassLoader(allUrls, RepositoryManager.class.getClassLoader());
                RepositoryManager.log.info(Markers.SYSTEM, "Java libraries loaded: " + libUrls.size());
            }
            else {
                RepositoryManager.librariesClassLoader = null;
            }
        }
        else {
            RepositoryManager.librariesClassLoader = null;
        }
        RepositoryManager.librariesClassLoaderInitialized = true;
        return RepositoryManager.librariesClassLoader;
    }
    
    public static void resetLibrariesClassLoader() {
        RepositoryManager.librariesClassLoaderInitialized = false;
        getLibrariesClassLoader();
    }
    
    private static Path createBackupDirectory() {
        final String backupLoc = SettingsManager.getInstance().getStringSetting(SettingName.BACKUP_LOCATION);
        if (backupLoc == null || backupLoc.trim().length() == 0) {
            return null;
        }
        final Path backupPath = Paths.get(backupLoc, new String[0]);
        if (!Files.exists(backupPath, new LinkOption[0])) {
            throw new RepositoryException("repo.backup.NoSuchDirectory", new Object[] { backupLoc });
        }
        if (!Files.isDirectory(backupPath, new LinkOption[0])) {
            throw new RepositoryException("repo.backup.NotADirectory", new Object[] { backupLoc });
        }
        if (!Files.isWritable(backupPath)) {
            throw new RepositoryException("repo.backup.NotWriteable", new Object[] { backupLoc });
        }
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        final String separator = FileSystems.getDefault().getSeparator();
        final String dateStr = String.format("%d" + separator + "%02d" + separator + "%02d" + separator + "%02d%02d%02d", cal.get(1), cal.get(2) + 1, cal.get(5), cal.get(11), cal.get(12), cal.get(13));
        final Path datePath = backupPath.resolve(dateStr);
        if (!Files.exists(datePath, new LinkOption[0])) {
            try {
                Files.createDirectories(datePath, (FileAttribute<?>[])new FileAttribute[0]);
            }
            catch (final IOException ioex) {
                throw new RepositoryException("repo.backup.CannotCreateDirectory", new Object[] { datePath, ioex.getMessage() });
            }
        }
        return datePath;
    }
    
    private static void copyRepositoryToBackup(final Path target) {
        RepositoryManager.log.debug(Markers.REPO, "Copying repository to backup: {}", (Object)target);
        final Path source = getMainRepository().path.getParent();
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes attrs) throws IOException {
                    final Path newPath = target.resolve(source.relativize(path));
                    Files.createDirectories(newPath, (FileAttribute<?>[])new FileAttribute[0]);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, target.resolve(source.relativize(file)), new CopyOption[0]);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (final IOException ioex) {
            throw new RepositoryException("repo.backup.ErrorCreatingBackup", new Object[] { ioex.getMessage() });
        }
    }
    
    private static void deleteRepository() {
        RepositoryManager.log.debug(Markers.REPO, "Deleting repository");
        final Path source = getMainRepository().path.getParent();
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(final Path path, final IOException ex) throws IOException {
                    if (!path.equals(source)) {
                        Files.delete(path);
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (final AccessDeniedException adex) {
            throw new RepositoryException("repo.backup.ErrorDeletingRepository", new Object[] { "Insufficient permissions - " + adex.getMessage() });
        }
        catch (final IOException ioex) {
            throw new RepositoryException("repo.backup.ErrorDeletingRepository", new Object[] { ioex.getMessage() });
        }
    }
    
    private static void unzipRepository(final ZipInputStream zis) throws IOException {
        RepositoryManager.log.debug(Markers.REPO, "Expanding new repository");
        ZipEntry zipEntry = zis.getNextEntry();
        final Path rootPath = getMainRepository().path.getParent();
        final byte[] buffer = new byte[1024];
        while (zipEntry != null) {
            final File newFile = newFile(rootPath.toFile(), zipEntry);
            if (newFile == null) {
                zipEntry = zis.getNextEntry();
            }
            else if (zipEntry.isDirectory()) {
                Files.createDirectories(newFile.toPath(), (FileAttribute<?>[])new FileAttribute[0]);
                zipEntry = zis.getNextEntry();
            }
            else {
                final FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zipEntry = zis.getNextEntry();
            }
        }
        zis.closeEntry();
        zis.close();
    }
    
    private static File newFile(final File destinationDir, final ZipEntry zipEntry) throws IOException {
        final String name = zipEntry.getName();
        final String[] nameParts = name.split("/");
        if (nameParts.length == 1) {
            return null;
        }
        Path filePath = Paths.get(zipEntry.getName(), new String[0]);
        filePath = filePath.subpath(1, filePath.getNameCount());
        final File destFile = new File(destinationDir, filePath.toString());
        final String destDirPath = destinationDir.getCanonicalPath();
        final String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new RepositoryException("repo.backup.InvalidRepositoryArchive", new Object[] { zipEntry.getName() });
        }
        return destFile;
    }
    
    static {
        RepositoryManager.mainRepo = null;
        log = LogManager.getLogger("galliumdata.core");
    }
}
