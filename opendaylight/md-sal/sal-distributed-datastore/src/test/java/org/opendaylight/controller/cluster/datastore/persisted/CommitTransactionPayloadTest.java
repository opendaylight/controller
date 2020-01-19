/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.persisted.DataTreeCandidateInputOutput.DataTreeCandidateWithVersion;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

public class CommitTransactionPayloadTest extends AbstractTest {
    static final QName LEAF_SET = QName.create(TestModel.TEST_QNAME, "leaf-set");

    private DataTreeCandidate candidate;

    private static DataTreeCandidateNode findNode(final Collection<DataTreeCandidateNode> nodes,
            final PathArgument arg) {
        for (DataTreeCandidateNode node : nodes) {
            if (arg.equals(node.getIdentifier())) {
                return node;
            }
        }
        return null;
    }

    private static void assertChildrenEquals(final Collection<DataTreeCandidateNode> expected,
            final Collection<DataTreeCandidateNode> actual) {
        // Make sure all expected nodes are there
        for (DataTreeCandidateNode exp : expected) {
            final DataTreeCandidateNode act = findNode(actual, exp.getIdentifier());
            assertNotNull("missing expected child", act);
            assertCandidateNodeEquals(exp, act);
        }
        // Make sure no nodes are present which are not in the expected set
        for (DataTreeCandidateNode act : actual) {
            final DataTreeCandidateNode exp = findNode(expected, act.getIdentifier());
            assertNull("unexpected child", exp);
        }
    }

    private static void assertCandidateEquals(final DataTreeCandidate expected,
            final DataTreeCandidateWithVersion actual) {
        assertEquals("root path", expected.getRootPath(), actual.candidate.getRootPath());
        assertCandidateNodeEquals(expected.getRootNode(), actual.candidate.getRootNode());
    }

    private static void assertCandidateNodeEquals(final DataTreeCandidateNode expected,
            final DataTreeCandidateNode actual) {
        assertEquals("child type", expected.getModificationType(), actual.getModificationType());

        switch (actual.getModificationType()) {
            case DELETE:
            case WRITE:
                assertEquals("child identifier", expected.getIdentifier(), actual.getIdentifier());
                assertEquals("child data", expected.getDataAfter(), actual.getDataAfter());
                break;
            case SUBTREE_MODIFIED:
                assertEquals("child identifier", expected.getIdentifier(), actual.getIdentifier());
                assertChildrenEquals(expected.getChildNodes(), actual.getChildNodes());
                break;
            case UNMODIFIED:
                break;
            default:
                fail("Unexpect root type " + actual.getModificationType());
                break;
        }
    }

    @Before
    public void setUp() {
        setUpStatic();
        final YangInstanceIdentifier writePath = TestModel.TEST_PATH;
        final NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME))
                .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();
        candidate = DataTreeCandidates.fromNormalizedNode(writePath, writeData);
    }

    @Test
    public void testCandidateSerialization() throws IOException {
        final CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertEquals("payload size", 156, payload.size());
    }

    @Test
    public void testCandidateSerDes() throws IOException {
        final CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }

    @Test
    public void testPayloadSerDes() throws IOException {
        final CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, SerializationUtils.clone(payload).getCandidate().getValue());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testLeafSetEntryNodeCandidate() throws Exception {
        YangInstanceIdentifier.NodeWithValue entryPathArg = new YangInstanceIdentifier.NodeWithValue(LEAF_SET, "one");
        YangInstanceIdentifier leafSetEntryPath = YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(LEAF_SET)
                .node(entryPathArg).build();

        NormalizedNode<?, ?> leafSetEntryNode = Builders.leafSetEntryBuilder().withNodeIdentifier(entryPathArg)
                .withValue("one").build();

        candidate = DataTreeCandidates.fromNormalizedNode(leafSetEntryPath, leafSetEntryNode);
        CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testLeafSetNodeCandidate() throws Exception {
        YangInstanceIdentifier.NodeWithValue entryPathArg = new YangInstanceIdentifier.NodeWithValue(LEAF_SET, "one");
        YangInstanceIdentifier leafSetPath = YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(LEAF_SET).build();

        LeafSetEntryNode leafSetEntryNode = Builders.leafSetEntryBuilder().withNodeIdentifier(entryPathArg)
                .withValue("one").build();
        NormalizedNode<?, ?> leafSetNode = Builders.leafSetBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(LEAF_SET)).withChild(leafSetEntryNode).build();

        candidate = DataTreeCandidates.fromNormalizedNode(leafSetPath, leafSetNode);
        CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testOrderedLeafSetNodeCandidate() throws Exception {
        YangInstanceIdentifier.NodeWithValue entryPathArg = new YangInstanceIdentifier.NodeWithValue(LEAF_SET, "one");
        YangInstanceIdentifier leafSetPath = YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(LEAF_SET).build();

        LeafSetEntryNode leafSetEntryNode = Builders.leafSetEntryBuilder().withNodeIdentifier(entryPathArg)
                .withValue("one").build();
        NormalizedNode<?, ?> leafSetNode = Builders.orderedLeafSetBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(LEAF_SET)).withChild(leafSetEntryNode).build();

        candidate = DataTreeCandidates.fromNormalizedNode(leafSetPath, leafSetNode);
        CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }

    @Test
    public void testLeafNodeCandidate() throws Exception {
        YangInstanceIdentifier leafPath = YangInstanceIdentifier.builder(TestModel.TEST_PATH)
                .node(TestModel.DESC_QNAME).build();
        LeafNode<Object> leafNode = Builders.leafBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.DESC_QNAME)).withValue("test").build();

        candidate = DataTreeCandidates.fromNormalizedNode(leafPath, leafNode);
        CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }

    @Test
    public void testUnmodifiedRootCandidate() throws Exception {
        final DataTree dataTree = new InMemoryDataTreeFactory().create(
            DataTreeConfiguration.DEFAULT_CONFIGURATION, SchemaContextHelper.select(SchemaContextHelper.CARS_YANG));

        DataTreeModification modification = dataTree.takeSnapshot().newModification();
        modification.ready();
        candidate = dataTree.prepare(modification);

        CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }
}
