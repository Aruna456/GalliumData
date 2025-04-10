// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.ConnectionState;

public class FeatureAckFedAuth extends FeatureAck
{
    private byte[] nonce;
    private byte[] signature;
    
    public FeatureAckFedAuth(final ConnectionState connectionState) {
        super(connectionState);
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        final int length = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (length == 0) {
            return idx - offset;
        }
        if (length != 64) {
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { "FedAuth", 64, length });
        }
        System.arraycopy(bytes, idx, this.nonce = new byte[32], 0, 32);
        idx += length;
        System.arraycopy(bytes, idx, this.signature = new byte[32], 0, 32);
        idx += length;
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        final int length = reader.readFourByteIntLow();
        if (length == 0) {
            return;
        }
        if (length != 64) {
            throw new ServerException("db.mssql.protocol.InvalidLengthForLoginFeature", new Object[] { "FedAuth", 64, length });
        }
        this.nonce = reader.readBytes(32);
        this.signature = reader.readBytes(32);
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.nonce != null) {
            size += this.nonce.length;
        }
        if (this.signature != null) {
            size += this.signature.length;
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        if (this.nonce == null) {
            writer.writeFourByteIntegerLow(0);
            return;
        }
        writer.writeBytes(this.nonce, 0, this.nonce.length);
        writer.writeBytes(this.signature, 0, this.signature.length);
    }
    
    @Override
    public byte getFeatureType() {
        return 2;
    }
    
    @Override
    public String getFeatureTypeName() {
        return "FedAuth";
    }
    
    public byte[] getNonce() {
        return this.nonce;
    }
    
    public void setNonce(final byte[] n) {
        this.nonce = n;
    }
    
    public byte[] getSignature() {
        return this.signature;
    }
    
    public void setSignature(final byte[] s) {
        this.signature = s;
    }
}
