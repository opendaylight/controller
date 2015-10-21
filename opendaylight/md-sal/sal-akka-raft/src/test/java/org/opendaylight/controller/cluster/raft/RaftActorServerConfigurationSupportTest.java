/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectFirstMatching;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
//import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.ForwardMessageToBehaviorActor;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Unit tests for RaftActorServerConfigurationSupport.
 *
 * @author Thomas Pantelis
 */
public class RaftActorServerConfigurationSupportTest extends AbstractActorTest {
    static final String LEADER_ID = "leader";
    static final String FOLLOWER_ID = "follower";
    static final String NEW_SERVER_ID = "new-server";
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorServerConfigurationSupportTest.class);
    private static final DataPersistenceProvider NO_PERSISTENCE = new NonPersistentDataProvider();

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private final TestActorRef<ForwardMessageToBehaviorActor> followerActor = actorFactory.createTestActor(
            Props.create(ForwardMessageToBehaviorActor.class).withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId(FOLLOWER_ID));

    private TestActorRef<MockNewFollowerRaftActor> newFollowerRaftActor;
    private TestActorRef<MessageCollectorActor> newFollowerCollectorActor;

    private RaftActorContext newFollowerActorContext;
    private final JavaTestKit testKit = new JavaTestKit(getSystem());

    @Before
    public void setup() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        configParams.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        newFollowerCollectorActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(NEW_SERVER_ID + "Collector"));
        newFollowerRaftActor = actorFactory.createTestActor(MockNewFollowerRaftActor.props(
                configParams, newFollowerCollectorActor).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(NEW_SERVER_ID));
        newFollowerActorContext = newFollowerRaftActor.underlyingActor().getRaftActorContext();
    }

    @After
    public void tearDown() throws Exception {
        actorFactory.close();
    }

    @Test
    public void testAddServerWithExistingFollower() throws Exception {
        RaftActorContext followerActorContext = newFollowerContext(FOLLOWER_ID, followerActor);
        followerActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(
                0, 3, 1).build());
        followerActorContext.setCommitIndex(2);
        followerActorContext.setLastApplied(2);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, followerActor.path().toString()),
                        followerActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        // Expect initial heartbeat from the leader.
        expectFirstMatching(followerActor, AppendEntries.class);
        clearMessages(followerActor);

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Leader should install snapshot - capture and verify ApplySnapshot contents

        ApplySnapshot applySnapshot = expectFirstMatching(newFollowerCollectorActor, ApplySnapshot.class);
        List<Object> snapshotState = (List<Object>) MockRaftActor.toObject(applySnapshot.getSnapshot().getState());
        assertEquals("Snapshot state", snapshotState, leaderRaftActor.getState());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint());

        // Verify ServerConfigurationPayload entry in leader's log

        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();
        assertEquals("Leader journal last index", 3, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 3, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 3, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), LEADER_ID, FOLLOWER_ID, NEW_SERVER_ID);

        // Verify ServerConfigurationPayload entry in both followers

        assertEquals("Follower journal last index", 3, followerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(followerActorContext.getReplicatedLog(), LEADER_ID, FOLLOWER_ID, NEW_SERVER_ID);

        assertEquals("New follower journal last index", 3, newFollowerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(newFollowerActorContext.getReplicatedLog(), LEADER_ID, FOLLOWER_ID, NEW_SERVER_ID);

        // Verify new server config was applied in both followers

        assertEquals("Follower peers", Sets.newHashSet(LEADER_ID, NEW_SERVER_ID),
                followerActorContext.getPeerAddresses().keySet());

        assertEquals("New follower peers", Sets.newHashSet(LEADER_ID, FOLLOWER_ID),
                newFollowerActorContext.getPeerAddresses().keySet());

        clearMessages(followerActor);
        clearMessages(newFollowerCollectorActor);

        expectFirstMatching(newFollowerCollectorActor, ApplyState.class);
        expectFirstMatching(followerActor, ApplyState.class);

        assertEquals("Follower commit index", 3, followerActorContext.getCommitIndex());
        assertEquals("Follower last applied index", 3, followerActorContext.getLastApplied());
        assertEquals("New follower commit index", 3, newFollowerActorContext.getCommitIndex());
        assertEquals("New follower last applied index", 3, newFollowerActorContext.getLastApplied());
    }

    @Test
    public void testAddServerWithNoExistingFollower() throws Exception {
        RaftActorContext initialActorContext = new MockRaftActorContext();
        initialActorContext.setCommitIndex(1);
        initialActorContext.setLastApplied(1);
        initialActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(
                0, 2, 1).build());

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Leader should install snapshot - capture and verify ApplySnapshot contents

        ApplySnapshot applySnapshot = expectFirstMatching(newFollowerCollectorActor, ApplySnapshot.class);
        List<Object> snapshotState = (List<Object>) MockRaftActor.toObject(applySnapshot.getSnapshot().getState());
        assertEquals("Snapshot state", snapshotState, leaderRaftActor.getState());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint());

        // Verify ServerConfigurationPayload entry in leader's log

        assertEquals("Leader journal last index", 2, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 2, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 2, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), LEADER_ID, NEW_SERVER_ID);

        // Verify ServerConfigurationPayload entry in the new follower

        clearMessages(newFollowerCollectorActor);

        expectFirstMatching(newFollowerCollectorActor, ApplyState.class);
        assertEquals("New follower journal last index", 2, newFollowerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(newFollowerActorContext.getReplicatedLog(), LEADER_ID, NEW_SERVER_ID);

        // Verify new server config was applied in the new follower

        assertEquals("New follower peers", Sets.newHashSet(LEADER_ID),
                newFollowerActorContext.getPeerAddresses().keySet());
    }

    @Test
    public void testAddServerAsNonVoting() throws Exception {
        RaftActorContext initialActorContext = new MockRaftActorContext();
        initialActorContext.setCommitIndex(-1);
        initialActorContext.setLastApplied(-1);
        initialActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), false), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint());

        // Verify ServerConfigurationPayload entry in leader's log

        assertEquals("Leader journal last index", 0, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 0, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 0, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), LEADER_ID, NEW_SERVER_ID);

        // Verify ServerConfigurationPayload entry in the new follower

        expectFirstMatching(newFollowerCollectorActor, ApplyState.class);
        assertEquals("New follower journal last index", 0, newFollowerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(newFollowerActorContext.getReplicatedLog(), LEADER_ID, NEW_SERVER_ID);

        // Verify new server config was applied in the new follower

        assertEquals("New follower peers", Sets.newHashSet(LEADER_ID),
                newFollowerActorContext.getPeerAddresses().keySet());

        MessageCollectorActor.assertNoneMatching(newFollowerCollectorActor, InstallSnapshot.SERIALIZABLE_CLASS, 500);
    }

    @Test
    public void testAddServerWithInstallSnapshotTimeout() throws Exception {
        newFollowerRaftActor.underlyingActor().setDropMessageOfType(InstallSnapshot.SERIALIZABLE_CLASS);

        RaftActorContext initialActorContext = new MockRaftActorContext();
        initialActorContext.setCommitIndex(-1);
        initialActorContext.setLastApplied(-1);
        initialActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.TIMEOUT, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerAddresses().keySet().size());
        assertEquals("Leader followers size", 0,
                ((AbstractLeader)leaderRaftActor.getCurrentBehavior()).getFollowerIds().size());
    }

    @Test
    public void testAddServerWithNoLeader() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MockRaftActor> noLeaderActor = actorFactory.createTestActor(
                MockRaftActor.props(LEADER_ID, ImmutableMap.<String,String>of(FOLLOWER_ID, followerActor.path().toString()),
                        Optional.<ConfigParams>of(configParams), NO_PERSISTENCE).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));
        noLeaderActor.underlyingActor().waitForInitializeBehaviorComplete();

        noLeaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());
        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());
    }

    @Test
    public void testAddServerForwardedToLeader() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MessageCollectorActor> leaderActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        TestActorRef<MockRaftActor> followerRaftActor = actorFactory.createTestActor(
                MockRaftActor.props(FOLLOWER_ID, ImmutableMap.<String,String>of(LEADER_ID, leaderActor.path().toString()),
                        Optional.<ConfigParams>of(configParams), NO_PERSISTENCE).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(FOLLOWER_ID));
        followerRaftActor.underlyingActor().waitForInitializeBehaviorComplete();

        followerRaftActor.tell(new AppendEntries(1, LEADER_ID, 0, 1, Collections.<ReplicatedLogEntry>emptyList(),
                -1, -1, (short)0), leaderActor);

        followerRaftActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());
        expectFirstMatching(leaderActor, AddServer.class);
    }

    private void verifyServerConfigurationPayloadEntry(ReplicatedLog log, String... cNew) {
        ReplicatedLogEntry logEntry = log.get(log.lastIndex());
        assertEquals("Last log entry payload class", ServerConfigurationPayload.class, logEntry.getData().getClass());
        ServerConfigurationPayload payload = (ServerConfigurationPayload)logEntry.getData();
        assertEquals("getNewServerConfig", Sets.newHashSet(cNew), Sets.newHashSet(payload.getNewServerConfig()));
    }

    private RaftActorContext newFollowerContext(String id, TestActorRef<? extends UntypedActor> actor) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        ElectionTermImpl termInfo = new ElectionTermImpl(NO_PERSISTENCE, id, LOG);
        termInfo.update(1, LEADER_ID);
        RaftActorContext followerActorContext = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
                id, termInfo, -1, -1,
                ImmutableMap.of(LEADER_ID, ""), configParams, NO_PERSISTENCE, LOG);

        return followerActorContext;
    }

    public static class MockLeaderRaftActor extends MockRaftActor {
        public MockLeaderRaftActor(Map<String, String> peerAddresses, ConfigParams config,
                RaftActorContext fromContext) {
            super(LEADER_ID, peerAddresses, Optional.of(config), NO_PERSISTENCE);

            RaftActorContext context = getRaftActorContext();
            for(int i = 0; i < fromContext.getReplicatedLog().size(); i++) {
                ReplicatedLogEntry entry = fromContext.getReplicatedLog().get(i);
                getState().add(entry.getData());
                context.getReplicatedLog().append(entry);
            }

            context.setCommitIndex(fromContext.getCommitIndex());
            context.setLastApplied(fromContext.getLastApplied());
            context.getTermInformation().update(fromContext.getTermInformation().getCurrentTerm(),
                    fromContext.getTermInformation().getVotedFor());
        }

        @Override
        protected void initializeBehavior() {
            changeCurrentBehavior(new Leader(getRaftActorContext()));
            initializeBehaviorComplete.countDown();
        }

        @Override
        public void createSnapshot(ActorRef actorRef) {
            try {
                actorRef.tell(new CaptureSnapshotReply(RaftActorTest.fromObject(getState()).toByteArray()), actorRef);
            } catch (Exception e) {
                LOG.error("createSnapshot failed", e);
            }
        }

        static Props props(Map<String, String> peerAddresses, RaftActorContext fromContext) {
            DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
            configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
            configParams.setElectionTimeoutFactor(1);
            return Props.create(MockLeaderRaftActor.class, peerAddresses, configParams, fromContext);
        }
    }

    public static class MockNewFollowerRaftActor extends MockRaftActor {
        private final TestActorRef<MessageCollectorActor> collectorActor;
        private volatile Class<?> dropMessageOfType;

        public MockNewFollowerRaftActor(ConfigParams config, TestActorRef<MessageCollectorActor> collectorActor) {
            super(NEW_SERVER_ID, Maps.<String, String>newHashMap(), Optional.of(config), null);
            this.collectorActor = collectorActor;
        }

        void setDropMessageOfType(Class<?> dropMessageOfType) {
            this.dropMessageOfType = dropMessageOfType;
        }

        @Override
        public void handleCommand(Object message) {
            if(dropMessageOfType != null && dropMessageOfType.equals(message.getClass())) {
                return;
            }

            super.handleCommand(message);
            collectorActor.tell(message, getSender());
        }

        static Props props(ConfigParams config, TestActorRef<MessageCollectorActor> collectorActor) {
            return Props.create(MockNewFollowerRaftActor.class, config, collectorActor);
        }
    }
}
