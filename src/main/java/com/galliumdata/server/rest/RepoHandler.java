// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import com.galliumdata.server.handler.ProtocolDataObject;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import com.galliumdata.server.repository.Repository;
import java.net.URI;
import java.io.OutputStream;
import com.galliumdata.server.Zippable;
import com.galliumdata.server.metarepo.MetaRepositoryManager;
import com.galliumdata.server.repository.RepositoryManager;
import com.galliumdata.server.log.Markers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;
import com.sun.net.httpserver.HttpHandler;

public class RepoHandler implements HttpHandler
{
    private static final Logger log;
    private static final String BASE_PATH_KEYWORD = "zip";
    public static final String BASE_PATH = "/zip/";
    
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        if (!RestManager.checkAddress(exchange)) {
            return;
        }
        if (RepoHandler.log.isTraceEnabled()) {
            RepoHandler.log.trace(Markers.REPO, "Repository request received: " + exchange.getRequestMethod() + " for " + String.valueOf(exchange.getRequestURI()));
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            this.handlePost(exchange);
            exchange.close();
            return;
        }
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            final String errMsg = "Method not supported: " + exchange.getRequestMethod();
            final OutputStream out = exchange.getResponseBody();
            out.write(errMsg.getBytes());
            exchange.sendResponseHeaders(405, errMsg.getBytes().length);
            out.close();
            return;
        }
        if (RepoHandler.log.isTraceEnabled()) {
            RepoHandler.log.trace(Markers.REPO, "Retrieving zip file for: {}", (Object)exchange.getRequestURI().toString());
        }
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 2 || !pathParts[1].equals("zip")) {
            RepoHandler.log.debug(Markers.REPO, "Request for invalid zip path: " + String.valueOf(uri));
            this.sendNotFoundMessage(exchange, "Request for invalid zip path: " + String.valueOf(uri));
            return;
        }
        if (pathParts.length < 3) {
            RepoHandler.log.debug(Markers.REPO, "Request for invalid zip path: " + String.valueOf(uri) + ", need more than one level");
            this.sendNotFoundMessage(exchange, "Request for invalid zip path: " + String.valueOf(uri) + ", need more than one level");
            return;
        }
        if (pathParts.length > 4) {
            RepoHandler.log.debug(Markers.REPO, "Request for invalid zip path: " + String.valueOf(uri) + ", too many levels");
            this.sendNotFoundMessage(exchange, "Request for invalid zip path: " + String.valueOf(uri) + ", too many levels");
            return;
        }
        final String downloadType = pathParts[2];
        final Repository repo = RepositoryManager.getMainRepository();
        Zippable objectToZip;
        if (downloadType.equals("repository")) {
            objectToZip = repo;
        }
        else if (downloadType.equals("metarepo")) {
            objectToZip = MetaRepositoryManager.getMainRepository();
        }
        else {
            if (!downloadType.equals("project")) {
                RepoHandler.log.debug(Markers.REPO, "Request for invalid zip path: " + String.valueOf(uri));
                this.sendNotFoundMessage(exchange, "Request for invalid zip path: " + String.valueOf(uri));
                return;
            }
            if (pathParts.length != 4) {
                RepoHandler.log.debug(Markers.REPO, "Request for invalid zip path: " + String.valueOf(uri) + " for project");
                this.sendNotFoundMessage(exchange, "Request for invalid zip path: " + String.valueOf(uri) + " for project");
                return;
            }
            final String projName = pathParts[3];
            objectToZip = repo.getProjects().get(projName);
            if (objectToZip == null) {
                this.sendNotFoundMessage(exchange, "No such project: " + projName);
                return;
            }
        }
        final byte[] repoBytes = objectToZip.zip();
        exchange.getResponseHeaders().add("Content-Type", "application/zip");
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + objectToZip.getName() + ".zip\"");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(200, repoBytes.length);
        final OutputStream out2 = exchange.getResponseBody();
        out2.write(repoBytes);
        out2.close();
        if (RepoHandler.log.isTraceEnabled()) {
            RepoHandler.log.trace(Markers.REPO, "Retrieved zip file for: {}, size was: {}", (Object)exchange.getRequestURI(), (Object)repoBytes.length);
        }
    }
    
    private void sendNotFoundMessage(final HttpExchange exchange, final String msg) {
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
    
    private void handlePost(final HttpExchange exchange) throws IOException {
        RepoHandler.log.debug(Markers.REPO, "Installing new repository");
        final ProtocolDataObject result = new ProtocolDataObject();
        try {
            final InputStream instr = exchange.getRequestBody();
            final ZipInputStream zis = new ZipInputStream(instr);
            RepositoryManager.installNewRepository(zis);
            zis.close();
        }
        catch (final Exception ex) {
            RepoHandler.log.error(Markers.REPO, "Error while installing new repository: {}", (Object)ex.getMessage());
            exchange.getRequestBody().close();
            result.putString("status", "error");
            result.putString("error", ex.getMessage());
            final String msg = result.toJSON();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
            exchange.sendResponseHeaders(400, msg.getBytes().length);
            final OutputStream out = exchange.getResponseBody();
            out.write(msg.getBytes());
            out.close();
            return;
        }
        result.putString("status", "success");
        final String msg2 = result.toJSON();
        exchange.getRequestBody().close();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
        exchange.sendResponseHeaders(200, msg2.getBytes().length);
        final OutputStream out2 = exchange.getResponseBody();
        out2.write(msg2.getBytes());
        out2.close();
        RepoHandler.log.info(Markers.REPO, "New repository was installed successfully");
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
