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
import akka.actor.Status;
import akka.actor.Status.Failure;
import akka.actor.Status.Success;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.dispatch.Dispatchers;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.persistence.RecoveryCompleted;
import akka.serialization.Serialization;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.exceptions.AlreadyExistsException;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.AddShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot.ShardSnapshot;
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
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
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
    private final String shardMgrID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

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

    private static DatastoreContextFactory newDatastoreContextFactory(DatastoreContext datastoreContext) {
        DatastoreContextFactory mockFactory = mock(DatastoreContextFactory.class);
        Mockito.doReturn(datastoreContext).when(mockFactory).getBaseDatastoreContext();
        Mockito.doReturn(datastoreContext).when(mockFactory).getShardDatastoreContext(Mockito.anyString());
        return mockFactory;
    }

    private Props newShardMgrProps(Configuration config) {
        return TestShardManager.builder(datastoreContextBuilder).configuration(config).props();
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
                return new ForwardingShardManager(ShardManager.builder().cluster(clusterWrapper).configuration(config).
                        datastoreContextFactory(newDatastoreContextFactory(datastoreContextBuilder.build())).
                        waitTillReadyCountdownLatch(ready).primaryShardInfoCache(primaryShardInfoCache), name, shardActor);
            }
        };

        return Props.create(new DelegatingShardManagerCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId());
    }

    private TestShardManager newTestShardManager() {
        return newTestShardManager(newShardMgrProps());
    }

    private TestShardManager newTestShardManager(Props props) {
        TestActorRef<TestShardManager> shardManagerActor = TestActorRef.create(getSystem(), props);
        TestShardManager shardManager = shardManagerActor.underlyingActor();
        shardManager.waitForRecoveryComplete();
        return shardManager;
    }

    @Test
    public void testPerShardDatastoreContext() throws Exception {
        final DatastoreContextFactory mockFactory = newDatastoreContextFactory(
                datastoreContextBuilder.shardElectionTimeoutFactor(5).build());

        Mockito.doReturn(DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).
                shardElectionTimeoutFactor(6).build()).when(mockFactory).getShardDatastoreContext("default");

        Mockito.doReturn(DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).
                shardElectionTimeoutFactor(7).build()).when(mockFactory).getShardDatastoreContext("topology");

        final MockConfiguration mockConfig = new MockConfiguration() {
            @Override
            public Collection<String> getMemberShardNames(String memberName) {
                return Arrays.asList("default", "topology");
            }

            @Override
            public Collection<String> getMembersFromShardName(String shardName) {
                return Arrays.asList("member-1");
            }
        };

        final TestActorRef<MessageCollectorActor> defaultShardActor = TestActorRef.create(getSystem(),
                Props.create(MessageCollectorActor.class), "default");
        final TestActorRef<MessageCollectorActor> topologyShardActor = TestActorRef.create(getSystem(),
                Props.create(MessageCollectorActor.class), "topology");

        final Map<String, Entry<ActorRef, DatastoreContext>> shardInfoMap = Collections.synchronizedMap(
                new HashMap<String, Entry<ActorRef, DatastoreContext>>());
        shardInfoMap.put("default", new AbstractMap.SimpleEntry<ActorRef, DatastoreContext>(defaultShardActor, null));
        shardInfoMap.put("topology", new AbstractMap.SimpleEntry<ActorRef, DatastoreContext>(topologyShardActor, null));

        final CountDownLatch newShardActorLatch = new CountDownLatch(2);
        final Creator<ShardManager> creator = new Creator<ShardManager>() {
            private static final long serialVersionUID = 1L;
            @Override
            public ShardManager create() throws Exception {
                return new ShardManager(ShardManager.builder().cluster(new MockClusterWrapper()).configuration(mockConfig).
                        datastoreContextFactory(mockFactory).waitTillReadyCountdownLatch(ready).
                        primaryShardInfoCache(primaryShardInfoCache)) {
                    @Override
                    protected ActorRef newShardActor(SchemaContext schemaContext, ShardInformation info) {
                        Entry<ActorRef, DatastoreContext> entry = shardInfoMap.get(info.getShardName());
                        ActorRef ref = null;
                        if(entry != null) {
                            ref = entry.getKey();
                            entry.setValue(info.getDatastoreContext());
                        }

                        newShardActorLatch.countDown();
                        return ref;
                    }
                };
            }
        };

        JavaTestKit kit = new JavaTestKit(getSystem());

        final ActorRef shardManager = getSystem().actorOf(Props.create(new DelegatingShardManagerCreator(creator)).
                    withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), kit.getRef());

        assertEquals("Shard actors created", true, newShardActorLatch.await(5, TimeUnit.SECONDS));
        assertEquals("getShardElectionTimeoutFactor", 6, shardInfoMap.get("default").getValue().
                getShardElectionTimeoutFactor());
        assertEquals("getShardElectionTimeoutFactor", 7, shardInfoMap.get("topology").getValue().
                getShardElectionTimeoutFactor());

        DatastoreContextFactory newMockFactory = newDatastoreContextFactory(
                datastoreContextBuilder.shardElectionTimeoutFactor(5).build());
        Mockito.doReturn(DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).
                shardElectionTimeoutFactor(66).build()).when(newMockFactory).getShardDatastoreContext("default");

        Mockito.doReturn(DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).
                shardElectionTimeoutFactor(77).build()).when(newMockFactory).getShardDatastoreContext("topology");

        shardManager.tell(newMockFactory, kit.getRef());

        DatastoreContext newContext = MessageCollectorActor.expectFirstMatching(defaultShardActor, DatastoreContext.class);
        assertEquals("getShardElectionTimeoutFactor", 66, newContext.getShardElectionTimeoutFactor());

        newContext = MessageCollectorActor.expectFirstMatching(topologyShardActor, DatastoreContext.class);
        assertEquals("getShardElectionTimeoutFactor", 77, newContext.getShardElectionTimeoutFactor());

        defaultShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        topologyShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
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
        String persistenceID = "shard-manager-" + shardMrgIDSuffix;
        InMemoryJournal.addEntry(persistenceID, 1L, new ShardManager.SchemaContextModules(
                ImmutableSet.of("foo")));
        InMemoryJournal.addEntry(persistenceID, 2L, new ShardManager.SchemaContextModules(
                ImmutableSet.of("bar")));
        InMemoryJournal.addDeleteMessagesCompleteLatch(persistenceID);

        TestShardManager shardManager = newTestShardManager();

        InMemoryJournal.waitForDeleteMessagesComplete(persistenceID);

        // Journal entries up to the last one should've been deleted
        Map<Long, Object> journal = InMemoryJournal.get(persistenceID);
        synchronized (journal) {
            assertEquals("Journal size", 0, journal.size());
        }
    }

    @Test
    public void testRoleChangeNotificationAndShardLeaderStateChangedReleaseReady() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.onReceiveCommand(new RoleChangeNotification(
                memberId, RaftState.Candidate.name(), RaftState.Leader.name()));

        verify(ready, never()).countDown();

        shardManager.onReceiveCommand(new ShardLeaderStateChanged(memberId, memberId,
                Optional.of(mock(DataTree.class)), DataStoreVersions.CURRENT_VERSION));

        verify(ready, times(1)).countDown();
    }

    @Test
    public void testRoleChangeNotificationToFollowerWithShardLeaderStateChangedReleaseReady() throws Exception {
        new JavaTestKit(getSystem()) {{
            TestShardManager shardManager = newTestShardManager();

            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager.onReceiveCommand(new RoleChangeNotification(
                    memberId, null, RaftState.Follower.name()));

            verify(ready, never()).countDown();

            shardManager.onReceiveCommand(MockClusterWrapper.createMemberUp("member-2", getRef().path().toString()));

            shardManager.onReceiveCommand(new ShardLeaderStateChanged(memberId,
                    "member-2-shard-default-" + shardMrgIDSuffix, Optional.of(mock(DataTree.class)),
                    DataStoreVersions.CURRENT_VERSION));

            verify(ready, times(1)).countDown();
        }};
    }

    @Test
    public void testReadyCountDownForMemberUpAfterLeaderStateChanged() throws Exception {
        new JavaTestKit(getSystem()) {{
            TestShardManager shardManager = newTestShardManager();

            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager.onReceiveCommand(new RoleChangeNotification(memberId, null, RaftState.Follower.name()));

            verify(ready, never()).countDown();

            shardManager.onReceiveCommand(new ShardLeaderStateChanged(memberId,
                    "member-2-shard-default-" + shardMrgIDSuffix, Optional.of(mock(DataTree.class)),
                    DataStoreVersions.CURRENT_VERSION));

            shardManager.onReceiveCommand(MockClusterWrapper.createMemberUp("member-2", getRef().path().toString()));

            verify(ready, times(1)).countDown();
        }};
    }

    @Test
    public void testRoleChangeNotificationDoNothingForUnknownShard() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        shardManager.onReceiveCommand(new RoleChangeNotification(
                "unknown", RaftState.Candidate.name(), RaftState.Leader.name()));

        verify(ready, never()).countDown();
    }

    @Test
    public void testByDefaultSyncStatusIsFalse() throws Exception{
        TestShardManager shardManager = newTestShardManager();

        assertEquals(false, shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsLeaderSyncStatusIsTrue() throws Exception{
        TestShardManager shardManager = newTestShardManager();

        shardManager.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                RaftState.Follower.name(), RaftState.Leader.name()));

        assertEquals(true, shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsCandidateSyncStatusIsFalse() throws Exception{
        TestShardManager shardManager = newTestShardManager();

        String shardId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.onReceiveCommand(new RoleChangeNotification(shardId,
                RaftState.Follower.name(), RaftState.Candidate.name()));

        assertEquals(false, shardManager.getMBean().getSyncStatus());

        // Send a FollowerInitialSyncStatus with status = true for the replica whose current state is candidate
        shardManager.onReceiveCommand(new FollowerInitialSyncUpStatus(
                true, shardId));

        assertEquals(false, shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsFollowerSyncStatusDependsOnFollowerInitialSyncStatus() throws Exception{
        TestShardManager shardManager = newTestShardManager();

        String shardId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.onReceiveCommand(new RoleChangeNotification(shardId,
                RaftState.Candidate.name(), RaftState.Follower.name()));

        // Initially will be false
        assertEquals(false, shardManager.getMBean().getSyncStatus());

        // Send status true will make sync status true
        shardManager.onReceiveCommand(new FollowerInitialSyncUpStatus(true, shardId));

        assertEquals(true, shardManager.getMBean().getSyncStatus());

        // Send status false will make sync status false
        shardManager.onReceiveCommand(new FollowerInitialSyncUpStatus(false, shardId));

        assertEquals(false, shardManager.getMBean().getSyncStatus());

    }

    @Test
    public void testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards() throws Exception{
        TestShardManager shardManager = newTestShardManager(newShardMgrProps(new MockConfiguration() {
            @Override
            public List<String> getMemberShardNames(String memberName) {
                return Arrays.asList("default", "astronauts");
            }
        }));

        // Initially will be false
        assertEquals(false, shardManager.getMBean().getSyncStatus());

        // Make default shard leader
        String defaultShardId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.onReceiveCommand(new RoleChangeNotification(defaultShardId,
                RaftState.Follower.name(), RaftState.Leader.name()));

        // default = Leader, astronauts is unknown so sync status remains false
        assertEquals(false, shardManager.getMBean().getSyncStatus());

        // Make astronauts shard leader as well
        String astronautsShardId = "member-1-shard-astronauts-" + shardMrgIDSuffix;
        shardManager.onReceiveCommand(new RoleChangeNotification(astronautsShardId,
                RaftState.Follower.name(), RaftState.Leader.name()));

        // Now sync status should be true
        assertEquals(true, shardManager.getMBean().getSyncStatus());

        // Make astronauts a Follower
        shardManager.onReceiveCommand(new RoleChangeNotification(astronautsShardId,
                RaftState.Leader.name(), RaftState.Follower.name()));

        // Sync status is not true
        assertEquals(false, shardManager.getMBean().getSyncStatus());

        // Make the astronauts follower sync status true
        shardManager.onReceiveCommand(new FollowerInitialSyncUpStatus(true, astronautsShardId));

        // Sync status is now true
        assertEquals(true, shardManager.getMBean().getSyncStatus());

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
    public void testOnCreateShard() {
        new JavaTestKit(getSystem()) {{
            datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

            ActorRef shardManager = getSystem().actorOf(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            SchemaContext schemaContext = TestModel.createTestContext();
            shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

            DatastoreContext datastoreContext = DatastoreContext.newBuilder().shardElectionTimeoutFactor(100).
                    persistent(false).build();
            Shard.Builder shardBuilder = Shard.builder();

            ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("foo-ns"), "foo-module",
                    "foo", null, Arrays.asList("member-1", "member-5", "member-6"));
            shardManager.tell(new CreateShard(config, shardBuilder, datastoreContext), getRef());

            expectMsgClass(duration("5 seconds"), Success.class);

            shardManager.tell(new FindLocalShard("foo", true), getRef());

            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            assertEquals("isRecoveryApplicable", false, shardBuilder.getDatastoreContext().isPersistent());
            assertTrue("Epxected ShardPeerAddressResolver", shardBuilder.getDatastoreContext().getShardRaftConfig().
                    getPeerAddressResolver() instanceof ShardPeerAddressResolver);
            assertEquals("peerMembers", Sets.newHashSet(new ShardIdentifier("foo", "member-5", shardMrgIDSuffix).toString(),
                    new ShardIdentifier("foo", "member-6", shardMrgIDSuffix).toString()),
                    shardBuilder.getPeerAddresses().keySet());
            assertEquals("ShardIdentifier", new ShardIdentifier("foo", "member-1", shardMrgIDSuffix),
                    shardBuilder.getId());
            assertSame("schemaContext", schemaContext, shardBuilder.getSchemaContext());

            // Send CreateShard with same name - should return Success with a message.

            shardManager.tell(new CreateShard(config, shardBuilder, null), getRef());

            Success success = expectMsgClass(duration("5 seconds"), Success.class);
            assertNotNull("Success status is null", success.status());
        }};
    }

    @Test
    public void testOnCreateShardWithLocalMemberNotInShardConfig() {
        new JavaTestKit(getSystem()) {{
            datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

            ActorRef shardManager = getSystem().actorOf(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), ActorRef.noSender());

            Shard.Builder shardBuilder = Shard.builder();
            ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("foo-ns"), "foo-module",
                    "foo", null, Arrays.asList("member-5", "member-6"));

            shardManager.tell(new CreateShard(config, shardBuilder, null), getRef());
            expectMsgClass(duration("5 seconds"), Success.class);

            shardManager.tell(new FindLocalShard("foo", true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            assertEquals("peerMembers size", 0, shardBuilder.getPeerAddresses().size());
            assertEquals("schemaContext", DisableElectionsRaftPolicy.class.getName(),
                    shardBuilder.getDatastoreContext().getShardRaftConfig().getCustomRaftPolicyImplementationClass());
        }};
    }

    @Test
    public void testOnCreateShardWithNoInitialSchemaContext() {
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = getSystem().actorOf(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            Shard.Builder shardBuilder = Shard.builder();

            ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("foo-ns"), "foo-module",
                    "foo", null, Arrays.asList("member-1"));
            shardManager.tell(new CreateShard(config, shardBuilder, null), getRef());

            expectMsgClass(duration("5 seconds"), Success.class);

            SchemaContext schemaContext = TestModel.createTestContext();
            shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

            shardManager.tell(new FindLocalShard("foo", true), getRef());

            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            assertSame("schemaContext", schemaContext, shardBuilder.getSchemaContext());
            assertNotNull("schemaContext is null", shardBuilder.getDatastoreContext());
        }};
    }

    @Test
    public void testGetSnapshot() throws Throwable {
        JavaTestKit kit = new JavaTestKit(getSystem());

        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                   put("shard1", Arrays.asList("member-1")).
                   put("shard2", Arrays.asList("member-1")).build());

        ActorRef shardManager = getSystem().actorOf(newShardMgrProps(mockConfig).withDispatcher(
                Dispatchers.DefaultDispatcherId()));

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());
        Failure failure = kit.expectMsgClass(Failure.class);
        assertEquals("Failure cause type", IllegalStateException.class, failure.cause().getClass());

        kit = new JavaTestKit(getSystem());

        shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), ActorRef.noSender());

        shardManager.tell(new FindLocalShard("shard1", true), kit.getRef());
        kit.expectMsgClass(LocalShardFound.class);
        shardManager.tell(new FindLocalShard("shard2", true), kit.getRef());
        kit.expectMsgClass(LocalShardFound.class);

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());

        DatastoreSnapshot datastoreSnapshot = kit.expectMsgClass(DatastoreSnapshot.class);

        assertEquals("getType", shardMrgIDSuffix, datastoreSnapshot.getType());
        List<ShardSnapshot> shardSnapshots = datastoreSnapshot.getShardSnapshots();
        Set<String> actualShardNames = new HashSet<>();
        for(ShardSnapshot s: shardSnapshots) {
            actualShardNames.add(s.getName());
        }

        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2"), actualShardNames);

        shardManager.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    public void testAddShardReplicaForNonExistentShardConfig() throws Exception {
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
            newReplicaShardManager.underlyingActor()
                .verifySnapshotPersisted(Sets.newHashSet("default", "astronauts"));
            expectMsgClass(duration("5 seconds"), Status.Success.class);
        }};

        JavaTestKit.shutdownActorSystem(system1);
        JavaTestKit.shutdownActorSystem(system2);
    }

    @Test
    public void testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader() throws Exception {
        new JavaTestKit(getSystem()) {{
            TestActorRef<ForwardingShardManager> shardManager = TestActorRef.create(getSystem(),
                    newPropsShardMgrWithMockShardActor(), shardMgrID);

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            String leaderId = "leader-member-shard-default-" + shardMrgIDSuffix;
            AddServerReply addServerReply = new AddServerReply(ServerChangeStatus.ALREADY_EXISTS, null);
            ActorRef leaderShardActor = shardManager.underlyingActor().getContext().actorOf(
                    Props.create(MockRespondActor.class, addServerReply), leaderId);

            MockClusterWrapper.sendMemberUp(shardManager, "leader-member", leaderShardActor.path().toString());

            String newReplicaId = "member-1-shard-default-" + shardMrgIDSuffix;
            shardManager.tell(new RoleChangeNotification(newReplicaId,
                    RaftState.Candidate.name(), RaftState.Follower.name()), mockShardActor);
            shardManager.tell(new ShardLeaderStateChanged(newReplicaId, leaderId, Optional.<DataTree>absent(),
                    DataStoreVersions.CURRENT_VERSION), mockShardActor);

            shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), getRef());

            MessageCollectorActor.expectFirstMatching(leaderShardActor, AddServer.class);

            Failure resp = expectMsgClass(duration("5 seconds"), Failure.class);
            assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            // Send message again to verify previous in progress state is cleared

            shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), getRef());
            resp = expectMsgClass(duration("5 seconds"), Failure.class);
            assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

            // Send message again with an AddServer timeout to verify the pre-existing shard actor isn't terminated.

            shardManager.tell(newDatastoreContextFactory(datastoreContextBuilder.
                    shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build()), getRef());
            leaderShardActor.tell(MockRespondActor.CLEAR_RESPONSE, ActorRef.noSender());
            shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), getRef());
            expectMsgClass(duration("5 seconds"), Failure.class);

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            leaderShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testAddShardReplicaWithPreExistingLocalReplicaLeader() throws Exception {
        new JavaTestKit(getSystem()) {{
            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
            ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);
            shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, Optional.of(mock(DataTree.class)),
                    DataStoreVersions.CURRENT_VERSION), getRef());
            shardManager.tell((new RoleChangeNotification(memberId, RaftState.Candidate.name(),
                    RaftState.Leader.name())), mockShardActor);

            shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), getRef());
            Failure resp = expectMsgClass(duration("5 seconds"), Failure.class);
            assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);
        }};
    }

    @Test
    public void testAddShardReplicaWithAddServerReplyFailure() throws Exception {
        new JavaTestKit(getSystem()) {{
            JavaTestKit mockShardLeaderKit = new JavaTestKit(getSystem());

            MockConfiguration mockConfig =
                    new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                       put("astronauts", Arrays.asList("member-2")).build());

            ActorRef mockNewReplicaShardActor = newMockShardActor(getSystem(), "astronauts", "member-1");
            TestActorRef<ForwardingShardManager> shardManager = TestActorRef.create(getSystem(),
                    newPropsShardMgrWithMockShardActor("newReplicaShardManager", mockNewReplicaShardActor,
                            new MockClusterWrapper(), mockConfig), shardMgrID);
            shardManager.underlyingActor().setMessageInterceptor(newFindPrimaryInterceptor(mockShardLeaderKit.getRef()));

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            JavaTestKit terminateWatcher = new JavaTestKit(getSystem());
            terminateWatcher.watch(mockNewReplicaShardActor);

            shardManager.tell(new AddShardReplica("astronauts"), getRef());

            AddServer addServerMsg = mockShardLeaderKit.expectMsgClass(AddServer.class);
            assertEquals("AddServer serverId", "member-1-shard-astronauts-" + shardMrgIDSuffix,
                    addServerMsg.getNewServerId());
            mockShardLeaderKit.reply(new AddServerReply(ServerChangeStatus.TIMEOUT, null));

            Failure failure = expectMsgClass(duration("5 seconds"), Failure.class);
            assertEquals("Failure cause", TimeoutException.class, failure.cause().getClass());

            shardManager.tell(new FindLocalShard("astronauts", false), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            terminateWatcher.expectTerminated(mockNewReplicaShardActor);

            shardManager.tell(new AddShardReplica("astronauts"), getRef());
            mockShardLeaderKit.expectMsgClass(AddServer.class);
            mockShardLeaderKit.reply(new AddServerReply(ServerChangeStatus.NO_LEADER, null));
            failure = expectMsgClass(duration("5 seconds"), Failure.class);
            assertEquals("Failure cause", NoShardLeaderException.class, failure.cause().getClass());
        }};
    }

    @Test
    public void testAddShardReplicaWithAlreadyInProgress() throws Exception {
        new JavaTestKit(getSystem()) {{
            JavaTestKit mockShardLeaderKit = new JavaTestKit(getSystem());
            JavaTestKit secondRequestKit = new JavaTestKit(getSystem());

            MockConfiguration mockConfig =
                    new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                       put("astronauts", Arrays.asList("member-2")).build());

            TestActorRef<ForwardingShardManager> shardManager = TestActorRef.create(getSystem(),
                    newPropsShardMgrWithMockShardActor("newReplicaShardManager", mockShardActor,
                            new MockClusterWrapper(), mockConfig), shardMgrID);
            shardManager.underlyingActor().setMessageInterceptor(newFindPrimaryInterceptor(mockShardLeaderKit.getRef()));

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new AddShardReplica("astronauts"), getRef());

            mockShardLeaderKit.expectMsgClass(AddServer.class);

            shardManager.tell(new AddShardReplica("astronauts"), secondRequestKit.getRef());

            secondRequestKit.expectMsgClass(duration("5 seconds"), Failure.class);
        }};
    }

    @Test
    public void testAddShardReplicaWithFindPrimaryTimeout() throws Exception {
        new JavaTestKit(getSystem()) {{
            MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                       put("astronauts", Arrays.asList("member-2")).build());

            ActorRef newReplicaShardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor(
                    "shardManager", mockShardActor, new MockClusterWrapper(), mockConfig));

            newReplicaShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            MockClusterWrapper.sendMemberUp(newReplicaShardManager, "member-2", getRef().path().toString());

            newReplicaShardManager.tell(new AddShardReplica("astronauts"), getRef());
            Status.Failure resp = expectMsgClass(duration("5 seconds"), Status.Failure.class);
            assertEquals("Failure obtained", true,
                          (resp.cause() instanceof RuntimeException));
        }};
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
            String[] restoredShards = {"default", "astronauts"};
            ShardManagerSnapshot snapshot = new ShardManagerSnapshot(Arrays.asList(restoredShards));
            InMemorySnapshotStore.addSnapshot("shard-manager-" + shardMrgIDSuffix, snapshot);

            //create shardManager to come up with restored data
            TestActorRef<TestShardManager> newRestoredShardManager = TestActorRef.create(getSystem(),
                    newShardMgrProps(mockConfig));

            newRestoredShardManager.underlyingActor().waitForRecoveryComplete();

            newRestoredShardManager.tell(new FindLocalShard("people", false), getRef());
            LocalShardNotFound notFound = expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);
            assertEquals("for uninitialized shard", "people", notFound.getShardName());

            //Verify a local shard is created for the restored shards,
            //although we expect a NotInitializedException for the shards as the actor initialization
            //message is not sent for them
            newRestoredShardManager.tell(new FindLocalShard("default", false), getRef());
            expectMsgClass(duration("5 seconds"), NotInitializedException.class);

            newRestoredShardManager.tell(new FindLocalShard("astronauts", false), getRef());
            expectMsgClass(duration("5 seconds"), NotInitializedException.class);
        }};
    }


    private static class TestShardManager extends ShardManager {
        private final CountDownLatch recoveryComplete = new CountDownLatch(1);

        private TestShardManager(Builder builder) {
            super(builder);
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

        public static Builder builder(DatastoreContext.Builder datastoreContextBuilder) {
            return new Builder(datastoreContextBuilder);
        }

        private static class Builder extends ShardManager.Builder {
            Builder(DatastoreContext.Builder datastoreContextBuilder) {
                cluster(new MockClusterWrapper()).configuration(new MockConfiguration());
                datastoreContextFactory(newDatastoreContextFactory(datastoreContextBuilder.build()));
                waitTillReadyCountdownLatch(ready).primaryShardInfoCache(new PrimaryShardInfoFutureCache());
            }

            @Override
            public Props props() {
                verify();
                return Props.create(TestShardManager.class, this);
            }
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

    interface MessageInterceptor extends Function<Object, Object> {
        boolean canIntercept(Object message);
    }

    private MessageInterceptor newFindPrimaryInterceptor(final ActorRef primaryActor) {
        return new MessageInterceptor(){
            @Override
            public Object apply(Object message) {
                return new RemotePrimaryShardFound(Serialization.serializedActorPath(primaryActor), (short) 1);
            }

            @Override
            public boolean canIntercept(Object message) {
                return message instanceof FindPrimary;
            }
        };
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
        private volatile MessageInterceptor messageInterceptor;

        public ForwardingShardManager(Builder builder, String name, ActorRef shardActor) {
            super(builder);
            this.shardActor = shardActor;
            this.name = name;
        }

        void setMessageInterceptor(MessageInterceptor messageInterceptor) {
            this.messageInterceptor = messageInterceptor;
        }


        @Override
        public void handleCommand(Object message) throws Exception {
            try{
                if(messageInterceptor != null && messageInterceptor.canIntercept(message)) {
                    getSender().tell(messageInterceptor.apply(message), getSelf());
                } else {
                    super.handleCommand(message);
                }
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

        void verifySnapshotPersisted(Set<String> shardList) {
            assertEquals("saveSnapshot invoked", true,
                Uninterruptibles.awaitUninterruptibly(snapshotPersist, 5, TimeUnit.SECONDS));
            assertEquals("Shard Persisted", shardList, Sets.newHashSet(snapshot.getShardList()));
        }
    }

    private static class MockRespondActor extends MessageCollectorActor {
        static final String CLEAR_RESPONSE = "clear-response";

        private volatile Object responseMsg;

        @SuppressWarnings("unused")
        public MockRespondActor() {
        }

        @SuppressWarnings("unused")
        public MockRespondActor(Object responseMsg) {
            this.responseMsg = responseMsg;
        }

        public void updateResponse(Object response) {
            responseMsg = response;
        }

        @Override
        public void onReceive(Object message) throws Exception {
            super.onReceive(message);
            if (message instanceof AddServer) {
                if (responseMsg != null) {
                    getSender().tell(responseMsg, getSelf());
                }
            } if(message.equals(CLEAR_RESPONSE)) {
                responseMsg = null;
            }
        }
    }
}
