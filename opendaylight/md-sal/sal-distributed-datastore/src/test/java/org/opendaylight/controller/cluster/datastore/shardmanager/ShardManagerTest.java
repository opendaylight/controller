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
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import java.net.URI;
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
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractShardManagerTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapperImpl;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.Shard;
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
import org.opendaylight.controller.cluster.datastore.messages.PeerDown;
import org.opendaylight.controller.cluster.datastore.messages.PeerUp;
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

public class ShardManagerTest extends AbstractShardManagerTest {
    private static final Logger LOG = LoggerFactory.getLogger(ShardManagerTest.class);
    private static final MemberName MEMBER_2 = MemberName.forName("member-2");
    private static final MemberName MEMBER_3 = MemberName.forName("member-3");

    private final String shardMgrID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

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

    private Props newShardMgrProps() {
        return newShardMgrProps(new MockConfiguration());
    }

    private static DatastoreContextFactory newDatastoreContextFactory(final DatastoreContext datastoreContext) {
        DatastoreContextFactory mockFactory = mock(DatastoreContextFactory.class);
        Mockito.doReturn(datastoreContext).when(mockFactory).getBaseDatastoreContext();
        Mockito.doReturn(datastoreContext).when(mockFactory).getShardDatastoreContext(Mockito.anyString());
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

        Mockito.doReturn(
                DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).shardElectionTimeoutFactor(6).build())
                .when(mockFactory).getShardDatastoreContext("default");

        Mockito.doReturn(
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

        final Creator<ShardManager> creator = new Creator<ShardManager>() {
            private static final long serialVersionUID = 1L;
            @Override
            public ShardManager create() {
                return new LocalShardManager(
                        new GenericCreator<>(LocalShardManager.class).datastoreContextFactory(mockFactory)
                                .primaryShardInfoCache(primaryShardInfoCache).configuration(mockConfig));
            }
        };

        TestKit kit = new TestKit(getSystem());

        final ActorRef shardManager = actorFactory.createActor(Props.create(
                new DelegatingShardManagerCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), kit.getRef());

        assertEquals("Shard actors created", true, newShardActorLatch.await(5, TimeUnit.SECONDS));
        assertEquals("getShardElectionTimeoutFactor", 6,
                shardInfoMap.get("default").getValue().getShardElectionTimeoutFactor());
        assertEquals("getShardElectionTimeoutFactor", 7,
                shardInfoMap.get("topology").getValue().getShardElectionTimeoutFactor());

        DatastoreContextFactory newMockFactory = newDatastoreContextFactory(
                datastoreContextBuilder.shardElectionTimeoutFactor(5).build());
        Mockito.doReturn(
                DatastoreContext.newBuilderFrom(datastoreContextBuilder.build()).shardElectionTimeoutFactor(66).build())
                .when(newMockFactory).getShardDatastoreContext("default");

        Mockito.doReturn(
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
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                shardManager.tell(new FindPrimary("non-existent", false), getRef());

                expectMsgClass(duration("5 seconds"), PrimaryNotFoundException.class);
            }
        };
    }

    @Test
    public void testOnReceiveFindPrimaryForLocalLeaderShard() {
        LOG.info("testOnReceiveFindPrimaryForLocalLeaderShard starting");
        new TestKit(getSystem()) {
            {
                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);

                DataTree mockDataTree = mock(DataTree.class);
                shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mockDataTree,
                        DataStoreVersions.CURRENT_VERSION), getRef());

                MessageCollectorActor.expectFirstMatching(mockShardActor, RegisterRoleChangeListener.class);
                shardManager.tell(
                        new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
                        mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

                LocalPrimaryShardFound primaryFound = expectMsgClass(duration("5 seconds"),
                        LocalPrimaryShardFound.class);
                assertTrue("Unexpected primary path " + primaryFound.getPrimaryPath(),
                        primaryFound.getPrimaryPath().contains("member-1-shard-default"));
                assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree());
            }
        };

        LOG.info("testOnReceiveFindPrimaryForLocalLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp() {
        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp starting");
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);

                String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
                String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.tell(
                        new RoleChangeNotification(memberId1, RaftState.Candidate.name(), RaftState.Follower.name()),
                        mockShardActor);
                shardManager.tell(new LeaderStateChanged(memberId1, memberId2, DataStoreVersions.CURRENT_VERSION),
                        mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

                expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);
            }
        };

        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShardBeforeMemberUp ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForNonLocalLeaderShard() {
        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShard starting");
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);

                String memberId2 = "member-2-shard-default-" + shardMrgIDSuffix;
                MockClusterWrapper.sendMemberUp(shardManager, "member-2", getRef().path().toString());

                String memberId1 = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.tell(
                        new RoleChangeNotification(memberId1, RaftState.Candidate.name(), RaftState.Follower.name()),
                        mockShardActor);
                short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
                shardManager.tell(new ShardLeaderStateChanged(memberId1, memberId2, leaderVersion), mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

                RemotePrimaryShardFound primaryFound = expectMsgClass(duration("5 seconds"),
                        RemotePrimaryShardFound.class);
                assertTrue("Unexpected primary path " + primaryFound.getPrimaryPath(),
                        primaryFound.getPrimaryPath().contains("member-2-shard-default"));
                assertEquals("getPrimaryVersion", leaderVersion, primaryFound.getPrimaryVersion());
            }
        };

        LOG.info("testOnReceiveFindPrimaryForNonLocalLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryForUninitializedShard() {
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

                expectMsgClass(duration("5 seconds"), NotInitializedException.class);
            }
        };
    }

    @Test
    public void testOnReceiveFindPrimaryForInitializedShardWithNoRole() {
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

                expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);
            }
        };
    }

    @Test
    public void testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId() {
        LOG.info("testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId starting");
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);

                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.tell(
                        new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Follower.name()),
                        mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

                expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);

                DataTree mockDataTree = mock(DataTree.class);
                shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mockDataTree,
                        DataStoreVersions.CURRENT_VERSION), mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

                LocalPrimaryShardFound primaryFound = expectMsgClass(duration("5 seconds"),
                        LocalPrimaryShardFound.class);
                assertTrue("Unexpected primary path " + primaryFound.getPrimaryPath(),
                        primaryFound.getPrimaryPath().contains("member-1-shard-default"));
                assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree());
            }
        };

        LOG.info("testOnReceiveFindPrimaryForFollowerShardWithNoInitialLeaderId starting");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForShardLeader() {
        LOG.info("testOnReceiveFindPrimaryWaitForShardLeader starting");
        datastoreContextBuilder.shardInitializationTimeout(10, TimeUnit.SECONDS);
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                // We're passing waitUntilInitialized = true to FindPrimary so
                // the response should be
                // delayed until we send ActorInitialized and
                // RoleChangeNotification.
                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

                expectNoMsg(FiniteDuration.create(150, TimeUnit.MILLISECONDS));

                shardManager.tell(new ActorInitialized(), mockShardActor);

                expectNoMsg(FiniteDuration.create(150, TimeUnit.MILLISECONDS));

                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.tell(
                        new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
                        mockShardActor);

                expectNoMsg(FiniteDuration.create(150, TimeUnit.MILLISECONDS));

                DataTree mockDataTree = mock(DataTree.class);
                shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mockDataTree,
                        DataStoreVersions.CURRENT_VERSION), mockShardActor);

                LocalPrimaryShardFound primaryFound = expectMsgClass(duration("5 seconds"),
                        LocalPrimaryShardFound.class);
                assertTrue("Unexpected primary path " + primaryFound.getPrimaryPath(),
                        primaryFound.getPrimaryPath().contains("member-1-shard-default"));
                assertSame("getLocalShardDataTree", mockDataTree, primaryFound.getLocalShardDataTree());

                expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));
            }
        };

        LOG.info("testOnReceiveFindPrimaryWaitForShardLeader ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard() {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard starting");
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

                expectMsgClass(duration("2 seconds"), NotInitializedException.class);

                shardManager.tell(new ActorInitialized(), mockShardActor);

                expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));
            }
        };

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithCandidateShard() {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithCandidateShard starting");
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);
                shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix, null,
                        RaftState.Candidate.name()), mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

                expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
            }
        };

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithCandidateShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard() {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard starting");
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);
                shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix, null,
                        RaftState.IsolatedLeader.name()), mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

                expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
            }
        };

        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithIsolatedLeaderShard ending");
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard() {
        LOG.info("testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard starting");
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);

                shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

                expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
            }
        };

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

        new TestKit(system1) {
            {
                shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                shardManager2.tell(new ActorInitialized(), mockShardActor2);

                String memberId2 = "member-2-shard-astronauts-" + shardMrgIDSuffix;
                short leaderVersion = DataStoreVersions.CURRENT_VERSION - 1;
                shardManager2.tell(
                        new ShardLeaderStateChanged(memberId2, memberId2, mock(DataTree.class), leaderVersion),
                        mockShardActor2);
                shardManager2.tell(
                        new RoleChangeNotification(memberId2, RaftState.Candidate.name(), RaftState.Leader.name()),
                        mockShardActor2);

                shardManager1.underlyingActor().waitForMemberUp();
                shardManager1.tell(new FindPrimary("astronauts", false), getRef());

                RemotePrimaryShardFound found = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
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
//                expectMsgClass(duration("5 seconds"), PrimaryNotFoundException.class);
            }
        };

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

        new TestKit(system1) {
            {
                shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
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

                shardManager1.tell(new FindPrimary("default", true), getRef());

                RemotePrimaryShardFound found = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
                String path = found.getPrimaryPath();
                assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-default-config"));

                shardManager1.tell(MockClusterWrapper.createUnreachableMember("member-2",
                        "akka://cluster-test@127.0.0.1:2558"), getRef());

                shardManager1.underlyingActor().waitForUnreachableMember();

                PeerDown peerDown = MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerDown.class);
                assertEquals("getMemberName", MEMBER_2, peerDown.getMemberName());
                MessageCollectorActor.clearMessages(mockShardActor1);

                shardManager1.tell(
                        MockClusterWrapper.createMemberRemoved("member-2", "akka://cluster-test@127.0.0.1:2558"),
                        getRef());

                MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerDown.class);

                shardManager1.tell(new FindPrimary("default", true), getRef());

                expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);

                shardManager1.tell(
                        MockClusterWrapper.createReachableMember("member-2", "akka://cluster-test@127.0.0.1:2558"),
                        getRef());

                shardManager1.underlyingActor().waitForReachableMember();

                PeerUp peerUp = MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerUp.class);
                assertEquals("getMemberName", MEMBER_2, peerUp.getMemberName());
                MessageCollectorActor.clearMessages(mockShardActor1);

                shardManager1.tell(new FindPrimary("default", true), getRef());

                RemotePrimaryShardFound found1 = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
                String path1 = found1.getPrimaryPath();
                assertTrue("Unexpected primary path " + path1, path1.contains("member-2-shard-default-config"));

                shardManager1.tell(
                        MockClusterWrapper.createMemberUp("member-2", "akka://cluster-test@127.0.0.1:2558"),
                        getRef());

                MessageCollectorActor.expectFirstMatching(mockShardActor1, PeerUp.class);

                // Test FindPrimary wait succeeds after reachable member event.

                shardManager1.tell(MockClusterWrapper.createUnreachableMember("member-2",
                        "akka://cluster-test@127.0.0.1:2558"), getRef());
                shardManager1.underlyingActor().waitForUnreachableMember();

                shardManager1.tell(new FindPrimary("default", true), getRef());

                shardManager1.tell(
                        MockClusterWrapper.createReachableMember("member-2", "akka://cluster-test@127.0.0.1:2558"),
                        getRef());

                RemotePrimaryShardFound found2 = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
                String path2 = found2.getPrimaryPath();
                assertTrue("Unexpected primary path " + path2, path2.contains("member-2-shard-default-config"));
            }
        };

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

        new TestKit(system1) {
            {
                shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
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

                shardManager1.tell(new FindPrimary("default", true), getRef());

                RemotePrimaryShardFound found = expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);
                String path = found.getPrimaryPath();
                assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-default-config"));

                primaryShardInfoCache.putSuccessful("default", new PrimaryShardInfo(
                        system1.actorSelection(mockShardActor1.path()), DataStoreVersions.CURRENT_VERSION));

                shardManager1.tell(MockClusterWrapper.createUnreachableMember("member-2",
                        "akka://cluster-test@127.0.0.1:2558"), getRef());

                shardManager1.underlyingActor().waitForUnreachableMember();

                shardManager1.tell(new FindPrimary("default", true), getRef());

                expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);

                assertNull("Expected primaryShardInfoCache entry removed",
                        primaryShardInfoCache.getIfPresent("default"));

                shardManager1.tell(new ShardLeaderStateChanged(memberId1, memberId1, mock(DataTree.class),
                        DataStoreVersions.CURRENT_VERSION), mockShardActor1);
                shardManager1.tell(
                        new RoleChangeNotification(memberId1, RaftState.Follower.name(), RaftState.Leader.name()),
                        mockShardActor1);

                shardManager1.tell(new FindPrimary("default", true), getRef());

                LocalPrimaryShardFound found1 = expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
                String path1 = found1.getPrimaryPath();
                assertTrue("Unexpected primary path " + path1, path1.contains("member-1-shard-default-config"));

            }
        };

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

        new TestKit(system256) {
            {
                shardManager256.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
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

                shardManager256.tell(new FindPrimary("default", true), getRef());

                LocalPrimaryShardFound found = expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
                String path = found.getPrimaryPath();
                assertTrue("Unexpected primary path " + path + " which must on member-256",
                            path.contains("member-256-shard-default-config"));

                PrimaryShardInfo primaryShardInfo = new PrimaryShardInfo(
                        system256.actorSelection(mockShardActor256.path()), DataStoreVersions.CURRENT_VERSION);
                primaryShardInfoCache.putSuccessful("default", primaryShardInfo);

                // Simulate member-2 become unreachable.
                shardManager256.tell(MockClusterWrapper.createUnreachableMember("member-2",
                        "akka://cluster-test@127.0.0.1:2558"), getRef());
                shardManager256.underlyingActor().waitForUnreachableMember();

                // Make sure leader shard on member-256 is still leader and still in the cache.
                shardManager256.tell(new FindPrimary("default", true), getRef());
                found = expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
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
            }
        };

        LOG.info("testShardAvailabilityChangeOnMemberWithNameContainedInLeaderIdUnreachable ending");
    }

    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() {
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                shardManager.tell(new FindLocalShard("non-existent", false), getRef());

                LocalShardNotFound notFound = expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

                assertEquals("getShardName", "non-existent", notFound.getShardName());
            }
        };
    }

    @Test
    public void testOnReceiveFindLocalShardForExistentShard() {
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);

                shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());

                LocalShardFound found = expectMsgClass(duration("5 seconds"), LocalShardFound.class);

                assertTrue("Found path contains " + found.getPath().path().toString(),
                        found.getPath().path().toString().contains("member-1-shard-default-config"));
            }
        };
    }

    @Test
    public void testOnReceiveFindLocalShardForNotInitializedShard() {
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());

                expectMsgClass(duration("5 seconds"), NotInitializedException.class);
            }
        };
    }

    @Test
    public void testOnReceiveFindLocalShardWaitForShardInitialized() throws Exception {
        LOG.info("testOnReceiveFindLocalShardWaitForShardInitialized starting");
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                // We're passing waitUntilInitialized = true to FindLocalShard
                // so the response should be
                // delayed until we send ActorInitialized.
                Future<Object> future = Patterns.ask(shardManager, new FindLocalShard(Shard.DEFAULT_NAME, true),
                        new Timeout(5, TimeUnit.SECONDS));

                shardManager.tell(new ActorInitialized(), mockShardActor);

                Object resp = Await.result(future, duration("5 seconds"));
                assertTrue("Expected: LocalShardFound, Actual: " + resp, resp instanceof LocalShardFound);
            }
        };

        LOG.info("testOnReceiveFindLocalShardWaitForShardInitialized starting");
    }

    @Test
    public void testRoleChangeNotificationAndShardLeaderStateChangedReleaseReady() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
        shardManager.onReceiveCommand(new RoleChangeNotification(
                memberId, RaftState.Candidate.name(), RaftState.Leader.name()));

        verify(ready, never()).countDown();

        shardManager.onReceiveCommand(new ShardLeaderStateChanged(memberId, memberId,
                mock(DataTree.class), DataStoreVersions.CURRENT_VERSION));

        verify(ready, times(1)).countDown();
    }

    @Test
    public void testRoleChangeNotificationToFollowerWithShardLeaderStateChangedReleaseReady() throws Exception {
        new TestKit(getSystem()) {
            {
                TestShardManager shardManager = newTestShardManager();

                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.onReceiveCommand(new RoleChangeNotification(memberId, null, RaftState.Follower.name()));

                verify(ready, never()).countDown();

                shardManager
                        .onReceiveCommand(MockClusterWrapper.createMemberUp("member-2", getRef().path().toString()));

                shardManager.onReceiveCommand(
                        new ShardLeaderStateChanged(memberId, "member-2-shard-default-" + shardMrgIDSuffix,
                                mock(DataTree.class), DataStoreVersions.CURRENT_VERSION));

                verify(ready, times(1)).countDown();
            }
        };
    }

    @Test
    public void testReadyCountDownForMemberUpAfterLeaderStateChanged() throws Exception {
        new TestKit(getSystem()) {
            {
                TestShardManager shardManager = newTestShardManager();

                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
                shardManager.onReceiveCommand(new RoleChangeNotification(memberId, null, RaftState.Follower.name()));

                verify(ready, never()).countDown();

                shardManager.onReceiveCommand(
                        new ShardLeaderStateChanged(memberId, "member-2-shard-default-" + shardMrgIDSuffix,
                                mock(DataTree.class), DataStoreVersions.CURRENT_VERSION));

                shardManager
                        .onReceiveCommand(MockClusterWrapper.createMemberUp("member-2", getRef().path().toString()));

                verify(ready, times(1)).countDown();
            }
        };
    }

    @Test
    public void testRoleChangeNotificationDoNothingForUnknownShard() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        shardManager.onReceiveCommand(new RoleChangeNotification(
                "unknown", RaftState.Candidate.name(), RaftState.Leader.name()));

        verify(ready, never()).countDown();
    }

    @Test
    public void testByDefaultSyncStatusIsFalse() {
        TestShardManager shardManager = newTestShardManager();

        assertEquals(false, shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsLeaderSyncStatusIsTrue() throws Exception {
        TestShardManager shardManager = newTestShardManager();

        shardManager.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                RaftState.Follower.name(), RaftState.Leader.name()));

        assertEquals(true, shardManager.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsCandidateSyncStatusIsFalse() throws Exception {
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
    public void testWhenShardIsFollowerSyncStatusDependsOnFollowerInitialSyncStatus() throws Exception {
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
    public void testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards() throws Exception {
        LOG.info("testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards starting");
        TestShardManager shardManager = newTestShardManager(newShardMgrProps(new MockConfiguration() {
            @Override
            public List<String> getMemberShardNames(final MemberName memberName) {
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
    public void testOnReceiveSwitchShardBehavior() {
        new TestKit(getSystem()) {
            {
                final ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);

                shardManager.tell(new SwitchShardBehavior(mockShardName, RaftState.Leader, 1000), getRef());

                SwitchBehavior switchBehavior = MessageCollectorActor.expectFirstMatching(mockShardActor,
                        SwitchBehavior.class);

                assertEquals(RaftState.Leader, switchBehavior.getNewState());
                assertEquals(1000, switchBehavior.getNewTerm());
            }
        };
    }

    private static List<MemberName> members(final String... names) {
        return Arrays.asList(names).stream().map(MemberName::forName).collect(Collectors.toList());
    }

    @Test
    public void testOnCreateShard() {
        LOG.info("testOnCreateShard starting");
        new TestKit(getSystem()) {
            {
                datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

                ActorRef shardManager = actorFactory
                        .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                                .withDispatcher(Dispatchers.DefaultDispatcherId()));

                SchemaContext schemaContext = TestModel.createTestContext();
                shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

                DatastoreContext datastoreContext = DatastoreContext.newBuilder().shardElectionTimeoutFactor(100)
                        .persistent(false).build();
                Shard.Builder shardBuilder = Shard.builder();

                ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("foo-ns"), "foo-module",
                        "foo", null, members("member-1", "member-5", "member-6"));
                shardManager.tell(new CreateShard(config, shardBuilder, datastoreContext), getRef());

                expectMsgClass(duration("5 seconds"), Success.class);

                shardManager.tell(new FindLocalShard("foo", true), getRef());

                expectMsgClass(duration("5 seconds"), LocalShardFound.class);

                assertEquals("isRecoveryApplicable", false, shardBuilder.getDatastoreContext().isPersistent());
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

                shardManager.tell(new CreateShard(config, shardBuilder, null), getRef());

                Success success = expectMsgClass(duration("5 seconds"), Success.class);
                assertNotNull("Success status is null", success.status());
            }
        };

        LOG.info("testOnCreateShard ending");
    }

    @Test
    public void testOnCreateShardWithLocalMemberNotInShardConfig() {
        LOG.info("testOnCreateShardWithLocalMemberNotInShardConfig starting");
        new TestKit(getSystem()) {
            {
                datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

                ActorRef shardManager = actorFactory
                        .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                                .withDispatcher(Dispatchers.DefaultDispatcherId()));

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), ActorRef.noSender());

                Shard.Builder shardBuilder = Shard.builder();
                ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("foo-ns"), "foo-module",
                        "foo", null, members("member-5", "member-6"));

                shardManager.tell(new CreateShard(config, shardBuilder, null), getRef());
                expectMsgClass(duration("5 seconds"), Success.class);

                shardManager.tell(new FindLocalShard("foo", true), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardFound.class);

                assertEquals("peerMembers size", 0, shardBuilder.getPeerAddresses().size());
                assertEquals("schemaContext", DisableElectionsRaftPolicy.class.getName(), shardBuilder
                        .getDatastoreContext().getShardRaftConfig().getCustomRaftPolicyImplementationClass());
            }
        };

        LOG.info("testOnCreateShardWithLocalMemberNotInShardConfig ending");
    }

    @Test
    public void testOnCreateShardWithNoInitialSchemaContext() {
        LOG.info("testOnCreateShardWithNoInitialSchemaContext starting");
        new TestKit(getSystem()) {
            {
                ActorRef shardManager = actorFactory
                        .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                                .withDispatcher(Dispatchers.DefaultDispatcherId()));

                Shard.Builder shardBuilder = Shard.builder();

                ModuleShardConfiguration config = new ModuleShardConfiguration(URI.create("foo-ns"), "foo-module",
                        "foo", null, members("member-1"));
                shardManager.tell(new CreateShard(config, shardBuilder, null), getRef());

                expectMsgClass(duration("5 seconds"), Success.class);

                SchemaContext schemaContext = TestModel.createTestContext();
                shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

                shardManager.tell(new FindLocalShard("foo", true), getRef());

                expectMsgClass(duration("5 seconds"), LocalShardFound.class);

                assertSame("schemaContext", schemaContext, shardBuilder.getSchemaContext());
                assertNotNull("schemaContext is null", shardBuilder.getDatastoreContext());
            }
        };

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

        shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), ActorRef.noSender());

        waitForShardInitialized(shardManager, "shard1", kit);
        waitForShardInitialized(shardManager, "shard2", kit);

        shardManager.tell(GetSnapshot.INSTANCE, kit.getRef());

        DatastoreSnapshot datastoreSnapshot = expectMsgClassOrFailure(DatastoreSnapshot.class, kit, "GetSnapshot");

        assertEquals("getType", shardMrgIDSuffix, datastoreSnapshot.getType());
        assertNull("Expected null ShardManagerSnapshot", datastoreSnapshot.getShardManagerSnapshot());

        Function<ShardSnapshot, String> shardNameTransformer = ShardSnapshot::getName;

        assertEquals("Shard names", Sets.newHashSet("shard1", "shard2"), Sets.newHashSet(
                Lists.transform(datastoreSnapshot.getShardSnapshots(), shardNameTransformer)));

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
                Lists.transform(datastoreSnapshot.getShardSnapshots(), shardNameTransformer)));

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
                new ShardManagerSnapshot(Arrays.asList("shard1", "shard2", "astronauts"), Collections.emptyMap());
        DatastoreSnapshot restoreFromSnapshot = new DatastoreSnapshot(shardMrgIDSuffix, snapshot,
                Collections.<ShardSnapshot>emptyList());
        TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(newTestShardMgrBuilder(mockConfig)
                .restoreFromSnapshot(restoreFromSnapshot).props().withDispatcher(Dispatchers.DefaultDispatcherId()));

        shardManager.underlyingActor().waitForRecoveryComplete();

        shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), ActorRef.noSender());

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
        new TestKit(getSystem()) {
            {
                ActorRef shardManager = actorFactory
                        .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                                .withDispatcher(Dispatchers.DefaultDispatcherId()));

                shardManager.tell(new AddShardReplica("model-inventory"), getRef());
                Status.Failure resp = expectMsgClass(duration("2 seconds"), Status.Failure.class);

                assertEquals("Failure obtained", true, resp.cause() instanceof IllegalArgumentException);
            }
        };
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

        new TestKit(system1) {
            {
                newReplicaShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                leaderShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

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
                        new ShardManagerSnapshot(Arrays.asList(restoredShards), Collections.emptyMap());
                InMemorySnapshotStore.addSnapshot(shardManagerID, snapshot);
                Uninterruptibles.sleepUninterruptibly(2, TimeUnit.MILLISECONDS);

                InMemorySnapshotStore.addSnapshotSavedLatch(shardManagerID);
                InMemorySnapshotStore.addSnapshotDeletedLatch(shardManagerID);

                // construct a mock response message
                newReplicaShardManager.tell(new AddShardReplica("astronauts"), getRef());
                AddServer addServerMsg = MessageCollectorActor.expectFirstMatching(mockShardLeaderActor,
                        AddServer.class);
                String addServerId = "member-1-shard-astronauts-" + shardMrgIDSuffix;
                assertEquals("AddServer serverId", addServerId, addServerMsg.getNewServerId());
                expectMsgClass(duration("5 seconds"), Status.Success.class);

                InMemorySnapshotStore.waitForSavedSnapshot(shardManagerID, ShardManagerSnapshot.class);
                InMemorySnapshotStore.waitForDeletedSnapshot(shardManagerID);
                List<ShardManagerSnapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(shardManagerID,
                        ShardManagerSnapshot.class);
                assertEquals("Number of snapshots persisted", 1, persistedSnapshots.size());
                ShardManagerSnapshot shardManagerSnapshot = persistedSnapshots.get(0);
                assertEquals("Persisted local shards", Sets.newHashSet("default", "astronauts"),
                        Sets.newHashSet(shardManagerSnapshot.getShardList()));
            }
        };
        LOG.info("testAddShardReplica ending");
    }

    @Test
    public void testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader() {
        LOG.info("testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader starting");
        new TestKit(getSystem()) {
            {
                TestActorRef<TestShardManager> shardManager = actorFactory
                        .createTestActor(newPropsShardMgrWithMockShardActor(), shardMgrID);

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
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

                shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), getRef());

                MessageCollectorActor.expectFirstMatching(leaderShardActor, AddServer.class);

                Failure resp = expectMsgClass(duration("5 seconds"), Failure.class);
                assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

                shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardFound.class);

                // Send message again to verify previous in progress state is
                // cleared

                shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), getRef());
                resp = expectMsgClass(duration("5 seconds"), Failure.class);
                assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

                // Send message again with an AddServer timeout to verify the
                // pre-existing shard actor isn't terminated.

                shardManager.tell(
                        newDatastoreContextFactory(
                                datastoreContextBuilder.shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build()),
                        getRef());
                leaderShardActor.tell(MockRespondActor.CLEAR_RESPONSE, ActorRef.noSender());
                shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), getRef());
                expectMsgClass(duration("5 seconds"), Failure.class);

                shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardFound.class);
            }
        };

        LOG.info("testAddShardReplicaWithPreExistingReplicaInRemoteShardLeader ending");
    }

    @Test
    public void testAddShardReplicaWithPreExistingLocalReplicaLeader() {
        LOG.info("testAddShardReplicaWithPreExistingLocalReplicaLeader starting");
        new TestKit(getSystem()) {
            {
                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;
                ActorRef shardManager = actorFactory.createActor(newPropsShardMgrWithMockShardActor());

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), mockShardActor);
                shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mock(DataTree.class),
                        DataStoreVersions.CURRENT_VERSION), getRef());
                shardManager.tell(
                        new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
                        mockShardActor);

                shardManager.tell(new AddShardReplica(Shard.DEFAULT_NAME), getRef());
                Failure resp = expectMsgClass(duration("5 seconds"), Failure.class);
                assertEquals("Failure cause", AlreadyExistsException.class, resp.cause().getClass());

                shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());
                expectMsgClass(duration("5 seconds"), LocalShardFound.class);
            }
        };

        LOG.info("testAddShardReplicaWithPreExistingLocalReplicaLeader ending");
    }

    @Test
    public void testAddShardReplicaWithAddServerReplyFailure() {
        LOG.info("testAddShardReplicaWithAddServerReplyFailure starting");
        new TestKit(getSystem()) {
            {
                TestKit mockShardLeaderKit = new TestKit(getSystem());

                MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                        .put("astronauts", Arrays.asList("member-2")).build());

                ActorRef mockNewReplicaShardActor = newMockShardActor(getSystem(), "astronauts", "member-1");
                final TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(
                        newTestShardMgrBuilder(mockConfig).shardActor(mockNewReplicaShardActor).props()
                            .withDispatcher(Dispatchers.DefaultDispatcherId()), shardMgrID);
                shardManager.underlyingActor()
                        .setMessageInterceptor(newFindPrimaryInterceptor(mockShardLeaderKit.getRef()));

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                TestKit terminateWatcher = new TestKit(getSystem());
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
            }
        };

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
        new TestKit(getSystem()) {
            {
                MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                        .put("astronauts", Arrays.asList("member-2")).build());

                final ActorRef newReplicaShardManager = actorFactory
                        .createActor(newTestShardMgrBuilder(mockConfig).shardActor(mockShardActor).props()
                                .withDispatcher(Dispatchers.DefaultDispatcherId()), shardMgrID);

                newReplicaShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                MockClusterWrapper.sendMemberUp(newReplicaShardManager, "member-2",
                        AddressFromURIString.parse("akka://non-existent@127.0.0.1:5").toString());

                newReplicaShardManager.tell(new AddShardReplica("astronauts"), getRef());
                Status.Failure resp = expectMsgClass(duration("5 seconds"), Status.Failure.class);
                assertEquals("Failure obtained", true, resp.cause() instanceof RuntimeException);
            }
        };

        LOG.info("testAddShardReplicaWithFindPrimaryTimeout ending");
    }

    @Test
    public void testRemoveShardReplicaForNonExistentShard() {
        new TestKit(getSystem()) {
            {
                ActorRef shardManager = actorFactory
                        .createActor(newShardMgrProps(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                                .withDispatcher(Dispatchers.DefaultDispatcherId()));

                shardManager.tell(new RemoveShardReplica("model-inventory", MEMBER_1), getRef());
                Status.Failure resp = expectMsgClass(duration("10 seconds"), Status.Failure.class);
                assertEquals("Failure obtained", true, resp.cause() instanceof PrimaryNotFoundException);
            }
        };
    }

    @Test
    /**
     * Primary is Local.
     */
    public void testRemoveShardReplicaLocal() {
        new TestKit(getSystem()) {
            {
                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

                final ActorRef respondActor = actorFactory.createActor(Props.create(MockRespondActor.class,
                        RemoveServer.class, new RemoveServerReply(ServerChangeStatus.OK, null)), memberId);

                ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor(respondActor));

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), respondActor);
                shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mock(DataTree.class),
                        DataStoreVersions.CURRENT_VERSION), getRef());
                shardManager.tell(
                        new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
                        respondActor);

                shardManager.tell(new RemoveShardReplica(Shard.DEFAULT_NAME, MEMBER_1), getRef());
                final RemoveServer removeServer = MessageCollectorActor.expectFirstMatching(respondActor,
                        RemoveServer.class);
                assertEquals(ShardIdentifier.create("default", MEMBER_1, shardMrgIDSuffix).toString(),
                        removeServer.getServerId());
                expectMsgClass(duration("5 seconds"), Success.class);
            }
        };
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

        new TestKit(system1) {
            {
                newReplicaShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                leaderShardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

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
                newReplicaShardManager.tell(new RemoveShardReplica("default", MEMBER_1), getRef());
                RemoveServer removeServer = MessageCollectorActor.expectFirstMatching(mockShardLeaderActor,
                        RemoveServer.class);
                String removeServerId = ShardIdentifier.create("default", MEMBER_1, shardMrgIDSuffix).toString();
                assertEquals("RemoveServer serverId", removeServerId, removeServer.getServerId());
                expectMsgClass(duration("5 seconds"), Status.Success.class);
            }
        };
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
        new TestKit(getSystem()) {
            {
                TestKit mockShardLeaderKit = new TestKit(getSystem());
                final TestKit secondRequestKit = new TestKit(getSystem());

                MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                        .put(shardName, Arrays.asList("member-2")).build());

                final TestActorRef<TestShardManager> shardManager = TestActorRef.create(getSystem(),
                        newTestShardMgrBuilder().configuration(mockConfig).shardActor(mockShardActor)
                                .cluster(new MockClusterWrapper()).props()
                                .withDispatcher(Dispatchers.DefaultDispatcherId()),
                        shardMgrID);

                shardManager.underlyingActor()
                        .setMessageInterceptor(newFindPrimaryInterceptor(mockShardLeaderKit.getRef()));

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

                shardManager.tell(firstServerChange, getRef());

                mockShardLeaderKit.expectMsgClass(firstForwardedServerChangeClass);

                shardManager.tell(secondServerChange, secondRequestKit.getRef());

                secondRequestKit.expectMsgClass(duration("5 seconds"), Failure.class);
            }
        };
    }

    @Test
    public void testServerRemovedShardActorNotRunning() {
        LOG.info("testServerRemovedShardActorNotRunning starting");
        new TestKit(getSystem()) {
            {
                MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                        .put("default", Arrays.asList("member-1", "member-2"))
                        .put("astronauts", Arrays.asList("member-2"))
                        .put("people", Arrays.asList("member-1", "member-2")).build());

                TestActorRef<TestShardManager> shardManager = actorFactory.createTestActor(
                        newShardMgrProps(mockConfig).withDispatcher(Dispatchers.DefaultDispatcherId()));

                shardManager.underlyingActor().waitForRecoveryComplete();
                shardManager.tell(new FindLocalShard("people", false), getRef());
                expectMsgClass(duration("5 seconds"), NotInitializedException.class);

                shardManager.tell(new FindLocalShard("default", false), getRef());
                expectMsgClass(duration("5 seconds"), NotInitializedException.class);

                // Removed the default shard replica from member-1
                ShardIdentifier.Builder builder = new ShardIdentifier.Builder();
                ShardIdentifier shardId = builder.shardName("default").memberName(MEMBER_1).type(shardMrgIDSuffix)
                        .build();
                shardManager.tell(new ServerRemoved(shardId.toString()), getRef());

                shardManager.underlyingActor().verifySnapshotPersisted(Sets.newHashSet("people"));
            }
        };

        LOG.info("testServerRemovedShardActorNotRunning ending");
    }

    @Test
    public void testServerRemovedShardActorRunning() {
        LOG.info("testServerRemovedShardActorRunning starting");
        new TestKit(getSystem()) {
            {
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

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), shard);

                waitForShardInitialized(shardManager, "people", this);
                waitForShardInitialized(shardManager, "default", this);

                // Removed the default shard replica from member-1
                shardManager.tell(new ServerRemoved(shardId), getRef());

                shardManager.underlyingActor().verifySnapshotPersisted(Sets.newHashSet("people"));

                MessageCollectorActor.expectFirstMatching(shard, Shutdown.class);
            }
        };

        LOG.info("testServerRemovedShardActorRunning ending");
    }

    @Test
    public void testShardPersistenceWithRestoredData() {
        LOG.info("testShardPersistenceWithRestoredData starting");
        new TestKit(getSystem()) {
            {
                MockConfiguration mockConfig =
                    new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                            .put("default", Arrays.asList("member-1", "member-2"))
                            .put("astronauts", Arrays.asList("member-2"))
                            .put("people", Arrays.asList("member-1", "member-2")).build());
                String[] restoredShards = {"default", "astronauts"};
                ShardManagerSnapshot snapshot =
                        new ShardManagerSnapshot(Arrays.asList(restoredShards), Collections.emptyMap());
                InMemorySnapshotStore.addSnapshot("shard-manager-" + shardMrgIDSuffix, snapshot);

                // create shardManager to come up with restored data
                TestActorRef<TestShardManager> newRestoredShardManager = actorFactory.createTestActor(
                        newShardMgrProps(mockConfig).withDispatcher(Dispatchers.DefaultDispatcherId()));

                newRestoredShardManager.underlyingActor().waitForRecoveryComplete();

                newRestoredShardManager.tell(new FindLocalShard("people", false), getRef());
                LocalShardNotFound notFound = expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);
                assertEquals("for uninitialized shard", "people", notFound.getShardName());

                // Verify a local shard is created for the restored shards,
                // although we expect a NotInitializedException for the shards
                // as the actor initialization
                // message is not sent for them
                newRestoredShardManager.tell(new FindLocalShard("default", false), getRef());
                expectMsgClass(duration("5 seconds"), NotInitializedException.class);

                newRestoredShardManager.tell(new FindLocalShard("astronauts", false), getRef());
                expectMsgClass(duration("5 seconds"), NotInitializedException.class);
            }
        };

        LOG.info("testShardPersistenceWithRestoredData ending");
    }

    @Test
    public void testShutDown() throws Exception {
        LOG.info("testShutDown starting");
        new TestKit(getSystem()) {
            {
                MockConfiguration mockConfig = new MockConfiguration(ImmutableMap.<String, List<String>>builder()
                        .put("shard1", Arrays.asList("member-1")).put("shard2", Arrays.asList("member-1")).build());

                String shardId1 = ShardIdentifier.create("shard1", MEMBER_1, shardMrgIDSuffix).toString();
                ActorRef shard1 = actorFactory.createActor(MessageCollectorActor.props(), shardId1);

                String shardId2 = ShardIdentifier.create("shard2", MEMBER_1, shardMrgIDSuffix).toString();
                ActorRef shard2 = actorFactory.createActor(MessageCollectorActor.props(), shardId2);

                ActorRef shardManager = actorFactory.createActor(newTestShardMgrBuilder(mockConfig)
                        .addShardActor("shard1", shard1).addShardActor("shard2", shard2).props());

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
                } catch (TimeoutException e) {
                    // expected
                }

                actorFactory.killActor(shard1, this);
                actorFactory.killActor(shard2, this);

                Boolean stopped = Await.result(stopFuture, duration);
                assertEquals("Stopped", Boolean.TRUE, stopped);
            }
        };

        LOG.info("testShutDown ending");
    }

    @Test
    public void testChangeServersVotingStatus() {
        new TestKit(getSystem()) {
            {
                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

                ActorRef respondActor = actorFactory
                        .createActor(Props.create(MockRespondActor.class, ChangeServersVotingStatus.class,
                                new ServerChangeReply(ServerChangeStatus.OK, null)), memberId);

                ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor(respondActor));

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), respondActor);
                shardManager.tell(new ShardLeaderStateChanged(memberId, memberId, mock(DataTree.class),
                        DataStoreVersions.CURRENT_VERSION), getRef());
                shardManager.tell(
                        new RoleChangeNotification(memberId, RaftState.Candidate.name(), RaftState.Leader.name()),
                        respondActor);

                shardManager.tell(
                        new ChangeShardMembersVotingStatus("default", ImmutableMap.of("member-2", Boolean.TRUE)),
                        getRef());

                ChangeServersVotingStatus actualChangeStatusMsg = MessageCollectorActor
                        .expectFirstMatching(respondActor, ChangeServersVotingStatus.class);
                assertEquals("ChangeServersVotingStatus map", actualChangeStatusMsg.getServerVotingStatusMap(),
                        ImmutableMap.of(ShardIdentifier
                                .create("default", MemberName.forName("member-2"), shardMrgIDSuffix).toString(),
                                Boolean.TRUE));

                expectMsgClass(duration("5 seconds"), Success.class);
            }
        };
    }

    @Test
    public void testChangeServersVotingStatusWithNoLeader() {
        new TestKit(getSystem()) {
            {
                String memberId = "member-1-shard-default-" + shardMrgIDSuffix;

                ActorRef respondActor = actorFactory
                        .createActor(Props.create(MockRespondActor.class, ChangeServersVotingStatus.class,
                                new ServerChangeReply(ServerChangeStatus.NO_LEADER, null)), memberId);

                ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor(respondActor));

                shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
                shardManager.tell(new ActorInitialized(), respondActor);
                shardManager.tell(new RoleChangeNotification(memberId, null, RaftState.Follower.name()), respondActor);

                shardManager.tell(
                        new ChangeShardMembersVotingStatus("default", ImmutableMap.of("member-2", Boolean.TRUE)),
                        getRef());

                MessageCollectorActor.expectFirstMatching(respondActor, ChangeServersVotingStatus.class);

                Status.Failure resp = expectMsgClass(duration("5 seconds"), Status.Failure.class);
                assertEquals("Failure resposnse", true, resp.cause() instanceof NoShardLeaderException);
            }
        };
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
            assertEquals("Recovery complete", true,
                    Uninterruptibles.awaitUninterruptibly(recoveryComplete, 5, TimeUnit.SECONDS));
        }

        public void waitForMemberUp() {
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
                this.shardActor = newShardActor;
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
            assertEquals("saveSnapshot invoked", true,
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
            cluster(new MockClusterWrapper()).configuration(new MockConfiguration()).waitTillReadyCountDownLatch(ready)
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
