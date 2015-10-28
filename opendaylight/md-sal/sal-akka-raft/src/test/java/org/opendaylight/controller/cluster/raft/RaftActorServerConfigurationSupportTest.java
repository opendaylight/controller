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
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.FollowerCatchUpTimeout;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.messages.UnInitializedFollowerSnapshotReply;
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
    static final String NEW_SERVER_ID2 = "new-server2";
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

        DefaultConfigParamsImpl configParams = newFollowerConfigParams();

        newFollowerCollectorActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(NEW_SERVER_ID + "Collector"));
        newFollowerRaftActor = actorFactory.createTestActor(MockNewFollowerRaftActor.props(
                configParams, newFollowerCollectorActor).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(NEW_SERVER_ID));

        try {
            newFollowerActorContext = newFollowerRaftActor.underlyingActor().getRaftActorContext();
        } catch (Exception e) {
            newFollowerActorContext = newFollowerRaftActor.underlyingActor().getRaftActorContext();
        }
    }

    private static DefaultConfigParamsImpl newFollowerConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        configParams.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());
        return configParams;
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
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(FOLLOWER_ID), votingServer(NEW_SERVER_ID));

        // Verify ServerConfigurationPayload entry in both followers

        assertEquals("Follower journal last index", 3, followerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(followerActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(FOLLOWER_ID), votingServer(NEW_SERVER_ID));

        assertEquals("New follower journal last index", 3, newFollowerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(newFollowerActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(FOLLOWER_ID), votingServer(NEW_SERVER_ID));

        // Verify new server config was applied in both followers

        assertEquals("Follower peers", Sets.newHashSet(LEADER_ID, NEW_SERVER_ID), followerActorContext.getPeerIds());

        assertEquals("New follower peers", Sets.newHashSet(LEADER_ID, FOLLOWER_ID), newFollowerActorContext.getPeerIds());

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
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(NEW_SERVER_ID));

        // Verify ServerConfigurationPayload entry in the new follower

        expectFirstMatching(newFollowerCollectorActor, ApplyState.class);
        assertEquals("New follower journal last index", 2, newFollowerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(newFollowerActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(NEW_SERVER_ID));

        // Verify new server config was applied in the new follower

        assertEquals("New follower peers", Sets.newHashSet(LEADER_ID), newFollowerActorContext.getPeerIds());
    }

    @Test
    public void testAddServersAsNonVoting() throws Exception {
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
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                nonVotingServer(NEW_SERVER_ID));

        // Verify ServerConfigurationPayload entry in the new follower

        expectFirstMatching(newFollowerCollectorActor, ApplyState.class);
        assertEquals("New follower journal last index", 0, newFollowerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(newFollowerActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                nonVotingServer(NEW_SERVER_ID));

        // Verify new server config was applied in the new follower

        assertEquals("New follower peers", Sets.newHashSet(LEADER_ID), newFollowerActorContext.getPeerIds());

        MessageCollectorActor.assertNoneMatching(newFollowerCollectorActor, InstallSnapshot.class, 500);

        // Add another non-voting server.

        RaftActorContext follower2ActorContext = newFollowerContext(NEW_SERVER_ID2, followerActor);
        Follower newFollower2 = new Follower(follower2ActorContext);
        followerActor.underlyingActor().setBehavior(newFollower2);

        leaderActor.tell(new AddServer(NEW_SERVER_ID2, followerActor.path().toString(), false), testKit.getRef());

        addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint());

        assertEquals("Leader journal last index", 1, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 1, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 1, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(),
                votingServer(LEADER_ID), nonVotingServer(NEW_SERVER_ID), nonVotingServer(NEW_SERVER_ID2));
    }

    @Test
    public void testAddServerWithOperationInProgress() throws Exception {
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

        RaftActorContext follower2ActorContext = newFollowerContext(NEW_SERVER_ID2, followerActor);
        Follower newFollower2 = new Follower(follower2ActorContext);
        followerActor.underlyingActor().setBehavior(newFollower2);

        MockNewFollowerRaftActor newFollowerRaftActorInstance = newFollowerRaftActor.underlyingActor();
        newFollowerRaftActorInstance.setDropMessageOfType(InstallSnapshot.SERIALIZABLE_CLASS);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Wait for leader's install snapshot and capture it

        Object installSnapshot = expectFirstMatching(newFollowerCollectorActor, InstallSnapshot.class);

        // Send a second AddServer - should get queued
        JavaTestKit testKit2 = new JavaTestKit(getSystem());
        leaderActor.tell(new AddServer(NEW_SERVER_ID2, followerActor.path().toString(), false), testKit2.getRef());

        // Continue the first AddServer
        newFollowerRaftActorInstance.setDropMessageOfType(null);
        newFollowerRaftActor.tell(installSnapshot, leaderActor);

        // Verify both complete successfully
        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());

        addServerReply = testKit2.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());

        // Verify ServerConfigurationPayload entries in leader's log

        assertEquals("Leader journal last index", 1, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 1, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 1, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(),
                votingServer(LEADER_ID), votingServer(NEW_SERVER_ID), nonVotingServer(NEW_SERVER_ID2));

        // Verify ServerConfigurationPayload entry in the new follower

        MessageCollectorActor.expectMatching(newFollowerCollectorActor, ApplyState.class, 2);

        assertEquals("New follower peers", Sets.newHashSet(LEADER_ID, NEW_SERVER_ID2),
               newFollowerActorContext.getPeerIds());
    }

    @Test
    public void testAddServerWithPriorSnapshotInProgress() throws Exception {
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

        TestActorRef<MessageCollectorActor> leaderCollectorActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID + "Collector"));
        leaderRaftActor.setCollectorActor(leaderCollectorActor);

        // Drop commit message for now to delay snapshot completion
        leaderRaftActor.setDropMessageOfType(String.class);

        leaderActor.tell(new InitiateCaptureSnapshot(), leaderActor);

        String commitMsg = expectFirstMatching(leaderCollectorActor, String.class);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        leaderRaftActor.setDropMessageOfType(null);
        leaderActor.tell(commitMsg, leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint());

        expectFirstMatching(newFollowerCollectorActor, ApplySnapshot.class);

        // Verify ServerConfigurationPayload entry in leader's log

        assertEquals("Leader journal last index", 0, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 0, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 0, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(NEW_SERVER_ID));
    }

    @Test
    public void testAddServerWithPriorSnapshotCompleteTimeout() throws Exception {
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

        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(1);

        TestActorRef<MessageCollectorActor> leaderCollectorActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID + "Collector"));
        leaderRaftActor.setCollectorActor(leaderCollectorActor);

        // Drop commit message so the snapshot doesn't complete.
        leaderRaftActor.setDropMessageOfType(String.class);

        leaderActor.tell(new InitiateCaptureSnapshot(), leaderActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.TIMEOUT, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());
    }

    @Test
    public void testAddServerWithLeaderChangeBeforePriorSnapshotComplete() throws Exception {
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
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(100);

        TestActorRef<MessageCollectorActor> leaderCollectorActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID + "Collector"));
        leaderRaftActor.setCollectorActor(leaderCollectorActor);

        // Drop the commit message so the snapshot doesn't complete yet.
        leaderRaftActor.setDropMessageOfType(String.class);

        leaderActor.tell(new InitiateCaptureSnapshot(), leaderActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        String commitMsg = expectFirstMatching(leaderCollectorActor, String.class);

        // Change the leader behavior to follower
        leaderActor.tell(new Follower(leaderActorContext), leaderActor);

        // Drop CaptureSnapshotReply in case install snapshot is incorrectly initiated after the prior
        // snapshot completes. This will prevent the invalid snapshot from completing and fail the
        // isCapturing assertion below.
        leaderRaftActor.setDropMessageOfType(CaptureSnapshotReply.class);

        // Complete the prior snapshot - this should be a no-op b/c it's no longer the leader
        leaderActor.tell(commitMsg, leaderActor);

        leaderActor.tell(new FollowerCatchUpTimeout(NEW_SERVER_ID), leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());
        assertEquals("isCapturing", false, leaderActorContext.getSnapshotManager().isCapturing());
    }

    @Test
    public void testAddServerWithLeaderChangeDuringInstallSnapshot() throws Exception {
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

        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(8);

        TestActorRef<MessageCollectorActor> leaderCollectorActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID + "Collector"));
        leaderRaftActor.setCollectorActor(leaderCollectorActor);

        // Drop the UnInitializedFollowerSnapshotReply to delay it.
        leaderRaftActor.setDropMessageOfType(UnInitializedFollowerSnapshotReply.class);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        UnInitializedFollowerSnapshotReply snapshotReply = expectFirstMatching(leaderCollectorActor,
                UnInitializedFollowerSnapshotReply.class);

        // Prevent election timeout when the leader switches to follower
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(100);

        // Change the leader behavior to follower
        leaderActor.tell(new Follower(leaderActorContext), leaderActor);

        // Send the captured UnInitializedFollowerSnapshotReply - should be a no-op
        leaderRaftActor.setDropMessageOfType(null);
        leaderActor.tell(snapshotReply, leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());
    }

    @Test
    public void testAddServerWithInstallSnapshotTimeout() throws Exception {
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
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(1);

        // Drop the InstallSnapshot message so it times out
        newFollowerRaftActor.underlyingActor().setDropMessageOfType(InstallSnapshot.SERIALIZABLE_CLASS);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        leaderActor.tell(new UnInitializedFollowerSnapshotReply("bogus"), leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.TIMEOUT, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());
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

    private ServerInfo votingServer(String id) {
        return new ServerInfo(id, true);
    }

    private ServerInfo nonVotingServer(String id) {
        return new ServerInfo(id, false);
    }

    private static void verifyServerConfigurationPayloadEntry(ReplicatedLog log, ServerInfo... expected) {
        ReplicatedLogEntry logEntry = log.get(log.lastIndex());
        assertEquals("Last log entry payload class", ServerConfigurationPayload.class, logEntry.getData().getClass());
        ServerConfigurationPayload payload = (ServerConfigurationPayload)logEntry.getData();
        assertEquals("getNewServerConfig", Sets.newHashSet(expected), Sets.newHashSet(payload.getServerConfig()));
    }

    private static RaftActorContext newFollowerContext(String id, TestActorRef<? extends UntypedActor> actor) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        ElectionTermImpl termInfo = new ElectionTermImpl(NO_PERSISTENCE, id, LOG);
        termInfo.update(1, LEADER_ID);
        RaftActorContext followerActorContext = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
                id, termInfo, -1, -1,
                ImmutableMap.of(LEADER_ID, ""), configParams, NO_PERSISTENCE, LOG);
        followerActorContext.setCommitIndex(-1);
        followerActorContext.setLastApplied(-1);
        followerActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());

        return followerActorContext;
    }

    static abstract class AbstractMockRaftActor extends MockRaftActor {
        private volatile TestActorRef<MessageCollectorActor> collectorActor;
        private volatile Class<?> dropMessageOfType;

        AbstractMockRaftActor(String id, Map<String, String> peerAddresses, Optional<ConfigParams> config,
                DataPersistenceProvider dataPersistenceProvider, TestActorRef<MessageCollectorActor> collectorActor) {
            super(id, peerAddresses, config, dataPersistenceProvider);
            this.collectorActor = collectorActor;
        }

        void setDropMessageOfType(Class<?> dropMessageOfType) {
            this.dropMessageOfType = dropMessageOfType;
        }

        void setCollectorActor(TestActorRef<MessageCollectorActor> collectorActor) {
            this.collectorActor = collectorActor;
        }

        @Override
        public void handleCommand(Object message) {
            if(dropMessageOfType == null || !dropMessageOfType.equals(message.getClass())) {
                super.handleCommand(message);
            }

            if(collectorActor != null) {
                collectorActor.tell(message, getSender());
            }
        }
    }

    public static class MockLeaderRaftActor extends AbstractMockRaftActor {
        public MockLeaderRaftActor(Map<String, String> peerAddresses, ConfigParams config,
                RaftActorContext fromContext) {
            super(LEADER_ID, peerAddresses, Optional.of(config), NO_PERSISTENCE, null);
            setPersistence(false);

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
            configParams.setElectionTimeoutFactor(10);
            return Props.create(MockLeaderRaftActor.class, peerAddresses, configParams, fromContext);
        }
    }

    public static class MockNewFollowerRaftActor extends AbstractMockRaftActor {
        public MockNewFollowerRaftActor(ConfigParams config, TestActorRef<MessageCollectorActor> collectorActor) {
            super(NEW_SERVER_ID, Maps.<String, String>newHashMap(), Optional.of(config), null, collectorActor);
        }

        static Props props(ConfigParams config, TestActorRef<MessageCollectorActor> collectorActor) {
            return Props.create(MockNewFollowerRaftActor.class, config, collectorActor);
        }
    }
}
