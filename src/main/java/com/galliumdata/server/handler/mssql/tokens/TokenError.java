// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;
import org.graalvm.polyglot.proxy.ProxyObject;

public class TokenError extends MessageToken implements ProxyObject
{
    private int errorNumber;
    private byte errorState;
    private byte errorClass;
    private String message;
    private String serverName;
    private String procedureName;
    private int lineNumber;
    
    public TokenError(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.errorNumber = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.errorState = bytes[idx];
        ++idx;
        this.errorClass = bytes[idx];
        ++idx;
        final int msgLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.message = new String(bytes, idx, msgLen * 2, StandardCharsets.UTF_16LE);
        idx += msgLen * 2;
        final byte serverNameLen = bytes[idx];
        ++idx;
        this.serverName = new String(bytes, idx, serverNameLen * 2, StandardCharsets.UTF_16LE);
        idx += serverNameLen * 2;
        final byte procNameLen = bytes[idx];
        ++idx;
        this.procedureName = new String(bytes, idx, procNameLen * 2, StandardCharsets.UTF_16LE);
        idx += procNameLen * 2;
        if (this.connectionState.tdsVersion72andHigher()) {
            this.lineNumber = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
        }
        else {
            this.lineNumber = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
        }
        if (idx - offset - 3 != length) {
            throw new ServerException("db.mssql.protocol.UnexpectedTokenSize", new Object[] { this.getTokenTypeName(), length, idx - offset - 3 });
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readTwoByteIntLow();
        this.errorNumber = reader.readFourByteIntLow();
        this.errorState = reader.readByte();
        this.errorClass = reader.readByte();
        final int msgLen = reader.readTwoByteIntLow();
        this.message = reader.readString(msgLen * 2);
        final byte serverNameLen = reader.readByte();
        this.serverName = reader.readString(serverNameLen * 2);
        final byte procNameLen = reader.readByte();
        this.procedureName = reader.readString(procNameLen * 2);
        if (this.connectionState.tdsVersion72andHigher()) {
            this.lineNumber = reader.readFourByteIntLow();
        }
        else {
            this.lineNumber = reader.readTwoByteIntLow();
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        size += 4;
        ++size;
        ++size;
        size += 2;
        if (this.message != null) {
            size += this.message.length() * 2;
        }
        ++size;
        if (this.serverName != null) {
            size += this.serverName.length() * 2;
        }
        ++size;
        if (this.procedureName != null) {
            size += this.procedureName.length() * 2;
        }
        if (this.connectionState.tdsVersion72andHigher()) {
            size += 4;
        }
        else {
            size += 2;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow(this.getSerializedSize() - 3);
        writer.writeFourByteIntegerLow(this.errorNumber);
        writer.writeByte(this.errorState);
        writer.writeByte(this.errorClass);
        if (this.message == null) {
            writer.writeTwoByteIntegerLow(0);
        }
        else {
            writer.writeTwoByteIntegerLow(this.message.length());
            if (this.message.length() > 0) {
                final byte[] strBytes = this.message.getBytes(StandardCharsets.UTF_16LE);
                writer.writeBytes(strBytes, 0, strBytes.length);
            }
        }
        if (this.serverName == null) {
            writer.writeByte((byte)0);
        }
        else {
            writer.writeByte((byte)this.serverName.length());
            if (this.serverName.length() > 0) {
                final byte[] strBytes = this.serverName.getBytes(StandardCharsets.UTF_16LE);
                writer.writeBytes(strBytes, 0, strBytes.length);
            }
        }
        if (this.procedureName == null) {
            writer.writeByte((byte)0);
        }
        else {
            writer.writeByte((byte)this.procedureName.length());
            if (this.procedureName.length() > 0) {
                final byte[] strBytes = this.procedureName.getBytes(StandardCharsets.UTF_16LE);
                writer.writeBytes(strBytes, 0, strBytes.length);
            }
        }
        if (this.connectionState.tdsVersion72andHigher()) {
            writer.writeFourByteIntegerLow(this.lineNumber);
        }
        else {
            writer.writeTwoByteIntegerLow(this.lineNumber);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -86;
    }
    
    @Override
    public String getTokenTypeName() {
        return "Error";
    }
    
    public int getErrorNumber() {
        return this.errorNumber;
    }
    
    public void setErrorNumber(final int errorNumber) {
        this.errorNumber = errorNumber;
    }
    
    public byte getErrorState() {
        return this.errorState;
    }
    
    public void setErrorState(final byte errorState) {
        this.errorState = errorState;
    }
    
    public byte getErrorClass() {
        return this.errorClass;
    }
    
    public void setErrorClass(final byte errorClass) {
        this.errorClass = errorClass;
    }
    
    public String getMessage() {
        return this.message;
    }
    
    public void setMessage(final String message) {
        this.message = message;
    }
    
    public String getServerName() {
        return this.serverName;
    }
    
    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }
    
    public String getProcedureName() {
        return this.procedureName;
    }
    
    public void setProcedureName(final String procedureName) {
        this.procedureName = procedureName;
    }
    
    public int getLineNumber() {
        return this.lineNumber;
    }
    
    public void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "errorNumber": {
                return this.errorNumber;
            }
            case "errorState": {
                return this.errorState;
            }
            case "errorClass": {
                return this.errorClass;
            }
            case "message": {
                return this.message;
            }
            case "serverName": {
                return this.serverName;
            }
            case "procedureName": {
                return this.procedureName;
            }
            case "lineNumber": {
                return this.lineNumber;
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
        return new String[] { "errorNumber", "errorState", "errorClass", "message", "serverName", "procedureName", "lineNumber", "remove", "toString" };
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "errorNumber":
            case "errorState":
            case "errorClass":
            case "message":
            case "serverName":
            case "procedureName":
            case "lineNumber":
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
            case "errorNumber": {
                this.setErrorNumber(value.asInt());
                break;
            }
            case "errorState": {
                this.setErrorState(value.asByte());
                break;
            }
            case "errorClass": {
                this.setErrorClass(value.asByte());
                break;
            }
            case "message": {
                this.setMessage(value.asString());
                break;
            }
            case "serverName": {
                this.setServerName(value.asString());
                break;
            }
            case "procedureName": {
                this.setProcedureName(value.asString());
                break;
            }
            case "lineNumber": {
                this.setLineNumber(value.asInt());
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "Error token" });
    }
}
