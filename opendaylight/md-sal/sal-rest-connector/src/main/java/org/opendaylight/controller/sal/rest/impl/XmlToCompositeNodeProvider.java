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
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;

import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

@Provider
@Consumes({ Draft02.MediaTypes.DATA + RestconfService.XML, Draft02.MediaTypes.OPERATION + RestconfService.XML,
        MediaType.APPLICATION_XML, MediaType.TEXT_XML })
public enum XmlToCompositeNodeProvider implements MessageBodyReader<CompositeNode> {
    INSTANCE;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public CompositeNode readFrom(Class<CompositeNode> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        XmlReader xmlReader = new XmlReader();
        try {
            return xmlReader.read(entityStream);
        } catch (XMLStreamException | UnsupportedFormatException e) {
            throw new ResponseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

}
