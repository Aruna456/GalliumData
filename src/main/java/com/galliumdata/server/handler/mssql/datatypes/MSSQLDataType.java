// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public abstract class MSSQLDataType
{
    public static final Object NULL_VALUE;
    protected ColumnMetadata meta;
    protected Object value;
    protected boolean changed;
    protected boolean resizable;
    public static final LocalDate DATE1;
    public static final LocalDateTime DATETIME1;
    public static final LocalDateTime DATETIME1900;
    
    public MSSQLDataType(final ColumnMetadata meta) {
        this.meta = meta;
    }
    
    public abstract int readFromBytes(final byte[] p0, final int p1);
    
    public void read(final RawPacketReader reader) {
        throw new RuntimeException("Not yet implemented");
    }
    
    public int readVariantBytes(final byte[] bytes, final int offset, final int valueLen) {
        throw new ServerException("db.mssql.protocol.DataTypeDoesNotImplement", new Object[] { this.getClass().getName(), "readVariantBytes" });
    }
    
    public void readVariantBytes(final RawPacketReader reader, final int valueLen) {
        throw new ServerException("db.mssql.protocol.DataTypeDoesNotImplement", new Object[] { this.getClass().getName(), "readVariantBytes" });
    }
    
    public abstract int getSerializedSize();
    
    public int getVariantSize() {
        return this.getSerializedSize();
    }
    
    public abstract void write(final RawPacketWriter p0);
    
    public void writeVariant(final RawPacketWriter writer) {
        this.write(writer);
    }
    
    public ColumnMetadata getMeta() {
        return this.meta;
    }
    
    public Object getValue() {
        if (MSSQLDataType.NULL_VALUE == this.value) {
            return null;
        }
        return this.value;
    }
    
    public void setValue(final Object obj) {
        this.value = obj;
        this.changed = true;
    }
    
    public abstract void setValueFromJS(final Value p0);
    
    public void setValueNoChange(final Object obj) {
        this.value = obj;
    }
    
    public boolean isNull() {
        return this.value == null || MSSQLDataType.NULL_VALUE == this.value;
    }
    
    public void setNull() {
        this.value = MSSQLDataType.NULL_VALUE;
        this.changed = true;
    }
    
    public void setNullNoChange() {
        this.value = MSSQLDataType.NULL_VALUE;
    }
    
    public static MSSQLDataType createDataType(final ColumnMetadata meta) {
        switch (meta.getTypeInfo().getType()) {
            case 31: {
                return new MSSQLNull(meta);
            }
            case 50: {
                return new MSSQLBit(meta);
            }
            case 48: {
                return new MSSQLTinyInt(meta);
            }
            case 52: {
                return new MSSQLSmallInt(meta);
            }
            case 56: {
                return new MSSQLInt(meta);
            }
            case Byte.MAX_VALUE: {
                return new MSSQLBigInt(meta);
            }
            case 61: {
                return new MSSQLDateTime(meta);
            }
            case 58: {
                return new MSSQLSmallDateTime(meta);
            }
            case 62: {
                return new MSSQLFloat(meta);
            }
            case 59: {
                return new MSSQLReal(meta);
            }
            case 60: {
                return new MSSQLMoney(meta);
            }
            case 122: {
                return new MSSQLSmallMoney(meta);
            }
            case 55:
            case 63: {
                throw new ServerException("db.mssql.protocol.UnknownDatatype", new Object[] { meta.getTypeInfo().getType() });
            }
            case 104: {
                return new MSSQLBitN(meta);
            }
            case 38: {
                return new MSSQLIntN(meta);
            }
            case 106: {
                return new MSSQLDecimal(meta);
            }
            case 109: {
                return new MSSQLFloatN(meta);
            }
            case 110: {
                return new MSSQLMoneyN(meta);
            }
            case 40: {
                return new MSSQLDateN(meta);
            }
            case 111: {
                return new MSSQLDateTimeN(meta);
            }
            case 42: {
                return new MSSQLDateTime2N(meta);
            }
            case 43: {
                return new MSSQLDateTimeOffsetN(meta);
            }
            case 41: {
                return new MSSQLTimeN(meta);
            }
            case 36: {
                return new MSSQLUUID(meta);
            }
            case 108: {
                return new MSSQLNumeric(meta);
            }
            case 37:
            case 39:
            case 45:
            case 47: {
                throw new ServerException("db.mssql.protocol.UnknownDatatype", new Object[] { meta.getTypeInfo().getType() });
            }
            case -81: {
                return new MSSQLChar(meta);
            }
            case -89: {
                return new MSSQLVarChar(meta);
            }
            case -17: {
                return new MSSQLNChar(meta);
            }
            case -25: {
                return new MSSQLNVarChar(meta);
            }
            case -83: {
                return new MSSQLBinary(meta);
            }
            case -91: {
                return new MSSQLVarBinary(meta);
            }
            case 35: {
                return new MSSQLText(meta);
            }
            case 99: {
                return new MSSQLNText(meta);
            }
            case 34: {
                return new MSSQLImage(meta);
            }
            case -15: {
                return new MSSQLXML(meta);
            }
            case 98: {
                return new MSSQLVariant(meta);
            }
            case -16: {
                return new MSSQLUDT(meta);
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownDatatype", new Object[] { meta.getTypeInfo().getType() });
            }
        }
    }
    
    public boolean isResizable() {
        return this.resizable;
    }
    
    public void setResizable(final boolean resizable) {
        this.resizable = resizable;
    }
    
    public boolean isChanged() {
        return this.changed;
    }
    
    public MSSQLDataType clone() {
        final MSSQLDataType copy = createDataType(this.getMeta());
        copy.value = this.value;
        copy.resizable = this.resizable;
        return copy;
    }
    
    @Override
    public String toString() {
        if (this.value == MSSQLDataType.NULL_VALUE) {
            return "NULL";
        }
        if (this.isNull()) {
            return "null";
        }
        return this.value.toString();
    }
    
    static {
        NULL_VALUE = new Object();
        DATE1 = LocalDate.of(1, 1, 1);
        DATETIME1 = LocalDateTime.of(1, 1, 1, 0, 0, 0);
        DATETIME1900 = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
    }
}
