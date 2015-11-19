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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.japi.Creator;
import akka.testkit.TestActorRef;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Abstract base for shard unit tests.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractShardTest extends AbstractActorTest{
    protected static final SchemaContext SCHEMA_CONTEXT = TestModel.createTestContext();

    private static final AtomicInteger NEXT_SHARD_NUM = new AtomicInteger();

    protected final ShardIdentifier shardID = ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("config" + NEXT_SHARD_NUM.getAndIncrement()).build();

    protected final Builder dataStoreContextBuilder = DatastoreContext.newBuilder().
            shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).
            shardHeartbeatIntervalInMillis(100);

    protected final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @Before
    public void setUp() {
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
        return Shard.builder().id(shardID).datastoreContext(newDatastoreContext()).schemaContext(SCHEMA_CONTEXT);
    }

    protected void testRecovery(final Set<Integer> listEntryKeys) throws Exception {
        // Create the actor and wait for recovery complete.

        final int nListEntries = listEntryKeys.size();

        final CountDownLatch recoveryComplete = new CountDownLatch(1);

        @SuppressWarnings("serial")
        final
        Creator<Shard> creator = new Creator<Shard>() {
            @Override
            public Shard create() throws Exception {
                return new Shard(newShardBuilder()) {
                    @Override
                    protected void onRecoveryComplete() {
                        try {
                            super.onRecoveryComplete();
                        } finally {
                            recoveryComplete.countDown();
                        }
                    }
                };
            }
        };

        final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                Props.create(new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()), "testRecovery");

        assertEquals("Recovery complete", true, recoveryComplete.await(5, TimeUnit.SECONDS));

        // Verify data in the data store.

        final NormalizedNode<?, ?> outerList = readStore(shard, TestModel.OUTER_LIST_PATH);
        assertNotNull(TestModel.OUTER_LIST_QNAME.getLocalName() + " not found", outerList);
        assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " value is not Iterable",
                outerList.getValue() instanceof Iterable);
        for(final Object entry: (Iterable<?>) outerList.getValue()) {
            assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " entry is not MapEntryNode",
                    entry instanceof MapEntryNode);
            final MapEntryNode mapEntry = (MapEntryNode)entry;
            final Optional<DataContainerChild<? extends PathArgument, ?>> idLeaf =
                    mapEntry.getChild(new YangInstanceIdentifier.NodeIdentifier(TestModel.ID_QNAME));
            assertTrue("Missing leaf " + TestModel.ID_QNAME.getLocalName(), idLeaf.isPresent());
            final Object value = idLeaf.get().getValue();
            assertTrue("Unexpected value for leaf "+ TestModel.ID_QNAME.getLocalName() + ": " + value,
                    listEntryKeys.remove(value));
        }

        if(!listEntryKeys.isEmpty()) {
            fail("Missing " + TestModel.OUTER_LIST_QNAME.getLocalName() + " entries with keys: " +
                    listEntryKeys);
        }

        assertEquals("Last log index", nListEntries,
                shard.underlyingActor().getShardMBean().getLastLogIndex());
        assertEquals("Commit index", nListEntries,
                shard.underlyingActor().getShardMBean().getCommitIndex());
        assertEquals("Last applied", nListEntries,
                shard.underlyingActor().getShardMBean().getLastApplied());

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    protected void verifyLastApplied(final TestActorRef<Shard> shard, final long expectedValue) {
        long lastApplied = -1;
        for(int i = 0; i < 20 * 5; i++) {
            lastApplied = shard.underlyingActor().getShardMBean().getLastApplied();
            if(lastApplied == expectedValue) {
                return;
            }
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail(String.format("Expected last applied: %d, Actual: %d", expectedValue, lastApplied));
    }

    protected ShardDataTreeCohort setupMockWriteTransaction(final String cohortName,
            final ShardDataTree dataStore, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data,
            final MutableCompositeModification modification) {
        return setupMockWriteTransaction(cohortName, dataStore, path, data, modification, null);
    }

    protected ShardDataTreeCohort setupMockWriteTransaction(final String cohortName,
            final ShardDataTree dataStore, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data,
            final MutableCompositeModification modification,
            final Function<ShardDataTreeCohort, ListenableFuture<Void>> preCommit) {

        final ReadWriteShardDataTreeTransaction tx = dataStore.newReadWriteTransaction("setup-mock-" + cohortName, null);
        tx.getSnapshot().write(path, data);
        final ShardDataTreeCohort cohort = createDelegatingMockCohort(cohortName, dataStore.finishTransaction(tx), preCommit);

        modification.addModification(new WriteModification(path, data));

        return cohort;
    }

    protected ShardDataTreeCohort createDelegatingMockCohort(final String cohortName,
            final ShardDataTreeCohort actual) {
        return createDelegatingMockCohort(cohortName, actual, null);
    }

    protected ShardDataTreeCohort createDelegatingMockCohort(final String cohortName,
            final ShardDataTreeCohort actual,
            final Function<ShardDataTreeCohort, ListenableFuture<Void>> preCommit) {
        final ShardDataTreeCohort cohort = mock(ShardDataTreeCohort.class, cohortName);

        doAnswer(new Answer<ListenableFuture<Boolean>>() {
            @Override
            public ListenableFuture<Boolean> answer(final InvocationOnMock invocation) {
                return actual.canCommit();
            }
        }).when(cohort).canCommit();

        doAnswer(new Answer<ListenableFuture<Void>>() {
            @Override
            public ListenableFuture<Void> answer(final InvocationOnMock invocation) throws Throwable {
                if(preCommit != null) {
                    return preCommit.apply(actual);
                } else {
                    return actual.preCommit();
                }
            }
        }).when(cohort).preCommit();

        doAnswer(new Answer<ListenableFuture<Void>>() {
            @Override
            public ListenableFuture<Void> answer(final InvocationOnMock invocation) throws Throwable {
                return actual.commit();
            }
        }).when(cohort).commit();

        doAnswer(new Answer<ListenableFuture<Void>>() {
            @Override
            public ListenableFuture<Void> answer(final InvocationOnMock invocation) throws Throwable {
                return actual.abort();
            }
        }).when(cohort).abort();

        doAnswer(new Answer<DataTreeCandidateTip>() {
            @Override
            public DataTreeCandidateTip answer(final InvocationOnMock invocation) {
                return actual.getCandidate();
            }
        }).when(cohort).getCandidate();

        return cohort;
    }

    public static NormalizedNode<?,?> readStore(final TestActorRef<? extends Shard> shard, final YangInstanceIdentifier id)
            throws ExecutionException, InterruptedException {
        return shard.underlyingActor().getDataStore().readNode(id).orNull();
    }

    public static NormalizedNode<?,?> readStore(final DataTree store, final YangInstanceIdentifier id) {
        return store.takeSnapshot().readNode(id).orNull();
    }

    public static void writeToStore(final TestActorRef<Shard> shard, final YangInstanceIdentifier id,
            final NormalizedNode<?,?> node) throws InterruptedException, ExecutionException {
        writeToStore(shard.underlyingActor().getDataStore(), id, node);
    }

    public static void writeToStore(final ShardDataTree store, final YangInstanceIdentifier id,
            final NormalizedNode<?,?> node) throws InterruptedException, ExecutionException {
        final ReadWriteShardDataTreeTransaction transaction = store.newReadWriteTransaction("writeToStore", null);

        transaction.getSnapshot().write(id, node);
        final ShardDataTreeCohort cohort = transaction.ready();
        cohort.canCommit().get();
        cohort.preCommit().get();
        cohort.commit();
    }

    public static void mergeToStore(final ShardDataTree store, final YangInstanceIdentifier id,
            final NormalizedNode<?,?> node) throws InterruptedException, ExecutionException {
        final ReadWriteShardDataTreeTransaction transaction = store.newReadWriteTransaction("writeToStore", null);

        transaction.getSnapshot().merge(id, node);
        final ShardDataTreeCohort cohort = transaction.ready();
        cohort.canCommit().get();
        cohort.preCommit().get();
        cohort.commit();
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
}
