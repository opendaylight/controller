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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.dispatch.Futures;
import akka.pattern.AskTimeoutException;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit.ShardStatsVerifier;
import org.opendaylight.controller.cluster.datastore.MemberNode.RaftStateVerifier;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.ShardLeaderNotRespondingException;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.GetShardDataTree;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * End-to-end distributed data store tests that exercise remote shards and transactions.
 *
 * @author Thomas Pantelis
 */
public class DistributedDataStoreRemotingIntegrationTest {

    private static final String[] CARS_AND_PEOPLE = {"cars", "people"};
    private static final String[] CARS = {"cars"};

    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
    private static final Address MEMBER_2_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2559");

    private static final String MODULE_SHARDS_CARS_ONLY_1_2 = "module-shards-cars-member-1-and-2.conf";
    private static final String MODULE_SHARDS_CARS_PEOPLE_1_2 = "module-shards-member1-and-2.conf";
    private static final String MODULE_SHARDS_CARS_PEOPLE_1_2_3 = "module-shards-member1-and-2-and-3.conf";

    private ActorSystem leaderSystem;
    private ActorSystem followerSystem;
    private ActorSystem follower2System;

    private final DatastoreContext.Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5).
                customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());

    private DistributedDataStore followerDistributedDataStore;
    private DistributedDataStore leaderDistributedDataStore;
    private IntegrationTestKit followerTestKit;
    private IntegrationTestKit leaderTestKit;

    @Before
    public void setUp() {
        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_1_ADDRESS);

        followerSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));
        Cluster.get(followerSystem).join(MEMBER_1_ADDRESS);

        follower2System = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member3"));
        Cluster.get(follower2System).join(MEMBER_1_ADDRESS);
    }

    @After
    public void tearDown() {
        JavaTestKit.shutdownActorSystem(leaderSystem);
        JavaTestKit.shutdownActorSystem(followerSystem);
        JavaTestKit.shutdownActorSystem(follower2System);
    }

    private void initDatastoresWithCars(String type) {
        initDatastores(type, MODULE_SHARDS_CARS_ONLY_1_2, CARS);
    }

    private void initDatastoresWithCarsAndPeople(String type) {
        initDatastores(type, MODULE_SHARDS_CARS_PEOPLE_1_2, CARS_AND_PEOPLE);
    }

    private void initDatastores(String type, String moduleShardsConfig, String[] shards) {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);

        leaderDistributedDataStore = leaderTestKit.setupDistributedDataStore(type, moduleShardsConfig, false, shards);

        followerTestKit = new IntegrationTestKit(followerSystem, followerDatastoreContextBuilder);
        followerDistributedDataStore = followerTestKit.setupDistributedDataStore(type, moduleShardsConfig, false, shards);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(), shards);
    }

    private static void verifyCars(DOMStoreReadTransaction readTx, MapEntryNode... entries) throws Exception {
        Optional<NormalizedNode<?, ?>> optional = readTx.read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
        assertEquals("isPresent", true, optional.isPresent());

        CollectionNodeBuilder<MapEntryNode, MapNode> listBuilder = ImmutableNodes.mapNodeBuilder(CarsModel.CAR_QNAME);
        for(NormalizedNode<?, ?> entry: entries) {
            listBuilder.withChild((MapEntryNode) entry);
        }

        assertEquals("Car list node", listBuilder.build(), optional.get());
    }

    private static void verifyNode(DOMStoreReadTransaction readTx, YangInstanceIdentifier path, NormalizedNode<?, ?> expNode)
            throws Exception {
        Optional<NormalizedNode<?, ?>> optional = readTx.read(path).get(5, TimeUnit.SECONDS);
        assertEquals("isPresent", true, optional.isPresent());
        assertEquals("Data node", expNode, optional.get());
    }

    private static void verifyExists(DOMStoreReadTransaction readTx, YangInstanceIdentifier path) throws Exception {
        Boolean exists = readTx.exists(path).get(5, TimeUnit.SECONDS);
        assertEquals("exists", true, exists);
    }

    @Test
    public void testWriteTransactionWithSingleShard() throws Exception {
        String testName = "testWriteTransactionWithSingleShard";
        initDatastoresWithCars(testName);

        String followerCarShardName = "member-2-shard-cars-" + testName;
        InMemoryJournal.addWriteMessagesCompleteLatch(followerCarShardName, 2, ApplyJournalEntries.class );

        DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        MapEntryNode car1 = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        YangInstanceIdentifier car1Path = CarsModel.newCarPath("optima");
        writeTx.merge(car1Path, car1);

        MapEntryNode car2 = CarsModel.newCarEntry("sportage", BigInteger.valueOf(25000));
        YangInstanceIdentifier car2Path = CarsModel.newCarPath("sportage");
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

        InMemoryJournal.waitForWriteMessagesComplete(followerCarShardName);

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);
        JavaTestKit.shutdownActorSystem(followerSystem, null, true);

        ActorSystem newSystem = ActorSystem.create("reinstated-member2", ConfigFactory.load().getConfig("Member2"));

        DistributedDataStore member2Datastore = new IntegrationTestKit(newSystem, leaderDatastoreContextBuilder).
                setupDistributedDataStore(testName, "module-shards-member2", true, CARS_AND_PEOPLE);

        verifyCars(member2Datastore.newReadOnlyTransaction(), car2);

        JavaTestKit.shutdownActorSystem(newSystem);
    }

    @Test
    public void testReadWriteTransactionWithSingleShard() throws Exception {
        initDatastoresWithCars("testReadWriteTransactionWithSingleShard");

        DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();
        assertNotNull("newReadWriteTransaction returned null", rwTx);

        rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        rwTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        MapEntryNode car1 = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        rwTx.merge(CarsModel.newCarPath("optima"), car1);

        verifyCars(rwTx, car1);

        MapEntryNode car2 = CarsModel.newCarEntry("sportage", BigInteger.valueOf(25000));
        YangInstanceIdentifier car2Path = CarsModel.newCarPath("sportage");
        rwTx.merge(car2Path, car2);

        verifyExists(rwTx, car2Path);

        followerTestKit.doCommit(rwTx.ready());

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car1, car2);
    }

    @Test
    public void testWriteTransactionWithMultipleShards() throws Exception {
        initDatastoresWithCarsAndPeople("testWriteTransactionWithMultipleShards");

        DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        YangInstanceIdentifier carsPath = CarsModel.BASE_PATH;
        NormalizedNode<?, ?> carsNode = CarsModel.emptyContainer();
        writeTx.write(carsPath, carsNode);

        YangInstanceIdentifier peoplePath = PeopleModel.BASE_PATH;
        NormalizedNode<?, ?> peopleNode = PeopleModel.emptyContainer();
        writeTx.write(peoplePath, peopleNode);

        followerTestKit.doCommit(writeTx.ready());

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();

        verifyNode(readTx, carsPath, carsNode);
        verifyNode(readTx, peoplePath, peopleNode);
    }

    @Test
    public void testReadWriteTransactionWithMultipleShards() throws Exception {
        initDatastoresWithCarsAndPeople("testReadWriteTransactionWithMultipleShards");

        DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();
        assertNotNull("newReadWriteTransaction returned null", rwTx);

        YangInstanceIdentifier carsPath = CarsModel.BASE_PATH;
        NormalizedNode<?, ?> carsNode = CarsModel.emptyContainer();
        rwTx.write(carsPath, carsNode);

        YangInstanceIdentifier peoplePath = PeopleModel.BASE_PATH;
        NormalizedNode<?, ?> peopleNode = PeopleModel.emptyContainer();
        rwTx.write(peoplePath, peopleNode);

        followerTestKit.doCommit(rwTx.ready());

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();

        verifyNode(readTx, carsPath, carsNode);
        verifyNode(readTx, peoplePath, peopleNode);
    }

    @Test
    public void testTransactionChainWithSingleShard() throws Exception {
        initDatastoresWithCars("testTransactionChainWithSingleShard");

        DOMStoreTransactionChain txChain = followerDistributedDataStore.createTransactionChain();

        // Add the top-level cars container with write-only.

        DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        writeTx.ready();

        // Verify the top-level cars container with read-only.

        verifyNode(txChain.newReadOnlyTransaction(), CarsModel.BASE_PATH, CarsModel.emptyContainer());

        // Perform car operations with read-write.

        DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

        verifyNode(rwTx, CarsModel.BASE_PATH, CarsModel.emptyContainer());

        rwTx.merge(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        MapEntryNode car1 = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        YangInstanceIdentifier car1Path = CarsModel.newCarPath("optima");
        rwTx.write(car1Path, car1);

        verifyExists(rwTx, car1Path);

        verifyCars(rwTx, car1);

        MapEntryNode car2 = CarsModel.newCarEntry("sportage", BigInteger.valueOf(25000));
        rwTx.merge(CarsModel.newCarPath("sportage"), car2);

        rwTx.delete(car1Path);

        followerTestKit.doCommit(rwTx.ready());

        txChain.close();

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car2);
    }

    @Test
    public void testTransactionChainWithMultipleShards() throws Exception{
        initDatastoresWithCarsAndPeople("testTransactionChainWithMultipleShards");

        DOMStoreTransactionChain txChain = followerDistributedDataStore.createTransactionChain();

        DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
        writeTx.write(PeopleModel.PERSON_LIST_PATH, PeopleModel.newPersonMapNode());

        followerTestKit.doCommit(writeTx.ready());

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

        writeTx.delete(personPath);

        DOMStoreThreePhaseCommitCohort cohort3 = writeTx.ready();

        followerTestKit.doCommit(cohort2);
        followerTestKit.doCommit(cohort3);

        txChain.close();

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        verifyCars(readTx, car);

        optional = readTx.read(personPath).get(5, TimeUnit.SECONDS);
        assertEquals("isPresent", false, optional.isPresent());
    }

    @Test
    public void testChainedTransactionFailureWithSingleShard() throws Exception {
        initDatastoresWithCars("testChainedTransactionFailureWithSingleShard");

        ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                ImmutableMap.<LogicalDatastoreType, DOMStore>builder().put(
                        LogicalDatastoreType.CONFIGURATION, followerDistributedDataStore).build(),
                        MoreExecutors.directExecutor());

        TransactionChainListener listener = Mockito.mock(TransactionChainListener.class);
        DOMTransactionChain txChain = broker.createTransactionChain(listener);

        DOMDataWriteTransaction writeTx = txChain.newWriteOnlyTransaction();

        ContainerNode invalidData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(CarsModel.BASE_QNAME)).
                    withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk")).build();

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
    }

    @Test
    public void testChainedTransactionFailureWithMultipleShards() throws Exception {
        initDatastoresWithCarsAndPeople("testChainedTransactionFailureWithMultipleShards");

        ConcurrentDOMDataBroker broker = new ConcurrentDOMDataBroker(
                ImmutableMap.<LogicalDatastoreType, DOMStore>builder().put(
                        LogicalDatastoreType.CONFIGURATION, followerDistributedDataStore).build(),
                        MoreExecutors.directExecutor());

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
    }

    @Test
    public void testSingleShardTransactionsWithLeaderChanges() throws Exception {
        String testName = "testSingleShardTransactionsWithLeaderChanges";
        initDatastoresWithCars(testName);

        String followerCarShardName = "member-2-shard-cars-" + testName;
        InMemoryJournal.addWriteMessagesCompleteLatch(followerCarShardName, 1, ApplyJournalEntries.class );

        // Write top-level car container from the follower so it uses a remote Tx.

        DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        followerTestKit.doCommit(writeTx.ready());

        InMemoryJournal.waitForWriteMessagesComplete(followerCarShardName);

        // Switch the leader to the follower

        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder.
                shardElectionTimeoutFactor(1).customRaftPolicyImplementation(null));

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);

        followerTestKit.waitUntilNoLeader(followerDistributedDataStore.getActorContext(), CARS);

        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_2_ADDRESS);

        DatastoreContext.Builder newMember1Builder = DatastoreContext.newBuilder().
                shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);
        IntegrationTestKit newMember1TestKit = new IntegrationTestKit(leaderSystem, newMember1Builder);
        newMember1TestKit.setupDistributedDataStore(testName, MODULE_SHARDS_CARS_ONLY_1_2, false, CARS);

        followerTestKit.waitUntilLeader(followerDistributedDataStore.getActorContext(), CARS);

        // Write a car entry to the new leader - should switch to local Tx

        writeTx = followerDistributedDataStore.newWriteOnlyTransaction();

        MapEntryNode car1 = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        YangInstanceIdentifier car1Path = CarsModel.newCarPath("optima");
        writeTx.merge(car1Path, car1);

        followerTestKit.doCommit(writeTx.ready());

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadyLocalTransactionForwardedToLeader() throws Exception {
        initDatastoresWithCars("testReadyLocalTransactionForwardedToLeader");
        followerTestKit.waitUntilLeader(followerDistributedDataStore.getActorContext(), "cars");

        Optional<ActorRef> carsFollowerShard = followerDistributedDataStore.getActorContext().findLocalShard("cars");
        assertEquals("Cars follower shard found", true, carsFollowerShard.isPresent());

        TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
        dataTree.setSchemaContext(SchemaContextHelper.full());

        // Send a tx with immediate commit.

        DataTreeModification modification = dataTree.takeSnapshot().newModification();
        new WriteModification(CarsModel.BASE_PATH, CarsModel.emptyContainer()).apply(modification);
        new MergeModification(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode()).apply(modification);

        MapEntryNode car1 = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        new WriteModification(CarsModel.newCarPath("optima"), car1).apply(modification);
        modification.ready();

        ReadyLocalTransaction readyLocal = new ReadyLocalTransaction("tx-1" , modification, true);

        carsFollowerShard.get().tell(readyLocal, followerTestKit.getRef());
        Object resp = followerTestKit.expectMsgClass(Object.class);
        if(resp instanceof akka.actor.Status.Failure) {
            throw new AssertionError("Unexpected failure response", ((akka.actor.Status.Failure)resp).cause());
        }

        assertEquals("Response type", CommitTransactionReply.class, resp.getClass());

        verifyCars(leaderDistributedDataStore.newReadOnlyTransaction(), car1);

        // Send another tx without immediate commit.

        modification = dataTree.takeSnapshot().newModification();
        MapEntryNode car2 = CarsModel.newCarEntry("sportage", BigInteger.valueOf(30000));
        new WriteModification(CarsModel.newCarPath("sportage"), car2).apply(modification);
        modification.ready();

        readyLocal = new ReadyLocalTransaction("tx-2" , modification, false);

        carsFollowerShard.get().tell(readyLocal, followerTestKit.getRef());
        resp = followerTestKit.expectMsgClass(Object.class);
        if(resp instanceof akka.actor.Status.Failure) {
            throw new AssertionError("Unexpected failure response", ((akka.actor.Status.Failure)resp).cause());
        }

        assertEquals("Response type", ReadyTransactionReply.class, resp.getClass());

        ActorSelection txActor = leaderDistributedDataStore.getActorContext().actorSelection(
                ((ReadyTransactionReply)resp).getCohortPath());

        Supplier<Short> versionSupplier = Mockito.mock(Supplier.class);
        Mockito.doReturn(DataStoreVersions.CURRENT_VERSION).when(versionSupplier).get();
        ThreePhaseCommitCohortProxy cohort = new ThreePhaseCommitCohortProxy(
                leaderDistributedDataStore.getActorContext(), Arrays.asList(
                        new ThreePhaseCommitCohortProxy.CohortInfo(Futures.successful(txActor), versionSupplier)), "tx-2");
        cohort.canCommit().get(5, TimeUnit.SECONDS);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);

        verifyCars(leaderDistributedDataStore.newReadOnlyTransaction(), car1, car2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForwardedReadyTransactionForwardedToLeader() throws Exception {
        initDatastoresWithCars("testForwardedReadyTransactionForwardedToLeader");
        followerTestKit.waitUntilLeader(followerDistributedDataStore.getActorContext(), "cars");

        Optional<ActorRef> carsFollowerShard = followerDistributedDataStore.getActorContext().findLocalShard("cars");
        assertEquals("Cars follower shard found", true, carsFollowerShard.isPresent());

        carsFollowerShard.get().tell(GetShardDataTree.INSTANCE, followerTestKit.getRef());
        DataTree dataTree = followerTestKit.expectMsgClass(DataTree.class);

        // Send a tx with immediate commit.

        DataTreeModification modification = dataTree.takeSnapshot().newModification();
        new WriteModification(CarsModel.BASE_PATH, CarsModel.emptyContainer()).apply(modification);
        new MergeModification(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode()).apply(modification);

        MapEntryNode car1 = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        new WriteModification(CarsModel.newCarPath("optima"), car1).apply(modification);

        ForwardedReadyTransaction forwardedReady = new ForwardedReadyTransaction("tx-1",
                DataStoreVersions.CURRENT_VERSION, new ReadWriteShardDataTreeTransaction(
                        Mockito.mock(ShardDataTreeTransactionParent.class), "tx-1", modification), true);

        carsFollowerShard.get().tell(forwardedReady, followerTestKit.getRef());
        Object resp = followerTestKit.expectMsgClass(Object.class);
        if(resp instanceof akka.actor.Status.Failure) {
            throw new AssertionError("Unexpected failure response", ((akka.actor.Status.Failure)resp).cause());
        }

        assertEquals("Response type", CommitTransactionReply.class, resp.getClass());

        verifyCars(leaderDistributedDataStore.newReadOnlyTransaction(), car1);

        // Send another tx without immediate commit.

        modification = dataTree.takeSnapshot().newModification();
        MapEntryNode car2 = CarsModel.newCarEntry("sportage", BigInteger.valueOf(30000));
        new WriteModification(CarsModel.newCarPath("sportage"), car2).apply(modification);

        forwardedReady = new ForwardedReadyTransaction("tx-2",
                DataStoreVersions.CURRENT_VERSION, new ReadWriteShardDataTreeTransaction(
                        Mockito.mock(ShardDataTreeTransactionParent.class), "tx-2", modification), false);

        carsFollowerShard.get().tell(forwardedReady, followerTestKit.getRef());
        resp = followerTestKit.expectMsgClass(Object.class);
        if(resp instanceof akka.actor.Status.Failure) {
            throw new AssertionError("Unexpected failure response", ((akka.actor.Status.Failure)resp).cause());
        }

        assertEquals("Response type", ReadyTransactionReply.class, resp.getClass());

        ActorSelection txActor = leaderDistributedDataStore.getActorContext().actorSelection(
                ((ReadyTransactionReply)resp).getCohortPath());

        Supplier<Short> versionSupplier = Mockito.mock(Supplier.class);
        Mockito.doReturn(DataStoreVersions.CURRENT_VERSION).when(versionSupplier).get();
        ThreePhaseCommitCohortProxy cohort = new ThreePhaseCommitCohortProxy(
                leaderDistributedDataStore.getActorContext(), Arrays.asList(
                        new ThreePhaseCommitCohortProxy.CohortInfo(Futures.successful(txActor), versionSupplier)), "tx-2");
        cohort.canCommit().get(5, TimeUnit.SECONDS);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);

        verifyCars(leaderDistributedDataStore.newReadOnlyTransaction(), car1, car2);
    }

    @Test
    public void testTransactionForwardedToLeaderAfterRetry() throws Exception {
        followerDatastoreContextBuilder.shardBatchedModificationCount(2);
        leaderDatastoreContextBuilder.shardBatchedModificationCount(2);
        initDatastoresWithCarsAndPeople("testTransactionForwardedToLeaderAfterRetry");

        // Do an initial write to get the primary shard info cached.

        DOMStoreWriteTransaction initialWriteTx = followerDistributedDataStore.newWriteOnlyTransaction();
        initialWriteTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        initialWriteTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());
        followerTestKit.doCommit(initialWriteTx.ready());

        // Wait for the commit to be replicated to the follower.

        MemberNode.verifyRaftState(followerDistributedDataStore, "cars", new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertEquals("getLastApplied", 0, raftState.getLastApplied());
            }
        });

        // Prepare, ready and canCommit a WO tx that writes to 2 shards. This will become the current tx in
        // the leader shard.

        DOMStoreWriteTransaction writeTx1 = followerDistributedDataStore.newWriteOnlyTransaction();
        writeTx1.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
        writeTx1.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());
        DOMStoreThreePhaseCommitCohort writeTx1Cohort = writeTx1.ready();
        ListenableFuture<Boolean> writeTx1CanCommit = writeTx1Cohort.canCommit();
        writeTx1CanCommit.get(5, TimeUnit.SECONDS);

        // Prepare and ready another WO tx that writes to 2 shards but don't canCommit yet. This will be queued
        // in the leader shard.

        DOMStoreWriteTransaction writeTx2 = followerDistributedDataStore.newWriteOnlyTransaction();
        LinkedList<MapEntryNode> cars = new LinkedList<>();
        int carIndex = 1;
        cars.add(CarsModel.newCarEntry("car" + carIndex, BigInteger.valueOf(carIndex)));
        writeTx2.write(CarsModel.newCarPath("car" + carIndex), cars.getLast());
        carIndex++;
        NormalizedNode<?, ?> people = PeopleModel.newPersonMapNode();
        writeTx2.write(PeopleModel.PERSON_LIST_PATH, people);
        DOMStoreThreePhaseCommitCohort writeTx2Cohort = writeTx2.ready();

        // Prepare another WO that writes to a single shard and thus will be directly committed on ready. This
        // tx writes 5 cars so 2 BatchedModidifications messages will be sent initially and cached in the
        // leader shard (with shardBatchedModificationCount set to 2). The 3rd BatchedModidifications will be
        // sent on ready.

        DOMStoreWriteTransaction writeTx3 = followerDistributedDataStore.newWriteOnlyTransaction();
        for(int i = 1; i <= 5; i++, carIndex++) {
            cars.add(CarsModel.newCarEntry("car" + carIndex, BigInteger.valueOf(carIndex)));
            writeTx3.write(CarsModel.newCarPath("car" + carIndex), cars.getLast());
        }

        // Prepare another WO that writes to a single shard. This will send a single BatchedModidifications
        // message on ready.

        DOMStoreWriteTransaction writeTx4 = followerDistributedDataStore.newWriteOnlyTransaction();
        cars.add(CarsModel.newCarEntry("car" + carIndex, BigInteger.valueOf(carIndex)));
        writeTx4.write(CarsModel.newCarPath("car" + carIndex), cars.getLast());
        carIndex++;

        // Prepare a RW tx that will create a tx actor and send a ForwardedReadyTransaciton message to the
        // leader shard on ready.

        DOMStoreReadWriteTransaction readWriteTx = followerDistributedDataStore.newReadWriteTransaction();
        cars.add(CarsModel.newCarEntry("car" + carIndex, BigInteger.valueOf(carIndex)));
        readWriteTx.write(CarsModel.newCarPath("car" + carIndex), cars.getLast());

        IntegrationTestKit.verifyShardStats(leaderDistributedDataStore, "cars", new ShardStatsVerifier() {
            @Override
            public void verify(ShardStats stats) {
                assertEquals("getReadWriteTransactionCount", 1, stats.getReadWriteTransactionCount());
            }
        });

        // Disable elections on the leader so it switches to follower.

        sendDatastoreContextUpdate(leaderDistributedDataStore, leaderDatastoreContextBuilder.
                customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName()).
                shardElectionTimeoutFactor(10));

        leaderTestKit.waitUntilNoLeader(leaderDistributedDataStore.getActorContext(), "cars");

        // Submit all tx's - the messages should get queued for retry.

        ListenableFuture<Boolean> writeTx2CanCommit = writeTx2Cohort.canCommit();
        DOMStoreThreePhaseCommitCohort writeTx3Cohort = writeTx3.ready();
        DOMStoreThreePhaseCommitCohort writeTx4Cohort = writeTx4.ready();
        DOMStoreThreePhaseCommitCohort rwTxCohort = readWriteTx.ready();

        // Enable elections on the other follower so it becomes the leader, at which point the
        // tx's should get forwarded from the previous leader to the new leader to complete the commits.

        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder.
                customRaftPolicyImplementation(null).shardElectionTimeoutFactor(1));

        followerTestKit.doCommit(writeTx1CanCommit, writeTx1Cohort);
        followerTestKit.doCommit(writeTx2CanCommit, writeTx2Cohort);
        followerTestKit.doCommit(writeTx3Cohort);
        followerTestKit.doCommit(writeTx4Cohort);
        followerTestKit.doCommit(rwTxCohort);

        DOMStoreReadTransaction readTx = leaderDistributedDataStore.newReadOnlyTransaction();
        verifyCars(readTx, cars.toArray(new MapEntryNode[cars.size()]));
        verifyNode(readTx, PeopleModel.PERSON_LIST_PATH, people);
    }

    @Test
    public void testLeadershipTransferOnShutdown() throws Exception {
        leaderDatastoreContextBuilder.shardBatchedModificationCount(1);
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(10).customRaftPolicyImplementation(null);
        String testName = "testLeadershipTransferOnShutdown";
        initDatastores(testName, MODULE_SHARDS_CARS_PEOPLE_1_2_3, CARS_AND_PEOPLE);

        IntegrationTestKit follower2TestKit = new IntegrationTestKit(follower2System, followerDatastoreContextBuilder);
        DistributedDataStore follower2DistributedDataStore = follower2TestKit.setupDistributedDataStore(testName,
                MODULE_SHARDS_CARS_PEOPLE_1_2_3, false);

        // Create and submit a couple tx's so they're pending.

        DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();
        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
        writeTx.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());
        DOMStoreThreePhaseCommitCohort cohort1 = writeTx.ready();

        IntegrationTestKit.verifyShardStats(leaderDistributedDataStore, "cars", new ShardStatsVerifier() {
            @Override
            public void verify(ShardStats stats) {
                assertEquals("getTxCohortCacheSize", 1, stats.getTxCohortCacheSize());
            }
        });

        writeTx = followerDistributedDataStore.newWriteOnlyTransaction();
        MapEntryNode car = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        writeTx.write(CarsModel.newCarPath("optima"), car);
        DOMStoreThreePhaseCommitCohort cohort2 = writeTx.ready();

        IntegrationTestKit.verifyShardStats(leaderDistributedDataStore, "cars", new ShardStatsVerifier() {
            @Override
            public void verify(ShardStats stats) {
                assertEquals("getTxCohortCacheSize", 2, stats.getTxCohortCacheSize());
            }
        });

        // Gracefully stop the leader via a Shutdown message.

        sendDatastoreContextUpdate(leaderDistributedDataStore, leaderDatastoreContextBuilder.
                shardElectionTimeoutFactor(100));

        FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
        Future<ActorRef> future = leaderDistributedDataStore.getActorContext().findLocalShardAsync("cars");
        ActorRef leaderActor = Await.result(future, duration);

        Future<Boolean> stopFuture = Patterns.gracefulStop(leaderActor, duration, Shutdown.INSTANCE);

        // Commit the 2 transactions. They should finish and succeed.

        followerTestKit.doCommit(cohort1);
        followerTestKit.doCommit(cohort2);

        // Wait for the leader actor stopped.

        Boolean stopped = Await.result(stopFuture, duration);
        assertEquals("Stopped", Boolean.TRUE, stopped);

        // Verify leadership was transferred by reading the committed data from the other nodes.

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car);
        verifyCars(follower2DistributedDataStore.newReadOnlyTransaction(), car);
    }

    @Test
    public void testTransactionWithIsolatedLeader() throws Throwable {
        leaderDatastoreContextBuilder.shardIsolatedLeaderCheckIntervalInMillis(200);
        String testName = "testTransactionWithIsolatedLeader";
        initDatastoresWithCars(testName);

        DOMStoreWriteTransaction failWriteTx = leaderDistributedDataStore.newWriteOnlyTransaction();
        failWriteTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        DOMStoreWriteTransaction successWriteTx = leaderDistributedDataStore.newWriteOnlyTransaction();
        successWriteTx.merge(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        followerTestKit.watch(followerDistributedDataStore.getActorContext().getShardManager());
        followerDistributedDataStore.close();
        followerTestKit.expectTerminated(followerDistributedDataStore.getActorContext().getShardManager());

        MemberNode.verifyRaftState(leaderDistributedDataStore, "cars", new RaftStateVerifier() {
            @Override
            public void verify(OnDemandRaftState raftState) {
                assertEquals("getRaftState", "IsolatedLeader", raftState.getRaftState());
            }
        });

        try {
            leaderTestKit.doCommit(failWriteTx.ready());
            fail("Expected NoShardLeaderException");
        } catch (ExecutionException e) {
            assertEquals("getCause", NoShardLeaderException.class, e.getCause().getClass());
        }

        sendDatastoreContextUpdate(leaderDistributedDataStore, leaderDatastoreContextBuilder.
                shardElectionTimeoutFactor(100));

        DOMStoreThreePhaseCommitCohort writeTxCohort = successWriteTx.ready();

        followerDistributedDataStore = followerTestKit.setupDistributedDataStore(testName,
                MODULE_SHARDS_CARS_ONLY_1_2, false, CARS);

        leaderTestKit.doCommit(writeTxCohort);
    }

    @Test(expected=AskTimeoutException.class)
    public void testTransactionWithShardLeaderNotResponding() throws Throwable {
        initDatastoresWithCars("testTransactionWithShardLeaderNotResponding");

        // Do an initial read to get the primary shard info cached.

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        readTx.read(CarsModel.BASE_PATH).checkedGet(5, TimeUnit.SECONDS);

        // Shutdown the leader and try to create a new tx.

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);

        followerDatastoreContextBuilder.operationTimeoutInMillis(50).shardElectionTimeoutFactor(1);
        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder);

        DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();

        rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        try {
            followerTestKit.doCommit(rwTx.ready());
        } catch (ExecutionException e) {
            assertTrue("Expected ShardLeaderNotRespondingException cause. Actual: " + e.getCause(),
                    e.getCause() instanceof ShardLeaderNotRespondingException);
            assertNotNull("Expected a nested cause", e.getCause().getCause());
            throw e.getCause().getCause();
        }
    }

    @Test(expected=NoShardLeaderException.class)
    public void testTransactionWithCreateTxFailureDueToNoLeader() throws Throwable {
        initDatastoresWithCars("testTransactionWithCreateTxFailureDueToNoLeader");

        // Do an initial read to get the primary shard info cached.

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        readTx.read(CarsModel.BASE_PATH).checkedGet(5, TimeUnit.SECONDS);

        // Shutdown the leader and try to create a new tx.

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);

        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder.
                operationTimeoutInMillis(10).shardElectionTimeoutFactor(1).customRaftPolicyImplementation(null));

        DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();

        rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        try {
            followerTestKit.doCommit(rwTx.ready());
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testTransactionRetryWithInitialAskTimeoutExOnCreateTx() throws Exception {
        String testName = "testTransactionRetryWithInitialAskTimeoutExOnCreateTx";
        initDatastores(testName, MODULE_SHARDS_CARS_PEOPLE_1_2_3, CARS);

        DatastoreContext.Builder follower2DatastoreContextBuilder = DatastoreContext.newBuilder().
                shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);
        IntegrationTestKit follower2TestKit = new IntegrationTestKit(follower2System, follower2DatastoreContextBuilder);
        follower2TestKit.setupDistributedDataStore(testName, MODULE_SHARDS_CARS_PEOPLE_1_2_3, false, CARS);

        // Do an initial read to get the primary shard info cached.

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        readTx.read(CarsModel.BASE_PATH).checkedGet(5, TimeUnit.SECONDS);

        // Shutdown the leader and try to create a new tx.

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);

        sendDatastoreContextUpdate(followerDistributedDataStore, followerDatastoreContextBuilder.
                operationTimeoutInMillis(500).shardElectionTimeoutFactor(1).customRaftPolicyImplementation(null));

        DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();

        rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        followerTestKit.doCommit(rwTx.ready());
    }

    private static void sendDatastoreContextUpdate(DistributedDataStore dataStore, final Builder builder) {
        final Builder newBuilder = DatastoreContext.newBuilderFrom(builder.build());
        DatastoreContextFactory mockContextFactory = Mockito.mock(DatastoreContextFactory.class);
        Answer<DatastoreContext> answer = new Answer<DatastoreContext>() {
            @Override
            public DatastoreContext answer(InvocationOnMock invocation) {
                return newBuilder.build();
            }
        };
        Mockito.doAnswer(answer).when(mockContextFactory).getBaseDatastoreContext();
        Mockito.doAnswer(answer).when(mockContextFactory).getShardDatastoreContext(Mockito.anyString());
        dataStore.onDatastoreContextUpdated(mockContextFactory);
    }
}
