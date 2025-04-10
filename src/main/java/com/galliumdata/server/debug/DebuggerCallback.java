// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.debug;

import org.apache.logging.log4j.LogManager;
import java.util.Set;
import java.util.Arrays;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import com.galliumdata.server.adapters.Variables;
import java.util.Map;
import java.util.Collection;
import java.lang.reflect.Array;
import org.graalvm.polyglot.Value;
import com.oracle.truffle.api.debug.DebugStackFrame;
import java.util.Iterator;
import com.oracle.truffle.api.debug.DebugScope;
import java.nio.file.Path;
import java.net.URI;
import com.galliumdata.server.rest.ServerSentEventHandler;
import com.galliumdata.server.rest.ServerEvent;
import com.oracle.truffle.api.debug.DebugValue;
import com.galliumdata.server.handler.ProtocolData;
import com.galliumdata.server.ServerException;
import com.galliumdata.server.repository.RepositoryManager;
import com.galliumdata.server.repository.Project;
import java.nio.file.Paths;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.Logger;
import com.galliumdata.server.handler.ProtocolDataObject;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.debug.SuspendedCallback;

public class DebuggerCallback implements SuspendedCallback
{
    private static final Object inBreakpoint;
    private static boolean breakpointLock;
    private String actionOnResume;
    private static SuspendedEvent currentEvent;
    private static final Object getVariablesLock;
    private static String[] getVariableName;
    private static ProtocolDataObject getVariableResult;
    private static final Object getVariablesResultLock;
    private static String evalExpression;
    private static final Logger log;
    
    public DebuggerCallback() {
        this.actionOnResume = "go";
        DebuggerCallback.log.trace(Markers.SYSTEM, "New DebuggerCallback created");
    }
    
    public static boolean isSuspended() {
        return DebuggerCallback.currentEvent != null;
    }
    
    public void onSuspend(final SuspendedEvent event) {
        if (DebuggerCallback.breakpointLock) {
            DebuggerCallback.log.trace(Markers.SYSTEM, "Already at breakpoint, returning");
            return;
        }
        DebuggerCallback.breakpointLock = true;
        try {
            this.doOnSuspend(event);
        }
        catch (final Throwable t) {
            t.printStackTrace();
        }
        DebuggerCallback.breakpointLock = false;
        DebuggerCallback.currentEvent = null;
        DebuggerCallback.getVariableResult = null;
    }
    
    private void doOnSuspend(final SuspendedEvent event) {
        DebuggerCallback.currentEvent = event;
        final URI codeUri = event.getSourceSection().getSource().getURI();
        final int lineNum = event.getSourceSection().getStartLine();
        if (lineNum == 1 && event.getSourceSection().getEndColumn() < 100) {
            return;
        }
        final String codePath = URLDecoder.decode(codeUri.toString(), StandardCharsets.UTF_8);
        Path path = Paths.get(codePath, new String[0]);
        final int numSegments = path.getNameCount();
        if (numSegments >= 6) {
            final String meta = path.subpath(numSegments - 6, numSegments - 5).toString();
            if ("metarepo".equals(meta)) {
                return;
            }
        }
        final String projectName = path.subpath(numSegments - 4, numSegments - 3).toString();
        final Project project = RepositoryManager.getMainRepository().getProjects().get(projectName);
        if (project == null) {
            event.getSession().close();
            throw new ServerException("debug.NoSuchProject", new Object[] { path.toString() });
        }
        final String[] pathParts = codePath.split("/");
        if (pathParts.length != 4) {
            throw new ServerException("debug.GeneralError", new Object[] { "invalid breakpoint path: " + String.valueOf(path) });
        }
        path = path.subpath(numSegments - 3, numSegments);
        final ProtocolData topNode = new ProtocolDataObject();
        topNode.putString("project", projectName);
        topNode.putString("filename", path.toString());
        topNode.putNumber("linenum", lineNum - 1);
        topNode.putString("filter_type", pathParts[1]);
        topNode.putString("filter_name", pathParts[2]);
        topNode.putString("code_name", pathParts[3]);
        final ProtocolDataObject globalsNode = new ProtocolDataObject();
        topNode.put("global variables", globalsNode);
        final DebugScope topScope = event.getSession().getTopScope("js");
        for (final DebugValue globVar : topScope.getDeclaredValues()) {
            globalsNode.put(globVar.getName(), getOneDebugValueAsJSON(globVar, new String[] { globVar.getName() }, false));
        }
        if (topScope.getParent() != null) {
            for (final DebugValue globVar2 : topScope.getParent().getDeclaredValues()) {
                globalsNode.put(globVar2.getName(), getOneDebugValueAsJSON(globVar2, new String[] { globVar2.getName() }, false));
            }
        }
        globalsNode.sort();
        final ProtocolDataObject localsNode = new ProtocolDataObject();
        topNode.put("local variables", localsNode);
        final DebugStackFrame frame = event.getTopStackFrame();
        final DebugScope frameScope = frame.getScope();
        for (final DebugValue local : event.getTopStackFrame().getScope().getDeclaredValues()) {
            localsNode.put(local.getName(), getOneDebugValueAsJSON(local, new String[] { local.getName() }, false));
        }
        for (DebugScope parentScope = frameScope.getParent(); parentScope != null; parentScope = parentScope.getParent()) {
            for (final DebugValue local2 : parentScope.getDeclaredValues()) {
                localsNode.put(local2.getName(), getOneDebugValueAsJSON(local2, new String[] { local2.getName() }, false));
            }
        }
        localsNode.sort();
        final ServerEvent serverEvent = new ServerEvent();
        serverEvent.eventType = "breakpoint";
        serverEvent.message = topNode.toJSONDebug(1000, 50);
        ServerSentEventHandler.queueServerEvent(serverEvent);
        synchronized (DebuggerCallback.inBreakpoint) {
            DebuggerCallback.log.trace(Markers.SYSTEM, "ScriptExecutor is now waiting to resume");
            while (true) {
                try {
                    DebuggerCallback.inBreakpoint.wait();
                }
                catch (final InterruptedException ie) {
                    DebuggerCallback.log.trace(Markers.SYSTEM, "doOnSuspend: synchronized(inBreakpoint) was interrupted, returning");
                    return;
                }
                if (DebuggerCallback.getVariableName != null) {
                    final Object val = getVariableByName(DebuggerCallback.getVariableName);
                    DebuggerCallback.getVariableResult = getOneObjectAsJSON(val, DebuggerCallback.getVariableName, true);
                    synchronized (DebuggerCallback.getVariablesResultLock) {
                        DebuggerCallback.getVariablesResultLock.notify();
                    }
                }
                else {
                    if (DebuggerCallback.evalExpression == null) {
                        break;
                    }
                    Object exprObj = null;
                    try {
                        exprObj = event.getTopStackFrame().eval(DebuggerCallback.evalExpression);
                    }
                    catch (Exception ex) {
                        if (ex.getCause() != null && ex.getCause() != ex) {
                            ex = (Exception)ex.getCause();
                        }
                        exprObj = "Unable to evaluate: " + ex.getMessage();
                    }
                    DebuggerCallback.getVariableResult = getOneObjectAsJSON(exprObj, new String[] { "<eval>" }, false);
                    synchronized (DebuggerCallback.getVariablesResultLock) {
                        DebuggerCallback.getVariablesResultLock.notify();
                    }
                }
            }
            DebuggerCallback.log.trace(Markers.SYSTEM, "ScriptExecutor is now resuming");
        }
        ServerSentEventHandler.deleteCurrentEvent();
        final String actionOnResume = this.actionOnResume;
        switch (actionOnResume) {
            case "go": {
                event.prepareContinue();
                break;
            }
            case "step": {
                event.prepareStepInto(1);
                break;
            }
            case "stepOut": {
                event.prepareStepOut(1);
                break;
            }
            case "stepIn": {
                event.prepareStepInto(1);
                break;
            }
            default: {
                throw new ServerException("debug.GeneralError", new Object[] { "Unknown actionOnResume: " + this.actionOnResume });
            }
        }
    }
    
    public void setActionOnResume(final String s) {
        this.actionOnResume = s;
    }
    
    public void resumeExecution() {
        DebuggerCallback.log.trace(Markers.SYSTEM, "Resuming execution");
        synchronized (DebuggerCallback.inBreakpoint) {
            DebuggerCallback.inBreakpoint.notifyAll();
        }
    }
    
    public static void unblock() {
        synchronized (DebuggerCallback.inBreakpoint) {
            DebuggerCallback.inBreakpoint.notifyAll();
        }
    }
    
    private static ProtocolDataObject getOneObjectAsJSON(Object obj, final String[] path, final boolean withChildren) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof DebugValue) {
            return getOneDebugValueAsJSON((DebugValue)obj, path, true);
        }
        if (obj instanceof Value) {
            final Value value = (Value)obj;
            obj = value.asHostObject();
        }
        final ProtocolDataObject topNode = new ProtocolDataObject();
        topNode.putString("path", String.join(".", (CharSequence[])path));
        final Class<?> cls = obj.getClass();
        if (cls.isArray()) {
            topNode.putString("type", "array");
            topNode.putString("asString", toShortString(obj));
            final int numMembers = Array.getLength(obj);
            topNode.putNumber("numChildren", numMembers);
            if (withChildren) {
                topNode.put("children", getChildrenNodes(obj, path));
            }
            return topNode;
        }
        if (obj instanceof Collection) {
            final Collection coll = (Collection)obj;
            topNode.putString("type", "array");
            topNode.putString("asString", toShortString(obj));
            topNode.putNumber("numChildren", coll.size());
            if (withChildren) {
                topNode.put("children", getChildrenNodes(coll, path));
            }
            return topNode;
        }
        if (obj instanceof Map) {
            topNode.putString("type", "object");
            topNode.putString("asString", toShortString(obj));
            final Map<?, ?> map = (Map<?, ?>)obj;
            topNode.putNumber("numChildren", map.size());
            if (withChildren) {
                topNode.put("children", getChildrenNodes(obj, path));
            }
            return topNode;
        }
        if (obj instanceof Variables) {
            final Variables vars = (Variables)obj;
            topNode.putString("type", "object");
            topNode.putString("asString", toShortString(obj));
            topNode.putNumber("numChildren", vars.size());
            if (withChildren) {
                topNode.put("children", getChildrenNodes(obj, path));
            }
        }
        if (obj instanceof String) {
            topNode.putString("type", "value");
            topNode.putString("asString", toShortString(obj));
            return topNode;
        }
        if (obj instanceof Boolean) {
            topNode.putString("type", "value");
            topNode.putString("asString", toShortString(obj));
            return topNode;
        }
        if (obj instanceof Number) {
            topNode.putString("type", "value");
            topNode.putString("asString", toShortString(obj));
            return topNode;
        }
        if (cls.isEnum()) {
            topNode.putString("type", "value");
            topNode.putString("asString", toShortString(obj));
            return topNode;
        }
        topNode.putString("type", getJavaObjectType(obj));
        topNode.putString("asString", toShortString(obj));
        final int numChildren = getNumberOfChildrenJava(obj);
        if (numChildren >= 0) {
            topNode.putNumber("numChildren", numChildren);
            if (withChildren) {
                topNode.put("children", getChildrenNodes(obj, path));
            }
        }
        return topNode;
    }
    
    private static String getJavaObjectType(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof DebugValue) {
            return getDebugValueType((DebugValue)obj);
        }
        if (obj instanceof ProxyExecutable) {
            return "function";
        }
        if (obj instanceof ProxyArray) {
            return "array";
        }
        if (obj instanceof ProxyObject) {
            return "object";
        }
        if (obj instanceof Value) {
            final Value valueObj = (Value)obj;
            if (valueObj.isHostObject()) {
                obj = ((Value)obj).asHostObject();
            }
            else {
                if (valueObj.isBoolean()) {
                    return "boolean";
                }
                if (valueObj.isDate()) {
                    return "date";
                }
                if (valueObj.isMetaObject()) {
                    return "meta";
                }
                if (valueObj.isDuration()) {
                    return "duration";
                }
                if (valueObj.isException()) {
                    return "exception";
                }
                if (valueObj.isInstant()) {
                    return "instant";
                }
                if (valueObj.isNativePointer()) {
                    return "native pointer";
                }
                if (valueObj.isNull()) {
                    return "null";
                }
                if (valueObj.isNumber()) {
                    return "number";
                }
                if (valueObj.isProxyObject()) {
                    return "proxy";
                }
                if (valueObj.isString()) {
                    return "string";
                }
                if (valueObj.isTime()) {
                    return "time";
                }
                if (valueObj.isTimeZone()) {
                    return "timezone";
                }
                return "unknown";
            }
        }
        final Class<?> cls = obj.getClass();
        if (cls.isArray()) {
            return "array";
        }
        final String clsName = cls.getName();
        if (clsName.startsWith("com.oracle.truffle.object")) {
            return "object";
        }
        if (obj instanceof Collection) {
            return "array";
        }
        if (obj instanceof Map) {
            return "object";
        }
        if (obj instanceof Variables) {
            return "object";
        }
        if (obj instanceof String) {
            return "string";
        }
        if (obj instanceof Boolean) {
            return "boolean";
        }
        if (obj instanceof Number) {
            return "number";
        }
        return "java:" + clsName;
    }
    
    private static String getJavaTypeFromClass(final Class<?> cls) {
        if (String.class.isAssignableFrom(cls)) {
            return "string";
        }
        if (Boolean.class.isAssignableFrom(cls)) {
            return "boolean";
        }
        if (Number.class.isAssignableFrom(cls)) {
            return "number";
        }
        if (Variables.class.isAssignableFrom(cls)) {
            return "object";
        }
        if (Map.class.isAssignableFrom(cls)) {
            return "object";
        }
        if (Collection.class.isAssignableFrom(cls)) {
            return "array";
        }
        if (cls.isArray()) {
            return "array";
        }
        if (cls.equals(Variables.class)) {
            return "object";
        }
        return "java:" + cls.getName();
    }
    
    private static Object getDebugValueProxy(final DebugValue val) {
        try {
            final Method readValue = val.getClass().getDeclaredMethod("readValue", (Class<?>[])new Class[0]);
            readValue.setAccessible(true);
            final Object value = readValue.invoke(val, new Object[0]);
            if (value == null) {
                return null;
            }
            final String valueClassName = value.getClass().getName();
            if (valueClassName.equals("com.oracle.truffle.polyglot.HostObject")) {
                final Field objField = value.getClass().getDeclaredField("obj");
                objField.setAccessible(true);
                return objField.get(value);
            }
            if (valueClassName.startsWith("com.oracle.truffle.object")) {
                return null;
            }
            final Field proxyField = value.getClass().getDeclaredField("proxy");
            proxyField.setAccessible(true);
            final Object proxyValue = proxyField.get(value);
            if (proxyValue != null && proxyValue instanceof Value) {
                return ((Value)proxyValue).asProxyObject();
            }
            return proxyValue;
        }
        catch (final Exception ex) {
            return null;
        }
    }
    
    private static boolean debugValueIsJavaObject(final DebugValue val) {
        return val != null && !val.isArray() && !val.isString() && !val.isBoolean() && !val.isDate() && !val.isDuration() && !val.isInstant() && !val.isInternal() && !val.isMetaObject() && !val.isNumber() && !val.isTime() && !val.isTimeZone() && !val.isNull() && getDebugValueProxy(val) != null;
    }
    
    private static String getDebugValueType(final DebugValue val) {
        if (val == null) {
            return "null";
        }
        if (val.isArray()) {
            return "array";
        }
        if (val.isString()) {
            return "string";
        }
        if (val.isBoolean()) {
            return "boolean";
        }
        if (val.isDate()) {
            return "date";
        }
        if (val.isDuration()) {
            return "duration";
        }
        if (val.isInstant()) {
            return "instant";
        }
        if (val.isInternal()) {
            return "internal";
        }
        if (val.isMetaObject()) {
            if (val.canExecute()) {
                return "function";
            }
            return "class";
        }
        else {
            if (val.isNumber()) {
                return "number";
            }
            if (val.isTime()) {
                return "time";
            }
            if (val.isTimeZone()) {
                return "timezone";
            }
            if (val.isNull()) {
                return "null";
            }
            final Object proxy = getDebugValueProxy(val);
            if (proxy == null) {
                try {
                    final Method readValue = val.getClass().getDeclaredMethod("readValue", (Class<?>[])new Class[0]);
                    readValue.setAccessible(true);
                    final Object value = readValue.invoke(val, new Object[0]);
                    if (value == null) {
                        return null;
                    }
                    if (value instanceof ProxyExecutable) {
                        return "function";
                    }
                    final String valueClassName = value.getClass().getName();
                    if (valueClassName.startsWith("com.oracle.truffle.object")) {
                        return "object";
                    }
                }
                catch (final Exception ex) {
                    try {
                        final Field valueField = val.getClass().getDeclaredField("value");
                        valueField.setAccessible(true);
                        final Object value2 = valueField.get(val);
                        if (value2 == null) {
                            return null;
                        }
                        final Field proxyField = value2.getClass().getDeclaredField("proxy");
                        proxyField.setAccessible(true);
                        final Object proxyObj = proxyField.get(value2);
                        return getJavaObjectType(proxyObj);
                    }
                    catch (final Exception ex2) {
                        return "unknown";
                    }
                }
                return "unknown";
            }
            return getJavaObjectType(proxy);
        }
    }
    
    private static int getNumberOfChildrenJava(Object obj) {
        if (obj == null) {
            return -1;
        }
        if (obj != null && obj instanceof Value) {
            final Value valueObj = (Value)obj;
            if (valueObj.isHostObject()) {
                obj = ((Value)obj).asHostObject();
            }
        }
        if (obj instanceof ProxyObject) {
            final ProxyObject proxy = (ProxyObject)obj;
            final Object[] keys = (Object[])proxy.getMemberKeys();
            if (keys == null) {
                return 0;
            }
            return keys.length;
        }
        else {
            if (obj instanceof ProxyArray) {
                final ProxyArray array = (ProxyArray)obj;
                return (int)array.getSize();
            }
            if (obj instanceof ProxyExecutable) {
                return 0;
            }
            final Class<?> cls = obj.getClass();
            if (cls.equals(String.class)) {
                return -1;
            }
            if (cls.equals(Character.class)) {
                return -1;
            }
            if (cls.equals(Boolean.class)) {
                return -1;
            }
            if (obj instanceof Number) {
                return -1;
            }
            if (obj instanceof Collection) {
                final Collection coll = (Collection)obj;
                return coll.size();
            }
            if (obj instanceof Map) {
                final Map map = (Map)obj;
                return map.size();
            }
            if (obj instanceof Variables) {
                final Variables vars = (Variables)obj;
                return vars.size();
            }
            if (cls.isArray()) {
                return Array.getLength(obj);
            }
            int numProps = cls.getFields().length;
            for (final Method method : cls.getMethods()) {
                final String methodName = method.getName();
                if (methodName.startsWith("get")) {
                    if (!methodName.equals("get")) {
                        if (Character.isUpperCase(methodName.charAt(3))) {
                            if (method.getParameterCount() <= 0) {
                                ++numProps;
                            }
                        }
                    }
                }
            }
            return numProps;
        }
    }
    
    private static int getNumberOfChildren(final DebugValue val) {
        if (val == null || val.isNull()) {
            return -1;
        }
        if (val.isArray()) {
            return val.getArray().size();
        }
        if (debugValueIsJavaObject(val)) {
            final Object javaVal = getDebugValueProxy(val);
            return getNumberOfChildrenJava(javaVal);
        }
        final Collection<DebugValue> props = val.getProperties();
        int numProps = 0;
        if (props != null && props.size() > 0) {
            for (final DebugValue prop : props) {
                if (!prop.canExecute()) {
                    ++numProps;
                }
            }
            return numProps;
        }
        return -1;
    }
    
    private static ProtocolDataObject getChildrenNodes(final Object obj, final String[] path) {
        final ProtocolDataObject children = new ProtocolDataObject();
        if (obj == null) {
            return children;
        }
        final int numChildren = getNumberOfChildrenJava(obj);
        if (numChildren == 0) {
            return children;
        }
        if (obj instanceof Variables) {
            final Variables variables = (Variables)obj;
            for (final String key : variables.keySet()) {
                final ProtocolDataObject child = new ProtocolDataObject();
                children.put(key, child);
                final String[] fullPath = Arrays.copyOf(path, path.length + 1);
                fullPath[path.length] = key;
                child.putString("path", String.join(".", (CharSequence[])fullPath));
                final Object childObj = variables.get(key);
                child.putString("type", getJavaObjectType(childObj));
                child.putString("asString", toShortString(childObj));
                final int numKids = getNumberOfChildrenJava(childObj);
                if (numKids >= 0) {
                    child.putNumber("numChildren", numKids);
                }
            }
            return children;
        }
        if (obj instanceof ProxyObject) {
            final ProxyObject proxy = (ProxyObject)obj;
            final Object[] array2;
            final Object[] keys = array2 = (Object[])proxy.getMemberKeys();
            for (final Object key2 : array2) {
                final ProtocolDataObject child2 = new ProtocolDataObject();
                final String keyStr = key2.toString();
                children.put(keyStr, child2);
                final String[] fullPath2 = Arrays.copyOf(path, path.length + 1);
                fullPath2[path.length] = keyStr;
                child2.putString("path", String.join(".", (CharSequence[])fullPath2));
                final Object childObj2 = proxy.getMember(keyStr);
                child2.putString("type", getJavaObjectType(childObj2));
                child2.putString("asString", toShortString(childObj2));
                final int numKids2 = getNumberOfChildrenJava(childObj2);
                if (numKids2 >= 0) {
                    child2.putNumber("numChildren", numKids2);
                }
            }
            return children;
        }
        if (obj instanceof ProxyArray) {
            final ProxyArray array = (ProxyArray)obj;
            for (int idx = 0; idx < array.getSize(); ++idx) {
                final ProtocolDataObject child3 = new ProtocolDataObject();
                children.put("" + idx, (ProtocolData)child3);
                final String[] fullPath3 = Arrays.copyOf(path, path.length + 1);
                fullPath3[path.length] = "" + idx;
                child3.putString("path", String.join(".", (CharSequence[])fullPath3));
                final Object childObj3 = array.get((long)idx);
                child3.putString("type", getJavaObjectType(childObj3));
                child3.putString("asString", toShortString(childObj3));
                final int numKids3 = getNumberOfChildrenJava(childObj3);
                if (numKids3 >= 0) {
                    child3.putNumber("numChildren", numKids3);
                }
            }
            return children;
        }
        if (obj instanceof Map) {
            final Map map = (Map)obj;
            final Set<Map.Entry> entries = map.entrySet();
            for (final Map.Entry entry : entries) {
                final ProtocolDataObject child4 = new ProtocolDataObject();
                final String keyStr2 = entry.getKey().toString();
                children.put(keyStr2, child4);
                final String[] fullPath4 = Arrays.copyOf(path, path.length + 1);
                fullPath4[path.length] = keyStr2;
                child4.putString("path", String.join(".", (CharSequence[])fullPath4));
                final Object childObj4 = entry.getValue();
                child4.putString("type", getJavaObjectType(childObj4));
                child4.putString("asString", toShortString(childObj4));
                final int numKids4 = getNumberOfChildrenJava(childObj4);
                if (numKids4 >= 0) {
                    child4.putNumber("numChildren", numKids4);
                }
            }
            return children;
        }
        if (obj instanceof Collection) {
            final Collection coll = (Collection)obj;
            int idx = 0;
            for (Object childObj5 : coll) {
                final ProtocolDataObject child4 = new ProtocolDataObject();
                final String keyStr2 = "" + idx;
                children.put(keyStr2, child4);
                final String[] fullPath4 = Arrays.copyOf(path, path.length + 1);
                fullPath4[path.length] = keyStr2;
                child4.putString("path", String.join(".", (CharSequence[])fullPath4));
                child4.putString("type", getJavaObjectType(childObj5));
                child4.putString("asString", toShortString(childObj5));
                final int numKids5 = getNumberOfChildrenJava(childObj5);
                if (numKids5 >= 0) {
                    child4.putNumber("numChildren", numKids5);
                }
                ++idx;
            }
            return children;
        }
        if (obj.getClass().isArray()) {
            for (int size = Array.getLength(obj), idx = 0; idx < size; ++idx) {
                final ProtocolDataObject child3 = new ProtocolDataObject();
                final String keyStr3 = "" + idx;
                children.put(keyStr3, child3);
                final String[] fullPath = Arrays.copyOf(path, path.length + 1);
                fullPath[path.length] = keyStr3;
                child3.putString("path", String.join(".", (CharSequence[])fullPath));
                final Object member = Array.get(obj, idx);
                child3.putString("type", getJavaObjectType(member));
                child3.putString("asString", toShortString(member));
                final int numKids = getNumberOfChildrenJava(member);
                if (numKids >= 0) {
                    child3.putNumber("numChildren", numKids);
                }
                if (idx >= 100) {
                    break;
                }
            }
            return children;
        }
        final Class<?> cls = obj.getClass();
        for (final Field field : cls.getFields()) {
            field.setAccessible(true);
            final ProtocolDataObject entryObj = new ProtocolDataObject();
            try {
                final String[] fullPath4 = Arrays.copyOf(path, path.length + 1);
                fullPath4[path.length] = field.getName();
                entryObj.putString("path", String.join(".", (CharSequence[])fullPath4));
                entryObj.putString("type", getJavaTypeFromClass(field.getType()));
                final Object fieldValue = field.get(obj);
                entryObj.putString("asString", toShortString(fieldValue));
                final int numChild = getNumberOfChildrenJava(fieldValue);
                if (numChild >= 0) {
                    entryObj.putNumber("numChildren", numChild);
                }
            }
            catch (final Exception ex) {
                entryObj.putString("asString", "<error getting value>");
            }
            children.put(field.getName(), entryObj);
        }
        for (final Method method : cls.getMethods()) {
            final String methodName = method.getName();
            if (methodName.startsWith("get")) {
                if (!methodName.equals("get")) {
                    if (Character.isUpperCase(methodName.charAt(3))) {
                        if (method.getParameterCount() <= 0) {
                            final ProtocolDataObject entryObj2 = new ProtocolDataObject();
                            try {
                                final String[] fullPath5 = Arrays.copyOf(path, path.length + 1);
                                fullPath5[path.length] = methodName;
                                entryObj2.putString("path", String.join(".", (CharSequence[])fullPath5));
                                entryObj2.putString("type", getJavaTypeFromClass(method.getReturnType()));
                                method.setAccessible(true);
                                final Object methodVal = method.invoke(obj, new Object[0]);
                                entryObj2.putString("asString", toShortString(methodVal));
                                final int numChild2 = getNumberOfChildrenJava(methodVal);
                                if (numChild2 >= 0) {
                                    entryObj2.putNumber("numChildren", numChild2);
                                }
                            }
                            catch (final Exception ex2) {
                                entryObj2.putString("asString", "<error getting value>");
                            }
                            children.put(methodName, entryObj2);
                        }
                    }
                }
            }
        }
        return children;
    }
    
    public static ProtocolDataObject getOneDebugValueAsJSON(final DebugValue val, final String[] path, final boolean withChildren) {
        final ProtocolDataObject topNode = new ProtocolDataObject();
        topNode.putString("asString", toShortString(val, path[0]));
        topNode.putString("path", String.join(".", (CharSequence[])path));
        topNode.putString("type", getDebugValueType(val));
        if (val == null || val.isNull()) {
            topNode.putString("type", "null");
            return topNode;
        }
        if (debugValueIsJavaObject(val)) {
            final int numChildren = getNumberOfChildren(val);
            if (numChildren >= 0) {
                topNode.putNumber("numChildren", numChildren);
            }
            if (withChildren) {
                final Object javaVal = getDebugValueProxy(val);
                topNode.put("children", getChildrenNodes(javaVal, path));
            }
            return topNode;
        }
        if (val.isArray()) {
            topNode.putString("type", "array");
            topNode.putNumber("numChildren", val.getArray().size());
            if (withChildren) {
                final ProtocolDataObject arrayValue = new ProtocolDataObject();
                topNode.put("children", arrayValue);
                for (int len = val.getArray().size(), i = 0; i < len; ++i) {
                    final ProtocolDataObject entryObj = new ProtocolDataObject();
                    final String[] fullPath = Arrays.copyOf(path, path.length + 1);
                    fullPath[path.length] = "" + i;
                    entryObj.putString("path", String.join(".", (CharSequence[])fullPath));
                    final DebugValue prop = val.getArray().get(i);
                    entryObj.putString("asString", toShortString(prop));
                    entryObj.putString("type", getDebugValueType(prop));
                    final int numChildren2 = getNumberOfChildren(prop);
                    if (numChildren2 >= 0) {
                        entryObj.putNumber("numChildren", numChildren2);
                    }
                    arrayValue.put("" + i, (ProtocolData)entryObj);
                    if (i > 100) {
                        break;
                    }
                }
            }
        }
        final Collection<DebugValue> props = val.getProperties();
        if (props != null && props.size() > 0) {
            topNode.putString("type", "object");
            if (withChildren) {
                final ProtocolData propsNode = new ProtocolDataObject();
                topNode.put("children", propsNode);
                int propCnt = 0;
                for (final DebugValue prop2 : props) {
                    if (prop2.canExecute()) {
                        continue;
                    }
                    final String propName = prop2.getName();
                    final ProtocolDataObject entryObj2 = new ProtocolDataObject();
                    final String[] fullPath2 = Arrays.copyOf(path, path.length + 1);
                    fullPath2[path.length] = propName;
                    entryObj2.putString("path", String.join(".", (CharSequence[])fullPath2));
                    entryObj2.putString("asString", toShortString(prop2));
                    entryObj2.putString("type", getDebugValueType(prop2));
                    final int numChildren3 = getNumberOfChildren(prop2);
                    if (numChildren3 >= 0) {
                        entryObj2.putNumber("numChildren", numChildren3);
                    }
                    propsNode.put(propName, entryObj2);
                    if (++propCnt >= 100) {
                        break;
                    }
                }
                topNode.putNumber("numChildren", propCnt);
            }
            else {
                int propCnt2 = 0;
                for (final DebugValue prop3 : props) {
                    if (prop3.canExecute()) {
                        continue;
                    }
                    ++propCnt2;
                }
                topNode.putNumber("numChildren", propCnt2);
            }
            return topNode;
        }
        topNode.putString("type", getDebugValueType(val));
        topNode.putString("asString", toShortString(val));
        return topNode;
    }
    
    private static Object getVariableByName(final String[] varNames) {
        if (varNames == null || varNames.length == 0) {
            throw new RuntimeException("Cannot have empty variable names");
        }
        Object obj = null;
        for (final String varName : varNames) {
            obj = getVariableByNameFromBase(obj, varName);
            if (obj != null && obj instanceof Value) {
                obj = ((Value)obj).asHostObject();
            }
            if (obj == null) {
                return null;
            }
        }
        return obj;
    }
    
    private static Object getVariableByNameFromBase(Object base, final String name) {
        if (base == null) {
            DebugValue val = null;
            try {
                val = DebuggerCallback.currentEvent.getTopStackFrame().getScope().getDeclaredValue(name);
            }
            catch (final Exception ex) {
                return "Cannot read: " + ex.getMessage();
            }
            if (val != null) {
                return val;
            }
            for (DebugScope parentScope = DebuggerCallback.currentEvent.getTopStackFrame().getScope().getParent(); parentScope != null; parentScope = parentScope.getParent()) {
                val = parentScope.getDeclaredValue(name);
                if (val != null) {
                    return val;
                }
            }
            final DebugScope topScope = DebuggerCallback.currentEvent.getSession().getTopScope("js");
            val = topScope.getDeclaredValue(name);
            if (val != null) {
                return val;
            }
            if (topScope.getParent() != null) {
                val = topScope.getParent().getDeclaredValue(name);
                if (val != null) {
                    return val;
                }
            }
            return null;
        }
        else {
            if (base instanceof DebugValue) {
                final DebugValue debugVal = (DebugValue)base;
                if (!debugValueIsJavaObject(debugVal)) {
                    return debugVal.getProperty(name);
                }
                base = getDebugValueProxy(debugVal);
            }
            if (base instanceof Variables) {
                final Variables variables = (Variables)base;
                return variables.get(name);
            }
            if (base instanceof Value) {
                base = ((Value)base).asHostObject();
            }
            if (base instanceof ProxyObject) {
                final ProxyObject proxy = (ProxyObject)base;
                return proxy.getMember(name);
            }
            if (base instanceof ProxyArray) {
                final ProxyArray array = (ProxyArray)base;
                final int idx = Integer.parseInt(name);
                return array.get((long)idx);
            }
            final Class<?> cls = base.getClass();
            if (cls.isArray()) {
                final int idx = Integer.parseInt(name);
                return Array.get(base, idx);
            }
            if (base instanceof Collection) {
                final Collection coll = (Collection)base;
                final int idx2 = Integer.parseInt(name);
                final Iterator it = coll.iterator();
                int i = 0;
                Object theObject = null;
                while (i <= idx2) {
                    theObject = it.next();
                    ++i;
                }
                return theObject;
            }
            if (base instanceof Map) {
                final Map<?, ?> map = (Map<?, ?>)base;
                return map.get(name);
            }
            try {
                final Field fld = cls.getField(name);
                fld.setAccessible(true);
                return fld.get(base);
            }
            catch (final Exception ex2) {
                if (!name.startsWith("get")) {
                    throw new RuntimeException("No such member: " + name);
                }
                try {
                    final Method method = cls.getMethod(name, (Class<?>[])new Class[0]);
                    return method.invoke(base, new Object[0]);
                }
                catch (final Exception ex) {
                    return null;
                }
            }
        }
    }
    
    public static ProtocolDataObject getDebugVariable(final String[] name) {
        try {
            synchronized (DebuggerCallback.getVariablesLock) {
                return getDebugVariableImpl(name);
            }
        }
        catch (final Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }
    
    private static ProtocolDataObject getDebugVariableImpl(final String[] name) {
        DebuggerCallback.getVariableName = name;
        synchronized (DebuggerCallback.inBreakpoint) {
            DebuggerCallback.inBreakpoint.notify();
        }
        synchronized (DebuggerCallback.getVariablesResultLock) {
            try {
                DebuggerCallback.getVariablesResultLock.wait();
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                DebuggerCallback.getVariableName = null;
            }
        }
        return DebuggerCallback.getVariableResult;
    }
    
    private static String toShortString(final Object obj) {
        return toShortString(obj, null);
    }
    
    private static String toShortString(final Object obj, String name) {
        if (obj == null) {
            return "(null)";
        }
        if (obj instanceof ProxyExecutable) {
            return "(function)";
        }
        if (obj instanceof ProxyArray) {
            final ProxyArray array = (ProxyArray)obj;
            return "array[" + array.getSize();
        }
        if (name == null) {
            name = "<unnamed>";
        }
        if (obj instanceof ProxyObject) {
            final ProxyObject proxy = (ProxyObject)obj;
            final Object[] members = (Object[])proxy.getMemberKeys();
            String res = "{";
            if (members == null) {
                res += "<null>";
            }
            else {
                Arrays.sort(members);
                final Object[] array2 = members;
                for (int length = array2.length, i = 0; i < length; ++i) {
                    final Object member = array2[i];
                    if (res.length() > 1) {
                        res += ", ";
                    }
                    res += member.toString();
                    res = res;
                    Object val = null;
                    try {
                        val = proxy.getMember(member.toString());
                    }
                    catch (final Exception ex) {
                        val = "Exception reading " + member.toString() + ": " + ex.getMessage();
                    }
                    if (val == null) {
                        res += "(null)";
                    }
                    else {
                        res += toShortString(val);
                    }
                    if (res.length() > 500) {
                        res = res.substring(0, 500) + "...";
                        break;
                    }
                }
            }
            return res;
        }
        String s;
        try {
            s = obj.toString();
        }
        catch (final Exception ex2) {
            s = "Unable to get value of " + name + ": " + ex2.getMessage();
        }
        if (s.startsWith("DebugValue(name=")) {
            final String valueStr = "value = ";
            final int idx = s.indexOf(valueStr);
            s = s.substring(idx + valueStr.length());
            if (s.endsWith(")")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        if (s.length() > 500) {
            s = s.substring(0, 500) + "...";
        }
        return s;
    }
    
    public static ProtocolDataObject evalExpression(final String expr) {
        try {
            synchronized (DebuggerCallback.getVariablesLock) {
                return evalExpressionImpl(expr);
            }
        }
        catch (final Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }
    
    private static ProtocolDataObject evalExpressionImpl(final String expr) {
        DebuggerCallback.evalExpression = expr;
        synchronized (DebuggerCallback.inBreakpoint) {
            DebuggerCallback.inBreakpoint.notify();
        }
        synchronized (DebuggerCallback.getVariablesResultLock) {
            try {
                DebuggerCallback.getVariablesResultLock.wait();
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                DebuggerCallback.evalExpression = null;
            }
        }
        return DebuggerCallback.getVariableResult;
    }
    
    static {
        inBreakpoint = new Object();
        DebuggerCallback.breakpointLock = false;
        getVariablesLock = new Object();
        getVariablesResultLock = new Object();
        log = LogManager.getLogger("galliumdata.core");
    }
}
