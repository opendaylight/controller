/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.service;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.md.sal.rest.RestConnectorProviderImpl;
import org.opendaylight.controller.md.sal.rest.common.RestconfInternalConstants;
import org.opendaylight.controller.md.sal.rest.common.RestconfParsingUtils;
import org.opendaylight.controller.md.sal.rest.common.RestconfServiceUtils;
import org.opendaylight.controller.md.sal.rest.common.RestconfUriUtils;
import org.opendaylight.controller.md.sal.rest.common.RestconfValidationUtils;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModifiedNodeDoesNotExistException;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.service
 *
 * Implementation of {@link RestconfService}
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Feb 5, 2015
 */
public class RestconfServiceImpl implements RestconfService {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfServiceImpl.class);

    @Override
    public Object getRoot() {
        return null;
    }

    @Override
    public NormalizedNodeContext getModules(final UriInfo uriInfo) {
        final Set<Module> modules = RestConnectorProviderImpl.getSchemaContext().getModules();
        return buildModules(modules, null);
    }

    @Override
    public NormalizedNodeContext getModules(final String identifier, final UriInfo uriInfo) {
        Preconditions.checkNotNull(identifier);
        RestconfValidationUtils.checkDocumentedError(identifier.contains(RestconfInternalConstants.MOUNT),
                ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE, "URI has bad format. If modules behind mount point"
                        + " should be showed, URI has to end with " +RestconfInternalConstants.MOUNT);

        final DOMMountPoint mountPoint = RestConnectorProviderImpl.getSchemaMinder()
                .parseUriRequestToMountPoint(identifier);
        final SchemaContext schemaContext = mountPoint.getSchemaContext();
        final Set<Module> modules = schemaContext.getModules();
        return buildModules(modules, mountPoint);
    }

    @Override
    public NormalizedNodeContext getModule(final String identifier, final UriInfo uriInfo) {
        Preconditions.checkArgument(identifier != null);
        final QName moduleQName = RestconfParsingUtils.getModuleNameAndRevision(identifier);
        DOMMountPoint mountPoint = null;
        Module module = null;
        if (identifier.contains(RestconfInternalConstants.MOUNT)) {
            mountPoint = RestConnectorProviderImpl.getSchemaMinder().parseUriRequestToMountPoint(identifier);
            final SchemaContext mountPointSchemaCx = mountPoint.getSchemaContext();
            module = mountPointSchemaCx.findModuleByName(moduleQName.getLocalName(), moduleQName.getRevision());
        } else {
            module = RestConnectorProviderImpl.getSchemaContext()
                    .findModuleByName(moduleQName.getLocalName(), moduleQName.getRevision());
        }

        RestconfValidationUtils.checkDocumentedError(module != null, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT,
                "Module with name '" + moduleQName.getLocalName() + "' and revision '" + moduleQName.getRevision() + "' was not found.");

        final Set<Module> modules = Collections.singleton(module);

        return buildModules(modules, mountPoint);
    }

    private static NormalizedNodeContext buildModules(final Set<Module> modules, final DOMMountPoint mPoint) {
        final SchemaContext schemaContext = RestConnectorProviderImpl.getSchemaContext();
        final ListSchemaNode mSchemaNode = RestConnectorProviderImpl.getSchemaMinder().getModuleListSchemaNode();
        final CollectionNodeBuilder<MapEntryNode, MapNode> listModuleBuilder = Builders.mapBuilder(mSchemaNode);

        for (final Module module : modules) {
            listModuleBuilder.withChild(RestconfServiceUtils.toModuleEntryNode(module, mSchemaNode));
        }

        final ContainerSchemaNode msn = RestConnectorProviderImpl.getSchemaMinder().getModuleContainerSchemaNode();
        final ContainerNode mContainer = Builders.containerBuilder(msn).withChild(listModuleBuilder.build()).build();

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(null, msn, mPoint, schemaContext), mContainer);
    }

    @Override
    public NormalizedNodeContext getOperations(final UriInfo uriInfo) {
        final Set<Module> modules = RestConnectorProviderImpl.getSchemaContext().getModules();
        return operationsFromModulesToNormalizedContext(modules, null);
    }

    @Override
    public NormalizedNodeContext getOperations(final String identifier, final UriInfo uriInfo) {
        Preconditions.checkArgument(identifier != null);
        RestconfValidationUtils.checkDocumentedError(identifier.contains(RestconfInternalConstants.MOUNT), ErrorType.PROTOCOL,
                ErrorTag.INVALID_VALUE, "URI has bad format. If operations behind mount point should be showed, URI has to"
                        + " end with " + RestconfInternalConstants.MOUNT);
        final DOMMountPoint mountPoint = RestConnectorProviderImpl.getSchemaMinder().parseUriRequestToMountPoint(identifier);
        final Set<Module> modules = mountPoint.getSchemaContext().getModules();
        return operationsFromModulesToNormalizedContext(modules, mountPoint);
    }

    private static NormalizedNodeContext operationsFromModulesToNormalizedContext(final Set<Module> modules,
            final DOMMountPoint mountPoint) {
        // FIXME find best way to change restconf-netconf yang schema for provide this functionality
        final String errMsg = "We are not able support view operations functionality yet.";
        throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final NormalizedNodeContext payload, final UriInfo uriInfo) {
        Preconditions.checkArgument(identifier != null);
        Preconditions.checkArgument(payload != null);
        final SchemaPath type = payload.getInstanceIdentifierContext().getSchemaNode().getPath();
        final URI namespace = payload.getInstanceIdentifierContext().getSchemaNode().getQName().getNamespace();
        final DOMMountPoint mountPoint = payload.getInstanceIdentifierContext().getMountPoint();
        final CheckedFuture<DOMRpcResult, DOMRpcException> response;
        final SchemaContext schemaContext;
        if (mountPoint != null) {
            final Optional<DOMRpcService> mountRpcServices = mountPoint.getService(DOMRpcService.class);
            RestconfValidationUtils.checkDocumentedError(mountRpcServices.isPresent(), ErrorType.PROTOCOL,
                    ErrorTag.MISSING_ATTRIBUTE, "Rpc service is missing.");
            schemaContext = mountPoint.getSchemaContext();
            response = mountRpcServices.get().invokeRpc(type, payload.getData());
        } else {
            if (namespace.toString().equals(RestconfInternalConstants.SAL_REMOTE_NAMESPACE)) {
                response = invokeSalRemoteRpcSubscribeRPC(identifier, payload);
            } else {
                response = RestConnectorProviderImpl.getRestBroker().invokeRpc(type, payload.getData());
            }
            schemaContext = RestConnectorProviderImpl.getSchemaContext();
        }

        final DOMRpcResult result = checkRpcResponse(response);

        final RpcDefinition resultNodeSchema = (RpcDefinition) payload.getInstanceIdentifierContext().getSchemaNode();
        final NormalizedNode<?, ?> resultData = result.getResult();

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(
                null, resultNodeSchema, mountPoint, schemaContext), resultData);
    }

    private static CheckedFuture<DOMRpcResult, DOMRpcException> invokeSalRemoteRpcSubscribeRPC(
            final String identifier, final NormalizedNodeContext payload) {
        final RpcDefinition rpc = (RpcDefinition) payload.getInstanceIdentifierContext().getSchemaNode();
        final QName outputQname = QName.create(rpc.getQName(), "output");
        final QName streamNameQname = QName.create(rpc.getQName(), "stream-name");

        final String streamName = Notificator.createStreamNameFromUri(identifier);
        RestconfValidationUtils.checkDocumentedError(( ! Strings.isNullOrEmpty(streamName)), ErrorType.PROTOCOL,
                ErrorTag.INVALID_VALUE, "Path is empty or contains value node which is not Container or List build-in type.");

        if ( ! Notificator.existListenerFor(streamName)) {
            Notificator.createListener(payload.getInstanceIdentifierContext().getInstanceIdentifier(), streamName);
        }

        final ContainerNode output = ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(outputQname))
                .withChild(ImmutableNodes.leafNode(streamNameQname, identifier)).build();

        final DOMRpcResult defaultDOMRpcResult = new DefaultDOMRpcResult(output);

        return Futures.immediateCheckedFuture(defaultDOMRpcResult);
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final String noPayload, final UriInfo uriInfo) {
        Preconditions.checkArgument(identifier != null);
        RestconfValidationUtils.checkDocumentedError(Strings.isNullOrEmpty(noPayload), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                "Content must be empty.");
        final InstanceIdentifierContext<?> yiiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(identifier);
        Preconditions.checkState(yiiCx.getSchemaNode() instanceof RpcDefinition);
        final RpcDefinition rpc = (RpcDefinition) yiiCx.getSchemaNode();
        // TODO check error in specification
        RestconfValidationUtils.checkDocumentedError(rpc.getInput() == null, ErrorType.PROTOCOL, ErrorTag.MISSING_ATTRIBUTE,
                "RPC " + rpc + " expect input value.");
        final NormalizedNodeContext nnCx = new NormalizedNodeContext(yiiCx, null);
        return invokeRpc(identifier, nnCx, uriInfo);
    }

    private static DOMRpcResult checkRpcResponse(final CheckedFuture<DOMRpcResult, DOMRpcException> response) {
        RestconfValidationUtils.checkDocumentedError(response != null, ErrorType.RPC, ErrorTag.OPERATION_FAILED, "Rpc response was null.");
        try {
            final DOMRpcResult retValue = response.get();
            if (retValue.getErrors() == null || retValue.getErrors().isEmpty()) {
                return retValue;
            }
            throw new RestconfDocumentedException("RpcError message", null, retValue.getErrors());
        }
        catch (final InterruptedException e) {
            final String errMsg = "The operation was interrupted while executing and did not complete.";
            throw new RestconfDocumentedException(errMsg, ErrorType.RPC, ErrorTag.PARTIAL_OPERATION);
        }
        catch (final ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CancellationException) {
                final String errMsg = "The operation was cancelled while executing.";
                throw new RestconfDocumentedException(errMsg, ErrorType.RPC, ErrorTag.PARTIAL_OPERATION);
            } else if (cause != null) {
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                if (cause instanceof IllegalArgumentException) {
                    throw new RestconfDocumentedException(cause.getMessage(), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
                }
                throw new RestconfDocumentedException("The operation encountered an unexpected error while executing.", cause);
            } else {
                throw new RestconfDocumentedException("The operation encountered an unexpected error while executing.", e);
            }
        }
    }

    @Override
    public NormalizedNodeContext readConfigurationData(final String identifier, final UriInfo uriInfo) {
        final InstanceIdentifierContext<?> yiiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(identifier);
        final YangInstanceIdentifier yii = yiiCx.getInstanceIdentifier();
        final DOMMountPoint mountPoint = yiiCx.getMountPoint();
        NormalizedNode<?, ?> data = null;
        if (mountPoint != null) {
            data = RestConnectorProviderImpl.getRestBroker().readConfigurationData(mountPoint, yii);
        } else {
            data = RestConnectorProviderImpl.getRestBroker().readConfigurationData(yii);
        }
        RestconfValidationUtils.checkDocumentedError(data != null, ErrorType.APPLICATION, ErrorTag.DATA_MISSING,
                "Request could not be completed because the relevant data model content does not exist.");
        return new NormalizedNodeContext(yiiCx, data);
    }

    @Override
    public NormalizedNodeContext readOperationalData(final String identifier, final UriInfo uriInfo) {
        final InstanceIdentifierContext<?> yiiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(identifier);
        final YangInstanceIdentifier yii = yiiCx.getInstanceIdentifier();
        final DOMMountPoint mountPoint = yiiCx.getMountPoint();
        NormalizedNode<?, ?> data = null;
        if (mountPoint != null) {
            data = RestConnectorProviderImpl.getRestBroker().readOperationalData(mountPoint, yii);
        } else {
            data = RestConnectorProviderImpl.getRestBroker().readOperationalData(yii);
        }
        RestconfValidationUtils.checkDocumentedError(data != null, ErrorType.APPLICATION, ErrorTag.DATA_MISSING,
                "Request could not be completed because the relevant data model content does not exist.");
        return new NormalizedNodeContext(yiiCx, data);
    }

    @Override
    public Response updateConfigurationData(final String identifier, final NormalizedNodeContext payload) {
        RestconfValidationUtils.checkDocumentedError(payload != null, ErrorType.PROTOCOL,
                ErrorTag.MALFORMED_MESSAGE, "Input is required.");
        RestconfValidationUtils.checkDocumentedError(payload.getInstanceIdentifierContext() != null,
                ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE, "InstanceIdentifierContext is required.");

        // TODO do we need validation here ? If yes where we could put it (RestValidationUtil)

        final DOMMountPoint mountPoint = payload.getInstanceIdentifierContext().getMountPoint();
        final YangInstanceIdentifier yII = payload.getInstanceIdentifierContext().getInstanceIdentifier();

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
                    RestConnectorProviderImpl.getRestBroker().commitConfigurationDataPut(
                            mountPoint, yII, payload.getData()).checkedGet();
                } else {
                    RestConnectorProviderImpl.getRestBroker().commitConfigurationDataPut(
                            yII, payload.getData()).checkedGet();
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

    @Override
    public Response createConfigurationData(final String identifier, final NormalizedNodeContext payload, final UriInfo uriInfo) {
        return createConfigurationData(payload, uriInfo);
    }

    @Override
    public Response createConfigurationData(final NormalizedNodeContext payload, final UriInfo uriInfo) {
        RestconfValidationUtils.checkDocumentedError(payload != null, ErrorType.PROTOCOL,
                ErrorTag.MALFORMED_MESSAGE, "Input is required.");
        RestconfValidationUtils.checkDocumentedError(payload.getData() != null, ErrorType.PROTOCOL,
                ErrorTag.MALFORMED_MESSAGE, "Data input is required.");
        RestconfValidationUtils.checkDocumentedError(payload.getData().getNodeType().getNamespace() != null,
                ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE, "Data has bad format. Root element node must"
                        + " have namespace (XML format) or module name(JSON format)");

        final DOMMountPoint mountPoint = payload.getInstanceIdentifierContext().getMountPoint();
        final YangInstanceIdentifier normalizedII = payload.getInstanceIdentifierContext().getInstanceIdentifier();

        try {
            if (mountPoint != null) {
                RestConnectorProviderImpl.getRestBroker().commitConfigurationDataPost(
                        mountPoint, normalizedII, payload.getData()).checkedGet();
            } else {
                RestConnectorProviderImpl.getRestBroker().commitConfigurationDataPost(
                        normalizedII, payload.getData()).checkedGet();
            }
        } catch(final RestconfDocumentedException e) {
            throw e;
        } catch (final Exception e) {
            throw new RestconfDocumentedException("Error creating data", e);
        }

        final ResponseBuilder responseBuilder = Response.status(Status.NO_CONTENT);
        // FIXME: Provide path to result.
        final URI location = uriInfo.getRequestUri();
        if (location != null) {
            responseBuilder.location(location);
        }
        return responseBuilder.build();
    }

    @Override
    public Response deleteConfigurationData(final String identifier) {
        Preconditions.checkArgument(identifier != null);
        final InstanceIdentifierContext<?> yiiCx = RestConnectorProviderImpl.getSchemaMinder().parseUriRequest(identifier);
        final YangInstanceIdentifier yii = yiiCx.getInstanceIdentifier();
        final DOMMountPoint mountPoint = yiiCx.getMountPoint();
        try {
            if (mountPoint != null) {
                RestConnectorProviderImpl.getRestBroker().commitConfigurationDataDelete(mountPoint, yii);
            } else {
                RestConnectorProviderImpl.getRestBroker().commitConfigurationDataDelete(yii);
            }
        }
        catch (final Exception e) {
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
        RestconfValidationUtils.checkDocumentedError(( ! Strings.isNullOrEmpty(streamName)), ErrorType.PROTOCOL,
                ErrorTag.INVALID_VALUE, "Stream name is empty.");

        final ListenerAdapter listener = Notificator.getListenerFor(streamName);
        RestconfValidationUtils.checkDocumentedError(listener != null, ErrorType.PROTOCOL,
                ErrorTag.UNKNOWN_ELEMENT, "Stream was not found.");

        final Map<String, String> paramToValues = RestconfUriUtils.resolveValuesFromUri(identifier);
        final LogicalDatastoreType datastore = RestconfUriUtils.parserURIEnumParameter(
                LogicalDatastoreType.class, paramToValues.get(RestconfInternalConstants.DATASTORE_PARAM_NAME));
        RestconfValidationUtils.checkDocumentedError(datastore != null, ErrorType.APPLICATION,
                ErrorTag.MISSING_ATTRIBUTE, "Stream name doesn't contains datastore value (pattern /datastore=)");

        final DataChangeScope scope = RestconfUriUtils.parserURIEnumParameter(DataChangeScope.class,
                paramToValues.get(RestconfInternalConstants.SCOPE_PARAM_NAME));
        RestconfValidationUtils.checkDocumentedError(scope != null, ErrorType.APPLICATION,
                ErrorTag.MISSING_ATTRIBUTE, "Stream name doesn't contains datastore value (pattern /scope=)");

        RestConnectorProviderImpl.getRestBroker().registerToListenDataChanges(datastore, scope, listener);

        final UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        int notificationPort = RestconfInternalConstants.NOTIFICATION_PORT;
        try {
            final WebSocketServer webSocketServerInstance = WebSocketServer.getInstance();
            notificationPort = webSocketServerInstance.getPort();
        } catch (final NullPointerException e) {
            WebSocketServer.createInstance(RestconfInternalConstants.NOTIFICATION_PORT);
        }
        final UriBuilder uriToWebsocketServerBuilder = uriBuilder.port(notificationPort).scheme("ws");
        final URI uriToWebsocketServer = uriToWebsocketServerBuilder.replacePath(streamName).build();

        return Response.status(Status.OK).location(uriToWebsocketServer).build();
    }

    @Override
    public NormalizedNodeContext getAvailableStreams(final UriInfo uriInfo) {
        final SchemaContext schemaContext = RestConnectorProviderImpl.getSchemaContext();
        final Set<String> availableStreams = Notificator.getStreamNames();
        final ListSchemaNode listStream = RestConnectorProviderImpl.getSchemaMinder().getStreamListSchemaNode();
        final CollectionNodeBuilder<MapEntryNode, MapNode> lsBuilder = Builders.mapBuilder(listStream);
        for (final String streamName : availableStreams) {
            lsBuilder.withChild(RestconfServiceUtils.toStreamEntryNode(streamName, listStream));
        }

        final ContainerSchemaNode contStream = RestConnectorProviderImpl.getSchemaMinder().getStreamContainerSchemaNode();
        final ContainerNode resultContainer = Builders.containerBuilder(contStream).withChild(lsBuilder.build()).build();

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(null, contStream, null, schemaContext), resultContainer);
    }

}
