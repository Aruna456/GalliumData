// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;
import com.galliumdata.server.handler.mssql.TypeInfo;

public class TokenReturnValue extends MessageToken
{
    private int paramOrdinal;
    private String paramName;
    private byte status;
    private int userType;
    private boolean fNullable;
    private boolean fCaseSen;
    private byte usUpdateable;
    private boolean fIdentity;
    private boolean fComputed;
    private byte usReservedODBC;
    private boolean fFixedLenCLRType;
    private boolean fEncrypted;
    private TypeInfo typeInfo;
    private MSSQLDataType value;
    private static final Logger log;
    
    public TokenReturnValue(final ConnectionState connectionState) {
        super(connectionState);
        this.typeInfo = new TypeInfo();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        this.paramOrdinal = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        final int paramNameLen = bytes[idx];
        ++idx;
        if (paramNameLen > 0) {
            this.paramName = new String(bytes, idx, paramNameLen * 2, StandardCharsets.UTF_16LE);
            idx += paramNameLen * 2;
        }
        this.status = bytes[idx];
        ++idx;
        if (this.connectionState.tdsVersion72andHigher()) {
            this.userType = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
        }
        else {
            this.userType = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            idx += 2;
        }
        final int userFlags = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
        idx += 2;
        this.fNullable = ((userFlags & 0x1) != 0x0);
        this.fCaseSen = ((userFlags & 0x2) != 0x0);
        this.usUpdateable = (byte)((userFlags & 0xC) >> 2);
        this.fIdentity = ((userFlags & 0x10) != 0x0);
        this.fComputed = ((userFlags & 0x20) != 0x0);
        this.fFixedLenCLRType = ((userFlags & 0x100) != 0x0);
        this.fEncrypted = ((userFlags & 0x800) != 0x0);
        idx += this.typeInfo.readFromBytes(bytes, idx);
        final ColumnMetadata colMeta = new ColumnMetadata(this.connectionState);
        colMeta.setColumnName(this.paramName);
        colMeta.setTypeInfo(this.typeInfo);
        this.value = MSSQLDataType.createDataType(colMeta);
        idx += this.value.readFromBytes(bytes, idx);
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        this.paramOrdinal = reader.readTwoByteIntLow();
        final int paramNameLen = reader.readByte();
        if (paramNameLen > 0) {
            this.paramName = reader.readString(paramNameLen * 2);
        }
        this.status = reader.readByte();
        if (this.connectionState.tdsVersion72andHigher()) {
            this.userType = reader.readFourByteIntLow();
        }
        else {
            this.userType = reader.readTwoByteIntLow();
        }
        final int userFlags = reader.readTwoByteIntLow();
        this.fNullable = ((userFlags & 0x1) != 0x0);
        this.fCaseSen = ((userFlags & 0x2) != 0x0);
        this.usUpdateable = (byte)((userFlags & 0xC) >> 2);
        this.fIdentity = ((userFlags & 0x10) != 0x0);
        this.fComputed = ((userFlags & 0x20) != 0x0);
        this.fFixedLenCLRType = ((userFlags & 0x100) != 0x0);
        this.fEncrypted = ((userFlags & 0x800) != 0x0);
        this.typeInfo.read(reader);
        final ColumnMetadata colMeta = new ColumnMetadata(this.connectionState);
        colMeta.setColumnName(this.paramName);
        colMeta.setTypeInfo(this.typeInfo);
        (this.value = MSSQLDataType.createDataType(colMeta)).read(reader);
        final String lastRpc = this.connectionState.getLastRPC().toLowerCase();
        if ("sp_cursoropen".equals(lastRpc) && this.connectionState.getRPCResultIndex() == 0) {
            final Object val = this.value.getValue();
            if (val instanceof Number) {
                this.connectionState.setCurrentCursorID(((Number)val).longValue());
            }
            else {
                TokenReturnValue.log.debug(Markers.MSSQL, "Unexpected: TokenReturnValue #0 for Sp_CursorOpen is null or not a number: " + String.valueOf(val));
            }
        }
        else if ("sp_cursorprepexec".equals(lastRpc) && this.connectionState.getRPCResultIndex() == 0) {
            final Object val = this.value.getValue();
            if (val instanceof Number) {
                this.connectionState.setCurrentPrepCursorHandle(((Number)val).longValue());
            }
            else {
                TokenReturnValue.log.debug(Markers.MSSQL, "Unexpected: TokenReturnValue #0 for Sp_CursorPrepExec is null or not a number: " + String.valueOf(val));
            }
        }
        else if ("sp_cursorprepexec".equals(lastRpc) && this.connectionState.getRPCResultIndex() == 1) {
            final Object val = this.value.getValue();
            if (val instanceof Number) {
                this.connectionState.setCurrentCursorID(((Number)val).longValue());
            }
            else {
                TokenReturnValue.log.debug(Markers.MSSQL, "Unexpected: TokenReturnValue #1 for Sp_CursorPrepExec is null or not a number: " + String.valueOf(val));
            }
        }
        else if ("sp_cursorprepare".equals(lastRpc) && this.connectionState.getRPCResultIndex() == 0) {
            final Object val = this.value.getValue();
            if (val instanceof Number) {
                this.connectionState.setCurrentPrepCursorHandle(((Number)val).longValue());
            }
            else {
                TokenReturnValue.log.debug(Markers.MSSQL, "Unexpected: TokenReturnValue #0 for Sp_CursorPrepare is null or not a number: " + String.valueOf(val));
            }
        }
        else if ("sp_cursorexecute".equals(lastRpc) && this.connectionState.getRPCResultIndex() == 0) {
            final Object val = this.value.getValue();
            if (val instanceof Number) {
                this.connectionState.setCurrentCursorID(((Number)val).longValue());
            }
            else {
                TokenReturnValue.log.debug(Markers.MSSQL, "Unexpected: TokenReturnValue #1 for Sp_CursorPrepare is null or not a number: " + String.valueOf(val));
            }
        }
        this.connectionState.incrementRPCResultIndex();
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 2;
        ++size;
        if (this.paramName != null) {
            size += this.paramName.length() * 2;
        }
        ++size;
        if (this.connectionState.tdsVersion72andHigher()) {
            size += 4;
        }
        else {
            size += 2;
        }
        size += 2;
        size += this.typeInfo.getSerializedSize();
        size += this.value.getSerializedSize();
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow((short)this.paramOrdinal);
        if (this.paramName == null) {
            writer.writeByte((byte)0);
        }
        else {
            writer.writeByte((byte)this.paramName.length());
            final byte[] paramNameBytes = this.paramName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(paramNameBytes, 0, paramNameBytes.length);
        }
        writer.writeByte(this.status);
        if (this.connectionState.tdsVersion72andHigher()) {
            writer.writeFourByteIntegerLow(this.userType);
        }
        else {
            writer.writeTwoByteIntegerLow((short)this.userType);
        }
        int userFlags = 0;
        if (this.fNullable) {
            userFlags |= 0x1;
        }
        if (this.fCaseSen) {
            userFlags |= 0x2;
        }
        userFlags |= (this.usUpdateable & 0x3) << 2;
        if (this.fIdentity) {
            userFlags |= 0x10;
        }
        if (this.fComputed) {
            userFlags |= 0x20;
        }
        if (this.fFixedLenCLRType) {
            userFlags |= 0x100;
        }
        if (this.fEncrypted) {
            userFlags |= 0x800;
        }
        writer.writeTwoByteIntegerLow((short)userFlags);
        this.typeInfo.write(writer);
        this.value.write(writer);
    }
    
    @Override
    public byte getTokenType() {
        return -84;
    }
    
    @Override
    public String getTokenTypeName() {
        return "ReturnValue";
    }
    
    @Override
    public String toString() {
        return "ReturnValue: " + String.valueOf(this.value);
    }
    
    public int getParamOrdinal() {
        return this.paramOrdinal;
    }
    
    public void setParamOrdinal(final int paramOrdinal) {
        this.paramOrdinal = paramOrdinal;
    }
    
    public String getParamName() {
        return this.paramName;
    }
    
    public void setParamName(final String paramName) {
        this.paramName = paramName;
    }
    
    public byte getStatus() {
        return this.status;
    }
    
    public void setStatus(final byte status) {
        this.status = status;
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
    
    public byte getUsReservedODBC() {
        return this.usReservedODBC;
    }
    
    public void setUsReservedODBC(final byte usReservedODBC) {
        this.usReservedODBC = usReservedODBC;
    }
    
    public boolean isfFixedLenCLRType() {
        return this.fFixedLenCLRType;
    }
    
    public void setfFixedLenCLRType(final boolean fFixedLenCLRType) {
        this.fFixedLenCLRType = fFixedLenCLRType;
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
    
    public void setTypeInfo(final TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }
    
    public MSSQLDataType getValue() {
        return this.value;
    }
    
    public void setValue(final MSSQLDataType value) {
        this.value = value;
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
