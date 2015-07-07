/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.restconf.api.JSONRestconfService;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the JSONRestconfService interface.
 *
 * @author Thomas Pantelis
 */
public class JSONRestconfServiceImpl implements JSONRestconfService, AutoCloseable {
    private final static Logger LOG = LoggerFactory.getLogger(JSONRestconfServiceImpl.class);

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    @Override
    public void put(String uriPath, String payload) throws OperationFailedException {
        Preconditions.checkNotNull(payload, "payload can't be null");

        LOG.debug("put: uriPath: {}, payload: {}", uriPath, payload);

        InputStream entityStream = new ByteArrayInputStream(payload.getBytes(Charsets.UTF_8));
        NormalizedNodeContext context = JsonNormalizedNodeBodyReader.readFrom(uriPath, entityStream, false);

        LOG.debug("Parsed YangInstanceIdentifier: {}", context.getInstanceIdentifierContext().getInstanceIdentifier());
        LOG.debug("Parsed NormalizedNode: {}", context.getData());

        try {
            RestconfImpl.getInstance().updateConfigurationData(uriPath, context);
        } catch (Exception e) {
            propagateExceptionAs(uriPath, e, "PUT");
        }
    }

    @Override
    public void post(String uriPath, String payload) throws OperationFailedException {
        Preconditions.checkNotNull(payload, "payload can't be null");

        LOG.debug("post: uriPath: {}, payload: {}", uriPath, payload);

        InputStream entityStream = new ByteArrayInputStream(payload.getBytes(Charsets.UTF_8));
        NormalizedNodeContext context = JsonNormalizedNodeBodyReader.readFrom(uriPath, entityStream, true);

        LOG.debug("Parsed YangInstanceIdentifier: {}", context.getInstanceIdentifierContext().getInstanceIdentifier());
        LOG.debug("Parsed NormalizedNode: {}", context.getData());

        try {
            RestconfImpl.getInstance().createConfigurationData(uriPath, context, null);
        } catch (Exception e) {
            propagateExceptionAs(uriPath, e, "POST");
        }
    }

    @Override
    public void delete(String uriPath) throws OperationFailedException {
        LOG.debug("delete: uriPath: {}", uriPath);

        try {
            RestconfImpl.getInstance().deleteConfigurationData(uriPath);
        } catch (Exception e) {
            propagateExceptionAs(uriPath, e, "DELETE");
        }
    }

    @Override
    public Optional<String> get(String uriPath, LogicalDatastoreType datastoreType) throws OperationFailedException {
        LOG.debug("get: uriPath: {}", uriPath);

        try {
            NormalizedNodeContext readData;
            if(datastoreType == LogicalDatastoreType.CONFIGURATION) {
                readData = RestconfImpl.getInstance().readConfigurationData(uriPath, null);
            } else {
                readData = RestconfImpl.getInstance().readOperationalData(uriPath, null);
            }

            Optional<String> result = Optional.of(toJson(readData));

            LOG.debug("get returning: {}", result.get());

            return result;
        } catch (Exception e) {
            if(!isDataMissing(e)) {
                propagateExceptionAs(uriPath, e, "GET");
            }

            LOG.debug("Data missing - returning absent");
            return Optional.absent();
        }
    }

    @Override
    public Optional<String> invokeRpc(String uriPath, Optional<String> input) throws OperationFailedException {
        Preconditions.checkNotNull(uriPath, "uriPath can't be null");

        String actualInput = input.isPresent() ? input.get() : null;

        LOG.debug("invokeRpc: uriPath: {}, input: {}", uriPath, actualInput);

        String output = null;
        try {
            NormalizedNodeContext outputContext;
            if(actualInput != null) {
                InputStream entityStream = new ByteArrayInputStream(actualInput.getBytes(Charsets.UTF_8));
                NormalizedNodeContext inputContext = JsonNormalizedNodeBodyReader.readFrom(uriPath, entityStream, true);

                LOG.debug("Parsed YangInstanceIdentifier: {}", inputContext.getInstanceIdentifierContext()
                        .getInstanceIdentifier());
                LOG.debug("Parsed NormalizedNode: {}", inputContext.getData());

                outputContext = RestconfImpl.getInstance().invokeRpc(uriPath, inputContext, null);
            } else {
                outputContext = RestconfImpl.getInstance().invokeRpc(uriPath, "", null);
            }

            if(outputContext.getData() != null) {
                output = toJson(outputContext);
            }
        } catch (Exception e) {
            propagateExceptionAs(uriPath, e, "RPC");
        }

        return Optional.fromNullable(output);
    }

    @Override
    public void close() {
    }

    private String toJson(NormalizedNodeContext readData) throws IOException {
        NormalizedNodeJsonBodyWriter writer = new NormalizedNodeJsonBodyWriter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.writeTo(readData, NormalizedNodeContext.class, null, EMPTY_ANNOTATIONS,
                MediaType.APPLICATION_JSON_TYPE, null, outputStream );
        return outputStream.toString(Charsets.UTF_8.name());
    }

    private boolean isDataMissing(Exception e) {
        boolean dataMissing = false;
        if(e instanceof RestconfDocumentedException) {
            RestconfDocumentedException rde = (RestconfDocumentedException)e;
            if(!rde.getErrors().isEmpty()) {
                if(rde.getErrors().get(0).getErrorTag() == ErrorTag.DATA_MISSING) {
                    dataMissing = true;
                }
            }
        }

        return dataMissing;
    }

    private static void propagateExceptionAs(String uriPath, Exception e, String operation) throws OperationFailedException {
        LOG.debug("Error for uriPath: {}", uriPath, e);

        if(e instanceof RestconfDocumentedException) {
            throw new OperationFailedException(String.format("%s failed for URI %s", operation, uriPath), e.getCause(),
                    toRpcErrors(((RestconfDocumentedException)e).getErrors()));
        }

        throw new OperationFailedException(String.format("%s failed for URI %s", operation, uriPath), e);
    }

    private static RpcError[] toRpcErrors(List<RestconfError> from) {
        RpcError[] to = new RpcError[from.size()];
        int i = 0;
        for(RestconfError e: from) {
            to[i++] = RpcResultBuilder.newError(toRpcErrorType(e.getErrorType()), e.getErrorTag().getTagValue(),
                    e.getErrorMessage());
        }

        return to;
    }

    private static ErrorType toRpcErrorType(
            org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType errorType) {
        switch(errorType) {
            case TRANSPORT: {
                return ErrorType.TRANSPORT;
            }
            case RPC: {
                return ErrorType.RPC;
            }
            case PROTOCOL: {
                return ErrorType.PROTOCOL;
            }
            default: {
                return ErrorType.APPLICATION;
            }
        }
    }
}
