/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javanet.staxutils.IndentingXMLStreamWriter;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.XMLConstants;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Provider
@Produces({ Draft02.MediaTypes.API + RestconfService.XML, Draft02.MediaTypes.DATA + RestconfService.XML,
        Draft02.MediaTypes.OPERATION + RestconfService.XML, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public class NormalizedNodeXmlBodyWriter implements MessageBodyWriter<NormalizedNodeContext> {

    private static final XMLOutputFactory XML_FACTORY;

    static {
        XML_FACTORY = XMLOutputFactory.newFactory();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return type.equals(NormalizedNodeContext.class);
    }

    @Override
    public long getSize(final NormalizedNodeContext t, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final NormalizedNodeContext t, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws IOException,
            WebApplicationException {
        final InstanceIdentifierContext<?> pathContext = t.getInstanceIdentifierContext();
        if (t.getData() == null) {
            return;
        }

        XMLStreamWriter xmlWriter;
        try {
            xmlWriter = XML_FACTORY.createXMLStreamWriter(entityStream);
            if (t.isPrettyPrint()) {
                xmlWriter = new IndentingXMLStreamWriter(xmlWriter);
            }
        } catch (final XMLStreamException e) {
            throw new IllegalStateException(e);
        } catch (final FactoryConfigurationError e) {
            throw new IllegalStateException(e);
        }
        NormalizedNode<?, ?> data = t.getData();
        SchemaPath schemaPath = pathContext.getSchemaNode().getPath();



        writeNormalizedNode(xmlWriter,schemaPath,pathContext,data);
    }

    private void writeNormalizedNode(XMLStreamWriter xmlWriter, SchemaPath schemaPath,InstanceIdentifierContext<?> pathContext, NormalizedNode<?, ?> data) throws IOException {
        final NormalizedNodeWriter nnWriter;
        final SchemaContext schemaCtx = pathContext.getSchemaContext();
        if (SchemaPath.ROOT.equals(schemaPath)) {
            nnWriter = createNormalizedNodeWriter(xmlWriter, schemaCtx, schemaPath);
            writeElements(xmlWriter, nnWriter, (ContainerNode) data);
        }  else if (pathContext.getSchemaNode() instanceof RpcDefinition) {
            nnWriter = createNormalizedNodeWriter(xmlWriter, schemaCtx, ((RpcDefinition) pathContext.getSchemaNode()).getOutput().getPath());
            writeElements(xmlWriter, nnWriter, (ContainerNode) data);
        } else {
            nnWriter = createNormalizedNodeWriter(xmlWriter, schemaCtx, schemaPath.getParent());
            if (data instanceof MapEntryNode) {
                // Restconf allows returning one list item. We need to wrap it
                // in map node in order to serialize it properly
                data = ImmutableNodes.mapNodeBuilder(data.getNodeType()).addChild((MapEntryNode) data).build();
            }
            nnWriter.write(data);
        }
        nnWriter.flush();
    }

    private NormalizedNodeWriter createNormalizedNodeWriter(XMLStreamWriter xmlWriter,
            SchemaContext schemaContext, SchemaPath schemaPath) {
        NormalizedNodeStreamWriter xmlStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(xmlWriter, schemaContext, schemaPath);
        return NormalizedNodeWriter.forStreamWriter(xmlStreamWriter);
    }

    private void writeElements(final XMLStreamWriter xmlWriter, final NormalizedNodeWriter nnWriter, final ContainerNode data)
            throws IOException {
        try {
            final QName name = data.getNodeType();
            xmlWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, name.getLocalName(), name.getNamespace().toString());
            xmlWriter.writeDefaultNamespace(name.getNamespace().toString());
            for(NormalizedNode<?,?> child : data.getValue()) {
                nnWriter.write(child);
            }
            nnWriter.flush();
            xmlWriter.writeEndElement();
            xmlWriter.flush();
        } catch (final XMLStreamException e) {
            Throwables.propagate(e);
        }
    }
}
