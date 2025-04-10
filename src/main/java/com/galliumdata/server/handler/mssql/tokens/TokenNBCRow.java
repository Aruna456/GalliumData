// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.List;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class TokenNBCRow extends TokenRow
{
    public TokenNBCRow(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    public TokenNBCRow(final TokenRow row) {
        super(row.connectionState);
        this.columnMetadata = row.columnMetadata;
        for (final MSSQLDataType value : row.values) {
            if (!value.isNull()) {
                this.values.add(value);
            }
        }
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        int nullBitmapIdx;
        idx = (nullBitmapIdx = idx + super.readBasicFromBytes(bytes, idx, numBytes));
        int nullBitmapSize = this.columnMetadata.size() / 8;
        if (this.columnMetadata.size() % 8 > 0) {
            ++nullBitmapSize;
        }
        idx += nullBitmapSize;
        int colNum = 0;
        for (int colIdx = 0; colIdx < this.columnMetadata.size(); ++colIdx) {
            final ColumnMetadata meta = this.columnMetadata.get(colIdx);
            final MSSQLDataType columnValue = this.values.get(colIdx);
            if ((bytes[nullBitmapIdx] & 1 << colNum) != 0x0) {
                columnValue.setNullNoChange();
            }
            else {
                idx += columnValue.readFromBytes(bytes, idx);
            }
            if (++colNum >= 8) {
                colNum = 0;
                ++nullBitmapIdx;
            }
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        List<ColumnMetadata> colMeta = this.columnMetadata;
        if (colMeta.isEmpty()) {
            colMeta = this.connectionState.getLastMetadata().getColumns();
            if (colMeta.isEmpty()) {
                throw new ServerException("db.mssql.protocol.NoResultSetMetadata", new Object[0]);
            }
            this.setColumnMetadata(colMeta);
        }
        int nullBitmapSize = this.columnMetadata.size() / 8;
        if (this.columnMetadata.size() % 8 > 0) {
            ++nullBitmapSize;
        }
        final byte[] nullBitmap = reader.readBytes(nullBitmapSize);
        int colNum = 0;
        int nullBitmapIdx = 0;
        for (int colIdx = 0; colIdx < this.columnMetadata.size(); ++colIdx) {
            final MSSQLDataType columnValue = this.values.get(colIdx);
            if ((nullBitmap[nullBitmapIdx] & 1 << colNum) != 0x0) {
                columnValue.setNullNoChange();
            }
            else {
                columnValue.read(reader);
            }
            if (++colNum >= 8) {
                colNum = 0;
                ++nullBitmapIdx;
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = 1;
        size += this.columnMetadata.size() / 8;
        if (this.columnMetadata.size() % 8 > 0) {
            ++size;
        }
        for (int colIdx = 0; colIdx < this.columnMetadata.size(); ++colIdx) {
            final MSSQLDataType val = this.values.get(colIdx);
            if (!val.isNull()) {
                size += val.getSerializedSize();
            }
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.writeBasic(writer);
        int nullBitmapSize = this.columnMetadata.size() / 8;
        if (this.columnMetadata.size() % 8 > 0) {
            ++nullBitmapSize;
        }
        final byte[] nullBitmap = new byte[nullBitmapSize];
        int idx = 0;
        int bitIdx = 0;
        for (int colIdx = 0; colIdx < this.columnMetadata.size(); ++colIdx) {
            final MSSQLDataType val = this.values.get(colIdx);
            if (val.isNull()) {
                final byte[] array = nullBitmap;
                final int n = idx;
                array[n] |= (byte)(1 << bitIdx);
            }
            if (++bitIdx >= 8) {
                bitIdx = 0;
                ++idx;
            }
        }
        writer.writeBytes(nullBitmap, 0, nullBitmap.length);
        for (int colIdx = 0; colIdx < this.columnMetadata.size(); ++colIdx) {
            final MSSQLDataType val = this.values.get(colIdx);
            if (!val.isNull()) {
                val.write(writer);
            }
        }
    }
    
    @Override
    public byte getTokenType() {
        return -46;
    }
    
    @Override
    public String getTokenTypeName() {
        return "NBCRow";
    }
    
    @Override
    public MessageToken duplicate() {
        final TokenNBCRow dup = new TokenNBCRow(this.connectionState);
        dup.columnMetadata = this.columnMetadata;
        for (final MSSQLDataType dt : this.values) {
            final MSSQLDataType dtCopy = MSSQLDataType.createDataType(dt.getMeta());
            dtCopy.setValue(dt.getValue());
            dup.values.add(dtCopy);
        }
        return dup;
    }
}
