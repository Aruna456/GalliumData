// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class SQLBatchPacket extends MSSQLPacket implements ProxyObject
{
    private List<StreamHeader> headers;
    private String sql;
    
    public SQLBatchPacket(final ConnectionState connectionState) {
        super(connectionState);
        this.headers = new ArrayList<StreamHeader>();
        this.typeCode = 1;
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        this.connectionState.clearLastRPC();
        if (this.connectionState.tdsVersion72andHigher() && this.getPacketId() == 1) {
            final int totalHeaders = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
            while (idx - offset - 8 < totalHeaders) {
                final byte type = bytes[idx + 4];
                final StreamHeader header = StreamHeader.createStreamHeader(type);
                idx += header.readFromBytes(bytes, idx, totalHeaders - (idx - offset - 4));
                this.headers.add(header);
            }
        }
        final int sqlLen = this.length - (idx - offset);
        this.sql = new String(bytes, idx, sqlLen, StandardCharsets.UTF_16LE);
        idx += sqlLen;
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.connectionState.tdsVersion72andHigher()) {
            size += 4;
            for (final StreamHeader header : this.headers) {
                size += header.getSerializedSize();
            }
        }
        size += this.sql.length() * 2;
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (!this.headers.isEmpty()) {
            int headersSize = 4;
            for (final StreamHeader header : this.headers) {
                headersSize += header.getSerializedSize();
            }
            writer.writeFourByteIntegerLow(headersSize);
            for (final StreamHeader header : this.headers) {
                header.write(writer);
            }
        }
        final byte[] sqlBytes = this.sql.getBytes(StandardCharsets.UTF_16LE);
        writer.writeBytes(sqlBytes, 0, sqlBytes.length);
    }
    
    public String getPacketType() {
        return "SQLBatch";
    }
    
    @Override
    public String toString() {
        String s = this.sql;
        s = s.replaceAll("\\v", " ");
        s = s.replaceAll("  +", " ");
        if (s.length() > 70) {
            s = s.substring(0, 70) + "... [" + (this.sql.length() - 70) + " more]";
        }
        return "SQL batch: " + s;
    }
    
    @Override
    public String toLongString() {
        return "SQL batch: " + this.sql;
    }
    
    public void copyStreamHeadersFrom(final SQLBatchPacket otherPacket) {
        this.headers = otherPacket.headers;
    }
    
    public void addStreamHeaders() {
        final StreamHeaderTxDescriptor header = new StreamHeaderTxDescriptor();
        header.setOutstandingRequestCount(1);
        this.headers.add(header);
    }
    
    public String getSql() {
        return this.sql;
    }
    
    public void setSql(final String sql) {
        this.sql = sql;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "sql": {
                return this.sql;
            }
            case "packetType": {
                return this.getPacketType();
            }
            case "remove": {
                return (Function<Value[],Object>) arguments -> {
                    this.remove();
                    return null;
                };
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        return new String[] { "sql", "packetType", "remove", "toString" };
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "sql":
            case "packetType":
            case "remove":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "sql": {
                this.sql = value.asString();
                return;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "SQLBatch packet" });
    }
}
