package org.opendaylight.controller.sal.restconf.impl

import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.sal.rest.api.RestconfService

class RestconfImpl implements RestconfService {
    
    val static RestconfImpl INSTANCE = new RestconfImpl

    @Property
    BrokerFacade broker

    @Property
    extension ControllerContext controllerContext
    
    private new() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
    }
    
    static def getInstance() {
        return INSTANCE
    }

    override readAllData() {
//        return broker.readOperationalData("".toInstanceIdentifier.getInstanceIdentifier);
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getModules() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getRoot() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    override readData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.toInstanceIdentifier
        val data = broker.readOperationalData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
        return new StructuredData(data, instanceIdentifierWithSchemaNode.schemaNode)
    }

    override createConfigurationData(String identifier, CompositeNode payload) {
//        return broker.commitConfigurationDataCreate(identifier.toInstanceIdentifier.getInstanceIdentifier, payload);
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override updateConfigurationData(String identifier, CompositeNode payload) {
//        return broker.commitConfigurationDataCreate(identifier.toInstanceIdentifier.getInstanceIdentifier, payload);
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override invokeRpc(String identifier, CompositeNode payload) {
        val rpcResult = broker.invokeRpc(identifier.toRpcQName, payload);
        return new StructuredData(rpcResult.result, identifier.toInstanceIdentifier.getSchemaNode)
    }

}
