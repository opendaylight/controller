/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.json.to.nn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

public class JsonToNnTest extends AbstractBodyReaderTest {

    private JsonNormalizedNodeBodyReader jsonBodyReader;
    private SchemaContext schemaContext;

    public JsonToNnTest() throws NoSuchFieldException, SecurityException {
        super();
    }

    public static void initialize(final String path, SchemaContext schemaContext) throws IOException, ReactorException {
        schemaContext = schemaContextLoader(path, schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void simpleListTest() throws IOException, ReactorException {
        simpleTest("/json-to-nn/simple-list.json",
                "/json-to-nn/simple-list-yang/1", "lst", "simple-list-yang1");
    }

    @Test
    public void simpleContainerTest() throws IOException, ReactorException {
        simpleTest("/json-to-nn/simple-container.json",
                "/json-to-nn/simple-container-yang", "cont",
                "simple-container-yang");
    }

    @Test
    public void multipleItemsInLeafListTest() throws IOException, ReactorException {

        initialize("/json-to-nn/simple-list-yang/1", schemaContext);

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                "/json-to-nn/multiple-leaflist-items.json",
                "simple-list-yang1:lst");
        assertNotNull(normalizedNodeContext);

        final String dataTree = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());
        assertTrue(dataTree.contains("45"));
        assertTrue(dataTree.contains("55"));
        assertTrue(dataTree.contains("66"));
    }

    @Test
    public void multipleItemsInListTest() throws IOException, ReactorException {
        initialize("/json-to-nn/simple-list-yang/3", schemaContext);

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                "/json-to-nn/multiple-items-in-list.json",
                "multiple-items-yang:lst");
        assertNotNull(normalizedNodeContext);

        assertEquals("lst", normalizedNodeContext.getData().getNodeType()
                .getLocalName());

        verityMultipleItemsInList(normalizedNodeContext);
    }

    @Test
    public void nullArrayToSimpleNodeWithNullValueTest() throws IOException, ReactorException {
        initialize("/json-to-nn/simple-list-yang/4", schemaContext);

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                "/json-to-nn/array-with-null.json", "array-with-null-yang:cont");
        assertNotNull(normalizedNodeContext);

        assertEquals("cont", normalizedNodeContext.getData().getNodeType()
                .getLocalName());

        final String dataTree = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());
        assertTrue(dataTree.contains("lf"));
        assertTrue(dataTree.contains("null"));
    }

    @Test
    public void incorrectTopLevelElementsTest() throws WebApplicationException,
            IOException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException, ReactorException {

        jsonBodyReader = new JsonNormalizedNodeBodyReader();
        initialize("/json-to-nn/simple-list-yang/1", schemaContext);
        mockBodyReader("simple-list-yang1:lst", jsonBodyReader, false);

        InputStream inputStream = this.getClass().getResourceAsStream(
                "/json-to-nn/wrong-top-level1.json");

        int countExceptions = 0;
        RestconfDocumentedException exception = null;

        try {
            jsonBodyReader.readFrom(null, null, null, mediaType, null,
                    inputStream);
        } catch (final RestconfDocumentedException e) {
            exception = e;
            countExceptions++;
        }
        assertNotNull(exception);
        assertEquals(
                "Error parsing input: Schema node with name cont wasn't found under (urn:ietf:params:xml:ns:netconf:base:1.0)data.",
                exception.getErrors().get(0).getErrorMessage());

        inputStream = this.getClass().getResourceAsStream(
                "/json-to-nn/wrong-top-level2.json");
        exception = null;
        try {
            jsonBodyReader.readFrom(null, null, null, mediaType, null,
                    inputStream);
        } catch (final RestconfDocumentedException e) {
            exception = e;
            countExceptions++;
        }
        assertNotNull(exception);
        assertEquals(
                "Error parsing input: Schema node with name lst1 wasn't found under (urn:ietf:params:xml:ns:netconf:base:1.0)data.",
                exception.getErrors().get(0).getErrorMessage());

        inputStream = this.getClass().getResourceAsStream(
                "/json-to-nn/wrong-top-level3.json");
        exception = null;
        try {
            jsonBodyReader.readFrom(null, null, null, mediaType, null,
                    inputStream);
        } catch (final RestconfDocumentedException e) {
            exception = e;
            countExceptions++;
        }
        assertNotNull(exception);
        assertEquals(
                "Error parsing input: Schema node with name lf wasn't found under (urn:ietf:params:xml:ns:netconf:base:1.0)data.",
                exception.getErrors().get(0).getErrorMessage());
        assertEquals(3, countExceptions);
    }

    @Test
    public void emptyDataReadTest() throws WebApplicationException,
            IOException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException, ReactorException {

        initialize("/json-to-nn/simple-list-yang/4", schemaContext);

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                "/json-to-nn/empty-data.json", "array-with-null-yang:cont");
        assertNotNull(normalizedNodeContext);

        assertEquals("cont", normalizedNodeContext.getData().getNodeType()
                .getLocalName());

        final String dataTree = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());

        assertTrue(dataTree.contains("lflst1"));

        assertTrue(dataTree.contains("lflst2 45"));

        jsonBodyReader = new JsonNormalizedNodeBodyReader();
        RestconfDocumentedException exception = null;
        mockBodyReader("array-with-null-yang:cont", jsonBodyReader, false);
        final InputStream inputStream = this.getClass().getResourceAsStream(
                "/json-to-nn/empty-data.json1");

        try {
            jsonBodyReader.readFrom(null, null, null, mediaType, null,
                    inputStream);
        } catch (final RestconfDocumentedException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals("Error parsing input: null", exception.getErrors().get(0)
                .getErrorMessage());
    }

    @Test
    public void testJsonBlankInput() throws NoSuchFieldException,
            SecurityException, IllegalArgumentException,
            IllegalAccessException, WebApplicationException, IOException, ReactorException {
        initialize("/json-to-nn/simple-list-yang/4", schemaContext);
        final NormalizedNodeContext normalizedNodeContext = prepareNNC("",
                "array-with-null-yang:cont");
        assertNull(normalizedNodeContext);
    }

    @Test
    public void notSupplyNamespaceIfAlreadySupplied()
            throws WebApplicationException, IOException, NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException, ReactorException {

        initialize("/json-to-nn/simple-list-yang/1", schemaContext);

        final String uri = "simple-list-yang1" + ":" + "lst";

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(
                "/json-to-nn/simple-list.json", uri);
        assertNotNull(normalizedNodeContext);

        verifyNormaluizedNodeContext(normalizedNodeContext, "lst");

        mockBodyReader("simple-list-yang2:lst", jsonBodyReader, false);
        final InputStream inputStream = this.getClass().getResourceAsStream(
                "/json-to-nn/simple-list.json");

        try {
            jsonBodyReader.readFrom(null, null, null, mediaType, null,
                    inputStream);
            fail("NormalizedNodeContext should not be create because of different namespace");
        } catch (final RestconfDocumentedException e) {
        }

        verifyNormaluizedNodeContext(normalizedNodeContext, "lst");
    }

    @Test
    public void dataAugmentedTest() throws IOException, ReactorException {

        initialize("/common/augment/yang", schemaContext);

        NormalizedNodeContext normalizedNodeContext = prepareNNC(
                "/common/augment/json/dataa.json", "main:cont");

        assertNotNull(normalizedNodeContext);
        assertEquals("cont", normalizedNodeContext.getData().getNodeType()
                .getLocalName());

        String dataTree = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());
        assertTrue(dataTree.contains("cont1"));
        assertTrue(dataTree.contains("lf11 lf11 value from a"));

        normalizedNodeContext = prepareNNC("/common/augment/json/datab.json",
                "main:cont");

        assertNotNull(normalizedNodeContext);
        assertEquals("cont", normalizedNodeContext.getData().getNodeType()
                .getLocalName());
        dataTree = NormalizedNodes
                .toStringTree(normalizedNodeContext.getData());
        assertTrue(dataTree.contains("cont1"));
        assertTrue(dataTree.contains("lf11 lf11 value from b"));
    }

    private void simpleTest(final String jsonPath, final String yangPath,
            final String topLevelElementName, final String moduleName) throws IOException, ReactorException {

        initialize(yangPath, schemaContext);

        final String uri = moduleName + ":" + topLevelElementName;

        final NormalizedNodeContext normalizedNodeContext = prepareNNC(jsonPath, uri);
        assertNotNull(normalizedNodeContext);

        verifyNormaluizedNodeContext(normalizedNodeContext, topLevelElementName);
    }

    private NormalizedNodeContext prepareNNC(final String jsonPath, final String uri) {
        jsonBodyReader = new JsonNormalizedNodeBodyReader();
        try {
            mockBodyReader(uri, jsonBodyReader, false);
        } catch (NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        final InputStream inputStream = this.getClass().getResourceAsStream(jsonPath);

        NormalizedNodeContext normalizedNodeContext = null;

        try {
            normalizedNodeContext = jsonBodyReader.readFrom(null, null, null,
                    mediaType, null, inputStream);
        } catch (WebApplicationException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return normalizedNodeContext;
    }

    private void verifyNormaluizedNodeContext(
            final NormalizedNodeContext normalizedNodeContext,
            final String topLevelElementName) {
        assertEquals(topLevelElementName, normalizedNodeContext.getData()
                .getNodeType().getLocalName());

        final String dataTree = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());
        assertTrue(dataTree.contains("cont1"));
        assertTrue(dataTree.contains("lst1"));
        assertTrue(dataTree.contains("lflst1"));
        assertTrue(dataTree.contains("lflst1_1"));
        assertTrue(dataTree.contains("lflst1_2"));
        assertTrue(dataTree.contains("lf1"));
    }

    private void verityMultipleItemsInList(
            final NormalizedNodeContext normalizedNodeContext) {

        final String dataTree = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());
        assertTrue(dataTree.contains("lf11"));
        assertTrue(dataTree.contains("lf11_1"));
        assertTrue(dataTree.contains("lflst11"));
        assertTrue(dataTree.contains("45"));
        assertTrue(dataTree.contains("cont11"));
        assertTrue(dataTree.contains("lst11"));
    }

    @Test
    public void unsupportedDataFormatTest() throws NoSuchFieldException,
            SecurityException, IllegalArgumentException,
            IllegalAccessException, WebApplicationException, IOException, ReactorException {
        jsonBodyReader = new JsonNormalizedNodeBodyReader();
        initialize("/json-to-nn/simple-list-yang/1", schemaContext);
        mockBodyReader("simple-list-yang1:lst", jsonBodyReader, false);

        final InputStream inputStream = this.getClass().getResourceAsStream(
                "/json-to-nn/unsupported-json-format.json");

        RestconfDocumentedException exception = null;

        try {
            jsonBodyReader.readFrom(null, null, null, mediaType, null,
                    inputStream);
        } catch (final RestconfDocumentedException e) {
            exception = e;
        }
        System.out.println(exception.getErrors().get(0).getErrorMessage());

        assertTrue(exception.getErrors().get(0).getErrorMessage()
                .contains("is not a simple type"));
    }

    @Test
    public void invalidUriCharacterInValue() throws NoSuchFieldException,
            SecurityException, IllegalArgumentException,
            IllegalAccessException, WebApplicationException, IOException, ReactorException {

        jsonBodyReader = new JsonNormalizedNodeBodyReader();
        initialize("/json-to-nn/simple-list-yang/4", schemaContext);
        mockBodyReader("array-with-null-yang:cont", jsonBodyReader, false);

        final InputStream inputStream = this.getClass().getResourceAsStream(
                "/json-to-nn/invalid-uri-character-in-value.json");

        final NormalizedNodeContext normalizedNodeContext = jsonBodyReader.readFrom(
                null, null, null, mediaType, null, inputStream);
        assertNotNull(normalizedNodeContext);

        assertEquals("cont", normalizedNodeContext.getData().getNodeType()
                .getLocalName());

        final String dataTree = NormalizedNodes.toStringTree(normalizedNodeContext
                .getData());
        assertTrue(dataTree.contains("lf1 module<Name:value lf1"));
        assertTrue(dataTree.contains("lf2 module>Name:value lf2"));
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

}
