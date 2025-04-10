// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import java.util.Collection;
import java.util.Arrays;
import com.galliumdata.server.js.JSListWrapper;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import java.util.List;
import java.util.function.Function;

public class TokenFeatureExtAck extends MessageToken
{
    private final List<FeatureAck> featureAcks;
    
    public TokenFeatureExtAck(final ConnectionState connectionState) {
        super(connectionState);
        this.featureAcks = new ArrayList<FeatureAck>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        while (bytes[idx] != -1 && idx - offset < numBytes) {
            FeatureAck featureAck = null;
            switch (bytes[idx]) {
                case 4: {
                    featureAck = new FeatureAckColumnEncryption(this.connectionState);
                    break;
                }
                case 9: {
                    featureAck = new FeatureAckDataClassification(this.connectionState);
                    break;
                }
                case 10: {
                    featureAck = new FeatureAckUTF8Support(this.connectionState);
                    break;
                }
                default: {
                    featureAck = new FeatureAckUnknown(this.connectionState, bytes[idx]);
                    break;
                }
            }
            idx += featureAck.readFromBytes(bytes, idx, numBytes - (idx - offset));
            this.featureAcks.add(featureAck);
        }
        if (bytes[idx] == -1) {
            ++idx;
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        for (byte type = reader.readByte(); type != -1; type = reader.readByte()) {
            FeatureAck featureAck = null;
            switch (type) {
                case 1: {
                    featureAck = new FeatureAckSessionRecovery(this.connectionState);
                    break;
                }
                case 2: {
                    featureAck = new FeatureAckFedAuth(this.connectionState);
                    break;
                }
                case 4: {
                    featureAck = new FeatureAckColumnEncryption(this.connectionState);
                    break;
                }
                case 5: {
                    featureAck = new FeatureAckGlobalTransactions(this.connectionState);
                    break;
                }
                case 8: {
                    featureAck = new FeatureAckAzureSQLSupport(this.connectionState);
                    break;
                }
                case 9: {
                    featureAck = new FeatureAckDataClassification(this.connectionState);
                    break;
                }
                case 10: {
                    featureAck = new FeatureAckUTF8Support(this.connectionState);
                    break;
                }
                case 11: {
                    featureAck = new FeatureAckAzureSQLDNSCaching(this.connectionState);
                    break;
                }
                case 13: {
                    featureAck = new FeatureAckJSONSupport(this.connectionState);
                    break;
                }
                default: {
                    featureAck = new FeatureAckUnknown(this.connectionState, type);
                    break;
                }
            }
            featureAck.read(reader);
            this.featureAcks.add(featureAck);
            if (featureAck instanceof FeatureAckDataClassification fadc) {
                this.connectionState.setDataClassificationVersion(fadc.getVersion());
            }
            if (featureAck instanceof FeatureAckColumnEncryption) {
                this.connectionState.setColumnEncryptionInUse(true);
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        for (final FeatureAck ack : this.featureAcks) {
            size += ack.getSerializedSize();
        }
        return ++size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        for (final FeatureAck ack : this.featureAcks) {
            if (!ack.removed) {
                ack.write(writer);
            }
        }
        writer.writeByte((byte)(-1));
    }
    
    @Override
    public byte getTokenType() {
        return -82;
    }
    
    @Override
    public String getTokenTypeName() {
        return "FeatureExtAck";
    }
    
    @Override
    public String toString() {
        String s = "FeatureExtAck: ";
        for (FeatureAck ack : this.featureAcks) {
            s = s + ack.getFeatureTypeName() + ", ";
        }
        return s;
    }
    
    public FeatureAck getFeatureAck(final String type) {
        for (final FeatureAck ack : this.featureAcks) {
            if (ack.getFeatureTypeName().equals(type)) {
                return ack;
            }
        }
        return null;
    }
    
    public FeatureAck removeFeatureAck(final String type) {
        FeatureAck found = null;
        for (final FeatureAck ack : this.featureAcks) {
            if (ack.getFeatureTypeName().equals(type)) {
                found = ack;
                break;
            }
        }
        if (found != null) {
            this.featureAcks.remove(found);
        }
        if (this.featureAcks.isEmpty()) {
            this.remove();
        }
        return found;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "featureAcks": {
                return new JSListWrapper(this.featureAcks, () -> {});
            }
            case "getFeatureAck": {
                return (Function<Value[],Object>) args -> this.getFeatureAck(args[0].asString());
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        final String[] parentKeys = (String[])super.getMemberKeys();
        final List<String> keys = new ArrayList<String>(Arrays.asList(parentKeys));
        keys.add("featureAcks");
        keys.add("getFeatureAck");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "featureAcks":
            case "getFeatureAck": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        super.putMember(key, value);
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "FeatureExtAck token");
    }
}
