// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.Iterator;
import java.util.List;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenDone;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;

public class BulkLoadBCPPacket extends MSSQLPacket
{
    private TokenColMetadata currentMetadata;
    
    public BulkLoadBCPPacket(final ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = 7;
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        final int idx = offset;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        super.read(reader);
    }
    
    public MessageToken readNextToken(final RawPacketReader reader) {
        if (reader.isDone()) {
            return null;
        }
        final byte tokenType = reader.readByte();
        final MessageToken token = MessageToken.createToken(tokenType, this.connectionState);
        if (token.getTokenType() == -47 || token.getTokenType() == -46) {
            ((TokenRow)token).setColumnMetadata(this.currentMetadata.getColumns());
        }
        if (token.getTokenType() == -3 && (!this.connectionState.tdsVersion72andHigher() || reader.getNumUnreadBytes() == 8)) {
            ((TokenDone)token).setUseShortRowCount(true);
        }
        token.read(reader);
        if (token.getTokenType() == -127) {
            this.currentMetadata = (TokenColMetadata)token;
        }
        return token;
    }
    
    @Override
    public int getSerializedSize() {
        final int size = super.getSerializedSize();
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
    }
    
    @Override
    public String getPacketType() {
        return "BulkLoadBCP";
    }
    
    @Override
    public String toString() {
        if (this.currentMetadata != null) {
            final List<ColumnMetadata> colMetas = this.currentMetadata.getColumns();
            if (colMetas != null && !colMetas.isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                for (final ColumnMetadata colMeta : colMetas) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(colMeta.getColumnName());
                    sb.append(" (");
                    sb.append(colMeta.getTypeInfo().getTypeName());
                    sb.append(")");
                    if (sb.length() > 100) {
                        break;
                    }
                }
                return "BulkLoadBCP: " + sb.toString();
            }
        }
        return "BulkLoadBCP";
    }
}
