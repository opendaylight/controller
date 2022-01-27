/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import akka.cluster.Member;
import akka.dispatch.Dispatchers;
import akka.dispatch.OnComplete;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.persistence.RecoveryCompleted;
import akka.serialization.Serialization;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.AbstractMap;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractClusterRefActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapperImpl;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.Shard;
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
import org.opendaylight.controller.cluster.datastore.messages.ChangeShardMembersVotingStatus;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.RemoveShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot;
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
import org.opendaylight.controller.cluster.raft.messages.ChangeServersVotingStatus;
import org.opendaylight.controller.cluster.raft.messages.RemoveServer;
import org.opendaylight.controller.cluster.raft.messages.RemoveServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ShardManagerTest extends AbstractClusterRefActorTest {
    private static final Logger LOG = LoggerFactory.getLogger(ShardManagerTest.class);
    private static final MemberName MEMBER_1 = MemberName.forName("member-1");
    private static final MemberName MEMBER_2 = MemberName.forName("member-2");
    private static final MemberName MEMBER_3 = MemberName.forName("member-3");

    private static int ID_COUNTER = 1;
    private static ActorRef mockShardActor;
    private static ShardIdentifier mockShardName;
    private static SettableFuture<Void> ready;
    private static EffectiveModelContext TEST_SCHEMA_CONTEXT;

    private final String shardMrgIDSuffix = "config" + ID_COUNTER++;
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());
    private final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder()
            .dataStoreName(shardMrgIDSuffix).shardInitializationTimeout(600, TimeUnit.MILLISECONDS)
            .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(6);

    private final String shardMgrID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

    @BeforeClass
    public static void beforeClass() {
        TEST_SCHEMA_CONTEXT = TestModel.createTestContext();
    }

    @AfterClass
    public static void afterClass() {
        TEST_SCHEMA_CONTEXT = null;
    }

    @Before
    public void setUp() {
        ready = SettableFuture.create();

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        if (mockShardActor == null) {
            mockShardName = ShardIdentifier.create(Shard.DEFAULT_NAME, MEMBER_1, "config");
            mockShardActor = getSystem().actorOf(MessageCollectorActor.props(), mockShardName.toString());
        }

        MessageCollectorActor.clearMessages(mockShardActor);
    }

    @After
    public void tearDown() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        mockShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        await().atMost(Duration.ofSeconds(10)).until(mockShardActor::isTerminated);
        mockShardActor = null;

        actorFactory.close();
    }

    private TestShardManager.Builder newTestShardMgrBuilder() {
        return TestShardManager.builder(datastoreContextBuilder).distributedDataStore(mock(DistributedDataStore.class));
    }

    private TestShardManager.Builder newTestShardMgrBuilder(final Configuration config) {
        return TestShardManager.builder(datastoreContextBuilder).configuration(config)
                .distributedDataStore(mock(DistributedDataStore.class));
    }

    private Props newShardMgrProps() {
        return newShardMgrProps(new MockConfiguration());
    }

    private Props newShardMgrProps(final Configuration config) {
        return newTestShardMgrBuilder(config).readinessFuture(ready).props();
    }

    private ActorSystem newActorSystem(final String config) {
        return newActorSystem("cluster-test", config);
    }

    private ActorRef newMockShardActor(final ActorSystem system, final String shardName, final String memberName) {
        String name = ShardIdentifier.create(shardName, MemberName.forName(memberName), "config").toString();
        if (system == getSystem()) {
            return actorFactory.createActor(MessageCollectorActor.props(), name);
        }

        return system.actorOf(MessageCollectorActor.props(), name);
    }

    private static DatastoreContextFactory newDatastoreContextFactory(final DatastoreContext datastoreContext) {
        DatastoreContextFactory mockFactory = mock(DatastoreContextFactory.class);
        doReturn(datastoreContext).when(mockFactory).getBaseDatastoreContext();
        doReturn(datastoreContext).when(mockFactory).getShardDatastoreContext(anyString());
        return mockFactory;
    }

    private TestShardManager.Builder newTestShardMgrBuilderWithMockShardActor() {
        return newTestShardMgrBuilderWithMockShardActor(mockShardActor);
    }

    private TestShardManager.Builder newTestShardMgrBuilderWithMockShardActor(final ActorRef shardActor) {
        return TestShardManager.builder(datastoreContextBuilder).shardActor(shardActor)
                .distributedDataStore(mock(DistributedDataStore.class));
    }


    private Props newPropsShardMgrWithMockShardActor() {
        return newTestShardMgrBuilderWithMockShardActor().props().withDispatcher(
                Dispatchers.DefaultDispatcherId());
    }

    private Props newPropsShardMgrWithMockShardActor(final ActorRef shardActor) {
        return newTestShardMgrBuilderWithMockShardActor(shardActor).props()
                .withDispatcher(Dispatchers.DefaultDispatcherId());
    }


    private TestShardManager newTestShardManager() {
        return newTestShardManager(newShardMgrProps());
    }

    private TestShardManager newTestShardManager(final Props props) {
        TestActorRef<TestShardManager> shardManagerActor = actorFactory.createTestActor(props);
        TestShardManager shardManager = shardManagerActor.underlyingActor();
        shardManager.waitForRecoveryComplete();
        return shardManager;
    }

    private static void waitForShardInitialized(final ActorRef shardManager, final String shardName,
            final TestKit kit) {
        AssertionError last = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            try {
                shardManager.tell(new FindLocalShard(shardName, true), kit.getRef());
                kit.expectMsgClass(LocalShardFound.class);
                return;
            } catch (AssertionError e) {
                last = e;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        throw last;
    }

    @SuppressWarnings("unchecked")
    private static <T> T expectMsgClassOrFailure(final Class<T> msgClass, final TestKit kit, final String msg) {
        Object reply = kit.expectMsgAnyClassOf(kit.duration("5 sec"), msgClass, Failure.class);
        if (reply instanceof Failure) {
            throw new AssertionError(msg + " failed", ((Failure)reply).cause());
        }

        return (T)reply;
    }

    @Test
    public void testPerShardDatastoreContext() throws Exception {
        LOG.info("testPerShardDatastoreContext starting");
        final DatastoreContextFactory mockFactory = newDatastoreContextFactory(
                datastoreContextBuilder.shardElectionTimeoutFactor(5).build());

        doReturn(
                DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).shardElectionTimeoutFactor(6).build())
                .when(mockFactory).getShardDatastoreContext("default");

        doReturn(
                DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).shardElectionTimeoutFactor(7).build())
                .when(mockFactory).getShardDatastoreContext("topology");

        final MockConfiguration mockConfig = new MockConfiguration() {
            @Override
            public Collection<String> getMemberShardNames(final MemberName memberName) {
                return Arrays.asList("default", "topology");
            }

            @Override
            public Collection<MemberName> getMembersFromShardName(final String shardName) {
                return members("member-1");
            }
        };

        final ActorRef defaultShardActor = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("default"));
        final ActorRef topologyShardActor = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("topology"));

        final Map<String, Entry<ActorRef, DatastoreContext>> shardInfoMap = Collections.synchronizedMap(
                new HashMap<String, Entry<ActorRef, DatastoreContext>>());
        shardInfoMap.put("default", new AbstractMap.SimpleEntry<>(defaultShardActor, null));
        shardInfoMap.put("topology", new AbstractMap.SimpleEntry<>(topologyShardActor, null));

        final PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();
        final CountDownLatch newShardActorLatch = new CountDownLatch(2);
        class LocalShardManager extends ShardManager {
            LocalShardManager(final AbstractShardManagerCreator<?> creator) {
                super(creator);
            }

            @Override
            protected ActorRef newShardActor(final ShardInformation info) {
                Entry<ActorRef, DatastoreContext> entry = shardInfoMap.get(info.getShardName());
                ActorRef ref = null;
                if (entry != null) {
                    ref = entry.getKey();
                    entry.setValue(info.getDatastoreContext());
                }

                newShardActorLatch.countDown();
                return ref;
            }
        }

        final Creator<ShardManager> creator = new Creator<>() {
            private static final long serialVersionUID = 1L;
            @Override
            public ShardManager create() {
                return new LocalShardManager(
                        new GenericCreator<>(LocalShardManager.class).datastoreContextFactory(mockFactory)
                                .primaryShardInfoCache(primaryShardInfoCache).configuration(mockConfig));
            }
        };

        final TestKit kit = new TestKit(getSystem());

        final ActorRef shardManager = actorFactory.createActor(Props.create(ShardManager.class,
                new DelegatingShardManagerCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        assertTrue("Shard actors created", newShardActorLatch.await(5, TimeUnit.SECONDS));
        assertEquals("getShardElectionTimeoutFactor", 6,
                shardInfoMap.get("default").getValue().getShardElectionTimeoutFactor());
        assertEquals("getShardElectionTimeoutFactor", 7,
                shardInfoMap.get("topology").getValue().getShardElectionTimeoutFactor());

        DatastoreContextFactory newMockFactory = newDatastoreContextFactory(
                datastoreContextBuilder.shardElectionTimeoutFactor(5).build());
        doReturn(
                DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).shardElectionTimeoutFactor(66).build())
                .when(newMockFactory).getShardDatastoreContext("default");

        doReturn(
                DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).shardElectionTimeoutFactor(77).build())
                .when(newMockFactory).getShardDatastoreContext("topology");

        shardManager.tell(newMockFactory, kit.getRef());

        DatastoreContext newContext = MessageCollectorActor.expectFirstMatching(defaultShardActor,
                DatastoreContext.class);
        assertEquals("getShardElectionTimeoutFactor", 66, newContext.getShardElectionTimeoutFactor());

        newContext = MessageCollectorActor.expectFirstMatching(topologyShardActor, DatastoreContext.class);
        assertEquals("getShardElectionTimeoutFactor", 77, newContext.getShardElectionTimeoutFactor());

        LOG.info("testPerShardDatastoreContext ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForNonExistentShard() {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        shardManager.tell(new FindPrimary("non-existent", false), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), PrimaryNotFoundException.class);
    }

    @Test
    public void testOnReceiveFindPrimaryForLocalLeaderShard() {
        LOG.info("testOnReceiveFindPrimaryForLocalLeaderShard starting");
        final TestKit kit = new TestKit(getSystem());
        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        DataTree mockDataTree = mock(DataTree.class);
        shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mockDataTree,
            DataStoreVersions.CURRENT_VERSION), kit.getRef());

        MessageCollectorActor.expectFirstMatching(mockShardActor, RegisterRoleChangeListener.class);
        shardManager.tell(
            new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), kit.getRef());

        LocalPrimaryShardFound primaryFound = kit.expectMsgClass(Duration.ofSeconds(5),
            LocalPrimaryShardFound.class);
        assertTrue("Unexpected primary path " + primaryFound.getPrimaryPath(),
            primaryFound.getPrimaryPath().contains("member-1-shard-default"));
        assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree());

        LOG.info("testOnReceiveFindPrimaryForLocalLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp() {
        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp starting");
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
        String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.tell(
            new RoleChangeNotification(memberId1, RaftState.Candidate.name(), RaftState.Follower.name()),
            mockShardActor);
        shardManager.tell(new LeaderStateChanged(memberId1, memberId2, DataStoreVersions.CURRENT_VERSION),
            mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), NoShardLeaderException.class);

        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForNonLocalLeaderShard() {
        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShard starting");
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
        MockClusterWrapper.sendMemberUp(shardManager, "member-2", kit.getRef().path().toString());

        String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.tell(
            new RoleChangeNotification(memberId1, RaftState.Candidate.name(), RaftState.Follower.name()),
            mockShardActor);
        short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
        shardManager.tell(new ShardLeaderStateChanged(memberId1, memberId2, leaderVersion), mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), kit.getRef());

        RemotePrimaryShardFound primaryFound = kit.expectMsgClass(Duration.ofSeconds(5), RemotePrimaryShardFound.class);
        assertTrue("Unexpected primary path " + primaryFound.getPrimaryPath(),
            primaryFound.getPrimaryPath().contains("member-2-shard-default"));
        assertEquals("getPrimaryVersion", leaderVersion, primaryFound.getPrimaryVersion());

        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForUninitializedShard() {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), NotInitializedException.class);
    }

    @Test
    public void testOnReceiveFindPrimaryForInitializedShardWithNoRole() {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), NoShardLeaderException.class);
    }

    @Test
    public void testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId() {
        LOG.info("testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId starting");
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.tell(
            new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Follower.name()),
            mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), NoShardLeaderException.class);

        DataTree mockDataTree = mock(DataTree.class);
        shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mockDataTree,
            DataStoreVersions.CURRENT_VERSION), mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), kit.getRef());

        LocalPrimaryShardFound primaryFound = kit.expectMsgClass(Duration.ofSeconds(5),
            LocalPrimaryShardFound.class);
        assertTrue("Unexpected primary path " + primaryFound.getPrimaryPath(),
            primaryFound.getPrimaryPath().contains("member-1-shard-default"));
        assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree());

        LOG.info("testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId starting");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForShardLeader() {
        LOG.info("testOnReceiveFindPrimaryWaitForShardLeader starting");
        datastoreContextBuilder.shardInitializationTimeout(10, TimeUnit.SECONDS);
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        // We're passing waitUntilInitialized = true to FindPrimary so
        // the response should be
        // delayed until we send ActorInitialized and
        // RoleChangeNotification.
        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), kit.getRef());

        kit.expectNoMessage(Duration.ofMillis(150));

        shardManager.tell(new ActorInitialized(), mockShardActor);

        kit.expectNoMessage(Duration.ofMillis(150));

        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.tell(
            new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardActor);

        kit.expectNoMessage(Duration.ofMillis(150));

        DataTree mockDataTree = mock(DataTree.class);
        shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mockDataTree,
            DataStoreVersions.CURRENT_VERSION), mockShardActor);

        LocalPrimaryShardFound primaryFound = kit.expectMsgClass(Duration.ofSeconds(5), LocalPrimaryShardFound.class);
        assertTrue("Unexpected primary path " + primaryFound.getPrimaryPath(),
            primaryFound.getPrimaryPath().contains("member-1-shard-default"));
        assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree());

        kit.expectNoMessage(Duration.ofMillis(200));

        LOG.info("testOnReceiveFindPrimaryWaitForShardLeader ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard() {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard starting");
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(2), NotInitializedException.class);

        shardManager.tell(new ActorInitialized(), mockShardActor);

        kit.expectNoMessage(Duration.ofMillis(200));

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithCandidateShard() {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithCandidateShard starting");
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);
        shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix, null,
            RaftState.Candidate.name()), mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(2), NoShardLeaderException.class);

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithCandidateShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard() {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard starting");
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);
        shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix, null,
            RaftState.IsolatedLeader.name()), mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true),kit. getRef());

        kit.expectMsgClass(Duration.ofSeconds(2), NoShardLeaderException.class);

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard() {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard starting");
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(2), NoShardLeaderException.class);

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForRemoteShard() {
        LOG.info("testOnReceiveFindPrimaryForRemoteShard starting");
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));

        final TestActorRef<TestShardManager> shardManager1 = TestActorRef.create(system1,
                newTestShardMgrBuilderWithMockShardActor().cluster(
                        new ClusterWrapperImpl(system1)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = newActorSystem("Member2");

        Cluster.get(system2).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, "astronauts", "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(
                ImmutableMap.<String, List<String>>builder().put("default", Arrays.asList("member-1", "member-2"))
                        .put("astronauts", Arrays.asList("member-2")).build());

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig2).shardActor(mockShardActor2).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

        final TestKit kit = new TestKit(system1);
        shardManager1.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager2.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        shardManager2.tell(new ActorInitialized(), mockShardActor2);

        String memberId2 = "member-2-shard-astronauts-" + shardMrgIDSuffix;
        short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
        shardManager2.tell(new ShardLeaderStateChanged(memberId2, memberId2, mock(DataTree.class), leaderVersion),
            mockShardActor2);
        shardManager2.tell(new RoleChangeNotification(memberId2, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardActor2);

        shardManager1.underlyingActor().waitForMemberUp();
        shardManager1.tell(new FindPrimary("astronauts", false), kit.getRef());

        RemotePrimaryShardFound found = kit.expectMsgClass(Duration.ofSeconds(5), RemotePrimaryShardFound.class);
        String path = found.getPrimaryPath();
        assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-astronauts-config"));
        assertEquals("getPrimaryVersion", leaderVersion, found.getPrimaryVersion());

        shardManager2.underlyingActor().verifyFindPrimary();

        // This part times out quite a bit on jenkins for some reason

//                Cluster.get(system2).down(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));
//
//                shardManager1.underlyingActor().waitForMemberRemoved();
//
//                shardManager1.tell(new FindPrimary("astronauts", false), getRef());
//
//                expectMsgClass(Duration.ofSeconds(5), PrimaryNotFoundException.class);

        LOG.info("testOnReceiveFindPrimaryForRemoteShard ending");
    }

    @Test
    public void testShardAvailabilityOnChangeOfMemberReachability() {
        LOG.info("testShardAvailabilityOnChangeOfMemberReachability starting");
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor1 = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final TestActorRef<TestShardManager> shardManager1 = TestActorRef.create(system1,
                newTestShardMgrBuilder().shardActor(mockShardActor1).cluster(
                        new ClusterWrapperImpl(system1)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = newActorSystem("Member2");

        Cluster.get(system2).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, Shard.DEFAULT_NAME, "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                .put("default", Arrays.asList("member-1", "member-2")).build());

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig2).shardActor(mockShardActor2).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

        final TestKit kit = new TestKit(system1);
        shardManager1.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager2.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager1.tell(new ActorInitialized(), mockShardActor1);
        shardManager2.tell(new ActorInitialized(), mockShardActor2);

        String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
        String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager1.tell(new ShardLeaderStateChanged(memberId1, memberId2, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), mockShardActor1);
        shardManager1.tell(
            new RoleChangeNotification(memberId1, RaftState.Candidate.name(), RaftState.Follower.name()),
            mockShardActor1);
        shardManager2.tell(new ShardLeaderStateChanged(memberId2, memberId2, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), mockShardActor2);
        shardManager2.tell(
            new RoleChangeNotification(memberId2, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardActor2);
        shardManager1.underlyingActor().waitForMemberUp();

        shardManager1.tell(new FindPrimary("default", true), kit.getRef());

        RemotePrimaryShardFound found = kit.expectMsgClass(Duration.ofSeconds(5), RemotePrimaryShardFound.class);
        String path = found.getPrimaryPath();
        assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-default-config"));

        shardManager1.tell(MockClusterWrapper.createUnreachableMember("member-2", "akka://cluster-test@127.0.0.1:2558"),
            kit.getRef());

        shardManager1.underlyingActor().waitForUnreachableMember();
        MessageCollectorActor.clearMessages(mockShardActor1);

        shardManager1.tell(MockClusterWrapper.createMemberRemoved("member-2", "akka://cluster-test@127.0.0.1:2558"),
            kit.getRef());

        shardManager1.tell(new FindPrimary("default", true), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), NoShardLeaderException.class);

        shardManager1.tell(MockClusterWrapper.createReachableMember("member-2", "akka://cluster-test@127.0.0.1:2558"),
            kit.getRef());

        shardManager1.underlyingActor().waitForReachableMember();

        shardManager1.tell(new FindPrimary("default", true), kit.getRef());

        RemotePrimaryShardFound found1 = kit.expectMsgClass(Duration.ofSeconds(5), RemotePrimaryShardFound.class);
        String path1 = found1.getPrimaryPath();
        assertTrue("Unexpected primary path " + path1, path1.contains("member-2-shard-default-config"));

        shardManager1.tell(MockClusterWrapper.createMemberUp("member-2", "akka://cluster-test@127.0.0.1:2558"),
            kit.getRef());

        // Test FindPrimary wait succeeds after reachable member event.

        shardManager1.tell(MockClusterWrapper.createUnreachableMember("member-2",
                "akka://cluster-test@127.0.0.1:2558"), kit.getRef());
        shardManager1.underlyingActor().waitForUnreachableMember();

        shardManager1.tell(new FindPrimary("default", true), kit.getRef());

        shardManager1.tell(
            MockClusterWrapper.createReachableMember("member-2", "akka://cluster-test@127.0.0.1:2558"), kit.getRef());

        RemotePrimaryShardFound found2 = kit.expectMsgClass(Duration.ofSeconds(5), RemotePrimaryShardFound.class);
        String path2 = found2.getPrimaryPath();
        assertTrue("Unexpected primary path " + path2, path2.contains("member-2-shard-default-config"));

        LOG.info("testShardAvailabilityOnChangeOfMemberReachability ending");
    }

    @Test
    public void testShardAvailabilityChangeOnMemberUnreachableAndLeadershipChange() {
        LOG.info("testShardAvailabilityChangeOnMemberUnreachableAndLeadershipChange starting");
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor1 = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();
        final TestActorRef<TestShardManager> shardManager1 = TestActorRef.create(system1,
                newTestShardMgrBuilder().shardActor(mockShardActor1).cluster(new ClusterWrapperImpl(system1))
                        .primaryShardInfoCache(primaryShardInfoCache).props()
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = newActorSystem("Member2");

        Cluster.get(system2).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, Shard.DEFAULT_NAME, "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                .put("default", Arrays.asList("member-1", "member-2")).build());

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig2).shardActor(mockShardActor2).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

        final TestKit kit = new TestKit(system1);
        shardManager1.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager2.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager1.tell(new ActorInitialized(), mockShardActor1);
        shardManager2.tell(new ActorInitialized(), mockShardActor2);

        String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
        String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager1.tell(new ShardLeaderStateChanged(memberId1, memberId2, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), mockShardActor1);
        shardManager1.tell(
            new RoleChangeNotification(memberId1, RaftState.Candidate.name(), RaftState.Follower.name()),
            mockShardActor1);
        shardManager2.tell(new ShardLeaderStateChanged(memberId2, memberId2, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), mockShardActor2);
        shardManager2.tell(
            new RoleChangeNotification(memberId2, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardActor2);
        shardManager1.underlyingActor().waitForMemberUp();

        shardManager1.tell(new FindPrimary("default", true), kit.getRef());

        RemotePrimaryShardFound found = kit.expectMsgClass(Duration.ofSeconds(5), RemotePrimaryShardFound.class);
        String path = found.getPrimaryPath();
        assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-default-config"));

        primaryShardInfoCache.putSuccessful("default", new PrimaryShardInfo(
            system1.actorSelection(mockShardActor1.path()), DataStoreVersions.CURRENT_VERSION));

        shardManager1.tell(MockClusterWrapper.createUnreachableMember("member-2",
                "akka://cluster-test@127.0.0.1:2558"), kit.getRef());

        shardManager1.underlyingActor().waitForUnreachableMember();

        shardManager1.tell(new FindPrimary("default", true), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), NoShardLeaderException.class);

        assertNull("Expected primaryShardInfoCache entry removed",
            primaryShardInfoCache.getIfPresent("default"));

        shardManager1.tell(new ShardLeaderStateChanged(memberId1, memberId1, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), mockShardActor1);
        shardManager1.tell(
            new RoleChangeNotification(memberId1, RaftState.Follower.name(), RaftState.Leader.name()),
            mockShardActor1);

        shardManager1.tell(new FindPrimary("default", true), kit.getRef());

        LocalPrimaryShardFound found1 = kit.expectMsgClass(Duration.ofSeconds(5), LocalPrimaryShardFound.class);
        String path1 = found1.getPrimaryPath();
        assertTrue("Unexpected primary path " + path1, path1.contains("member-1-shard-default-config"));

        LOG.info("testShardAvailabilityChangeOnMemberUnreachableAndLeadershipChange ending");
    }

    @Test
    public void testShardAvailabilityChangeOnMemberWithNameContainedInLeaderIdUnreachable() {
        LOG.info("testShardAvailabilityChangeOnMemberWithNameContainedInLeaderIdUnreachable starting");
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                .put("default", Arrays.asList("member-256", "member-2")).build());

        // Create an ActorSystem, ShardManager and actor for member-256.

        final ActorSystem system256 = newActorSystem("Member256");
        // 2562 is the tcp port of Member256 in src/test/resources/application.conf.
        Cluster.get(system256).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2562"));

        final ActorRef mockShardActor256 = newMockShardActor(system256, Shard.DEFAULT_NAME, "member-256");

        final PrimaryShardInfoFutureCache primaryShardInfoCache = new PrimaryShardInfoFutureCache();

        // ShardManager must be created with shard configuration to let its localShards has shards.
        final TestActorRef<TestShardManager> shardManager256 = TestActorRef.create(system256,
                newTestShardMgrBuilder(mockConfig).shardActor(mockShardActor256)
                        .cluster(new ClusterWrapperImpl(system256))
                        .primaryShardInfoCache(primaryShardInfoCache).props()
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Create an ActorSystem, ShardManager and actor for member-2 whose name is contained in member-256.

        final ActorSystem system2 = newActorSystem("Member2");

        // Join member-2 into the cluster of member-256.
        Cluster.get(system2).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2562"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, Shard.DEFAULT_NAME, "member-2");

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig).shardActor(mockShardActor2).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(
                                Dispatchers.DefaultDispatcherId()), shardManagerID);

        final TestKit kit256 = new TestKit(system256);
        shardManager256.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit256.getRef());
        shardManager2.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit256.getRef());
        shardManager256.tell(new ActorInitialized(), mockShardActor256);
        shardManager2.tell(new ActorInitialized(), mockShardActor2);

        String memberId256 = "member-256-shard-default-" + shardMrgIDSuffix;
        String memberId2   = "member-2-shard-default-"   + shardMrgIDSuffix;
        shardManager256.tell(new ShardLeaderStateChanged(memberId256, memberId256, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), mockShardActor256);
        shardManager256.tell(
            new RoleChangeNotification(memberId256, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardActor256);
        shardManager2.tell(new ShardLeaderStateChanged(memberId2, memberId256, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), mockShardActor2);
        shardManager2.tell(
            new RoleChangeNotification(memberId2, RaftState.Candidate.name(), RaftState.Follower.name()),
            mockShardActor2);
        shardManager256.underlyingActor().waitForMemberUp();

        shardManager256.tell(new FindPrimary("default", true), kit256.getRef());

        LocalPrimaryShardFound found = kit256.expectMsgClass(Duration.ofSeconds(5), LocalPrimaryShardFound.class);
        String path = found.getPrimaryPath();
        assertTrue("Unexpected primary path " + path + " which must on member-256",
            path.contains("member-256-shard-default-config"));

        PrimaryShardInfo primaryShardInfo = new PrimaryShardInfo(
            system256.actorSelection(mockShardActor256.path()), DataStoreVersions.CURRENT_VERSION);
        primaryShardInfoCache.putSuccessful("default", primaryShardInfo);

        // Simulate member-2 become unreachable.
        shardManager256.tell(MockClusterWrapper.createUnreachableMember("member-2",
                "akka://cluster-test@127.0.0.1:2558"), kit256.getRef());
        shardManager256.underlyingActor().waitForUnreachableMember();

        // Make sure leader shard on member-256 is still leader and still in the cache.
        shardManager256.tell(new FindPrimary("default", true), kit256.getRef());
        found = kit256.expectMsgClass(Duration.ofSeconds(5), LocalPrimaryShardFound.class);
        path = found.getPrimaryPath();
        assertTrue("Unexpected primary path " + path + " which must still not on member-256",
            path.contains("member-256-shard-default-config"));
        Future<PrimaryShardInfo> futurePrimaryShard = primaryShardInfoCache.getIfPresent("default");
        futurePrimaryShard.onComplete(new OnComplete<PrimaryShardInfo>() {
            @Override
            public void onComplete(final Throwable failure, final PrimaryShardInfo futurePrimaryShardInfo) {
                if (failure != null) {
                    assertTrue("Primary shard info is unexpectedly removed from primaryShardInfoCache", false);
                } else {
                    assertEquals("Expected primaryShardInfoCache entry",
                        primaryShardInfo, futurePrimaryShardInfo);
                }
            }
        }, system256.dispatchers().defaultGlobalDispatcher());

        LOG.info("testShardAvailabilityChangeOnMemberWithNameContainedInLeaderIdUnreachable ending");
    }

    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        shardManager.tell(new FindLocalShard("non-existent", false), kit.getRef());

        LocalShardNotFound notFound = kit.expectMsgClass(Duration.ofSeconds(5), LocalShardNotFound.class);

        assertEquals("getShardName", "non-existent", notFound.getShardName());
    }

    @Test
    public void testOnReceiveFindLocalShardForExistentShard() {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), kit.getRef());

        LocalShardFound found = kit.expectMsgClass(Duration.ofSeconds(5), LocalShardFound.class);

        assertTrue("Found path contains " + found.getPath().path().toString(),
            found.getPath().path().toString().contains("member-1-shard-default-config"));
    }

    @Test
    public void testOnReceiveFindLocalShardForNotInitializedShard() {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), NotInitializedException.class);
    }

    @Test
    public void testOnReceiveFindLocalShardWaitForShardInitialized() throws Exception {
        LOG.info("testOnReceiveFindLocalShardWaitForShardInitialized starting");
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        // We're passing waitUntilInitialized = true to FindLocalShard
        // so the response should be
        // delayed until we send ActorInitialized.
        Future<Object> future = Patterns.ask(shardManager, new FindLocalShard(Shard.DEFAULT_NAME, true),
            new Timeout(5, TimeUnit.SECONDS));

        shardManager.tell(new ActorInitialized(), mockShardActor);

        Object resp = Await.result(future, kit.duration("5 seconds"));
        assertTrue("Expected: LocalShardFound, Actual: " + resp, resp instanceof LocalShardFound);

        LOG.info("testOnReceiveFindLocalShardWaitForShardInitialized starting");
    }

    @Test
    public void testRoleChangeNotificationAndShardLeaderStateChangedReleaseReady() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.handleCommand(new RoleChangeNotification(
                memberId, RaftState.Candidate.name(), RaftState.Leader.name()));
        assertFalse(ready.isDone());

        shardManager.handleCommand(new ShardLeaderStateChanged(memberId, memberId,
                mock(DataTree.class), DataStoreVersions.CURRENT_VERSION));
        assertTrue(ready.isDone());
    }

    @Test
    public void testRoleChangeNotificationToFollowerWithShardLeaderStateChangedReleaseReady() throws Exception {
        final TestKit kit = new TestKit(getSystem());
        TestShardManager shardManager = newTestShardManager();

        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.handleCommand(new RoleChangeNotification(memberId, null, RaftState.Follower.name()));
        assertFalse(ready.isDone());

        shardManager.handleCommand(MockClusterWrapper.createMemberUp("member-2", kit.getRef().path().toString()));

        shardManager.handleCommand(
            new ShardLeaderStateChanged(memberId, "member-2-shard-default-" + shardMrgIDSuffix,
                mock(DataTree.class), DataStoreVersions.CURRENT_VERSION));
        assertTrue(ready.isDone());
    }

    @Test
    public void testReadyCountDownForMemberUpAfterLeaderStateChanged() throws Exception {
        final TestKit kit = new TestKit(getSystem());
        TestShardManager shardManager = newTestShardManager();

        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.handleCommand(new RoleChangeNotification(memberId, null, RaftState.Follower.name()));
        assertFalse(ready.isDone());

        shardManager.handleCommand(
            new ShardLeaderStateChanged(memberId, "member-2-shard-default-" + shardMrgIDSuffix,
                mock(DataTree.class), DataStoreVersions.CURRENT_VERSION));

        shardManager.handleCommand(MockClusterWrapper.createMemberUp("member-2", kit.getRef().path().toString()));
        assertTrue(ready.isDone());
    }

    @Test
    public void testRoleChangeNotificationDoNothingForUnknownShard() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        shardManager.handleCommand(new RoleChangeNotification("unknown", RaftState.Candidate.name(),
            RaftState.Leader.name()));
        assertFalse(ready.isDone());
    }

    @Test
    public void testByDefaultSyncStatusIsFalse() {
        TestShardManager shardManager = newTestShardManager();

        assertFalse(shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsLeaderSyncStatusIsTrue() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        shardManager.handleCommand(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                RaftState.Follower.name(), RaftState.Leader.name()));

        assertTrue(shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsCandidateSyncStatusIsFalse() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        String shardId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.handleCommand(new RoleChangeNotification(shardId,
                RaftState.Follower.name(), RaftState.Candidate.name()));

        assertFalse(shardManager.getMBean().getSyncStatus());

        // Send a FollowerInitialSyncStatus with status = true for the replica whose current state is candidate
        shardManager.handleCommand(new FollowerInitialSyncUpStatus(
                true, shardId));

        assertFalse(shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsFollowerSyncStatusDependsOnFollowerInitialSyncStatus() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        String shardId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.handleCommand(new RoleChangeNotification(shardId,
                RaftState.Candidate.name(), RaftState.Follower.name()));

        // Initially will be false
        assertFalse(shardManager.getMBean().getSyncStatus());

        // Send status true will make sync status true
        shardManager.handleCommand(new FollowerInitialSyncUpStatus(true, shardId));

        assertTrue(shardManager.getMBean().getSyncStatus());

        // Send status false will make sync status false
        shardManager.handleCommand(new FollowerInitialSyncUpStatus(false, shardId));

        assertFalse(shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards() throws Exception {
        LOG.info("testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards starting");
        TestShardManager shardManager = newTestShardManager(newShardMgrProps(new MockConfiguration() {
            @Override
            public List<String> getMemberShardNames(final MemberName memberName) {
                return Arrays.asList("default", "astronauts");
            }
        }));

        // Initially will be false
        assertFalse(shardManager.getMBean().getSyncStatus());

        // Make default shard leader
        String defaultShardId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.handleCommand(new RoleChangeNotification(defaultShardId,
                RaftState.Follower.name(), RaftState.Leader.name()));

        // default = Leader, astronauts is unknown so sync status remains false
        assertFalse(shardManager.getMBean().getSyncStatus());

        // Make astronauts shard leader as well
        String astronautsShardId = "member-1-shard-astronauts-" + shardMrgIDSuffix;
        shardManager.handleCommand(new RoleChangeNotification(astronautsShardId,
                RaftState.Follower.name(), RaftState.Leader.name()));

        // Now sync status should be true
        assertTrue(shardManager.getMBean().getSyncStatus());

        // Make astronauts a Follower
        shardManager.handleCommand(new RoleChangeNotification(astronautsShardId,
                RaftState.Leader.name(), RaftState.Follower.name()));

        // Sync status is not true
        assertFalse(shardManager.getMBean().getSyncStatus());

        // Make the astronauts follower sync status true
        shardManager.handleCommand(new FollowerInitialSyncUpStatus(true, astronautsShardId));

        // Sync status is now true
        assertTrue(shardManager.getMBean().getSyncStatus());

        LOG.info("testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards ending");
    }

    @Test
    public void testOnReceiveSwitchShardBehavior() {
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        shardManager.tell(new SwitchShardBehavior(mockShardName, RaftState.Leader, 1000), kit.getRef());

        SwitchBehavior switchBehavior = MessageCollectorActor.expectFirstMatching(mockShardActor,
            SwitchBehavior.class);

        assertEquals(RaftState.Leader, switchBehavior.getNewState());
        assertEquals(1000, switchBehavior.getNewTerm());
    }

    private static List<MemberName> members(final String... names) {
        return Arrays.asList(names).stream().map(MemberName::forName).collect(Collectors.toList());
    }

    @Test
    public void testOnCreateShard() {
        LOG.info("testOnCreateShard starting");
        final TestKit kit = new TestKit(getSystem());
        datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

        ActorRef shardManager = actorFactory
                .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                    .withDispatcher(Dispatchers.DefaultDispatcherId()));

        EffectiveModelContext schemaContext = TEST_SCHEMA_CONTEXT;
        shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

        DatastoreContext datastoreContext = DatastoreContext.newBuilder().shardElectionTimeoutFactor(100)
                .persistent(false).build();
        Shard.Builder shardBuilder = Shard.builder();

        ModuleShardConfiguration config = new ModuleShardConfiguration(XMLNamespace.of("foo-ns"), "foo-module",
            "foo", null, members("member-1", "member-5", "member-6"));
        shardManager.tell(new CreateShard(config, shardBuilder, datastoreContext), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), Success.class);

        shardManager.tell(new FindLocalShard("foo", true), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), LocalShardFound.class);

        assertFalse("isRecoveryApplicable", shardBuilder.getDatastoreContext().isPersistent());
        assertTrue("Epxected ShardPeerAddressResolver", shardBuilder.getDatastoreContext().getShardRaftConfig()
            .getPeerAddressResolver() instanceof ShardPeerAddressResolver);
        assertEquals("peerMembers", Sets.newHashSet(
            ShardIdentifier.create("foo", MemberName.forName("member-5"), shardMrgIDSuffix).toString(),
            ShardIdentifier.create("foo", MemberName.forName("member-6"), shardMrgIDSuffix).toString()),
            shardBuilder.getPeerAddresses().keySet());
        assertEquals("ShardIdentifier", ShardIdentifier.create("foo", MEMBER_1, shardMrgIDSuffix),
            shardBuilder.getId());
        assertSame("schemaContext", schemaContext, shardBuilder.getSchemaContext());

        // Send CreateShard with same name - should return Success with
        // a message.

        shardManager.tell(new CreateShard(config, shardBuilder, null), kit.getRef());

        Success success = kit.expectMsgClass(Duration.ofSeconds(5), Success.class);
        assertNotNull("Success status is null", success.status());

        LOG.info("testOnCreateShard ending");
    }

    @Test
    public void testOnCreateShardWithLocalMemberNotInShardConfig() {
        LOG.info("testOnCreateShardWithLocalMemberNotInShardConfig starting");
        final TestKit kit = new TestKit(getSystem());
        datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

        ActorRef shardManager = actorFactory
                .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                    .withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), ActorRef.noSender());

        Shard.Builder shardBuilder = Shard.builder();
        ModuleShardConfiguration config = new ModuleShardConfiguration(XMLNamespace.of("foo-ns"), "foo-module",
            "foo", null, members("member-5", "member-6"));

        shardManager.tell(new CreateShard(config, shardBuilder, null), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), Success.class);

        shardManager.tell(new FindLocalShard("foo", true), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), LocalShardFound.class);

        assertEquals("peerMembers size", 0, shardBuilder.getPeerAddresses().size());
        assertEquals("schemaContext", DisableElectionsRaftPolicy.class.getName(), shardBuilder
            .getDatastoreContext().getShardRaftConfig().getCustomRaftPolicyImplementationClass());

        LOG.info("testOnCreateShardWithLocalMemberNotInShardConfig ending");
    }

    @Test
    public void testOnCreateShardWithNoInitialSchemaContext() {
        LOG.info("testOnCreateShardWithNoInitialSchemaContext starting");
        final TestKit kit = new TestKit(getSystem());
        ActorRef shardManager = actorFactory
                .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                    .withDispatcher(Dispatchers.DefaultDispatcherId()));

        Shard.Builder shardBuilder = Shard.builder();

        ModuleShardConfiguration config = new ModuleShardConfiguration(XMLNamespace.of("foo-ns"), "foo-module",
            "foo", null, members("member-1"));
        shardManager.tell(new CreateShard(config, shardBuilder, null), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), Success.class);

        EffectiveModelContext schemaContext = TEST_SCHEMA_CONTEXT;
        shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

        shardManager.tell(new FindLocalShard("foo", true), kit.getRef());

        kit.expectMsgClass(Duration.ofSeconds(5), LocalShardFound.class);

        assertSame("schemaContext", schemaContext, shardBuilder.getSchemaContext());
        assertNotNull("schemaContext is null", shardBuilder.getDatastoreContext());

        LOG.info("testOnCreateShardWithNoInitialSchemaContext ending");
    }

    @Test
    public void testGetSnapshot() {
        LOG.info("testGetSnapshot starting");
        TestKit kit = new TestKit(getSystem());

        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                .put("shard1", Arrays.asList("member-1")).put("shard2", Arrays.asList("member-1"))
                .put("astronauts", Collections.<String>emptyList()).build());

        TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(newShardMgrProps(mockConfig)
                .withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());
        Failure failure = kit.expectMsgClass(Failure.class);
        assertEquals("Failure cause type", IllegalStateException.class, failure.cause().getClass());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), ActorRef.noSender());

        waitForShardInitialized(shardManager, "shard1", kit);
        waitForShardInitialized(shardManager, "shard2", kit);

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());

        DatastoreSnapshot datastoreSnapshot = expectMsgClassOrFailure(DatastoreSnapshot.class, kit, "GetSnapshot");

        assertEquals("getType", shardMrgIDSuffix, datastoreSnapshot.getType());
        assertNull("Expected null ShardManagerSnapshot", datastoreSnapshot.getShardManagerSnapshot());

        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2"), Sets.newHashSet(
            datastoreSnapshot.getShardSnapshots().stream().map(ShardSnapshot::getName).collect(Collectors.toSet())));

        // Add a new replica

        TestKit mockShardLeaderKit = new TestKit(getSystem());

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
                Lists.transform(datastoreSnapshot.getShardSnapshots(), ShardSnapshot::getName)));

        ShardManagerSnapshot snapshot = datastoreSnapshot.getShardManagerSnapshot();
        assertNotNull("Expected ShardManagerSnapshot", snapshot);
        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2", "astronauts"),
                Sets.newHashSet(snapshot.getShardList()));

        LOG.info("testGetSnapshot ending");
    }

    @Test
    public void testRestoreFromSnapshot() {
        LOG.info("testRestoreFromSnapshot starting");

        datastoreContextBuilder.shardInitializationTimeout(3, TimeUnit.SECONDS);

        TestKit kit = new TestKit(getSystem());

        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                .put("shard1", Collections.<String>emptyList()).put("shard2", Collections.<String>emptyList())
                .put("astronauts", Collections.<String>emptyList()).build());

        ShardManagerSnapshot snapshot =
                new ShardManagerSnapshot(Arrays.asList("shard1", "shard2", "astronauts"));
        DatastoreSnapshot restoreFromSnapshot = new DatastoreSnapshot(shardMrgIDSuffix, snapshot,
                Collections.<ShardSnapshot>emptyList());
        TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(newTestShardMgrBuilder(mockConfig)
                .restoreFromSnapshot(restoreFromSnapshot).props().withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.underlyingActor().waitForRecoveryComplete();

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), ActorRef.noSender());

        waitForShardInitialized(shardManager, "shard1", kit);
        waitForShardInitialized(shardManager, "shard2", kit);
        waitForShardInitialized(shardManager, "astronauts", kit);

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());

        DatastoreSnapshot datastoreSnapshot = expectMsgClassOrFailure(DatastoreSnapshot.class, kit, "GetSnapshot");

        assertEquals("getType", shardMrgIDSuffix, datastoreSnapshot.getType());

        assertNotNull("Expected ShardManagerSnapshot", datastoreSnapshot.getShardManagerSnapshot());
        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2", "astronauts"),
                Sets.newHashSet(datastoreSnapshot.getShardManagerSnapshot().getShardList()));

        LOG.info("testRestoreFromSnapshot ending");
    }

    @Test
    public void testAddShardReplicaForNonExistentShardConfig() {
        final TestKit kit = new TestKit(getSystem());
        ActorRef shardManager = actorFactory
                .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                    .withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.tell(new AddShardReplica("model-inventory"), kit.getRef());
        Status.Failure resp = kit.expectMsgClass(Duration.ofSeconds(2), Status.Failure.class);

        assertTrue("Failure obtained", resp.cause() instanceof IllegalArgumentException);
    }

    @Test
    public void testAddShardReplica() {
        LOG.info("testAddShardReplica starting");
        MockConfiguration mockConfig = new MockConfiguration(
                ImmutableMap.<String, List<String>>builder().put("default", Arrays.asList("member-1", "member-2"))
                        .put("astronauts", Arrays.asList("member-2")).build());

        final String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();
        datastoreContextBuilder.shardManagerPersistenceId(shardManagerID);

        // Create an ActorSystem ShardManager actor for member-1.
        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));
        ActorRef mockDefaultShardActor = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");
        final TestActorRef<TestShardManager> newReplicaShardManager = TestActorRef.create(system1,
                newTestShardMgrBuilder(mockConfig).shardActor(mockDefaultShardActor)
                        .cluster(new ClusterWrapperImpl(system1)).props()
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.
        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));

        String memberId2 = "member-2-shard-astronauts-" + shardMrgIDSuffix;
        String name = ShardIdentifier.create("astronauts", MEMBER_2, "config").toString();
        final TestActorRef<MockRespondActor> mockShardLeaderActor = TestActorRef.create(system2,
                Props.create(MockRespondActor.class, AddServer.class,
                        new AddServerReply(ServerChangeStatus.OK, memberId2))
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                name);
        final TestActorRef<TestShardManager> leaderShardManager = TestActorRef.create(system2,
                newTestShardMgrBuilder(mockConfig).shardActor(mockShardLeaderActor)
                        .cluster(new ClusterWrapperImpl(system2)).props()
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        final TestKit kit = new TestKit(getSystem());
        newReplicaShardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        leaderShardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        leaderShardManager.tell(new ActorInitialized(), mockShardLeaderActor);

        short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
        leaderShardManager.tell(
            new ShardLeaderStateChanged(memberId2, memberId2, mock(DataTree.class), leaderVersion),
            mockShardLeaderActor);
        leaderShardManager.tell(
            new RoleChangeNotification(memberId2, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardLeaderActor);

        newReplicaShardManager.underlyingActor().waitForMemberUp();
        leaderShardManager.underlyingActor().waitForMemberUp();

        // Have a dummy snapshot to be overwritten by the new data
        // persisted.
        String[] restoredShards = { "default", "people" };
        ShardManagerSnapshot snapshot =
                new ShardManagerSnapshot(Arrays.asList(restoredShards));
        InMemorySnapshotStore.addSnapshot(shardManagerID, snapshot);
        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.MILLISECONDS);

        InMemorySnapshotStore.addSnapshotSavedLatch(shardManagerID);
        InMemorySnapshotStore.addSnapshotDeletedLatch(shardManagerID);

        // construct a mock response message
        newReplicaShardManager.tell(new AddShardReplica("astronauts"), kit.getRef());
        AddServer addServerMsg = MessageCollectorActor.expectFirstMatching(mockShardLeaderActor,
            AddServer.class);
        String addServerId = "member-1-shard-astronauts-" + shardMrgIDSuffix;
        assertEquals("AddServer serverId", addServerId, addServerMsg.getNewServerId());
        kit.expectMsgClass(Duration.ofSeconds(5), Status.Success.class);

        InMemorySnapshotStore.waitForSavedSnapshot(shardManagerID, ShardManagerSnapshot.class);
        InMemorySnapshotStore.waitForDeletedSnapshot(shardManagerID);
        List<ShardManagerSnapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(shardManagerID,
            ShardManagerSnapshot.class);
        assertEquals("Number of snapshots persisted", 1, persistedSnapshots.size());
        ShardManagerSnapshot shardManagerSnapshot = persistedSnapshots.get(0);
        assertEquals("Persisted local shards", Sets.newHashSet("default", "astronauts"),
            Sets.newHashSet(shardManagerSnapshot.getShardList()));
        LOG.info("testAddShardReplica ending");
    }

    @Test
    public void testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader() {
        LOG.info("testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader starting");
        final TestKit kit = new TestKit(getSystem());
        TestActorRef<TestShardManager> shardManager = actorFactory
                .createTestActor(newPropsShardMgrWithMockShardActor(), shardMgrID);

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        String leaderId = "leader-member-shard-default-" + shardMrgIDSuffix;
        AddServerReply addServerReply = new AddServerReply(ServerChangeStatus.ALREADY_EXISTS, null);
        ActorRef leaderShardActor = shardManager.underlyingActor().getContext()
                .actorOf(Props.create(MockRespondActor.class, AddServer.class, addServerReply), leaderId);

        MockClusterWrapper.sendMemberUp(shardManager, "leader-member", leaderShardActor.path().toString());

        String newReplicaId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.tell(
            new RoleChangeNotification(newReplicaId, RaftState.Candidate.name(), RaftState.Follower.name()),
            mockShardActor);
        shardManager.tell(
            new ShardLeaderStateChanged(newReplicaId, leaderId, DataStoreVersions.CURRENT_VERSION),
            mockShardActor);

        shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), kit.getRef());

        MessageCollectorActor.expectFirstMatching(leaderShardActor, AddServer.class);

        Failure resp = kit.expectMsgClass(Duration.ofSeconds(5), Failure.class);
        assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

        shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), LocalShardFound.class);

        // Send message again to verify previous in progress state is
        // cleared

        shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), kit.getRef());
        resp = kit.expectMsgClass(Duration.ofSeconds(5), Failure.class);
        assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

        // Send message again with an AddServer timeout to verify the
        // pre-existing shard actor isn't terminated.

        shardManager.tell(
            newDatastoreContextFactory(
                datastoreContextBuilder.shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build()), kit.getRef());
        leaderShardActor.tell(MockRespondActor.CLEAR_RESPONSE, ActorRef.noSender());
        shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), Failure.class);

        shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), LocalShardFound.class);

        LOG.info("testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader ending");
    }

    @Test
    public void testAddShardReplicaWithPreExistingLocalReplicaLeader() {
        LOG.info("testAddShardReplicaWithPreExistingLocalReplicaLeader starting");
        final TestKit kit = new TestKit(getSystem());
        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
        ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);
        shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), kit.getRef());
        shardManager.tell(
            new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardActor);

        shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), kit.getRef());
        Failure resp = kit.expectMsgClass(Duration.ofSeconds(5), Failure.class);
        assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

        shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), LocalShardFound.class);

        LOG.info("testAddShardReplicaWithPreExistingLocalReplicaLeader ending");
    }

    @Test
    public void testAddShardReplicaWithAddServerReplyFailure() {
        LOG.info("testAddShardReplicaWithAddServerReplyFailure starting");
        final TestKit kit = new TestKit(getSystem());
        final TestKit mockShardLeaderKit = new TestKit(getSystem());

        MockConfiguration mockConfig = new MockConfiguration(
            ImmutableMap.of("astronauts", Arrays.asList("member-2")));

        ActorRef mockNewReplicaShardActor = newMockShardActor(getSystem(), "astronauts", "member-1");
        final TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(
            newTestShardMgrBuilder(mockConfig).shardActor(mockNewReplicaShardActor).props()
            .withDispatcher(Dispatchers.DefaultDispatcherId()), shardMgrID);
        shardManager.underlyingActor().setMessageInterceptor(newFindPrimaryInterceptor(mockShardLeaderKit.getRef()));

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        TestKit terminateWatcher = new TestKit(getSystem());
        terminateWatcher.watch(mockNewReplicaShardActor);

        shardManager.tell(new AddShardReplica("astronauts"), kit.getRef());

        AddServer addServerMsg = mockShardLeaderKit.expectMsgClass(AddServer.class);
        assertEquals("AddServer serverId", "member-1-shard-astronauts-" + shardMrgIDSuffix,
            addServerMsg.getNewServerId());
        mockShardLeaderKit.reply(new AddServerReply(ServerChangeStatus.TIMEOUT, null));

        Failure failure = kit.expectMsgClass(Duration.ofSeconds(5), Failure.class);
        assertEquals("Failure cause", TimeoutException.class, failure.cause().getClass());

        shardManager.tell(new FindLocalShard("astronauts", false), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), LocalShardNotFound.class);

        terminateWatcher.expectTerminated(mockNewReplicaShardActor);

        shardManager.tell(new AddShardReplica("astronauts"), kit.getRef());
        mockShardLeaderKit.expectMsgClass(AddServer.class);
        mockShardLeaderKit.reply(new AddServerReply(ServerChangeStatus.NO_LEADER, null));
        failure = kit.expectMsgClass(Duration.ofSeconds(5), Failure.class);
        assertEquals("Failure cause", NoShardLeaderException.class, failure.cause().getClass());

        LOG.info("testAddShardReplicaWithAddServerReplyFailure ending");
    }

    @Test
    public void testAddShardReplicaWithAlreadyInProgress() {
        testServerChangeWhenAlreadyInProgress("astronauts", new AddShardReplica("astronauts"),
                AddServer.class, new AddShardReplica("astronauts"));
    }

    @Test
    public void testAddShardReplicaWithFindPrimaryTimeout() {
        LOG.info("testAddShardReplicaWithFindPrimaryTimeout starting");
        datastoreContextBuilder.shardInitializationTimeout(100, TimeUnit.MILLISECONDS);
        final TestKit kit = new TestKit(getSystem());
        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.of("astronauts", Arrays.asList("member-2")));

        final ActorRef newReplicaShardManager = actorFactory
                .createActor(newTestShardMgrBuilder(mockConfig).shardActor(mockShardActor).props()
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), shardMgrID);

        newReplicaShardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        MockClusterWrapper.sendMemberUp(newReplicaShardManager, "member-2",
            AddressFromURIString.parse("akka://non-existent@127.0.0.1:5").toString());

        newReplicaShardManager.tell(new AddShardReplica("astronauts"), kit.getRef());
        Status.Failure resp = kit.expectMsgClass(Duration.ofSeconds(5), Status.Failure.class);
        assertTrue("Failure obtained", resp.cause() instanceof RuntimeException);

        LOG.info("testAddShardReplicaWithFindPrimaryTimeout ending");
    }

    @Test
    public void testRemoveShardReplicaForNonExistentShard() {
        final TestKit kit = new TestKit(getSystem());
        ActorRef shardManager = actorFactory
                .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                    .withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.tell(new RemoveShardReplica("model-inventory", MEMBER_1), kit.getRef());
        Status.Failure resp = kit.expectMsgClass(Duration.ofSeconds(10), Status.Failure.class);
        assertTrue("Failure obtained", resp.cause() instanceof PrimaryNotFoundException);
    }

    @Test
    /**
     * Primary is Local.
     */
    public void testRemoveShardReplicaLocal() {
        final TestKit kit = new TestKit(getSystem());
        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

        final ActorRef respondActor = actorFactory.createActor(Props.create(MockRespondActor.class,
            RemoveServer.class, new RemoveServerReply(ServerChangeStatus.OK, null)), memberId);

        ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor(respondActor));

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), respondActor);
        shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), kit.getRef());
        shardManager.tell(
            new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
            respondActor);

        shardManager.tell(new RemoveShardReplica(Shard.DEFAULT_NAME, MEMBER_1), kit.getRef());
        final RemoveServer removeServer = MessageCollectorActor.expectFirstMatching(respondActor,
            RemoveServer.class);
        assertEquals(ShardIdentifier.create("default", MEMBER_1, shardMrgIDSuffix).toString(),
            removeServer.getServerId());
        kit.expectMsgClass(Duration.ofSeconds(5), Success.class);
    }

    @Test
    public void testRemoveShardReplicaRemote() {
        MockConfiguration mockConfig = new MockConfiguration(
                ImmutableMap.<String, List<String>>builder().put("default", Arrays.asList("member-1", "member-2"))
                        .put("astronauts", Arrays.asList("member-1")).build());

        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.
        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));
        ActorRef mockDefaultShardActor = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final TestActorRef<TestShardManager> newReplicaShardManager = TestActorRef.create(system1,
                newTestShardMgrBuilder().configuration(mockConfig).shardActor(mockDefaultShardActor).cluster(
                        new ClusterWrapperImpl(system1)).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.
        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558"));

        String name = ShardIdentifier.create("default", MEMBER_2, shardMrgIDSuffix).toString();
        String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
        final TestActorRef<MockRespondActor> mockShardLeaderActor =
                TestActorRef.create(system2, Props.create(MockRespondActor.class, RemoveServer.class,
                        new RemoveServerReply(ServerChangeStatus.OK, memberId2)), name);

        LOG.error("Mock Shard Leader Actor : {}", mockShardLeaderActor);

        final TestActorRef<TestShardManager> leaderShardManager = TestActorRef.create(system2,
                newTestShardMgrBuilder().configuration(mockConfig).shardActor(mockShardLeaderActor).cluster(
                        new ClusterWrapperImpl(system2)).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Because mockShardLeaderActor is created at the top level of the actor system it has an address like so,
        //    akka://cluster-test@127.0.0.1:2559/user/member-2-shard-default-config1
        // However when a shard manager has a local shard which is a follower and a leader that is remote it will
        // try to compute an address for the remote shard leader using the ShardPeerAddressResolver. This address will
        // look like so,
        //    akka://cluster-test@127.0.0.1:2559/user/shardmanager-config1/member-2-shard-default-config1
        // In this specific case if we did a FindPrimary for shard default from member-1 we would come up
        // with the address of an actor which does not exist, therefore any message sent to that actor would go to
        // dead letters.
        // To work around this problem we create a ForwardingActor with the right address and pass to it the
        // mockShardLeaderActor. The ForwardingActor simply forwards all messages to the mockShardLeaderActor and every
        // thing works as expected
        final ActorRef actorRef = leaderShardManager.underlyingActor().context()
                .actorOf(Props.create(ForwardingActor.class, mockShardLeaderActor),
                        "member-2-shard-default-" + shardMrgIDSuffix);

        LOG.error("Forwarding actor : {}", actorRef);

        final TestKit kit = new TestKit(getSystem());
        newReplicaShardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        leaderShardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        leaderShardManager.tell(new ActorInitialized(), mockShardLeaderActor);
        newReplicaShardManager.tell(new ActorInitialized(), mockShardLeaderActor);

        short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
        leaderShardManager.tell(
            new ShardLeaderStateChanged(memberId2, memberId2, mock(DataTree.class), leaderVersion),
            mockShardLeaderActor);
        leaderShardManager.tell(
            new RoleChangeNotification(memberId2, RaftState.Candidate.name(), RaftState.Leader.name()),
            mockShardLeaderActor);

        String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
        newReplicaShardManager.tell(
            new ShardLeaderStateChanged(memberId1, memberId2, mock(DataTree.class), leaderVersion),
            mockShardActor);
        newReplicaShardManager.tell(
            new RoleChangeNotification(memberId1, RaftState.Candidate.name(), RaftState.Follower.name()),
            mockShardActor);

        newReplicaShardManager.underlyingActor().waitForMemberUp();
        leaderShardManager.underlyingActor().waitForMemberUp();

        // construct a mock response message
        newReplicaShardManager.tell(new RemoveShardReplica("default", MEMBER_1), kit.getRef());
        RemoveServer removeServer = MessageCollectorActor.expectFirstMatching(mockShardLeaderActor,
            RemoveServer.class);
        String removeServerId = ShardIdentifier.create("default", MEMBER_1, shardMrgIDSuffix).toString();
        assertEquals("RemoveServer serverId", removeServerId, removeServer.getServerId());
        kit.expectMsgClass(Duration.ofSeconds(5), Status.Success.class);
    }

    @Test
    public void testRemoveShardReplicaWhenAnotherRemoveShardReplicaAlreadyInProgress() {
        testServerChangeWhenAlreadyInProgress("astronauts", new RemoveShardReplica("astronauts", MEMBER_2),
                RemoveServer.class, new RemoveShardReplica("astronauts", MEMBER_3));
    }

    @Test
    public void testRemoveShardReplicaWhenAddShardReplicaAlreadyInProgress() {
        testServerChangeWhenAlreadyInProgress("astronauts", new AddShardReplica("astronauts"),
                AddServer.class, new RemoveShardReplica("astronauts", MEMBER_2));
    }


    public void testServerChangeWhenAlreadyInProgress(final String shardName, final Object firstServerChange,
                                                      final Class<?> firstForwardedServerChangeClass,
                                                      final Object secondServerChange) {
        final TestKit kit = new TestKit(getSystem());
        final TestKit mockShardLeaderKit = new TestKit(getSystem());
        final TestKit secondRequestKit = new TestKit(getSystem());

        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
            .put(shardName, Arrays.asList("member-2")).build());

        final TestActorRef<TestShardManager> shardManager = TestActorRef.create(getSystem(),
            newTestShardMgrBuilder().configuration(mockConfig).shardActor(mockShardActor)
            .cluster(new MockClusterWrapper()).props()
            .withDispatcher(Dispatchers.DefaultDispatcherId()),
            shardMgrID);

        shardManager.underlyingActor().setMessageInterceptor(newFindPrimaryInterceptor(mockShardLeaderKit.getRef()));

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());

        shardManager.tell(firstServerChange, kit.getRef());

        mockShardLeaderKit.expectMsgClass(firstForwardedServerChangeClass);

        shardManager.tell(secondServerChange, secondRequestKit.getRef());

        secondRequestKit.expectMsgClass(Duration.ofSeconds(5), Failure.class);
    }

    @Test
    public void testServerRemovedShardActorNotRunning() {
        LOG.info("testServerRemovedShardActorNotRunning starting");
        final TestKit kit = new TestKit(getSystem());
        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
            .put("default", Arrays.asList("member-1", "member-2"))
            .put("astronauts", Arrays.asList("member-2"))
            .put("people", Arrays.asList("member-1", "member-2")).build());

        TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(
            newShardMgrProps(mockConfig).withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.underlyingActor().waitForRecoveryComplete();
        shardManager.tell(new FindLocalShard("people", false), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), NotInitializedException.class);

        shardManager.tell(new FindLocalShard("default", false), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), NotInitializedException.class);

        // Removed the default shard replica from member-1
        ShardIdentifier.Builder builder = new ShardIdentifier.Builder();
        ShardIdentifier shardId = builder.shardName("default").memberName(MEMBER_1).type(shardMrgIDSuffix)
                .build();
        shardManager.tell(new ServerRemoved(shardId.toString()), kit.getRef());

        shardManager.underlyingActor().verifySnapshotPersisted(Sets.newHashSet("people"));

        LOG.info("testServerRemovedShardActorNotRunning ending");
    }

    @Test
    public void testServerRemovedShardActorRunning() {
        LOG.info("testServerRemovedShardActorRunning starting");
        final TestKit kit = new TestKit(getSystem());
        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
            .put("default", Arrays.asList("member-1", "member-2"))
            .put("astronauts", Arrays.asList("member-2"))
            .put("people", Arrays.asList("member-1", "member-2")).build());

        String shardId = ShardIdentifier.create("default", MEMBER_1, shardMrgIDSuffix).toString();
        ActorRef shard = actorFactory.createActor(MessageCollectorActor.props(), shardId);

        TestActorRef<TestShardManager> shardManager = actorFactory
                .createTestActor(newTestShardMgrBuilder(mockConfig).addShardActor("default", shard).props()
                    .withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.underlyingActor().waitForRecoveryComplete();

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), shard);

        waitForShardInitialized(shardManager, "people", kit);
        waitForShardInitialized(shardManager, "default", kit);

        // Removed the default shard replica from member-1
        shardManager.tell(new ServerRemoved(shardId), kit.getRef());

        shardManager.underlyingActor().verifySnapshotPersisted(Sets.newHashSet("people"));

        MessageCollectorActor.expectFirstMatching(shard, Shutdown.class);

        LOG.info("testServerRemovedShardActorRunning ending");
    }

    @Test
    public void testShardPersistenceWithRestoredData() {
        LOG.info("testShardPersistenceWithRestoredData starting");
        final TestKit kit = new TestKit(getSystem());
        MockConfiguration mockConfig =
                new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                    .put("default", Arrays.asList("member-1", "member-2"))
                    .put("astronauts", Arrays.asList("member-2"))
                    .put("people", Arrays.asList("member-1", "member-2")).build());
        String[] restoredShards = {"default", "astronauts"};
        ShardManagerSnapshot snapshot =
                new ShardManagerSnapshot(Arrays.asList(restoredShards));
        InMemorySnapshotStore.addSnapshot("shard-manager-" + shardMrgIDSuffix, snapshot);

        // create shardManager to come up with restored data
        TestActorRef<TestShardManager> newRestoredShardManager = actorFactory.createTestActor(
            newShardMgrProps(mockConfig).withDispatcher(Dispatchers.DefaultDispatcherId()));

        newRestoredShardManager.underlyingActor().waitForRecoveryComplete();

        newRestoredShardManager.tell(new FindLocalShard("people", false), kit.getRef());
        LocalShardNotFound notFound = kit.expectMsgClass(Duration.ofSeconds(5), LocalShardNotFound.class);
        assertEquals("for uninitialized shard", "people", notFound.getShardName());

        // Verify a local shard is created for the restored shards,
        // although we expect a NotInitializedException for the shards
        // as the actor initialization
        // message is not sent for them
        newRestoredShardManager.tell(new FindLocalShard("default", false), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), NotInitializedException.class);

        newRestoredShardManager.tell(new FindLocalShard("astronauts", false), kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), NotInitializedException.class);

        LOG.info("testShardPersistenceWithRestoredData ending");
    }

    @Test
    public void testShutDown() throws Exception {
        LOG.info("testShutDown starting");
        final TestKit kit = new TestKit(getSystem());
        MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
            .put("shard1", Arrays.asList("member-1")).put("shard2", Arrays.asList("member-1")).build());

        String shardId1 = ShardIdentifier.create("shard1", MEMBER_1, shardMrgIDSuffix).toString();
        ActorRef shard1 = actorFactory.createActor(MessageCollectorActor.props(), shardId1);

        String shardId2 = ShardIdentifier.create("shard2", MEMBER_1, shardMrgIDSuffix).toString();
        ActorRef shard2 = actorFactory.createActor(MessageCollectorActor.props(), shardId2);

        ActorRef shardManager = actorFactory.createActor(newTestShardMgrBuilder(mockConfig)
            .addShardActor("shard1", shard1).addShardActor("shard2", shard2).props());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), shard1);
        shardManager.tell(new ActorInitialized(), shard2);

        FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
        Future<Boolean> stopFuture = Patterns.gracefulStop(shardManager, duration, Shutdown.INSTANCE);

        MessageCollectorActor.expectFirstMatching(shard1, Shutdown.class);
        MessageCollectorActor.expectFirstMatching(shard2, Shutdown.class);

        try {
            Await.ready(stopFuture, FiniteDuration.create(500, TimeUnit.MILLISECONDS));
            fail("ShardManager actor stopped without waiting for the Shards to be stopped");
        } catch (TimeoutException e) {
            // expected
        }

        actorFactory.killActor(shard1, kit);
        actorFactory.killActor(shard2, kit);

        Boolean stopped = Await.result(stopFuture, duration);
        assertEquals("Stopped", Boolean.TRUE, stopped);

        LOG.info("testShutDown ending");
    }

    @Test
    public void testChangeServersVotingStatus() {
        final TestKit kit = new TestKit(getSystem());
        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

        ActorRef respondActor = actorFactory
                .createActor(Props.create(MockRespondActor.class, ChangeServersVotingStatus.class,
                    new ServerChangeReply(ServerChangeStatus.OK, null)), memberId);

        ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor(respondActor));

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), respondActor);
        shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mock(DataTree.class),
            DataStoreVersions.CURRENT_VERSION), kit.getRef());
        shardManager.tell(
            new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
            respondActor);

        shardManager.tell(
            new ChangeShardMembersVotingStatus("default", ImmutableMap.of("member-2", Boolean.TRUE)), kit.getRef());

        ChangeServersVotingStatus actualChangeStatusMsg = MessageCollectorActor
                .expectFirstMatching(respondActor, ChangeServersVotingStatus.class);
        assertEquals("ChangeServersVotingStatus map", actualChangeStatusMsg.getServerVotingStatusMap(),
            ImmutableMap.of(ShardIdentifier
                .create("default", MemberName.forName("member-2"), shardMrgIDSuffix).toString(),
                Boolean.TRUE));

        kit.expectMsgClass(Duration.ofSeconds(5), Success.class);
    }

    @Test
    public void testChangeServersVotingStatusWithNoLeader() {
        final TestKit kit = new TestKit(getSystem());
        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

        ActorRef respondActor = actorFactory
                .createActor(Props.create(MockRespondActor.class, ChangeServersVotingStatus.class,
                    new ServerChangeReply(ServerChangeStatus.NO_LEADER, null)), memberId);

        ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor(respondActor));

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), respondActor);
        shardManager.tell(new RoleChangeNotification(memberId, null, RaftState.Follower.name()), respondActor);

        shardManager.tell(
            new ChangeShardMembersVotingStatus("default", ImmutableMap.of("member-2", Boolean.TRUE)), kit.getRef());

        MessageCollectorActor.expectFirstMatching(respondActor, ChangeServersVotingStatus.class);

        Status.Failure resp = kit.expectMsgClass(Duration.ofSeconds(5), Status.Failure.class);
        assertTrue("Failure resposnse", resp.cause() instanceof NoShardLeaderException);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterForShardLeaderChanges() {
        LOG.info("testRegisterForShardLeaderChanges starting");

        final String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
        final String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
        final TestKit kit = new TestKit(getSystem());
        final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

        shardManager.tell(new UpdateSchemaContext(TEST_SCHEMA_CONTEXT), kit.getRef());
        shardManager.tell(new ActorInitialized(), mockShardActor);

        final Consumer<String> mockCallback = mock(Consumer.class);
        shardManager.tell(new RegisterForShardAvailabilityChanges(mockCallback), kit.getRef());

        final Success reply = kit.expectMsgClass(Duration.ofSeconds(5), Success.class);
        final Registration reg = (Registration) reply.status();

        final DataTree mockDataTree = mock(DataTree.class);
        shardManager.tell(new ShardLeaderStateChanged(memberId1, memberId1, mockDataTree,
            DataStoreVersions.CURRENT_VERSION), mockShardActor);

        verify(mockCallback, timeout(5000)).accept("default");

        reset(mockCallback);
        shardManager.tell(new ShardLeaderStateChanged(memberId1, memberId1, mockDataTree,
                DataStoreVersions.CURRENT_VERSION), mockShardActor);

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyNoMoreInteractions(mockCallback);

        shardManager.tell(new ShardLeaderStateChanged(memberId1, null, mockDataTree,
                DataStoreVersions.CURRENT_VERSION), mockShardActor);

        verify(mockCallback, timeout(5000)).accept("default");

        reset(mockCallback);
        shardManager.tell(new ShardLeaderStateChanged(memberId1, memberId2, mockDataTree,
                DataStoreVersions.CURRENT_VERSION), mockShardActor);

        verify(mockCallback, timeout(5000)).accept("default");

        reset(mockCallback);
        reg.close();

        shardManager.tell(new ShardLeaderStateChanged(memberId1, memberId1, mockDataTree,
                DataStoreVersions.CURRENT_VERSION), mockShardActor);

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyNoMoreInteractions(mockCallback);

        LOG.info("testRegisterForShardLeaderChanges ending");
    }

    public static class TestShardManager extends ShardManager {
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

        TestShardManager(final Builder builder) {
            super(builder);
            shardActor = builder.shardActor;
            shardActors = builder.shardActors;
        }

        @Override
        protected void handleRecover(final Object message) throws Exception {
            try {
                super.handleRecover(message);
            } finally {
                if (message instanceof RecoveryCompleted) {
                    recoveryComplete.countDown();
                }
            }
        }

        private void countDownIfOther(final Member member, final CountDownLatch latch) {
            if (!getCluster().getCurrentMemberName().equals(memberToName(member))) {
                latch.countDown();
            }
        }

        @Override
        public void handleCommand(final Object message) throws Exception {
            try {
                if (messageInterceptor != null && messageInterceptor.canIntercept(message)) {
                    getSender().tell(messageInterceptor.apply(message), getSelf());
                } else {
                    super.handleCommand(message);
                }
            } finally {
                if (message instanceof FindPrimary) {
                    findPrimaryMessageReceived.countDown();
                } else if (message instanceof ClusterEvent.MemberUp) {
                    countDownIfOther(((ClusterEvent.MemberUp) message).member(), memberUpReceived);
                } else if (message instanceof ClusterEvent.MemberRemoved) {
                    countDownIfOther(((ClusterEvent.MemberRemoved) message).member(), memberRemovedReceived);
                } else if (message instanceof ClusterEvent.UnreachableMember) {
                    countDownIfOther(((ClusterEvent.UnreachableMember) message).member(), memberUnreachableReceived);
                } else if (message instanceof ClusterEvent.ReachableMember) {
                    countDownIfOther(((ClusterEvent.ReachableMember) message).member(), memberReachableReceived);
                }
            }
        }

        void setMessageInterceptor(final MessageInterceptor messageInterceptor) {
            this.messageInterceptor = messageInterceptor;
        }

        void waitForRecoveryComplete() {
            assertTrue("Recovery complete",
                    Uninterruptibles.awaitUninterruptibly(recoveryComplete, 5, TimeUnit.SECONDS));
        }

        public void waitForMemberUp() {
            assertTrue("MemberUp received",
                    Uninterruptibles.awaitUninterruptibly(memberUpReceived, 5, TimeUnit.SECONDS));
            memberUpReceived = new CountDownLatch(1);
        }

        void waitForMemberRemoved() {
            assertTrue("MemberRemoved received",
                    Uninterruptibles.awaitUninterruptibly(memberRemovedReceived, 5, TimeUnit.SECONDS));
            memberRemovedReceived = new CountDownLatch(1);
        }

        void waitForUnreachableMember() {
            assertTrue("UnreachableMember received",
                Uninterruptibles.awaitUninterruptibly(memberUnreachableReceived, 5, TimeUnit.SECONDS));
            memberUnreachableReceived = new CountDownLatch(1);
        }

        void waitForReachableMember() {
            assertTrue("ReachableMember received",
                Uninterruptibles.awaitUninterruptibly(memberReachableReceived, 5, TimeUnit.SECONDS));
            memberReachableReceived = new CountDownLatch(1);
        }

        void verifyFindPrimary() {
            assertTrue("FindPrimary received",
                    Uninterruptibles.awaitUninterruptibly(findPrimaryMessageReceived, 5, TimeUnit.SECONDS));
            findPrimaryMessageReceived = new CountDownLatch(1);
        }

        public static Builder builder(final DatastoreContext.Builder datastoreContextBuilder) {
            return new Builder(datastoreContextBuilder);
        }

        public static class Builder extends AbstractGenericCreator<Builder, TestShardManager> {
            private ActorRef shardActor;
            private final Map<String, ActorRef> shardActors = new HashMap<>();

            Builder(final DatastoreContext.Builder datastoreContextBuilder) {
                super(TestShardManager.class);
                datastoreContextFactory(newDatastoreContextFactory(datastoreContextBuilder.build()));
            }

            Builder shardActor(final ActorRef newShardActor) {
                shardActor = newShardActor;
                return this;
            }

            Builder addShardActor(final String shardName, final ActorRef actorRef) {
                shardActors.put(shardName, actorRef);
                return this;
            }
        }

        @Override
        public void saveSnapshot(final Object obj) {
            snapshot = (ShardManagerSnapshot) obj;
            snapshotPersist.countDown();
            super.saveSnapshot(obj);
        }

        void verifySnapshotPersisted(final Set<String> shardList) {
            assertTrue("saveSnapshot invoked",
                    Uninterruptibles.awaitUninterruptibly(snapshotPersist, 5, TimeUnit.SECONDS));
            assertEquals("Shard Persisted", shardList, Sets.newHashSet(snapshot.getShardList()));
        }

        @Override
        protected ActorRef newShardActor(final ShardInformation info) {
            if (shardActors.get(info.getShardName()) != null) {
                return shardActors.get(info.getShardName());
            }

            if (shardActor != null) {
                return shardActor;
            }

            return super.newShardActor(info);
        }
    }

    private abstract static class AbstractGenericCreator<T extends AbstractGenericCreator<T, ?>, C extends ShardManager>
                                                     extends AbstractShardManagerCreator<T> {
        private final Class<C> shardManagerClass;

        AbstractGenericCreator(final Class<C> shardManagerClass) {
            this.shardManagerClass = shardManagerClass;
            cluster(new MockClusterWrapper()).configuration(new MockConfiguration()).readinessFuture(ready)
                    .primaryShardInfoCache(new PrimaryShardInfoFutureCache());
        }

        @Override
        public Props props() {
            verify();
            return Props.create(shardManagerClass, this);
        }
    }

    private static class GenericCreator<C extends ShardManager> extends AbstractGenericCreator<GenericCreator<C>, C> {
        GenericCreator(final Class<C> shardManagerClass) {
            super(shardManagerClass);
        }
    }

    private static class DelegatingShardManagerCreator implements Creator<ShardManager> {
        private static final long serialVersionUID = 1L;
        private final Creator<ShardManager> delegate;

        DelegatingShardManagerCreator(final Creator<ShardManager> delegate) {
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
        return new MessageInterceptor() {
            @Override
            public Object apply(final Object message) {
                return new RemotePrimaryShardFound(Serialization.serializedActorPath(primaryActor), (short) 1);
            }

            @Override
            public boolean canIntercept(final Object message) {
                return message instanceof FindPrimary;
            }
        };
    }

    private static class MockRespondActor extends MessageCollectorActor {
        static final String CLEAR_RESPONSE = "clear-response";

        private Object responseMsg;
        private final Class<?> requestClass;

        @SuppressWarnings("unused")
        MockRespondActor(final Class<?> requestClass, final Object responseMsg) {
            this.requestClass = requestClass;
            this.responseMsg = responseMsg;
        }

        @Override
        public void onReceive(final Object message) throws Exception {
            if (message.equals(CLEAR_RESPONSE)) {
                responseMsg = null;
            } else {
                super.onReceive(message);
                if (message.getClass().equals(requestClass) && responseMsg != null) {
                    getSender().tell(responseMsg, getSelf());
                }
            }
        }
    }
}
