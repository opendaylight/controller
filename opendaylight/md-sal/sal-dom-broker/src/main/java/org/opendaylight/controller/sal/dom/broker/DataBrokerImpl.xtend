package org.opendaylight.controller.sal.dom.broker

import org.opendaylight.controller.sal.core.api.data.DataProviderService
import org.opendaylight.controller.sal.common.DataStoreIdentifier
import org.opendaylight.controller.sal.core.api.data.DataProviderService.DataRefresher
import org.opendaylight.controller.sal.core.api.data.DataValidator
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.controller.sal.dom.broker.impl.DataReaderRouter
import org.opendaylight.controller.sal.core.api.data.DataChangeListener
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.md.sal.common.api.data.DataReader

class DataBrokerImpl implements DataProviderService {

    val readRouter = new DataReaderRouter();

    override addRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        // NOOP
    }

    override addValidator(DataStoreIdentifier store, DataValidator validator) {
        // NOOP
    }

    override beginTransaction() {
        // NOOP
    }

    override readConfigurationData(InstanceIdentifier path) {
        readRouter.readConfigurationData(path)
    }

    override readOperationalData(InstanceIdentifier path) {
        readRouter.readOperationalData(path)
    }

    override registerConfigurationReader(InstanceIdentifier path, DataReader<InstanceIdentifier, CompositeNode> reader) {
        readRouter.registerConfigurationReader(path, reader);
    }

    override registerOperationalReader(InstanceIdentifier path, DataReader<InstanceIdentifier, CompositeNode> reader) {
        readRouter.registerOperationalReader(path, reader);
    }

    override removeRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        // NOOP
    }

    override removeValidator(DataStoreIdentifier store, DataValidator validator) {
        // NOOP
    }

    override registerDataChangeListener(InstanceIdentifier path, DataChangeListener listener) {
        // NOOP
    }

    override registerCommitHandler(InstanceIdentifier path,
        DataCommitHandler<InstanceIdentifier, CompositeNode> commitHandler) {
        // NOOP
    }

}
