package org.opendaylight.controller.sal.restconf.impl

import java.util.List
import javax.ws.rs.core.Response
import org.opendaylight.controller.sal.rest.api.RestconfService
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import javax.ws.rs.WebApplicationException

class RestconfImpl implements RestconfService {
    
    val static RestconfImpl INSTANCE = new RestconfImpl

    @Property
    BrokerFacade broker

    @Property
    extension ControllerContext controllerContext
    
    private new() {
        if (INSTANCE !== null) {
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
        val instanceIdentifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val data = broker.readOperationalData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
        return new StructuredData(data, instanceIdentifierWithSchemaNode.schemaNode)
    }

    override createConfigurationData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val value = resolveNodeNamespaceBySchema(payload, identifierWithSchemaNode.schemaNode)
        val status = broker.commitConfigurationDataPut(identifierWithSchemaNode.instanceIdentifier,value).get();
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(Response.Status.OK).build
            default: Response.status(Response.Status.INTERNAL_SERVER_ERROR).build
        }
    }

    override updateConfigurationData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val value = resolveNodeNamespaceBySchema(payload, identifierWithSchemaNode.schemaNode)
        val status = broker.commitConfigurationDataPut(identifierWithSchemaNode.instanceIdentifier,value).get();
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(Response.Status.NO_CONTENT).build
            default: Response.status(Response.Status.INTERNAL_SERVER_ERROR).build
        }
    }

    override invokeRpc(String identifier, CompositeNode payload) {
        val rpc = identifier.toQName;
        val value = resolveNodeNamespaceBySchema(payload, controllerContext.getRpcInputSchema(rpc))
        val rpcResult = broker.invokeRpc(rpc, value);
        val schema = controllerContext.getRpcOutputSchema(rpc);
        return new StructuredData(rpcResult.result, schema);
    }
    
    override readConfigurationData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val data = broker.readOperationalData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
        return new StructuredData(data, instanceIdentifierWithSchemaNode.schemaNode)
    }
    
    override readOperationalData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.resolveInstanceIdentifier
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
        val identifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val value = resolveNodeNamespaceBySchema(payload, identifierWithSchemaNode.schemaNode)
        val status = broker.commitOperationalDataPut(identifierWithSchemaNode.instanceIdentifier,value).get();
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(Response.Status.OK).build
            default: Response.status(Response.Status.INTERNAL_SERVER_ERROR).build
        }
    }
    
    override updateOperationalData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val value = resolveNodeNamespaceBySchema(payload, identifierWithSchemaNode.schemaNode)
        val status = broker.commitOperationalDataPut(identifierWithSchemaNode.instanceIdentifier,value).get();
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(Response.Status.NO_CONTENT).build
            default: Response.status(Response.Status.INTERNAL_SERVER_ERROR).build
        }
    }
    
    private def InstanceIdWithSchemaNode resolveInstanceIdentifier(String identifier) {
        val identifierWithSchemaNode = identifier.toInstanceIdentifier
        if (identifierWithSchemaNode === null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("URI has bad format")
                    .build());
        }
        return identifierWithSchemaNode
    }
    
    private def CompositeNode resolveNodeNamespaceBySchema(CompositeNode node, DataSchemaNode schema) {
        if (node instanceof CompositeNodeWrapper) {
            addNamespaceToNodeFromSchemaRecursively(node as CompositeNodeWrapper, schema)
            return (node as CompositeNodeWrapper).unwrap(null)
        }
        return node
    }

    private def void addNamespaceToNodeFromSchemaRecursively(NodeWrapper<?> nodeBuilder, DataSchemaNode schema) {
        if (nodeBuilder.namespace === null) {
            nodeBuilder.namespace = schema.QName.namespace
        }
        if (nodeBuilder instanceof CompositeNodeWrapper) {
            val List<NodeWrapper<?>> children = (nodeBuilder as CompositeNodeWrapper).getValues
            for (child : children) {
                addNamespaceToNodeFromSchemaRecursively(child,
                    (schema as DataNodeContainer).childNodes.findFirst[n|n.QName.localName.equals(child.localName)])
            }
        }
    }

}
