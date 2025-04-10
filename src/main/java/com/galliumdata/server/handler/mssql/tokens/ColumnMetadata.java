// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import java.util.Arrays;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.galliumdata.server.handler.mssql.TypeInfo;
import com.galliumdata.server.handler.mssql.ConnectionState;
import org.graalvm.polyglot.proxy.ProxyObject;

public class ColumnMetadata implements ProxyObject
{
    protected ConnectionState connectionState;
    private int userType;
    private boolean fNullable;
    private boolean fCaseSen;
    private byte usUpdateable;
    private boolean fIdentity;
    private boolean fComputed;
    private byte usReservedODBC;
    private boolean fFixedLenCLRType;
    private boolean fSparseColumnSet;
    private boolean fEncrypted;
    private boolean usReserved3;
    private byte usReserved;
    private boolean fHidden;
    private boolean fKey;
    private boolean fNullableUnknown;
    private TypeInfo typeInfo;
    private List<String> tableName;
    private CryptoMetadata cryptoMetadata;
    private String columnName;
    
    public ColumnMetadata(final ConnectionState connState) {
        this.tableName = new ArrayList<String>();
        this.connectionState = connState;
    }
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        if (this.connectionState.tdsVersion72andHigher()) {
            this.userType = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
        }
        else {
            this.userType = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
        }
        final short flags = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.fNullable = ((flags & 0x1) > 0);
        this.fCaseSen = ((flags & 0x2) > 0);
        this.usUpdateable = (byte)((flags & 0xC) >> 2);
        this.fIdentity = ((flags & 0x10) > 0);
        if (this.connectionState.tdsVersion72andHigher()) {
            this.fComputed = ((flags & 0x20) > 0);
        }
        if (this.connectionState.tdsVersion73andLower()) {
            this.usReservedODBC = (byte)((flags & 0xC0) >> 6);
        }
        if (this.connectionState.tdsVersion73andHigher()) {
            this.fFixedLenCLRType = ((flags & 0x100) > 0);
        }
        if (this.connectionState.tdsVersion74andHigher()) {
            this.fSparseColumnSet = ((flags & 0x400) > 0);
            this.fEncrypted = ((flags & 0x800) > 0);
        }
        if (this.connectionState.tdsVersion72andHigher()) {
            this.fHidden = ((flags & 0x2000) > 0);
            this.fKey = ((flags & 0x4000) > 0);
            this.fNullableUnknown = ((flags & 0x8000) != 0x0);
        }
        this.typeInfo = new TypeInfo();
        idx += this.typeInfo.readFromBytes(bytes, idx);
        if (this.typeInfo.getType() == 34 || this.typeInfo.getType() == 35 || this.typeInfo.getType() == 99) {
            final int numParts = bytes[idx];
            ++idx;
            for (int i = 0; i < numParts; ++i) {
                final int partLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
                idx += 2;
                final String part = new String(bytes, idx, partLen * 2, StandardCharsets.UTF_16LE);
                idx += partLen * 2;
                this.tableName.add(part);
            }
        }
        if (this.connectionState.tdsVersion74andHigher() && this.fEncrypted) {
            this.cryptoMetadata = new CryptoMetadata();
            idx += this.cryptoMetadata.readFromBytes(bytes, idx, numBytes - (idx - offset));
        }
        final byte nameLen = bytes[idx];
        ++idx;
        this.columnName = new String(bytes, idx, nameLen * 2, StandardCharsets.UTF_16LE);
        idx += nameLen * 2;
        return idx - offset;
    }
    
    public void read(final RawPacketReader reader) {
        if (this.connectionState.tdsVersion72andHigher()) {
            this.userType = reader.readFourByteIntLow();
        }
        else {
            this.userType = reader.readTwoByteIntLow();
        }
        final int flags = reader.readTwoByteIntLow();
        this.fNullable = ((flags & 0x1) > 0);
        this.fCaseSen = ((flags & 0x2) > 0);
        this.usUpdateable = (byte)((flags & 0xC) >> 2);
        this.fIdentity = ((flags & 0x10) > 0);
        this.fComputed = ((flags & 0x20) > 0);
        if (this.connectionState.tdsVersion73andLower()) {
            this.usReservedODBC = (byte)((flags & 0xC0) >> 6);
        }
        if (this.connectionState.tdsVersion73andHigher()) {
            this.fFixedLenCLRType = ((flags & 0x100) > 0);
        }
        if (this.connectionState.tdsVersion74andHigher()) {
            this.fSparseColumnSet = ((flags & 0x400) > 0);
            this.fEncrypted = ((flags & 0x800) > 0);
        }
        if (this.connectionState.tdsVersion72andHigher()) {
            this.fHidden = ((flags & 0x2000) > 0);
            this.fKey = ((flags & 0x4000) > 0);
            this.fNullableUnknown = ((flags & 0x8000) != 0x0);
        }
        (this.typeInfo = new TypeInfo(this.connectionState)).read(reader);
        if (this.typeInfo.getType() == 34 || this.typeInfo.getType() == 35 || this.typeInfo.getType() == 99) {
            if (this.connectionState.tdsVersion71AndLower()) {
                final int strLen = reader.readTwoByteIntLow();
                if (strLen > 0) {
                    final String part = reader.readString(strLen * 2);
                    this.tableName.add(part);
                }
            }
            else {
                for (int numParts = reader.readByte(), i = 0; i < numParts; ++i) {
                    final int partLen = reader.readTwoByteIntLow();
                    final String part2 = reader.readString(partLen * 2);
                    this.tableName.add(part2);
                }
            }
        }
        if (this.connectionState.tdsVersion74andHigher() && this.fEncrypted) {
            (this.cryptoMetadata = new CryptoMetadata()).read(reader);
        }
        final byte nameLen = reader.readByte();
        this.columnName = reader.readString(nameLen * 2);
    }
    
    public int getSerializedSize() {
        int size = 0;
        if (this.connectionState.tdsVersion72andHigher()) {
            size += 4;
        }
        else {
            size += 2;
        }
        size += 2;
        size += this.typeInfo.getSerializedSize();
        if (this.typeInfo.getType() == 34 || this.typeInfo.getType() == 35 || this.typeInfo.getType() == 99) {
            if (this.connectionState.tdsVersion71AndLower()) {
                size += 2;
                if (!this.tableName.isEmpty()) {
                    size += this.tableName.get(0).length();
                }
            }
            else {
                ++size;
                for (final String namePart : this.tableName) {
                    size += 2;
                    size += namePart.length() * 2;
                }
            }
        }
        if (this.connectionState.tdsVersion74andHigher() && this.fEncrypted) {
            size += this.cryptoMetadata.getSerializedSize();
        }
        size = ++size + this.columnName.length() * 2;
        return size;
    }
    
    public void write(final RawPacketWriter writer) {
        if (this.connectionState.tdsVersion72andHigher()) {
            writer.writeFourByteIntegerLow(this.userType);
        }
        else {
            writer.writeTwoByteIntegerLow(this.userType);
        }
        short flags = 0;
        if (this.fNullable) {
            flags |= 0x1;
        }
        if (this.fCaseSen) {
            flags |= 0x2;
        }
        flags |= (short)(this.usUpdateable << 2);
        if (this.fIdentity) {
            flags |= 0x10;
        }
        if (this.fComputed) {
            flags |= 0x20;
        }
        if (this.connectionState.tdsVersion73andLower()) {
            flags |= (short)(this.usReservedODBC << 6);
        }
        if (this.connectionState.tdsVersion73andHigher() && this.fFixedLenCLRType) {
            flags |= 0x100;
        }
        if (this.connectionState.tdsVersion74andHigher()) {
            if (this.fSparseColumnSet) {
                flags |= 0x400;
            }
            if (this.fEncrypted) {
                flags |= 0x800;
            }
        }
        if (this.connectionState.tdsVersion72andHigher()) {
            if (this.fHidden) {
                flags |= 0x2000;
            }
            if (this.fKey) {
                flags |= 0x4000;
            }
            if (this.fNullableUnknown) {
                flags |= (short)32768;
            }
        }
        writer.writeTwoByteIntegerLow(flags);
        this.typeInfo.write(writer);
        if (this.typeInfo.getType() == 34 || this.typeInfo.getType() == 35 || this.typeInfo.getType() == 99) {
            if (this.connectionState.tdsVersion71AndLower()) {
                if (this.tableName.isEmpty()) {
                    writer.writeTwoByteIntegerLow(0);
                }
                else {
                    final String name = this.tableName.get(0);
                    writer.writeTwoByteIntegerLow(name.length());
                    final byte[] nameBytes = name.getBytes(StandardCharsets.UTF_16LE);
                    writer.writeBytes(nameBytes, 0, nameBytes.length);
                }
            }
            else {
                writer.writeByte((byte)this.tableName.size());
                for (final String namePart : this.tableName) {
                    writer.writeTwoByteIntegerLow((short)namePart.length());
                    final byte[] strBytes = namePart.getBytes(StandardCharsets.UTF_16LE);
                    writer.writeBytes(strBytes, 0, strBytes.length);
                }
            }
        }
        if (this.connectionState.tdsVersion74andHigher() && this.fEncrypted) {
            this.cryptoMetadata.write(writer);
        }
        writer.writeByte((byte)this.columnName.length());
        final byte[] strBytes2 = this.columnName.getBytes(StandardCharsets.UTF_16LE);
        writer.writeBytes(strBytes2, 0, strBytes2.length);
    }
    
    @Override
    public int hashCode() {
        int hash = this.userType;
        hash += this.typeInfo.hashCode();
        for (final String t : this.tableName) {
            hash += t.hashCode();
        }
        if (this.columnName != null) {
            hash += this.columnName.hashCode();
        }
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ColumnMetadata)) {
            return false;
        }
        final ColumnMetadata other = (ColumnMetadata)obj;
        return other.userType == this.userType && (this.typeInfo == null || this.typeInfo.equals(other.typeInfo)) && this.tableName.equals(other.tableName) && (this.columnName == null || this.columnName.equals(other.columnName));
    }
    
    @Override
    public String toString() {
        String cName = this.columnName;
        if (cName == null) {
            cName = "<none>";
        }
        return "Column metadata for " + cName + " (" + this.typeInfo.getTypeName();
    }
    
    public ConnectionState getConnectionState() {
        return this.connectionState;
    }
    
    public void setConnectionState(final ConnectionState connectionState) {
        this.connectionState = connectionState;
    }
    
    public int getUserType() {
        return this.userType;
    }
    
    public void setUserType(final int userType) {
        this.userType = userType;
    }
    
    public boolean isfNullable() {
        return this.fNullable;
    }
    
    public void setfNullable(final boolean fNullable) {
        this.fNullable = fNullable;
    }
    
    public boolean isfCaseSen() {
        return this.fCaseSen;
    }
    
    public void setfCaseSen(final boolean fCaseSen) {
        this.fCaseSen = fCaseSen;
    }
    
    public byte getUsUpdateable() {
        return this.usUpdateable;
    }
    
    public void setUsUpdateable(final byte usUpdateable) {
        if (usUpdateable < 0 || usUpdateable > 2) {
            throw new ServerException("db.mssql.logic.InvalidValue", new Object[] { "usUpdateable", usUpdateable, "0, 1, 2" });
        }
        this.usUpdateable = usUpdateable;
    }
    
    public boolean isfIdentity() {
        return this.fIdentity;
    }
    
    public void setfIdentity(final boolean fIdentity) {
        this.fIdentity = fIdentity;
    }
    
    public boolean isfComputed() {
        return this.fComputed;
    }
    
    public void setfComputed(final boolean fComputed) {
        this.fComputed = fComputed;
    }
    
    public boolean isfFixedLenCLRType() {
        return this.fFixedLenCLRType;
    }
    
    public void setfFixedLenCLRType(final boolean fFixedLenCLRType) {
        this.fFixedLenCLRType = fFixedLenCLRType;
    }
    
    public boolean isfSparseColumnSet() {
        return this.fSparseColumnSet;
    }
    
    public void setfSparseColumnSet(final boolean fSparseColumnSet) {
        this.fSparseColumnSet = fSparseColumnSet;
    }
    
    public boolean isfEncrypted() {
        return this.fEncrypted;
    }
    
    public void setfEncrypted(final boolean fEncrypted) {
        this.fEncrypted = fEncrypted;
    }
    
    public boolean isfHidden() {
        return this.fHidden;
    }
    
    public void setfHidden(final boolean fHidden) {
        this.fHidden = fHidden;
    }
    
    public boolean isfKey() {
        return this.fKey;
    }
    
    public void setfKey(final boolean fKey) {
        this.fKey = fKey;
    }
    
    public boolean isfNullableUnknown() {
        return this.fNullableUnknown;
    }
    
    public void setfNullableUnknown(final boolean fNullableUnknown) {
        this.fNullableUnknown = fNullableUnknown;
    }
    
    public TypeInfo getTypeInfo() {
        return this.typeInfo;
    }
    
    public void setTypeInfo(final TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }
    
    public List<String> getTableName() {
        return this.tableName;
    }
    
    public void setTableName(final List<String> tableName) {
        this.tableName = tableName;
    }
    
    public String getColumnName() {
        return this.columnName;
    }
    
    public void setColumnName(final String columnName) {
        this.columnName = columnName;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "userType": {
                return this.getUserType();
            }
            case "fNullable": {
                return this.isfNullable();
            }
            case "fCaseSen": {
                return this.isfCaseSen();
            }
            case "usUpdateable": {
                return this.getUsUpdateable();
            }
            case "fIdentity": {
                return this.isfIdentity();
            }
            case "fComputed": {
                return this.isfComputed();
            }
            case "fFixedLenCLRType": {
                return this.isfFixedLenCLRType();
            }
            case "fSparseColumnSet": {
                return this.isfSparseColumnSet();
            }
            case "fEncrypted": {
                return this.isfEncrypted();
            }
            case "fHidden": {
                return this.isfHidden();
            }
            case "fKey": {
                return this.isfKey();
            }
            case "fNullableUnknown": {
                return this.isfNullableUnknown();
            }
            case "typeInfo": {
                return this.getTypeInfo();
            }
            case "tableName": {
                if (this.tableName == null || this.tableName.size() == 0) {
                    return null;
                }
                return String.join(".", this.tableName);
            }
            case "cryptoMetadata": {
                return this.cryptoMetadata;
            }
            case "columnName": {
                return this.getColumnName();
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
        return new String[] { "userType", "fNullable", "fCaseSen", "usUpdateable", "fIdentity", "fComputed", "fFixedLenCLRType", "fSparseColumnSet", "fEncrypted", "fHidden", "fKey", "fNullableUnknown", "typeInfo", "tableName", "cryptoMetadata", "columnName", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "userType":
            case "fNullable":
            case "fCaseSen":
            case "usUpdateable":
            case "fIdentity":
            case "fComputed":
            case "fFixedLenCLRType":
            case "fSparseColumnSet":
            case "fEncrypted":
            case "fHidden":
            case "fKey":
            case "fNullableUnknown":
            case "typeInfo":
            case "tableName":
            case "cryptoMetadata":
            case "columnName":
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
            case "userType": {
                this.setUserType(val.asInt());
                return;
            }
            case "fNullable": {
                this.setfNullable(val.asBoolean());
                return;
            }
            case "fCaseSen": {
                this.setfCaseSen(val.asBoolean());
                return;
            }
            case "usUpdateable": {
                this.setUsUpdateable(val.asByte());
                return;
            }
            case "fIdentity": {
                this.setfIdentity(val.asBoolean());
                return;
            }
            case "fComputed": {
                this.setfComputed(val.asBoolean());
                return;
            }
            case "fFixedLenCLRType": {
                this.setfFixedLenCLRType(val.asBoolean());
                return;
            }
            case "fSparseColumnSet": {
                this.setfSparseColumnSet(val.asBoolean());
                return;
            }
            case "fEncrypted": {
                this.setfEncrypted(val.asBoolean());
                return;
            }
            case "fHidden": {
                this.setfHidden(val.asBoolean());
                return;
            }
            case "fKey": {
                this.setfKey(val.asBoolean());
                return;
            }
            case "fNullableUnknown": {
                this.setfNullableUnknown(val.asBoolean());
                return;
            }
            case "tableName": {
                if (val == null || val.isNull()) {
                    this.tableName = new ArrayList<String>();
                    return;
                }
                this.tableName = Arrays.asList(val.asString().split("."));
                return;
            }
            case "columnName": {
                this.setColumnName(val.asString());
                break;
            }
        }
        throw new ServerException("db.mssql.logic.NoSuchMember", new Object[] { key });
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", new Object[] { key, "ColumnMetadata packet" });
    }
}
