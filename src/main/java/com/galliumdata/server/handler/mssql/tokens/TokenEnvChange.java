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
import java.util.function.Function;

import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenEnvChange extends MessageToken
{
    private byte type;
    private Object oldValue;
    private Object newValue;
    private byte[] padding;
    private byte protocol;
    private int protocolProperty;
    private String alternateServer;
    
    public TokenEnvChange(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.type = bytes[idx];
        final int idxBeforeValue = ++idx;
        switch (this.type) {
            case 1:
            case 2:
            case 3:
            case 4: {
                int len = bytes[idx] * 2;
                ++idx;
                this.newValue = new String(bytes, idx, len, StandardCharsets.UTF_16LE);
                idx += len;
                len = bytes[idx] * 2;
                ++idx;
                this.oldValue = new String(bytes, idx, len, StandardCharsets.UTF_16LE);
                idx += len;
                break;
            }
            case 5:
            case 6:
            case 13:
            case 19: {
                final int len = bytes[idx] * 2;
                ++idx;
                this.newValue = new String(bytes, idx + 1, len, StandardCharsets.UTF_16LE);
                idx += len;
                break;
            }
            case 7: {
                int len = bytes[idx];
                ++idx;
                System.arraycopy(bytes, idx, this.newValue = new byte[len], 0, len);
                idx += len;
                len = bytes[idx];
                ++idx;
                System.arraycopy(bytes, idx, this.oldValue = new byte[len], 0, len);
                idx += len;
                break;
            }
            case 8:
            case 12:
            case 16: {
                final int len = bytes[idx];
                ++idx;
                System.arraycopy(bytes, idx, this.newValue = new byte[len], 0, len);
                idx += len;
                break;
            }
            case 9:
            case 10:
            case 11:
            case 17: {
                final int len = bytes[idx];
                ++idx;
                System.arraycopy(bytes, idx, this.oldValue = new byte[len], 0, len);
                idx += len;
                break;
            }
            case 15: {
                final int len = DataTypeReader.readFourByteIntegerLow(bytes, idx);
                idx += 4;
                System.arraycopy(bytes, idx, this.newValue = new byte[len], 0, len);
                idx += len;
                break;
            }
            case 18: {
                break;
            }
            case 20: {
                final int routingSize = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
                idx += 2;
                this.protocol = bytes[idx];
                ++idx;
                this.protocolProperty = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
                idx += 2;
                final int strLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
                idx += 2;
                this.alternateServer = new String(bytes, idx, strLen * 2, StandardCharsets.UTF_16LE);
                idx += strLen * 2;
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownEnvChangeType", new Object[] { this.type });
            }
        }
        idx = idxBeforeValue + length - 1;
        if (idx - offset - 3 != length) {
            throw new ServerException("db.mssql.protocol.UnexpectedTokenSize", new Object[] { this.getTokenTypeName(), length, idx - offset - 3 });
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final short length = reader.readTwoByteIntLow();
        reader.resetMarker();
        switch (this.type = reader.readByte()) {
            case 1:
            case 2:
            case 3:
            case 4: {
                int len = reader.readByte();
                this.newValue = reader.readString(len * 2);
                len = reader.readByte();
                this.oldValue = reader.readString(len * 2);
                break;
            }
            case 5:
            case 6:
            case 13:
            case 19: {
                final int len = reader.readByte();
                this.newValue = reader.readString(len * 2);
                break;
            }
            case 7: {
                int len = reader.readByte();
                this.newValue = reader.readBytes(len);
                len = reader.readByte();
                this.oldValue = reader.readBytes(len);
                break;
            }
            case 8:
            case 12:
            case 16: {
                final int len = reader.readByte();
                this.newValue = reader.readBytes(len);
                break;
            }
            case 9:
            case 10:
            case 11:
            case 17: {
                int len = reader.readByte();
                if (len != 0) {
                    throw new ServerException("db.mssql.protocol.ErrorInEnvChange", new Object[] { "Commit Transaction newValue != 0" });
                }
                len = reader.readByte();
                this.oldValue = reader.readBytes(len);
                break;
            }
            case 15: {
                final int len = reader.readFourByteIntLow();
                this.newValue = reader.readBytes(len);
                break;
            }
            case 18: {
                break;
            }
            case 20: {
                final int routingSize = reader.readTwoByteIntLow();
                this.protocol = reader.readByte();
                this.protocolProperty = reader.readTwoByteIntLow();
                final int strLen = reader.readTwoByteIntLow();
                this.alternateServer = reader.readString(strLen);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownEnvChangeType", new Object[] { this.type });
            }
        }
        final int numWritten = reader.getMarker();
        if (numWritten < length) {
            this.padding = reader.readBytes(length - numWritten);
        }
        if (numWritten > length) {
            throw new ServerException("db.mssql.protocol.InternalError", new Object[] { "EnvChange token wrote too many bytes" });
        }
        if (this.type == 4) {
            final int newPacketSize = Integer.parseInt(this.newValue.toString());
            this.connectionState.setPacketSize(newPacketSize);
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        ++size;
        switch (this.type) {
            case 1:
            case 2:
            case 3:
            case 4: {
                size += 1 + this.newValue.toString().length() * 2;
                size += 1 + this.oldValue.toString().length() * 2;
                break;
            }
            case 5:
            case 6:
            case 13:
            case 19: {
                size += 1 + this.newValue.toString().length() * 2;
                break;
            }
            case 7: {
                size += 1 + ((byte[])this.newValue).length;
                size += 1 + ((byte[])this.oldValue).length;
                break;
            }
            case 8:
            case 12:
            case 16: {
                size += 1 + ((byte[])this.newValue).length;
                break;
            }
            case 9:
            case 10:
            case 11:
            case 17: {
                ++size;
                size = ++size + ((byte[])this.oldValue).length;
                break;
            }
            case 15: {
                size += 4 + ((byte[])this.newValue).length;
                break;
            }
            case 18: {
                break;
            }
            case 20: {
                size += 2;
                ++size;
                size += 2;
                size += 2 + this.alternateServer.length() * 2;
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownEnvChangeType", new Object[] { this.type });
            }
        }
        if (this.padding != null) {
            size += this.padding.length;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow((short)(this.getSerializedSize() - 3));
        writer.writeByte(this.type);
        switch (this.type) {
            case 1:
            case 2:
            case 3:
            case 4: {
                byte[] valBytes = ((String)this.newValue).getBytes(StandardCharsets.UTF_16LE);
                writer.writeByte((byte)(valBytes.length / 2));
                writer.writeBytes(valBytes, 0, valBytes.length);
                valBytes = ((String)this.oldValue).getBytes(StandardCharsets.UTF_16LE);
                writer.writeByte((byte)(valBytes.length / 2));
                writer.writeBytes(valBytes, 0, valBytes.length);
                break;
            }
            case 5:
            case 6:
            case 13:
            case 19: {
                final byte[] valBytes = ((String)this.newValue).getBytes(StandardCharsets.UTF_16LE);
                writer.writeByte((byte)(valBytes.length / 2));
                writer.writeBytes(valBytes, 0, valBytes.length);
                break;
            }
            case 7: {
                byte[] valBytes = (byte[])this.newValue;
                writer.writeByte((byte)valBytes.length);
                writer.writeBytes(valBytes, 0, valBytes.length);
                valBytes = (byte[])this.oldValue;
                writer.writeByte((byte)valBytes.length);
                writer.writeBytes(valBytes, 0, valBytes.length);
                break;
            }
            case 8:
            case 12:
            case 16: {
                final byte[] valBytes = (byte[])this.newValue;
                writer.writeByte((byte)valBytes.length);
                writer.writeBytes(valBytes, 0, valBytes.length);
                break;
            }
            case 9:
            case 10:
            case 11:
            case 17: {
                writer.writeByte((byte)0);
                final byte[] valBytes = (byte[])this.oldValue;
                writer.writeByte((byte)valBytes.length);
                writer.writeBytes(valBytes, 0, valBytes.length);
                break;
            }
            case 15: {
                final byte[] valBytes = (byte[])this.newValue;
                writer.writeByte((byte)valBytes.length);
                writer.writeBytes(valBytes, 0, valBytes.length);
                break;
            }
            case 18: {
                writer.writeByte((byte)0);
                writer.writeByte((byte)0);
                break;
            }
            case 20: {
                writer.writeByte(this.protocol);
                writer.writeTwoByteIntegerLow((short)this.protocolProperty);
                final byte[] valBytes = this.alternateServer.getBytes(StandardCharsets.UTF_16LE);
                writer.writeTwoByteIntegerLow((short)(valBytes.length / 2));
                writer.writeBytes(valBytes, 0, valBytes.length);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownEnvChangeType", new Object[] { this.type });
            }
        }
        if (this.padding != null) {
            writer.writeBytes(this.padding, 0, this.padding.length);
        }
    }
    
    @Override
    public byte getTokenType() {
        return -29;
    }
    
    @Override
    public String getTokenTypeName() {
        return "EnvChange";
    }
    
    @Override
    public String toString() {
        String oldValueStr = "<null>";
        if (this.oldValue != null) {
            if (this.oldValue instanceof String) {
                oldValueStr = this.oldValue.toString();
            }
            else {
                final byte[] oldValueBytes = (byte[])this.oldValue;
                oldValueStr = "byte[" + oldValueBytes.length;
            }
        }
        String newValueStr = "<null>";
        if (this.newValue != null) {
            if (this.newValue instanceof String) {
                newValueStr = this.newValue.toString();
            }
            else {
                final byte[] newValueBytes = (byte[])this.newValue;
                newValueStr = "byte[" + newValueBytes.length;
            }
        }
        return "TokenEnvChange: " + this.getTypeName() + " - " + oldValueStr + "->" + newValueStr;
    }
    
    public byte getType() {
        return this.type;
    }
    
    public void setType(final byte type) {
        this.type = type;
    }
    
    public String getTypeName() {
        switch (this.type) {
            case 1: {
                return "Database";
            }
            case 2: {
                return "Language";
            }
            case 3: {
                return "Character set";
            }
            case 4: {
                return "Packet size";
            }
            case 5: {
                return "Unicode data sorting local id";
            }
            case 6: {
                return "Unicode data sorting comparison flags";
            }
            case 7: {
                return "SQL collation";
            }
            case 8: {
                return "Begin transaction";
            }
            case 9: {
                return "Commit transaction";
            }
            case 10: {
                return "Rollback transaction";
            }
            case 11: {
                return "Enlist DTC transaction";
            }
            case 12: {
                return "Defect transaction";
            }
            case 13: {
                return "Real time log shipping";
            }
            case 15: {
                return "Promote transaction";
            }
            case 16: {
                return "Transaction manager address";
            }
            case 17: {
                return "Transaction ended";
            }
            case 18: {
                return "RESETCONNECTION/RESETCONNECTIONSKIPTRAN completion acknowledgement";
            }
            case 19: {
                return "Name of user instance started per login request";
            }
            case 20: {
                return "Routing information";
            }
            default: {
                return "Unknown: " + this.type;
            }
        }
    }
    
    public Object getOldValue() {
        return this.oldValue;
    }
    
    public void setOldValue(final Object oldValue) {
        this.oldValue = oldValue;
    }
    
    public Object getNewValue() {
        return this.newValue;
    }
    
    public void setNewValue(final Object newValue) {
        this.newValue = newValue;
    }
    
    public byte getProtocol() {
        return this.protocol;
    }
    
    public void setProtocol(final byte protocol) {
        this.protocol = protocol;
    }
    
    public int getProtocolProperty() {
        return this.protocolProperty;
    }
    
    public void setProtocolProperty(final int protocolProperty) {
        this.protocolProperty = protocolProperty;
    }
    
    public String getAlternateServer() {
        return this.alternateServer;
    }
    
    public void setAlternateServer(final String alternateServer) {
        this.alternateServer = alternateServer;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "changeType": {
                return this.type;
            }
            case "changeTypeName": {
                return this.getTypeName();
            }
            case "oldValue": {
                return this.oldValue;
            }
            case "newValue": {
                return this.newValue;
            }
            case "protocol": {
                return this.protocol;
            }
            case "protocolProperty": {
                return this.protocolProperty;
            }
            case "alternateServer": {
                return this.alternateServer;
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
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        final String[] parentKeys = (String[])super.getMemberKeys();
        final List<String> keys = Arrays.asList(parentKeys);
        keys.add("changeType");
        keys.add("changeTypeName");
        keys.add("oldValue");
        keys.add("newValue");
        keys.add("protocol");
        keys.add("protocolProperty");
        keys.add("alternateServer");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "changeType":
            case "changeTypeName":
            case "oldValue":
            case "newValue":
            case "protocol":
            case "protocolProperty":
            case "alternateServer":
            case "remove":
            case "toString": {
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
            case "changeType": {
                this.setType(value.asByte());
                break;
            }
            case "oldValue": {
                this.setOldValue(value.asHostObject());
                break;
            }
            case "newValue": {
                this.setNewValue(value.asHostObject());
                break;
            }
            case "protocol": {
                this.setProtocol(value.asByte());
                break;
            }
            case "protocolProperty": {
                this.setProtocolProperty(value.asInt());
                break;
            }
            case "alternateServer": {
                this.setAlternateServer(value.asString());
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
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "EnvChange token" });
    }
}
