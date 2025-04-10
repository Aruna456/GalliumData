// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.filters.http;

import org.graalvm.polyglot.Value;
import com.fasterxml.jackson.core.JsonPointer;
import java.nio.charset.StandardCharsets;
import com.jayway.jsonpath.JsonPath;
import com.galliumdata.server.handler.http.java.json.JsonWrapper;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import com.galliumdata.server.ServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import com.jayway.jsonpath.Option;
import java.util.Set;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.Configuration;
import com.galliumdata.server.handler.http.java.json.JsonPathWrapper;
import com.jayway.jsonpath.DocumentContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.util.function.Function;

import org.graalvm.polyglot.proxy.ProxyObject;

public class JsonHolder implements ProxyObject
{
    private final InputStream jsonStream;
    private byte[] jsonBytes;
    private Object wrapper;
    private JsonNode topNode;
    private DocumentContext docCtxt;
    private JsonPathWrapper jsonPathWrapper;
    private static boolean jsonParserInitialized;
    
    public static void init() {
        if (JsonHolder.jsonParserInitialized) {
            return;
        }
        Configuration.setDefaults((Configuration.Defaults)new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();
            
            public JsonProvider jsonProvider() {
                return this.jsonProvider;
            }
            
            public MappingProvider mappingProvider() {
                return this.mappingProvider;
            }
            
            public Set<Option> options() {
                final Set<Option> options = new HashSet<Option>();
                options.add(Option.SUPPRESS_EXCEPTIONS);
                return options;
            }
        });
        JsonHolder.jsonParserInitialized = true;
    }
    
    public JsonHolder(final InputStream jsonStream) {
        init();
        this.jsonStream = jsonStream;
    }
    
    public Object getTopNode() {
        if (this.wrapper == null) {
            final ObjectMapper mapper = new ObjectMapper();
            Label_0125: {
                if (this.jsonBytes != null) {
                    try {
                        this.topNode = mapper.readTree(this.jsonBytes);
                        break Label_0125;
                    }
                    catch (final Exception ex) {
                        throw new ServerException("db.http.logic.ErrorReadingJSON", new Object[] { ex });
                    }
                }
                try {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
                    this.jsonStream.transferTo(baos);
                    this.jsonStream.close();
                    this.jsonBytes = baos.toByteArray();
                    this.topNode = mapper.readTree(this.jsonBytes);
                }
                catch (final Exception ex) {
                    throw new ServerException("db.http.logic.ErrorReadingJSON", new Object[] { ex });
                }
            }
            this.wrapper = JsonWrapper.wrap(null, this.topNode);
        }
        return this.wrapper;
    }
    
    public JsonPathWrapper getJsonPath() {
        if (this.jsonPathWrapper != null) {
            return this.jsonPathWrapper;
        }
        this.getTopNode();
        this.docCtxt = JsonPath.parse(this.getJson());
        return new JsonPathWrapper(this.docCtxt, this);
    }
    
    public boolean jsonHasChanged() {
        return this.wrapper != null && this.wrapper instanceof JsonWrapper && ((JsonWrapper)this.wrapper).isChanged();
    }
    
    public boolean jsonHasBeenRead() {
        return this.jsonBytes != null;
    }
    
    public String getJson() {
        this.getTopNode();
        if (this.wrapper == null) {
            return null;
        }
        if (this.wrapper instanceof String) {
            return (String)this.wrapper;
        }
        if (this.wrapper instanceof Boolean) {
            return this.wrapper.toString();
        }
        if (this.wrapper instanceof Number) {
            return this.wrapper.toString();
        }
        final JsonWrapper jw = (JsonWrapper)this.wrapper;
        return jw.getJson();
    }
    
    public void setJson(final String newJson) {
        this.jsonBytes = newJson.getBytes(StandardCharsets.UTF_8);
        this.wrapper = null;
        this.docCtxt = null;
    }
    
    public Object getByJsonPointer(final String ptrStr) {
        if (this.topNode == null) {
            this.getTopNode();
        }
        if (this.topNode == null) {
            return null;
        }
        JsonPointer ptr;
        try {
            ptr = JsonPointer.valueOf(ptrStr);
        }
        catch (final Exception ex) {
            throw new ServerException("db.http.logic.BadJsonPointer", new Object[] { ptrStr, ex });
        }
        final JsonNode n = this.topNode.at(ptr);
        return JsonWrapper.wrap((JsonWrapper)this.wrapper, n);
    }
    
    @Override
    public String toString() {
        String j = this.getJson();
        if (j != null && j.length() > 60) {
            final int origLen = j.length();
            j = j.substring(0, 60) + " [+ " + (origLen - 60) + " chars]";
        }
        return "JsonHolder - " + j;
    }
    
    public Object getMember(final String key) {
        switch (key) {
            case "topNode": {
                return this.getTopNode();
            }
            case "json": {
                return this.getJson();
            }
            case "jsonHasChanged": {
                return this.jsonHasChanged();
            }
            case "jsonPath": {
                return this.getJsonPath();
            }
            case "getByJsonPointer": {
                return (Function<Value[],Object>) arguments -> this.getByJsonPointer(arguments[0].asString());
            }
            case "toString": {
                return (Function<Value[],Object>) arguments -> this.toString();
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key, "JsonHolder" });
            }
        }
    }
    
    public Object getMemberKeys() {
        return new String[] { "topNode", "json", "jsonHasChanged", "jsonPath", "getByJsonPointer", "toString" };
    }
    
    public boolean hasMember(final String key) {
        switch (key) {
            case "topNode":
            case "json":
            case "jsonHasChanged":
            case "jsonPath":
            case "getByJsonPointer":
            case "toString": {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public void putMember(final String key, final Value val) {
        switch (key) {
            case "topNode": {
                this.wrapper = JsonWrapper.convertJS(val);
                break;
            }
            case "json": {
                this.setJson(val.asString());
                break;
            }
            default: {
                throw new ServerException("db.http.logic.NoSuchMember", new Object[] { key, "JsonHolder" });
            }
        }
    }
    
    public boolean removeMember(final String key) {
        throw new ServerException("db.http.logic.CannotRemoveMember", new Object[] { key, "JsonHolder" });
    }
    
    static {
        JsonHolder.jsonParserInitialized = false;
    }
}
