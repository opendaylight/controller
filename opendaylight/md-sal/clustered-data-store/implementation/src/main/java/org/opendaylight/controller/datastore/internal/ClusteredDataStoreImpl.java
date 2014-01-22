
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
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final ConcurrentMap<InstanceIdentifier, CompositeNode> operationalDataCache;
    private final ConcurrentMap<InstanceIdentifier, CompositeNode> configurationDataCache;

    private Logger logger = LoggerFactory.getLogger(ClusteredDataStoreImpl.class);

    public ClusteredDataStoreImpl(IClusterGlobalServices clusterGlobalServices) throws CacheConfigException {
        logger.trace("Constructing clustered data store");
        Preconditions.checkNotNull(clusterGlobalServices, "clusterGlobalServices cannot be null");

        operationalDataCache = getOrCreateCache(clusterGlobalServices, OPERATIONAL_DATA_CACHE);

        Preconditions.checkNotNull(operationalDataCache, "operationalDataCache cannot be null");

        configurationDataCache = getOrCreateCache(clusterGlobalServices, CONFIGURATION_DATA_CACHE);

        Preconditions.checkNotNull(configurationDataCache, "configurationDataCache cannot be null");
    }

    @Override
    public DataCommitTransaction<InstanceIdentifier, CompositeNode> requestCommit(DataModification<InstanceIdentifier, CompositeNode> modification) {
        return new ClusteredDataStoreTransaction(modification);
    }

    @Override
    public CompositeNode readOperationalData(InstanceIdentifier path) {
        Preconditions.checkNotNull(path, "path cannot be null");
        return operationalDataCache.get(path);
    }

    @Override
    public boolean containsConfigurationPath(InstanceIdentifier path) {
        return configurationDataCache.containsKey(path);
    }

    @Override
    public boolean containsOperationalPath(InstanceIdentifier path) {
        return operationalDataCache.containsKey(path);
    }

    @Override
    public Iterable<InstanceIdentifier> getStoredConfigurationPaths() {
        return configurationDataCache.keySet();
    }

    @Override
    public Iterable<InstanceIdentifier> getStoredOperationalPaths() {
        return operationalDataCache.keySet();
    }



    @Override
    public CompositeNode readConfigurationData(InstanceIdentifier path) {
        Preconditions.checkNotNull(path, "path cannot be null");
        return configurationDataCache.get(path);
    }

    private RpcResult<Void> finish(final ClusteredDataStoreTransaction transaction) {
      final DataModification<InstanceIdentifier,CompositeNode> modification = transaction.getModification();

      this.configurationDataCache.putAll(modification.getUpdatedConfigurationData());
      this.operationalDataCache.putAll(modification.getUpdatedOperationalData());

      for (final InstanceIdentifier removal : modification.getRemovedConfigurationData()) {
        this.configurationDataCache.remove(removal);
      }

      for (final InstanceIdentifier removal : modification.getRemovedOperationalData()) {
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

    private class ClusteredDataStoreTransaction implements DataCommitTransaction<InstanceIdentifier, CompositeNode> {
        private final DataModification<InstanceIdentifier,CompositeNode> modification;

        public ClusteredDataStoreTransaction(DataModification<InstanceIdentifier,CompositeNode> modification){
            Preconditions.checkNotNull(modification, "modification cannot be null");

            this.modification = modification;
        }

        @Override
        public DataModification<InstanceIdentifier, CompositeNode> getModification() {
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
