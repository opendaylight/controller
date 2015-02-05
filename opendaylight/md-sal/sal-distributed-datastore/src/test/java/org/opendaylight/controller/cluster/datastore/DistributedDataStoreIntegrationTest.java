package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockDataChangeListener;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DistributedDataStoreIntegrationTest extends AbstractActorTest {

    private final DatastoreContext.Builder datastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100);

    @Test
    public void testWriteTransactionWithSingleShard() throws Exception{
        new IntegrationTestKit(getSystem()) {{
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
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("testWriteTransactionWithMultipleShards", "cars-1", "people-1");

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            YangInstanceIdentifier nodePath1 = CarsModel.BASE_PATH;
            NormalizedNode<?, ?> nodeToWrite1 = CarsModel.emptyContainer();
            writeTx.write(nodePath1, nodeToWrite1);

            YangInstanceIdentifier nodePath2 = PeopleModel.BASE_PATH;
            NormalizedNode<?, ?> nodeToWrite2 = PeopleModel.emptyContainer();
            writeTx.write(nodePath2, nodeToWrite2);

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            doCommit(cohort);

            // Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(nodePath1).get();
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite1, optional.get());

            optional = readTx.read(nodePath2).get();
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite2, optional.get());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testReadWriteTransaction() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("testReadWriteTransaction", "test-1");

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
    public void testTransactionWritesWithShardNotInitiallyReady() throws Exception{
        new IntegrationTestKit(getSystem()) {{
            String testName = "testTransactionWritesWithShardNotInitiallyReady";
            String shardName = "test-1";

            // Setup the InMemoryJournal to block shard recovery to ensure the shard isn't
            // initialized until we create and submit the write the Tx.
            String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
            CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
            InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

            DistributedDataStore dataStore = setupDistributedDataStore(testName, false, shardName);

            // Create the write Tx

            final DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
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
    public void testTransactionReadsWithShardNotInitiallyReady() throws Exception{
        new IntegrationTestKit(getSystem()) {{
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
        new IntegrationTestKit(getSystem()) {{
            String testName = "testTransactionCommitFailureWithShardNotInitialized";
            String shardName = "test-1";

            // Set the shard initialization timeout low for the test.

            datastoreContextBuilder.shardInitializationTimeout(300, TimeUnit.MILLISECONDS);

            // Setup the InMemoryJournal to block shard recovery indefinitely.

            String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
            CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
            InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

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
        new IntegrationTestKit(getSystem()) {{
            String testName = "testTransactionReadFailureWithShardNotInitialized";
            String shardName = "test-1";

            // Set the shard initialization timeout low for the test.

            datastoreContextBuilder.shardInitializationTimeout(300, TimeUnit.MILLISECONDS);

            // Setup the InMemoryJournal to block shard recovery indefinitely.

            String persistentID = String.format("member-1-shard-%s-%s", shardName, testName);
            CountDownLatch blockRecoveryLatch = new CountDownLatch(1);
            InMemoryJournal.addBlockReadMessagesLatch(persistentID, blockRecoveryLatch);

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

    @Test(expected=NoShardLeaderException.class)
    public void testTransactionCommitFailureWithNoShardLeader() throws Throwable{
        new IntegrationTestKit(getSystem()) {{
            String testName = "testTransactionCommitFailureWithNoShardLeader";
            String shardName = "test-1";

            // We don't want the shard to become the leader so prevent shard election from completing
            // by setting the election timeout, which is based on the heartbeat interval, really high.

            datastoreContextBuilder.shardHeartbeatIntervalInMillis(30000);

            // Set the leader election timeout low for the test.

            datastoreContextBuilder.shardLeaderElectionTimeout(1, TimeUnit.MILLISECONDS);

            DistributedDataStore dataStore = setupDistributedDataStore(testName, false, shardName);

            // Create the write Tx.

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

    @Test
    public void testTransactionAbort() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
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
    public void testTransactionChain() throws Exception{
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore = setupDistributedDataStore("testTransactionChain", "test-1");

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
    public void testCreateChainedTransactionsInQuickSuccession() throws Exception{
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testCreateChainedTransactionsInQuickSuccession", "test-1");

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

            int nTxs = 20;
            List<DOMStoreThreePhaseCommitCohort> cohorts = new ArrayList<>(nTxs);
            for(int i = 0; i < nTxs; i++) {
                DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

                rwTx.merge(TestModel.TEST_PATH, testNode);

                cohorts.add(rwTx.ready());

            }

            for(DOMStoreThreePhaseCommitCohort cohort: cohorts) {
                doCommit(cohort);
            }

            txChain.close();

            cleanup(dataStore);
        }};
    }

    @Test
    public void testCreateChainedTransactionAfterEmptyTxReadied() throws Exception{
        new IntegrationTestKit(getSystem()) {{
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
        new IntegrationTestKit(getSystem()) {{
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
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore = setupDistributedDataStore(
                    "testCreateChainedTransactionAfterClose", "test-1");

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            txChain.close();

            // Try to create another Tx of each type - should fail b/c the previous Tx was closed.

            assertExceptionOnTxChainCreates(txChain, TransactionChainClosedException.class);
        }};
    }

    @Test
    public void testChangeListenerRegistration() throws Exception{
        new IntegrationTestKit(getSystem()) {{
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

    class IntegrationTestKit extends ShardTestKit {

        IntegrationTestKit(ActorSystem actorSystem) {
            super(actorSystem);
        }

        DistributedDataStore setupDistributedDataStore(String typeName, String... shardNames) {
            return setupDistributedDataStore(typeName, true, shardNames);
        }

        DistributedDataStore setupDistributedDataStore(String typeName, boolean waitUntilLeader,
                String... shardNames) {
            MockClusterWrapper cluster = new MockClusterWrapper();
            Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
            ShardStrategyFactory.setConfiguration(config);

            datastoreContextBuilder.dataStoreType(typeName);

            DatastoreContext datastoreContext = datastoreContextBuilder.build();

            DistributedDataStore dataStore = new DistributedDataStore(getSystem(), cluster,
                    config, datastoreContext);

            SchemaContext schemaContext = SchemaContextHelper.full();
            dataStore.onGlobalContextUpdated(schemaContext);

            if(waitUntilLeader) {
                for(String shardName: shardNames) {
                    ActorRef shard = null;
                    for(int i = 0; i < 20 * 5 && shard == null; i++) {
                        Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
                        Optional<ActorRef> shardReply = dataStore.getActorContext().findLocalShard(shardName);
                        if(shardReply.isPresent()) {
                            shard = shardReply.get();
                        }
                    }

                    assertNotNull("Shard was not created", shard);

                    waitUntilLeader(shard);
                }
            }

            return dataStore;
        }

        void testWriteTransaction(DistributedDataStore dataStore, YangInstanceIdentifier nodePath,
                NormalizedNode<?, ?> nodeToWrite) throws Exception {

            // 1. Create a write-only Tx

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            // 2. Write some data

            writeTx.write(nodePath, nodeToWrite);

            // 3. Ready the Tx for commit

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            // 4. Commit the Tx

            doCommit(cohort);

            // 5. Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(nodePath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite, optional.get());
        }

        void doCommit(final DOMStoreThreePhaseCommitCohort cohort) throws Exception {
            Boolean canCommit = cohort.canCommit().get(5, TimeUnit.SECONDS);
            assertEquals("canCommit", true, canCommit);
            cohort.preCommit().get(5, TimeUnit.SECONDS);
            cohort.commit().get(5, TimeUnit.SECONDS);
        }

        void cleanup(DistributedDataStore dataStore) {
            dataStore.getActorContext().getShardManager().tell(PoisonPill.getInstance(), null);
        }

        void assertExceptionOnCall(Callable<Void> callable, Class<? extends Exception> expType)
                throws Exception {
            try {
                callable.call();
                fail("Expected " + expType.getSimpleName());
            } catch(Exception e) {
                assertEquals("Exception type", expType, e.getClass());
            }
        }

        void assertExceptionOnTxChainCreates(final DOMStoreTransactionChain txChain,
                Class<? extends Exception> expType) throws Exception {
            assertExceptionOnCall(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    txChain.newWriteOnlyTransaction();
                    return null;
                }
            }, expType);

            assertExceptionOnCall(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    txChain.newReadWriteTransaction();
                    return null;
                }
            }, expType);

            assertExceptionOnCall(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    txChain.newReadOnlyTransaction();
                    return null;
                }
            }, expType);
        }
    }

}
