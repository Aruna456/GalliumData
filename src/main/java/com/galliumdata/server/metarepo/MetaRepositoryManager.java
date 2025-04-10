// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.metarepo;

import org.apache.logging.log4j.LogManager;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import org.apache.logging.log4j.Logger;

public class MetaRepositoryManager
{
    private static MetaRepository mainRepo;
    private static final Logger log;
    
    public static MetaRepository getMainRepository() {
        if (MetaRepositoryManager.mainRepo != null) {
            return MetaRepositoryManager.mainRepo;
        }
        final String repoLoc = SettingsManager.getInstance().getStringSetting(SettingName.META_REPO_LOCATION);
        if (null == repoLoc) {
            throw new RepositoryException("repo.BadLocation", new Object[] { "null", "Location of meta-repository must be specified" });
        }
        return MetaRepositoryManager.mainRepo = new MetaRepository(repoLoc);
    }
    
    static {
        MetaRepositoryManager.mainRepo = null;
        log = LogManager.getLogger("galliumdata.core");
    }
}
