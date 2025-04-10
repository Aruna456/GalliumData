// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.loginfeatures;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.function.Function;

public class Login7FeatureDataClassification extends Login7Feature implements ProxyObject
{
    private byte version;
    
    @Override
    public String getFeatureType() {
        return "DataClassification";
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        final int len = DataTypeReader.readFourByteIntegerLow(bytes, offset);
        if (len != 1) {
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { this.getFeatureType(), 1, len });
        }
        this.version = bytes[offset + 4];
        return 5;
    }
    
    @Override
    public int getSerializedSize() {
        return 6;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        writer.writeByte((byte)9);
        writer.writeFourByteIntegerLow(1);
        writer.writeByte(this.version);
    }
    
    public byte getVersion() {
        return this.version;
    }
    
    public void setVersion(final byte version) {
        this.version = version;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "version": {
                return this.version;
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
        return new String[] { "version", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "version":
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
            case "version": {
                this.setVersion(value.asByte());
                return;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "Login7 packet" });
    }
}
