// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.io.IOException;
import java.net.URI;
import com.galliumdata.server.maven.IvyRepository;
import com.galliumdata.server.handler.ProtocolDataArray;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.ProtocolData;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;

public class LibrariesHandler
{
    private static final String BASE_PATH_KEYWORD = "libraries";
    private static final Logger log;
    
    public static ProtocolData handle(final HttpExchange exchange) throws IOException {
        if (!RestManager.checkAddress(exchange)) {
            return null;
        }
        final String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            VersionsHandler.handleOptions(exchange);
            return null;
        }
        final URI uri = exchange.getRequestURI();
        final String path = uri.getPath();
        final String[] pathParts = path.split("/");
        if (pathParts.length < 4 || pathParts.length > 5 || !pathParts[2].equals("libraries")) {
            LibrariesHandler.log.debug(Markers.REPO, "Request for libraries is invalid: " + String.valueOf(uri));
            VersionsHandler.sendErrorMessage(exchange, "Request for invalid libraries: " + String.valueOf(uri));
            return null;
        }
        final ProtocolData topObj = new ProtocolDataObject();
        final ProtocolDataArray artifactsArray = new ProtocolDataArray();
        topObj.put("artifacts", artifactsArray);
        final IvyRepository repo = IvyRepository.getInstance();
        final String orgId = pathParts[3];
        if (pathParts.length == 4) {
            final String[] artifacts;
            final String[] entries = artifacts = repo.findArtifacts(orgId);
            for (final String modEntry : artifacts) {
                final ProtocolDataObject module = new ProtocolDataObject();
                module.putString("orgId", orgId);
                module.putString("artifactId", modEntry);
                artifactsArray.add(module);
            }
        }
        else {
            final String artifactId = pathParts[4];
            String[] versions;
            try {
                versions = repo.findVersions(orgId, artifactId);
            }
            catch (final Exception ex) {
                ex.printStackTrace();
                VersionsHandler.sendErrorMessage(exchange, "Error fetching libraries: " + String.valueOf(ex));
                return null;
            }
            for (final String version : versions) {
                final ProtocolDataObject module2 = new ProtocolDataObject();
                module2.putString("orgId", orgId);
                module2.putString("artifactId", artifactId);
                module2.putString("version", version);
                artifactsArray.add(module2);
            }
        }
        LibrariesHandler.log.trace(Markers.WEB, "Getting REST - libraries");
        return topObj;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
