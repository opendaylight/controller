/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.collect.Iterables;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.data.impl.schema.ResultAlreadySetException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Consumes({ Draft02.MediaTypes.DATA + RestconfService.JSON, Draft02.MediaTypes.OPERATION + RestconfService.JSON,
        MediaType.APPLICATION_JSON })
public class JsonNormalizedNodeBodyReader extends AbstractIdentifierAwareJaxRsProvider implements MessageBodyReader<NormalizedNodeContext> {

    private final static Logger LOG = LoggerFactory.getLogger(JsonNormalizedNodeBodyReader.class);

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return true;
    }

    @Override
    public NormalizedNodeContext readFrom(final Class<NormalizedNodeContext> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
                    throws WebApplicationException, IOException {
        try {
            return readFrom(getInstanceIdentifierContext(), entityStream, isPost());
        } catch (final Exception e) {
            propagateExceptionAs(e);
            return null; // no-op
        }
    }

    private static void propagateExceptionAs(Exception e) throws RestconfDocumentedException {
        LOG.info("Error parsing json input", e);

        if(e instanceof RestconfDocumentedException) {
            throw (RestconfDocumentedException)e;
        }

        if(e instanceof ResultAlreadySetException) {
            throw new RestconfDocumentedException("Error parsing json input: Failed to create new parse result data. " +
                    "Are you creating multiple resources/subresources in POST request?");
        }

        throw new RestconfDocumentedException("Error parsing input: " + e.getMessage(), ErrorType.PROTOCOL,
                ErrorTag.MALFORMED_MESSAGE, e);
    }

    public static NormalizedNodeContext readFrom(final String uriPath, final InputStream entityStream,
            final boolean isPost) throws RestconfDocumentedException {

        try {
            return readFrom(ControllerContext.getInstance().toInstanceIdentifier(uriPath), entityStream, isPost);
        } catch (final Exception e) {
            propagateExceptionAs(e);
            return null; // no-op
        }
    }

    private static NormalizedNodeContext readFrom(final InstanceIdentifierContext<?> path, final InputStream entityStream,
            final boolean isPost) throws IOException {
        if (entityStream.available() < 1) {
            return new NormalizedNodeContext(path, null);
        }
        final NormalizedNodeResult resultHolder = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);

        final SchemaNode parentSchema;
        if(isPost) {
            // FIXME: We need dispatch for RPC.
            parentSchema = path.getSchemaNode();
        } else if(path.getSchemaNode() instanceof SchemaContext) {
            parentSchema = path.getSchemaContext();
        } else {
            if (SchemaPath.ROOT.equals(path.getSchemaNode().getPath().getParent())) {
                parentSchema = path.getSchemaContext();
            } else {
                parentSchema = SchemaContextUtil.findDataSchemaNode(path.getSchemaContext(), path.getSchemaNode().getPath().getParent());
            }
        }

        final JsonParserStream jsonParser = JsonParserStream.create(writer, path.getSchemaContext(), parentSchema);
        final JsonReader reader = new JsonReader(new InputStreamReader(entityStream));
        jsonParser.parse(reader);

        NormalizedNode<?, ?> result = resultHolder.getResult();
        final List<YangInstanceIdentifier.PathArgument> iiToDataList = new ArrayList<>();
        InstanceIdentifierContext<? extends SchemaNode> newIIContext;

        while (result instanceof AugmentationNode || result instanceof ChoiceNode) {
            final Object childNode = ((DataContainerNode) result).getValue().iterator().next();
            if (isPost) {
                iiToDataList.add(result.getIdentifier());
            }
            result = (NormalizedNode<?, ?>) childNode;
        }

        if (isPost) {
            if (result instanceof MapEntryNode) {
                iiToDataList.add(new YangInstanceIdentifier.NodeIdentifier(result.getNodeType()));
                iiToDataList.add(result.getIdentifier());
            } else {
                iiToDataList.add(result.getIdentifier());
            }
        } else {
            if (result instanceof MapNode) {
                result = Iterables.getOnlyElement(((MapNode) result).getValue());
            }
        }

        final YangInstanceIdentifier fullIIToData = YangInstanceIdentifier.create(Iterables.concat(
                path.getInstanceIdentifier().getPathArguments(), iiToDataList));

        newIIContext = new InstanceIdentifierContext<>(fullIIToData, path.getSchemaNode(), path.getMountPoint(),
                path.getSchemaContext());

        return new NormalizedNodeContext(newIIContext, result);
    }
}

