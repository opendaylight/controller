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
        throw new UnsupportedOperationException("Reading all data is currently not supported.")
    }

    override getModules() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getRoot() {
        return null;
    }

    override readData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.toInstanceIdentifier
        val data = broker.readOperationalData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
        return new StructuredData(data, instanceIdentifierWithSchemaNode.schemaNode)
    }

    override createConfigurationData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.toInstanceIdentifier
        return broker.commitConfigurationDataPut(identifierWithSchemaNode.instanceIdentifier,payload).get();
    }

    override updateConfigurationData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.toInstanceIdentifier
        return broker.commitConfigurationDataPut(identifierWithSchemaNode.instanceIdentifier,payload).get();
    }

    override invokeRpc(String identifier, CompositeNode payload) {
        val rpc = identifier.toQName;
        val rpcResult = broker.invokeRpc(rpc, payload);
        val schema = controllerContext.getRpcOutputSchema(rpc);
        return new StructuredData(rpcResult.result, schema);
    }
    
    override readConfigurationData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.toInstanceIdentifier
        val data = broker.readOperationalData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
        return new StructuredData(data, instanceIdentifierWithSchemaNode.schemaNode)
    }
    
    override readOperationalData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.toInstanceIdentifier
        val data = broker.readOperationalData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
        return new StructuredData(data, instanceIdentifierWithSchemaNode.schemaNode)
    }
    
    override updateConfigurationDataLegacy(String identifier, CompositeNode payload) {
        updateConfigurationData(identifier,payload);
    }
    
    override createConfigurationDataLegacy(String identifier, CompositeNode payload) {
        createConfigurationData(identifier,payload);
    }
    
    override createOperationalData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.toInstanceIdentifier
        return broker.commitOperationalDataPut(identifierWithSchemaNode.instanceIdentifier,payload).get();
    }
    
    override updateOperationalData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.toInstanceIdentifier
        return broker.commitOperationalDataPut(identifierWithSchemaNode.instanceIdentifier,payload).get();
    }

}
