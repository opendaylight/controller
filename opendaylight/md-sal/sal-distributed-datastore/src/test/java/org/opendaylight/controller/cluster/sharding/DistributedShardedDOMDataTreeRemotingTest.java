/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.controller.cluster.datastore.IntegrationTestKit.findLocalShard;
import static org.opendaylight.controller.cluster.datastore.IntegrationTestKit.waitUntilShardIsDown;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedShardedDOMDataTreeRemotingTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardedDOMDataTreeRemotingTest.class);

    private static final Address MEMBER_1_ADDRESS =
            AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558");

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private static final String MODULE_SHARDS_CONFIG = "module-shards-default.conf";

    private ActorSystem leaderSystem;
    private ActorSystem followerSystem;


    private final Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);

    private DistributedDataStore leaderConfigDatastore;
    private DistributedDataStore leaderOperDatastore;

    private DistributedDataStore followerConfigDatastore;
    private DistributedDataStore followerOperDatastore;


    private IntegrationTestKit followerTestKit;
    private IntegrationTestKit leaderTestKit;
    private DistributedShardedDOMDataTree leaderShardFactory;

    private DistributedShardedDOMDataTree followerShardFactory;
    private ActorSystemProvider leaderSystemProvider;
    private ActorSystemProvider followerSystemProvider;

    @Before
    public void setUp() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_1_ADDRESS);

        followerSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));
        Cluster.get(followerSystem).join(MEMBER_1_ADDRESS);

        leaderSystemProvider = Mockito.mock(ActorSystemProvider.class);
        doReturn(leaderSystem).when(leaderSystemProvider).getActorSystem();

        followerSystemProvider = Mockito.mock(ActorSystemProvider.class);
        doReturn(followerSystem).when(followerSystemProvider).getActorSystem();

    }

    @After
    public void tearDown() {
        if (leaderConfigDatastore != null) {
            leaderConfigDatastore.close();
        }
        if (leaderOperDatastore != null) {
            leaderOperDatastore.close();
        }

        if (followerConfigDatastore != null) {
            followerConfigDatastore.close();
        }
        if (followerOperDatastore != null) {
            followerOperDatastore.close();
        }

        TestKit.shutdownActorSystem(leaderSystem, true);
        TestKit.shutdownActorSystem(followerSystem, true);

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    private void initEmptyDatastores() throws Exception {
        initEmptyDatastores(MODULE_SHARDS_CONFIG);
    }

    private void initEmptyDatastores(final String moduleShardsConfig) throws Exception {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);

        leaderConfigDatastore = leaderTestKit.setupDistributedDataStore(
                "config", moduleShardsConfig, true,
                SchemaContextHelper.distributedShardedDOMDataTreeSchemaContext());
        leaderOperDatastore = leaderTestKit.setupDistributedDataStore(
                "operational", moduleShardsConfig, true,
                SchemaContextHelper.distributedShardedDOMDataTreeSchemaContext());

        leaderShardFactory = new DistributedShardedDOMDataTree(leaderSystemProvider,
                leaderOperDatastore,
                leaderConfigDatastore);

        followerTestKit = new IntegrationTestKit(followerSystem, followerDatastoreContextBuilder);

        followerConfigDatastore = followerTestKit.setupDistributedDataStore(
                "config", moduleShardsConfig, true, SchemaContextHelper.distributedShardedDOMDataTreeSchemaContext());
        followerOperDatastore = followerTestKit.setupDistributedDataStore(
                "operational", moduleShardsConfig, true,
                SchemaContextHelper.distributedShardedDOMDataTreeSchemaContext());

        followerShardFactory = new DistributedShardedDOMDataTree(followerSystemProvider,
                followerOperDatastore,
                followerConfigDatastore);

        followerTestKit.waitForMembersUp("member-1");

        LOG.info("Initializing leader DistributedShardedDOMDataTree");
        leaderShardFactory.init();

        leaderTestKit.waitUntilLeader(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(YangInstanceIdentifier.empty()));

        leaderTestKit.waitUntilLeader(leaderOperDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(YangInstanceIdentifier.empty()));

        LOG.info("Initializing follower DistributedShardedDOMDataTree");
        followerShardFactory.init();
    }

    @Test
    public void testProducerRegistrations() throws Exception {
        LOG.info("testProducerRegistrations starting");
        initEmptyDatastores();

        leaderTestKit.waitForMembersUp("member-2");

        // TODO refactor shard creation and verification to own method
        final DistributedShardRegistration shardRegistration =
                waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                        TEST_ID, null, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                        DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        leaderTestKit.waitUntilLeader(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

        final ActorRef leaderShardManager = leaderConfigDatastore.getActorUtils().getShardManager();

        assertNotNull(findLocalShard(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier())));

        assertNotNull(findLocalShard(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier())));

        final Set<String> peers  = new HashSet<>();
        IntegrationTestKit.verifyShardState(leaderConfigDatastore,
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()), onDemandShardState ->
                        peers.addAll(onDemandShardState.getPeerAddresses().values()));
        assertEquals(peers.size(), 1);

        final DOMDataTreeProducer producer = leaderShardFactory.createProducer(Collections.singleton(TEST_ID));
        try {
            followerShardFactory.createProducer(Collections.singleton(TEST_ID));
            fail("Producer should be already registered on the other node");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("is attached to producer"));
        }

        producer.close();

        final DOMDataTreeProducer followerProducer =
                followerShardFactory.createProducer(Collections.singleton(TEST_ID));
        try {
            leaderShardFactory.createProducer(Collections.singleton(TEST_ID));
            fail("Producer should be already registered on the other node");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("is attached to producer"));
        }

        followerProducer.close();
        // try to create a shard on an already registered prefix on follower
        try {
            waitOnAsyncTask(followerShardFactory.createDistributedShard(
                    TEST_ID, null, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                    DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);
            fail("This prefix already should have a shard registration that was forwarded from the other node");
        } catch (final DOMDataTreeShardingConflictException e) {
            assertTrue(e.getMessage().contains("is already occupied by another shard"));
        }

        shardRegistration.close().toCompletableFuture().get();

        LOG.info("testProducerRegistrations ending");
    }

    @Test
    public void testWriteIntoMultipleShards() throws Exception {
        LOG.info("testWriteIntoMultipleShards starting");
        initEmptyDatastores();

        leaderTestKit.waitForMembersUp("member-2");

        LOG.debug("registering first shard");
        final DistributedShardRegistration shardRegistration =
                waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                        TEST_ID, null, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                        DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);


        leaderTestKit.waitUntilLeader(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));
        findLocalShard(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

        final Set<String> peers  = new HashSet<>();
        IntegrationTestKit.verifyShardState(leaderConfigDatastore,
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()), onDemandShardState ->
                        peers.addAll(onDemandShardState.getPeerAddresses().values()));
        assertEquals(peers.size(), 1);

        LOG.debug("Got after waiting for nonleader");
        final DOMDataTreeProducer producer = leaderShardFactory.createProducer(Collections.singleton(TEST_ID));

        final DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(true);
        final DOMDataTreeWriteCursor cursor = tx.createCursor(TEST_ID);
        Assert.assertNotNull(cursor);
        final YangInstanceIdentifier nameId =
                YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(TestModel.NAME_QNAME).build();
        cursor.write(nameId.getLastPathArgument(),
                ImmutableLeafNodeBuilder.<String>create().withNodeIdentifier(
                        new NodeIdentifier(TestModel.NAME_QNAME)).withValue("Test Value").build());

        cursor.close();
        LOG.warn("Got to pre submit");

        tx.commit().get();

        shardRegistration.close().toCompletableFuture().get();

        LOG.info("testWriteIntoMultipleShards ending");
    }

    @Test
    public void testMultipleShardRegistrations() throws Exception {
        LOG.info("testMultipleShardRegistrations starting");
        initEmptyDatastores();

        final DistributedShardRegistration reg1 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                TEST_ID, null, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        final DistributedShardRegistration reg2 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.OUTER_CONTAINER_PATH), null,
                Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        final DistributedShardRegistration reg3 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.INNER_LIST_PATH), null,
                Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        final DistributedShardRegistration reg4 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.JUNK_PATH), null,
                Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        leaderTestKit.waitUntilLeader(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH));
        leaderTestKit.waitUntilLeader(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH));
        leaderTestKit.waitUntilLeader(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH));
        leaderTestKit.waitUntilLeader(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH));

        // check leader has local shards
        assertNotNull(findLocalShard(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));

        assertNotNull(findLocalShard(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH)));

        assertNotNull(findLocalShard(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH)));

        assertNotNull(findLocalShard(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH)));

        // check follower has local shards
        assertNotNull(findLocalShard(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));

        assertNotNull(findLocalShard(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH)));

        assertNotNull(findLocalShard(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH)));

        assertNotNull(findLocalShard(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH)));

        LOG.debug("Closing registrations");

        reg1.close().toCompletableFuture().get();
        reg2.close().toCompletableFuture().get();
        reg3.close().toCompletableFuture().get();
        reg4.close().toCompletableFuture().get();

        waitUntilShardIsDown(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

        waitUntilShardIsDown(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH));

        waitUntilShardIsDown(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH));

        waitUntilShardIsDown(leaderConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH));

        LOG.debug("All leader shards gone");

        waitUntilShardIsDown(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

        waitUntilShardIsDown(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH));

        waitUntilShardIsDown(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH));

        waitUntilShardIsDown(followerConfigDatastore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH));

        LOG.debug("All follower shards gone");
        LOG.info("testMultipleShardRegistrations ending");
    }

    @Test
    public void testMultipleRegistrationsAtOnePrefix() throws Exception {
        LOG.info("testMultipleRegistrationsAtOnePrefix starting");
        initEmptyDatastores();

        for (int i = 0; i < 5; i++) {
            LOG.info("Round {}", i);
            final DistributedShardRegistration reg1 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                    TEST_ID, null, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                    DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

            leaderTestKit.waitUntilLeader(leaderConfigDatastore.getActorUtils(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

            assertNotNull(findLocalShard(leaderConfigDatastore.getActorUtils(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));

            assertNotNull(findLocalShard(followerConfigDatastore.getActorUtils(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));


            final Set<String> peers  = new HashSet<>();
            IntegrationTestKit.verifyShardState(leaderConfigDatastore,
                    ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()), onDemandShardState ->
                            peers.addAll(onDemandShardState.getPeerAddresses().values()));
            assertEquals(peers.size(), 1);

            waitOnAsyncTask(reg1.close(), DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

            waitUntilShardIsDown(leaderConfigDatastore.getActorUtils(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

            waitUntilShardIsDown(followerConfigDatastore.getActorUtils(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH));
        }

        LOG.info("testMultipleRegistrationsAtOnePrefix ending");
    }

    @Test
    public void testInitialBootstrappingWithNoModuleShards() throws Exception {
        LOG.info("testInitialBootstrappingWithNoModuleShards starting");
        initEmptyDatastores("module-shards-default-member-1.conf");

        // We just verify the DistributedShardedDOMDataTree initialized without error.
    }
}
