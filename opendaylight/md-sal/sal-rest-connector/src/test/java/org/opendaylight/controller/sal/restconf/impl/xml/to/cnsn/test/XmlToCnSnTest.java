/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.xml.to.cnsn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;

public class XmlToCnSnTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/xml-to-cnsn/leafref");
    }

    @Test
    public void testXmlLeafrefToCnSn() {
        CompositeNode compositeNode = TestUtils.readInputToCnSn("/xml-to-cnsn/leafref/xml/data.xml", false,
                XmlToCompositeNodeProvider.INSTANCE);
        assertNotNull(compositeNode);
        assertNotNull(dataSchemaNode);
        TestUtils.normalizeCompositeNode(compositeNode, modules, schemaNodePath);

        assertEquals("cont", compositeNode.getNodeType().getLocalName());

        SimpleNode<?> lf2 = null;
        for (Node<?> childNode : compositeNode.getValue()) {
            if (childNode instanceof SimpleNode) {
                if (childNode.getNodeType().getLocalName().equals("lf2")) {
                    lf2 = (SimpleNode<?>) childNode;
                    break;
                }
            }
        }

        assertNotNull(lf2);
        assertTrue(lf2.getValue() instanceof String);
        assertEquals("121", lf2.getValue());
    }

    @Test
    public void testXmlBlankInput() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("".getBytes());
        CompositeNode compositeNode = XmlToCompositeNodeProvider.INSTANCE.readFrom(null, null, null, null, null,
                inputStream);

        assertNull(compositeNode);
    }

    @Test
    public void testXmlBlankInputUnmarkableStream() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("".getBytes()) {
            @Override
            public boolean markSupported() {
                return false;
            }
        };
        CompositeNode compositeNode = XmlToCompositeNodeProvider.INSTANCE.readFrom(null, null, null, null, null,
                inputStream);

        assertNull(compositeNode);
    }

}
