// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.metarepo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galliumdata.server.repository.FilterStage;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.io.IOException;
import com.galliumdata.server.repository.RepositoryUtil;
import java.nio.file.Paths;
import com.galliumdata.server.repository.RepositoryException;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;
import com.galliumdata.server.Zippable;

public class MetaRepository implements Zippable
{
    private final Path rootDir;
    private Map<String, FilterImplementation> filterTypes;
    private final Map<String, Adapter> adapters;
    
    public MetaRepository(final String location) {
        this.adapters = new HashMap<String, Adapter>(10);
        if (null == location) {
            throw new RepositoryException("repo.BadLocation", new Object[] { "null", "null" });
        }
        RepositoryUtil.checkDirectory(this.rootDir = Paths.get(location, new String[0]));
    }
    
    public FilterImplementation getFilterType(final String name) {
        synchronized (this) {
            if (this.filterTypes == null) {
                this.filterTypes = new HashMap<String, FilterImplementation>(20);
                try {
                    this.readFilterTypes();
                }
                catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return this.filterTypes.get(name);
    }
    
    @Override
    public String getName() {
        return "metarepo";
    }
    
    private void readFilterTypes() throws IOException {
        // 
        // This method could not be decompiled.
        // 
        // Original Bytecode:
        // 
        //     1: getfield        com/galliumdata/server/metarepo/MetaRepository.rootDir:Ljava/nio/file/Path;
        //     4: ldc             "filter_types"
        //     6: invokeinterface java/nio/file/Path.resolve:(Ljava/lang/String;)Ljava/nio/file/Path;
        //    11: astore_1        /* filterTypesRoot */
        //    12: aload_1         /* filterTypesRoot */
        //    13: iconst_0       
        //    14: anewarray       Ljava/nio/file/LinkOption;
        //    17: invokestatic    java/nio/file/Files.exists:(Ljava/nio/file/Path;[Ljava/nio/file/LinkOption;)Z
        //    20: ifne            41
        //    23: new             Lcom/galliumdata/server/repository/RepositoryException;
        //    26: dup            
        //    27: ldc             "repo.MissingDirectory"
        //    29: iconst_1       
        //    30: anewarray       Ljava/lang/Object;
        //    33: dup            
        //    34: iconst_0       
        //    35: aload_1         /* filterTypesRoot */
        //    36: aastore        
        //    37: invokespecial   com/galliumdata/server/repository/RepositoryException.<init>:(Ljava/lang/String;[Ljava/lang/Object;)V
        //    40: athrow         
        //    41: aload_1         /* filterTypesRoot */
        //    42: iconst_1       
        //    43: iconst_0       
        //    44: anewarray       Ljava/nio/file/FileVisitOption;
        //    47: invokestatic    java/nio/file/Files.walk:(Ljava/nio/file/Path;I[Ljava/nio/file/FileVisitOption;)Ljava/util/stream/Stream;
        //    50: aload_0         /* this */
        //    51: aload_1         /* filterTypesRoot */
        //    52: invokedynamic   BootstrapMethod #0, accept:(Lcom/galliumdata/server/metarepo/MetaRepository;Ljava/nio/file/Path;)Ljava/util/function/Consumer;
        //    57: invokeinterface java/util/stream/Stream.forEach:(Ljava/util/function/Consumer;)V
        //    62: return         
        //    Exceptions:
        //  throws java.io.IOException
        //    StackMapTable: 00 01 FC 00 29 07 00 44
        // 
        // The error that occurred was:
        // 
        // java.lang.NullPointerException: Cannot invoke "com.strobel.assembler.metadata.TypeReference.getSimpleType()" because the return value of "com.strobel.decompiler.ast.Variable.getType()" is null
        //     at com.strobel.decompiler.languages.java.ast.NameVariables.generateNameForVariable(NameVariables.java:252)
        //     at com.strobel.decompiler.languages.java.ast.NameVariables.assignNamesToVariables(NameVariables.java:185)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.nameVariables(AstMethodBodyBuilder.java:1482)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.populateVariables(AstMethodBodyBuilder.java:1411)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:210)
        //     at com.strobel.decompiler.languages.java.ast.AstMethodBodyBuilder.createMethodBody(AstMethodBodyBuilder.java:93)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethodBody(AstBuilder.java:868)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createMethod(AstBuilder.java:761)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addTypeMembers(AstBuilder.java:638)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeCore(AstBuilder.java:605)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createTypeNoCache(AstBuilder.java:195)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.createType(AstBuilder.java:162)
        //     at com.strobel.decompiler.languages.java.ast.AstBuilder.addType(AstBuilder.java:137)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.buildAst(JavaLanguage.java:71)
        //     at com.strobel.decompiler.languages.java.JavaLanguage.decompileType(JavaLanguage.java:59)
        //     at com.strobel.decompiler.DecompilerDriver.decompileType(DecompilerDriver.java:333)
        //     at com.strobel.decompiler.DecompilerDriver.decompileJar(DecompilerDriver.java:254)
        //     at com.strobel.decompiler.DecompilerDriver.main(DecompilerDriver.java:129)
        // 
        throw new IllegalStateException("An error occurred while decompiling this method.");
    }
    
    public void forgetFilterTypes() {
        if (this.filterTypes != null) {
            this.filterTypes.values().forEach(FilterImplementation::forgetAllFilters);
        }
    }
    
    public Map<String, Adapter> getAdapters() {
        synchronized (this.adapters) {
            if (this.adapters.size() > 0) {
                return this.adapters;
            }
            try {
                this.readAdapters();
            }
            catch (final IOException ioex) {
                throw new RepositoryException("repo.ErrorReadingMetaRepository", new Object[] { this.rootDir, ioex.getMessage() });
            }
        }
        return this.adapters;
    }
    
    private void readAdapters() throws IOException {
        final Path adaptersRoot = this.rootDir.resolve("adapters");
        if (!Files.exists(adaptersRoot, new LinkOption[0])) {
            throw new RepositoryException("repo.MissingDirectory", new Object[] { adaptersRoot });
        }
        Files.walk(adaptersRoot, 1, new FileVisitOption[0]).forEach(adapterPath -> {
            try {
                if (adapterPath.equals(adaptersRoot) || Files.isHidden(adapterPath) || !Files.isDirectory(adapterPath, new LinkOption[0])) {
                    return;
                }
            }
            catch (final IOException ioex) {
                throw new RepositoryException("repo.ErrorReadingMetaRepository", new Object[] { adapterPath, ioex.getMessage() });
            }
            final String adapterName = adapterPath.getFileName().toString();
            final Adapter adapter = new Adapter(adapterPath, adapterName);
            adapter.readFromJson();
            this.adapters.put(adapterName, adapter);
        });
    }
    
    @Override
    public byte[] zip() throws IOException {
        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        final ZipOutputStream zipOut = new ZipOutputStream(fos);
        final File fileToZip = this.rootDir.toFile();
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
}
