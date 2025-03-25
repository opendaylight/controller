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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;

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
                        TestModel.EMPTY_TEST))));

        DatastoreSnapshotList snapshotList = new DatastoreSnapshotList(Arrays.asList(configSnapshot, operSnapshot));

        try (var fos = Files.newOutputStream(backupFile.toPath())) {
            SerializationUtils.serialize(snapshotList, fos);
        }

        DefaultDatastoreSnapshotRestore instance = new DefaultDatastoreSnapshotRestore(restoreDirectoryPath);
        instance.activate();

        assertDatastoreSnapshotEquals(configSnapshot, instance.getAndRemove("config").orElse(null));
        assertDatastoreSnapshotEquals(operSnapshot, instance.getAndRemove("oper").orElse(null));

        assertEquals("DatastoreSnapshot was not removed", Optional.empty(), instance.getAndRemove("config"));

        assertFalse(backupFile + " was not deleted", backupFile.exists());
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
        assertEquals(prefix + " electionTerm", expected.termInfo(), actual.termInfo());
        assertEquals(prefix + " Root node", ((ShardSnapshotState)expected.getState()).getSnapshot().getRootNode(),
                ((ShardSnapshotState)actual.getState()).getSnapshot().getRootNode());
    }

    private static ShardManagerSnapshot newShardManagerSnapshot(final String... shards) {
        return new ShardManagerSnapshot(Arrays.asList(shards));
    }

    private static Snapshot newSnapshot(final YangInstanceIdentifier path, final NormalizedNode node) throws Exception {
        DataTree dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL,
            SchemaContextHelper.full());
        AbstractShardTest.writeToStore(dataTree, path, node);
        NormalizedNode root = AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.of());

        return Snapshot.create(new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
                List.of(), 2, 1, 2, 1, new TermInfo(1, "member-1"), null);
    }
}
