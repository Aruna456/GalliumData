// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import org.graalvm.polyglot.Value;
import java.util.List;
import java.util.Arrays;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenInfo extends MessageToken
{
    private int infoNumber;
    private byte state;
    private byte infoClass;
    private String message;
    private String serverName;
    private String procedureName;
    private int lineNumber;
    
    public TokenInfo(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.infoNumber = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.state = bytes[idx];
        ++idx;
        this.infoClass = bytes[idx];
        ++idx;
        final int msgLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.message = new String(bytes, idx, msgLen * 2, StandardCharsets.UTF_16LE);
        idx += msgLen * 2;
        final int serverNameLen = bytes[idx];
        ++idx;
        this.serverName = new String(bytes, idx, serverNameLen * 2, StandardCharsets.UTF_16LE);
        idx += serverNameLen * 2;
        final int procNameLen = bytes[idx];
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
        this.infoNumber = reader.readFourByteIntLow();
        this.state = reader.readByte();
        this.infoClass = reader.readByte();
        final int msgLen = reader.readTwoByteIntLow();
        this.message = reader.readString(msgLen * 2);
        final int serverNameLen = reader.readByte();
        this.serverName = reader.readString(serverNameLen * 2);
        final int procNameLen = reader.readByte();
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
        size += this.message.length() * 2;
        size = ++size + this.serverName.length() * 2;
        size = ++size + this.procedureName.length() * 2;
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
        writer.writeFourByteIntegerLow(this.infoNumber);
        writer.writeByte(this.state);
        writer.writeByte(this.infoClass);
        byte[] strBytes = this.message.getBytes(StandardCharsets.UTF_16LE);
        writer.writeTwoByteIntegerLow((short)(strBytes.length / 2));
        writer.writeBytes(strBytes, 0, strBytes.length);
        strBytes = this.serverName.getBytes(StandardCharsets.UTF_16LE);
        writer.writeByte((byte)(strBytes.length / 2));
        writer.writeBytes(strBytes, 0, strBytes.length);
        strBytes = this.procedureName.getBytes(StandardCharsets.UTF_16LE);
        writer.writeByte((byte)(strBytes.length / 2));
        writer.writeBytes(strBytes, 0, strBytes.length);
        if (this.connectionState.tdsVersion72andHigher()) {
            writer.writeFourByteIntegerLow(this.lineNumber);
        }
        else {
            writer.writeTwoByteIntegerLow((short)this.lineNumber);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -85;
    }
    
    @Override
    public String getTokenTypeName() {
        return "Info";
    }
    
    @Override
    public String toString() {
        return "Info: " + this.message;
    }
    
    public int getInfoNumber() {
        return this.infoNumber;
    }
    
    public void setInfoNumber(final int infoNumber) {
        this.infoNumber = infoNumber;
    }
    
    public byte getState() {
        return this.state;
    }
    
    public void setState(final byte state) {
        this.state = state;
    }
    
    public byte getInfoClass() {
        return this.infoClass;
    }
    
    public void setInfoClass(final byte infoClass) {
        this.infoClass = infoClass;
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
            case "infoNumber": {
                return this.infoNumber;
            }
            case "state": {
                return this.state;
            }
            case "infoClass": {
                return this.infoClass;
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
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        final String[] parentKeys = (String[])super.getMemberKeys();
        final List<String> keys = Arrays.asList(parentKeys);
        keys.add("infoNumber");
        keys.add("state");
        keys.add("infoClass");
        keys.add("message");
        keys.add("serverName");
        keys.add("procedureName");
        keys.add("lineNumber");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "infoNumber":
            case "state":
            case "infoClass":
            case "message":
            case "serverName":
            case "procedureName":
            case "lineNumber": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "infoNumber": {
                this.setInfoNumber(value.asInt());
                break;
            }
            case "state": {
                this.setState(value.asByte());
                break;
            }
            case "infoClass": {
                this.setInfoClass(value.asByte());
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
                super.putMember(key, value);
                break;
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "Info token" });
    }
}
