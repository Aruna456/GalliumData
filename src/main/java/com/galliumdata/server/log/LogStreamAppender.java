// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.logging.log4j.core.config.Property;
import java.io.Serializable;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import java.util.Deque;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.appender.AbstractAppender;

@Plugin(name = "LogStreamAppender", category = "Core", elementType = "appender")
public class LogStreamAppender extends AbstractAppender
{
    protected Deque<LogEvent> events;
    protected static LogStreamAppender instance;
    private static final int EVENT_QUEUE_SIZE = 1000;
    private static int eventQueueSize;
    private static final Logger log;
    
    protected LogStreamAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions, final Property[] properties) {
        super(name, filter, (Layout)layout, ignoreExceptions, properties);
        this.events = new ConcurrentLinkedDeque<LogEvent>();
    }
    
    @PluginFactory
    public static LogStreamAppender createAppender(@PluginAttribute("name") final String name, @PluginElement("Filter") final Filter filter, @PluginElement("Layout") final Layout layout) {
        if (LogStreamAppender.instance != null) {
            throw new RuntimeException("Did not expect to create more than one instance of LogStreamAppender");
        }
        return LogStreamAppender.instance = new LogStreamAppender(name, filter, (Layout<? extends Serializable>)layout, false, null);
    }
    
    public void append(final LogEvent logEvent) {
        while (this.events.size() >= LogStreamAppender.eventQueueSize) {
            this.events.remove();
        }
        this.events.add(logEvent.toImmutable());
    }
    
    public static LogStreamAppender getInstance() {
        return LogStreamAppender.instance;
    }
    
    public Deque<LogEvent> getEventQueue() {
        return this.events;
    }
    
    public int getQueueSize() {
        return LogStreamAppender.eventQueueSize;
    }
    
    public void setQueueSize(final int newSize) {
        if (newSize < 0 || newSize > 10000) {
            LogStreamAppender.log.debug("We were asked to set the log size to a nonsensical number: {}, no action taken", (Object)newSize);
            return;
        }
        if (newSize < LogStreamAppender.eventQueueSize) {
            while (this.events.size() > newSize) {
                this.events.remove();
            }
        }
        LogStreamAppender.eventQueueSize = newSize;
    }
    
    static {
        LogStreamAppender.eventQueueSize = 1000;
        log = LogManager.getLogger("galliumdata.core");
    }
}
