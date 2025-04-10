// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import com.galliumdata.server.handler.ProtocolDataValue;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import java.util.Iterator;
import java.util.Deque;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import org.apache.logging.log4j.core.LogEvent;
import com.galliumdata.server.log.LogStreamAppender;
import com.galliumdata.server.handler.ProtocolDataArray;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.ProtocolData;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.Logger;

public class RestLogHandler
{
    private static final Logger log;
    
    public static ProtocolData getCurrentLogs(final HttpExchange exchange) {
        RestLogHandler.log.trace(Markers.WEB, "Getting REST - logs");
        final ProtocolData topObj = new ProtocolDataObject();
        final ProtocolDataArray eventsArray = new ProtocolDataArray();
        topObj.put("events", eventsArray);
        if (LogStreamAppender.getInstance() == null) {
            return topObj;
        }
        int numEvents = 50;
        final String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            final String[] split;
            final String[] queryParts = split = query.split("&");
            for (final String queryPart : split) {
                final String[] paramParts = queryPart.split("=");
                if (paramParts.length == 2 && paramParts[0].equals("numEvents")) {
                    try {
                        numEvents = Integer.valueOf(paramParts[1]);
                    }
                    catch (final Exception ex) {
                        numEvents = 50;
                    }
                }
            }
        }
        final Deque<LogEvent> events = LogStreamAppender.getInstance().getEventQueue();
        final Iterator<LogEvent> eventsIter = events.descendingIterator();
        int evtCnt = 0;
        while (eventsIter.hasNext()) {
            final LogEvent evt = eventsIter.next();
            final ProtocolDataObject evtObj = new ProtocolDataObject();
            final Instant evtInstant = Instant.ofEpochMilli(evt.getInstant().getEpochMillisecond());
            evtObj.putString("time", DateTimeFormatter.ISO_INSTANT.format(evtInstant));
            evtObj.putString("level", evt.getLevel().toString());
            evtObj.putString("loggerName", evt.getLoggerName());
            evtObj.putString("marker", (evt.getMarker() == null) ? null : evt.getMarker().getName());
            evtObj.putNumber("threadId", evt.getThreadId());
            evtObj.putString("threadName", evt.getThreadName());
            evtObj.putString("message", evt.getMessage().getFormattedMessage());
            eventsArray.add(evtObj);
            if (++evtCnt >= numEvents) {
                break;
            }
        }
        return topObj;
    }
    
    public static ProtocolData getLogSettings(final HttpExchange exchange) {
        final ProtocolData topObj = new ProtocolDataObject();
        final ProtocolDataObject loggersMap = new ProtocolDataObject();
        topObj.put("loggers", loggersMap);
        LogStreamAppender lsa = null;
        final LoggerContext logContext = (LoggerContext)LogManager.getContext(false);
        for (final Map.Entry<String, Appender> entry : logContext.getConfiguration().getAppenders().entrySet()) {
            if (entry.getValue() instanceof LogStreamAppender) {
                lsa = (LogStreamAppender)entry.getValue();
                break;
            }
        }
        int queueSize = 0;
        if (lsa != null) {
            queueSize = lsa.getQueueSize();
        }
        topObj.putNumber("eventQueueSize", queueSize);
        for (final Logger logger : logContext.getLoggers()) {
            final String loggerName = logger.getName();
            final Level level = logger.getLevel();
            final ProtocolDataObject loggerMap = new ProtocolDataObject();
            loggerMap.putString("level", (level == null) ? null : level.name());
            loggersMap.put(loggerName, loggerMap);
        }
        return topObj;
    }
    
    public static ProtocolData postLogSettings(final HttpExchange exchange, final JsonNode payload) {
        RestLogHandler.log.trace(Markers.WEB, "Updating log settings from POST");
        try {
            final LoggerContext logContext = (LoggerContext)LogManager.getContext(false);
            LogStreamAppender lsa = null;
            final Iterator iterator = logContext.getConfiguration().getAppenders().entrySet().iterator();
            Map.Entry<String, Appender> entry = null;
            while (iterator.hasNext()) {
                entry = (Map.Entry<String, Appender>)iterator.next();
                if (entry.getValue() instanceof LogStreamAppender) {
                    lsa = (LogStreamAppender)entry.getValue();
                    break;
                }
            }
            if (lsa != null) {
                final int desiredQueueSize = payload.get("eventQueueSize").asInt();
                if (desiredQueueSize != lsa.getQueueSize()) {
                    RestLogHandler.log.debug("Setting in-memory logging queue to new size {}", (Object)desiredQueueSize);
                    lsa.setQueueSize(desiredQueueSize);
                }
            }
            final JsonNode loggersNode = payload.get("loggers");
            loggersNode.fields().forEachRemaining(eentry -> {
                final String loggerName = eentry.getKey();
                final Logger logger = LogManager.getLogger(loggerName);
                final JsonNode loggerNode = eentry.getValue();
                final String level = loggerNode.get("level").asText();
                RestLogHandler.log.trace(Markers.WEB, "Setting logger {} to level {}", (Object)loggerName, (Object)level);
                logContext.getConfiguration().getLoggerConfig(loggerName).setLevel(Level.getLevel(level));
                return;
            });
            logContext.updateLoggers();
        }
        catch (final Exception ex) {
            return getErrorObject(ex.getMessage());
        }
        return getLogSettings(exchange);
    }
    
    public static ProtocolData clearLogs(final HttpExchange exchange) {
        RestLogHandler.log.trace(Markers.WEB, "Clearing logs");
        final ProtocolData topObj = new ProtocolDataObject();
        topObj.put("numCleared", new ProtocolDataValue(0));
        if (LogStreamAppender.getInstance() == null) {
            return topObj;
        }
        final Deque<LogEvent> events = LogStreamAppender.getInstance().getEventQueue();
        topObj.put("numCleared", new ProtocolDataValue(events.size()));
        events.clear();
        return topObj;
    }
    
    private static ProtocolData getErrorObject(final String errMsg) {
        final ProtocolData topObj = new ProtocolDataObject();
        topObj.putString("errMsg", errMsg);
        return topObj;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.rest");
    }
}
