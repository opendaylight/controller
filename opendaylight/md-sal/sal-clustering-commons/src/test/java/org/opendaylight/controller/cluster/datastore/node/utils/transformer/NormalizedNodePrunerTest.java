/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.transform.dom.DOMSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.node.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.cluster.datastore.node.utils.NormalizedNodeVisitor;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;

public class NormalizedNodePrunerTest {

    private NormalizedNodePruner prunerFullSchema;

    private NormalizedNodePruner prunerNoAugSchema;

    @Mock
    private NormalizedNodeBuilderWrapper normalizedNodeBuilderWrapper;

    @Mock
    private NormalizedNodeContainerBuilder normalizedNodeContainerBuilder;

    @Mock
    private NormalizedNode<?,?> normalizedNode;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        prunerFullSchema = new NormalizedNodePruner(TestModel.createTestContext());
        prunerNoAugSchema = new NormalizedNodePruner(TestModel.createTestContextWithoutAugmentationSchema());
        doReturn(normalizedNodeContainerBuilder).when(normalizedNodeBuilderWrapper).builder();
        doReturn(TestModel.BOOLEAN_LEAF_QNAME).when(normalizedNodeBuilderWrapper).nodeType();
        doReturn(normalizedNode).when(normalizedNodeContainerBuilder).build();
        doReturn(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME)).when(normalizedNodeBuilderWrapper).identifier();
    }

    @Test
    public void testNodesNotPrunedWhenSchemaPresent() throws IOException {
        NormalizedNodePruner pruner = prunerFullSchema;

        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(pruner);

        NormalizedNode<?, ?> expected = createTestContainer();

        normalizedNodeWriter.write(expected);

        NormalizedNode<?, ?> actual = pruner.normalizedNode();

        assertEquals(expected, actual);

    }

    @Test(expected = IllegalStateException.class)
    public void testReusePruner() throws IOException {
        NormalizedNodePruner pruner = prunerFullSchema;

        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(pruner);

        NormalizedNode<?, ?> expected = createTestContainer();

        normalizedNodeWriter.write(expected);

        NormalizedNode<?, ?> actual = pruner.normalizedNode();

        assertEquals(expected, actual);

        NormalizedNodeWriter.forStreamWriter(pruner).write(expected);

    }


    @Test
    public void testNodesPrunedWhenAugmentationSchemaNotPresent() throws IOException {
        NormalizedNodePruner pruner = prunerNoAugSchema;

        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(pruner);

        NormalizedNode<?, ?> expected = createTestContainer();

        normalizedNodeWriter.write(expected);

        NormalizedNode<?, ?> actual = pruner.normalizedNode();

        Assert.assertNotEquals(expected, actual);

        // Asserting true here instead of checking actual value because I don't want this assertion to be fragile
        assertTrue(countNodes(expected, "store:aug") > 0);

        // All nodes from the augmentation module are gone from the resulting node
        assertEquals(0, countNodes(actual, "store:aug"));
    }

    @Test
    public void testNodesPrunedWhenTestSchemaNotPresent() throws IOException {
        NormalizedNodePruner pruner = new NormalizedNodePruner(TestModel.createTestContextWithoutTestSchema());

        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(pruner);

        NormalizedNode<?, ?> expected = createTestContainer();

        normalizedNodeWriter.write(expected);

        NormalizedNode<?, ?> actual = pruner.normalizedNode();

        // Since top level schema is missing null is returned
        assertNull(actual);

        // Asserting true here instead of checking actual value because I don't want this assertion to be fragile
        assertTrue(countNodes(expected, "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test") > 0);

    }


    private static int countNodes(NormalizedNode<?,?> normalizedNode, final String namespaceFilter){
        if(normalizedNode == null){
            return 0;
        }
        final AtomicInteger count = new AtomicInteger();
        new NormalizedNodeNavigator(new NormalizedNodeVisitor() {

            @Override
            public void visitNode(int level, String parentPath, NormalizedNode<?, ?> normalizedNode) {
                if(!(normalizedNode.getIdentifier() instanceof YangInstanceIdentifier.AugmentationIdentifier)) {
                    if (normalizedNode.getIdentifier().getNodeType().getNamespace().toString().contains(namespaceFilter)) {
                        count.incrementAndGet();
                    }
                }
            }
        }).navigate(YangInstanceIdentifier.builder().build().toString(), normalizedNode);

        return count.get();
    }

    @Test(expected = IllegalStateException.class)
    public void testLeafNodeHasNoParent() throws IOException {
        prunerFullSchema.leafNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), mock(Object.class));
    }

    @Test
    public void testLeafNodeHasParent() throws IOException {
        prunerFullSchema.stack().push(normalizedNodeBuilderWrapper);
        Object o = mock(Object.class);
        prunerFullSchema.leafNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), o);

        ArgumentCaptor<NormalizedNode> captor = ArgumentCaptor.forClass(NormalizedNode.class);

        verify(normalizedNodeContainerBuilder).addChild(captor.capture());

        NormalizedNode<?, ?> value = captor.getValue();
        assertEquals(normalizedNodeBuilderWrapper.identifier().getNodeType(), value.getNodeType());
        assertEquals(normalizedNodeBuilderWrapper.identifier(), value.getIdentifier());
        assertEquals(o, value.getValue());

    }

    @Test
    public void testLeafNodeSchemaMissing() throws IOException {
        prunerNoAugSchema.stack().push(normalizedNodeBuilderWrapper);
        prunerNoAugSchema.leafNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.AUG_CONT_QNAME), mock(Object.class));
        verify(normalizedNodeContainerBuilder, never()).addChild(any(NormalizedNode.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testLeafSetEntryNodeHasNoParent() throws IOException {
        prunerFullSchema.leafSetEntryNode(mock(Object.class));
    }

    @Test
    public void testLeafSetEntryNodeHasParent() throws IOException {
        prunerFullSchema.stack().push(normalizedNodeBuilderWrapper);
        Object o = mock(Object.class);
        YangInstanceIdentifier.PathArgument nodeIdentifier
                = new YangInstanceIdentifier.NodeWithValue(normalizedNodeBuilderWrapper.identifier().getNodeType(), o);
        prunerFullSchema.leafSetEntryNode(o);

        ArgumentCaptor<NormalizedNode> captor = ArgumentCaptor.forClass(NormalizedNode.class);

        verify(normalizedNodeContainerBuilder).addChild(captor.capture());

        NormalizedNode<?, ?> value = captor.getValue();
        assertEquals(nodeIdentifier.getNodeType(), value.getNodeType());
        assertEquals(nodeIdentifier, value.getIdentifier());
        assertEquals(o, value.getValue());

    }

    @Test
    public void testLeafSetEntryNodeSchemaMissing() throws IOException {
        doReturn(new YangInstanceIdentifier.NodeIdentifier(TestModel.AUG_CONT_QNAME)).when(normalizedNodeBuilderWrapper).identifier();

        prunerNoAugSchema.stack().push(normalizedNodeBuilderWrapper);
        prunerNoAugSchema.leafSetEntryNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.AUG_CONT_QNAME));

        verify(normalizedNodeContainerBuilder, never()).addChild(any(NormalizedNode.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testAnyXMLNodeHasNoParent() throws IOException {
        prunerFullSchema.anyxmlNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), mock(Object.class));
    }

    @Test
    public void testAnyXMLNodeHasParent() throws IOException {
        prunerFullSchema.stack().push(normalizedNodeBuilderWrapper);
        YangInstanceIdentifier.NodeIdentifier nodeIdentifier = new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME);
        DOMSource o = mock(DOMSource.class);
        prunerFullSchema.anyxmlNode(nodeIdentifier, o);

        ArgumentCaptor<NormalizedNode> captor = ArgumentCaptor.forClass(NormalizedNode.class);

        verify(normalizedNodeContainerBuilder).addChild(captor.capture());

        NormalizedNode<?, ?> value = captor.getValue();
        assertEquals(nodeIdentifier.getNodeType(), value.getNodeType());
        assertEquals(nodeIdentifier, value.getIdentifier());
        assertEquals(o, value.getValue());
    }

    @Test
    public void testAnyXmlNodeSchemaMissing() throws IOException {
        prunerNoAugSchema.stack().push(normalizedNodeBuilderWrapper);
        prunerNoAugSchema.anyxmlNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.AUG_CONT_QNAME), mock(DOMSource.class));

        verify(normalizedNodeContainerBuilder, never()).addChild(any(NormalizedNode.class));
    }


    @Test
    public void testLeafSetPushesBuilderToStack() throws IOException {
        prunerFullSchema.startLeafSet(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), 10);

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof ListNodeBuilder);
    }

    @Test
    public void testStartContainerNodePushesBuilderToStack() throws IOException {
        prunerFullSchema.startContainerNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), 10);

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof DataContainerNodeAttrBuilder);
    }

    @Test
    public void testStartUnkeyedListPushesBuilderToStack() throws IOException {
        prunerFullSchema.startUnkeyedList(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), 10);

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof CollectionNodeBuilder);
    }

    @Test
    public void testStartUnkeyedListItemPushesBuilderToStack() throws IOException {
        prunerFullSchema.startUnkeyedListItem(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), 10);

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof DataContainerNodeAttrBuilder);
    }

    @Test
    public void testStartMapNodePushesBuilderToStack() throws IOException {
        prunerFullSchema.startMapNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), 10);

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof CollectionNodeBuilder);
    }

    @Test
    public void testStartMapEntryNodePushesBuilderToStack() throws IOException {
        prunerFullSchema.startMapEntryNode(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(TestModel.BOOLEAN_LEAF_QNAME,
                        ImmutableMap.<QName, Object>of(TestModel.BOOLEAN_LEAF_QNAME, "value")), 10);

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof DataContainerNodeAttrBuilder);
    }

    @Test
    public void testStartOrderedMapNodePushesBuilderToStack() throws IOException {
        prunerFullSchema.startOrderedMapNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), 10);

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof CollectionNodeBuilder);
    }

    @Test
    public void testStartChoiceNodePushesBuilderToStack() throws IOException {
        prunerFullSchema.startChoiceNode(new YangInstanceIdentifier.NodeIdentifier(TestModel.BOOLEAN_LEAF_QNAME), 10);

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof DataContainerNodeBuilder);
    }

    @Test
    public void testStartAugmentationPushesBuilderToStack() throws IOException {
        prunerFullSchema.startAugmentationNode(new YangInstanceIdentifier.AugmentationIdentifier(ImmutableSet.of(TestModel.AUG_CONT_QNAME)));

        assertEquals(1, prunerFullSchema.stack().size());
        assertNotNull(prunerFullSchema.stack().peek());
        assertTrue(prunerFullSchema.stack().peek().builder().toString(), prunerFullSchema.stack().peek().builder() instanceof DataContainerNodeBuilder);
    }

    @Test(expected = IllegalStateException.class)
    public void testEndNodeWhenNoBuildersOnStack() throws IOException {
        prunerFullSchema.endNode();
    }

    @Test
    public void testEndNodeWhenOneBuildersOnStack() throws IOException {
        prunerFullSchema.stack().push(normalizedNodeBuilderWrapper);
        prunerFullSchema.endNode();
        assertEquals(normalizedNode, prunerFullSchema.normalizedNode());
    }

    @Test
    public void testEndNodeSchemaMissing() throws IOException {
        doReturn(new YangInstanceIdentifier.NodeIdentifier(TestModel.AUG_CONT_QNAME)).when(normalizedNodeBuilderWrapper).identifier();

        prunerNoAugSchema.stack().push(normalizedNodeBuilderWrapper);
        prunerNoAugSchema.endNode();

        assertEquals(null, prunerNoAugSchema.normalizedNode());
    }

    @Test
    public void testEndNodeWhenMoreThanOneBuilderOnStack() throws IOException {
        // A little lazy in adding the "parent" builder
        prunerFullSchema.stack().push(normalizedNodeBuilderWrapper);
        prunerFullSchema.stack().push(normalizedNodeBuilderWrapper);
        prunerFullSchema.endNode();
        assertEquals(null, prunerFullSchema.normalizedNode());

        verify(normalizedNodeContainerBuilder).addChild(any(NormalizedNode.class));
    }

    private static NormalizedNode<?, ?> createTestContainer() {
        byte[] bytes1 = {1,2,3};
        LeafSetEntryNode<Object> entry1 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeWithValue(TestModel.BINARY_LEAF_LIST_QNAME, bytes1)).
                withValue(bytes1).build();

        byte[] bytes2 = {};
        LeafSetEntryNode<Object> entry2 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeWithValue(TestModel.BINARY_LEAF_LIST_QNAME, bytes2)).
                withValue(bytes2).build();

        LeafSetEntryNode<Object> entry3 = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeWithValue(TestModel.BINARY_LEAF_LIST_QNAME, null)).
                withValue(null).build();


        return TestModel.createBaseTestContainerBuilder().
                withChild(ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.BINARY_LEAF_LIST_QNAME)).
                        withChild(entry1).withChild(entry2).withChild(entry3).build()).
                withChild(ImmutableNodes.leafNode(TestModel.SOME_BINARY_DATA_QNAME, new byte[]{1, 2, 3, 4})).
                withChild(Builders.orderedMapBuilder().
                        withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.ORDERED_LIST_QNAME)).
                        withChild(ImmutableNodes.mapEntry(TestModel.ORDERED_LIST_ENTRY_QNAME,
                                TestModel.ID_QNAME, 11)).build()).
                build();
    }
}