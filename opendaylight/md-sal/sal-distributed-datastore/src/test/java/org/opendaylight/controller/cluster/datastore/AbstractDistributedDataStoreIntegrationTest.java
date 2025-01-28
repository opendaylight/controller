/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.ActorSystem;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;
import org.opendaylight.controller.cluster.access.client.RequestTimeoutException;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker;
import org.opendaylight.controller.cluster.datastore.TestShard.RequestFrontendMetadata;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendShardDataTreeSnapshotMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.datastore.utils.MockDataTreeChangeListener;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainClosedException;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class AbstractDistributedDataStoreIntegrationTest extends AbstractTest {
    @Parameter
    public Class<? extends ClientBackedDataStore> testParameter;

    protected ActorSystem system;

    protected final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder()
            .shardHeartbeatIntervalInMillis(100);

    protected ActorSystem getSystem() {
        return system;
    }

    @Test
    public void testWriteTransactionWithSingleShard() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "transactionIntegrationTest", "test-1")) {

            testKit.testWriteTransaction(dataStore, TestModel.TEST_PATH, TestModel.EMPTY_TEST);

            testKit.testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH, TestModel.outerNode(42));
        }
    }

    @Test
    public void testWriteTransactionWithMultipleShards() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testWriteTransactionWithMultipleShards",
            "cars-1", "people-1")) {

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

            testKit.doCommit(writeTx.ready());

            writeTx = dataStore.newWriteOnlyTransaction();

            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            writeTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

            testKit.doCommit(writeTx.ready());

            writeTx = dataStore.newWriteOnlyTransaction();

            final MapEntryNode car = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
            final YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
            writeTx.write(carPath, car);

            final MapEntryNode person = PeopleModel.newPersonEntry("jack");
            final YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
            writeTx.write(personPath, person);

            testKit.doCommit(writeTx.ready());

            // Verify the data in the store
            final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            assertEquals(Optional.of(car), readTx.read(carPath).get(5, TimeUnit.SECONDS));
            assertEquals(Optional.of(person), readTx.read(personPath).get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testReadWriteTransactionWithSingleShard() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testReadWriteTransactionWithSingleShard",
            "test-1")) {

            // 1. Create a read-write Tx
            final DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", readWriteTx);

            // 2. Write some data
            final YangInstanceIdentifier nodePath = TestModel.TEST_PATH;
            final ContainerNode nodeToWrite = TestModel.EMPTY_TEST;
            readWriteTx.write(nodePath, nodeToWrite);

            // 3. Read the data from Tx
            final Boolean exists = readWriteTx.exists(nodePath).get(5, TimeUnit.SECONDS);
            assertEquals("exists", Boolean.TRUE, exists);

            assertEquals(Optional.of(nodeToWrite), readWriteTx.read(nodePath).get(5, TimeUnit.SECONDS));

            // 4. Ready the Tx for commit
            final DOMStoreThreePhaseCommitCohort cohort = readWriteTx.ready();

            // 5. Commit the Tx
            testKit.doCommit(cohort);

            // 6. Verify the data in the store
            final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            assertEquals(Optional.of(nodeToWrite), readTx.read(nodePath).get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testReadWriteTransactionWithMultipleShards() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testReadWriteTransactionWithMultipleShards",
            "cars-1", "people-1")) {

            DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", readWriteTx);

            readWriteTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            readWriteTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

            testKit.doCommit(readWriteTx.ready());

            readWriteTx = dataStore.newReadWriteTransaction();

            readWriteTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            readWriteTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

            testKit.doCommit(readWriteTx.ready());

            readWriteTx = dataStore.newReadWriteTransaction();

            final MapEntryNode car = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
            final YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
            readWriteTx.write(carPath, car);

            final MapEntryNode person = PeopleModel.newPersonEntry("jack");
            final YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
            readWriteTx.write(personPath, person);

            final Boolean exists = readWriteTx.exists(carPath).get(5, TimeUnit.SECONDS);
            assertEquals("exists", Boolean.TRUE, exists);

            assertEquals("Data node", Optional.of(car), readWriteTx.read(carPath).get(5, TimeUnit.SECONDS));

            testKit.doCommit(readWriteTx.ready());

            // Verify the data in the store
            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            assertEquals(Optional.of(car), readTx.read(carPath).get(5, TimeUnit.SECONDS));
            assertEquals(Optional.of(person), readTx.read(personPath).get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testSingleTransactionsWritesInQuickSuccession() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testSingleTransactionsWritesInQuickSuccession",
            "cars-1")) {

            final var txChain = dataStore.createTransactionChain();

            var writeTx = txChain.newWriteOnlyTransaction();
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            testKit.doCommit(writeTx.ready());

            int numCars = 5;
            for (int i = 0; i < numCars; i++) {
                writeTx = txChain.newWriteOnlyTransaction();
                writeTx.write(CarsModel.newCarPath("car" + i), CarsModel.newCarEntry("car" + i, Uint64.valueOf(20000)));

                testKit.doCommit(writeTx.ready());

                try (var tx = txChain.newReadOnlyTransaction()) {
                    tx.read(CarsModel.BASE_PATH).get();
                }
            }

            // wait to let the shard catch up with purged
            await("transaction state propagation").atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // verify frontend metadata has no holes in purged transactions causing overtime memory leak
                    final var localShard = dataStore.getActorUtils().findLocalShard("cars-1") .orElseThrow();
                    final var frontendMetadata = (FrontendShardDataTreeSnapshotMetadata) dataStore.getActorUtils()
                            .executeOperation(localShard, new RequestFrontendMetadata());

                    final var clientMeta = frontendMetadata.getClients().get(0);
                    final var iterator = clientMeta.currentHistories().iterator();
                    var metadata = iterator.next();
                    while (iterator.hasNext() && metadata.getHistoryId() != 1) {
                        metadata = iterator.next();
                    }
                    assertEquals("[[0..10]]", metadata.getPurgedTransactions().ranges().toString());
                });

            final var body = txChain.newReadOnlyTransaction().read(CarsModel.CAR_LIST_PATH)
                .get(5, TimeUnit.SECONDS)
                .orElseThrow()
                .body();
            assertThat(body, instanceOf(Collection.class));
            assertEquals("# cars", numCars, ((Collection<?>) body).size());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void testTransactionCommitFailureWithNoShardLeader(final boolean writeOnly, final String testName)
            throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final String shardName = "default";

        // We don't want the shard to become the leader so prevent shard
        // elections.
        datastoreContextBuilder.customRaftPolicyImplementation(
                "org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy");

        // The ShardManager uses the election timeout for FindPrimary so
        // reset it low so it will timeout quickly.
        datastoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(1)
        .shardInitializationTimeout(200, TimeUnit.MILLISECONDS).frontendRequestTimeoutInSeconds(2);

        try (var dataStore = testKit.setupDataStore(testParameter, testName, false, shardName)) {
            final Object result = dataStore.getActorUtils().executeOperation(
                dataStore.getActorUtils().getShardManager(), new FindLocalShard(shardName, true));
            assertTrue("Expected LocalShardFound. Actual: " + result, result instanceof LocalShardFound);

            // Create the write Tx.
            DOMStoreWriteTransaction writeTxToClose = null;
            try {
                writeTxToClose = writeOnly ? dataStore.newWriteOnlyTransaction()
                        : dataStore.newReadWriteTransaction();
                final DOMStoreWriteTransaction writeTx = writeTxToClose;
                assertNotNull("newReadWriteTransaction returned null", writeTx);

                // Do some modifications and ready the Tx on a separate
                // thread.
                final AtomicReference<DOMStoreThreePhaseCommitCohort> txCohort = new AtomicReference<>();
                final AtomicReference<Exception> caughtEx = new AtomicReference<>();
                final CountDownLatch txReady = new CountDownLatch(1);
                final Thread txThread = new Thread(() -> {
                    try {
                        writeTx.write(TestModel.JUNK_PATH, ImmutableNodes.newContainerBuilder()
                            .withNodeIdentifier(new NodeIdentifier(TestModel.JUNK_QNAME))
                            .build());

                        txCohort.set(writeTx.ready());
                    } catch (Exception e) {
                        caughtEx.set(e);
                    } finally {
                        txReady.countDown();
                    }
                });

                txThread.start();

                // Wait for the Tx operations to complete.
                boolean done = Uninterruptibles.awaitUninterruptibly(txReady, 5, TimeUnit.SECONDS);
                if (caughtEx.get() != null) {
                    throw caughtEx.get();
                }

                assertTrue("Tx ready", done);

                // Wait for the commit to complete. Since no shard
                // leader was elected in time, the Tx
                // should have timed out and throw an appropriate
                // exception cause.
                final var ex = assertThrows(ExecutionException.class,
                    () -> txCohort.get().canCommit().get(10, TimeUnit.SECONDS));
                assertTrue("Unexpected exception: " + Throwables.getStackTraceAsString(ex.getCause()),
                    Throwables.getRootCause(ex) instanceof RequestTimeoutException);
            } finally {
                try {
                    if (writeTxToClose != null) {
                        writeTxToClose.close();
                    }
                } catch (Exception e) {
                    // FIXME TransactionProxy.close throws IllegalStateException:
                    // Transaction is ready, it cannot be closed
                }
            }
        }
    }

    @Test
    public void testWriteOnlyTransactionCommitFailureWithNoShardLeader() throws Exception {
        datastoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);
        testTransactionCommitFailureWithNoShardLeader(true, "testWriteOnlyTransactionCommitFailureWithNoShardLeader");
    }

    @Test
    public void testReadWriteTransactionCommitFailureWithNoShardLeader() throws Exception {
        testTransactionCommitFailureWithNoShardLeader(false, "testReadWriteTransactionCommitFailureWithNoShardLeader");
    }

    @Test
    public void testTransactionAbort() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "transactionAbortIntegrationTest", "test-1")) {

            final DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            writeTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);

            final DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            cohort.canCommit().get(5, TimeUnit.SECONDS);

            cohort.abort().get(5, TimeUnit.SECONDS);

            testKit.testWriteTransaction(dataStore, TestModel.TEST_PATH, TestModel.EMPTY_TEST);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionChainWithSingleShard() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testTransactionChainWithSingleShard", "test-1")) {

            // 1. Create a Tx chain and write-only Tx
            final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            final DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            // 2. Write some data
            final NormalizedNode testNode = TestModel.EMPTY_TEST;
            writeTx.write(TestModel.TEST_PATH, testNode);

            // 3. Ready the Tx for commit
            final DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

            // 4. Commit the Tx on another thread that first waits for
            // the second read Tx.
            final CountDownLatch continueCommit1 = new CountDownLatch(1);
            final CountDownLatch commit1Done = new CountDownLatch(1);
            final AtomicReference<Exception> commit1Error = new AtomicReference<>();
            new Thread(() -> {
                try {
                    continueCommit1.await();
                    testKit.doCommit(cohort1);
                } catch (Exception e) {
                    commit1Error.set(e);
                } finally {
                    commit1Done.countDown();
                }
            }).start();

            // 5. Create a new read Tx from the chain to read and verify
            // the data from the first
            // Tx is visible after being readied.
            DOMStoreReadTransaction readTx = txChain.newReadOnlyTransaction();
            assertEquals(Optional.of(testNode), readTx.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS));

            // 6. Create a new RW Tx from the chain, write more data,
            // and ready it
            final DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();
            final MapNode outerNode = TestModel.outerNode(42);
            rwTx.write(TestModel.OUTER_LIST_PATH, outerNode);

            final DOMStoreThreePhaseCommitCohort cohort2 = rwTx.ready();

            // 7. Create a new read Tx from the chain to read the data
            // from the last RW Tx to
            // verify it is visible.
            readTx = txChain.newReadWriteTransaction();
            assertEquals(Optional.of(outerNode), readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS));

            // 8. Wait for the 2 commits to complete and close the
            // chain.
            continueCommit1.countDown();
            Uninterruptibles.awaitUninterruptibly(commit1Done, 5, TimeUnit.SECONDS);

            if (commit1Error.get() != null) {
                throw commit1Error.get();
            }

            testKit.doCommit(cohort2);

            txChain.close();

            // 9. Create a new read Tx from the data store and verify
            // committed data.
            readTx = dataStore.newReadOnlyTransaction();
            assertEquals(Optional.of(outerNode), readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testTransactionChainWithMultipleShards() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testTransactionChainWithMultipleShards",
            "cars-1", "people-1")) {

            final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            writeTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

            final DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

            final DOMStoreReadWriteTransaction readWriteTx = txChain.newReadWriteTransaction();

            final MapEntryNode car = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
            final YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
            readWriteTx.write(carPath, car);

            final MapEntryNode person = PeopleModel.newPersonEntry("jack");
            final YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
            readWriteTx.merge(personPath, person);

            assertEquals(Optional.of(car), readWriteTx.read(carPath).get(5, TimeUnit.SECONDS));
            assertEquals(Optional.of(person), readWriteTx.read(personPath).get(5, TimeUnit.SECONDS));

            final DOMStoreThreePhaseCommitCohort cohort2 = readWriteTx.ready();

            writeTx = txChain.newWriteOnlyTransaction();

            writeTx.delete(carPath);

            final DOMStoreThreePhaseCommitCohort cohort3 = writeTx.ready();

            final ListenableFuture<Boolean> canCommit1 = cohort1.canCommit();
            final ListenableFuture<Boolean> canCommit2 = cohort2.canCommit();

            testKit.doCommit(canCommit1, cohort1);
            testKit.doCommit(canCommit2, cohort2);
            testKit.doCommit(cohort3);

            txChain.close();

            final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            assertEquals(Optional.empty(), readTx.read(carPath).get(5, TimeUnit.SECONDS));
            assertEquals(Optional.of(person), readTx.read(personPath).get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testCreateChainedTransactionsInQuickSuccession() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testCreateChainedTransactionsInQuickSuccession",
            "cars-1")) {

            final ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.CONFIGURATION, dataStore).build(),
                MoreExecutors.directExecutor());

            DOMTransactionChain txChain = broker.createTransactionChain();

            final List<ListenableFuture<?>> futures = new ArrayList<>();

            final DOMDataTreeWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.put(LogicalDatastoreType.CONFIGURATION, CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            futures.add(writeTx.commit());

            int numCars = 100;
            for (int i = 0; i < numCars; i++) {
                final DOMDataTreeReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

                rwTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.newCarPath("car" + i),
                    CarsModel.newCarEntry("car" + i, Uint64.valueOf(20000)));

                futures.add(rwTx.commit());
            }

            for (final ListenableFuture<?> f : futures) {
                f.get(5, TimeUnit.SECONDS);
            }

            final Optional<NormalizedNode> optional = txChain.newReadOnlyTransaction()
                    .read(LogicalDatastoreType.CONFIGURATION, CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertTrue("isPresent", optional.isPresent());
            assertEquals("# cars", numCars, ((Collection<?>) optional.orElseThrow().body()).size());

            txChain.close();

            broker.close();
        }
    }

    @Test
    public void testCreateChainedTransactionAfterEmptyTxReadied() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testCreateChainedTransactionAfterEmptyTxReadied",
            "test-1")) {

            final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            final DOMStoreReadWriteTransaction rwTx1 = txChain.newReadWriteTransaction();

            rwTx1.ready();

            final DOMStoreReadWriteTransaction rwTx2 = txChain.newReadWriteTransaction();

            final Optional<NormalizedNode> optional = rwTx2.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);
            assertFalse("isPresent", optional.isPresent());

            txChain.close();
        }
    }

    @Test
    public void testCreateChainedTransactionWhenPreviousNotReady() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testCreateChainedTransactionWhenPreviousNotReady",
            "test-1")) {

            final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            final DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            writeTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);

            // Try to create another Tx of each type - each should fail
            // b/c the previous Tx wasn't
            // readied.
            testKit.assertExceptionOnTxChainCreates(txChain, IllegalStateException.class);
        }
    }

    @Test
    public void testCreateChainedTransactionAfterClose() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testCreateChainedTransactionAfterClose",
            "test-1")) {

            final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();
            txChain.close();

            // Try to create another Tx of each type - should fail b/c
            // the previous Tx was closed.
            testKit.assertExceptionOnTxChainCreates(txChain, DOMTransactionChainClosedException.class);
        }
    }

    @Test
    public void testChainWithReadOnlyTxAfterPreviousReady() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testChainWithReadOnlyTxAfterPreviousReady",
            "test-1")) {

            final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            // Create a write tx and submit.
            final DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            writeTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);
            final DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

            // Create read-only tx's and issue a read.
            FluentFuture<Optional<NormalizedNode>> readFuture1 = txChain
                    .newReadOnlyTransaction().read(TestModel.TEST_PATH);

            FluentFuture<Optional<NormalizedNode>> readFuture2 = txChain
                    .newReadOnlyTransaction().read(TestModel.TEST_PATH);

            // Create another write tx and issue the write.
            DOMStoreWriteTransaction writeTx2 = txChain.newWriteOnlyTransaction();
            writeTx2.write(TestModel.OUTER_LIST_PATH, TestModel.outerNode(42));

            // Ensure the reads succeed.

            assertTrue("isPresent", readFuture1.get(5, TimeUnit.SECONDS).isPresent());
            assertTrue("isPresent", readFuture2.get(5, TimeUnit.SECONDS).isPresent());

            // Ensure the writes succeed.
            DOMStoreThreePhaseCommitCohort cohort2 = writeTx2.ready();

            testKit.doCommit(cohort1);
            testKit.doCommit(cohort2);

            assertTrue("isPresent", txChain.newReadOnlyTransaction().read(TestModel.OUTER_LIST_PATH)
                .get(5, TimeUnit.SECONDS).isPresent());
        }
    }

    @Test
    public void testChainedTransactionFailureWithSingleShard() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testChainedTransactionFailureWithSingleShard",
            "cars-1")) {

            final var broker = new ConcurrentDOMDataBroker(
                ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.CONFIGURATION, dataStore).build(),
                MoreExecutors.directExecutor());

            final var listener = mock(FutureCallback.class);
            final var txChain = broker.createTransactionChain();
            txChain.addCallback(listener);

            final var writeTx = txChain.newReadWriteTransaction();

            writeTx.put(LogicalDatastoreType.CONFIGURATION, PeopleModel.BASE_PATH,
                PeopleModel.emptyContainer());

            final var invalidData = ImmutableNodes.newContainerBuilder()
                    .withNodeIdentifier(new NodeIdentifier(CarsModel.BASE_QNAME))
                    .withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk"))
                    .build();

            writeTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, invalidData);

            assertThrows(ExecutionException.class, () -> writeTx.commit().get(5, TimeUnit.SECONDS));

            verify(listener, timeout(5000)).onFailure(any());

            txChain.close();
            broker.close();
        }
    }

    @Test
    public void testChainedTransactionFailureWithMultipleShards() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testChainedTransactionFailureWithMultipleShards",
            "cars-1", "people-1")) {

            final ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.CONFIGURATION, dataStore).build(),
                MoreExecutors.directExecutor());

            final var listener = mock(FutureCallback.class);
            final DOMTransactionChain txChain = broker.createTransactionChain();
            txChain.addCallback(listener);

            final DOMDataTreeWriteTransaction writeTx = txChain.newReadWriteTransaction();

            writeTx.put(LogicalDatastoreType.CONFIGURATION, PeopleModel.BASE_PATH,
                PeopleModel.emptyContainer());

            final ContainerNode invalidData = ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(CarsModel.BASE_QNAME))
                .withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk"))
                .build();

            writeTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, invalidData);

            // Note that merge will validate the data and fail but put
            // succeeds b/c deep validation is not
            // done for put for performance reasons.
            assertThrows(ExecutionException.class, () -> writeTx.commit().get(5, TimeUnit.SECONDS));

            verify(listener, timeout(5000)).onFailure(any());

            txChain.close();
            broker.close();
        }
    }

    @Test
    public void testDataTreeChangeListenerRegistration() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        try (var dataStore = testKit.setupDataStore(testParameter, "testDataTreeChangeListenerRegistration",
            "test-1")) {

            testKit.testWriteTransaction(dataStore, TestModel.TEST_PATH, TestModel.EMPTY_TEST);

            final MockDataTreeChangeListener listener = new MockDataTreeChangeListener(1);

            final var listenerReg = dataStore.registerTreeChangeListener(TestModel.TEST_PATH, listener);

            assertNotNull("registerTreeChangeListener returned null", listenerReg);

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("getTreeChangeListenerActors", 1,
                        state.getTreeChangeListenerActors().size()));

            // Wait for the initial notification
            listener.waitForChangeEvents(TestModel.TEST_PATH);
            listener.reset(2);

            // Write 2 updates.
            testKit.testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH, TestModel.outerNode(42));

            final var listPath = TestModel.outerEntryPath(1);
            testKit.testWriteTransaction(dataStore, listPath, TestModel.outerEntry(1));

            // Wait for the 2 updates.
            listener.waitForChangeEvents(TestModel.OUTER_LIST_PATH, listPath);
            listenerReg.close();

            IntegrationTestKit.verifyShardState(dataStore, "test-1",
                state -> assertEquals("getTreeChangeListenerActors", 0,
                    state.getTreeChangeListenerActors().size()));

            testKit.testWriteTransaction(dataStore, TestModel.outerEntryPath(2), TestModel.outerEntry(2));

            listener.expectNoMoreChanges("Received unexpected change after close");
        }
    }

    @Test
    public void testRestoreFromDatastoreSnapshot() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final String name = "transactionIntegrationTest";

        final ContainerNode carsNode = CarsModel.newCarsNode(
            CarsModel.newCarsMapNode(CarsModel.newCarEntry("optima", Uint64.valueOf(20000)),
                CarsModel.newCarEntry("sportage", Uint64.valueOf(30000))));

        DataTree dataTree = new InMemoryDataTreeFactory().create(
            DataTreeConfiguration.DEFAULT_OPERATIONAL, SchemaContextHelper.full());
        AbstractShardTest.writeToStore(dataTree, CarsModel.BASE_PATH, carsNode);
        NormalizedNode root = AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.of());

        final Snapshot carsSnapshot = Snapshot.create(
            new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
            List.of(), 2, 1, 2, 1, new TermInfo(1, "member-1"), null);

        dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL,
            SchemaContextHelper.full());

        final NormalizedNode peopleNode = PeopleModel.create();
        AbstractShardTest.writeToStore(dataTree, PeopleModel.BASE_PATH, peopleNode);

        root = AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.of());

        final Snapshot peopleSnapshot = Snapshot.create(
            new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
            List.of(), 2, 1, 2, 1, new TermInfo(1, "member-1"), null);

        testKit.restoreFromSnapshot = new DatastoreSnapshot(name, null, Arrays.asList(
            new DatastoreSnapshot.ShardSnapshot("cars", carsSnapshot),
            new DatastoreSnapshot.ShardSnapshot("people", peopleSnapshot)));

        try (var dataStore = testKit.setupDataStore(testParameter, name, "module-shards-member1.conf", true,
            "cars", "people")) {

            final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            // two reads
            assertEquals(Optional.of(carsNode), readTx.read(CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS));
            assertEquals(Optional.of(peopleNode), readTx.read(PeopleModel.BASE_PATH).get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @Ignore("ClientBackedDatastore does not have stable indexes/term, the snapshot index seems to fluctuate")
    // FIXME: re-enable this test
    public void testSnapshotOnRootOverwrite() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(),
            datastoreContextBuilder.snapshotOnRootOverwrite(true));
        try (var dataStore = testKit.setupDataStore(testParameter, "testRootOverwrite",
            "module-shards-default-cars-member1.conf", true, "cars", "default")) {

            final var rootNode = ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SchemaContext.NAME))
                .withChild(CarsModel.create())
                .build();

            testKit.testWriteTransaction(dataStore, YangInstanceIdentifier.of(), rootNode);
            IntegrationTestKit.verifyShardState(dataStore, "cars",
                state -> assertEquals(1, state.getSnapshotIndex()));

            // root has been written expect snapshot at index 0
            verifySnapshot("member-1-shard-cars-testRootOverwrite", 1, 1);

            for (int i = 0; i < 10; i++) {
                testKit.testWriteTransaction(dataStore, CarsModel.newCarPath("car " + i),
                    CarsModel.newCarEntry("car " + i, Uint64.ONE));
            }

            // fake snapshot causes the snapshotIndex to move
            IntegrationTestKit.verifyShardState(dataStore, "cars",
                state -> assertEquals(10, state.getSnapshotIndex()));

            // however the real snapshot still has not changed and was taken at index 0
            verifySnapshot("member-1-shard-cars-testRootOverwrite", 1, 1);

            // root overwrite so expect a snapshot
            testKit.testWriteTransaction(dataStore, YangInstanceIdentifier.of(), rootNode);

            // this was a real snapshot so everything should be in it(1 + 10 + 1)
            IntegrationTestKit.verifyShardState(dataStore, "cars",
                state -> assertEquals(12, state.getSnapshotIndex()));

            verifySnapshot("member-1-shard-cars-testRootOverwrite", 12, 1);
        }
    }

    private static void verifySnapshot(final String persistenceId, final long lastAppliedIndex,
            final long lastAppliedTerm) {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Snapshot> snap = InMemorySnapshotStore.getSnapshots(persistenceId, Snapshot.class);
                assertEquals(1, snap.size());
                assertEquals(lastAppliedIndex, snap.get(0).getLastAppliedIndex());
                assertEquals(lastAppliedTerm, snap.get(0).getLastAppliedTerm());
            }
        );
    }
}
