/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.json.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.util.concurrent.UncheckedExecutionException;

public class NnToJsonLeafrefType extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private NormalizedNodeJsonBodyWriter jsonBodyWriter;

    public NnToJsonLeafrefType() throws NoSuchFieldException, SecurityException {
        super();
        jsonBodyWriter = new NormalizedNodeJsonBodyWriter();
    }

    @BeforeClass
    public static void initialization() {
        schemaContext = schemaContextLoader("/nn-to-json/leafref",
                schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void leafrefAbsolutePathToExistingLeafTest()
            throws WebApplicationException, IOException {
        String json = toJson("/nn-to-json/leafref/xml/data_absolut_ref_to_existing_leaf.xml");
        validateJson(".*\"lf3\":\\p{Blank}*\"true\".*", json);
    }

    @Test
    public void leafrefRelativePathToExistingLeafTest()
            throws WebApplicationException, IOException {
        String json = toJson("/nn-to-json/leafref/xml/data_relativ_ref_to_existing_leaf.xml");
        validateJson(".*\"lf2\":\\p{Blank}*\"121\".*", json);
    }

    @Test(expected = UncheckedExecutionException.class)
    public void leafrefToNonExistingLeafTest() throws WebApplicationException,
            IOException {
        toJson("/nn-to-json/leafref/xml/data_ref_to_non_existing_leaf.xml");
    }

    @Test
    public void leafrefToNotLeafTest() throws WebApplicationException,
            IOException {
        String json = toJson("/nn-to-json/leafref/xml/data_ref_to_not_leaf.xml");
        validateJson(
                ".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lf6\":\\p{Blank}*\"44\".*",
                json);
    }

    @Test
    public void leafrefFromLeafListToLeafTest() throws WebApplicationException,
            IOException {
        String json = toJson("/nn-to-json/leafref/xml/data_relativ_ref_from_leaflist_to_existing_leaf.xml");
        validateJson(
                ".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lflst1\":\\p{Blank}*.*\"346\",*\"347\",*\"345\".*",
                json);
    }

    @Test
    public void leafrefFromLeafrefToLeafrefTest()
            throws WebApplicationException, IOException {
        String json = toJson("/nn-to-json/leafref/xml/data_from_leafref_to_leafref.xml");
        validateJson(
                ".*\"cont-augment-module\\p{Blank}*:\\p{Blank}*lf7\":\\p{Blank}*\"200\".*",
                json);
    }

    private void validateJson(String regex, String value) {
        assertNotNull(value);
        Pattern ptrn = Pattern.compile(regex, Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(value);
        assertTrue(mtch.matches());
    }

    private String toJson(String xmlDataPath) throws WebApplicationException,
            IOException {
        final String uri = "main-module:cont";
        final String pathToInputFile = xmlDataPath;

        final NormalizedNodeContext testNN = TestRestconfUtils
                .loadNormalizedContextFromXmlFile(pathToInputFile, uri);

        final OutputStream output = new ByteArrayOutputStream();
        jsonBodyWriter.writeTo(testNN, null, null, null, mediaType, null,
                output);
        String jsonOutput = output.toString();

        return jsonOutput;
    }

    @Override
    protected MediaType getMediaType() {
        // TODO Auto-generated method stub
        return new MediaType(MediaType.APPLICATION_XML, null);
    }
}
