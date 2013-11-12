
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
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * The ClusteredDataStoreImpl stores global data to be shared across a controller cluster. It uses Clustering Services.
 */
public class ClusteredDataStoreImpl implements ClusteredDataStore {


    public static final String OPERATIONAL_DATA_CACHE = "clustered_data_store.operational_data_cache";
    public static final String CONFIGURATION_DATA_CACHE = "clustered_data_store.configuration_data_cache";

    private final ConcurrentMap operationalDataCache;
    private final ConcurrentMap configurationDataCache;

    public ClusteredDataStoreImpl(IClusterGlobalServices clusterGlobalServices) throws CacheExistException, CacheConfigException {
        Preconditions.checkNotNull(clusterGlobalServices, "clusterGlobalServices cannot be null");

        operationalDataCache = getOrCreateCache(clusterGlobalServices, OPERATIONAL_DATA_CACHE);

        if(operationalDataCache == null){
            Preconditions.checkNotNull(operationalDataCache, "operationalDataCache cannot be null");
        }

        configurationDataCache = getOrCreateCache(clusterGlobalServices, CONFIGURATION_DATA_CACHE);

        if(configurationDataCache == null){
            Preconditions.checkNotNull(configurationDataCache, "configurationDataCache cannot be null");
        }

    }

    @Override
    public DataCommitTransaction<InstanceIdentifier<? extends Object>, Object> requestCommit(DataModification<InstanceIdentifier<? extends Object>, Object> modification) {
        return new ClusteredDataStoreTransaction(modification);
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

    private RpcResult<Void> finish(final ClusteredDataStoreTransaction transaction) {
      final DataModification<InstanceIdentifier<? extends Object>,Object> modification = transaction.getModification();

      this.configurationDataCache.putAll(modification.getUpdatedConfigurationData());
      this.operationalDataCache.putAll(modification.getUpdatedOperationalData());

      for (final InstanceIdentifier<? extends Object> removal : modification.getRemovedConfigurationData()) {
        this.configurationDataCache.remove(removal);
      }

      for (final InstanceIdentifier<? extends Object> removal : modification.getRemovedOperationalData()) {
        this.operationalDataCache.remove(removal  );
      }

      Set<RpcError> _emptySet = Collections.<RpcError>emptySet();
      return Rpcs.<Void>getRpcResult(true, null, _emptySet);
    }

    private RpcResult<Void> rollback(final ClusteredDataStoreTransaction transaction) {
      Set<RpcError> _emptySet = Collections.<RpcError>emptySet();
      return Rpcs.<Void>getRpcResult(true, null, _emptySet);
    }


    private ConcurrentMap getOrCreateCache(IClusterGlobalServices clusterGlobalServices, String name) throws CacheConfigException {
        ConcurrentMap cache = clusterGlobalServices.getCache(name);

        if(cache == null) {
            try {
                cache = clusterGlobalServices.createCache(name, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            } catch (CacheExistException e) {
                cache = clusterGlobalServices.getCache(name);
            }
        }
        return cache;
    }

    private class ClusteredDataStoreTransaction implements DataCommitTransaction<InstanceIdentifier<? extends Object>, Object> {
        private final DataModification<InstanceIdentifier<? extends Object>,Object> modification;

        public ClusteredDataStoreTransaction(DataModification<InstanceIdentifier<? extends Object>,Object> modification){
            Preconditions.checkNotNull(modification, "modification cannot be null");

            this.modification = modification;
        }

        @Override
        public DataModification<InstanceIdentifier<? extends Object>, Object> getModification() {
            return this.modification;
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            return ClusteredDataStoreImpl.this.finish(this);
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            return ClusteredDataStoreImpl.this.rollback(this);
        }
    }
}
