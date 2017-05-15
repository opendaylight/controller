/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.PayloadVersion;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.datastore.utils.MockDataChangeListener;
import org.opendaylight.controller.cluster.datastore.utils.MockDataTreeChangeListener;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

@RunWith(Parameterized.class)
public class DistributedDataStoreIntegrationTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { DistributedDataStore.class }, { ClientBackedDataStore.class }
        });
    }

    @Parameter
    public Class<? extends AbstractDataStore> testParameter;

    private ActorSystem system;

    private final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder()
            .shardHeartbeatIntervalInMillis(100);

    @Before
    public void setUp() throws IOException {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
        system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Address member1Address = AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558");
        Cluster.get(system).join(member1Address);
    }

    @After
    public void tearDown() throws IOException {
        JavaTestKit.shutdownActorSystem(system, null, Boolean.TRUE);
        system = null;
    }

    protected ActorSystem getSystem() {
        return system;
    }

    @Test
    public void testWriteTransactionWithSingleShard() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "transactionIntegrationTest", "test-1")) {

                    testWriteTransaction(dataStore, TestModel.TEST_PATH,
                            ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                    testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                            ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());
                }
            }
        };
    }

    @Test
    public void testWriteTransactionWithMultipleShards() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testWriteTransactionWithMultipleShards", "cars-1", "people-1")) {

                    DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
                    assertNotNull("newWriteOnlyTransaction returned null", writeTx);

                    writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
                    writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

                    doCommit(writeTx.ready());

                    writeTx = dataStore.newWriteOnlyTransaction();

                    writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
                    writeTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

                    doCommit(writeTx.ready());

                    writeTx = dataStore.newWriteOnlyTransaction();

                    final MapEntryNode car = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
                    final YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
                    writeTx.write(carPath, car);

                    final MapEntryNode person = PeopleModel.newPersonEntry("jack");
                    final YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
                    writeTx.write(personPath, person);

                    doCommit(writeTx.ready());

                    // Verify the data in the store
                    final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

                    Optional<NormalizedNode<?, ?>> optional = readTx.read(carPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", car, optional.get());

                    optional = readTx.read(personPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", person, optional.get());
                }
            }
        };
    }

    @Test
    public void testReadWriteTransactionWithSingleShard() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testReadWriteTransactionWithSingleShard", "test-1")) {

                    // 1. Create a read-write Tx
                    final DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
                    assertNotNull("newReadWriteTransaction returned null", readWriteTx);

                    // 2. Write some data
                    final YangInstanceIdentifier nodePath = TestModel.TEST_PATH;
                    final NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
                    readWriteTx.write(nodePath, nodeToWrite);

                    // 3. Read the data from Tx
                    final Boolean exists = readWriteTx.exists(nodePath).checkedGet(5, TimeUnit.SECONDS);
                    assertEquals("exists", true, exists);

                    Optional<NormalizedNode<?, ?>> optional = readWriteTx.read(nodePath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", nodeToWrite, optional.get());

                    // 4. Ready the Tx for commit
                    final DOMStoreThreePhaseCommitCohort cohort = readWriteTx.ready();

                    // 5. Commit the Tx
                    doCommit(cohort);

                    // 6. Verify the data in the store
                    final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

                    optional = readTx.read(nodePath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", nodeToWrite, optional.get());
                }
            }
        };
    }

    @Test
    public void testReadWriteTransactionWithMultipleShards() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testReadWriteTransactionWithMultipleShards", "cars-1", "people-1")) {

                    DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
                    assertNotNull("newReadWriteTransaction returned null", readWriteTx);

                    readWriteTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
                    readWriteTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

                    doCommit(readWriteTx.ready());

                    readWriteTx = dataStore.newReadWriteTransaction();

                    readWriteTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
                    readWriteTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

                    doCommit(readWriteTx.ready());

                    readWriteTx = dataStore.newReadWriteTransaction();

                    final MapEntryNode car = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
                    final YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
                    readWriteTx.write(carPath, car);

                    final MapEntryNode person = PeopleModel.newPersonEntry("jack");
                    final YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
                    readWriteTx.write(personPath, person);

                    final Boolean exists = readWriteTx.exists(carPath).checkedGet(5, TimeUnit.SECONDS);
                    assertEquals("exists", true, exists);

                    Optional<NormalizedNode<?, ?>> optional = readWriteTx.read(carPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", car, optional.get());

                    doCommit(readWriteTx.ready());

                    // Verify the data in the store
                    DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

                    optional = readTx.read(carPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", car, optional.get());

                    optional = readTx.read(personPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", person, optional.get());

                }
            }
        };
    }

    @Test
    public void testSingleTransactionsWritesInQuickSuccession() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testSingleTransactionsWritesInQuickSuccession", "cars-1")) {

                    final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

                    DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
                    writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
                    writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
                    doCommit(writeTx.ready());

                    writeTx = txChain.newWriteOnlyTransaction();

                    int numCars = 5;
                    for (int i = 0; i < numCars; i++) {
                        writeTx.write(CarsModel.newCarPath("car" + i),
                                CarsModel.newCarEntry("car" + i, BigInteger.valueOf(20000)));
                    }

                    doCommit(writeTx.ready());

                    final Optional<NormalizedNode<?, ?>> optional = txChain.newReadOnlyTransaction()
                            .read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("# cars", numCars, ((Collection<?>) optional.get().getValue()).size());
                }
            }
        };
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void testTransactionWritesWithShardNotInitiallyReady(final String testName, final boolean writeOnly)
            throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final String shardName = "test-1";

                // Setup the InMemoryJournal to block shard recovery to ensure
                // the shard isn't
                // initialized until we create and submit the write the Tx.
                final String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
                final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
                InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, testName, false, shardName)) {

                    // Create the write Tx
                    final DOMStoreWriteTransaction writeTx = writeOnly ? dataStore.newWriteOnlyTransaction()
                            : dataStore.newReadWriteTransaction();
                    assertNotNull("newReadWriteTransaction returned null", writeTx);

                    // Do some modification operations and ready the Tx on a
                    // separate thread.
                    final YangInstanceIdentifier listEntryPath = YangInstanceIdentifier
                            .builder(TestModel.OUTER_LIST_PATH)
                            .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build();

                    final AtomicReference<DOMStoreThreePhaseCommitCohort> txCohort = new AtomicReference<>();
                    final AtomicReference<Exception> caughtEx = new AtomicReference<>();
                    final CountDownLatch txReady = new CountDownLatch(1);
                    final Thread txThread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                                writeTx.merge(TestModel.OUTER_LIST_PATH,
                                        ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

                                writeTx.write(listEntryPath,
                                        ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1));

                                writeTx.delete(listEntryPath);

                                txCohort.set(writeTx.ready());
                            } catch (Exception e) {
                                caughtEx.set(e);
                            } finally {
                                txReady.countDown();
                            }
                        }
                    };

                    txThread.start();

                    // Wait for the Tx operations to complete.
                    final boolean done = Uninterruptibles.awaitUninterruptibly(txReady, 5, TimeUnit.SECONDS);
                    if (caughtEx.get() != null) {
                        throw caughtEx.get();
                    }

                    assertEquals("Tx ready", true, done);

                    // At this point the Tx operations should be waiting for the
                    // shard to initialize so
                    // trigger the latch to let the shard recovery to continue.
                    blockRecoveryLatch.countDown();

                    // Wait for the Tx commit to complete.
                    doCommit(txCohort.get());

                    // Verify the data in the store
                    final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

                    Optional<NormalizedNode<?, ?>> optional = readTx.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());

                    optional = readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());

                    optional = readTx.read(listEntryPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", false, optional.isPresent());
                }
            }
        };
    }

    @Test
    public void testWriteOnlyTransactionWithShardNotInitiallyReady() throws Exception {
        datastoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);
        testTransactionWritesWithShardNotInitiallyReady("testWriteOnlyTransactionWithShardNotInitiallyReady", true);
    }

    @Test
    public void testReadWriteTransactionWithShardNotInitiallyReady() throws Exception {
        testTransactionWritesWithShardNotInitiallyReady("testReadWriteTransactionWithShardNotInitiallyReady", false);
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionReadsWithShardNotInitiallyReady() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final String testName = "testTransactionReadsWithShardNotInitiallyReady";
                final String shardName = "test-1";

                // Setup the InMemoryJournal to block shard recovery to ensure
                // the shard isn't
                // initialized until we create the Tx.
                final String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
                final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
                InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, testName, false, shardName)) {

                    // Create the read-write Tx
                    final DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
                    assertNotNull("newReadWriteTransaction returned null", readWriteTx);

                    // Do some reads on the Tx on a separate thread.
                    final AtomicReference<CheckedFuture<Boolean, ReadFailedException>> txExistsFuture =
                            new AtomicReference<>();
                    final AtomicReference<CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>>
                            txReadFuture = new AtomicReference<>();
                    final AtomicReference<Exception> caughtEx = new AtomicReference<>();
                    final CountDownLatch txReadsDone = new CountDownLatch(1);
                    final Thread txThread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                readWriteTx.write(TestModel.TEST_PATH,
                                        ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                                txExistsFuture.set(readWriteTx.exists(TestModel.TEST_PATH));

                                txReadFuture.set(readWriteTx.read(TestModel.TEST_PATH));
                            } catch (Exception e) {
                                caughtEx.set(e);
                            } finally {
                                txReadsDone.countDown();
                            }
                        }
                    };

                    txThread.start();

                    // Wait for the Tx operations to complete.
                    boolean done = Uninterruptibles.awaitUninterruptibly(txReadsDone, 5, TimeUnit.SECONDS);
                    if (caughtEx.get() != null) {
                        throw caughtEx.get();
                    }

                    assertEquals("Tx reads done", true, done);

                    // At this point the Tx operations should be waiting for the
                    // shard to initialize so
                    // trigger the latch to let the shard recovery to continue.
                    blockRecoveryLatch.countDown();

                    // Wait for the reads to complete and verify.
                    assertEquals("exists", true, txExistsFuture.get().checkedGet(5, TimeUnit.SECONDS));
                    assertEquals("read", true, txReadFuture.get().checkedGet(5, TimeUnit.SECONDS).isPresent());

                    readWriteTx.close();
                }
            }
        };
    }

    @Test(expected = NotInitializedException.class)
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionCommitFailureWithShardNotInitialized() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final String testName = "testTransactionCommitFailureWithShardNotInitialized";
                final String shardName = "test-1";

                // Set the shard initialization timeout low for the test.
                datastoreContextBuilder.shardInitializationTimeout(300, TimeUnit.MILLISECONDS);

                // Setup the InMemoryJournal to block shard recovery
                // indefinitely.
                final String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
                final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
                InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

                InMemoryJournal.addEntry(persistentID, 1, "Dummy data so akka will read from persistence");

                final AbstractDataStore dataStore =
                        setupAbstractDataStore(testParameter, testName, false, shardName);

                // Create the write Tx
                final DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
                assertNotNull("newReadWriteTransaction returned null", writeTx);

                // Do some modifications and ready the Tx on a separate
                // thread.
                final AtomicReference<DOMStoreThreePhaseCommitCohort> txCohort = new AtomicReference<>();
                final AtomicReference<Exception> caughtEx = new AtomicReference<>();
                final CountDownLatch txReady = new CountDownLatch(1);
                final Thread txThread = new Thread(() -> {
                    try {
                        writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

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

                assertEquals("Tx ready", true, done);

                // Wait for the commit to complete. Since the shard never
                // initialized, the Tx should
                // have timed out and throw an appropriate exception cause.
                try {
                    txCohort.get().canCommit().get(5, TimeUnit.SECONDS);
                    fail("Expected NotInitializedException");
                } catch (final Exception e) {
                    Throwables.propagate(Throwables.getRootCause(e));
                } finally {
                    blockRecoveryLatch.countDown();
                }
            }
        };
    }

    @Test(expected = NotInitializedException.class)
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionReadFailureWithShardNotInitialized() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final String testName = "testTransactionReadFailureWithShardNotInitialized";
                final String shardName = "test-1";

                // Set the shard initialization timeout low for the test.
                datastoreContextBuilder.shardInitializationTimeout(300, TimeUnit.MILLISECONDS);

                // Setup the InMemoryJournal to block shard recovery
                // indefinitely.
                final String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
                final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
                InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

                InMemoryJournal.addEntry(persistentID, 1, "Dummy data so akka will read from persistence");

                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, testName, false, shardName)) {

                    // Create the read-write Tx
                    final DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
                    assertNotNull("newReadWriteTransaction returned null", readWriteTx);

                    // Do a read on the Tx on a separate thread.
                    final AtomicReference<CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>>
                            txReadFuture = new AtomicReference<>();
                    final AtomicReference<Exception> caughtEx = new AtomicReference<>();
                    final CountDownLatch txReadDone = new CountDownLatch(1);
                    final Thread txThread = new Thread(() -> {
                        try {
                            readWriteTx.write(TestModel.TEST_PATH,
                                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                            txReadFuture.set(readWriteTx.read(TestModel.TEST_PATH));

                            readWriteTx.close();
                        } catch (Exception e) {
                            caughtEx.set(e);
                        } finally {
                            txReadDone.countDown();
                        }
                    });

                    txThread.start();

                    // Wait for the Tx operations to complete.
                    boolean done = Uninterruptibles.awaitUninterruptibly(txReadDone, 5, TimeUnit.SECONDS);
                    if (caughtEx.get() != null) {
                        throw caughtEx.get();
                    }

                    assertEquals("Tx read done", true, done);

                    // Wait for the read to complete. Since the shard never
                    // initialized, the Tx should
                    // have timed out and throw an appropriate exception cause.
                    try {
                        txReadFuture.get().checkedGet(5, TimeUnit.SECONDS);
                        fail("Expected NotInitializedException");
                    } catch (final ReadFailedException e) {
                        Throwables.propagate(Throwables.getRootCause(e));
                    } finally {
                        blockRecoveryLatch.countDown();
                    }
                }
            }
        };
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void testTransactionCommitFailureWithNoShardLeader(final boolean writeOnly, final String testName)
            throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final String shardName = "default";

                // We don't want the shard to become the leader so prevent shard
                // elections.
                datastoreContextBuilder.customRaftPolicyImplementation(
                        "org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy");

                // The ShardManager uses the election timeout for FindPrimary so
                // reset it low so it will timeout quickly.
                datastoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(1)
                        .shardInitializationTimeout(200, TimeUnit.MILLISECONDS);

                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, testName, false, shardName)) {

                    final Object result = dataStore.getActorContext().executeOperation(
                            dataStore.getActorContext().getShardManager(), new FindLocalShard(shardName, true));
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
                                writeTx.write(TestModel.JUNK_PATH,
                                        ImmutableNodes.containerNode(TestModel.JUNK_QNAME));

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

                        assertEquals("Tx ready", true, done);

                        // Wait for the commit to complete. Since no shard
                        // leader was elected in time, the Tx
                        // should have timed out and throw an appropriate
                        // exception cause.
                        try {
                            txCohort.get().canCommit().get(5, TimeUnit.SECONDS);
                            fail("Expected NoShardLeaderException");
                        } catch (final ExecutionException e) {
                            assertTrue(Throwables.getRootCause(e) instanceof NoShardLeaderException);
                            assertEquals(DistributedDataStore.class, testParameter);
                        } catch (TimeoutException e) {
                            // ClientBackedDataStore doesn't set cause to ExecutionException, future just time outs
                            assertEquals(ClientBackedDataStore.class, testParameter);
                        }
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
        };
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
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "transactionAbortIntegrationTest", "test-1")) {

                    final DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
                    assertNotNull("newWriteOnlyTransaction returned null", writeTx);

                    writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                    final DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

                    cohort.canCommit().get(5, TimeUnit.SECONDS);

                    cohort.abort().get(5, TimeUnit.SECONDS);

                    testWriteTransaction(dataStore, TestModel.TEST_PATH,
                            ImmutableNodes.containerNode(TestModel.TEST_QNAME));
                }
            }
        };
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionChainWithSingleShard() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testTransactionChainWithSingleShard", "test-1")) {

                    // 1. Create a Tx chain and write-only Tx
                    final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

                    final DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
                    assertNotNull("newWriteOnlyTransaction returned null", writeTx);

                    // 2. Write some data
                    final NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
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
                            doCommit(cohort1);
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
                    Optional<NormalizedNode<?, ?>> optional = readTx.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", testNode, optional.get());

                    // 6. Create a new RW Tx from the chain, write more data,
                    // and ready it
                    final DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();
                    final MapNode outerNode = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build();
                    rwTx.write(TestModel.OUTER_LIST_PATH, outerNode);

                    final DOMStoreThreePhaseCommitCohort cohort2 = rwTx.ready();

                    // 7. Create a new read Tx from the chain to read the data
                    // from the last RW Tx to
                    // verify it is visible.
                    readTx = txChain.newReadWriteTransaction();
                    optional = readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", outerNode, optional.get());

                    // 8. Wait for the 2 commits to complete and close the
                    // chain.
                    continueCommit1.countDown();
                    Uninterruptibles.awaitUninterruptibly(commit1Done, 5, TimeUnit.SECONDS);

                    if (commit1Error.get() != null) {
                        throw commit1Error.get();
                    }

                    doCommit(cohort2);

                    txChain.close();

                    // 9. Create a new read Tx from the data store and verify
                    // committed data.
                    readTx = dataStore.newReadOnlyTransaction();
                    optional = readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", outerNode, optional.get());
                }
            }
        };
    }

    @Test
    public void testTransactionChainWithMultipleShards() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testTransactionChainWithMultipleShards", "cars-1", "people-1")) {

                    final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

                    DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
                    assertNotNull("newWriteOnlyTransaction returned null", writeTx);

                    writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
                    writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

                    writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
                    writeTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

                    final DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

                    final DOMStoreReadWriteTransaction readWriteTx = txChain.newReadWriteTransaction();

                    final MapEntryNode car = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
                    final YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
                    readWriteTx.write(carPath, car);

                    final MapEntryNode person = PeopleModel.newPersonEntry("jack");
                    final YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
                    readWriteTx.merge(personPath, person);

                    Optional<NormalizedNode<?, ?>> optional = readWriteTx.read(carPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", car, optional.get());

                    optional = readWriteTx.read(personPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", person, optional.get());

                    final DOMStoreThreePhaseCommitCohort cohort2 = readWriteTx.ready();

                    writeTx = txChain.newWriteOnlyTransaction();

                    writeTx.delete(carPath);

                    final DOMStoreThreePhaseCommitCohort cohort3 = writeTx.ready();

                    final ListenableFuture<Boolean> canCommit1 = cohort1.canCommit();
                    final ListenableFuture<Boolean> canCommit2 = cohort2.canCommit();

                    doCommit(canCommit1, cohort1);
                    doCommit(canCommit2, cohort2);
                    doCommit(cohort3);

                    txChain.close();

                    final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

                    optional = readTx.read(carPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", false, optional.isPresent());

                    optional = readTx.read(personPath).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", person, optional.get());
                }
            }
        };
    }

    @Test
    public void testCreateChainedTransactionsInQuickSuccession() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testCreateChainedTransactionsInQuickSuccession", "cars-1")) {

                    final ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                            ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                                    .put(LogicalDatastoreType.CONFIGURATION, dataStore).build(),
                            MoreExecutors.directExecutor());

                    final TransactionChainListener listener = Mockito.mock(TransactionChainListener.class);
                    DOMTransactionChain txChain = broker.createTransactionChain(listener);

                    final List<CheckedFuture<Void, TransactionCommitFailedException>> futures = new ArrayList<>();

                    final DOMDataWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
                    writeTx.put(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, CarsModel.emptyContainer());
                    writeTx.put(LogicalDatastoreType.CONFIGURATION, CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
                    futures.add(writeTx.submit());

                    int numCars = 100;
                    for (int i = 0; i < numCars; i++) {
                        final DOMDataReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

                        rwTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.newCarPath("car" + i),
                                CarsModel.newCarEntry("car" + i, BigInteger.valueOf(20000)));

                        futures.add(rwTx.submit());
                    }

                    for (final CheckedFuture<Void, TransactionCommitFailedException> f : futures) {
                        f.checkedGet();
                    }

                    final Optional<NormalizedNode<?, ?>> optional = txChain.newReadOnlyTransaction()
                            .read(LogicalDatastoreType.CONFIGURATION, CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("# cars", numCars, ((Collection<?>) optional.get().getValue()).size());

                    txChain.close();

                    broker.close();
                }
            }
        };
    }

    @Test
    public void testCreateChainedTransactionAfterEmptyTxReadied() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testCreateChainedTransactionAfterEmptyTxReadied", "test-1")) {

                    final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

                    final DOMStoreReadWriteTransaction rwTx1 = txChain.newReadWriteTransaction();

                    rwTx1.ready();

                    final DOMStoreReadWriteTransaction rwTx2 = txChain.newReadWriteTransaction();

                    final Optional<NormalizedNode<?, ?>> optional = rwTx2.read(TestModel.TEST_PATH).get(
                            5, TimeUnit.SECONDS);
                    assertEquals("isPresent", false, optional.isPresent());

                    txChain.close();
                }
            }
        };
    }

    @Test
    public void testCreateChainedTransactionWhenPreviousNotReady() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testCreateChainedTransactionWhenPreviousNotReady", "test-1")) {

                    final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

                    final DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
                    assertNotNull("newWriteOnlyTransaction returned null", writeTx);

                    writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                    // Try to create another Tx of each type - each should fail
                    // b/c the previous Tx wasn't
                    // readied.
                    assertExceptionOnTxChainCreates(txChain, IllegalStateException.class);
                }
            }
        };
    }

    @Test
    public void testCreateChainedTransactionAfterClose() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testCreateChainedTransactionAfterClose", "test-1")) {

                    final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();
                    txChain.close();

                    // Try to create another Tx of each type - should fail b/c
                    // the previous Tx was closed.
                    assertExceptionOnTxChainCreates(txChain, TransactionChainClosedException.class);
                }
            }
        };
    }

    @Test
    public void testChainWithReadOnlyTxAfterPreviousReady() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testChainWithReadOnlyTxAfterPreviousReady", "test-1")) {

                    final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

                    // Create a write tx and submit.
                    final DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
                    writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
                    final DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

                    // Create read-only tx's and issue a read.
                    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture1 = txChain
                            .newReadOnlyTransaction().read(TestModel.TEST_PATH);

                    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture2 = txChain
                            .newReadOnlyTransaction().read(TestModel.TEST_PATH);

                    // Create another write tx and issue the write.
                    DOMStoreWriteTransaction writeTx2 = txChain.newWriteOnlyTransaction();
                    writeTx2.write(TestModel.OUTER_LIST_PATH,
                            ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

                    // Ensure the reads succeed.

                    assertEquals("isPresent", true, readFuture1.checkedGet(5, TimeUnit.SECONDS).isPresent());
                    assertEquals("isPresent", true, readFuture2.checkedGet(5, TimeUnit.SECONDS).isPresent());

                    // Ensure the writes succeed.
                    DOMStoreThreePhaseCommitCohort cohort2 = writeTx2.ready();

                    doCommit(cohort1);
                    doCommit(cohort2);

                    assertEquals("isPresent", true, txChain.newReadOnlyTransaction().read(TestModel.OUTER_LIST_PATH)
                            .checkedGet(5, TimeUnit.SECONDS).isPresent());
                }
            }
        };
    }

    @Test
    public void testChainedTransactionFailureWithSingleShard() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testChainedTransactionFailureWithSingleShard", "cars-1")) {

                    final ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                            ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                                    .put(LogicalDatastoreType.CONFIGURATION, dataStore).build(),
                            MoreExecutors.directExecutor());

                    final TransactionChainListener listener = Mockito.mock(TransactionChainListener.class);
                    final DOMTransactionChain txChain = broker.createTransactionChain(listener);

                    final DOMDataReadWriteTransaction writeTx = txChain.newReadWriteTransaction();

                    writeTx.put(LogicalDatastoreType.CONFIGURATION, PeopleModel.BASE_PATH,
                            PeopleModel.emptyContainer());

                    final ContainerNode invalidData = ImmutableContainerNodeBuilder.create()
                            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CarsModel.BASE_QNAME))
                            .withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk")).build();

                    writeTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, invalidData);

                    try {
                        writeTx.submit().checkedGet(5, TimeUnit.SECONDS);
                        fail("Expected TransactionCommitFailedException");
                    } catch (final TransactionCommitFailedException e) {
                        // Expected
                    }

                    verify(listener, timeout(5000)).onTransactionChainFailed(eq(txChain), eq(writeTx),
                            any(Throwable.class));

                    txChain.close();
                    broker.close();
                }
            }
        };
    }

    @Test
    public void testChainedTransactionFailureWithMultipleShards() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testChainedTransactionFailureWithMultipleShards", "cars-1", "people-1")) {

                    final ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                            ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                                    .put(LogicalDatastoreType.CONFIGURATION, dataStore).build(),
                            MoreExecutors.directExecutor());

                    final TransactionChainListener listener = Mockito.mock(TransactionChainListener.class);
                    final DOMTransactionChain txChain = broker.createTransactionChain(listener);

                    final DOMDataReadWriteTransaction writeTx = txChain.newReadWriteTransaction();

                    writeTx.put(LogicalDatastoreType.CONFIGURATION, PeopleModel.BASE_PATH,
                            PeopleModel.emptyContainer());

                    final ContainerNode invalidData = ImmutableContainerNodeBuilder.create()
                            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CarsModel.BASE_QNAME))
                            .withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk")).build();

                    writeTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, invalidData);

                    // Note that merge will validate the data and fail but put
                    // succeeds b/c deep validation is not
                    // done for put for performance reasons.
                    try {
                        writeTx.submit().checkedGet(5, TimeUnit.SECONDS);
                        fail("Expected TransactionCommitFailedException");
                    } catch (final TransactionCommitFailedException e) {
                        // Expected
                    }

                    verify(listener, timeout(5000)).onTransactionChainFailed(eq(txChain), eq(writeTx),
                            any(Throwable.class));

                    txChain.close();
                    broker.close();
                }
            }
        };
    }

    @Test
    public void testChangeListenerRegistration() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testChangeListenerRegistration", "test-1")) {

                    testWriteTransaction(dataStore, TestModel.TEST_PATH,
                            ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                    final MockDataChangeListener listener = new MockDataChangeListener(1);

                    final ListenerRegistration<MockDataChangeListener> listenerReg = dataStore
                            .registerChangeListener(TestModel.TEST_PATH, listener, DataChangeScope.SUBTREE);

                    assertNotNull("registerChangeListener returned null", listenerReg);

                    IntegrationTestKit.verifyShardState(dataStore, "test-1",
                        state -> assertEquals("getDataChangeListenerActors", 1,
                                state.getDataChangeListenerActors().size()));

                    // Wait for the initial notification
                    listener.waitForChangeEvents(TestModel.TEST_PATH);
                    listener.reset(2);

                    // Write 2 updates.
                    testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                            ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

                    YangInstanceIdentifier listPath = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                            .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build();
                    testWriteTransaction(dataStore, listPath,
                            ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1));

                    // Wait for the 2 updates.
                    listener.waitForChangeEvents(TestModel.OUTER_LIST_PATH, listPath);
                    listenerReg.close();

                    IntegrationTestKit.verifyShardState(dataStore, "test-1",
                        state -> assertEquals("getDataChangeListenerActors", 0,
                                state.getDataChangeListenerActors().size()));

                    testWriteTransaction(dataStore,
                            YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2).build(),
                            ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2));

                    listener.expectNoMoreChanges("Received unexpected change after close");
                }
            }
        };
    }

    @Test
    public void testDataTreeChangeListenerRegistration() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, "testDataTreeChangeListenerRegistration", "test-1")) {

                    testWriteTransaction(dataStore, TestModel.TEST_PATH,
                            ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                    final MockDataTreeChangeListener listener = new MockDataTreeChangeListener(1);

                    ListenerRegistration<MockDataTreeChangeListener> listenerReg = dataStore
                            .registerTreeChangeListener(TestModel.TEST_PATH, listener);

                    assertNotNull("registerTreeChangeListener returned null", listenerReg);

                    IntegrationTestKit.verifyShardState(dataStore, "test-1",
                        state -> assertEquals("getTreeChangeListenerActors", 1,
                                state.getTreeChangeListenerActors().size()));

                    // Wait for the initial notification
                    listener.waitForChangeEvents(TestModel.TEST_PATH);
                    listener.reset(2);

                    // Write 2 updates.
                    testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                            ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

                    YangInstanceIdentifier listPath = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                            .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build();
                    testWriteTransaction(dataStore, listPath,
                            ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1));

                    // Wait for the 2 updates.
                    listener.waitForChangeEvents(TestModel.OUTER_LIST_PATH, listPath);
                    listenerReg.close();

                    IntegrationTestKit.verifyShardState(dataStore, "test-1",
                        state -> assertEquals("getTreeChangeListenerActors", 0,
                                state.getTreeChangeListenerActors().size()));

                    testWriteTransaction(dataStore,
                            YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2).build(),
                            ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2));

                    listener.expectNoMoreChanges("Received unexpected change after close");
                }
            }
        };
    }

    @Test
    public void testRestoreFromDatastoreSnapshot() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final String name = "transactionIntegrationTest";

                final ContainerNode carsNode = CarsModel.newCarsNode(
                        CarsModel.newCarsMapNode(CarsModel.newCarEntry("optima", BigInteger.valueOf(20000L)),
                                CarsModel.newCarEntry("sportage", BigInteger.valueOf(30000L))));

                DataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
                dataTree.setSchemaContext(SchemaContextHelper.full());
                AbstractShardTest.writeToStore(dataTree, CarsModel.BASE_PATH, carsNode);
                NormalizedNode<?, ?> root = AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.EMPTY);

                final Snapshot carsSnapshot = Snapshot.create(
                        new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
                        Collections.emptyList(), 2, 1, 2, 1, 1, "member-1", null);

                dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
                dataTree.setSchemaContext(SchemaContextHelper.full());

                final NormalizedNode<?, ?> peopleNode = PeopleModel.create();
                AbstractShardTest.writeToStore(dataTree, PeopleModel.BASE_PATH, peopleNode);

                root = AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.EMPTY);

                final Snapshot peopleSnapshot = Snapshot.create(
                        new ShardSnapshotState(new MetadataShardDataTreeSnapshot(root)),
                        Collections.emptyList(), 2, 1, 2, 1, 1, "member-1", null);

                restoreFromSnapshot = new DatastoreSnapshot(name, null, Arrays.asList(
                        new DatastoreSnapshot.ShardSnapshot("cars", carsSnapshot),
                        new DatastoreSnapshot.ShardSnapshot("people", peopleSnapshot)));

                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, name, "module-shards-member1.conf", true, "cars", "people")) {

                    final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

                    // two reads
                    Optional<NormalizedNode<?, ?>> optional = readTx.read(CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", carsNode, optional.get());

                    optional = readTx.read(PeopleModel.BASE_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", peopleNode, optional.get());
                }
            }
        };
    }

    @Test
    @Deprecated
    public void testRecoveryFromPreCarbonSnapshot() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {
            {
                final String name = "testRecoveryFromPreCarbonSnapshot";

                final ContainerNode carsNode = CarsModel.newCarsNode(
                        CarsModel.newCarsMapNode(CarsModel.newCarEntry("optima", BigInteger.valueOf(20000L)),
                                CarsModel.newCarEntry("sportage", BigInteger.valueOf(30000L))));

                DataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
                dataTree.setSchemaContext(SchemaContextHelper.full());
                AbstractShardTest.writeToStore(dataTree, CarsModel.BASE_PATH, carsNode);
                NormalizedNode<?, ?> root = AbstractShardTest.readStore(dataTree, YangInstanceIdentifier.EMPTY);

                MetadataShardDataTreeSnapshot shardSnapshot = new MetadataShardDataTreeSnapshot(root);
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (DataOutputStream dos = new DataOutputStream(bos)) {
                    PayloadVersion.BORON.writeTo(dos);
                    try (ObjectOutputStream oos = new ObjectOutputStream(dos)) {
                        oos.writeObject(shardSnapshot);
                    }
                }

                final org.opendaylight.controller.cluster.raft.Snapshot snapshot =
                        org.opendaylight.controller.cluster.raft.Snapshot.create(bos.toByteArray(),
                                Collections.emptyList(), 2, 1, 2, 1, 1, "member-1", null);

                InMemorySnapshotStore.addSnapshot("member-1-shard-cars-" + name, snapshot);

                try (AbstractDataStore dataStore = setupAbstractDataStore(
                        testParameter, name, "module-shards-member1.conf", true, "cars")) {

                    DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

                    Optional<NormalizedNode<?, ?>> optional = readTx.read(CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS);
                    assertEquals("isPresent", true, optional.isPresent());
                    assertEquals("Data node", carsNode, optional.get());
                }
            }
        };
    }
}
