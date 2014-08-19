/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Provider
@Produces({ Draft02.MediaTypes.API + RestconfService.XML, Draft02.MediaTypes.DATA + RestconfService.XML,
    Draft02.MediaTypes.OPERATION + RestconfService.XML, MediaType.APPLICATION_XML, MediaType.TEXT_XML })

public class NormalizedNodeXmlBodyWriter implements MessageBodyWriter<NormalizedNodeContext> {


    private static final XMLOutputFactory XML_FACTORY;

    static {
        XML_FACTORY  = XMLOutputFactory.newFactory();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }


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
        InstanceIdentifierContext pathContext = t.getInstanceIdentifierContext();
        if (t.getData() == null) {
            throw new RestconfDocumentedException(Response.Status.NOT_FOUND);
        }

        XMLStreamWriter xmlWriter;
        try {
            xmlWriter = XML_FACTORY.createXMLStreamWriter(entityStream);
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        } catch (FactoryConfigurationError e) {
            throw new IllegalStateException(e);
        }
        NormalizedNode<?, ?> data = t.getData();
        SchemaPath schemaPath = pathContext.getSchemaNode().getPath().getParent();
        if(data instanceof MapEntryNode) {
            data = ImmutableNodes.mapNodeBuilder(data.getNodeType()).addChild((MapEntryNode) data).build();
            //schemaPath = pathContext.getSchemaNode().getPath();
        }

        NormalizedNodeStreamWriter jsonWriter = XMLStreamNormalizedNodeStreamWriter.create(xmlWriter,pathContext.getSchemaContext(),schemaPath);
        NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(jsonWriter);

        nnWriter.write(data);
        nnWriter.flush();
    }
}
