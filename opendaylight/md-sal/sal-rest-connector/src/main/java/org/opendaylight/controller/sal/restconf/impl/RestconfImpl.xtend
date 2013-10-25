package org.opendaylight.controller.sal.restconf.impl

import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.yangtools.yang.data.api.CompositeNode

import static com.google.common.base.Preconditions.*

class RestconfImpl implements RestconfService {

    @Property
    BrokerFacade broker

    @Property
    extension ControllerContext controllerContext

    val JsonMapper jsonMapper = new JsonMapper;

    def init(SchemaService schemaService) {
        checkState(broker !== null)
        checkState(controllerContext !== null)
        checkState(schemaService !== null)
        controllerContext.schemas = schemaService.globalContext
    }

    override readAllData() {
        return broker.readOperationalData("".removePrefixes.toInstanceIdentifier.getInstanceIdentifier);
    }

    override getModules() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override getRoot() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    override readData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.removePrefixes.toInstanceIdentifier
        val data = broker.readOperationalData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
        jsonMapper.convert(instanceIdentifierWithSchemaNode.getSchemaNode, data)
    }

    override createConfigurationData(String identifier, CompositeNode payload) {
        return broker.commitConfigurationDataCreate(identifier.removePrefixes.toInstanceIdentifier.getInstanceIdentifier, payload);
    }

    override updateConfigurationData(String identifier, CompositeNode payload) {
        return broker.commitConfigurationDataCreate(identifier.removePrefixes.toInstanceIdentifier.getInstanceIdentifier, payload);
    }

    override invokeRpc(String identifier, CompositeNode payload) {
        val rpcResult = broker.invokeRpc(identifier.removePrefixes.toRpcQName, payload);
        jsonMapper.convert(identifier.removePrefixes.toInstanceIdentifier.getSchemaNode, rpcResult.result);
    }

    private def String removePrefixes(String path) {
        return path;
    }

}
