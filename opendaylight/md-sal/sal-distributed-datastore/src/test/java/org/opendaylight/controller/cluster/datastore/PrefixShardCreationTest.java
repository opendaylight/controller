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
import akka.actor.AddressFromURIString;
import akka.actor.Status.Success;
import akka.cluster.Cluster;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.Shard.Builder;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AddPrefixShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.CreatePrefixedShard;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerTest.TestShardManager;
import org.opendaylight.controller.cluster.datastore.shardstrategy.PrefixShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests prefix shard creation in ShardManager.
 */
public class PrefixShardCreationTest extends AbstractShardManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixShardCreationTest.class);

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private static final MemberName MEMBER_2 = MemberName.forName("member-2");

    @Test
    public void testPrefixShardCreation() throws Exception {

        LOG.info("testPrefixShardCreation starting");
        new JavaTestKit(getSystem()) {
            {
                datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

                final ActorRef shardManager = actorFactory.createActor(newShardMgrProps(
                        new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

                final SchemaContext schemaContext = TestModel.createTestContext();
                shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

                shardManager.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

                final Builder builder = Shard.builder();

                final CreatePrefixedShard createPrefixedShard = new CreatePrefixedShard(
                        new PrefixShardConfiguration(TEST_ID,
                                PrefixShardStrategy.NAME,
                                Collections.singletonList(MEMBER_1)),
                        datastoreContextBuilder.build(), builder);

                shardManager.tell(createPrefixedShard, getRef());
                expectMsgClass(duration("5 seconds"), Success.class);

                shardManager.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardFound.class);
            }
        };
    }

    @Test
    @Ignore("Replicas now handled via distributed config")
    public void testPrefixShardReplicas() throws Exception {
        LOG.info("testPrefixShardReplicas starting");
        final String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create ACtorSystem for member-1
        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final TestActorRef<TestShardManager> shardManager1 = TestActorRef.create(system1,
                newTestShardMgrBuilder(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                        .waitTillReadyCountDownLatch(ready)
                        .cluster(new ClusterWrapperImpl(system1))
                        .props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = newActorSystem("Member2");

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder()
                        .configuration(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                        .waitTillReadyCountDownLatch(ready)
                        .cluster(new ClusterWrapperImpl(system2)).props().withDispatcher(
                        Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        final JavaTestKit kit2 = new JavaTestKit(system2);

        new JavaTestKit(system1) {
            {
                shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                // check shard does not exist
                shardManager1.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

                shardManager2.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), kit2.getRef());
                kit2.expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

                // create shard on node1
                final Builder builder = Shard.builder();

                final CreatePrefixedShard createPrefixedShard = new CreatePrefixedShard(
                        new PrefixShardConfiguration(TEST_ID,
                                PrefixShardStrategy.NAME,
                                Lists.newArrayList(MEMBER_1, MEMBER_2)),
                        datastoreContextBuilder.build(), builder);

                shardManager1.tell(createPrefixedShard, getRef());
                expectMsgClass(duration("5 seconds"), Success.class);

                shardManager1.underlyingActor().waitForMemberUp();

                LOG.info("changed leader state");

                // check node2 cannot find it locally
                shardManager1.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardFound.class);

                shardManager2.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), kit2.getRef());
                kit2.expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

                // but can remotely
                shardManager2.tell(new FindPrimary(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), kit2.getRef());
                kit2.expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);

                // add replica and verify if succesful
                shardManager2.tell(new AddPrefixShardReplica(TEST_ID.getRootIdentifier()), kit2.getRef());
                kit2.expectMsgClass(duration("5 seconds"), Success.class);

                // verify we have a replica on manager2 now
                shardManager2.tell(new FindLocalShard(
                        ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), kit2.getRef());
                kit2.expectMsgClass(duration("5 seconds"), LocalShardFound.class);
            }
        };
    }

    private ActorSystem newActorSystem(final String config) {
        final ActorSystem system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig(config));
        actorSystems.add(system);
        return system;
    }
}