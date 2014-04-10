package org.opendaylight.controller.datastore;

import org.opendaylight.controller.datastore.infinispan.DataStoreImpl;
import org.opendaylight.controller.datastore.infinispan.InfinispanDataStoreManager;
import org.opendaylight.controller.datastore.ispn.TreeCacheManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator{
    TreeCacheManager treeCacheManager = null;
    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        logger.info("Starting Infinispan datastore");

//        treeCacheManager = new TreeCacheManager();
//        bundleContext.registerService(org.opendaylight.controller.datastore.infinispan.InfinispanDataStoreManager.class, new InfinispanDataStoreManagerImpl(), null);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }

    private class InfinispanDataStoreManagerImpl implements InfinispanDataStoreManager{

        @Override
        public DataStoreImpl getConfigurationDataStore() {
            return new DataStoreImpl(treeCacheManager);
        }

        @Override
        public DataStoreImpl getOperationalDataStore() {
            return new DataStoreImpl(treeCacheManager);
        }
    }
}
