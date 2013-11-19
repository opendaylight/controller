package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.net.*;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.*;
import org.opendaylight.yangtools.yang.data.api.*;
import org.slf4j.*;

import com.google.gson.JsonSyntaxException;

public class FromJsonToCompositeNode {
    Logger LOG = LoggerFactory.getLogger(FromJsonToCompositeNode.class);

    @Test
    public void simpleListTest() {
        CompositeNode compositeNode = compositeContainerFromJson("/json-to-composite-node/simple-list.json");
        assertNotNull(compositeNode);

        assertEquals("lst", compositeNode.getNodeType().getLocalName());
        verifyCompositeNode(compositeNode);
    }

    @Test
    public void simpleContainerTest() {
        CompositeNode compositeNode = compositeContainerFromJson("/json-to-composite-node/simple-container.json");
        assertNotNull(compositeNode);

        assertEquals("cont", compositeNode.getNodeType().getLocalName());

        verifyCompositeNode(compositeNode);
    }

    /**
     * List contains 4 items and in every item are other elements. It is
     * supposed that there should be: lf11, lflst11, cont11, lst11
     */
    @Test
    public void multipleItemsInListTest() {
        CompositeNode compositeNode = compositeContainerFromJson("/json-to-composite-node/multiple-items-in-list.json");
        assertNotNull(compositeNode);

        assertEquals("lst", compositeNode.getNodeType().getLocalName());

        verityMultipleItemsInList(compositeNode);
    }

    @Test
    public void incorrectTopLevelElementsTest() {
        Throwable cause1 = null;
        try {
            compositeContainerFromJson("/json-to-composite-node/wrong-top-level1.json");
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
            compositeContainerFromJson("/json-to-composite-node/wrong-top-level2.json");
        } catch (WebApplicationException e) {
            cause2 = e;
        }
        assertNotNull(cause2);
        assertTrue(cause2.getCause().getMessage().contains("Json Object should contain one element"));

        Throwable cause3 = null;
        try {
            compositeContainerFromJson("/json-to-composite-node/wrong-top-level3.json");
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
        CompositeNode compositeNode = compositeContainerFromJson("/json-to-composite-node/empty-data.json");

        assertNotNull(compositeNode);

        assertEquals("cont", compositeNode.getNodeType().getLocalName());
        assertTrue(compositeNode instanceof CompositeNode);
        List<Node<?>> children = ((CompositeNode) compositeNode).getChildren();
        assertEquals(1, children.size());
        assertEquals("lflst2", children.get(0).getNodeType().getLocalName());
        assertEquals("45", children.get(0).getValue());

        String reason = null;
        try {
            compositeContainerFromJson("/json-to-composite-node/empty-data1.json");
        } catch (JsonSyntaxException e) {
            reason = e.getMessage();
        }

        assertTrue(reason.contains("Expected value at line"));

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

    private void verifyCompositeNode(CompositeNode compositeNode) {
        boolean cont1Found = false;
        boolean lst1Found = false;
        boolean lflst1_1Found = false;
        boolean lflst1_2Found = false;
        boolean lf1Found = false;

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
        }
        assertTrue(cont1Found);
        assertTrue(lst1Found);
        assertTrue(lflst1_1Found);
        assertTrue(lflst1_2Found);
        assertTrue(lf1Found);
    }

    private CompositeNode compositeContainerFromJson(String jsonPath) throws WebApplicationException {

        JsonToCompositeNodeProvider jsonToCompositeNodeProvider = JsonToCompositeNodeProvider.INSTANCE;
        InputStream jsonStream = FromJsonToCompositeNode.class.getResourceAsStream(jsonPath);
        try {
            CompositeNode compositeNode = jsonToCompositeNodeProvider
                    .readFrom(null, null, null, null, null, jsonStream);
            assertTrue(compositeNode instanceof CompositeNodeWrapper);
            try {
                addDummyNamespaceToAllNodes((CompositeNodeWrapper) compositeNode);
                return ((CompositeNodeWrapper) compositeNode).unwrap(null);
            } catch (URISyntaxException e) {
                LOG.error(e.getMessage());
                assertTrue(e.getMessage(), false);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
            assertTrue(e.getMessage(), false);
        }
        return null;
    }

    private void addDummyNamespaceToAllNodes(NodeWrapper<?> wrappedNode) throws URISyntaxException {
        wrappedNode.setNamespace(new URI(""));
        if (wrappedNode instanceof CompositeNodeWrapper) {
            for (NodeWrapper<?> childNodeWrapper : ((CompositeNodeWrapper) wrappedNode).getValues()) {
                addDummyNamespaceToAllNodes(childNodeWrapper);
            }
        }
    }
}
