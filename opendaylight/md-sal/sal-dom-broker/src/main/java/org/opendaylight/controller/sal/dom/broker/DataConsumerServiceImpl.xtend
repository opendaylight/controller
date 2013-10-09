package org.opendaylight.controller.sal.dom.broker

import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.common.DataStoreIdentifier
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.controller.sal.core.api.data.DataChangeListener

class DataConsumerServiceImpl implements DataBrokerService {
    
    override beginTransaction() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override readConfigurationData(InstanceIdentifier path) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
    override readOperationalData(InstanceIdentifier path) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
    override registerDataChangeListener(InstanceIdentifier path, DataChangeListener listener) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
}