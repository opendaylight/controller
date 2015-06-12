/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * If more then one data element with equal name exists where container or leaf
 * schema node should be present the RestconfDocumentedException has to be
 * raised
 *
 * Tests for BUG 1204
 */
public class MultipleEqualNamesForDataNodesTest extends AbstractBodyReaderTest {

    private JsonNormalizedNodeBodyReader jsonBodyReader;
    private static SchemaContext schemaContext;

    public MultipleEqualNamesForDataNodesTest() throws NoSuchFieldException,
            SecurityException {
        super();
        jsonBodyReader = new JsonNormalizedNodeBodyReader();
    }

    @BeforeClass
    public static void initialize() {
        schemaContext = schemaContextLoader("/equal-data-node-names/yang",
                schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void multipleEqualNameDataNodeTestForContainerJsonTest()
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException,
            WebApplicationException, IOException {
        multipleEqualNameDataNodeTest(
                "/equal-data-node-names/equal-name-data-for-container.json",
                ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
    }

    @Test
    public void multipleEqualNameDataNodeTestForLeafJsonTest()
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException,
            WebApplicationException, IOException {
        multipleEqualNameDataNodeTest(
                "/equal-data-node-names/equal-name-data-for-leaf.json",
                ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
    }

    @Test
    public void multipleEqualNameDataNodeTestForContainerXmlTest()
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException,
            WebApplicationException, IOException {
        multipleEqualNameDataNodeTest(
                "/equal-data-node-names/equal-name-data-for-container.xml",
                ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
    }

    @Test
    public void multipleEqualNameDataNodeTestForLeafXmlTest()
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException,
            WebApplicationException, IOException {
        multipleEqualNameDataNodeTest(
                "/equal-data-node-names/equal-name-data-for-leaf.xml",
                ErrorType.PROTOCOL, ErrorTag.MALFORMED_MESSAGE);
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    private void multipleEqualNameDataNodeTest(String path,
            ErrorType errorType, ErrorTag errorTag)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException,
            WebApplicationException, IOException {
        mockBodyReader("equal-data-node-names:cont", jsonBodyReader, false);
        InputStream inputStream = this.getClass().getResourceAsStream(path);

        try {
            jsonBodyReader.readFrom(null, null, null, mediaType, null,
                    inputStream);
            fail("Exception RestconfDocumentedException should be raised");
        } catch (RestconfDocumentedException e) {
            List<RestconfError> errors = e.getErrors();
            assertNotNull(errors);

            assertEquals(1, errors.size());

            RestconfError restconfError = errors.get(0);

            assertEquals(errorType, restconfError.getErrorType());
            assertEquals(errorTag, restconfError.getErrorTag());
        }
    }

}
