/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AddressFromURIString;
import akka.actor.PoisonPill;
import akka.actor.Props;
//import akka.actor.Terminated;
import akka.actor.Status;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.dispatch.Dispatchers;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.persistence.RecoveryCompleted;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.AddShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.messages.CreateShardReply;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PeerDown;
import org.opendaylight.controller.cluster.datastore.messages.PeerUp;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.RemoveShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.SwitchShardBehavior;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class ShardManagerTest extends AbstractActorTest {
    private static int ID_COUNTER = 1;

    private final String shardMrgIDSuffix = "config" + ID_COUNTER++;
    private final String shardMgrID = "shard-manager-" + shardMrgIDSuffix;

    @Mock
    private static CountDownLatch ready;

    private static TestActorRef<MessageCollectorActor> mockShardActor;

    private static String mockShardName;

    private final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder().
            dataStoreType(shardMrgIDSuffix).shardInitializationTimeout(600, TimeUnit.MILLISECONDS)
                   .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(6);

    private static ActorRef newMockShardActor(ActorSystem system, String shardName, String memberName) {
        String name = new ShardIdentifier(shardName, memberName,"config").toString();
        return TestActorRef.create(system, Props.create(MessageCollectorActor.class), name);
    }

    private final PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        if(mockShardActor == null) {
            mockShardName = new ShardIdentifier(Shard.DEFAULT_NAME, "member-1", "config").toString();
            mockShardActor = TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class), mockShardName);
        }

        mockShardActor.underlyingActor().clear();
    }

    @After
    public void tearDown() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    private Props newShardMgrProps() {
        return newShardMgrProps(new MockConfiguration());
    }

    private Props newShardMgrProps(Configuration config) {
        return ShardManager.props(new MockClusterWrapper(), config, datastoreContextBuilder.build(), ready,
                primaryShardInfoCache);
    }

    private Props newPropsShardMgrWithMockShardActor() {
        return newPropsShardMgrWithMockShardActor("shardManager", mockShardActor, new MockClusterWrapper(),
                new MockConfiguration());
    }

    private Props newPropsShardMgrWithMockShardActor(final String name, final ActorRef shardActor,
            final ClusterWrapper clusterWrapper, final Configuration config) {
        Creator<ShardManager> creator = new Creator<ShardManager>() {
            private static final long serialVersionUID = 1L;
            @Override
            public ShardManager create() throws Exception {
                return new ForwardingShardManager(clusterWrapper, config, datastoreContextBuilder.build(),
                        ready, name, shardActor, primaryShardInfoCache);
            }
        };

        return Props.create(new DelegatingShardManagerCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId());
    }

    @Test
    public void testOnReceiveFindPrimaryForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary("non-existent", false), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryNotFoundException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForLocalLeaderShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            DataTree mockDataTree = mock(DataTree.class);
            shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, Optional.of(mockDataTree),
                    DataStoreVersions.CURRENT_VERSION), getRef());

            MessageCollectorActor.expectFirstMatching(mockShardActor, RegisterRoleChangeListener.class);
            shardManager.tell((new RoleChangeNotification(memberId, RaftState.Candidate.name(),
                    RaftState.Leader.name())), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            LocalPrimaryShardFound primaryFound = expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
            assertTrue("Unexpected primary path " +  primaryFound.getPrimaryPath(),
                    primaryFound.getPrimaryPath().contains("member-1-shard-default"));
            assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree() );
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
            String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager.tell(new RoleChangeNotification(memberId1,
                    RaftState.Candidate.name(), RaftState.Follower.name()), mockShardActor);
            shardManager.tell(new LeaderStateChanged(memberId1, memberId2, DataStoreVersions.CURRENT_VERSION), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForNonLocalLeaderShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
            MockClusterWrapper.sendMemberUp(shardManager, "member-2", getRef().path().toString());

            String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager.tell(new RoleChangeNotification(memberId1,
                    RaftState.Candidate.name(), RaftState.Follower.name()), mockShardActor);
            short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
            shardManager.tell(new ShardLeaderStateChanged(memberId1, memberId2, Optional.<DataTree>absent(),
                    leaderVersion), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            RemotePrimaryShardFound primaryFound = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
            assertTrue("Unexpected primary path " +  primaryFound.getPrimaryPath(),
                    primaryFound.getPrimaryPath().contains("member-2-shard-default"));
            assertEquals("getPrimaryVersion", leaderVersion, primaryFound.getPrimaryVersion());
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForUninitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NotInitializedException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForInitializedShardWithNoRole() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager.tell(new RoleChangeNotification(memberId,
                    RaftState.Candidate.name(), RaftState.Follower.name()), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);

            DataTree mockDataTree = mock(DataTree.class);
            shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, Optional.of(mockDataTree),
                    DataStoreVersions.CURRENT_VERSION), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            LocalPrimaryShardFound primaryFound = expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
            assertTrue("Unexpected primary path " +  primaryFound.getPrimaryPath(),
                    primaryFound.getPrimaryPath().contains("member-1-shard-default"));
            assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree() );
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForShardLeader() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            // We're passing waitUntilInitialized = true to FindPrimary so the response should be
            // delayed until we send ActorInitialized and RoleChangeNotification.
            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectNoMsg(FiniteDuration.create(150, TimeUnit.MILLISECONDS));

            shardManager.tell(new ActorInitialized(), mockShardActor);

            expectNoMsg(FiniteDuration.create(150, TimeUnit.MILLISECONDS));

            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager.tell(new RoleChangeNotification(memberId,
                    RaftState.Candidate.name(), RaftState.Leader.name()), mockShardActor);

            expectNoMsg(FiniteDuration.create(150, TimeUnit.MILLISECONDS));

            DataTree mockDataTree = mock(DataTree.class);
            shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, Optional.of(mockDataTree),
                    DataStoreVersions.CURRENT_VERSION), mockShardActor);

            LocalPrimaryShardFound primaryFound = expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
            assertTrue("Unexpected primary path " +  primaryFound.getPrimaryPath(),
                    primaryFound.getPrimaryPath().contains("member-1-shard-default"));
            assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree() );

            expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NotInitializedException.class);

            shardManager.tell(new ActorInitialized(), mockShardActor);

            expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithCandidateShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);
            shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                    null, RaftState.Candidate.name()), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);
            shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                    null, RaftState.IsolatedLeader.name()), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForRemoteShard() throws Exception {
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        ActorRef mockShardActor1 = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final TestActorRef<ForwardingShardManager> shardManager1 = TestActorRef.create(system1,
                newPropsShardMgrWithMockShardActor("shardManager1", mockShardActor1, new ClusterWrapperImpl(system1),
                        new MockConfiguration()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, "astronauts", "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                put("default", Arrays.asList("member-1", "member-2")).
                put("astronauts", Arrays.asList("member-2")).build());

        final TestActorRef<ForwardingShardManager> shardManager2 = TestActorRef.create(system2,
                newPropsShardMgrWithMockShardActor("shardManager2", mockShardActor2, new ClusterWrapperImpl(system2),
                        mockConfig2), shardManagerID);

        new JavaTestKit(system1) {{

            shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager2.tell(new ActorInitialized(), mockShardActor2);

            String memberId2 = "member-2-shard-astronauts-" + shardMrgIDSuffix;
            short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
            shardManager2.tell(new ShardLeaderStateChanged(memberId2, memberId2,
                    Optional.of(mock(DataTree.class)), leaderVersion), mockShardActor2);
            shardManager2.tell(new RoleChangeNotification(memberId2,
                    RaftState.Candidate.name(), RaftState.Leader.name()), mockShardActor2);

            shardManager1.underlyingActor().waitForMemberUp();

            shardManager1.tell(new FindPrimary("astronauts", false), getRef());

            RemotePrimaryShardFound found = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
            String path = found.getPrimaryPath();
            assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-astronauts-config"));
            assertEquals("getPrimaryVersion", leaderVersion, found.getPrimaryVersion());

            shardManager2.underlyingActor().verifyFindPrimary();

            Cluster.get(system2).down(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

            shardManager1.underlyingActor().waitForMemberRemoved();

            shardManager1.tell(new FindPrimary("astronauts", false), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryNotFoundException.class);
        }};

        JavaTestKit.shutdownActorSystem(system1);
        JavaTestKit.shutdownActorSystem(system2);
    }

    @Test
    public void testShardAvailabilityOnChangeOfMemberReachability() throws Exception {
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor1 = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final TestActorRef<ForwardingShardManager> shardManager1 = TestActorRef.create(system1,
            newPropsShardMgrWithMockShardActor("shardManager1", mockShardActor1, new ClusterWrapperImpl(system1),
                new MockConfiguration()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, Shard.DEFAULT_NAME, "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
            put("default", Arrays.asList("member-1", "member-2")).build());

        final TestActorRef<ForwardingShardManager> shardManager2 = TestActorRef.create(system2,
            newPropsShardMgrWithMockShardActor("shardManager2", mockShardActor2, new ClusterWrapperImpl(system2),
                mockConfig2), shardManagerID);

        new JavaTestKit(system1) {{

            shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager1.tell(new ActorInitialized(), mockShardActor1);
            shardManager2.tell(new ActorInitialized(), mockShardActor2);

            String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
            String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager1.tell(new ShardLeaderStateChanged(memberId1, memberId2,
                Optional.of(mock(DataTree.class)), DataStoreVersions.CURRENT_VERSION), mockShardActor1);
            shardManager1.tell(new RoleChangeNotification(memberId1,
                RaftState.Candidate.name(), RaftState.Follower.name()), mockShardActor1);
            shardManager2.tell(new ShardLeaderStateChanged(memberId2, memberId2, Optional.of(mock(DataTree.class)),
                    DataStoreVersions.CURRENT_VERSION),
                mockShardActor2);
            shardManager2.tell(new RoleChangeNotification(memberId2,
                RaftState.Candidate.name(), RaftState.Leader.name()), mockShardActor2);
            shardManager1.underlyingActor().waitForMemberUp();

            shardManager1.tell(new FindPrimary("default", true), getRef());

            RemotePrimaryShardFound found = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
            String path = found.getPrimaryPath();
            assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-default-config"));

            shardManager1.underlyingActor().onReceiveCommand(MockClusterWrapper.
                createUnreachableMember("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"));

            shardManager1.underlyingActor().waitForUnreachableMember();

            PeerDown peerDown = MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerDown.class);
            assertEquals("getMemberName", "member-2", peerDown.getMemberName());
            MessageCollectorActor.clearMessages(mockShardActor1);

            shardManager1.underlyingActor().onReceiveCommand(MockClusterWrapper.
                    createMemberRemoved("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"));

            MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerDown.class);

            shardManager1.tell(new FindPrimary("default", true), getRef());

            expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);

            shardManager1.underlyingActor().onReceiveCommand(MockClusterWrapper.
                createReachableMember("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"));

            shardManager1.underlyingActor().waitForReachableMember();

            PeerUp peerUp = MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerUp.class);
            assertEquals("getMemberName", "member-2", peerUp.getMemberName());
            MessageCollectorActor.clearMessages(mockShardActor1);

            shardManager1.tell(new FindPrimary("default", true), getRef());

            RemotePrimaryShardFound found1 = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
            String path1 = found1.getPrimaryPath();
            assertTrue("Unexpected primary path " + path1, path1.contains("member-2-shard-default-config"));

            shardManager1.underlyingActor().onReceiveCommand(MockClusterWrapper.
                    createMemberUp("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"));

            MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerUp.class);

        }};

        JavaTestKit.shutdownActorSystem(system1);
        JavaTestKit.shutdownActorSystem(system2);
    }

    @Test
    public void testShardAvailabilityChangeOnMemberUnreachableAndLeadershipChange() throws Exception {
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor1 = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final TestActorRef<ForwardingShardManager> shardManager1 = TestActorRef.create(system1,
            newPropsShardMgrWithMockShardActor("shardManager1", mockShardActor1, new ClusterWrapperImpl(system1),
                new MockConfiguration()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, Shard.DEFAULT_NAME, "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
            put("default", Arrays.asList("member-1", "member-2")).build());

        final TestActorRef<ForwardingShardManager> shardManager2 = TestActorRef.create(system2,
            newPropsShardMgrWithMockShardActor("shardManager2", mockShardActor2, new ClusterWrapperImpl(system2),
                mockConfig2), shardManagerID);

        new JavaTestKit(system1) {{

            shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager1.tell(new ActorInitialized(), mockShardActor1);
            shardManager2.tell(new ActorInitialized(), mockShardActor2);

            String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
            String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager1.tell(new ShardLeaderStateChanged(memberId1, memberId2,
                Optional.of(mock(DataTree.class)), DataStoreVersions.CURRENT_VERSION), mockShardActor1);
            shardManager1.tell(new RoleChangeNotification(memberId1,
                RaftState.Candidate.name(), RaftState.Follower.name()), mockShardActor1);
            shardManager2.tell(new ShardLeaderStateChanged(memberId2, memberId2, Optional.of(mock(DataTree.class)),
                    DataStoreVersions.CURRENT_VERSION),
                mockShardActor2);
            shardManager2.tell(new RoleChangeNotification(memberId2,
                RaftState.Candidate.name(), RaftState.Leader.name()), mockShardActor2);
            shardManager1.underlyingActor().waitForMemberUp();

            shardManager1.tell(new FindPrimary("default", true), getRef());

            RemotePrimaryShardFound found = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
            String path = found.getPrimaryPath();
            assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-default-config"));

            primaryShardInfoCache.putSuccessful("default", new PrimaryShardInfo(system1.actorSelection(
                    mockShardActor1.path()), DataStoreVersions.CURRENT_VERSION, Optional.<DataTree>absent()));

            shardManager1.underlyingActor().onReceiveCommand(MockClusterWrapper.
                createUnreachableMember("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"));

            shardManager1.underlyingActor().waitForUnreachableMember();

            shardManager1.tell(new FindPrimary("default", true), getRef());

            expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);

            assertNull("Expected primaryShardInfoCache entry removed", primaryShardInfoCache.getIfPresent("default"));

            shardManager1.tell(new ShardLeaderStateChanged(memberId1, memberId1, Optional.of(mock(DataTree.class)),
                    DataStoreVersions.CURRENT_VERSION), mockShardActor1);
            shardManager1.tell(new RoleChangeNotification(memberId1,
                RaftState.Follower.name(), RaftState.Leader.name()), mockShardActor1);

            shardManager1.tell(new FindPrimary("default", true), getRef());

            LocalPrimaryShardFound found1 = expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
            String path1 = found1.getPrimaryPath();
            assertTrue("Unexpected primary path " + path1, path1.contains("member-1-shard-default-config"));

        }};

        JavaTestKit.shutdownActorSystem(system1);
        JavaTestKit.shutdownActorSystem(system2);
    }


    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindLocalShard("non-existent", false), getRef());

            LocalShardNotFound notFound = expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            assertEquals("getShardName", "non-existent", notFound.getShardName());
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());

            LocalShardFound found = expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            assertTrue("Found path contains " + found.getPath().path().toString(),
                    found.getPath().path().toString().contains("member-1-shard-default-config"));
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForNotInitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NotInitializedException.class);
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardWaitForShardInitialized() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            // We're passing waitUntilInitialized = true to FindLocalShard so the response should be
            // delayed until we send ActorInitialized.
            Future<Object> future = Patterns.ask(shardManager, new FindLocalShard(Shard.DEFAULT_NAME, true),
                    new Timeout(5, TimeUnit.SECONDS));

            shardManager.tell(new ActorInitialized(), mockShardActor);

            Object resp = Await.result(future, duration("5 seconds"));
            assertTrue("Expected: LocalShardFound, Actual: " + resp, resp instanceof LocalShardFound);
        }};
    }

    @Test
    public void testOnRecoveryJournalIsCleaned() {
        InMemoryJournal.addEntry(shardMgrID, 1L, new ShardManager.SchemaContextModules(
                ImmutableSet.of("foo")));
        InMemoryJournal.addEntry(shardMgrID, 2L, new ShardManager.SchemaContextModules(
                ImmutableSet.of("bar")));
        InMemoryJournal.addDeleteMessagesCompleteLatch(shardMgrID);

        new JavaTestKit(getSystem()) {{
            TestActorRef<TestShardManager> shardManager = TestActorRef.create(getSystem(),
                    Props.create(new TestShardManagerCreator(shardMrgIDSuffix)));

            shardManager.underlyingActor().waitForRecoveryComplete();
            InMemoryJournal.waitForDeleteMessagesComplete(shardMgrID);

            // Journal entries up to the last one should've been deleted
            Map<Long, Object> journal = InMemoryJournal.get(shardMgrID);
            synchronized (journal) {
                assertEquals("Journal size", 0, journal.size());
            }
        }};
    }

    @Test
    public void testRoleChangeNotificationAndShardLeaderStateChangedReleaseReady() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                TestActorRef<ShardManager> shardManager = TestActorRef.create(getSystem(), newShardMgrProps());

                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.underlyingActor().onReceiveCommand(new RoleChangeNotification(
                        memberId, RaftState.Candidate.name(), RaftState.Leader.name()));

                verify(ready, never()).countDown();

                shardManager.underlyingActor().onReceiveCommand(new ShardLeaderStateChanged(memberId, memberId,
                        Optional.of(mock(DataTree.class)), DataStoreVersions.CURRENT_VERSION));

                verify(ready, times(1)).countDown();

            }};
    }

    @Test
    public void testRoleChangeNotificationToFollowerWithShardLeaderStateChangedReleaseReady() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                TestActorRef<ShardManager> shardManager = TestActorRef.create(getSystem(), newShardMgrProps());

                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.underlyingActor().onReceiveCommand(new RoleChangeNotification(
                        memberId, null, RaftState.Follower.name()));

                verify(ready, never()).countDown();

                shardManager.underlyingActor().onReceiveCommand(MockClusterWrapper.createMemberUp("member-2", getRef().path().toString()));

                shardManager.underlyingActor().onReceiveCommand(new ShardLeaderStateChanged(memberId,
                        "member-2-shard-default-" + shardMrgIDSuffix, Optional.of(mock(DataTree.class)),
                        DataStoreVersions.CURRENT_VERSION));

                verify(ready, times(1)).countDown();

            }};
    }

    @Test
    public void testReadyCountDownForMemberUpAfterLeaderStateChanged() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                TestActorRef<ShardManager> shardManager = TestActorRef.create(getSystem(), newShardMgrProps());

                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.underlyingActor().onReceiveCommand(new RoleChangeNotification(
                        memberId, null, RaftState.Follower.name()));

                verify(ready, never()).countDown();

                shardManager.underlyingActor().onReceiveCommand(new ShardLeaderStateChanged(memberId,
                        "member-2-shard-default-" + shardMrgIDSuffix, Optional.of(mock(DataTree.class)),
                        DataStoreVersions.CURRENT_VERSION));

                shardManager.underlyingActor().onReceiveCommand(MockClusterWrapper.createMemberUp("member-2", getRef().path().toString()));

                verify(ready, times(1)).countDown();

            }};
    }

    @Test
    public void testRoleChangeNotificationDoNothingForUnknownShard() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                TestActorRef<ShardManager> shardManager = TestActorRef.create(getSystem(), newShardMgrProps());

                shardManager.underlyingActor().onReceiveCommand(new RoleChangeNotification(
                        "unknown", RaftState.Candidate.name(), RaftState.Leader.name()));

                verify(ready, never()).countDown();

            }};
    }


    @Test
    public void testByDefaultSyncStatusIsFalse() throws Exception{
        final Props persistentProps = newShardMgrProps();
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();

        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsLeaderSyncStatusIsTrue() throws Exception{
        final Props persistentProps = ShardManager.props(
                new MockClusterWrapper(),
                new MockConfiguration(),
                DatastoreContext.newBuilder().persistent(true).build(), ready, primaryShardInfoCache);
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-unknown",
                RaftState.Follower.name(), RaftState.Leader.name()));

        assertEquals(true, shardManagerActor.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsCandidateSyncStatusIsFalse() throws Exception{
        final Props persistentProps = newShardMgrProps();
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-unknown",
                RaftState.Follower.name(), RaftState.Candidate.name()));

        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Send a FollowerInitialSyncStatus with status = true for the replica whose current state is candidate
        shardManagerActor.onReceiveCommand(new FollowerInitialSyncUpStatus(true, "member-1-shard-default-unknown"));

        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsFollowerSyncStatusDependsOnFollowerInitialSyncStatus() throws Exception{
        final Props persistentProps = ShardManager.props(
                new MockClusterWrapper(),
                new MockConfiguration(),
                DatastoreContext.newBuilder().persistent(true).build(), ready, primaryShardInfoCache);
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-unknown",
                RaftState.Candidate.name(), RaftState.Follower.name()));

        // Initially will be false
        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Send status true will make sync status true
        shardManagerActor.onReceiveCommand(new FollowerInitialSyncUpStatus(true, "member-1-shard-default-unknown"));

        assertEquals(true, shardManagerActor.getMBean().getSyncStatus());

        // Send status false will make sync status false
        shardManagerActor.onReceiveCommand(new FollowerInitialSyncUpStatus(false, "member-1-shard-default-unknown"));

        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

    }

    @Test
    public void testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards() throws Exception{
        final Props persistentProps = ShardManager.props(
                new MockClusterWrapper(),
                new MockConfiguration() {
                    @Override
                    public List<String> getMemberShardNames(String memberName) {
                        return Arrays.asList("default", "astronauts");
                    }
                },
                DatastoreContext.newBuilder().persistent(true).build(), ready, primaryShardInfoCache);
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();

        // Initially will be false
        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Make default shard leader
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-unknown",
                RaftState.Follower.name(), RaftState.Leader.name()));

        // default = Leader, astronauts is unknown so sync status remains false
        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Make astronauts shard leader as well
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-astronauts-unknown",
                RaftState.Follower.name(), RaftState.Leader.name()));

        // Now sync status should be true
        assertEquals(true, shardManagerActor.getMBean().getSyncStatus());

        // Make astronauts a Follower
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-astronauts-unknown",
                RaftState.Leader.name(), RaftState.Follower.name()));

        // Sync status is not true
        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Make the astronauts follower sync status true
        shardManagerActor.onReceiveCommand(new FollowerInitialSyncUpStatus(true, "member-1-shard-astronauts-unknown"));

        // Sync status is now true
        assertEquals(true, shardManagerActor.getMBean().getSyncStatus());

    }

    @Test
    public void testOnReceiveSwitchShardBehavior() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new SwitchShardBehavior(mockShardName, "Leader", 1000), getRef());

            SwitchBehavior switchBehavior = MessageCollectorActor.expectFirstMatching(mockShardActor, SwitchBehavior.class);

            assertEquals(RaftState.Leader, switchBehavior.getNewState());
            assertEquals(1000, switchBehavior.getNewTerm());
        }};
    }

    @Test
    public void testOnReceiveCreateShard() {
        new JavaTestKit(getSystem()) {{
            datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

            ActorRef shardManager = getSystem().actorOf(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            SchemaContext schemaContext = TestModel.createTestContext();
            shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

            DatastoreContext datastoreContext = DatastoreContext.newBuilder().shardElectionTimeoutFactor(100).
                    persistent(false).build();
            TestShardPropsCreator shardPropsCreator = new TestShardPropsCreator();

            ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("foo-ns"), "foo-module",
                    "foo", null, Arrays.asList("member-1", "member-5", "member-6"));
            shardManager.tell(new CreateShard(config, shardPropsCreator, datastoreContext), getRef());

            expectMsgClass(duration("5 seconds"), CreateShardReply.class);

            shardManager.tell(new FindLocalShard("foo", true), getRef());

            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            assertEquals("isRecoveryApplicable", false, shardPropsCreator.datastoreContext.isPersistent());
            assertTrue("Epxected ShardPeerAddressResolver", shardPropsCreator.datastoreContext.getShardRaftConfig().
                    getPeerAddressResolver() instanceof ShardPeerAddressResolver);
            assertEquals("peerMembers", Sets.newHashSet(new ShardIdentifier("foo", "member-5", shardMrgIDSuffix).toString(),
                    new ShardIdentifier("foo", "member-6", shardMrgIDSuffix).toString()),
                    shardPropsCreator.peerAddresses.keySet());
            assertEquals("ShardIdentifier", new ShardIdentifier("foo", "member-1", shardMrgIDSuffix),
                    shardPropsCreator.shardId);
            assertSame("schemaContext", schemaContext, shardPropsCreator.schemaContext);

            // Send CreateShard with same name - should fail.

            shardManager.tell(new CreateShard(config, shardPropsCreator, null), getRef());

            expectMsgClass(duration("5 seconds"), akka.actor.Status.Failure.class);
        }};
    }

    @Test
    public void testOnReceiveCreateShardWithNoInitialSchemaContext() {
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = getSystem().actorOf(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            TestShardPropsCreator shardPropsCreator = new TestShardPropsCreator();

            ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("foo-ns"), "foo-module",
                    "foo", null, Arrays.asList("member-1"));
            shardManager.tell(new CreateShard(config, shardPropsCreator, null), getRef());

            expectMsgClass(duration("5 seconds"), CreateShardReply.class);

            SchemaContext schemaContext = TestModel.createTestContext();
            shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

            shardManager.tell(new FindLocalShard("foo", true), getRef());

            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            assertSame("schemaContext", schemaContext, shardPropsCreator.schemaContext);
            assertNotNull("schemaContext is null", shardPropsCreator.datastoreContext);
        }};
    }

    @Test
    public void testAddShardReplicaForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = getSystem().actorOf(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            shardManager.tell(new AddShardReplica("model-inventory"), getRef());
            Status.Failure resp = expectMsgClass(duration("2 seconds"), Status.Failure.class);

            assertEquals("Failure obtained", true,
                          (resp.cause() instanceof IllegalArgumentException));
        }};
    }

    @Test
    public void testAddShardReplicaForAlreadyCreatedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = getSystem().actorOf(newShardMgrProps());
            shardManager.tell(new AddShardReplica("default"), getRef());
            Status.Failure resp = expectMsgClass(duration("2 seconds"), Status.Failure.class);
            assertEquals("Failure obtained", true,
                          (resp.cause() instanceof IllegalArgumentException));
        }};
    }

    @Test
    public void testAddShardReplica() throws Exception {
        MockConfiguration mockConfig =
                new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                   put("default", Arrays.asList("member-1", "member-2")).
                   put("astronauts", Arrays.asList("member-2")).build());

        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.
        final ActorSystem system1 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        ActorRef mockDefaultShardActor = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");
        final TestActorRef<ForwardingShardManager> newReplicaShardManager = TestActorRef.create(system1,
                newPropsShardMgrWithMockShardActor("shardManager1", mockDefaultShardActor,
                   new ClusterWrapperImpl(system1), mockConfig), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.
        final ActorSystem system2 = ActorSystem.create("cluster-test",
            ConfigFactory.load().getConfig("Member2"));
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        String name = new ShardIdentifier("astronauts", "member-2", "config").toString();
        final TestActorRef<MockRespondActor> mockShardLeaderActor =
            TestActorRef.create(system2, Props.create(MockRespondActor.class), name);
        final TestActorRef<ForwardingShardManager> leaderShardManager = TestActorRef.create(system2,
                newPropsShardMgrWithMockShardActor("shardManager2", mockShardLeaderActor,
                        new ClusterWrapperImpl(system2), mockConfig), shardManagerID);

        new JavaTestKit(system1) {{

            newReplicaShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            leaderShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            leaderShardManager.tell(new ActorInitialized(), mockShardLeaderActor);

            String memberId2 = "member-2-shard-astronauts-" + shardMrgIDSuffix;
            short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
            leaderShardManager.tell(new ShardLeaderStateChanged(memberId2, memberId2,
                    Optional.of(mock(DataTree.class)), leaderVersion), mockShardLeaderActor);
            leaderShardManager.tell(new RoleChangeNotification(memberId2,
                    RaftState.Candidate.name(), RaftState.Leader.name()), mockShardLeaderActor);

            newReplicaShardManager.underlyingActor().waitForMemberUp();
            leaderShardManager.underlyingActor().waitForMemberUp();

            //construct a mock response message
            AddServerReply response = new AddServerReply(ServerChangeStatus.OK, memberId2);
            mockShardLeaderActor.underlyingActor().updateResponse(response);
            newReplicaShardManager.tell(new AddShardReplica("astronauts"), getRef());
            AddServer addServerMsg = MessageCollectorActor.expectFirstMatching(mockShardLeaderActor,
                AddServer.class);
            String addServerId = "member-1-shard-astronauts-" + shardMrgIDSuffix;
            assertEquals("AddServer serverId", addServerId, addServerMsg.getNewServerId());
            newReplicaShardManager.underlyingActor().verifySnapshotPersisted("astronauts");

            expectMsgClass(duration("5 seconds"), Status.Success.class);
        }};

        JavaTestKit.shutdownActorSystem(system1);
        JavaTestKit.shutdownActorSystem(system2);
    }

    @Test
    public void testAddShardReplicaWithFindPrimaryTimeout() throws Exception {
        MockConfiguration mockConfig =
                new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                   put("default", Arrays.asList("member-1", "member-2")).
                   put("astronauts", Arrays.asList("member-2")).build());

        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.
        final ActorSystem system1 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        ActorRef mockDefaultShardActor = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");
        final TestActorRef<ForwardingShardManager> newReplicaShardManager = TestActorRef.create(system1,
                newPropsShardMgrWithMockShardActor("shardManager1", mockDefaultShardActor,
                   new ClusterWrapperImpl(system1), mockConfig), shardManagerID);

        new JavaTestKit(system1) {{

            newReplicaShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            MockClusterWrapper.sendMemberUp(newReplicaShardManager, "member-2", getRef().path().toString());
            newReplicaShardManager.underlyingActor().waitForMemberUp();

            newReplicaShardManager.tell(new AddShardReplica("astronauts"), getRef());
            Status.Failure resp = expectMsgClass(duration("5 seconds"), Status.Failure.class);
            assertEquals("Failure obtained", true,
                          (resp.cause() instanceof RuntimeException));
        }};

        JavaTestKit.shutdownActorSystem(system1);
    }

    @Test
    public void testRemoveShardReplicaForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = getSystem().actorOf(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            shardManager.tell(new RemoveShardReplica("model-inventory"), getRef());
            Status.Failure resp = expectMsgClass(duration("2 seconds"), Status.Failure.class);
            assertEquals("Failure obtained", true,
                         (resp.cause() instanceof IllegalArgumentException));
        }};
    }

    @Test
    public void testShardPersistenceWithRestoredData() throws Exception {
        new JavaTestKit(getSystem()) {{
        MockConfiguration mockConfig =
                new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                   put("default", Arrays.asList("member-1", "member-2")).
                   put("astronauts", Arrays.asList("member-2")).
                   put("people", Arrays.asList("member-1", "member-2")).build());

        TestActorRef<TestShardManager> newRestoredShardManager = TestActorRef.create(getSystem(),
            Props.create(new TestShardManagerCreator(shardMrgIDSuffix, mockConfig)));

        String[] restoredShards = {"default", "astronauts"};
        ShardManagerSnapshot snapshot = new ShardManagerSnapshot(Arrays.asList(restoredShards));
        String shardManagerID = newRestoredShardManager.underlyingActor().persistenceId();
        InMemorySnapshotStore.addSnapshot(shardManagerID, snapshot);

    //    ActorRef mockDefaultShardActor = newMockShardActor(getSystem(), Shard.DEFAULT_NAME, "member-1");
        newRestoredShardManager.tell(PoisonPill.getInstance(), null);
        Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
//        expectMsgClass(duration("5 seconds"), Terminated.class);

        //create shardManager again to come up with restored data
        newRestoredShardManager = TestActorRef.create(getSystem(),
            Props.create(new TestShardManagerCreator(shardMrgIDSuffix, mockConfig)));

        newRestoredShardManager.underlyingActor().waitForRecoveryComplete();

            newRestoredShardManager.tell(new FindLocalShard("people", false), getRef());
            LocalShardNotFound notFound = expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);
            assertEquals("for uninitialized shard", "people", notFound.getShardName());

            // As the member-2 system is not created, astronauts shard will be not up yet.
            // Expecting NotInitializedException for the shard
            newRestoredShardManager.tell(new FindLocalShard("astronauts", false), getRef());
            expectMsgClass(duration("5 seconds"), NotInitializedException.class);
           /* LocalShardFound found = expectMsgClass(duration("5 seconds"), NotInitializedException.class);
            assertTrue("for added shard",
                found.getPath().path().toString().contains("member-1-shard-astronauts-config"));*/
        }};
    }

    private static class TestShardPropsCreator implements ShardPropsCreator {
        ShardIdentifier shardId;
        Map<String, String> peerAddresses;
        SchemaContext schemaContext;
        DatastoreContext datastoreContext;

        @Override
        public Props newProps(ShardIdentifier shardId, Map<String, String> peerAddresses,
                DatastoreContext datastoreContext, SchemaContext schemaContext) {
            this.shardId = shardId;
            this.peerAddresses = peerAddresses;
            this.schemaContext = schemaContext;
            this.datastoreContext = datastoreContext;
            return Shard.props(shardId, peerAddresses, datastoreContext, schemaContext);
        }

    }

    private static class TestShardManager extends ShardManager {
        private final CountDownLatch recoveryComplete = new CountDownLatch(1);

        TestShardManager(String shardMrgIDSuffix, Configuration config) {
            super(new MockClusterWrapper(), config,
                    DatastoreContext.newBuilder().dataStoreType(shardMrgIDSuffix).build(), ready,
                    new PrimaryShardInfoFutureCache());
        }

        @Override
        public void handleRecover(Object message) throws Exception {
            try {
                super.handleRecover(message);
            } finally {
                if(message instanceof RecoveryCompleted) {
                    recoveryComplete.countDown();
                }
            }
        }

        void waitForRecoveryComplete() {
            assertEquals("Recovery complete", true,
                    Uninterruptibles.awaitUninterruptibly(recoveryComplete, 5, TimeUnit.SECONDS));
        }
    }

    @SuppressWarnings("serial")
    static class TestShardManagerCreator implements Creator<TestShardManager> {
        String shardMrgIDSuffix;
        Configuration config;

        TestShardManagerCreator(String shardMrgIDSuffix) {
            this.shardMrgIDSuffix = shardMrgIDSuffix;
            this.config = new MockConfiguration();
        }

        TestShardManagerCreator(String shardMrgIDSuffix, Configuration config) {
            this.shardMrgIDSuffix = shardMrgIDSuffix;
            this.config = config;
        }


        @Override
        public TestShardManager create() throws Exception {
            return new TestShardManager(shardMrgIDSuffix, config);
        }

    }

    private static class DelegatingShardManagerCreator implements Creator<ShardManager> {
        private static final long serialVersionUID = 1L;
        private final Creator<ShardManager> delegate;

        public DelegatingShardManagerCreator(Creator<ShardManager> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ShardManager create() throws Exception {
            return delegate.create();
        }
    }

    private static class ForwardingShardManager extends ShardManager {
        private CountDownLatch findPrimaryMessageReceived = new CountDownLatch(1);
        private CountDownLatch memberUpReceived = new CountDownLatch(1);
        private CountDownLatch memberRemovedReceived = new CountDownLatch(1);
        private CountDownLatch memberUnreachableReceived = new CountDownLatch(1);
        private CountDownLatch memberReachableReceived = new CountDownLatch(1);
        private final ActorRef shardActor;
        private final String name;
        private final CountDownLatch snapshotPersist = new CountDownLatch(1);
        private ShardManagerSnapshot snapshot;

        protected ForwardingShardManager(ClusterWrapper cluster, Configuration configuration,
                DatastoreContext datastoreContext, CountDownLatch waitTillReadyCountdownLatch, String name,
                ActorRef shardActor, PrimaryShardInfoFutureCache primaryShardInfoCache) {
            super(cluster, configuration, datastoreContext, waitTillReadyCountdownLatch, primaryShardInfoCache);
            this.shardActor = shardActor;
            this.name = name;
        }

        @Override
        public void handleCommand(Object message) throws Exception {
            try{
                super.handleCommand(message);
            } finally {
                if(message instanceof FindPrimary) {
                    findPrimaryMessageReceived.countDown();
                } else if(message instanceof ClusterEvent.MemberUp) {
                    String role = ((ClusterEvent.MemberUp)message).member().roles().head();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberUpReceived.countDown();
                    }
                } else if(message instanceof ClusterEvent.MemberRemoved) {
                    String role = ((ClusterEvent.MemberRemoved)message).member().roles().head();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberRemovedReceived.countDown();
                    }
                } else if(message instanceof ClusterEvent.UnreachableMember) {
                    String role = ((ClusterEvent.UnreachableMember)message).member().roles().head();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberUnreachableReceived.countDown();
                    }
                } else if(message instanceof ClusterEvent.ReachableMember) {
                    String role = ((ClusterEvent.ReachableMember)message).member().roles().head();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberReachableReceived.countDown();
                    }
                }
            }
        }

        @Override
        public String persistenceId() {
            return name;
        }

        @Override
        protected ActorRef newShardActor(SchemaContext schemaContext, ShardInformation info) {
            return shardActor;
        }

        void waitForMemberUp() {
            assertEquals("MemberUp received", true,
                    Uninterruptibles.awaitUninterruptibly(memberUpReceived, 5, TimeUnit.SECONDS));
            memberUpReceived = new CountDownLatch(1);
        }

        void waitForMemberRemoved() {
            assertEquals("MemberRemoved received", true,
                    Uninterruptibles.awaitUninterruptibly(memberRemovedReceived, 5, TimeUnit.SECONDS));
            memberRemovedReceived = new CountDownLatch(1);
        }

        void waitForUnreachableMember() {
            assertEquals("UnreachableMember received", true,
                Uninterruptibles.awaitUninterruptibly(memberUnreachableReceived, 5, TimeUnit.SECONDS
                ));
            memberUnreachableReceived = new CountDownLatch(1);
        }

        void waitForReachableMember() {
            assertEquals("ReachableMember received", true,
                Uninterruptibles.awaitUninterruptibly(memberReachableReceived, 5, TimeUnit.SECONDS));
            memberReachableReceived = new CountDownLatch(1);
        }

        void verifyFindPrimary() {
            assertEquals("FindPrimary received", true,
                    Uninterruptibles.awaitUninterruptibly(findPrimaryMessageReceived, 5, TimeUnit.SECONDS));
            findPrimaryMessageReceived = new CountDownLatch(1);
        }

        @Override
        public void saveSnapshot(Object obj) {
            snapshot = (ShardManagerSnapshot) obj;
            snapshotPersist.countDown();
        }

        void verifySnapshotPersisted(String shardName) {
            assertEquals("saveSnapshot invoked", true,
                Uninterruptibles.awaitUninterruptibly(snapshotPersist, 5, TimeUnit.SECONDS));
            assertTrue("Shard Persisted", snapshot.getShardList().contains(shardName));
        }
    }

    private static class MockRespondActor extends MessageCollectorActor {

        private volatile Object responseMsg;

        public void updateResponse(Object response) {
            responseMsg = response;
        }

        @Override
        public void onReceive(Object message) throws Exception {
            super.onReceive(message);
            if (message instanceof AddServer) {
                if (responseMsg != null) {
                    getSender().tell(responseMsg, getSelf());
                    responseMsg = null;
                }
            }
        }
    }
}
