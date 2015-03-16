/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.xml.to.cnsn.test;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlLeafrefToCnSnTest {
    private static final Logger LOG = LoggerFactory.getLogger(XmlLeafrefToCnSnTest.class);

    /**
     * top level element represents container. second level element is list with two elements.
     */
    @Test
    public void testXmlDataContainer() {
//        Node<?> node = TestUtils.readInputToCnSn("/xml-to-cnsn/data-container.xml", false,
//                XmlToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode compNode = (CompositeNode)node;
//
//        assertNotNull(compNode);
//        Set<Module> modules = TestUtils.loadModulesFrom("/xml-to-cnsn/data-container-yang");
//
//        assertNotNull(modules);
//        TestUtils.normalizeCompositeNode(compNode, modules, "data-container-yang:cont");
//
//        String nameSpace = "data:container:yang";
//        assertEquals(nameSpace, compNode.getNodeType().getNamespace().toString());
//
//        verifyNullAndEmptyStringSingleNode(compNode, nameSpace);
//        verifyCommonPartAOfXml(compNode, "", nameSpace);
    }

//    private void verifyNullAndEmptyStringSingleNode(final CompositeNode compNode, final String nameSpace) {
//        assertEquals("cont", compNode.getNodeType().getLocalName());
//
//        SimpleNode<?> lf2 = null;
//        SimpleNode<?> lf3 = null;
//        int found = 0;
//        for (Node<?> child : compNode.getValue()) {
//            if (found == 0x3)
//                break;
//            if (child instanceof SimpleNode<?>) {
//                SimpleNode<?> childSimple = (SimpleNode<?>) child;
//                if (childSimple.getNodeType().getLocalName().equals("lf3")) {
//                    lf3 = childSimple;
//                    found = found | (1 << 0);
//                } else if (childSimple.getNodeType().getLocalName().equals("lf2")) {
//                    lf2 = childSimple;
//                    found = found | (1 << 1);
//                }
//            }
//            assertEquals(nameSpace, child.getNodeType().getNamespace().toString());
//        }
//
//        assertEquals("", lf2.getValue());
//        assertEquals(null, lf3.getValue());
//    }

    @Test
    public void testXmlDataList() {
//        Node<?> node = TestUtils.readInputToCnSn("/xml-to-cnsn/data-list.xml", false,
//                XmlToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode compNode = (CompositeNode)node;
//
//
//        Set<Module> modules = TestUtils.loadModulesFrom("/xml-to-cnsn/data-list-yang");
//        assertNotNull(modules);
//
//        TestUtils.normalizeCompositeNode(compNode, modules, "data-container-yang:cont");
//
//        String nameSpaceList = "data:list:yang";
//        String nameSpaceCont = "data:container:yang";
//        assertEquals(nameSpaceCont, compNode.getNodeType().getNamespace().toString());
//        assertEquals("cont", compNode.getNodeType().getLocalName());
//        assertEquals(3, compNode.getValue().size());
//        CompositeNode lst1_1 = null;
//        CompositeNode lst1_2 = null;
//        int loopCount = 0;
//        for (Node<?> nd : compNode.getValue()) {
//            if (nd.getNodeType().getLocalName().equals("lf1")) {
//                assertEquals(nameSpaceList, nd.getNodeType().getNamespace().toString());
//                assertTrue(nd instanceof SimpleNode<?>);
//                assertEquals("lf1", nd.getValue());
//            } else {
//                assertTrue(nd instanceof CompositeNode);
//                switch (loopCount++) {
//                case 0:
//                    lst1_1 = (CompositeNode) nd;
//                    break;
//                case 1:
//                    lst1_2 = (CompositeNode) nd;
//                    break;
//                }
//                assertEquals(nameSpaceCont, nd.getNodeType().getNamespace().toString());
//            }
//        }
//        // lst1_1
//        verifyCommonPartAOfXml(lst1_1, "1", nameSpaceCont);
//        // :lst1_1
//
//        // lst1_2
//        SimpleNode<?> lflst11 = null;
//        CompositeNode cont11 = null;
//        for (Node<?> nd : lst1_2.getValue()) {
//            String nodeName = nd.getNodeType().getLocalName();
//            if (nodeName.equals("lflst11")) {
//                assertTrue(nd instanceof SimpleNode<?>);
//                lflst11 = (SimpleNode<?>) nd;
//
//            } else if (nodeName.equals("cont11")) {
//                assertTrue(nd instanceof CompositeNode);
//                cont11 = (CompositeNode) nd;
//            }
//            assertEquals(nameSpaceCont, compNode.getNodeType().getNamespace().toString());
//        }
//        assertEquals("221", lflst11.getValue());
//
//        assertEquals(1, cont11.getValue().size());
//        assertTrue(cont11.getValue().get(0) instanceof SimpleNode<?>);
//        SimpleNode<?> cont11_lf111 = (SimpleNode<?>) cont11.getValue().get(0);
//        assertEquals(nameSpaceCont, cont11_lf111.getNodeType().getNamespace().toString());
//        assertEquals("lf111", cont11_lf111.getNodeType().getLocalName());
//        assertEquals((short) 100, cont11_lf111.getValue());
//        // :lst1_2

    }

    @Test
    public void testXmlEmptyData() {
//        Node<?> node = TestUtils.readInputToCnSn("/xml-to-cnsn/empty-data.xml", true,
//                XmlToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode compNode = (CompositeNode)node;
//
//        assertEquals("cont", compNode.getNodeType().getLocalName());
//        SimpleNode<?> lf1 = null;
//        SimpleNode<?> lflst1_1 = null;
//        SimpleNode<?> lflst1_2 = null;
//        CompositeNode lst1 = null;
//        int lflst1Count = 0;
//        for (Node<?> nd : compNode.getValue()) {
//            if (nd.getNodeType().getLocalName().equals("lf1")) {
//                assertTrue(nd instanceof SimpleNode<?>);
//                lf1 = (SimpleNode<?>) nd;
//            } else if (nd.getNodeType().getLocalName().equals("lflst1")) {
//                assertTrue(nd instanceof SimpleNode<?>);
//
//                switch (lflst1Count++) {
//                case 0:
//                    lflst1_1 = (SimpleNode<?>) nd;
//                    break;
//                case 1:
//                    lflst1_2 = (SimpleNode<?>) nd;
//                    break;
//                }
//            } else if (nd.getNodeType().getLocalName().equals("lst1")) {
//                assertTrue(nd instanceof CompositeNode);
//                lst1 = (CompositeNode) nd;
//            }
//        }
//
//        assertNotNull(lf1);
//        assertNotNull(lflst1_1);
//        assertNotNull(lflst1_2);
//        assertNotNull(lst1);
//
//        assertEquals("", lf1.getValue());
//        assertEquals("", lflst1_1.getValue());
//        assertEquals("", lflst1_2.getValue());
//        assertEquals(1, lst1.getValue().size());
//        assertEquals("lf11", lst1.getValue().get(0).getNodeType().getLocalName());
//
//        assertTrue(lst1.getValue().get(0) instanceof SimpleNode<?>);
//        assertEquals("", lst1.getValue().get(0).getValue());

    }

    /**
     * Test case like this <lf11 xmlns:x="namespace">x:identity</lf11>
     */
    @Test
    public void testIdentityrefNmspcInElement() {
//        testIdentityrefToCnSn("/xml-to-cnsn/identityref/xml/data-nmspc-in-element.xml", "/xml-to-cnsn/identityref",
//                "identityref-module", "cont", 2, "iden", "identity:module");
    }

    /**
     *
     * Test case like <lf11 xmlns="namespace1" xmlns:x="namespace">identity</lf11>
     */

    @Test
    public void testIdentityrefDefaultNmspcInElement() {
//        testIdentityrefToCnSn("/xml-to-cnsn/identityref/xml/data-default-nmspc-in-element.xml",
//                "/xml-to-cnsn/identityref/yang-augments", "general-module", "cont", 3, "iden", "identityref:module");
    }

    /**
     *
     * Test case like <cont1 xmlns="namespace1"> <lf11 xmlns:x="namespace">identity</lf11> </cont1>
     */
    @Test
    public void testIdentityrefDefaultNmspcInParrentElement() {
//        testIdentityrefToCnSn("/xml-to-cnsn/identityref/xml/data-default-nmspc-in-parrent-element.xml",
//                "/xml-to-cnsn/identityref", "identityref-module", "cont", 2, "iden", "identityref:module");
    }

    /**
     *
     * Test case like <cont1 xmlns="namespace1" xmlns:x="namespace"> <lf11>x:identity</lf11> </cont1>
     */
    @Ignore
    @Test
    public void testIdentityrefNmspcInParrentElement() {
//        testIdentityrefToCnSn("/xml-to-cnsn/identityref/xml/data-nmspc-in-parrent-element.xml",
//                "/xml-to-cnsn/identityref", "identityref-module", "cont", 2, "iden", "z:namespace");
    }

    /**
     *
     * Test case like (without namespace in xml) <cont1> <lf11>x:identity</lf11> </cont1>
     */
    @Test
    public void testIdentityrefNoNmspcValueWithPrefix() {
//        testIdentityrefToCnSn("/xml-to-cnsn/identityref/xml/data-no-nmspc-value-with-prefix.xml",
//                "/xml-to-cnsn/identityref", "identityref-module", "cont", 2, "x:iden", "identityref:module");
    }

    /**
     *
     * Test case like (without namespace in xml) <cont1> <lf11>identity</lf11> </cont1>
     */
    @Test
    public void testIdentityrefNoNmspcValueWithoutPrefix() {
//        testIdentityrefToCnSn("/xml-to-cnsn/identityref/xml/data-no-nmspc-value-without-prefix.xml",
//                "/xml-to-cnsn/identityref", "identityref-module", "cont", 2, "iden", "identityref:module");
    }

//    private void verifyCommonPartAOfXml(final CompositeNode compNode, final String suf, final String nameSpace) {
//        SimpleNode<?> lf1suf = null;
//        SimpleNode<?> lflst1suf_1 = null;
//        SimpleNode<?> lflst1suf_2 = null;
//        SimpleNode<?> lflst1suf_3 = null;
//        CompositeNode cont1suf = null;
//        CompositeNode lst1suf = null;
//
//        int lflstCount = 0;
//
//        for (Node<?> node : compNode.getValue()) {
//            String localName = node.getNodeType().getLocalName();
//            if (localName.equals("lf1" + suf)) {
//                assertTrue(node instanceof SimpleNode<?>);
//                lf1suf = (SimpleNode<?>) node;
//            } else if (localName.equals("lflst1" + suf)) {
//                assertTrue(node instanceof SimpleNode<?>);
//                switch (lflstCount++) {
//                case 0:
//                    lflst1suf_1 = (SimpleNode<?>) node;
//                    break;
//                case 1:
//                    lflst1suf_2 = (SimpleNode<?>) node;
//                    break;
//                case 2:
//                    lflst1suf_3 = (SimpleNode<?>) node;
//                    break;
//                }
//            } else if (localName.equals("lst1" + suf)) {
//                assertTrue(node instanceof CompositeNode);
//                lst1suf = (CompositeNode) node;
//            } else if (localName.equals("cont1" + suf)) {
//                assertTrue(node instanceof CompositeNode);
//                cont1suf = (CompositeNode) node;
//            }
//            assertEquals(nameSpace, node.getNodeType().getNamespace().toString());
//        }
//
//        assertNotNull(lf1suf);
//        assertNotNull(lflst1suf_1);
//        assertNotNull(lflst1suf_2);
//        assertNotNull(lflst1suf_3);
//        assertNotNull(lst1suf);
//        assertNotNull(cont1suf);
//
//        assertEquals("str0", lf1suf.getValue());
//        assertEquals("121", lflst1suf_1.getValue());
//        assertEquals("131", lflst1suf_2.getValue());
//        assertEquals("str1", lflst1suf_3.getValue());
//
//        assertEquals(1, lst1suf.getValue().size());
//
//        assertTrue(lst1suf.getValue().get(0) instanceof SimpleNode<?>);
//        SimpleNode<?> lst11_lf11 = (SimpleNode<?>) lst1suf.getValue().get(0);
//        assertEquals(nameSpace, lst11_lf11.getNodeType().getNamespace().toString());
//        assertEquals("lf11" + suf, lst11_lf11.getNodeType().getLocalName());
//        assertEquals("str2", lst11_lf11.getValue());
//
//        assertTrue(cont1suf.getValue().get(0) instanceof SimpleNode<?>);
//        SimpleNode<?> cont1_lf11 = (SimpleNode<?>) cont1suf.getValue().get(0);
//        assertEquals(nameSpace, cont1_lf11.getNodeType().getNamespace().toString());
//        assertEquals("lf11" + suf, cont1_lf11.getNodeType().getLocalName());
//        assertEquals((short) 100, cont1_lf11.getValue());
//    }
//
//    private void testIdentityrefToCnSn(final String xmlPath, final String yangPath, final String moduleName,
//            final String schemaName, final int moduleCount, final String resultLocalName, final String resultNamespace) {
//        Node<?> node = TestUtils.readInputToCnSn(xmlPath, false, XmlToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode compositeNode = (CompositeNode)node;
//
//
//        Set<Module> modules = TestUtils.loadModulesFrom(yangPath);
//        assertEquals(moduleCount, modules.size());
//
//        TestUtils.normalizeCompositeNode(compositeNode, modules, moduleName + ":" + schemaName);
//
//        SimpleNode<?> lf11 = getLf11(compositeNode);
//        assertTrue(lf11.getValue() instanceof QName);
//        QName qName = (QName) lf11.getValue();
//        assertEquals(resultLocalName, qName.getLocalName());
//        assertEquals(resultNamespace, qName.getNamespace().toString());
//    }
//
//    private SimpleNode<?> getLf11(final CompositeNode compositeNode) {
//        assertEquals("cont", compositeNode.getNodeType().getLocalName());
//
//        List<Node<?>> childs = compositeNode.getValue();
//        assertEquals(1, childs.size());
//        Node<?> nd = childs.iterator().next();
//        assertTrue(nd instanceof CompositeNode);
//        assertEquals("cont1", nd.getNodeType().getLocalName());
//
//        childs = ((CompositeNode) nd).getValue();
//        SimpleNode<?> lf11 = null;
//        for (Node<?> child : childs) {
//            assertTrue(child instanceof SimpleNode);
//            if (child.getNodeType().getLocalName().equals("lf11")) {
//                lf11 = (SimpleNode<?>) child;
//            }
//        }
//        assertNotNull(lf11);
//        return lf11;
//    }

}
