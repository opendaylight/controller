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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

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
        final DataTreeCandidate candidate = actual.getCandidate();
        assertEquals("root path", expected.getRootPath(), candidate.getRootPath());
        assertCandidateNodeEquals(expected.getRootNode(), candidate.getRootNode());
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
        candidate = DataTreeCandidates.fromNormalizedNode(TestModel.TEST_PATH, Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
            .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo"))
            .build());
    }

    @Test
    public void testCandidateSerialization() throws IOException {
        final CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertEquals("payload size", 156, payload.size());
        assertEquals("serialized size", 242, SerializationUtils.serialize(payload).length);
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

    @Test
    public void testLeafSetEntryNodeCandidate() throws Exception {
        NodeWithValue<String> entryPathArg = new NodeWithValue<>(LEAF_SET, "one");
        YangInstanceIdentifier leafSetEntryPath = YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(LEAF_SET)
                .node(entryPathArg).build();

        candidate = DataTreeCandidates.fromNormalizedNode(leafSetEntryPath, Builders.leafSetEntryBuilder()
            .withNodeIdentifier(entryPathArg)
            .withValue("one")
            .build());
        CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }

    @Test
    public void testLeafSetNodeCandidate() throws Exception {
        YangInstanceIdentifier leafSetPath = YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(LEAF_SET).build();

        candidate = DataTreeCandidates.fromNormalizedNode(leafSetPath, Builders.leafSetBuilder()
            .withNodeIdentifier(new NodeIdentifier(LEAF_SET))
            .withChild(Builders.leafSetEntryBuilder()
                .withNodeIdentifier(new NodeWithValue<>(LEAF_SET, "one"))
                .withValue("one")
                .build())
            .build());
        CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }

    @Test
    public void testOrderedLeafSetNodeCandidate() throws Exception {
        YangInstanceIdentifier leafSetPath = YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(LEAF_SET).build();

        candidate = DataTreeCandidates.fromNormalizedNode(leafSetPath, Builders.orderedLeafSetBuilder()
            .withNodeIdentifier(new NodeIdentifier(LEAF_SET))
            .withChild(Builders.leafSetEntryBuilder()
                .withNodeIdentifier(new NodeWithValue<>(LEAF_SET, "one"))
                .withValue("one")
                .build())
            .build());
        CommitTransactionPayload payload = CommitTransactionPayload.create(nextTransactionId(), candidate);
        assertCandidateEquals(candidate, payload.getCandidate().getValue());
    }

    @Test
    public void testLeafNodeCandidate() throws Exception {
        YangInstanceIdentifier leafPath = YangInstanceIdentifier.builder(TestModel.TEST_PATH)
                .node(TestModel.DESC_QNAME).build();

        candidate = DataTreeCandidates.fromNormalizedNode(leafPath,
            ImmutableNodes.leafNode(TestModel.DESC_QNAME, "test"));
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
