// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.settings;

public enum SettingName
{
    SETTINGS_LOCATION("settings_location", "s", "settings-location", true), 
    REPO_LOCATION("repository_location", "r", "repository-location", true), 
    BACKUP_LOCATION("backup_location", "b", "backup-location", true), 
    META_REPO_LOCATION("meta_repo_location", "m", "meta-repo-location", true), 
    REST_PORT("rest_port", (String)null, "rest-port", true), 
    REST_PORT_SSL("rest_port_ssl", (String)null, "rest-port-ssl", true), 
    REST_KEYSTORE("rest_keystore", (String)null, "rest-keystore", true), 
    REST_KEYSTORE_PW("rest_keystore_pw", (String)null, "rest-keystore-pw", true), 
    WEB_BASE("web_base", "w", "web-base", true), 
    REST_PASSWORD("rest_password", "p", "rest-password", true), 
    REST_ADDR_RANGE("rest_address_range", (String)null, "rest-address-range", true), 
    NODE_MODULES_DIR("node_modules_dir", (String)null, "node-modules-dir", true), 
    NODE_GLOBALS("node_globals", (String)null, "node-globals", true), 
    NODE_MODULES_REPLACEMENTS("node_modules_replacements", (String)null, "node-modules-replacements", true), 
    IVY_SETTINGS("ivy_settings", (String)null, "ivy-settings", true), 
    NPM_REGISTRY_URL("npm_registry_url", (String)null, "npm-registry-url", true), 
    NODE_MODULES_DOWNLOAD_DIR("node_modules_download_dir", (String)null, "node-modules-download-dir", true), 
    SYSTEM_UUID("system_uuid", (String)null, "system-uuid", true), 
    INSTANCE_ID("instance_id", (String)null, "instance-id", true), 
    CAPTURE_DIR("capture_dir", (String)null, "capture-dir", true);
    
    private final String name;
    private final String shortOption;
    private final String longOption;
    private final boolean hasValue;
    
    private SettingName(final String name, final String shortOption, final String longOption, final boolean hasValue) {
        this.name = name;
        this.shortOption = shortOption;
        this.longOption = longOption;
        this.hasValue = hasValue;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getShortOption() {
        return this.shortOption;
    }
    
    public String getLongOption() {
        return this.longOption;
    }
    
    public boolean hasValue() {
        return this.hasValue;
    }
    
    public static SettingName getByName(final String name) {
        for (final SettingName set : values()) {
            if (set.getName().equals(name)) {
                return set;
            }
        }
        return null;
    }
    
    public static SettingName getByShortOption(final String opt) {
        for (final SettingName set : values()) {
            if (set.getShortOption() != null && set.getShortOption().equals(opt)) {
                return set;
            }
        }
        return null;
    }
    
    public static SettingName getByLongOption(final String opt) {
        for (final SettingName set : values()) {
            if (set.getLongOption() != null && set.getLongOption().equals(opt)) {
                return set;
            }
        }
        return null;
    }
}
