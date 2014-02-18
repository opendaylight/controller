/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl

import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.yangtools.yang.common.RpcResult
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.controller.sal.common.util.Rpcs
import java.util.Collections
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.sal.core.api.data.DataStore
import java.util.HashSet
import org.slf4j.LoggerFactory
import org.slf4j.Logger

final class HashMapDataStore implements DataStore, AutoCloseable {
    private val Logger LOG = LoggerFactory.getLogger(HashMapDataStore)

    val Map<InstanceIdentifier, CompositeNode> configuration = new ConcurrentHashMap();
    val Map<InstanceIdentifier, CompositeNode> operational = new ConcurrentHashMap();
    
    
    
    override containsConfigurationPath(InstanceIdentifier path) {
        return configuration.containsKey(path)
    }
    
    override containsOperationalPath(InstanceIdentifier path) {
        return operational.containsKey(path)
    }
    
    override getStoredConfigurationPaths() {
        configuration.keySet
    }
    
    override getStoredOperationalPaths() {
        operational.keySet
    }

    override readConfigurationData(InstanceIdentifier path) {
        LOG.trace("Reading configuration path {}", path)
        configuration.get(path);
    }

    override readOperationalData(InstanceIdentifier path) {
        LOG.trace("Reading operational path {}", path)
        operational.get(path);
    }



    override requestCommit(DataModification<InstanceIdentifier, CompositeNode> modification) {
        return new HashMapDataStoreTransaction(modification, this);
    }

    def RpcResult<Void> rollback(HashMapDataStoreTransaction transaction) {
        return Rpcs.getRpcResult(true, null, Collections.emptySet);
    }

    def RpcResult<Void> finish(HashMapDataStoreTransaction transaction) {
        val modification = transaction.modification;
        for (removal : modification.removedConfigurationData) {
            LOG.trace("Removing configuration path {}", removal)
            remove(configuration,removal);
        }
        for (removal : modification.removedOperationalData) {
            LOG.trace("Removing operational path {}", removal)
            remove(operational,removal);
        }
        if (LOG.isTraceEnabled()) {
            for (a : modification.updatedConfigurationData.keySet) {
                LOG.trace("Adding configuration path {}", a)
            }
            for (a : modification.updatedOperationalData.keySet) {
                LOG.trace("Adding operational path {}", a)
            }
        }
        configuration.putAll(modification.updatedConfigurationData);
        operational.putAll(modification.updatedOperationalData);

        return Rpcs.getRpcResult(true, null, Collections.emptySet);
    }
    
    def remove(Map<InstanceIdentifier, CompositeNode> map, InstanceIdentifier identifier) {
        val affected = new HashSet<InstanceIdentifier>();
        for(path : map.keySet) {
            if(identifier.contains(path)) {
                affected.add(path);
            }
        }
        for(pathToRemove : affected) {
            LOG.trace("Removed path {}", pathToRemove)
            map.remove(pathToRemove);
        }
        
    }


    override close()  {
        // NOOP
    }
    
}

class HashMapDataStoreTransaction implements // 
DataCommitTransaction<InstanceIdentifier, CompositeNode> {
    @Property
    val DataModification<InstanceIdentifier, CompositeNode> modification

    @Property
    val HashMapDataStore datastore;

    new(
        DataModification<InstanceIdentifier, CompositeNode> modify,
        HashMapDataStore store
    ) {
        _modification = modify;
        _datastore = store;
    }

    override finish() throws IllegalStateException {
        datastore.finish(this);

    }

    override getModification() {
        this._modification;
    }

    override rollback() throws IllegalStateException {
        datastore.rollback(this);
    }
}
