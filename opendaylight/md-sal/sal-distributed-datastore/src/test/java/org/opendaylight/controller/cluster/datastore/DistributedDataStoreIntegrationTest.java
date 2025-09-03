/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opendaylight.controller.md.cluster.datastore.model.CarsModel.CAR_QNAME;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.AddressFromURIString;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.databroker.TestClientBackedDataStore;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

public class DistributedDataStoreIntegrationTest extends AbstractDistributedDataStoreIntegrationTest {
    @Before
    public void setUp() {
        system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Address member1Address = AddressFromURIString.parse("pekko://cluster-test@127.0.0.1:2558");
        Cluster.get(system).join(member1Address);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system, true);
        system = null;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void testTransactionWritesWithShardNotInitiallyReady(final String testName, final boolean writeOnly)
            throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final String shardName = "test-1";

        // Setup the InMemoryJournal to block shard recovery to ensure the shard is not initialized until we create and
        // submit the write the Tx.
        final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
        // InMemoryJournal.addBlockReadMessagesLatch(String.format("member-1-shard-%s-%s", shardName, testName),
        //    blockRecoveryLatch);

        try (var dataStore = testKit.setupDataStore(TestClientBackedDataStore.class, testName, false, shardName)) {
            // Create the write Tx
            final DOMStoreWriteTransaction writeTx = writeOnly ? dataStore.newWriteOnlyTransaction()
                    : dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", writeTx);

            // Do some modification operations and ready the Tx on a
            // separate thread.
            final var listEntryPath = TestModel.outerEntryPath(1);

            final AtomicReference<DOMStoreThreePhaseCommitCohort> txCohort = new AtomicReference<>();
            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch txReady = new CountDownLatch(1);
            final Thread txThread = new Thread(() -> {
                try {
                    writeTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);

                    writeTx.merge(TestModel.OUTER_LIST_PATH, TestModel.outerNode(42));

                    writeTx.write(listEntryPath, TestModel.outerEntry(1));

                    writeTx.delete(listEntryPath);

                    txCohort.set(writeTx.ready());
                } catch (Exception e) {
                    caughtEx.set(e);
                } finally {
                    txReady.countDown();
                }
            });

            txThread.start();

            // Wait for the Tx operations to complete.
            final boolean done = Uninterruptibles.awaitUninterruptibly(txReady, 5, TimeUnit.SECONDS);
            if (caughtEx.get() != null) {
                throw caughtEx.get();
            }

            assertTrue("Tx ready", done);

            // At this point the Tx operations should be waiting for the
            // shard to initialize so
            // trigger the latch to let the shard recovery to continue.
            blockRecoveryLatch.countDown();

            // Wait for the Tx commit to complete.
            testKit.doCommit(txCohort.get());

            // Verify the data in the store
            final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode> optional = readTx.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);
            assertTrue("isPresent", optional.isPresent());

            optional = readTx.read(TestModel.OUTER_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertTrue("isPresent", optional.isPresent());

            optional = readTx.read(listEntryPath).get(5, TimeUnit.SECONDS);
            assertFalse("isPresent", optional.isPresent());
        }
    }

    @Test
    @Ignore("FIXME: CONTROLLER-2073: figure out blocking based on ShardManager")
    public void testWriteOnlyTransactionWithShardNotInitiallyReady() throws Exception {
        datastoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);
        testTransactionWritesWithShardNotInitiallyReady("testWriteOnlyTransactionWithShardNotInitiallyReady", true);
    }

    @Test
    @Ignore("FIXME: CONTROLLER-2073: figure out blocking based on ShardManager")
    public void testReadWriteTransactionWithShardNotInitiallyReady() throws Exception {
        testTransactionWritesWithShardNotInitiallyReady("testReadWriteTransactionWithShardNotInitiallyReady", false);
    }

    @Test
    @Ignore("FIXME: CONTROLLER-2073: figure out blocking based on ShardManager")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionReadsWithShardNotInitiallyReady() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final String testName = "testTransactionReadsWithShardNotInitiallyReady";
        final String shardName = "test-1";

        // Setup the InMemoryJournal to block shard recovery to ensure the shard is not initialized until we create the
        // Tx.
        final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
        // InMemoryJournal.addBlockReadMessagesLatch(String.format("member-1-shard-%s-%s", shardName, testName),
        //     blockRecoveryLatch);

        try (var dataStore = testKit.setupDataStore(TestClientBackedDataStore.class, testName, false, shardName)) {
            // Create the read-write Tx
            final DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", readWriteTx);

            // Do some reads on the Tx on a separate thread.
            final AtomicReference<FluentFuture<Boolean>> txExistsFuture = new AtomicReference<>();
            final AtomicReference<FluentFuture<Optional<NormalizedNode>>> txReadFuture = new AtomicReference<>();
            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch txReadsDone = new CountDownLatch(1);
            final Thread txThread = new Thread(() -> {
                try {
                    readWriteTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);

                    txExistsFuture.set(readWriteTx.exists(TestModel.TEST_PATH));

                    txReadFuture.set(readWriteTx.read(TestModel.TEST_PATH));
                } catch (Exception e) {
                    caughtEx.set(e);
                } finally {
                    txReadsDone.countDown();
                }
            });

            txThread.start();

            // Wait for the Tx operations to complete.
            boolean done = Uninterruptibles.awaitUninterruptibly(txReadsDone, 5, TimeUnit.SECONDS);
            if (caughtEx.get() != null) {
                throw caughtEx.get();
            }

            assertTrue("Tx reads done", done);

            // At this point the Tx operations should be waiting for the
            // shard to initialize so
            // trigger the latch to let the shard recovery to continue.
            blockRecoveryLatch.countDown();

            // Wait for the reads to complete and verify.
            assertEquals("exists", Boolean.TRUE, txExistsFuture.get().get(5, TimeUnit.SECONDS));
            assertTrue("read", txReadFuture.get().get(5, TimeUnit.SECONDS).isPresent());

            readWriteTx.close();
        }
    }

    @Test
    @Ignore("FIXME: CONTROLLER-2073: figure out blocking based on ShardManager")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionCommitFailureWithShardNotInitialized() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final String testName = "testTransactionCommitFailureWithShardNotInitialized";
        final String shardName = "test-1";

        // Set the shard initialization timeout low for the test.
        datastoreContextBuilder.shardInitializationTimeout(300, TimeUnit.MILLISECONDS);

        // Setup the InMemoryJournal to block shard recovery indefinitely.
        final var blockRecoveryLatch = new CountDownLatch(1);
        // InMemoryJournal.addBlockReadMessagesLatch(String.format("member-1-shard-%s-%s", shardName, testName),
        //    blockRecoveryLatch);

        final var dataStore = testKit.setupDataStore(TestClientBackedDataStore.class, testName, false, shardName);

        // Create the write Tx
        final DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
        assertNotNull("newReadWriteTransaction returned null", writeTx);

        // Do some modifications and ready the Tx on a separate
        // thread.
        final var txCohort = new AtomicReference<DOMStoreThreePhaseCommitCohort>();
        final var caughtEx = new AtomicReference<Exception>();
        final var txReady = new CountDownLatch(1);
        final var txThread = new Thread(() -> {
            try {
                writeTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);

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
        assertNull(caughtEx.get());

        assertTrue("Tx ready", done);

        // Wait for the commit to complete. Since the shard never
        // initialized, the Tx should
        // have timed out and throw an appropriate exception cause.

        final ExecutionException ee;
        try {
            ee = assertThrows(ExecutionException.class, () -> txCohort.get().canCommit().get(5, TimeUnit.SECONDS));
        } finally {
            blockRecoveryLatch.countDown();
        }

        final var nie = assertInstanceOf(NotInitializedException.class, ee.getCause());
        assertEquals("""
            Found primary shard member-1-shard-test-1-testTransactionCommitFailureWithShardNotInitialized but it's \
            not initialized yet. Please try again later""", nie.getMessage());
    }

    @Test
    @Ignore("FIXME: CONTROLLER-2073: figure out blocking based on ShardManager")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionReadFailureWithShardNotInitialized() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final String testName = "testTransactionReadFailureWithShardNotInitialized";
        final String shardName = "test-1";

        // Set the shard initialization timeout low for the test.
        datastoreContextBuilder.shardInitializationTimeout(300, TimeUnit.MILLISECONDS);

        // Setup the InMemoryJournal to block shard recovery
        // indefinitely.
        final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
        // InMemoryJournal.addBlockReadMessagesLatch(String.format("member-1-shard-%s-%s", shardName, testName),
        //    blockRecoveryLatch);

        try (var dataStore = testKit.setupDataStore(TestClientBackedDataStore.class, testName, false, shardName)) {
            // Create the read-write Tx
            final DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", readWriteTx);

            // Do a read on the Tx on a separate thread.
            final var txReadFuture = new AtomicReference<FluentFuture<Optional<NormalizedNode>>>();
            final var caughtEx = new AtomicReference<Exception>();
            final var txReadDone = new CountDownLatch(1);
            final var txThread = new Thread(() -> {
                try {
                    readWriteTx.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);

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

            assertTrue("Tx read done", done);

            // Wait for the read to complete. Since the shard never
            // initialized, the Tx should
            // have timed out and throw an appropriate exception cause.

            final ExecutionException ee;
            try {
                ee = assertThrows(ExecutionException.class, () -> txReadFuture.get().get(5, TimeUnit.SECONDS));
            } finally {
                blockRecoveryLatch.countDown();
            }

            final var rfe = assertInstanceOf(ReadFailedException.class, ee.getCause());
            assertEquals("""
                Error reading data for path /(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?\
                revision=2014-03-13)test""", rfe.getMessage());
            final var rre = assertInstanceOf(RuntimeRequestException.class, rfe.getCause());
            assertEquals("Failed to resolve shard 1", rre.getMessage());
            final var nie = assertInstanceOf(NotInitializedException.class, rre.getCause());
            assertEquals("""
                Found primary shard member-1-shard-test-1-testTransactionReadFailureWithShardNotInitialized but it's \
                not initialized yet. Please try again later""", nie.getMessage());
        }
    }

    @Test
    public void testManyWritesDeletes() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final var carMapBuilder = ImmutableNodes.newSystemMapBuilder()
            .withNodeIdentifier(new NodeIdentifier(CAR_QNAME));

        try (var dataStore = testKit.setupDataStore(TestClientBackedDataStore.class, "testManyWritesDeletes",
            "module-shards-cars-member-1.conf", true, "cars")) {

            try (var txChain = dataStore.createTransactionChain()) {
                final var writeTx = txChain.newWriteOnlyTransaction();
                writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
                writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
                testKit.doCommit(writeTx.ready());

                int numCars = 20;
                for (int i = 0; i < numCars; ++i) {
                    var rwTx = txChain.newReadWriteTransaction();

                    final var path = CarsModel.newCarPath("car" + i);
                    final var data = CarsModel.newCarEntry("car" + i, Uint64.valueOf(20000));

                    rwTx.merge(path, data);
                    carMapBuilder.withChild(data);

                    testKit.doCommit(rwTx.ready());

                    if (i % 5 == 0) {
                        rwTx = txChain.newReadWriteTransaction();

                        rwTx.delete(path);
                        carMapBuilder.withoutChild(path.getLastPathArgument());
                        testKit.doCommit(rwTx.ready());
                    }
                }

                try (var readTx = txChain.newReadOnlyTransaction()) {
                    assertEquals("cars not matching result", Optional.of(carMapBuilder.build()),
                        readTx.read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS));
                }
            }


            // wait until the journal is actually persisted, killing the datastore early results in missing entries
            Stopwatch sw = Stopwatch.createStarted();
            AtomicBoolean done = new AtomicBoolean(false);
            while (!done.get()) {
                MemberNode.verifyRaftState(dataStore, "cars", raftState -> {
                    if (raftState.getLastApplied() == raftState.getLastLogIndex()) {
                        done.set(true);
                    }
                });

                assertTrue("Shard did not persist all journal entries in time.", sw.elapsed(TimeUnit.SECONDS) <= 5);

                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        // test restoration from journal and verify data matches
        try (var dataStore = testKit.setupDataStore(TestClientBackedDataStore.class, "testManyWritesDeletes",
            "module-shards-cars-member-1.conf", true, "cars")) {

            try (var txChain = dataStore.createTransactionChain()) {
                try (var readTx = txChain.newReadOnlyTransaction()) {
                    assertEquals("restored cars do not match snapshot", Optional.of(carMapBuilder.build()),
                        readTx.read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS));
                }
            }
        }
    }

}
