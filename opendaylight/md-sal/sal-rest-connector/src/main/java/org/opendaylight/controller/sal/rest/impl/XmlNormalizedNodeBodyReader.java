/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Optional;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Provider
@Consumes({ Draft02.MediaTypes.DATA + RestconfService.XML, Draft02.MediaTypes.OPERATION + RestconfService.XML,
    MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public class XmlNormalizedNodeBodyReader extends AbstractIdentifierAwareJaxRsProvider implements MessageBodyReader<NormalizedNodeContext> {

    private final static Logger LOG = LoggerFactory.getLogger(XmlNormalizedNodeBodyReader.class);
    private static final DocumentBuilderFactory BUILDERFACTORY;

    static {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
        } catch (final ParserConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        BUILDERFACTORY = factory;
    }

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
            final Optional<InstanceIdentifierContext> path = getIdentifierWithSchema();

            if (entityStream.available() < 1) {
                // represent empty nopayload input
                return new NormalizedNodeContext(path.get(), null);
            }

            final DocumentBuilder dBuilder;
            try {
                dBuilder = BUILDERFACTORY.newDocumentBuilder();
            } catch (final ParserConfigurationException e) {
                throw new RuntimeException("Failed to parse XML document", e);
            }
            final Document doc = dBuilder.parse(entityStream);

            final NormalizedNode<?, ?> result = parse(path.get(),doc);
            return new NormalizedNodeContext(path.get(),result);
        } catch (final Exception e) {
            LOG.debug("Error parsing xml input", e);

            throw new RestconfDocumentedException("Error parsing input: " + e.getMessage(), ErrorType.PROTOCOL,
                    ErrorTag.MALFORMED_MESSAGE);
        }
    }

    private static NormalizedNode<?,?> parse(final InstanceIdentifierContext<?> pathContext,final Document doc) {

        final List<Element> elements = Collections.singletonList(doc.getDocumentElement());
        final SchemaNode schemaNodeContext = pathContext.getSchemaNode();
        DataSchemaNode schemaNode = null;
        if (schemaNodeContext instanceof RpcDefinition) {
            schemaNode = ((RpcDefinition) schemaNodeContext).getInput();
        } else if (schemaNodeContext instanceof DataSchemaNode) {
            schemaNode = (DataSchemaNode) schemaNodeContext;
        } else {
            throw new IllegalStateException("Unknow SchemaNode");
        }

        final String docRootElm = doc.getDocumentElement().getLocalName();
        final String schemaNodeName = pathContext.getSchemaNode().getQName().getLocalName();

        if (!schemaNodeName.equalsIgnoreCase(docRootElm)) {
            final Collection<DataSchemaNode> children = ((DataNodeContainer) schemaNode).getChildNodes();
            for (final DataSchemaNode child : children) {
                if (child.getQName().getLocalName().equalsIgnoreCase(docRootElm)) {
                    schemaNode = child;
                    break;
                }
            }
        }

        // FIXME the factory instance should be cached if the schema context is the same
        final DomToNormalizedNodeParserFactory parserFactory =
                DomToNormalizedNodeParserFactory.getInstance(XmlUtils.DEFAULT_XML_CODEC_PROVIDER, pathContext.getSchemaContext());

        if(schemaNode instanceof ContainerSchemaNode) {
            return parserFactory.getContainerNodeParser().parse(Collections.singletonList(doc.getDocumentElement()), (ContainerSchemaNode) schemaNode);
        } else if(schemaNode instanceof ListSchemaNode) {
            final ListSchemaNode casted = (ListSchemaNode) schemaNode;
            return parserFactory.getMapEntryNodeParser().parse(elements, casted);
        } // FIXME : add another DataSchemaNode extensions e.g. LeafSchemaNode
        return null;
    }
}

