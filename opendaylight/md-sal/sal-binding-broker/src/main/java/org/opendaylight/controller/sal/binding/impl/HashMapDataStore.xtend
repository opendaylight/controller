package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.yangtools.yang.common.RpcResult
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.controller.sal.common.util.Rpcs
import java.util.Collections

class HashMapDataStore //
implements //
RuntimeDataProvider, DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

    val Map<InstanceIdentifier<? extends DataObject>,DataObject> configuration = new ConcurrentHashMap();
    val Map<InstanceIdentifier<? extends DataObject>,DataObject> operational = new ConcurrentHashMap();


    override readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        configuration.get(path);
    }

    override readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        operational.get(path);
    }

    override requestCommit(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return new HashMapDataStoreTransaction(modification,this);
    }
    
    def RpcResult<Void> rollback(HashMapDataStoreTransaction transaction) {
        return Rpcs.getRpcResult(true,null,Collections.emptySet);
    }
    
    def RpcResult<Void> finish(HashMapDataStoreTransaction transaction) {
        val modification = transaction.modification;
        configuration.putAll(modification.updatedConfigurationData);
        operational.putAll(modification.updatedOperationalData);
        
        for(removal : modification.removedConfigurationData) {
            configuration.remove(removal);
        }
        for(removal : modification.removedOperationalData) {
            operational.remove(removal);
        }
        return Rpcs.getRpcResult(true,null,Collections.emptySet);
    }

}

class HashMapDataStoreTransaction implements // 
DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {
    @Property
    val DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification

    @Property
    val HashMapDataStore datastore;
    
    
    new(
        DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modify,
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
