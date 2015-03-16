/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.xml.to.cnsn.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

public class XmlToCnSnTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/xml-to-cnsn/leafref");
    }

    @Test
    public void testXmlLeafrefToCnSn() {
//        Node<?> node = TestUtils.readInputToCnSn("/xml-to-cnsn/leafref/xml/data.xml", false,
//                XmlToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode compositeNode = (CompositeNode)node;
//
//
//        assertNotNull(dataSchemaNode);
//        TestUtils.normalizeCompositeNode(compositeNode, modules, schemaNodePath);
//
//        assertEquals("cont", compositeNode.getNodeType().getLocalName());
//
//        SimpleNode<?> lf2 = null;
//        for (Node<?> childNode : compositeNode.getValue()) {
//            if (childNode instanceof SimpleNode) {
//                if (childNode.getNodeType().getLocalName().equals("lf2")) {
//                    lf2 = (SimpleNode<?>) childNode;
//                    break;
//                }
//            }
//        }
//
//        assertNotNull(lf2);
//        assertEquals(121, lf2.getValue());
    }

    @Test
    public void testXmlBlankInput() throws Exception {
//        InputStream inputStream = new ByteArrayInputStream("".getBytes());
//        Node<?> node =
//                XmlToCompositeNodeProvider.INSTANCE.readFrom(null, null, null, null, null, inputStream);
//
//        assertNull( node );
    }

    @Test
    public void testXmlBlankInputUnmarkableStream() throws Exception {
//        InputStream inputStream = new ByteArrayInputStream("".getBytes()) {
//            @Override
//            public boolean markSupported() {
//                return false;
//            }
//        };
//        Node<?> node =
//                XmlToCompositeNodeProvider.INSTANCE.readFrom(null, null, null, null, null, inputStream);
//
//        assertNull( node );
    }

}
