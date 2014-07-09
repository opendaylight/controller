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
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Produces({ Draft02.MediaTypes.API + RestconfService.XML, Draft02.MediaTypes.DATA + RestconfService.XML,
        Draft02.MediaTypes.OPERATION + RestconfService.XML, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public enum StructuredDataToXmlProvider implements MessageBodyWriter<StructuredData> {
    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(StructuredDataToXmlProvider.class);

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return type.equals(StructuredData.class);
    }

    @Override
    public long getSize(final StructuredData t, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final StructuredData t, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws IOException,
            WebApplicationException {
        CompositeNode data = t.getData();
        if (data == null) {
            throw new RestconfDocumentedException(Response.Status.NOT_FOUND);
        }

        XMLStreamWriter writer;
        try {
            writer = XMLOutputFactory.newFactory().createXMLStreamWriter(entityStream);
        } catch (XMLStreamException | FactoryConfigurationError e) {
            LOG.error("Failed to allocate XML writer", e);
            throw new RestconfDocumentedException(e.getMessage(), ErrorType.TRANSPORT,
                    ErrorTag.OPERATION_FAILED);
        }

        if (t.isPrettyPrintMode()) {
            // FIXME: replace the writer with a proxy which does the indentation
            // final Transformer trans = TRANSFORMER.get();
            // trans.setOutputProperty(OutputKeys.INDENT, "yes");
            // trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        }

        try {
            XmlMapper.write(writer, data, t.getSchema());
            writer.close();
        } catch (XMLStreamException | FactoryConfigurationError e) {
            LOG.error("Error during translation data to OutputStream", e);
            throw new RestconfDocumentedException(e.getMessage(), ErrorType.TRANSPORT, ErrorTag.OPERATION_FAILED);
        }
    }
}
