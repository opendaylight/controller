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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @deprecated class will be removed in Lithium release
 */
@Provider
@Produces({ Draft02.MediaTypes.API + RestconfService.XML, Draft02.MediaTypes.DATA + RestconfService.XML,
        Draft02.MediaTypes.OPERATION + RestconfService.XML, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public enum StructuredDataToXmlProvider implements MessageBodyWriter<StructuredData> {
    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(StructuredDataToXmlProvider.class);
    private static final TransformerFactory FACTORY = TransformerFactory.newInstance();
    private static final ThreadLocal<Transformer> TRANSFORMER = new ThreadLocal<Transformer>() {
        @Override
        protected Transformer initialValue() {
            final Transformer ret;
            try {
                ret = FACTORY.newTransformer();
            } catch (final TransformerConfigurationException e) {
                LOG.error("Failed to instantiate XML transformer", e);
                throw new IllegalStateException("XML encoding currently unavailable", e);
            }

            ret.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            ret.setOutputProperty(OutputKeys.METHOD, "xml");
            ret.setOutputProperty(OutputKeys.INDENT, "yes");
            ret.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            ret.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            return ret;
        }
    };

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
        final CompositeNode data = t.getData();
        if (data == null) {
            throw new RestconfDocumentedException(Response.Status.NOT_FOUND);
        }

        final Transformer trans;
        try {
            trans = TRANSFORMER.get();
            if (t.isPrettyPrintMode()) {
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
            } else {
                trans.setOutputProperty(OutputKeys.INDENT, "no");
            }
        } catch (final RuntimeException e) {
            throw new RestconfDocumentedException(e.getMessage(), ErrorType.TRANSPORT, ErrorTag.OPERATION_FAILED);
        }

        // FIXME: BUG-1281: eliminate the intermediate Document
        final Document domTree = new XmlMapper().write(data, (DataNodeContainer) t.getSchema());
        try {
            trans.transform(new DOMSource(domTree), new StreamResult(entityStream));
        } catch (final TransformerException e) {
            LOG.error("Error during translation of Document to OutputStream", e);
            throw new RestconfDocumentedException(e.getMessage(), ErrorType.TRANSPORT, ErrorTag.OPERATION_FAILED);
        }
    }

}
