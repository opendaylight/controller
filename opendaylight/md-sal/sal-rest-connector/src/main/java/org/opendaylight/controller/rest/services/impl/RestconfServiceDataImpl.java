/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.services.impl;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.rest.common.InstanceIdentifierContext;
import org.opendaylight.controller.rest.common.NormalizedNodeContext;
import org.opendaylight.controller.rest.common.QueryParametersParser;
import org.opendaylight.controller.rest.common.RestconfValidationUtils;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaController;
import org.opendaylight.controller.rest.errors.RestconfDocumentedException;
import org.opendaylight.controller.rest.errors.RestconfError.ErrorTag;
import org.opendaylight.controller.rest.errors.RestconfError.ErrorType;
import org.opendaylight.controller.rest.services.RestconfServiceData;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModifiedNodeDoesNotExistException;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * Implementation of {@link RestconfServiceData}
 */
public class RestconfServiceDataImpl extends AbstractRestconfServiceImpl implements RestconfServiceData {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfServiceDataImpl.class);

    /**
     * Default constructor extends from {@link AbstractRestconfServiceImpl}
     *
     * @param dataBroker
     * @param schemaCx
     */
    public RestconfServiceDataImpl(final RestBrokerFacade dataBroker, final RestSchemaController schemaCx) {
        super(dataBroker, schemaCx);
    }

    @Override
    public NormalizedNodeContext readConfigurationData(final String identifier, final UriInfo uriInfo) {
        final InstanceIdentifierContext<?> iiWithData = schemaController.toInstanceIdentifier(identifier);
        final DOMMountPoint mountPoint = iiWithData.getMountPoint();
        NormalizedNode<?, ?> data = null;
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();
        if (mountPoint != null) {
            data = broker.readConfigurationData(mountPoint, normalizedII);
        } else {
            data = broker.readConfigurationData(normalizedII);
        }
        if (data == null) {
            final String errMsg = "Request could not be completed because the relevant data model content does not exist ";
            LOG.debug(errMsg + identifier);
            throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.DATA_MISSING);
        }
        return new NormalizedNodeContext(iiWithData, data, QueryParametersParser.parseWriterParameters(uriInfo));
    }

    @Override
    public NormalizedNodeContext readOperationalData(final String identifier, final UriInfo uriInfo) {
        final InstanceIdentifierContext<?> iiWithData = schemaController.toInstanceIdentifier(identifier);
        final DOMMountPoint mountPoint = iiWithData.getMountPoint();
        NormalizedNode<?, ?> data = null;
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();
        if (mountPoint != null) {
            data = broker.readOperationalData(mountPoint, normalizedII);
        } else {
            data = broker.readOperationalData(normalizedII);
        }
        if (data == null) {
            final String errMsg = "Request could not be completed because the relevant data model content does not exist ";
            LOG.debug(errMsg + identifier);
            throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.DATA_MISSING);
        }
        return new NormalizedNodeContext(iiWithData, data, QueryParametersParser.parseWriterParameters(uriInfo));
    }

    @Override
    public Response updateConfigurationData(final String identifier, final NormalizedNodeContext payload) {
        Preconditions.checkNotNull(identifier);
        final InstanceIdentifierContext<?> iiWithData = payload.getInstanceIdentifierContext();

        validateInput(iiWithData.getSchemaNode(), payload);
        validateTopLevelNodeName(payload, iiWithData.getInstanceIdentifier());
        validateListKeysEqualityInPayloadAndUri(payload);

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
        while (true) {
            try {
                if (mountPoint != null) {
                    broker.commitConfigurationDataPut(mountPoint, normalizedII, payload.getData()).checkedGet();
                } else {
                    broker.commitConfigurationDataPut(schemaController.getGlobalSchema(), normalizedII,
                            payload.getData()).checkedGet();
                }

                break;
            } catch (final TransactionCommitFailedException e) {
                if (e instanceof OptimisticLockFailedException) {
                    if (--tries <= 0) {
                        LOG.debug("Got OptimisticLockFailedException on last try - failing " + identifier);
                        throw new RestconfDocumentedException(e.getMessage(), e, e.getErrorList());
                    }

                    LOG.debug("Got OptimisticLockFailedException - trying again " + identifier);
                } else {
                    LOG.debug("Update ConfigDataStore fail " + identifier, e);
                    throw new RestconfDocumentedException(e.getMessage(), e, e.getErrorList());
                }
            }
        }

        return Response.status(Status.OK).build();
    }

    @Override
    public Response createConfigurationData(final String identifier, final NormalizedNodeContext payload,
            final UriInfo uriInfo) {
        return createConfigurationData(payload, uriInfo);
    }

    @Override
    public Response createConfigurationData(final NormalizedNodeContext payload, final UriInfo uriInfo) {
        if (payload == null) {
            throw new RestconfDocumentedException("Input is required.", ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
        }

        // FIXME: move this to parsing stage (we can have augmentation nodes here which do not have namespace)
        // final URI payloadNS = payload.getData().getNodeType().getNamespace();
        // if (payloadNS == null) {
        // throw new RestconfDocumentedException(
        // "Data has bad format. Root element node must have namespace (XML format) or module name(JSON format)",
        // ErrorType.PROTOCOL, ErrorTag.UNKNOWN_NAMESPACE);
        // }

        final DOMMountPoint mountPoint = payload.getInstanceIdentifierContext().getMountPoint();
        final InstanceIdentifierContext<?> iiWithData = payload.getInstanceIdentifierContext();
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();
        try {
            if (mountPoint != null) {
                broker.commitConfigurationDataPost(mountPoint, normalizedII, payload.getData()).checkedGet();
            } else {
                broker.commitConfigurationDataPost(schemaController.getGlobalSchema(), normalizedII,
                        payload.getData()).checkedGet();
            }
        } catch (final RestconfDocumentedException e) {
            throw e;
        } catch (final Exception e) {
            final String errMsg = "Error creating data ";
            LOG.info(errMsg + uriInfo.getPath(), e);
            throw new RestconfDocumentedException(errMsg, e);
        }

        final ResponseBuilder responseBuilder = Response.status(Status.NO_CONTENT);
        // FIXME: Provide path to result.
        final URI location = resolveLocation(uriInfo, "", mountPoint, normalizedII);
        if (location != null) {
            responseBuilder.location(location);
        }
        return responseBuilder.build();
    }

    @Override
    public Response patchConfigurationData(final String identifier, final NormalizedNodeContext payload) {

        final InstanceIdentifierContext<?> iiWithData = payload.getInstanceIdentifierContext();
        final YangInstanceIdentifier normalizedII = iiWithData.getInstanceIdentifier();

        if (broker.readConfigurationData(normalizedII) == payload.getData()) {
            throw new RestconfDocumentedException("Conflict.", ErrorType.PROTOCOL, ErrorTag.LOCK_DENIED);
        }

        validateInput(iiWithData.getSchemaNode(), payload);
        validateTopLevelNodeName(payload, iiWithData.getInstanceIdentifier());
        validateListKeysEqualityInPayloadAndUri(payload);

        final DOMMountPoint mountPoint = iiWithData.getMountPoint();
        try {
            if (mountPoint == null) {
                broker.commitConfigurationDataPatch(schemaController.getGlobalSchema(), normalizedII,
                        payload.getData()).checkedGet();
            } else {
                broker.commitConfigurationDataPatch(mountPoint, normalizedII, payload.getData()).checkedGet();
            }
        } catch (final TransactionCommitFailedException e) {
            LOG.debug("Patch ConfigDataStore fail " + identifier, e);
            throw new RestconfDocumentedException(e.getMessage(), e, e.getErrorList());
        }

        return Response.status(Status.OK).build();
    }

    @Override
    public Response deleteConfigurationData(final String identifier) {
        final InstanceIdentifierContext<?> iiWithData = schemaController.toInstanceIdentifier(identifier);
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
                throw new RestconfDocumentedException("Data specified for deleting doesn't exist.",
                        ErrorType.APPLICATION, ErrorTag.DATA_MISSING);
            }
            final String errMsg = "Error while deleting data";
            LOG.info(errMsg, e);
            throw new RestconfDocumentedException(errMsg, e);
        }
        return Response.status(Status.OK).build();
    }

    private URI resolveLocation(final UriInfo uriInfo, final String uriBehindBase, final DOMMountPoint mountPoint,
            final YangInstanceIdentifier normalizedII) {
        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("config");
        try {
            uriBuilder.path(schemaController.toFullRestconfIdentifier(normalizedII, mountPoint));
        } catch (final Exception e) {
            LOG.info("Location for instance identifier" + normalizedII + "wasn't created", e);
            return null;
        }
        return uriBuilder.build();
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

    private void validateTopLevelNodeName(final NormalizedNodeContext node, final YangInstanceIdentifier identifier) {

        final String payloadName = node.getData().getNodeType().getLocalName();

        // no arguments
        if (identifier.isEmpty()) {
            // no "data" payload
            if (!node.getData().getNodeType().equals(NETCONF_BASE_QNAME)) {
                throw new RestconfDocumentedException("Instance identifier has to contain at least one path argument",
                        ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
            }
            // any arguments
        } else {
            final String identifierName = identifier.getLastPathArgument().getNodeType().getLocalName();
            if (!payloadName.equals(identifierName)) {
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
    private static void validateListKeysEqualityInPayloadAndUri(final NormalizedNodeContext payload) {
        Preconditions.checkArgument(payload != null);
        final InstanceIdentifierContext<?> iiWithData = payload.getInstanceIdentifierContext();
        final PathArgument lastPathArgument = iiWithData.getInstanceIdentifier().getLastPathArgument();
        final SchemaNode schemaNode = iiWithData.getSchemaNode();
        final NormalizedNode<?, ?> data = payload.getData();
        if (schemaNode instanceof ListSchemaNode) {
            final List<QName> keyDefinitions = ((ListSchemaNode) schemaNode).getKeyDefinition();
            if (lastPathArgument instanceof NodeIdentifierWithPredicates && data instanceof MapEntryNode) {
                final Map<QName, Object> uriKeyValues = ((NodeIdentifierWithPredicates) lastPathArgument)
                        .getKeyValues();
                isEqualUriAndPayloadKeyValues(uriKeyValues, (MapEntryNode) data, keyDefinitions);
            }
        }
    }

    private static void isEqualUriAndPayloadKeyValues(final Map<QName, Object> uriKeyValues,
            final MapEntryNode payload, final List<QName> keyDefinitions) {

        final Map<QName, Object> mutableCopyUriKeyValues = Maps.newHashMap(uriKeyValues);
        for (final QName keyDefinition : keyDefinitions) {
            final Object uriKeyValue = mutableCopyUriKeyValues.remove(keyDefinition);
            // should be caught during parsing URI to InstanceIdentifier
            RestconfValidationUtils.checkDocumentedError(uriKeyValue != null, ErrorType.PROTOCOL,
                    ErrorTag.DATA_MISSING, "Missing key " + keyDefinition + " in URI.");

            final Object dataKeyValue = payload.getIdentifier().getKeyValues().get(keyDefinition);

            if (!uriKeyValue.equals(dataKeyValue)) {
                final String errMsg = "The value '" + uriKeyValue + "' for key '" + keyDefinition.getLocalName()
                        + "' specified in the URI doesn't match the value '" + dataKeyValue
                        + "' specified in the message body. ";
                throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
            }
        }
    }
}
