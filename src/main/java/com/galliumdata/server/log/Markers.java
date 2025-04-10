// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.log;

import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.Marker;

public class Markers
{
    public static final Marker SYSTEM;
    public static final Marker REPO;
    public static final Marker WEB;
    public static final Marker MYSQL;
    public static final Marker MONGO;
    public static final Marker POSTGRES;
    public static final Marker VERTICA;
    public static final Marker DNS;
    public static final Marker MSSQL;
    public static final Marker ORACLE;
    public static final Marker REDIS;
    public static final Marker HTTP;
    public static final Marker CASSANDRA;
    public static final Marker DB2;
    public static final Marker USER_LOGIC;
    
    static {
        SYSTEM = (Marker)new MarkerManager.Log4jMarker("Sys");
        REPO = (Marker)new MarkerManager.Log4jMarker("Repos");
        WEB = (Marker)new MarkerManager.Log4jMarker("Web");
        MYSQL = (Marker)new MarkerManager.Log4jMarker("MySQL");
        MONGO = (Marker)new MarkerManager.Log4jMarker("MongoDB");
        POSTGRES = (Marker)new MarkerManager.Log4jMarker("Postgr");
        VERTICA = (Marker)new MarkerManager.Log4jMarker("Vertica");
        DNS = (Marker)new MarkerManager.Log4jMarker("Dns");
        MSSQL = (Marker)new MarkerManager.Log4jMarker("MSSQL");
        ORACLE = (Marker)new MarkerManager.Log4jMarker("Oracle");
        REDIS = (Marker)new MarkerManager.Log4jMarker("Redis");
        HTTP = (Marker)new MarkerManager.Log4jMarker("HTTP");
        CASSANDRA = (Marker)new MarkerManager.Log4jMarker("Cassandra");
        DB2 = (Marker)new MarkerManager.Log4jMarker("DB2");
        USER_LOGIC = (Marker)new MarkerManager.Log4jMarker("Logic");
    }
}
