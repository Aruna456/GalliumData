// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql;

import java.util.Iterator;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.tokens.TokenDataClassification;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import java.util.List;

public class MessagePacket extends MSSQLPacket
{
    private List<MessageToken> tokens;
    private TokenColMetadata currentMetadata;
    private TokenDataClassification currentDataClassification;
    
    public MessagePacket(final ConnectionState connectionState) {
        super(connectionState);
        this.tokens = new ArrayList<MessageToken>();
        this.typeCode = 4;
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        while (idx - offset < numBytes && idx - offset < this.length) {
            final MessageToken token = MessageToken.createToken(bytes[idx], this.connectionState);
            if (token.getTokenType() == -47 || token.getTokenType() == -46) {
                ((TokenRow)token).setColumnMetadata(this.currentMetadata.getColumns());
            }
            idx += token.readFromBytes(bytes, idx, numBytes - (idx - offset));
            this.tokens.add(token);
            if (token.getTokenType() == -127) {
                this.currentMetadata = (TokenColMetadata)token;
            }
        }
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
            if (this.currentMetadata == null) {
                throw new UnableToParseException("Row/NBCRow", "Mo current metadata");
            }
            ((TokenRow)token).setColumnMetadata(this.currentMetadata.getColumns());
            ((TokenRow)token).setDataClassification(this.currentDataClassification);
        }
        token.read(reader);
        if (token.getTokenType() == -127) {
            this.currentMetadata = (TokenColMetadata)token;
            this.currentDataClassification = null;
        }
        if (token.getTokenType() == -93) {
            this.currentDataClassification = (TokenDataClassification)token;
        }
        return token;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        for (final MessageToken token : this.tokens) {
            size += token.getSerializedSize();
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        this.tokens.forEach(t -> t.write(writer));
    }
    
    @Override
    public String getPacketType() {
        return "Message";
    }
    
    public List<MessageToken> getTokens() {
        return this.tokens;
    }
    
    public void setTokens(final List<MessageToken> tokens) {
        this.tokens = tokens;
    }
}
