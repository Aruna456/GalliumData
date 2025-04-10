// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.TypeInfo;
import org.graalvm.polyglot.proxy.ProxyObject;

public class CryptoMetadata implements ProxyObject
{
    private int ordinal;
    private int userType;
    private TypeInfo baseTypeInfo;
    private byte encryptionAlgorithm;
    private String algorithmName;
    private byte encryptionAlgorithmType;
    private byte normVersion;
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        this.ordinal = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
        idx += 2;
        this.userType = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        this.baseTypeInfo = new TypeInfo();
        idx += this.baseTypeInfo.readFromBytes(bytes, idx);
        this.encryptionAlgorithm = bytes[idx];
        ++idx;
        final byte nameLen = bytes[idx];
        ++idx;
        this.algorithmName = new String(bytes, idx, nameLen * 2, StandardCharsets.UTF_16LE);
        idx += nameLen * 2;
        this.encryptionAlgorithmType = bytes[idx];
        ++idx;
        this.normVersion = bytes[idx];
        return ++idx - offset;
    }
    
    public void read(final RawPacketReader reader) {
        this.ordinal = reader.readTwoByteIntLow();
        this.userType = reader.readFourByteIntLow();
        (this.baseTypeInfo = new TypeInfo()).read(reader);
        this.encryptionAlgorithm = reader.readByte();
        if (this.encryptionAlgorithm == 0) {
            final byte nameLen = reader.readByte();
            this.algorithmName = reader.readString(nameLen * 2);
        }
        this.encryptionAlgorithmType = reader.readByte();
        this.normVersion = reader.readByte();
    }
    
    public int getSerializedSize() {
        int size = 0;
        size += 2;
        size += 4;
        size += this.baseTypeInfo.getSerializedSize();
        ++size;
        size = ++size + this.algorithmName.length() * 2;
        ++size;
        return ++size;
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeTwoByteIntegerLow((short)this.ordinal);
        writer.writeFourByteIntegerLow(this.userType);
        this.baseTypeInfo.write(writer);
        writer.writeByte(this.encryptionAlgorithm);
        if (this.encryptionAlgorithm == 0) {
            writer.writeByte((byte)this.algorithmName.length());
            final byte[] strBytes = this.algorithmName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
        writer.writeByte(this.encryptionAlgorithmType);
        writer.writeByte(this.normVersion);
    }
    
    public int getOrdinal() {
        return this.ordinal;
    }
    
    public void setOrdinal(final int ordinal) {
        this.ordinal = ordinal;
    }
    
    public int getUserType() {
        return this.userType;
    }
    
    public void setUserType(final int userType) {
        this.userType = userType;
    }
    
    public TypeInfo getBaseTypeInfo() {
        return this.baseTypeInfo;
    }
    
    public void setBaseTypeInfo(final TypeInfo baseTypeInfo) {
        this.baseTypeInfo = baseTypeInfo;
    }
    
    public byte getEncryptionAlgorithm() {
        return this.encryptionAlgorithm;
    }
    
    public void setEncryptionAlgorithm(final byte encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }
    
    public String getAlgorithmName() {
        return this.algorithmName;
    }
    
    public void setAlgorithmName(final String algorithmName) {
        this.algorithmName = algorithmName;
    }
    
    public byte getEncryptionAlgorithmType() {
        return this.encryptionAlgorithmType;
    }
    
    public void setEncryptionAlgorithmType(final byte encryptionAlgorithmType) {
        this.encryptionAlgorithmType = encryptionAlgorithmType;
    }
    
    public byte getNormVersion() {
        return this.normVersion;
    }
    
    public void setNormVersion(final byte normVersion) {
        this.normVersion = normVersion;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "ordinal": {
                return this.getOrdinal();
            }
            case "userType": {
                return this.getUserType();
            }
            case "baseTypeInfo": {
                return this.getBaseTypeInfo();
            }
            case "encryptionAlgorithm": {
                return this.getEncryptionAlgorithm();
            }
            case "algorithmName": {
                return this.getAlgorithmName();
            }
            case "encryptionAlgorithmType": {
                return this.getEncryptionAlgorithmType();
            }
            case "normVersion": {
                return this.getNormVersion();
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
        return new String[] { "ordinal", "userType", "baseTypeInfo", "encryptionAlgorithm", "algorithmName", "encryptionAlgorithmType", "normVersion", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "ordinal":
            case "userType":
            case "baseTypeInfo":
            case "encryptionAlgorithm":
            case "algorithmName":
            case "encryptionAlgorithmType":
            case "normVersion":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value val) {
        switch (key) {
            case "ordinal": {
                this.setOrdinal(val.asInt());
                break;
            }
            case "userType": {
                this.setUserType(val.asInt());
                break;
            }
            case "encryptionAlgorithm": {
                this.setEncryptionAlgorithm(val.asByte());
                break;
            }
            case "algorithmName": {
                this.setAlgorithmName(val.asString());
                break;
            }
            case "encryptionAlgorithmType": {
                this.setEncryptionAlgorithmType(val.asByte());
                break;
            }
            case "normVersion": {
                this.setNormVersion(val.asByte());
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "CryptoMetadata packet" });
    }
}
