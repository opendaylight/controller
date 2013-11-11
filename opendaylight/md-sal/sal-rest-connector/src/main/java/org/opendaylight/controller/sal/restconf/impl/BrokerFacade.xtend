package org.opendaylight.controller.sal.restconf.impl

import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import static org.opendaylight.controller.sal.restconf.impl.BrokerFacade.*

class BrokerFacade implements DataReader<InstanceIdentifier, CompositeNode> {

    val static BrokerFacade INSTANCE = new BrokerFacade

    @Property
    private ConsumerSession context;

    @Property
    private DataBrokerService dataService;
    
    private new() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
    }
    
    def static BrokerFacade getInstance() {
        return INSTANCE
    }

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

    def commitConfigurationDataPut(InstanceIdentifier path, CompositeNode payload) {
        val transaction = dataService.beginTransaction;
        transaction.putConfigurationData(path, payload);
        return transaction.commit()
    }

    def commitOperationalDataPut(InstanceIdentifier path, CompositeNode payload) {
        val transaction = dataService.beginTransaction;
        transaction.putOperationalData(path, payload);
        return transaction.commit()
    }
    
}
