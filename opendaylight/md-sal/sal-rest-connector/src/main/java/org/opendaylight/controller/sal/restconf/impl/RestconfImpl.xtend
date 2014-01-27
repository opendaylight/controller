package org.opendaylight.controller.sal.restconf.impl

import com.google.common.base.Preconditions
import java.net.URI
import java.text.ParseException
import java.text.SimpleDateFormat
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
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.model.api.Module
import org.opendaylight.yangtools.yang.model.api.RpcDefinition
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.yang.model.api.TypeDefinition
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition

import static javax.ws.rs.core.Response.Status.*

class RestconfImpl implements RestconfService {

    val static RestconfImpl INSTANCE = new RestconfImpl
    val static MOUNT_POINT_MODULE_NAME = "ietf-netconf"
    val static REVISION_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
    val static RESTCONF_MODULE_DRAFT02_REVISION = "2013-10-19"
    val static RESTCONF_MODULE_DRAFT02_NAME = "ietf-restconf"
    val static RESTCONF_MODULE_DRAFT02_RESTCONF_SCHEMA_NODE = "restconf"
    val static RESTCONF_MODULE_DRAFT02_MODULES_SCHEMA_NODE = "modules"
    val static RESTCONF_MODULE_DRAFT02_MODULE_SCHEMA_NODE = "module"
    val static RESTCONF_MODULE_DRAFT02_OPERATIONS_SCHEMA_NODE = "operations"

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

    override getModules() {
        val modules = allModules
        val restconfModule = findModuleByNameAndRevision(
            QName.create(null, RESTCONF_MODULE_DRAFT02_REVISION, RESTCONF_MODULE_DRAFT02_NAME))
        val List<Node<?>> modulesAsData = new ArrayList
        val moduleSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULE_SCHEMA_NODE)
        for (module : modules) {
            modulesAsData.add(module.toModuleCompositeNode(moduleSchemaNode))
        }
        val modulesSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULES_SCHEMA_NODE)
        val modulesNode = NodeFactory.createImmutableCompositeNode(modulesSchemaNode.QName, null, modulesAsData)
        return new StructuredData(modulesNode, modulesSchemaNode, null)
    }

    override getModules(String identifier) {
        var Set<Module> modules = null
        var MountInstance mountPoint = null
        if (identifier.contains(ControllerContext.MOUNT)) {
            mountPoint = identifier.toMountPointIdentifier.mountPoint
            modules = mountPoint.allModules
        } else {
            throw new ResponseException(BAD_REQUEST, "URI has bad format. If modules behind mount point should be showed, URI has to end with " + ControllerContext.MOUNT)
        }
        val restconfModule = findModuleByNameAndRevision(
            QName.create(null, RESTCONF_MODULE_DRAFT02_REVISION, RESTCONF_MODULE_DRAFT02_NAME))
        val List<Node<?>> modulesAsData = new ArrayList
        val moduleSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULE_SCHEMA_NODE)
        for (module : modules) {
            modulesAsData.add(module.toModuleCompositeNode(moduleSchemaNode))
        }
        val modulesSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULES_SCHEMA_NODE)
        val modulesNode = NodeFactory.createImmutableCompositeNode(modulesSchemaNode.QName, null, modulesAsData)
        return new StructuredData(modulesNode, modulesSchemaNode, mountPoint)
    }

    override getModule(String identifier) {
        val moduleNameAndRevision = identifier.moduleNameAndRevision
        var Module module = null
        var MountInstance mountPoint = null
        if (identifier.contains(ControllerContext.MOUNT)) {
            mountPoint = identifier.toMountPointIdentifier.mountPoint
            module = mountPoint.findModuleByNameAndRevision(moduleNameAndRevision)
        } else {
            module = findModuleByNameAndRevision(moduleNameAndRevision)
        }
        if (module === null) {
            throw new ResponseException(BAD_REQUEST,
                "Module with name '" + moduleNameAndRevision.localName + "' and revision '" +
                    moduleNameAndRevision.revision + "' was not found.")
        }
        val restconfModule = findModuleByNameAndRevision(
            QName.create(null, RESTCONF_MODULE_DRAFT02_REVISION, RESTCONF_MODULE_DRAFT02_NAME))
        val moduleSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULE_SCHEMA_NODE)
        val moduleNode = module.toModuleCompositeNode(moduleSchemaNode)
        return new StructuredData(moduleNode, moduleSchemaNode, mountPoint)
    }

    override getOperations() {
        val modules = allModules
        val restconfModule = findModuleByNameAndRevision(
            QName.create(null, RESTCONF_MODULE_DRAFT02_REVISION, RESTCONF_MODULE_DRAFT02_NAME))
        val List<Node<?>> operationsAsData = new ArrayList
        for (module : modules) {
            for (rpc : module.rpcs) {
                operationsAsData.add(NodeFactory.createImmutableSimpleNode(rpc.QName, null, null))
            }
        }
        val operationsSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_OPERATIONS_SCHEMA_NODE)
        val operationsNode = NodeFactory.createImmutableCompositeNode(operationsSchemaNode.QName, null, operationsAsData)
        return new StructuredData(operationsNode, operationsSchemaNode, null)
    }
    
    override getOperations(String identifier) {
        var Set<Module> modules = null
        var MountInstance mountPoint = null
        if (identifier.contains(ControllerContext.MOUNT)) {
            mountPoint = identifier.toMountPointIdentifier.mountPoint
            modules = mountPoint.allModules
        } else {
            throw new ResponseException(BAD_REQUEST, "URI has bad format. If operations behind mount point should be showed, URI has to end with " + ControllerContext.MOUNT)
        }
        val restconfModule = findModuleByNameAndRevision(
            QName.create(null, RESTCONF_MODULE_DRAFT02_REVISION, RESTCONF_MODULE_DRAFT02_NAME))
        val List<Node<?>> operationsAsData = new ArrayList
        for (module : modules) {
            for (rpc : module.rpcs) {
                operationsAsData.add(NodeFactory.createImmutableSimpleNode(rpc.QName, null, null))
            }
        }
        val operationsSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_OPERATIONS_SCHEMA_NODE)
        val operationsNode = NodeFactory.createImmutableCompositeNode(operationsSchemaNode.QName, null, operationsAsData)
        return new StructuredData(operationsNode, operationsSchemaNode, mountPoint)
    }

    private def QName getModuleNameAndRevision(String identifier) {
        val indexOfMountPointFirstLetter = identifier.indexOf(ControllerContext.MOUNT)
        var moduleNameAndRevision = "";
        if (indexOfMountPointFirstLetter !== -1) { // module and revision is behind mount point string
            moduleNameAndRevision = identifier.substring(indexOfMountPointFirstLetter + ControllerContext.MOUNT.length)
        } else (
            moduleNameAndRevision = identifier
        )
        val pathArgs = moduleNameAndRevision.split("/")
        if (pathArgs.empty) {
            return null;
        }
        if (pathArgs.head.empty) { // remove first '/' if it is there
            pathArgs.remove(0)
        }
        if (pathArgs.length < 2) {
            throw new ResponseException(BAD_REQUEST, "URI has bad format. It should be 'moduleName/yyyy-MM-dd'")
        }
        try {
            val moduleName = pathArgs.head
            val moduleRevision = REVISION_FORMAT.parse(pathArgs.get(1))
            return QName.create(null, moduleRevision, moduleName)
        } catch(ParseException e) {
            throw new ResponseException(BAD_REQUEST, "URI has bad format. It should be 'moduleName/yyyy-MM-dd'")
        }
    }

    private def CompositeNode toModuleCompositeNode(Module module, DataSchemaNode moduleSchemaNode) {
        val List<Node<?>> moduleNodeValues = new ArrayList
        val nameSchemaNode = (moduleSchemaNode as DataNodeContainer).findInstanceDataChildrenByName("name").head
        moduleNodeValues.add(NodeFactory.createImmutableSimpleNode(nameSchemaNode.QName, null, module.name))
        val revisionSchemaNode = (moduleSchemaNode as DataNodeContainer).findInstanceDataChildrenByName("revision").head
        moduleNodeValues.add(NodeFactory.createImmutableSimpleNode(revisionSchemaNode.QName, null, REVISION_FORMAT.format(module.revision)))
        val namespaceSchemaNode = (moduleSchemaNode as DataNodeContainer).findInstanceDataChildrenByName("namespace").head
        moduleNodeValues.add(NodeFactory.createImmutableSimpleNode(namespaceSchemaNode.QName, null, module.namespace))
        val featureSchemaNode = (moduleSchemaNode as DataNodeContainer).findInstanceDataChildrenByName("feature").head
        for (feature : module.features) {
            moduleNodeValues.add(NodeFactory.createImmutableSimpleNode(featureSchemaNode.QName, null, feature.QName.localName))
        }
        return NodeFactory.createImmutableCompositeNode(moduleSchemaNode.QName, null, moduleNodeValues)
    }

    private def DataSchemaNode getSchemaNode(Module restconfModule, String schemaNodeName) {
        val restconf = restconfModule.groupings.filter[g|g.QName.localName == RESTCONF_MODULE_DRAFT02_RESTCONF_SCHEMA_NODE].head
        val operations = (restconf as DataNodeContainer).findInstanceDataChildrenByName(RESTCONF_MODULE_DRAFT02_OPERATIONS_SCHEMA_NODE).head
        if (schemaNodeName == RESTCONF_MODULE_DRAFT02_OPERATIONS_SCHEMA_NODE) return operations
        val modules = (restconf as DataNodeContainer).findInstanceDataChildrenByName(RESTCONF_MODULE_DRAFT02_MODULES_SCHEMA_NODE).head
        if (schemaNodeName == RESTCONF_MODULE_DRAFT02_MODULES_SCHEMA_NODE) return modules
        val module = (modules as DataNodeContainer).findInstanceDataChildrenByName(RESTCONF_MODULE_DRAFT02_MODULE_SCHEMA_NODE).head
        if (schemaNodeName == RESTCONF_MODULE_DRAFT02_MODULE_SCHEMA_NODE) return module
        return null
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

    override createConfigurationData(String identifier, CompositeNode payload) {
        if (payload.namespace === null) {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)");
        }
        var InstanceIdWithSchemaNode iiWithData;
        var CompositeNode value;
        if (payload.representsMountPointRootData) { // payload represents mount point data and URI represents path to the mount point
            if (identifier.endsWithMountPoint) {
                throw new ResponseException(BAD_REQUEST,
                    "URI has bad format. URI should be without \"" + ControllerContext.MOUNT + "\" for POST operation.");
            }
            val completIdentifier = identifier.addMountPointIdentifier
            iiWithData = completIdentifier.toInstanceIdentifier
            value = normalizeNode(payload, iiWithData.schemaNode, iiWithData.mountPoint)
        } else {
            val uncompleteInstIdWithData = identifier.toInstanceIdentifier
            val parentSchema = uncompleteInstIdWithData.schemaNode as DataNodeContainer
            val module = uncompleteInstIdWithData.mountPoint.findModule(payload)
            if (module === null) {
                throw new ResponseException(BAD_REQUEST, "Module was not found for \"" + payload.namespace + "\"")
            }
            val schemaNode = parentSchema.findInstanceDataChildByNameAndNamespace(payload.name, module.namespace)
            value = normalizeNode(payload, schemaNode, uncompleteInstIdWithData.mountPoint)
            iiWithData = uncompleteInstIdWithData.addLastIdentifierFromData(value, schemaNode)
        }
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

    override createConfigurationData(CompositeNode payload) {
        if (payload.namespace === null) {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)");
        }
        val module = findModule(null, payload)
        if (module === null) {
            throw new ResponseException(BAD_REQUEST,
                "Data has bad format. Root element node has incorrect namespace (XML format) or module name(JSON format)");
        }
        val schemaNode = module.findInstanceDataChildByNameAndNamespace(payload.name, module.namespace)
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

    private def dispatch String localName(CompositeNode data) {
        return data.nodeType.localName
    }

    private def dispatch String localName(CompositeNodeWrapper data) {
        return data.localName
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

    private def dispatch getName(CompositeNode data) {
        return data.nodeType.localName
    }

    private def dispatch getName(CompositeNodeWrapper data) {
        return data.localName
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

    private def endsWithMountPoint(String identifier) {
        return (identifier.endsWith(ControllerContext.MOUNT) || identifier.endsWith(ControllerContext.MOUNT + "/"))
    }

    private def representsMountPointRootData(CompositeNode data) {
        return ((data.namespace == SchemaContext.NAME.namespace || data.namespace == MOUNT_POINT_MODULE_NAME) &&
            data.localName == SchemaContext.NAME.localName)
    }

    private def addMountPointIdentifier(String identifier) {
        if (identifier.endsWith("/")) {
            return identifier + ControllerContext.MOUNT
        }
        return identifier + "/" + ControllerContext.MOUNT
    }

    private def CompositeNode normalizeNode(CompositeNode node, DataSchemaNode schema, MountInstance mountPoint) {
        if (schema === null) {
            throw new ResponseException(INTERNAL_SERVER_ERROR,
                "Data schema node was not found for " + node?.nodeType?.localName)
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

        var QName currentAugment;
        if (nodeBuilder.qname !== null) {
            currentAugment = previousAugment
        } else {
            currentAugment = normalizeNodeName(nodeBuilder, schema, previousAugment, mountPoint)
            if (nodeBuilder.qname === null) {
                throw new ResponseException(BAD_REQUEST,
                    "Data has bad format.\nIf data is in XML format then namespace for \"" + nodeBuilder.localName +
                        "\" should be \"" + schema.QName.namespace + "\".\n" +
                        "If data is in JSON format then module name for \"" + nodeBuilder.localName +
                         "\" should be corresponding to namespace \"" + schema.QName.namespace + "\".");
            }
        }

        if (nodeBuilder instanceof CompositeNodeWrapper) {
            val List<NodeWrapper<?>> children = (nodeBuilder as CompositeNodeWrapper).getValues
            for (child : children) {
                val potentialSchemaNodes = (schema as DataNodeContainer).findInstanceDataChildrenByName(child.localName)
                if (potentialSchemaNodes.size > 1 && child.namespace === null) {
                    val StringBuilder namespacesOfPotentialModules = new StringBuilder;
                    for (potentialSchemaNode : potentialSchemaNodes) {
                        namespacesOfPotentialModules.append("   ").append(potentialSchemaNode.QName.namespace.toString).append("\n")
                    }
                    throw new ResponseException(BAD_REQUEST,
                        "Node \"" + child.localName + "\" is added as augment from more than one module. " 
                        + "Therefore node must have namespace (XML format) or module name (JSON format)."
                        + "\nThe node is added as augment from modules with namespaces:\n" + namespacesOfPotentialModules)
                }
                var rightNodeSchemaFound = false
                for (potentialSchemaNode : potentialSchemaNodes) {
                    if (!rightNodeSchemaFound) {
                        val potentialCurrentAugment = normalizeNodeName(child, potentialSchemaNode, currentAugment,
                            mountPoint)
                        if (child.qname !== null) {
                            normalizeNode(child, potentialSchemaNode, potentialCurrentAugment, mountPoint)
                            rightNodeSchemaFound = true
                        }
                    }
                }
                if (!rightNodeSchemaFound) {
                    throw new ResponseException(BAD_REQUEST,
                        "Schema node \"" + child.localName + "\" was not found in module.")
                }
            }
            if (schema instanceof ListSchemaNode) {
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
                            "Missing key in URI \"" + listKey.localName + "\" of list \"" + schema.QName.localName +
                                "\"")
                    }
                }
            }
        } else if (nodeBuilder instanceof SimpleNodeWrapper) {
            val simpleNode = (nodeBuilder as SimpleNodeWrapper)
            val value = simpleNode.value
            var inputValue = value;

            if (schema.typeDefinition instanceof IdentityrefTypeDefinition) {
                if (value instanceof String) {
                    inputValue = new IdentityValuesDTO(nodeBuilder.namespace.toString, value as String, null)
                } // else value is already instance of IdentityValuesDTO
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
    
    private def QName normalizeNodeName(NodeWrapper<?> nodeBuilder, DataSchemaNode schema, QName previousAugment,
        MountInstance mountPoint) {
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
            nodeBuilder.namespace.toString == moduleName || nodeBuilder.namespace == MOUNT_POINT_MODULE_NAME) {
            nodeBuilder.qname = validQName
        }
        return currentAugment
    }

}
