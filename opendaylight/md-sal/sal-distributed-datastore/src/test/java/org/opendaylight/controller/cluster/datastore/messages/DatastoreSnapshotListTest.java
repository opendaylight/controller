/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerSnapshot;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
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
        DatastoreSnapshot legacyConfigSnapshot = new DatastoreSnapshot("config",
                SerializationUtils.serialize(newLegacyShardManagerSnapshot("config-one", "config-two")),
                Arrays.asList(newLegacyShardSnapshot("config-one", newLegacySnapshot(CarsModel.BASE_PATH,
                        CarsModel.newCarsNode(CarsModel.newCarsMapNode(CarsModel.newCarEntry("optima",
                            BigInteger.valueOf(20000L)),CarsModel.newCarEntry("sportage",
                                BigInteger.valueOf(30000L)))))),
                    newLegacyShardSnapshot("config-two", newLegacySnapshot(PeopleModel.BASE_PATH,
                            PeopleModel.emptyContainer()))));

        DatastoreSnapshot legacyOperSnapshot = new DatastoreSnapshot("oper",
                null, Arrays.asList(newLegacyShardSnapshot("oper-one", newLegacySnapshot(TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME)))));

        DatastoreSnapshotList legacy = new DatastoreSnapshotList(Arrays.asList(legacyConfigSnapshot,
                legacyOperSnapshot));

        org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList cloned =
            (org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList)
                SerializationUtils.clone(legacy);

        assertEquals("DatastoreSnapshotList size", 2, cloned.size());
        assertDatastoreSnapshotEquals(legacyConfigSnapshot, cloned.get(0));
        assertDatastoreSnapshotEquals(legacyOperSnapshot, cloned.get(1));
    }

    private void assertDatastoreSnapshotEquals(DatastoreSnapshot legacy,
            org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot actual) {
        assertEquals("Type", legacy.getType(), actual.getType());

        if (legacy.getShardManagerSnapshot() == null) {
            assertNull("Expected null ShardManagerSnapshot", actual.getShardManagerSnapshot());
        } else {
            ShardManagerSnapshot legacyShardManagerSnapshot =
                    (ShardManagerSnapshot) SerializationUtils.deserialize(legacy.getShardManagerSnapshot());
            ShardManagerSnapshot actualShardManagerSnapshot =
                    (ShardManagerSnapshot) SerializationUtils.deserialize(actual.getShardManagerSnapshot());
            assertEquals("ShardManagerSnapshot", legacyShardManagerSnapshot.getShardList(),
                    actualShardManagerSnapshot.getShardList());
        }

        assertEquals("ShardSnapshots size", legacy.getShardSnapshots().size(), actual.getShardSnapshots().size());

        for (int i = 0; i < actual.getShardSnapshots().size(); i++) {
            ShardSnapshot legacyShardSnapshot = legacy.getShardSnapshots().get(i);
            org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot
                actualShardSnapshot = actual.getShardSnapshots().get(i);
            assertEquals("Shard name", legacyShardSnapshot.getName(), actualShardSnapshot.getName());
            assertSnapshotEquals((Snapshot) SerializationUtils.deserialize(legacyShardSnapshot.getSnapshot()),
                    (Snapshot) SerializationUtils.deserialize(actualShardSnapshot.getSnapshot()));
        }
    }

    private static void assertSnapshotEquals(Snapshot expected, Snapshot actual) {
        assertEquals("lastIndex", expected.getLastIndex(), actual.getLastIndex());
        assertEquals("lastTerm", expected.getLastTerm(), actual.getLastTerm());
        assertEquals("lastAppliedIndex", expected.getLastAppliedIndex(), actual.getLastAppliedIndex());
        assertEquals("lastAppliedTerm", expected.getLastAppliedTerm(), actual.getLastAppliedTerm());
        assertEquals("unAppliedEntries", expected.getUnAppliedEntries(), actual.getUnAppliedEntries());
        assertEquals("electionTerm", expected.getElectionTerm(), actual.getElectionTerm());
        assertEquals("electionVotedFor", expected.getElectionVotedFor(), actual.getElectionVotedFor());
        assertArrayEquals("state", expected.getState(), actual.getState());
    }

    private static ShardManagerSnapshot newLegacyShardManagerSnapshot(String... shards) {
        return ShardManagerSnapshot.forShardList(Arrays.asList(shards));
    }

    private static DatastoreSnapshot.ShardSnapshot newLegacyShardSnapshot(String name,
            org.opendaylight.controller.cluster.raft.Snapshot snapshot) {
        return new DatastoreSnapshot.ShardSnapshot(name, SerializationUtils.serialize(snapshot));
    }

    private static Snapshot newLegacySnapshot(YangInstanceIdentifier path, NormalizedNode<?, ?> node)
            throws Exception {
        DataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        dataTree.setSchemaContext(SchemaContextHelper.full());
        AbstractShardTest.writeToStore(dataTree, path, node);
        NormalizedNode<?, ?> root = AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.EMPTY);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new MetadataShardDataTreeSnapshot(root).serialize(bos);
        return Snapshot.create(bos.toByteArray(), Collections.<ReplicatedLogEntry>emptyList(), 2, 1, 2, 1, 1,
                "member-1", null);
    }
}
