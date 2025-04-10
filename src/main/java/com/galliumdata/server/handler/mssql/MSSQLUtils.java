// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

public class MSSQLUtils
{
    private final ClientForwarder clientForwarder;
    
    public MSSQLUtils(final ClientForwarder clientForwarder) {
        this.clientForwarder = clientForwarder;
    }
    
    public MSSQLResultSet executeQuery(final String sql) {
        final ConnectionState connState = this.clientForwarder.getConnectionState();
        final MSSQLPacket lastPacket = connState.getLastPacketFromClient();
        final MSSQLResultSet rs = this.clientForwarder.queryForRows(lastPacket, sql);
        return rs;
    }
}
