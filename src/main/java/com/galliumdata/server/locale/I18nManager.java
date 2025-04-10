// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.locale;

import org.apache.logging.log4j.LogManager;
import java.util.MissingResourceException;
import com.galliumdata.server.log.Markers;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.HashMap;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import java.util.ResourceBundle;

public class I18nManager
{
    private static ResourceBundle defaultErrorMessageBundle;
    private static Map<String, ResourceBundle> errorMessagesBundles;
    private static final Logger log;
    
    public static String getString(final String s, final Object... args) {
        synchronized (I18nManager.class) {
            if (I18nManager.errorMessagesBundles == null) {
                I18nManager.errorMessagesBundles = new HashMap<String, ResourceBundle>();
                try {
                    I18nManager.defaultErrorMessageBundle = ResourceBundle.getBundle("i18n.ErrorMessages", Locale.getDefault());
                }
                catch (final Exception ex) {
                    throw new RuntimeException("Unable to find error messages file i18n.ErrorMessages");
                }
            }
        }
        final String[] parts = s.split("\\.");
        if (parts.length < 2) {
            throw new RuntimeException("Internal error: message name invalid: " + s);
        }
        final String bundleName = parts[0] + "." + parts[1] + ".ErrorMessages";
        ResourceBundle bundle = I18nManager.defaultErrorMessageBundle;
        if (I18nManager.errorMessagesBundles.containsKey(bundleName)) {
            bundle = I18nManager.errorMessagesBundles.get(bundleName);
        }
        MessageFormat formatter;
        try {
            formatter = new MessageFormat(bundle.getString(s));
        }
        catch (final MissingResourceException mrex) {
            ResourceBundle dbBundle = null;
            try {
                dbBundle = ResourceBundle.getBundle(bundleName, Locale.getDefault());
                I18nManager.errorMessagesBundles.put(bundleName, dbBundle);
            }
            catch (final Exception ex2) {
                I18nManager.log.debug(Markers.REPO, "Unable to find error message bundle: " + bundleName);
                return "Unable to find error message bundle: " + bundleName;
            }
            try {
                formatter = new MessageFormat(dbBundle.getString(s));
            }
            catch (final MissingResourceException mrex2) {
                I18nManager.log.debug(Markers.REPO, "Error formatting error message: " + s + ": " + mrex.getMessage());
                return "Unable to find error message: " + s;
            }
        }
        try {
            return formatter.format(args);
        }
        catch (final Exception ex3) {
            I18nManager.log.debug(Markers.SYSTEM, "Error formatting message " + s + ": " + ex3.getMessage());
            return "Unable to format message " + s;
        }
    }
    
    public static void main(final String[] args) {
        System.out.println(getString("db.mysql.server.TooManyRequests", 12, 34));
        System.out.println(getString("db.postgres.server.TooManyRequests", 56, 78));
    }
    
    static {
        log = LogManager.getLogger("galliumdata.dbproto");
    }
}
