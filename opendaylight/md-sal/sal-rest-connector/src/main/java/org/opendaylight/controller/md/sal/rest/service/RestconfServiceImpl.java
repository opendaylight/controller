/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.net.URI;
import java.util.Map;
import java.util.Set;
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
import org.opendaylight.controller.md.sal.rest.RestConnectorProviderImpl;
import org.opendaylight.controller.md.sal.rest.common.RestconfInternalConstants;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
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
        final SchemaContext schemaContext = RestConnectorProviderImpl.getSchemaContext();
        final Set<Module> allModules = schemaContext.getModules();
        final ListSchemaNode mSchemaNode = RestConnectorProviderImpl.getSchemaMinder().getModuleListSchemaNode();
        final CollectionNodeBuilder<MapEntryNode, MapNode> listModuleBuilder = Builders.mapBuilder(mSchemaNode);

        for (final Module module : allModules) {
            listModuleBuilder.withChild(RestconfServiceUtils.toModuleEntryNode(module, mSchemaNode));
        }

        final ContainerSchemaNode msn = RestConnectorProviderImpl.getSchemaMinder().getModuleContainerSchemaNode();
        final ContainerNode modules = Builders.containerBuilder(msn).withChild(listModuleBuilder.build()).build();

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(null, msn, null, schemaContext), modules);
    }

    @Override
    public NormalizedNodeContext getModules(final String identifier, final UriInfo uriInfo) {
        Preconditions.checkNotNull(identifier);
        RestconfValidationUtils.checkDocumentedError(( ! identifier.contains(RestconfInternalConstants.MOUNT)),
                ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE, "URI has bad format. If modules behind mount point"
                        + " should be showed, URI has to end with " +RestconfInternalConstants.MOUNT);

        // FIXME : missing Restconf Request URI parser to mountpoint
        return null;
    }

    @Override
    public NormalizedNodeContext getModule(final String identifier, final UriInfo uriInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NormalizedNodeContext getOperations(final UriInfo uriInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NormalizedNodeContext getOperations(final String identifier, final UriInfo uriInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final NormalizedNodeContext payload, final UriInfo uriInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NormalizedNodeContext invokeRpc(final String identifier, final String noPayload, final UriInfo uriInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NormalizedNodeContext readConfigurationData(final String identifier, final UriInfo uriInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NormalizedNodeContext readOperationalData(final String identifier, final UriInfo uriInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Response updateConfigurationData(final String identifier, final NormalizedNodeContext payload) {
        RestconfValidationUtils.checkDocumentedError(payload != null, ErrorType.PROTOCOL,
                ErrorTag.MALFORMED_MESSAGE, "Input is required.");
        RestconfValidationUtils.checkDocumentedError(payload.getInstanceIdentifierContext() != null,
                ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE, "InstanceIdentifierContext is required.");

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
        // TODO Auto-generated method stub
        return null;
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
