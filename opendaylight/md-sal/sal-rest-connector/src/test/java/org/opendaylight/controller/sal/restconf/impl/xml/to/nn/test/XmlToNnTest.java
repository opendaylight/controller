/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.xml.to.nn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.rest.impl.test.providers.TestXmlBodyReader;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class XmlToNnTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private final XmlNormalizedNodeBodyReader xmlBodyReader;

    public XmlToNnTest() throws NoSuchFieldException, SecurityException {
        super();
        xmlBodyReader = new XmlNormalizedNodeBodyReader();
    }

    @BeforeClass
    public static void initialize() {
        schemaContext = schemaContextLoader("/xml-to-nn/leafref", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void testXmlLeafrefToNn() throws NoSuchFieldException,
            SecurityException, IllegalArgumentException,
            IllegalAccessException, WebApplicationException, IOException {

        String uri = "leafref-module:cont";
        mockBodyReader(uri, xmlBodyReader, false);
        InputStream inputStream = TestXmlBodyReader.class
                .getResourceAsStream("/xml-to-nn/leafref/xml/data.xml");
        NormalizedNodeContext normalizedNodeContext = xmlBodyReader.readFrom(
                null, null, null, mediaType, null, inputStream);

        assertNotNull(normalizedNodeContext);

        assertEquals("cont", normalizedNodeContext.getData().getNodeType()
                .getLocalName());

        String nn = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());

        assertTrue(nn.contains("lf2"));
        assertTrue(nn.contains("121"));
    }

    @Test
    public void testXmlBlankInput() throws NoSuchFieldException,
            SecurityException, IllegalArgumentException,
            IllegalAccessException, WebApplicationException, IOException {

        String uri = "leafref-module:cont";
        mockBodyReader(uri, xmlBodyReader, false);
        InputStream inputStream = null;
        NormalizedNodeContext normalizedNodeContext = null;

        try {
            normalizedNodeContext = xmlBodyReader.readFrom(null, null, null,
                    mediaType, null, inputStream);
            fail("Normalized Node should not be created.");
        } catch (RestconfDocumentedException e) {

        }
        assertNull(normalizedNodeContext);
    }

    @Test
    public void testXmlBlankInputUnmarkableStream()
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException,
            WebApplicationException, IOException {
        String uri = "leafref-module:cont";
        mockBodyReader(uri, xmlBodyReader, false);
        InputStream inputStream = new ByteArrayInputStream("".getBytes()) {
            @Override
            public boolean markSupported() {
                return false;
            }
        };

        NormalizedNodeContext normalizedNodeContext = xmlBodyReader.readFrom(
                null, null, null, mediaType, null, inputStream);

        assertNotNull(normalizedNodeContext);
        assertNull(normalizedNodeContext.getData());
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

}
