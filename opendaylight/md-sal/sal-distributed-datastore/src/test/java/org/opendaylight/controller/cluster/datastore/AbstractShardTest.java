/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.successfulCanCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.successfulCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.successfulPreCommit;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.japi.Creator;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

/**
 * Abstract base for shard unit tests.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractShardTest extends AbstractActorTest {
    protected static final EffectiveModelContext SCHEMA_CONTEXT = TestModel.createTestContext();

    protected static final AtomicInteger SHARD_NUM = new AtomicInteger();
    protected static final int HEARTBEAT_MILLIS = 100;

    protected final Builder dataStoreContextBuilder = DatastoreContext.newBuilder()
        .shardJournalRecoveryLogBatchSize(3)
        .shardSnapshotBatchCount(5000)
        .shardHeartbeatIntervalInMillis(HEARTBEAT_MILLIS)
        .logicalStoreType(LogicalDatastoreType.CONFIGURATION);

    protected final TestActorFactory actorFactory = new TestActorFactory(getSystem());
    protected final int nextShardNum = SHARD_NUM.getAndIncrement();
    protected final ShardIdentifier shardID = ShardIdentifier.create("inventory", MemberName.forName("member-1"),
        "config" + nextShardNum);

    @Before
    public void setUp() throws Exception {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
    }

    @After
    public void tearDown() {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
        actorFactory.close();
    }

    protected DatastoreContext newDatastoreContext() {
        return dataStoreContextBuilder.build();
    }

    protected Props newShardProps() {
        return newShardBuilder().props(stateDir());
    }

    protected Shard.Builder newShardBuilder() {
        return Shard.builder().id(shardID).datastoreContext(newDatastoreContext())
            .schemaContextProvider(() -> SCHEMA_CONTEXT);
    }

    protected void testRecovery(final Set<Integer> listEntryKeys, final boolean stopActorOnFinish) throws Exception {
        // Create the actor and wait for recovery complete.

        final int nListEntries = listEntryKeys.size();

        final CountDownLatch recoveryComplete = new CountDownLatch(1);

        final Creator<Shard> creator = () -> new Shard(stateDir(), newShardBuilder()) {
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
                new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()), "testRecovery");

        assertTrue("Recovery complete", recoveryComplete.await(5, TimeUnit.SECONDS));

        // Verify data in the data store.

        final NormalizedNode outerList = readStore(shard, TestModel.OUTER_LIST_PATH);
        assertNotNull(TestModel.OUTER_LIST_QNAME.getLocalName() + " not found", outerList);
        assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " value is not Iterable",
                outerList.body() instanceof Iterable);
        for (final Object entry: (Iterable<?>) outerList.body()) {
            assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " entry is not MapEntryNode",
                    entry instanceof MapEntryNode);
            final MapEntryNode mapEntry = (MapEntryNode)entry;
            final Optional<DataContainerChild> idLeaf =
                    mapEntry.findChildByArg(new YangInstanceIdentifier.NodeIdentifier(TestModel.ID_QNAME));
            assertTrue("Missing leaf " + TestModel.ID_QNAME.getLocalName(), idLeaf.isPresent());
            final Object value = idLeaf.orElseThrow().body();
            assertTrue("Unexpected value for leaf " + TestModel.ID_QNAME.getLocalName() + ": " + value,
                    listEntryKeys.remove(value));
        }

        if (!listEntryKeys.isEmpty()) {
            fail("Missing " + TestModel.OUTER_LIST_QNAME.getLocalName() + " entries with keys: " + listEntryKeys);
        }

        assertEquals("Last log index", nListEntries,
                shard.underlyingActor().getShardMBean().getLastLogIndex());
        assertEquals("Commit index", nListEntries,
                shard.underlyingActor().getShardMBean().getCommitIndex());
        assertEquals("Last applied", nListEntries,
                shard.underlyingActor().getShardMBean().getLastApplied());

        if (stopActorOnFinish) {
            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    protected void verifyLastApplied(final TestActorRef<Shard> shard, final long expectedValue) {
        long lastApplied = -1;
        for (int i = 0; i < 20 * 5; i++) {
            lastApplied = shard.underlyingActor().getShardMBean().getLastApplied();
            if (lastApplied == expectedValue) {
                return;
            }
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail(String.format("Expected last applied: %d, Actual: %d", expectedValue, lastApplied));
    }

    protected DataTree createDelegatingMockDataTree() throws Exception {
        final DataTree actual = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION);
        final DataTree mock = mock(DataTree.class);

        doAnswer(invocation -> {
            actual.validate(invocation.getArgument(0));
            return null;
        }).when(mock).validate(any(DataTreeModification.class));

        doAnswer(invocation -> actual.prepare(invocation.getArgument(0))).when(
                mock).prepare(any(DataTreeModification.class));

        doAnswer(invocation -> {
            actual.commit(invocation.getArgument(0));
            return null;
        }).when(mock).commit(any(DataTreeCandidate.class));

        doAnswer(invocation -> {
            actual.setEffectiveModelContext(invocation.getArgument(0));
            return null;
        }).when(mock).setEffectiveModelContext(any(EffectiveModelContext.class));

        doAnswer(invocation -> actual.takeSnapshot()).when(mock).takeSnapshot();

        doAnswer(invocation -> actual.getRootPath()).when(mock).getRootPath();

        return mock;
    }

    protected CommitCohort mockShardDataTreeCohort() {
        CommitCohort cohort = mock(CommitCohort.class);
        DataTreeCandidate candidate = mockCandidate("candidate");
        successfulCanCommit(cohort);
        successfulPreCommit(cohort, candidate);
        successfulCommit(cohort);
        doReturn(candidate).when(cohort).getCandidate();
        return cohort;
    }

    public static NormalizedNode readStore(final TestActorRef<? extends Shard> shard,
            final YangInstanceIdentifier id) {
        return shard.underlyingActor().getDataStore().readNode(id).orElse(null);
    }

    public static NormalizedNode readStore(final DataTree store, final YangInstanceIdentifier id) {
        return store.takeSnapshot().readNode(id).orElse(null);
    }

    public static void writeToStore(final TestActorRef<Shard> shard, final YangInstanceIdentifier id,
            final NormalizedNode node) {
        final var store = shard.underlyingActor().getDataStore();
        final var dataTree = store.getDataTree();
        store.notifyListeners(assertDoesNotThrow(() -> writeToStore(dataTree, id, node)));
    }

    public static DataTreeCandidate writeToStore(final DataTree store, final YangInstanceIdentifier id,
            final NormalizedNode node) throws DataValidationFailedException {
        final var mod = store.takeSnapshot().newModification();
        mod.write(id, node);
        return commitTransaction(store, mod);
    }

    DataTree setupInMemorySnapshotStore() throws DataValidationFailedException {
        final DataTree testStore = new InMemoryDataTreeFactory().create(
            DataTreeConfiguration.DEFAULT_OPERATIONAL, SCHEMA_CONTEXT);

        writeToStore(testStore, TestModel.TEST_PATH, TestModel.EMPTY_TEST);

        final NormalizedNode root = readStore(testStore, YangInstanceIdentifier.of());

        InMemorySnapshotStore.addSnapshot(shardID.toString(), Snapshot.create(
                new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
                List.of(), 0, 1, -1, -1, new TermInfo(1, null), null));
        return testStore;
    }

    static CommitTransactionPayload payloadForModification(final DataTree source, final DataTreeModification mod,
            final TransactionIdentifier transactionId) throws DataValidationFailedException, IOException {
        source.validate(mod);
        final DataTreeCandidate candidate = source.prepare(mod);
        source.commit(candidate);
        return CommitTransactionPayload.create(transactionId, candidate);
    }

    static void verifyOuterListEntry(final TestActorRef<Shard> shard, final int expIDValue) {
        final var entry = assertInstanceOf(MapEntryNode.class, readStore(shard, TestModel.outerEntryPath(expIDValue)));
        final var leaf = assertInstanceOf(LeafNode.class, entry.childByArg(new NodeIdentifier(TestModel.ID_QNAME)));
        assertEquals(expIDValue, leaf.body());
    }

    public static DataTreeCandidateTip mockCandidate(final String name) {
        final DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class, name);
        final DataTreeCandidateNode mockCandidateNode = mock(DataTreeCandidateNode.class, name + "-node");
        doReturn(ModificationType.WRITE).when(mockCandidateNode).modificationType();
        doReturn(ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(CarsModel.CARS_QNAME))
            .build()).when(mockCandidateNode).dataAfter();
        doReturn(CarsModel.BASE_PATH).when(mockCandidate).getRootPath();
        doReturn(mockCandidateNode).when(mockCandidate).getRootNode();
        return mockCandidate;
    }

    static DataTreeCandidateTip mockUnmodifiedCandidate(final String name) {
        final DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class, name);
        final DataTreeCandidateNode mockCandidateNode = mock(DataTreeCandidateNode.class, name + "-node");
        doReturn(ModificationType.UNMODIFIED).when(mockCandidateNode).modificationType();
        doReturn(YangInstanceIdentifier.of()).when(mockCandidate).getRootPath();
        doReturn(mockCandidateNode).when(mockCandidate).getRootNode();
        return mockCandidate;
    }

    static DataTreeCandidate commitTransaction(final DataTree store, final DataTreeModification modification)
            throws DataValidationFailedException {
        modification.ready();
        store.validate(modification);
        final DataTreeCandidate candidate = store.prepare(modification);
        store.commit(candidate);
        return candidate;
    }

    public static final class DelegatingShardCreator implements Creator<Shard> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private final Creator<Shard> delegate;

        DelegatingShardCreator(final Creator<Shard> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Shard create() throws Exception {
            return delegate.create();
        }
    }
}
