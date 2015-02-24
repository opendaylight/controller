/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.base.Charsets;
import com.google.gson.stream.JsonWriter;
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
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;

/**
 * @deprecated class will be removed in Lithium release
 */
@Provider
@Produces({ Draft02.MediaTypes.API + RestconfService.JSON, Draft02.MediaTypes.DATA + RestconfService.JSON,
    Draft02.MediaTypes.OPERATION + RestconfService.JSON, MediaType.APPLICATION_JSON })
public enum StructuredDataToJsonProvider implements MessageBodyWriter<StructuredData> {
    INSTANCE;

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return type.equals(StructuredData.class);
    }

    @Override
    public long getSize(final StructuredData t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final StructuredData t, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
                    throws IOException, WebApplicationException {
        final CompositeNode data = t.getData();
        if (data == null) {
            throw new RestconfDocumentedException(Response.Status.NOT_FOUND);
        }

        final JsonWriter writer = new JsonWriter(new OutputStreamWriter(entityStream, Charsets.UTF_8));

        if (t.isPrettyPrintMode()) {
            writer.setIndent("    ");
        } else {
            writer.setIndent("");
        }
        final JsonMapper jsonMapper = new JsonMapper(t.getMountPoint());
        jsonMapper.write(writer, data, (DataNodeContainer) t.getSchema());
        writer.flush();
    }
}
