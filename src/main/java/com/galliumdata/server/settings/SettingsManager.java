// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.settings;

import java.nio.file.Path;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileInputStream;
import com.galliumdata.server.repository.RepositoryException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import com.galliumdata.server.log.Markers;
import java.util.HashMap;
import com.galliumdata.server.ServerException;
import org.apache.logging.log4j.Logger;
import java.util.Map;

public class SettingsManager
{
    private static SettingsManager instance;
    private static boolean ready;
    private final Map<SettingName, Object> settings;
    private static final Logger log;
    
    public static void initialize(final String[] params) {
        SettingsManager.instance = new SettingsManager(params);
        SettingsManager.ready = true;
    }
    
    public static SettingsManager getInstance() {
        if (null == SettingsManager.instance) {
            ServerException.throwException("settings.NotYetReady");
        }
        return SettingsManager.instance;
    }
    
    public String getStringSetting(final SettingName s) {
        return (String) this.settings.get(s);
    }
    
    private SettingsManager(final String[] params) {
        this.settings = new HashMap<SettingName, Object>();
        for (int i = 0; i < params.length; ++i) {
            SettingsManager.log.debug(Markers.SYSTEM, "Option received: {}", (Object)params[i]);
            if (params[i].length() == 2 && params[i].charAt(0) == '-' && params[i].charAt(1) != '-') {
                final SettingName setName = SettingName.getByShortOption(params[i].substring(1, 2));
                if (null == setName) {
                    throw new SettingsException(null, params[i].substring(1, 2));
                }
                if (setName.hasValue()) {
                    if (params.length < i + 2) {
                        throw new SettingsException(setName, "settings.NoValueSpecified");
                    }
                    this.setOptionValue(setName, params[i + 1]);
                    ++i;
                }
                else {
                    this.setOptionValue(setName, true);
                }
                if (SettingsManager.log.isDebugEnabled()) {
                    SettingsManager.log.debug(Markers.SYSTEM, "Command-line setting: {} = {}", (Object)setName.getName(), this.settings.get(setName));
                }
            }
            else if (params[i].length() > 2 && params[i].startsWith("--")) {
                String opt = params[i];
                final int equalsIdx = opt.indexOf(61);
                if (equalsIdx != -1) {
                    opt = params[i].substring(0, equalsIdx);
                    final SettingName setName2 = SettingName.getByLongOption(opt.substring(2));
                    if (null == setName2) {
                        throw new SettingsException(null, opt);
                    }
                    if (!setName2.hasValue()) {
                        throw new SettingsException(setName2, "settings.OptionDoesNotTakeValue");
                    }
                    final String value = params[i].substring(equalsIdx + 1);
                    this.setOptionValue(setName2, value);
                }
                else {
                    final SettingName setName2 = SettingName.getByLongOption(opt.substring(2));
                    if (null == setName2) {
                        throw new SettingsException(null, opt.substring(2));
                    }
                    if (setName2.hasValue()) {
                        throw new SettingsException(setName2, "settings.NoValueSpecified");
                    }
                    this.setOptionValue(setName2, true);
                }
            }
        }
        final SettingName[] values = SettingName.values();
        for (int length = values.length, j = 0; j < length; ++j) {
            final SettingName settingName = values[j];
            String value = System.getenv(settingName.getName());
            if (value != null && value.trim().length() > 0) {
                SettingsManager.log.debug(Markers.SYSTEM, "Parameter set by environment variable: " + settingName.getName() + "=" + value);
                this.setOptionValue(settingName, value);
            }
            else {
                value = System.getenv(settingName.getLongOption());
                if (value != null && value.trim().length() > 0) {
                    SettingsManager.log.debug(Markers.SYSTEM, "Parameter set by environment variable: " + settingName.getLongOption() + "=" + value);
                    this.setOptionValue(settingName, value);
                }
            }
        }
        final LoggerContext context = (LoggerContext)LogManager.getContext(false);
        final Configuration config = context.getConfiguration();
        SettingsManager.log.debug(Markers.SYSTEM, "Logging configuration loaded from: " + String.valueOf(config.getConfigurationSource()));
        SettingsManager.ready = true;
    }
    
    private void setOptionValue(final SettingName settingName, final Object value) {
        if (this.settings.containsKey(settingName)) {
            SettingsManager.log.debug(Markers.SYSTEM, "Overriding setting " + String.valueOf(settingName) + " with: " + String.valueOf(value));
        }
        this.settings.put(settingName, value);
        if (settingName == SettingName.SETTINGS_LOCATION) {
            this.readSettingsFile((String)value);
        }
    }
    
    private void readSettingsFile(final String filePath) {
        SettingsManager.log.debug(Markers.SYSTEM, "Reading settings from file {}", (Object)filePath);
        final Path path = Paths.get(filePath, new String[0]);
        if (!Files.exists(path, new LinkOption[0])) {
            throw new RepositoryException("repo.NoSuchFile", new Object[] { "settings", filePath });
        }
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            throw new RepositoryException("repo.UnableToRead", new Object[] { filePath, "not a regular file" });
        }
        if (!Files.isReadable(path)) {
            throw new RepositoryException("repo.UnableToRead", new Object[] { filePath, "file is not readable" });
        }
        try {
            final FileInputStream fis = new FileInputStream(filePath);
            final Properties props = new Properties();
            props.load(fis);
            fis.close();
            props.entrySet().forEach(e -> {
                final String name = (String) e.getKey();
                final SettingName sn = SettingName.getByLongOption(name);
                if (sn == null) {
                   throw  new SettingsException("settings.InvalidOptionSpecified", new Object[] { name });

                }
                else {
                    final String value = (String) e.getValue();
                    this.setOptionValue(sn, value);
                }
            });
        }
        catch (final Exception ex) {
            throw new RepositoryException("repo.UnableToRead", new Object[] { filePath, ex.getMessage() });
        }
    }
    
    static {
        SettingsManager.ready = false;
        log = LogManager.getLogger("galliumdata.core");
    }
}
