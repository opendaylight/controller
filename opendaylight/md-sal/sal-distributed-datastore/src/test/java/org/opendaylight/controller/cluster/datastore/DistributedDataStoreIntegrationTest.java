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
import static org.junit.Assert.fail;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.AddressFromURIString;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opendaylight.controller.cluster.databroker.TestClientBackedDataStore;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@RunWith(Parameterized.class)
public class DistributedDataStoreIntegrationTest extends AbstractDistributedDataStoreIntegrationTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { TestClientBackedDataStore.class }
        });
    }

    @Before
    public void setUp() {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
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

        // Setup the InMemoryJournal to block shard recovery to ensure
        // the shard isn't
        // initialized until we create and submit the write the Tx.
        final String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
        final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
        InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

        try (var dataStore = testKit.setupDataStore(testParameter, testName, false, shardName)) {
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
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final String testName = "testTransactionReadsWithShardNotInitiallyReady";
        final String shardName = "test-1";

        // Setup the InMemoryJournal to block shard recovery to ensure
        // the shard isn't
        // initialized until we create the Tx.
        final String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
        final CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
        InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

        try (var dataStore = testKit.setupDataStore(testParameter, testName, false, shardName)) {
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

    @Test(expected = NotInitializedException.class)
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionCommitFailureWithShardNotInitialized() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
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

        final var dataStore = testKit.setupDataStore(testParameter, testName, false, shardName);

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
        if (caughtEx.get() != null) {
            throw caughtEx.get();
        }

        assertTrue("Tx ready", done);

        // Wait for the commit to complete. Since the shard never
        // initialized, the Tx should
        // have timed out and throw an appropriate exception cause.
        try {
            txCohort.get().canCommit().get(5, TimeUnit.SECONDS);
            fail("Expected NotInitializedException");
        } catch (final Exception e) {
            final Throwable root = Throwables.getRootCause(e);
            Throwables.throwIfUnchecked(root);
            throw new RuntimeException(root);
        } finally {
            blockRecoveryLatch.countDown();
        }
    }

    @Test(expected = NotInitializedException.class)
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testTransactionReadFailureWithShardNotInitialized() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
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

        try (var dataStore = testKit.setupDataStore(testParameter, testName, false, shardName)) {
            // Create the read-write Tx
            final DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", readWriteTx);

            // Do a read on the Tx on a separate thread.
            final AtomicReference<FluentFuture<Optional<NormalizedNode>>> txReadFuture = new AtomicReference<>();
            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch txReadDone = new CountDownLatch(1);
            final Thread txThread = new Thread(() -> {
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
            try {
                txReadFuture.get().get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                assertTrue("Expected ReadFailedException cause: " + e.getCause(),
                    e.getCause() instanceof ReadFailedException);
                final Throwable root = Throwables.getRootCause(e);
                Throwables.throwIfUnchecked(root);
                throw new RuntimeException(root);
            } finally {
                blockRecoveryLatch.countDown();
            }
        }
    }

}
