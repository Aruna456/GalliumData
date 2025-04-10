// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import org.graalvm.polyglot.proxy.ProxyObject;

public class RPCParameter implements ProxyObject
{
    private ConnectionState connectionState;
    private final ColumnMetadata columnMetaData;
    private String paramName;
    private boolean fByRefValue;
    private boolean fDefaultValue;
    private boolean fEncrypted;
    private final TypeInfo typeInfo;
    private TVPTypeInfo tvpTypeInfo;
    private MSSQLDataType value;
    
    public RPCParameter(final ConnectionState connectionState) {
        this.columnMetaData = new ColumnMetadata(this.connectionState);
        this.typeInfo = new TypeInfo();
        this.connectionState = connectionState;
    }
    
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final byte nameLen = bytes[idx];
        ++idx;
        if (nameLen > 0) {
            this.paramName = new String(bytes, idx, nameLen * 2, StandardCharsets.UTF_16LE);
            idx += nameLen * 2;
        }
        final byte statusFlags = bytes[idx];
        ++idx;
        this.fByRefValue = ((statusFlags & 0x1) > 0);
        this.fDefaultValue = ((statusFlags & 0x2) > 0);
        this.fEncrypted = ((statusFlags & 0x8) > 0);
        final byte paramType = bytes[idx];
        if (paramType == -13) {
            this.tvpTypeInfo = new TVPTypeInfo();
            idx += this.tvpTypeInfo.readFromBytes(bytes, idx);
        }
        else {
            idx += this.typeInfo.readFromBytes(bytes, idx);
            this.columnMetaData.setTypeInfo(this.typeInfo);
            (this.value = MSSQLDataType.createDataType(this.columnMetaData)).setResizable(true);
            idx += this.value.readFromBytes(bytes, idx);
            this.columnMetaData.setColumnName(this.paramName);
            this.columnMetaData.setTypeInfo(this.typeInfo);
            this.columnMetaData.setConnectionState(this.connectionState);
        }
        return idx - offset;
    }
    
    public int getSerializedSize() {
        int size = 1;
        if (this.paramName != null) {
            size += this.paramName.length() * 2;
        }
        size = ++size + this.typeInfo.getSerializedSize();
        size += this.value.getSerializedSize();
        return size;
    }
    
    public void write(final RawPacketWriter writer) {
        if (this.paramName == null) {
            writer.writeByte((byte)0);
        }
        else {
            writer.writeByte((byte)this.paramName.length());
            final byte[] strBytes = this.paramName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
        byte statusFlags = 0;
        if (this.fByRefValue) {
            statusFlags |= 0x1;
        }
        if (this.fDefaultValue) {
            statusFlags |= 0x2;
        }
        if (this.fEncrypted) {
            statusFlags |= 0x8;
        }
        writer.writeByte(statusFlags);
        if (this.tvpTypeInfo != null) {
            this.tvpTypeInfo.write(writer);
        }
        else {
            this.typeInfo.write(writer);
            this.value.write(writer);
        }
    }
    
    @Override
    public String toString() {
        String s = (this.paramName == null) ? "<no name>" : this.paramName;
        if (this.tvpTypeInfo != null) {
            s = s + ": TVP - " + this.tvpTypeInfo.getTvpTypeName() + " - " + this.tvpTypeInfo.getColumnMetas().size() + " columns, " + this.tvpTypeInfo.getTvpRows().size() + " rows";
            return s;
        }
        return s + "=" + String.valueOf(this.getValue());
    }
    
    public ColumnMetadata getColumnMetaData() {
        return this.columnMetaData;
    }
    
    public String getParamName() {
        return this.paramName;
    }
    
    public void setParamName(final String paramName) {
        this.paramName = paramName;
    }
    
    public boolean isfByRefValue() {
        return this.fByRefValue;
    }
    
    public void setfByRefValue(final boolean fByRefValue) {
        this.fByRefValue = fByRefValue;
    }
    
    public boolean isfDefaultValue() {
        return this.fDefaultValue;
    }
    
    public void setfDefaultValue(final boolean fDefaultValue) {
        this.fDefaultValue = fDefaultValue;
    }
    
    public boolean isfEncrypted() {
        return this.fEncrypted;
    }
    
    public void setfEncrypted(final boolean fEncrypted) {
        this.fEncrypted = fEncrypted;
    }
    
    public TypeInfo getTypeInfo() {
        return this.typeInfo;
    }
    
    public MSSQLDataType getValue() {
        return this.value;
    }
    
    public void setValue(final MSSQLDataType value) {
        this.value = value;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "columnMetaData": {
                return this.columnMetaData;
            }
            case "paramName": {
                return this.paramName;
            }
            case "fByRefValue": {
                return this.fByRefValue;
            }
            case "fDefaultValue": {
                return this.fDefaultValue;
            }
            case "fEncrypted": {
                return this.fEncrypted;
            }
            case "typeInfo": {
                return this.typeInfo;
            }
            case "value": {
                return this.value.getValue();
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
        return new String[] { "columnMetaData", "paramName", "fByRefValue", "fDefaultValue", "fEncrypted", "typeInfo", "value", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "columnMetaData":
            case "paramName":
            case "fByRefValue":
            case "fDefaultValue":
            case "fEncrypted":
            case "typeInfo":
            case "value":
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
            case "paramName": {
                this.paramName = val.asString();
                break;
            }
            case "fByRefValue": {
                this.fByRefValue = val.asBoolean();
                break;
            }
            case "fDefaultValue": {
                this.fDefaultValue = val.asBoolean();
                break;
            }
            case "fEncrypted": {
                this.fEncrypted = val.asBoolean();
                break;
            }
            case "value": {
                this.value.setValueFromJS(val);
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "RPC packet" });
    }
}
