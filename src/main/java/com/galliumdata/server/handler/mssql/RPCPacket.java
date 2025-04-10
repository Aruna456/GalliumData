// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.js.JSListWrapper;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.log.Markers;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class RPCPacket extends DataStreamPacket implements ProxyObject
{
    public static final String SP_CURSOREXECUTE = "sp_cursorexecute";
    public static final String SP_CURSOROPEN = "sp_cursoropen";
    public static final String SP_CURSORCLOSE = "sp_cursorclose";
    public static final String SP_CURSORPREPARE = "sp_cursorprepare";
    public static final String SP_CURSORUNPREPARE = "sp_cursorunprepare";
    public static final String SP_CURSORPREPEXEC = "sp_cursorprepexec";
    public static final String SP_CURSORFETCH = "sp_cursorfetch";
    private int procID;
    private String procName;
    private boolean noExec;
    private boolean fWithRecomp;
    private boolean fNoMetaData;
    private boolean fReuseMetaData;
    private final List<RPCParameter> parameters;
    private RPCPacket nextInBatch;
    
    public RPCPacket(final ConnectionState connectionState) {
        super(connectionState);
        this.parameters = new ArrayList<RPCParameter>();
        this.typeCode = 3;
    }
    
    public void setSMPSessionId(final int id) {
        this.smpSessionId = id;
        if (this.nextInBatch != null) {
            this.nextInBatch.setSMPSessionId(id);
        }
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        if (numBytes > this.length) {
            this.length = numBytes;
        }
        idx += this.readBatch(bytes, idx, numBytes);
        return idx - offset;
    }
    
    public int readBatch(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        final int nameLenProcID = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
        idx += 2;
        if (nameLenProcID == 65535) {
            this.procID = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
        }
        else {
            this.procName = new String(bytes, idx, nameLenProcID * 2, StandardCharsets.UTF_16LE);
            idx += nameLenProcID * 2;
        }
        this.connectionState.setLastRPC(this.getProcName());
        final short flags = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.fWithRecomp = ((flags & 0x1) > 0);
        this.fNoMetaData = ((flags & 0x2) > 0);
        this.fReuseMetaData = ((flags & 0x4) > 0);
        while (idx < numBytes) {
            if (bytes[idx] == -128 || bytes[idx] == -1) {
                ++idx;
                this.nextInBatch = new RPCPacket(this.connectionState);
                idx += this.nextInBatch.readBatch(bytes, idx, numBytes);
            }
            else if (this.connectionState.tdsVersion72andHigher() && bytes[idx] == -2) {
                this.noExec = true;
                ++idx;
            }
            else {
                final RPCParameter param = new RPCParameter(this.connectionState);
                idx += param.readFromBytes(bytes, idx);
                this.parameters.add(param);
            }
        }
        this.handleState();
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += this.getBatchSize();
        return size;
    }
    
    public int getBatchSize() {
        int size = 2;
        if (this.procName == null) {
            size += 2;
        }
        else {
            size += this.procName.length() * 2;
        }
        size += 2;
        for (final RPCParameter param : this.parameters) {
            size += param.getSerializedSize();
        }
        if (this.connectionState.tdsVersion72andHigher() && this.noExec) {
            ++size;
        }
        if (this.nextInBatch != null) {
            size = ++size + this.nextInBatch.getBatchSize();
        }
        return size;
    }
    
    private void handleState() {
        String sql = null;
        Object cursorIdObj = null;
        final String lowerCase = this.getProcName().toLowerCase();
        switch (lowerCase) {
            case "sp_cursorclose": {
                cursorIdObj = this.parameters.get(0).getValue().getValue();
                if (!(cursorIdObj instanceof Number)) {
                    RPCPacket.log.debug(Markers.MSSQL, "sp_cursorclose parameter 0 was not a number: {}", cursorIdObj);
                    throw new ServerException("db.mssql.protocol.InvalidCursorId", cursorIdObj, "sp_cursorclose");
                }
                this.connectionState.closeCursor(((Number)cursorIdObj).longValue());
                RPCPacket.log.trace(Markers.MSSQL, "sp_cursorclose: {}", cursorIdObj);
                break;
            }
            case "sp_cursorexecute": {
                cursorIdObj = this.parameters.get(0).getValue().getValue();
                if (!(cursorIdObj instanceof Number)) {
                    RPCPacket.log.debug(Markers.MSSQL, "sp_cursorexecute parameter 0 was not a number: {}", cursorIdObj);
                    throw new ServerException("db.mssql.protocol.InvalidCursorId", cursorIdObj, "sp_cursorexecute");
                }
                final String execSql = this.connectionState.executePrepCursor(((Number)cursorIdObj).longValue());
                RPCPacket.log.trace(Markers.MSSQL, "sp_cursorexecute for handle: {}, SQL: {}", cursorIdObj, execSql);
                break;
            }
            case "sp_cursorfetch": {
                cursorIdObj = this.parameters.get(0).getValue().getValue();
                if (!(cursorIdObj instanceof Number)) {
                    RPCPacket.log.debug(Markers.MSSQL, "sp_cursorfetch parameter 0 was not a number: {}", cursorIdObj);
                    throw new ServerException("db.mssql.protocol.InvalidCursorId", cursorIdObj, "sp_cursorfetch");
                }
                final String fetchSql2 = this.connectionState.useCursor(((Number)cursorIdObj).longValue());
                this.connectionState.setConnectionContextValue("lastSQL", fetchSql2);
                RPCPacket.log.trace(Markers.MSSQL, "sp_cursorfetch for cursor: {}, SQL: {}", cursorIdObj, fetchSql2);
                break;
            }
            case "sp_cursoropen": {
                sql = this.parameters.get(1).getValue().getValue().toString();
                this.connectionState.setLastCursorSql(sql);
                RPCPacket.log.trace(Markers.MSSQL, "sp_cursoropen: {}", sql);
                break;
            }
            case "sp_cursorprepare": {
                sql = this.parameters.get(2).getValue().getValue().toString();
                this.connectionState.setLastCursorSql(sql);
                RPCPacket.log.trace(Markers.MSSQL, "sp_cursorprepare: {}", sql);
                break;
            }
            case "sp_cursorprepexec": {
                sql = this.parameters.get(3).getValue().getValue().toString();
                this.connectionState.setConnectionContextValue("lastSQL", sql);
                this.connectionState.setLastCursorSql(sql);
                RPCPacket.log.trace(Markers.MSSQL, "sp_cursorprepexec: {}", sql);
                break;
            }
            case "sp_cursorunprepare": {
                final Object handle = this.parameters.get(0).getValue().getValue();
                if (!(handle instanceof Number)) {
                    RPCPacket.log.debug(Markers.MSSQL, "sp_cursorunprepare parameter 0 was not a number: {}", handle);
                    throw new ServerException("db.mssql.protocol.InvalidCursorId", handle, "sp_cursorunprepare");
                }
                this.connectionState.closePrepCursor(((Number)handle).longValue());
                break;
            }
            case "sp_execute": {
                final int id = (int)this.parameters.get(0).getValue().getValue();
                sql = this.connectionState.getPreparedStatement(id);
                this.connectionState.setConnectionContextValue("lastSQL", sql);
                RPCPacket.log.trace(Markers.MSSQL, "sp_execute: {}", sql);
                break;
            }
            case "sp_executesql": {
                sql = this.parameters.get(0).getValue().getValue().toString();
                this.connectionState.setConnectionContextValue("lastSQL", sql);
                RPCPacket.log.trace(Markers.MSSQL, "sp_executesql: {}", sql);
                break;
            }
            case "sp_prepare":
            case "sp_prepexec": {
                sql = this.parameters.get(2).getValue().getValue().toString();
                this.connectionState.setConnectionContextValue("lastSQL", sql);
                RPCPacket.log.trace(Markers.MSSQL, "{}: {}", this.getProcName().toLowerCase(), sql);
                break;
            }
            case "sp_prepexecrpc": {
                sql = this.parameters.get(1).getValue().getValue().toString();
                this.connectionState.setConnectionContextValue("lastSQL", sql);
                RPCPacket.log.trace(Markers.MSSQL, "sp_prepexecrpc: {}", sql);
                break;
            }
            case "sp_unprepare": {
                final int id2 = (int)this.parameters.get(0).getValue().getValue();
                this.connectionState.closePreparedStatement(id2);
                RPCPacket.log.trace(Markers.MSSQL, "sp_unprepare: {}", id2);
                break;
            }
        }
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        this.writeBatch(writer);
    }
    
    public void writeBatch(final RawPacketWriter writer) {
        if (this.procName == null) {
            writer.writeTwoByteIntegerLow(-1);
            writer.writeTwoByteIntegerLow((short)this.procID);
        }
        else {
            writer.writeTwoByteIntegerLow((short)this.procName.length());
            final byte[] strBytes = this.procName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
        short flags = 0;
        if (this.fWithRecomp) {
            flags |= 0x1;
        }
        if (this.fNoMetaData) {
            flags |= 0x2;
        }
        if (this.fReuseMetaData) {
            flags |= 0x4;
        }
        writer.writeTwoByteIntegerLow(flags);
        for (final RPCParameter param : this.parameters) {
            param.write(writer);
        }
        if (this.connectionState.tdsVersion72andHigher() && this.noExec) {
            writer.writeByte((byte)(-2));
        }
        if (this.nextInBatch != null) {
            if (this.connectionState.tdsVersion72andHigher()) {
                writer.writeByte((byte)(-1));
            }
            else {
                writer.writeByte((byte)(-128));
            }
            this.nextInBatch.writeBatch(writer);
        }
    }
    
    @Override
    public String getPacketType() {
        if (this.nextInBatch != null) {
            return "RPC (batch)";
        }
        return "RPC";
    }
    
    public String toString() {
        String params = " ";
        int num = 0;
        for (RPCParameter param : this.parameters) {
            params = params + param.toString() + ", ";
            if (++num > 5) {
                params = params + (this.parameters.size() - num) + " more...";
                break;
            }
        }
        return "RPCPacket: " + this.getProcName() + params;
    }
    
    public String toLongString() {
        String params = " ";
        for (RPCParameter param : this.parameters) {
            params = params + param.toString() + ", ";
        }
        return "RPCPacket: " + this.getProcName() + params;
    }
    
    public int getProcID() {
        return this.procID;
    }
    
    public void setProcID(final int procID) {
        this.procID = procID;
    }
    
    public String getProcName() {
        if (this.procName != null) {
            return this.procName;
        }
        switch (this.procID) {
            case 1: {
                return "Sp_Cursor";
            }
            case 2: {
                return "Sp_CursorOpen";
            }
            case 3: {
                return "Sp_CursorPrepare";
            }
            case 4: {
                return "Sp_CursorExecute";
            }
            case 5: {
                return "Sp_CursorPrepExec";
            }
            case 6: {
                return "Sp_CursorUnprepare";
            }
            case 7: {
                return "Sp_CursorFetch";
            }
            case 8: {
                return "Sp_CursorOption";
            }
            case 9: {
                return "Sp_CursorClose";
            }
            case 10: {
                return "Sp_ExecuteSql";
            }
            case 11: {
                return "Sp_Prepare";
            }
            case 12: {
                return "Sp_Execute";
            }
            case 13: {
                return "Sp_PrepExec";
            }
            case 14: {
                return "Sp_PrepExecRpc";
            }
            case 15: {
                return "Sp_Unprepare";
            }
            default: {
                return "Unknown system proc " + this.procID;
            }
        }
    }
    
    public void setProcName(final String procName) {
        final String lowerCase = procName.toLowerCase();
        switch (lowerCase) {
            case "sp_cursor": {
                this.procID = 1;
                this.procName = null;
                break;
            }
            case "sp_cursoropen": {
                this.procID = 2;
                this.procName = null;
                break;
            }
            case "sp_cursorprepare": {
                this.procID = 3;
                this.procName = null;
                break;
            }
            case "sp_cursorexecute": {
                this.procID = 4;
                this.procName = null;
                break;
            }
            case "sp_cursorprepexec": {
                this.procID = 5;
                this.procName = null;
                break;
            }
            case "sp_cursorunprepare": {
                this.procID = 6;
                this.procName = null;
                break;
            }
            case "sp_cursorfetch": {
                this.procID = 7;
                this.procName = null;
                break;
            }
            case "sp_cursoroption": {
                this.procID = 8;
                this.procName = null;
                break;
            }
            case "sp_cursorclose": {
                this.procID = 9;
                this.procName = null;
                break;
            }
            case "sp_executesql": {
                this.procID = 10;
                this.procName = null;
                break;
            }
            case "sp_prepare": {
                this.procID = 11;
                this.procName = null;
                break;
            }
            case "sp_execute": {
                this.procID = 12;
                this.procName = null;
                break;
            }
            case "sp_prepexec": {
                this.procID = 13;
                this.procName = null;
                break;
            }
            case "sp_prepexecrpc": {
                this.procID = 14;
                this.procName = null;
                break;
            }
            case "sp_unprepare": {
                this.procID = 15;
                this.procName = null;
                break;
            }
            default: {
                this.procName = procName;
                break;
            }
        }
    }
    
    public List<RPCParameter> getParameters() {
        return this.parameters;
    }
    
    public boolean isNoExec() {
        return this.noExec;
    }
    
    public void setNoExec(final boolean b) {
        this.noExec = b;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "procID": {
                return this.procID;
            }
            case "procName": {
                return this.getProcName();
            }
            case "fWithRecomp": {
                return this.fWithRecomp;
            }
            case "fNoMetaData": {
                return this.fNoMetaData;
            }
            case "fReuseMetaData": {
                return this.fReuseMetaData;
            }
            case "noExec": {
                return this.noExec;
            }
            case "parameters": {
                return new JSListWrapper(this.parameters, this::setModified);
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
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "procID", "procName", "fWithRecomp", "fNoMetaData", "fReuseMetaData", "noExec", "parameters", "packetType", "remove", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "procID":
            case "procName":
            case "fWithRecomp":
            case "fNoMetaData":
            case "fReuseMetaData":
            case "noExec":
            case "parameters":
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
    
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "procID": {
                this.procID = value.asInt();
                break;
            }
            case "procName": {
                this.procName = value.asString();
                break;
            }
            case "fWithRecomp": {
                this.fWithRecomp = value.asBoolean();
                break;
            }
            case "fNoMetaData": {
                this.fNoMetaData = value.asBoolean();
                break;
            }
            case "fReuseMetaData": {
                this.fReuseMetaData = value.asBoolean();
                break;
            }
            case "noExec": {
                this.noExec = value.asBoolean();
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "RPC packet");
    }
}
