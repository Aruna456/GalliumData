// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.rest;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.atomic.AtomicLong;
import com.sun.net.httpserver.BasicAuthenticator;

public class RestAuthenticator extends BasicAuthenticator
{
    private final String adminPassword;
    private static final AtomicLong lastPasswordFailure;
    private static final Logger log;
    
    public RestAuthenticator(final String realm) {
        super(realm);
        this.adminPassword = SettingsManager.getInstance().getStringSetting(SettingName.REST_PASSWORD);
    }
    
    @Override
    public boolean checkCredentials(final String username, final String password) {
        if (this.adminPassword == null || this.adminPassword.trim().length() == 0) {
            return true;
        }
        if (!"admin".equals(username)) {
            return false;
        }
        final boolean passwordMatches = this.adminPassword.equals(password);
        if (passwordMatches) {
            RestAuthenticator.lastPasswordFailure.set(0L);
        }
        else {
            if (RestAuthenticator.lastPasswordFailure.get() > 0L && System.currentTimeMillis() - RestAuthenticator.lastPasswordFailure.get() < 1000L) {
                try {
                    RestAuthenticator.log.debug(Markers.SYSTEM, "Repeated password failure - waiting 1 second");
                    Thread.sleep(1000L);
                }
                catch (final Exception ex) {}
            }
            RestAuthenticator.lastPasswordFailure.set(System.currentTimeMillis());
        }
        return passwordMatches;
    }
    
    static {
        lastPasswordFailure = new AtomicLong(0L);
        log = LogManager.getLogger("galliumdata.rest");
    }
}
