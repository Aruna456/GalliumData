// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.DataTypeReader;

public class EncryptionKeyValue
{
    private byte[] encryptedKey;
    private String keystoreName;
    private String keyPath;
    private String asymmetricAlgorithm;
    
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        final int keyLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        System.arraycopy(bytes, idx, this.encryptedKey = new byte[keyLen], 0, keyLen);
        idx += keyLen;
        byte strLen = bytes[idx];
        ++idx;
        this.keystoreName = new String(bytes, idx, strLen * 2, StandardCharsets.UTF_16LE);
        idx += strLen * 2;
        final int keyPathLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        this.keyPath = new String(bytes, idx, keyPathLen * 2, StandardCharsets.UTF_16LE);
        idx += keyPathLen * 2;
        strLen = bytes[idx];
        ++idx;
        this.asymmetricAlgorithm = new String(bytes, idx, strLen * 2, StandardCharsets.UTF_16LE);
        idx += strLen * 2;
        return idx - offset;
    }
    
    public void read(final RawPacketReader reader) {
        final int keyLen = reader.readTwoByteIntLow();
        this.encryptedKey = reader.readBytes(keyLen);
        final byte strLen = reader.readByte();
        this.keystoreName = reader.readString(strLen * 2);
        final short keyPathLen = reader.readTwoByteIntLow();
        this.keyPath = reader.readString(keyPathLen * 2);
        final byte algLen = reader.readByte();
        this.asymmetricAlgorithm = reader.readString(algLen * 2);
    }
    
    public void write(final RawPacketWriter writer) {
        writer.writeTwoByteIntegerLow(this.encryptedKey.length);
        writer.writeBytes(this.encryptedKey, 0, this.encryptedKey.length);
        final byte[] keystoreNameBytes = this.keystoreName.getBytes(StandardCharsets.UTF_16LE);
        writer.writeByte((byte)this.keystoreName.length());
        writer.writeBytes(keystoreNameBytes, 0, keystoreNameBytes.length);
        final byte[] keyPathBytes = this.keyPath.getBytes(StandardCharsets.UTF_16LE);
        writer.writeTwoByteIntegerLow((short)this.keyPath.length());
        writer.writeBytes(keyPathBytes, 0, keyPathBytes.length);
        final byte[] algBytes = this.asymmetricAlgorithm.getBytes(StandardCharsets.UTF_16LE);
        writer.writeByte((byte)this.asymmetricAlgorithm.length());
        writer.writeBytes(algBytes, 0, algBytes.length);
    }
    
    public byte[] getEncryptedKey() {
        return this.encryptedKey;
    }
    
    public void setEncryptedKey(final byte[] encryptedKey) {
        this.encryptedKey = encryptedKey;
    }
    
    public String getKeystoreName() {
        return this.keystoreName;
    }
    
    public void setKeystoreName(final String keystoreName) {
        this.keystoreName = keystoreName;
    }
    
    public String getKeyPath() {
        return this.keyPath;
    }
    
    public void setKeyPath(final String keyPath) {
        this.keyPath = keyPath;
    }
    
    public String getAsymmetricAlgorithm() {
        return this.asymmetricAlgorithm;
    }
    
    public void setAsymmetricAlgorithm(final String asymmetricAlgorithm) {
        this.asymmetricAlgorithm = asymmetricAlgorithm;
    }
}
