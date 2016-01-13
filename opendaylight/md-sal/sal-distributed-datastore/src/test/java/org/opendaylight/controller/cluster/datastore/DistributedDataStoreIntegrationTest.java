/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.utils.MockDataChangeListener;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

public class DistributedDataStoreIntegrationTest {

    private static ActorSystem system;

    private final DatastoreContext.Builder datastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100);

    @BeforeClass
    public static void setUpClass() throws IOException {
        system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Address member1Address = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
        Cluster.get(system).join(member1Address);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    protected ActorSystem getSystem() {
        return system;
    }

    @Test
    public void testWriteTransactionWithSingleShard() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("transactionIntegrationTest", "test-1");

            testWriteTransaction(dataStore, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testWriteTransactionWithMultipleShards() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("testWriteTransactionWithMultipleShards", "cars-1", "people-1");

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

            MapEntryNode car = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
            YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
            writeTx.write(carPath, car);

            MapEntryNode person = PeopleModel.newPersonEntry("jack");
            YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
            writeTx.write(personPath, person);

            doCommit(writeTx.ready());

            // Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(carPath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", car, optional.get());

            optional = readTx.read(personPath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", person, optional.get());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testReadWriteTransactionWithSingleShard() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("testReadWriteTransactionWithSingleShard", "test-1");

            // 1. Create a read-write Tx

            DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", readWriteTx);

            // 2. Write some data

            YangInstanceIdentifier nodePath = TestModel.TEST_PATH;
            NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            readWriteTx.write(nodePath, nodeToWrite );

            // 3. Read the data from Tx

            Boolean exists = readWriteTx.exists(nodePath).checkedGet(5, TimeUnit.SECONDS);
            assertEquals("exists", true, exists);

            Optional<NormalizedNode<?, ?>> optional = readWriteTx.read(nodePath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite, optional.get());

            // 4. Ready the Tx for commit

            DOMStoreThreePhaseCommitCohort cohort = readWriteTx.ready();

            // 5. Commit the Tx

            doCommit(cohort);

            // 6. Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            optional = readTx.read(nodePath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite, optional.get());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testReadWriteTransactionWithMultipleShards() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("testReadWriteTransactionWithMultipleShards", "cars-1", "people-1");

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

            MapEntryNode car = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
            YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
            readWriteTx.write(carPath, car);

            MapEntryNode person = PeopleModel.newPersonEntry("jack");
            YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
            readWriteTx.write(personPath, person);

            Boolean exists = readWriteTx.exists(carPath).checkedGet(5, TimeUnit.SECONDS);
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

            cleanup(dataStore);
        }};
    }

    @Test
    public void testSingleTransactionsWritesInQuickSuccession() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testSingleTransactionsWritesInQuickSuccession", "cars-1");

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            doCommit(writeTx.ready());

            writeTx = txChain.newWriteOnlyTransaction();

            int nCars = 5;
            for(int i = 0; i < nCars; i++) {
                writeTx.write(CarsModel.newCarPath("car" + i),
                        CarsModel.newCarEntry("car" + i, BigInteger.valueOf(20000)));
            }

            doCommit(writeTx.ready());

            Optional<NormalizedNode<?, ?>> optional = txChain.newReadOnlyTransaction().read(
                    CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("# cars", nCars, ((Collection<?>)optional.get().getValue()).size());

            cleanup(dataStore);
        }};
    }

    private void testTransactionWritesWithShardNotInitiallyReady(final String testName,
            final boolean writeOnly) throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            String shardName = "test-1";

            // Setup the InMemoryJournal to block shard recovery to ensure the shard isn't
            // initialized until we create and submit the write the Tx.
            String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
            CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
            InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

            DistributedDataStore dataStore = setupDistributedDataStore(testName, false, shardName);

            // Create the write Tx

            final DOMStoreWriteTransaction writeTx = writeOnly ? dataStore.newWriteOnlyTransaction() :
                    dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", writeTx);

            // Do some modification operations and ready the Tx on a separate thread.

            final YangInstanceIdentifier listEntryPath = YangInstanceIdentifier.builder(
                    TestModel.OUTER_LIST_PATH).nodeWithKey(TestModel.OUTER_LIST_QNAME,
                            TestModel.ID_QNAME, 1).build();

            final AtomicReference<DOMStoreThreePhaseCommitCohort> txCohort = new AtomicReference<>();
            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch txReady = new CountDownLatch(1);
            Thread txThread = new Thread() {
                @Override
                public void run() {
                    try {
                        writeTx.write(TestModel.TEST_PATH,
                                ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                        writeTx.merge(TestModel.OUTER_LIST_PATH, ImmutableNodes.mapNodeBuilder(
                                TestModel.OUTER_LIST_QNAME).build());

                        writeTx.write(listEntryPath, ImmutableNodes.mapEntry(
                                TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1));

                        writeTx.delete(listEntryPath);

                        txCohort.set(writeTx.ready());
                    } catch(Exception e) {
                        caughtEx.set(e);
                        return;
                    } finally {
                        txReady.countDown();
                    }
                }
            };

            txThread.start();

            // Wait for the Tx operations to complete.

            boolean done = Uninterruptibles.awaitUninterruptibly(txReady, 5, TimeUnit.SECONDS);
            if(caughtEx.get() != null) {
                throw caughtEx.get();
            }

            assertEquals("Tx ready", true, done);

            // At this point the Tx operations should be waiting for the shard to initialize so
            // trigger the latch to let the shard recovery to continue.

            blockRecoveryLatch.countDown();

            // Wait for the Tx commit to complete.

            doCommit(txCohort.get());

            // Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(TestModel.TEST_PATH).
                    get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());

            optional = readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());

            optional = readTx.read(listEntryPath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", false, optional.isPresent());

            cleanup(dataStore);
        }};
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
    public void testTransactionReadsWithShardNotInitiallyReady() throws Exception {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            String testName = "testTransactionReadsWithShardNotInitiallyReady";
            String shardName = "test-1";

            // Setup the InMemoryJournal to block shard recovery to ensure the shard isn't
            // initialized until we create the Tx.
            String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
            CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
            InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

            DistributedDataStore dataStore = setupDistributedDataStore(testName, false, shardName);

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
            Thread txThread = new Thread() {
                @Override
                public void run() {
                    try {
                        readWriteTx.write(TestModel.TEST_PATH,
                                ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                        txExistsFuture.set(readWriteTx.exists(TestModel.TEST_PATH));

                        txReadFuture.set(readWriteTx.read(TestModel.TEST_PATH));
                    } catch(Exception e) {
                        caughtEx.set(e);
                        return;
                    } finally {
                        txReadsDone.countDown();
                    }
                }
            };

            txThread.start();

            // Wait for the Tx operations to complete.

            boolean done = Uninterruptibles.awaitUninterruptibly(txReadsDone, 5, TimeUnit.SECONDS);
            if(caughtEx.get() != null) {
                throw caughtEx.get();
            }

            assertEquals("Tx reads done", true, done);

            // At this point the Tx operations should be waiting for the shard to initialize so
            // trigger the latch to let the shard recovery to continue.

            blockRecoveryLatch.countDown();

            // Wait for the reads to complete and verify.

            assertEquals("exists", true, txExistsFuture.get().checkedGet(5, TimeUnit.SECONDS));
            assertEquals("read", true, txReadFuture.get().checkedGet(5, TimeUnit.SECONDS).isPresent());

            readWriteTx.close();

            cleanup(dataStore);
        }};
    }

    @Test(expected=NotInitializedException.class)
    public void testTransactionCommitFailureWithShardNotInitialized() throws Throwable{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            String testName = "testTransactionCommitFailureWithShardNotInitialized";
            String shardName = "test-1";

            // Set the shard initialization timeout low for the test.

            datastoreContextBuilder.shardInitializationTimeout(300, TimeUnit.MILLISECONDS);

            // Setup the InMemoryJournal to block shard recovery indefinitely.

            String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
            CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
            InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

            InMemoryJournal.addEntry(persistentID, 1, "Dummy data so akka will read from persistence");

            DistributedDataStore dataStore = setupDistributedDataStore(testName, false, shardName);

            // Create the write Tx

            final DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newReadWriteTransaction returned null", writeTx);

            // Do some modifications and ready the Tx on a separate thread.

            final AtomicReference<DOMStoreThreePhaseCommitCohort> txCohort = new AtomicReference<>();
            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch txReady = new CountDownLatch(1);
            Thread txThread = new Thread() {
                @Override
                public void run() {
                    try {
                        writeTx.write(TestModel.TEST_PATH,
                                ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                        txCohort.set(writeTx.ready());
                    } catch(Exception e) {
                        caughtEx.set(e);
                        return;
                    } finally {
                        txReady.countDown();
                    }
                }
            };

            txThread.start();

            // Wait for the Tx operations to complete.

            boolean done = Uninterruptibles.awaitUninterruptibly(txReady, 5, TimeUnit.SECONDS);
            if(caughtEx.get() != null) {
                throw caughtEx.get();
            }

            assertEquals("Tx ready", true, done);

            // Wait for the commit to complete. Since the shard never initialized, the Tx should
            // have timed out and throw an appropriate exception cause.

            try {
                txCohort.get().canCommit().get(5, TimeUnit.SECONDS);
            } catch(ExecutionException e) {
                throw e.getCause();
            } finally {
                blockRecoveryLatch.countDown();
                cleanup(dataStore);
            }
        }};
    }

    @Test(expected=NotInitializedException.class)
    public void testTransactionReadFailureWithShardNotInitialized() throws Throwable{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            String testName = "testTransactionReadFailureWithShardNotInitialized";
            String shardName = "test-1";

            // Set the shard initialization timeout low for the test.

            datastoreContextBuilder.shardInitializationTimeout(300, TimeUnit.MILLISECONDS);

            // Setup the InMemoryJournal to block shard recovery indefinitely.

            String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
            CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
            InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

            InMemoryJournal.addEntry(persistentID, 1, "Dummy data so akka will read from persistence");

            DistributedDataStore dataStore = setupDistributedDataStore(testName, false, shardName);

            // Create the read-write Tx

            final DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", readWriteTx);

            // Do a read on the Tx on a separate thread.

            final AtomicReference<CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>>
                    txReadFuture = new AtomicReference<>();
            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch txReadDone = new CountDownLatch(1);
            Thread txThread = new Thread() {
                @Override
                public void run() {
                    try {
                        readWriteTx.write(TestModel.TEST_PATH,
                                ImmutableNodes.containerNode(TestModel.TEST_QNAME));

                        txReadFuture.set(readWriteTx.read(TestModel.TEST_PATH));

                        readWriteTx.close();
                    } catch(Exception e) {
                        caughtEx.set(e);
                        return;
                    } finally {
                        txReadDone.countDown();
                    }
                }
            };

            txThread.start();

            // Wait for the Tx operations to complete.

            boolean done = Uninterruptibles.awaitUninterruptibly(txReadDone, 5, TimeUnit.SECONDS);
            if(caughtEx.get() != null) {
                throw caughtEx.get();
            }

            assertEquals("Tx read done", true, done);

            // Wait for the read to complete. Since the shard never initialized, the Tx should
            // have timed out and throw an appropriate exception cause.

            try {
                txReadFuture.get().checkedGet(5, TimeUnit.SECONDS);
            } catch(ReadFailedException e) {
                throw e.getCause();
            } finally {
                blockRecoveryLatch.countDown();
                cleanup(dataStore);
            }
        }};
    }

    private void testTransactionCommitFailureWithNoShardLeader(final boolean writeOnly, final String testName) throws Throwable {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            String shardName = "default";

            // We don't want the shard to become the leader so prevent shard elections.
            datastoreContextBuilder.customRaftPolicyImplementation(
                    "org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy");

            // The ShardManager uses the election timeout for FindPrimary so reset it low so it will timeout quickly.
            datastoreContextBuilder.shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(1).
                    shardInitializationTimeout(200, TimeUnit.MILLISECONDS);

            DistributedDataStore dataStore = setupDistributedDataStore(testName, false, shardName);

            Object result = dataStore.getActorContext().executeOperation(dataStore.getActorContext().getShardManager(),
                    new FindLocalShard(shardName, true));
            assertTrue("Expected LocalShardFound. Actual: " + result, result instanceof LocalShardFound);

            // Create the write Tx.

            final DOMStoreWriteTransaction writeTx = writeOnly ? dataStore.newWriteOnlyTransaction() :
                dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", writeTx);

            // Do some modifications and ready the Tx on a separate thread.

            final AtomicReference<DOMStoreThreePhaseCommitCohort> txCohort = new AtomicReference<>();
            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch txReady = new CountDownLatch(1);
            Thread txThread = new Thread() {
                @Override
                public void run() {
                    try {
                        writeTx.write(TestModel.JUNK_PATH,
                                ImmutableNodes.containerNode(TestModel.JUNK_QNAME));

                        txCohort.set(writeTx.ready());
                    } catch(Exception e) {
                        caughtEx.set(e);
                        return;
                    } finally {
                        txReady.countDown();
                    }
                }
            };

            txThread.start();

            // Wait for the Tx operations to complete.

            boolean done = Uninterruptibles.awaitUninterruptibly(txReady, 5, TimeUnit.SECONDS);
            if(caughtEx.get() != null) {
                throw caughtEx.get();
            }

            assertEquals("Tx ready", true, done);

            // Wait for the commit to complete. Since no shard leader was elected in time, the Tx
            // should have timed out and throw an appropriate exception cause.

            try {
                txCohort.get().canCommit().get(5, TimeUnit.SECONDS);
            } catch(ExecutionException e) {
                throw e.getCause();
            } finally {
                cleanup(dataStore);
            }
        }};
    }

    @Test(expected=NoShardLeaderException.class)
    public void testWriteOnlyTransactionCommitFailureWithNoShardLeader() throws Throwable {
        datastoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);
        testTransactionCommitFailureWithNoShardLeader(true, "testWriteOnlyTransactionCommitFailureWithNoShardLeader");
    }

    @Test(expected=NoShardLeaderException.class)
    public void testReadWriteTransactionCommitFailureWithNoShardLeader() throws Throwable {
        testTransactionCommitFailureWithNoShardLeader(false, "testReadWriteTransactionCommitFailureWithNoShardLeader");
    }

    @Test
    public void testTransactionAbort() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("transactionAbortIntegrationTest", "test-1");

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            cohort.canCommit().get(5, TimeUnit.SECONDS);

            cohort.abort().get(5, TimeUnit.SECONDS);

            testWriteTransaction(dataStore, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            cleanup(dataStore);
        }};
    }

    @Test
    public void testTransactionChainWithSingleShard() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore("testTransactionChainWithSingleShard", "test-1");

            // 1. Create a Tx chain and write-only Tx

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            // 2. Write some data

            NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            writeTx.write(TestModel.TEST_PATH, testNode);

            // 3. Ready the Tx for commit

            final DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

            // 4. Commit the Tx on another thread that first waits for the second read Tx.

            final CountDownLatch continueCommit1 = new CountDownLatch(1);
            final CountDownLatch commit1Done = new CountDownLatch(1);
            final AtomicReference<Exception> commit1Error = new AtomicReference<>();
            new Thread() {
                @Override
                public void run() {
                    try {
                        continueCommit1.await();
                        doCommit(cohort1);
                    } catch (Exception e) {
                        commit1Error.set(e);
                    } finally {
                        commit1Done.countDown();
                    }
                }
            }.start();

            // 5. Create a new read Tx from the chain to read and verify the data from the first
            // Tx is visible after being readied.

            DOMStoreReadTransaction readTx = txChain.newReadOnlyTransaction();
            Optional<NormalizedNode<?, ?>> optional = readTx.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", testNode, optional.get());

            // 6. Create a new RW Tx from the chain, write more data, and ready it

            DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();
            MapNode outerNode = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build();
            rwTx.write(TestModel.OUTER_LIST_PATH, outerNode);

            DOMStoreThreePhaseCommitCohort cohort2 = rwTx.ready();

            // 7. Create a new read Tx from the chain to read the data from the last RW Tx to
            // verify it is visible.

            readTx = txChain.newReadWriteTransaction();
            optional = readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", outerNode, optional.get());

            // 8. Wait for the 2 commits to complete and close the chain.

            continueCommit1.countDown();
            Uninterruptibles.awaitUninterruptibly(commit1Done, 5, TimeUnit.SECONDS);

            if(commit1Error.get() != null) {
                throw commit1Error.get();
            }

            doCommit(cohort2);

            txChain.close();

            // 9. Create a new read Tx from the data store and verify committed data.

            readTx = dataStore.newReadOnlyTransaction();
            optional = readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", outerNode, optional.get());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testTransactionChainWithMultipleShards() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore("testTransactionChainWithMultipleShards",
                    "cars-1", "people-1");

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            writeTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

            DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

            DOMStoreReadWriteTransaction readWriteTx = txChain.newReadWriteTransaction();

            MapEntryNode car = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
            YangInstanceIdentifier carPath = CarsModel.newCarPath("optima");
            readWriteTx.write(carPath, car);

            MapEntryNode person = PeopleModel.newPersonEntry("jack");
            YangInstanceIdentifier personPath = PeopleModel.newPersonPath("jack");
            readWriteTx.merge(personPath, person);

            Optional<NormalizedNode<?, ?>> optional = readWriteTx.read(carPath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", car, optional.get());

            optional = readWriteTx.read(personPath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", person, optional.get());

            DOMStoreThreePhaseCommitCohort cohort2 = readWriteTx.ready();

            writeTx = txChain.newWriteOnlyTransaction();

            writeTx.delete(carPath);

            DOMStoreThreePhaseCommitCohort cohort3 = writeTx.ready();

            ListenableFuture<Boolean> canCommit1 = cohort1.canCommit();
            ListenableFuture<Boolean> canCommit2 = cohort2.canCommit();

            doCommit(canCommit1, cohort1);
            doCommit(canCommit2, cohort2);
            doCommit(cohort3);

            txChain.close();

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            optional = readTx.read(carPath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", false, optional.isPresent());

            optional = readTx.read(personPath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", person, optional.get());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testCreateChainedTransactionsInQuickSuccession() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testCreateChainedTransactionsInQuickSuccession", "cars-1");

            ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                    ImmutableMap.<LogicalDatastoreType, DOMStore>builder().put(
                            LogicalDatastoreType.CONFIGURATION, dataStore).build(), MoreExecutors.directExecutor());

            TransactionChainListener listener = Mockito.mock(TransactionChainListener.class);
            DOMTransactionChain txChain = broker.createTransactionChain(listener);

            List<CheckedFuture<Void, TransactionCommitFailedException>> futures = new ArrayList<>();

            DOMDataWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.put(LogicalDatastoreType.CONFIGURATION, CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            futures.add(writeTx.submit());

            int nCars = 100;
            for(int i = 0; i < nCars; i++) {
                DOMDataReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

                rwTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.newCarPath("car" + i),
                        CarsModel.newCarEntry("car" + i, BigInteger.valueOf(20000)));

                futures.add(rwTx.submit());
            }

            for(CheckedFuture<Void, TransactionCommitFailedException> f: futures) {
                f.checkedGet();
            }

            Optional<NormalizedNode<?, ?>> optional = txChain.newReadOnlyTransaction().read(
                    LogicalDatastoreType.CONFIGURATION, CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("# cars", nCars, ((Collection<?>)optional.get().getValue()).size());

            txChain.close();

            broker.close();

            cleanup(dataStore);
        }};
    }

    @Test
    public void testCreateChainedTransactionAfterEmptyTxReadied() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testCreateChainedTransactionAfterEmptyTxReadied", "test-1");

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreReadWriteTransaction rwTx1 = txChain.newReadWriteTransaction();

            rwTx1.ready();

            DOMStoreReadWriteTransaction rwTx2 = txChain.newReadWriteTransaction();

            Optional<NormalizedNode<?, ?>> optional = rwTx2.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", false, optional.isPresent());

            txChain.close();

            cleanup(dataStore);
        }};
    }

    @Test
    public void testCreateChainedTransactionWhenPreviousNotReady() throws Throwable {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testCreateChainedTransactionWhenPreviousNotReady", "test-1");

            final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            // Try to create another Tx of each type - each should fail b/c the previous Tx wasn't
            // readied.

            assertExceptionOnTxChainCreates(txChain, IllegalStateException.class);
        }};
    }

    @Test
    public void testCreateChainedTransactionAfterClose() throws Throwable {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testCreateChainedTransactionAfterClose", "test-1");

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            txChain.close();

            // Try to create another Tx of each type - should fail b/c the previous Tx was closed.

            assertExceptionOnTxChainCreates(txChain, TransactionChainClosedException.class);
        }};
    }

    @Test
    public void testChainWithReadOnlyTxAfterPreviousReady() throws Throwable {
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testChainWithReadOnlyTxAfterPreviousReady", "test-1");

            final DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            // Create a write tx and submit.

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
            DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

            // Create read-only tx's and issue a read.

            CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture1 =
                    txChain.newReadOnlyTransaction().read(TestModel.TEST_PATH);

            CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture2 =
                    txChain.newReadOnlyTransaction().read(TestModel.TEST_PATH);

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

            assertEquals("isPresent", true, txChain.newReadOnlyTransaction().read(TestModel.OUTER_LIST_PATH).
                    checkedGet(5, TimeUnit.SECONDS).isPresent());
        }};
    }

    @Test
    public void testChainedTransactionFailureWithSingleShard() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testChainedTransactionFailureWithSingleShard", "cars-1");

            ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                    ImmutableMap.<LogicalDatastoreType, DOMStore>builder().put(
                            LogicalDatastoreType.CONFIGURATION, dataStore).build(), MoreExecutors.directExecutor());

            TransactionChainListener listener = Mockito.mock(TransactionChainListener.class);
            DOMTransactionChain txChain = broker.createTransactionChain(listener);

            DOMDataReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

            ContainerNode invalidData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                    new YangInstanceIdentifier.NodeIdentifier(CarsModel.BASE_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk")).build();

            rwTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, invalidData);

            try {
                rwTx.submit().checkedGet(5, TimeUnit.SECONDS);
                fail("Expected TransactionCommitFailedException");
            } catch (TransactionCommitFailedException e) {
                // Expected
            }

            verify(listener, timeout(5000)).onTransactionChainFailed(eq(txChain), eq(rwTx), any(Throwable.class));

            txChain.close();
            broker.close();
            cleanup(dataStore);
        }};
    }

    @Test
    public void testChainedTransactionFailureWithMultipleShards() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testChainedTransactionFailureWithMultipleShards", "cars-1", "people-1");

            ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                    ImmutableMap.<LogicalDatastoreType, DOMStore>builder().put(
                            LogicalDatastoreType.CONFIGURATION, dataStore).build(), MoreExecutors.directExecutor());

            TransactionChainListener listener = Mockito.mock(TransactionChainListener.class);
            DOMTransactionChain txChain = broker.createTransactionChain(listener);

            DOMDataWriteTransaction writeTx = txChain.newWriteOnlyTransaction();

            writeTx.put(LogicalDatastoreType.CONFIGURATION, PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

            ContainerNode invalidData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                    new YangInstanceIdentifier.NodeIdentifier(CarsModel.BASE_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk")).build();

            // Note that merge will validate the data and fail but put succeeds b/c deep validation is not
            // done for put for performance reasons.
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, invalidData);

            try {
                writeTx.submit().checkedGet(5, TimeUnit.SECONDS);
                fail("Expected TransactionCommitFailedException");
            } catch (TransactionCommitFailedException e) {
                // Expected
            }

            verify(listener, timeout(5000)).onTransactionChainFailed(eq(txChain), eq(writeTx), any(Throwable.class));

            txChain.close();
            broker.close();
            cleanup(dataStore);
        }};
    }

    @Test
    public void testChangeListenerRegistration() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("testChangeListenerRegistration", "test-1");

            testWriteTransaction(dataStore, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            MockDataChangeListener listener = new MockDataChangeListener(1);

            ListenerRegistration<MockDataChangeListener>
                    listenerReg = dataStore.registerChangeListener(TestModel.TEST_PATH, listener,
                            DataChangeScope.SUBTREE);

            assertNotNull("registerChangeListener returned null", listenerReg);

            // Wait for the initial notification

            listener.waitForChangeEvents(TestModel.TEST_PATH);

            listener.reset(2);

            // Write 2 updates.

            testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

            YangInstanceIdentifier listPath = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH).
                    nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build();
            testWriteTransaction(dataStore, listPath,
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1));

            // Wait for the 2 updates.

            listener.waitForChangeEvents(TestModel.OUTER_LIST_PATH, listPath);

            listenerReg.close();

            testWriteTransaction(dataStore, YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH).
                    nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2).build(),
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2));

            listener.expectNoMoreChanges("Received unexpected change after close");

            cleanup(dataStore);
        }};
    }

    @Test
    public void testRestoreFromDatastoreSnapshot() throws Exception{
        new IntegrationTestKit(getSystem(), datastoreContextBuilder) {{
            String name = "transactionIntegrationTest";

            ContainerNode carsNode = CarsModel.newCarsNode(CarsModel.newCarsMapNode(
                    CarsModel.newCarEntry("optima", BigInteger.valueOf(20000L)),
                    CarsModel.newCarEntry("sportage", BigInteger.valueOf(30000L))));

            ShardDataTree dataTree = new ShardDataTree(SchemaContextHelper.full(), TreeType.OPERATIONAL);
            AbstractShardTest.writeToStore(dataTree, CarsModel.BASE_PATH, carsNode);
            NormalizedNode<?, ?> root = AbstractShardTest.readStore(dataTree.getDataTree(),
                    YangInstanceIdentifier.builder().build());

            Snapshot carsSnapshot = Snapshot.create(SerializationUtils.serializeNormalizedNode(root),
                    Collections.<ReplicatedLogEntry>emptyList(), 2, 1, 2, 1, 1, "member-1");

            NormalizedNode<?, ?> peopleNode = PeopleModel.create();
            dataTree = new ShardDataTree(SchemaContextHelper.full(), TreeType.OPERATIONAL);
            AbstractShardTest.writeToStore(dataTree, PeopleModel.BASE_PATH, peopleNode);
            root = AbstractShardTest.readStore(dataTree.getDataTree(), YangInstanceIdentifier.builder().build());

            Snapshot peopleSnapshot = Snapshot.create(SerializationUtils.serializeNormalizedNode(root),
                    Collections.<ReplicatedLogEntry>emptyList(), 2, 1, 2, 1, 1, "member-1");

            restoreFromSnapshot = new DatastoreSnapshot(name, null, Arrays.asList(
                    new DatastoreSnapshot.ShardSnapshot("cars",
                            org.apache.commons.lang3.SerializationUtils.serialize(carsSnapshot)),
                    new DatastoreSnapshot.ShardSnapshot("people",
                            org.apache.commons.lang3.SerializationUtils.serialize(peopleSnapshot))));

            DistributedDataStore dataStore = setupDistributedDataStore(name, "module-shards-member1.conf",
                    true, "cars", "people");

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", carsNode, optional.get());

            optional = readTx.read(PeopleModel.BASE_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", peopleNode, optional.get());

            cleanup(dataStore);
        }};
    }
}
