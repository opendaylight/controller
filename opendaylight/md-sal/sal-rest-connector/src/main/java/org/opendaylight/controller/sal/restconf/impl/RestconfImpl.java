/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2014 Brocade Communication Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModifiedNodeDoesNotExistException;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
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
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.util.ExtendedType;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestconfImpl implements RestconfService {

    private enum UriParameters {
        PRETTY_PRINT("prettyPrint"),
        DEPTH("depth");

        private String uriParameterName;

        UriParameters(final String uriParameterName) {
            this.uriParameterName = uriParameterName;
        }

        @Override
        public String toString() {
            return uriParameterName;
        }
    }

    private static class TypeDef {
        public final TypeDefinition<? extends Object> typedef;
        public final QName qName;

        TypeDef(final TypeDefinition<? extends Object> typedef, final QName qName) {
            this.typedef = typedef;
            this.qName = qName;
        }
    }

    private final static RestconfImpl INSTANCE = new RestconfImpl();

    private static final int NOTIFICATION_PORT = 8181;

    private static final int CHAR_NOT_FOUND = -1;

    private final static String MOUNT_POINT_MODULE_NAME = "ietf-netconf";

    private final static SimpleDateFormat REVISION_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final static String SAL_REMOTE_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote";

    private final static String SAL_REMOTE_RPC_SUBSRCIBE = "create-data-change-event-subscription";

    private BrokerFacade broker;

    private ControllerContext controllerContext;

    private static final Logger LOG = LoggerFactory.getLogger(RestconfImpl.class);

    private static final DataChangeScope DEFAULT_SCOPE = DataChangeScope.BASE;

    private static final LogicalDatastoreType DEFAULT_DATASTORE = LogicalDatastoreType.CONFIGURATION;

    private static final URI NAMESPACE_EVENT_SUBSCRIPTION_AUGMENT = URI.create("urn:sal:restconf:event:subscription");

    private static final Date EVENT_SUBSCRIPTION_AUGMENT_REVISION;

    private static final String DATASTORE_PARAM_NAME = "datastore";

    private static final String SCOPE_PARAM_NAME = "scope";

    private static final String NETCONF_BASE = "urn:ietf:params:xml:ns:netconf:base:1.0";

    private static final String NETCONF_BASE_PAYLOAD_NAME = "data";

    private static final QName NETCONF_BASE_QNAME;

    static {
        try {
            EVENT_SUBSCRIPTION_AUGMENT_REVISION = new SimpleDateFormat("yyyy-MM-dd").parse("2014-07-08");
            NETCONF_BASE_QNAME = QName.create(QNameModule.create(new URI(NETCONF_BASE), null), NETCONF_BASE_PAYLOAD_NAME );
        } catch (final ParseException e) {
            throw new RestconfDocumentedException(
                    "It wasn't possible to convert revision date of sal-remote-augment to date", ErrorType.APPLICATION,
                    ErrorTag.OPERATION_FAILED);
        } catch (final URISyntaxException e) {
            throw new RestconfDocumentedException(
                    "It wasn't possible to create instance of URI class with "+NETCONF_BASE+" URI", ErrorType.APPLICATION,
                    ErrorTag.OPERATION_FAILED);
        }
    }

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
    public NormalizedNodeContext getModules(final UriInfo uriInfo) {
        final Set<Module> allModules = controllerContext.getAllModules();
        final MapNode allModuleMap = makeModuleMapNode(allModules);

        final SchemaContext schemaContext = controllerContext.getGlobalSchema();

        final Module restconfModule = getRestconfModule();
        final DataSchemaNode modulesSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(
                restconfModule, Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
        Preconditions.checkState(modulesSchemaNode instanceof ContainerSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> moduleContainerBuilder =
                Builders.containerBuilder((ContainerSchemaNode) modulesSchemaNode);
        moduleContainerBuilder.withChild(allModuleMap);

        return new NormalizedNodeContext(new InstanceIdentifierContext(null, modulesSchemaNode,
                null, schemaContext), moduleContainerBuilder.build());
    }

    /**
     * Valid only for mount point
     */
    @Override
    public NormalizedNodeContext getModules(final String identifier, final UriInfo uriInfo) {
        Preconditions.checkNotNull(identifier);
        if ( ! identifier.contains(ControllerContext.MOUNT)) {
            final String errMsg = "URI has bad format. If modules behind mount point should be showed,"
                    + " URI has to end with " + ControllerContext.MOUNT;
            throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        final InstanceIdentifierContext mountPointIdentifier = controllerContext.toMountPointIdentifier(identifier);
        final DOMMountPoint mountPoint = mountPointIdentifier.getMountPoint();
        final Set<Module> modules = controllerContext.getAllModules(mountPoint);
        final SchemaContext schemaContext = mountPoint.getSchemaContext();
        final MapNode mountPointModulesMap = makeModuleMapNode(modules);

        final Module restconfModule = getRestconfModule();
        final DataSchemaNode modulesSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(
                restconfModule, Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
        Preconditions.checkState(modulesSchemaNode instanceof ContainerSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> moduleContainerBuilder =
                Builders.containerBuilder((ContainerSchemaNode) modulesSchemaNode);
        moduleContainerBuilder.withChild(mountPointModulesMap);

        return new NormalizedNodeContext(new InstanceIdentifierContext(null, modulesSchemaNode,
                mountPoint, schemaContext), moduleContainerBuilder.build());
    }

    @Override
    public NormalizedNodeContext getModule(final String identifier, final UriInfo uriInfo) {
        Preconditions.checkNotNull(identifier);
        final QName moduleNameAndRevision = getModuleNameAndRevision(identifier);
        Module module = null;
        DOMMountPoint mountPoint = null;
        final SchemaContext schemaContext;
        if (identifier.contains(ControllerContext.MOUNT)) {
            final InstanceIdentifierContext mountPointIdentifier = controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            module = controllerContext.findModuleByNameAndRevision(mountPoint, moduleNameAndRevision);
            schemaContext = mountPoint.getSchemaContext();
        } else {
            module = controllerContext.findModuleByNameAndRevision(moduleNameAndRevision);
            schemaContext = controllerContext.getGlobalSchema();
        }

        if (module == null) {
            final String errMsg = "Module with name '" + moduleNameAndRevision.getLocalName()
                    + "' and revision '" + moduleNameAndRevision.getRevision() + "' was not found.";
            throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
        }

        final Module restconfModule = getRestconfModule();
        final Set<Module> modules = Collections.singleton(module);
        final MapNode moduleMap = makeModuleMapNode(modules);

        final DataSchemaNode moduleSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(
                restconfModule, Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);
        Preconditions.checkState(moduleSchemaNode instanceof ListSchemaNode);

        return new NormalizedNodeContext(new InstanceIdentifierContext(null, moduleSchemaNode, mountPoint,
                schemaContext), moduleMap);
    }

    @Override
    public NormalizedNodeContext getAvailableStreams(final UriInfo uriInfo) {
        final SchemaContext schemaContext = controllerContext.getGlobalSchema();
        final Set<String> availableStreams = Notificator.getStreamNames();
        final Module restconfModule = getRestconfModule();
        final DataSchemaNode streamSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.STREAM_LIST_SCHEMA_NODE);
        Preconditions.checkState(streamSchemaNode instanceof ListSchemaNode);

        final CollectionNodeBuilder<MapEntryNode, MapNode> listStreamsBuilder = Builders
                .mapBuilder((ListSchemaNode) streamSchemaNode);

        for (final String streamName : availableStreams) {
            listStreamsBuilder.withChild(toStreamEntryNode(streamName, streamSchemaNode));
        }

        final DataSchemaNode streamsContainerSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(
                restconfModule, Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
        Preconditions.checkState(streamsContainerSchemaNode instanceof ContainerSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> streamsContainerBuilder =
                Builders.containerBuilder((ContainerSchemaNode) streamsContainerSchemaNode);
        streamsContainerBuilder.withChild(listStreamsBuilder.build());


        return new NormalizedNodeContext(new InstanceIdentifierContext(null, streamsContainerSchemaNode, null,
                schemaContext), streamsContainerBuilder.build());
    }

    @Override
    public NormalizedNodeContext getOperations(final UriInfo uriInfo) {
        final Set<Module> allModules = controllerContext.getAllModules();
        return operationsFromModulesToNormalizedContext(allModules, null);
    }

    @Override
    public NormalizedNodeContext getOperations(final String identifier, final UriInfo uriInfo) {
        Set<Module> modules = null;
        DOMMountPoint mountPoint = null;
        if (identifier.contains(ControllerContext.MOUNT)) {
            final InstanceIdentifierContext mountPointIdentifier = controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            modules = controllerContext.getAllModules(mountPoint);

        } else {
            final String errMsg = "URI has bad format. If operations behind mount point should be showed, URI has to end with ";
            throw new RestconfDocumentedException(errMsg + ControllerContext.MOUNT, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        return operationsFromModulesToNormalizedContext(modules, mountPoint);
    }

    private NormalizedNodeContext operationsFromModulesToNormalizedContext(final Set<Module> modules,
            final DOMMountPoint mountPoint) {

        // FIXME find best way to change restconf-netconf yang schema for provide this functionality
        final String errMsg = "We are not able support view operations functionality yet.";
        throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.OPERATION_NOT_SUPPORTED);
    }

    private Module getRestconfModule() {
        final Module restconfModule = controllerContext.getRestconfModule();
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

        final Splitter splitter = Splitter.on("/").omitEmptyStrings();
        final Iterable<String> split = splitter.split(moduleNameAndRevision);
        final List<String> pathArgs = Lists.<String> newArrayList(split);
        if (pathArgs.size() < 2) {
            throw new RestconfDocumentedException(
                    "URI has bad format. End of URI should be in format \'moduleName/yyyy-MM-dd\'", ErrorType.PROTOCOL,
                    ErrorTag.INVALID_VALUE);
        }

        try {
            final String moduleName = pathArgs.get(0);
            final String revision = pathArgs.get(1);
            final Date moduleRevision = REVISION_FORMAT.parse(revision);
            return QName.create(null, moduleRevision, moduleName);
        } catch (final ParseException e) {
            throw new RestconfDocumentedException("URI has bad format. It should be \'moduleName/yyyy-MM-dd\'",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    @Override
    public Object getRoot() {
        return null;
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final NormalizedNodeContext payload, final UriInfo uriInfo) {
        final SchemaPath type = payload.getInstanceIdentifierContext().getSchemaNode().getPath();
        final URI namespace = payload.getInstanceIdentifierContext().getSchemaNode().getQName().getNamespace();
        final CheckedFuture<DOMRpcResult, DOMRpcException> response;
        final DOMMountPoint mountPoint = payload.getInstanceIdentifierContext().getMountPoint();
        final SchemaContext schemaContext;
        if (identifier.contains(MOUNT_POINT_MODULE_NAME) && mountPoint != null) {
            final Optional<DOMRpcService> mountRpcServices = mountPoint.getService(DOMRpcService.class);
            if ( ! mountRpcServices.isPresent()) {
                throw new RestconfDocumentedException("Rpc service is missing.");
            }
            schemaContext = mountPoint.getSchemaContext();
            response = mountRpcServices.get().invokeRpc(type, payload.getData());
        } else {
            if (namespace.toString().equals(SAL_REMOTE_NAMESPACE)) {
                response = invokeSalRemoteRpcSubscribeRPC(payload);
            } else {
                response = broker.invokeRpc(type, payload.getData());
            }
            schemaContext = controllerContext.getGlobalSchema();
        }

        final DOMRpcResult result = checkRpcResponse(response);

        RpcDefinition resultNodeSchema = null;
        final NormalizedNode<?, ?> resultData = result.getResult();
        if (result != null && result.getResult() != null) {
            resultNodeSchema = (RpcDefinition) payload.getInstanceIdentifierContext().getSchemaNode();
        }

        return new NormalizedNodeContext(new InstanceIdentifierContext<RpcDefinition>(null,
                resultNodeSchema, mountPoint, schemaContext), resultData);
    }

    private DOMRpcResult checkRpcResponse(final CheckedFuture<DOMRpcResult, DOMRpcException> response) {
        if (response == null) {
            return null;
        }
        try {
            final DOMRpcResult retValue = response.get();
            if (retValue.getErrors() == null || retValue.getErrors().isEmpty()) {
                return retValue;
            }
            throw new RestconfDocumentedException("RpcError message", null, retValue.getErrors());
        }
        catch (final InterruptedException e) {
            throw new RestconfDocumentedException(
                    "The operation was interrupted while executing and did not complete.", ErrorType.RPC,
                    ErrorTag.PARTIAL_OPERATION);
        }
        catch (final ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CancellationException) {
                throw new RestconfDocumentedException("The operation was cancelled while executing.", ErrorType.RPC,
                        ErrorTag.PARTIAL_OPERATION);
            } else if (cause != null) {
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }

                if (cause instanceof IllegalArgumentException) {
                    throw new RestconfDocumentedException(cause.getMessage(), ErrorType.PROTOCOL,
                            ErrorTag.INVALID_VALUE);
                }

                throw new RestconfDocumentedException("The operation encountered an unexpected error while executing.",
                        cause);
            } else {
                throw new RestconfDocumentedException("The operation encountered an unexpected error while executing.",
                        e);
            }
        }
    }

    private void validateInput(final SchemaNode inputSchema, final NormalizedNodeContext payload) {
        if (inputSchema != null && payload.getData() == null) {
            // expected a non null payload
            throw new RestconfDocumentedException("Input is required.", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
        } else if (inputSchema == null && payload.getData() != null) {
            // did not expect any input
            throw new RestconfDocumentedException("No input expected.", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
        }
        // else
        // {
        // TODO: Validate "mandatory" and "config" values here??? Or should those be
        // those be
        // validate in a more central location inside MD-SAL core.
        // }
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> invokeSalRemoteRpcSubscribeRPC(final NormalizedNodeContext payload) {
        final ContainerNode value = (ContainerNode) payload.getData();
        final QName rpcQName = payload.getInstanceIdentifierContext().getSchemaNode().getQName();
        final Optional<DataContainerChild<? extends PathArgument, ?>> path = value.getChild(new NodeIdentifier(
                QName.create(payload.getInstanceIdentifierContext().getSchemaNode().getQName(), "path")));
        final Object pathValue = path.isPresent() ? path.get().getValue() : null;

        if (!(pathValue instanceof YangInstanceIdentifier)) {
            throw new RestconfDocumentedException("Instance identifier was not normalized correctly.",
                    ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED);
        }

        final YangInstanceIdentifier pathIdentifier = ((YangInstanceIdentifier) pathValue);
        String streamName = null;
        if (!Iterables.isEmpty(pathIdentifier.getPathArguments())) {
            final String fullRestconfIdentifier = controllerContext.toFullRestconfIdentifier(pathIdentifier, null);

            LogicalDatastoreType datastore = parseEnumTypeParameter(value, LogicalDatastoreType.class,
                    DATASTORE_PARAM_NAME);
            datastore = datastore == null ? DEFAULT_DATASTORE : datastore;

            DataChangeScope scope = parseEnumTypeParameter(value, DataChangeScope.class, SCOPE_PARAM_NAME);
            scope = scope == null ? DEFAULT_SCOPE : scope;

            streamName = Notificator.createStreamNameFromUri(fullRestconfIdentifier + "/datastore=" + datastore
                    + "/scope=" + scope);
        }

        if (Strings.isNullOrEmpty(streamName)) {
            throw new RestconfDocumentedException(
                    "Path is empty or contains value node which is not Container or List build-in type.",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        final QName outputQname = QName.create(rpcQName, "output");
        final QName streamNameQname = QName.create(rpcQName, "stream-name");

        final ContainerNode output = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(outputQname))
                .withChild(ImmutableNodes.leafNode(streamNameQname, streamName)).build();

        if (!Notificator.existListenerFor(streamName)) {
            final YangInstanceIdentifier normalizedPathIdentifier = controllerContext.toNormalized(pathIdentifier);
            Notificator.createListener(normalizedPathIdentifier, streamName);
        }

        final DOMRpcResult defaultDOMRpcResult = new DefaultDOMRpcResult(output);

        return Futures.immediateCheckedFuture(defaultDOMRpcResult);
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final String noPayload, final UriInfo uriInfo) {
        if (StringUtils.isNotBlank(noPayload)) {
            throw new RestconfDocumentedException("Content must be empty.", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        String identifierEncoded = null;
        DOMMountPoint mountPoint = null;
        final SchemaContext schemaContext;
        if (identifier.contains(ControllerContext.MOUNT)) {
            // mounted RPC call - look up mount instance.
            final InstanceIdentifierContext mountPointId = controllerContext.toMountPointIdentifier(identifier);
            mountPoint = mountPointId.getMountPoint();
            schemaContext = mountPoint.getSchemaContext();
            final int startOfRemoteRpcName = identifier.lastIndexOf(ControllerContext.MOUNT)
                    + ControllerContext.MOUNT.length() + 1;
            final String remoteRpcName = identifier.substring(startOfRemoteRpcName);
            identifierEncoded = remoteRpcName;

        } else if (identifier.indexOf("/") != CHAR_NOT_FOUND) {
            final String slashErrorMsg = String.format("Identifier %n%s%ncan\'t contain slash "
                    + "character (/).%nIf slash is part of identifier name then use %%2F placeholder.", identifier);
            throw new RestconfDocumentedException(slashErrorMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        } else {
            identifierEncoded = identifier;
            schemaContext = controllerContext.getGlobalSchema();
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

        if (rpc.getInput() != null) {
            // FIXME : find a correct Error from specification
            throw new IllegalStateException("RPC " + rpc + " does'n need input value!");
        }

        final CheckedFuture<DOMRpcResult, DOMRpcException> response;
        if (mountPoint != null) {
            final Optional<DOMRpcService> mountRpcServices = mountPoint.getService(DOMRpcService.class);
            if ( ! mountRpcServices.isPresent()) {
                throw new RestconfDocumentedException("Rpc service is missing.");
            }
            response = mountRpcServices.get().invokeRpc(rpc.getPath(), null);
        } else {
            response = broker.invokeRpc(rpc.getPath(), null);
        }

        final DOMRpcResult result = checkRpcResponse(response);

        DataSchemaNode resultNodeSchema = null;
        NormalizedNode<?, ?> resultData = null;
        if (result != null && result.getResult() != null) {
            resultData = result.getResult();
            final ContainerSchemaNode rpcDataSchemaNode =
                    SchemaContextUtil.getRpcDataSchema(schemaContext, rpc.getOutput().getPath());
            resultNodeSchema = rpcDataSchemaNode.getDataChildByName(result.getResult().getNodeType());
        }

        return new NormalizedNodeContext(new InstanceIdentifierContext(null, resultNodeSchema, mountPoint,
                schemaContext), resultData);
    }

    private RpcDefinition findRpc(final SchemaContext schemaContext, final String identifierDecoded) {
        final String[] splittedIdentifier = identifierDecoded.split(":");
        if (splittedIdentifier.length != 2) {
            throw new RestconfDocumentedException(identifierDecoded
                    + " couldn't be splitted to 2 parts (module:rpc name)", ErrorType.APPLICATION,
                    ErrorTag.INVALID_VALUE);
        }
        for (final Module module : schemaContext.getModules()) {
            if (module.getName().equals(splittedIdentifier[0])) {
                for (final RpcDefinition rpcDefinition : module.getRpcs()) {
                    if (rpcDefinition.getQName().getLocalName().equals(splittedIdentifier[1])) {
                        return rpcDefinition;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public NormalizedNodeContext readConfigurationData(final String identifier, final UriInfo uriInfo) {
        final InstanceIdentifierContext iiWithData = controllerContext.toInstanceIdentifier(identifier);
        final DOMMountPoint mountPoint = iiWithData.getMountPoint();
        NormalizedNode<?, ?> data = null;
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();
        if (mountPoint != null) {
            data = broker.readConfigurationData(mountPoint, normalizedII);
        } else {
            data = broker.readConfigurationData(normalizedII);
        }
        if(data == null) {
            throw new RestconfDocumentedException(
                "Request could not be completed because the relevant data model content does not exist.",
                ErrorType.APPLICATION, ErrorTag.DATA_MISSING);
        }
        return new NormalizedNodeContext(iiWithData, data);
    }

    private Integer parseDepthParameter(final UriInfo info) {
        final String param = info.getQueryParameters(false).getFirst(UriParameters.DEPTH.toString());
        if (Strings.isNullOrEmpty(param) || "unbounded".equals(param)) {
            return null;
        }

        try {
            final Integer depth = Integer.valueOf(param);
            if (depth < 1) {
                throw new RestconfDocumentedException(new RestconfError(ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                        "Invalid depth parameter: " + depth, null,
                        "The depth parameter must be an integer > 1 or \"unbounded\""));
            }

            return depth;
        } catch (final NumberFormatException e) {
            throw new RestconfDocumentedException(new RestconfError(ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    "Invalid depth parameter: " + e.getMessage(), null,
                    "The depth parameter must be an integer > 1 or \"unbounded\""));
        }
    }

    @Override
    public NormalizedNodeContext readOperationalData(final String identifier, final UriInfo info) {
        final InstanceIdentifierContext iiWithData = controllerContext.toInstanceIdentifier(identifier);
        final DOMMountPoint mountPoint = iiWithData.getMountPoint();
        NormalizedNode<?, ?> data = null;
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();
        if (mountPoint != null) {
            data = broker.readOperationalData(mountPoint, normalizedII);
        } else {
            data = broker.readOperationalData(normalizedII);
        }
        if(data == null) {
            throw new RestconfDocumentedException(
                "Request could not be completed because the relevant data model content does not exist.",
                ErrorType.APPLICATION, ErrorTag.DATA_MISSING);
        }
        return new NormalizedNodeContext(iiWithData, data);
    }

    private boolean parsePrettyPrintParameter(final UriInfo info) {
        final String param = info.getQueryParameters(false).getFirst(UriParameters.PRETTY_PRINT.toString());
        return Boolean.parseBoolean(param);
    }

    @Override
    public Response updateConfigurationData(final String identifier, final NormalizedNodeContext payload) {
        Preconditions.checkNotNull(identifier);
        final InstanceIdentifierContext<DataSchemaNode> iiWithData =
                (InstanceIdentifierContext<DataSchemaNode>) payload.getInstanceIdentifierContext();

        validateInput(iiWithData.getSchemaNode(), payload);
        validateTopLevelNodeName(payload, iiWithData.getInstanceIdentifier());
        validateListKeysEqualityInPayloadAndUri(iiWithData, payload.getData());

        final DOMMountPoint mountPoint = iiWithData.getMountPoint();
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();

        /*
         * There is a small window where another write transaction could be updating the same data
         * simultaneously and we get an OptimisticLockFailedException. This error is likely
         * transient and The WriteTransaction#submit API docs state that a retry will likely
         * succeed. So we'll try again if that scenario occurs. If it fails a third time then it
         * probably will never succeed so we'll fail in that case.
         *
         * By retrying we're attempting to hide the internal implementation of the data store and
         * how it handles concurrent updates from the restconf client. The client has instructed us
         * to put the data and we should make every effort to do so without pushing optimistic lock
         * failures back to the client and forcing them to handle it via retry (and having to
         * document the behavior).
         */
        int tries = 2;
        while(true) {
            try {
                if (mountPoint != null) {
                    broker.commitConfigurationDataPut(mountPoint, normalizedII, payload.getData()).checkedGet();
                } else {
                    broker.commitConfigurationDataPut(normalizedII, payload.getData()).checkedGet();
                }

                break;
            } catch (final TransactionCommitFailedException e) {
                if(e instanceof OptimisticLockFailedException) {
                    if(--tries <= 0) {
                        LOG.debug("Got OptimisticLockFailedException on last try - failing");
                        throw new RestconfDocumentedException(e.getMessage(), e, e.getErrorList());
                    }

                    LOG.debug("Got OptimisticLockFailedException - trying again");
                } else {
                    throw new RestconfDocumentedException(e.getMessage(), e, e.getErrorList());
                }
            }
        }

        return Response.status(Status.OK).build();
    }

    private void validateTopLevelNodeName(final NormalizedNodeContext node,
            final YangInstanceIdentifier identifier) {

        final String payloadName = node.getData().getNodeType().getLocalName();
        final Iterator<PathArgument> pathArguments = identifier.getReversePathArguments().iterator();

        //no arguments
        if ( ! pathArguments.hasNext()) {
            //no "data" payload
            if ( ! node.getData().getNodeType().equals(NETCONF_BASE_QNAME)) {
                throw new RestconfDocumentedException("Instance identifier has to contain at least one path argument",
                        ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
            }
        //any arguments
        } else {
            final String identifierName = pathArguments.next().getNodeType().getLocalName();
            if ( ! payloadName.equals(identifierName)) {
                throw new RestconfDocumentedException("Payload name (" + payloadName
                        + ") is different from identifier name (" + identifierName + ")", ErrorType.PROTOCOL,
                        ErrorTag.MALFORMED_MESSAGE);
            }
        }
    }

    /**
     * Validates whether keys in {@code payload} are equal to values of keys in {@code iiWithData} for list schema node
     *
     * @throws RestconfDocumentedException
     *             if key values or key count in payload and URI isn't equal
     *
     */
    private void validateListKeysEqualityInPayloadAndUri(final InstanceIdentifierContext<DataSchemaNode> iiWithData,
            final NormalizedNode<?, ?> payload) {
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

    private void isEqualUriAndPayloadKeyValues(final Map<QName, Object> uriKeyValues, final NormalizedNode<?, ?> payload,
            final List<QName> keyDefinitions) {
        for (final QName keyDefinition : keyDefinitions) {
            final Object uriKeyValue = uriKeyValues.get(keyDefinition);
            // should be caught during parsing URI to InstanceIdentifier
            if (uriKeyValue == null) {
                final String errMsg = "Missing key " + keyDefinition + " in URI.";
                throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.DATA_MISSING);
            }
            // TODO thing about the possibility to fix
//            final List<SimpleNode<?>> payloadKeyValues = payload.getSimpleNodesByName(keyDefinition.getLocalName());
//            if (payloadKeyValues.isEmpty()) {
//                final String errMsg = "Missing key " + keyDefinition.getLocalName() + " in the message body.";
//                throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.DATA_MISSING);
//            }
//
//            final Object payloadKeyValue = payloadKeyValues.iterator().next().getValue();
//            if (!uriKeyValue.equals(payloadKeyValue)) {
//                final String errMsg = "The value '" + uriKeyValue + "' for key '" + keyDefinition.getLocalName() +
//                        "' specified in the URI doesn't match the value '" + payloadKeyValue + "' specified in the message body. ";
//                throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
//            }
        }
    }

    @Override
    public Response createConfigurationData(final String identifier, final NormalizedNodeContext payload, final UriInfo uriInfo) {
       return createConfigurationData(payload, uriInfo);
    }

    // FIXME create RestconfIdetifierHelper and move this method there
    private YangInstanceIdentifier checkConsistencyOfNormalizedNodeContext(final NormalizedNodeContext payload) {
        Preconditions.checkArgument(payload != null);
        Preconditions.checkArgument(payload.getData() != null);
        Preconditions.checkArgument(payload.getData().getNodeType() != null);
        Preconditions.checkArgument(payload.getInstanceIdentifierContext() != null);
        Preconditions.checkArgument(payload.getInstanceIdentifierContext().getInstanceIdentifier() != null);

        final QName payloadNodeQname = payload.getData().getNodeType();
        final YangInstanceIdentifier yangIdent = payload.getInstanceIdentifierContext().getInstanceIdentifier();
        if (payloadNodeQname.compareTo(yangIdent.getLastPathArgument().getNodeType()) > 0) {
            return yangIdent;
        }
        final InstanceIdentifierContext parentContext = payload.getInstanceIdentifierContext();
        final SchemaNode parentSchemaNode = parentContext.getSchemaNode();
        if(parentSchemaNode instanceof DataNodeContainer) {
            final DataNodeContainer cast = (DataNodeContainer) parentSchemaNode;
            for (final DataSchemaNode child : cast.getChildNodes()) {
                if (payloadNodeQname.compareTo(child.getQName()) == 0) {
                    return YangInstanceIdentifier.builder(yangIdent).node(child.getQName()).build();
                }
            }
        }
        if (parentSchemaNode instanceof RpcDefinition) {
            return yangIdent;
        }
        final String errMsg = "Error parsing input: DataSchemaNode has not children";
        throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
    }

    @Override
    public Response createConfigurationData(final NormalizedNodeContext payload, final UriInfo uriInfo) {
        if (payload == null) {
            throw new RestconfDocumentedException("Input is required.", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
        }

        final URI payloadNS = payload.getData().getNodeType().getNamespace();
        if (payloadNS == null) {
            throw new RestconfDocumentedException(
                    "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)",
                    ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE);
        }

        final DOMMountPoint mountPoint = payload.getInstanceIdentifierContext().getMountPoint();
        final InstanceIdentifierContext<DataSchemaNode> iiWithData = (InstanceIdentifierContext<DataSchemaNode>) payload.getInstanceIdentifierContext();
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();
        final YangInstanceIdentifier resultII;
        try {
            if (mountPoint != null) {
                broker.commitConfigurationDataPost(mountPoint, normalizedII, payload.getData()).checkedGet();
            } else {
                broker.commitConfigurationDataPost(normalizedII, payload.getData()).checkedGet();
            }
        } catch(final RestconfDocumentedException e) {
            throw e;
        } catch (final Exception e) {
            throw new RestconfDocumentedException("Error creating data", e);
        }

        final ResponseBuilder responseBuilder = Response.status(Status.NO_CONTENT);
        // FIXME: Provide path to result.
        final URI location = resolveLocation(uriInfo, "", mountPoint, normalizedII);
        if (location != null) {
            responseBuilder.location(location);
        }
        return responseBuilder.build();
    }

    private URI resolveLocation(final UriInfo uriInfo, final String uriBehindBase, final DOMMountPoint mountPoint, final YangInstanceIdentifier normalizedII) {
        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("config");
        try {
            uriBuilder.path(controllerContext.toFullRestconfIdentifier(normalizedII, mountPoint));
        } catch (final Exception e) {
            LOG.debug("Location for instance identifier"+normalizedII+"wasn't created", e);
            return null;
        }
        return uriBuilder.build();
    }

    @Override
    public Response deleteConfigurationData(final String identifier) {
        final InstanceIdentifierContext<DataSchemaNode> iiWithData = controllerContext.toInstanceIdentifier(identifier);
        final DOMMountPoint mountPoint = iiWithData.getMountPoint();
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();

        try {
            if (mountPoint != null) {
                broker.commitConfigurationDataDelete(mountPoint, normalizedII);
            } else {
                broker.commitConfigurationDataDelete(normalizedII).get();
            }
        } catch (final Exception e) {
            final Optional<Throwable> searchedException = Iterables.tryFind(Throwables.getCausalChain(e),
                    Predicates.instanceOf(ModifiedNodeDoesNotExistException.class));
            if (searchedException.isPresent()) {
                throw new RestconfDocumentedException("Data specified for deleting doesn't exist.", ErrorType.APPLICATION, ErrorTag.DATA_MISSING);
            }
            throw new RestconfDocumentedException("Error while deleting data", e);
        }
        return Response.status(Status.OK).build();
    }

    /**
     * Subscribes to some path in schema context (stream) to listen on changes on this stream.
     *
     * Additional parameters for subscribing to stream are loaded via rpc input parameters:
     * <ul>
     * <li>datastore</li> - default CONFIGURATION (other values of {@link LogicalDatastoreType} enum type)
     * <li>scope</li> - default BASE (other values of {@link DataChangeScope})
     * </ul>
     */
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

        final Map<String, String> paramToValues = resolveValuesFromUri(identifier);
        final LogicalDatastoreType datastore = parserURIEnumParameter(LogicalDatastoreType.class,
                paramToValues.get(DATASTORE_PARAM_NAME));
        if (datastore == null) {
            throw new RestconfDocumentedException("Stream name doesn't contains datastore value (pattern /datastore=)",
                    ErrorType.APPLICATION, ErrorTag.MISSING_ATTRIBUTE);
        }
        final DataChangeScope scope = parserURIEnumParameter(DataChangeScope.class, paramToValues.get(SCOPE_PARAM_NAME));
        if (scope == null) {
            throw new RestconfDocumentedException("Stream name doesn't contains datastore value (pattern /scope=)",
                    ErrorType.APPLICATION, ErrorTag.MISSING_ATTRIBUTE);
        }

        broker.registerToListenDataChanges(datastore, scope, listener);

        final UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        int notificationPort = NOTIFICATION_PORT;
        try {
            final WebSocketServer webSocketServerInstance = WebSocketServer.getInstance();
            notificationPort = webSocketServerInstance.getPort();
        } catch (final NullPointerException e) {
            WebSocketServer.createInstance(NOTIFICATION_PORT);
        }
        final UriBuilder uriToWebsocketServerBuilder = uriBuilder.port(notificationPort).scheme("ws");
        final URI uriToWebsocketServer = uriToWebsocketServerBuilder.replacePath(streamName).build();

        return Response.status(Status.OK).location(uriToWebsocketServer).build();
    }

    /**
     * Load parameter for subscribing to stream from input composite node
     *
     * @param compNode
     *            contains value
     * @return enum object if its string value is equal to {@code paramName}. In other cases null.
     */
    private <T> T parseEnumTypeParameter(final ContainerNode value, final Class<T> classDescriptor,
            final String paramName) {
        final QNameModule salRemoteAugment = QNameModule.create(NAMESPACE_EVENT_SUBSCRIPTION_AUGMENT,
                EVENT_SUBSCRIPTION_AUGMENT_REVISION);
        final Optional<DataContainerChild<? extends PathArgument, ?>> enumNode = value.getChild(new NodeIdentifier(
                QName.create(salRemoteAugment, paramName)));
        if (!enumNode.isPresent()) {
            return null;
        }
        final Object rawValue = enumNode.get().getValue();
        if (!(rawValue instanceof String)) {
            return null;
        }

        return resolveAsEnum(classDescriptor, (String) rawValue);
    }

    /**
     * Checks whether {@code value} is one of the string representation of enumeration {@code classDescriptor}
     *
     * @return enum object if string value of {@code classDescriptor} enumeration is equal to {@code value}. Other cases
     *         null.
     */
    private <T> T parserURIEnumParameter(final Class<T> classDescriptor, final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        return resolveAsEnum(classDescriptor, value);
    }

    private <T> T resolveAsEnum(final Class<T> classDescriptor, final String value) {
        final T[] enumConstants = classDescriptor.getEnumConstants();
        if (enumConstants != null) {
            for (final T enm : classDescriptor.getEnumConstants()) {
                if (((Enum<?>) enm).name().equals(value)) {
                    return enm;
                }
            }
        }
        return null;
    }

    private Map<String, String> resolveValuesFromUri(final String uri) {
        final Map<String, String> result = new HashMap<>();
        final String[] tokens = uri.split("/");
        for (int i = 1; i < tokens.length; i++) {
            final String[] parameterTokens = tokens[i].split("=");
            if (parameterTokens.length == 2) {
                result.put(parameterTokens[0], parameterTokens[1]);
            }
        }
        return result;
    }

    private boolean endsWithMountPoint(final String identifier) {
        return identifier.endsWith(ControllerContext.MOUNT) || identifier.endsWith(ControllerContext.MOUNT + "/");
    }

    private String addMountPointIdentifier(final String identifier) {
        final boolean endsWith = identifier.endsWith("/");
        if (endsWith) {
            return (identifier + ControllerContext.MOUNT);
        }

        return identifier + "/" + ControllerContext.MOUNT;
    }

    private TypeDef typeDefinition(final TypeDefinition<?> type, final QName nodeQName) {
        TypeDefinition<?> baseType = type;
        QName qName = nodeQName;
        while (baseType.getBaseType() != null) {
            if (baseType instanceof ExtendedType) {
                qName = baseType.getQName();
            }
            baseType = baseType.getBaseType();
        }

        return new TypeDef(baseType, qName);

    }

    private TypeDef typeDefinition(final DataSchemaNode node) {
        if (node instanceof LeafListSchemaNode) {
            return typeDefinition(((LeafListSchemaNode)node).getType(), node.getQName());
        } else if (node instanceof LeafSchemaNode) {
            return typeDefinition(((LeafSchemaNode)node).getType(), node.getQName());
        } else if (node instanceof AnyXmlSchemaNode) {
            return null;
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object> asList(node).toString());
        }
    }

    private InstanceIdentifierContext normalizeInstanceIdentifierWithSchemaNode(
            final InstanceIdentifierContext iiWithSchemaNode) {
        return normalizeInstanceIdentifierWithSchemaNode(iiWithSchemaNode, false);
    }

    private InstanceIdentifierContext normalizeInstanceIdentifierWithSchemaNode(
            final InstanceIdentifierContext iiWithSchemaNode, final boolean unwrapLastListNode) {
        return new InstanceIdentifierContext(instanceIdentifierToReadableFormForNormalizeNode(
                iiWithSchemaNode.getInstanceIdentifier(), unwrapLastListNode), iiWithSchemaNode.getSchemaNode(),
                iiWithSchemaNode.getMountPoint(),iiWithSchemaNode.getSchemaContext());
    }

    private YangInstanceIdentifier instanceIdentifierToReadableFormForNormalizeNode(
            final YangInstanceIdentifier instIdentifier, final boolean unwrapLastListNode) {
        Preconditions.checkNotNull(instIdentifier, "Instance identifier can't be null");
        final List<PathArgument> result = new ArrayList<PathArgument>();
        final Iterator<PathArgument> iter = instIdentifier.getPathArguments().iterator();
        while (iter.hasNext()) {
            final PathArgument pathArgument = iter.next();
            if (pathArgument instanceof NodeIdentifierWithPredicates && (iter.hasNext() || unwrapLastListNode)) {
                result.add(new YangInstanceIdentifier.NodeIdentifier(pathArgument.getNodeType()));
            }
            result.add(pathArgument);
        }
        return YangInstanceIdentifier.create(result);
    }

    private boolean isDataContainerNode(final DataSchemaNode schemaNode) {
        if (schemaNode instanceof ContainerSchemaNode || schemaNode instanceof ListSchemaNode) {
            return true;
        }
        return false;
    }

    public BigInteger getOperationalReceived() {
        // TODO Auto-generated method stub
        return null;
    }

    private MapNode makeModuleMapNode(final Set<Module> modules) {
        Preconditions.checkNotNull(modules);
        final Module restconfModule = getRestconfModule();
        final DataSchemaNode moduleSchemaNode = controllerContext.getRestconfModuleRestConfSchemaNode(
                restconfModule, Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);
        Preconditions.checkState(moduleSchemaNode instanceof ListSchemaNode);

        final CollectionNodeBuilder<MapEntryNode, MapNode> listModuleBuilder = Builders
                .mapBuilder((ListSchemaNode) moduleSchemaNode);

        for (final Module module : modules) {
            listModuleBuilder.withChild(toModuleEntryNode(module, moduleSchemaNode));
        }
        return listModuleBuilder.build();
    }

    protected MapEntryNode toModuleEntryNode(final Module module, final DataSchemaNode moduleSchemaNode) {
        Preconditions.checkArgument(moduleSchemaNode instanceof ListSchemaNode,
                "moduleSchemaNode has to be of type ListSchemaNode");
        final ListSchemaNode listModuleSchemaNode = (ListSchemaNode) moduleSchemaNode;
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> moduleNodeValues = Builders
                .mapEntryBuilder(listModuleSchemaNode);

        List<DataSchemaNode> instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listModuleSchemaNode), "name");
        final DataSchemaNode nameSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(nameSchemaNode instanceof LeafSchemaNode);
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode).withValue(module.getName())
                .build());

        instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listModuleSchemaNode), "revision");
        final DataSchemaNode revisionSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(revisionSchemaNode instanceof LeafSchemaNode);
        final String revision = REVISION_FORMAT.format(module.getRevision());
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) revisionSchemaNode).withValue(revision)
                .build());

        instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listModuleSchemaNode), "namespace");
        final DataSchemaNode namespaceSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(namespaceSchemaNode instanceof LeafSchemaNode);
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) namespaceSchemaNode)
                .withValue(module.getNamespace().toString()).build());

        instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listModuleSchemaNode), "feature");
        final DataSchemaNode featureSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(featureSchemaNode instanceof LeafListSchemaNode);
        final ListNodeBuilder<Object, LeafSetEntryNode<Object>> featuresBuilder = Builders
                .leafSetBuilder((LeafListSchemaNode) featureSchemaNode);
        for (final FeatureDefinition feature : module.getFeatures()) {
            featuresBuilder.withChild(Builders.leafSetEntryBuilder(((LeafListSchemaNode) featureSchemaNode))
                    .withValue(feature.getQName().getLocalName()).build());
        }
        moduleNodeValues.withChild(featuresBuilder.build());

        return moduleNodeValues.build();
    }

    protected MapEntryNode toStreamEntryNode(final String streamName, final DataSchemaNode streamSchemaNode) {
        Preconditions.checkArgument(streamSchemaNode instanceof ListSchemaNode,
                "streamSchemaNode has to be of type ListSchemaNode");
        final ListSchemaNode listStreamSchemaNode = (ListSchemaNode) streamSchemaNode;
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> streamNodeValues = Builders
                .mapEntryBuilder(listStreamSchemaNode);

        List<DataSchemaNode> instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listStreamSchemaNode), "name");
        final DataSchemaNode nameSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(nameSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode).withValue(streamName)
                .build());

        instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listStreamSchemaNode), "description");
        final DataSchemaNode descriptionSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(descriptionSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode)
                .withValue("DESCRIPTION_PLACEHOLDER").build());

        instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listStreamSchemaNode), "replay-support");
        final DataSchemaNode replaySupportSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(replaySupportSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) replaySupportSchemaNode)
                .withValue(Boolean.valueOf(true)).build());

        instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listStreamSchemaNode), "replay-log-creation-time");
        final DataSchemaNode replayLogCreationTimeSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(replayLogCreationTimeSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) replayLogCreationTimeSchemaNode)
                .withValue("").build());

        instanceDataChildrenByName = ControllerContext.findInstanceDataChildrenByName(
                (listStreamSchemaNode), "events");
        final DataSchemaNode eventsSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(eventsSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) eventsSchemaNode)
                .withValue("").build());

        return streamNodeValues.build();
    }
}
