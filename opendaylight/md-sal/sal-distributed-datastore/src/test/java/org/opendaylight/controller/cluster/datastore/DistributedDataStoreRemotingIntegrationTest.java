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
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.pattern.AskTimeoutException;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.ShardLeaderNotRespondingException;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

/**
 * End-to-end distributed data store tests that exercise remote shards and transactions.
 *
 * @author Thomas Pantelis
 */
public class DistributedDataStoreRemotingIntegrationTest {

    private static final String[] SHARD_NAMES = {"cars", "people"};

    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
    private static final Address MEMBER_2_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2559");

    private static final String MODULE_SHARDS_CONFIG_2 = "module-shards-member1-and-2.conf";
    private static final String MODULE_SHARDS_CONFIG_3 = "module-shards-member1-and-2-and-3.conf";

    private ActorSystem leaderSystem;
    private ActorSystem followerSystem;
    private ActorSystem follower2System;

    private final DatastoreContext.Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(1);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);

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

    private void initDatastores(String type) {
        initDatastores(type, MODULE_SHARDS_CONFIG_2);
    }

    private void initDatastores(String type, String moduleShardsConfig) {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);

        leaderDistributedDataStore = leaderTestKit.setupDistributedDataStore(type, moduleShardsConfig, false, SHARD_NAMES);

        followerTestKit = new IntegrationTestKit(followerSystem, followerDatastoreContextBuilder);
        followerDistributedDataStore = followerTestKit.setupDistributedDataStore(type, moduleShardsConfig, false, SHARD_NAMES);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(), SHARD_NAMES);
    }

    private void verifyCars(DOMStoreReadTransaction readTx, MapEntryNode... entries) throws Exception {
        Optional<NormalizedNode<?, ?>> optional = readTx.read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
        assertEquals("isPresent", true, optional.isPresent());

        CollectionNodeBuilder<MapEntryNode, MapNode> listBuilder = ImmutableNodes.mapNodeBuilder(CarsModel.CAR_QNAME);
        for(NormalizedNode<?, ?> entry: entries) {
            listBuilder.withChild((MapEntryNode) entry);
        }

        assertEquals("Car list node", listBuilder.build(), optional.get());
    }

    private void verifyNode(DOMStoreReadTransaction readTx, YangInstanceIdentifier path, NormalizedNode<?, ?> expNode)
            throws Exception {
        Optional<NormalizedNode<?, ?>> optional = readTx.read(path).get(5, TimeUnit.SECONDS);
        assertEquals("isPresent", true, optional.isPresent());
        assertEquals("Data node", expNode, optional.get());
    }

    private void verifyExists(DOMStoreReadTransaction readTx, YangInstanceIdentifier path) throws Exception {
        Boolean exists = readTx.exists(path).get(5, TimeUnit.SECONDS);
        assertEquals("exists", true, exists);
    }

    @Test
    public void testWriteTransactionWithSingleShard() throws Exception {
        String testName = "testWriteTransactionWithSingleShard";
        initDatastores(testName);

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
                setupDistributedDataStore(testName, "module-shards-member2", true, SHARD_NAMES);

        verifyCars(member2Datastore.newReadOnlyTransaction(), car2);

        JavaTestKit.shutdownActorSystem(newSystem);
    }

    @Test
    public void testReadWriteTransactionWithSingleShard() throws Exception {
        initDatastores("testReadWriteTransactionWithSingleShard");

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
        initDatastores("testWriteTransactionWithMultipleShards");

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
        initDatastores("testReadWriteTransactionWithMultipleShards");

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
        initDatastores("testTransactionChainWithSingleShard");

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
        initDatastores("testTransactionChainWithMultipleShards");

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
        initDatastores("testChainedTransactionFailureWithSingleShard");

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
        initDatastores("testChainedTransactionFailureWithMultipleShards");

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
        initDatastores(testName);

        String followerCarShardName = "member-2-shard-cars-" + testName;
        InMemoryJournal.addWriteMessagesCompleteLatch(followerCarShardName, 1, ApplyJournalEntries.class );

        // Write top-level car container from the follower so it uses a remote Tx.

        DOMStoreWriteTransaction writeTx = followerDistributedDataStore.newWriteOnlyTransaction();

        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());

        followerTestKit.doCommit(writeTx.ready());

        InMemoryJournal.waitForWriteMessagesComplete(followerCarShardName);

        // Switch the leader to the follower

        followerDatastoreContextBuilder.shardElectionTimeoutFactor(1);
        followerDistributedDataStore.onDatastoreContextUpdated(followerDatastoreContextBuilder.build());

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);

        followerTestKit.waitUntilNoLeader(followerDistributedDataStore.getActorContext(), SHARD_NAMES);

        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_2_ADDRESS);

        DatastoreContext.Builder newMember1Builder = DatastoreContext.newBuilder().
                shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);
        IntegrationTestKit newMember1TestKit = new IntegrationTestKit(leaderSystem, newMember1Builder);
        newMember1TestKit.setupDistributedDataStore(testName, MODULE_SHARDS_CONFIG_2, false, SHARD_NAMES);

        followerTestKit.waitUntilLeader(followerDistributedDataStore.getActorContext(), SHARD_NAMES);

        // Write a car entry to the new leader - should switch to local Tx

        writeTx = followerDistributedDataStore.newWriteOnlyTransaction();

        MapEntryNode car1 = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        YangInstanceIdentifier car1Path = CarsModel.newCarPath("optima");
        writeTx.merge(car1Path, car1);

        followerTestKit.doCommit(writeTx.ready());

        verifyCars(followerDistributedDataStore.newReadOnlyTransaction(), car1);
    }

    @Test
    public void testReadyLocalTransactionForwardedToLeader() throws Exception {
        initDatastores("testReadyLocalTransactionForwardedToLeader");

        Optional<ActorRef> carsFollowerShard = followerDistributedDataStore.getActorContext().findLocalShard("cars");
        assertEquals("Cars follower shard found", true, carsFollowerShard.isPresent());

        TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create();
        dataTree.setSchemaContext(SchemaContextHelper.full());
        DataTreeModification modification = dataTree.takeSnapshot().newModification();

        new WriteModification(CarsModel.BASE_PATH, CarsModel.emptyContainer()).apply(modification);
        new MergeModification(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode()).apply(modification);

        MapEntryNode car = CarsModel.newCarEntry("optima", BigInteger.valueOf(20000));
        new WriteModification(CarsModel.newCarPath("optima"), car).apply(modification);

        String transactionID = "tx-1";
        ReadyLocalTransaction readyLocal = new ReadyLocalTransaction(transactionID , modification, true);

        carsFollowerShard.get().tell(readyLocal, followerTestKit.getRef());
        followerTestKit.expectMsgClass(CommitTransactionReply.SERIALIZABLE_CLASS);

        verifyCars(leaderDistributedDataStore.newReadOnlyTransaction(), car);
    }

    @Test(expected=NoShardLeaderException.class)
    public void testTransactionWithIsolatedLeader() throws Throwable {
        leaderDatastoreContextBuilder.shardIsolatedLeaderCheckIntervalInMillis(300);
        String testName = "testTransactionWithIsolatedLeader";
        initDatastores(testName);

        JavaTestKit.shutdownActorSystem(followerSystem, null, true);

        Uninterruptibles.sleepUninterruptibly(leaderDistributedDataStore.getActorContext().getDatastoreContext()
                .getShardRaftConfig().getElectionTimeOutInterval().toMillis() * 3, TimeUnit.MILLISECONDS);

        DOMStoreWriteTransaction writeTx = leaderDistributedDataStore.newWriteOnlyTransaction();
        writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        try {
            followerTestKit.doCommit(writeTx.ready());
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected=AskTimeoutException.class)
    public void testTransactionWithShardLeaderNotResponding() throws Throwable {
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(30);
        initDatastores("testTransactionWithShardLeaderNotResponding");

        // Do an initial read to get the primary shard info cached.

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        readTx.read(CarsModel.BASE_PATH).checkedGet(5, TimeUnit.SECONDS);

        // Shutdown the leader and try to create a new tx.

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);

        followerDatastoreContextBuilder.operationTimeoutInMillis(50).shardElectionTimeoutFactor(1);
        followerDistributedDataStore.onDatastoreContextUpdated(followerDatastoreContextBuilder.build());

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
        initDatastores("testTransactionWithCreateTxFailureDueToNoLeader");

        // Do an initial read to get the primary shard info cached.

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        readTx.read(CarsModel.BASE_PATH).checkedGet(5, TimeUnit.SECONDS);

        // Shutdown the leader and try to create a new tx.

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);

        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

        followerDatastoreContextBuilder.operationTimeoutInMillis(10).shardElectionTimeoutFactor(1);
        followerDistributedDataStore.onDatastoreContextUpdated(followerDatastoreContextBuilder.build());

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
        followerDatastoreContextBuilder.shardElectionTimeoutFactor(30);
        String testName = "testTransactionRetryWithInitialAskTimeoutExOnCreateTx";
        initDatastores(testName, MODULE_SHARDS_CONFIG_3);

        DatastoreContext.Builder follower2DatastoreContextBuilder = DatastoreContext.newBuilder().
                shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);
        IntegrationTestKit follower2TestKit = new IntegrationTestKit(follower2System, follower2DatastoreContextBuilder);
        follower2TestKit.setupDistributedDataStore(testName, MODULE_SHARDS_CONFIG_3, false, SHARD_NAMES);

        // Do an initial read to get the primary shard info cached.

        DOMStoreReadTransaction readTx = followerDistributedDataStore.newReadOnlyTransaction();
        readTx.read(CarsModel.BASE_PATH).checkedGet(5, TimeUnit.SECONDS);

        // Shutdown the leader and try to create a new tx.

        JavaTestKit.shutdownActorSystem(leaderSystem, null, true);

        followerDatastoreContextBuilder.operationTimeoutInMillis(500);
        followerDistributedDataStore.onDatastoreContextUpdated(followerDatastoreContextBuilder.build());

        DOMStoreReadWriteTransaction rwTx = followerDistributedDataStore.newReadWriteTransaction();

        rwTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());

        followerTestKit.doCommit(rwTx.ready());
    }
}
