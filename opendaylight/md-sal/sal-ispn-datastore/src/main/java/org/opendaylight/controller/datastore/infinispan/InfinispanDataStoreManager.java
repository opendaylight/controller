package org.opendaylight.controller.datastore.infinispan;

public interface InfinispanDataStoreManager {
    DataStoreImpl getConfigurationDataStore();
    DataStoreImpl getOperationalDataStore();
}
