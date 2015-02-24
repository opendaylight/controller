/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated class will be removed in Lithium release
 */
@Provider
@Consumes({ Draft02.MediaTypes.DATA + RestconfService.XML, Draft02.MediaTypes.OPERATION + RestconfService.XML,
        MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public enum XmlToCompositeNodeProvider implements MessageBodyReader<Node<?>> {
    INSTANCE;
    private final static Logger LOG = LoggerFactory.getLogger(XmlToCompositeNodeProvider.class);

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return true;
    }

    @Override
    public Node<?> readFrom(final Class<Node<?>> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
            throws IOException, WebApplicationException {
        final XmlToCompositeNodeReader xmlReader = new XmlToCompositeNodeReader();
        try {
            return xmlReader.read(entityStream);
        } catch (XMLStreamException | UnsupportedFormatException e) {
            LOG.debug("Error parsing json input", e);
            throw new RestconfDocumentedException("Error parsing input: " + e.getMessage(), ErrorType.PROTOCOL,
                    ErrorTag.MALFORMED_MESSAGE);
        }
    }

}
