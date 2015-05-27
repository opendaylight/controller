/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.nn.to.json.test;

import static org.junit.Assert.assertTrue;
import com.google.common.base.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NnToJsonWithAugmentTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private final NormalizedNodeJsonBodyWriter xmlBodyWriter;

    public NnToJsonWithAugmentTest() throws NoSuchFieldException,
            SecurityException {
        super();
        xmlBodyWriter = new NormalizedNodeJsonBodyWriter();
    }

    @BeforeClass
    public static void initialize() {
        schemaContext = schemaContextLoader("/nn-to-json/augmentation",
                schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void augmentedElementsToJson() throws WebApplicationException,
            IOException {
        final String uri = "yang:cont";
        final String pathToInputFile = "/nn-to-json/augmentation/xml/data.xml";

        final NormalizedNodeContext testNN = TestRestconfUtils
                .loadNormalizedContextFromXmlFile(pathToInputFile, uri);

        final OutputStream output = new ByteArrayOutputStream();
        xmlBodyWriter
                .writeTo(testNN, null, null, null, mediaType, null, output);
        final String jsonOutput = output.toString();

        Preconditions.checkNotNull(jsonOutput);

        assertTrue(jsonOutput.contains("\"cont1\"" + ":" + '{'));
        assertTrue(jsonOutput.contains("\"lf11\"" + ":" + "\"lf11\""));
        assertTrue(jsonOutput.contains("\"lst1\"" + ":" + '['));
        assertTrue(jsonOutput.contains("\"lf11\"" + ":" + "\"lf1_1\""));
        assertTrue(jsonOutput.contains("\"lf11\"" + ":" + "\"lf1_2\""));
        assertTrue(jsonOutput.contains("\"lflst1\"" + ":" + "["));
        assertTrue(jsonOutput.contains("\"lf2\"" + ":" + "\"lf2\""));
    }

    @Override
    protected MediaType getMediaType() {
        return new MediaType(MediaType.APPLICATION_XML, null);
    }
}
