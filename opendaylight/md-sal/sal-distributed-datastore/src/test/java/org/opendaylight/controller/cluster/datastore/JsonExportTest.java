/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class JsonExportTest extends AbstractShardTest {
    private static final String DUMMY_DATA = "Dummy data as snapshot sequence number is set to 0 in "
            + "InMemorySnapshotStore and journal recovery seq number will start from 1";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final DatastoreContext.Builder dataStoreContextBuilder = DatastoreContext.newBuilder()
            .shardJournalRecoveryLogBatchSize(1).shardSnapshotBatchCount(5000)
            .shardHeartbeatIntervalInMillis(HEARTBEAT_MILLIS).exportOnRecovery(true)
            .recoveryExportBaseDir(temporaryFolder.toString());

    private static final String EXPECTED_JOURNAL_FILE = "expectedJournalExport.json";
    private static final String ACTUAL_JOURNAL_FILE = temporaryFolder.toString() + "/journals/"
            + "member-1-shard-inventory-config" + NEXT_SHARD_NUM.get() + "-journal.json";
    private static final String EXPECTED_SNAPSHOT_FILE = "expectedSnapshotExport.json";
    private static final String ACTUAL_SNAPSHOT_FILE = temporaryFolder.toString() + "/snapshots/"
            + "member-1-shard-inventory-config" + NEXT_SHARD_NUM.get() + "-snapshot.json";

    @Override
    protected DatastoreContext newDatastoreContext() {
        return dataStoreContextBuilder.build();
    }

    @Test
    public void testJsonExport() throws Exception {
        // Set up the InMemorySnapshotStore.
        final DataTree source = setupInMemorySnapshotStore();

        final DataTreeModification writeMod = source.takeSnapshot().newModification();
        writeMod.write(TestModel.OUTER_LIST_PATH, ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());
        writeMod.ready();
        InMemoryJournal.addEntry(shardID.toString(), 0, DUMMY_DATA);

        // Set up the InMemoryJournal.
        InMemoryJournal.addEntry(shardID.toString(), 1, new SimpleReplicatedLogEntry(0, 1,
                payloadForModification(source, writeMod, nextTransactionId())));

        final int nListEntries = 16;
        final Set<Integer> listEntryKeys = new HashSet<>();

        // Add some ModificationPayload entries
        for (int i = 1; i <= nListEntries; i++) {
            listEntryKeys.add(Integer.valueOf(i));

            final YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i).build();

            final DataTreeModification mod = source.takeSnapshot().newModification();
            mod.merge(path, ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i));
            mod.ready();

            InMemoryJournal.addEntry(shardID.toString(), i + 1, new SimpleReplicatedLogEntry(i, 1,
                    payloadForModification(source, mod, nextTransactionId())));
        }

        InMemoryJournal.addEntry(shardID.toString(), nListEntries + 2,
                new ApplyJournalEntries(nListEntries));

        testRecovery(listEntryKeys);

        verifyExport();
    }

    private void verifyExport() throws IOException, InterruptedException {
        verifyJournalExport();
        verifySnapshotExport();
    }

    private void verifyJournalExport() throws IOException, InterruptedException {
        String expectedJournalData = readExpectedFile(EXPECTED_JOURNAL_FILE);
        String actualJournalData = readActualFile(ACTUAL_JOURNAL_FILE);
        assertEquals("Exported journal is not expected ", expectedJournalData, actualJournalData);
    }

    private void verifySnapshotExport() throws IOException, InterruptedException {
        String expectedSnapshotData = readExpectedFile(EXPECTED_SNAPSHOT_FILE);
        String actualSnapshotData = readActualFile(ACTUAL_SNAPSHOT_FILE);
        assertEquals("Exported snapshot is not expected ", expectedSnapshotData, actualSnapshotData);
    }

    private String readExpectedFile(String filePath) throws IOException {
        File exportFile = new File(JsonExportTest.class.getClassLoader().getResource(filePath).getFile());
        return new String(Files.readAllBytes(Path.of(exportFile.getPath())));
    }

    private String readActualFile(String filePath) throws IOException {
        File exportFile = new File(filePath);
        await().atMost(10, TimeUnit.SECONDS).until(exportFile::exists);
        return new String(Files.readAllBytes(Path.of(filePath)));
    }

}
