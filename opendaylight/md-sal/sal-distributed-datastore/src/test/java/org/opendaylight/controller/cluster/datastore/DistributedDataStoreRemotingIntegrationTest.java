/*
 * Copyright (c) 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapNodeBuilder;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.AddressFromURIString;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.Member;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.access.client.RequestTimeoutException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker;
import org.opendaylight.controller.cluster.databroker.TestClientBackedDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.TestShard.RequestFrontendMetadata;
import org.opendaylight.controller.cluster.datastore.TestShard.StartDropMessages;
import org.opendaylight.controller.cluster.datastore.TestShard.StopDropMessages;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendClientMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendShardDataTreeSnapshotMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.spi.UnsignedLongBitmap;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.collection.Set;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * End-to-end distributed data store tests that exercise remote shards and transactions.
 *
 * @author Thomas Pantelis
 */
@RunWith(Parameterized.class)
public class DistributedDataStoreRemotingIntegrationTest extends AbstractTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { TestClientBackedDataStore.class, 12 }
        });
    }

    @Parameter(0)
    public Class<? extends ClientBackedDataStore> testParameter;
    @Parameter(1)
    public int commitTimeout;

    private static final String[] CARS_AND_PEOPLE = {"cars", "people"};
    private static final String[] CARS = {"cars"};

    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse(
            "pekko://cluster-test@127.0.0.1:2558");
    private static final Address MEMBER_2_ADDRESS = AddressFromURIString.parse(
            "pekko://cluster-test@127.0.0.1:2559");

    private static final String MODULE_SHARDS_CARS_ONLY_1_2 = "module-shards-cars-member-1-and-2.conf";
    private static final String MODULE_SHARDS_CARS_PEOPLE_1_2 = "module-shards-member1-and-2.conf";
    private static final String MODULE_SHARDS_CARS_PEOPLE_1_2_3 = "module-shards-member1-and-2-and-3.conf";
    private static final String MODULE_SHARDS_CARS_1_2_3 = "module-shards-cars-member-1-and-2-and-3.conf";

    private ActorSystem leaderSystem;
    private ActorSystem followerSystem;
    private ActorSystem follower2System;

    private final DatastoreContext.Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5)
                .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());
    private final TransactionIdentifier tx1 = nextTransactionId();
    private final TransactionIdentifier tx2 = nextTransactionId();

    private ClientBackedDataStore followerDistributedDataStore;
    private ClientBackedDataStore leaderDistributedDataStore;
    private IntegrationTestKit followerTestKit;
    private IntegrationTestKit leaderTestKit;

    @Before
    public void setUp() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_1_ADDRESS);

        followerSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));
        Cluster.get(followerSystem).join(MEMBER_1_ADDRESS);

        follower2System = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member3"));
        Cluster.get(follower2System).join(MEMBER_1_ADDRESS);
    }

    @After
    public void tearDown() {
        if (followerDistributedDataStore != null) {
            followerDistributedDataStore.close();
        }
        if (leaderDistributedDataStore != null) {
            leaderDistributedDataStore.close();
        }

        TestKit.shutdownActorSystem(leaderSystem, true);
        TestKit.shutdownActorSystem(followerSystem, true);
        TestKit.shutdownActorSystem(follower2System,true);

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    private void initDatastoresWithCars(final String type) throws Exception {
        initDatastores(type, MODULE_SHARDS_CARS_ONLY_1_2, CARS);
    }

    private void initDatastoresWithCarsAndPeople(final String type) throws Exception {
        initDatastores(type, MODULE_SHARDS_CARS_PEOPLE_1_2, CARS_AND_PEOPLE);
    }

    private void initDatastores(final String type, final String moduleShardsConfig, final String[] shards)
            throws Exception {
        initDatastores(type, moduleShardsConfig, shards, leaderDatastoreContextBuilder,
                followerDatastoreContextBuilder);
    }

    private void initDatastores(final String type, final String moduleShardsConfig, final String[] shards,
            final DatastoreContext.Builder leaderBuilder, final DatastoreContext.Builder followerBuilder)
                    throws Exception {
        leaderTestKit = new IntegrationTestKit(stateDir(), leaderSystem, leaderBuilder, commitTimeout);

        leaderDistributedDataStore = leaderTestKit.setupDataStore(testParameter, type, moduleShardsConfig, false,
            shards);

        followerTestKit = new IntegrationTestKit(stateDir(), followerSystem, followerBuilder, commitTimeout);
        followerDistributedDataStore = followerTestKit.setupDataStore(
                testParameter, type, moduleShardsConfig, false, shards);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorUtils(), shards);

        leaderTestKit.waitForMembersUp("member-2");
        followerTestKit.waitForMembersUp("member-1");
    }

    private static void verifyCars(final DOMStoreReadTransaction readTx, final MapEntryNode... entries)
            throws Exception {
        assertEquals("Car list node",
            Optional.of(mapNodeBuilder(CarsModel.CAR_QNAME).withValue(Arrays.asList(entries)).build()),
            readTx.read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS));
    }

    private static void verifyNode(final DOMStoreReadTransaction readTx, final YangInstanceIdentifier path,
            final NormalizedNode expNode) throws Exception {
        assertEquals(Optional.of(expNode), readTx.read(path).get(5, TimeUnit.SECONDS));
    }

    private static void verifyExists(final DOMStoreReadTransaction readTx, final YangInstanceIdentifier path)
            throws Exception {
        assertEquals("exists", Boolean.TRUE, readTx.exists(path).get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteTransactionWithSingleShard() throws Exception {
        final String testName = "testWriteTransactionWithSingleShard";
        initDatastoresWithCars(testName);

        final String followerCarShardName = "member-2-shard-cars-" + testName;

        DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        final MapEntryNode car1 = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
        final YangInstanceIdentifier car1Path = CarsModel.newCarPath("optima");
        writeTx.merge(car1Path, car1);

        final MapEntryNode car2 = CarsModel.newCarEntry("sportage", Uint64.valueOf(25000));
        final YangInstanceIdentifier car2Path = CarsModel.newCarPath("sportage");
        writeTx.merge(car2Path, car2);

        followerTestKit.doCommit(writeTx.ready());

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car1, car2);

        verifyCars(leaderDistributedDataStore.newReadOnlyTransaction(), car1, car2);

        // Test delete

        writeTx = followerDistributedDataStore.newWriteOnlyTransaction();

        writeTx.delete(car1Path);

        followerTestKit.doCommit(writeTx.ready());

        verifyExists(followerDistributedDataStore.newReadOnlyTransaction(), car2Path);

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car2);

        verifyCars(leaderDistributedDataStore.newReadOnlyTransaction(), car2);

        // Re-instate the follower member 2 as a single-node to verify replication and recovery.

        // The following is a bit tricky. Before we reinstate the follower we need to ensure it has persisted and
        // applied and all the log entries from the leader. Since we've verified the car data above we know that
        // all the transactions have been applied on the leader so we first read and capture its lastAppliedIndex.
        final AtomicLong leaderLastAppliedIndex = new AtomicLong();
        IntegrationTestKit.verifyShardState(leaderDistributedDataStore, CARS[0],
            state -> leaderLastAppliedIndex.set(state.getLastApplied()));

        // Now we need to make sure the follower has persisted the leader's lastAppliedIndex via ApplyJournalEntries.
        // However we don't know exactly how many ApplyJournalEntries messages there will be as it can differ between
        // the tell-based and ask-based front-ends. For ask-based there will be exactly 2 ApplyJournalEntries but
        // tell-based persists additional payloads which could be replicated and applied in a batch resulting in
        // either 2 or 3 ApplyJournalEntries. To handle this we read the follower's persisted ApplyJournalEntries
        // until we find the one that encompasses the leader's lastAppliedIndex.
        Stopwatch sw = Stopwatch.createStarted();
        boolean done = false;
        while (!done) {
            final List<ApplyJournalEntries> entries = InMemoryJournal.get(followerCarShardName,
                    ApplyJournalEntries.class);
            for (ApplyJournalEntries aje: entries) {
                if (aje.getToIndex() >= leaderLastAppliedIndex.get()) {
                    done = true;
                    break;
                }
            }

            assertTrue("Follower did not persist ApplyJournalEntries containing leader's lastAppliedIndex "
                    + leaderLastAppliedIndex + ". Entries persisted: " + entries, sw.elapsed(TimeUnit.SECONDS) <= 5);

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        TestKit.shutdownActorSystem(leaderSystem, true);
        TestKit.shutdownActorSystem(followerSystem, true);

        final ActorSystem newSystem = newActorSystem("reinstated-member2", "Member2");

        try (var member2Datastore =
            new IntegrationTestKit(stateDir(), newSystem, leaderDatastoreContextBuilder, commitTimeout)
                .setupDataStore(testParameter, testName, "module-shards-member2", true, CARS)) {
            verifyCars(member2Datastore.newReadOnlyTransaction(), car2);
        }
    }

    @Test
    public void testSingleTransactionsWritesInQuickSuccession() throws Exception {
        initDatastoresWithCars("testSingleTransactionsWritesInQuickSuccession");

        final DOMStoreTransactionChain txChain = followerDistributedDataStore.createTransactionChain();

        DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
        followerTestKit.doCommit(writeTx.ready());

        int numCars = 5;
        for (int i = 0; i < numCars; i++) {
            writeTx = txChain.newWriteOnlyTransaction();
            writeTx.write(CarsModel.newCarPath("car" + i), CarsModel.newCarEntry("car" + i, Uint64.valueOf(20000)));
            followerTestKit.doCommit(writeTx.ready());

            try (var tx = txChain.newReadOnlyTransaction()) {
                tx.read(CarsModel.BASE_PATH).get();
            }
        }

        // wait to let the shard catch up with purged
        await("Range set leak test").atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                final var localShard = leaderDistributedDataStore.getActorUtils().findLocalShard("cars").orElseThrow();
                final var frontendMetadata =
                    (FrontendShardDataTreeSnapshotMetadata) leaderDistributedDataStore.getActorUtils()
                    .executeOperation(localShard, new RequestFrontendMetadata());

                assertClientMetadata(frontendMetadata.getClients().get(0), numCars * 2);
            });

        try (var tx = txChain.newReadOnlyTransaction()) {
            final var body = assertInstanceOf(Collection.class,
                tx.read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS).orElseThrow().body());
            assertEquals(numCars, ((Collection<?>) body).size());
        }
    }

    private static void assertClientMetadata(final FrontendClientMetadata clientMeta, final long lastPurged) {
        final var iterator = clientMeta.currentHistories().iterator();
        var metadata = iterator.next();
        while (iterator.hasNext() && metadata.getHistoryId() != 1) {
            metadata = iterator.next();
        }

        assertEquals(UnsignedLongBitmap.of(), metadata.getClosedTransactions());
        assertEquals("[[0.." + lastPurged + "]]", metadata.getPurgedTransactions().ranges().toString());
    }

    @Test
    public void testCloseTransactionMetadataLeak() throws Exception {
        initDatastoresWithCars("testCloseTransactionMetadataLeak");

        final var txChain = followerDistributedDataStore.createTransactionChain();

        var writeTx = txChain.newWriteOnlyTransaction();
        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
        followerTestKit.doCommit(writeTx.ready());

        int numCars = 5;
        for (int i = 0; i < numCars; i++) {
            try (var tx = txChain.newWriteOnlyTransaction()) {
                // Empty on purpose
            }

            try (var tx = txChain.newReadOnlyTransaction()) {
                tx.read(CarsModel.BASE_PATH).get();
            }
        }

        // wait to let the shard catch up with purged
        await("wait for purges to settle").atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                final var localShard = leaderDistributedDataStore.getActorUtils().findLocalShard("cars").orElseThrow();
                final var frontendMetadata =
                    (FrontendShardDataTreeSnapshotMetadata) leaderDistributedDataStore.getActorUtils()
                    .executeOperation(localShard, new RequestFrontendMetadata());

                assertClientMetadata(frontendMetadata.getClients().get(0), numCars * 2);
            });
    }

    @Test
    public void testReadWriteTransactionWithSingleShard() throws Exception {
        initDatastoresWithCars("testReadWriteTransactionWithSingleShard");

        final DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();
        assertNotNull("newReadWriteTransaction returned null", rwTx);

        rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        rwTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        final MapEntryNode car1 = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
        rwTx.merge(CarsModel.newCarPath("optima"), car1);

        verifyCars(rwTx, car1);

        final MapEntryNode car2 = CarsModel.newCarEntry("sportage", Uint64.valueOf(25000));
        final YangInstanceIdentifier car2Path = CarsModel.newCarPath("sportage");
        rwTx.merge(car2Path, car2);

        verifyExists(rwTx, car2Path);

        followerTestKit.doCommit(rwTx.ready());

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car1, car2);
    }

    @Test
    public void testWriteTransactionWithMultipleShards() throws Exception {
        initDatastoresWithCarsAndPeople("testWriteTransactionWithMultipleShards");

        final DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        final YangInstanceIdentifier carsPath = CarsModel.BASE_PATH;
        final NormalizedNode carsNode = CarsModel.emptyContainer();
        writeTx.write(carsPath, carsNode);

        final YangInstanceIdentifier peoplePath = PeopleModel.BASE_PATH;
        final NormalizedNode peopleNode = PeopleModel.emptyContainer();
        writeTx.write(peoplePath, peopleNode);

        followerTestKit.doCommit(writeTx.ready());

        final DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();

        verifyNode(readTx, carsPath, carsNode);
        verifyNode(readTx, peoplePath, peopleNode);
    }

    @Test
    public void testReadWriteTransactionWithMultipleShards() throws Exception {
        initDatastoresWithCarsAndPeople("testReadWriteTransactionWithMultipleShards");

        final DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();
        assertNotNull("newReadWriteTransaction returned null", rwTx);

        final YangInstanceIdentifier carsPath = CarsModel.BASE_PATH;
        final NormalizedNode carsNode = CarsModel.emptyContainer();
        rwTx.write(carsPath, carsNode);

        final YangInstanceIdentifier peoplePath = PeopleModel.BASE_PATH;
        final NormalizedNode peopleNode = PeopleModel.emptyContainer();
        rwTx.write(peoplePath, peopleNode);

        followerTestKit.doCommit(rwTx.ready());

        final DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();

        verifyNode(readTx, carsPath, carsNode);
        verifyNode(readTx, peoplePath, peopleNode);
    }

    @Test
    public void testTransactionChainWithSingleShard() throws Exception {
        initDatastoresWithCars("testTransactionChainWithSingleShard");

        final DOMStoreTransactionChain txChain = followerDistributedDataStore.createTransactionChain();

        // Add the top-level cars container with write-only.

        final DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        final DOMStoreThreePhaseCommitCohort writeTxReady = writeTx.ready();

        // Verify the top-level cars container with read-only.

        verifyNode(txChain.newReadOnlyTransaction(), CarsModel.BASE_PATH, CarsModel.emptyContainer());

        // Perform car operations with read-write.

        final DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

        verifyNode(rwTx, CarsModel.BASE_PATH, CarsModel.emptyContainer());

        rwTx.merge(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        final MapEntryNode car1 = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
        final YangInstanceIdentifier car1Path = CarsModel.newCarPath("optima");
        rwTx.write(car1Path, car1);

        verifyExists(rwTx, car1Path);

        verifyCars(rwTx, car1);

        final MapEntryNode car2 = CarsModel.newCarEntry("sportage", Uint64.valueOf(25000));
        rwTx.merge(CarsModel.newCarPath("sportage"), car2);

        rwTx.delete(car1Path);

        followerTestKit.doCommit(writeTxReady);

        followerTestKit.doCommit(rwTx.ready());

        txChain.close();

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car2);
    }

    @Test
    public void testTransactionChainWithMultipleShards() throws Exception {
        initDatastoresWithCarsAndPeople("testTransactionChainWithMultipleShards");

        final DOMStoreTransactionChain txChain = followerDistributedDataStore.createTransactionChain();

        DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
        writeTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

        followerTestKit.doCommit(writeTx.ready());

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

        writeTx.delete(personPath);

        final DOMStoreThreePhaseCommitCohort cohort3 = writeTx.ready();

        followerTestKit.doCommit(cohort2);
        followerTestKit.doCommit(cohort3);

        txChain.close();

        final DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        verifyCars(readTx, car);

        assertEquals(Optional.empty(), readTx.read(personPath).get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testChainedTransactionFailureWithSingleShard() throws Exception {
        initDatastoresWithCars("testChainedTransactionFailureWithSingleShard");

        final ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                ImmutableMap.<LogicalDatastoreType, DOMStore>builder().put(
                        LogicalDatastoreType.CONFIGURATION, followerDistributedDataStore).build(),
                        MoreExecutors.directExecutor());

        final var listener = mock(FutureCallback.class);
        final DOMTransactionChain txChain = broker.createTransactionChain();
        txChain.addCallback(listener);

        final DOMDataTreeWriteTransaction writeTx = txChain.newWriteOnlyTransaction();

        writeTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(CarsModel.BASE_QNAME))
            .withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk"))
            .build());

        final var ex = assertThrows(ExecutionException.class, () -> writeTx.commit().get(5, TimeUnit.SECONDS));
        assertInstanceOf(TransactionCommitFailedException.class, ex.getCause());

        verify(listener, timeout(5000)).onFailure(any());

        txChain.close();
        broker.close();
    }

    @Test
    public void testChainedTransactionFailureWithMultipleShards() throws Exception {
        initDatastoresWithCarsAndPeople("testChainedTransactionFailureWithMultipleShards");

        try (var broker = new ConcurrentDOMDataBroker(
            Map.of(LogicalDatastoreType.CONFIGURATION, followerDistributedDataStore), MoreExecutors.directExecutor())) {

            final var listener = mock(FutureCallback.class);
            final DOMTransactionChain txChain = broker.createTransactionChain();
            txChain.addCallback(listener);

            final DOMDataTreeWriteTransaction writeTx = txChain.newWriteOnlyTransaction();

            writeTx.put(LogicalDatastoreType.CONFIGURATION, PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

            // Note that merge will validate the data and fail but put succeeds b/c deep validation is not
            // done for put for performance reasons.
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, CarsModel.BASE_PATH, ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(CarsModel.BASE_QNAME))
                .withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk"))
                .build());

            final var ex = assertThrows(ExecutionException.class, () -> writeTx.commit().get(5, TimeUnit.SECONDS));
            assertInstanceOf(TransactionCommitFailedException.class, ex.getCause());

            verify(listener, timeout(5000)).onFailure(any());

            txChain.close();
        }
    }

    @Test
    public void testSingleShardTransactionsWithLeaderChanges() throws Exception {
        followerDatastoreContextBuilder.backendAlivenessTimerIntervalInSeconds(2);
        final String testName = "testSingleShardTransactionsWithLeaderChanges";
        initDatastoresWithCars(testName);

        final String followerCarShardName = "member-2-shard-cars-" + testName;
        InMemoryJournal.addWriteMessagesCompleteLatch(followerCarShardName, 1, ApplyJournalEntries.class);

        // Write top-level car container from the follower so it uses a remote Tx.

        DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        followerTestKit.doCommit(writeTx.ready());

        InMemoryJournal.waitForWriteMessagesComplete(followerCarShardName);

        // Switch the leader to the follower

        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder
                .shardElectionTimeoutFactor(1).customRaftPolicyImplementation(null));

        TestKit.shutdownActorSystem(leaderSystem, true);
        Cluster.get(followerSystem).leave(MEMBER_1_ADDRESS);

        followerTestKit.waitUntilNoLeader(followerDistributedDataStore.getActorUtils(), CARS);

        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_2_ADDRESS);

        final DatastoreContext.Builder newMember1Builder = DatastoreContext.newBuilder()
                .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);
        final var newMember1TestKit = new IntegrationTestKit(stateDir(), leaderSystem, newMember1Builder,
            commitTimeout);

        try (var ds = newMember1TestKit.setupDataStore(testParameter, testName, MODULE_SHARDS_CARS_ONLY_1_2, false,
            CARS)) {

            followerTestKit.waitUntilLeader(followerDistributedDataStore.getActorUtils(), CARS);

            // Write a car entry to the new leader - should switch to local Tx

            writeTx = followerDistributedDataStore.newWriteOnlyTransaction();

            MapEntryNode car1 = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
            YangInstanceIdentifier car1Path = CarsModel.newCarPath("optima");
            writeTx.merge(car1Path, car1);

            followerTestKit.doCommit(writeTx.ready());

            verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car1);
        }
    }

    @Test
    public void testTransactionForwardedToLeaderAfterRetry() throws Exception {
        followerDatastoreContextBuilder.shardBatchedModificationCount(2);
        leaderDatastoreContextBuilder.shardBatchedModificationCount(2);
        initDatastoresWithCarsAndPeople("testTransactionForwardedToLeaderAfterRetry");

        // Verify backend statistics on start
        verifyCarsReadWriteTransactions(leaderDistributedDataStore, 0);
        verifyCarsReadWriteTransactions(followerDistributedDataStore, 0);

        // Do an initial write to get the primary shard info cached.

        final DOMStoreWriteTransaction initialWriteTx = followerDistributedDataStore.newWriteOnlyTransaction();
        initialWriteTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        initialWriteTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());
        followerTestKit.doCommit(initialWriteTx.ready());

        // Wait for the commit to be replicated to the follower.

        verifyRaftState(followerDistributedDataStore, "cars",
            raftState -> assertEquals("getLastApplied", 1, raftState.getLastApplied()));

        verifyRaftState(followerDistributedDataStore, "people",
            raftState -> assertEquals("getLastApplied", 1, raftState.getLastApplied()));

        // Prepare, ready and canCommit a WO tx that writes to 2 shards. This will become the current tx in
        // the leader shard.

        final DOMStoreWriteTransaction writeTx1 = followerDistributedDataStore.newWriteOnlyTransaction();
        writeTx1.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
        writeTx1.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());
        final DOMStoreThreePhaseCommitCohort writeTx1Cohort = writeTx1.ready();
        final ListenableFuture<Boolean> writeTx1CanCommit = writeTx1Cohort.canCommit();
        writeTx1CanCommit.get(5, TimeUnit.SECONDS);

        // Prepare and ready another WO tx that writes to 2 shards but don't canCommit yet. This will be queued
        // in the leader shard.

        final DOMStoreWriteTransaction writeTx2 = followerDistributedDataStore.newWriteOnlyTransaction();
        final LinkedList<MapEntryNode> cars = new LinkedList<>();
        int carIndex = 1;
        cars.add(CarsModel.newCarEntry("car" + carIndex, Uint64.valueOf(carIndex)));
        writeTx2.write(CarsModel.newCarPath("car" + carIndex), cars.getLast());
        carIndex++;
        NormalizedNode people = mapNodeBuilder(PeopleModel.PERSON_QNAME)
                .withChild(PeopleModel.newPersonEntry("Dude")).build();
        writeTx2.write(PeopleModel.PERSON_LIST_PATH, people);
        final DOMStoreThreePhaseCommitCohort writeTx2Cohort = writeTx2.ready();

        // At this point only leader should see the transactions
        verifyCarsReadWriteTransactions(leaderDistributedDataStore, 2);
        verifyCarsReadWriteTransactions(followerDistributedDataStore, 0);

        // Prepare another WO that writes to a single shard and thus will be directly committed on ready. This
        // tx writes 5 cars so 2 BatchedModifications messages will be sent initially and cached in the leader shard
        // (with shardBatchedModificationCount set to 2). The 3rd BatchedModifications will be sent on ready.

        final DOMStoreWriteTransaction writeTx3 = followerDistributedDataStore.newWriteOnlyTransaction();
        for (int i = 1; i <= 5; i++, carIndex++) {
            cars.add(CarsModel.newCarEntry("car" + carIndex, Uint64.valueOf(carIndex)));
            writeTx3.write(CarsModel.newCarPath("car" + carIndex), cars.getLast());
        }

        // Prepare another WO that writes to a single shard. This will send a single BatchedModifications message
        // on ready.

        final DOMStoreWriteTransaction writeTx4 = followerDistributedDataStore.newWriteOnlyTransaction();
        cars.add(CarsModel.newCarEntry("car" + carIndex, Uint64.valueOf(carIndex)));
        writeTx4.write(CarsModel.newCarPath("car" + carIndex), cars.getLast());
        carIndex++;

        // Prepare a RW tx that will create a tx actor and send a ForwardedReadyTransaction message to the leader shard
        // on ready.

        final DOMStoreReadWriteTransaction readWriteTx = followerDistributedDataStore.newReadWriteTransaction();
        cars.add(CarsModel.newCarEntry("car" + carIndex, Uint64.valueOf(carIndex)));
        final YangInstanceIdentifier carPath = CarsModel.newCarPath("car" + carIndex);
        readWriteTx.write(carPath, cars.getLast());

        // There is a difference here between implementations: tell-based protocol enforces batching on per-transaction
        // level whereas ask-based protocol has a global limit towards a shard -- and hence flushes out last two
        // transactions eagerly.
        verifyCarsReadWriteTransactions(leaderDistributedDataStore, 3);
        verifyCarsReadWriteTransactions(followerDistributedDataStore, 0);

        // Disable elections on the leader so it switches to follower.

        sendDatastoreContextUpdate(leaderDistributedDataStore, leaderDatastoreContextBuilder
                .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName())
                .shardElectionTimeoutFactor(10));

        leaderTestKit.waitUntilNoLeader(leaderDistributedDataStore.getActorUtils(), "cars");

        // Submit all tx's - the messages should get queued for retry.

        final ListenableFuture<Boolean> writeTx2CanCommit = writeTx2Cohort.canCommit();
        final DOMStoreThreePhaseCommitCohort writeTx3Cohort = writeTx3.ready();
        final DOMStoreThreePhaseCommitCohort writeTx4Cohort = writeTx4.ready();
        final DOMStoreThreePhaseCommitCohort rwTxCohort = readWriteTx.ready();

        // Enable elections on the other follower so it becomes the leader, at which point the
        // tx's should get forwarded from the previous leader to the new leader to complete the commits.

        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder
                .customRaftPolicyImplementation(null).shardElectionTimeoutFactor(1));
        IntegrationTestKit.findLocalShard(followerDistributedDataStore.getActorUtils(), "cars")
                .tell(TimeoutNow.INSTANCE, ActorRef.noSender());
        IntegrationTestKit.findLocalShard(followerDistributedDataStore.getActorUtils(), "people")
                .tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        followerTestKit.doCommit(writeTx1CanCommit, writeTx1Cohort);
        followerTestKit.doCommit(writeTx2CanCommit, writeTx2Cohort);
        followerTestKit.doCommit(writeTx3Cohort);
        followerTestKit.doCommit(writeTx4Cohort);
        followerTestKit.doCommit(rwTxCohort);

        // At this point everything is committed and the follower datastore should see 5 transactions, but leader should
        // only see the initial transactions
        verifyCarsReadWriteTransactions(leaderDistributedDataStore, 3);
        verifyCarsReadWriteTransactions(followerDistributedDataStore, 5);

        DOMStoreReadTransaction readTx = leaderDistributedDataStore.newReadOnlyTransaction();
        verifyCars(readTx, cars.toArray(new MapEntryNode[cars.size()]));
        verifyNode(readTx, PeopleModel.PERSON_LIST_PATH, people);
    }

    private static void verifyCarsReadWriteTransactions(final ClientBackedDataStore datastore, final int expected)
            throws Exception {
        IntegrationTestKit.verifyShardStats(datastore, "cars",
            stats -> assertEquals("getReadWriteTransactionCount", expected, stats.getReadWriteTransactionCount()));
    }

    @Test
    public void testLeadershipTransferOnShutdown() throws Exception {
        leaderDatastoreContextBuilder.shardBatchedModificationCount(1);
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(10).customRaftPolicyImplementation(null);
        final String testName = "testLeadershipTransferOnShutdown";
        initDatastores(testName, MODULE_SHARDS_CARS_PEOPLE_1_2_3, CARS_AND_PEOPLE);

        final var follower2TestKit = new IntegrationTestKit(stateDir(), follower2System,
                DatastoreContext.newBuilderFrom(followerDatastoreContextBuilder.build()).operationTimeoutInMillis(500),
                commitTimeout);
        try (var follower2DistributedDataStore = follower2TestKit.setupDataStore(testParameter, testName,
            MODULE_SHARDS_CARS_PEOPLE_1_2_3, false)) {

            followerTestKit.waitForMembersUp("member-3");
            follower2TestKit.waitForMembersUp("member-1", "member-2");

            // Create and submit a couple tx's so they're pending.

            DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());
            final DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

            // FIXME: this assertion should be made in an explicit Shard test
            //            IntegrationTestKit.verifyShardStats(leaderDistributedDataStore, "cars",
            //                stats -> assertEquals("getTxCohortCacheSize", 1, stats.getTxCohortCacheSize()));

            writeTx = followerDistributedDataStore.newWriteOnlyTransaction();
            final MapEntryNode car = CarsModel.newCarEntry("optima", Uint64.valueOf(20000));
            writeTx.write(CarsModel.newCarPath("optima"), car);
            final DOMStoreThreePhaseCommitCohort cohort2 = writeTx.ready();

            // FIXME: this assertion should be made in an explicit Shard test
            //            IntegrationTestKit.verifyShardStats(leaderDistributedDataStore, "cars",
            //                stats -> assertEquals("getTxCohortCacheSize", 2, stats.getTxCohortCacheSize()));

            // Gracefully stop the leader via a Shutdown message.

            sendDatastoreContextUpdate(leaderDistributedDataStore, leaderDatastoreContextBuilder
                .shardElectionTimeoutFactor(100));

            final FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
            final Future<ActorRef> future = leaderDistributedDataStore.getActorUtils().findLocalShardAsync("cars");
            final ActorRef leaderActor = Await.result(future, duration);

            final Future<Boolean> stopFuture = Patterns.gracefulStop(leaderActor, duration, Shutdown.INSTANCE);

            // Commit the 2 transactions. They should finish and succeed.

            followerTestKit.doCommit(cohort1);
            followerTestKit.doCommit(cohort2);

            // Wait for the leader actor stopped.

            final Boolean stopped = Await.result(stopFuture, duration);
            assertEquals("Stopped", Boolean.TRUE, stopped);

            // Verify leadership was transferred by reading the committed data from the other nodes.

            verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car);
            verifyCars(follower2DistributedDataStore.newReadOnlyTransaction(), car);
        }
    }

    @Test
    public void testTransactionWithIsolatedLeader() throws Exception {
        // Set the isolated leader check interval high so we can control the switch to IsolatedLeader.
        leaderDatastoreContextBuilder.shardIsolatedLeaderCheckIntervalInMillis(10000000);
        final String testName = "testTransactionWithIsolatedLeader";
        initDatastoresWithCars(testName);

        // Tx that is submitted after the follower is stopped but before the leader transitions to IsolatedLeader.
        final DOMStoreWriteTransaction preIsolatedLeaderWriteTx = leaderDistributedDataStore.newWriteOnlyTransaction();
        preIsolatedLeaderWriteTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        // Tx that is submitted after the leader transitions to IsolatedLeader.
        final DOMStoreWriteTransaction noShardLeaderWriteTx = leaderDistributedDataStore.newWriteOnlyTransaction();
        noShardLeaderWriteTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        // Tx that is submitted after the follower is reinstated.
        final DOMStoreWriteTransaction successWriteTx = leaderDistributedDataStore.newWriteOnlyTransaction();
        successWriteTx.merge(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        // Stop the follower
        followerTestKit.watch(followerDistributedDataStore.getActorUtils().getShardManager());
        followerDistributedDataStore.close();
        followerTestKit.expectTerminated(followerDistributedDataStore.getActorUtils().getShardManager());

        // Submit the preIsolatedLeaderWriteTx so it's pending
        final DOMStoreThreePhaseCommitCohort preIsolatedLeaderTxCohort = preIsolatedLeaderWriteTx.ready();

        // Change the isolated leader check interval low so it changes to IsolatedLeader.
        sendDatastoreContextUpdate(leaderDistributedDataStore, leaderDatastoreContextBuilder
                .shardIsolatedLeaderCheckIntervalInMillis(200));

        verifyRaftState(leaderDistributedDataStore, "cars",
            raftState -> assertEquals("getRaftState", "IsolatedLeader", raftState.getRaftState()));

        final var noShardLeaderCohort = noShardLeaderWriteTx.ready();
        // tell-based canCommit() does not have a real timeout and hence continues
        final var canCommit = noShardLeaderCohort.canCommit();
        Uninterruptibles.sleepUninterruptibly(commitTimeout, TimeUnit.SECONDS);
        assertFalse(canCommit.isDone());

        sendDatastoreContextUpdate(leaderDistributedDataStore, leaderDatastoreContextBuilder
                .shardElectionTimeoutFactor(100));

        final DOMStoreThreePhaseCommitCohort successTxCohort = successWriteTx.ready();

        followerDistributedDataStore = followerTestKit.setupDataStore(testParameter, testName,
            MODULE_SHARDS_CARS_ONLY_1_2, false, CARS);

        leaderTestKit.doCommit(preIsolatedLeaderTxCohort);
        leaderTestKit.doCommit(successTxCohort);

        // continuation of canCommit(): readied transaction will complete commit, but will report an OLFE
        final var ex = assertThrows(ExecutionException.class,
            () -> canCommit.get(commitTimeout, TimeUnit.SECONDS)).getCause();
        assertInstanceOf(OptimisticLockFailedException.class, ex);
        assertEquals("Optimistic lock failed for path " + CarsModel.BASE_PATH, ex.getMessage());
        final var cause = assertInstanceOf(ConflictingModificationAppliedException.class, ex.getCause());
        assertEquals("Node was created by other transaction.", cause.getMessage());
        assertEquals(CarsModel.BASE_PATH, cause.getPath());
    }

    @Test
    public void testTransactionWithShardLeaderNotResponding() throws Exception {
        followerDatastoreContextBuilder.frontendRequestTimeoutInSeconds(2);
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(50);
        initDatastoresWithCars("testTransactionWithShardLeaderNotResponding");

        // Do an initial read to get the primary shard info cached.

        final DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        readTx.read(CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS);

        // Shutdown the leader and try to create a new tx.

        TestKit.shutdownActorSystem(leaderSystem, true);

        followerDatastoreContextBuilder.operationTimeoutInMillis(50).shardElectionTimeoutFactor(1);
        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder);

        final DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();

        rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        final var ex = assertThrows(ExecutionException.class, () -> followerTestKit.doCommit(rwTx.ready()));
        assertThat("Unexpected exception: " + Throwables.getStackTraceAsString(ex.getCause()),
            Throwables.getRootCause(ex), instanceOf(RequestTimeoutException.class));
    }

    @Test
    public void testTransactionWithCreateTxFailureDueToNoLeader() throws Exception {
        followerDatastoreContextBuilder.frontendRequestTimeoutInSeconds(2);
        initDatastoresWithCars("testTransactionWithCreateTxFailureDueToNoLeader");

        // Do an initial read to get the primary shard info cached.

        final DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        readTx.read(CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS);

        // Shutdown the leader and try to create a new tx.

        TestKit.shutdownActorSystem(leaderSystem, true);

        Cluster.get(followerSystem).leave(MEMBER_1_ADDRESS);

        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder
                .operationTimeoutInMillis(10).shardElectionTimeoutFactor(1).customRaftPolicyImplementation(null));

        final DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();

        rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        final var ex = assertThrows(ExecutionException.class, () -> followerTestKit.doCommit(rwTx.ready()));
        assertThat("Unexpected exception: " + Throwables.getStackTraceAsString(ex.getCause()),
            Throwables.getRootCause(ex), instanceOf(RequestTimeoutException.class));
    }

    @Test
    public void testTransactionRetryWithInitialAskTimeoutExOnCreateTx() throws Exception {
        followerDatastoreContextBuilder.backendAlivenessTimerIntervalInSeconds(2);
        String testName = "testTransactionRetryWithInitialAskTimeoutExOnCreateTx";
        initDatastores(testName, MODULE_SHARDS_CARS_1_2_3, CARS);

        final DatastoreContext.Builder follower2DatastoreContextBuilder = DatastoreContext.newBuilder()
                .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(10);
        final var follower2TestKit = new IntegrationTestKit(stateDir(), follower2System,
            follower2DatastoreContextBuilder, commitTimeout);

        try (var ds = follower2TestKit.setupDataStore(testParameter, testName, MODULE_SHARDS_CARS_1_2_3, false, CARS)) {

            followerTestKit.waitForMembersUp("member-1", "member-3");
            follower2TestKit.waitForMembersUp("member-1", "member-2");

            // Do an initial read to get the primary shard info cached.

            final DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
            readTx.read(CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS);

            // Shutdown the leader and try to create a new tx.

            TestKit.shutdownActorSystem(leaderSystem, true);

            Cluster.get(followerSystem).leave(MEMBER_1_ADDRESS);

            sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder
                .operationTimeoutInMillis(500).shardElectionTimeoutFactor(5).customRaftPolicyImplementation(null));

            final DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();

            rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

            followerTestKit.doCommit(rwTx.ready());
        }
    }

    @Test
    public void testSemiReachableCandidateNotDroppingLeader() throws Exception {
        final String testName = "testSemiReachableCandidateNotDroppingLeader";
        initDatastores(testName, MODULE_SHARDS_CARS_1_2_3, CARS);

        final DatastoreContext.Builder follower2DatastoreContextBuilder = DatastoreContext.newBuilder()
                .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(10);
        final var follower2TestKit = new IntegrationTestKit(stateDir(), follower2System,
            follower2DatastoreContextBuilder, commitTimeout);

        final var ds2 = follower2TestKit.setupDataStore(testParameter, testName, MODULE_SHARDS_CARS_1_2_3, false, CARS);

        followerTestKit.waitForMembersUp("member-1", "member-3");
        follower2TestKit.waitForMembersUp("member-1", "member-2");

        // behavior is controlled by akka.coordinated-shutdown.run-by-actor-system-terminate configuration option
        TestKit.shutdownActorSystem(follower2System, true);

        ActorRef cars = leaderDistributedDataStore.getActorUtils().findLocalShard("cars").orElseThrow();
        final OnDemandRaftState initialState = (OnDemandRaftState) leaderDistributedDataStore.getActorUtils()
                .executeOperation(cars, GetOnDemandRaftState.INSTANCE);

        Cluster leaderCluster = Cluster.get(leaderSystem);
        Cluster followerCluster = Cluster.get(followerSystem);
        Cluster follower2Cluster = Cluster.get(follower2System);

        Member follower2Member = follower2Cluster.readView().self();

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> containsUnreachable(leaderCluster, follower2Member));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> containsUnreachable(followerCluster, follower2Member));

        ActorRef followerCars = followerDistributedDataStore.getActorUtils().findLocalShard("cars").orElseThrow();

        // to simulate a follower not being able to receive messages, but still being able to send messages and becoming
        // candidate, we can just send a couple of RequestVotes to both leader and follower.
        cars.tell(new RequestVote(initialState.getCurrentTerm() + 1, "member-3-shard-cars", -1, -1), null);
        followerCars.tell(new RequestVote(initialState.getCurrentTerm() + 1, "member-3-shard-cars", -1, -1), null);
        cars.tell(new RequestVote(initialState.getCurrentTerm() + 3, "member-3-shard-cars", -1, -1), null);
        followerCars.tell(new RequestVote(initialState.getCurrentTerm() + 3, "member-3-shard-cars", -1, -1), null);

        OnDemandRaftState stateAfter = (OnDemandRaftState) leaderDistributedDataStore.getActorUtils()
                .executeOperation(cars, GetOnDemandRaftState.INSTANCE);
        OnDemandRaftState followerState = (OnDemandRaftState) followerDistributedDataStore.getActorUtils()
                .executeOperation(cars, GetOnDemandRaftState.INSTANCE);

        assertEquals(initialState.getCurrentTerm(), stateAfter.getCurrentTerm());
        assertEquals(initialState.getCurrentTerm(), followerState.getCurrentTerm());

        ds2.close();
    }

    private static Boolean containsUnreachable(final Cluster cluster, final Member member) {
        // unreachableMembers() returns scala.collection.immutable.Set, but we are using scala.collection.Set to fix JDT
        // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=468276#c32
        final Set<Member> members = cluster.readView().unreachableMembers();
        return members.contains(member);
    }

    @Test
    public void testInstallSnapshot() throws Exception {
        final String testName = "testInstallSnapshot";
        final String leaderCarShardName = "member-1-shard-cars-" + testName;
        final String followerCarShardName = "member-2-shard-cars-" + testName;

        // Setup a saved snapshot on the leader. The follower will startup with no data and the leader should
        // install a snapshot to sync the follower.

        DataTree tree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION,
            SchemaContextHelper.full());

        final ContainerNode carsNode = CarsModel.newCarsNode(
                CarsModel.newCarsMapNode(CarsModel.newCarEntry("optima", Uint64.valueOf(20000))));
        AbstractShardTest.writeToStore(tree, CarsModel.BASE_PATH, carsNode);

        final NormalizedNode snapshotRoot = AbstractShardTest.readStore(tree, YangInstanceIdentifier.of());
        final Snapshot initialSnapshot = Snapshot.create(
                new ShardSnapshotState(new MetadataShardDataTreeSnapshot(snapshotRoot)),
                List.of(), 5, 1, 5, 1, new TermInfo(1, null), null);
        InMemorySnapshotStore.addSnapshot(leaderCarShardName, initialSnapshot);

        InMemorySnapshotStore.addSnapshotSavedLatch(leaderCarShardName);
        InMemorySnapshotStore.addSnapshotSavedLatch(followerCarShardName);

        initDatastoresWithCars(testName);

        assertEquals(Optional.of(carsNode), leaderDistributedDataStore.newReadOnlyTransaction().read(
            CarsModel.BASE_PATH).get(5, TimeUnit.SECONDS));

        verifySnapshot(InMemorySnapshotStore.waitForSavedSnapshot(leaderCarShardName, Snapshot.class),
                initialSnapshot, snapshotRoot);

        verifySnapshot(InMemorySnapshotStore.waitForSavedSnapshot(followerCarShardName, Snapshot.class),
                initialSnapshot, snapshotRoot);
    }

    @Test
    public void testReadWriteMessageSlicing() throws Exception {
        leaderDatastoreContextBuilder.maximumMessageSliceSize(100);
        followerDatastoreContextBuilder.maximumMessageSliceSize(100);
        initDatastoresWithCars("testLargeReadReplySlicing");

        final DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();

        final NormalizedNode carsNode = CarsModel.create();
        rwTx.write(CarsModel.BASE_PATH, carsNode);

        verifyNode(rwTx, CarsModel.BASE_PATH, carsNode);
    }

    @SuppressWarnings("IllegalCatch")
    @Test
    public void testRaftCallbackDuringLeadershipDrop() throws Exception {
        final String testName = "testRaftCallbackDuringLeadershipDrop";
        initDatastores(testName, MODULE_SHARDS_CARS_1_2_3, CARS);

        final ExecutorService executor = Executors.newSingleThreadExecutor();

        final var follower2TestKit = new IntegrationTestKit(stateDir(), follower2System,
                DatastoreContext.newBuilderFrom(followerDatastoreContextBuilder.build()).operationTimeoutInMillis(500)
                        .shardLeaderElectionTimeoutInSeconds(3600),
                commitTimeout);

        final DOMStoreWriteTransaction initialWriteTx = leaderDistributedDataStore.newWriteOnlyTransaction();
        initialWriteTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        leaderTestKit.doCommit(initialWriteTx.ready());

        try (var follower2DistributedDataStore = follower2TestKit.setupDataStore(testParameter, testName,
            MODULE_SHARDS_CARS_1_2_3, false)) {

            final ActorRef member3Cars = ((LocalShardStore) follower2DistributedDataStore).getLocalShards()
                    .getLocalShards().get("cars").getActor();
            final ActorRef member2Cars = ((LocalShardStore)followerDistributedDataStore).getLocalShards()
                    .getLocalShards().get("cars").getActor();
            member2Cars.tell(new StartDropMessages<>(AppendEntries.class), null);
            member3Cars.tell(new StartDropMessages<>(AppendEntries.class), null);

            final DOMStoreWriteTransaction newTx = leaderDistributedDataStore.newWriteOnlyTransaction();
            newTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            final AtomicBoolean submitDone = new AtomicBoolean(false);
            executor.submit(() -> {
                try {
                    leaderTestKit.doCommit(newTx.ready());
                    submitDone.set(true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            final ActorRef leaderCars = ((LocalShardStore) leaderDistributedDataStore).getLocalShards()
                    .getLocalShards().get("cars").getActor();
            await().atMost(10, TimeUnit.SECONDS)
                    .until(() -> ((OnDemandRaftState) leaderDistributedDataStore.getActorUtils()
                            .executeOperation(leaderCars, GetOnDemandRaftState.INSTANCE)).getLastIndex() >= 1);

            final OnDemandRaftState raftState = (OnDemandRaftState)leaderDistributedDataStore.getActorUtils()
                    .executeOperation(leaderCars, GetOnDemandRaftState.INSTANCE);

            // Simulate a follower not receiving heartbeats but still being able to send messages ie RequestVote with
            // new term(switching to candidate after election timeout)
            leaderCars.tell(new RequestVote(raftState.getCurrentTerm() + 1,
                    "member-3-shard-cars-testRaftCallbackDuringLeadershipDrop", -1,
                            -1), member3Cars);

            member2Cars.tell(new StopDropMessages<>(AppendEntries.class), null);
            member3Cars.tell(new StopDropMessages<>(AppendEntries.class), null);

            await("Is tx stuck in COMMIT_PENDING")
                    .atMost(10, TimeUnit.SECONDS).untilAtomic(submitDone, equalTo(Boolean.TRUE));

        }

        executor.shutdownNow();
    }

    @Test
    public void testSnapshotOnRootOverwrite() throws Exception {
        initDatastores("testSnapshotOnRootOverwrite", "module-shards-default-cars-member1-and-2.conf",
            new String[] {"cars", "default"},
            leaderDatastoreContextBuilder.snapshotOnRootOverwrite(true),
            followerDatastoreContextBuilder.snapshotOnRootOverwrite(true));

        leaderTestKit.waitForMembersUp("member-2");
        final ContainerNode rootNode = ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(SchemaContext.NAME))
                .withChild(CarsModel.create())
                .build();

        leaderTestKit.testWriteTransaction(leaderDistributedDataStore, YangInstanceIdentifier.of(), rootNode);

        // FIXME: CONTROLLER-2020: ClientBackedDatastore does not have stable indexes/term,
        //                         the snapshot index seems to fluctuate
        assumeTrue(false);
        IntegrationTestKit.verifyShardState(leaderDistributedDataStore, "cars",
            state -> assertEquals(1, state.getSnapshotIndex()));

        IntegrationTestKit.verifyShardState(followerDistributedDataStore, "cars",
            state -> assertEquals(1, state.getSnapshotIndex()));

        verifySnapshot("member-1-shard-cars-testSnapshotOnRootOverwrite", 1);
        verifySnapshot("member-2-shard-cars-testSnapshotOnRootOverwrite", 1);

        for (int i = 0; i < 10; i++) {
            leaderTestKit.testWriteTransaction(leaderDistributedDataStore, CarsModel.newCarPath("car " + i),
                    CarsModel.newCarEntry("car " + i, Uint64.ONE));
        }

        // fake snapshot causes the snapshotIndex to move
        IntegrationTestKit.verifyShardState(leaderDistributedDataStore, "cars",
            state -> assertEquals(10, state.getSnapshotIndex()));
        IntegrationTestKit.verifyShardState(followerDistributedDataStore, "cars",
            state -> assertEquals(10, state.getSnapshotIndex()));

        // however the real snapshot still has not changed and was taken at index 1
        verifySnapshot("member-1-shard-cars-testSnapshotOnRootOverwrite", 1);
        verifySnapshot("member-2-shard-cars-testSnapshotOnRootOverwrite", 1);

        // root overwrite so expect a snapshot
        leaderTestKit.testWriteTransaction(leaderDistributedDataStore, YangInstanceIdentifier.of(), rootNode);

        // this was a real snapshot so everything should be in it(1(DisableTrackingPayload) + 1 + 10 + 1)
        IntegrationTestKit.verifyShardState(leaderDistributedDataStore, "cars",
            state -> assertEquals(12, state.getSnapshotIndex()));
        IntegrationTestKit.verifyShardState(followerDistributedDataStore, "cars",
            state -> assertEquals(12, state.getSnapshotIndex()));

        verifySnapshot("member-1-shard-cars-testSnapshotOnRootOverwrite", 12);
        verifySnapshot("member-2-shard-cars-testSnapshotOnRootOverwrite", 12);
    }

    private static void verifySnapshot(final String persistenceId, final long lastAppliedIndex) {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Snapshot> snap = InMemorySnapshotStore.getSnapshots(persistenceId, Snapshot.class);
                assertEquals(1, snap.size());
                assertEquals(lastAppliedIndex, snap.get(0).getLastAppliedIndex());
            }
        );
    }

    private static void verifySnapshot(final Snapshot actual, final Snapshot expected,
                                       final NormalizedNode expRoot) {
        assertEquals("Snapshot getLastAppliedTerm", expected.getLastAppliedTerm(), actual.getLastAppliedTerm());
        assertEquals("Snapshot getLastAppliedIndex", expected.getLastAppliedIndex(), actual.getLastAppliedIndex());
        assertEquals("Snapshot getLastTerm", expected.getLastTerm(), actual.getLastTerm());
        assertEquals("Snapshot getLastIndex", expected.getLastIndex(), actual.getLastIndex());
        assertEquals("Snapshot state type", ShardSnapshotState.class, actual.getState().getClass());
        MetadataShardDataTreeSnapshot shardSnapshot =
                (MetadataShardDataTreeSnapshot) ((ShardSnapshotState)actual.getState()).getSnapshot();
        assertEquals("Snapshot root node", expRoot, shardSnapshot.getRootNode().orElseThrow());
    }

    private static void sendDatastoreContextUpdate(final ClientBackedDataStore dataStore, final Builder builder) {
        final Builder newBuilder = DatastoreContext.newBuilderFrom(builder.build());
        final DatastoreContextFactory mockContextFactory = mock(DatastoreContextFactory.class);
        final Answer<DatastoreContext> answer = invocation -> newBuilder.build();
        doAnswer(answer).when(mockContextFactory).getBaseDatastoreContext();
        doAnswer(answer).when(mockContextFactory).getShardDatastoreContext(anyString());
        dataStore.onDatastoreContextUpdated(mockContextFactory);
    }
}
