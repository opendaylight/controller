/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.PayloadVersion;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerSnapshot;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

/**
 * Unit tests for DatastoreSnapshotList.
 *
 * @author Thomas Pantelis
 */
@Deprecated
public class DatastoreSnapshotListTest {
    @Test
    public void testSerialization() throws Exception {
        NormalizedNode<?, ?> legacyConfigRoot1 = toRootNode(CarsModel.BASE_PATH,
                CarsModel.newCarsNode(CarsModel.newCarsMapNode(CarsModel.newCarEntry("optima",
                        BigInteger.valueOf(20000L)),CarsModel.newCarEntry("sportage",
                            BigInteger.valueOf(30000L)))));

        NormalizedNode<?, ?> legacyConfigRoot2 = toRootNode(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

        ShardManagerSnapshot legacyShardManagerSnapshot = newLegacyShardManagerSnapshot("config-one", "config-two");
        DatastoreSnapshot legacyConfigSnapshot = new DatastoreSnapshot("config",
                SerializationUtils.serialize(legacyShardManagerSnapshot),
                Arrays.asList(newLegacyShardSnapshot("config-one", newLegacySnapshot(legacyConfigRoot1)),
                    newLegacyShardSnapshot("config-two", newLegacySnapshot(legacyConfigRoot2))));

        DatastoreSnapshot legacyOperSnapshot = new DatastoreSnapshot("oper",
                null, Arrays.asList(newLegacyShardSnapshot("oper-one", newLegacySnapshot(null))));

        DatastoreSnapshotList legacy = new DatastoreSnapshotList(Arrays.asList(legacyConfigSnapshot,
                legacyOperSnapshot));

        org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList cloned =
            (org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList)
                SerializationUtils.clone(legacy);

        assertEquals("DatastoreSnapshotList size", 2, cloned.size());
        assertDatastoreSnapshotEquals(legacyConfigSnapshot, cloned.get(0),
                new org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot(
                        legacyShardManagerSnapshot.getShardList(), Collections.emptyMap()),
                Optional.of(legacyConfigRoot1), Optional.of(legacyConfigRoot2));
        assertDatastoreSnapshotEquals(legacyOperSnapshot, cloned.get(1),
                (org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot)null,
                Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private void assertDatastoreSnapshotEquals(DatastoreSnapshot legacy,
            org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot actual,
            org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot expShardMgrSnapshot,
            Optional<NormalizedNode<?, ?>>... shardRoots) throws IOException {
        assertEquals("Type", legacy.getType(), actual.getType());

        if (legacy.getShardManagerSnapshot() == null) {
            assertNull("Expected null ShardManagerSnapshot", actual.getShardManagerSnapshot());
        } else {
            org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot actualShardManagerSnapshot =
                (org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot)
                    SerializationUtils.deserialize(legacy.getShardManagerSnapshot());
            assertEquals("ShardManagerSnapshot", expShardMgrSnapshot.getShardList(),
                    actualShardManagerSnapshot.getShardList());
        }

        assertEquals("ShardSnapshots size", legacy.getShardSnapshots().size(), actual.getShardSnapshots().size());

        for (int i = 0; i < actual.getShardSnapshots().size(); i++) {
            ShardSnapshot legacyShardSnapshot = legacy.getShardSnapshots().get(i);
            org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot
                actualShardSnapshot = actual.getShardSnapshots().get(i);
            assertEquals("Shard name", legacyShardSnapshot.getName(), actualShardSnapshot.getName());
            assertSnapshotEquals((Snapshot) SerializationUtils.deserialize(legacyShardSnapshot.getSnapshot()),
                    shardRoots[i], actualShardSnapshot.getSnapshot());
        }
    }

    private static void assertSnapshotEquals(Snapshot expected, Optional<NormalizedNode<?, ?>> expRoot,
            org.opendaylight.controller.cluster.raft.persisted.Snapshot actual) throws IOException {
        assertEquals("lastIndex", expected.getLastIndex(), actual.getLastIndex());
        assertEquals("lastTerm", expected.getLastTerm(), actual.getLastTerm());
        assertEquals("lastAppliedIndex", expected.getLastAppliedIndex(), actual.getLastAppliedIndex());
        assertEquals("lastAppliedTerm", expected.getLastAppliedTerm(), actual.getLastAppliedTerm());
        assertEquals("unAppliedEntries", expected.getUnAppliedEntries(), actual.getUnAppliedEntries());
        assertEquals("electionTerm", expected.getElectionTerm(), actual.getElectionTerm());
        assertEquals("electionVotedFor", expected.getElectionVotedFor(), actual.getElectionVotedFor());

        if (expRoot.isPresent()) {
            ShardDataTreeSnapshot actualSnapshot = ((ShardSnapshotState)actual.getState()).getSnapshot();
            assertEquals("ShardDataTreeSnapshot type", MetadataShardDataTreeSnapshot.class, actualSnapshot.getClass());
            assertTrue("Expected root node present", actualSnapshot.getRootNode().isPresent());
            assertEquals("Root node", expRoot.get(), actualSnapshot.getRootNode().get());
        } else {
            assertEquals("State type", EmptyState.class, actual.getState().getClass());
        }
    }

    private static ShardManagerSnapshot newLegacyShardManagerSnapshot(String... shards) {
        return ShardManagerSnapshot.forShardList(Arrays.asList(shards));
    }

    private static DatastoreSnapshot.ShardSnapshot newLegacyShardSnapshot(String name,
            org.opendaylight.controller.cluster.raft.Snapshot snapshot) {
        return new DatastoreSnapshot.ShardSnapshot(name, SerializationUtils.serialize(snapshot));
    }

    private static Snapshot newLegacySnapshot(NormalizedNode<?, ?> root)
            throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (root != null) {
            MetadataShardDataTreeSnapshot snapshot = new MetadataShardDataTreeSnapshot(root);
            try (DataOutputStream dos = new DataOutputStream(bos)) {
                PayloadVersion.BORON.writeTo(dos);
                try (ObjectOutputStream oos = new ObjectOutputStream(dos)) {
                    oos.writeObject(snapshot);
                }
            }
        }

        return Snapshot.create(bos.toByteArray(), Collections.<ReplicatedLogEntry>emptyList(), 2, 1, 2, 1, 1,
                "member-1", null);
    }

    private static NormalizedNode<?, ?> toRootNode(YangInstanceIdentifier path, NormalizedNode<?, ?> node)
            throws DataValidationFailedException {
        DataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        dataTree.setSchemaContext(SchemaContextHelper.full());
        AbstractShardTest.writeToStore(dataTree, path, node);
        return AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.EMPTY);
    }
}
