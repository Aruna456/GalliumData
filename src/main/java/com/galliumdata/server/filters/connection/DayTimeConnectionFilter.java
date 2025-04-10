// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.connection;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Source;
import java.util.Iterator;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import java.time.temporal.TemporalField;
import java.time.temporal.ChronoField;
import com.galliumdata.server.log.Markers;
import java.time.LocalDateTime;
import java.net.InetSocketAddress;
import com.galliumdata.server.logic.FilterResult;
import java.net.Socket;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.logic.LogicException;
import java.util.HashSet;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import com.galliumdata.server.repository.FilterUse;
import com.galliumdata.server.logic.ConnectionFilter;

public class DayTimeConnectionFilter implements ConnectionFilter
{
    private FilterUse filterUse;
    private Set<String> daysAllowed;
    private Set<HourSpec> hoursAllowed;
    private String deniedMessage;
    private boolean executeCode;
    private static final Logger log;
    
    public DayTimeConnectionFilter() {
        this.daysAllowed = new HashSet<String>();
        this.hoursAllowed = new HashSet<HourSpec>();
        this.executeCode = false;
    }
    
    @Override
    public void configure(final FilterUse def) {
        this.filterUse = def;
        this.daysAllowed = new HashSet<String>();
        this.hoursAllowed = new HashSet<HourSpec>();
        this.deniedMessage = null;
        this.executeCode = false;
        final Variables filterContext = def.getFilterContext();
        if (filterContext.containsKey("_initialized")) {
            this.daysAllowed = (Set)filterContext.get("daysAllowed");
            this.hoursAllowed = (Set)filterContext.get("hoursAllowed");
            this.deniedMessage = (String)filterContext.get("deniedMessage");
            this.executeCode = (boolean)filterContext.get("executeCode");
            return;
        }
        final String days = (String) this.filterUse.getParameters().get("Days");
        if (days != null && days.trim().length() > 0) {
            final String[] split;
            final String[] daysParts = split = days.split(",");
            for (String dayPart : split) {
                dayPart = dayPart.trim();
                this.daysAllowed.add(dayPart.toUpperCase());
            }
        }
        else {
            this.daysAllowed = new HashSet<String>();
        }
        filterContext.put("daysAllowed", this.daysAllowed);
        final String hours = (String) this.filterUse.getParameters().get("Hours");
        if (hours != null && hours.trim().length() > 0) {
            final String[] split2;
            final String[] intervals = split2 = hours.split(",");
            for (final String interval : split2) {
                final String[] hrs = interval.split("-");
                try {
                    if (hrs.length == 1) {
                        final int hr = Integer.parseInt(hrs[0].trim());
                        this.hoursAllowed.add(new HourSpec(hr, Integer.MIN_VALUE));
                    }
                    else {
                        if (hrs.length != 2) {
                            throw new LogicException("Invalid hours specification in filter " + this.filterUse.getName() + ": " + interval + " - this needs to be either a single number of two numbers separated by a dash, e.g. 8,13,15-20,22");
                        }
                        int hrStart = Integer.parseInt(hrs[0].trim());
                        if (hrStart == 24) {
                            hrStart = 0;
                        }
                        int hrEnd = Integer.parseInt(hrs[1].trim());
                        if (hrEnd == 0) {
                            hrEnd = 24;
                        }
                        this.hoursAllowed.add(new HourSpec(hrStart, hrEnd));
                    }
                }
                catch (final NumberFormatException nfex) {
                    throw new LogicException("Invalid hours specification in filter " + this.filterUse.getName() + ": " + interval + " - the hours are not in a valid format");
                }
            }
        }
        else {
            this.hoursAllowed = new HashSet<HourSpec>();
        }
        filterContext.put("hoursAllowed", this.hoursAllowed);
        this.deniedMessage = (String) this.filterUse.getParameters().get("Denied message");
        if (this.deniedMessage == null || this.deniedMessage.trim().length() == 0) {
            this.deniedMessage = "Connection denied";
        }
        filterContext.put("deniedMessage", this.deniedMessage);
        final Boolean runCode = (Boolean) this.filterUse.getParameters().get("Execute code");
        if (runCode == null) {
            this.executeCode = false;
        }
        else {
            this.executeCode = runCode;
        }
        filterContext.put("executeCode", this.executeCode);
        filterContext.put("_initialized", true);
    }
    
    @Override
    public FilterResult acceptConnection(final Socket socket, final Variables context) {
        String clientAddress = null;
        int clientPort = 0;
        if (socket == null) {
            final Variables connCtxt = (Variables)context.get("connectionContext");
            clientAddress = (String)connCtxt.get("clientIP");
            if (connCtxt.hasMember("clientPort")) {
                clientPort = (int)connCtxt.get("clientPort");
            }
        }
        else {
            clientAddress = ((InetSocketAddress)socket.getRemoteSocketAddress()).getHostString();
            clientPort = ((InetSocketAddress)socket.getRemoteSocketAddress()).getPort();
        }
        final FilterResult result = new FilterResult();
        final LocalDateTime now = LocalDateTime.now();
        final String dayOfWeek = now.getDayOfWeek().name();
        if (this.daysAllowed.size() > 0 && !this.daysAllowed.contains(dayOfWeek.toUpperCase())) {
            result.setSuccess(false);
            result.setErrorMessage(this.deniedMessage);
            result.setFilterName(this.filterUse.getName());
            if (DayTimeConnectionFilter.log.isDebugEnabled()) {
                DayTimeConnectionFilter.log.debug(Markers.USER_LOGIC, "Filter " + this.filterUse.getName() + " is rejecting connection from " + clientAddress + ":" + clientPort + " because the day of the week is not allowed");
            }
            return result;
        }
        if (this.hoursAllowed.size() > 0 && result.isSuccess()) {
            final int nowHours = now.get(ChronoField.HOUR_OF_DAY);
            boolean hourIsOK = false;
            for (final HourSpec spec : this.hoursAllowed) {
                if (spec.endTime == Integer.MIN_VALUE) {
                    if (nowHours == spec.startTime) {
                        hourIsOK = true;
                        break;
                    }
                    continue;
                }
                else {
                    if (nowHours >= spec.startTime && nowHours <= spec.endTime) {
                        hourIsOK = true;
                        break;
                    }
                    continue;
                }
            }
            if (!hourIsOK) {
                result.setSuccess(false);
                result.setErrorMessage(this.deniedMessage);
                result.setFilterName(this.filterUse.getName());
                if (DayTimeConnectionFilter.log.isDebugEnabled()) {
                    DayTimeConnectionFilter.log.debug(Markers.USER_LOGIC, "Filter " + this.filterUse.getName() + " is rejecting connection from " + clientAddress + ":" + clientPort + " because the hour of the hour is not allowed");
                }
                return result;
            }
        }
        if (this.executeCode) {
            final Variables ctxt = new Variables();
            ctxt.put("socket", socket);
            ctxt.put("result", result);
            if (clientAddress.startsWith("/")) {
                clientAddress = clientAddress.substring(1);
            }
            ctxt.put("clientIP", clientAddress);
            final Source src = ScriptManager.getInstance().getSource(this.filterUse.getPath().toString());
            ScriptExecutor.executeFilterScript(src, result, ctxt);
        }
        if (!result.isSuccess() && DayTimeConnectionFilter.log.isDebugEnabled()) {
            DayTimeConnectionFilter.log.debug(Markers.USER_LOGIC, "Filter " + this.filterUse.getName() + " is rejecting connection  from " + clientAddress + ":" + clientPort + " : " + this.deniedMessage);
        }
        return result;
    }
    
    @Override
    public String getName() {
        return DayTimeConnectionFilter.class.getName();
    }
    
    static {
        log = LogManager.getLogger("galliumdata.uselog");
    }
    
    private static class HourSpec
    {
        int startTime;
        int endTime;
        
        private HourSpec(final int startTime, final int endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
