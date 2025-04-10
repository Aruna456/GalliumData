// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.datatypes;

import java.math.MathContext;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.math.BigInteger;
import com.galliumdata.server.util.BinaryUtil;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.TypeInfo;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class MSSQLVariant extends MSSQLDataType
{
    private byte dataType;
    private ColumnMetadata variantMeta;
    
    public MSSQLVariant(final ColumnMetadata meta) {
        super(meta);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset) {
        int idx = offset;
        final int varSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (varSize == 0) {
            this.value = MSSQLVariant.NULL_VALUE;
            return idx - offset;
        }
        this.dataType = bytes[idx];
        ++idx;
        this.variantMeta = new ColumnMetadata(this.meta.getConnectionState());
        final TypeInfo typeInfo = new TypeInfo();
        typeInfo.setType(this.dataType);
        this.variantMeta.setTypeInfo(typeInfo);
        final MSSQLDataType dt = MSSQLDataType.createDataType(this.variantMeta);
        switch (this.dataType) {
            case 48:
            case 50:
            case 52:
            case 56:
            case 58:
            case 59:
            case 60:
            case 61:
            case 122:
            case Byte.MAX_VALUE: {
                if (bytes[idx] != 0) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 0, bytes[idx] });
                }
                idx = ++idx + dt.readFromBytes(bytes, idx);
                break;
            }
            case 36:
            case 40:
            case 62: {
                if (bytes[idx] != 0) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 0, bytes[idx] });
                }
                idx = ++idx + dt.readVariantBytes(bytes, idx, varSize - 2);
                break;
            }
            case 41:
            case 42:
            case 43: {
                if (bytes[idx] != 1) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 1, bytes[idx] });
                }
                ++idx;
                typeInfo.setScale(bytes[idx]);
                idx = ++idx + dt.readVariantBytes(bytes, idx, varSize - 3);
                break;
            }
            case 106:
            case 108: {
                if (bytes[idx] != 2) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 2, bytes[idx] });
                }
                ++idx;
                typeInfo.setPrecision(bytes[idx]);
                ++idx;
                typeInfo.setScale(bytes[idx]);
                ++idx;
                final byte sign = bytes[idx];
                ++idx;
                final byte[] bigIntBytes = new byte[16];
                System.arraycopy(bytes, idx, bigIntBytes, 0, 16);
                idx += 16;
                BinaryUtil.reverseByteArray(bigIntBytes);
                final BigInteger rawValue = new BigInteger(bigIntBytes);
                BigDecimal bd = new BigDecimal(rawValue);
                final BigDecimal scaleBd = new BigDecimal(10).pow(typeInfo.getScale());
                bd = bd.divide(scaleBd, 6, RoundingMode.UNNECESSARY);
                if (sign == 0) {
                    bd = bd.negate();
                }
                dt.setValue(bd);
                break;
            }
            case -89: {
                if (bytes[idx] != 7) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 7, bytes[idx] });
                }
                idx = ++idx + typeInfo.readCollation(bytes, idx);
                final int len = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
                typeInfo.setVarLen(len);
                idx += 2;
                final int varLen = varSize - 9;
                final String strVal = new String(bytes, idx, varLen, StandardCharsets.UTF_8);
                dt.setValue(strVal);
                idx += varLen;
                break;
            }
            case -81: {
                if (bytes[idx] != 7) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 7, bytes[idx] });
                }
                idx = ++idx + typeInfo.readCollation(bytes, idx);
                final int varLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
                idx += 2;
                typeInfo.setVarLen(varLen);
                final String strVal = new String(bytes, idx, varLen, StandardCharsets.UTF_8);
                dt.setValue(strVal);
                idx += varSize - 9;
                break;
            }
            case -25:
            case -17: {
                if (bytes[idx] != 7) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 7, bytes[idx] });
                }
                idx = ++idx + typeInfo.readCollation(bytes, idx);
                final int varLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
                idx += 2;
                typeInfo.setVarLen(varLen);
                dt.setValue(new String(bytes, idx, varSize - 9, StandardCharsets.UTF_16LE));
                idx += varSize - 9;
                break;
            }
            case -91:
            case -83: {
                if (bytes[idx] != 2) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 2, bytes[idx] });
                }
                ++idx;
                final int varLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
                idx += 2;
                typeInfo.setVarLen(varLen);
                final byte[] bytesVal = new byte[varSize - 4];
                System.arraycopy(bytes, idx, bytesVal, 0, varSize - 4);
                dt.setValue(bytesVal);
                idx += varSize - 4;
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownDatatype", new Object[] { this.dataType });
            }
        }
        this.value = dt;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int varSize = reader.readFourByteIntLow();
        if (varSize == 0) {
            this.value = MSSQLVariant.NULL_VALUE;
            return;
        }
        this.dataType = reader.readByte();
        this.variantMeta = new ColumnMetadata(this.meta.getConnectionState());
        final TypeInfo typeInfo = new TypeInfo();
        typeInfo.setType(this.dataType);
        this.variantMeta.setTypeInfo(typeInfo);
        final MSSQLDataType dt = MSSQLDataType.createDataType(this.variantMeta);
        switch (this.dataType) {
            case 48:
            case 50:
            case 52:
            case 56:
            case 58:
            case 59:
            case 60:
            case 61:
            case 122:
            case Byte.MAX_VALUE: {
                final byte sizeByte = reader.readByte();
                if (sizeByte != 0) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 0, sizeByte });
                }
                dt.read(reader);
                break;
            }
            case 36:
            case 40:
            case 62: {
                final byte sizeByte = reader.readByte();
                if (sizeByte != 0) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 0, sizeByte });
                }
                dt.readVariantBytes(reader, varSize - 2);
                break;
            }
            case 41:
            case 42:
            case 43: {
                final byte sizeByte = reader.readByte();
                if (sizeByte != 1) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 1, sizeByte });
                }
                typeInfo.setScale(reader.readByte());
                dt.readVariantBytes(reader, varSize - 3);
                break;
            }
            case 106:
            case 108: {
                final byte sizeByte = reader.readByte();
                if (sizeByte != 2) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 2, sizeByte });
                }
                typeInfo.setPrecision(reader.readByte());
                typeInfo.setScale(reader.readByte());
                final byte sign = reader.readByte();
                final byte[] biBytes = reader.readBytes(16);
                BinaryUtil.reverseByteArray(biBytes);
                final BigInteger biVal = new BigInteger(biBytes);
                BigDecimal bd = new BigDecimal(biVal);
                final int scale = typeInfo.getScale();
                final BigDecimal scaleBd = new BigDecimal(10).pow(scale);
                bd = bd.divide(scaleBd, scale, RoundingMode.UNNECESSARY);
                if (sign == 0) {
                    bd = bd.negate();
                }
                dt.setValue(bd);
                break;
            }
            case -89: {
                final byte sizeByte = reader.readByte();
                if (sizeByte != 7) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 7, sizeByte });
                }
                typeInfo.readCollation(reader);
                final int len = reader.readTwoByteIntLow() & 0xFFFF;
                typeInfo.setVarLen(len);
                final int varLen = varSize - 9;
                final String strVal = reader.readStringWithEncoding(varLen, this.meta.getTypeInfo().getCollationLCID());
                dt.setValue(strVal);
                break;
            }
            case -81: {
                final byte sizeByte = reader.readByte();
                if (sizeByte != 7) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 7, sizeByte });
                }
                typeInfo.readCollation(reader);
                final int varLen = reader.readTwoByteIntLow() & 0xFFFF;
                typeInfo.setVarLen(varLen);
                final String strVal = reader.readStringWithEncoding(varLen, this.meta.getTypeInfo().getCollationLCID());
                dt.setValue(strVal);
                break;
            }
            case -25:
            case -17: {
                final byte sizeByte = reader.readByte();
                if (sizeByte != 7) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 7, sizeByte });
                }
                typeInfo.readCollation(reader);
                final int varLen = reader.readTwoByteIntLow() & 0xFFFF;
                typeInfo.setVarLen(varLen);
                dt.setValue(reader.readString(varSize - 9));
                break;
            }
            case -91:
            case -83: {
                final byte sizeByte = reader.readByte();
                if (sizeByte != 2) {
                    throw new ServerException("db.mssql.protocol.InvalidPropsForVariant", new Object[] { this.meta.getColumnName(), 2, sizeByte });
                }
                final int varLen = reader.readTwoByteIntLow() & 0xFFFF;
                typeInfo.setVarLen(varLen);
                dt.setValue(reader.readBytes(varSize - 4));
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownDatatype", new Object[] { this.dataType });
            }
        }
        this.value = dt;
    }
    
    @Override
    public int getSerializedSize() {
        int size = 4;
        if (this.value == MSSQLVariant.NULL_VALUE) {
            return size;
        }
        ++size;
        final MSSQLDataType dt = (MSSQLDataType)this.value;
        ++size;
        switch (this.dataType) {
            case 41:
            case 42:
            case 43: {
                ++size;
                break;
            }
            case -91:
            case -83:
            case 106:
            case 108: {
                size += 2;
                break;
            }
            case -89:
            case -81:
            case -25:
            case -17: {
                size += 7;
                break;
            }
        }
        size += dt.getVariantSize();
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        if (this.value == null || this.value == MSSQLVariant.NULL_VALUE) {
            writer.writeFourByteIntegerLow(0);
            return;
        }
        final MSSQLDataType dt = (MSSQLDataType)this.value;
        final int len = this.getSerializedSize() - 4;
        writer.writeFourByteIntegerLow(len);
        writer.writeByte(dt.getMeta().getTypeInfo().getType());
        switch (this.dataType) {
            case 41:
            case 42:
            case 43: {
                writer.writeByte((byte)1);
                break;
            }
            case -91:
            case -83:
            case 106:
            case 108: {
                writer.writeByte((byte)2);
                break;
            }
            case -89:
            case -81:
            case -25:
            case -17: {
                writer.writeByte((byte)7);
                break;
            }
            default: {
                writer.writeByte((byte)0);
                break;
            }
        }
        dt.writeVariant(writer);
    }
    
    @Override
    public Object getValue() {
        if (this.value == null || this.value == MSSQLVariant.NULL_VALUE) {
            return null;
        }
        final MSSQLDataType dt = (MSSQLDataType)this.value;
        return dt.getValue();
    }
    
    @Override
    public void setValueFromJS(final Value val) {
        this.changed = true;
        if (val == null || val.isNull()) {
            this.value = MSSQLVariant.NULL_VALUE;
            this.variantMeta = null;
            return;
        }
        if (this.variantMeta == null) {
            this.variantMeta = new ColumnMetadata(this.meta.getConnectionState());
            final TypeInfo typeInfo = new TypeInfo();
            if (val.isNumber()) {
                this.dataType = 106;
                BigDecimal bd = new BigDecimal(val.asDouble());
                bd = bd.multiply(new BigDecimal(1000000));
                bd = bd.round(MathContext.DECIMAL64);
                bd = bd.divide(new BigDecimal(1000000));
                typeInfo.setPrecision(bd.precision());
                typeInfo.setScale(bd.scale());
            }
            else if (val.isString()) {
                this.dataType = -25;
                typeInfo.setCollationLCID(1033);
                typeInfo.setCollationSortId((byte)52);
                typeInfo.setVarLen(val.asString().length() * 2);
            }
            else if (val.isBoolean()) {
                this.dataType = 50;
            }
            else if (val.isDate()) {
                this.dataType = 61;
            }
            else if (val.hasArrayElements()) {
                this.dataType = -91;
                typeInfo.setVarLen((int)val.getArraySize());
            }
            typeInfo.setType(this.dataType);
            this.variantMeta.setTypeInfo(typeInfo);
        }
        final MSSQLDataType dt = MSSQLDataType.createDataType(this.variantMeta);
        dt.setValueFromJS(val);
        this.value = dt;
    }
    
    public byte getDataType() {
        return this.dataType;
    }
}
