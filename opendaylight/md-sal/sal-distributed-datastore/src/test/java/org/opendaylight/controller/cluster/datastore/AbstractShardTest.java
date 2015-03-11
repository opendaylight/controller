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
import akka.japi.Creator;
import akka.testkit.TestActorRef;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.datastore.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
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

    @Before
    public void setUp() {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
    }

    @After
    public void tearDown() {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
    }

    protected DatastoreContext newDatastoreContext() {
        return dataStoreContextBuilder.build();
    }

    protected Props newShardProps() {
        return Shard.props(shardID, Collections.<String,String>emptyMap(),
                newDatastoreContext(), SCHEMA_CONTEXT);
    }

    protected void testRecovery(Set<Integer> listEntryKeys) throws Exception {
        // Create the actor and wait for recovery complete.

        int nListEntries = listEntryKeys.size();

        final CountDownLatch recoveryComplete = new CountDownLatch(1);

        @SuppressWarnings("serial")
        Creator<Shard> creator = new Creator<Shard>() {
            @Override
            public Shard create() throws Exception {
                return new Shard(shardID, Collections.<String,String>emptyMap(),
                        newDatastoreContext(), SCHEMA_CONTEXT) {
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

        TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                Props.create(new DelegatingShardCreator(creator)), "testRecovery");

        assertEquals("Recovery complete", true, recoveryComplete.await(5, TimeUnit.SECONDS));

        // Verify data in the data store.

        NormalizedNode<?, ?> outerList = readStore(shard, TestModel.OUTER_LIST_PATH);
        assertNotNull(TestModel.OUTER_LIST_QNAME.getLocalName() + " not found", outerList);
        assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " value is not Iterable",
                outerList.getValue() instanceof Iterable);
        for(Object entry: (Iterable<?>) outerList.getValue()) {
            assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " entry is not MapEntryNode",
                    entry instanceof MapEntryNode);
            MapEntryNode mapEntry = (MapEntryNode)entry;
            Optional<DataContainerChild<? extends PathArgument, ?>> idLeaf =
                    mapEntry.getChild(new YangInstanceIdentifier.NodeIdentifier(TestModel.ID_QNAME));
            assertTrue("Missing leaf " + TestModel.ID_QNAME.getLocalName(), idLeaf.isPresent());
            Object value = idLeaf.get().getValue();
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

    protected void verifyLastLogIndex(TestActorRef<Shard> shard, long expectedValue) {
        for(int i = 0; i < 20 * 5; i++) {
            long lastLogIndex = shard.underlyingActor().getShardMBean().getLastLogIndex();
            if(lastLogIndex == expectedValue) {
                break;
            }
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        assertEquals("Last log index", expectedValue, shard.underlyingActor().getShardMBean().getLastLogIndex());
    }

    protected NormalizedNode<?, ?> readStore(final InMemoryDOMDataStore store) throws ReadFailedException {
        DOMStoreReadTransaction transaction = store.newReadOnlyTransaction();
        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
            transaction.read(YangInstanceIdentifier.builder().build());

        Optional<NormalizedNode<?, ?>> optional = read.checkedGet();

        NormalizedNode<?, ?> normalizedNode = optional.get();

        transaction.close();

        return normalizedNode;
    }

    protected DOMStoreThreePhaseCommitCohort setupMockWriteTransaction(final String cohortName,
            final InMemoryDOMDataStore dataStore, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data,
            final MutableCompositeModification modification) {
        return setupMockWriteTransaction(cohortName, dataStore, path, data, modification, null);
    }

    protected DOMStoreThreePhaseCommitCohort setupMockWriteTransaction(final String cohortName,
            final InMemoryDOMDataStore dataStore, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data,
            final MutableCompositeModification modification,
            final Function<DOMStoreThreePhaseCommitCohort,ListenableFuture<Void>> preCommit) {

        DOMStoreWriteTransaction tx = dataStore.newWriteOnlyTransaction();
        tx.write(path, data);
        DOMStoreThreePhaseCommitCohort cohort = createDelegatingMockCohort(cohortName, tx.ready(), preCommit);

        modification.addModification(new WriteModification(path, data));

        return cohort;
    }

    protected DOMStoreThreePhaseCommitCohort createDelegatingMockCohort(final String cohortName,
            final DOMStoreThreePhaseCommitCohort actual) {
        return createDelegatingMockCohort(cohortName, actual, null);
    }

    protected DOMStoreThreePhaseCommitCohort createDelegatingMockCohort(final String cohortName,
            final DOMStoreThreePhaseCommitCohort actual,
            final Function<DOMStoreThreePhaseCommitCohort,ListenableFuture<Void>> preCommit) {
        DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class, cohortName);

        doAnswer(new Answer<ListenableFuture<Boolean>>() {
            @Override
            public ListenableFuture<Boolean> answer(final InvocationOnMock invocation) {
                return actual.canCommit();
            }
        }).when(cohort).canCommit();

        doAnswer(new Answer<ListenableFuture<Void>>() {
            @Override
            public ListenableFuture<Void> answer(final InvocationOnMock invocation) throws Throwable {
                return actual.preCommit();
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

        return cohort;
    }

    public static NormalizedNode<?,?> readStore(final TestActorRef<Shard> shard, final YangInstanceIdentifier id)
            throws ExecutionException, InterruptedException {
        return readStore(shard.underlyingActor().getDataStore(), id);
    }

    public static NormalizedNode<?,?> readStore(final InMemoryDOMDataStore store, final YangInstanceIdentifier id)
            throws ExecutionException, InterruptedException {
        DOMStoreReadTransaction transaction = store.newReadOnlyTransaction();

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
            transaction.read(id);

        Optional<NormalizedNode<?, ?>> optional = future.get();
        NormalizedNode<?, ?> node = optional.isPresent()? optional.get() : null;

        transaction.close();

        return node;
    }

    public static void writeToStore(final TestActorRef<Shard> shard, final YangInstanceIdentifier id,
            final NormalizedNode<?,?> node) throws ExecutionException, InterruptedException {
        writeToStore(shard.underlyingActor().getDataStore(), id, node);
    }

    public static void writeToStore(final InMemoryDOMDataStore store, final YangInstanceIdentifier id,
            final NormalizedNode<?,?> node) throws ExecutionException, InterruptedException {
        DOMStoreWriteTransaction transaction = store.newWriteOnlyTransaction();

        transaction.write(id, node);

        DOMStoreThreePhaseCommitCohort commitCohort = transaction.ready();
        commitCohort.preCommit().get();
        commitCohort.commit().get();
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
