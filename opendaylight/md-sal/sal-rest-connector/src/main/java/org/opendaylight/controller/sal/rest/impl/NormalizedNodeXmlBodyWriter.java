/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Throwables;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
        InstanceIdentifierContext pathContext = t.getInstanceIdentifierContext();
        if (t.getData() == null) {
            throw new RestconfDocumentedException(Response.Status.NOT_FOUND);
        }

        XMLStreamWriter xmlWriter;
        OutputStream xmlOutputStream = entityStream;
        if (t.isPrettyPrint()) {
            xmlOutputStream = new ByteArrayOutputStream();
        }

        try {
            xmlWriter = XML_FACTORY.createXMLStreamWriter(xmlOutputStream);
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        } catch (FactoryConfigurationError e) {
            throw new IllegalStateException(e);
        }
        NormalizedNode<?, ?> data = t.getData();
        SchemaPath schemaPath = pathContext.getSchemaNode().getPath();

        boolean isDataRoot = false;
        if (SchemaPath.ROOT.equals(schemaPath)) {
            isDataRoot = true;
        } else {
            schemaPath = schemaPath.getParent();
        }

        NormalizedNodeStreamWriter xmlNNStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(xmlWriter,
                pathContext.getSchemaContext(), schemaPath);
        NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(xmlNNStreamWriter);
        if (isDataRoot) {
            writeRootElement(xmlWriter, nnWriter, (ContainerNode) data);
        } else {
            if (data instanceof MapEntryNode) {
                // Restconf allows returning one list item. We need to wrap it
                // in map node in order to serialize it properly
                data = ImmutableNodes.mapNodeBuilder(data.getNodeType()).addChild((MapEntryNode) data).build();
            }
            nnWriter.write(data);
            nnWriter.flush();
        }

        if (t.isPrettyPrint()) {
            prettyPrintXml(xmlOutputStream, entityStream);
        }
    }
    private void prettyPrintXml(final OutputStream xmlOutputStream, final OutputStream prettyPrintedOutputStream) {
        InputStream inputStreamAsXml = new ByteArrayInputStream(((ByteArrayOutputStream)xmlOutputStream).toByteArray());
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStreamAsXml);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            trans.transform(new DOMSource(document), new StreamResult(prettyPrintedOutputStream));
        } catch (SAXException | ParserConfigurationException |  TransformerFactoryConfigurationError | TransformerException | IOException e) {
            throw new IllegalStateException("Pretty printing of xml output wasn't successfull.");
        }

    }

    private void writeRootElement(XMLStreamWriter xmlWriter, NormalizedNodeWriter nnWriter, ContainerNode data)
            throws IOException {
        try {
            QName name = SchemaContext.NAME;
            xmlWriter.writeStartElement(name.getNamespace().toString(), name.getLocalName());
            for (DataContainerChild<? extends PathArgument, ?> child : data.getValue()) {
                nnWriter.write(child);
            }
            nnWriter.flush();
            xmlWriter.writeEndElement();
            xmlWriter.flush();
        } catch (XMLStreamException e) {
            Throwables.propagate(e);
        }
    }
}
