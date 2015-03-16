/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.json.to.cnsn.test;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonToCnSnTest {

    private static final Logger LOG = LoggerFactory.getLogger(JsonToCnSnTest.class);

    @Test
    public void simpleListTest() {
        simpleTest("/json-to-cnsn/simple-list.json", "/json-to-cnsn/simple-list-yang/1", "lst", "simple:list:yang1",
                "simple-list-yang1");
    }

    @Test
    public void simpleContainerTest() {
        simpleTest("/json-to-cnsn/simple-container.json", "/json-to-cnsn/simple-container-yang", "cont",
                "simple:container:yang", "simple-container-yang");
    }

    /**
     * test if for every leaf list item is simple node instance created
     */
    @Test
    public void multipleItemsInLeafList() {
//        Node<?> node = TestUtils.readInputToCnSn("/json-to-cnsn/multiple-leaflist-items.json", true,
//                JsonToCompositeNodeProvider.INSTANCE);
//        assertNotNull(node);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode compositeNode = (CompositeNode)node;
//        assertEquals(3, compositeNode.getValue().size());
//
//        boolean lflst1_1 = false;
//        boolean lflst1_2 = false;
//        boolean lflst1_3 = false;
//
//        for (Node<?> nd : compositeNode.getValue()) {
//            assertEquals("lflst1", nd.getNodeType().getLocalName());
//            assertTrue(nd instanceof SimpleNode<?>);
//            SimpleNode<?> simpleNode = (SimpleNode<?>) nd;
//            if (simpleNode.getValue().equals("45")) {
//                lflst1_1 = true;
//            } else if (simpleNode.getValue().equals("55")) {
//                lflst1_2 = true;
//            } else if (simpleNode.getValue().equals("66")) {
//                lflst1_3 = true;
//            }
//        }
//
//        assertTrue(lflst1_1);
//        assertTrue(lflst1_2);
//        assertTrue(lflst1_3);

    }

    /**
     * List contains 4 items and in every item are other elements. It is supposed that there should be: lf11, lflst11,
     * cont11, lst11
     */
    @Test
    public void multipleItemsInListTest() {
//        final Node<?> node = TestUtils.readInputToCnSn("/json-to-cnsn/multiple-items-in-list.json", true,
//                JsonToCompositeNodeProvider.INSTANCE);
//
//        assertTrue(node instanceof CompositeNode);
//        final CompositeNode compositeNode = (CompositeNode)node;
//
//        assertNotNull(compositeNode);
//        assertEquals("lst", compositeNode.getNodeType().getLocalName());
//
//        verityMultipleItemsInList(compositeNode);
    }

    @Test
    public void nullArrayToSimpleNodeWithNullValueTest() {
//        final Node<?> node = TestUtils.readInputToCnSn("/json-to-cnsn/array-with-null.json", true,
//                JsonToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        final CompositeNode compositeNode = (CompositeNode)node;
//        assertEquals("cont", compositeNode.getNodeType().getLocalName());
//
//        assertNotNull(compositeNode.getValue());
//        assertEquals(1, compositeNode.getValue().size());
//        final Node<?> lfNode = compositeNode.getValue().iterator().next();
//
//        assertTrue(lfNode instanceof SimpleNode<?>);
//        assertEquals(null, ((SimpleNode<?>) lfNode).getValue());

    }

    @Test
    public void incorrectTopLevelElementsTest() {
//        RestconfDocumentedException cause1 = null;
//        try {
//            TestUtils
//                    .readInputToCnSn("/json-to-cnsn/wrong-top-level1.json", true, JsonToCompositeNodeProvider.INSTANCE);
//        } catch (final RestconfDocumentedException e) {
//            cause1 = e;
//        }
//
//        assertNotNull(cause1);
//        assertTrue(cause1
//                .getErrors()
//                .get(0)
//                .getErrorMessage()
//                .contains(
//                        "First element in Json Object has to be \"Object\" or \"Array with one Object element\". Other scenarios are not supported yet."));
//
//        RestconfDocumentedException cause2 = null;
//        try {
//            TestUtils
//                    .readInputToCnSn("/json-to-cnsn/wrong-top-level2.json", true, JsonToCompositeNodeProvider.INSTANCE);
//        } catch (final RestconfDocumentedException e) {
//            cause2 = e;
//        }
//        assertNotNull(cause2);
//        assertTrue(cause2.getErrors().get(0).getErrorMessage().contains("Json Object should contain one element"));
//
//        RestconfDocumentedException cause3 = null;
//        try {
//            TestUtils
//
//            .readInputToCnSn("/json-to-cnsn/wrong-top-level3.json", true, JsonToCompositeNodeProvider.INSTANCE);
//        } catch (final RestconfDocumentedException e) {
//            cause3 = e;
//        }
//        assertNotNull(cause3);
//        assertTrue(cause3
//                .getErrors()
//                .get(0)
//                .getErrorMessage()
//                .contains(
//                        "First element in Json Object has to be \"Object\" or \"Array with one Object element\". Other scenarios are not supported yet."));

    }

    /**
     * if leaf list with no data is in json then no corresponding data is created in composite node. if leaf with no
     * data then exception is raised
     */
    @Test
    public void emptyDataReadTest() {
//        final Node<?> node = TestUtils.readInputToCnSn("/json-to-cnsn/empty-data.json", true,
//                JsonToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        final CompositeNode compositeNode = (CompositeNode)node;
//
//        assertEquals("cont", compositeNode.getNodeType().getLocalName());
//        assertTrue(compositeNode instanceof CompositeNode);
//        final List<Node<?>> children = compositeNode.getValue();
//        assertEquals(1, children.size());
//        assertEquals("lflst2", children.get(0).getNodeType().getLocalName());
//        assertEquals("45", children.get(0).getValue());
//
//        String reason = null;
//        try {
//            TestUtils.readInputToCnSn("/json-to-cnsn/empty-data1.json", true, JsonToCompositeNodeProvider.INSTANCE);
//        } catch (final RestconfDocumentedException e) {
//            reason = e.getErrors().get(0).getErrorMessage();
//        }
//
//        assertTrue(reason.contains("Expected value at line"));

    }

    @Test
    public void testJsonBlankInput() throws Exception {
//        final InputStream inputStream = new ByteArrayInputStream("".getBytes());
//        final Node<?> node =
//                JsonToCompositeNodeProvider.INSTANCE.readFrom(null, null, null, null, null, inputStream);
//        assertNull( node );
    }

    /**
     * Tests whether namespace <b>stay unchanged</b> if concrete values are present in composite or simple node and if
     * the method for update is called.
     *
     */
    @Test
    public void notSupplyNamespaceIfAlreadySupplied() {

//        final Node<?> node = TestUtils.readInputToCnSn("/json-to-cnsn/simple-list.json", false,
//                JsonToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        final CompositeNode compositeNode = (CompositeNode)node;
//
//        // supplement namespaces according to first data schema -
//        // "simple:data:types1"
//        Set<Module> modules1 = new HashSet<>();
//        Set<Module> modules2 = new HashSet<>();
//        modules1 = TestUtils.loadModulesFrom("/json-to-cnsn/simple-list-yang/1");
//        modules2 = TestUtils.loadModulesFrom("/json-to-cnsn/simple-list-yang/2");
//        assertNotNull(modules1);
//        assertNotNull(modules2);
//
//        TestUtils.normalizeCompositeNode(compositeNode, modules1, "simple-list-yang1:lst");
//
//        assertTrue(compositeNode instanceof CompositeNodeWrapper);
//        final CompositeNode compNode = ((CompositeNodeWrapper) compositeNode).unwrap();
//
//        assertEquals("lst", compNode.getNodeType().getLocalName());
//        verifyCompositeNode(compNode, "simple:list:yang1");
//
//        try {
//            TestUtils.normalizeCompositeNode(compositeNode, modules2, "simple-list-yang2:lst");
//            fail("Conversion to normalized node shouldn't be successfull because of different namespaces");
//        } catch (final IllegalStateException e) {
//        }
////        veryfing has still meaning. despite exception, first phase where normalization of NodeWrappers is called passed successfuly.
//        verifyCompositeNode(compNode, "simple:list:yang1");
    }

    @Test
    public void jsonIdentityrefToCompositeNode() {
//        final Node<?> node = TestUtils.readInputToCnSn("/json-to-cnsn/identityref/json/data.json", false,
//                JsonToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        final CompositeNode compositeNode = (CompositeNode)node;
//
//        final Set<Module> modules = TestUtils.loadModulesFrom("/json-to-cnsn/identityref");
//        assertEquals(2, modules.size());
//
//        TestUtils.normalizeCompositeNode(compositeNode, modules, "identityref-module:cont");
//
//        assertEquals("cont", compositeNode.getNodeType().getLocalName());
//
//        List<Node<?>> childs = compositeNode.getValue();
//        assertEquals(1, childs.size());
//        final Node<?> nd = childs.iterator().next();
//        assertTrue(nd instanceof CompositeNode);
//        assertEquals("cont1", nd.getNodeType().getLocalName());
//
//        childs = ((CompositeNode) nd).getValue();
//        assertEquals(4, childs.size());
//        SimpleNode<?> lf11 = null;
//        SimpleNode<?> lf12 = null;
//        SimpleNode<?> lf13 = null;
//        SimpleNode<?> lf14 = null;
//        for (final Node<?> child : childs) {
//            assertTrue(child instanceof SimpleNode);
//            if (child.getNodeType().getLocalName().equals("lf11")) {
//                lf11 = (SimpleNode<?>) child;
//            } else if (child.getNodeType().getLocalName().equals("lf12")) {
//                lf12 = (SimpleNode<?>) child;
//            } else if (child.getNodeType().getLocalName().equals("lf13")) {
//                lf13 = (SimpleNode<?>) child;
//            } else if (child.getNodeType().getLocalName().equals("lf14")) {
//                lf14 = (SimpleNode<?>) child;
//            }
//        }
//
//        assertTrue(lf11.getValue() instanceof QName);
//        assertEquals("iden", ((QName) lf11.getValue()).getLocalName());
//        assertEquals("identity:module", ((QName) lf11.getValue()).getNamespace().toString());
//
//        assertTrue(lf12.getValue() instanceof QName);
//        assertEquals("iden_local", ((QName) lf12.getValue()).getLocalName());
//        assertEquals("identityref:module", ((QName) lf12.getValue()).getNamespace().toString());
//
//        assertTrue(lf13.getValue() instanceof QName);
//        assertEquals("iden_local", ((QName) lf13.getValue()).getLocalName());
//        assertEquals("identityref:module", ((QName) lf13.getValue()).getNamespace().toString());
//
//        assertTrue(lf14.getValue() instanceof QName);
//        assertEquals("iden_local", ((QName) lf14.getValue()).getLocalName());
//        assertEquals("identity:module", ((QName) lf14.getValue()).getNamespace().toString());
    }

    @Ignore
    @Test
    public void loadDataAugmentedSchemaMoreEqualNamesTest() {
//        loadAndNormalizeData("/common/augment/json/dataa.json", "/common/augment/yang", "cont", "main");
//        loadAndNormalizeData("/common/augment/json/datab.json", "/common/augment/yang", "cont", "main");

    }

    private void simpleTest(final String jsonPath, final String yangPath, final String topLevelElementName,
            final String namespace, final String moduleName) {
//        final CompositeNode compNode = loadAndNormalizeData(jsonPath, yangPath, topLevelElementName, moduleName);
//        verifyCompositeNode(compNode, namespace);
    }

//    private CompositeNode loadAndNormalizeData(final String jsonPath, final String yangPath,
//            final String topLevelElementName, final String moduleName) {
//        final Node<?> node = TestUtils.readInputToCnSn(jsonPath, false, JsonToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        final CompositeNode compositeNode = (CompositeNode)node;
//
//        Set<Module> modules = null;
//        modules = TestUtils.loadModulesFrom(yangPath);
//        assertNotNull(modules);
//
//        TestUtils.normalizeCompositeNode(compositeNode, modules, moduleName + ":" + topLevelElementName);
//
//        assertTrue(compositeNode instanceof CompositeNodeWrapper);
//        final CompositeNode compNode = ((CompositeNodeWrapper) compositeNode).unwrap();
//
//        assertEquals(topLevelElementName, compNode.getNodeType().getLocalName());
//        return compNode;
//    }

//    private void verityMultipleItemsInList(final CompositeNode compositeNode) {
//        final List<Node<?>> childrenNodes = compositeNode.getValue();
//        assertEquals(4, childrenNodes.size());
//        boolean lf11Found = false;
//        boolean cont11Found = false;
//        boolean lst11Found = false;
//        for (final Node<?> lst1Item : childrenNodes) {
//            assertEquals("lst1", lst1Item.getNodeType().getLocalName());
//            assertTrue(lst1Item instanceof CompositeNode);
//
//            final List<Node<?>> childrenLst1 = ((CompositeNode) lst1Item).getValue();
//            assertEquals(1, childrenLst1.size());
//            final String localName = childrenLst1.get(0).getNodeType().getLocalName();
//            if (localName.equals("lf11")) {
//                assertTrue(childrenLst1.get(0) instanceof SimpleNode);
//                lf11Found = true;
//            } else if (localName.equals("lflst11")) {
//                assertTrue(childrenLst1.get(0) instanceof SimpleNode);
//                assertEquals("45", ((SimpleNode<?>) childrenLst1.get(0)).getValue());
//                lf11Found = true;
//            } else if (localName.equals("cont11")) {
//                assertTrue(childrenLst1.get(0) instanceof CompositeNode);
//                cont11Found = true;
//            } else if (localName.equals("lst11")) {
//                lst11Found = true;
//                assertTrue(childrenLst1.get(0) instanceof CompositeNode);
//                assertEquals(0, ((CompositeNode) childrenLst1.get(0)).getValue().size());
//            }
//
//        }
//        assertTrue(lf11Found);
//        assertTrue(cont11Found);
//        assertTrue(lst11Found);
//    }

//    private void verifyCompositeNode(final CompositeNode compositeNode, final String namespace) {
//        boolean cont1Found = false;
//        boolean lst1Found = false;
//        boolean lflst1_1Found = false;
//        boolean lflst1_2Found = false;
//        boolean lf1Found = false;
//
//        // assertEquals(namespace,
//        // compositeNode.getNodeType().getNamespace().toString());
//
//        for (final Node<?> node : compositeNode.getValue()) {
//            if (node.getNodeType().getLocalName().equals("cont1")) {
//                if (node instanceof CompositeNode) {
//                    cont1Found = true;
//                    assertEquals(0, ((CompositeNode) node).getValue().size());
//                }
//            } else if (node.getNodeType().getLocalName().equals("lst1")) {
//                if (node instanceof CompositeNode) {
//                    lst1Found = true;
//                    assertEquals(0, ((CompositeNode) node).getValue().size());
//                }
//            } else if (node.getNodeType().getLocalName().equals("lflst1")) {
//                if (node instanceof SimpleNode) {
//                    if (((SimpleNode<?>) node).getValue().equals("lflst1_1")) {
//                        lflst1_1Found = true;
//                    } else if (((SimpleNode<?>) node).getValue().equals("lflst1_2")) {
//                        lflst1_2Found = true;
//                    }
//                }
//
//            } else if (node.getNodeType().getLocalName().equals("lf1")) {
//                if (node instanceof SimpleNode) {
//                    if (((SimpleNode<?>) node).getValue().equals("lf1")) {
//                        lf1Found = true;
//                    }
//                }
//            }
//            assertEquals(namespace, node.getNodeType().getNamespace().toString());
//        }
//        assertTrue(cont1Found);
//        assertTrue(lst1Found);
//        assertTrue(lflst1_1Found);
//        assertTrue(lflst1_2Found);
//        assertTrue(lf1Found);
//    }

    @Test
    public void unsupportedDataFormatTest() {
//        String exceptionMessage = "";
//        try {
//            TestUtils.readInputToCnSn("/json-to-cnsn/unsupported-json-format.json", true,
//                    JsonToCompositeNodeProvider.INSTANCE);
//        } catch (final RestconfDocumentedException e) {
//            exceptionMessage = e.getErrors().get(0).getErrorMessage();
//        }
//        assertTrue(exceptionMessage.contains("Root element of Json has to be Object"));
    }

    /**
     * Tests case when JSON input data value is in format string1:string2 and first string contain characters "<" or ">" (invalid URI characters).
     *
     * During loading data it is also interpreting as data value in moduleName:localName (potential leafref value).
     * ModuleName part is transformed to URI which causes exception which is caught and URI value is null which cause that potential value in simple node is
     * simple string (value from JSON input) and not IdentityValueDTO instance which is used for leaf-ref candidates.
     */
    @Test
    public void invalidUriCharacterInValue() {
//        final Node<?> rootNode = TestUtils.readInputToCnSn("/json-to-cnsn/invalid-uri-character-in-value.json", true,
//                    JsonToCompositeNodeProvider.INSTANCE);
//
//        assertTrue(rootNode instanceof CompositeNode);
//        Node<?> lf1 = null;
//        Node<?> lf2 = null;
//        for(final Node<?> child : ((CompositeNode)rootNode).getChildren()) {
//            if (child.getNodeType().getLocalName().equals("lf1")) {
//                lf1 = child;
//            } else if (child.getNodeType().getLocalName().equals("lf2")) {
//                lf2 = child;
//            }
//        }
//
//        assertNotNull(lf1);
//        assertNotNull(lf2);
//        assertTrue(lf1 instanceof SimpleNode<?>);
//        assertTrue(lf2 instanceof SimpleNode<?>);
//
//        assertEquals("module<Name:value lf1", ((SimpleNode<?>) lf1).getValue());
//        assertEquals("module>Name:value lf2", ((SimpleNode<?>) lf2).getValue());
    }

}
