/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.json.to.nn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class JsonIdentityrefToNnTest extends AbstractBodyReaderTest {

    private final JsonNormalizedNodeBodyReader jsonBodyReader;
    private static SchemaContext schemaContext;

    public JsonIdentityrefToNnTest() throws NoSuchFieldException,
            SecurityException {
        super();
        jsonBodyReader = new JsonNormalizedNodeBodyReader();
    }

    @BeforeClass
    public static void initialize() {
        schemaContext = schemaContextLoader("/json-to-nn/identityref",
                schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void jsonIdentityrefToNn() throws NoSuchFieldException,
            SecurityException, IllegalArgumentException,
            IllegalAccessException, WebApplicationException, IOException {

        String uri = "identityref-module:cont";
        mockBodyReader(uri, jsonBodyReader, false);
        InputStream inputStream = this.getClass().getResourceAsStream(
                "/json-to-nn/identityref/json/data.json");

        NormalizedNodeContext normalizedNodeContext = jsonBodyReader.readFrom(
                null, null, null, mediaType, null, inputStream);

        assertEquals("cont", normalizedNodeContext.getData().getNodeType()
                .getLocalName());

        String dataTree = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());

        assertTrue(dataTree.contains("cont1"));
        assertTrue(dataTree
                .contains("lf11 (identity:module?revision=2013-12-02)iden"));
        assertTrue(dataTree
                .contains("lf12 (identityref:module?revision=2013-12-02)iden_local"));
        assertTrue(dataTree
                .contains("lf13 (identityref:module?revision=2013-12-02)iden_local"));
        assertTrue(dataTree
                .contains("lf14 (identity:module?revision=2013-12-02)iden"));
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

}
