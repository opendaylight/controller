package org.opendaylight.controller.sal.restconf.impl

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.rest.impl.RestconfProvider
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier

class BrokerFacade implements DataReader<InstanceIdentifier, CompositeNode> {

    val static BrokerFacade INSTANCE = new BrokerFacade

    @Property
    private ConsumerSession context;

    @Property
    private DataBrokerService dataService;
    
    private new() {
        if (INSTANCE !== null) {
            throw new IllegalStateException("Already instantiated");
        }
    }

    def static BrokerFacade getInstance() {
        return INSTANCE
    }

    private def void checkPreconditions() {
        if (context === null || dataService === null) {
            throw new WebApplicationException(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(RestconfProvider::NOT_INITALIZED_MSG).build())
        }
    }

    override readConfigurationData(InstanceIdentifier path) {
        checkPreconditions
        return dataService.readConfigurationData(path);
    }

    override readOperationalData(InstanceIdentifier path) {
        checkPreconditions
        return dataService.readOperationalData(path);
    }

    def RpcResult<CompositeNode> invokeRpc(QName type, CompositeNode payload) {
        checkPreconditions
        val future = context.rpc(type, payload);
        return future.get;
    }

    def commitConfigurationDataPut(InstanceIdentifier path, CompositeNode payload) {
        checkPreconditions
        val transaction = dataService.beginTransaction;
        transaction.putConfigurationData(path, payload);
        return transaction.commit()
    }

    def commitOperationalDataPut(InstanceIdentifier path, CompositeNode payload) {
        checkPreconditions
        val transaction = dataService.beginTransaction;
        transaction.putOperationalData(path, payload);
        return transaction.commit()
    }
    
}
