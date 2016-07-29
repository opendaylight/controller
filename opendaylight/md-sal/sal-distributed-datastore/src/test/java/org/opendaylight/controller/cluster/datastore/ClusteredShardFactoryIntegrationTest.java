/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;

public class ClusteredShardFactoryIntegrationTest extends AbstractTest {

    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
    private static final Address MEMBER_2_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2559");

    private static final DOMDataTreeIdentifier TEST_ID = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private ShardedDOMDataTree shardedDOMDataTree = new ShardedDOMDataTree();

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
        shardedDOMDataTree = new ShardedDOMDataTree();

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
            leaderDistributedDataStore.close();
        }
        if (leaderDistributedDataStore != null) {
            leaderDistributedDataStore.close();
        }

        JavaTestKit.shutdownActorSystem(leaderSystem);
        JavaTestKit.shutdownActorSystem(followerSystem);
        JavaTestKit.shutdownActorSystem(follower2System);
    }

    private void initEmptyDatastore(final String type) {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);

        leaderDistributedDataStore = leaderTestKit.setupDistributedDataStoreWithoutConfig(type, SchemaContextHelper.full());

        followerTestKit = new IntegrationTestKit(followerSystem, followerDatastoreContextBuilder);
        followerDistributedDataStore = followerTestKit.setupDistributedDataStoreWithoutConfig(type, SchemaContextHelper.full());
    }

    @Test
    public void testWriteIntoMultipleShards() throws Exception {
        initEmptyDatastore("config");

        new JavaTestKit(leaderSystem){{
            final ClusteredShardFactory clusteredShardFactory = new ClusteredShardFactory(shardedDOMDataTree,
                    Mockito.mock(DistributedDataStore.class),
                    leaderDistributedDataStore,
                    leaderSystem,
                    MEMBER_NAME);

            clusteredShardFactory.addClusteredShard(TEST_ID, Lists.newArrayList(MEMBER_NAME, MEMBER_2_NAME));

            leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(), ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

            final ActorRef leaderShardManager = leaderDistributedDataStore.getActorContext().getShardManager();

            leaderShardManager.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            final ActorRef followerShardManager = followerDistributedDataStore.getActorContext().getShardManager();

            followerShardManager.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("10 seconds"), LocalShardFound.class);

//            final DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(Collections.singleton(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.EMPTY)));

        }};

        Thread.sleep(10000);

        final DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(Collections.singleton(TEST_ID));

        final DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(true);
        final DOMDataTreeWriteCursor cursor = tx.createCursor(TEST_ID);
        Assert.assertNotNull(cursor);
        final YangInstanceIdentifier nameId = YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(TestModel.NAME_QNAME).build();
        cursor.write(nameId.getLastPathArgument(),
                ImmutableLeafNodeBuilder.<String>create().withNodeIdentifier(new NodeIdentifier(TestModel.NAME_QNAME)).withValue("Test Value").build());

        cursor.close();
        tx.submit();


//        Thread.sleep(30000);
    }
}
