package org.opendaylight.controller.sal.restconf.impl

import java.util.ArrayList
import java.util.List
import java.util.Set
import javax.ws.rs.core.Response
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import org.opendaylight.controller.sal.rest.api.RestconfService
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.impl.NodeFactory
import org.opendaylight.yangtools.yang.model.api.ChoiceNode
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec
import org.opendaylight.yangtools.yang.model.api.TypeDefinition
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode

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
        val value = normalizeNode(payload, identifierWithSchemaNode.schemaNode)
        val status = broker.commitConfigurationDataPut(identifierWithSchemaNode.instanceIdentifier,value).get();
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(Response.Status.OK).build
            default: Response.status(Response.Status.INTERNAL_SERVER_ERROR).build
        }
    }

    override updateConfigurationData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val value = normalizeNode(payload, identifierWithSchemaNode.schemaNode)
        val status = broker.commitConfigurationDataPut(identifierWithSchemaNode.instanceIdentifier,value).get();
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(Response.Status.NO_CONTENT).build
            default: Response.status(Response.Status.INTERNAL_SERVER_ERROR).build
        }
    }

    override invokeRpc(String identifier, CompositeNode payload) {
        val rpc = identifier.rpcDefinition
        if (rpc === null) {
            throw new ResponseException(Response.Status.NOT_FOUND, "RPC does not exist.");
        }
        val value = normalizeNode(payload, rpc.input)
        val List<Node<?>> input = new ArrayList
        input.add(value)
        val rpcRequest = NodeFactory.createMutableCompositeNode(rpc.QName, null, input, null, null)
        val rpcResult = broker.invokeRpc(rpc.QName, rpcRequest);
        return new StructuredData(rpcResult.result, rpc.output);
    }
    
    override readConfigurationData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val data = broker.readConfigurationData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
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
    
    private def InstanceIdWithSchemaNode resolveInstanceIdentifier(String identifier) {
        val identifierWithSchemaNode = identifier.toInstanceIdentifier
        if (identifierWithSchemaNode === null) {
            throw new ResponseException(Response.Status.BAD_REQUEST, "URI has bad format");
        }
        return identifierWithSchemaNode
    }
    
    private def CompositeNode normalizeNode(CompositeNode node, DataSchemaNode schema) {
        if (node instanceof CompositeNodeWrapper) {
            normalizeNode(node as CompositeNodeWrapper, schema,null)
            return (node as CompositeNodeWrapper).unwrap()
        }
        return node
    }

    private def void normalizeNode(NodeWrapper<?> nodeBuilder, DataSchemaNode schema,QName previousAugment) {
        if (schema === null) {
            throw new ResponseException(Response.Status.BAD_REQUEST,
                "Data has bad format\n" + nodeBuilder.localName + " does not exist in yang schema.");
        }
        var validQName = schema.QName
        var currentAugment = previousAugment;
        if(schema.augmenting) {
            currentAugment = schema.QName
        } else if(previousAugment !== null && schema.QName.namespace !== previousAugment.namespace) {
            validQName = QName.create(currentAugment,schema.QName.localName);
        }
        val moduleName = controllerContext.findModuleByNamespace(validQName.namespace);
        if (nodeBuilder.namespace === null || nodeBuilder.namespace == validQName.namespace ||
            nodeBuilder.namespace.path == moduleName) {
            nodeBuilder.qname = validQName
        } else {
            throw new ResponseException(Response.Status.BAD_REQUEST,
                "Data has bad format\nIf data is in XML format then namespace for " + nodeBuilder.localName +
                    " should be " + schema.QName.namespace + ".\n If data is in Json format then module name for " +
                    nodeBuilder.localName + " should be " + moduleName + ".");
        }
        
        if (nodeBuilder instanceof CompositeNodeWrapper) {
            val List<NodeWrapper<?>> children = (nodeBuilder as CompositeNodeWrapper).getValues
            for (child : children) {
                normalizeNode(child,
                    findFirstSchemaByLocalName(child.localName, (schema as DataNodeContainer).childNodes),currentAugment)
            }
        } else if (nodeBuilder instanceof SimpleNodeWrapper) {
            val simpleNode = (nodeBuilder as SimpleNodeWrapper)
            val stringValue = simpleNode.value as String;
            
            val objectValue = TypeDefinitionAwareCodec.from(schema.typeDefinition)?.deserialize(stringValue);
            simpleNode.setValue(objectValue)
        } else if (nodeBuilder instanceof EmptyNodeWrapper) {
            val emptyNodeBuilder = nodeBuilder as EmptyNodeWrapper
            if(schema instanceof LeafSchemaNode) {
                emptyNodeBuilder.setComposite(false);
            } else if(schema instanceof ContainerSchemaNode) {
                // FIXME: Add presence check
                emptyNodeBuilder.setComposite(true);
            }
        }
    }
    
    private def dispatch TypeDefinition<?> typeDefinition(LeafSchemaNode node) {
        node.type
    }
    
    private def dispatch TypeDefinition<?> typeDefinition(LeafListSchemaNode node) {
        node.type
    }
    
    
    private def DataSchemaNode findFirstSchemaByLocalName(String localName, Set<DataSchemaNode> schemas) {
        for (schema : schemas) {
            if (schema instanceof ChoiceNode) {
                for (caze : (schema as ChoiceNode).cases) {
                    val result =  findFirstSchemaByLocalName(localName, caze.childNodes)
                    if (result !== null) {
                        return result
                    }
                }
            } else {
                val result = schemas.findFirst[n|n.QName.localName.equals(localName)]
                if(result !== null) {
                    return result;
                
                }
            }
        }
        return null
    }

}
