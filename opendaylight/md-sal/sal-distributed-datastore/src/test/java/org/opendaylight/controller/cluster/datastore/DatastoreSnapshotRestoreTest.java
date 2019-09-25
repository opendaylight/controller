/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

/**
 * Unit tests for DatastoreSnapshotRestore.
 *
 * @author Thomas Pantelis
 */
public class DatastoreSnapshotRestoreTest {
    String restoreDirectoryPath = "target/DatastoreSnapshotRestoreTest-" + System.nanoTime();
    File restoreDirectoryFile = new File(restoreDirectoryPath);
    File backupFile = new File(restoreDirectoryFile, "backup");

    @After
    public void tearDown() {
        backupFile.delete();
        restoreDirectoryFile.delete();
    }

    @Test
    public void test() throws Exception {
        assertTrue("Failed to mkdir " + restoreDirectoryPath, restoreDirectoryFile.mkdirs());

        final DatastoreSnapshot configSnapshot = new DatastoreSnapshot("config",
                newShardManagerSnapshot("config-one", "config-two"),
                Arrays.asList(new DatastoreSnapshot.ShardSnapshot("config-one", newSnapshot(CarsModel.BASE_PATH,
                        CarsModel.newCarsNode(CarsModel.newCarsMapNode(CarsModel.newCarEntry("optima",
                            Uint64.valueOf(20000)),CarsModel.newCarEntry("sportage", Uint64.valueOf(30000)))))),
                        new DatastoreSnapshot.ShardSnapshot("config-two", newSnapshot(PeopleModel.BASE_PATH,
                            PeopleModel.emptyContainer()))));

        DatastoreSnapshot operSnapshot = new DatastoreSnapshot("oper",
                null, Arrays.asList(new DatastoreSnapshot.ShardSnapshot("oper-one", newSnapshot(TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME)))));

        DatastoreSnapshotList snapshotList = new DatastoreSnapshotList(Arrays.asList(configSnapshot, operSnapshot));

        try (FileOutputStream fos = new FileOutputStream(backupFile)) {
            SerializationUtils.serialize(snapshotList, fos);
        }

        DatastoreSnapshotRestore instance = DatastoreSnapshotRestore.instance(restoreDirectoryPath);

        assertDatastoreSnapshotEquals(configSnapshot, instance.getAndRemove("config"));
        assertDatastoreSnapshotEquals(operSnapshot, instance.getAndRemove("oper"));

        assertNull("DatastoreSnapshot was not removed", instance.getAndRemove("config"));

        assertFalse(backupFile + " was not deleted", backupFile.exists());

        instance = DatastoreSnapshotRestore.instance(restoreDirectoryPath);
        assertNull("Expected null DatastoreSnapshot", instance.getAndRemove("config"));
        assertNull("Expected null DatastoreSnapshot", instance.getAndRemove("oper"));
    }

    private static void assertDatastoreSnapshotEquals(final DatastoreSnapshot expected,
            final DatastoreSnapshot actual) {
        assertNotNull("DatastoreSnapshot is null", actual);
        assertEquals("getType", expected.getType(), actual.getType());

        if (expected.getShardManagerSnapshot() == null) {
            assertNull("Expected null ShardManagerSnapshot", actual.getShardManagerSnapshot());
        } else {
            assertEquals("ShardManagerSnapshot", expected.getShardManagerSnapshot().getShardList(),
                    actual.getShardManagerSnapshot().getShardList());
        }

        assertEquals("ShardSnapshots size", expected.getShardSnapshots().size(), actual.getShardSnapshots().size());
        for (int i = 0; i < expected.getShardSnapshots().size(); i++) {
            assertEquals("ShardSnapshot " + (i + 1) + " name", expected.getShardSnapshots().get(i).getName(),
                    actual.getShardSnapshots().get(i).getName());
            assertSnapshotEquals("ShardSnapshot " + (i + 1) + " snapshot",
                    expected.getShardSnapshots().get(i).getSnapshot(), actual.getShardSnapshots().get(i).getSnapshot());
        }
    }

    private static void assertSnapshotEquals(final String prefix, final Snapshot expected, final Snapshot actual) {
        assertEquals(prefix + " lastIndex", expected.getLastIndex(), actual.getLastIndex());
        assertEquals(prefix + " lastTerm", expected.getLastTerm(), actual.getLastTerm());
        assertEquals(prefix + " lastAppliedIndex", expected.getLastAppliedIndex(), actual.getLastAppliedIndex());
        assertEquals(prefix + " lastAppliedTerm", expected.getLastAppliedTerm(), actual.getLastAppliedTerm());
        assertEquals(prefix + " unAppliedEntries", expected.getUnAppliedEntries(), actual.getUnAppliedEntries());
        assertEquals(prefix + " electionTerm", expected.getElectionTerm(), actual.getElectionTerm());
        assertEquals(prefix + " electionVotedFor", expected.getElectionVotedFor(), actual.getElectionVotedFor());
        assertEquals(prefix + " Root node", ((ShardSnapshotState)expected.getState()).getSnapshot().getRootNode(),
                ((ShardSnapshotState)actual.getState()).getSnapshot().getRootNode());
    }

    private static ShardManagerSnapshot newShardManagerSnapshot(final String... shards) {
        return new ShardManagerSnapshot(Arrays.asList(shards), Collections.emptyMap());
    }

    private static Snapshot newSnapshot(final YangInstanceIdentifier path, final NormalizedNode<?, ?> node)
            throws Exception {
        DataTree dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL,
            SchemaContextHelper.full());
        AbstractShardTest.writeToStore(dataTree, path, node);
        NormalizedNode<?, ?> root = AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.empty());

        return Snapshot.create(new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
                Collections.<ReplicatedLogEntry>emptyList(), 2, 1, 2, 1, 1, "member-1", null);
    }
}
