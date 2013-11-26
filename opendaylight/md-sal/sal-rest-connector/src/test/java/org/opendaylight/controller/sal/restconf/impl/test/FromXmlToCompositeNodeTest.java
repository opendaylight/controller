package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;

import org.junit.*;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.*;
import org.opendaylight.yangtools.yang.data.api.*;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.slf4j.*;

public class FromXmlToCompositeNodeTest {
    private static final Logger LOG = LoggerFactory.getLogger(FromXmlToCompositeNodeTest.class);

    /**
     * top level element represents container. second level element is list with
     * two elements.
     */
    @Test
    public void testXmlDataContainer() {
        CompositeNode compNode = compositeContainerFromXml("/xml-to-composite-node/data-container.xml", false);
        assertNotNull(compNode);
        DataSchemaNode dataSchemaNode = null;
        try {
            dataSchemaNode = TestUtils.obtainSchemaFromYang("/xml-to-composite-node/data-container-yang");
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage());
            assertTrue(false);
        }

        assertNotNull(dataSchemaNode);
        TestUtils.supplementNamespace(dataSchemaNode, compNode);

        String nameSpace = "data:container:yang";
        assertEquals(nameSpace, compNode.getNodeType().getNamespace().toString());

        verifyNullAndEmptyStringSingleNode(compNode, nameSpace);
        verifyCommonPartAOfXml(compNode, "", nameSpace);
    }

    private void verifyNullAndEmptyStringSingleNode(CompositeNode compNode, String nameSpace) {
        assertEquals("cont", compNode.getNodeType().getLocalName());

        SimpleNode<?> lf2 = null;
        SimpleNode<?> lf3 = null;
        int found = 0;
        for (Node<?> child : compNode.getChildren()) {
            if (found == 0x3)
                break;
            if (child instanceof SimpleNode<?>) {
                SimpleNode<?> childSimple = (SimpleNode<?>) child;
                if (childSimple.getNodeType().getLocalName().equals("lf3")) {
                    lf3 = childSimple;
                    found = found | (1 << 0);
                } else if (childSimple.getNodeType().getLocalName().equals("lf2")) {
                    lf2 = childSimple;
                    found = found | (1 << 1);
                }
            }
            assertEquals(nameSpace, child.getNodeType().getNamespace().toString());
        }

        assertEquals("", lf2.getValue());
        assertEquals(null, lf3.getValue());
    }

    @Test
    public void testXmlDataList() {
        CompositeNode compNode = compositeContainerFromXml("/xml-to-composite-node/data-list.xml", false);
        assertNotNull(compNode);

        DataSchemaNode dataSchemaNode = null;
        try {
            dataSchemaNode = TestUtils.obtainSchemaFromYang("/xml-to-composite-node/data-list-yang",
                    "data-container-yang");
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage());
        }
        assertNotNull(dataSchemaNode);
        TestUtils.supplementNamespace(dataSchemaNode, compNode);

        String nameSpaceList = "data:list:yang";
        String nameSpaceCont = "data:container:yang";
        assertEquals(nameSpaceCont, compNode.getNodeType().getNamespace().toString());
        assertEquals("cont", compNode.getNodeType().getLocalName());
        assertEquals(3, compNode.getChildren().size());
        CompositeNode lst1_1 = null;
        CompositeNode lst1_2 = null;
        int loopCount = 0;
        for (Node<?> node : compNode.getChildren()) {
            if (node.getNodeType().getLocalName().equals("lf1")) {
                assertEquals(nameSpaceList, node.getNodeType().getNamespace().toString());
                assertTrue(node instanceof SimpleNode<?>);
                assertEquals("lf1", node.getValue());
            } else {
                assertTrue(node instanceof CompositeNode);
                switch (loopCount++) {
                case 0:
                    lst1_1 = (CompositeNode) node;
                    break;
                case 1:
                    lst1_2 = (CompositeNode) node;
                    break;
                }
                assertEquals(nameSpaceCont, node.getNodeType().getNamespace().toString());
            }
        }
        // lst1_1
        verifyCommonPartAOfXml(lst1_1, "1", nameSpaceCont);
        // :lst1_1

        // lst1_2
        SimpleNode<?> lflst11 = null;
        CompositeNode cont11 = null;
        for (Node<?> node : lst1_2.getChildren()) {
            String nodeName = node.getNodeType().getLocalName();
            if (nodeName.equals("lflst11")) {
                assertTrue(node instanceof SimpleNode<?>);
                lflst11 = (SimpleNode<?>) node;

            } else if (nodeName.equals("cont11")) {
                assertTrue(node instanceof CompositeNode);
                cont11 = (CompositeNode) node;
            }
            assertEquals(nameSpaceCont, compNode.getNodeType().getNamespace().toString());
        }
        assertEquals("221", lflst11.getValue());

        assertEquals(1, cont11.getChildren().size());
        assertTrue(cont11.getChildren().get(0) instanceof SimpleNode<?>);
        SimpleNode<?> cont11_lf111 = (SimpleNode<?>) cont11.getChildren().get(0);
        assertEquals(nameSpaceCont, cont11_lf111.getNodeType().getNamespace().toString());
        assertEquals("lf111", cont11_lf111.getNodeType().getLocalName());
        assertEquals("100", cont11_lf111.getValue());
        // :lst1_2

    }

    @Test
    public void testXmlEmptyData() {
        CompositeNode compNode = compositeContainerFromXml("/xml-to-composite-node/empty-data.xml", true);
        assertEquals("cont", compNode.getNodeType().getLocalName());
        SimpleNode<?> lf1 = null;
        SimpleNode<?> lflst1_1 = null;
        SimpleNode<?> lflst1_2 = null;
        CompositeNode lst1 = null;
        int lflst1Count = 0;
        for (Node<?> node : compNode.getChildren()) {
            if (node.getNodeType().getLocalName().equals("lf1")) {
                assertTrue(node instanceof SimpleNode<?>);
                lf1 = (SimpleNode<?>) node;
            } else if (node.getNodeType().getLocalName().equals("lflst1")) {
                assertTrue(node instanceof SimpleNode<?>);

                switch (lflst1Count++) {
                case 0:
                    lflst1_1 = (SimpleNode<?>) node;
                    break;
                case 1:
                    lflst1_2 = (SimpleNode<?>) node;
                    break;
                }
            } else if (node.getNodeType().getLocalName().equals("lst1")) {
                assertTrue(node instanceof CompositeNode);
                lst1 = (CompositeNode) node;
            }
        }

        assertNotNull(lf1);
        assertNotNull(lflst1_1);
        assertNotNull(lflst1_2);
        assertNotNull(lst1);

        assertEquals("", lf1.getValue());
        assertEquals("", lflst1_1.getValue());
        assertEquals("", lflst1_2.getValue());
        assertEquals(1, lst1.getChildren().size());
        assertEquals("lf11", lst1.getChildren().get(0).getNodeType().getLocalName());

        assertTrue(lst1.getChildren().get(0) instanceof SimpleNode<?>);
        assertEquals("", lst1.getChildren().get(0).getValue());

    }

    private void verifyCommonPartAOfXml(CompositeNode compNode, String suf, String nameSpace) {
        SimpleNode<?> lf1suf = null;
        SimpleNode<?> lflst1suf_1 = null;
        SimpleNode<?> lflst1suf_2 = null;
        SimpleNode<?> lflst1suf_3 = null;
        CompositeNode cont1suf = null;
        CompositeNode lst1suf = null;

        int lflstCount = 0;

        for (Node<?> node : compNode.getChildren()) {
            String localName = node.getNodeType().getLocalName();
            if (localName.equals("lf1" + suf)) {
                assertTrue(node instanceof SimpleNode<?>);
                lf1suf = (SimpleNode<?>) node;
            } else if (localName.equals("lflst1" + suf)) {
                assertTrue(node instanceof SimpleNode<?>);
                switch (lflstCount++) {
                case 0:
                    lflst1suf_1 = (SimpleNode<?>) node;
                    break;
                case 1:
                    lflst1suf_2 = (SimpleNode<?>) node;
                    break;
                case 2:
                    lflst1suf_3 = (SimpleNode<?>) node;
                    break;
                }
            } else if (localName.equals("lst1" + suf)) {
                assertTrue(node instanceof CompositeNode);
                lst1suf = (CompositeNode) node;
            } else if (localName.equals("cont1" + suf)) {
                assertTrue(node instanceof CompositeNode);
                cont1suf = (CompositeNode) node;
            }
            assertEquals(nameSpace, node.getNodeType().getNamespace().toString());
        }

        assertNotNull(lf1suf);
        assertNotNull(lflst1suf_1);
        assertNotNull(lflst1suf_2);
        assertNotNull(lflst1suf_3);
        assertNotNull(lst1suf);
        assertNotNull(cont1suf);

        assertEquals("str0", lf1suf.getValue());
        assertEquals("121", lflst1suf_1.getValue());
        assertEquals("131", lflst1suf_2.getValue());
        assertEquals("str1", lflst1suf_3.getValue());

        assertEquals(1, lst1suf.getChildren().size());

        assertTrue(lst1suf.getChildren().get(0) instanceof SimpleNode<?>);
        SimpleNode<?> lst11_lf11 = (SimpleNode<?>) lst1suf.getChildren().get(0);
        assertEquals(nameSpace, lst11_lf11.getNodeType().getNamespace().toString());
        assertEquals("lf11" + suf, lst11_lf11.getNodeType().getLocalName());
        assertEquals("str2", lst11_lf11.getValue());

        assertTrue(cont1suf.getChildren().get(0) instanceof SimpleNode<?>);
        SimpleNode<?> cont1_lf11 = (SimpleNode<?>) cont1suf.getChildren().get(0);
        assertEquals(nameSpace, cont1_lf11.getNodeType().getNamespace().toString());
        assertEquals("lf11" + suf, cont1_lf11.getNodeType().getLocalName());
        assertEquals("100", cont1_lf11.getValue());
    }

    private CompositeNode compositeContainerFromXml(String xmlPath, boolean dummyNamespaces) {
        XmlToCompositeNodeProvider xmlToCompositeNodeProvider = XmlToCompositeNodeProvider.INSTANCE;
        try {
            InputStream xmlStream = FromXmlToCompositeNodeTest.class.getResourceAsStream(xmlPath);
            CompositeNode compositeNode = xmlToCompositeNodeProvider.readFrom(null, null, null, null, null, xmlStream);
            if (dummyNamespaces) {
                try {
                    TestUtils.addDummyNamespaceToAllNodes((CompositeNodeWrapper) compositeNode);
                    return ((CompositeNodeWrapper) compositeNode).unwrap(null);
                } catch (URISyntaxException e) {
                    LOG.error(e.getMessage());
                    assertTrue(e.getMessage(), false);
                }
            }
            return compositeNode;

        } catch (WebApplicationException | IOException e) {
            LOG.error(e.getMessage());
            assertTrue(false);
        }
        return null;
    }

}
