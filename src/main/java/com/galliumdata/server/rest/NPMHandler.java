// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.net.URI;
import com.galliumdata.server.npm.NPMPackage;
import com.galliumdata.server.npm.NPMManager;
import com.galliumdata.server.handler.ProtocolDataArray;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.ProtocolData;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;

public class NPMHandler
{
    private static final String BASE_PATH_KEYWORD = "npms";
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
        if (pathParts.length < 4 || pathParts.length > 5 || !pathParts[2].equals("npms")) {
            NPMHandler.log.debug(Markers.REPO, "Request for npms is invalid: " + String.valueOf(uri));
            VersionsHandler.sendErrorMessage(exchange, "Request for invalid npms: " + String.valueOf(uri));
            return null;
        }
        final ProtocolData topObj = new ProtocolDataObject();
        final ProtocolDataArray artifactsArray = new ProtocolDataArray();
        topObj.put("artifacts", artifactsArray);
        if (pathParts.length == 4) {
            final List<NPMPackage> packages = NPMManager.searchForModule(pathParts[3]);
            for (final NPMPackage pack : packages) {
                final ProtocolData packObj = new ProtocolDataObject();
                packObj.putString("artifactId", pack.getName());
                packObj.putString("description", pack.getDescription());
                packObj.putString("homepage", pack.getHomePage());
                packObj.putString("url", pack.getNpmUrl());
                artifactsArray.add(packObj);
            }
        }
        else if (pathParts.length == 5) {
            final List<NPMPackage> packages = NPMManager.getVersions(pathParts[3]);
            for (final NPMPackage pack : packages) {
                final ProtocolData packObj = new ProtocolDataObject();
                packObj.putString("artifactId", pack.getName());
                packObj.putString("version", pack.getVersion());
                artifactsArray.add(packObj);
            }
        }
        return topObj;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
