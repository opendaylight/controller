/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl

import com.google.common.base.Preconditions
import com.google.common.base.Splitter
import com.google.common.collect.Lists
import java.net.URI
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Set
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import org.opendaylight.controller.sal.core.api.mount.MountInstance
import org.opendaylight.controller.sal.rest.api.RestconfService
import org.opendaylight.controller.sal.streams.listeners.Notificator
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer
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
import org.opendaylight.yangtools.yang.model.util.EmptyType
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafSchemaNodeBuilder

import static javax.ws.rs.core.Response.Status.*

class RestconfImpl implements RestconfService {

    val static RestconfImpl INSTANCE = new RestconfImpl
    val static MOUNT_POINT_MODULE_NAME = "ietf-netconf"
    val static REVISION_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
    val static RESTCONF_MODULE_DRAFT02_REVISION = "2013-10-19"
    val static RESTCONF_MODULE_DRAFT02_NAME = "ietf-restconf"
    val static RESTCONF_MODULE_DRAFT02_NAMESPACE = "urn:ietf:params:xml:ns:yang:ietf-restconf"
    val static RESTCONF_MODULE_DRAFT02_RESTCONF_GROUPING_SCHEMA_NODE = "restconf"
    val static RESTCONF_MODULE_DRAFT02_RESTCONF_CONTAINER_SCHEMA_NODE = "restconf"
    val static RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE = "modules"
    val static RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE = "module"
    val static RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE = "operations"
    val static SAL_REMOTE_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote"
    val static SAL_REMOTE_RPC_SUBSRCIBE = "create-data-change-event-subscription"

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
        val restconfModule = getRestconfModule()
        val List<Node<?>> modulesAsData = new ArrayList
        val moduleSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE)
        for (module : allModules) {
            modulesAsData.add(module.toModuleCompositeNode(moduleSchemaNode))
        }
        val modulesSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE)
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
        val List<Node<?>> modulesAsData = new ArrayList
        val moduleSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE)
        for (module : modules) {
            modulesAsData.add(module.toModuleCompositeNode(moduleSchemaNode))
        }
        val modulesSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE)
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
        val moduleSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE)
        val moduleNode = module.toModuleCompositeNode(moduleSchemaNode)
        return new StructuredData(moduleNode, moduleSchemaNode, mountPoint)
    }

    override getOperations() {
        return operationsFromModulesToStructuredData(allModules,null)
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
        return operationsFromModulesToStructuredData(modules,mountPoint)
    }
    
    private def StructuredData operationsFromModulesToStructuredData(Set<Module> modules,MountInstance mountPoint) {
        val List<Node<?>> operationsAsData = new ArrayList
        val operationsSchemaNode = restconfModule.getSchemaNode(RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE)        
        val fakeOperationsSchemaNode = new ContainerSchemaNodeBuilder(RESTCONF_MODULE_DRAFT02_NAME, 0, operationsSchemaNode.QName, operationsSchemaNode.path)
        for (module : modules) {
            for (rpc : module.rpcs) {
                operationsAsData.add(NodeFactory.createImmutableSimpleNode(rpc.QName, null, null))
                val fakeRpcSchemaNode = new LeafSchemaNodeBuilder(module.name, 0, rpc.QName, null)
                fakeRpcSchemaNode.setAugmenting(true)
                fakeRpcSchemaNode.setType(EmptyType.instance)
                fakeOperationsSchemaNode.addChildNode(fakeRpcSchemaNode.build)                
            }
        }
        val operationsNode = NodeFactory.createImmutableCompositeNode(operationsSchemaNode.QName, null, operationsAsData)
        return new StructuredData(operationsNode, fakeOperationsSchemaNode.build, mountPoint)        
    }

    private def Module getRestconfModule() {
        val restconfModule = findModuleByNameAndRevision(
            QName.create(RESTCONF_MODULE_DRAFT02_NAMESPACE, RESTCONF_MODULE_DRAFT02_REVISION,
                RESTCONF_MODULE_DRAFT02_NAME))
        if (restconfModule === null) {
            throw new ResponseException(INTERNAL_SERVER_ERROR, "Restconf module was not found.")
        }
        return restconfModule
    }

    private def QName getModuleNameAndRevision(String identifier) {
        val indexOfMountPointFirstLetter = identifier.indexOf(ControllerContext.MOUNT)
        var moduleNameAndRevision = "";
        if (indexOfMountPointFirstLetter !== -1) { // module and revision is behind mount point string
            moduleNameAndRevision = identifier.substring(indexOfMountPointFirstLetter + ControllerContext.MOUNT.length)
        } else (
            moduleNameAndRevision = identifier
        )
        val pathArgs = Lists.newArrayList(Splitter.on("/").omitEmptyStrings.split(moduleNameAndRevision))
        if (pathArgs.length < 2) {
            throw new ResponseException(BAD_REQUEST,
                "URI has bad format. End of URI should be in format 'moduleName/yyyy-MM-dd'")
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
        moduleNodeValues.add(NodeFactory.createImmutableSimpleNode(namespaceSchemaNode.QName, null, module.namespace.toString))
        val featureSchemaNode = (moduleSchemaNode as DataNodeContainer).findInstanceDataChildrenByName("feature").head
        for (feature : module.features) {
            moduleNodeValues.add(NodeFactory.createImmutableSimpleNode(featureSchemaNode.QName, null, feature.QName.localName))
        }
        return NodeFactory.createImmutableCompositeNode(moduleSchemaNode.QName, null, moduleNodeValues)
    }

    private def DataSchemaNode getSchemaNode(Module restconfModule, String schemaNodeName) {
        val restconfGrouping = restconfModule.groupings.filter[g|g.QName.localName == RESTCONF_MODULE_DRAFT02_RESTCONF_GROUPING_SCHEMA_NODE].head
        val restconfContainer = restconfGrouping.findInstanceDataChildrenByName(RESTCONF_MODULE_DRAFT02_RESTCONF_CONTAINER_SCHEMA_NODE).head
        if (schemaNodeName == RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE) {
            return (restconfContainer as DataNodeContainer).findInstanceDataChildrenByName(RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE).head
        } else if (schemaNodeName == RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE) {
            return (restconfContainer as DataNodeContainer).findInstanceDataChildrenByName(RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE).head
        } else if (schemaNodeName == RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE) {
            val modules = (restconfContainer as DataNodeContainer).findInstanceDataChildrenByName(RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE).head
            return (modules as DataNodeContainer).findInstanceDataChildrenByName(RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE).head
        }
        return null
    }

    override getRoot() {
        return null;
    }

    override invokeRpc(String identifier, CompositeNode payload) {
        val rpc = resolveIdentifierInInvokeRpc(identifier)
        if (rpc.QName.namespace.toString == SAL_REMOTE_NAMESPACE && rpc.QName.localName == SAL_REMOTE_RPC_SUBSRCIBE) {
            val value = normalizeNode(payload, rpc.input, null)
            val pathNode = value?.getFirstSimpleByName(QName.create(rpc.QName, "path"))
            val pathValue = pathNode?.value
            if (pathValue === null && !(pathValue instanceof InstanceIdentifier)) {
                throw new ResponseException(INTERNAL_SERVER_ERROR, "Instance identifier was not normalized correctly.");
            }
            val pathIdentifier = (pathValue as InstanceIdentifier)
            var String streamName = null
            if (!pathIdentifier.path.nullOrEmpty) {
                streamName = Notificator.createStreamNameFromUri(pathIdentifier.toFullRestconfIdentifier)
            }
            if (streamName.nullOrEmpty) {
                throw new ResponseException(BAD_REQUEST, "Path is empty or contains data node which is not Container or List build-in type.");
            }
            val streamNameNode = NodeFactory.createImmutableSimpleNode(QName.create(rpc.output.QName, "stream-name"), null, streamName)
            val List<Node<?>> output = new ArrayList
            output.add(streamNameNode)
            val responseData = NodeFactory.createMutableCompositeNode(rpc.output.QName, null, output, null, null)

            if (!Notificator.existListenerFor(pathIdentifier)) {
                Notificator.createListener(pathIdentifier, streamName)
            }

            return new StructuredData(responseData, rpc.output, null)
        }
        return callRpc(identifier.rpcDefinition, payload)
    }

    override invokeRpc(String identifier, String noPayload) {
        if (!noPayload.nullOrEmpty) {
            throw new ResponseException(UNSUPPORTED_MEDIA_TYPE, "Content-Type contains unsupported Media Type.");
        }
        val rpc = resolveIdentifierInInvokeRpc(identifier)
        return callRpc(rpc, null)
    }

    def resolveIdentifierInInvokeRpc(String identifier) {
        if (identifier.indexOf("/") === -1) {
            val identifierDecoded = identifier.urlPathArgDecode
            val rpc = identifierDecoded.rpcDefinition
            if (rpc !== null) {
                return rpc
            }
            throw new ResponseException(NOT_FOUND, "RPC does not exist.");
        }
        val slashErrorMsg  = String.format("Identifier %n%s%ncan't contain slash character (/). +
            If slash is part of identifier name then use %2F placeholder.",identifier)
        throw new ResponseException(NOT_FOUND, slashErrorMsg);
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

    override subscribeToStream(String identifier, UriInfo uriInfo) {
        val streamName = Notificator.createStreamNameFromUri(identifier)
        if (streamName.nullOrEmpty) {
            throw new ResponseException(BAD_REQUEST, "Stream name is empty.")
        }
        val listener = Notificator.getListenerFor(streamName);
        if (listener === null) {
            throw new ResponseException(BAD_REQUEST, "Stream was not found.")
        }
        broker.registerToListenDataChanges(listener)
        val uriBuilder = uriInfo.getAbsolutePathBuilder()
        val uriToWebsocketServer = uriBuilder.port(WebSocketServer.PORT).replacePath(streamName).build()
        return Response.status(OK).location(uriToWebsocketServer).build
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
