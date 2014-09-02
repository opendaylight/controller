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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
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
        SchemaPath path = context.getSchemaNode().getPath();
        OutputStreamWriter outputWriter = new OutputStreamWriter(entityStream, Charsets.UTF_8);
        if (data == null) {
            throw new RestconfDocumentedException(Response.Status.NOT_FOUND);
        }

        outputWriter.write('{');
        if (SchemaPath.ROOT.equals(path)) {
            // FIXME: Add proper handling of reading root.
        } else if (data instanceof MapEntryNode) {
            writeMapEntryStart(outputWriter,context.getSchemaNode(),context.getSchemaContext());
        } else if (data instanceof ContainerNode) {
            path = path.getParent();
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + data.getClass());
        }

        NormalizedNodeStreamWriter jsonWriter = JSONNormalizedNodeStreamWriter.create(context.getSchemaContext(),path,outputWriter);
        NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(jsonWriter);
        nnWriter.write(data);
        nnWriter.flush();

        outputWriter.write('}');
        outputWriter.flush();
    }

    private void writeMapEntryStart(OutputStreamWriter writer, DataSchemaNode dataSchemaNode, SchemaContext schemaContext) throws IOException {
        QName qname = dataSchemaNode.getQName();
        writer.write('"');
        /*
         * FIXME: JSON Draft allows both forms (e.g. moduleName:localName and localName) but module name is
         * preffered. Commented out code used preffered way, but as it turned out there are some users
         * out there,which requires original behaviour, so we will print fully qualified name
         * only for root JSON node which was added by augmenation.
         */
        if(dataSchemaNode.isAugmenting()) {
            Module module = schemaContext.findModuleByNamespaceAndRevision(qname.getNamespace(), qname.getRevision());
            writer.write(module.getName());
            writer.write(':');
        }
        writer.write(qname.getLocalName());
        writer.write('"');
        writer.write(':');
    }

}
