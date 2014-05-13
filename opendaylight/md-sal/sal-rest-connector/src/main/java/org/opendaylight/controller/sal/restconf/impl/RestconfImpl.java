/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.EmptyNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.NodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.FeatureDefinition;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.EmptyType;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafSchemaNodeBuilder;

@SuppressWarnings("all")
public class RestconfImpl implements RestconfService {
    private final static RestconfImpl INSTANCE = new RestconfImpl();

    private final static String MOUNT_POINT_MODULE_NAME = "ietf-netconf";

    private final static SimpleDateFormat REVISION_FORMAT =  new SimpleDateFormat("yyyy-MM-dd");

    private final static String RESTCONF_MODULE_DRAFT02_REVISION = "2013-10-19";

    private final static String RESTCONF_MODULE_DRAFT02_NAME = "ietf-restconf";

    private final static String RESTCONF_MODULE_DRAFT02_NAMESPACE = "urn:ietf:params:xml:ns:yang:ietf-restconf";

    private final static String RESTCONF_MODULE_DRAFT02_RESTCONF_GROUPING_SCHEMA_NODE = "restconf";

    private final static String RESTCONF_MODULE_DRAFT02_RESTCONF_CONTAINER_SCHEMA_NODE = "restconf";

    private final static String RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE = "modules";

    private final static String RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE = "module";

    private final static String RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE = "streams";

    private final static String RESTCONF_MODULE_DRAFT02_STREAM_LIST_SCHEMA_NODE = "stream";

    private final static String RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE = "operations";

    private final static String SAL_REMOTE_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote";

    private final static String SAL_REMOTE_RPC_SUBSRCIBE = "create-data-change-event-subscription";

    private BrokerFacade broker;

    private ControllerContext controllerContext;

    public void setBroker(final BrokerFacade broker) {
        this.broker = broker;
    }

    public void setControllerContext(final ControllerContext controllerContext) {
        this.controllerContext = controllerContext;
    }

    private RestconfImpl() {
    }

    public static RestconfImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public StructuredData getModules() {
        final Module restconfModule = this.getRestconfModule();

        final List<Node<?>> modulesAsData = new ArrayList<Node<?>>();
        final DataSchemaNode moduleSchemaNode =
                this.getSchemaNode(restconfModule, RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);

        Set<Module> allModules = this.controllerContext.getAllModules();
        for (final Module module : allModules) {
            CompositeNode moduleCompositeNode = this.toModuleCompositeNode(module, moduleSchemaNode);
            modulesAsData.add(moduleCompositeNode);
        }

        final DataSchemaNode modulesSchemaNode =
                this.getSchemaNode(restconfModule, RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
        QName qName = modulesSchemaNode.getQName();
        final CompositeNode modulesNode = NodeFactory.createImmutableCompositeNode(qName, null, modulesAsData);
        return new StructuredData(modulesNode, modulesSchemaNode, null);
    }

    @Override
    public StructuredData getAvailableStreams() {
        Set<String> availableStreams = Notificator.getStreamNames();

        final List<Node<?>> streamsAsData = new ArrayList<Node<?>>();
        Module restconfModule = this.getRestconfModule();
        final DataSchemaNode streamSchemaNode =
            this.getSchemaNode(restconfModule, RESTCONF_MODULE_DRAFT02_STREAM_LIST_SCHEMA_NODE);
        for (final String streamName : availableStreams) {
            streamsAsData.add(this.toStreamCompositeNode(streamName, streamSchemaNode));
        }

        final DataSchemaNode streamsSchemaNode =
            this.getSchemaNode(restconfModule, RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE);
        QName qName = streamsSchemaNode.getQName();
        final CompositeNode streamsNode = NodeFactory.createImmutableCompositeNode(qName, null, streamsAsData);
        return new StructuredData(streamsNode, streamsSchemaNode, null);
    }

    @Override
    public StructuredData getModules(final String identifier) {
        Set<Module> modules = null;
        MountInstance mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            InstanceIdWithSchemaNode mountPointIdentifier =
                                           this.controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            modules = this.controllerContext.getAllModules(mountPoint);
        }
        else {
            throw new ResponseException(Status.BAD_REQUEST,
                "URI has bad format. If modules behind mount point should be showed, URI has to end with " +
                ControllerContext.MOUNT);
        }

        final List<Node<?>> modulesAsData = new ArrayList<Node<?>>();
        Module restconfModule = this.getRestconfModule();
        final DataSchemaNode moduleSchemaNode =
            this.getSchemaNode(restconfModule, RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);

        for (final Module module : modules) {
            modulesAsData.add(this.toModuleCompositeNode(module, moduleSchemaNode));
        }

        final DataSchemaNode modulesSchemaNode =
            this.getSchemaNode(restconfModule, RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
        QName qName = modulesSchemaNode.getQName();
        final CompositeNode modulesNode = NodeFactory.createImmutableCompositeNode(qName, null, modulesAsData);
        return new StructuredData(modulesNode, modulesSchemaNode, mountPoint);
    }

    @Override
    public StructuredData getModule(final String identifier) {
        final QName moduleNameAndRevision = this.getModuleNameAndRevision(identifier);
        Module module = null;
        MountInstance mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            InstanceIdWithSchemaNode mountPointIdentifier =
                                            this.controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            module = this.controllerContext.findModuleByNameAndRevision(mountPoint, moduleNameAndRevision);
        }
        else {
            module = this.controllerContext.findModuleByNameAndRevision(moduleNameAndRevision);
        }

        if (module == null) {
            throw new ResponseException(Status.BAD_REQUEST,
                    "Module with name '" + moduleNameAndRevision.getLocalName() + "' and revision '" +
                    moduleNameAndRevision.getRevision() + "' was not found.");
        }

        Module restconfModule = this.getRestconfModule();
        final DataSchemaNode moduleSchemaNode =
            this.getSchemaNode(restconfModule, RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);
        final CompositeNode moduleNode = this.toModuleCompositeNode(module, moduleSchemaNode);
        return new StructuredData(moduleNode, moduleSchemaNode, mountPoint);
    }

    @Override
    public StructuredData getOperations() {
        Set<Module> allModules = this.controllerContext.getAllModules();
        return this.operationsFromModulesToStructuredData(allModules, null);
    }

    @Override
    public StructuredData getOperations(final String identifier) {
        Set<Module> modules = null;
        MountInstance mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            InstanceIdWithSchemaNode mountPointIdentifier =
                                         this.controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            modules = this.controllerContext.getAllModules(mountPoint);
        }
        else {
            throw new ResponseException(Status.BAD_REQUEST,
                "URI has bad format. If operations behind mount point should be showed, URI has to end with " +
            ControllerContext.MOUNT);
        }

        return this.operationsFromModulesToStructuredData(modules, mountPoint);
    }

    private StructuredData operationsFromModulesToStructuredData(final Set<Module> modules,
                                                                 final MountInstance mountPoint) {
        final List<Node<?>> operationsAsData = new ArrayList<Node<?>>();
        Module restconfModule = this.getRestconfModule();
        final DataSchemaNode operationsSchemaNode =
            this.getSchemaNode(restconfModule, RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE);
        QName qName = operationsSchemaNode.getQName();
        SchemaPath path = operationsSchemaNode.getPath();
        ContainerSchemaNodeBuilder containerSchemaNodeBuilder =
                             new ContainerSchemaNodeBuilder(RESTCONF_MODULE_DRAFT02_NAME, 0, qName, path);
        final ContainerSchemaNodeBuilder fakeOperationsSchemaNode = containerSchemaNodeBuilder;
        for (final Module module : modules) {
            Set<RpcDefinition> rpcs = module.getRpcs();
            for (final RpcDefinition rpc : rpcs) {
                QName rpcQName = rpc.getQName();
                SimpleNode<Object> immutableSimpleNode =
                                     NodeFactory.<Object>createImmutableSimpleNode(rpcQName, null, null);
                operationsAsData.add(immutableSimpleNode);

                String name = module.getName();
                LeafSchemaNodeBuilder leafSchemaNodeBuilder = new LeafSchemaNodeBuilder(name, 0, rpcQName, null);
                final LeafSchemaNodeBuilder fakeRpcSchemaNode = leafSchemaNodeBuilder;
                fakeRpcSchemaNode.setAugmenting(true);

                EmptyType instance = EmptyType.getInstance();
                fakeRpcSchemaNode.setType(instance);
                fakeOperationsSchemaNode.addChildNode(fakeRpcSchemaNode.build());
            }
        }

        final CompositeNode operationsNode =
                                  NodeFactory.createImmutableCompositeNode(qName, null, operationsAsData);
        ContainerSchemaNode schemaNode = fakeOperationsSchemaNode.build();
        return new StructuredData(operationsNode, schemaNode, mountPoint);
    }

    private Module getRestconfModule() {
        QName qName = QName.create(RESTCONF_MODULE_DRAFT02_NAMESPACE, RESTCONF_MODULE_DRAFT02_REVISION,
                                   RESTCONF_MODULE_DRAFT02_NAME);
        final Module restconfModule = this.controllerContext.findModuleByNameAndRevision(qName);
        if (restconfModule == null) {
            throw new ResponseException(Status.INTERNAL_SERVER_ERROR, "Restconf module was not found.");
        }

        return restconfModule;
    }

    private QName getModuleNameAndRevision(final String identifier) {
        final int mountIndex = identifier.indexOf(ControllerContext.MOUNT);
        String moduleNameAndRevision = "";
        if (mountIndex >= 0) {
            moduleNameAndRevision = identifier.substring(mountIndex + ControllerContext.MOUNT.length());
        }
        else {
            moduleNameAndRevision = identifier;
        }

        Splitter splitter = Splitter.on("/").omitEmptyStrings();
        Iterable<String> split = splitter.split(moduleNameAndRevision);
        final List<String> pathArgs = Lists.<String>newArrayList(split);
        if (pathArgs.size() < 2) {
            throw new ResponseException(Status.BAD_REQUEST,
                    "URI has bad format. End of URI should be in format \'moduleName/yyyy-MM-dd\'");
        }

        try {
            final String moduleName = pathArgs.get( 0 );
            String revision = pathArgs.get(1);
            final Date moduleRevision = REVISION_FORMAT.parse(revision);
            return QName.create(null, moduleRevision, moduleName);
        }
        catch (ParseException e) {
            throw new ResponseException(Status.BAD_REQUEST, "URI has bad format. It should be \'moduleName/yyyy-MM-dd\'");
        }
    }

    private CompositeNode toStreamCompositeNode(final String streamName, final DataSchemaNode streamSchemaNode) {
        final List<Node<?>> streamNodeValues = new ArrayList<Node<?>>();
        List<DataSchemaNode> instanceDataChildrenByName =
                this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) streamSchemaNode),
                                                                       "name");
        final DataSchemaNode nameSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<String>createImmutableSimpleNode(nameSchemaNode.getQName(), null,
                                                                           streamName));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                                                 ((DataNodeContainer) streamSchemaNode), "description");
        final DataSchemaNode descriptionSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<String>createImmutableSimpleNode(descriptionSchemaNode.getQName(), null,
                                                                           "DESCRIPTION_PLACEHOLDER"));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                                               ((DataNodeContainer) streamSchemaNode), "replay-support");
        final DataSchemaNode replaySupportSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<Boolean>createImmutableSimpleNode(replaySupportSchemaNode.getQName(), null,
                                                                            Boolean.valueOf(true)));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                                           ((DataNodeContainer) streamSchemaNode), "replay-log-creation-time");
        final DataSchemaNode replayLogCreationTimeSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<String>createImmutableSimpleNode(replayLogCreationTimeSchemaNode.getQName(),
                                                                           null, ""));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                                                        ((DataNodeContainer) streamSchemaNode), "events");
        final DataSchemaNode eventsSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<String>createImmutableSimpleNode(eventsSchemaNode.getQName(),
                                                                           null, ""));

        return NodeFactory.createImmutableCompositeNode(streamSchemaNode.getQName(), null, streamNodeValues);
    }

    private CompositeNode toModuleCompositeNode(final Module module, final DataSchemaNode moduleSchemaNode) {
        final List<Node<?>> moduleNodeValues = new ArrayList<Node<?>>();
        List<DataSchemaNode> instanceDataChildrenByName =
            this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) moduleSchemaNode), "name");
        final DataSchemaNode nameSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        moduleNodeValues.add(NodeFactory.<String>createImmutableSimpleNode(nameSchemaNode.getQName(),
                                                                           null, module.getName()));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                                                          ((DataNodeContainer) moduleSchemaNode), "revision");
        final DataSchemaNode revisionSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Date _revision = module.getRevision();
        moduleNodeValues.add(NodeFactory.<String>createImmutableSimpleNode(revisionSchemaNode.getQName(), null,
                                                                           REVISION_FORMAT.format(_revision)));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                                                        ((DataNodeContainer) moduleSchemaNode), "namespace");
        final DataSchemaNode namespaceSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        moduleNodeValues.add(NodeFactory.<String>createImmutableSimpleNode(namespaceSchemaNode.getQName(), null,
                                                                           module.getNamespace().toString()));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                                                           ((DataNodeContainer) moduleSchemaNode), "feature");
        final DataSchemaNode featureSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        for (final FeatureDefinition feature : module.getFeatures()) {
            moduleNodeValues.add(NodeFactory.<String>createImmutableSimpleNode(featureSchemaNode.getQName(), null,
                                                                               feature.getQName().getLocalName()));
        }

        return NodeFactory.createImmutableCompositeNode(moduleSchemaNode.getQName(), null, moduleNodeValues);
    }

    private DataSchemaNode getSchemaNode(final Module restconfModule, final String schemaNodeName) {
        Set<GroupingDefinition> groupings = restconfModule.getGroupings();

        final Predicate<GroupingDefinition> filter = new Predicate<GroupingDefinition>() {
            @Override
            public boolean apply(final GroupingDefinition g) {
                return Objects.equal(g.getQName().getLocalName(),
                                     RESTCONF_MODULE_DRAFT02_RESTCONF_GROUPING_SCHEMA_NODE);
            }
        };

        Iterable<GroupingDefinition> filteredGroups = Iterables.filter(groupings, filter);

        final GroupingDefinition restconfGrouping = Iterables.getFirst(filteredGroups, null);

        List<DataSchemaNode> instanceDataChildrenByName =
                this.controllerContext.findInstanceDataChildrenByName(restconfGrouping,
                                                            RESTCONF_MODULE_DRAFT02_RESTCONF_CONTAINER_SCHEMA_NODE);
        final DataSchemaNode restconfContainer = Iterables.getFirst(instanceDataChildrenByName, null);

        if (Objects.equal(schemaNodeName, RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE)) {
            List<DataSchemaNode> instances =
                    this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer),
                                                    RESTCONF_MODULE_DRAFT02_OPERATIONS_CONTAINER_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        }
        else if(Objects.equal(schemaNodeName, RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE)) {
            List<DataSchemaNode> instances =
                    this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer),
                                                   RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        }
        else if(Objects.equal(schemaNodeName, RESTCONF_MODULE_DRAFT02_STREAM_LIST_SCHEMA_NODE)) {
            List<DataSchemaNode> instances =
                    this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer),
                                                   RESTCONF_MODULE_DRAFT02_STREAMS_CONTAINER_SCHEMA_NODE);
            final DataSchemaNode modules = Iterables.getFirst(instances, null);
            instances = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) modules),
                                                               RESTCONF_MODULE_DRAFT02_STREAM_LIST_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        }
        else if(Objects.equal(schemaNodeName, RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE)) {
            List<DataSchemaNode> instances =
                    this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer),
                                                         RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        }
        else if(Objects.equal(schemaNodeName, RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE)) {
            List<DataSchemaNode> instances =
                    this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) restconfContainer),
                                                         RESTCONF_MODULE_DRAFT02_MODULES_CONTAINER_SCHEMA_NODE);
            final DataSchemaNode modules = Iterables.getFirst(instances, null);
            instances = this.controllerContext.findInstanceDataChildrenByName(((DataNodeContainer) modules),
                                                                 RESTCONF_MODULE_DRAFT02_MODULE_LIST_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        }

        return null;
    }

    @Override
    public Object getRoot() {
        return null;
    }

    @Override
    public StructuredData invokeRpc(final String identifier, final CompositeNode payload) {
        final RpcDefinition rpc = this.resolveIdentifierInInvokeRpc(identifier);
        if (Objects.equal(rpc.getQName().getNamespace().toString(), SAL_REMOTE_NAMESPACE) &&
            Objects.equal(rpc.getQName().getLocalName(), SAL_REMOTE_RPC_SUBSRCIBE)) {

            final CompositeNode value = this.normalizeNode(payload, rpc.getInput(), null);
            final SimpleNode<? extends Object> pathNode = value == null ? null :
                                   value.getFirstSimpleByName( QName.create(rpc.getQName(), "path") );
            final Object pathValue = pathNode == null ? null : pathNode.getValue();

            if (!(pathValue instanceof InstanceIdentifier)) {
                throw new ResponseException(Status.INTERNAL_SERVER_ERROR,
                                             "Instance identifier was not normalized correctly.");
            }

            final InstanceIdentifier pathIdentifier = ((InstanceIdentifier) pathValue);
            String streamName = null;
            if (!Iterables.isEmpty(pathIdentifier.getPath())) {
                String fullRestconfIdentifier = this.controllerContext.toFullRestconfIdentifier(pathIdentifier);
                streamName = Notificator.createStreamNameFromUri(fullRestconfIdentifier);
            }

            if (Strings.isNullOrEmpty(streamName)) {
                throw new ResponseException(Status.BAD_REQUEST,
                         "Path is empty or contains data node which is not Container or List build-in type.");
            }

            final SimpleNode<String> streamNameNode = NodeFactory.<String>createImmutableSimpleNode(
                                 QName.create(rpc.getOutput().getQName(), "stream-name"), null, streamName);
            final List<Node<?>> output = new ArrayList<Node<?>>();
            output.add(streamNameNode);

            final MutableCompositeNode responseData = NodeFactory.createMutableCompositeNode(
                                                  rpc.getOutput().getQName(), null, output, null, null);

            if (!Notificator.existListenerFor(pathIdentifier)) {
                Notificator.createListener(pathIdentifier, streamName);
            }

            return new StructuredData(responseData, rpc.getOutput(), null);
        }

        RpcDefinition rpcDefinition = this.controllerContext.getRpcDefinition(identifier);
        return this.callRpc(rpcDefinition, payload);
    }

    @Override
    public StructuredData invokeRpc(final String identifier, final String noPayload) {
        if (!Strings.isNullOrEmpty(noPayload)) {
            throw new ResponseException(Status.UNSUPPORTED_MEDIA_TYPE,
                                                       "Content-Type contains unsupported Media Type.");
        }

        final RpcDefinition rpc = this.resolveIdentifierInInvokeRpc(identifier);
        return this.callRpc(rpc, null);
    }

    private RpcDefinition resolveIdentifierInInvokeRpc(final String identifier) {
        if (identifier.indexOf("/") < 0) {
            final String identifierDecoded = this.controllerContext.urlPathArgDecode(identifier);
            final RpcDefinition rpc = this.controllerContext.getRpcDefinition(identifierDecoded);
            if (rpc != null) {
                return rpc;
            }

            throw new ResponseException(Status.NOT_FOUND, "RPC does not exist.");
        }

        final String slashErrorMsg = String.format(
                "Identifier %n%s%ncan\'t contain slash character (/).%nIf slash is part of identifier name then use %%2F placeholder.",
                identifier);

        throw new ResponseException(Status.NOT_FOUND, slashErrorMsg);
    }

    private StructuredData callRpc(final RpcDefinition rpc, final CompositeNode payload) {
        if (rpc == null) {
            throw new ResponseException(Status.NOT_FOUND, "RPC does not exist.");
        }

        CompositeNode rpcRequest = null;
        if (payload == null) {
            rpcRequest = NodeFactory.createMutableCompositeNode(rpc.getQName(), null, null, null, null);
        }
        else {
            final CompositeNode value = this.normalizeNode(payload, rpc.getInput(), null);
            final List<Node<?>> input = new ArrayList<Node<?>>();
            input.add(value);

            rpcRequest = NodeFactory.createMutableCompositeNode(rpc.getQName(), null, input, null, null);
        }

        final RpcResult<CompositeNode> rpcResult = broker.invokeRpc(rpc.getQName(), rpcRequest);

        if (!rpcResult.isSuccessful()) {
            throw new ResponseException(Status.INTERNAL_SERVER_ERROR, "Operation failed");
        }

        CompositeNode result = rpcResult.getResult();
        if (result == null) {
            return null;
        }

        return new StructuredData(result, rpc.getOutput(), null);
    }

    @Override
    public StructuredData readConfigurationData(final String identifier) {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        CompositeNode data = null;
        MountInstance mountPoint = iiWithData.getMountPoint();
        if (mountPoint != null) {
            data = broker.readConfigurationDataBehindMountPoint(mountPoint, iiWithData.getInstanceIdentifier());
        }
        else {
            data = broker.readConfigurationData(iiWithData.getInstanceIdentifier());
        }

        return new StructuredData(data, iiWithData.getSchemaNode(), iiWithData.getMountPoint());
    }

    @Override
    public StructuredData readOperationalData(final String identifier) {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        CompositeNode data = null;
        MountInstance mountPoint = iiWithData.getMountPoint();
        if (mountPoint != null) {
            data = broker.readOperationalDataBehindMountPoint(mountPoint, iiWithData.getInstanceIdentifier());
        }
        else {
            data = broker.readOperationalData(iiWithData.getInstanceIdentifier());
        }

        return new StructuredData(data, iiWithData.getSchemaNode(), mountPoint);
    }

    @Override
    public Response updateConfigurationData(final String identifier, final CompositeNode payload) {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        MountInstance mountPoint = iiWithData.getMountPoint();
        final CompositeNode value = this.normalizeNode(payload, iiWithData.getSchemaNode(), mountPoint);
        RpcResult<TransactionStatus> status = null;

        try {
            if (mountPoint != null) {
                status = broker.commitConfigurationDataPutBehindMountPoint(
                                                mountPoint, iiWithData.getInstanceIdentifier(), value).get();
            } else {
                status = broker.commitConfigurationDataPut(iiWithData.getInstanceIdentifier(), value).get();
            }
        }
        catch( Exception e ) {
            throw new ResponseException( e, "Error updating data" );
        }

        if( status.getResult() == TransactionStatus.COMMITED )
            return Response.status(Status.OK).build();

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    public Response createConfigurationData(final String identifier, final CompositeNode payload) {
        URI payloadNS = this.namespace(payload);
        if (payloadNS == null) {
            throw new ResponseException(Status.BAD_REQUEST,
                    "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)");
        }

        InstanceIdWithSchemaNode iiWithData = null;
        CompositeNode value = null;
        if (this.representsMountPointRootData(payload)) {
             // payload represents mount point data and URI represents path to the mount point

            if (this.endsWithMountPoint(identifier)) {
                throw new ResponseException(Status.BAD_REQUEST,
                            "URI has bad format. URI should be without \"" + ControllerContext.MOUNT +
                            "\" for POST operation.");
            }

            final String completeIdentifier = this.addMountPointIdentifier(identifier);
            iiWithData = this.controllerContext.toInstanceIdentifier(completeIdentifier);

            value = this.normalizeNode(payload, iiWithData.getSchemaNode(), iiWithData.getMountPoint());
        }
        else {
            final InstanceIdWithSchemaNode incompleteInstIdWithData =
                                               this.controllerContext.toInstanceIdentifier(identifier);
            final DataNodeContainer parentSchema = (DataNodeContainer) incompleteInstIdWithData.getSchemaNode();
            MountInstance mountPoint = incompleteInstIdWithData.getMountPoint();
            final Module module = this.findModule(mountPoint, payload);
            if (module == null) {
                throw new ResponseException(Status.BAD_REQUEST,
                                            "Module was not found for \"" + payloadNS + "\"");
            }

            String payloadName = this.getName(payload);
            final DataSchemaNode schemaNode = this.controllerContext.findInstanceDataChildByNameAndNamespace(
                                                              parentSchema, payloadName, module.getNamespace());
            value = this.normalizeNode(payload, schemaNode, mountPoint);

            iiWithData = this.addLastIdentifierFromData(incompleteInstIdWithData, value, schemaNode);
        }

        RpcResult<TransactionStatus> status = null;
        MountInstance mountPoint = iiWithData.getMountPoint();
        try {
            if (mountPoint != null) {
                Future<RpcResult<TransactionStatus>> future =
                                          broker.commitConfigurationDataPostBehindMountPoint(
                                                       mountPoint, iiWithData.getInstanceIdentifier(), value);
                status = future == null ? null : future.get();
            }
            else {
                Future<RpcResult<TransactionStatus>> future =
                               broker.commitConfigurationDataPost(iiWithData.getInstanceIdentifier(), value);
                status = future == null ? null : future.get();
            }
        }
        catch( Exception e ) {
            throw new ResponseException( e, "Error creating data" );
        }

        if (status == null) {
            return Response.status(Status.ACCEPTED).build();
        }

        if( status.getResult() == TransactionStatus.COMMITED )
            return Response.status(Status.NO_CONTENT).build();

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    public Response createConfigurationData(final CompositeNode payload) {
        URI payloadNS = this.namespace(payload);
        if (payloadNS == null) {
            throw new ResponseException(Status.BAD_REQUEST,
                    "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)");
        }

        final Module module = this.findModule(null, payload);
        if (module == null) {
            throw new ResponseException(Status.BAD_REQUEST,
                    "Data has bad format. Root element node has incorrect namespace (XML format) or module name(JSON format)");
        }

        String payloadName = this.getName(payload);
        final DataSchemaNode schemaNode = this.controllerContext.findInstanceDataChildByNameAndNamespace(
                                                                   module, payloadName, module.getNamespace());
        final CompositeNode value = this.normalizeNode(payload, schemaNode, null);
        final InstanceIdWithSchemaNode iiWithData = this.addLastIdentifierFromData(null, value, schemaNode);
        RpcResult<TransactionStatus> status = null;
        MountInstance mountPoint = iiWithData.getMountPoint();

        try {
            if (mountPoint != null) {
                Future<RpcResult<TransactionStatus>> future =
                                             broker.commitConfigurationDataPostBehindMountPoint(
                                                          mountPoint, iiWithData.getInstanceIdentifier(), value);
                status = future == null ? null : future.get();
            }
            else {
                Future<RpcResult<TransactionStatus>> future =
                                 broker.commitConfigurationDataPost(iiWithData.getInstanceIdentifier(), value);
                status = future == null ? null : future.get();
            }
        }
        catch( Exception e ) {
            throw new ResponseException( e, "Error creating data" );
        }

        if (status == null) {
            return Response.status(Status.ACCEPTED).build();
        }

        if( status.getResult() == TransactionStatus.COMMITED )
            return Response.status(Status.NO_CONTENT).build();

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    public Response deleteConfigurationData(final String identifier) {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        RpcResult<TransactionStatus> status = null;
        MountInstance mountPoint = iiWithData.getMountPoint();

        try {
            if (mountPoint != null) {
                status = broker.commitConfigurationDataDeleteBehindMountPoint(
                                        mountPoint, iiWithData.getInstanceIdentifier()).get();
            }
            else {
                status = broker.commitConfigurationDataDelete(iiWithData.getInstanceIdentifier()).get();
            }
        }
        catch( Exception e ) {
            throw new ResponseException( e, "Error creating data" );
        }

        if( status.getResult() == TransactionStatus.COMMITED )
            return Response.status(Status.OK).build();

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    public Response subscribeToStream(final String identifier, final UriInfo uriInfo) {
        final String streamName = Notificator.createStreamNameFromUri(identifier);
        if (Strings.isNullOrEmpty(streamName)) {
            throw new ResponseException(Status.BAD_REQUEST, "Stream name is empty.");
        }

        final ListenerAdapter listener = Notificator.getListenerFor(streamName);
        if (listener == null) {
            throw new ResponseException(Status.BAD_REQUEST, "Stream was not found.");
        }

        broker.registerToListenDataChanges(listener);

        final UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        UriBuilder port = uriBuilder.port(WebSocketServer.PORT);
        final URI uriToWebsocketServer = port.replacePath(streamName).build();

        return Response.status(Status.OK).location(uriToWebsocketServer).build();
    }

    private Module findModule(final MountInstance mountPoint, final CompositeNode data) {
        if (data instanceof CompositeNodeWrapper) {
            return findModule(mountPoint, (CompositeNodeWrapper)data);
        }
        else if (data != null) {
            URI namespace = data.getNodeType().getNamespace();
            if (mountPoint != null) {
                return this.controllerContext.findModuleByNamespace(mountPoint, namespace);
            }
            else {
                return this.controllerContext.findModuleByNamespace(namespace);
            }
        }
        else {
            throw new IllegalArgumentException("Unhandled parameter types: " +
                    Arrays.<Object>asList(mountPoint, data).toString());
        }
    }

    private Module findModule(final MountInstance mountPoint, final CompositeNodeWrapper data) {
        URI namespace = data.getNamespace();
        Preconditions.<URI>checkNotNull(namespace);

        Module module = null;
        if (mountPoint != null) {
            module = this.controllerContext.findModuleByNamespace(mountPoint, namespace);
            if (module == null) {
                module = this.controllerContext.findModuleByName(mountPoint, namespace.toString());
            }
        }
        else {
            module = this.controllerContext.findModuleByNamespace(namespace);
            if (module == null) {
                module = this.controllerContext.findModuleByName(namespace.toString());
            }
        }

        return module;
    }

    private InstanceIdWithSchemaNode addLastIdentifierFromData(
                                              final InstanceIdWithSchemaNode identifierWithSchemaNode,
                                              final CompositeNode data, final DataSchemaNode schemaOfData) {
        InstanceIdentifier instanceIdentifier = null;
        if (identifierWithSchemaNode != null) {
            instanceIdentifier = identifierWithSchemaNode.getInstanceIdentifier();
        }

        final InstanceIdentifier iiOriginal = instanceIdentifier;
        InstanceIdentifierBuilder iiBuilder = null;
        if (iiOriginal == null) {
            iiBuilder = InstanceIdentifier.builder();
        }
        else {
            iiBuilder = InstanceIdentifier.builder(iiOriginal);
        }

        if ((schemaOfData instanceof ListSchemaNode)) {
            HashMap<QName,Object> keys = this.resolveKeysFromData(((ListSchemaNode) schemaOfData), data);
            iiBuilder.nodeWithKey(schemaOfData.getQName(), keys);
        }
        else {
            iiBuilder.node(schemaOfData.getQName());
        }

        InstanceIdentifier instance = iiBuilder.toInstance();
        MountInstance mountPoint = null;
        if (identifierWithSchemaNode != null) {
            mountPoint=identifierWithSchemaNode.getMountPoint();
        }

        return new InstanceIdWithSchemaNode(instance, schemaOfData, mountPoint);
    }

    private HashMap<QName,Object> resolveKeysFromData(final ListSchemaNode listNode,
                                                      final CompositeNode dataNode) {
        final HashMap<QName,Object> keyValues = new HashMap<QName, Object>();
        List<QName> _keyDefinition = listNode.getKeyDefinition();
        for (final QName key : _keyDefinition) {
            SimpleNode<? extends Object> head = null;
            String localName = key.getLocalName();
            List<SimpleNode<? extends Object>> simpleNodesByName = dataNode.getSimpleNodesByName(localName);
            if (simpleNodesByName != null) {
                head = Iterables.getFirst(simpleNodesByName, null);
            }

            Object dataNodeKeyValueObject = null;
            if (head != null) {
                dataNodeKeyValueObject = head.getValue();
            }

            if (dataNodeKeyValueObject == null) {
                throw new ResponseException(Status.BAD_REQUEST,
                            "Data contains list \"" + dataNode.getNodeType().getLocalName() +
                            "\" which does not contain key: \"" + key.getLocalName() + "\"");
            }

            keyValues.put(key, dataNodeKeyValueObject);
        }

        return keyValues;
    }

    private boolean endsWithMountPoint(final String identifier) {
        return identifier.endsWith(ControllerContext.MOUNT) ||
               identifier.endsWith(ControllerContext.MOUNT + "/");
    }

    private boolean representsMountPointRootData(final CompositeNode data) {
        URI namespace = this.namespace(data);
        return (SchemaContext.NAME.getNamespace().equals( namespace ) /* ||
                MOUNT_POINT_MODULE_NAME.equals( namespace.toString() )*/ ) &&
                SchemaContext.NAME.getLocalName().equals( this.localName(data) );
    }

    private String addMountPointIdentifier(final String identifier) {
        boolean endsWith = identifier.endsWith("/");
        if (endsWith) {
            return (identifier + ControllerContext.MOUNT);
        }

        return identifier + "/" + ControllerContext.MOUNT;
    }

    private CompositeNode normalizeNode(final CompositeNode node, final DataSchemaNode schema,
                                        final MountInstance mountPoint) {
        if (schema == null) {
            QName nodeType = node == null ? null : node.getNodeType();
            String localName = nodeType == null ? null : nodeType.getLocalName();
            String _plus = ("Data schema node was not found for " + localName);
            throw new ResponseException(Status.INTERNAL_SERVER_ERROR,
                                        "Data schema node was not found for " + localName );
        }

        if (!(schema instanceof DataNodeContainer)) {
            throw new ResponseException(Status.BAD_REQUEST,
                                        "Root element has to be container or list yang datatype.");
        }

        if ((node instanceof CompositeNodeWrapper)) {
            boolean isChangeAllowed = ((CompositeNodeWrapper) node).isChangeAllowed();
            if (isChangeAllowed) {
                try {
                    this.normalizeNode(((CompositeNodeWrapper) node), schema, null, mountPoint);
                }
                catch (NumberFormatException e) {
                    throw new ResponseException(Status.BAD_REQUEST, e.getMessage());
                }
            }

            return ((CompositeNodeWrapper) node).unwrap();
        }

        return node;
    }

    private void normalizeNode(final NodeWrapper<? extends Object> nodeBuilder,
                               final DataSchemaNode schema, final QName previousAugment,
                               final MountInstance mountPoint) {
        if (schema == null) {
            throw new ResponseException(Status.BAD_REQUEST,
                                        "Data has bad format.\n\"" + nodeBuilder.getLocalName() +
                                        "\" does not exist in yang schema.");
        }

        QName currentAugment = null;
        if (nodeBuilder.getQname() != null) {
            currentAugment = previousAugment;
        }
        else {
            currentAugment = this.normalizeNodeName(nodeBuilder, schema, previousAugment, mountPoint);
            if (nodeBuilder.getQname() == null) {
                throw new ResponseException(Status.BAD_REQUEST,
                        "Data has bad format.\nIf data is in XML format then namespace for \"" +
                        nodeBuilder.getLocalName() +
                        "\" should be \"" + schema.getQName().getNamespace() + "\".\n" +
                        "If data is in JSON format then module name for \"" + nodeBuilder.getLocalName() +
                         "\" should be corresponding to namespace \"" +
                        schema.getQName().getNamespace() + "\".");
            }
        }

        if ((nodeBuilder instanceof CompositeNodeWrapper)) {
            final List<NodeWrapper<?>> children = ((CompositeNodeWrapper) nodeBuilder).getValues();
            for (final NodeWrapper<? extends Object> child : children) {
                final List<DataSchemaNode> potentialSchemaNodes =
                        this.controllerContext.findInstanceDataChildrenByName(
                                             ((DataNodeContainer) schema), child.getLocalName());

                if (potentialSchemaNodes.size() > 1 && child.getNamespace() == null) {
                    StringBuilder builder = new StringBuilder();
                    for (final DataSchemaNode potentialSchemaNode : potentialSchemaNodes) {
                        builder.append("   ").append(potentialSchemaNode.getQName().getNamespace().toString())
                               .append("\n");
                    }

                    throw new ResponseException(Status.BAD_REQUEST,
                                 "Node \"" + child.getLocalName() +
                                 "\" is added as augment from more than one module. " +
                                 "Therefore node must have namespace (XML format) or module name (JSON format)." +
                                 "\nThe node is added as augment from modules with namespaces:\n" + builder);
                }

                boolean rightNodeSchemaFound = false;
                for (final DataSchemaNode potentialSchemaNode : potentialSchemaNodes) {
                    if (!rightNodeSchemaFound) {
                        final QName potentialCurrentAugment =
                                this.normalizeNodeName(child, potentialSchemaNode, currentAugment, mountPoint);
                        if (child.getQname() != null ) {
                            this.normalizeNode(child, potentialSchemaNode, potentialCurrentAugment, mountPoint);
                            rightNodeSchemaFound = true;
                        }
                    }
                }

                if (!rightNodeSchemaFound) {
                    throw new ResponseException(Status.BAD_REQUEST,
                                      "Schema node \"" + child.getLocalName() + "\" was not found in module.");
                }
            }

            if ((schema instanceof ListSchemaNode)) {
                final List<QName> listKeys = ((ListSchemaNode) schema).getKeyDefinition();
                for (final QName listKey : listKeys) {
                    boolean foundKey = false;
                    for (final NodeWrapper<? extends Object> child : children) {
                        if (Objects.equal(child.unwrap().getNodeType().getLocalName(), listKey.getLocalName())) {
                            foundKey = true;
                        }
                    }

                    if (!foundKey) {
                        throw new ResponseException(Status.BAD_REQUEST,
                                       "Missing key in URI \"" + listKey.getLocalName() +
                                       "\" of list \"" + schema.getQName().getLocalName() + "\"");
                    }
                }
            }
        }
        else {
            if ((nodeBuilder instanceof SimpleNodeWrapper)) {
                final SimpleNodeWrapper simpleNode = ((SimpleNodeWrapper) nodeBuilder);
                final Object value = simpleNode.getValue();
                Object inputValue = value;
                TypeDefinition<? extends Object> typeDefinition = this.typeDefinition(schema);
                if ((typeDefinition instanceof IdentityrefTypeDefinition)) {
                    if ((value instanceof String)) {
                        inputValue = new IdentityValuesDTO( nodeBuilder.getNamespace().toString(),
                                                            (String) value, null, (String) value );
                    } // else value is already instance of IdentityValuesDTO
                }

                Codec<Object,Object> codec = RestCodec.from(typeDefinition, mountPoint);
                Object outputValue = codec == null ? null : codec.deserialize(inputValue);

                simpleNode.setValue(outputValue);
            }
            else {
                if ((nodeBuilder instanceof EmptyNodeWrapper)) {
                    final EmptyNodeWrapper emptyNodeBuilder = ((EmptyNodeWrapper) nodeBuilder);
                    if ((schema instanceof LeafSchemaNode)) {
                        emptyNodeBuilder.setComposite(false);
                    }
                    else {
                        if ((schema instanceof ContainerSchemaNode)) {
                            // FIXME: Add presence check
                            emptyNodeBuilder.setComposite(true);
                        }
                    }
                }
            }
        }
    }

    private QName normalizeNodeName(final NodeWrapper<? extends Object> nodeBuilder,
                                    final DataSchemaNode schema, final QName previousAugment,
                                    final MountInstance mountPoint) {
        QName validQName = schema.getQName();
        QName currentAugment = previousAugment;
        if (schema.isAugmenting()) {
            currentAugment = schema.getQName();
        }
        else if (previousAugment != null &&
                 !Objects.equal( schema.getQName().getNamespace(), previousAugment.getNamespace())) {
            validQName = QName.create(currentAugment, schema.getQName().getLocalName());
        }

        String moduleName = null;
        if (mountPoint == null) {
            moduleName = controllerContext.findModuleNameByNamespace(validQName.getNamespace());
        }
        else {
            moduleName = controllerContext.findModuleNameByNamespace(mountPoint, validQName.getNamespace());
        }

        if (nodeBuilder.getNamespace() == null ||
            Objects.equal(nodeBuilder.getNamespace(), validQName.getNamespace()) ||
            Objects.equal(nodeBuilder.getNamespace().toString(), moduleName) /*||
            Note: this check is wrong - can never be true as it compares a URI with a String
                  not sure what the intention is so commented out...
            Objects.equal(nodeBuilder.getNamespace(), MOUNT_POINT_MODULE_NAME)*/ ) {

            nodeBuilder.setQname(validQName);
        }

        return currentAugment;
    }

    private URI namespace(final CompositeNode data) {
        if (data instanceof CompositeNodeWrapper) {
            return ((CompositeNodeWrapper)data).getNamespace();
        }
        else if (data != null) {
            return data.getNodeType().getNamespace();
        }
        else {
            throw new IllegalArgumentException("Unhandled parameter types: " +
                    Arrays.<Object>asList(data).toString());
        }
    }

    private String localName(final CompositeNode data) {
        if (data instanceof CompositeNodeWrapper) {
            return ((CompositeNodeWrapper)data).getLocalName();
        }
        else if (data != null) {
            return data.getNodeType().getLocalName();
        }
        else {
            throw new IllegalArgumentException("Unhandled parameter types: " +
                    Arrays.<Object>asList(data).toString());
        }
    }

    private String getName(final CompositeNode data) {
        if (data instanceof CompositeNodeWrapper) {
            return ((CompositeNodeWrapper)data).getLocalName();
        }
        else if (data != null) {
            return data.getNodeType().getLocalName();
        }
        else {
            throw new IllegalArgumentException("Unhandled parameter types: " +
                    Arrays.<Object>asList(data).toString());
        }
    }

    private TypeDefinition<? extends Object> _typeDefinition(final LeafSchemaNode node) {
        TypeDefinition<?> baseType = node.getType();
        while (baseType.getBaseType() != null) {
            baseType = baseType.getBaseType();
        }

        return baseType;
    }

    private TypeDefinition<? extends Object> typeDefinition(final LeafListSchemaNode node) {
        TypeDefinition<?> baseType = node.getType();
        while (baseType.getBaseType() != null) {
            baseType = baseType.getBaseType();
        }

        return baseType;
    }

    private TypeDefinition<? extends Object> typeDefinition(final DataSchemaNode node) {
        if (node instanceof LeafListSchemaNode) {
            return typeDefinition((LeafListSchemaNode)node);
        }
        else if (node instanceof LeafSchemaNode) {
            return _typeDefinition((LeafSchemaNode)node);
        }
        else {
            throw new IllegalArgumentException("Unhandled parameter types: " +
                    Arrays.<Object>asList(node).toString());
        }
    }
}
