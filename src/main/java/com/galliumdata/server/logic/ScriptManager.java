// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import org.apache.logging.log4j.LogManager;
import com.google.common.cache.RemovalNotification;
import java.io.File;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.galliumdata.server.log.Markers;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.FileSystems;
import com.galliumdata.server.debug.DebugManager;
import com.google.common.cache.CacheLoader;
import java.time.Duration;
import com.google.common.cache.CacheBuilder;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Source;
import com.google.common.cache.LoadingCache;

public class ScriptManager
{
    private static final ScriptManager instance;
    private final LoadingCache<String, Source> sources;
    private static final Logger log;
    
    private ScriptManager() {
        this.sources = (LoadingCache<String, Source>)CacheBuilder.newBuilder().maximumSize(200L).expireAfterAccess(Duration.ofMinutes(10L)).removalListener(removalNotification -> {
            if (ScriptManager.log.isTraceEnabled()) {
                ScriptManager.log.trace(Markers.SYSTEM, "Removing script from cache: {}", removalNotification.getKey());
            }
        }).build((CacheLoader)new CacheLoader<String, Source>() {
            public Source load(final String key) throws RuntimeException {
                return ScriptManager.this.loadSource(key);
            }
        });
    }
    
    public static ScriptManager getInstance() {
        return ScriptManager.instance;
    }
    
    public Source getSource(final String srcName) {
        if (DebugManager.debugIsActive()) {
            return this.loadSource(srcName);
        }
        try {
            return (Source)this.sources.get((String) srcName);
        }
        catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void forgetAllScripts() {
        this.sources.invalidateAll();
    }
    
    private Source loadSource(final String key) {
        final Path jsonPath = FileSystems.getDefault().getPath(key, new String[0]);
        Path scriptPath = jsonPath.resolveSibling("code.js");
        final int numNames = scriptPath.getNameCount();
        final Path pathFromProject = scriptPath.subpath(numNames - 4, numNames);
        if (Files.exists(scriptPath, new LinkOption[0])) {
            try {
                String srcText = Files.readString(scriptPath);
                srcText = "context = Polyglot.import(\"context\"); log = Polyglot.import(\"log\"); (function(){ " + srcText + " \n})();";
                if (ScriptManager.log.isTraceEnabled()) {
                    ScriptManager.log.trace(Markers.SYSTEM, "Loading script " + key);
                }
                if (DebugManager.debugIsActive()) {
                    final URI uri = new URI(URLEncoder.encode(pathFromProject.toString(), StandardCharsets.UTF_8));
                    return Source.newBuilder("js", (CharSequence)srcText, scriptPath.toString()).uri(uri).cached(false).build();
                }
                final URI uri = new URI(URLEncoder.encode(String.valueOf(pathFromProject) + ":NOCACHE", StandardCharsets.UTF_8));
                return Source.newBuilder("js", (CharSequence)srcText, scriptPath.toString()).uri(uri).build();
            }
            catch (final IOException | URISyntaxException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
        scriptPath = jsonPath.resolveSibling("code.py");
        if (Files.exists(scriptPath, new LinkOption[0])) {
            final File file = scriptPath.toFile();
            try {
                final URI uri = new URI(URLEncoder.encode(pathFromProject.toString(), StandardCharsets.UTF_8));
                return Source.newBuilder("python", file).uri(uri).build();
            }
            catch (final IOException | URISyntaxException ex2) {
                ex2.printStackTrace();
                throw new RuntimeException(ex2);
            }
        }
        try {
            final URI uri2 = new URI(URLEncoder.encode(pathFromProject.toString(), StandardCharsets.UTF_8));
            return Source.newBuilder("js", (CharSequence)"", scriptPath.toString()).uri(uri2).build();
        }
        catch (final IOException | URISyntaxException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    
    static {
        instance = new ScriptManager();
        log = LogManager.getLogger("galliumdata.core");
    }
}
