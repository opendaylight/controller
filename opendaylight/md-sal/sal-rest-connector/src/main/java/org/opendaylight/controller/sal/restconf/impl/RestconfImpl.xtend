package org.opendaylight.controller.sal.restconf.impl

import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Set
import javax.ws.rs.core.Response
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import org.opendaylight.controller.sal.rest.api.RestconfService
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.impl.NodeFactory
import org.opendaylight.yangtools.yang.model.api.ChoiceNode
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.model.api.Module
import org.opendaylight.yangtools.yang.model.api.TypeDefinition
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition

import static javax.ws.rs.core.Response.Status.*

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

    override invokeRpc(String identifier, CompositeNode payload) {
        val rpc = identifier.rpcDefinition
        if (rpc === null) {
            throw new ResponseException(NOT_FOUND, "RPC does not exist.");
        }
        val value = normalizeNode(payload, rpc.input, null)
        val List<Node<?>> input = new ArrayList
        input.add(value)
        val rpcRequest = NodeFactory.createMutableCompositeNode(rpc.QName, null, input, null, null)
        val rpcResult = broker.invokeRpc(rpc.QName, rpcRequest);
        if (!rpcResult.successful) {
            throw new ResponseException(INTERNAL_SERVER_ERROR, "Operation failed")
        }
        if (rpcResult.result === null) {
            return null
        }
        return new StructuredData(rpcResult.result, rpc.output)
    }

    override readData(String identifier) {
        val instanceIdentifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val data = broker.readOperationalData(instanceIdentifierWithSchemaNode.getInstanceIdentifier);
        return new StructuredData(data, instanceIdentifierWithSchemaNode.schemaNode)
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
        updateConfigurationData(identifier, payload);
    }

    override updateConfigurationData(String identifier, CompositeNode payload) {
        val identifierWithSchemaNode = identifier.resolveInstanceIdentifier
        val value = normalizeNode(payload, identifierWithSchemaNode.schemaNode, identifierWithSchemaNode.mountPoint)
        val status = broker.commitConfigurationDataPut(identifierWithSchemaNode.instanceIdentifier, value).get();
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(OK).build
            default: Response.status(INTERNAL_SERVER_ERROR).build
        }
    }

    override createConfigurationDataLegacy(String identifier, CompositeNode payload) {
        createConfigurationData(identifier, payload);
    }

    override createConfigurationData(String identifier, CompositeNode payload) {
        val uncompleteIdentifierWithSchemaNode = identifier.resolveInstanceIdentifier
        var schemaNode = (uncompleteIdentifierWithSchemaNode.schemaNode as DataNodeContainer).getSchemaChildNode(payload)
        if (schemaNode === null) {
            schemaNode = payload.findModule(uncompleteIdentifierWithSchemaNode.instanceIdentifier)?.getSchemaChildNode(payload)
        }
        val value = normalizeNode(payload, schemaNode, uncompleteIdentifierWithSchemaNode.instanceIdentifier)
        val completeIdentifierWithSchemaNode = uncompleteIdentifierWithSchemaNode.addLastIdentifierFromData(value, schemaNode)
        val status = broker.commitConfigurationDataPost(completeIdentifierWithSchemaNode.instanceIdentifier, value)?.get();
        if (status === null) {
            return Response.status(ACCEPTED).build
        }
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(NO_CONTENT).build
            default: Response.status(INTERNAL_SERVER_ERROR).build
        }
    }
    
    override createConfigurationData(CompositeNode payload) {
        val schemaNode = payload.findModule(null)?.getSchemaChildNode(payload)
        val value = normalizeNode(payload, schemaNode, null)
        val identifierWithSchemaNode = addLastIdentifierFromData(null, value, schemaNode)
        val status = broker.commitConfigurationDataPost(identifierWithSchemaNode.instanceIdentifier, value)?.get();
        if (status === null) {
            return Response.status(ACCEPTED).build
        }
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(NO_CONTENT).build
            default: Response.status(INTERNAL_SERVER_ERROR).build
        }
    }

    private def InstanceIdWithSchemaNode resolveInstanceIdentifier(String identifier) {
        val identifierWithSchemaNode = identifier.toInstanceIdentifier
        if (identifierWithSchemaNode === null) {
            throw new ResponseException(BAD_REQUEST, "URI has bad format");
        }
        return identifierWithSchemaNode
    }

    private def dispatch Module findModule(CompositeNode data, InstanceIdentifier partialPath) {
        if (partialPath !== null && !partialPath.path.empty) {
            return data.nodeType.namespace.findModuleByNamespace(partialPath)
        } else {
            return data.nodeType.namespace.findModuleByNamespace
        }
    }

    private def dispatch Module findModule(CompositeNodeWrapper data, InstanceIdentifier partialPath) {
        var Module module = null;
        if (partialPath !== null && !partialPath.path.empty) {
            module = data.namespace.findModuleByNamespace(partialPath) // namespace from XML
            if (module === null) {
                module = data.namespace.toString.findModuleByName(partialPath) // namespace (module name) from JSON
            }
        } else {
            module = data.namespace.findModuleByNamespace // namespace from XML
            if (module === null) {
                module = data.namespace.toString.findModuleByName // namespace (module name) from JSON
            }
        }
        return module
    }
    
    private def dispatch DataSchemaNode getSchemaChildNode(DataNodeContainer parentSchemaNode, CompositeNode data) {
        return parentSchemaNode?.getDataChildByName(data.nodeType.localName)
    }
    
    private def dispatch DataSchemaNode getSchemaChildNode(DataNodeContainer parentSchemaNode, CompositeNodeWrapper data) {
        return parentSchemaNode?.getDataChildByName(data.localName)
    }

    private def InstanceIdWithSchemaNode addLastIdentifierFromData(InstanceIdWithSchemaNode identifierWithSchemaNode, CompositeNode data, DataSchemaNode schemaOfData) {
        val iiOriginal = identifierWithSchemaNode?.instanceIdentifier
        var  InstanceIdentifierBuilder iiBuilder = null
        if (iiOriginal === null) { 
            iiBuilder = InstanceIdentifier.builder
        } else {
            iiBuilder = InstanceIdentifier.builder(iiOriginal)
        }

        if (schemaOfData instanceof ListSchemaNode) {
            iiBuilder.nodeWithKey(schemaOfData.QName, (schemaOfData as ListSchemaNode).resolveKeysFromData(data))
        } else {
            iiBuilder.node(schemaOfData.QName)
        }
        return new InstanceIdWithSchemaNode(iiBuilder.toInstance, schemaOfData, identifierWithSchemaNode?.mountPoint)
    }

    private def resolveKeysFromData(ListSchemaNode listNode, CompositeNode dataNode) {
        val keyValues = new HashMap<QName, Object>();
        for (key : listNode.keyDefinition) {
            val dataNodeKeyValueObject = dataNode.getSimpleNodesByName(key.localName)?.head?.value
            if (dataNodeKeyValueObject === null) {
                throw new ResponseException(BAD_REQUEST, "List " + dataNode.nodeType.localName + " does not contain key: " + key.localName)
            }
            keyValues.put(key, dataNodeKeyValueObject);
        }
        return keyValues
    }

    private def CompositeNode normalizeNode(CompositeNode node, DataSchemaNode schema, InstanceIdentifier mountPoint) {
        if (schema !== null && !schema.containerOrList) {
            throw new ResponseException(BAD_REQUEST, "Root element has to be container or list yang datatype.");
        }
        if (node instanceof CompositeNodeWrapper) {
            if ((node  as CompositeNodeWrapper).changeAllowed) {
                normalizeNode(node as CompositeNodeWrapper, schema, null, mountPoint)
            }
            return (node as CompositeNodeWrapper).unwrap()
        }
        return node
    }

    private def isContainerOrList(DataSchemaNode schemaNode) {
        return (schemaNode instanceof ContainerSchemaNode) || (schemaNode instanceof ListSchemaNode)
    }

    private def void normalizeNode(NodeWrapper<?> nodeBuilder, DataSchemaNode schema, QName previousAugment,
        InstanceIdentifier mountPoint) {
        if (schema === null) {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format\n" + nodeBuilder.localName + " does not exist in yang schema.");
        }
        var validQName = schema.QName
        var currentAugment = previousAugment;
        if (schema.augmenting) {
            currentAugment = schema.QName
        } else if (previousAugment !== null && schema.QName.namespace !== previousAugment.namespace) {
            validQName = QName.create(currentAugment, schema.QName.localName);
        }
        var moduleName = controllerContext.findModuleNameByNamespace(validQName.namespace);
        if (moduleName === null && mountPoint !== null && !mountPoint.path.empty) {
            moduleName = controllerContext.findModuleByNamespace(validQName.namespace, mountPoint)?.name
        }
        if (nodeBuilder.namespace === null || nodeBuilder.namespace == validQName.namespace ||
            nodeBuilder.namespace.toString == moduleName) {
            nodeBuilder.qname = validQName
        } else {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format.\nIf data is in XML format then namespace for " + nodeBuilder.localName +
                    " should be " + schema.QName.namespace + ".\nIf data is in Json format then module name for " +
                    nodeBuilder.localName + " should be " + moduleName + ".");
        }

        if (nodeBuilder instanceof CompositeNodeWrapper) {
            val List<NodeWrapper<?>> children = (nodeBuilder as CompositeNodeWrapper).getValues
            for (child : children) {
                normalizeNode(child,
                    findFirstSchemaByLocalName(child.localName, (schema as DataNodeContainer).childNodes),
                    currentAugment, mountPoint)
            }
            if(schema instanceof ListSchemaNode) {
                val listKeys = (schema as ListSchemaNode).keyDefinition
                for (listKey : listKeys) {
                    var foundKey = false
                    for (child : children) {
                        if (child.unwrap.nodeType.localName == listKey.localName) {
                            foundKey = true;
                        }
                    }
                    if (!foundKey) {
                        throw new ResponseException(BAD_REQUEST,
                            "Missing key \"" + listKey.localName + "\" of list \"" + schema.QName.localName + "\"")
                    }
                }
            }
        } else if (nodeBuilder instanceof SimpleNodeWrapper) {
            val simpleNode = (nodeBuilder as SimpleNodeWrapper)
            val value = simpleNode.value
            var inputValue = value;
            
            if (schema.typeDefinition instanceof IdentityrefTypeDefinition) {
                if (value instanceof String) {
                    inputValue = new IdentityValuesDTO(validQName.namespace.toString, value as String, null)
                } // else value is instance of ValuesDTO
            }
            
            val outputValue = RestCodec.from(schema.typeDefinition)?.deserialize(inputValue);
            simpleNode.setValue(outputValue)
        } else if (nodeBuilder instanceof EmptyNodeWrapper) {
            val emptyNodeBuilder = nodeBuilder as EmptyNodeWrapper
            if (schema instanceof LeafSchemaNode) {
                emptyNodeBuilder.setComposite(false);
            } else if (schema instanceof ContainerSchemaNode) {

                // FIXME: Add presence check
                emptyNodeBuilder.setComposite(true);
            }
        }
    }

    private def dispatch TypeDefinition<?> typeDefinition(LeafSchemaNode node) {
        var baseType = node.type
        while (baseType.baseType !== null) {
            baseType = baseType.baseType;
        }
        baseType
    }

    private def dispatch TypeDefinition<?> typeDefinition(LeafListSchemaNode node) {
        var TypeDefinition<?> baseType = node.type
        while (baseType.baseType !== null) {
            baseType = baseType.baseType;
        }
        baseType
    }

    private def DataSchemaNode findFirstSchemaByLocalName(String localName, Set<DataSchemaNode> schemas) {
        for (schema : schemas) {
            if (schema instanceof ChoiceNode) {
                for (caze : (schema as ChoiceNode).cases) {
                    val result = findFirstSchemaByLocalName(localName, caze.childNodes)
                    if (result !== null) {
                        return result
                    }
                }
            } else {
                val result = schemas.findFirst[n|n.QName.localName.equals(localName)]
                if (result !== null) {
                    return result;

                }
            }
        }
        return null
    }

}
