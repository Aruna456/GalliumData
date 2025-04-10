// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.telemetry;

import org.apache.logging.log4j.LogManager;
import org.apache.http.impl.client.HttpClients;
import com.galliumdata.server.adapters.AdapterStatus;
import com.galliumdata.server.adapters.AdapterInterface;
import java.util.HashSet;
import java.util.Iterator;
import com.galliumdata.server.handler.ProtocolDataArray;
import java.util.Set;
import com.galliumdata.server.handler.ProtocolData;
import org.apache.http.client.methods.CloseableHttpResponse;
import java.io.IOException;
import org.apache.http.util.EntityUtils;
import com.galliumdata.server.log.Markers;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.Main;
import org.apache.logging.log4j.Logger;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;

public class TelemetryNewRelicService
{
    private static long lastErrorTime;
    private static long lastUptime;
    private static long lastTotalReqBytes;
    private static long lastTotalRespBytes;
    private static long lastTotalReqs;
    private static long lastTotalResps;
    private static Thread uptimeThread;
    private static final CloseableHttpClient httpclient;
    private static final HttpPost eventsPost;
    private static final HttpPost metricsPost;
    private static final long NO_REPEAT_NORMAL_INTERVAL = 900000L;
    private static final long NO_REPEAT_MAX_INTERVAL = 86400000L;
    private static long noRepeatInterval;
    private static final Logger log;
    
    public static void sendEventAsync(final String eventType, final String attName, final String attValue) {
        new Thread(() -> sendEvent(eventType, attName, attValue), "Telemetry - send event").start();
    }
    
    public static void sendEvent(final String eventType, final String attName, final String attValue) {
        sendEvent(eventType, attName, attValue, null, null);
    }
    
    public static void sendEvent(final String eventType, final String attName1, final String attValue1, final String attName2, final String attValue2) {
        if ("DevLabIJ".equals(Main.uuid)) {
            return;
        }
        final ProtocolDataObject topObj = new ProtocolDataObject();
        topObj.putString("eventType", eventType);
        topObj.putString("clientId", (Main.uuid.length() == 0) ? "TEST" : Main.uuid);
        if (attName1 != null) {
            topObj.putString(attName1, attValue1);
        }
        if (attName2 != null) {
            topObj.putString(attName2, attValue2);
        }
        CloseableHttpResponse response = null;
        try {
            TelemetryNewRelicService.eventsPost.setEntity((HttpEntity)new StringEntity(topObj.toJSON()));
            response = TelemetryNewRelicService.httpclient.execute((HttpUriRequest)TelemetryNewRelicService.eventsPost);
            final int respCode = response.getStatusLine().getStatusCode();
            if (respCode > 299) {
                TelemetryNewRelicService.log.debug(Markers.SYSTEM, "Error sending event, response code was: " + respCode + ", reason: " + response.getStatusLine().getReasonPhrase());
            }
            else {
                final HttpEntity entity2 = response.getEntity();
                EntityUtils.consume(entity2);
            }
            TelemetryNewRelicService.noRepeatInterval = 900000L;
        }
        catch (final Exception ex) {
            logButNotTooOften("Exception sending event: " + ex.getMessage());
        }
        finally {
            try {
                if (response != null) {
                    response.close();
                }
            }
            catch (final IOException ex2) {}
        }
    }
    
    public static ProtocolDataObject createMetricEntry(final String metricName, final int value, final String unit, final long interval) {
        final ProtocolDataObject metricsObj = new ProtocolDataObject();
        metricsObj.putString("name", metricName);
        metricsObj.putString("type", "count");
        metricsObj.putNumber("value", value);
        metricsObj.putNumber("timestamp", System.currentTimeMillis());
        metricsObj.putNumber("interval.ms", interval);
        final ProtocolData attributesObj = new ProtocolDataObject();
        metricsObj.put("attributes", attributesObj);
        attributesObj.putString("entity.guid", (Main.uuid.length() == 0) ? "TEST" : Main.uuid);
        attributesObj.putString("Metric Unit", unit);
        attributesObj.putString("App Name", "Gallium Data server");
        attributesObj.putString("Build Number", "2245");
        return metricsObj;
    }
    
    public static void sendMetricsAsync(final Set<ProtocolDataObject> entries) {
        new Thread(() -> sendMetrics(entries), "Telemetry - send metrics").start();
    }
    
    public static void sendMetrics(final Set<ProtocolDataObject> entries) {
        if ("DevLabIJ".equals(Main.uuid)) {
            return;
        }
        final ProtocolDataArray topArray = new ProtocolDataArray();
        final ProtocolData topObj = new ProtocolDataObject();
        topArray.add(topObj);
        final ProtocolDataArray metricsArray = new ProtocolDataArray();
        topObj.put("metrics", metricsArray);
        for (final ProtocolDataObject entry : entries) {
            metricsArray.add(entry);
        }
        CloseableHttpResponse response = null;
        try {
            TelemetryNewRelicService.metricsPost.setEntity((HttpEntity)new StringEntity(topArray.toJSON()));
            response = TelemetryNewRelicService.httpclient.execute((HttpUriRequest)TelemetryNewRelicService.metricsPost);
            final int respCode = response.getStatusLine().getStatusCode();
            if (respCode > 299) {
                TelemetryNewRelicService.log.debug(Markers.SYSTEM, "Error sending telemetry, response code was: " + respCode);
            }
            else {
                final HttpEntity entity2 = response.getEntity();
                EntityUtils.consume(entity2);
            }
            TelemetryNewRelicService.noRepeatInterval = 900000L;
        }
        catch (final Exception ex) {
            logButNotTooOften("Exception sending metrics: " + ex.getMessage());
        }
        finally {
            try {
                if (response != null) {
                    response.close();
                }
            }
            catch (final IOException ex2) {}
        }
    }
    
    private static void logButNotTooOften(final String msg) {
        if (System.currentTimeMillis() - TelemetryNewRelicService.lastErrorTime > TelemetryNewRelicService.noRepeatInterval) {
            if (TelemetryNewRelicService.noRepeatInterval < 86400000L) {
                TelemetryNewRelicService.noRepeatInterval *= 2L;
            }
            TelemetryNewRelicService.lastErrorTime = System.currentTimeMillis();
            TelemetryNewRelicService.log.debug(Markers.SYSTEM, "Error while sending telemetry: " + msg + ". This log message will not be repeated for " + (int)(TelemetryNewRelicService.noRepeatInterval / 60000L) + " minutes.");
        }
    }
    
    public static void startUptimeTelemetry() {
        if (TelemetryNewRelicService.uptimeThread != null) {
            return;
        }
        (TelemetryNewRelicService.uptimeThread = new Thread(() -> {
            try {
                while (true) {
                    if (TelemetryNewRelicService.lastUptime > 0L) {
                        final long sinceLast = System.currentTimeMillis() - TelemetryNewRelicService.lastUptime;
                        final HashSet<ProtocolDataObject> entries = new HashSet<ProtocolDataObject>();
                        final ProtocolDataObject uptime = createMetricEntry("uptime", (int)sinceLast / 1000, "seconds", sinceLast);
                        entries.add(uptime);
                        long totalReqBytes = 0L;
                        long totalRespBytes = 0L;
                        long totalReqs = 0L;
                        long totalResps = 0L;
                        Main.getRunningAdapters().values().iterator();
                        final Iterator iterator = null;
                        while (iterator.hasNext()) {
                            final AdapterInterface adapt = (AdapterInterface) iterator.next();
                            final AdapterStatus status = adapt.getStatus();
                            totalReqBytes += status.numRequestBytes;
                            totalRespBytes += status.numResponseBytes;
                            totalReqs += status.numRequests;
                            totalResps += status.numResponses;
                        }
                        if (totalReqs - TelemetryNewRelicService.lastTotalReqs > 0L) {
                            entries.add(createMetricEntry("traffic.reqs", (int)(totalReqs - TelemetryNewRelicService.lastTotalReqs), "requests", sinceLast));
                        }
                        if (totalResps - TelemetryNewRelicService.lastTotalResps > 0L) {
                            entries.add(createMetricEntry("traffic.resps", (int)(totalResps - TelemetryNewRelicService.lastTotalResps), "requests", sinceLast));
                        }
                        if (totalReqBytes - TelemetryNewRelicService.lastTotalReqBytes > 0L) {
                            entries.add(createMetricEntry("traffic.reqBytes", (int)(totalReqBytes - TelemetryNewRelicService.lastTotalReqBytes), "bytes", sinceLast));
                        }
                        if (totalRespBytes - TelemetryNewRelicService.lastTotalRespBytes > 0L) {
                            entries.add(createMetricEntry("traffic.respBytes", (int)(totalRespBytes - TelemetryNewRelicService.lastTotalRespBytes), "bytes", sinceLast));
                        }
                        sendMetricsAsync(entries);
                        TelemetryNewRelicService.lastTotalReqBytes = totalReqBytes;
                        TelemetryNewRelicService.lastTotalRespBytes = totalRespBytes;
                        TelemetryNewRelicService.lastTotalReqs = totalReqs;
                        TelemetryNewRelicService.lastTotalResps = totalResps;
                    }
                    TelemetryNewRelicService.lastUptime = System.currentTimeMillis();
                    Thread.sleep(600000L);
                }
            }
            catch (final Exception ex) {
                if (TelemetryNewRelicService.log.isDebugEnabled()) {
                    ex.printStackTrace();
                }
            }
        }, "Uptime telemetry thread")).start();
    }
    
    static {
        TelemetryNewRelicService.lastErrorTime = 0L;
        TelemetryNewRelicService.lastUptime = 0L;
        TelemetryNewRelicService.lastTotalReqBytes = 0L;
        TelemetryNewRelicService.lastTotalRespBytes = 0L;
        TelemetryNewRelicService.lastTotalReqs = 0L;
        TelemetryNewRelicService.lastTotalResps = 0L;
        httpclient = HttpClients.createDefault();
        TelemetryNewRelicService.noRepeatInterval = 900000L;
        log = LogManager.getLogger("galliumdata.core");
        (eventsPost = new HttpPost("https://insights-collector.newrelic.com/v1/accounts/2909879/events")).addHeader("Content-Type", "application/json");
        TelemetryNewRelicService.eventsPost.addHeader("X-Insert-Key", "NRII-xKnX06EvC1ySpeckmvf74qjxQCSIrsIN");
        (metricsPost = new HttpPost("https://metric-api.newrelic.com/metric/v1")).addHeader("Content-Type", "application/json");
        TelemetryNewRelicService.metricsPost.addHeader("Api-Key", "NRII-xKnX06EvC1ySpeckmvf74qjxQCSIrsIN");
    }
}
