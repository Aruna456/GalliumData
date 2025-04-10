// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.log.Markers;
import java.util.Iterator;
import com.galliumdata.server.ServerException;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.adapters.Variables;

public class SMPSession
{
    private short sid;
    private int clientSeqNum;
    private int serverSeqNum;
    private int clientWindow;
    private int serverWindow;
    private Variables smpConnectionContext;
    private static final Logger log;
    
    public SMPSession(final SMPPacket pkt, final Variables connectionContext) {
        this.clientSeqNum = 1;
        this.serverSeqNum = 1;
        this.smpConnectionContext = new Variables();
        if (pkt.getSerializedSize() != 16) {
            throw new ServerException("db.mssql.protocol.BadSMPSyn", new Object[] { "Unexpected size, expected 16, got: " + pkt.getSerializedSize() });
        }
        if (pkt.getSMPPacketType() != SMPPacket.SMPPacketType.SYN) {
            throw new ServerException("db.mssql.protocol.BadSMPSyn", new Object[] { "SYN has type " + String.valueOf(pkt.getSMPPacketType()) });
        }
        if (pkt.getSeqNum() != 0) {
            throw new ServerException("db.mssql.protocol.BadSMPSyn", new Object[] { "SYN does not have seqNum==0 but " + pkt.getSeqNum() });
        }
        this.sid = (short)pkt.getSMPSessionId();
        this.clientWindow = pkt.getSMPWindow();
        for (final String key : connectionContext.keySet()) {
            this.smpConnectionContext.put(key, connectionContext.get(key));
        }
    }
    
    public short getSid() {
        return this.sid;
    }
    
    public void setSid(final short sid) {
        this.sid = sid;
    }
    
    public int getClientSeqNum() {
        return this.clientSeqNum;
    }
    
    public int getAndIncrementClientSeqNum() {
        final int seq = this.clientSeqNum;
        ++this.clientSeqNum;
        return seq;
    }
    
    public void setClientSeqNum(final int clientSeqNum) {
        this.clientSeqNum = clientSeqNum;
    }
    
    public int getServerSeqNum() {
        return this.serverSeqNum;
    }
    
    public int getAndIncrementServerSeqNum() {
        final int seq = this.serverSeqNum;
        ++this.serverSeqNum;
        return seq;
    }
    
    public void setServerSeqNum(final int serverSeqNum) {
        this.serverSeqNum = serverSeqNum;
    }
    
    public int getClientWindow() {
        return this.clientWindow;
    }
    
    public void setClientWindow(final int clientWindow) {
        if (SMPSession.log.isTraceEnabled()) {
            SMPSession.log.trace(Markers.MSSQL, "SMP Session: setting client window to " + clientWindow);
        }
        this.clientWindow = clientWindow;
    }
    
    public int getServerWindow() {
        return this.serverWindow;
    }
    
    public void setServerWindow(final int serverWindow) {
        if (SMPSession.log.isTraceEnabled()) {
            SMPSession.log.trace(Markers.MSSQL, "SMP Session: setting server window to " + serverWindow);
        }
        this.serverWindow = serverWindow;
    }
    
    public Variables getSMPConnectionContext() {
        return this.smpConnectionContext;
    }
    
    public void setSMPConnectionContext(final Variables connectionContext) {
        this.smpConnectionContext = connectionContext;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
