/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2014 Brocade Communication Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.rpc.impl.BrokerRpcExecutor;
import org.opendaylight.controller.sal.restconf.rpc.impl.MountPointRpcExecutor;
import org.opendaylight.controller.sal.restconf.rpc.impl.RpcExecutor;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.FeatureDefinition;
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

public class RestconfImpl implements RestconfService {
    private enum UriParameters {
        PRETTY_PRINT("prettyPrint"),
        DEPTH("depth");

        private String uriParameterName;

        UriParameters(String uriParameterName) {
            this.uriParameterName = uriParameterName;
        }

        @Override
        public String toString() {
            return uriParameterName;
        }
    }

    private final static RestconfImpl INSTANCE = new RestconfImpl();

    private static final int CHAR_NOT_FOUND = -1;

    private final static String MOUNT_POINT_MODULE_NAME = "ietf-netconf";

    private final static SimpleDateFormat REVISION_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

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
    public StructuredData getModules(final UriInfo uriInfo) {
        final Module restconfModule = this.getRestconfModule();

        final List<Node<?>> modulesAsData = new ArrayList<Node<?>>();
        final DataSchemaNode moduleSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);

        Set<Module> allModules = this.controllerContext.getAllModules();
        for (final Module module : allModules) {
            CompositeNode moduleCompositeNode = this.toModuleCompositeNode(module, moduleSchemaNode);
            modulesAsData.add(moduleCompositeNode);
        }

        final DataSchemaNode modulesSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
        QName qName = modulesSchemaNode.getQName();
        final CompositeNode modulesNode = NodeFactory.createImmutableCompositeNode(qName, null, modulesAsData);
        return new StructuredData(modulesNode, modulesSchemaNode, null, parsePrettyPrintParameter(uriInfo));
    }

    @Override
    public StructuredData getAvailableStreams(final UriInfo uriInfo) {
        Set<String> availableStreams = Notificator.getStreamNames();

        final List<Node<?>> streamsAsData = new ArrayList<Node<?>>();
        Module restconfModule = this.getRestconfModule();
        final DataSchemaNode streamSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.STREAM_LIST_SCHEMA_NODE);
        for (final String streamName : availableStreams) {
            streamsAsData.add(this.toStreamCompositeNode(streamName, streamSchemaNode));
        }

        final DataSchemaNode streamsSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
        QName qName = streamsSchemaNode.getQName();
        final CompositeNode streamsNode = NodeFactory.createImmutableCompositeNode(qName, null, streamsAsData);
        return new StructuredData(streamsNode, streamsSchemaNode, null, parsePrettyPrintParameter(uriInfo));
    }

    @Override
    public StructuredData getModules(final String identifier, final UriInfo uriInfo) {
        Set<Module> modules = null;
        MountInstance mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            InstanceIdWithSchemaNode mountPointIdentifier = this.controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            modules = this.controllerContext.getAllModules(mountPoint);
        } else {
            throw new RestconfDocumentedException(
                    "URI has bad format. If modules behind mount point should be showed, URI has to end with "
                            + ControllerContext.MOUNT, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        final List<Node<?>> modulesAsData = new ArrayList<Node<?>>();
        Module restconfModule = this.getRestconfModule();
        final DataSchemaNode moduleSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);

        for (final Module module : modules) {
            modulesAsData.add(this.toModuleCompositeNode(module, moduleSchemaNode));
        }

        final DataSchemaNode modulesSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
        QName qName = modulesSchemaNode.getQName();
        final CompositeNode modulesNode = NodeFactory.createImmutableCompositeNode(qName, null, modulesAsData);
        return new StructuredData(modulesNode, modulesSchemaNode, mountPoint, parsePrettyPrintParameter(uriInfo));
    }

    @Override
    public StructuredData getModule(final String identifier, final UriInfo uriInfo) {
        final QName moduleNameAndRevision = this.getModuleNameAndRevision(identifier);
        Module module = null;
        MountInstance mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            InstanceIdWithSchemaNode mountPointIdentifier = this.controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            module = this.controllerContext.findModuleByNameAndRevision(mountPoint, moduleNameAndRevision);
        } else {
            module = this.controllerContext.findModuleByNameAndRevision(moduleNameAndRevision);
        }

        if (module == null) {
            throw new RestconfDocumentedException("Module with name '" + moduleNameAndRevision.getLocalName()
                    + "' and revision '" + moduleNameAndRevision.getRevision() + "' was not found.",
                    ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
        }

        Module restconfModule = this.getRestconfModule();
        final DataSchemaNode moduleSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);
        final CompositeNode moduleNode = this.toModuleCompositeNode(module, moduleSchemaNode);
        return new StructuredData(moduleNode, moduleSchemaNode, mountPoint, parsePrettyPrintParameter(uriInfo));
    }

    @Override
    public StructuredData getOperations(final UriInfo uriInfo) {
        Set<Module> allModules = this.controllerContext.getAllModules();
        return this.operationsFromModulesToStructuredData(allModules, null, parsePrettyPrintParameter(uriInfo));
    }

    @Override
    public StructuredData getOperations(final String identifier, final UriInfo uriInfo) {
        Set<Module> modules = null;
        MountInstance mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            InstanceIdWithSchemaNode mountPointIdentifier = this.controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            modules = this.controllerContext.getAllModules(mountPoint);
        } else {
            throw new RestconfDocumentedException(
                    "URI has bad format. If operations behind mount point should be showed, URI has to end with "
                            + ControllerContext.MOUNT, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        return this.operationsFromModulesToStructuredData(modules, mountPoint, parsePrettyPrintParameter(uriInfo));
    }

    private StructuredData operationsFromModulesToStructuredData(final Set<Module> modules,
            final MountInstance mountPoint, boolean prettyPrint) {
        final List<Node<?>> operationsAsData = new ArrayList<Node<?>>();
        Module restconfModule = this.getRestconfModule();
        final DataSchemaNode operationsSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(
                restconfModule, Draft02.RestConfModule.OPERATIONS_CONTAINER_SCHEMA_NODE);
        QName qName = operationsSchemaNode.getQName();
        SchemaPath path = operationsSchemaNode.getPath();
        ContainerSchemaNodeBuilder containerSchemaNodeBuilder = new ContainerSchemaNodeBuilder(
                Draft02.RestConfModule.NAME, 0, qName, path);
        final ContainerSchemaNodeBuilder fakeOperationsSchemaNode = containerSchemaNodeBuilder;
        for (final Module module : modules) {
            Set<RpcDefinition> rpcs = module.getRpcs();
            for (final RpcDefinition rpc : rpcs) {
                QName rpcQName = rpc.getQName();
                SimpleNode<Object> immutableSimpleNode = NodeFactory.<Object> createImmutableSimpleNode(rpcQName, null,
                        null);
                operationsAsData.add(immutableSimpleNode);

                String name = module.getName();
                LeafSchemaNodeBuilder leafSchemaNodeBuilder = new LeafSchemaNodeBuilder(name, 0, rpcQName,
                        SchemaPath.create(true, QName.create("dummy")));
                final LeafSchemaNodeBuilder fakeRpcSchemaNode = leafSchemaNodeBuilder;
                fakeRpcSchemaNode.setAugmenting(true);

                EmptyType instance = EmptyType.getInstance();
                fakeRpcSchemaNode.setType(instance);
                fakeOperationsSchemaNode.addChildNode(fakeRpcSchemaNode.build());
            }
        }

        final CompositeNode operationsNode = NodeFactory.createImmutableCompositeNode(qName, null, operationsAsData);
        ContainerSchemaNode schemaNode = fakeOperationsSchemaNode.build();
        return new StructuredData(operationsNode, schemaNode, mountPoint, prettyPrint);
    }

    private Module getRestconfModule() {
        Module restconfModule = controllerContext.getRestconfModule();
        if (restconfModule == null) {
            throw new RestconfDocumentedException("ietf-restconf module was not found.", ErrorType.APPLICATION,
                    ErrorTag.OPERATION_NOT_SUPPORTED);
        }

        return restconfModule;
    }

    private QName getModuleNameAndRevision(final String identifier) {
        final int mountIndex = identifier.indexOf(ControllerContext.MOUNT);
        String moduleNameAndRevision = "";
        if (mountIndex >= 0) {
            moduleNameAndRevision = identifier.substring(mountIndex + ControllerContext.MOUNT.length());
        } else {
            moduleNameAndRevision = identifier;
        }

        Splitter splitter = Splitter.on("/").omitEmptyStrings();
        Iterable<String> split = splitter.split(moduleNameAndRevision);
        final List<String> pathArgs = Lists.<String> newArrayList(split);
        if (pathArgs.size() < 2) {
            throw new RestconfDocumentedException(
                    "URI has bad format. End of URI should be in format \'moduleName/yyyy-MM-dd\'", ErrorType.PROTOCOL,
                    ErrorTag.INVALID_VALUE);
        }

        try {
            final String moduleName = pathArgs.get(0);
            String revision = pathArgs.get(1);
            final Date moduleRevision = REVISION_FORMAT.parse(revision);
            return QName.create(null, moduleRevision, moduleName);
        } catch (ParseException e) {
            throw new RestconfDocumentedException("URI has bad format. It should be \'moduleName/yyyy-MM-dd\'",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    private CompositeNode toStreamCompositeNode(final String streamName, final DataSchemaNode streamSchemaNode) {
        final List<Node<?>> streamNodeValues = new ArrayList<Node<?>>();
        List<DataSchemaNode> instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) streamSchemaNode), "name");
        final DataSchemaNode nameSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues
                .add(NodeFactory.<String> createImmutableSimpleNode(nameSchemaNode.getQName(), null, streamName));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) streamSchemaNode), "description");
        final DataSchemaNode descriptionSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<String> createImmutableSimpleNode(descriptionSchemaNode.getQName(), null,
                "DESCRIPTION_PLACEHOLDER"));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) streamSchemaNode), "replay-support");
        final DataSchemaNode replaySupportSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<Boolean> createImmutableSimpleNode(replaySupportSchemaNode.getQName(), null,
                Boolean.valueOf(true)));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) streamSchemaNode), "replay-log-creation-time");
        final DataSchemaNode replayLogCreationTimeSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<String> createImmutableSimpleNode(replayLogCreationTimeSchemaNode.getQName(),
                null, ""));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) streamSchemaNode), "events");
        final DataSchemaNode eventsSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        streamNodeValues.add(NodeFactory.<String> createImmutableSimpleNode(eventsSchemaNode.getQName(), null, ""));

        return NodeFactory.createImmutableCompositeNode(streamSchemaNode.getQName(), null, streamNodeValues);
    }

    private CompositeNode toModuleCompositeNode(final Module module, final DataSchemaNode moduleSchemaNode) {
        final List<Node<?>> moduleNodeValues = new ArrayList<Node<?>>();
        List<DataSchemaNode> instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) moduleSchemaNode), "name");
        final DataSchemaNode nameSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        moduleNodeValues.add(NodeFactory.<String> createImmutableSimpleNode(nameSchemaNode.getQName(), null,
                module.getName()));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) moduleSchemaNode), "revision");
        final DataSchemaNode revisionSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Date _revision = module.getRevision();
        moduleNodeValues.add(NodeFactory.<String> createImmutableSimpleNode(revisionSchemaNode.getQName(), null,
                REVISION_FORMAT.format(_revision)));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) moduleSchemaNode), "namespace");
        final DataSchemaNode namespaceSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        moduleNodeValues.add(NodeFactory.<String> createImmutableSimpleNode(namespaceSchemaNode.getQName(), null,
                module.getNamespace().toString()));

        instanceDataChildrenByName = this.controllerContext.findInstanceDataChildrenByName(
                ((DataNodeContainer) moduleSchemaNode), "feature");
        final DataSchemaNode featureSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        for (final FeatureDefinition feature : module.getFeatures()) {
            moduleNodeValues.add(NodeFactory.<String> createImmutableSimpleNode(featureSchemaNode.getQName(), null,
                    feature.getQName().getLocalName()));
        }

        return NodeFactory.createImmutableCompositeNode(moduleSchemaNode.getQName(), null, moduleNodeValues);
    }

    @Override
    public Object getRoot() {
        return null;
    }

    @Override
    public StructuredData invokeRpc(final String identifier, final CompositeNode payload, final UriInfo uriInfo) {
        final RpcExecutor rpc = this.resolveIdentifierInInvokeRpc(identifier);
        QName rpcName = rpc.getRpcDefinition().getQName();
        URI rpcNamespace = rpcName.getNamespace();
        if (Objects.equal(rpcNamespace.toString(), SAL_REMOTE_NAMESPACE)
                && Objects.equal(rpcName.getLocalName(), SAL_REMOTE_RPC_SUBSRCIBE)) {
            return invokeSalRemoteRpcSubscribeRPC(payload, rpc.getRpcDefinition(), parsePrettyPrintParameter(uriInfo));
        }

        validateInput(rpc.getRpcDefinition().getInput(), payload);

        return callRpc(rpc, payload, parsePrettyPrintParameter(uriInfo));
    }

    private void validateInput(final DataSchemaNode inputSchema, final CompositeNode payload) {
        if (inputSchema != null && payload == null) {
            // expected a non null payload
            throw new RestconfDocumentedException("Input is required.", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
        } else if (inputSchema == null && payload != null) {
            // did not expect any input
            throw new RestconfDocumentedException("No input expected.", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
        }
        // else
        // {
        // TODO: Validate "mandatory" and "config" values here??? Or should those be
        // validate in a more central location inside MD-SAL core.
        // }
    }

    private StructuredData invokeSalRemoteRpcSubscribeRPC(final CompositeNode payload, final RpcDefinition rpc,
            final boolean prettyPrint) {
        final CompositeNode value = this.normalizeNode(payload, rpc.getInput(), null);
        final SimpleNode<? extends Object> pathNode = value == null ? null : value.getFirstSimpleByName(QName.create(
                rpc.getQName(), "path"));
        final Object pathValue = pathNode == null ? null : pathNode.getValue();

        if (!(pathValue instanceof InstanceIdentifier)) {
            throw new RestconfDocumentedException("Instance identifier was not normalized correctly.",
                    ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED);
        }

        final InstanceIdentifier pathIdentifier = ((InstanceIdentifier) pathValue);
        String streamName = null;
        if (!Iterables.isEmpty(pathIdentifier.getPathArguments())) {
            String fullRestconfIdentifier = this.controllerContext.toFullRestconfIdentifier(pathIdentifier);
            streamName = Notificator.createStreamNameFromUri(fullRestconfIdentifier);
        }

        if (Strings.isNullOrEmpty(streamName)) {
            throw new RestconfDocumentedException(
                    "Path is empty or contains data node which is not Container or List build-in type.",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        final SimpleNode<String> streamNameNode = NodeFactory.<String> createImmutableSimpleNode(
                QName.create(rpc.getOutput().getQName(), "stream-name"), null, streamName);
        final List<Node<?>> output = new ArrayList<Node<?>>();
        output.add(streamNameNode);

        final MutableCompositeNode responseData = NodeFactory.createMutableCompositeNode(rpc.getOutput().getQName(),
                null, output, null, null);

        if (!Notificator.existListenerFor(pathIdentifier)) {
            Notificator.createListener(pathIdentifier, streamName);
        }

        return new StructuredData(responseData, rpc.getOutput(), null, prettyPrint);
    }

    @Override
    public StructuredData invokeRpc(final String identifier, final String noPayload, final UriInfo uriInfo) {
        if (StringUtils.isNotBlank(noPayload)) {
            throw new RestconfDocumentedException("Content must be empty.", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
        return invokeRpc(identifier, (CompositeNode) null, uriInfo);
    }

    private RpcExecutor resolveIdentifierInInvokeRpc(final String identifier) {
        String identifierEncoded = null;
        MountInstance mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            // mounted RPC call - look up mount instance.
            InstanceIdWithSchemaNode mountPointId = controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointId.getMountPoint();

            int startOfRemoteRpcName = identifier.lastIndexOf(ControllerContext.MOUNT)
                    + ControllerContext.MOUNT.length() + 1;
            String remoteRpcName = identifier.substring(startOfRemoteRpcName);
            identifierEncoded = remoteRpcName;

        } else if (identifier.indexOf("/") != CHAR_NOT_FOUND) {
            final String slashErrorMsg = String.format("Identifier %n%s%ncan\'t contain slash "
                    + "character (/).%nIf slash is part of identifier name then use %%2F placeholder.", identifier);
            throw new RestconfDocumentedException(slashErrorMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        } else {
            identifierEncoded = identifier;
        }

        final String identifierDecoded = controllerContext.urlPathArgDecode(identifierEncoded);

        RpcDefinition rpc = null;
        if (mountPoint == null) {
            rpc = controllerContext.getRpcDefinition(identifierDecoded);
        } else {
            rpc = findRpc(mountPoint.getSchemaContext(), identifierDecoded);
        }

        if (rpc == null) {
            throw new RestconfDocumentedException("RPC does not exist.", ErrorType.RPC, ErrorTag.UNKNOWN_ELEMENT);
        }

        if (mountPoint == null) {
            return new BrokerRpcExecutor(rpc, broker);
        } else {
            return new MountPointRpcExecutor(rpc, mountPoint);
        }

    }

    private RpcDefinition findRpc(final SchemaContext schemaContext, final String identifierDecoded) {
        final String[] splittedIdentifier = identifierDecoded.split(":");
        if (splittedIdentifier.length != 2) {
            throw new RestconfDocumentedException(identifierDecoded
                    + " couldn't be splitted to 2 parts (module:rpc name)", ErrorType.APPLICATION,
                    ErrorTag.INVALID_VALUE);
        }
        for (Module module : schemaContext.getModules()) {
            if (module.getName().equals(splittedIdentifier[0])) {
                for (RpcDefinition rpcDefinition : module.getRpcs()) {
                    if (rpcDefinition.getQName().getLocalName().equals(splittedIdentifier[1])) {
                        return rpcDefinition;
                    }
                }
            }
        }
        return null;
    }

    private StructuredData callRpc(final RpcExecutor rpcExecutor, final CompositeNode payload, boolean prettyPrint) {
        if (rpcExecutor == null) {
            throw new RestconfDocumentedException("RPC does not exist.", ErrorType.RPC, ErrorTag.UNKNOWN_ELEMENT);
        }

        CompositeNode rpcRequest = null;
        RpcDefinition rpc = rpcExecutor.getRpcDefinition();
        QName rpcName = rpc.getQName();

        if (payload == null) {
            rpcRequest = NodeFactory.createMutableCompositeNode(rpcName, null, null, null, null);
        } else {
            final CompositeNode value = this.normalizeNode(payload, rpc.getInput(), null);
            List<Node<?>> input = Collections.<Node<?>> singletonList(value);
            rpcRequest = NodeFactory.createMutableCompositeNode(rpcName, null, input, null, null);
        }

        RpcResult<CompositeNode> rpcResult = rpcExecutor.invokeRpc(rpcRequest);

        checkRpcSuccessAndThrowException(rpcResult);

        if (rpcResult.getResult() == null) {
            return null;
        }

        if (rpc.getOutput() == null) {
            return null; // no output, nothing to send back.
        }

        return new StructuredData(rpcResult.getResult(), rpc.getOutput(), null, prettyPrint);
    }

    private void checkRpcSuccessAndThrowException(final RpcResult<CompositeNode> rpcResult) {
        if (rpcResult.isSuccessful() == false) {

            Collection<RpcError> rpcErrors = rpcResult.getErrors();
            if (rpcErrors == null || rpcErrors.isEmpty()) {
                throw new RestconfDocumentedException(
                        "The operation was not successful and there were no RPC errors returned", ErrorType.RPC,
                        ErrorTag.OPERATION_FAILED);
            }

            List<RestconfError> errorList = Lists.newArrayList();
            for (RpcError rpcError : rpcErrors) {
                errorList.add(new RestconfError(rpcError));
            }

            throw new RestconfDocumentedException(errorList);
        }
    }

    @Override
    public StructuredData readConfigurationData(final String identifier, final UriInfo uriInfo) {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        CompositeNode data = null;
        MountInstance mountPoint = iiWithData.getMountPoint();
        if (mountPoint != null) {
            data = broker.readConfigurationDataBehindMountPoint(mountPoint, iiWithData.getInstanceIdentifier());
        } else {
            data = broker.readConfigurationData(iiWithData.getInstanceIdentifier());
        }

        data = pruneDataAtDepth(data, parseDepthParameter(uriInfo));
        boolean prettyPrintMode = parsePrettyPrintParameter(uriInfo);
        return new StructuredData(data, iiWithData.getSchemaNode(), iiWithData.getMountPoint(), prettyPrintMode);
    }

    @SuppressWarnings("unchecked")
    private <T extends Node<?>> T pruneDataAtDepth(final T node, final Integer depth) {
        if (depth == null) {
            return node;
        }

        if (node instanceof CompositeNode) {
            ImmutableList.Builder<Node<?>> newChildNodes = ImmutableList.<Node<?>> builder();
            if (depth > 1) {
                for (Node<?> childNode : ((CompositeNode) node).getValue()) {
                    newChildNodes.add(pruneDataAtDepth(childNode, depth - 1));
                }
            }

            return (T) ImmutableCompositeNode.create(node.getNodeType(), newChildNodes.build());
        } else { // SimpleNode
            return node;
        }
    }

    private Integer parseDepthParameter(final UriInfo info) {
        String param = info.getQueryParameters(false).getFirst(UriParameters.DEPTH.toString());
        if (Strings.isNullOrEmpty(param) || "unbounded".equals(param)) {
            return null;
        }

        try {
            Integer depth = Integer.valueOf(param);
            if (depth < 1) {
                throw new RestconfDocumentedException(new RestconfError(ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                        "Invalid depth parameter: " + depth, null,
                        "The depth parameter must be an integer > 1 or \"unbounded\""));
            }

            return depth;
        } catch (NumberFormatException e) {
            throw new RestconfDocumentedException(new RestconfError(ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    "Invalid depth parameter: " + e.getMessage(), null,
                    "The depth parameter must be an integer > 1 or \"unbounded\""));
        }
    }

    @Override
    public StructuredData readOperationalData(final String identifier, final UriInfo info) {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        CompositeNode data = null;
        MountInstance mountPoint = iiWithData.getMountPoint();
        if (mountPoint != null) {
            data = broker.readOperationalDataBehindMountPoint(mountPoint, iiWithData.getInstanceIdentifier());
        } else {
            data = broker.readOperationalData(iiWithData.getInstanceIdentifier());
        }

        data = pruneDataAtDepth(data, parseDepthParameter(info));
        boolean prettyPrintMode = parsePrettyPrintParameter(info);
        return new StructuredData(data, iiWithData.getSchemaNode(), mountPoint, prettyPrintMode);
    }

    private boolean parsePrettyPrintParameter(UriInfo info) {
        String param = info.getQueryParameters(false).getFirst(UriParameters.PRETTY_PRINT.toString());
        return Boolean.parseBoolean(param);
    }

    @Override
    public Response updateConfigurationData(final String identifier, final CompositeNode payload) {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);

        validateInput(iiWithData.getSchemaNode(), payload);

        MountInstance mountPoint = iiWithData.getMountPoint();
        final CompositeNode value = this.normalizeNode(payload, iiWithData.getSchemaNode(), mountPoint);
        validateListKeysEqualityInPayloadAndUri(iiWithData, payload);
        RpcResult<TransactionStatus> status = null;

        try {
            if (mountPoint != null) {
                status = broker.commitConfigurationDataPutBehindMountPoint(mountPoint,
                        iiWithData.getInstanceIdentifier(), value).get();
            } else {
                status = broker.commitConfigurationDataPut(iiWithData.getInstanceIdentifier(), value).get();
            }
        } catch (Exception e) {
            throw new RestconfDocumentedException("Error updating data", e);
        }

        if (status.getResult() == TransactionStatus.COMMITED) {
            return Response.status(Status.OK).build();
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Validates whether keys in {@code payload} are equal to values of keys in {@code iiWithData} for list schema node
     *
     * @throws RestconfDocumentedException
     *             if key values or key count in payload and URI isn't equal
     *
     */
    private void validateListKeysEqualityInPayloadAndUri(final InstanceIdWithSchemaNode iiWithData,
            final CompositeNode payload) {
        if (iiWithData.getSchemaNode() instanceof ListSchemaNode) {
            final List<QName> keyDefinitions = ((ListSchemaNode) iiWithData.getSchemaNode()).getKeyDefinition();
            final PathArgument lastPathArgument = iiWithData.getInstanceIdentifier().getLastPathArgument();
            if (lastPathArgument instanceof NodeIdentifierWithPredicates) {
                final Map<QName, Object> uriKeyValues = ((NodeIdentifierWithPredicates) lastPathArgument)
                        .getKeyValues();
                isEqualUriAndPayloadKeyValues(uriKeyValues, payload, keyDefinitions);
            }
        }
    }

    private void isEqualUriAndPayloadKeyValues(final Map<QName, Object> uriKeyValues, final CompositeNode payload,
            final List<QName> keyDefinitions) {
        for (QName keyDefinition : keyDefinitions) {
            final Object uriKeyValue = uriKeyValues.get(keyDefinition);
            // should be caught during parsing URI to InstanceIdentifier
            if (uriKeyValue == null) {
                throw new RestconfDocumentedException("Missing key " + keyDefinition + " in URI.", ErrorType.PROTOCOL,
                        ErrorTag.DATA_MISSING);
            }
            final List<SimpleNode<?>> payloadKeyValues = payload.getSimpleNodesByName(keyDefinition.getLocalName());
            if (payloadKeyValues.isEmpty()) {
                throw new RestconfDocumentedException("Missing key " + keyDefinition.getLocalName()
                        + " in the message body.", ErrorType.PROTOCOL, ErrorTag.DATA_MISSING);
            }

            Object payloadKeyValue = payloadKeyValues.iterator().next().getValue();
            if (!uriKeyValue.equals(payloadKeyValue)) {
                throw new RestconfDocumentedException("The value '" + uriKeyValue + "' for key '"
                        + keyDefinition.getLocalName() + "' specified in the URI doesn't match the value '"
                        + payloadKeyValue + "' specified in the message body. ", ErrorType.PROTOCOL,
                        ErrorTag.INVALID_VALUE);
            }
        }
    }

    @Override
    public Response createConfigurationData(final String identifier, final CompositeNode payload) {
        if (payload == null) {
            throw new RestconfDocumentedException("Input is required.", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
        }

        URI payloadNS = this.namespace(payload);
        if (payloadNS == null) {
            throw new RestconfDocumentedException(
                    "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)",
                    ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE);
        }

        InstanceIdWithSchemaNode iiWithData = null;
        CompositeNode value = null;
        if (this.representsMountPointRootData(payload)) {
            // payload represents mount point data and URI represents path to the mount point

            if (this.endsWithMountPoint(identifier)) {
                throw new RestconfDocumentedException("URI has bad format. URI should be without \""
                        + ControllerContext.MOUNT + "\" for POST operation.", ErrorType.PROTOCOL,
                        ErrorTag.INVALID_VALUE);
            }

            final String completeIdentifier = this.addMountPointIdentifier(identifier);
            iiWithData = this.controllerContext.toInstanceIdentifier(completeIdentifier);

            value = this.normalizeNode(payload, iiWithData.getSchemaNode(), iiWithData.getMountPoint());
        } else {
            final InstanceIdWithSchemaNode incompleteInstIdWithData = this.controllerContext
                    .toInstanceIdentifier(identifier);
            final DataNodeContainer parentSchema = (DataNodeContainer) incompleteInstIdWithData.getSchemaNode();
            MountInstance mountPoint = incompleteInstIdWithData.getMountPoint();
            final Module module = this.findModule(mountPoint, payload);
            if (module == null) {
                throw new RestconfDocumentedException("Module was not found for \"" + payloadNS + "\"",
                        ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
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
                Future<RpcResult<TransactionStatus>> future = broker.commitConfigurationDataPostBehindMountPoint(
                        mountPoint, iiWithData.getInstanceIdentifier(), value);
                status = future == null ? null : future.get();
            } else {
                Future<RpcResult<TransactionStatus>> future = broker.commitConfigurationDataPost(
                        iiWithData.getInstanceIdentifier(), value);
                status = future == null ? null : future.get();
            }
        } catch (Exception e) {
            throw new RestconfDocumentedException("Error creating data", e);
        }

        if (status == null) {
            return Response.status(Status.ACCEPTED).build();
        }

        if (status.getResult() == TransactionStatus.COMMITED) {
            return Response.status(Status.NO_CONTENT).build();
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    public Response createConfigurationData(final CompositeNode payload) {
        if (payload == null) {
            throw new RestconfDocumentedException("Input is required.", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
        }

        URI payloadNS = this.namespace(payload);
        if (payloadNS == null) {
            throw new RestconfDocumentedException(
                    "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)",
                    ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE);
        }

        final Module module = this.findModule(null, payload);
        if (module == null) {
            throw new RestconfDocumentedException(
                    "Data has bad format. Root element node has incorrect namespace (XML format) or module name(JSON format)",
                    ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE);
        }

        String payloadName = this.getName(payload);
        final DataSchemaNode schemaNode = this.controllerContext.findInstanceDataChildByNameAndNamespace(module,
                payloadName, module.getNamespace());
        final CompositeNode value = this.normalizeNode(payload, schemaNode, null);
        final InstanceIdWithSchemaNode iiWithData = this.addLastIdentifierFromData(null, value, schemaNode);
        RpcResult<TransactionStatus> status = null;
        MountInstance mountPoint = iiWithData.getMountPoint();

        try {
            if (mountPoint != null) {
                Future<RpcResult<TransactionStatus>> future = broker.commitConfigurationDataPostBehindMountPoint(
                        mountPoint, iiWithData.getInstanceIdentifier(), value);
                status = future == null ? null : future.get();
            } else {
                Future<RpcResult<TransactionStatus>> future = broker.commitConfigurationDataPost(
                        iiWithData.getInstanceIdentifier(), value);
                status = future == null ? null : future.get();
            }
        } catch (Exception e) {
            throw new RestconfDocumentedException("Error creating data", e);
        }

        if (status == null) {
            return Response.status(Status.ACCEPTED).build();
        }

        if (status.getResult() == TransactionStatus.COMMITED) {
            return Response.status(Status.NO_CONTENT).build();
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    public Response deleteConfigurationData(final String identifier) {
        final InstanceIdWithSchemaNode iiWithData = this.controllerContext.toInstanceIdentifier(identifier);
        RpcResult<TransactionStatus> status = null;
        MountInstance mountPoint = iiWithData.getMountPoint();

        try {
            if (mountPoint != null) {
                status = broker.commitConfigurationDataDeleteBehindMountPoint(mountPoint,
                        iiWithData.getInstanceIdentifier()).get();
            } else {
                status = broker.commitConfigurationDataDelete(iiWithData.getInstanceIdentifier()).get();
            }
        } catch (Exception e) {
            throw new RestconfDocumentedException("Error creating data", e);
        }

        if (status.getResult() == TransactionStatus.COMMITED) {
            return Response.status(Status.OK).build();
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    public Response subscribeToStream(final String identifier, final UriInfo uriInfo) {
        final String streamName = Notificator.createStreamNameFromUri(identifier);
        if (Strings.isNullOrEmpty(streamName)) {
            throw new RestconfDocumentedException("Stream name is empty.", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        final ListenerAdapter listener = Notificator.getListenerFor(streamName);
        if (listener == null) {
            throw new RestconfDocumentedException("Stream was not found.", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
        }

        broker.registerToListenDataChanges(listener);

        final UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        UriBuilder port = uriBuilder.port(WebSocketServer.getInstance().getPort());
        final URI uriToWebsocketServer = port.replacePath(streamName).build();

        return Response.status(Status.OK).location(uriToWebsocketServer).build();
    }

    private Module findModule(final MountInstance mountPoint, final CompositeNode data) {
        if (data instanceof CompositeNodeWrapper) {
            return findModule(mountPoint, (CompositeNodeWrapper) data);
        } else if (data != null) {
            URI namespace = data.getNodeType().getNamespace();
            if (mountPoint != null) {
                return this.controllerContext.findModuleByNamespace(mountPoint, namespace);
            } else {
                return this.controllerContext.findModuleByNamespace(namespace);
            }
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: "
                    + Arrays.<Object> asList(mountPoint, data).toString());
        }
    }

    private Module findModule(final MountInstance mountPoint, final CompositeNodeWrapper data) {
        URI namespace = data.getNamespace();
        Preconditions.<URI> checkNotNull(namespace);

        Module module = null;
        if (mountPoint != null) {
            module = this.controllerContext.findModuleByNamespace(mountPoint, namespace);
            if (module == null) {
                module = this.controllerContext.findModuleByName(mountPoint, namespace.toString());
            }
        } else {
            module = this.controllerContext.findModuleByNamespace(namespace);
            if (module == null) {
                module = this.controllerContext.findModuleByName(namespace.toString());
            }
        }

        return module;
    }

    private InstanceIdWithSchemaNode addLastIdentifierFromData(final InstanceIdWithSchemaNode identifierWithSchemaNode,
            final CompositeNode data, final DataSchemaNode schemaOfData) {
        InstanceIdentifier instanceIdentifier = null;
        if (identifierWithSchemaNode != null) {
            instanceIdentifier = identifierWithSchemaNode.getInstanceIdentifier();
        }

        final InstanceIdentifier iiOriginal = instanceIdentifier;
        InstanceIdentifierBuilder iiBuilder = null;
        if (iiOriginal == null) {
            iiBuilder = InstanceIdentifier.builder();
        } else {
            iiBuilder = InstanceIdentifier.builder(iiOriginal);
        }

        if ((schemaOfData instanceof ListSchemaNode)) {
            HashMap<QName, Object> keys = this.resolveKeysFromData(((ListSchemaNode) schemaOfData), data);
            iiBuilder.nodeWithKey(schemaOfData.getQName(), keys);
        } else {
            iiBuilder.node(schemaOfData.getQName());
        }

        InstanceIdentifier instance = iiBuilder.toInstance();
        MountInstance mountPoint = null;
        if (identifierWithSchemaNode != null) {
            mountPoint = identifierWithSchemaNode.getMountPoint();
        }

        return new InstanceIdWithSchemaNode(instance, schemaOfData, mountPoint);
    }

    private HashMap<QName, Object> resolveKeysFromData(final ListSchemaNode listNode, final CompositeNode dataNode) {
        final HashMap<QName, Object> keyValues = new HashMap<QName, Object>();
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
                throw new RestconfDocumentedException("Data contains list \"" + dataNode.getNodeType().getLocalName()
                        + "\" which does not contain key: \"" + key.getLocalName() + "\"", ErrorType.PROTOCOL,
                        ErrorTag.INVALID_VALUE);
            }

            keyValues.put(key, dataNodeKeyValueObject);
        }

        return keyValues;
    }

    private boolean endsWithMountPoint(final String identifier) {
        return identifier.endsWith(ControllerContext.MOUNT) || identifier.endsWith(ControllerContext.MOUNT + "/");
    }

    private boolean representsMountPointRootData(final CompositeNode data) {
        URI namespace = this.namespace(data);
        return (SchemaContext.NAME.getNamespace().equals(namespace) /*
                                                                     * || MOUNT_POINT_MODULE_NAME .equals( namespace .
                                                                     * toString( ) )
                                                                     */)
                && SchemaContext.NAME.getLocalName().equals(this.localName(data));
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

            throw new RestconfDocumentedException("Data schema node was not found for " + localName,
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        if (!(schema instanceof DataNodeContainer)) {
            throw new RestconfDocumentedException("Root element has to be container or list yang datatype.",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        if ((node instanceof CompositeNodeWrapper)) {
            boolean isChangeAllowed = ((CompositeNodeWrapper) node).isChangeAllowed();
            if (isChangeAllowed) {
                try {
                    this.normalizeNode(((CompositeNodeWrapper) node), schema, null, mountPoint);
                } catch (IllegalArgumentException e) {
                    throw new RestconfDocumentedException(e.getMessage(), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
                }
            }

            return ((CompositeNodeWrapper) node).unwrap();
        }

        return node;
    }

    private void normalizeNode(final NodeWrapper<? extends Object> nodeBuilder, final DataSchemaNode schema,
            final QName previousAugment, final MountInstance mountPoint) {
        if (schema == null) {
            throw new RestconfDocumentedException("Data has bad format.\n\"" + nodeBuilder.getLocalName()
                    + "\" does not exist in yang schema.", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        QName currentAugment = null;
        if (nodeBuilder.getQname() != null) {
            currentAugment = previousAugment;
        } else {
            currentAugment = this.normalizeNodeName(nodeBuilder, schema, previousAugment, mountPoint);
            if (nodeBuilder.getQname() == null) {
                throw new RestconfDocumentedException(
                        "Data has bad format.\nIf data is in XML format then namespace for \""
                                + nodeBuilder.getLocalName() + "\" should be \"" + schema.getQName().getNamespace()
                                + "\".\n" + "If data is in JSON format then module name for \""
                                + nodeBuilder.getLocalName() + "\" should be corresponding to namespace \""
                                + schema.getQName().getNamespace() + "\".", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
            }
        }

        if (nodeBuilder instanceof CompositeNodeWrapper) {
            if (schema instanceof DataNodeContainer) {
                normalizeCompositeNode((CompositeNodeWrapper) nodeBuilder, (DataNodeContainer) schema, mountPoint,
                        currentAugment);
            } else if (schema instanceof AnyXmlSchemaNode) {
                normalizeAnyXmlNode((CompositeNodeWrapper) nodeBuilder, (AnyXmlSchemaNode) schema);
            }
        } else if (nodeBuilder instanceof SimpleNodeWrapper) {
            normalizeSimpleNode((SimpleNodeWrapper) nodeBuilder, schema, mountPoint);
        } else if ((nodeBuilder instanceof EmptyNodeWrapper)) {
            normalizeEmptyNode((EmptyNodeWrapper) nodeBuilder, schema);
        }
    }

    private void normalizeAnyXmlNode(final CompositeNodeWrapper compositeNode, final AnyXmlSchemaNode schema) {
        List<NodeWrapper<?>> children = compositeNode.getValues();
        for (NodeWrapper<? extends Object> child : children) {
            child.setNamespace(schema.getQName().getNamespace());
            if (child instanceof CompositeNodeWrapper) {
                normalizeAnyXmlNode((CompositeNodeWrapper) child, schema);
            }
        }
    }

    private void normalizeEmptyNode(final EmptyNodeWrapper emptyNodeBuilder, final DataSchemaNode schema) {
        if ((schema instanceof LeafSchemaNode)) {
            emptyNodeBuilder.setComposite(false);
        } else {
            if ((schema instanceof ContainerSchemaNode)) {
                // FIXME: Add presence check
                emptyNodeBuilder.setComposite(true);
            }
        }
    }

    private void normalizeSimpleNode(final SimpleNodeWrapper simpleNode, final DataSchemaNode schema,
            final MountInstance mountPoint) {
        final Object value = simpleNode.getValue();
        Object inputValue = value;
        TypeDefinition<? extends Object> typeDefinition = this.typeDefinition(schema);
        if ((typeDefinition instanceof IdentityrefTypeDefinition)) {
            if ((value instanceof String)) {
                inputValue = new IdentityValuesDTO(simpleNode.getNamespace().toString(), (String) value, null,
                        (String) value);
            } // else value is already instance of IdentityValuesDTO
        }

        Object outputValue = inputValue;

        if (typeDefinition != null) {
            Codec<Object, Object> codec = RestCodec.from(typeDefinition, mountPoint);
            outputValue = codec == null ? null : codec.deserialize(inputValue);
        }

        simpleNode.setValue(outputValue);
    }

    private void normalizeCompositeNode(final CompositeNodeWrapper compositeNodeBuilder,
            final DataNodeContainer schema, final MountInstance mountPoint, final QName currentAugment) {
        final List<NodeWrapper<?>> children = compositeNodeBuilder.getValues();
        checkNodeMultiplicityAccordingToSchema(schema, children);
        for (final NodeWrapper<? extends Object> child : children) {
            final List<DataSchemaNode> potentialSchemaNodes = this.controllerContext.findInstanceDataChildrenByName(
                    schema, child.getLocalName());

            if (potentialSchemaNodes.size() > 1 && child.getNamespace() == null) {
                StringBuilder builder = new StringBuilder();
                for (final DataSchemaNode potentialSchemaNode : potentialSchemaNodes) {
                    builder.append("   ").append(potentialSchemaNode.getQName().getNamespace().toString()).append("\n");
                }

                throw new RestconfDocumentedException("Node \"" + child.getLocalName()
                        + "\" is added as augment from more than one module. "
                        + "Therefore node must have namespace (XML format) or module name (JSON format)."
                        + "\nThe node is added as augment from modules with namespaces:\n" + builder,
                        ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
            }

            boolean rightNodeSchemaFound = false;
            for (final DataSchemaNode potentialSchemaNode : potentialSchemaNodes) {
                if (!rightNodeSchemaFound) {
                    final QName potentialCurrentAugment = this.normalizeNodeName(child, potentialSchemaNode,
                            currentAugment, mountPoint);
                    if (child.getQname() != null) {
                        this.normalizeNode(child, potentialSchemaNode, potentialCurrentAugment, mountPoint);
                        rightNodeSchemaFound = true;
                    }
                }
            }

            if (!rightNodeSchemaFound) {
                throw new RestconfDocumentedException("Schema node \"" + child.getLocalName()
                        + "\" was not found in module.", ErrorType.APPLICATION, ErrorTag.UNKNOWN_ELEMENT);
            }
        }

        if ((schema instanceof ListSchemaNode)) {
            ListSchemaNode listSchemaNode = (ListSchemaNode) schema;
            final List<QName> listKeys = listSchemaNode.getKeyDefinition();
            for (final QName listKey : listKeys) {
                boolean foundKey = false;
                for (final NodeWrapper<? extends Object> child : children) {
                    if (Objects.equal(child.unwrap().getNodeType().getLocalName(), listKey.getLocalName())) {
                        foundKey = true;
                    }
                }

                if (!foundKey) {
                    throw new RestconfDocumentedException("Missing key in URI \"" + listKey.getLocalName()
                            + "\" of list \"" + listSchemaNode.getQName().getLocalName() + "\"", ErrorType.PROTOCOL,
                            ErrorTag.DATA_MISSING);
                }
            }
        }
    }

    private void checkNodeMultiplicityAccordingToSchema(final DataNodeContainer dataNodeContainer,
            final List<NodeWrapper<?>> nodes) {
        Map<String, Integer> equalNodeNamesToCounts = new HashMap<String, Integer>();
        for (NodeWrapper<?> child : nodes) {
            Integer count = equalNodeNamesToCounts.get(child.getLocalName());
            equalNodeNamesToCounts.put(child.getLocalName(), count == null ? 1 : ++count);
        }

        for (DataSchemaNode childSchemaNode : dataNodeContainer.getChildNodes()) {
            if (childSchemaNode instanceof ContainerSchemaNode || childSchemaNode instanceof LeafSchemaNode) {
                String localName = childSchemaNode.getQName().getLocalName();
                Integer count = equalNodeNamesToCounts.get(localName);
                if (count != null && count > 1) {
                    throw new RestconfDocumentedException("Multiple input data elements were specified for '"
                            + childSchemaNode.getQName().getLocalName()
                            + "'. The data for this element type can only be specified once.", ErrorType.APPLICATION,
                            ErrorTag.BAD_ELEMENT);
                }
            }
        }
    }

    private QName normalizeNodeName(final NodeWrapper<? extends Object> nodeBuilder, final DataSchemaNode schema,
            final QName previousAugment, final MountInstance mountPoint) {
        QName validQName = schema.getQName();
        QName currentAugment = previousAugment;
        if (schema.isAugmenting()) {
            currentAugment = schema.getQName();
        } else if (previousAugment != null
                && !Objects.equal(schema.getQName().getNamespace(), previousAugment.getNamespace())) {
            validQName = QName.create(currentAugment, schema.getQName().getLocalName());
        }

        String moduleName = null;
        if (mountPoint == null) {
            moduleName = controllerContext.findModuleNameByNamespace(validQName.getNamespace());
        } else {
            moduleName = controllerContext.findModuleNameByNamespace(mountPoint, validQName.getNamespace());
        }

        if (nodeBuilder.getNamespace() == null || Objects.equal(nodeBuilder.getNamespace(), validQName.getNamespace())
                || Objects.equal(nodeBuilder.getNamespace().toString(), moduleName) /*
                                                                                     * || Note : this check is wrong -
                                                                                     * can never be true as it compares
                                                                                     * a URI with a String not sure what
                                                                                     * the intention is so commented out
                                                                                     * ... Objects . equal ( nodeBuilder
                                                                                     * . getNamespace ( ) ,
                                                                                     * MOUNT_POINT_MODULE_NAME )
                                                                                     */) {

            nodeBuilder.setQname(validQName);
        }

        return currentAugment;
    }

    private URI namespace(final CompositeNode data) {
        if (data instanceof CompositeNodeWrapper) {
            return ((CompositeNodeWrapper) data).getNamespace();
        } else if (data != null) {
            return data.getNodeType().getNamespace();
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(data).toString());
        }
    }

    private String localName(final CompositeNode data) {
        if (data instanceof CompositeNodeWrapper) {
            return ((CompositeNodeWrapper) data).getLocalName();
        } else if (data != null) {
            return data.getNodeType().getLocalName();
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(data).toString());
        }
    }

    private String getName(final CompositeNode data) {
        if (data instanceof CompositeNodeWrapper) {
            return ((CompositeNodeWrapper) data).getLocalName();
        } else if (data != null) {
            return data.getNodeType().getLocalName();
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(data).toString());
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
            return typeDefinition((LeafListSchemaNode) node);
        } else if (node instanceof LeafSchemaNode) {
            return _typeDefinition((LeafSchemaNode) node);
        } else if (node instanceof AnyXmlSchemaNode) {
            return null;
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(node).toString());
        }
    }
}
