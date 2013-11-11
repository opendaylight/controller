package org.opendaylight.controller.sal.dom.broker.impl

import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.yangtools.yang.common.RpcResult
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.controller.sal.common.util.Rpcs
import java.util.Collections
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import static extension org.opendaylight.controller.sal.dom.broker.impl.DataUtils.*;

class HashMapDataStore //
implements //
DataReader<InstanceIdentifier, CompositeNode>, DataCommitHandler<InstanceIdentifier, CompositeNode> {

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
            configuration.remove(removal);
        }
        for (removal : modification.removedOperationalData) {
            operational.remove(removal);
        }
        return Rpcs.getRpcResult(true, null, Collections.emptySet);
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
