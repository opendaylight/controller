/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.testkit.TestActorRef;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardTestKit;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationByteStringPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

/**
 * Unit tests for backwards compatibility with pre-Lithium versions.
 *
 * @author Thomas Pantelis
 */
public class PreLithiumShardTest extends AbstractShardTest {

    private static CompositeModificationPayload newLegacyPayload(final Modification... mods) {
        MutableCompositeModification compMod = new MutableCompositeModification(DataStoreVersions.HELIUM_2_VERSION);
        for(Modification mod: mods) {
            compMod.addModification(mod);
        }

        return new CompositeModificationPayload(compMod.toSerializable());
    }

    private static CompositeModificationByteStringPayload newLegacyByteStringPayload(final Modification... mods) {
        MutableCompositeModification compMod = new MutableCompositeModification(DataStoreVersions.HELIUM_2_VERSION);
        for(Modification mod: mods) {
            compMod.addModification(mod);
        }

        return new CompositeModificationByteStringPayload(compMod.toSerializable());
    }

    @Test
    public void testApplyHelium2VersionSnapshot() throws Exception {
        TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(),
                "testApplyHelium2VersionSnapshot");

        NormalizedNodeToNodeCodec codec = new NormalizedNodeToNodeCodec(SCHEMA_CONTEXT);

        DataTree store = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        store.setSchemaContext(SCHEMA_CONTEXT);

        writeToStore(store, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        YangInstanceIdentifier root = YangInstanceIdentifier.builder().build();
        NormalizedNode<?,?> expected = readStore(store, root);

        NormalizedNodeMessages.Container encode = codec.encode(expected);

        Snapshot snapshot = Snapshot.create(encode.getNormalizedNode().toByteString().toByteArray(),
                Collections.<ReplicatedLogEntry>emptyList(), 1, 2, 3, 4);

        shard.underlyingActor().getRaftActorSnapshotCohort().applySnapshot(snapshot.getState());

        NormalizedNode<?,?> actual = readStore(shard, root);

        assertEquals("Root node", expected, actual);

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    public void testHelium2VersionApplyStateLegacy() throws Exception {
        new ShardTestKit(getSystem()) {{
            TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(),
                    "testHelium2VersionApplyStateLegacy");

            waitUntilLeader(shard);

            NormalizedNode<?, ?> node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

            ApplyState applyState = new ApplyState(null, "test", new ReplicatedLogImplEntry(1, 2,
                    newLegacyByteStringPayload(new WriteModification(TestModel.TEST_PATH, node))));

            shard.underlyingActor().onReceiveCommand(applyState);

            NormalizedNode<?,?> actual = readStore(shard, TestModel.TEST_PATH);
            assertEquals("Applied state", node, actual);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testHelium2VersionRecovery() throws Exception {

        DataTree testStore = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        testStore.setSchemaContext(SCHEMA_CONTEXT);

        writeToStore(testStore, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        NormalizedNode<?, ?> root = readStore(testStore, YangInstanceIdentifier.builder().build());

        InMemorySnapshotStore.addSnapshot(shardID.toString(), Snapshot.create(
                new NormalizedNodeToNodeCodec(SCHEMA_CONTEXT).encode(root).
                                getNormalizedNode().toByteString().toByteArray(),
                                Collections.<ReplicatedLogEntry>emptyList(), 0, 1, -1, -1));

        InMemoryJournal.addEntry(shardID.toString(), 0, new String("Dummy data as snapshot sequence number is " +
                "set to 0 in InMemorySnapshotStore and journal recovery seq number will start from 1"));

        // Set up the InMemoryJournal.

        InMemoryJournal.addEntry(shardID.toString(), 1, new ReplicatedLogImplEntry(0, 1, newLegacyPayload(
                  new WriteModification(TestModel.OUTER_LIST_PATH,
                          ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build()))));

        int nListEntries = 16;
        Set<Integer> listEntryKeys = new HashSet<>();
        int i = 1;

        // Add some CompositeModificationPayload entries
        for(; i <= 8; i++) {
            listEntryKeys.add(Integer.valueOf(i));
            YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i).build();
            Modification mod = new MergeModification(path,
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i));
            InMemoryJournal.addEntry(shardID.toString(), i+1, new ReplicatedLogImplEntry(i, 1,
                    newLegacyPayload(mod)));
        }

        // Add some CompositeModificationByteStringPayload entries
        for(; i <= nListEntries; i++) {
            listEntryKeys.add(Integer.valueOf(i));
            YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i).build();
            Modification mod = new MergeModification(path,
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i));
            InMemoryJournal.addEntry(shardID.toString(), i+1, new ReplicatedLogImplEntry(i, 1,
                    newLegacyByteStringPayload(mod)));
        }

        InMemoryJournal.addEntry(shardID.toString(), nListEntries + 2, new ApplyLogEntries(nListEntries));

        testRecovery(listEntryKeys);
    }
}
