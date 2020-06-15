/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.japi.Creator;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardTransactionTest;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class JsonExportTest {
    private static final String DUMMY_DATA = "Dummy data as snapshot sequence number is set to 0 in "
            + "InMemorySnapshotStore and journal recovery seq number will start from 1";

    private static final String EXPECTED_JOURNAL_FILE = "expectedJournalExport.json";
    private static final String EXPECTED_SNAPSHOT_FILE = "expectedSnapshotExport.json";
    private static final int HEARTBEAT_MILLIS = 100;
    private static final EffectiveModelContext SCHEMA_CONTEXT = TestModel.createTestContext();
    private final ShardIdentifier shardID = ShardIdentifier.create("inventory", MemberName.forName("member-1"),
            "config");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName(ShardTransactionTest.class.getSimpleName());
    protected static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    protected static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    private static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 0);
    private static final AtomicLong TX_COUNTER = new AtomicLong();

    private static ActorSystem system;
    private DatastoreContext datastoreContext;
    private String actualJournalFilePath;
    private String actualSnapshotFilePath;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() throws IOException {
        System.setProperty("shard.persistent", "false");
        system = ActorSystem.create("test");
        final File exportTmpFolder = temporaryFolder.newFolder("persistence-export");
        actualJournalFilePath = exportTmpFolder.getAbsolutePath() + File.separator + "journals"
            + File.separator +  "member-1-shard-inventory-config-journal.json";
        actualSnapshotFilePath = exportTmpFolder.getAbsolutePath() + File.separator + "snapshots"
            + File.separator + "member-1-shard-inventory-config-snapshot.json";
        datastoreContext = DatastoreContext.newBuilder()
            .shardJournalRecoveryLogBatchSize(1).shardSnapshotBatchCount(5000)
            .shardHeartbeatIntervalInMillis(HEARTBEAT_MILLIS).exportOnRecovery(true)
            .recoveryExportBaseDir(exportTmpFolder.getAbsolutePath()).build();
    }

    @After
    public void after() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    protected DatastoreContext newDatastoreContext() {
        return datastoreContext;
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
            listEntryKeys.add(i);

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

        verifyJournalExport();
        verifySnapshotExport();
    }

    private void verifyJournalExport() throws IOException {
        final String expectedJournalData = readExpectedFile(EXPECTED_JOURNAL_FILE);
        final String actualJournalData = readActualFile(actualJournalFilePath);
        assertEquals("Exported journal is not expected ", expectedJournalData, actualJournalData);
    }

    private void verifySnapshotExport() throws IOException {
        final String expectedSnapshotData = readExpectedFile(EXPECTED_SNAPSHOT_FILE);
        final String actualSnapshotData = readActualFile(actualSnapshotFilePath);
        assertEquals("Exported snapshot is not expected ", expectedSnapshotData, actualSnapshotData);
    }

    private String readExpectedFile(final String filePath) throws IOException {
        final File exportFile = new File(JsonExportTest.class.getClassLoader().getResource(filePath).getFile());
        return new String(Files.readAllBytes(Path.of(exportFile.getPath())));
    }

    private String readActualFile(final String filePath) throws IOException {
        final File exportFile = new File(filePath);
        await().atMost(10, TimeUnit.SECONDS).until(exportFile::exists);
        return new String(Files.readAllBytes(Path.of(filePath)));
    }

    private DataTree setupInMemorySnapshotStore() throws DataValidationFailedException {
        final DataTree testStore = new InMemoryDataTreeFactory().create(
                DataTreeConfiguration.DEFAULT_OPERATIONAL, SCHEMA_CONTEXT);

        writeToStore(testStore, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        final NormalizedNode<?, ?> root = readStore(testStore, YangInstanceIdentifier.empty());

        InMemorySnapshotStore.addSnapshot(shardID.toString(), Snapshot.create(
                new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
                Collections.<ReplicatedLogEntry>emptyList(), 0, 1, -1, -1, 1, null, null));
        return testStore;
    }

    public static void writeToStore(final DataTree store, final YangInstanceIdentifier id,
                                    final NormalizedNode<?,?> node) throws DataValidationFailedException {
        final DataTreeModification transaction = store.takeSnapshot().newModification();

        transaction.write(id, node);
        transaction.ready();
        store.validate(transaction);
        final DataTreeCandidate candidate = store.prepare(transaction);
        store.commit(candidate);
    }

    public static NormalizedNode<?,?> readStore(final DataTree store, final YangInstanceIdentifier id) {
        return store.takeSnapshot().readNode(id).orElse(null);
    }

    private static NormalizedNode<?,?> readStore(final TestActorRef<? extends Shard> shard,
                                                 final YangInstanceIdentifier id) {
        return shard.underlyingActor().getDataStore().readNode(id).orElse(null);
    }

    private static CommitTransactionPayload payloadForModification(final DataTree source,
                   final DataTreeModification mod, final TransactionIdentifier transactionId)
            throws DataValidationFailedException, IOException {
        source.validate(mod);
        final DataTreeCandidate candidate = source.prepare(mod);
        source.commit(candidate);
        return CommitTransactionPayload.create(transactionId, candidate);
    }

    private void testRecovery(final Set<Integer> listEntryKeys) throws Exception {
        // Create the actor and wait for recovery complete.
        final CountDownLatch recoveryComplete = new CountDownLatch(1);

        final Creator<Shard> creator = () -> new Shard(newShardBuilder()) {
            @Override
            protected void onRecoveryComplete() {
                try {
                    super.onRecoveryComplete();
                } finally {
                    recoveryComplete.countDown();
                }
            }
        };

        final TestActorRef<Shard> shard = TestActorRef.create(getSystem(), Props.create(Shard.class,
                new AbstractShardTest.DelegatingShardCreator(creator)).withDispatcher(
                        Dispatchers.DefaultDispatcherId()), "testRecovery");

        assertTrue("Recovery complete", recoveryComplete.await(5, TimeUnit.SECONDS));

        // Verify data in the data store.

        final NormalizedNode<?, ?> outerList = readStore(shard, TestModel.OUTER_LIST_PATH);
        assertNotNull(TestModel.OUTER_LIST_QNAME.getLocalName() + " not found", outerList);
        assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " value is not Iterable",
                outerList.getValue() instanceof Iterable);
        for (final Object entry: (Iterable<?>) outerList.getValue()) {
            assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " entry is not MapEntryNode",
                    entry instanceof MapEntryNode);
            final MapEntryNode mapEntry = (MapEntryNode)entry;
            final Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> idLeaf =
                    mapEntry.getChild(new YangInstanceIdentifier.NodeIdentifier(TestModel.ID_QNAME));
            assertTrue("Missing leaf " + TestModel.ID_QNAME.getLocalName(), idLeaf.isPresent());
            final Object value = idLeaf.get().getValue();
            assertTrue("Unexpected value for leaf " + TestModel.ID_QNAME.getLocalName() + ": " + value,
                    listEntryKeys.remove(value));
        }

        if (!listEntryKeys.isEmpty()) {
            fail("Missing " + TestModel.OUTER_LIST_QNAME.getLocalName() + " entries with keys: " + listEntryKeys);
        }

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    private Shard.Builder newShardBuilder() {
        return Shard.builder().id(shardID).datastoreContext(newDatastoreContext())
                .schemaContextProvider(() -> SCHEMA_CONTEXT);
    }


    private static ActorSystem getSystem() {
        return system;
    }

    private static TransactionIdentifier nextTransactionId() {
        return new TransactionIdentifier(HISTORY_ID, TX_COUNTER.getAndIncrement());
    }
}
