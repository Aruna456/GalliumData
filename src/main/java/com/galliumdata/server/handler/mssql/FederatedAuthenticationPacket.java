// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.function.Function;

public class FederatedAuthenticationPacket extends MSSQLPacket implements ProxyObject
{
    private byte[] payload;
    private byte[] nonce;
    private static final int NONCE_SIZE = 32;
    
    public FederatedAuthenticationPacket(final ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = 8;
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        final int totalLength = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        final int payloadLength = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (payloadLength > 0) {
            System.arraycopy(bytes, idx, this.payload = new byte[payloadLength], 0, payloadLength);
            idx += payloadLength;
        }
        if (totalLength >= payloadLength + 32) {
            System.arraycopy(bytes, idx, this.nonce = new byte[32], 0, 32);
            idx += 32;
        }
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = 4;
        size += 4;
        if (this.payload != null) {
            size += this.payload.length;
        }
        if (this.nonce != null) {
            size += 32;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        int size = 4;
        if (this.payload != null) {
            size += this.payload.length;
        }
        if (this.nonce != null) {
            size += 32;
        }
        writer.writeFourByteIntegerLow(size);
        if (this.payload != null) {
            writer.writeBytes(this.payload, 0, this.payload.length);
        }
        if (this.nonce != null) {
            writer.writeBytes(this.nonce, 0, this.nonce.length);
        }
    }
    
    public String getPacketType() {
        return "FederatedAuthentication";
    }
    
    @Override
    public String toString() {
        return "Federated authentication: " + ((this.payload == null) ? "[empty]" : ("[" + this.payload.length + " bytes]"));
    }
    
    public byte[] getPayload() {
        return this.payload;
    }
    
    public void setPayload(final byte[] payload) {
        this.payload = payload;
    }
    
    public byte[] getNonce() {
        return this.nonce;
    }
    
    public void setNonce(final byte[] nonce) {
        this.nonce = nonce;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "payload": {
                return this.getPayload();
            }
            case "nonce": {
                return this.getNonce();
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
        return new String[] { "payload", "nonce", "packetType", "remove", "toString" };
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "payload":
            case "nonce":
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
            case "payload": {
                if (!value.hasArrayElements()) {
                    throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "byte array", value });
                }
                final int arraySize = (int)value.getArraySize();
                final byte[] newPayload = new byte[arraySize];
                for (int i = 0; i < arraySize; ++i) {
                    final Value elem = value.getArrayElement((long)i);
                    if (!elem.fitsInByte()) {
                        throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "byte", elem });
                    }
                    newPayload[i] = elem.asByte();
                }
                this.setPayload(newPayload);
                break;
            }
            case "nonce": {
                if (!value.hasArrayElements()) {
                    throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "byte array", value });
                }
                final int arraySize = (int)value.getArraySize();
                final byte[] newNonce = new byte[arraySize];
                for (int j = 0; j < arraySize; ++j) {
                    final Value elem2 = value.getArrayElement((long)j);
                    if (!elem2.fitsInByte()) {
                        throw new ServerException("db.mssql.logic.ValueHasWrongType", new Object[] { "byte", elem2 });
                    }
                    newNonce[j] = elem2.asByte();
                }
                this.setNonce(newNonce);
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "FederatedAuthentication packet" });
    }
}
