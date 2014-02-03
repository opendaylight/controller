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
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;

import com.google.gson.stream.JsonWriter;

@Provider
@Produces({ Draft02.MediaTypes.API + RestconfService.JSON, Draft02.MediaTypes.DATA + RestconfService.JSON,
        Draft02.MediaTypes.OPERATION + RestconfService.JSON, MediaType.APPLICATION_JSON })
public enum StructuredDataToJsonProvider implements MessageBodyWriter<StructuredData> {
    INSTANCE;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(StructuredData t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(StructuredData t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        CompositeNode data = t.getData();
        if (data == null) {
            throw new ResponseException(Response.Status.NOT_FOUND, "No data exists.");
        }

        JsonWriter writer = new JsonWriter(new OutputStreamWriter(entityStream, "UTF-8"));
        writer.setIndent("    ");
        JsonMapper jsonMapper = new JsonMapper();
        jsonMapper.write(writer, data, (DataNodeContainer) t.getSchema(), t.getMountPoint());
        writer.flush();
    }

}
