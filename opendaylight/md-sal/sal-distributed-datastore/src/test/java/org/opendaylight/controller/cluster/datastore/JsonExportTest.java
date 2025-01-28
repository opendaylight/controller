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
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev250130.DataStoreProperties.ExportOnRecovery;

public class JsonExportTest extends AbstractShardTest {
    private static final String DUMMY_DATA = "Dummy data as snapshot sequence number is set to 0 in "
            + "InMemorySnapshotStore and journal recovery seq number will start from 1";
    private static final String EXPECTED_JOURNAL_FILE = "expectedJournalExport.json";
    private static final String EXPECTED_SNAPSHOT_FILE = "expectedSnapshotExport.json";
    private static String actualJournalFilePath;
    private static String actualSnapshotFilePath;
    private DatastoreContext datastoreContext;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final var exportTmpFolder = temporaryFolder.newFolder("persistence-export");
        actualJournalFilePath = exportTmpFolder.getAbsolutePath() + "/journals/"
            + "member-1-shard-inventory-config" + nextShardNum + "-journal.json";
        actualSnapshotFilePath = exportTmpFolder.getAbsolutePath() + "/snapshots/"
            + "member-1-shard-inventory-config" + nextShardNum + "-snapshot.json";
        datastoreContext = DatastoreContext.newBuilder().shardJournalRecoveryLogBatchSize(1)
            .shardSnapshotBatchCount(5000).shardHeartbeatIntervalInMillis(HEARTBEAT_MILLIS).persistent(true)
            .exportOnRecovery(ExportOnRecovery.Json)
            .recoveryExportBaseDir(exportTmpFolder.getAbsolutePath()).build();
    }

    @Override
    protected DatastoreContext newDatastoreContext() {
        return datastoreContext;
    }

    @Test
    public void testJsonExport() throws Exception {
        // Set up the InMemorySnapshotStore.
        final var source = setupInMemorySnapshotStore();

        final var writeMod = source.takeSnapshot().newModification();
        writeMod.write(TestModel.OUTER_LIST_PATH, TestModel.EMPTY_OUTER_LIST);
        writeMod.ready();
        InMemoryJournal.addEntry(shardID.toString(), 0, DUMMY_DATA);

        // Set up the InMemoryJournal.
        InMemoryJournal.addEntry(shardID.toString(), 1, new SimpleReplicatedLogEntry(0, 1,
                payloadForModification(source, writeMod, nextTransactionId())));

        final int nListEntries = 16;
        final var listEntryKeys = new HashSet<Integer>();

        // Add some ModificationPayload entries
        for (int i = 1; i <= nListEntries; i++) {
            listEntryKeys.add(i);

            final var mod = source.takeSnapshot().newModification();
            mod.merge(TestModel.outerEntryPath(i), TestModel.outerEntry(i));
            mod.ready();

            InMemoryJournal.addEntry(shardID.toString(), i + 1, new SimpleReplicatedLogEntry(i, 1,
                    payloadForModification(source, mod, nextTransactionId())));
        }

        InMemoryJournal.addEntry(shardID.toString(), nListEntries + 2,
                new ApplyJournalEntries(nListEntries));

        testRecovery(listEntryKeys, false);

        verifyJournalExport();
        verifySnapshotExport();
    }

    private static void verifyJournalExport() throws IOException {
        final String expectedJournalData = readExpectedFile(EXPECTED_JOURNAL_FILE);
        final String actualJournalData = readActualFile(actualJournalFilePath);
        assertEquals("Exported journal is not expected ", expectedJournalData, actualJournalData);
    }

    private static void verifySnapshotExport() throws IOException {
        final String expectedSnapshotData = readExpectedFile(EXPECTED_SNAPSHOT_FILE);
        final String actualSnapshotData = readActualFile(actualSnapshotFilePath);
        assertEquals("Exported snapshot is not expected ", expectedSnapshotData, actualSnapshotData);
    }

    private static String readExpectedFile(final String filePath) throws IOException {
        final File exportFile = new File(JsonExportTest.class.getClassLoader().getResource(filePath).getFile());
        return new String(Files.readAllBytes(Path.of(exportFile.getPath())));
    }

    private static String readActualFile(final String filePath) throws IOException {
        final File exportFile = new File(filePath);
        await().atMost(10, TimeUnit.SECONDS).until(exportFile::exists);
        return new String(Files.readAllBytes(Path.of(filePath)));
    }
}
