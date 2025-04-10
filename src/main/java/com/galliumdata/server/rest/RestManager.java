// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Authenticator;
import org.apache.logging.log4j.Marker;
import com.sun.net.httpserver.HttpContext;
import java.io.IOException;
import java.util.Objects;
import org.apache.logging.log4j.util.Supplier;
import javax.net.ssl.SSLParameters;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsConfigurator;
import java.security.SecureRandom;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.util.concurrent.ThreadPoolExecutor;
import com.sun.net.httpserver.HttpsServer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import com.galliumdata.server.settings.SettingsException;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import org.apache.logging.log4j.Logger;
import java.util.regex.Pattern;

public class RestManager
{
    public static final String REST_BASE_PATH = "/rest";
    private static RestAuthenticator authenticator;
    private static Pattern addressPattern;
    private static final Logger log;
    
    public static void startService() {
        final String restPortNumStr = SettingsManager.getInstance().getStringSetting(SettingName.REST_PORT);
        final String restPortSSLNumStr = SettingsManager.getInstance().getStringSetting(SettingName.REST_PORT_SSL);
        if ((restPortNumStr == null || restPortNumStr.trim().length() == 0) && (restPortSSLNumStr == null || restPortSSLNumStr.trim().length() == 0)) {
            RestManager.log.debug(Markers.WEB, "Options rest-port and rest-port-ssl were not specified, so REST/web services will not get started.");
            return;
        }
        int restPort;
        try {
            restPort = Integer.parseInt(restPortNumStr);
        }
        catch (final Exception ex) {
            throw new SettingsException("settings.RestPortInvalid", new Object[] { restPortNumStr });
        }
        int restPortSSL = 0;
        try {
            restPortSSL = Integer.parseInt(restPortSSLNumStr);
        }
        catch (final Exception ex2) {
            RestManager.log.debug(Markers.WEB, "Option rest-port-ssl was not specified or invalid, REST/web services will not be available over SSL");
        }
        RestManager.log.debug(Markers.SYSTEM, "REST service is starting...");
        try {
            if (restPort > 0) {
                RestManager.log.debug(Markers.WEB, "Option rest-port was specified, REST/web services starting on port {}", (Object)restPort);
                HttpServer server;
                try {
                    server = HttpServer.create(new InetSocketAddress(restPort), 0);
                }
                catch (final Exception ex3) {
                    RestManager.log.error(Markers.WEB, "Error while opening REST port " + restPort + ": " + ex3.getMessage());
                    throw ex3;
                }
                addAuth(server.createContext("/rest", new RestHandler()));
                final String webDir = SettingsManager.getInstance().getStringSetting(SettingName.WEB_BASE);
                if (webDir == null || webDir.trim().length() <= 0) {
                    RestManager.log.debug(Markers.WEB, "web-base option was not specified, so web service will not run.");
                    return;
                }
                addAuth(server.createContext("/web/", new WebHandler(webDir, "/web/")));
                addAuth(server.createContext("/", new WebHandler(webDir, "/")));
                addAuth(server.createContext("/sse/", new ServerSentEventHandler()));
                addAuth(server.createContext("/zip/", new RepoHandler()));
                addAuth(server.createContext("/versions/", new VersionsHandler()));
                addAuth(server.createContext("/logfile/", new LogFileHandler()));
                server.setExecutor(Executors.newFixedThreadPool(10));
                server.start();
                RestManager.log.info(Markers.SYSTEM, "RESTManager: HttpServer has started on port " + restPort);
            }
            else {
                RestManager.log.debug(Markers.WEB, "Option rest-port was not specified, so REST/web services will not get started on HTTP.");
            }
            if (restPortSSL > 0) {
                RestManager.log.debug(Markers.WEB, "Option rest-port-ssl was specified, REST/web services starting on SSL port {}", (Object)restPortSSL);
                try {
                    HttpsServer httpsServer;
                    try {
                        httpsServer = HttpsServer.create(new InetSocketAddress(restPortSSL), 0);
                    }
                    catch (final Exception ex3) {
                        RestManager.log.error(Markers.WEB, "Error while opening HTTPS REST port " + restPortSSL + ": " + ex3.getMessage());
                        throw ex3;
                    }
                    final ThreadPoolExecutor tpe = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
                    final SSLContext sslContext = SSLContext.getInstance("TLS");
                    final String restKeystoreLoc = SettingsManager.getInstance().getStringSetting(SettingName.REST_KEYSTORE);
                    if (restKeystoreLoc != null && restKeystoreLoc.trim().length() > 0) {
                        final String restKeystorePw = SettingsManager.getInstance().getStringSetting(SettingName.REST_KEYSTORE_PW);
                        char[] keystorePassword = null;
                        if (restKeystorePw != null && restKeystorePw.length() > 0) {
                            keystorePassword = restKeystorePw.toCharArray();
                        }
                        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                        final KeyStore ks = KeyStore.getInstance("JKS");
                        RestManager.log.debug(Markers.SYSTEM, "REST Manager: Loading keystore file: {}", (Object)restKeystoreLoc);
                        ks.load(new FileInputStream(restKeystoreLoc), keystorePassword);
                        kmf.init(ks, keystorePassword);
                        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                        tmf.init(ks);
                        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
                        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                            @Override
                            public void configure(final HttpsParameters params) {
                                final SSLContext c = this.getSSLContext();
                                final SSLParameters sslparams = c.getDefaultSSLParameters();
                                sslparams.setNeedClientAuth(true);
                                params.setSSLParameters(sslparams);
                            }
                        });
                    }
                    final HttpContext httpContext = httpsServer.createContext("/rest");
                    httpContext.setHandler(new RestHandler());
                    httpsServer.setExecutor(tpe);
                    httpsServer.start();
                    RestManager.log.info(Markers.SYSTEM, "RESTManager: HttpsServer has started on port " + restPortSSL);
                }
                catch (final Exception ex2) {
                    final Logger log = RestManager.log;
                    final Marker web = Markers.WEB;
                    final String s = "Error while starting web/REST services on SSL: {}";
                    final Supplier[] array = { null };
                    final int n = 0;
                    final Exception obj = ex2;
                    Objects.requireNonNull(obj);
                    array[n] = obj::getMessage;
                    log.error(web, s, array);
                    throw new RuntimeException(ex2);
                }
            }
        }
        catch (final IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }
    
    private static void addAuth(final HttpContext context) {
        final String adminPassword = SettingsManager.getInstance().getStringSetting(SettingName.REST_PASSWORD);
        if (adminPassword == null || adminPassword.trim().length() == 0) {
            return;
        }
        if (RestManager.authenticator == null) {
            RestManager.authenticator = new RestAuthenticator("gallium-data");
        }
        context.setAuthenticator(RestManager.authenticator);
    }
    
    protected static boolean checkAddress(final HttpExchange exchange) {
        if (RestManager.addressPattern == null) {
            String addrPattern = SettingsManager.getInstance().getStringSetting(SettingName.REST_ADDR_RANGE);
            if (addrPattern == null) {
                addrPattern = ".*";
            }
            RestManager.addressPattern = Pattern.compile(addrPattern);
        }
        String addrStr = exchange.getRemoteAddress().getAddress().toString();
        if (addrStr.startsWith("/")) {
            addrStr = addrStr.substring(1);
        }
        if (!RestManager.addressPattern.matcher(addrStr).matches()) {
            try {
                RestManager.log.debug(Markers.WEB, "Rejecting request from unauthorized address: " + addrStr);
                final String msg = "Your address is not authorized: " + addrStr;
                exchange.sendResponseHeaders(403, msg.getBytes().length);
                exchange.getResponseBody().write(msg.getBytes());
                exchange.getResponseBody().close();
                exchange.close();
                return false;
            }
            catch (final IOException ex) {
                RestManager.log.debug(Markers.WEB, "Error sending 403 back to unauthorized address");
            }
        }
        return true;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
