// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.settings;

import com.galliumdata.server.locale.I18nManager;
import com.galliumdata.server.ServerException;

public class SettingsException extends ServerException
{
    private SettingName setting;
    
    public SettingsException(final SettingName setting, final String msg) {
        super(msg, new Object[0]);
        this.setting = setting;
    }
    
    public SettingsException(final String msgName, final Object... args) {
        super(msgName, args);
    }
    
    @Override
    public String getMessage() {
        if (null == this.setting) {
            return I18nManager.getString(this.msgName, this.args);
        }
        String fullName = this.setting.getName();
        if (null != this.setting.getShortOption() || null != this.setting.getLongOption()) {
            fullName += " (";
        }
        if (null != this.setting.getShortOption()) {
            fullName = fullName + " -" + this.setting.getShortOption();
        }
        if (null != this.setting.getShortOption() && null != this.setting.getLongOption()) {
            fullName += " / ";
        }
        if (null != this.setting.getLongOption()) {
            fullName = fullName + "--" + this.setting.getLongOption();
        }
        if (null != this.setting.getShortOption() || null != this.setting.getLongOption()) {
            fullName += " )";
        }
        return I18nManager.getString(this.msgName, fullName);
    }
}
