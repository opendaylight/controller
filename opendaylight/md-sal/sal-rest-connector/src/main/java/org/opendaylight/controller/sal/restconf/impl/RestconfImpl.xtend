package org.opendaylight.controller.sal.restconf.impl

import com.google.common.base.Preconditions
import java.net.URI
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Set
import javax.ws.rs.core.Response
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import org.opendaylight.controller.sal.core.api.mount.MountInstance
import org.opendaylight.controller.sal.rest.api.RestconfService
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.RpcResult
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
import org.opendaylight.yangtools.yang.model.api.RpcDefinition
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
        return callRpc(identifier.rpcDefinition, payload)
    }
    
    override invokeRpc(String identifier) {
        return callRpc(identifier.rpcDefinition, null)
    }
    
    private def StructuredData callRpc(RpcDefinition rpc, CompositeNode payload) {
        if (rpc === null) {
            throw new ResponseException(NOT_FOUND, "RPC does not exist.");
        }
        var CompositeNode rpcRequest;
        if (payload === null) {
            rpcRequest = NodeFactory.createMutableCompositeNode(rpc.QName, null, null, null, null)
        } else {
            val value = normalizeNode(payload, rpc.input, null)
            val List<Node<?>> input = new ArrayList
            input.add(value)
            rpcRequest = NodeFactory.createMutableCompositeNode(rpc.QName, null, input, null, null)
        }
        val rpcResult = broker.invokeRpc(rpc.QName, rpcRequest);
        if (!rpcResult.successful) {
            throw new ResponseException(INTERNAL_SERVER_ERROR, "Operation failed")
        }
        if (rpcResult.result === null) {
            return null
        }
        return new StructuredData(rpcResult.result, rpc.output, null)
    }

    override readData(String identifier) {
        val iiWithData = identifier.toInstanceIdentifier
        var CompositeNode data = null;
        if (iiWithData.mountPoint !== null) {
            data = broker.readOperationalDataBehindMountPoint(iiWithData.mountPoint, iiWithData.instanceIdentifier)
        } else {
            data = broker.readOperationalData(iiWithData.getInstanceIdentifier);
        }
        return new StructuredData(data, iiWithData.schemaNode, iiWithData.mountPoint)
    }

    override readConfigurationData(String identifier) {
        val iiWithData = identifier.toInstanceIdentifier
        var CompositeNode data = null;
        if (iiWithData.mountPoint !== null) {
            data = broker.readConfigurationDataBehindMountPoint(iiWithData.mountPoint, iiWithData.getInstanceIdentifier)
        } else {
            data = broker.readConfigurationData(iiWithData.getInstanceIdentifier);
        }
        return new StructuredData(data, iiWithData.schemaNode, iiWithData.mountPoint)
    }

    override readOperationalData(String identifier) {
        val iiWithData = identifier.toInstanceIdentifier
        var CompositeNode data = null;
        if (iiWithData.mountPoint !== null) {
            data = broker.readOperationalDataBehindMountPoint(iiWithData.mountPoint, iiWithData.getInstanceIdentifier)
        } else {
            data = broker.readOperationalData(iiWithData.getInstanceIdentifier);
        }
        return new StructuredData(data, iiWithData.schemaNode, iiWithData.mountPoint)
    }

    override updateConfigurationDataLegacy(String identifier, CompositeNode payload) {
        updateConfigurationData(identifier, payload);
    }

    override updateConfigurationData(String identifier, CompositeNode payload) {
        val iiWithData = identifier.toInstanceIdentifier
        val value = normalizeNode(payload, iiWithData.schemaNode, iiWithData.mountPoint)
        var RpcResult<TransactionStatus> status = null
        if (iiWithData.mountPoint !== null) {
            status = broker.commitConfigurationDataPutBehindMountPoint(iiWithData.mountPoint,
                iiWithData.instanceIdentifier, value).get()
        } else {
            status = broker.commitConfigurationDataPut(iiWithData.instanceIdentifier, value).get();
        }
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(OK).build
            default: Response.status(INTERNAL_SERVER_ERROR).build
        }
    }

    override createConfigurationDataLegacy(String identifier, CompositeNode payload) {
        createConfigurationData(identifier, payload);
    }

    override createConfigurationData(String identifier, CompositeNode payload) {
        if (payload.namespace === null) {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)");
        }
        val uncompleteInstIdWithData = identifier.toInstanceIdentifier
        val schemaNode = uncompleteInstIdWithData.mountPoint.findModule(payload)?.getSchemaChildNode(payload)
        val value = normalizeNode(payload, schemaNode, uncompleteInstIdWithData.mountPoint)
        val completeInstIdWithData = uncompleteInstIdWithData.addLastIdentifierFromData(value, schemaNode)
        var RpcResult<TransactionStatus> status = null
        if (completeInstIdWithData.mountPoint !== null) {
            status = broker.commitConfigurationDataPostBehindMountPoint(completeInstIdWithData.mountPoint,
                completeInstIdWithData.instanceIdentifier, value)?.get();
        } else {
            status = broker.commitConfigurationDataPost(completeInstIdWithData.instanceIdentifier, value)?.get();
        }
        if (status === null) {
            return Response.status(ACCEPTED).build
        }
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(NO_CONTENT).build
            default: Response.status(INTERNAL_SERVER_ERROR).build
        }
    }
    
    override createConfigurationData(CompositeNode payload) {
        if (payload.namespace === null) {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)");
        }
        val schemaNode = findModule(null, payload)?.getSchemaChildNode(payload)
        val value = normalizeNode(payload, schemaNode, null)
        val iiWithData = addLastIdentifierFromData(null, value, schemaNode)
        var RpcResult<TransactionStatus> status = null
        if (iiWithData.mountPoint !== null) {
            status = broker.commitConfigurationDataPostBehindMountPoint(iiWithData.mountPoint,
                iiWithData.instanceIdentifier, value)?.get();
        } else {
            status = broker.commitConfigurationDataPost(iiWithData.instanceIdentifier, value)?.get();
        }
        if (status === null) {
            return Response.status(ACCEPTED).build
        }
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(NO_CONTENT).build
            default: Response.status(INTERNAL_SERVER_ERROR).build
        }
    }
    
    override deleteConfigurationData(String identifier) {
        val iiWithData = identifier.toInstanceIdentifier
        var RpcResult<TransactionStatus> status = null
        if (iiWithData.mountPoint !== null) {
            status = broker.commitConfigurationDataDeleteBehindMountPoint(iiWithData.mountPoint,
                iiWithData.getInstanceIdentifier).get;
        } else {
            status = broker.commitConfigurationDataDelete(iiWithData.getInstanceIdentifier).get;
        }
        switch status.result {
            case TransactionStatus.COMMITED: Response.status(OK).build
            default: Response.status(INTERNAL_SERVER_ERROR).build
        }
    }
    
    private def dispatch URI namespace(CompositeNode data) {
        return data.nodeType.namespace
    }
    
    private def dispatch URI namespace(CompositeNodeWrapper data) {
        return data.namespace
    }

    private def dispatch Module findModule(MountInstance mountPoint, CompositeNode data) {
        if (mountPoint !== null) {
            return mountPoint.findModuleByNamespace(data.nodeType.namespace)
        } else {
            return findModuleByNamespace(data.nodeType.namespace)
        }
    }

    private def dispatch Module findModule(MountInstance mountPoint, CompositeNodeWrapper data) {
        Preconditions.checkNotNull(data.namespace)
        var Module module = null;
        if (mountPoint !== null) {
            module = mountPoint.findModuleByNamespace(data.namespace) // namespace from XML
            if (module === null) {
                module = mountPoint.findModuleByName(data.namespace.toString) // namespace (module name) from JSON
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

    private def InstanceIdWithSchemaNode addLastIdentifierFromData(InstanceIdWithSchemaNode identifierWithSchemaNode,
        CompositeNode data, DataSchemaNode schemaOfData) {
        val iiOriginal = identifierWithSchemaNode?.instanceIdentifier
        var InstanceIdentifierBuilder iiBuilder = null
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
                throw new ResponseException(BAD_REQUEST,
                    "Data contains list \"" + dataNode.nodeType.localName + "\" which does not contain key: \"" +
                        key.localName + "\"")
            }
            keyValues.put(key, dataNodeKeyValueObject);
        }
        return keyValues
    }

    private def CompositeNode normalizeNode(CompositeNode node, DataSchemaNode schema, MountInstance mountPoint) {
        if (schema === null) {
            throw new ResponseException(INTERNAL_SERVER_ERROR, "Data schema node was not found for " + node?.nodeType?.localName)
        }
        if (!(schema instanceof DataNodeContainer)) {
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

    private def void normalizeNode(NodeWrapper<?> nodeBuilder, DataSchemaNode schema, QName previousAugment,
        MountInstance mountPoint) {
        if (schema === null) {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format.\n\"" + nodeBuilder.localName + "\" does not exist in yang schema.");
        }
        var validQName = schema.QName
        var currentAugment = previousAugment;
        if (schema.augmenting) {
            currentAugment = schema.QName
        } else if (previousAugment !== null && schema.QName.namespace !== previousAugment.namespace) {
            validQName = QName.create(currentAugment, schema.QName.localName);
        }
        var String moduleName = null;
        if (mountPoint === null) {
            moduleName = controllerContext.findModuleNameByNamespace(validQName.namespace);
        } else {
            moduleName = controllerContext.findModuleNameByNamespace(mountPoint, validQName.namespace)
        }
        if (nodeBuilder.namespace === null || nodeBuilder.namespace == validQName.namespace ||
            nodeBuilder.namespace.toString == moduleName) {
            nodeBuilder.qname = validQName
        } else {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format.\nIf data is in XML format then namespace for \"" + nodeBuilder.localName +
                    "\" should be \"" + schema.QName.namespace + "\".\nIf data is in Json format then module name for \"" +
                    nodeBuilder.localName + "\" should be \"" + moduleName + "\".");
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
                            "Missing key in URI \"" + listKey.localName + "\" of list \"" + schema.QName.localName + "\"")
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
            
            val outputValue = RestCodec.from(schema.typeDefinition, mountPoint)?.deserialize(inputValue);
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
