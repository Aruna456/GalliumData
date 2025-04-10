// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.ProtocolException;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.proxy.ProxyObject;
import com.galliumdata.server.handler.GenericPacket;

import java.util.function.Function;

public abstract class MSSQLPacket implements GenericPacket, ProxyObject
{
    protected ConnectionState connectionState;
    protected byte typeCode;
    private boolean statusEndOfMessage;
    private boolean statusIgnoreThisEvent;
    private boolean statusEventNotification;
    private boolean statusResetConnection;
    private boolean statusResetConnectionSkipTran;
    private boolean status5;
    private boolean status6;
    private boolean status7;
    protected int length;
    protected int totalLength;
    private short spid;
    private byte packetId;
    private byte window;
    private boolean removed;
    private boolean modified;
    protected int smpSessionId;
    protected static final Logger log;
    
    public MSSQLPacket(final ConnectionState connState) {
        this.smpSessionId = -1;
        this.connectionState = connState;
    }
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        if (null == bytes) {
            throw new ProtocolException("db.mssql.protocol.ProtocolViolation", new Object[] { "no bytes provided for packet of type " + this.getPacketType() + " (from MSSQLPacket)" });
        }
        int idx = offset;
        this.typeCode = DataTypeReader.readByte(bytes, idx);
        ++idx;
        final byte status = DataTypeReader.readByte(bytes, idx);
        ++idx;
        this.statusEndOfMessage = ((status & 0x1) != 0x0);
        this.statusIgnoreThisEvent = ((status & 0x2) != 0x0);
        this.statusEventNotification = ((status & 0x4) != 0x0);
        this.statusResetConnection = ((status & 0x8) != 0x0);
        this.statusResetConnectionSkipTran = ((status & 0x10) != 0x0);
        this.status5 = ((status & 0x20) != 0x0);
        this.status6 = ((status & 0x40) != 0x0);
        this.status7 = ((status & 0x80) != 0x0);
        if (this.totalLength == 0) {
            this.length = DataTypeReader.readTwoByteInteger(bytes, idx);
            if (this.typeCode == 1 && numBytes > this.length) {
                this.length = numBytes;
            }
        }
        else {
            this.length = this.totalLength;
        }
        idx += 2;
        this.spid = DataTypeReader.readTwoByteInteger(bytes, idx);
        idx += 2;
        this.packetId = DataTypeReader.readByte(bytes, idx);
        ++idx;
        this.window = DataTypeReader.readByte(bytes, idx);
        return ++idx - offset;
    }
    
    public void read(final RawPacketReader reader) {
        if (reader.getCurrentPacket() != null && reader.getCurrentPacket().isWrappedInSMP()) {
            this.setSMPSessionId(reader.getCurrentPacket().getSMPSessionId());
        }
        this.typeCode = reader.readByte();
        final byte status = reader.readByte();
        this.statusEndOfMessage = ((status & 0x1) != 0x0);
        this.statusIgnoreThisEvent = ((status & 0x2) != 0x0);
        this.statusEventNotification = ((status & 0x4) != 0x0);
        this.statusResetConnection = ((status & 0x8) != 0x0);
        this.statusResetConnectionSkipTran = ((status & 0x10) != 0x0);
        this.status5 = ((status & 0x20) != 0x0);
        this.status6 = ((status & 0x40) != 0x0);
        this.status7 = ((status & 0x80) != 0x0);
        this.length = reader.readTwoByteInt();
        this.spid = reader.readTwoByteInt();
        this.packetId = reader.readByte();
        this.window = reader.readByte();
    }
    
    public int readFromRawPacket(final RawPacket rawPkt) {
        return this.readFromBytes(rawPkt.getBuffer(), 0, rawPkt.getBuffer().length);
    }
    
    public int getSerializedSize() {
        return 8;
    }
    
    public final int writeHeaderToBytes(final byte[] buffer, final int offset) {
        int idx = offset;
        buffer[idx] = this.typeCode;
        ++idx;
        buffer[idx] = this.getStatus();
        ++idx;
        DataTypeWriter.encodeTwoByteInteger(buffer, idx, (short)this.getSerializedSize());
        idx += 2;
        DataTypeWriter.encodeTwoByteInteger(buffer, idx, this.spid);
        idx += 2;
        buffer[idx] = this.packetId;
        ++idx;
        buffer[idx] = this.window;
        return ++idx - offset;
    }
    
    public byte getStatus() {
        byte status = 0;
        if (this.statusEndOfMessage) {
            status |= 0x1;
        }
        if (this.statusIgnoreThisEvent) {
            status |= 0x2;
        }
        if (this.statusEventNotification) {
            status |= 0x4;
        }
        if (this.statusResetConnection) {
            status |= 0x8;
        }
        if (this.statusResetConnectionSkipTran) {
            status |= 0x10;
        }
        if (this.status5) {
            status |= 0x20;
        }
        if (this.status6) {
            status |= 0x40;
        }
        if (this.status7) {
            status |= (byte)128;
        }
        return status;
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeByte(this.typeCode);
        writer.writeByte(this.getStatus());
        writer.writeTwoByteIntegerLow(0);
        writer.writeTwoByteInteger(this.spid);
        writer.writeByte((byte)0);
        writer.writeByte(this.window);
    }
    
    @Override
    public void remove() {
        this.removed = true;
    }
    
    @Override
    public boolean isRemoved() {
        return this.removed;
    }
    
    public void setModified() {
        this.modified = true;
    }
    
    @Override
    public boolean isModified() {
        return this.modified;
    }
    
    public void copyHeadersFrom(final MSSQLPacket otherPacket) {
    }
    
    @Override
    public String toString() {
        return "Packet " + this.getPacketType();
    }
    
    public String toLongString() {
        return this.toString();
    }
    
    public boolean isStatusEndOfMessage() {
        return this.statusEndOfMessage;
    }
    
    public void setStatusEndOfMessage(final boolean statusEndOfMessage) {
        this.statusEndOfMessage = statusEndOfMessage;
    }
    
    public boolean isStatusIgnoreThisEvent() {
        return this.statusIgnoreThisEvent;
    }
    
    public void setStatusIgnoreThisEvent(final boolean statusIgnoreThisEvent) {
        this.statusIgnoreThisEvent = statusIgnoreThisEvent;
    }
    
    public boolean isStatusEventNotification() {
        return this.statusEventNotification;
    }
    
    public void setStatusEventNotification(final boolean statusEventNotification) {
        this.statusEventNotification = statusEventNotification;
    }
    
    public boolean isStatusResetConnection() {
        return this.statusResetConnection;
    }
    
    public void setStatusResetConnection(final boolean statusResetConnection) {
        this.statusResetConnection = statusResetConnection;
    }
    
    public boolean isStatusResetConnectionSkipTran() {
        return this.statusResetConnectionSkipTran;
    }
    
    public void setStatusResetConnectionSkipTran(final boolean statusResetConnectionSkipTran) {
        this.statusResetConnectionSkipTran = statusResetConnectionSkipTran;
    }
    
    public short getSpid() {
        return this.spid;
    }
    
    public void setSpid(final short spid) {
        this.spid = spid;
        this.setModified();
    }
    
    public byte getPacketId() {
        return this.packetId;
    }
    
    public void setPacketId(final byte packetId) {
        this.packetId = packetId;
        this.setModified();
    }
    
    public byte getTypeCode() {
        return this.typeCode;
    }
    
    public void setTypeCode(final byte code) {
        this.typeCode = code;
        this.setModified();
    }
    
    public byte getWindow() {
        return this.window;
    }
    
    public void setWindow(final byte window) {
        this.window = window;
        this.setModified();
    }
    
    public boolean isWrappedInSMP() {
        return this.smpSessionId != -1;
    }
    
    public int getSMPSessionId() {
        return this.smpSessionId;
    }
    
    public void setSMPSessionId(final int id) {
        this.smpSessionId = id;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "typeCode": {
                return this.typeCode;
            }
            case "packetType": {
                return this.getPacketType();
            }
            case "statusEndOfMessage": {
                return this.statusEndOfMessage;
            }
            case "statusIgnoreThisEvent": {
                return this.statusIgnoreThisEvent;
            }
            case "statusEventNotification": {
                return this.statusEventNotification;
            }
            case "statusResetConnection": {
                return this.statusResetConnection;
            }
            case "statusResetConnectionSkipTran": {
                return this.statusResetConnectionSkipTran;
            }
            case "spid": {
                return this.spid;
            }
            case "packetId": {
                return this.packetId;
            }
            case "window": {
                return this.window;
            }
            case "remove": {
                return(Function<Value[],Object>) arguments -> {
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
    
    public Object getMemberKeys() {
        return new String[] { "typeCode", "packetType", "statusEndOfMessage", "statusIgnoreThisEvent", "statusEventNotification", "statusResetConnection", "statusResetConnectionSkipTran", "spid", "packetId", "window", "remove", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "typeCode":
            case "packetType":
            case "statusEndOfMessage":
            case "statusIgnoreThisEvent":
            case "statusEventNotification":
            case "statusResetConnection":
            case "statusResetConnectionSkipTran":
            case "spid":
            case "packetId":
            case "window":
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
            case "statusEndOfMessage": {
                this.setStatusEndOfMessage(value.asBoolean());
                break;
            }
            case "statusIgnoreThisEvent": {
                this.setStatusIgnoreThisEvent(value.asBoolean());
                break;
            }
            case "statusResetConnection": {
                this.setStatusResetConnection(value.asBoolean());
                break;
            }
            case "statusEventNotification": {
                this.setStatusEventNotification(value.asBoolean());
                break;
            }
            case "statusResetConnectionSkipTran": {
                this.setStatusResetConnectionSkipTran(value.asBoolean());
                break;
            }
            case "spid": {
                this.setSpid(value.asShort());
                break;
            }
            case "packetId": {
                this.setPacketId(value.asByte());
                break;
            }
            case "window": {
                this.setWindow(value.asByte());
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, this.getPacketType() + " packet" });
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
