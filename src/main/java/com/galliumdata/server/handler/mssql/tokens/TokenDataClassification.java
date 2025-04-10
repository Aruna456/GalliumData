// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import com.galliumdata.server.ServerException;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.js.JSListWrapper;

import java.util.Collections;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import java.util.List;

public class TokenDataClassification extends MessageToken
{
    private final List<NameId> sensitivityLabels;
    private final List<NameId> informationTypes;
    private int sensitivityRank;
    private final List<List<SensitivityProperty>> sensitivityProperties;
    
    public TokenDataClassification(final ConnectionState connectionState) {
        super(connectionState);
        this.sensitivityLabels = new ArrayList<NameId>();
        this.informationTypes = new ArrayList<NameId>();
        this.sensitivityProperties = new ArrayList<List<SensitivityProperty>>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        throw new RuntimeException("Not implemented");
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        for (int numSensitivityLabels = reader.readTwoByteIntLow(), i = 0; i < numSensitivityLabels; ++i) {
            final NameId lbl = new NameId();
            int len = reader.readByte();
            lbl.name = reader.readString(len * 2);
            len = reader.readByte();
            lbl.id = reader.readString(len * 2);
            this.sensitivityLabels.add(lbl);
        }
        for (int numInfoTypes = reader.readTwoByteIntLow(), j = 0; j < numInfoTypes; ++j) {
            final NameId lbl2 = new NameId();
            int len2 = reader.readByte();
            lbl2.name = reader.readString(len2 * 2);
            len2 = reader.readByte();
            lbl2.id = reader.readString(len2 * 2);
            this.informationTypes.add(lbl2);
        }
        if (this.connectionState.getDataClassificationVersion() == 2) {
            this.sensitivityRank = reader.readFourByteIntLow();
        }
        for (int numRSCols = reader.readTwoByteIntLow(), k = 0; k < numRSCols; ++k) {
            final List<SensitivityProperty> props = new ArrayList<SensitivityProperty>();
            this.sensitivityProperties.add(props);
            for (int numProps = reader.readTwoByteIntLow(), l = 0; l < numProps; ++l) {
                final SensitivityProperty prop = new SensitivityProperty();
                prop.sensitivityLabelIndex = reader.readTwoByteIntLow();
                prop.informationTypeIndex = reader.readTwoByteIntLow();
                if (this.connectionState.getDataClassificationVersion() == 2) {
                    prop.sensitivityRank = reader.readFourByteIntLow();
                }
                props.add(prop);
            }
        }
    }
    
    @Override
    public int getSerializedSize() {
        throw new RuntimeException("Not implemented");
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        writer.writeTwoByteIntegerLow(this.sensitivityLabels.size());
        for (final NameId lbl : this.sensitivityLabels) {
            byte[] strBytes = lbl.name.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(strBytes.length / 2));
            writer.writeBytes(strBytes, 0, strBytes.length);
            strBytes = lbl.id.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(strBytes.length / 2));
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
        writer.writeTwoByteIntegerLow(this.informationTypes.size());
        for (final NameId lbl : this.informationTypes) {
            byte[] strBytes = lbl.name.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(strBytes.length / 2));
            writer.writeBytes(strBytes, 0, strBytes.length);
            strBytes = lbl.id.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(strBytes.length / 2));
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
        if (this.connectionState.getDataClassificationVersion() == 2) {
            writer.writeFourByteIntegerLow(this.sensitivityRank);
        }
        writer.writeTwoByteIntegerLow(this.sensitivityProperties.size());
        for (final List<SensitivityProperty> props : this.sensitivityProperties) {
            writer.writeTwoByteIntegerLow(props.size());
            for (final SensitivityProperty prop : props) {
                writer.writeTwoByteIntegerLow(prop.sensitivityLabelIndex);
                writer.writeTwoByteIntegerLow(prop.informationTypeIndex);
                if (this.connectionState.getDataClassificationVersion() == 2) {
                    writer.writeFourByteIntegerLow(prop.sensitivityRank);
                }
            }
        }
    }
    
    @Override
    public byte getTokenType() {
        return -93;
    }
    
    @Override
    public String getTokenTypeName() {
        return "DataClassification";
    }
    
    @Override
    public String toString() {
        final String s = "Data classification";
        return s;
    }
    
    public List<NameId> getSensitivityLabels() {
        return this.sensitivityLabels;
    }
    
    public List<NameId> getInformationTypes() {
        return this.informationTypes;
    }
    
    public int getSensitivityRank() {
        return this.sensitivityRank;
    }
    
    public void setSensitivityRank(final int sensitivityRank) {
        this.sensitivityRank = sensitivityRank;
    }
    
    public List<List<SensitivityProperty>> getSensitivityProperties() {
        return this.sensitivityProperties;
    }
    
    @Override
    public Object getMember(final String key) {
        switch (key) {
            case "sensitivityLabels": {
                return new JSListWrapper(this.sensitivityLabels, () -> {});
            }
            case "informationTypes": {
                return new JSListWrapper(this.informationTypes, () -> {});
            }
            case "sensitivityRank": {
                return this.sensitivityRank;
            }
            case "sensitivityProperties": {
                return new JSListWrapper(this.sensitivityProperties, () -> {});
            }
            default: {
                return super.getMember(key);
            }
        }
    }
    
    @Override
    public Object getMemberKeys() {
        final String[] parentKeys = (String[])super.getMemberKeys();
        final List<String> keys = new ArrayList<String>();
        Collections.addAll(keys, parentKeys);
        keys.add("sensitivityLabels");
        keys.add("informationTypes");
        keys.add("sensitivityRank");
        keys.add("sensitivityProperties");
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        switch (key) {
            case "sensitivityLabels":
            case "informationTypes":
            case "sensitivityRank":
            case "sensitivityProperties": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        switch (key) {
            case "sensitivityRank": {
                this.sensitivityRank = value.asInt();
                return;
            }
            default: {
                super.putMember(key, value);
            }
        }
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "Data classification token");
    }
    
    public static class NameId
    {
        public String name;
        public String id;
    }
    
    public static class SensitivityProperty
    {
        public int sensitivityLabelIndex;
        public int informationTypeIndex;
        public int sensitivityRank;
    }
}
