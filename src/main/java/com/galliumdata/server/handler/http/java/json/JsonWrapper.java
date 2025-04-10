// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java.json;

import java.util.Iterator;
import java.util.Set;
import com.fasterxml.jackson.databind.node.DecimalNode;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import org.graalvm.polyglot.Value;
import com.galliumdata.server.ServerException;
import com.fasterxml.jackson.databind.JsonNode;

public abstract class JsonWrapper
{
    protected JsonWrapper parent;
    protected boolean changed;
    
    public static Object wrap(final JsonWrapper parent, final JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return new JsonArrayWrapper(parent, node);
        }
        if (node.isObject()) {
            return new JsonObjectWrapper(parent, node);
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isFloatingPointNumber() || node.isFloat()) {
            return node.asDouble();
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        throw new ServerException("db.http.logic.UnsupportedJsonValue", new Object[] { node });
    }
    
    public static JsonNode convertJS(final Value value) {
        if (value == null || value.isNull()) {
            return (JsonNode)NullNode.getInstance();
        }
        if (value.hasArrayElements()) {
            final ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
            for (long size = value.getArraySize(), i = 0L; i < size; ++i) {
                final Value childValue = value.getArrayElement(i);
                arrayNode.add(convertJS(childValue));
            }
            return (JsonNode)arrayNode;
        }
        if (value.hasMembers()) {
            final ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
            final Set<String> keys = value.getMemberKeys();
            for (final String key : keys) {
                final Value childValue2 = value.getMember(key);
                objectNode.set(key, convertJS(childValue2));
            }
            return (JsonNode)objectNode;
        }
        if (value.isString() || value.isTimeZone()) {
            return (JsonNode)new TextNode(value.asString());
        }
        if (value.isBoolean()) {
            return (JsonNode)BooleanNode.valueOf(value.asBoolean());
        }
        if (value.isNumber()) {
            return (JsonNode)new DecimalNode(new BigDecimal(value.asDouble()));
        }
        throw new ServerException("db.http.logic.UnsupportedJsonValue", new Object[] { value });
    }
    
    public JsonWrapper(final JsonWrapper parent) {
        this.parent = parent;
    }
    
    public void markAsChanged() {
        this.changed = true;
        if (this.parent != null && this.parent != this) {
            this.parent.markAsChanged();
        }
    }
    
    public boolean isChanged() {
        return this.changed;
    }
    
    public abstract String getJson();
    
    public abstract Object at(final String p0);
}
