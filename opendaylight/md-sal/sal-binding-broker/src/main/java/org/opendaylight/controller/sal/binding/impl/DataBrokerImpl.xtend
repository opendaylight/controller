package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.common.DataStoreIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.DataRoot
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class DataBrokerImpl implements DataProviderService {

    override beginTransaction() {
    }

    override commit(DataStoreIdentifier store) {
        throw new UnsupportedOperationException("Deprecated")
    }

    override editCandidateData(DataStoreIdentifier store, DataRoot changeSet) {
        throw new UnsupportedOperationException("Deprecated")
    }

    override <T extends DataRoot> getCandidateData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("Deprecated")
    }

    override <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("Deprecated")
    }

    override getConfigurationData(InstanceIdentifier<?> data) {
        throw new UnsupportedOperationException("Deprecated")
    }

    override <T extends DataRoot> getData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("Deprecated")
    }

    override <T extends DataRoot> T getData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("Deprecated")
    }

    override getData(InstanceIdentifier<? extends DataObject> path) {
        return readOperationalData(path);
    }

    override readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
    }

    override readOperationalData(InstanceIdentifier<? extends DataObject> path) {
    }

    override registerChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener) {
    }

    override registerCommitHandler(InstanceIdentifier<? extends DataObject> path,
        DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> commitHandler) {
    }

    override registerDataChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener listener) {
    }

    override unregisterChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener) {
    }

}
