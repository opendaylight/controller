/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Optional;
import java.util.SortedSet;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for BatchedModifications.
 *
 * @author Thomas Pantelis
 */
public class BatchedModificationsTest extends AbstractTest {

    @Test
    public void testSerialization() {
        YangInstanceIdentifier writePath = TestModel.TEST_PATH;
        NormalizedNode writeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME))
                .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        YangInstanceIdentifier mergePath = TestModel.OUTER_LIST_PATH;
        NormalizedNode mergeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.OUTER_LIST_QNAME)).build();

        YangInstanceIdentifier deletePath = TestModel.TEST_PATH;

        final TransactionIdentifier tx1 = nextTransactionId();
        BatchedModifications batched = new BatchedModifications(tx1, DataStoreVersions.CURRENT_VERSION);
        batched.addModification(new WriteModification(writePath, writeData));
        batched.addModification(new MergeModification(mergePath, mergeData));
        batched.addModification(new DeleteModification(deletePath));
        assertFalse("isReady", batched.isReady());
        batched.setReady();
        assertTrue("isReady", batched.isReady());
        batched.setTotalMessagesSent(5);

        BatchedModifications clone = (BatchedModifications) SerializationUtils.clone(
                (Serializable) batched.toSerializable());

        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, clone.getVersion());
        assertEquals("getTransactionID", tx1, clone.getTransactionId());
        assertTrue("isReady", clone.isReady());
        assertFalse("isDoCommitOnReady", clone.isDoCommitOnReady());
        assertFalse("participatingShardNames present", clone.getParticipatingShardNames().isPresent());
        assertEquals("getTotalMessagesSent", 5, clone.getTotalMessagesSent());

        assertEquals("getModifications size", 3, clone.getModifications().size());

        WriteModification write = (WriteModification)clone.getModifications().get(0);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, write.getVersion());
        assertEquals("getPath", writePath, write.getPath());
        assertEquals("getData", writeData, write.getData());

        MergeModification merge = (MergeModification)clone.getModifications().get(1);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, merge.getVersion());
        assertEquals("getPath", mergePath, merge.getPath());
        assertEquals("getData", mergeData, merge.getData());

        DeleteModification delete = (DeleteModification)clone.getModifications().get(2);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, delete.getVersion());
        assertEquals("getPath", deletePath, delete.getPath());

        // Test with participating shard names.

        final TransactionIdentifier tx2 = nextTransactionId();
        batched = new BatchedModifications(tx2, (short)10000);
        final SortedSet<String> shardNames = ImmutableSortedSet.of("one", "two");
        batched.setReady(Optional.of(shardNames));
        batched.setDoCommitOnReady(true);
        assertTrue("isReady", batched.isReady());

        clone = (BatchedModifications) SerializationUtils.clone((Serializable) batched.toSerializable());

        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, clone.getVersion());
        assertEquals("getTransactionID", tx2, clone.getTransactionId());
        assertTrue("isReady", clone.isReady());
        assertTrue("isDoCommitOnReady", clone.isDoCommitOnReady());
        assertTrue("participatingShardNames present", clone.getParticipatingShardNames().isPresent());
        assertEquals("participatingShardNames", shardNames, clone.getParticipatingShardNames().get());
        assertEquals("getModifications size", 0, clone.getModifications().size());

        // Test not ready.

        batched = new BatchedModifications(tx2, DataStoreVersions.CURRENT_VERSION);

        clone = (BatchedModifications) SerializationUtils.clone((Serializable) batched.toSerializable());

        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, clone.getVersion());
        assertEquals("getTransactionID", tx2, clone.getTransactionId());
        assertFalse("isReady", clone.isReady());
        assertEquals("getModifications size", 0, clone.getModifications().size());

        // Test pre-Flourine

        batched = new BatchedModifications(tx2, DataStoreVersions.BORON_VERSION);
        batched.addModification(new WriteModification(writePath, writeData));
        batched.setReady(Optional.of(ImmutableSortedSet.of("one", "two")));

        clone = (BatchedModifications) SerializationUtils.clone((Serializable) batched.toSerializable());

        assertEquals("getVersion", DataStoreVersions.BORON_VERSION, clone.getVersion());
        assertEquals("getTransactionID", tx2, clone.getTransactionId());
        assertTrue("isReady", clone.isReady());
        assertFalse("participatingShardNames present", clone.getParticipatingShardNames().isPresent());
        assertEquals("getModifications size", 1, clone.getModifications().size());
    }

    @Test
    public void testBatchedModificationsReplySerialization() {
        BatchedModificationsReply clone = (BatchedModificationsReply) SerializationUtils.clone(
                (Serializable) new BatchedModificationsReply(100).toSerializable());
        assertEquals("getNumBatched", 100, clone.getNumBatched());
    }
}
