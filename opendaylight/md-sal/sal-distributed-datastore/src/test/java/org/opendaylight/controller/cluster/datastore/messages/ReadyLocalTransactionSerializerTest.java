/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import akka.actor.ExtendedActorSystem;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableSortedSet;
import java.io.NotSerializableException;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;

/**
 * Unit tests for ReadyLocalTransactionSerializer.
 *
 * @author Thomas Pantelis
 */
public class ReadyLocalTransactionSerializerTest extends AbstractTest {

    @Test
    public void testToAndFromBinary() throws NotSerializableException {
        DataTree dataTree = new InMemoryDataTreeFactory().create(
            DataTreeConfiguration.DEFAULT_OPERATIONAL, TestModel.createTestContext());
        DataTreeModification modification = dataTree.takeSnapshot().newModification();

        ContainerNode writeData = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        new WriteModification(TestModel.TEST_PATH, writeData).apply(modification);
        MapNode mergeData = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build();
        new MergeModification(TestModel.OUTER_LIST_PATH, mergeData).apply(modification);

        final SortedSet<String> shardNames = ImmutableSortedSet.of("one", "two");
        TransactionIdentifier txId = nextTransactionId();
        ReadyLocalTransaction readyMessage = new ReadyLocalTransaction(txId, modification, true,
                Optional.of(shardNames));

        final ExtendedActorSystem system = (ExtendedActorSystem) ExtendedActorSystem.create("test");
        final Object deserialized;
        try {
            final ReadyLocalTransactionSerializer serializer = new ReadyLocalTransactionSerializer(system);
            final byte[] bytes = serializer.toBinary(readyMessage);
            deserialized = serializer.fromBinary(bytes, ReadyLocalTransaction.class);
        } finally {
            TestKit.shutdownActorSystem(system);
        }

        assertNotNull("fromBinary returned null", deserialized);
        assertEquals("fromBinary return type", BatchedModifications.class, deserialized.getClass());
        BatchedModifications batched = (BatchedModifications)deserialized;
        assertEquals("getTransactionID", txId, batched.getTransactionId());
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, batched.getVersion());
        assertTrue("isReady", batched.isReady());
        assertTrue("isDoCommitOnReady", batched.isDoCommitOnReady());
        assertEquals("participatingShardNames", Optional.of(shardNames), batched.getParticipatingShardNames());

        List<Modification> batchedMods = batched.getModifications();
        assertEquals("getModifications size", 2, batchedMods.size());

        Modification mod = batchedMods.get(0);
        assertEquals("Modification type", WriteModification.class, mod.getClass());
        assertEquals("Modification getPath", TestModel.TEST_PATH, ((WriteModification)mod).getPath());
        assertEquals("Modification getData", writeData, ((WriteModification)mod).getData());

        mod = batchedMods.get(1);
        assertEquals("Modification type", MergeModification.class, mod.getClass());
        assertEquals("Modification getPath", TestModel.OUTER_LIST_PATH, ((MergeModification)mod).getPath());
        assertEquals("Modification getData", mergeData, ((MergeModification)mod).getData());
    }
}
