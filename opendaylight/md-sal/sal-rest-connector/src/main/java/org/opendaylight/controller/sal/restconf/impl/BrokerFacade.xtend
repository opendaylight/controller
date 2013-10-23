package org.opendaylight.controller.sal.restconf.impl

import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier

class BrokerFacade implements DataReader<InstanceIdentifier, CompositeNode> {

    @Property
    private ConsumerSession context;
    
    @Property
    private DataBrokerService dataService;
    
    override readConfigurationData(InstanceIdentifier path) {
        return dataService.readConfigurationData(path);
    }

    override readOperationalData(InstanceIdentifier path) {
        return dataService.readOperationalData(path);
    }
    
    def RpcResult<CompositeNode> invokeRpc(QName type, CompositeNode payload) {
        val future = context.rpc(type, payload);
        return future.get;
    }
    
    def commitConfigurationDataUpdate(InstanceIdentifier path, CompositeNode payload) {
        val transaction = dataService.beginTransaction;
        transaction.putConfigurationData(path, payload);
        return transaction.commit()
    }
    
    def commitConfigurationDataCreate(InstanceIdentifier path, CompositeNode payload) {
        val transaction = dataService.beginTransaction;
        transaction.putConfigurationData(path, payload);
        return transaction.commit()
    }
    
}
