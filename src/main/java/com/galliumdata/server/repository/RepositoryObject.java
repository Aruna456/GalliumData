// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import org.apache.logging.log4j.LogManager;
import java.lang.reflect.Method;
import com.galliumdata.server.log.Markers;
import java.util.Collection;
import java.util.stream.Stream;
import java.nio.file.FileVisitOption;
import java.nio.file.LinkOption;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.io.BufferedReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.lang.reflect.Field;
import java.io.Reader;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import java.nio.file.Path;
import com.galliumdata.server.Zippable;

public abstract class RepositoryObject implements Zippable
{
    protected Repository repository;
    protected RepositoryObject parentObject;
    protected Path path;
    protected boolean isRead;
    @Persisted(JSONName = "active")
    protected boolean isActive;
    protected String name;
    private static final Logger log;
    
    public RepositoryObject() {
        this.isRead = false;
        this.isActive = true;
    }
    
    public RepositoryObject(final Repository repo) {
        this.isRead = false;
        this.isActive = true;
        this.repository = repo;
    }
    
    protected void setRepository(final Repository repo) {
        this.repository = repo;
    }
    
    public RepositoryObject getParentObject() {
        return this.parentObject;
    }
    
    protected void setParentObject(final RepositoryObject parent) {
        this.parentObject = parent;
    }
    
    public void readFromJson() {
        if (this.isRead) {
            return;
        }
        this.isRead = true;
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try (final BufferedReader reader = Files.newBufferedReader(this.path)) {
            node = mapper.readTree((Reader)reader);
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.BadFile", new Object[] { this.path.toString(), ex.getMessage() });
        }
        final Field[] fields = this.getClass().getDeclaredFields();
        final Field[] superFields = this.getClass().getSuperclass().getDeclaredFields();
        final Field[] allFields = new Field[fields.length + superFields.length];
        System.arraycopy(fields, 0, allFields, 0, fields.length);
        System.arraycopy(superFields, 0, allFields, fields.length, superFields.length);
        for (int i = 0; i < allFields.length; ++i) {
            final Field field = allFields[i];
            final Persisted pers = field.getAnnotation(Persisted.class);
            if (null != pers) {
                final String subDirName = pers.directoryName();
                if ("".equals(subDirName)) {
                    final String jsonName = pers.JSONName();
                    final Class<?> cls = field.getType();
                    Object value = null;
                    final JsonNode valueNode = node.get(jsonName);
                    if (valueNode == null) {
                        value = null;
                    }
                    else if (cls.equals(String.class)) {
                        value = valueNode.asText();
                        if (pers.allowedValues().length > 0 && !Arrays.asList(pers.allowedValues()).contains(value)) {
                            throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { jsonName, value, this.getName() });
                        }
                    }
                    else if (cls.equals(Boolean.class) || cls.equals(Boolean.TYPE)) {
                        value = valueNode.booleanValue();
                    }
                    else if (cls.equals(Integer.TYPE) || cls.equals(Integer.class)) {
                        if (valueNode.isNull()) {
                            value = 0;
                        }
                        else {
                            value = valueNode.intValue();
                        }
                    }
                    else if (cls.equals(Map.class)) {
                        value = new HashMap();
                        final Iterator<Map.Entry<String, JsonNode>> iter = valueNode.fields();
                        while (iter.hasNext()) {
                            final Map.Entry<String, JsonNode> entry = iter.next();
                            final String name = entry.getKey();
                            final Object val = RepositoryUtil.getJsonNodeValue((JsonNode)entry.getValue(), "Property " + name + " in " + String.valueOf(this.path));
                            ((Map)value).put(name, val);
                        }
                    }
                    else {
                        if (!cls.equals(Set.class)) {
                            throw new RepositoryException("", new Object[] { field.toString(), cls.getName() });
                        }
                        value = new HashSet();
                        for (int j = 0; j < valueNode.size(); ++j) {
                            final JsonNode libNode = valueNode.get(j);
                            Constructor<? extends RepositoryObject> constructor = null;
                            try {
                                constructor = pers.memberClass().getConstructor(Repository.class, JsonNode.class);
                                final RepositoryObject newObj = (RepositoryObject)constructor.newInstance(this.repository, libNode);
                                ((Set)value).add(newObj);
                            }
                            catch (final InvocationTargetException itex) {
                                throw new RepositoryException("repo.ErrorLoading", new Object[] { itex.getTargetException().getMessage() });
                            }
                            catch (final Exception ex2) {
                                throw new RepositoryException("repo.ErrorLoading", new Object[] { ex2.getMessage() });
                            }
                        }
                    }
                    if (null != value) {
                        try {
                            field.set(this, value);
                        }
                        catch (final Exception ex3) {
                            throw new RepositoryException("repo.AccessError", new Object[] { field.getName(), "repository", ex3.getMessage() });
                        }
                    }
                }
            }
        }
    }
    
    public void forgetEverything() {
    }
    
    @Override
    public byte[] zip() throws IOException {
        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        final ZipOutputStream zipOut = new ZipOutputStream(fos);
        final File fileToZip = this.path.getParent().toFile();
        this.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
        return fos.toByteArray();
    }
    
    protected void zipFile(final File fileToZip, final String fileName, final ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            }
            else {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            }
            final File[] listFiles;
            final File[] children = listFiles = fileToZip.listFiles();
            for (int length2 = listFiles.length, i = 0; i < length2; ++i) {
                final File childFile = listFiles[i];
                this.zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        final FileInputStream fis = new FileInputStream(fileToZip);
        final ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        final byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
    
    protected void readCollectionFromJSON(final Field fld) {
        final Persisted pers = fld.getAnnotation(Persisted.class);
        if (null == pers) {
            return;
        }
        final String subDirName = pers.directoryName();
        final Path subDirPath = this.path.resolveSibling(subDirName);
        if (!Files.exists(subDirPath, new LinkOption[0])) {
            return;
        }
        if (!Files.isDirectory(subDirPath, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", new Object[] { subDirPath, "not a directory" });
        }
        if (!Files.isReadable(subDirPath)) {
            throw new RepositoryException("repo.BadLocation", new Object[] { subDirPath, "cannot read" });
        }
        final Class<? extends RepositoryObject> compCls = pers.memberClass();
        if (compCls.equals(NullClass.class)) {
            return;
        }
        try (final Stream<Path> paths = Files.walk(subDirPath, 1, new FileVisitOption[0])) {
            final Throwable t;
            paths.forEach(tt -> this.addFileToField(fld, pers, tt));
        }
        catch (final IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }
    
    private void addFileToField(final Field field, final Persisted pers, final Path path) {
        Path fileToReadPath = path;
        final String subFileName = pers.fileName();
        if (Files.isDirectory(path, new LinkOption[0])) {
            if ("".equals(subFileName)) {
                return;
            }
            fileToReadPath = fileToReadPath.resolve(subFileName);
            if (!Files.exists(fileToReadPath, new LinkOption[0])) {
                return;
            }
        }
        else if (!path.getFileName().toString().endsWith(".json")) {
            return;
        }
        final Class<? extends RepositoryObject> compCls = pers.memberClass();
        if (compCls.equals(NullClass.class)) {
            throw new RepositoryException("repo.UnexpectedDirectory", new Object[] { path });
        }
        final Class<?> fldCls = field.getType();
        Class<Collection> collCls = null;
        Class<Map> mapCls = null;
        if (Map.class.isAssignableFrom(fldCls)) {
            mapCls = (Class<Map>)fldCls;
        }
        else if (Collection.class.isAssignableFrom(fldCls)) {
            collCls = (Class<Collection>)fldCls;
        }
        try {
            final Constructor<? extends RepositoryObject> constructor = compCls.getConstructor(Repository.class);
            final RepositoryObject newChild = (RepositoryObject)constructor.newInstance(this.repository);
            newChild.path = fileToReadPath;
            newChild.setParentObject(this);
            newChild.readFromJson();
            final Object fieldValueObj = field.get(this);
            if (collCls != null) {
                final Method method = collCls.getMethod("add", Object.class);
                method.invoke(fieldValueObj, newChild);
            }
            else {
                final Method method = mapCls.getMethod("put", Object.class, Object.class);
                String objName;
                if ("".equals(subFileName)) {
                    objName = fileToReadPath.getName(fileToReadPath.getNameCount() - 1).toString();
                    objName = objName.substring(0, objName.length() - 5);
                }
                else {
                    objName = fileToReadPath.getName(fileToReadPath.getNameCount() - 2).toString();
                }
                try {
                    method.invoke(fieldValueObj, objName, newChild);
                }
                catch (final Exception ex2) {
                    RepositoryObject.log.debug(Markers.REPO, "Exception calling method " + method.getName() + " : " + ex2.getMessage());
                    throw ex2;
                }
                newChild.setName(objName);
            }
        }
        catch (final Exception ex3) {
            ex3.printStackTrace();
        }
    }
    
    public String getComments() {
        final Path commentsPath = this.path.resolveSibling("comments.md");
        if (!Files.exists(commentsPath, new LinkOption[0])) {
            return null;
        }
        if (!Files.isRegularFile(commentsPath, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", new Object[] { commentsPath, "not a regular file" });
        }
        if (!Files.isReadable(commentsPath)) {
            throw new RepositoryException("repo.BadLocation", new Object[] { commentsPath, "cannot read" });
        }
        try {
            return Files.readString(commentsPath);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    public void setName(final String s) {
        this.name = s;
    }
    
    public boolean isActive() {
        return this.isActive;
    }
    
    public Path getPath() {
        return this.path;
    }
    
    public String getCode() {
        final Path codePath = this.path.resolveSibling("code.js");
        if (!Files.exists(codePath, new LinkOption[0])) {
            return null;
        }
        if (!Files.isRegularFile(codePath, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", new Object[] { codePath, "not a regular file" });
        }
        if (!Files.isReadable(codePath)) {
            throw new RepositoryException("repo.BadLocation", new Object[] { codePath, "cannot read" });
        }
        try {
            return Files.readString(codePath);
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
