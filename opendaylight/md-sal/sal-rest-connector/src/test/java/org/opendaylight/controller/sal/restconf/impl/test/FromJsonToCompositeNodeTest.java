package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.ws.rs.WebApplicationException;

import org.junit.*;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.*;
import org.opendaylight.yangtools.yang.data.api.*;
import org.slf4j.*;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.*;

import com.google.gson.JsonSyntaxException;

public class FromJsonToCompositeNodeTest {

    private static final Logger LOG = LoggerFactory.getLogger(FromJsonToCompositeNodeTest.class);

    @Test
    public void simpleListTest() {
        simpleTest("/json-to-composite-node/simple-list.json", "/json-to-composite-node/simple-list-yang", "lst",
                "simple:list:yang1", "simple-list-yang1");
    }

    @Test
    public void simpleContainerTest() {
        simpleTest("/json-to-composite-node/simple-container.json", "/json-to-composite-node/simple-container-yang",
                "cont", "simple:container:yang", "simple-container-yang");
    }

    /**
     * test if for every leaf list item is simple node instance created
     */
    @Test
    public void multipleItemsInLeafList() {
        CompositeNode compositeNode = compositeContainerFromJson(
                "/json-to-composite-node/multiple-leaflist-items.json", true);
        assertNotNull(compositeNode);
        assertEquals(3, compositeNode.getChildren().size());

        boolean lflst1_1 = false;
        boolean lflst1_2 = false;
        boolean lflst1_3 = false;

        for (Node<?> node : compositeNode.getChildren()) {
            assertEquals("lflst1", node.getNodeType().getLocalName());
            assertTrue(node instanceof SimpleNode<?>);
            SimpleNode<?> simpleNode = (SimpleNode<?>) node;
            if (simpleNode.getValue().equals("45")) {
                lflst1_1 = true;
            } else if (simpleNode.getValue().equals("55")) {
                lflst1_2 = true;
            } else if (simpleNode.getValue().equals("66")) {
                lflst1_3 = true;
            }
        }

        assertTrue(lflst1_1);
        assertTrue(lflst1_2);
        assertTrue(lflst1_3);

    }

    /**
     * List contains 4 items and in every item are other elements. It is
     * supposed that there should be: lf11, lflst11, cont11, lst11
     */
    @Test
    public void multipleItemsInListTest() {
        CompositeNode compositeNode = compositeContainerFromJson("/json-to-composite-node/multiple-items-in-list.json",
                true);
        assertNotNull(compositeNode);

        assertEquals("lst", compositeNode.getNodeType().getLocalName());

        verityMultipleItemsInList(compositeNode);
    }

    @Test
    public void nullArrayToSimpleNodeWithNullValueTest() {
        CompositeNode compositeNode = compositeContainerFromJson("/json-to-composite-node/array-with-null.json", true);
        assertNotNull(compositeNode);
        assertEquals("cont", compositeNode.getNodeType().getLocalName());

        assertNotNull(compositeNode.getChildren());
        assertEquals(1, compositeNode.getChildren().size());
        Node<?> lfNode = compositeNode.getChildren().iterator().next();

        assertTrue(lfNode instanceof SimpleNode<?>);
        assertEquals(null, ((SimpleNode<?>) lfNode).getValue());

    }

    @Test
    public void incorrectTopLevelElementsTest() {
        Throwable cause1 = null;
        try {
            compositeContainerFromJson("/json-to-composite-node/wrong-top-level1.json", true);
        } catch (WebApplicationException e) {
            cause1 = e;
        }

        assertNotNull(cause1);
        assertTrue(cause1
                .getCause()
                .getMessage()
                .contains(
                        "First element in Json Object has to be \"Object\" or \"Array with one Object element\". Other scenarios are not supported yet."));

        Throwable cause2 = null;
        try {
            compositeContainerFromJson("/json-to-composite-node/wrong-top-level2.json", true);
        } catch (WebApplicationException e) {
            cause2 = e;
        }
        assertNotNull(cause2);
        assertTrue(cause2.getCause().getMessage().contains("Json Object should contain one element"));

        Throwable cause3 = null;
        try {
            compositeContainerFromJson("/json-to-composite-node/wrong-top-level3.json", true);
        } catch (WebApplicationException e) {
            cause3 = e;
        }
        assertNotNull(cause3);
        assertTrue(cause3
                .getCause()
                .getMessage()
                .contains(
                        "First element in Json Object has to be \"Object\" or \"Array with one Object element\". Other scenarios are not supported yet."));

    }

    /**
     * if leaf list with no data is in json then no corresponding data is
     * created in composite node. if leaf with no data then exception is raised
     */
    @Test
    public void emptyDataReadTest() {
        CompositeNode compositeNode = compositeContainerFromJson("/json-to-composite-node/empty-data.json", true);

        assertNotNull(compositeNode);

        assertEquals("cont", compositeNode.getNodeType().getLocalName());
        assertTrue(compositeNode instanceof CompositeNode);
        List<Node<?>> children = ((CompositeNode) compositeNode).getChildren();
        assertEquals(1, children.size());
        assertEquals("lflst2", children.get(0).getNodeType().getLocalName());
        assertEquals("45", children.get(0).getValue());

        String reason = null;
        try {
            compositeContainerFromJson("/json-to-composite-node/empty-data1.json", true);
        } catch (JsonSyntaxException e) {
            reason = e.getMessage();
        }

        assertTrue(reason.contains("Expected value at line"));

    }

    /**
     * Tests whether namespace <b>stay unchanged</b> if concrete values are
     * present in composite or simple node and if the method for update is
     * called.
     * 
     */
    @Test
    public void notSupplyNamespaceIfAlreadySupplied() {

        CompositeNode compositeNode = compositeContainerFromJson("/json-to-composite-node/simple-list.json");
        assertNotNull(compositeNode);

        DataSchemaNode dataSchemaNode1 = null;
        DataSchemaNode dataSchemaNode2 = null;
        try {
            dataSchemaNode1 = TestUtils.obtainSchemaFromYang("/json-to-composite-node/simple-list-yang",
                    "simple-list-yang1");
            dataSchemaNode2 = TestUtils.obtainSchemaFromYang("/json-to-composite-node/simple-list-yang",
                    "simple-list-yang2");
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage());
            assertTrue(false);
        }
        assertNotNull(dataSchemaNode1);
        assertNotNull(dataSchemaNode2);

        // supplement namespaces according to first data schema -
        // "simple:data:types1"
        TestUtils.supplementNamespace(dataSchemaNode1, compositeNode);

        assertTrue(compositeNode instanceof CompositeNodeWrapper);
        CompositeNode compNode = ((CompositeNodeWrapper) compositeNode).unwrap(null);

        assertEquals("lst", compNode.getNodeType().getLocalName());
        verifyCompositeNode(compNode, "simple:list:yang1");

        // dataSchemaNode2 should't be taken into account, because compNode
        // isn't CompositeNodeWrapper
        TestUtils.supplementNamespace(dataSchemaNode2, compNode);
        verifyCompositeNode(compNode, "simple:list:yang1");

    }

    private void simpleTest(String jsonPath, String yangPath, String topLevelElementName, String namespace,
            String moduleName) {
        CompositeNode compositeNode = compositeContainerFromJson(jsonPath);
        assertNotNull(compositeNode);

        DataSchemaNode dataSchemaNode = null;
        try {
            dataSchemaNode = TestUtils.obtainSchemaFromYang(yangPath, moduleName);
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage());
            assertTrue(false);
        }
        assertNotNull(dataSchemaNode);

        TestUtils.supplementNamespace(dataSchemaNode, compositeNode);

        assertTrue(compositeNode instanceof CompositeNodeWrapper);
        CompositeNode compNode = ((CompositeNodeWrapper) compositeNode).unwrap(null);

        assertEquals(topLevelElementName, compNode.getNodeType().getLocalName());
        verifyCompositeNode(compNode, namespace);
    }

    private void verityMultipleItemsInList(CompositeNode compositeNode) {
        List<Node<?>> childrenNodes = compositeNode.getChildren();
        assertEquals(4, childrenNodes.size());
        boolean lf11Found = false;
        boolean cont11Found = false;
        boolean lst11Found = false;
        for (Node<?> lst1Item : childrenNodes) {
            assertEquals("lst1", lst1Item.getNodeType().getLocalName());
            assertTrue(lst1Item instanceof CompositeNode);

            List<Node<?>> childrenLst1 = ((CompositeNode) lst1Item).getChildren();
            assertEquals(1, childrenLst1.size());
            String localName = childrenLst1.get(0).getNodeType().getLocalName();
            if (localName.equals("lf11")) {
                assertTrue(childrenLst1.get(0) instanceof SimpleNode);
                lf11Found = true;
            } else if (localName.equals("lflst11")) {
                assertTrue(childrenLst1.get(0) instanceof SimpleNode);
                assertEquals("45", ((SimpleNode<?>) childrenLst1.get(0)).getValue());
                lf11Found = true;
            } else if (localName.equals("cont11")) {
                assertTrue(childrenLst1.get(0) instanceof CompositeNode);
                cont11Found = true;
            } else if (localName.equals("lst11")) {
                lst11Found = true;
                assertTrue(childrenLst1.get(0) instanceof CompositeNode);
                assertEquals(0, ((CompositeNode) childrenLst1.get(0)).getChildren().size());
            }

        }
        assertTrue(lf11Found);
        assertTrue(cont11Found);
        assertTrue(lst11Found);
    }

    private void verifyCompositeNode(CompositeNode compositeNode, String namespace) {
        boolean cont1Found = false;
        boolean lst1Found = false;
        boolean lflst1_1Found = false;
        boolean lflst1_2Found = false;
        boolean lf1Found = false;

        assertEquals(namespace, compositeNode.getNodeType().getNamespace().toString());

        for (Node<?> node : compositeNode.getChildren()) {
            if (node.getNodeType().getLocalName().equals("cont1")) {
                if (node instanceof CompositeNode) {
                    cont1Found = true;
                    assertEquals(0, ((CompositeNode) node).getChildren().size());
                }
            } else if (node.getNodeType().getLocalName().equals("lst1")) {
                if (node instanceof CompositeNode) {
                    lst1Found = true;
                    assertEquals(0, ((CompositeNode) node).getChildren().size());
                }
            } else if (node.getNodeType().getLocalName().equals("lflst1")) {
                if (node instanceof SimpleNode) {
                    if (((SimpleNode<?>) node).getValue().equals("lflst1_1")) {
                        lflst1_1Found = true;
                    } else if (((SimpleNode<?>) node).getValue().equals("lflst1_2")) {
                        lflst1_2Found = true;
                    }
                }

            } else if (node.getNodeType().getLocalName().equals("lf1")) {
                if (node instanceof SimpleNode) {
                    if (((SimpleNode<?>) node).getValue().equals("lf1")) {
                        lf1Found = true;
                    }
                }
            }
            assertEquals(namespace, node.getNodeType().getNamespace().toString());
        }
        assertTrue(cont1Found);
        assertTrue(lst1Found);
        assertTrue(lflst1_1Found);
        assertTrue(lflst1_2Found);
        assertTrue(lf1Found);
    }

    private CompositeNode compositeContainerFromJson(String jsonPath) {
        return compositeContainerFromJson(jsonPath, false);
    }

    private CompositeNode compositeContainerFromJson(String jsonPath, boolean dummyNamespaces)
            throws WebApplicationException {

        JsonToCompositeNodeProvider jsonToCompositeNodeProvider = JsonToCompositeNodeProvider.INSTANCE;
        InputStream jsonStream = FromJsonToCompositeNodeTest.class.getResourceAsStream(jsonPath);
        try {
            CompositeNode compositeNode = jsonToCompositeNodeProvider
                    .readFrom(null, null, null, null, null, jsonStream);
            assertTrue(compositeNode instanceof CompositeNodeWrapper);
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
        } catch (IOException e) {
            LOG.error(e.getMessage());
            assertTrue(e.getMessage(), false);
        }
        return null;
    }

}
