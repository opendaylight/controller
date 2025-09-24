/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntry;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntryBuilder;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapNodeBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.transform.dom.DOMSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.datastore.node.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyxmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class NormalizedNodePrunerTest {
    private static final EffectiveModelContext NO_TEST_SCHEMA = TestModel.createTestContextWithoutTestSchema();
    private static final EffectiveModelContext NO_AUG_SCHEMA = TestModel.createTestContextWithoutAugmentationSchema();
    private static final EffectiveModelContext FULL_SCHEMA = TestModel.createTestContext();

    private static AbstractNormalizedNodePruner prunerFullSchema(final YangInstanceIdentifier path) {
        final ReusableNormalizedNodePruner pruner = ReusableNormalizedNodePruner.forSchemaContext(FULL_SCHEMA);
        pruner.initializeForPath(path);
        return pruner;
    }

    private static AbstractNormalizedNodePruner prunerNoAugSchema(final YangInstanceIdentifier path) {
        final ReusableNormalizedNodePruner pruner = ReusableNormalizedNodePruner.forSchemaContext(NO_AUG_SCHEMA);
        pruner.initializeForPath(path);
        return pruner;
    }

    private static AbstractNormalizedNodePruner prunerNoTestSchema(final YangInstanceIdentifier path) {
        final ReusableNormalizedNodePruner pruner = ReusableNormalizedNodePruner.forSchemaContext(NO_TEST_SCHEMA);
        pruner.initializeForPath(path);
        return pruner;
    }

    @Test
    public void testNodesNotPrunedWhenSchemaPresent() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH);

        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(pruner);

        NormalizedNode expected = createTestContainer();

        normalizedNodeWriter.write(expected);

        NormalizedNode actual = pruner.getResult().orElseThrow();

        assertEquals(expected, actual);
    }

    @Test(expected = IllegalStateException.class)
    public void testReusePruner() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH);

        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(pruner);

        NormalizedNode expected = createTestContainer();

        normalizedNodeWriter.write(expected);

        NormalizedNode actual = pruner.getResult().orElseThrow();

        assertEquals(expected, actual);

        NormalizedNodeWriter.forStreamWriter(pruner).write(expected);
    }

    @Test
    public void testNodesPrunedWhenAugmentationSchemaMissing() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerNoAugSchema(TestModel.TEST_PATH);

        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(pruner);

        NormalizedNode expected = createTestContainer();

        normalizedNodeWriter.write(expected);

        NormalizedNode actual = pruner.getResult().orElseThrow();

        assertNotEquals(expected, actual);

        // Asserting true here instead of checking actual value because I don't want this assertion to be fragile
        assertTrue(countNodes(expected, "store:aug") > 0);

        // All nodes from the augmentation module are gone from the resulting node
        assertEquals(0, countNodes(actual, "store:aug"));
    }

    @Test
    public void testNodesPrunedWhenTestSchemaMissing() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerNoTestSchema(TestModel.TEST_PATH);

        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(pruner);

        NormalizedNode expected = createTestContainer();

        normalizedNodeWriter.write(expected);

        // Since top level schema is missing empty is returned
        assertEquals(Optional.empty(), pruner.getResult());

        // Asserting true here instead of checking actual value because I don't want this assertion to be fragile
        assertTrue(countNodes(expected, "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test") > 0);
    }

    private static int countNodes(final NormalizedNode normalizedNode, final String namespaceFilter) {
        if (normalizedNode == null) {
            return 0;
        }
        final AtomicInteger count = new AtomicInteger();
        new NormalizedNodeNavigator((level, parentPath, normalizedNode1) -> {
            if (normalizedNode1.name().getNodeType().getNamespace().toString().contains(namespaceFilter)) {
                count.incrementAndGet();
            }
        }).navigate(YangInstanceIdentifier.of().toString(), normalizedNode);

        return count.get();
    }

    @Test
    public void testLeafNodeNotPrunedWhenHasNoParent() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH.node(TestModel.DESC_QNAME));
        NormalizedNode input = ImmutableNodes.leafNode(TestModel.DESC_QNAME, "test");
        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        assertEquals("normalizedNode", input, pruner.getResult().orElseThrow());
    }

    @Test
    public void testLeafNodePrunedWhenHasNoParentAndSchemaMissing() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH.node(TestModel.INVALID_QNAME));
        LeafNode<String> input = ImmutableNodes.leafNode(TestModel.INVALID_QNAME, "test");
        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        assertEquals(Optional.empty(), pruner.getResult());
    }

    @Test
    public void testLeafSetEntryNodeNotPrunedWhenHasNoParent() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH.node(TestModel.SHOE_QNAME));
        LeafSetEntryNode<?> input = ImmutableNodes.leafSetEntry(TestModel.SHOE_QNAME, "puma");
        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        NormalizedNode actual = pruner.getResult().orElseThrow();
        assertEquals("normalizedNode", input, actual);
    }

    @Test
    public void testLeafSetEntryNodeNotPrunedWhenHasParent() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH.node(TestModel.SHOE_QNAME));
        SystemLeafSetNode<?> input = ImmutableNodes.<String>newSystemLeafSetBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.SHOE_QNAME))
            .withChildValue("puma")
            .build();
        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        NormalizedNode actual = pruner.getResult().orElseThrow();
        assertEquals("normalizedNode", input, actual);
    }

    @Test
    public void testLeafSetEntryNodePrunedWhenHasNoParentAndSchemaMissing() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH.node(TestModel.INVALID_QNAME));
        LeafSetEntryNode<?> input = ImmutableNodes.leafSetEntry(TestModel.INVALID_QNAME, "test");
        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        assertEquals(Optional.empty(), pruner.getResult());
    }

    @Test
    public void testLeafSetEntryNodePrunedWhenHasParentAndSchemaMissing() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH.node(TestModel.INVALID_QNAME));
        NormalizedNodeWriter.forStreamWriter(pruner).write(ImmutableNodes.<String>newSystemLeafSetBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.INVALID_QNAME))
            .withChildValue("test")
            .build());

        assertEquals(Optional.empty(), pruner.getResult());
    }

    @Test
    public void testAnyXMLNodeNotPrunedWhenHasNoParent() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerFullSchema(TestModel.TEST_PATH.node(TestModel.ANY_XML_QNAME));
        AnyxmlNode<DOMSource> input = ImmutableNodes.newAnyxmlBuilder(DOMSource.class)
            .withNodeIdentifier(new NodeIdentifier(TestModel.ANY_XML_QNAME))
            .withValue(mock(DOMSource.class))
            .build();
        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        assertEquals(input, pruner.getResult().orElseThrow());
    }

    @Test
    public void testAnyXMLNodeNotPrunedWhenHasParent() throws IOException {
        final var pruner = prunerFullSchema(TestModel.TEST_PATH);
        final var input = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
            .withChild(ImmutableNodes.newAnyxmlBuilder(DOMSource.class)
                .withNodeIdentifier(new NodeIdentifier(TestModel.ANY_XML_QNAME))
                .withValue(mock(DOMSource.class))
                .build())
            .build();
        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        assertEquals(input, pruner.getResult().orElseThrow());
    }

    @Test
    public void testAnyXmlNodePrunedWhenHasNoParentAndSchemaMissing() throws IOException {
        AbstractNormalizedNodePruner pruner = prunerNoTestSchema(TestModel.TEST_PATH.node(TestModel.ANY_XML_QNAME));
        NormalizedNodeWriter.forStreamWriter(pruner).write(ImmutableNodes.newAnyxmlBuilder(DOMSource.class)
            .withNodeIdentifier(new NodeIdentifier(TestModel.ANY_XML_QNAME))
            .withValue(mock(DOMSource.class))
            .build());

        assertEquals(Optional.empty(), pruner.getResult());
    }

    @Test
    public void testInnerContainerNodeWithFullPathPathNotPruned() throws IOException {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(TestModel.TEST_QNAME)
                .node(TestModel.OUTER_LIST_QNAME).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                .node(TestModel.INNER_LIST_QNAME).nodeWithKey(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, "one")
                .node(TestModel.INNER_CONTAINER_QNAME).build();
        AbstractNormalizedNodePruner pruner = prunerFullSchema(path);

        ContainerNode input = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.INNER_CONTAINER_QNAME))
            .build();

        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        assertEquals(input, pruner.getResult().orElseThrow());
    }

    @Test
    public void testInnerContainerNodeWithFullPathPrunedWhenSchemaMissing() throws IOException {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(TestModel.TEST_QNAME)
                .node(TestModel.OUTER_LIST_QNAME).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                .node(TestModel.INNER_LIST_QNAME).nodeWithKey(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, "one")
                .node(TestModel.INVALID_QNAME).build();
        AbstractNormalizedNodePruner pruner = prunerFullSchema(path);

        NormalizedNodeWriter.forStreamWriter(pruner).write(ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.INVALID_QNAME))
            .build());

        assertEquals(Optional.empty(), pruner.getResult());
    }

    @Test
    public void testInnerContainerNodeWithParentPathPrunedWhenSchemaMissing() throws IOException {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(TestModel.TEST_QNAME)
                .node(TestModel.OUTER_LIST_QNAME).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                .build();
        AbstractNormalizedNodePruner pruner = prunerFullSchema(path);

        NormalizedNodeWriter.forStreamWriter(pruner)
            .write(mapEntryBuilder(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                .withChild(mapNodeBuilder(TestModel.INNER_LIST_QNAME)
                    .withChild(mapEntryBuilder(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, "one")
                        .withChild(ImmutableNodes.newContainerBuilder()
                            .withNodeIdentifier(new NodeIdentifier(TestModel.INVALID_QNAME))
                            .build())
                        .build())
                    .build())
                .build());

        assertEquals(mapEntryBuilder(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
            .withChild(mapNodeBuilder(TestModel.INNER_LIST_QNAME)
                .withChild(mapEntryBuilder(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, "one").build())
                .build())
            .build(), pruner.getResult().orElseThrow());
    }

    @Test
    public void testInnerListNodeWithFullPathNotPruned() throws IOException {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(TestModel.TEST_QNAME)
                .node(TestModel.OUTER_LIST_QNAME).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                .node(TestModel.INNER_LIST_QNAME).build();
        AbstractNormalizedNodePruner pruner = prunerFullSchema(path);

        SystemMapNode input = mapNodeBuilder(TestModel.INNER_LIST_QNAME)
            .withChild(mapEntryBuilder(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, "one")
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(new NodeIdentifier(TestModel.INNER_CONTAINER_QNAME))
                    .build())
                .build())
            .build();
        NormalizedNodeWriter.forStreamWriter(pruner).write(input);

        assertEquals(input, pruner.getResult().orElseThrow());
    }

    @Test
    public void testInnerListNodeWithFullPathPrunedWhenSchemaMissing() throws IOException {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(TestModel.TEST_QNAME)
                .node(TestModel.OUTER_LIST_QNAME).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                .node(TestModel.INVALID_QNAME).build();
        AbstractNormalizedNodePruner pruner = prunerFullSchema(path);

        NormalizedNodeWriter.forStreamWriter(pruner).write(mapNodeBuilder(TestModel.INVALID_QNAME)
            .withChild(mapEntryBuilder(TestModel.INVALID_QNAME, TestModel.NAME_QNAME, "one")
                .withChild(ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(new NodeIdentifier(TestModel.INNER_CONTAINER_QNAME))
                    .build())
                .build())
            .build());

        assertEquals(Optional.empty(), pruner.getResult());
    }

    @Test
    public void testInnerListNodeWithParentPathPrunedWhenSchemaMissing() throws IOException {
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(TestModel.TEST_QNAME)
                .node(TestModel.OUTER_LIST_QNAME).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                .build();
        AbstractNormalizedNodePruner pruner = prunerFullSchema(path);

        NormalizedNodeWriter.forStreamWriter(pruner)
            .write(mapEntryBuilder(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                .withChild(mapNodeBuilder(TestModel.INVALID_QNAME)
                    .withChild(mapEntryBuilder(TestModel.INVALID_QNAME, TestModel.NAME_QNAME, "one")
                        .withChild(ImmutableNodes.newContainerBuilder()
                            .withNodeIdentifier(new NodeIdentifier(TestModel.INNER_CONTAINER_QNAME))
                            .build())
                        .build())
                    .build())
                .build());

        assertEquals(mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1),
            pruner.getResult().orElseThrow());
    }

    private static ContainerNode createTestContainer() {
        return TestModel.createBaseTestContainerBuilder()
            .withChild(ImmutableNodes.newSystemLeafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(TestModel.BINARY_LEAF_LIST_QNAME))
                .withChildValue(new byte[] {1, 2, 3})
                .withChildValue(new byte[0])
                .build())
            .withChild(ImmutableNodes.leafNode(TestModel.SOME_BINARY_DATA_QNAME, new byte[] {1, 2, 3, 4}))
            .build();
    }
}
