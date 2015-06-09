/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.xml.to.nn.test;

import static org.junit.Assert.assertTrue;
import com.google.common.base.Preconditions;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.test.providers.AbstractBodyReaderTest;
import org.opendaylight.controller.sal.rest.impl.test.providers.TestXmlBodyReader;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class XmlAugmentedElementToNnTest extends AbstractBodyReaderTest {

    private static SchemaContext schemaContext;
    private final XmlNormalizedNodeBodyReader xmlBodyReader;

    public XmlAugmentedElementToNnTest() throws NoSuchFieldException, SecurityException {
        super();
        xmlBodyReader = new XmlNormalizedNodeBodyReader();
    }

    @BeforeClass
    public static void initialize() {
        schemaContext = schemaContextLoader("/common/augment/yang", schemaContext);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void loadDataAugmentedSchemaMoreEqualNamesTest() throws Exception {
        String nn;
        nn = toNn("/common/augment/xml/dataa.xml");
        assertTrue(nn.contains("lf11 value for a"));

        nn = toNn("/common/augment/xml/datab.xml");
        assertTrue(nn.contains("lf11 value for b"));
    }

    private String toNn(final String xmlPath) throws Exception {
        final String uri = "main:cont";
        mockBodyReader(uri, xmlBodyReader, false);

        final InputStream inputStream = TestXmlBodyReader.class.getResourceAsStream(xmlPath);
        final NormalizedNodeContext normalizedNodeContext = xmlBodyReader.readFrom(null, null, null, mediaType, null,
                inputStream);

        Preconditions.checkNotNull(normalizedNodeContext);

        final String nn = NormalizedNodes.toStringTree(normalizedNodeContext.getData());
        return nn;
    }

    @Override
    protected MediaType getMediaType() {
        return null;
    }
}
