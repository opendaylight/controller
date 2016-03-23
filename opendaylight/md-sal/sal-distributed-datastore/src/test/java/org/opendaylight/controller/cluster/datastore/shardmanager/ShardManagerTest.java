/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AddressFromURIString;
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
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapperImpl;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardManager.SchemaContextModules;
import org.opendaylight.controller.cluster.datastore.ShardManagerSnapshot;
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
import org.opendaylight.controller.cluster.datastore.utils.ForwardingActor;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.RemoveServer;
import org.opendaylight.controller.cluster.raft.messages.RemoveServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class ShardManagerTest extends AbstractActorTest {
    private static final Logger LOG = LoggerFactory.getLogger(ShardManagerTest.class);

    private static int ID_COUNTER = 1;

    private final String shardMrgIDSuffix = "config" + ID_COUNTER++;
    private final String shardMgrID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

    @Mock
    private static CountDownLatch ready;

    private static TestActorRef<MessageCollectorActor> mockShardActor;

    private static String mockShardName;

    private final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder().
            dataStoreName(shardMrgIDSuffix).shardInitializationTimeout(600, TimeUnit.MILLISECONDS)
                   .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(6);

    private final Collection<ActorSystem> actorSystems = new ArrayList<>();

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

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

        for(ActorSystem system: actorSystems) {
            JavaTestKit.shutdownActorSystem(system, null, Boolean.TRUE);
        }

        actorFactory.close();
    }

    private ActorSystem newActorSystem(String config) {
        ActorSystem system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig(config));
        actorSystems.add(system);
        return system;
    }

    private ActorRef newMockShardActor(ActorSystem system, String shardName, String memberName) {
        String name = new ShardIdentifier(shardName, memberName,"config").toString();
        if(system == getSystem()) {
            return actorFactory.createTestActor(Props.create(MessageCollectorActor.class), name);
        }

        return TestActorRef.create(system, Props.create(MessageCollectorActor.class), name);
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

    private TestShardManager.Builder newTestShardMgrBuilder() {
        return TestShardManager.builder(datastoreContextBuilder);
    }

    private TestShardManager.Builder newTestShardMgrBuilder(Configuration config) {
        return TestShardManager.builder(datastoreContextBuilder).configuration(config);
    }

    private Props newShardMgrProps(Configuration config) {
        return newTestShardMgrBuilder(config).props();
    }

    private TestShardManager.Builder newTestShardMgrBuilderWithMockShardActor() {
        return newTestShardMgrBuilderWithMockShardActor(mockShardActor);
    }

    private TestShardManager.Builder newTestShardMgrBuilderWithMockShardActor(ActorRef shardActor) {
        return TestShardManager.builder(datastoreContextBuilder).shardActor(shardActor);
    }


    private Props newPropsShardMgrWithMockShardActor() {
        return newTestShardMgrBuilderWithMockShardActor().props();
    }

    private Props newPropsShardMgrWithMockShardActor(ActorRef shardActor) {
        return newTestShardMgrBuilderWithMockShardActor(shardActor).props();
    }


    private TestShardManager newTestShardManager() {
        return newTestShardManager(newShardMgrProps());
    }

    private TestShardManager newTestShardManager(Props props) {
        TestActorRef<TestShardManager> shardManagerActor = actorFactory.createTestActor(props);
        TestShardManager shardManager = shardManagerActor.underlyingActor();
        shardManager.waitForRecoveryComplete();
        return shardManager;
    }

    private static void waitForShardInitialized(ActorRef shardManager, String shardName, JavaTestKit kit) {
        AssertionError last = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 5) {
            try {
                shardManager.tell(new FindLocalShard(shardName, true), kit.getRef());
                kit.expectMsgClass(LocalShardFound.class);
                return;
            } catch(AssertionError e) {
                last = e;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        throw last;
    }

    private static <T> T expectMsgClassOrFailure(Class<T> msgClass, JavaTestKit kit, String msg) {
        Object reply = kit.expectMsgAnyClassOf(JavaTestKit.duration("5 sec"), msgClass, Failure.class);
        if(reply instanceof Failure) {
            throw new AssertionError(msg + " failed", ((Failure)reply).cause());
        }

        return (T)reply;
    }

    @Test
    public void testPerShardDatastoreContext() throws Exception {
        LOG.info("testPerShardDatastoreContext starting");
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

        final TestActorRef<MessageCollectorActor> defaultShardActor = actorFactory.createTestActor(
                Props.create(MessageCollectorActor.class), actorFactory.generateActorId("default"));
        final TestActorRef<MessageCollectorActor> topologyShardActor = actorFactory.createTestActor(
                Props.create(MessageCollectorActor.class), actorFactory.generateActorId("topology"));

        final Map<String, Entry<ActorRef, DatastoreContext>> shardInfoMap = Collections.synchronizedMap(
                new HashMap<String, Entry<ActorRef, DatastoreContext>>());
        shardInfoMap.put("default", new AbstractMap.SimpleEntry<ActorRef, DatastoreContext>(defaultShardActor, null));
        shardInfoMap.put("topology", new AbstractMap.SimpleEntry<ActorRef, DatastoreContext>(topologyShardActor, null));

        final PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();
        final CountDownLatch newShardActorLatch = new CountDownLatch(2);
        class LocalShardManager extends ShardManager {
            public LocalShardManager(AbstractBuilder<?> builder) {
                super(builder);
            }

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
        }

        final Creator<ShardManager> creator = new Creator<ShardManager>() {
            private static final long serialVersionUID = 1L;
            @Override
            public ShardManager create() throws Exception {
                return new LocalShardManager(new GenericBuilder<LocalShardManager>(LocalShardManager.class).
                        datastoreContextFactory(mockFactory).primaryShardInfoCache(primaryShardInfoCache).
                        configuration(mockConfig));
            }
        };

        JavaTestKit kit = new JavaTestKit(getSystem());

        final ActorRef shardManager = actorFactory.createActor(Props.create(
                new DelegatingShardManagerCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()));

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

        LOG.info("testPerShardDatastoreContext ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary("non-existent", false), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryNotFoundException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForLocalLeaderShard() throws Exception {
        LOG.info("testOnReceiveFindPrimaryForLocalLeaderShard starting");
        new JavaTestKit(getSystem()) {{
            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

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
            assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree());
        }};

        LOG.info("testOnReceiveFindPrimaryForLocalLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp() throws Exception {
        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp starting");
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

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

        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForNonLocalLeaderShard() throws Exception {
        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShard starting");
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

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

        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForUninitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NotInitializedException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForInitializedShardWithNoRole() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId() throws Exception {
        LOG.info("testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId starting");
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

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
            assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree());
        }};

        LOG.info("testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId starting");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForShardLeader() throws Exception {
        LOG.info("testOnReceiveFindPrimaryWaitForShardLeader starting");
        datastoreContextBuilder.shardInitializationTimeout(10, TimeUnit.SECONDS);
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

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

        LOG.info("testOnReceiveFindPrimaryWaitForShardLeader ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard() throws Exception {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard starting");
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NotInitializedException.class);

            shardManager.tell(new ActorInitialized(), mockShardActor);

            expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));
        }};

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithCandidateShard() throws Exception {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithCandidateShard starting");
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);
            shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                    null, RaftState.Candidate.name()), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
        }};

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithCandidateShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard() throws Exception {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard starting");
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);
            shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                    null, RaftState.IsolatedLeader.name()), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
        }};

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard() throws Exception {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard starting");
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
        }};

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForRemoteShard() throws Exception {
        LOG.info("testOnReceiveFindPrimaryForRemoteShard starting");
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final TestActorRef<TestShardManager> shardManager1 = TestActorRef.create(system1,
                newTestShardMgrBuilderWithMockShardActor().cluster(
                        new ClusterWrapperImpl(system1)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = newActorSystem("Member2");

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, "astronauts", "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                put("default", Arrays.asList("member-1", "member-2")).
                put("astronauts", Arrays.asList("member-2")).build());

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig2).shardActor(mockShardActor2).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

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

        LOG.info("testOnReceiveFindPrimaryForRemoteShard ending");
    }

    @Test
    public void testShardAvailabilityOnChangeOfMemberReachability() throws Exception {
        LOG.info("testShardAvailabilityOnChangeOfMemberReachability starting");
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor1 = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final TestActorRef<TestShardManager> shardManager1 = TestActorRef.create(system1,
                newTestShardMgrBuilder().shardActor(mockShardActor1).cluster(
                        new ClusterWrapperImpl(system1)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = newActorSystem("Member2");

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, Shard.DEFAULT_NAME, "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
            put("default", Arrays.asList("member-1", "member-2")).build());

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig2).shardActor(mockShardActor2).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

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

            shardManager1.tell(MockClusterWrapper.
                createUnreachableMember("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"), getRef());

            shardManager1.underlyingActor().waitForUnreachableMember();

            PeerDown peerDown = MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerDown.class);
            assertEquals("getMemberName", "member-2", peerDown.getMemberName());
            MessageCollectorActor.clearMessages(mockShardActor1);

            shardManager1.tell(MockClusterWrapper.
                    createMemberRemoved("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"), getRef());

            MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerDown.class);

            shardManager1.tell(new FindPrimary("default", true), getRef());

            expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);

            shardManager1.tell(MockClusterWrapper.
                createReachableMember("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"), getRef());

            shardManager1.underlyingActor().waitForReachableMember();

            PeerUp peerUp = MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerUp.class);
            assertEquals("getMemberName", "member-2", peerUp.getMemberName());
            MessageCollectorActor.clearMessages(mockShardActor1);

            shardManager1.tell(new FindPrimary("default", true), getRef());

            RemotePrimaryShardFound found1 = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
            String path1 = found1.getPrimaryPath();
            assertTrue("Unexpected primary path " + path1, path1.contains("member-2-shard-default-config"));

            shardManager1.tell(MockClusterWrapper.
                    createMemberUp("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"), getRef());

            MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerUp.class);

            // Test FindPrimary wait succeeds after reachable member event.

            shardManager1.tell(MockClusterWrapper.
                    createUnreachableMember("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"), getRef());
            shardManager1.underlyingActor().waitForUnreachableMember();

            shardManager1.tell(new FindPrimary("default", true), getRef());

            shardManager1.tell(MockClusterWrapper.
                    createReachableMember("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"), getRef());

            RemotePrimaryShardFound found2 = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
            String path2 = found2.getPrimaryPath();
            assertTrue("Unexpected primary path " + path2, path2.contains("member-2-shard-default-config"));
        }};

        LOG.info("testShardAvailabilityOnChangeOfMemberReachability ending");
    }

    @Test
    public void testShardAvailabilityChangeOnMemberUnreachableAndLeadershipChange() throws Exception {
        LOG.info("testShardAvailabilityChangeOnMemberUnreachableAndLeadershipChange starting");
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor1 = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();
        final TestActorRef<TestShardManager> shardManager1 = TestActorRef.create(system1,
                newTestShardMgrBuilder().shardActor(mockShardActor1).cluster(
                        new ClusterWrapperImpl(system1)).primaryShardInfoCache(primaryShardInfoCache).props().
                            withDispatcher(Dispatchers.DefaultDispatcherId()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = newActorSystem("Member2");

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, Shard.DEFAULT_NAME, "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
            put("default", Arrays.asList("member-1", "member-2")).build());

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig2).shardActor(mockShardActor2).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

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

            shardManager1.tell(MockClusterWrapper.
                createUnreachableMember("member-2", "akka.tcp://cluster-test@127.0.0.1:2558"), getRef());

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

        LOG.info("testShardAvailabilityChangeOnMemberUnreachableAndLeadershipChange ending");
    }


    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindLocalShard("non-existent", false), getRef());

            LocalShardNotFound notFound = expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            assertEquals("getShardName", "non-existent", notFound.getShardName());
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

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
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NotInitializedException.class);
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardWaitForShardInitialized() throws Exception {
        LOG.info("testOnReceiveFindLocalShardWaitForShardInitialized starting");
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            // We're passing waitUntilInitialized = true to FindLocalShard so the response should be
            // delayed until we send ActorInitialized.
            Future<Object> future = Patterns.ask(shardManager, new FindLocalShard(Shard.DEFAULT_NAME, true),
                    new Timeout(5, TimeUnit.SECONDS));

            shardManager.tell(new ActorInitialized(), mockShardActor);

            Object resp = Await.result(future, duration("5 seconds"));
            assertTrue("Expected: LocalShardFound, Actual: " + resp, resp instanceof LocalShardFound);
        }};

        LOG.info("testOnReceiveFindLocalShardWaitForShardInitialized starting");
    }

    @Test
    public void testOnRecoveryJournalIsCleaned() {
        String persistenceID = "shard-manager-" + shardMrgIDSuffix;
        InMemoryJournal.addEntry(persistenceID, 1L, new SchemaContextModules(ImmutableSet.of("foo")));
        InMemoryJournal.addEntry(persistenceID, 2L, new SchemaContextModules(ImmutableSet.of("bar")));
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
        LOG.info("testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards starting");
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

        LOG.info("testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards ending");
    }

    @Test
    public void testOnReceiveSwitchShardBehavior() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new SwitchShardBehavior(mockShardName, RaftState.Leader, 1000), getRef());

            SwitchBehavior switchBehavior = MessageCollectorActor.expectFirstMatching(mockShardActor, SwitchBehavior.class);

            assertEquals(RaftState.Leader, switchBehavior.getNewState());
            assertEquals(1000, switchBehavior.getNewTerm());
        }};
    }

    @Test
    public void testOnCreateShard() {
        LOG.info("testOnCreateShard starting");
        new JavaTestKit(getSystem()) {{
            datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

            ActorRef shardManager = actorFactory.createActor(newShardMgrProps(
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

        LOG.info("testOnCreateShard ending");
    }

    @Test
    public void testOnCreateShardWithLocalMemberNotInShardConfig() {
        LOG.info("testOnCreateShardWithLocalMemberNotInShardConfig starting");
        new JavaTestKit(getSystem()) {{
            datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

            ActorRef shardManager = actorFactory.createActor(newShardMgrProps(
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

        LOG.info("testOnCreateShardWithLocalMemberNotInShardConfig ending");
    }

    @Test
    public void testOnCreateShardWithNoInitialSchemaContext() {
        LOG.info("testOnCreateShardWithNoInitialSchemaContext starting");
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = actorFactory.createActor(newShardMgrProps(
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

        LOG.info("testOnCreateShardWithNoInitialSchemaContext ending");
    }

    @Test
    public void testGetSnapshot() throws Throwable {
        LOG.info("testGetSnapshot starting");
        JavaTestKit kit = new JavaTestKit(getSystem());

        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                   put("shard1", Arrays.asList("member-1")).
                   put("shard2", Arrays.asList("member-1")).
                   put("astronauts", Collections.<String>emptyList()).build());

        TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(newShardMgrProps(mockConfig).
                withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());
        Failure failure = kit.expectMsgClass(Failure.class);
        assertEquals("Failure cause type", IllegalStateException.class, failure.cause().getClass());

        shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), ActorRef.noSender());

        waitForShardInitialized(shardManager, "shard1", kit);
        waitForShardInitialized(shardManager, "shard2", kit);

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());

        DatastoreSnapshot datastoreSnapshot = expectMsgClassOrFailure(DatastoreSnapshot.class, kit, "GetSnapshot");

        assertEquals("getType", shardMrgIDSuffix, datastoreSnapshot.getType());
        assertNull("Expected null ShardManagerSnapshot", datastoreSnapshot.getShardManagerSnapshot());

        Function<ShardSnapshot, String> shardNameTransformer = new Function<ShardSnapshot, String>() {
            @Override
            public String apply(ShardSnapshot s) {
                return s.getName();
            }
        };

        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2"), Sets.newHashSet(
                Lists.transform(datastoreSnapshot.getShardSnapshots(), shardNameTransformer)));

        // Add a new replica

        JavaTestKit mockShardLeaderKit = new JavaTestKit(getSystem());

        TestShardManager shardManagerInstance = shardManager.underlyingActor();
        shardManagerInstance.setMessageInterceptor(newFindPrimaryInterceptor(mockShardLeaderKit.getRef()));

        shardManager.tell(new AddShardReplica("astronauts"), kit.getRef());
        mockShardLeaderKit.expectMsgClass(AddServer.class);
        mockShardLeaderKit.reply(new AddServerReply(ServerChangeStatus.OK, ""));
        kit.expectMsgClass(Status.Success.class);
        waitForShardInitialized(shardManager, "astronauts", kit);

        // Send another GetSnapshot and verify

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());
        datastoreSnapshot = expectMsgClassOrFailure(DatastoreSnapshot.class, kit, "GetSnapshot");

        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2", "astronauts"), Sets.newHashSet(
                Lists.transform(datastoreSnapshot.getShardSnapshots(), shardNameTransformer)));

        byte[] snapshotBytes = datastoreSnapshot.getShardManagerSnapshot();
        assertNotNull("Expected ShardManagerSnapshot", snapshotBytes);
        ShardManagerSnapshot snapshot = SerializationUtils.deserialize(snapshotBytes);
        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2", "astronauts"),
                Sets.newHashSet(snapshot.getShardList()));

        LOG.info("testGetSnapshot ending");
    }

    @Test
    public void testRestoreFromSnapshot() throws Throwable {
        LOG.info("testRestoreFromSnapshot starting");

        datastoreContextBuilder.shardInitializationTimeout(3, TimeUnit.SECONDS);

        JavaTestKit kit = new JavaTestKit(getSystem());

        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                   put("shard1", Collections.<String>emptyList()).
                   put("shard2", Collections.<String>emptyList()).
                   put("astronauts", Collections.<String>emptyList()).build());


        ShardManagerSnapshot snapshot = new ShardManagerSnapshot(Arrays.asList("shard1", "shard2", "astronauts"));
        DatastoreSnapshot restoreFromSnapshot = new DatastoreSnapshot(shardMrgIDSuffix,
                SerializationUtils.serialize(snapshot), Collections.<ShardSnapshot>emptyList());
        TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(newTestShardMgrBuilder(mockConfig).
                restoreFromSnapshot(restoreFromSnapshot).props().withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.underlyingActor().waitForRecoveryComplete();

        shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), ActorRef.noSender());

        waitForShardInitialized(shardManager, "shard1", kit);
        waitForShardInitialized(shardManager, "shard2", kit);
        waitForShardInitialized(shardManager, "astronauts", kit);

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());

        DatastoreSnapshot datastoreSnapshot = expectMsgClassOrFailure(DatastoreSnapshot.class, kit, "GetSnapshot");

        assertEquals("getType", shardMrgIDSuffix, datastoreSnapshot.getType());

        byte[] snapshotBytes = datastoreSnapshot.getShardManagerSnapshot();
        assertNotNull("Expected ShardManagerSnapshot", snapshotBytes);
        snapshot = SerializationUtils.deserialize(snapshotBytes);
        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2", "astronauts"),
                Sets.newHashSet(snapshot.getShardList()));

        LOG.info("testRestoreFromSnapshot ending");
    }

    @Test
    public void testAddShardReplicaForNonExistentShardConfig() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = actorFactory.createActor(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            shardManager.tell(new AddShardReplica("model-inventory"), getRef());
            Status.Failure resp = expectMsgClass(duration("2 seconds"), Status.Failure.class);

            assertEquals("Failure obtained", true,
                          (resp.cause() instanceof IllegalArgumentException));
        }};
    }

    @Test
    public void testAddShardReplica() throws Exception {
        LOG.info("testAddShardReplica starting");
        MockConfiguration mockConfig =
                new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                   put("default", Arrays.asList("member-1", "member-2")).
                   put("astronauts", Arrays.asList("member-2")).build());

        final String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();
        datastoreContextBuilder.shardManagerPersistenceId(shardManagerID);

        // Create an ActorSystem ShardManager actor for member-1.
        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        ActorRef mockDefaultShardActor = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");
        final TestActorRef<TestShardManager> newReplicaShardManager = TestActorRef.create(system1,
                newTestShardMgrBuilder(mockConfig).shardActor(mockDefaultShardActor).cluster(
                        new ClusterWrapperImpl(system1)).props().withDispatcher(Dispatchers.DefaultDispatcherId()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.
        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        String name = new ShardIdentifier("astronauts", "member-2", "config").toString();
        final TestActorRef<MockRespondActor> mockShardLeaderActor =
                TestActorRef.create(system2, Props.create(MockRespondActor.class).
                        withDispatcher(Dispatchers.DefaultDispatcherId()), name);
        final TestActorRef<TestShardManager> leaderShardManager = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig).shardActor(mockShardLeaderActor).cluster(
                        new ClusterWrapperImpl(system2)).props().
                            withDispatcher(Dispatchers.DefaultDispatcherId()), shardManagerID);

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

            //Have a dummy snapshot to be overwritten by the new data persisted.
            String[] restoredShards = {"default", "people"};
            ShardManagerSnapshot snapshot = new ShardManagerSnapshot(Arrays.asList(restoredShards));
            InMemorySnapshotStore.addSnapshot(shardManagerID, snapshot);
            Uninterruptibles.sleepUninterruptibly(2, TimeUnit.MILLISECONDS);

            InMemorySnapshotStore.addSnapshotSavedLatch(shardManagerID);
            InMemorySnapshotStore.addSnapshotDeletedLatch(shardManagerID);

            //construct a mock response message
            AddServerReply response = new AddServerReply(ServerChangeStatus.OK, memberId2);
            mockShardLeaderActor.underlyingActor().updateResponse(response);
            newReplicaShardManager.tell(new AddShardReplica("astronauts"), getRef());
            AddServer addServerMsg = MessageCollectorActor.expectFirstMatching(mockShardLeaderActor,
                AddServer.class);
            String addServerId = "member-1-shard-astronauts-" + shardMrgIDSuffix;
            assertEquals("AddServer serverId", addServerId, addServerMsg.getNewServerId());
            expectMsgClass(duration("5 seconds"), Status.Success.class);

            InMemorySnapshotStore.waitForSavedSnapshot(shardManagerID, ShardManagerSnapshot.class);
            InMemorySnapshotStore.waitForDeletedSnapshot(shardManagerID);
            List<ShardManagerSnapshot> persistedSnapshots =
                InMemorySnapshotStore.getSnapshots(shardManagerID, ShardManagerSnapshot.class);
            assertEquals("Number of snapshots persisted", 1, persistedSnapshots.size());
            ShardManagerSnapshot shardManagerSnapshot = persistedSnapshots.get(0);
            assertEquals("Persisted local shards", Sets.newHashSet("default", "astronauts"),
                    Sets.newHashSet(shardManagerSnapshot.getShardList()));
        }};
        LOG.info("testAddShardReplica ending");
    }

    @Test
    public void testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader() throws Exception {
        LOG.info("testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader starting");
        new JavaTestKit(getSystem()) {{
            TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(
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
        }};

        LOG.info("testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader ending");
    }

    @Test
    public void testAddShardReplicaWithPreExistingLocalReplicaLeader() throws Exception {
        LOG.info("testAddShardReplicaWithPreExistingLocalReplicaLeader starting");
        new JavaTestKit(getSystem()) {{
            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
            ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

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

        LOG.info("testAddShardReplicaWithPreExistingLocalReplicaLeader ending");
    }

    @Test
    public void testAddShardReplicaWithAddServerReplyFailure() throws Exception {
        LOG.info("testAddShardReplicaWithAddServerReplyFailure starting");
        new JavaTestKit(getSystem()) {{
            JavaTestKit mockShardLeaderKit = new JavaTestKit(getSystem());

            MockConfiguration mockConfig =
                    new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                       put("astronauts", Arrays.asList("member-2")).build());

            ActorRef mockNewReplicaShardActor = newMockShardActor(getSystem(), "astronauts", "member-1");
            final TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(
                    newTestShardMgrBuilder(mockConfig).shardActor(mockNewReplicaShardActor).props(), shardMgrID);
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

        LOG.info("testAddShardReplicaWithAddServerReplyFailure ending");
    }

    @Test
    public void testAddShardReplicaWithAlreadyInProgress() throws Exception {
        testServerChangeWhenAlreadyInProgress("astronauts", new AddShardReplica("astronauts"),
                AddServer.class, new AddShardReplica("astronauts"));
    }

    @Test
    public void testAddShardReplicaWithFindPrimaryTimeout() throws Exception {
        LOG.info("testAddShardReplicaWithFindPrimaryTimeout starting");
        datastoreContextBuilder.shardInitializationTimeout(100, TimeUnit.MILLISECONDS);
        new JavaTestKit(getSystem()) {{
            MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                       put("astronauts", Arrays.asList("member-2")).build());

            final ActorRef newReplicaShardManager = actorFactory.createActor(newTestShardMgrBuilder(mockConfig).
                    shardActor(mockShardActor).props(), shardMgrID);

            newReplicaShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            MockClusterWrapper.sendMemberUp(newReplicaShardManager, "member-2",
                    AddressFromURIString.parse("akka.tcp://non-existent@127.0.0.1:5").toString());

            newReplicaShardManager.tell(new AddShardReplica("astronauts"), getRef());
            Status.Failure resp = expectMsgClass(duration("5 seconds"), Status.Failure.class);
            assertEquals("Failure obtained", true,
                          (resp.cause() instanceof RuntimeException));
        }};

        LOG.info("testAddShardReplicaWithFindPrimaryTimeout ending");
    }

    @Test
    public void testRemoveShardReplicaForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef shardManager = actorFactory.createActor(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            shardManager.tell(new RemoveShardReplica("model-inventory", "member-1"), getRef());
            Status.Failure resp = expectMsgClass(duration("10 seconds"), Status.Failure.class);
            assertEquals("Failure obtained", true,
                         (resp.cause() instanceof PrimaryNotFoundException));
        }};
    }

    @Test
    /**
     * Primary is Local
     */
    public void testRemoveShardReplicaLocal() throws Exception {
        new JavaTestKit(getSystem()) {{
            String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

            final TestActorRef<MockRespondActor> respondActor =
                    TestActorRef.create(getSystem(), Props.create(MockRespondActor.class), memberId);

            ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor(respondActor));

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), respondActor);
            shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, Optional.of(mock(DataTree.class)),
                    DataStoreVersions.CURRENT_VERSION), getRef());
            shardManager.tell((new RoleChangeNotification(memberId, RaftState.Candidate.name(),
                    RaftState.Leader.name())), respondActor);

            respondActor.underlyingActor().updateResponse(new RemoveServerReply(ServerChangeStatus.OK, null));
            shardManager.tell(new RemoveShardReplica(Shard.DEFAULT_NAME, "member-1"), getRef());
            final RemoveServer removeServer = MessageCollectorActor.expectFirstMatching(respondActor, RemoveServer.class);
            assertEquals(new ShardIdentifier("default", "member-1", shardMrgIDSuffix).toString(),
                    removeServer.getServerId());
            expectMsgClass(duration("5 seconds"), Success.class);
        }};
    }

    @Test
    public void testRemoveShardReplicaRemote() throws Exception {
        MockConfiguration mockConfig =
                new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                        put("default", Arrays.asList("member-1", "member-2")).
                        put("astronauts", Arrays.asList("member-1")).build());

        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.
        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        ActorRef mockDefaultShardActor = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final TestActorRef<TestShardManager> newReplicaShardManager = TestActorRef.create(system1,
                newTestShardMgrBuilder().configuration(mockConfig).shardActor(mockDefaultShardActor).cluster(
                        new ClusterWrapperImpl(system1)).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.
        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        String name = new ShardIdentifier("default", "member-2", shardMrgIDSuffix).toString();
        final TestActorRef<MockRespondActor> mockShardLeaderActor =
                TestActorRef.create(system2, Props.create(MockRespondActor.class), name);

        LOG.error("Mock Shard Leader Actor : {}", mockShardLeaderActor);

        final TestActorRef<TestShardManager> leaderShardManager = TestActorRef.create(system2,
                newTestShardMgrBuilder().configuration(mockConfig).shardActor(mockShardLeaderActor).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Because mockShardLeaderActor is created at the top level of the actor system it has an address like so,
        //    akka.tcp://cluster-test@127.0.0.1:2559/user/member-2-shard-default-config1
        // However when a shard manager has a local shard which is a follower and a leader that is remote it will
        // try to compute an address for the remote shard leader using the ShardPeerAddressResolver. This address will
        // look like so,
        //    akka.tcp://cluster-test@127.0.0.1:2559/user/shardmanager-config1/member-2-shard-default-config1
        // In this specific case if we did a FindPrimary for shard default from member-1 we would come up
        // with the address of an actor which does not exist, therefore any message sent to that actor would go to
        // dead letters.
        // To work around this problem we create a ForwardingActor with the right address and pass to it the
        // mockShardLeaderActor. The ForwardingActor simply forwards all messages to the mockShardLeaderActor and every
        // thing works as expected
        final ActorRef actorRef = leaderShardManager.underlyingActor().context()
                .actorOf(Props.create(ForwardingActor.class, mockShardLeaderActor), "member-2-shard-default-" + shardMrgIDSuffix);

        LOG.error("Forwarding actor : {}", actorRef);

        new JavaTestKit(system1) {{

            newReplicaShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            leaderShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            leaderShardManager.tell(new ActorInitialized(), mockShardLeaderActor);
            newReplicaShardManager.tell(new ActorInitialized(), mockShardLeaderActor);

            String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
            short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
            leaderShardManager.tell(new ShardLeaderStateChanged(memberId2, memberId2,
                    Optional.of(mock(DataTree.class)), leaderVersion), mockShardLeaderActor);
            leaderShardManager.tell(new RoleChangeNotification(memberId2,
                    RaftState.Candidate.name(), RaftState.Leader.name()), mockShardLeaderActor);

            String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
            newReplicaShardManager.tell(new ShardLeaderStateChanged(memberId1, memberId2,
                    Optional.of(mock(DataTree.class)), leaderVersion), mockShardActor);
            newReplicaShardManager.tell(new RoleChangeNotification(memberId1,
                    RaftState.Candidate.name(), RaftState.Follower.name()), mockShardActor);

            newReplicaShardManager.underlyingActor().waitForMemberUp();
            leaderShardManager.underlyingActor().waitForMemberUp();

            //construct a mock response message
            RemoveServerReply response = new RemoveServerReply(ServerChangeStatus.OK, memberId2);
            mockShardLeaderActor.underlyingActor().updateResponse(response);
            newReplicaShardManager.tell(new RemoveShardReplica("default", "member-1"), getRef());
            RemoveServer removeServer = MessageCollectorActor.expectFirstMatching(mockShardLeaderActor,
                    RemoveServer.class);
            String removeServerId = new ShardIdentifier("default", "member-1", shardMrgIDSuffix).toString();
            assertEquals("RemoveServer serverId", removeServerId, removeServer.getServerId());
            expectMsgClass(duration("5 seconds"), Status.Success.class);
        }};

    }

    @Test
    public void testRemoveShardReplicaWhenAnotherRemoveShardReplicaAlreadyInProgress() throws Exception {
        testServerChangeWhenAlreadyInProgress("astronauts", new RemoveShardReplica("astronauts", "member-2"),
                RemoveServer.class, new RemoveShardReplica("astronauts", "member-3"));
    }

    @Test
    public void testRemoveShardReplicaWhenAddShardReplicaAlreadyInProgress() throws Exception {
        testServerChangeWhenAlreadyInProgress("astronauts", new AddShardReplica("astronauts"),
                AddServer.class, new RemoveShardReplica("astronauts", "member-2"));
    }


    public void testServerChangeWhenAlreadyInProgress(final String shardName, final Object firstServerChange,
                                                      final Class<?> firstForwardedServerChangeClass,
                                                      final Object secondServerChange) throws Exception {
        new JavaTestKit(getSystem()) {{
            JavaTestKit mockShardLeaderKit = new JavaTestKit(getSystem());
            JavaTestKit secondRequestKit = new JavaTestKit(getSystem());

            MockConfiguration mockConfig =
                    new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                            put(shardName, Arrays.asList("member-2")).build());

            final TestActorRef<TestShardManager> shardManager = TestActorRef.create(getSystem(),
                    newTestShardMgrBuilder().configuration(mockConfig).shardActor(mockShardActor).cluster(
                            new MockClusterWrapper()).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    shardMgrID);

            shardManager.underlyingActor().setMessageInterceptor(newFindPrimaryInterceptor(mockShardLeaderKit.getRef()));

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(firstServerChange, getRef());

            mockShardLeaderKit.expectMsgClass(firstForwardedServerChangeClass);

            shardManager.tell(secondServerChange, secondRequestKit.getRef());

            secondRequestKit.expectMsgClass(duration("5 seconds"), Failure.class);
        }};
    }

    @Test
    public void testServerRemovedShardActorNotRunning() throws Exception {
        LOG.info("testServerRemovedShardActorNotRunning starting");
        new JavaTestKit(getSystem()) {{
            MockConfiguration mockConfig =
                    new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                            put("default", Arrays.asList("member-1", "member-2")).
                            put("astronauts", Arrays.asList("member-2")).
                            put("people", Arrays.asList("member-1", "member-2")).build());

            TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(newShardMgrProps(mockConfig));

            shardManager.underlyingActor().waitForRecoveryComplete();
            shardManager.tell(new FindLocalShard("people", false), getRef());
            expectMsgClass(duration("5 seconds"), NotInitializedException.class);

            shardManager.tell(new FindLocalShard("default", false), getRef());
            expectMsgClass(duration("5 seconds"), NotInitializedException.class);

            // Removed the default shard replica from member-1
            ShardIdentifier.Builder builder = new ShardIdentifier.Builder();
            ShardIdentifier shardId = builder.shardName("default").memberName("member-1").type(shardMrgIDSuffix).build();
            shardManager.tell(new ServerRemoved(shardId.toString()), getRef());

            shardManager.underlyingActor().verifySnapshotPersisted(Sets.newHashSet("people"));
        }};

        LOG.info("testServerRemovedShardActorNotRunning ending");
    }

    @Test
    public void testServerRemovedShardActorRunning() throws Exception {
        LOG.info("testServerRemovedShardActorRunning starting");
        new JavaTestKit(getSystem()) {{
            MockConfiguration mockConfig =
                    new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                            put("default", Arrays.asList("member-1", "member-2")).
                            put("astronauts", Arrays.asList("member-2")).
                            put("people", Arrays.asList("member-1", "member-2")).build());

            String shardId = ShardIdentifier.builder().shardName("default").memberName("member-1").
                    type(shardMrgIDSuffix).build().toString();
            TestActorRef<MessageCollectorActor> shard = actorFactory.createTestActor(
                    MessageCollectorActor.props(), shardId);

            TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(
                    newTestShardMgrBuilder(mockConfig).addShardActor("default", shard).props());

            shardManager.underlyingActor().waitForRecoveryComplete();

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), shard);

            waitForShardInitialized(shardManager, "people", this);
            waitForShardInitialized(shardManager, "default", this);

            // Removed the default shard replica from member-1
            shardManager.tell(new ServerRemoved(shardId), getRef());

            shardManager.underlyingActor().verifySnapshotPersisted(Sets.newHashSet("people"));

            MessageCollectorActor.expectFirstMatching(shard, Shutdown.class);
        }};

        LOG.info("testServerRemovedShardActorRunning ending");
    }


    @Test
    public void testShardPersistenceWithRestoredData() throws Exception {
        LOG.info("testShardPersistenceWithRestoredData starting");
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
            TestActorRef<TestShardManager> newRestoredShardManager = actorFactory.createTestActor(
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

        LOG.info("testShardPersistenceWithRestoredData ending");
    }

    @Test
    public void testShutDown() throws Exception {
        LOG.info("testShutDown starting");
        new JavaTestKit(getSystem()) {{
            MockConfiguration mockConfig =
                    new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                            put("shard1", Arrays.asList("member-1")).
                            put("shard2", Arrays.asList("member-1")).build());

            String shardId1 = ShardIdentifier.builder().shardName("shard1").memberName("member-1").
                    type(shardMrgIDSuffix).build().toString();
            TestActorRef<MessageCollectorActor> shard1 = actorFactory.createTestActor(
                    MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()), shardId1);

            String shardId2 = ShardIdentifier.builder().shardName("shard2").memberName("member-1").
                    type(shardMrgIDSuffix).build().toString();
            TestActorRef<MessageCollectorActor> shard2 = actorFactory.createTestActor(
                    MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()), shardId2);

            TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(newTestShardMgrBuilder(
                    mockConfig).addShardActor("shard1", shard1).addShardActor("shard2", shard2).props().
                        withDispatcher(Dispatchers.DefaultDispatcherId()));

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), shard1);
            shardManager.tell(new ActorInitialized(), shard2);

            FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
            Future<Boolean> stopFuture = Patterns.gracefulStop(shardManager, duration, Shutdown.INSTANCE);

            MessageCollectorActor.expectFirstMatching(shard1, Shutdown.class);
            MessageCollectorActor.expectFirstMatching(shard2, Shutdown.class);

            try {
                Await.ready(stopFuture, FiniteDuration.create(500, TimeUnit.MILLISECONDS));
                fail("ShardManager actor stopped without waiting for the Shards to be stopped");
            } catch(TimeoutException e) {
                // expected
            }

            actorFactory.killActor(shard1, this);
            actorFactory.killActor(shard2, this);

            Boolean stopped = Await.result(stopFuture, duration);
            assertEquals("Stopped", Boolean.TRUE, stopped);
        }};

        LOG.info("testShutDown ending");
    }

    private static class TestShardManager extends ShardManager {
        private final CountDownLatch recoveryComplete = new CountDownLatch(1);
        private final CountDownLatch snapshotPersist = new CountDownLatch(1);
        private ShardManagerSnapshot snapshot;
        private final Map<String, ActorRef> shardActors;
        private final ActorRef shardActor;
        private CountDownLatch findPrimaryMessageReceived = new CountDownLatch(1);
        private CountDownLatch memberUpReceived = new CountDownLatch(1);
        private CountDownLatch memberRemovedReceived = new CountDownLatch(1);
        private CountDownLatch memberUnreachableReceived = new CountDownLatch(1);
        private CountDownLatch memberReachableReceived = new CountDownLatch(1);
        private volatile MessageInterceptor messageInterceptor;

        private TestShardManager(Builder builder) {
            super(builder);
            shardActor = builder.shardActor;
            shardActors = builder.shardActors;
        }

        @Override
        protected void handleRecover(Object message) throws Exception {
            try {
                super.handleRecover(message);
            } finally {
                if(message instanceof RecoveryCompleted) {
                    recoveryComplete.countDown();
                }
            }
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
                    String role = ((ClusterEvent.MemberUp)message).member().roles().iterator().next();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberUpReceived.countDown();
                    }
                } else if(message instanceof ClusterEvent.MemberRemoved) {
                    String role = ((ClusterEvent.MemberRemoved)message).member().roles().iterator().next();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberRemovedReceived.countDown();
                    }
                } else if(message instanceof ClusterEvent.UnreachableMember) {
                    String role = ((ClusterEvent.UnreachableMember)message).member().roles().iterator().next();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberUnreachableReceived.countDown();
                    }
                } else if(message instanceof ClusterEvent.ReachableMember) {
                    String role = ((ClusterEvent.ReachableMember)message).member().roles().iterator().next();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberReachableReceived.countDown();
                    }
                }
            }
        }

        void setMessageInterceptor(MessageInterceptor messageInterceptor) {
            this.messageInterceptor = messageInterceptor;
        }

        void waitForRecoveryComplete() {
            assertEquals("Recovery complete", true,
                    Uninterruptibles.awaitUninterruptibly(recoveryComplete, 5, TimeUnit.SECONDS));
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

        public static Builder builder(DatastoreContext.Builder datastoreContextBuilder) {
            return new Builder(datastoreContextBuilder);
        }

        private static class Builder extends AbstractGenericBuilder<Builder, TestShardManager> {
            private ActorRef shardActor;
            private final Map<String, ActorRef> shardActors = new HashMap<>();

            Builder(DatastoreContext.Builder datastoreContextBuilder) {
                super(TestShardManager.class);
                datastoreContextFactory(newDatastoreContextFactory(datastoreContextBuilder.build()));
            }

            Builder shardActor(ActorRef shardActor) {
                this.shardActor = shardActor;
                return this;
            }

            Builder addShardActor(String shardName, ActorRef actorRef){
                shardActors.put(shardName, actorRef);
                return this;
            }
        }

        @Override
        public void saveSnapshot(Object obj) {
            snapshot = (ShardManagerSnapshot) obj;
            snapshotPersist.countDown();
            super.saveSnapshot(obj);
        }

        void verifySnapshotPersisted(Set<String> shardList) {
            assertEquals("saveSnapshot invoked", true,
                    Uninterruptibles.awaitUninterruptibly(snapshotPersist, 5, TimeUnit.SECONDS));
            assertEquals("Shard Persisted", shardList, Sets.newHashSet(snapshot.getShardList()));
        }

        @Override
        protected ActorRef newShardActor(SchemaContext schemaContext, ShardInformation info) {
            if(shardActors.get(info.getShardName()) != null){
                return shardActors.get(info.getShardName());
            }

            if(shardActor != null) {
                return shardActor;
            }

            return super.newShardActor(schemaContext, info);
        }
    }

    private static abstract class AbstractGenericBuilder<T extends AbstractGenericBuilder<T, ?>, C extends ShardManager>
                                                     extends ShardManager.AbstractBuilder<T> {
        private final Class<C> shardManagerClass;

        AbstractGenericBuilder(Class<C> shardManagerClass) {
            this.shardManagerClass = shardManagerClass;
            cluster(new MockClusterWrapper()).configuration(new MockConfiguration()).
                    waitTillReadyCountdownLatch(ready).primaryShardInfoCache(new PrimaryShardInfoFutureCache());
        }

        @Override
        public Props props() {
            verify();
            return Props.create(shardManagerClass, this);
        }
    }

    private static class GenericBuilder<C extends ShardManager> extends AbstractGenericBuilder<GenericBuilder<C>, C> {
        GenericBuilder(Class<C> shardManagerClass) {
            super(shardManagerClass);
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

    private static MessageInterceptor newFindPrimaryInterceptor(final ActorRef primaryActor) {
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

    private static class MockRespondActor extends MessageCollectorActor {
        static final String CLEAR_RESPONSE = "clear-response";
        static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MockRespondActor.class);

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
            if(!"get-all-messages".equals(message)) {
                LOG.debug("Received message : {}", message);
            }
            super.onReceive(message);
            if (message instanceof AddServer && responseMsg != null) {
                getSender().tell(responseMsg, getSelf());
            } else if(message instanceof RemoveServer && responseMsg != null){
                getSender().tell(responseMsg, getSelf());
            } else if(message.equals(CLEAR_RESPONSE)) {
                responseMsg = null;
            }
        }
    }
}
