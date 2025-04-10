// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.dns;

import com.galliumdata.server.ServerException;
import java.util.Map;
import java.net.InetAddress;

public class DNSAdapterConfiguration
{
    protected InetAddress localAddress;
    protected int localPort;
    protected InetAddress serverAddress;
    protected int serverPort;
    protected boolean defaultConnection;
    protected boolean runTCP;
    
    protected void readParameters(final Map<String, Object> parameters) {
        String localAddressStr = (String) parameters.get("Local address");
        if (null == localAddressStr || localAddressStr.trim().length() == 0) {
            localAddressStr = "0.0.0.0";
        }
        try {
            this.localAddress = InetAddress.getByName(localAddressStr);
        }
        catch (final Exception ex) {
            throw new ServerException("db.dns.config.InvalidLocalAddress", new Object[] { localAddressStr });
        }
        try {
            Object obj = parameters.get("Local port");
            if (obj == null) {
                obj = 53;
            }
            if (!(obj instanceof Integer)) {
                throw new ServerException("repo.BadProperty", new Object[] { "Local port", "Local port must be an integer" });
            }
            this.localPort = (int)obj;
        }
        catch (final Exception ex) {
            throw new ServerException("repo.BadProperty", new Object[] { "Local port", ex.getMessage() });
        }
        final String serverAddressStr = (String) parameters.get("Server host");
        if (null == serverAddressStr || serverAddressStr.trim().length() == 0) {
            throw new ServerException("repo.BadProperty", new Object[] { "Server host", "<empty>" });
        }
        try {
            this.serverAddress = InetAddress.getByName(serverAddressStr);
        }
        catch (final Exception ex2) {
            throw new ServerException("db.dns.config.InvalidServerAddress", new Object[] { serverAddressStr });
        }
        try {
            Object obj2 = parameters.get("Server port");
            if (obj2 == null) {
                obj2 = 53;
            }
            if (!(obj2 instanceof Integer)) {
                throw new ServerException("repo.BadProperty", new Object[] { "Server port", "Server port must be an integer" });
            }
            this.serverPort = (int)obj2;
        }
        catch (final Exception ex2) {
            throw new ServerException("repo.BadProperty", new Object[] { "Server port", ex2.getMessage() });
        }
        final Object defaultConnObj = parameters.get("Default connection");
        if (defaultConnObj == null) {
            this.defaultConnection = false;
        }
        else {
            if (!(defaultConnObj instanceof Boolean)) {
                throw new ServerException("repo.BadProperty", new Object[] { "Default connection", "Must be a boolean" });
            }
            this.defaultConnection = (boolean)defaultConnObj;
        }
        final Object obj3 = parameters.get("Run on TCP");
        if (obj3 == null) {
            this.runTCP = false;
        }
        else {
            if (!(obj3 instanceof Boolean)) {
                throw new ServerException("repo.BadProperty", new Object[] { "Run on TCP", "Must be a boolean" });
            }
            this.runTCP = (boolean)obj3;
        }
    }
}
