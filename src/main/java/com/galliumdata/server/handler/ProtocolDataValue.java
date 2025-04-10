// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler;

import org.graalvm.polyglot.Value;
//import com.galliumdata.server.handler.mysql.MySQLDateTime;
import org.bson.types.ObjectId;
import java.util.Date;

public class ProtocolDataValue extends ProtocolData
{
    private Object value;
    private ProtocolDataType type;
    
    public ProtocolDataValue(final String s) {
        this.value = s;
        this.type = ProtocolDataType.STRING;
    }
    
    public ProtocolDataValue(final Number n) {
        this.value = n;
        if (n == null) {
            this.type = null;
            return;
        }
        if (n instanceof Integer || n instanceof Long) {
            this.type = ProtocolDataType.INTEGER;
        }
        else if (n instanceof Float || n instanceof Double) {
            this.type = ProtocolDataType.FLOAT;
        }
        else {
            if (!(n instanceof Byte)) {
                throw new RuntimeException("Unknown number type in JSON: " + String.valueOf(n.getClass()));
            }
            this.type = ProtocolDataType.BYTE;
        }
    }
    
    public ProtocolDataValue(final Boolean b) {
        this.value = b;
        this.type = ProtocolDataType.BOOLEAN;
    }
    
    public ProtocolDataValue(final Object obj) {
        this.value = obj;
        if (obj == null) {
            this.type = null;
            return;
        }
        if (obj instanceof String) {
            this.type = ProtocolDataType.STRING;
        }
        else if (obj instanceof Integer || obj instanceof Long) {
            this.type = ProtocolDataType.INTEGER;
        }
        else if (obj instanceof Float || obj instanceof Double) {
            this.type = ProtocolDataType.FLOAT;
        }
        else if (obj instanceof Boolean) {
            this.type = ProtocolDataType.BOOLEAN;
        }
        else if (obj instanceof Date) {
            this.type = ProtocolDataType.DATE;
        }
        else if (obj instanceof ObjectId) {
            this.type = ProtocolDataType.OBJECT_ID;
        }
//        else {
//            if (!(obj instanceof MySQLDateTime)) {
//                throw new RuntimeException("Unsupported type for JSON: " + String.valueOf(obj.getClass()));
//            }
//            this.type = ProtocolDataType.DATE;
//        }
    }
    
    public ProtocolDataValue(final Object obj, final ProtocolDataType type) {
        this.value = obj;
        this.type = type;
    }
    
    @Override
    public boolean isObject() {
        return false;
    }
    
    @Override
    public boolean isArray() {
        return false;
    }
    
    @Override
    public boolean isValue() {
        return true;
    }
    
    @Override
    public int getSize() {
        return 0;
    }
    
    @Override
    public String toJSON() {
        if (this.value == null) {
            return "null";
        }
        if (this.value instanceof String) {
            String s = (String)this.value;
            s = s.replaceAll("\\\\", "\\\\\\\\");
            s = s.replaceAll("\n", "\\\\n");
            s = s.replaceAll("\r", "\\\\r");
            s = s.replaceAll("\t", "\\\\t");
            s = s.replaceAll("\\t|\\xA0|\\u1680|\\u180e|[\\u2000-\\u200a]|\\u202f|\\u205f|\\u3000", "\\\\t");
            s = s.replaceAll("\\v", "\\\\n");
            s = s.replaceAll("\\\"", "\\\\\"");
            return "\"" + s;
        }
        if (this.value instanceof Number) {
            return this.value.toString();
        }
        if (this.value instanceof Boolean) {
            return this.value.toString();
        }
        if (this.value instanceof byte[]) {
            final byte[] byteArray = (byte[])this.value;
            String s2 = "";
            for (int i = 0; i < byteArray.length; ++i) {
                if (i > 0) {
                    s2 = s2;
                }
                s2 += byteArray[i];
            }
            return s2;
        }
        if (this.value instanceof Date) {
            return this.value.toString();
        }
        throw new ProtocolException("core.UnknownDataType", new Object[] { this.value.getClass().getName() });
    }
    
    @Override
    public String toPrettyJSON(final int padding) {
        return this.toJSON();
    }
    
    @Override
    public String toJSONDebug(final int maxValueLen, final int maxMembers) {
        if (this.value == null) {
            return "null";
        }
        if (this.value instanceof String) {
            String s = (String)this.value;
            if (s.length() >= maxValueLen) {
                s = s.substring(0, maxValueLen);
            }
            s = s.replaceAll("\\\\", "\\\\\\\\");
            s = s.replaceAll("\n", "\\\\n");
            s = s.replaceAll("\r", "\\\\r");
            s = s.replaceAll("\\\"", "\\\\\"");
            return "\"" + s;
        }
        if (this.value instanceof Number) {
            return this.value.toString();
        }
        if (this.value instanceof Boolean) {
            return this.value.toString();
        }
        if (this.value instanceof byte[]) {
            final byte[] byteArray = (byte[])this.value;
            String s2 = "";
            for (int i = 0; i < byteArray.length; ++i) {
                if (i > 0) {
                    s2 = s2;
                }
                s2 += byteArray[i];
                if (i > maxMembers) {
                    s2 = s2 + ", \"(" + (byteArray.length - i) + " more)\"";
                    break;
                }
            }
            return s2;
        }
        if (this.value instanceof Date) {
            return this.value.toString();
        }
        throw new ProtocolException("core.UnknownDataType", new Object[] { this.value.getClass().getName() });
    }
    
    @Override
    public ProtocolData get(final String name) {
        throw new ProtocolException("", new Object[0]);
    }
    
    @Override
    public void put(final String name, final ProtocolData value) {
        throw new ProtocolException("Not implemented", new Object[0]);
    }
    
    @Override
    public ProtocolData remove(final String name) {
        throw new ProtocolException("Not implemented", new Object[0]);
    }
    
    @Override
    public boolean hasProperty(final String name) {
        throw new ProtocolException("Not implemented", new Object[0]);
    }
    
    @Override
    public String asString() {
        return this.value.toString();
    }
    
    public Number asNumber() {
        if (this.value == null) {
            throw new ProtocolException("Number is null", new Object[0]);
        }
        if (!(this.value instanceof Number)) {
            throw new ProtocolException("Not an instance of Number", new Object[0]);
        }
        return (Number)this.value;
    }
    
    @Override
    public boolean asBoolean() {
        if (this.value == null) {
            throw new ProtocolException("Boolean is null", new Object[0]);
        }
        if (!(this.value instanceof Boolean)) {
            throw new ProtocolException("Not an instance of Boolean", new Object[0]);
        }
        return (boolean)this.value;
    }
    
    public Object asRawObject() {
        return this.value;
    }
    
    public Object getMember(final String key) {
        throw new RuntimeException("Cannot call getMember on ProtocolDataValue");
    }
    
    public Object getMemberKeys() {
        return new String[0];
    }
    
    public boolean hasMember(final String key) {
        return false;
    }
    
    public void putMember(final String key, final Value value) {
        throw new RuntimeException("Cannot call putMember on ProtocolDataValue");
    }
    
    public boolean removeMember(final String key) {
        throw new RuntimeException("Cannot call removeMember on ProtocolDataValue");
    }
    
    enum ProtocolDataType
    {
        STRING, 
        DATE, 
        INTEGER, 
        FLOAT, 
        BYTE, 
        BOOLEAN, 
        OBJECT_ID;
    }
}
