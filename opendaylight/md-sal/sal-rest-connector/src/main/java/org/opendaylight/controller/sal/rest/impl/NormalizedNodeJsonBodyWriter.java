/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Provider
@Produces({ Draft02.MediaTypes.API + RestconfService.JSON, Draft02.MediaTypes.DATA + RestconfService.JSON,
    Draft02.MediaTypes.OPERATION + RestconfService.JSON, MediaType.APPLICATION_JSON })
public class NormalizedNodeJsonBodyWriter implements MessageBodyWriter<NormalizedNodeContext> {

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return type.equals(NormalizedNodeContext.class);
    }

    @Override
    public long getSize(final NormalizedNodeContext t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final NormalizedNodeContext t, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
                    throws IOException, WebApplicationException {
        NormalizedNode<?, ?> data = t.getData();
        InstanceIdentifierContext context = t.getInstanceIdentifierContext();
        DataSchemaNode schema = context.getSchemaNode();
        SchemaPath path = context.getSchemaNode().getPath();
        OutputStreamWriter outputWriter = new OutputStreamWriter(entityStream, Charsets.UTF_8);
        if (data == null) {
            throw new RestconfDocumentedException(Response.Status.NOT_FOUND);
        }


        URI initialNs = null;
        outputWriter.write('{');
        if (!SchemaPath.ROOT.equals(path)) {
            path = path.getParent();
            // FIXME: Add proper handling of reading root.
        }
        if(data instanceof MapEntryNode) {
            data = ImmutableNodes.mapNodeBuilder(data.getNodeType()).withChild(((MapEntryNode) data)).build();
        }
        if(!schema.isAugmenting() && !(schema instanceof SchemaContext)) {
            initialNs = schema.getQName().getNamespace();
        }
        NormalizedNodeStreamWriter jsonWriter = JSONNormalizedNodeStreamWriter.create(context.getSchemaContext(),path,initialNs,outputWriter);
        NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(jsonWriter);
        nnWriter.write(data);
        nnWriter.flush();

        outputWriter.write('}');
        outputWriter.flush();
    }

}
