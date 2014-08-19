/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.gson.stream.JsonReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.opendaylight.controller.sal.rest.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JsonToNormalizedNodeReader {
    private static final Logger LOG = LoggerFactory.getLogger(JsonReader.class);

    private JsonToNormalizedNodeReader() {

    }

    public static NormalizedNode<?, ?> read(final InputStream entityStream, final DataSchemaNode dataSchemaNode)
            throws UnsupportedFormatException {
        NormalizedNodeContainerBuilder<?, ?, ?, ?> result = Builders.containerBuilder();
        NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        JsonParserStream parser = new JsonParserStream(streamWriter, null);
        parser.parse(new JsonReader(new InputStreamReader(entityStream)), dataSchemaNode);
        return result.build();
    }

}
