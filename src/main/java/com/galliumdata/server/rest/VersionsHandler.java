// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;
import com.galliumdata.server.handler.ProtocolDataValue;
import com.galliumdata.server.handler.ProtocolData;
import com.galliumdata.server.handler.ProtocolDataArray;
import com.galliumdata.server.handler.ProtocolDataObject;
import java.util.Comparator;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.io.File;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import com.galliumdata.server.log.Markers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;
import com.sun.net.httpserver.HttpHandler;

public class VersionsHandler implements HttpHandler
{
    private static final Logger log;
    private static final String BASE_PATH_KEYWORD = "versions";
    public static final String BASE_PATH = "/versions/";
    
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        if (!RestManager.checkAddress(exchange)) {
            return;
        }
        final String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            handleOptions(exchange);
            return;
        }
        if ("DELETE".equals(method)) {
            final URI uri = exchange.getRequestURI();
            final String path = uri.getPath();
            final String[] pathParts = path.split("/");
            if (pathParts.length != 5 || !pathParts[1].equals("versions")) {
                VersionsHandler.log.debug(Markers.REPO, "Request to delete invalid versions: " + String.valueOf(uri));
                sendErrorMessage(exchange, "Request for invalid versions path: " + String.valueOf(uri));
                return;
            }
            this.deleteVersions(exchange, pathParts);
        }
        else {
            if (!"GET".equals(method)) {
                final String errMsg = "Method not supported: " + exchange.getRequestMethod();
                final OutputStream out = exchange.getResponseBody();
                out.write(errMsg.getBytes());
                exchange.sendResponseHeaders(405, errMsg.getBytes().length);
                out.close();
                return;
            }
            if (VersionsHandler.log.isTraceEnabled()) {
                VersionsHandler.log.trace(Markers.REPO, "Retrieving versions for: {}", (Object)exchange.getRequestURI().toString());
            }
            final URI uri = exchange.getRequestURI();
            final String path = uri.getPath();
            final String[] pathParts = path.split("/");
            if (pathParts.length < 2 || !pathParts[1].equals("versions")) {
                VersionsHandler.log.debug(Markers.REPO, "Request for invalid versions: " + String.valueOf(uri));
                sendErrorMessage(exchange, "Request for invalid versions path: " + String.valueOf(uri));
                return;
            }
            if (pathParts.length == 2) {
                sendVersions(exchange);
                return;
            }
            if (pathParts.length != 6) {
                VersionsHandler.log.debug(Markers.REPO, "Request for invalid version path: " + String.valueOf(uri) + ", wrong number of levels");
                sendErrorMessage(exchange, "Request for invalid version path: " + String.valueOf(uri) + ", wrong number of levels");
                return;
            }
            final String sep = System.getProperty("file.separator");
            final String versionPath = pathParts[2] + sep + pathParts[3] + sep + pathParts[4] + sep + pathParts[5];
            final String versionName = pathParts[2] + pathParts[3] + pathParts[4] + "_" + pathParts[5];
            VersionsHandler.log.trace(Markers.REPO, "Retrieving repo version " + versionName);
            final String backupLoc = SettingsManager.getInstance().getStringSetting(SettingName.BACKUP_LOCATION);
            if (backupLoc == null || backupLoc.trim().length() == 0) {
                VersionsHandler.log.trace(Markers.REPO, "Request for version: versions are not enabled");
                sendErrorMessage(exchange, "Backups are not currently enabled on this server");
                return;
            }
            final File topDir = new File(backupLoc);
            if (!topDir.exists() || !topDir.isDirectory() || !topDir.canRead()) {
                VersionsHandler.log.trace(Markers.REPO, "Request for version: backup directory does not exist or is not readable");
                sendErrorMessage(exchange, "Request for version: backup directory does not exist or is not readable");
                return;
            }
            final File backupDir = topDir.toPath().resolve(versionPath).toFile();
            if (!backupDir.exists() || !backupDir.isDirectory() || !backupDir.canRead()) {
                VersionsHandler.log.trace(Markers.REPO, "Request for version: backup does not exist or is not readable");
                sendErrorMessage(exchange, "Request for version: backup does not exist or is not readable");
                return;
            }
            final byte[] repoBytes = this.zipDir(backupDir);
            exchange.getResponseHeaders().add("Content-Type", "application/zip");
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + versionName + ".zip\"");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
            exchange.sendResponseHeaders(200, repoBytes.length);
            final OutputStream out2 = exchange.getResponseBody();
            out2.write(repoBytes);
            out2.close();
            if (VersionsHandler.log.isTraceEnabled()) {
                VersionsHandler.log.trace(Markers.REPO, "Retrieved backup file for: {}, size was: {}", (Object)exchange.getRequestURI(), (Object)repoBytes.length);
            }
        }
    }
    
    public static void sendErrorMessage(final HttpExchange exchange, final String msg) {
        try {
            final byte[] errMsgBytes = msg.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(404, errMsgBytes.length);
            final OutputStream out = exchange.getResponseBody();
            out.write(errMsgBytes);
            out.close();
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void handleOptions(final HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(200, 0L);
        final OutputStream out = exchange.getResponseBody();
        out.close();
    }
    
    private static void sendVersions(final HttpExchange exchange) {
        final String backupLoc = SettingsManager.getInstance().getStringSetting(SettingName.BACKUP_LOCATION);
        if (backupLoc == null || backupLoc.trim().length() == 0) {
            VersionsHandler.log.trace(Markers.REPO, "Request for versions: versions are not enabled");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            try {
                exchange.sendResponseHeaders(204, -1L);
                exchange.close();
            }
            catch (final IOException ioex) {
                VersionsHandler.log.debug(Markers.WEB, "Exception while sending list of backups to REST client: " + ioex.getMessage());
            }
            return;
        }
        final File topDir = new File(backupLoc);
        if (!topDir.exists() || !topDir.isDirectory()) {
            sendErrorMessage(exchange, "Versions not available");
            return;
        }
        final List<String> allVersions = new ArrayList<String>();
        final File[] yearDirs = topDir.listFiles(f -> f.isDirectory() && !".".equals(f.getName()) && !"..".equals(f.getName()));
        if (yearDirs != null) {
            final File[] array = yearDirs;
            for (int length = array.length, i = 0; i < length; ++i) {
                final File yearDir = array[i];
                final File[] monthDirs = yearDir.listFiles(f -> f.isDirectory() && !".".equals(f.getName()) && !"..".equals(f.getName()));
                if (monthDirs != null) {
                    final File[] array2 = monthDirs;
                    for (int length2 = array2.length, j = 0; j < length2; ++j) {
                        final File monthDir = array2[j];
                        final File[] dayDirs = monthDir.listFiles(f -> f.isDirectory() && !".".equals(f.getName()) && !"..".equals(f.getName()));
                        if (dayDirs != null) {
                            final File[] array3 = dayDirs;
                            for (int length3 = array3.length, k = 0; k < length3; ++k) {
                                final File dayDir = array3[k];
                                final File[] repoDirs = dayDir.listFiles(f -> f.isDirectory() && !".".equals(f.getName()) && !"..".equals(f.getName()));
                                if (repoDirs != null) {
                                    final File[] array4 = repoDirs;
                                    for (int length4 = array4.length, l = 0; l < length4; ++l) {
                                        final File repoDir = array4[l];
                                        allVersions.add(yearDir.getName() + "/" + monthDir.getName() + "/" + dayDir.getName() + "/" + repoDir.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        allVersions.sort(Comparator.naturalOrder());
        final ProtocolData topNode = new ProtocolDataObject();
        topNode.putString("status", "OK");
        final ProtocolDataArray versions = new ProtocolDataArray();
        topNode.put("versions", versions);
        for (final String ver : allVersions) {
            versions.add(new ProtocolDataValue(ver));
        }
        final String json = topNode.toJSON();
        try {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, json.getBytes().length);
            exchange.getResponseBody().write(json.getBytes());
            exchange.getResponseBody().close();
            exchange.close();
        }
        catch (final Exception ex) {
            VersionsHandler.log.debug(Markers.WEB, "Error returning versions to client: " + ex.getMessage());
        }
    }
    
    public byte[] zipDir(final File topDir) throws IOException {
        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        final ZipOutputStream zipOut = new ZipOutputStream(fos);
        this.zipFile(topDir, topDir.getName(), zipOut);
        zipOut.close();
        fos.close();
        return fos.toByteArray();
    }
    
    protected void zipFile(final File fileToZip, final String fileName, final ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            }
            else {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            }
            final File[] children = fileToZip.listFiles();
            if (children != null) {
                final File[] array = children;
                for (int length2 = array.length, i = 0; i < length2; ++i) {
                    final File childFile = array[i];
                    this.zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            return;
        }
        final FileInputStream fis = new FileInputStream(fileToZip);
        final ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        final byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
    
    protected void deleteVersions(final HttpExchange exchange, final String[] dayStr) {
        final String backupLoc = SettingsManager.getInstance().getStringSetting(SettingName.BACKUP_LOCATION);
        final String delim = System.getProperty("file.separator");
        final String dayDir = backupLoc + delim + dayStr[2] + delim + dayStr[3] + delim + dayStr[4];
        final File backupDir = new File(dayDir);
        final File topDir = new File(backupLoc);
        if (!backupDir.getParentFile().getParentFile().getParentFile().equals(topDir)) {
            VersionsHandler.log.warn(Markers.WEB, "Someone tried to delete a non-backup directory: " + dayDir);
            return;
        }
        VersionsHandler.log.debug(Markers.WEB, "Deleting backup directory " + dayDir);
        final ProtocolData topNode = new ProtocolDataObject();
        topNode.putString("status", "OK");
        try {
            if (!deleteDirectory(backupDir)) {
                topNode.putString("status", "Unable to delete directory: " + String.valueOf(backupDir));
            }
        }
        catch (final Exception ex) {
            topNode.putString("status", "Error: " + ex.getMessage());
        }
        final String json = topNode.toJSON();
        try {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, json.getBytes().length);
            exchange.getResponseBody().write(json.getBytes());
            exchange.getResponseBody().close();
            exchange.close();
        }
        catch (final Exception ex2) {
            VersionsHandler.log.debug(Markers.WEB, "Error returning delete confirmation to client: " + ex2.getMessage());
        }
    }
    
    private static boolean deleteDirectory(final File file) {
        final File[] files = file.listFiles();
        if (files != null) {
            for (final File f : files) {
                final boolean res = deleteDirectory(f);
                if (!res) {
                    return false;
                }
            }
        }
        return file.delete();
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
