
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.datastore.internal;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.datastore.ClusteredDataStore;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentMap;

/**
 * The ClusteredDataStoreImpl stores global data to be share across a controller cluster. It uses Clustering Services.
 */
public class ClusteredDataStoreImpl implements ClusteredDataStore {


    public static final String OPERATIONAL_DATA_CACHE = "clustered_data_store.operational_data_cache";
    public static final String CONFIGURATION_DATA_CACHE = "clustered_data_store.configuration_data_cache";

    private ConcurrentMap<?,?> operationalDataCache;
    private ConcurrentMap<?,?> configurationDataCache;

    public ClusteredDataStoreImpl(IClusterGlobalServices clusterGlobalServices) throws CacheExistException, CacheConfigException {
        Preconditions.checkNotNull(clusterGlobalServices, "clusterGlobalServices cannot be null");

        operationalDataCache = clusterGlobalServices.createCache(OPERATIONAL_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

        if(operationalDataCache == null){
            Preconditions.checkNotNull(operationalDataCache, "operationalDataCache cannot be null");
        }

        configurationDataCache = clusterGlobalServices.createCache(CONFIGURATION_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

        if(configurationDataCache == null){
            Preconditions.checkNotNull(configurationDataCache, "configurationDataCache cannot be null");
        }

    }

    @Override
    public DataCommitTransaction<InstanceIdentifier<? extends Object>, Object> requestCommit(DataModification<InstanceIdentifier<? extends Object>, Object> modification) {
        return null;
    }

    @Override
    public Object readOperationalData(InstanceIdentifier<? extends Object> path) {
        Preconditions.checkNotNull(path, "path cannot be null");
        return operationalDataCache.get(path);
    }

    @Override
    public Object readConfigurationData(InstanceIdentifier<? extends Object> path) {
        Preconditions.checkNotNull(path, "path cannot be null");
        return configurationDataCache.get(path);
    }
}
