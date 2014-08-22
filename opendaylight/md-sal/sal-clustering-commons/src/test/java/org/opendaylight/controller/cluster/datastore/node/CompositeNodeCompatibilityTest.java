package org.opendaylight.controller.cluster.datastore.node;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class CompositeNodeCompatibilityTest extends TestCase {
    static class LegacyNodeData {
        QName nodeKey;
        Object nodeData; // List for a CompositeNode, value Object for a
        // SimpeNode

        LegacyNodeData(final QName nodeKey, final Object nodeData) {
            this.nodeKey = nodeKey;
            this.nodeData = nodeData;
        }

        @Override
        public String toString() {
            return nodeKey.toString();
        }
    }


    static final QName TEST_QNAME = QName.create(
        "urn:opendaylight:params:xml:ns:yang:controller:md:sal:normalization:test",
        "2014-03-13", "test");
    static final QName OUTER_LIST_QNAME =
        QName.create(TEST_QNAME, "outer-list");
    static final QName INNER_LIST_QNAME =
        QName.create(TEST_QNAME, "inner-list");
    static final QName OUTER_CHOICE_QNAME =
        QName.create(TEST_QNAME, "outer-choice");
    static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");

    static final YangInstanceIdentifier TEST_PATH =
        YangInstanceIdentifier.of(TEST_QNAME);
    static final YangInstanceIdentifier OUTER_LIST_PATH =
        YangInstanceIdentifier.builder(TEST_PATH).node(OUTER_LIST_QNAME)
            .build();
    static final QName ONE_QNAME = QName.create(TEST_QNAME, "one");
    static final QName TWO_QNAME = QName.create(TEST_QNAME, "two");
    static final QName THREE_QNAME = QName.create(TEST_QNAME, "three");

    static final QName ANY_XML_DATA_QNAME =
        QName.create(TEST_QNAME, "any-xml-data");
    static final QName OUTER_CONTAINER_QNAME =
        QName.create(TEST_QNAME, "outer-container");
    static final QName AUGMENTED_LEAF_QNAME =
        QName.create(TEST_QNAME, "augmented-leaf");
    static final QName UNKEYED_LIST_QNAME =
        QName.create(TEST_QNAME, "unkeyed-list");
    static final QName UNORDERED_LEAF_LIST_QNAME =
        QName.create(TEST_QNAME, "unordered-leaf-list");
    static final QName ORDERED_LEAF_LIST_QNAME =
        QName.create(TEST_QNAME, "ordered-leaf-list");

    static final Short OUTER_LIST_ID = (short) 10;

    static final YangInstanceIdentifier OUTER_LIST_PATH_LEGACY =
        YangInstanceIdentifier.builder(TEST_QNAME)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, OUTER_LIST_ID).build();

    static final YangInstanceIdentifier LEAF_TWO_PATH_LEGACY =
        YangInstanceIdentifier.builder(OUTER_LIST_PATH_LEGACY)
            .node(TWO_QNAME).build();

    static final QName ANY_XML_LEAF_QNAME = QName.create(TEST_QNAME, "leaf");

    static final QName ANY_XML_INNER_QNAME = QName.create(TEST_QNAME, "inner");
    static final QName ANY_XML_INNER_LEAF_QNAME =
        QName.create(TEST_QNAME, "inner-leaf");

    SchemaContext createTestContext() {
        YangParserImpl parser = new YangParserImpl();
        Set<Module> modules = parser.parseYangModelsFromStreams(
            Collections.singletonList(CompositeNodeCompatibilityTest.class
                .getResourceAsStream("/normalization-test.yang"))
        );
        return parser.resolveSchemaContext(modules);
    }

    @Test
    public void testToLegacyNormalizedNode() throws DataNormalizationException {
        DataNormalizer normalizer =
            new DataNormalizer(createTestContext());

        ChoiceNode choiceNode1 = Builders
            .choiceBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(OUTER_CHOICE_QNAME))
            .withChild(ImmutableNodes.leafNode(TWO_QNAME, "two"))
            .withChild(ImmutableNodes.leafNode(THREE_QNAME, "three")).build();

        MapEntryNode innerListEntryNode1 = Builders.mapEntryBuilder()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                    INNER_LIST_QNAME, NAME_QNAME, "inner-name1"))
            .withChild(ImmutableNodes.leafNode(NAME_QNAME, "inner-name1"))
            .withChild(ImmutableNodes.leafNode(VALUE_QNAME, "inner-value1"))
            .build();

        MapEntryNode innerListEntryNode2 = Builders.mapEntryBuilder()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                    INNER_LIST_QNAME, NAME_QNAME, "inner-name2"))
            .withChild(ImmutableNodes.leafNode(NAME_QNAME, "inner-name2"))
            .withChild(ImmutableNodes.leafNode(VALUE_QNAME, "inner-value2"))
            .build();

        OrderedMapNode innerListNode = Builders.orderedMapBuilder()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(INNER_LIST_QNAME))
            .withChild(innerListEntryNode1)
            .withChild(innerListEntryNode2).build();

        Integer outerListID1 = Integer.valueOf((short) 10);
        MapEntryNode outerListEntryNode1 = Builders.mapEntryBuilder()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                    OUTER_LIST_QNAME, ID_QNAME, outerListID1))
            .withChild(ImmutableNodes.leafNode(ID_QNAME, outerListID1))
            .withChild(choiceNode1)
            .withChild(innerListNode).build();

        ChoiceNode choiceNode2 = Builders.choiceBuilder().withNodeIdentifier(
            new YangInstanceIdentifier.NodeIdentifier(OUTER_CHOICE_QNAME))
            .withChild(ImmutableNodes.leafNode(ONE_QNAME, "one")).build();

        Integer outerListID2 = Integer.valueOf((short) 20);
        MapEntryNode outerListEntryNode2 = Builders.mapEntryBuilder()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                    OUTER_LIST_QNAME, ID_QNAME, outerListID2))
            .withChild(ImmutableNodes.leafNode(ID_QNAME, outerListID2))
            .withChild(choiceNode2).build();

        MapNode
            outerListNode = Builders.mapBuilder().withNodeIdentifier(
            new YangInstanceIdentifier.NodeIdentifier(OUTER_LIST_QNAME))
            .withChild(outerListEntryNode1).withChild(outerListEntryNode2)
            .build();

        UnkeyedListEntryNode unkeyedListEntryNode1 =
            Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                    UNKEYED_LIST_QNAME))
                .withChild(ImmutableNodes.leafNode(NAME_QNAME, "unkeyed1"))
                .build();

        UnkeyedListEntryNode unkeyedListEntryNode2 =
            Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                    UNKEYED_LIST_QNAME))
                .withChild(ImmutableNodes.leafNode(NAME_QNAME, "unkeyed2"))
                .build();

        UnkeyedListNode unkeyedListNode = Builders.unkeyedListBuilder()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(UNKEYED_LIST_QNAME))
            .withChild(unkeyedListEntryNode1)
            .withChild(unkeyedListEntryNode2).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME))
            .withChild(outerListNode).withChild(unkeyedListNode)
            .build();



        NormalizedNodeMessages.Node node =
            CompositeNodeCompatibility.toNode(testContainerNode);

        Node<?> legacyNode = CompositeNodeCompatibility.toComposite(node,
            normalizer.getOperation(TEST_PATH).getDataSchemaNode().get());

        verifyLegacyNode(
            legacyNode,
            expectCompositeNode(
                TEST_QNAME,
                expectCompositeNode(
                    OUTER_LIST_QNAME,
                    expectSimpleNode(ID_QNAME, outerListID1),
                    expectSimpleNode(TWO_QNAME, "two"),
                    expectSimpleNode(THREE_QNAME, "three"),

                    expectCompositeNode(INNER_LIST_QNAME,
                        expectSimpleNode(NAME_QNAME, "inner-name1"),
                        expectSimpleNode(VALUE_QNAME, "inner-value1")),

                    expectCompositeNode(INNER_LIST_QNAME,
                        expectSimpleNode(NAME_QNAME, "inner-name2"),
                        expectSimpleNode(VALUE_QNAME, "inner-value2"))
                ),
                expectCompositeNode(OUTER_LIST_QNAME,
                    expectSimpleNode(ID_QNAME, outerListID2),
                    expectSimpleNode(ONE_QNAME, "one")),
                expectCompositeNode(UNKEYED_LIST_QNAME,
                    expectSimpleNode(NAME_QNAME, "unkeyed1")),
                expectCompositeNode(UNKEYED_LIST_QNAME,
                    expectSimpleNode(NAME_QNAME, "unkeyed2"))
            )
        );

        normalizer.toNormalized(TEST_PATH, (CompositeNode) legacyNode);

        // Conversion of Mixin type nodes is not supported.

        assertNull("Expected null returned for Mixin type node",
            DataNormalizer.toLegacy(outerListNode));
    }

    /**
     * Following data are constructed: <any-xml-data> <inner>
     * <inner-leaf>inner-leaf-value</inner-leaf> </inner>
     * <leaf>leaf-value</leaf> <any-xml-data>
     */
    @Test
    public void testToLegacyNormalizedNodeWithAnyXml()
        throws DataNormalizationException {

        Node<?> innerLeafChild = NodeFactory
            .createImmutableSimpleNode(ANY_XML_INNER_LEAF_QNAME, null,
                "inner-leaf-value");
        CompositeNode innerContainer = NodeFactory.createImmutableCompositeNode(ANY_XML_INNER_QNAME, null,
            Collections.<Node<?>> singletonList(innerLeafChild));

        Node<?> leafChild = NodeFactory.createImmutableSimpleNode(ANY_XML_LEAF_QNAME, null, "leaf-value");
        CompositeNode anyXmlNodeValue = NodeFactory.createImmutableCompositeNode(ANY_XML_DATA_QNAME, null,
            Arrays.asList(leafChild, innerContainer));

        AnyXmlNode
            testAnyXmlNode = Builders.anyXmlBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME))
            .withValue(anyXmlNodeValue).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(testAnyXmlNode).build();

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        NormalizedNodeMessages.Node node =
            CompositeNodeCompatibility.toNode(testContainerNode);

        Node<?> legacyNode = CompositeNodeCompatibility.toComposite(node,
            normalizer.getOperation(TEST_PATH).getDataSchemaNode().get());

        verifyLegacyNode(
            legacyNode,
            expectCompositeNode(
                TEST_QNAME,
                expectCompositeNode(
                    ANY_XML_DATA_QNAME,
                    expectSimpleNode(ANY_XML_LEAF_QNAME, "leaf-value"),
                    expectCompositeNode(ANY_XML_INNER_QNAME,
                        expectSimpleNode(ANY_XML_INNER_LEAF_QNAME, "inner-leaf-value")))));




    }

    @Test
    public void testToLegacyNormalizedNodeWithLeafLists()
        throws DataNormalizationException {

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName(TEST_QNAME);

        ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafSetBuilder = Builders.leafSetBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(UNORDERED_LEAF_LIST_QNAME));
        for (int i = 1; i <= 3; i++) {
            leafSetBuilder.withChildValue("unordered-value" + i);
        }

        ListNodeBuilder<Object, LeafSetEntryNode<Object>> orderedLeafSetBuilder = Builders.orderedLeafSetBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(ORDERED_LEAF_LIST_QNAME));
        for (int i = 3; i > 0; i--) {
            orderedLeafSetBuilder.withChildValue("ordered-value" + i);
        }

        ContainerNode testContainerNode = Builders.containerBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(leafSetBuilder.build())
            .withChild(orderedLeafSetBuilder.build()).build();

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        NormalizedNodeMessages.Node node =
            CompositeNodeCompatibility.toNode(testContainerNode);

        Node<?> legacyNode = CompositeNodeCompatibility.toComposite(node,
            normalizer.getOperation(TEST_PATH).getDataSchemaNode().get());

        verifyLegacyNode(
            legacyNode,
            expectCompositeNode(TEST_QNAME, expectSimpleNode(UNORDERED_LEAF_LIST_QNAME, "unordered-value1"),
                expectSimpleNode(UNORDERED_LEAF_LIST_QNAME, "unordered-value2"),
                expectSimpleNode(UNORDERED_LEAF_LIST_QNAME, "unordered-value3"),
                expectSimpleNode(ORDERED_LEAF_LIST_QNAME, "ordered-value3"),
                expectSimpleNode(ORDERED_LEAF_LIST_QNAME, "ordered-value2"),
                expectSimpleNode(ORDERED_LEAF_LIST_QNAME, "ordered-value1")));
    }

    @Test
    public void testToLegacyNormalizedNodeWithAugmentation()
        throws DataNormalizationException {

        AugmentationNode augmentationNode = Builders.augmentationBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.AugmentationIdentifier(
                Sets.newHashSet(AUGMENTED_LEAF_QNAME)))
            .withChild(ImmutableNodes.leafNode(AUGMENTED_LEAF_QNAME, "augmented-value")).build();

        ContainerNode outerContainerNode = Builders.containerBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(OUTER_CONTAINER_QNAME)).withChild(augmentationNode).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(outerContainerNode).build();

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        NormalizedNodeMessages.Node node =
            CompositeNodeCompatibility.toNode(testContainerNode);

        Node<?> legacyNode = CompositeNodeCompatibility.toComposite(node,
            normalizer.getOperation(TEST_PATH).getDataSchemaNode().get());

        verifyLegacyNode(
            legacyNode,
            expectCompositeNode(
                TEST_QNAME,
                expectCompositeNode(OUTER_CONTAINER_QNAME,
                    expectSimpleNode(AUGMENTED_LEAF_QNAME, "augmented-value"))));
    }

    @Test
    public void testToLeafNode() throws DataNormalizationException {

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        YangInstanceIdentifier id =
            YangInstanceIdentifier.builder().node(TEST_QNAME).node(OUTER_LIST_QNAME)
                .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, Integer.valueOf(50))
                .node(ID_QNAME).build();

        LeafNode<Integer> expected =
            ImmutableNodes.leafNode(ID_QNAME, Integer.valueOf(50));

        NormalizedNodeMessages.Node node =
            CompositeNodeCompatibility.toNode(expected);

        NormalizedNode normalizedNode = CompositeNodeCompatibility
            .toSimpleNormalizedNode(node,
                normalizer.getOperation(id).getDataSchemaNode().get());

        assertTrue(normalizedNode instanceof LeafNode);

        LeafNode actual = (LeafNode) normalizedNode;

        assertEquals(expected.getValue(), actual.getValue());

        assertEquals(expected.getNodeType(), actual.getNodeType());

        assertEquals(expected.getIdentifier(), actual.getIdentifier());

    }

    @Test
    public void testToLeafSetEntryNode() throws DataNormalizationException {

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        YangInstanceIdentifier id =
            YangInstanceIdentifier.create()
                .node(TEST_QNAME)
                .node(UNORDERED_LEAF_LIST_QNAME)
                .node(new YangInstanceIdentifier.NodeWithValue(UNORDERED_LEAF_LIST_QNAME, "foobar"));

        YangInstanceIdentifier.NodeWithValue
            nodeId = new YangInstanceIdentifier.NodeWithValue(UNORDERED_LEAF_LIST_QNAME, "foobar");
        LeafSetEntryNode<Object> expected =
            Builders.leafSetEntryBuilder().withNodeIdentifier(nodeId)
                .withValue("foobar").build();

        NormalizedNodeMessages.Node node =
            CompositeNodeCompatibility.toNode(expected);

        NormalizedNode normalizedNode = CompositeNodeCompatibility
            .toSimpleNormalizedNode(node,
                normalizer.getOperation(id).getDataSchemaNode().get());

        assertTrue(normalizedNode instanceof LeafSetEntryNode);

        LeafSetEntryNode actual = (LeafSetEntryNode) normalizedNode;

        assertEquals(expected.getValue(), actual.getValue());

        assertEquals(expected.getNodeType(), actual.getNodeType());

        assertEquals(expected.getIdentifier(), actual.getIdentifier());

    }

    @Test
    public void testToEmptyContainer() throws DataNormalizationException {

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        YangInstanceIdentifier id =
            YangInstanceIdentifier.create()
                .node(TEST_QNAME);

        NormalizedNode<?, ?> expected =
            ImmutableNodes.containerNode(TEST_QNAME);

        NormalizedNodeMessages.Node node =
            CompositeNodeCompatibility.toNode(expected);

        NormalizedNode normalizedNode = CompositeNodeCompatibility
            .toSimpleNormalizedNode(node,
                normalizer.getOperation(id).getDataSchemaNode().get());

        assertTrue(normalizedNode instanceof ContainerNode);

        ContainerNode actual = (ContainerNode) normalizedNode;

        assertEquals(expected.getNodeType(), actual.getNodeType());

        assertEquals(expected.getIdentifier(), actual.getIdentifier());

    }

    private boolean isOrdered(final QName nodeName) {
        return ORDERED_LEAF_LIST_QNAME.equals(nodeName) || INNER_LIST_QNAME
            .equals(nodeName);
    }

    @SuppressWarnings("unchecked")
    private void verifyLegacyNode(final Node<?> actual,
        final LegacyNodeData expNodeData) {

        assertNotNull("Actual Node is null", actual);
        assertTrue("Expected CompositeNode instance",
            actual instanceof CompositeNode);
        CompositeNode actualCN = (CompositeNode) actual;
        assertEquals("Node key", expNodeData.nodeKey, actualCN.getKey());

        List<LegacyNodeData> expChildData = Lists.newArrayList();
        List<LegacyNodeData> unorderdChildData = Lists.newArrayList();
        for (LegacyNodeData data : (List<LegacyNodeData>) expNodeData.nodeData) {
            if (isOrdered(data.nodeKey)) {
                expChildData.add(data);
            } else {
                unorderdChildData.add(data);
            }
        }

        Collections.sort(unorderdChildData, new Comparator<LegacyNodeData>() {
            @Override
            public int compare(final LegacyNodeData arg1,
                final LegacyNodeData arg2) {
                if (!(arg1.nodeData instanceof List)
                    && !(arg2.nodeData instanceof List)) {
                    // if neither is a list, just compare them
                    String str1 = arg1.nodeKey.getLocalName() + arg1.nodeData;
                    String str2 = arg2.nodeKey.getLocalName() + arg2.nodeData;
                    return str1.compareTo(str2);
                } else if (arg1.nodeData instanceof List
                    && arg2.nodeData instanceof List) {
                    // if both are lists, first check their local name
                    String str1 = arg1.nodeKey.getLocalName();
                    String str2 = arg2.nodeKey.getLocalName();
                    if (!str1.equals(str2)) {
                        return str1.compareTo(str2);
                    } else {
                        // if local names are the same, then look at the list contents
                        List<LegacyNodeData> l1 =
                            (List<LegacyNodeData>) arg1.nodeData;
                        List<LegacyNodeData> l2 =
                            (List<LegacyNodeData>) arg2.nodeData;

                        if (l1.size() != l2.size()) {
                            // if the sizes are different, use that
                            return l2.size() - l1.size();
                        } else {
                            // lastly sort and recursively check the list contents
                            Collections.sort(l1, this);
                            Collections.sort(l2, this);

                            for (int i = 0; i < l1.size(); i++) {
                                int diff = this.compare(l1.get(i), l2.get(i));
                                if (diff != 0) {
                                    return diff;
                                }
                            }
                            return 0;
                        }
                    }
                } else if (arg1.nodeData instanceof List) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        expChildData.addAll(unorderdChildData);

        List<Node<?>> actualChildNodes = Lists.newArrayList();
        List<Node<?>> unorderedChildNodes = Lists.newArrayList();
        for (Node<?> node : actualCN.getValue()) {
            if (isOrdered(node.getKey())) {
                actualChildNodes.add(node);
            } else {
                unorderedChildNodes.add(node);
            }
        }

        Collections.sort(unorderedChildNodes, new Comparator<Node<?>>() {
            @Override
            public int compare(final Node<?> n1, final Node<?> n2) {
                if (n1 instanceof SimpleNode && n2 instanceof SimpleNode) {
                    // if they're SimpleNodes just compare their strings
                    String str1 =
                        n1.getKey().getLocalName() + ((SimpleNode<?>) n1)
                            .getValue();
                    String str2 =
                        n2.getKey().getLocalName() + ((SimpleNode<?>) n2)
                            .getValue();
                    return str1.compareTo(str2);
                } else if (n1 instanceof CompositeNode
                    && n2 instanceof CompositeNode) {
                    // if they're CompositeNodes, things are more interesting
                    String str1 = n1.getKey().getLocalName();
                    String str2 = n2.getKey().getLocalName();
                    if (!str1.equals(str2)) {
                        // if their local names differ, return that difference
                        return str1.compareTo(str2);
                    } else {
                        // otherwise, we need to look at their contents
                        ArrayList<Node<?>> l1 = new ArrayList<Node<?>>(
                            ((CompositeNode) n1).getValue());
                        ArrayList<Node<?>> l2 = new ArrayList<Node<?>>(
                            ((CompositeNode) n2).getValue());

                        if (l1.size() != l2.size()) {
                            // if they have different numbers of things in them return that
                            return l2.size() - l1.size();
                        } else {
                            // otherwise, compare the individual elements, first sort them
                            Collections.sort(l1, this);
                            Collections.sort(l2, this);

                            // then compare them individually
                            for (int i = 0; i < l2.size(); i++) {
                                int diff = this.compare(l1.get(i), l2.get(i));
                                if (diff != 0) {
                                    return diff;
                                }
                            }
                            return 0;
                        }
                    }
                } else if (n1 instanceof CompositeNode
                    && n2 instanceof SimpleNode) {
                    return -1;
                } else if (n2 instanceof CompositeNode
                    && n1 instanceof SimpleNode) {
                    return 1;
                } else {
                    assertTrue("Expected either SimpleNodes CompositeNodes",
                        false);
                    return 0;
                }
            }
        });

        actualChildNodes.addAll(unorderedChildNodes);

        for (Node<?> actualChild : actualChildNodes) {
            LegacyNodeData expData =
                expChildData.isEmpty() ? null : expChildData.remove(0);
            assertNotNull(
                "Unexpected child node with key " + actualChild.getKey(),
                expData);
            assertEquals("Child node QName", expData.nodeKey,
                actualChild.getKey());

            if (expData.nodeData instanceof List) { // List represents a
                // composite node
                verifyLegacyNode(actualChild, expData);
            } else { // else a simple node
                assertTrue("Expected SimpleNode instance",
                    actualChild instanceof SimpleNode);
                assertEquals("Child node value with key " + actualChild.getKey(), expData.nodeData.getClass().getSimpleName(), actualChild
                    .getValue().getClass().getSimpleName());
                assertEquals(
                    "Child node value with key " + actualChild.getKey(),
                    expData.nodeData,
                    ((SimpleNode<?>) actualChild).getValue());
            }
        }

        if (!expChildData.isEmpty()) {
            fail("Missing child nodes: " + expChildData);
        }
    }

    private LegacyNodeData expectCompositeNode(final QName key,
        final LegacyNodeData... childData) {
        return new LegacyNodeData(key, Lists.newArrayList(childData));
    }

    private LegacyNodeData expectSimpleNode(final QName key,
        final Object value) {
        return new LegacyNodeData(key, value);
    }
}
