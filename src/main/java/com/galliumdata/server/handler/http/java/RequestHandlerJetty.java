// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java;

import org.apache.logging.log4j.LogManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import com.galliumdata.server.repository.FilterStage;
import java.net.Socket;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.config.Registry;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.config.Lookup;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.TrustStrategy;
import java.security.KeyStore;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import java.util.Enumeration;
import org.apache.http.client.methods.HttpUriRequest;
import org.eclipse.jetty.server.Response;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import java.net.URL;
import java.io.InputStream;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletException;
import java.io.IOException;
import com.galliumdata.server.log.Markers;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Request;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.Logger;
import org.apache.http.impl.client.CloseableHttpClient;
import com.galliumdata.server.adapters.Variables;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class RequestHandlerJetty extends AbstractHandler
{
    private final ThreadLocal<Variables> threadContexts;
    private final HttpAdapter adapter;
    private CloseableHttpClient httpClient;
    private static final Logger log;
    
    public RequestHandlerJetty(final HttpAdapter adapter) {
        this.threadContexts = new ThreadLocal<Variables>();
        this.adapter = adapter;
        adapter.getStatus().startTime = ZonedDateTime.now();
    }
    
    public void handle(final String url, final Request request, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException, ServletException {
        try {
            this.handleExchange(url, request, httpServletRequest, httpServletResponse);
        }
        catch (final IOException ioex) {
            if (RequestHandlerJetty.log.isDebugEnabled()) {
                RequestHandlerJetty.log.debug(Markers.HTTP, "Connection closed by client: " + request.getRemoteAddr());
            }
            this.adapter.getStatus().incrementNumErrors(1L);
        }
        catch (final Exception ex) {
            if (RequestHandlerJetty.log.isDebugEnabled()) {
                RequestHandlerJetty.log.debug(Markers.HTTP, "Exception while handling request: " + ex.getMessage());
                ex.printStackTrace();
            }
            this.adapter.getStatus().incrementNumErrors(1L);
        }
    }
    
    private void handleExchange(final String requestUrl, final Request request, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException {
        if (RequestHandlerJetty.log.isTraceEnabled()) {
            RequestHandlerJetty.log.trace(Markers.HTTP, "Received " + request.getMethod() + " HTTP request on connection \"" + this.adapter.getConnection().getName() + "\" for URL " + requestUrl);
        }
        this.adapter.getStatus().incrementNumRequests(1L);
        final HttpRequest req = new HttpRequest(request);
        final String connectionMsg = this.connectionIsAccepted(req);
        if (connectionMsg != null) {
            request.getResponse().sendError(400, connectionMsg);
            return;
        }
        final Variables requestContext = new Variables();
        final AdapterCallbackResponse cbResponse = this.callRequestFilters(req, requestContext);
        if (cbResponse.reject) {
            RequestHandlerJetty.log.trace(Markers.HTTP, "Request has been rejected by user logic: {}", cbResponse.errorMessage);
            int errorCode = (int)cbResponse.errorCode;
            if (errorCode == 0) {
                errorCode = 400;
            }
            final Response jettyResponse = request.getResponse();
            if (cbResponse.errorMessage != null) {
                jettyResponse.sendError(errorCode, cbResponse.errorMessage);
            }
            else {
                jettyResponse.sendError(errorCode);
            }
            return;
        }
        if (cbResponse.response != null) {
            final Response resp = request.getResponse();
            byte[] respBytes;
            if (cbResponse.response instanceof String respStr) {
                respBytes = respStr.getBytes(StandardCharsets.UTF_8);
                if (respStr.startsWith("{") || respStr.startsWith("[")) {
                    resp.setHeader("Content-type", "application/json");
                }
                else {
                    resp.setHeader("Content-type", "application/text");
                }
            }
            else {
                if (!(cbResponse.response instanceof byte[])) {
                    throw new ServerException("db.http.logic.ResponseTypeNotSupported", cbResponse.logicName, cbResponse.response.getClass().getName());
                }
                respBytes = (byte[])cbResponse.response;
                resp.setHeader("Content-type", "application/binary");
            }
            resp.getHttpOutput().write(respBytes);
            resp.setStatus(200);
            resp.getHttpOutput().close();
            return;
        }
        this.createHttpClient();
        String url = "http://";
        if (req.getDestinationFullUrl() != null) {
            url = req.getDestinationFullUrl();
        }
        else {
            if (this.adapter.isHttpsWithServer()) {
                url = "https://";
            }
            if (req.getDestinationProtocol() != null) {
                url = req.getDestinationProtocol() + "://";
            }
            if (req.getDestinationHost() != null) {
                url += req.getDestinationHost();
            }
            else {
                url += this.adapter.getServerAddress().getHostName();
            }
            url = url;
            if (req.getDestinationPort() != 0) {
                url += req.getDestinationPort();
            }
            else {
                url += this.adapter.getServerPort();
            }
            if (req.getDestinationUrl() != null) {
                url += req.getDestinationUrl();
            }
            else {
                url += req.getUrl();
            }
        }
        String method = request.getMethod();
        if (req.getDestinationMethod() != null) {
            method = req.getDestinationMethod();
        }
        final String s2 = method;
        HttpUriRequest message = null;
        switch (s2) {
            case "GET": {
                message = new HttpGet(url);
                break;
            }
            case "POST": {
                message = new HttpPost(url);
                break;
            }
            case "PUT": {
                message = new HttpPut(url);
                break;
            }
            case "DELETE": {
                message = new HttpDelete(url);
                break;
            }
            case "OPTIONS": {
                message = new HttpOptions(url);
                break;
            }
            case "PATCH": {
                message = new HttpPatch(url);
                break;
            }
            case "HEAD": {
                message = new HttpHead(url);
                break;
            }
            case "TRACE": {
                message = new HttpTrace(url);
                break;
            }
            default: {
                throw new ServerException("db.http.protocol.UnknownRequestType", request.getMethod());
            }
        }
        final Enumeration<String> headersEnum = request.getHeaderNames();
        while (headersEnum.hasMoreElements()) {
            final String key = headersEnum.nextElement();
            if (req.hasOverrideHeader(key)) {
                final String value = req.getOverrideHeaders().get(key);
                if (value == null) {
                    continue;
                }
                message.addHeader(key, value);
            }
            else if ("Host".equalsIgnoreCase(key)) {
                final String newHost = this.adapter.getServerAddress().getHostName() + ":" + this.adapter.getServerPort();
                message.addHeader("Host", newHost);
            }
            else if ("Referer".equalsIgnoreCase(key)) {
                String newReferer = "http";
                if (this.adapter.isHttpsWithServer()) {
                    newReferer = "https";
                }
                newReferer += "://";
                newReferer += this.adapter.getServerAddress().getHostName();
                newReferer = newReferer;
                newReferer += this.adapter.getServerPort();
                message.addHeader("Referer", newReferer);
            }
            else if ("Origin".equalsIgnoreCase(key)) {
                String newReferer = "http";
                if (this.adapter.isHttpsWithServer()) {
                    newReferer = "https";
                }
                newReferer += "://";
                newReferer += request.getServerName();
                newReferer = newReferer;
                newReferer += this.adapter.getLocalPort();
                message.addHeader("Origin", newReferer);
            }
            else if ("Content-length".equalsIgnoreCase(key)) {
                final String contentLengthStr = request.getHeader("Content-length");
                if (contentLengthStr == null) {
                    continue;
                }
                try {
                    final long len = Long.parseLong(contentLengthStr);
                    if (len <= 0L) {
                        continue;
                    }
                    this.adapter.getStatus().incrementNumRequestBytes(len);
                }
                catch (final Exception ex4) {}
            }
            else if ("Content-encoding".equalsIgnoreCase(key)) {
                final String encStr = request.getHeader("Content-encoding");
                message.addHeader(key, encStr);
                final String s = req.getPayloadString();
                System.out.println("Content: " + s);
            }
            else {
                if (message.containsHeader(key)) {
                    message.removeHeaders(key);
                }
                message.addHeader(key, request.getHeader(key));
            }
        }
        if (message instanceof HttpEntityEnclosingRequestBase) {
            if (req.bodyHasBeenRead()) {
                ((HttpEntityEnclosingRequestBase)message).setEntity(new ByteArrayEntity(req.getPayload()));
            }
            else {
                ((HttpEntityEnclosingRequestBase)message).setEntity(new InputStreamEntity(request.getInputStream()));
            }
        }
        CloseableHttpResponse resp2;
        try {
            resp2 = this.httpClient.execute(message);
        }
        catch (final Exception ex) {
            final Response jettyResponse2 = request.getResponse();
            String msg = ex.getMessage();
            if (msg == null && ex.getCause() != null) {
                msg = ex.getCause().getMessage();
            }
            if (msg == null) {
                msg = ex.toString();
            }
            jettyResponse2.sendError(500, "Error while forwarding request to server: " + msg);
            throw new ServerException("db.http.server.ErrorReadingResponse", ex);
        }
        this.adapter.getStatus().incrementNumResponses(1L);
        final Response jettyResponse3 = request.getResponse();
        final HttpResponse response = new HttpResponse(req, resp2);
        final AdapterCallbackResponse respResponse = this.callResponseFilters(req, response, requestContext);
        if (respResponse.reject) {
            RequestHandlerJetty.log.trace(Markers.HTTP, "Response has been rejected by user logic: {}", respResponse.errorMessage);
            int errorCode2 = (int)respResponse.errorCode;
            if (errorCode2 == 0) {
                errorCode2 = 400;
            }
            if (respResponse.errorMessage != null) {
                jettyResponse3.sendError(errorCode2, respResponse.errorMessage);
            }
            else {
                jettyResponse3.sendError(errorCode2);
            }
            return;
        }
        if (response.getResponseCode() == 303) {
            final Header locHdr = resp2.getFirstHeader("Location");
            if (locHdr == null) {
                throw new ServerException("db.http.logic.RedirectHasNoLocation");
            }
            final String redirectUrlStr = locHdr.getValue();
            URL redirectUrl;
            try {
                redirectUrl = new URL(redirectUrlStr);
            }
            catch (final Exception ex2) {
                throw new ServerException("db.http.server.ErrorReadingResponse", "Location in 303 response was invalid URL: " + redirectUrlStr + " : " + ex2.getMessage());
            }
            String finalRedirect = this.adapter.isHttpsWithClients() ? "https" : "http";
            finalRedirect = finalRedirect + "://" + this.adapter.getLocalHostName() + ":" + this.adapter.getLocalPort();
            finalRedirect += redirectUrl.getPath();
            jettyResponse3.sendRedirect(303, finalRedirect, true);
            resp2.close();
            if (RequestHandlerJetty.log.isTraceEnabled()) {
                RequestHandlerJetty.log.trace(Markers.HTTP, "Forwarding redirect to client: " + locHdr.getValue());
            }
        }
        else {
            if (response.getResponseCode() >= 400) {
                jettyResponse3.sendError(response.getResponseCode(), response.getResponseMessage());
                resp2.close();
                return;
            }
            final Header[] allHeaders = resp2.getAllHeaders();
            for (int length = allHeaders.length, i = 0; i < length; ++i) {
                final Header header = allHeaders[i];
                final String name = header.getName();
                if (jettyResponse3.containsHeader(name)) {
                    jettyResponse3.setHeader(name, null);
                }
                if ("Access-Control-Allow-Origin".equalsIgnoreCase(name)) {
                    final String value2 = header.getValue();
                    if (!"*".equals(value2)) {
                        jettyResponse3.addHeader(name, request.getServerName());
                    }
                    else {
                        jettyResponse3.addHeader(name, header.getValue());
                    }
                }
                else if ("Content-Length".equalsIgnoreCase(name)) {
                    jettyResponse3.addHeader("Content-Length", "" + response.getPayload().length);
                }
                else {
                    jettyResponse3.addHeader(name, header.getValue());
                }
            }
            Label_2438: {
                if (resp2.getEntity() != null) {
                    if (response.isModified()) {
                        try {
                            final byte[] respBytes2 = response.getPayload();
                            jettyResponse3.setStatusWithReason(resp2.getStatusLine().getStatusCode(), resp2.getStatusLine().getReasonPhrase());
                            jettyResponse3.setContentLength(respBytes2.length);
                            jettyResponse3.getHttpOutput().write(respBytes2);
                            jettyResponse3.getHttpOutput().close();
                            this.adapter.getStatus().incrementNumResponseBytes(respBytes2.length);
                            break Label_2438;
                        }
                        catch (final IOException ioex) {
                            throw ioex;
                        }
                        catch (final Exception ex3) {
                            throw new ServerException("db.http.server.ErrorWritingResponse", ex3);
                        }
                    }
                    try {
                        long responseLength = resp2.getEntity().getContentLength();
                        if (response.getPayloadNoRead() != null) {
                            responseLength = response.getPayloadNoRead().length;
                        }
                        jettyResponse3.setStatusWithReason(resp2.getStatusLine().getStatusCode(), resp2.getStatusLine().getReasonPhrase());
                        jettyResponse3.setContentLength((int)responseLength);
                        if (responseLength != 0L) {
                            if (response.getPayloadNoRead() != null) {
                                jettyResponse3.getHttpOutput().write(response.getPayloadNoRead());
                            }
                            else {
                                jettyResponse3.getHttpOutput().write(response.getPayload());
                            }
                            jettyResponse3.getHttpOutput().close();
                        }
                    }
                    catch (final Exception ex3) {
                        throw new ServerException("db.http.server.ErrorWritingResponse", ex3);
                    }
                    this.adapter.getStatus().incrementNumResponseBytes(response.getPayloadSize());
                }
            }
            resp2.close();
            if (RequestHandlerJetty.log.isTraceEnabled()) {
                RequestHandlerJetty.log.trace(Markers.HTTP, "Finished with " + request.getMethod() + " HTTP request on connection " + this.adapter.getConnection().getName() + " for URL " + request.getRequestURI());
            }
        }
    }
    
    private void createHttpClient() {
        if (this.httpClient != null) {
            return;
        }
        if (this.adapter.isHttpsWithServer()) {
            if (this.httpClient == null) {
                final org.apache.http.conn.ssl.TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
                try {
                    final SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
                    HostnameVerifier hostnameVerifier;
                    if (this.adapter.isTrustServerCertificate()) {
                        hostnameVerifier = NoopHostnameVerifier.INSTANCE;
                    }
                    else {
                        hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();
                    }
                    final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
                    final Registry<Object> socketFactoryRegistry = RegistryBuilder.create().register("https", (Object) sslsf).build();
                    final BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager();
                    this.httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).setConnectionManager(connectionManager).build();
                }
                catch (final Exception ex) {
                    throw new ServerException("db.http.server.ErrorStartingSSL", "connection to server: " + this.adapter.getConnection().getName(), ex);
                }
            }
        }
        else if (this.httpClient == null) {
            this.httpClient = HttpClients.custom().disableAuthCaching().disableDefaultUserAgent().disableRedirectHandling().build();
        }
    }
    
    private String connectionIsAccepted(final HttpRequest req) {
        final Variables context = new Variables();
        context.put("request", req);
        final Variables connectionContext = new Variables();
        final String clientIP = req.getClientAddress().getHostAddress();
        connectionContext.put("clientIP", clientIP);
        connectionContext.put("clientPort", req.getClientPort());
        context.put("connectionContext", connectionContext);
        final AdapterCallbackResponse callbackResponse = this.adapter.getCallback().connectionRequested(null, context);
        if (callbackResponse.reject) {
            String msg = callbackResponse.errorMessage;
            if (msg == null) {
                msg = "Connection rejected";
            }
            return msg;
        }
        return null;
    }
    
    private AdapterCallbackResponse callRequestFilters(final HttpRequest req, final Variables requestContext) {
        if (!this.adapter.getCallback().hasFiltersForPacketType(FilterStage.REQUEST, req.getMethod())) {
            return new AdapterCallbackResponse();
        }
        requestContext.put("request", req);
        requestContext.put("threadContext", this.getThreadContext());
        requestContext.put("projectContext", this.adapter.getProject().getProjectContext());
        return this.adapter.getCallback().invokeRequestFilters(req.getMethod(), requestContext);
    }
    
    private AdapterCallbackResponse callResponseFilters(final HttpRequest req, final HttpResponse resp, final Variables requestContext) {
        if (!this.adapter.getCallback().hasFiltersForPacketType(FilterStage.RESPONSE, req.getMethod())) {
            return new AdapterCallbackResponse();
        }
        requestContext.put("request", req);
        requestContext.put("response", resp);
        requestContext.put("threadContext", this.getThreadContext());
        requestContext.put("projectContext", this.adapter.getProject().getProjectContext());
        return this.adapter.getCallback().invokeResponseFilters(req.getMethod(), requestContext);
    }
    
    private Variables getThreadContext() {
        Variables vars = this.threadContexts.get();
        if (vars == null) {
            vars = new Variables();
            this.threadContexts.set(vars);
        }
        return vars;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
