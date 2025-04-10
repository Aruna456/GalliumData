// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java.json;

import java.util.Iterator;
import java.util.Map;
import java.util.List;
import com.galliumdata.server.ServerException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.function.Function;

import org.graalvm.polyglot.Value;
import com.jayway.jsonpath.Predicate;
import com.galliumdata.server.filters.http.JsonHolder;
import com.jayway.jsonpath.DocumentContext;
import org.graalvm.polyglot.proxy.ProxyObject;

public class JsonPathWrapper implements ProxyObject
{
    private final DocumentContext docCtxt;
    private final JsonHolder holder;
    private static final String TYPE_NAME_STRING = "string";
    private static final String TYPE_NAME_NUMBER = "number";
    private static final String TYPE_NAME_BOOLEAN = "boolean";
    private static final String TYPE_NAME_ANY = "any";
    
    public JsonPathWrapper(final DocumentContext docCtxt, final JsonHolder holder) {
        this.docCtxt = docCtxt;
        this.holder = holder;
    }
    
    public Object read(final String path) {
        return this.docCtxt.read(path, new Predicate[0]);
    }
    
    public JsonPathWrapper addToArray(final String path, final Value value) {
        this.checkCanModify();
        this.docCtxt.add(path, convertValueToJson(value), new Predicate[0]);
        this.notifyChange();
        return this;
    }
    
    public JsonPathWrapper put(final String path, final String key, final Value value) {
        this.checkCanModify();
        this.docCtxt.put(path, key, convertValueToJson(value), new Predicate[0]);
        this.notifyChange();
        return this;
    }
    
    public JsonPathWrapper set(final String path, final Value value) {
        this.checkCanModify();
        this.docCtxt.set(path, convertValueToJson(value), new Predicate[0]);
        this.notifyChange();
        return this;
    }
    
    public JsonPathWrapper delete(final String path) {
        this.checkCanModify();
        this.docCtxt.delete(path, new Predicate[0]);
        this.notifyChange();
        return this;
    }
    
    private static Object convertValueToJson(final Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (value.isString() || value.isDate()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.hasArrayElements()) {
            final List<Object> list = new ArrayList<Object>();
            for (int i = 0; i < value.getArraySize(); ++i) {
                list.add(convertValueToJson(value.getArrayElement((long)i)));
            }
            return list;
        }
        if (value.hasMembers()) {
            final Map<String, Object> map = new HashMap<String, Object>();
            for (final String name : value.getMemberKeys()) {
                map.put(name, convertValueToJson(value.getMember(name)));
            }
            return map;
        }
        throw new ServerException("db.http.logic.UnsupportedJsonValue", new Object[] { value });
    }
    
    private void checkCanModify() {
        if (this.holder == null) {
            throw new ServerException("db.http.logic.ObjectCannotBeChanged", new Object[0]);
        }
    }
    
    private void notifyChange() {
        if (this.holder == null) {
            return;
        }
        this.holder.setJson(this.docCtxt.jsonString());
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "read": {
                return (Function<Value[],Object>) arguments -> {
                    this.checkJSArguments("read", 1, arguments, "string");
                    return this.read(arguments[0].asString());
                };
            }
            case "addToArray": {
                return (Function<Value[],Object>) arguments -> {
                    this.checkJSArguments("addToArray", 2, arguments, "string", "any");
                    return this.addToArray(arguments[0].asString(), arguments[1]);
                };
            }
            case "put": {
                return (Function<Value[],Object>) arguments -> {
                    this.checkJSArguments("put", 3, arguments, "string", "string", "any");
                    return this.put(arguments[0].asString(), arguments[1].asString(), arguments[2]);
                };
            }
            case "set": {
                return (Function<Value[],Object>) arguments -> {
                    this.checkJSArguments("set", 2, arguments, "string", "any");
                    return this.set(arguments[0].asString(), arguments[1]);
                };
            }
            case "delete": {
                return (Function<Value[],Object>) arguments -> {
                    this.checkJSArguments("delete", 1, arguments, "string");
                    return this.delete(arguments[0].asString());
                };
            }
            case "toString": {
                this.checkJSArguments("toString", 0, null, new String[0]);
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "read", "addToArray", "put", "set", "delete", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "read":
            case "addToArray":
            case "put":
            case "set":
            case "delete":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value value) {
        throw new ServerException("db.http.logic.CannotPutMember", new Object[] { key, "JsonPathWrapper" });
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.http.logic.CannotRemoveMember", new Object[] { key, "JsonPathWrapper" });
    }
    
    private void checkJSArguments(final String methodName, final int numArgs, final Value[] args, final String... types) {
        if ((args == null && numArgs != 0) || (args != null && args.length != numArgs)) {
            throw new ServerException("db.http.logic.InvalidNumberOfArguments", new Object[] { methodName, numArgs });
        }
        for (int i = 0; i < numArgs; ++i) {
            final String type = types[i];
            final Value arg = args[i];
            final String s = type;
            switch (s) {
                case "string": {
                    if (!arg.isString()) {
                        throw new ServerException("db.http.logic.InvalidArgumentType", new Object[] { methodName, "string", i });
                    }
                    break;
                }
                case "number": {
                    if (!arg.isNumber()) {
                        throw new ServerException("db.http.logic.InvalidArgumentType", new Object[] { methodName, "number", i });
                    }
                    break;
                }
                case "boolean": {
                    if (!arg.isBoolean()) {
                        throw new ServerException("db.http.logic.InvalidArgumentType", new Object[] { methodName, "boolean", i });
                    }
                    break;
                }
            }
        }
    }
}
