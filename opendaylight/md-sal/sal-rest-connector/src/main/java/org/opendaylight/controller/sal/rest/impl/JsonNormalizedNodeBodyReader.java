/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Optional;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
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
            final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream) throws IOException,
            WebApplicationException {
        try {
            Optional<InstanceIdentifierContext> path = getIdentifierWithSchema();
            NormalizedNodeResult resultHolder = new NormalizedNodeResult();
            NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
            JsonParserStream jsonParser = JsonParserStream.create(writer, path.get().getSchemaContext(), parentSchemaNode(path));
            JsonReader reader = new JsonReader(new InputStreamReader(entityStream));
            jsonParser.parse(reader);
            return new NormalizedNodeContext(path.get(),resultHolder.getResult());
        } catch (Exception e) {
            LOG.debug("Error parsing json input", e);

            throw new RestconfDocumentedException("Error parsing input: " + e.getMessage(), ErrorType.PROTOCOL,
                    ErrorTag.MALFORMED_MESSAGE);
        }
    }

    /**
     * This method resolve parent schema node or schema context if schema node from {@code path} is top level
     * @param path
     * @return
     */
    private SchemaNode parentSchemaNode(final Optional<InstanceIdentifierContext> path) {
        final SchemaPath currentNodeParentPath = path.get().getSchemaNode().getPath().getParent();
        if (currentNodeParentPath.getPathFromRoot().iterator().hasNext()) {
            return SchemaContextUtil.findDataSchemaNode(path.get().getSchemaContext(), currentNodeParentPath);
        }
        return path.get().getSchemaContext();
    }
}

