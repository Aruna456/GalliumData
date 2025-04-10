// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.log.Markers;
import com.sun.net.httpserver.HttpExchange;
import com.galliumdata.server.settings.SettingsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.Path;
import org.apache.logging.log4j.Logger;
import com.sun.net.httpserver.HttpHandler;

public class WebHandler implements HttpHandler
{
    private static final Logger log;
    public static final String BASE_PATH = "/web/";
    private static Path baseDir;
    private final String urlBase;
    
    public WebHandler(final String base, final String urlBase) {
        WebHandler.baseDir = Paths.get(base, new String[0]);
        if (!Files.exists(WebHandler.baseDir, new LinkOption[0])) {
            throw new SettingsException("settings.WebDirNotFound", new Object[] { base });
        }
        if (!Files.isDirectory(WebHandler.baseDir, new LinkOption[0])) {
            throw new SettingsException("settings.WebDirNotADir", new Object[] { base });
        }
        if (!Files.isReadable(WebHandler.baseDir)) {
            throw new SettingsException("settings.WebDirNotReadable", new Object[] { base });
        }
        this.urlBase = urlBase;
    }
    
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        if (WebHandler.log.isTraceEnabled()) {
            WebHandler.log.trace(Markers.WEB, "Request received for: {}", (Object)exchange.getRequestURI());
        }
        if (!RestManager.checkAddress(exchange)) {
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path.length() == 0 || path.equals("/")) {
            exchange.getResponseHeaders().add("Location", "/web/index.html");
            exchange.sendResponseHeaders(301, 0L);
            final OutputStream out = exchange.getResponseBody();
            out.close();
            return;
        }
        path = path.substring(this.urlBase.length());
        final String[] pathParts = path.split("/");
        if (pathParts.length > 3 && "adapters".equals(pathParts[0])) {
            String realPath = "db";
            for (int i = 1; i < pathParts.length; ++i) {
                realPath = realPath + "/" + pathParts[i];
            }
            if (realPath.indexOf("..") != -1) {
                this.sendErrorMessage(exchange, "Invalid path: " + realPath, 404);
                return;
            }
            byte[] bytes;
            try {
                final InputStream is = this.getClass().getClassLoader().getResourceAsStream(realPath);
                if (is == null) {
                    this.sendErrorMessage(exchange, "No such document: " + path, 404);
                    return;
                }
                final Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name());
                bytes = scanner.useDelimiter("\\A").next().getBytes();
            }
            catch (final Exception ex) {
                bytes = ("Unable to read help file: " + path).getBytes();
            }
            exchange.getResponseHeaders().add("Content-Type", this.getContentType(exchange));
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            final OutputStream out2 = exchange.getResponseBody();
            out2.write(bytes);
            out2.close();
        }
        else {
            Path filePath = WebHandler.baseDir;
            for (String pathPart : pathParts) {
                filePath = filePath.resolve(pathPart);
                if (pathPart.equals("..")) {
                    this.sendErrorMessage(exchange, "No such document: " + path, 404);
                    return;
                }
                if (!Files.exists(filePath, new LinkOption[0])) {
                    this.sendErrorMessage(exchange, "No such document: " + path, 404);
                    return;
                }
            }
            if (!filePath.toAbsolutePath().startsWith(WebHandler.baseDir.toAbsolutePath())) {
                this.sendErrorMessage(exchange, "No such document: " + path, 403);
                return;
            }
            final byte[] bytes2 = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().add("Content-Type", this.getContentType(exchange));
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes2.length);
            final OutputStream out2 = exchange.getResponseBody();
            out2.write(bytes2);
            out2.close();
        }
    }
    
    private void sendErrorMessage(final HttpExchange exchange, final String msg, final int errorCode) {
        try {
            final byte[] errMsgBytes = msg.getBytes("UTF-8");
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(errorCode, errMsgBytes.length);
            final OutputStream out = exchange.getResponseBody();
            out.write(errMsgBytes);
            out.close();
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private String getContentType(final HttpExchange exchange) {
        final String path = exchange.getRequestURI().getPath();
        if (path.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (path.endsWith(".js")) {
            return "text/javascript; charset=UTF-8";
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        return "text/plain; ; charset=UTF-8";
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
        WebHandler.baseDir = null;
    }
}
