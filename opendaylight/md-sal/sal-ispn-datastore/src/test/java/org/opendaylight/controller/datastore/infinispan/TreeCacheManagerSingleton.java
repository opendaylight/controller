package org.opendaylight.controller.datastore.infinispan;

import org.opendaylight.controller.datastore.ispn.TreeCacheManager;

public class TreeCacheManagerSingleton {
    private static TreeCacheManager tsm = null;
    public static TreeCacheManager get(){
        if(tsm == null){
            tsm =  new TreeCacheManager();
        }
        return tsm;
    }
}
