// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.handler.mssql.ConnectionState;
import org.graalvm.polyglot.proxy.ProxyObject;
import com.galliumdata.server.handler.GenericPacket;

import java.util.function.Function;

public abstract class MessageToken implements GenericPacket, ProxyObject
{
    protected ConnectionState connectionState;
    protected boolean modified;
    protected boolean removed;
    private static final Logger log;
    
    public MessageToken(final ConnectionState connectionState) {
        this.connectionState = connectionState;
    }
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        throw new RuntimeException("Should no longer get called");
    }
    
    public void read(final RawPacketReader reader) {
        throw new RuntimeException("Not yet implemented");
    }
    
    public int getSerializedSize() {
        return 1;
    }
    
    public void write(final RawPacketWriter writer) {
        final byte tt = this.getTokenType();
        writer.writeByte(this.getTokenType());
    }
    
    public abstract byte getTokenType();
    
    public abstract String getTokenTypeName();
    
    public MessageToken duplicate() {
        throw new RuntimeException("Not implemented: MessageToken.duplicate");
    }
    
    @Override
    public String toString() {
        return "Token " + this.getTokenTypeName();
    }
    
    public String toLongString() {
        return this.toString();
    }
    
    @Override
    public boolean isModified() {
        return this.modified;
    }
    
    @Override
    public boolean isRemoved() {
        return this.removed;
    }
    
    @Override
    public void remove() {
        this.removed = true;
    }
    
    @Override
    public String getPacketType() {
        return this.getTokenTypeName();
    }
    
    public static MessageToken createToken(final byte tokenType, final ConnectionState connectionState) {
        switch (tokenType) {
            case 121: {
                return new TokenReturnStatus(connectionState);
            }
            case -127: {
                return new TokenColMetadata(connectionState);
            }
            case -93: {
                return new TokenDataClassification(connectionState);
            }
            case -92: {
                return new TokenTabName(connectionState);
            }
            case -91: {
                return new TokenColInfo(connectionState);
            }
            case -87: {
                return new TokenOrder(connectionState);
            }
            case -86: {
                return new TokenError(connectionState);
            }
            case -85: {
                return new TokenInfo(connectionState);
            }
            case -84: {
                return new TokenReturnValue(connectionState);
            }
            case -83: {
                return new TokenLoginAck(connectionState);
            }
            case -82: {
                return new TokenFeatureExtAck(connectionState);
            }
            case -47: {
                return new TokenRow(connectionState);
            }
            case -46: {
                return new TokenNBCRow(connectionState);
            }
            case -29: {
                return new TokenEnvChange(connectionState);
            }
            case -28: {
                return new TokenSessionState(connectionState);
            }
            case -19: {
                return new TokenSSPI(connectionState);
            }
            case -18: {
                return new TokenFedAuthInfo(connectionState);
            }
            case -3: {
                return new TokenDone(connectionState);
            }
            case -2: {
                return new TokenDoneProc(connectionState);
            }
            case -1: {
                return new TokenDoneInProc(connectionState);
            }
            case 100: {
                MessageToken.log.debug(Markers.MSSQL, "RETURN token received");
                return null;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownTokenType", new Object[] { tokenType });
            }
        }
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "tokenType":
            case "typeCode": {
                return this.getTokenType();
            }
            case "tokenTypeName":
            case "packetType": {
                return this.getTokenTypeName();
            }
            case "remove": {
                return (Function<Value[],Object>) arguments -> {
                    this.remove();
                    return null;
                };
            }
            case "duplicate": {
                return (Function<Value[],Object>) arguments -> this.duplicate();
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
        return new String[] { "tokenType", "typeCode", "tokenTypeName", "packetType", "remove", "duplicate", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "tokenType":
            case "typeCode":
            case "tokenTypeName":
            case "packetType":
            case "remove":
            case "duplicate":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value value) {
        key.hashCode();
        throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "Token token" });
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
