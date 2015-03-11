/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl.test.providers;

import static org.junit.Assert.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * sal-rest-connector
 * org.opendaylight.controller.sal.rest.impl.test.providers
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 12, 2015
 */
public class TestXmlBodyWriter extends AbstractBodyReaderTest {

    private final NormalizedNodeXmlBodyWriter xmlBodyWriter;
    private static SchemaContext schemaContext;

    public TestXmlBodyWriter () throws NoSuchFieldException, SecurityException {
        super();
        xmlBodyWriter = new NormalizedNodeXmlBodyWriter();
    }

    @Override
    MediaType getMediaType() {
        return new MediaType(MediaType.APPLICATION_XML, null);
    }

    @BeforeClass
    public static void initialization() throws NoSuchFieldException, SecurityException {
        schemaContext = schemaContextLoader("/instanceidentifier/yang", schemaContext);
        schemaContext = schemaContextLoader("/modules", schemaContext);
        schemaContext = schemaContextLoader("/invoke-rpc", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void rpcModuleInputTest() throws Exception {
        final String uri = "invoke-rpc-module:rpc-test";
        final String pathToInputFile = "/invoke-rpc/xml/rpc-output.xml";
        final NormalizedNodeContext nnContext =
                TestRestconfUtils.loadNormalizedContextFromXmlFile(pathToInputFile, uri);
        final OutputStream output = new ByteArrayOutputStream();
        xmlBodyWriter.writeTo(nnContext, null, null, null, mediaType, null, output);
        assertTrue(output.toString().contains("lf-test"));
    }
}