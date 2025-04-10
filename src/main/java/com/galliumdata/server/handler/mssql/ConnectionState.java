// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.log.Markers;
import java.util.HashMap;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import java.util.Map;

public class ConnectionState
{
    private int tdsMajorVersion;
    private int tdsMinorVersion;
    private int tdsRevision;
    private int packetSize;
    private String connectionName;
    private String serverName;
    private int serverMajorVersion;
    private boolean columnEncryptionInUse;
    private boolean binaryXml;
    private byte dataClassificationVersion;
    protected Map<Short, SMPSession> smpSessions;
    private TokenColMetadata lastMetadata;
    private MSSQLPacket lastPacketFromClient;
    protected Variables connectionContext;
    private String lastCursorSql;
    private final Map<Integer, String> openStatements;
    private static final int MAX_OPEN_PREP_STMTS = 5000;
    private static final Logger protoLog;
    private String lastRPC;
    private int rpcResultIndex;
    private long currentCursorId;
    private TokenColMetadata currentCursorMetadata;
    private final Map<Long, CursorEntry> cursorMetadata;
    private final Map<Long, CursorEntry> prepCursorMetadata;
    
    public ConnectionState() {
        this.tdsMajorVersion = 7;
        this.tdsMinorVersion = 0;
        this.tdsRevision = 0;
        this.packetSize = 8000;
        this.smpSessions = new HashMap<Short, SMPSession>();
        this.openStatements = new HashMap<Integer, String>();
        this.cursorMetadata = new HashMap<Long, CursorEntry>();
        this.prepCursorMetadata = new HashMap<Long, CursorEntry>();
    }
    
    public synchronized int getTdsMajorVersion() {
        return this.tdsMajorVersion;
    }
    
    public synchronized void setTdsMajorVersion(final int tdsMajorVersion) {
        this.tdsMajorVersion = tdsMajorVersion;
    }
    
    public synchronized int getTdsMinorVersion() {
        return this.tdsMinorVersion;
    }
    
    public synchronized void setTdsMinorVersion(final int tdsMinorVersion) {
        this.tdsMinorVersion = tdsMinorVersion;
    }
    
    public synchronized int getTdsRevision() {
        return this.tdsRevision;
    }
    
    public synchronized void setTdsRevision(final int rev) {
        this.tdsRevision = rev;
    }
    
    public synchronized boolean tdsVersion71andHigher() {
        return this.tdsMajorVersion >= 7 && (this.tdsMajorVersion > 7 || this.tdsMinorVersion >= 1);
    }
    
    public synchronized boolean tdsVersion71Revision1andHigher() {
        return this.tdsMajorVersion >= 7 && (this.tdsMajorVersion > 7 || (this.tdsMinorVersion >= 1 && (this.tdsMinorVersion >= 2 || this.tdsRevision >= 1)));
    }
    
    public synchronized boolean tdsVersion71AndLower() {
        return this.tdsMajorVersion <= 7 && (this.tdsMajorVersion < 7 || this.tdsMinorVersion <= 1);
    }
    
    public synchronized boolean tdsVersion72andHigher() {
        return this.tdsMajorVersion >= 7 && (this.tdsMajorVersion > 7 || this.tdsMinorVersion >= 2);
    }
    
    public synchronized boolean tdsVersion73andHigher() {
        return this.tdsMajorVersion >= 7 && (this.tdsMajorVersion > 7 || this.tdsMinorVersion >= 3);
    }
    
    public synchronized boolean tdsVersion73andLower() {
        return this.tdsMajorVersion <= 7 && (this.tdsMajorVersion < 7 || this.tdsMinorVersion <= 3);
    }
    
    public synchronized boolean tdsVersion74andHigher() {
        return this.tdsMajorVersion >= 7 && (this.tdsMajorVersion > 7 || this.tdsMinorVersion >= 4);
    }
    
    public int getPacketSize() {
        return this.packetSize;
    }
    
    public void setPacketSize(final int packetSize) {
        this.packetSize = packetSize;
    }
    
    public String getConnectionName() {
        return this.connectionName;
    }
    
    public void setConnectionName(final String connectionName) {
        this.connectionName = connectionName;
    }
    
    public void setServerName(final String name) {
        this.serverName = name;
    }
    
    public String getServerName() {
        return this.serverName;
    }
    
    public int getServerMajorVersion() {
        return this.serverMajorVersion;
    }
    
    public void setServerMajorVersion(final int serverMajorVersion) {
        this.serverMajorVersion = serverMajorVersion;
    }
    
    public boolean isColumnEncryptionInUse() {
        return this.columnEncryptionInUse;
    }
    
    public void setColumnEncryptionInUse(final boolean columnEncryptionInUse) {
        this.columnEncryptionInUse = columnEncryptionInUse;
    }
    
    public boolean isBinaryXml() {
        return this.binaryXml;
    }
    
    public void setBinaryXml(final boolean binaryXml) {
        this.binaryXml = binaryXml;
    }
    
    public byte getDataClassificationVersion() {
        return this.dataClassificationVersion;
    }
    
    public void setDataClassificationVersion(final byte dataClassificationVersion) {
        this.dataClassificationVersion = dataClassificationVersion;
    }
    
    public TokenColMetadata getLastMetadata() {
        return this.lastMetadata;
    }
    
    public void setLastMetadata(final TokenColMetadata lastMetadata) {
        if ("sp_cursoropen".equalsIgnoreCase(this.lastRPC) || "sp_cursorprepexec".equalsIgnoreCase(this.lastRPC)) {
            if (this.currentCursorId == 0L) {
                this.currentCursorMetadata = lastMetadata;
            }
            else {
                final CursorEntry entry = new CursorEntry();
                entry.id = this.currentCursorId;
                entry.sql = this.lastCursorSql;
                entry.metadata = lastMetadata;
                this.cursorMetadata.put(this.currentCursorId, entry);
                this.currentCursorId = 0L;
            }
        }
        else if ("sp_cursorprepare".equalsIgnoreCase(this.lastRPC) || "sp_cursorexecute".equalsIgnoreCase(this.lastRPC)) {
            if (this.currentCursorId == 0L) {
                this.currentCursorMetadata = lastMetadata;
            }
            else {
                final CursorEntry entry = new CursorEntry();
                entry.id = this.currentCursorId;
                entry.sql = this.lastCursorSql;
                entry.metadata = lastMetadata;
                this.cursorMetadata.put(this.currentCursorId, entry);
                this.currentCursorId = 0L;
            }
        }
        else {
            this.lastMetadata = lastMetadata;
        }
    }
    
    public MSSQLPacket getLastPacketFromClient() {
        return this.lastPacketFromClient;
    }
    
    public void setLastPacketFromClient(final MSSQLPacket lastPacketFromClient) {
        this.lastPacketFromClient = lastPacketFromClient;
    }
    
    public void setConnectionContext(final Variables vars) {
        this.connectionContext = vars;
    }
    
    public void setConnectionContextValue(final String name, final Object value) {
        this.connectionContext.put(name, value);
    }
    
    public void startSMPSession(final SMPPacket pkt, final MSSQLForwarder forwarder) {
        final SMPSession session = new SMPSession(pkt, forwarder.connectionContext);
        this.smpSessions.put(session.getSid(), session);
        if (ConnectionState.protoLog.isTraceEnabled()) {
            ConnectionState.protoLog.trace(Markers.MSSQL, "Starting SMP session " + session.getSid());
        }
    }
    
    public void closeSMPSession(final SMPPacket pkt) {
        final SMPSession session = this.smpSessions.get((short)pkt.getSMPSessionId());
        if (session == null) {
            ConnectionState.protoLog.trace(Markers.MSSQL, "SMP session close received for unknown session");
            return;
        }
        this.smpSessions.remove((short)pkt.getSMPSessionId());
        if (ConnectionState.protoLog.isTraceEnabled()) {
            ConnectionState.protoLog.trace(Markers.MSSQL, "Closing SMP session " + pkt.getSMPSessionId());
        }
    }
    
    public SMPSession getSMPSession(final short sid) {
        return this.smpSessions.get(sid);
    }
    
    public void openPreparedStatement(final String sql, final int id) {
        this.openStatements.put(id, sql);
        if (this.openStatements.size() > 5000) {
            throw new ServerException("db.mssql.server.TooManyOpenStatements", new Object[] { 5000 });
        }
    }
    
    public void closePreparedStatement(final int id) {
        if (!this.openStatements.containsKey(id)) {
            ConnectionState.protoLog.debug(Markers.MSSQL, "Closing unknown prepared statement: {}", (Object)id);
        }
        this.openStatements.remove(id);
        ConnectionState.protoLog.debug(Markers.MSSQL, "Closed prepared statement {}, still open: {}", (Object)id, (Object)this.openStatements.size());
    }
    
    public String getPreparedStatement(final int id) {
        return this.openStatements.get(id);
    }
    
    public void setLastRPC(final String s) {
        this.lastRPC = s;
        this.rpcResultIndex = 0;
        this.currentCursorId = 0L;
        ConnectionState.protoLog.trace(Markers.MSSQL, "Setting last RPC in ConnectionState to: {} for connection: {}", (Object)s, (Object)this.connectionName);
    }
    
    public String getLastRPC() {
        return this.lastRPC;
    }
    
    public void clearLastRPC() {
        this.lastRPC = null;
        this.rpcResultIndex = 0;
        this.currentCursorId = 0L;
        this.currentCursorMetadata = null;
    }
    
    public int getRPCResultIndex() {
        return this.rpcResultIndex;
    }
    
    public void incrementRPCResultIndex() {
        ++this.rpcResultIndex;
    }
    
    public void setCurrentCursorID(final long id) {
        this.currentCursorId = id;
        if (this.currentCursorMetadata != null) {
            final CursorEntry entry = new CursorEntry();
            entry.id = id;
            entry.sql = this.lastCursorSql;
            entry.metadata = this.currentCursorMetadata;
            this.cursorMetadata.put(id, entry);
            ConnectionState.protoLog.trace(Markers.MSSQL, "Adding metadata for cursor: {} on connection: {}, SQL: {}", (Object)id, (Object)this.connectionName, (Object)entry.sql);
            this.currentCursorId = 0L;
            this.currentCursorMetadata = null;
            this.lastCursorSql = null;
        }
        else {
            ConnectionState.protoLog.trace(Markers.MSSQL, "Current cursor ID is now: {} for connection: {}, no metadata yet, SQL: {}", (Object)id, (Object)this.connectionName, (Object)this.lastCursorSql);
        }
    }
    
    public void setCurrentPrepCursorHandle(final long id) {
        if (this.currentCursorMetadata != null) {
            final CursorEntry entry = new CursorEntry();
            entry.id = id;
            entry.sql = this.lastCursorSql;
            entry.metadata = this.currentCursorMetadata;
            this.prepCursorMetadata.put(id, entry);
            ConnectionState.protoLog.trace(Markers.MSSQL, "Adding metadata for prep cursor: {} on connection: {}, SQL: {}", (Object)id, (Object)this.connectionName, (Object)entry.sql);
        }
        else {
            ConnectionState.protoLog.trace(Markers.MSSQL, "Current prep cursor ID is now: {} for connection: {}, no metadata yet, SQL: {}", (Object)id, (Object)this.connectionName, (Object)this.lastCursorSql);
        }
    }
    
    public String executePrepCursor(final long id) {
        if (!this.prepCursorMetadata.containsKey(id)) {
            ConnectionState.protoLog.debug(Markers.MSSQL, "sp_cursorexecute called for unknown prepared cursor statement: {}", (Object)id);
            return null;
        }
        final CursorEntry entry = this.prepCursorMetadata.get(id);
        this.lastCursorSql = entry.sql;
        this.currentCursorMetadata = entry.metadata;
        return entry.sql;
    }
    
    public void setLastCursorSql(final String s) {
        this.lastCursorSql = s;
    }
    
    public String useCursor(final long id) {
        this.rpcResultIndex = 0;
        if (!this.cursorMetadata.containsKey(id)) {
            ConnectionState.protoLog.debug(Markers.MSSQL, "Current cursor ID is unknown: {} for connection: {}", (Object)id, (Object)this.connectionName);
            return null;
        }
        final CursorEntry entry = this.cursorMetadata.get(id);
        this.lastMetadata = entry.metadata;
        return entry.sql;
    }
    
    public void closeCursor(final long id) {
        if (ConnectionState.protoLog.isDebugEnabled() && !this.cursorMetadata.containsKey(id)) {
            ConnectionState.protoLog.debug(Markers.MSSQL, "Unable to get metadata for unknown cursor: {} on connection: {}", (Object)id, (Object)this.connectionName);
        }
        if (!this.cursorMetadata.containsKey(id)) {
            ConnectionState.protoLog.debug(Markers.MSSQL, "Unable to close unknown cursor: {} on connection: {}", (Object)id, (Object)this.connectionName);
        }
        this.cursorMetadata.remove(id);
        this.currentCursorId = 0L;
        this.rpcResultIndex = 0;
        if (ConnectionState.protoLog.isTraceEnabled()) {
            ConnectionState.protoLog.trace(Markers.MSSQL, "Closing cursor: {} for connection: {}", (Object)id, (Object)this.connectionName);
            if (this.cursorMetadata.isEmpty()) {
                ConnectionState.protoLog.trace(Markers.MSSQL, "All cursors are closed for connection: {}", (Object)this.connectionName);
            }
        }
    }
    
    public void closePrepCursor(final long id) {
        if (ConnectionState.protoLog.isDebugEnabled() && !this.prepCursorMetadata.containsKey(id)) {
            ConnectionState.protoLog.debug(Markers.MSSQL, "Unable to get metadata for unknown prep cursor: {} on connection: {}", (Object)id, (Object)this.connectionName);
        }
        if (!this.prepCursorMetadata.containsKey(id)) {
            ConnectionState.protoLog.debug(Markers.MSSQL, "Unable to close unknown prep cursor: {} on connection: {}", (Object)id, (Object)this.connectionName);
        }
        this.prepCursorMetadata.remove(id);
        this.rpcResultIndex = 0;
        if (ConnectionState.protoLog.isTraceEnabled()) {
            ConnectionState.protoLog.trace(Markers.MSSQL, "Closing prep cursor: {} for connection: {}", (Object)id, (Object)this.connectionName);
            if (this.prepCursorMetadata.isEmpty()) {
                ConnectionState.protoLog.trace(Markers.MSSQL, "All prep cursors are closed for connection: {}", (Object)this.connectionName);
            }
        }
    }
    
    static {
        protoLog = LogManager.getLogger("galliumdata.dbproto");
    }
}
