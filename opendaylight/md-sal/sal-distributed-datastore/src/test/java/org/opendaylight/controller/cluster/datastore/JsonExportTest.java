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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.EntryJournalV1;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev250130.DataStoreProperties.ExportOnRecovery;

public class JsonExportTest extends AbstractShardTest {
    private String actualJournalFilePath;
    private String actualSnapshotFilePath;
    private DatastoreContext datastoreContext;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        final var exportTmpFolder = temporaryFolder.newFolder("persistence-export");
        actualJournalFilePath = exportTmpFolder.getAbsolutePath() + "/journals/"
            + "member-1-shard-inventory-config" + nextShardNum + "-journal.json";
        actualSnapshotFilePath = exportTmpFolder.getAbsolutePath() + "/snapshots/"
            + "member-1-shard-inventory-config" + nextShardNum + "-snapshot.json";
        datastoreContext = DatastoreContext.newBuilder()
            .shardJournalRecoveryLogBatchSize(1)
            .shardSnapshotBatchCount(5000)
            .shardHeartbeatIntervalInMillis(HEARTBEAT_MILLIS)
            .persistent(true)
            .exportOnRecovery(ExportOnRecovery.Json)
            .logicalStoreType(LogicalDatastoreType.OPERATIONAL)
            .recoveryExportBaseDir(exportTmpFolder.getAbsolutePath()).build();
    }

    @Override
    protected DatastoreContext newDatastoreContext() {
        return datastoreContext;
    }

    @Test
    public void testJsonExport() throws Exception {
        // Set up the InMemorySnapshotStore.
        final var source = setupWithSnapshot();

        final var writeMod = source.takeSnapshot().newModification();
        writeMod.write(TestModel.OUTER_LIST_PATH, TestModel.EMPTY_OUTER_LIST);
        writeMod.ready();

        final var listEntryKeys = new HashSet<Integer>();

        // Setup journal
        try (var journal = new EntryJournalV1("test", stateDir().resolve("shards").resolve(shardID.toString()),
                CompressionType.NONE, false)) {
            journal.appendEntry(new DefaultLogEntry(0, 1,
                payloadForModification(source, writeMod, nextTransactionId())));


            final int nListEntries = 16;

            // Add some ModificationPayload entries
            for (int i = 1; i <= nListEntries; i++) {
                listEntryKeys.add(i);

                final var mod = source.takeSnapshot().newModification();
                mod.merge(TestModel.outerEntryPath(i), TestModel.outerEntry(i));
                mod.ready();

                journal.appendEntry(new DefaultLogEntry(i, 1,
                    payloadForModification(source, mod, nextTransactionId())));
            }

            journal.setApplyTo(journal.nextToWrite() - 1);
        }

        testRecovery(listEntryKeys, false);

        verifyJournalExport();
        verifySnapshotExport();
    }

    private void verifyJournalExport() throws IOException {
        assertEquals("Exported journal is not expected ", """
            {"Entries":[{"Entry":[{"Node":[{"Path\
            ":"/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-\
            list"},{"ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:\
            controller:md:sal:dom:store:test?revision=2014-03-13)id, body=1}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=2}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=3}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=4}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=5}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=6}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=7}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=8}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=9}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=10}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{\
            "ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:\
            controller:md:sal:dom:store:test?revision=2014-03-13)id, body=11}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=12}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{\
            "ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:\
            controller:md:sal:dom:store:test?revision=2014-03-13)id, body=13}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn\
            :opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=14}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{"\
            ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:controller\
            :md:sal:dom:store:test?revision=2014-03-13)id, body=15}]"}]}]},{"Entry":[{"Node":[{"Path":"/(urn:\
            opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/outer-list"},{\
            "ModificationType":"WRITE"},{"Data":"[ImmutableLeafNode{name=(urn:opendaylight:params:xml:ns:yang:\
            controller:md:sal:dom:store:test?revision=2014-03-13)id, body=16}]"}]}]}]}""",
            readActualFile(actualJournalFilePath));
    }

    private void verifySnapshotExport() throws IOException {
        assertEquals("Exported snapshot is not expected ", """
            {"odl-datastore-test:test":{}}""", readActualFile(actualSnapshotFilePath));
    }

    private static String readActualFile(final String filePath) throws IOException {
        final var exportFile = Path.of(filePath);
        await().atMost(10, TimeUnit.SECONDS).until(() -> Files.exists(exportFile));
        return Files.readString(exportFile);
    }
}
