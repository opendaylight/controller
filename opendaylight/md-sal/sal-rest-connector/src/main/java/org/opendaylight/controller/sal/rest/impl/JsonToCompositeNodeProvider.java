/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.gson.stream.JsonToken;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Consumes({ Draft02.MediaTypes.DATA + RestconfService.JSON, Draft02.MediaTypes.OPERATION + RestconfService.JSON,
        MediaType.APPLICATION_JSON })
public enum JsonToCompositeNodeProvider implements MessageBodyReader<CompositeNode> {
    INSTANCE;

    private final static Logger LOG = LoggerFactory.getLogger(JsonToCompositeNodeProvider.class);

    private static final int BUFFER_SIZE = 100;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public CompositeNode readFrom(Class<CompositeNode> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        byte[] jsonData = loadInputStream(entityStream);

        checkJsonDuplicates(jsonData);

        InputStream inputStream = new ByteArrayInputStream(jsonData);
        JsonReader customJsonReader = new JsonReader();
        try {
            return customJsonReader.read(inputStream);
        } catch (Exception e) {
            LOG.debug("Error parsing json input", e);
            throw new RestconfDocumentedException("Error parsing input: " + e.getMessage(), ErrorType.PROTOCOL,
                    ErrorTag.MALFORMED_MESSAGE);
        }
    }

    private void checkJsonDuplicates(byte[] jsonData) {
        InputStream inputStream = new ByteArrayInputStream(jsonData);
        com.google.gson.stream.JsonReader jsonReader = new com.google.gson.stream.JsonReader(new InputStreamReader(
                inputStream));
        checkJsonDuplicatesInObject(jsonReader, JsonToken.END_OBJECT);
    }

    private byte[] loadInputStream(InputStream inputStream) throws IOException {
        final BufferedInputStream bis = new BufferedInputStream(inputStream);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesCount = 0;
        while((bytesCount  = bis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesCount);
        }
        return baos.toByteArray();
    }

    private static void checkJsonDuplicatesInObject(final com.google.gson.stream.JsonReader reader,
            final JsonToken endToken) throws RestconfDocumentedException {
        final Set<String> set = new HashSet<>();
        JsonToken token =null;

        try {
            while (!(token = reader.peek()).equals(JsonToken.END_DOCUMENT)) {
                switch (token) {
                case NAME:
                    final String fieldName = reader.nextName();
                    if (!set.add(fieldName))
                        throw new RestconfDocumentedException("Duplicate key name " + fieldName + " in json input",
                                ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
                    break;
                case BEGIN_ARRAY:
                case BEGIN_OBJECT:
                    if (token.equals(JsonToken.BEGIN_OBJECT)) {
                        reader.beginObject();
                    } else {
                        reader.beginArray();
                    }
                    checkJsonDuplicatesInObject(reader, token.equals(JsonToken.BEGIN_OBJECT) ? JsonToken.END_OBJECT
                            : JsonToken.END_ARRAY);
                    break;
                case END_ARRAY:
                case END_OBJECT:
                    if (token.equals(JsonToken.END_ARRAY)) {
                        reader.endArray();
                    } else {
                        reader.endObject();
                    }
                    break;
                case NULL:
                    reader.nextNull();
                    break;
                case BOOLEAN:
                    reader.nextBoolean();
                    break;
                default:
                    reader.nextString();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RestconfDocumentedException(e.getMessage(), e);
        }
    }
}
