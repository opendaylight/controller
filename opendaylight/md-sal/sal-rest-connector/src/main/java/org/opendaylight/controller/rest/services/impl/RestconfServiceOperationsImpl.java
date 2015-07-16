/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.services.impl;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.rest.common.InstanceIdentifierContext;
import org.opendaylight.controller.rest.common.NormalizedNodeContext;
import org.opendaylight.controller.rest.common.QueryParametersParser;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaContext;
import org.opendaylight.controller.rest.connector.impl.RestSchemaContextImpl;
import org.opendaylight.controller.rest.errors.RestconfDocumentedException;
import org.opendaylight.controller.rest.errors.RestconfError.ErrorTag;
import org.opendaylight.controller.rest.errors.RestconfError.ErrorType;
import org.opendaylight.controller.rest.services.RestconfServiceOperations;
import org.opendaylight.controller.rest.streams.listeners.Notificator;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.EmptyType;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.opendaylight.yangtools.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestconfServiceOperationsImpl extends AbstractRestconfServiceImpl implements RestconfServiceOperations {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfServiceOperationsImpl.class);

    private static final String SAL_REMOTE_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote";
    private static final String MOUNT_POINT_MODULE_NAME = "ietf-netconf";
    private static final LogicalDatastoreType DEFAULT_DATASTORE = LogicalDatastoreType.CONFIGURATION;
    private static final DataChangeScope DEFAULT_SCOPE = DataChangeScope.BASE;
    private static final String DATASTORE_PARAM_NAME = "datastore";
    private static final String SCOPE_PARAM_NAME = "scope";
    private static final int CHAR_NOT_FOUND = -1;

    public RestconfServiceOperationsImpl(final RestBrokerFacade dataBroker, final RestSchemaContext schemaCx) {
        super(dataBroker, schemaCx);
    }

    @Override
    public NormalizedNodeContext getOperations(final UriInfo uriInfo) {
        final Set<Module> allModules = schemaCx.getAllModules();
        return operationsFromModulesToNormalizedContext(allModules, null);
    }

    @Override
    public NormalizedNodeContext getOperations(final String identifier, final UriInfo uriInfo) {
        Set<Module> modules = null;
        DOMMountPoint mountPoint = null;
        if (identifier.contains(RestSchemaContextImpl.MOUNT)) {
            final InstanceIdentifierContext<?> mountPointIdentifier = schemaCx.toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            modules = schemaCx.getAllModules(mountPoint);

        } else {
            final String errMsg = "URI has bad format. If operations behind mount point should be showed, URI has to end with ";
            LOG.debug(errMsg + RestSchemaContextImpl.MOUNT + " for " + identifier);
            throw new RestconfDocumentedException(errMsg + RestSchemaContextImpl.MOUNT, ErrorType.PROTOCOL,
                    ErrorTag.INVALID_VALUE);
        }

        return operationsFromModulesToNormalizedContext(modules, mountPoint);
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final NormalizedNodeContext payload,
            final UriInfo uriInfo) {
        final SchemaPath type = payload.getInstanceIdentifierContext().getSchemaNode().getPath();
        final URI namespace = payload.getInstanceIdentifierContext().getSchemaNode().getQName().getNamespace();
        final CheckedFuture<DOMRpcResult, DOMRpcException> response;
        final DOMMountPoint mountPoint = payload.getInstanceIdentifierContext().getMountPoint();
        final SchemaContext schemaContext;
        if (identifier.contains(MOUNT_POINT_MODULE_NAME) && mountPoint != null) {
            final Optional<DOMRpcService> mountRpcServices = mountPoint.getService(DOMRpcService.class);
            if (!mountRpcServices.isPresent()) {
                LOG.debug("Error: Rpc service is missing.");
                throw new RestconfDocumentedException("Rpc service is missing.");
            }
            schemaContext = mountPoint.getSchemaContext();
            response = mountRpcServices.get().invokeRpc(type, payload.getData());
        } else {
            if (namespace.toString().equals(SAL_REMOTE_NAMESPACE)) {
                response = invokeSalRemoteRpcSubscribeRPC(payload);
            } else {
                response = dataBroker.invokeRpc(type, payload.getData());
            }
            schemaContext = schemaCx.getGlobalSchema();
        }

        final DOMRpcResult result = checkRpcResponse(response);

        RpcDefinition resultNodeSchema = null;
        final NormalizedNode<?, ?> resultData = result.getResult();
        if (result != null && result.getResult() != null) {
            resultNodeSchema = (RpcDefinition) payload.getInstanceIdentifierContext().getSchemaNode();
        }

        return new NormalizedNodeContext(new InstanceIdentifierContext<RpcDefinition>(null, resultNodeSchema,
                mountPoint, schemaContext), resultData, QueryParametersParser.parseWriterParameters(uriInfo));
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final String noPayload, final UriInfo uriInfo) {
        if (StringUtils.isNotBlank(noPayload)) {
            throw new RestconfDocumentedException("Content must be empty.", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        String identifierEncoded = null;
        DOMMountPoint mountPoint = null;
        final SchemaContext schemaContext;
        if (identifier.contains(RestSchemaContextImpl.MOUNT)) {
            // mounted RPC call - look up mount instance.
            final InstanceIdentifierContext<?> mountPointId = schemaCx.toMountPointIdentifier(identifier);
            mountPoint = mountPointId.getMountPoint();
            schemaContext = mountPoint.getSchemaContext();
            final int startOfRemoteRpcName = identifier.lastIndexOf(RestSchemaContextImpl.MOUNT)
                    + RestSchemaContextImpl.MOUNT.length() + 1;
            final String remoteRpcName = identifier.substring(startOfRemoteRpcName);
            identifierEncoded = remoteRpcName;

        } else if (identifier.indexOf("/") != CHAR_NOT_FOUND) {
            final String slashErrorMsg = String.format("Identifier %n%s%ncan\'t contain slash "
                    + "character (/).%nIf slash is part of identifier name then use %%2F placeholder.", identifier);
            LOG.debug(slashErrorMsg);
            throw new RestconfDocumentedException(slashErrorMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        } else {
            identifierEncoded = identifier;
            schemaContext = schemaCx.getGlobalSchema();
        }

        final String identifierDecoded = schemaCx.urlPathArgDecode(identifierEncoded);

        RpcDefinition rpc = null;
        if (mountPoint == null) {
            rpc = schemaCx.getRpcDefinition(identifierDecoded);
        } else {
            rpc = findRpc(mountPoint.getSchemaContext(), identifierDecoded);
        }

        if (rpc == null) {
            LOG.debug("RPC " + identifierDecoded + " does not exist.");
            throw new RestconfDocumentedException("RPC does not exist.", ErrorType.RPC, ErrorTag.UNKNOWN_ELEMENT);
        }

        if (rpc.getInput() != null) {
            LOG.debug("RPC " + rpc + " does not need input value.");
            // FIXME : find a correct Error from specification
            throw new IllegalStateException("RPC " + rpc + " does'n need input value!");
        }

        final CheckedFuture<DOMRpcResult, DOMRpcException> response;
        if (mountPoint != null) {
            final Optional<DOMRpcService> mountRpcServices = mountPoint.getService(DOMRpcService.class);
            if (!mountRpcServices.isPresent()) {
                throw new RestconfDocumentedException("Rpc service is missing.");
            }
            response = mountRpcServices.get().invokeRpc(rpc.getPath(), null);
        } else {
            response = dataBroker.invokeRpc(rpc.getPath(), null);
        }

        final DOMRpcResult result = checkRpcResponse(response);

        DataSchemaNode resultNodeSchema = null;
        NormalizedNode<?, ?> resultData = null;
        if (result != null && result.getResult() != null) {
            resultData = result.getResult();
            final ContainerSchemaNode rpcDataSchemaNode = SchemaContextUtil.getRpcDataSchema(schemaContext, rpc
                    .getOutput().getPath());
            resultNodeSchema = rpcDataSchemaNode.getDataChildByName(result.getResult().getNodeType());
        }

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(null, resultNodeSchema, mountPoint,
                schemaContext), resultData, QueryParametersParser.parseWriterParameters(uriInfo));
    }

    private static final Predicate<GroupingBuilder> GROUPING_FILTER = new Predicate<GroupingBuilder>() {
        @Override
        public boolean apply(final GroupingBuilder g) {
            return Draft02.RestConfModule.RESTCONF_GROUPING_SCHEMA_NODE.equals(g.getQName().getLocalName());
        }
    };

    private NormalizedNodeContext operationsFromModulesToNormalizedContext(final Set<Module> modules,
            final DOMMountPoint mountPoint) {

        final Module restconfModule = getRestconfModule();
        final ModuleBuilder restConfModuleBuilder = new ModuleBuilder(restconfModule);
        final Set<GroupingBuilder> gropingBuilders = restConfModuleBuilder.getGroupingBuilders();
        final Iterable<GroupingBuilder> filteredGroups = Iterables.filter(gropingBuilders, GROUPING_FILTER);
        final GroupingBuilder restconfGroupingBuilder = Iterables.getFirst(filteredGroups, null);
        final ContainerSchemaNodeBuilder restContainerSchemaNodeBuilder = (ContainerSchemaNodeBuilder) restconfGroupingBuilder
                .getDataChildByName(Draft02.RestConfModule.RESTCONF_CONTAINER_SCHEMA_NODE);
        final ContainerSchemaNodeBuilder containerSchemaNodeBuilder = (ContainerSchemaNodeBuilder) restContainerSchemaNodeBuilder
                .getDataChildByName(Draft02.RestConfModule.OPERATIONS_CONTAINER_SCHEMA_NODE);

        final ContainerSchemaNodeBuilder fakeOperationsSchemaNodeBuilder = containerSchemaNodeBuilder;
        final SchemaPath fakeSchemaPath = fakeOperationsSchemaNodeBuilder.getPath().createChild(QName.create("dummy"));

        final List<LeafNode<Object>> operationsAsData = new ArrayList<>();

        for (final Module module : modules) {
            final Set<RpcDefinition> rpcs = module.getRpcs();
            for (final RpcDefinition rpc : rpcs) {
                final QName rpcQName = rpc.getQName();
                final String name = module.getName();

                final QName qName = QName.create(restconfModule.getQNameModule(), rpcQName.getLocalName());
                final LeafSchemaNodeBuilder leafSchemaNodeBuilder = new LeafSchemaNodeBuilder(name, 0, qName,
                        fakeSchemaPath);
                final LeafSchemaNodeBuilder fakeRpcSchemaNodeBuilder = leafSchemaNodeBuilder;
                fakeRpcSchemaNodeBuilder.setAugmenting(true);

                final EmptyType instance = EmptyType.getInstance();
                fakeRpcSchemaNodeBuilder.setType(instance);
                final LeafSchemaNode fakeRpcSchemaNode = fakeRpcSchemaNodeBuilder.build();
                fakeOperationsSchemaNodeBuilder.addChildNode(fakeRpcSchemaNode);

                final LeafNode<Object> leaf = Builders.leafBuilder(fakeRpcSchemaNode).build();
                operationsAsData.add(leaf);
            }
        }

        final ContainerSchemaNode operContainerSchemaNode = fakeOperationsSchemaNodeBuilder.build();
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> operContainerNode = Builders
                .containerBuilder(operContainerSchemaNode);

        for (final LeafNode<Object> oper : operationsAsData) {
            operContainerNode.withChild(oper);
        }

        final Set<Module> fakeRpcModules = Collections.singleton(restConfModuleBuilder.build());

        final YangParserImpl yangParser = new YangParserImpl();
        final SchemaContext fakeSchemaCx = yangParser.resolveSchemaContext(fakeRpcModules);

        final InstanceIdentifierContext<?> fakeIICx = new InstanceIdentifierContext<>(null, operContainerSchemaNode,
                mountPoint, fakeSchemaCx);

        return new NormalizedNodeContext(fakeIICx, operContainerNode.build());
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> invokeSalRemoteRpcSubscribeRPC(
            final NormalizedNodeContext payload) {
        final ContainerNode value = (ContainerNode) payload.getData();
        final QName rpcQName = payload.getInstanceIdentifierContext().getSchemaNode().getQName();
        final Optional<DataContainerChild<? extends PathArgument, ?>> path = value.getChild(new NodeIdentifier(QName
                .create(payload.getInstanceIdentifierContext().getSchemaNode().getQName(), "path")));
        final Object pathValue = path.isPresent() ? path.get().getValue() : null;

        if (!(pathValue instanceof YangInstanceIdentifier)) {
            final String errMsg = "Instance identifier was not normalized correctly ";
            LOG.debug(errMsg + rpcQName);
            throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED);
        }

        final YangInstanceIdentifier pathIdentifier = ((YangInstanceIdentifier) pathValue);
        String streamName = null;
        if (!pathIdentifier.isEmpty()) {
            final String fullRestconfIdentifier = schemaCx.toFullRestconfIdentifier(pathIdentifier, null);

            LogicalDatastoreType datastore = parseEnumTypeParameter(value, LogicalDatastoreType.class,
                    DATASTORE_PARAM_NAME);
            datastore = datastore == null ? DEFAULT_DATASTORE : datastore;

            DataChangeScope scope = parseEnumTypeParameter(value, DataChangeScope.class, SCOPE_PARAM_NAME);
            scope = scope == null ? DEFAULT_SCOPE : scope;

            streamName = Notificator.createStreamNameFromUri(fullRestconfIdentifier + "/datastore=" + datastore
                    + "/scope=" + scope);
        }

        if (Strings.isNullOrEmpty(streamName)) {
            final String errMsg = "Path is empty or contains value node which is not Container or List build-in type.";
            LOG.debug(errMsg + pathIdentifier);
            throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        final QName outputQname = QName.create(rpcQName, "output");
        final QName streamNameQname = QName.create(rpcQName, "stream-name");

        final ContainerNode output = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(outputQname))
                .withChild(ImmutableNodes.leafNode(streamNameQname, streamName)).build();

        if (!Notificator.existListenerFor(streamName)) {
            Notificator.createListener(pathIdentifier, streamName, schemaCx);
        }

        final DOMRpcResult defaultDOMRpcResult = new DefaultDOMRpcResult(output);

        return Futures.immediateCheckedFuture(defaultDOMRpcResult);
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
        final Optional<DataContainerChild<? extends PathArgument, ?>> augNode = value
                .getChild(SAL_REMOTE_AUG_IDENTIFIER);
        if (!augNode.isPresent() && !(augNode instanceof AugmentationNode)) {
            return null;
        }
        final Optional<DataContainerChild<? extends PathArgument, ?>> enumNode = ((AugmentationNode) augNode.get())
                .getChild(new NodeIdentifier(QName.create(SAL_REMOTE_AUGMENT, paramName)));
        if (!enumNode.isPresent()) {
            return null;
        }
        final Object rawValue = enumNode.get().getValue();
        if (!(rawValue instanceof String)) {
            return null;
        }

        return resolveAsEnum(classDescriptor, (String) rawValue);
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
            LOG.debug("RpcError message", retValue.getErrors());
            throw new RestconfDocumentedException("RpcError message", null, retValue.getErrors());
        } catch (final InterruptedException e) {
            final String errMsg = "The operation was interrupted while executing and did not complete.";
            LOG.debug("Rpc Interrupt - " + errMsg, e);
            throw new RestconfDocumentedException(errMsg, ErrorType.RPC, ErrorTag.PARTIAL_OPERATION);
        } catch (final ExecutionException e) {
            LOG.debug("Execution RpcError: ", e);
            Throwable cause = e.getCause();
            if (cause != null) {
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
        } catch (final CancellationException e) {
            final String errMsg = "The operation was cancelled while executing.";
            LOG.debug("Cancel RpcExecution: " + errMsg, e);
            throw new RestconfDocumentedException(errMsg, ErrorType.RPC, ErrorTag.PARTIAL_OPERATION);
        }
    }

    private RpcDefinition findRpc(final SchemaContext schemaContext, final String identifierDecoded) {
        final String[] splittedIdentifier = identifierDecoded.split(":");
        if (splittedIdentifier.length != 2) {
            final String errMsg = identifierDecoded + " couldn't be splitted to 2 parts (module:rpc name)";
            LOG.debug(errMsg);
            throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.INVALID_VALUE);
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
}
