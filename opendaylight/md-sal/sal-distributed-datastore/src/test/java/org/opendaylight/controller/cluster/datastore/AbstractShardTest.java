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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.datastore.DataStoreVersions.CURRENT_VERSION;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.successfulCanCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.successfulCommit;
import static org.opendaylight.controller.cluster.datastore.ShardDataTreeMocking.successfulPreCommit;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

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
            .shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000)
            .shardHeartbeatIntervalInMillis(HEARTBEAT_MILLIS);

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
        return newShardBuilder().props();
    }

    protected Shard.Builder newShardBuilder() {
        return Shard.builder().id(shardID).datastoreContext(newDatastoreContext())
            .schemaContextProvider(() -> SCHEMA_CONTEXT);
    }

    protected void testRecovery(final Set<Integer> listEntryKeys, final boolean stopActorOnFinish) throws Exception {
        // Create the actor and wait for recovery complete.

        final int nListEntries = listEntryKeys.size();

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
                new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()), "testRecovery");

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
            final Optional<DataContainerChild<? extends PathArgument, ?>> idLeaf =
                    mapEntry.getChild(new YangInstanceIdentifier.NodeIdentifier(TestModel.ID_QNAME));
            assertTrue("Missing leaf " + TestModel.ID_QNAME.getLocalName(), idLeaf.isPresent());
            final Object value = idLeaf.get().getValue();
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

    protected ShardDataTreeCohort mockShardDataTreeCohort() {
        ShardDataTreeCohort cohort = mock(ShardDataTreeCohort.class);
        DataTreeCandidate candidate = mockCandidate("candidate");
        successfulCanCommit(cohort);
        successfulPreCommit(cohort, candidate);
        successfulCommit(cohort);
        doReturn(candidate).when(cohort).getCandidate();
        return cohort;
    }

    protected Map<TransactionIdentifier, CapturingShardDataTreeCohort> setupCohortDecorator(final Shard shard,
            final TransactionIdentifier... transactionIDs) {
        final Map<TransactionIdentifier, CapturingShardDataTreeCohort> cohortMap = new HashMap<>();
        for (TransactionIdentifier id: transactionIDs) {
            cohortMap.put(id, new CapturingShardDataTreeCohort());
        }

        shard.getCommitCoordinator().setCohortDecorator((transactionID, actual) -> {
            CapturingShardDataTreeCohort cohort = cohortMap.get(transactionID);
            cohort.setDelegate(actual);
            return cohort;
        });

        return cohortMap;
    }

    protected BatchedModifications prepareBatchedModifications(final TransactionIdentifier transactionID,
                                                               final MutableCompositeModification modification) {
        return prepareBatchedModifications(transactionID, modification, false);
    }

    protected static BatchedModifications prepareBatchedModifications(final TransactionIdentifier transactionID,
                                                             final MutableCompositeModification modification,
                                                             final boolean doCommitOnReady) {
        final BatchedModifications batchedModifications = new BatchedModifications(transactionID, CURRENT_VERSION);
        batchedModifications.addModification(modification);
        batchedModifications.setReady();
        batchedModifications.setDoCommitOnReady(doCommitOnReady);
        batchedModifications.setTotalMessagesSent(1);
        return batchedModifications;
    }

    protected static BatchedModifications prepareBatchedModifications(final TransactionIdentifier transactionID,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> data, final boolean doCommitOnReady) {
        final MutableCompositeModification modification = new MutableCompositeModification();
        modification.addModification(new WriteModification(path, data));
        return prepareBatchedModifications(transactionID, modification, doCommitOnReady);
    }

    protected static ForwardedReadyTransaction prepareForwardedReadyTransaction(final TestActorRef<Shard> shard,
            final TransactionIdentifier transactionID, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data, final boolean doCommitOnReady) {
        ReadWriteShardDataTreeTransaction rwTx = shard.underlyingActor().getDataStore()
                .newReadWriteTransaction(transactionID);
        rwTx.getSnapshot().write(path, data);
        return new ForwardedReadyTransaction(transactionID, CURRENT_VERSION, rwTx, doCommitOnReady, Optional.empty());
    }

    public static NormalizedNode<?,?> readStore(final TestActorRef<? extends Shard> shard,
            final YangInstanceIdentifier id) {
        return shard.underlyingActor().getDataStore().readNode(id).orElse(null);
    }

    public static NormalizedNode<?,?> readStore(final DataTree store, final YangInstanceIdentifier id) {
        return store.takeSnapshot().readNode(id).orElse(null);
    }

    public void writeToStore(final TestActorRef<Shard> shard, final YangInstanceIdentifier id,
            final NormalizedNode<?,?> node) throws InterruptedException, ExecutionException {
        Future<Object> future = Patterns.ask(shard, newBatchedModifications(nextTransactionId(),
                id, node, true, true, 1), new Timeout(5, TimeUnit.SECONDS));
        try {
            Await.ready(future, FiniteDuration.create(5, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            throw new ExecutionException(e);
        }
    }

    public static void writeToStore(final ShardDataTree store, final YangInstanceIdentifier id,
            final NormalizedNode<?,?> node) throws DataValidationFailedException {
        BatchedModifications batched = newBatchedModifications(nextTransactionId(), id, node, true, true, 1);
        DataTreeModification modification = store.getDataTree().takeSnapshot().newModification();
        batched.apply(modification);
        store.notifyListeners(commitTransaction(store.getDataTree(), modification));
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

    public void mergeToStore(final ShardDataTree store, final YangInstanceIdentifier id,
            final NormalizedNode<?,?> node) throws DataValidationFailedException {
        final BatchedModifications batched = new BatchedModifications(nextTransactionId(), CURRENT_VERSION);
        batched.addModification(new MergeModification(id, node));
        batched.setReady();
        batched.setDoCommitOnReady(true);
        batched.setTotalMessagesSent(1);

        DataTreeModification modification = store.getDataTree().takeSnapshot().newModification();
        batched.apply(modification);
        store.notifyListeners(commitTransaction(store.getDataTree(), modification));
    }

    DataTree setupInMemorySnapshotStore() throws DataValidationFailedException {
        final DataTree testStore = new InMemoryDataTreeFactory().create(
            DataTreeConfiguration.DEFAULT_OPERATIONAL, SCHEMA_CONTEXT);

        writeToStore(testStore, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        final NormalizedNode<?, ?> root = readStore(testStore, YangInstanceIdentifier.empty());

        InMemorySnapshotStore.addSnapshot(shardID.toString(), Snapshot.create(
                new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
                Collections.<ReplicatedLogEntry>emptyList(), 0, 1, -1, -1, 1, null, null));
        return testStore;
    }

    static CommitTransactionPayload payloadForModification(final DataTree source, final DataTreeModification mod,
            final TransactionIdentifier transactionId) throws DataValidationFailedException, IOException {
        source.validate(mod);
        final DataTreeCandidate candidate = source.prepare(mod);
        source.commit(candidate);
        return CommitTransactionPayload.create(transactionId, candidate);
    }

    static BatchedModifications newBatchedModifications(final TransactionIdentifier transactionID,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> data, final boolean ready,
            final boolean doCommitOnReady, final int messagesSent) {
        final BatchedModifications batched = new BatchedModifications(transactionID, CURRENT_VERSION);
        batched.addModification(new WriteModification(path, data));
        if (ready) {
            batched.setReady();
        }
        batched.setDoCommitOnReady(doCommitOnReady);
        batched.setTotalMessagesSent(messagesSent);
        return batched;
    }

    static BatchedModifications newReadyBatchedModifications(final TransactionIdentifier transactionID,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> data,
            final SortedSet<String> participatingShardNames) {
        final BatchedModifications batched = new BatchedModifications(transactionID, CURRENT_VERSION);
        batched.addModification(new WriteModification(path, data));
        batched.setReady(Optional.of(participatingShardNames));
        batched.setTotalMessagesSent(1);
        return batched;
    }

    @SuppressWarnings("unchecked")
    static void verifyOuterListEntry(final TestActorRef<Shard> shard, final Object expIDValue) {
        final NormalizedNode<?, ?> outerList = readStore(shard, TestModel.OUTER_LIST_PATH);
        assertNotNull(TestModel.OUTER_LIST_QNAME.getLocalName() + " not found", outerList);
        assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " value is not Iterable",
                outerList.getValue() instanceof Iterable);
        final Object entry = ((Iterable<Object>)outerList.getValue()).iterator().next();
        assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " entry is not MapEntryNode",
                entry instanceof MapEntryNode);
        final MapEntryNode mapEntry = (MapEntryNode)entry;
        final Optional<DataContainerChild<? extends PathArgument, ?>> idLeaf =
                mapEntry.getChild(new YangInstanceIdentifier.NodeIdentifier(TestModel.ID_QNAME));
        assertTrue("Missing leaf " + TestModel.ID_QNAME.getLocalName(), idLeaf.isPresent());
        assertEquals(TestModel.ID_QNAME.getLocalName() + " value", expIDValue, idLeaf.get().getValue());
    }

    public static DataTreeCandidateTip mockCandidate(final String name) {
        final DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class, name);
        final DataTreeCandidateNode mockCandidateNode = mock(DataTreeCandidateNode.class, name + "-node");
        doReturn(ModificationType.WRITE).when(mockCandidateNode).getModificationType();
        doReturn(Optional.of(ImmutableNodes.containerNode(CarsModel.CARS_QNAME)))
                .when(mockCandidateNode).getDataAfter();
        doReturn(CarsModel.BASE_PATH).when(mockCandidate).getRootPath();
        doReturn(mockCandidateNode).when(mockCandidate).getRootNode();
        return mockCandidate;
    }

    static DataTreeCandidateTip mockUnmodifiedCandidate(final String name) {
        final DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class, name);
        final DataTreeCandidateNode mockCandidateNode = mock(DataTreeCandidateNode.class, name + "-node");
        doReturn(ModificationType.UNMODIFIED).when(mockCandidateNode).getModificationType();
        doReturn(YangInstanceIdentifier.empty()).when(mockCandidate).getRootPath();
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

    @SuppressWarnings("serial")
    public static final class DelegatingShardCreator implements Creator<Shard> {
        private final Creator<Shard> delegate;

        DelegatingShardCreator(final Creator<Shard> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Shard create() throws Exception {
            return delegate.create();
        }
    }

    public static class CapturingShardDataTreeCohort extends ShardDataTreeCohort {
        private volatile ShardDataTreeCohort delegate;
        private FutureCallback<Void> canCommit;
        private FutureCallback<DataTreeCandidate> preCommit;
        private FutureCallback<UnsignedLong> commit;

        public void setDelegate(final ShardDataTreeCohort delegate) {
            this.delegate = delegate;
        }

        public FutureCallback<Void> getCanCommit() {
            assertNotNull("canCommit was not invoked", canCommit);
            return canCommit;
        }

        public FutureCallback<DataTreeCandidate> getPreCommit() {
            assertNotNull("preCommit was not invoked", preCommit);
            return preCommit;
        }

        public FutureCallback<UnsignedLong> getCommit() {
            assertNotNull("commit was not invoked", commit);
            return commit;
        }

        @Override
        public TransactionIdentifier getIdentifier() {
            return delegate.getIdentifier();
        }

        @Override
        DataTreeCandidateTip getCandidate() {
            return delegate.getCandidate();
        }

        @Override
        DataTreeModification getDataTreeModification() {
            return delegate.getDataTreeModification();
        }

        @Override
        public void canCommit(final FutureCallback<Void> callback) {
            canCommit = mockFutureCallback(callback);
            delegate.canCommit(canCommit);
        }

        @Override
        public void preCommit(final FutureCallback<DataTreeCandidate> callback) {
            preCommit = mockFutureCallback(callback);
            delegate.preCommit(preCommit);
        }

        @Override
        public void commit(final FutureCallback<UnsignedLong> callback) {
            commit = mockFutureCallback(callback);
            delegate.commit(commit);
        }

        @SuppressWarnings("unchecked")
        private static <T> FutureCallback<T> mockFutureCallback(final FutureCallback<T> actual) {
            FutureCallback<T> mock = mock(FutureCallback.class);
            doAnswer(invocation -> {
                actual.onFailure(invocation.getArgument(0));
                return null;
            }).when(mock).onFailure(any(Throwable.class));

            doAnswer(invocation -> {
                actual.onSuccess(invocation.getArgument(0));
                return null;
            }).when(mock).onSuccess((T) nullable(Object.class));

            return mock;
        }

        @Override
        public void abort(final FutureCallback<Void> callback) {
            delegate.abort(callback);
        }

        @Override
        public boolean isFailed() {
            return delegate.isFailed();
        }

        @Override
        public State getState() {
            return delegate.getState();
        }

        @Override
        Optional<SortedSet<String>> getParticipatingShardNames() {
            return delegate.getParticipatingShardNames();
        }
    }
}
