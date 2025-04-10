// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.security;

import org.apache.logging.log4j.LogManager;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.HashMap;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManager;

public class KeyLoaderManager
{
    private KeyManager[] keyManagers;
    private TrustManager[] trustManagers;
    public static final String GALLIUM_KEY_LOADER = "GALLIUM_KEY_LOADER";
    private static final Logger log;
    
    public KeyManager[] getKeyManagers() {
        return this.keyManagers;
    }
    
    public TrustManager[] getTrustManagers() {
        return this.trustManagers;
    }
    
    public void loadKeyLoaders() {
        this.keyManagers = null;
        this.trustManagers = null;
        String clsName = System.getenv("GALLIUM_KEY_LOADER");
        if (clsName == null || clsName.isBlank()) {
            return;
        }
        clsName = clsName.trim();
        if (KeyLoaderManager.log.isDebugEnabled()) {
            KeyLoaderManager.log.debug(Markers.SYSTEM, "KeyLoaderManager is attempting to load keys using " + clsName);
        }
        Class<?> cls;
        try {
            cls = Class.forName(clsName);
        }
        catch (final Exception ex) {
            KeyLoaderManager.log.error(Markers.SYSTEM, "Unable to load class for key loader " + clsName + ": " + ex.getMessage());
            ex.printStackTrace();
            return;
        }
        Constructor<?> constr;
        try {
            constr = cls.getDeclaredConstructor((Class<?>[])new Class[0]);
        }
        catch (final Exception ex2) {
            KeyLoaderManager.log.error(Markers.SYSTEM, "Unable to instantiate key loader " + clsName + ": " + ex2.getMessage());
            ex2.printStackTrace();
            return;
        }
        Object keyLoaderObj;
        try {
            keyLoaderObj = constr.newInstance(new Object[0]);
        }
        catch (final Exception ex3) {
            KeyLoaderManager.log.error(Markers.SYSTEM, "Unable to invoke key loader " + clsName + ": " + ex3.getMessage());
            ex3.printStackTrace();
            return;
        }
        if (!(keyLoaderObj instanceof KeyLoader)) {
            KeyLoaderManager.log.error(Markers.SYSTEM, "Key loader " + clsName + " does not implement the KeyLoader interface");
            return;
        }
        final KeyLoader keyLoader = (KeyLoader)keyLoaderObj;
        Map<String, Object> res;
        try {
            res = keyLoader.loadKeys(new HashMap<String, Object>());
        }
        catch (final Exception ex4) {
            KeyLoaderManager.log.error(Markers.SYSTEM, "Exception while invoking key loader " + clsName + ": " + ex4.getMessage());
            ex4.printStackTrace();
            return;
        }
        final Object keyManagersObj = res.get("keyManagers");
        if (keyManagersObj != null) {
            if (!(keyManagersObj instanceof KeyManager[])) {
                KeyLoaderManager.log.error(Markers.SYSTEM, "Key loader " + clsName + " returned a value for keyManagers that is not an array of KeyManager");
                return;
            }
            this.keyManagers = (KeyManager[])keyManagersObj;
        }
        final Object trustManagersObj = res.get("trustManagers");
        if (trustManagersObj != null) {
            if (!(trustManagersObj instanceof TrustManager[])) {
                KeyLoaderManager.log.error(Markers.SYSTEM, "Key loader " + clsName + " returned a value for trustManagers that is not an array of TrustManager");
                return;
            }
            this.trustManagers = (TrustManager[])trustManagersObj;
        }
    }
    
    static {
        log = LogManager.getLogger("galliumdata.core");
    }
}
