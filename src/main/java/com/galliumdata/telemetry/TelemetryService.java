// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.telemetry;

import org.apache.logging.log4j.LogManager;
import java.util.Iterator;
import com.galliumdata.server.adapters.AdapterInterface;
import com.galliumdata.server.log.Markers;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import com.galliumdata.server.Main;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.atomic.AtomicLong;

public class TelemetryService
{
    private static long lastErrorTime;
    private static long lastUptime;
    private static long lastTotalReqBytes;
    private static long lastTotalRespBytes;
    private static long lastTotalReqs;
    private static long lastTotalResps;
    private static Thread uptimeThread;
    private static AtomicLong lastExceptionSentTime;
    private static final Logger log;
    
    public static void sendTelemetryAsync(final String eventCategory, final String eventAction) {
        final Thread thread = new Thread(() -> sendTelemetry(eventCategory, eventAction));
        thread.start();
    }
    
    public static void sendTelemetry(final String eventCategory, final String eventAction) {
        sendTelemetry(eventCategory, eventAction, null, Integer.MIN_VALUE);
    }
    
    public static void sendTelemetry(final String eventCategory, final String eventAction, final String eventLabel) {
        sendTelemetry(eventCategory, eventAction, eventLabel, Integer.MIN_VALUE);
    }
    
    public static void sendTelemetry(String eventCategory, String eventAction, String eventLabel, final int eventValue) {
        final HttpClient client = HttpClient.newHttpClient();
        final URI uri = URI.create("https://www.google-analytics.com/collect");
        String eventValueStr = null;
        try {
            eventCategory = URLEncoder.encode(eventCategory, StandardCharsets.UTF_8);
            eventAction = URLEncoder.encode(eventAction, StandardCharsets.UTF_8);
            if (eventLabel != null && eventLabel.trim().length() > 0) {
                eventLabel = URLEncoder.encode(eventLabel, StandardCharsets.UTF_8);
            }
            if (eventValue != Integer.MIN_VALUE) {
                eventValueStr = "" + eventValue;
            }
        }
        catch (final Exception ex2) {}
        String payload = "v=1&tid=UA-179270324-2&cid=" + Main.uuid + "&t=event&ec=" + eventCategory + "&ea=" + eventAction + "&aip=1&npa=1";
        if (eventLabel != null) {
            payload = payload + "&el=" + eventLabel;
        }
        if (eventValueStr != null) {
            payload = payload + "&ev=" + eventValueStr;
        }
        final HttpRequest request = HttpRequest.newBuilder().uri(uri).POST(HttpRequest.BodyPublishers.ofString(payload)).build();
        try {
            client.send(request, HttpResponse.BodyHandlers.discarding());
        }
        catch (final Exception ex) {
            logButNotTooOften(ex.getMessage());
        }
    }
    
    public static void sendException(String desc) {
        final HttpClient client = HttpClient.newHttpClient();
        final URI uri = URI.create("https://www.google-analytics.com/collect");
        desc = URLEncoder.encode(desc, StandardCharsets.UTF_8);
        final String payload = "v=1&tid=UA-178413709-1&cid=" + Main.uuid + "&t=exception&exd=" + desc + "&aip=1&npa=1";
        final HttpRequest request = HttpRequest.newBuilder().uri(uri).POST(HttpRequest.BodyPublishers.ofString(payload)).build();
        try {
            client.send(request, HttpResponse.BodyHandlers.discarding());
        }
        catch (final Exception ex) {
            logButNotTooOften(ex.getMessage());
        }
    }
    
    public static void sendException(final Exception ex) {
        if (System.currentTimeMillis() - TelemetryService.lastExceptionSentTime.longValue() < 60000L) {
            return;
        }
        TelemetryService.lastExceptionSentTime.set(System.currentTimeMillis());
        final StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            String msg = ex.getMessage();
            if (msg.length() > 150) {
                msg = msg.substring(0, 150);
            }
            sendException(msg);
            return;
        }
        final StackTraceElement elem = stackTrace[0];
        String className = elem.getClassName();
        if (className.length() > 40) {
            className = className.substring(className.length() - 40);
        }
        String msg2 = ex.getMessage();
        if (msg2.length() > 100) {
            msg2 = msg2.substring(0, 100);
        }
        final String totalMsg = msg2 + "-" + className + ":" + elem.getLineNumber();
        sendException(totalMsg);
    }
    
    private static void logButNotTooOften(final String msg) {
        if (System.currentTimeMillis() - TelemetryService.lastErrorTime > 300000L) {
            TelemetryService.lastErrorTime = System.currentTimeMillis();
            TelemetryService.log.debug(Markers.SYSTEM, "Error while sending telemetry: " + msg + ". This log message will not be repeated for 5 minutes.");
        }
    }
    
    public static void startUptimeTelemetry() {
        if (TelemetryService.uptimeThread != null) {
            return;
        }
        (TelemetryService.uptimeThread = new Thread(() -> {
            try {
                while (true) {
                    sendTelemetry("server", "uptime", "Build 2245", (int)((System.currentTimeMillis() - TelemetryService.lastUptime) / 1000L));
                    long totalReqBytes = 0L;
                    long totalRespBytes = 0L;
                    long totalReqs = 0L;
                    long totalResps = 0L;
                    Main.getRunningAdapters().values().iterator();
                    final Iterator iterator = null;
                    while (iterator.hasNext()) {
                        final AdapterInterface adapt = (AdapterInterface) iterator.next();
                        totalReqBytes += adapt.getStatus().numRequestBytes;
                        totalRespBytes += adapt.getStatus().numResponseBytes;
                        totalReqs += adapt.getStatus().numRequests;
                        totalResps += adapt.getStatus().numResponses;
                    }
                    if (totalReqs - TelemetryService.lastTotalReqs > 0L) {
                        sendTelemetry("server", "traffic", "numReqs", (int)(totalReqs - TelemetryService.lastTotalReqs));
                    }
                    if (totalResps - TelemetryService.lastTotalResps > 0L) {
                        sendTelemetry("server", "traffic", "numResps", (int)(totalResps - TelemetryService.lastTotalResps));
                    }
                    if (totalReqBytes - TelemetryService.lastTotalReqBytes > 0L) {
                        sendTelemetry("server", "traffic", "numReqBytes", (int)(totalReqBytes - TelemetryService.lastTotalReqBytes));
                    }
                    if (totalRespBytes - TelemetryService.lastTotalRespBytes > 0L) {
                        sendTelemetry("server", "traffic", "numRespBytes", (int)(totalRespBytes - TelemetryService.lastTotalRespBytes));
                    }
                    TelemetryService.lastTotalReqBytes = totalReqBytes;
                    TelemetryService.lastTotalRespBytes = totalRespBytes;
                    TelemetryService.lastTotalReqs = totalReqs;
                    TelemetryService.lastTotalResps = totalResps;
                    TelemetryService.lastUptime = System.currentTimeMillis();
                    Thread.sleep(3600000L);
                }
            }
            catch (final Exception ex) {
                ex.printStackTrace();
            }
        }, "Uptime telemetry thread")).start();
    }
    
    static {
        TelemetryService.lastErrorTime = 0L;
        TelemetryService.lastUptime = 0L;
        TelemetryService.lastTotalReqBytes = 0L;
        TelemetryService.lastTotalRespBytes = 0L;
        TelemetryService.lastTotalReqs = 0L;
        TelemetryService.lastTotalResps = 0L;
        TelemetryService.lastExceptionSentTime = new AtomicLong(0L);
        log = LogManager.getLogger("galliumdata.core");
    }
}
