package org.opendaylight.controller.sal.dom.broker.impl

import java.util.Collections
import java.util.HashSet
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.sal.common.util.Rpcs
import org.opendaylight.controller.sal.core.api.data.DataStore
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.slf4j.LoggerFactory

import static extension org.opendaylight.controller.sal.dom.broker.impl.DataUtils.*

class HashMapDataStore implements DataStore, AutoCloseable {
    static val LOG = LoggerFactory.getLogger(HashMapDataStore);   
    val Map<InstanceIdentifier, CompositeNode> configuration = new ConcurrentHashMap();
    val Map<InstanceIdentifier, CompositeNode> operational = new ConcurrentHashMap();

    override readConfigurationData(InstanceIdentifier path) {
        configuration.read(path);
    }

    override readOperationalData(InstanceIdentifier path) {
        operational.read(path);
    }
    



    override requestCommit(DataModification<InstanceIdentifier, CompositeNode> modification) {
        return new HashMapDataStoreTransaction(modification, this);
    }

    def RpcResult<Void> rollback(HashMapDataStoreTransaction transaction) {
        return Rpcs.getRpcResult(true, null, Collections.emptySet);
    }

    def RpcResult<Void> finish(HashMapDataStoreTransaction transaction) {
        val modification = transaction.modification;
        configuration.putAll(modification.updatedConfigurationData);
        operational.putAll(modification.updatedOperationalData);

        for (removal : modification.removedConfigurationData) {
            remove(configuration,removal);
        }
        for (removal : modification.removedOperationalData) {
            remove(operational,removal);
        }
        LOG.info("New Operational Data: " + operational);
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
