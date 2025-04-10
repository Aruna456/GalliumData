// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.mssql.tokens;

import org.apache.logging.log4j.LogManager;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.js.JSListWrapper;
import com.galliumdata.server.util.StringUtil;
import com.galliumdata.server.ServerException;
import java.io.IOException;
import com.galliumdata.server.log.Markers;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import com.galliumdata.server.handler.mssql.RawPacketWriter;

import java.util.Collections;
import java.util.Iterator;
import com.galliumdata.server.handler.mssql.UnableToParseException;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import java.util.ArrayList;
import com.galliumdata.server.handler.mssql.ConnectionState;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;
import java.util.List;
import java.util.function.Function;

public class TokenRow extends MessageToken
{
    protected List<ColumnMetadata> columnMetadata;
    protected TokenDataClassification dataClassification;
    protected List<MSSQLDataType> values;
    private static final Logger logicLog;
    
    public TokenRow(final ConnectionState connectionState) {
        super(connectionState);
        this.values = new ArrayList<MSSQLDataType>();
    }
    
    @Override
    public int readFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        for (int colIdx = 0; colIdx < this.columnMetadata.size(); ++colIdx) {
            final MSSQLDataType value = this.values.get(colIdx);
            idx += value.readFromBytes(bytes, idx);
        }
        return idx - offset;
    }
    
    @Override
    public void read(final RawPacketReader reader) {
        List<ColumnMetadata> colMeta = this.columnMetadata;
        if (colMeta.isEmpty()) {
            final TokenColMetadata metadata = this.connectionState.getLastMetadata();
            if (metadata == null) {
                throw new UnableToParseException("Row", "No metadata for result set");
            }
            colMeta = metadata.getColumns();
            if (colMeta.isEmpty()) {
                throw new UnableToParseException("Row", "Metadata for result set has no columns");
            }
            this.setColumnMetadata(colMeta);
        }
        for (int colIdx = 0; colIdx < colMeta.size(); ++colIdx) {
            final MSSQLDataType value = this.values.get(colIdx);
            value.read(reader);
        }
    }
    
    @Override
    public MessageToken duplicate() {
        final TokenRow dup = new TokenRow(this.connectionState);
        dup.columnMetadata = this.columnMetadata;
        for (final MSSQLDataType dt : this.values) {
            final MSSQLDataType dtCopy = MSSQLDataType.createDataType(dt.getMeta());
            dtCopy.setValue(dt.getValue());
            dup.values.add(dtCopy);
        }
        return dup;
    }
    
    protected int readBasicFromBytes(final byte[] bytes, final int offset, final int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        return idx - offset;
    }
    
    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        for (int colIdx = 0; colIdx < this.columnMetadata.size(); ++colIdx) {
            final MSSQLDataType val = this.values.get(colIdx);
            size += val.getSerializedSize();
        }
        return size;
    }
    
    @Override
    public void write(final RawPacketWriter writer) {
        super.write(writer);
        for (int colIdx = 0; colIdx < this.columnMetadata.size(); ++colIdx) {
            final MSSQLDataType val = this.values.get(colIdx);
            val.write(writer);
        }
    }
    
    protected void writeBasic(final RawPacketWriter writer) {
        super.write(writer);
    }
    
    @Override
    public byte getTokenType() {
        return -47;
    }
    
    @Override
    public String getTokenTypeName() {
        return "Row";
    }
    
    @Override
    public String toString() {
        String s = "Row ";
        for (int i = 0; i < this.columnMetadata.size(); ++i) {
            final ColumnMetadata col = this.columnMetadata.get(i);
            String colName = col.getColumnName();
            if (colName == null || colName.trim().length() == 0) {
                colName = "<no name>";
            }
            if (col.getTableName() != null && col.getTableName().size() > 0) {
                colName = col.getTableName().get(col.getTableName().size() - 1) + "." + colName;
            }
            Object value = this.values.get(i);
            if (value == null) {
                value = "<null>";
            }
            final String valueStr = value.toString();
            if (valueStr.length() > 50) {
                s = s + colName + "=" + valueStr.substring(0, 50) + "..., ";
            }
            else {
                s = s + colName + "=" + value + ", ";
            }
            if (s.length() > 100) {
                s = s.substring(0, 100) + "...";
                break;
            }
        }
        return s;
    }
    
    @Override
    public String toLongString() {
        String s = "Row ";
        for (int i = 0; i < this.columnMetadata.size(); ++i) {
            final ColumnMetadata col = this.columnMetadata.get(i);
            String colName = col.getColumnName();
            if (colName == null || colName.trim().length() == 0) {
                colName = "<no name>";
            }
            if (col.getTableName().size() > 0) {
                colName = col.getTableName().get(col.getTableName().size() - 1) + "." + colName;
            }
            Object value = this.values.get(i);
            if (value == null) {
                value = "<null>";
            }
            String valueStr = value.toString();
            if (valueStr.length() > 50) {
                valueStr = valueStr.substring(0, 50) + "...";
            }
            s = s + colName + "=" + valueStr + ", ";
        }
        return s;
    }
    
    public TokenRow clone() {
        final TokenRow newRow = new TokenRow(this.connectionState);
        newRow.columnMetadata = this.columnMetadata;
        newRow.dataClassification = this.dataClassification;
        for (final MSSQLDataType val : this.values) {
            newRow.values.add(val.clone());
        }
        return newRow;
    }
    
    public List<ColumnMetadata> getColumnMetadata() {
        return this.columnMetadata;
    }
    
    public void setColumnMetadata(final List<ColumnMetadata> columnMetadata) {
        this.columnMetadata = columnMetadata;
        this.values = new ArrayList<MSSQLDataType>(columnMetadata.size());
        for (final ColumnMetadata colMeta : columnMetadata) {
            final MSSQLDataType col = MSSQLDataType.createDataType(colMeta);
            this.values.add(col);
        }
    }
    
    public InputStream getJavaStream(final String name) {
        MSSQLDataType value = null;
        for (int i = 0; i < this.columnMetadata.size(); ++i) {
            final String colName = this.columnMetadata.get(i).getColumnName();
            if (name.equals(colName)) {
                value = this.values.get(i);
            }
        }
        if (value == null || value.getValue() == null) {
            return null;
        }
        final byte valueType = value.getMeta().getTypeInfo().getType();
        switch (valueType) {
            case -89:
            case -81:
            case -25:
            case -17:
            case 35:
            case 99: {
                final String stringValue = (String)value.getValue();
                return new ByteArrayInputStream(stringValue.getBytes(StandardCharsets.UTF_8));
            }
            case -91:
            case -83:
            case 37:
            case 45: {
                final byte[] byteValue = (byte[])value.getValue();
                return new ByteArrayInputStream(byteValue);
            }
            default: {
                return null;
            }
        }
    }
    
    public void setJavaStream(final String name, final InputStream stream) {
        MSSQLDataType value = null;
        for (int i = 0; i < this.columnMetadata.size(); ++i) {
            final String colName = this.columnMetadata.get(i).getColumnName();
            if (name.equals(colName)) {
                value = this.values.get(i);
            }
        }
        if (value == null || value.getValue() == null) {
            TokenRow.logicLog.debug(Markers.MSSQL, "Attempt to call setJavaStream on unknown column: " + name);
            return;
        }
        final byte valueType = value.getMeta().getTypeInfo().getType();
        switch (valueType) {
            case -89:
            case -81:
            case -25:
            case -17:
            case 35:
            case 99: {
                try {
                    final String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    value.setValue(text);
                }
                catch (final IOException ioex) {
                    TokenRow.logicLog.debug(Markers.MSSQL, "Exception in setJavaOutputStream: " + ioex.getMessage());
                }
                break;
            }
            case -91:
            case -83:
            case 37:
            case 45: {
                try {
                    value.setValue(stream.readAllBytes());
                }
                catch (final IOException ioex) {
                    TokenRow.logicLog.debug(Markers.MSSQL, "Exception in setJavaOutputStream: " + ioex.getMessage());
                }
                break;
            }
        }
    }
    
    public void setDataClassification(final TokenDataClassification token) {
        this.dataClassification = token;
    }
    
    public MSSQLDataType getColumnValue(final ColumnMetadata meta) {
        for (final MSSQLDataType val : this.values) {
            if (val.getMeta().equals(meta)) {
                return val;
            }
        }
        return null;
    }
    
    public Object getValue(final String colName) {
        for (final MSSQLDataType val : this.values) {
            if (val.getMeta().getColumnName().equals(colName)) {
                return val.getValue();
            }
        }
        throw new ServerException("db.mssql.logic.NoSuchColumn", colName);
    }
    
    public int getIndexOfColumn(final String colName) {
        for (int i = 0; i < this.columnMetadata.size(); ++i) {
            if (colName.equals(this.columnMetadata.get(i).getColumnName())) {
                return i;
            }
        }
        return -1;
    }
    
    public ColumnMetadata getMetadataForColumn(final String colName) {
        final int idx = this.getIndexOfColumn(colName);
        if (idx == -1) {
            return null;
        }
        return this.columnMetadata.get(idx);
    }
    
    public List<ClassificationEntry> getSensitivityPropertiesForColumn(final String colName) {
        if (this.dataClassification == null) {
            return new ArrayList<ClassificationEntry>();
        }
        final int idx = this.getIndexOfColumn(colName);
        if (idx == -1) {
            return null;
        }
        final List<ClassificationEntry> res = new ArrayList<ClassificationEntry>();
        final List<TokenDataClassification.SensitivityProperty> props = this.dataClassification.getSensitivityProperties().get(idx);
        for (final TokenDataClassification.SensitivityProperty prop : props) {
            final ClassificationEntry entry = new ClassificationEntry();
            entry.sensitivityLabel = this.dataClassification.getSensitivityLabels().get(prop.sensitivityLabelIndex).name;
            entry.informationType = this.dataClassification.getInformationTypes().get(prop.informationTypeIndex).name;
            entry.sensitivityRank = prop.sensitivityRank;
            res.add(entry);
        }
        return res;
    }
    
    public boolean columnHasSensitivity(final String columnName, final String sensitivity) {
        if (this.dataClassification == null) {
            return false;
        }
        final int idx = this.getIndexOfColumn(columnName);
        if (idx == -1) {
            return false;
        }
        final List<TokenDataClassification.SensitivityProperty> props = this.dataClassification.getSensitivityProperties().get(idx);
        for (final TokenDataClassification.SensitivityProperty prop : props) {
            final String sen = this.dataClassification.getSensitivityLabels().get(prop.sensitivityLabelIndex).name;
            if (sen.equals(sensitivity)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean columnHasInformationType(final String columnName, final String infoType) {
        if (this.dataClassification == null) {
            return false;
        }
        final int idx = this.getIndexOfColumn(columnName);
        if (idx == -1) {
            return false;
        }
        final List<TokenDataClassification.SensitivityProperty> props = this.dataClassification.getSensitivityProperties().get(idx);
        for (final TokenDataClassification.SensitivityProperty prop : props) {
            final String type = this.dataClassification.getInformationTypes().get(prop.informationTypeIndex).name;
            if (type.equals(infoType)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Object getMember(final String key) {
        for (int i = 0; i < this.columnMetadata.size(); ++i) {
            final String colName = this.columnMetadata.get(i).getColumnName();
            if (key.equals(colName)) {
                return this.values.get(i).getValue();
            }
        }
        if (StringUtil.stringIsInteger(key)) {
            final int colIdx = Integer.parseInt(key);
            return this.values.get(colIdx).getValue();
        }
        switch (key) {
            case "columnMetadata": {
                return new JSListWrapper(this.columnMetadata, () -> {});
            }
            case "dataClassification": {
                return this.dataClassification;
            }
            case "getIndexOfColumn": {
                return (Function<Value[],Object>) args -> this.getIndexOfColumn(args[0].asString());
            }
            case "getJavaStream": {
                return (Function<Value[],Object>) args -> this.getJavaStream(args[0].asString());
            }
            case "setJavaStream": {
                return (Function<Value[],Object>) args -> {
                    this.setJavaStream(args[0].asString(), (InputStream)args[1].asHostObject());
                    return null;
                };
            }
            case "getMetadataForColumn": {
                return (Function<Value[],Object>) args -> this.getMetadataForColumn(args[0].asString());
            }
            case "getSensitivityPropertiesForColumn": {
                return (Function<Value[],Object>) args -> this.getSensitivityPropertiesForColumn(args[0].asString());
            }
            case "columnHasSensitivity": {
                return (Function<Value[],Object>) args -> this.columnHasSensitivity(args[0].asString(), args[1].asString());
            }
            case "columnHasInformationType": {
                return (Function<Value[],Object>) args -> this.columnHasInformationType(args[0].asString(), args[1].asString());
            }
            case "clone": {
                return (Function<Value[],Object>) args -> this.clone();
            }
            case "remove": {
                return (Function<Value[],Object>) args -> {
                    this.remove();
                    return null;
                };
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
        for (int i = 0; i < this.columnMetadata.size(); ++i) {
            keys.add("" + i);
        }
        keys.add("columnMetadata");
        keys.add("dataClassification");
        keys.add("getIndexOfColumn");
        keys.add("getJavaStream");
        keys.add("setJavaStream");
        keys.add("getMetadataForColumn");
        keys.add("getSensitivityPropertiesForColumn");
        keys.add("columnHasSensitivity");
        keys.add("columnHasInformationType");
        keys.add("clone");
        keys.add("remove");
        for (final ColumnMetadata colMeta : this.columnMetadata) {
            final String colName = colMeta.getColumnName();
            if (colName != null) {
                keys.add(colName);
            }
        }
        return keys.toArray();
    }
    
    @Override
    public boolean hasMember(final String key) {
        for (final ColumnMetadata colMeta : this.columnMetadata) {
            if (key.equals(colMeta.getColumnName())) {
                return true;
            }
        }
        if (StringUtil.stringIsInteger(key)) {
            final int colIdx = Integer.parseInt(key);
            return colIdx >= 0 && colIdx < this.columnMetadata.size();
        }
        switch (key) {
            case "columnMetadata":
            case "dataClassification":
            case "getIndexOfColumn":
            case "getJavaStream":
            case "setJavaStream":
            case "getMetadataForColumn":
            case "getSensitivityPropertiesForColumn":
            case "columnHasSensitivity":
            case "columnHasInformationType":
            case "clone":
            case "remove": {
                return true;
            }
            default: {
                return super.hasMember(key);
            }
        }
    }
    
    @Override
    public void putMember(final String key, final Value value) {
        for (int i = 0; i < this.columnMetadata.size(); ++i) {
            final String colName = this.columnMetadata.get(i).getColumnName();
            if (key.equals(colName)) {
                this.values.get(i).setValueFromJS(value);
                this.modified = true;
                return;
            }
        }
        super.putMember(key, value);
    }
    
    @Override
    public boolean removeMember(final String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "Row token");
    }
    
    static {
        logicLog = LogManager.getLogger("galliumdata.uselog");
    }
    
    public static class ClassificationEntry
    {
        public String sensitivityLabel;
        public String informationType;
        public int sensitivityRank;
    }
}
