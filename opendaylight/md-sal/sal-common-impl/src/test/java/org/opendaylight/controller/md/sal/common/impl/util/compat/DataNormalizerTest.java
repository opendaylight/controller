/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.util.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedLeafSetNode;
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

@Deprecated
public class DataNormalizerTest {

    static class NormalizedNodeData {
        PathArgument nodeID;
        Class<?> nodeClass;
        Object nodeData; // List for a container, value Object for a leaf

        NormalizedNodeData(final PathArgument nodeID, final Class<?> nodeClass, final Object nodeData) {
            this.nodeID = nodeID;
            this.nodeClass = nodeClass;
            this.nodeData = nodeData;
        }
    }

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
            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:normalization:test", "2014-03-13", "test");
    static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
    static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");

    static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);
    static final YangInstanceIdentifier OUTER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH).node(OUTER_LIST_QNAME)
            .build();
    static final QName ONE_QNAME = QName.create(TEST_QNAME, "one");
    static final QName TWO_QNAME = QName.create(TEST_QNAME, "two");
    static final QName THREE_QNAME = QName.create(TEST_QNAME, "three");

    static final QName ANY_XML_DATA_QNAME = QName.create(TEST_QNAME, "any-xml-data");
    static final QName OUTER_CONTAINER_QNAME = QName.create(TEST_QNAME, "outer-container");
    static final QName AUGMENTED_LEAF_QNAME = QName.create(TEST_QNAME, "augmented-leaf");
    static final QName UNKEYED_LIST_QNAME = QName.create(TEST_QNAME, "unkeyed-list");
    static final QName UNORDERED_LEAF_LIST_QNAME = QName.create(TEST_QNAME, "unordered-leaf-list");
    static final QName ORDERED_LEAF_LIST_QNAME = QName.create(TEST_QNAME, "ordered-leaf-list");

    static final Short OUTER_LIST_ID = (short) 10;

    static final YangInstanceIdentifier OUTER_LIST_PATH_LEGACY = YangInstanceIdentifier.builder(TEST_PATH)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, OUTER_LIST_ID).build();

    static final YangInstanceIdentifier LEAF_TWO_PATH_LEGACY = YangInstanceIdentifier.builder(OUTER_LIST_PATH_LEGACY)
            .node(TWO_QNAME).build();

    static final QName ANY_XML_LEAF_QNAME = QName.create(TEST_QNAME, "leaf");;
    static final QName ANY_XML_INNER_QNAME = QName.create(TEST_QNAME, "inner");
    static final QName ANY_XML_INNER_LEAF_QNAME = QName.create(TEST_QNAME, "inner-leaf");

    SchemaContext createTestContext() {
        YangParserImpl parser = new YangParserImpl();
        Set<Module> modules = parser.parseYangModelsFromStreams(Collections.singletonList(DataNormalizerTest.class
                .getResourceAsStream("/normalization-test.yang")));
        return parser.resolveSchemaContext(modules);
    }

    @Test
    public void testToNormalizedInstanceIdentifier() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        YangInstanceIdentifier normalizedPath = normalizer.toNormalized(LEAF_TWO_PATH_LEGACY);

        verifyNormalizedInstanceIdentifier(normalizedPath, TEST_QNAME, OUTER_LIST_QNAME, new Object[] {
                OUTER_LIST_QNAME, ID_QNAME, OUTER_LIST_ID }, OUTER_CHOICE_QNAME, TWO_QNAME);
    }

    private void verifyNormalizedInstanceIdentifier(final YangInstanceIdentifier actual, final Object... expPath) {

        assertNotNull("Actual InstanceIdentifier is null", actual);
        assertEquals("InstanceIdentifier path length", expPath.length, Iterables.size(actual.getPathArguments()));

        for (int i = 0; i < expPath.length; i++) {
            PathArgument actualArg = Iterables.get(actual.getPathArguments(), i);
            if (expPath[i] instanceof Object[]) { // NodeIdentifierWithPredicates
                Object[] exp = (Object[]) expPath[i];
                assertEquals("Actual path arg " + (i + 1) + " class", NodeIdentifierWithPredicates.class,
                        actualArg.getClass());
                NodeIdentifierWithPredicates actualNode = (NodeIdentifierWithPredicates) actualArg;
                assertEquals("Actual path arg " + (i + 1) + " node type", exp[0], actualNode.getNodeType());
                assertEquals("Actual path arg " + (i + 1) + " key values map size", 1, actualNode.getKeyValues().size());
                Entry<QName, Object> keyValuesEntry = actualNode.getKeyValues().entrySet().iterator().next();
                assertEquals("Actual path arg " + (i + 1) + " key values map key", exp[1], keyValuesEntry.getKey());
                assertEquals("Actual path arg " + (i + 1) + " key values map value", exp[2], keyValuesEntry.getValue());
            } else if (expPath[i] instanceof Set) { // AugmentationIdentifier
                assertEquals("Actual path arg " + (i + 1) + " class", AugmentationIdentifier.class,
                        actualArg.getClass());
                AugmentationIdentifier actualNode = (AugmentationIdentifier) actualArg;
                assertEquals("Actual path arg " + (i + 1) + " PossibleChildNames", expPath[i],
                        actualNode.getPossibleChildNames());
            } else {
                assertEquals("Actual path arg " + (i + 1) + " node type", expPath[i], actualArg.getNodeType());
            }
        }
    }

    @Test
    public void testToLegacyInstanceIdentifier() throws DataNormalizationException {

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        YangInstanceIdentifier normalized = YangInstanceIdentifier.builder().node(TEST_QNAME).node(OUTER_LIST_QNAME)
                .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, OUTER_LIST_ID).node(OUTER_CHOICE_QNAME).node(TWO_QNAME)
                .build();

        YangInstanceIdentifier legacy = normalizer.toLegacy(normalized);

        assertEquals("Legacy InstanceIdentifier", LEAF_TWO_PATH_LEGACY, legacy);
    }

    @Test
    public void testToLegacyNormalizedNode() {

        ChoiceNode choiceNode1 = Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(OUTER_CHOICE_QNAME))
                .withChild(ImmutableNodes.leafNode(TWO_QNAME, "two"))
                .withChild(ImmutableNodes.leafNode(THREE_QNAME, "three")).build();

        MapEntryNode innerListEntryNode1 = Builders.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(INNER_LIST_QNAME, NAME_QNAME, "inner-name1"))
                .withChild(ImmutableNodes.leafNode(NAME_QNAME, "inner-name1"))
                .withChild(ImmutableNodes.leafNode(VALUE_QNAME, "inner-value1")).build();

        MapEntryNode innerListEntryNode2 = Builders.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(INNER_LIST_QNAME, NAME_QNAME, "inner-name2"))
                .withChild(ImmutableNodes.leafNode(NAME_QNAME, "inner-name2"))
                .withChild(ImmutableNodes.leafNode(VALUE_QNAME, "inner-value2")).build();

        OrderedMapNode innerListNode = Builders.orderedMapBuilder()
                .withNodeIdentifier(new NodeIdentifier(INNER_LIST_QNAME)).withChild(innerListEntryNode1)
                .withChild(innerListEntryNode2).build();

        Short outerListID1 = Short.valueOf((short) 10);
        MapEntryNode outerListEntryNode1 = Builders.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(OUTER_LIST_QNAME, ID_QNAME, outerListID1))
                .withChild(ImmutableNodes.leafNode(ID_QNAME, outerListID1)).withChild(choiceNode1)
                .withChild(innerListNode).build();

        ChoiceNode choiceNode2 = Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(OUTER_CHOICE_QNAME))
                .withChild(ImmutableNodes.leafNode(ONE_QNAME, "one")).build();

        Short outerListID2 = Short.valueOf((short) 20);
        MapEntryNode outerListEntryNode2 = Builders.mapEntryBuilder()
                .withNodeIdentifier(new NodeIdentifierWithPredicates(OUTER_LIST_QNAME, ID_QNAME, outerListID2))
                .withChild(ImmutableNodes.leafNode(ID_QNAME, outerListID2)).withChild(choiceNode2).build();

        MapNode outerListNode = Builders.mapBuilder().withNodeIdentifier(new NodeIdentifier(OUTER_LIST_QNAME))
                .withChild(outerListEntryNode1).withChild(outerListEntryNode2).build();

        UnkeyedListEntryNode unkeyedListEntryNode1 = Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(new NodeIdentifier(UNKEYED_LIST_QNAME))
                .withChild(ImmutableNodes.leafNode(NAME_QNAME, "unkeyed1")).build();

        UnkeyedListEntryNode unkeyedListEntryNode2 = Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(new NodeIdentifier(UNKEYED_LIST_QNAME))
                .withChild(ImmutableNodes.leafNode(NAME_QNAME, "unkeyed2")).build();

        UnkeyedListNode unkeyedListNode = Builders.unkeyedListBuilder()
                .withNodeIdentifier(new NodeIdentifier(UNKEYED_LIST_QNAME)).withChild(unkeyedListEntryNode1)
                .withChild(unkeyedListEntryNode2).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(TEST_QNAME)).withChild(outerListNode).withChild(unkeyedListNode)
                .build();

        Node<?> legacyNode = DataNormalizer.toLegacy(testContainerNode);

        verifyLegacyNode(
                legacyNode,
                expectCompositeNode(
                        TEST_QNAME,
                        expectCompositeNode(
                                OUTER_LIST_QNAME,
                                expectSimpleNode(ID_QNAME, outerListID1),
                                expectSimpleNode(TWO_QNAME, "two"),
                                expectSimpleNode(THREE_QNAME, "three"),

                                expectCompositeNode(INNER_LIST_QNAME, expectSimpleNode(NAME_QNAME, "inner-name1"),
                                        expectSimpleNode(VALUE_QNAME, "inner-value1")),

                                        expectCompositeNode(INNER_LIST_QNAME, expectSimpleNode(NAME_QNAME, "inner-name2"),
                                                expectSimpleNode(VALUE_QNAME, "inner-value2"))),
                                                expectCompositeNode(OUTER_LIST_QNAME, expectSimpleNode(ID_QNAME, outerListID2),
                                                        expectSimpleNode(ONE_QNAME, "one")),
                                                        expectCompositeNode(UNKEYED_LIST_QNAME, expectSimpleNode(NAME_QNAME, "unkeyed1")),
                                                        expectCompositeNode(UNKEYED_LIST_QNAME, expectSimpleNode(NAME_QNAME, "unkeyed2"))));

        // Conversion of Mixin type nodes is not supported.

        assertNull("Expected null returned for Mixin type node", DataNormalizer.toLegacy(outerListNode));
    }

    /**
     * Following data are constructed: <any-xml-data> <inner>
     * <inner-leaf>inner-leaf-value</inner-leaf> </inner>
     * <leaf>leaf-value</leaf> <any-xml-data>
     */
    @Test
    public void testToLegacyNormalizedNodeWithAnyXml() {

        Node<?> innerLeafChild = NodeFactory.createImmutableSimpleNode(ANY_XML_INNER_LEAF_QNAME, null,
                "inner-leaf-value");
        CompositeNode innerContainer = NodeFactory.createImmutableCompositeNode(ANY_XML_INNER_QNAME, null,
                Collections.<Node<?>> singletonList(innerLeafChild));

        Node<?> leafChild = NodeFactory.createImmutableSimpleNode(ANY_XML_LEAF_QNAME, null, "leaf-value");
        CompositeNode anyXmlNodeValue = NodeFactory.createImmutableCompositeNode(ANY_XML_DATA_QNAME, null,
                Arrays.asList(leafChild, innerContainer));

        AnyXmlNode testAnyXmlNode = Builders.anyXmlBuilder().withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
                .withValue(anyXmlNodeValue).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(TEST_QNAME)).withChild(testAnyXmlNode).build();

        DataNormalizer normalizer = new DataNormalizer(createTestContext());
        Node<?> legacyNode = normalizer.toLegacy(YangInstanceIdentifier.builder().node(TEST_QNAME).build(), testContainerNode);

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
    public void testToLegacyNormalizedNodeWithLeafLists() {

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName(TEST_QNAME);

        ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafSetBuilder = Builders.leafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(UNORDERED_LEAF_LIST_QNAME));
        for (int i = 1; i <= 3; i++) {
            leafSetBuilder.withChildValue("unordered-value" + i);
        }

        ListNodeBuilder<Object, LeafSetEntryNode<Object>> orderedLeafSetBuilder = Builders.orderedLeafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(ORDERED_LEAF_LIST_QNAME));
        for (int i = 3; i > 0; i--) {
            orderedLeafSetBuilder.withChildValue("ordered-value" + i);
        }

        ContainerNode testContainerNode = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(TEST_QNAME)).withChild(leafSetBuilder.build())
                .withChild(orderedLeafSetBuilder.build()).build();

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        Node<?> legacyNode = normalizer.toLegacy(YangInstanceIdentifier.builder().node(TEST_QNAME).build(), testContainerNode);

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
    public void testToLegacyNormalizedNodeWithAugmentation() {

        AugmentationNode augmentationNode = Builders.augmentationBuilder()
                .withNodeIdentifier(new AugmentationIdentifier(Sets.newHashSet(AUGMENTED_LEAF_QNAME)))
                .withChild(ImmutableNodes.leafNode(AUGMENTED_LEAF_QNAME, "augmented-value")).build();

        ContainerNode outerContainerNode = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(OUTER_CONTAINER_QNAME)).withChild(augmentationNode).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(TEST_QNAME)).withChild(outerContainerNode).build();

        DataNormalizer normalizer = new DataNormalizer(createTestContext());

        Node<?> legacyNode = normalizer.toLegacy(YangInstanceIdentifier.builder().node(TEST_QNAME).build(), testContainerNode);

        verifyLegacyNode(
                legacyNode,
                expectCompositeNode(
                        TEST_QNAME,
                        expectCompositeNode(OUTER_CONTAINER_QNAME,
                                expectSimpleNode(AUGMENTED_LEAF_QNAME, "augmented-value"))));
    }

    private boolean isOrdered(final QName nodeName) {
        return ORDERED_LEAF_LIST_QNAME.equals(nodeName) || INNER_LIST_QNAME.equals(nodeName);
    }

    @SuppressWarnings("unchecked")
    private void verifyLegacyNode(final Node<?> actual, final LegacyNodeData expNodeData) {

        assertNotNull("Actual Node is null", actual);
        assertTrue("Expected CompositeNode instance", actual instanceof CompositeNode);
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
            public int compare(final LegacyNodeData arg1, final LegacyNodeData arg2) {
                if (!(arg1.nodeData instanceof List) && !(arg2.nodeData instanceof List)) {
                    // if neither is a list, just compare them
                    String str1 = arg1.nodeKey.getLocalName() + arg1.nodeData;
                    String str2 = arg2.nodeKey.getLocalName() + arg2.nodeData;
                    return str1.compareTo(str2);
                } else if (arg1.nodeData instanceof List && arg2.nodeData instanceof List) {
                    // if both are lists, first check their local name
                    String str1 = arg1.nodeKey.getLocalName();
                    String str2 = arg2.nodeKey.getLocalName();
                    if (!str1.equals(str2)) {
                        return str1.compareTo(str2);
                    } else {
                        // if local names are the same, then look at the list contents
                        List<LegacyNodeData> l1 = (List<LegacyNodeData>) arg1.nodeData;
                        List<LegacyNodeData> l2 = (List<LegacyNodeData>) arg2.nodeData;

                        if (l1.size() != l2.size()) {
                            // if the sizes are different, use that
                            return l2.size() - l1.size();
                        } else {
                            // lastly sort and recursively check the list contents
                            Collections.sort(l1, this);
                            Collections.sort(l2, this);

                            for (int i = 0 ; i < l1.size() ; i++) {
                                int diff = this.compare(l1.get(i), l2.get(i));
                                if (diff != 0) {
                                    return diff;
                                }
                            }
                            return 0;
                        }
                    }
                } else if( arg1.nodeData instanceof List ) {
                    return -1;
                } else{
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
                    String str1 = n1.getKey().getLocalName() + ((SimpleNode<?>)n1).getValue();
                    String str2 = n2.getKey().getLocalName() + ((SimpleNode<?>)n2).getValue();
                    return str1.compareTo(str2);
                } else if (n1 instanceof CompositeNode && n2 instanceof CompositeNode) {
                    // if they're CompositeNodes, things are more interesting
                    String str1 = n1.getKey().getLocalName();
                    String str2 = n2.getKey().getLocalName();
                    if (!str1.equals(str2)) {
                        // if their local names differ, return that difference
                        return str1.compareTo(str2);
                    } else {
                        // otherwise, we need to look at their contents
                        ArrayList<Node<?>> l1 = new ArrayList<Node<?>>( ((CompositeNode)n1).getValue() );
                        ArrayList<Node<?>> l2 = new ArrayList<Node<?>>( ((CompositeNode)n2).getValue() );

                        if (l1.size() != l2.size()) {
                            // if they have different numbers of things in them return that
                            return l2.size() - l1.size();
                        } else {
                            // otherwise, compare the individual elements, first sort them
                            Collections.sort(l1, this);
                            Collections.sort(l2, this);

                            // then compare them individually
                            for(int i = 0 ; i < l2.size() ; i++) {
                                int diff = this.compare(l1.get(i), l2.get(i));
                                if(diff != 0){
                                    return diff;
                                }
                            }
                            return 0;
                        }
                    }
                } else if (n1 instanceof CompositeNode && n2 instanceof SimpleNode) {
                    return -1;
                } else if (n2 instanceof CompositeNode && n1 instanceof SimpleNode) {
                    return 1;
                } else {
                    assertTrue("Expected either SimpleNodes CompositeNodes", false);
                    return 0;
                }
            }
        });

        actualChildNodes.addAll(unorderedChildNodes);

        for (Node<?> actualChild : actualChildNodes) {
            LegacyNodeData expData = expChildData.isEmpty() ? null : expChildData.remove(0);
            assertNotNull("Unexpected child node with key " + actualChild.getKey(), expData);
            assertEquals("Child node QName", expData.nodeKey, actualChild.getKey());

            if (expData.nodeData instanceof List) { // List represents a
                // composite node
                verifyLegacyNode(actualChild, expData);
            } else { // else a simple node
                assertTrue("Expected SimpleNode instance", actualChild instanceof SimpleNode);
                assertEquals("Child node value with key " + actualChild.getKey(), expData.nodeData,
                        ((SimpleNode<?>) actualChild).getValue());
            }
        }

        if (!expChildData.isEmpty()) {
            fail("Missing child nodes: " + expChildData);
        }
    }

    private LegacyNodeData expectCompositeNode(final QName key, final LegacyNodeData... childData) {
        return new LegacyNodeData(key, Lists.newArrayList(childData));
    }

    private LegacyNodeData expectSimpleNode(final QName key, final Object value) {
        return new LegacyNodeData(key, value);
    }

    @Test
    public void testToNormalizedCompositeNode() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName(TEST_QNAME);

        CompositeNodeBuilder<ImmutableCompositeNode> outerListBuilder = ImmutableCompositeNode.builder();
        outerListBuilder.setQName(OUTER_LIST_QNAME);
        outerListBuilder.addLeaf(ID_QNAME, 10);
        outerListBuilder.addLeaf(ONE_QNAME, "one");

        for (int i = 3; i > 0; i--) {
            CompositeNodeBuilder<ImmutableCompositeNode> innerListBuilder = ImmutableCompositeNode.builder();
            innerListBuilder.setQName(INNER_LIST_QNAME);
            innerListBuilder.addLeaf(NAME_QNAME, "inner-name" + i);
            innerListBuilder.addLeaf(VALUE_QNAME, "inner-value" + i);
            outerListBuilder.add(innerListBuilder.toInstance());
        }

        testBuilder.add(outerListBuilder.toInstance());

        outerListBuilder = ImmutableCompositeNode.builder();
        outerListBuilder.setQName(OUTER_LIST_QNAME);
        outerListBuilder.addLeaf(ID_QNAME, 20);
        outerListBuilder.addLeaf(TWO_QNAME, "two");
        outerListBuilder.addLeaf(THREE_QNAME, "three");
        testBuilder.add(outerListBuilder.toInstance());

        for (int i = 1; i <= 2; i++) {
            CompositeNodeBuilder<ImmutableCompositeNode> unkeyedListBuilder = ImmutableCompositeNode.builder();
            unkeyedListBuilder.setQName(UNKEYED_LIST_QNAME);
            unkeyedListBuilder.addLeaf(NAME_QNAME, "unkeyed-name" + i);
            testBuilder.add(unkeyedListBuilder.toInstance());
        }

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNodeEntry = normalizer
                .toNormalized(new AbstractMap.SimpleEntry<YangInstanceIdentifier, CompositeNode>(YangInstanceIdentifier.create(
                        ImmutableList.<PathArgument> of(new NodeIdentifier(TEST_QNAME))), testBuilder.toInstance()));

        verifyNormalizedInstanceIdentifier(normalizedNodeEntry.getKey(), TEST_QNAME);

        verifyNormalizedNode(
                normalizedNodeEntry.getValue(),
                expectContainerNode(
                        TEST_QNAME,
                        expectMapNode(
                                OUTER_LIST_QNAME,
                                expectMapEntryNode(
                                        OUTER_LIST_QNAME,
                                        ID_QNAME,
                                        10,
                                        expectLeafNode(ID_QNAME, 10),
                                        expectChoiceNode(OUTER_CHOICE_QNAME, expectLeafNode(ONE_QNAME, "one")),
                                        expectOrderedMapNode(
                                                INNER_LIST_QNAME,
                                                expectMapEntryNode(INNER_LIST_QNAME, NAME_QNAME, "inner-name3",
                                                        expectLeafNode(NAME_QNAME, "inner-name3"),
                                                        expectLeafNode(VALUE_QNAME, "inner-value3")),
                                                        expectMapEntryNode(INNER_LIST_QNAME, NAME_QNAME, "inner-name2",
                                                                expectLeafNode(NAME_QNAME, "inner-name2"),
                                                                expectLeafNode(VALUE_QNAME, "inner-value2")),
                                                                expectMapEntryNode(INNER_LIST_QNAME, NAME_QNAME, "inner-name1",
                                                                        expectLeafNode(NAME_QNAME, "inner-name1"),
                                                                        expectLeafNode(VALUE_QNAME, "inner-value1")))),
                                                                        expectMapEntryNode(
                                                                                OUTER_LIST_QNAME,
                                                                                ID_QNAME,
                                                                                20,
                                                                                expectLeafNode(ID_QNAME, 20),
                                                                                expectChoiceNode(OUTER_CHOICE_QNAME, expectLeafNode(TWO_QNAME, "two"),
                                                                                        expectLeafNode(THREE_QNAME, "three")))),
                                                                                        expectUnkeyedListNode(
                                                                                                UNKEYED_LIST_QNAME,
                                                                                                expectUnkeyedListEntryNode(UNKEYED_LIST_QNAME,
                                                                                                        expectLeafNode(NAME_QNAME, "unkeyed-name1")),
                                                                                                        expectUnkeyedListEntryNode(UNKEYED_LIST_QNAME,
                                                                                                                expectLeafNode(NAME_QNAME, "unkeyed-name2")))));
    }

    @Test
    public void testToNormalizedCompositeNodeWithAnyXml() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName(TEST_QNAME);

        CompositeNodeBuilder<ImmutableCompositeNode> anyXmlBuilder = ImmutableCompositeNode.builder();
        anyXmlBuilder.setQName(ANY_XML_DATA_QNAME);
        anyXmlBuilder.addLeaf(ANY_XML_LEAF_QNAME, "leaf-value");

        CompositeNodeBuilder<ImmutableCompositeNode> innerBuilder = ImmutableCompositeNode.builder();
        innerBuilder.setQName(ANY_XML_INNER_QNAME);
        innerBuilder.addLeaf(ANY_XML_INNER_LEAF_QNAME, "inner-leaf-value");

        anyXmlBuilder.add(innerBuilder.toInstance());
        CompositeNode anyXmlLegacy = anyXmlBuilder.toInstance();
        testBuilder.add(anyXmlLegacy);

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNodeEntry = normalizer
                .toNormalized(new AbstractMap.SimpleEntry<YangInstanceIdentifier, CompositeNode>(YangInstanceIdentifier.create(
                        ImmutableList.<PathArgument> of(new NodeIdentifier(TEST_QNAME))), testBuilder.toInstance()));

        verifyNormalizedInstanceIdentifier(normalizedNodeEntry.getKey(), TEST_QNAME);

        verifyNormalizedNode(normalizedNodeEntry.getValue(),
                expectContainerNode(TEST_QNAME, expectAnyXmlNode(ANY_XML_DATA_QNAME, anyXmlLegacy)));
    }

    @Test
    public void testToNormalizedCompositeNodeWithAugmentation() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName(TEST_QNAME);

        CompositeNodeBuilder<ImmutableCompositeNode> outerContBuilder = ImmutableCompositeNode.builder();
        outerContBuilder.setQName(OUTER_CONTAINER_QNAME);
        outerContBuilder.addLeaf(AUGMENTED_LEAF_QNAME, "augmented-value");

        testBuilder.add(outerContBuilder.toInstance());

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNodeEntry = normalizer
                .toNormalized(new AbstractMap.SimpleEntry<YangInstanceIdentifier, CompositeNode>(YangInstanceIdentifier.create(
                        ImmutableList.<PathArgument> of(new NodeIdentifier(TEST_QNAME))), testBuilder.toInstance()));

        verifyNormalizedInstanceIdentifier(normalizedNodeEntry.getKey(), TEST_QNAME);

        NormalizedNodeData expAugmentation = expectAugmentation(AUGMENTED_LEAF_QNAME,
                expectLeafNode(AUGMENTED_LEAF_QNAME, "augmented-value"));

        verifyNormalizedNode(normalizedNodeEntry.getValue(),
                expectContainerNode(TEST_QNAME, expectContainerNode(OUTER_CONTAINER_QNAME, expAugmentation)));

        normalizedNodeEntry = normalizer.toNormalized(new AbstractMap.SimpleEntry<YangInstanceIdentifier, CompositeNode>(
                YangInstanceIdentifier.create(Lists.newArrayList(new NodeIdentifier(TEST_QNAME), new NodeIdentifier(
                        OUTER_CONTAINER_QNAME))), outerContBuilder.toInstance()));

    }

    @Test
    public void testToNormalizedCompositeNodeWithLeafLists() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName(TEST_QNAME);

        for (int i = 1; i <= 3; i++) {
            testBuilder.addLeaf(UNORDERED_LEAF_LIST_QNAME, "unordered-value" + i);
        }

        for (int i = 3; i > 0; i--) {
            testBuilder.addLeaf(ORDERED_LEAF_LIST_QNAME, "ordered-value" + i);
        }

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNodeEntry = normalizer
                .toNormalized(new AbstractMap.SimpleEntry<YangInstanceIdentifier, CompositeNode>(YangInstanceIdentifier.create(
                        ImmutableList.<PathArgument> of(new NodeIdentifier(TEST_QNAME))), testBuilder.toInstance()));

        verifyNormalizedInstanceIdentifier(normalizedNodeEntry.getKey(), TEST_QNAME);

        verifyNormalizedNode(
                normalizedNodeEntry.getValue(),
                expectContainerNode(
                        TEST_QNAME,
                        expectLeafSetNode(UNORDERED_LEAF_LIST_QNAME,
                                expectLeafSetEntryNode(UNORDERED_LEAF_LIST_QNAME, "unordered-value1"),
                                expectLeafSetEntryNode(UNORDERED_LEAF_LIST_QNAME, "unordered-value2"),
                                expectLeafSetEntryNode(UNORDERED_LEAF_LIST_QNAME, "unordered-value3")),
                                expectOrderedLeafSetNode(ORDERED_LEAF_LIST_QNAME,
                                        expectLeafSetEntryNode(ORDERED_LEAF_LIST_QNAME, "ordered-value3"),
                                        expectLeafSetEntryNode(ORDERED_LEAF_LIST_QNAME, "ordered-value2"),
                                        expectLeafSetEntryNode(ORDERED_LEAF_LIST_QNAME, "ordered-value1"))));
    }

    @SuppressWarnings("unchecked")
    private void verifyNormalizedNode(final NormalizedNode<?, ?> actual, final NormalizedNodeData expNodeData) {

        Class<?> expNodeClass = expNodeData.nodeClass;
        PathArgument expNodeID = expNodeData.nodeID;

        assertNotNull("Actual NormalizedNode is null", actual);
        assertTrue("NormalizedNode instance " + actual.getClass() + " is not derived from " + expNodeClass,
                expNodeClass.isAssignableFrom(actual.getClass()));
        assertEquals("NormalizedNode identifier", expNodeID, actual.getIdentifier());

        if (expNodeData.nodeData instanceof List) {
            Map<PathArgument, Integer> orderingMap = null;
            if (expNodeClass.equals(OrderedMapNode.class) || expNodeClass.equals(OrderedLeafSetNode.class)) {
                orderingMap = Maps.newHashMap();
            }

            int i = 1;
            Map<PathArgument, NormalizedNodeData> expChildDataMap = Maps.newHashMap();
            List<NormalizedNodeData> expChildDataList = (List<NormalizedNodeData>) expNodeData.nodeData;
            for (NormalizedNodeData data : expChildDataList) {
                expChildDataMap.put(data.nodeID, data);

                if (orderingMap != null) {
                    orderingMap.put(data.nodeID, i++);
                }
            }

            assertNotNull("Actual value is null for node " + actual.getIdentifier(), actual.getValue());
            assertTrue("Expected value instance Iterable for node " + actual.getIdentifier(),
                    Iterable.class.isAssignableFrom(actual.getValue().getClass()));

            i = 1;
            for (NormalizedNode<?, ?> actualChild : (Iterable<NormalizedNode<?, ?>>) actual.getValue()) {
                NormalizedNodeData expChildData = expNodeClass.equals(UnkeyedListNode.class) ? expChildDataList
                        .remove(0) : expChildDataMap.remove(actualChild.getIdentifier());

                        assertNotNull(
                                "Unexpected child node " + actualChild.getClass() + " with identifier "
                                        + actualChild.getIdentifier() + " for parent node " + actual.getClass()
                                        + " with identifier " + actual.getIdentifier(), expChildData);

                        if (orderingMap != null) {
                            assertEquals("Order index for child node " + actualChild.getIdentifier(),
                                    orderingMap.get(actualChild.getIdentifier()), Integer.valueOf(i));
                        }

                        verifyNormalizedNode(actualChild, expChildData);
                        i++;
            }

            if (expNodeClass.equals(UnkeyedListNode.class)) {
                if (expChildDataList.size() > 0) {
                    fail("Missing " + expChildDataList.size() + " child nodes for parent " + actual.getIdentifier());
                }
            } else {
                if (!expChildDataMap.isEmpty()) {
                    fail("Missing child nodes for parent " + actual.getIdentifier() + ": " + expChildDataMap.keySet());
                }
            }
        } else {
            assertEquals("Leaf value for node " + actual.getIdentifier(), expNodeData.nodeData, actual.getValue());
        }
    }

    private NormalizedNodeData expectOrderedLeafSetNode(final QName nodeName, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifier(nodeName), OrderedLeafSetNode.class,
                Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectLeafSetNode(final QName nodeName, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifier(nodeName), LeafSetNode.class, Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectLeafSetEntryNode(final QName nodeName, final Object value) {
        return new NormalizedNodeData(new NodeWithValue(nodeName, value), LeafSetEntryNode.class, value);
    }

    private NormalizedNodeData expectUnkeyedListNode(final QName nodeName, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifier(nodeName), UnkeyedListNode.class,
                Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectUnkeyedListEntryNode(final QName nodeName, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifier(nodeName), UnkeyedListEntryNode.class,
                Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectAugmentation(final QName augmentedNodeName, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new AugmentationIdentifier(Sets.newHashSet(augmentedNodeName)),
                AugmentationNode.class, Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectAnyXmlNode(final QName nodeName, final Object value) {
        return new NormalizedNodeData(new NodeIdentifier(nodeName), AnyXmlNode.class, value);
    }

    private NormalizedNodeData expectContainerNode(final QName nodeName, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifier(nodeName), ContainerNode.class, Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectChoiceNode(final QName nodeName, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifier(nodeName), ChoiceNode.class, Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectLeafNode(final QName nodeName, final Object value) {
        return new NormalizedNodeData(new NodeIdentifier(nodeName), LeafNode.class, value);

    }

    private NormalizedNodeData expectMapEntryNode(final QName nodeName, final QName key, final Object value,
            final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifierWithPredicates(nodeName, key, value), MapEntryNode.class,
                Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectMapNode(final QName key, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifier(key), MapNode.class, Lists.newArrayList(childData));
    }

    private NormalizedNodeData expectOrderedMapNode(final QName key, final NormalizedNodeData... childData) {
        return new NormalizedNodeData(new NodeIdentifier(key), OrderedMapNode.class, Lists.newArrayList(childData));
    }
}
