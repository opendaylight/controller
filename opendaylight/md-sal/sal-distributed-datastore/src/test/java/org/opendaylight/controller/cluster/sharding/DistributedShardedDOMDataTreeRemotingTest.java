/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

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
import akka.actor.PoisonPill;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.testkit.JavaTestKit;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory.DistributedShardRegistration;
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

@Ignore("Needs to have the configuration backend switched from distributed-data")
public class DistributedShardedDOMDataTreeRemotingTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardedDOMDataTreeRemotingTest.class);

    private static final Address MEMBER_1_ADDRESS =
            AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558");

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private ActorSystem leaderSystem;
    private ActorSystem followerSystem;


    private final Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5)
                    .logicalStoreType(
                            org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5)
                    .logicalStoreType(
                            org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION);

    private DistributedDataStore followerDistributedDataStore;
    private DistributedDataStore leaderDistributedDataStore;
    private IntegrationTestKit followerTestKit;
    private IntegrationTestKit leaderTestKit;

    private DistributedShardedDOMDataTree leaderShardFactory;
    private DistributedShardedDOMDataTree followerShardFactory;

    private ActorSystemProvider leaderSystemProvider;
    private ActorSystemProvider followerSystemProvider;

    @Before
    public void setUp() {

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
        if (followerDistributedDataStore != null) {
            followerDistributedDataStore.close();
        }
        if (leaderDistributedDataStore != null) {
            leaderDistributedDataStore.close();
        }

        DistributedData.get(leaderSystem).replicator().tell(PoisonPill.getInstance(), ActorRef.noSender());
        DistributedData.get(followerSystem).replicator().tell(PoisonPill.getInstance(), ActorRef.noSender());

        JavaTestKit.shutdownActorSystem(leaderSystem);
        JavaTestKit.shutdownActorSystem(followerSystem);
    }

    private void initEmptyDatastores(final String type) {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);

        leaderDistributedDataStore =
                leaderTestKit.setupDistributedDataStoreWithoutConfig(type, SchemaContextHelper.full());

        followerTestKit = new IntegrationTestKit(followerSystem, followerDatastoreContextBuilder);
        followerDistributedDataStore =
                followerTestKit.setupDistributedDataStoreWithoutConfig(type, SchemaContextHelper.full());

        leaderShardFactory = new DistributedShardedDOMDataTree(leaderSystemProvider,
                leaderDistributedDataStore,
                leaderDistributedDataStore);

        followerShardFactory = new DistributedShardedDOMDataTree(followerSystemProvider,
                followerDistributedDataStore,
                followerDistributedDataStore);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(YangInstanceIdentifier.EMPTY));
    }

    @Test
    public void testProducerRegistrations() throws Exception {
        initEmptyDatastores("config");

        leaderTestKit.waitForMembersUp("member-2");

        final DistributedShardRegistration shardRegistration =
                waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                        TEST_ID, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                        DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

        final ActorRef leaderShardManager = leaderDistributedDataStore.getActorContext().getShardManager();

        assertNotNull(findLocalShard(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier())));

        assertNotNull(findLocalShard(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier())));

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
                    TEST_ID, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                    DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);
            fail("This prefix already should have a shard registration that was forwarded from the other node");
        } catch (final DOMDataTreeShardingConflictException e) {
            assertTrue(e.getMessage().contains("is already occupied by another shard"));
        }
    }

    @Test
    public void testWriteIntoMultipleShards() throws Exception {
        initEmptyDatastores("config");

        leaderTestKit.waitForMembersUp("member-2");

        LOG.debug("registering first shard");
        final DistributedShardRegistration shardRegistration =
                waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                        TEST_ID, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                        DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);


        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));
        findLocalShard(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

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

        tx.submit().checkedGet();
    }

    @Test
    public void testMultipleShardRegistrations() throws Exception {
        initEmptyDatastores("config");

        final DistributedShardRegistration reg1 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                TEST_ID, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        final DistributedShardRegistration reg2 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.OUTER_CONTAINER_PATH),
                Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        final DistributedShardRegistration reg3 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.INNER_LIST_PATH),
                Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        final DistributedShardRegistration reg4 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.JUNK_PATH),
                Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH));
        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH));
        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH));
        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH));

        // check leader has local shards
        assertNotNull(findLocalShard(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));

        assertNotNull(findLocalShard(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH)));

        assertNotNull(findLocalShard(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH)));

        assertNotNull(findLocalShard(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH)));

        // check follower has local shards
        assertNotNull(findLocalShard(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));

        assertNotNull(findLocalShard(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH)));

        assertNotNull(findLocalShard(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH)));

        assertNotNull(findLocalShard(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH)));


        LOG.debug("Closing registrations");

        reg1.close();
        reg2.close();
        reg3.close();
        reg4.close();

        waitUntilShardIsDown(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

        waitUntilShardIsDown(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH));

        waitUntilShardIsDown(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH));

        waitUntilShardIsDown(leaderDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH));

        LOG.debug("All leader shards gone");

        waitUntilShardIsDown(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

        waitUntilShardIsDown(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.OUTER_CONTAINER_PATH));

        waitUntilShardIsDown(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.INNER_LIST_PATH));

        waitUntilShardIsDown(followerDistributedDataStore.getActorContext(),
                ClusterUtils.getCleanShardName(TestModel.JUNK_PATH));

        LOG.debug("All follower shards gone");
    }

    @Test
    public void testMultipleRegistrationsAtOnePrefix() throws Exception {
        initEmptyDatastores("config");

        for (int i = 0; i < 10; i++) {
            LOG.debug("Round {}", i);
            final DistributedShardRegistration reg1 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                    TEST_ID, Lists.newArrayList(AbstractTest.MEMBER_NAME, AbstractTest.MEMBER_2_NAME)),
                    DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

            leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

            assertNotNull(findLocalShard(leaderDistributedDataStore.getActorContext(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));

            assertNotNull(findLocalShard(followerDistributedDataStore.getActorContext(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));

            waitOnAsyncTask(reg1.close(), DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

            waitUntilShardIsDown(leaderDistributedDataStore.getActorContext(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

            waitUntilShardIsDown(followerDistributedDataStore.getActorContext(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH));
        }
    }
}
