/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.assertNoneMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectFirstMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectMatching;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.ChangeServersVotingStatus;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.RemoveServer;
import org.opendaylight.controller.cluster.raft.messages.RemoveServerReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
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
    static final String FOLLOWER_ID2 = "follower2";
    static final String NEW_SERVER_ID = "new-server";
    static final String NEW_SERVER_ID2 = "new-server2";
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorServerConfigurationSupportTest.class);
    private static final boolean NO_PERSISTENCE = false;
    private static final boolean PERSISTENT = true;

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
    }

    private void setupNewFollower() {
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
        LOG.info("testAddServerWithExistingFollower starting");
        setupNewFollower();
        RaftActorContextImpl followerActorContext = newFollowerContext(FOLLOWER_ID, followerActor);
        followerActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(
                0, 3, 1).build());
        followerActorContext.setCommitIndex(2);
        followerActorContext.setLastApplied(2);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);
        followerActorContext.setCurrentBehavior(follower);

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, followerActor.path().toString()),
                        followerActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        // Expect initial heartbeat from the leader.
        expectFirstMatching(followerActor, AppendEntries.class);
        clearMessages(followerActor);

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        TestActorRef<MessageCollectorActor> leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Leader should install snapshot - capture and verify ApplySnapshot contents

        ApplySnapshot applySnapshot = expectFirstMatching(newFollowerCollectorActor, ApplySnapshot.class);
        @SuppressWarnings("unchecked")
        List<Object> snapshotState = (List<Object>) MockRaftActor.toObject(applySnapshot.getSnapshot().getState());
        assertEquals("Snapshot state", snapshotState, leaderRaftActor.getState());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().get());

        // Verify ServerConfigurationPayload entry in leader's log

        expectFirstMatching(leaderCollectorActor, ApplyState.class);
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();
        assertEquals("Leader journal last index", 3, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 3, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 3, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(FOLLOWER_ID), votingServer(NEW_SERVER_ID));

        // Verify ServerConfigurationPayload entry in both followers

        expectFirstMatching(followerActor, ApplyState.class);
        assertEquals("Follower journal last index", 3, followerActorContext.getReplicatedLog().lastIndex());
        verifyServerConfigurationPayloadEntry(followerActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(FOLLOWER_ID), votingServer(NEW_SERVER_ID));

        expectFirstMatching(newFollowerCollectorActor, ApplyState.class);
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

        List<ReplicatedLogImplEntry> persistedLogEntries = InMemoryJournal.get(LEADER_ID, ReplicatedLogImplEntry.class);
        assertEquals("Leader ReplicatedLogImplEntry entries", 1, persistedLogEntries.size());
        ReplicatedLogImplEntry logEntry = persistedLogEntries.get(0);
        assertEquals("Leader ReplicatedLogImplEntry getTerm", 1, logEntry.getTerm());
        assertEquals("Leader ReplicatedLogImplEntry getIndex", 3, logEntry.getIndex());
        assertEquals("Leader ReplicatedLogImplEntry getData", ServerConfigurationPayload.class, logEntry.getData().getClass());

        persistedLogEntries = InMemoryJournal.get(NEW_SERVER_ID, ReplicatedLogImplEntry.class);
        assertEquals("New follower ReplicatedLogImplEntry entries", 1, persistedLogEntries.size());
        logEntry = persistedLogEntries.get(0);
        assertEquals("New follower ReplicatedLogImplEntry getTerm", 1, logEntry.getTerm());
        assertEquals("New follower ReplicatedLogImplEntry getIndex", 3, logEntry.getIndex());
        assertEquals("New follower ReplicatedLogImplEntry getData", ServerConfigurationPayload.class,
                logEntry.getData().getClass());

        LOG.info("testAddServerWithExistingFollower ending");
    }

    @Test
    public void testAddServerWithNoExistingFollower() throws Exception {
        LOG.info("testAddServerWithNoExistingFollower starting");

        setupNewFollower();
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

        TestActorRef<MessageCollectorActor> leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Leader should install snapshot - capture and verify ApplySnapshot contents

        ApplySnapshot applySnapshot = expectFirstMatching(newFollowerCollectorActor, ApplySnapshot.class);
        @SuppressWarnings("unchecked")
        List<Object> snapshotState = (List<Object>) MockRaftActor.toObject(applySnapshot.getSnapshot().getState());
        assertEquals("Snapshot state", snapshotState, leaderRaftActor.getState());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().get());

        // Verify ServerConfigurationPayload entry in leader's log

        expectFirstMatching(leaderCollectorActor, ApplyState.class);
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

        LOG.info("testAddServerWithNoExistingFollower ending");
    }

    @Test
    public void testAddServersAsNonVoting() throws Exception {
        LOG.info("testAddServersAsNonVoting starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        TestActorRef<MessageCollectorActor> leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), false), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().get());

        // Verify ServerConfigurationPayload entry in leader's log

        expectFirstMatching(leaderCollectorActor, ApplyState.class);

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

        assertNoneMatching(newFollowerCollectorActor, InstallSnapshot.class, 500);

        // Add another non-voting server.

        clearMessages(leaderCollectorActor);

        RaftActorContext follower2ActorContext = newFollowerContext(NEW_SERVER_ID2, followerActor);
        Follower newFollower2 = new Follower(follower2ActorContext);
        followerActor.underlyingActor().setBehavior(newFollower2);

        leaderActor.tell(new AddServer(NEW_SERVER_ID2, followerActor.path().toString(), false), testKit.getRef());

        addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", java.util.Optional.of(LEADER_ID), addServerReply.getLeaderHint());

        expectFirstMatching(leaderCollectorActor, ApplyState.class);
        assertEquals("Leader journal last index", 1, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 1, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 1, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(),
                votingServer(LEADER_ID), nonVotingServer(NEW_SERVER_ID), nonVotingServer(NEW_SERVER_ID2));

        LOG.info("testAddServersAsNonVoting ending");
    }

    @Test
    public void testAddServerWithOperationInProgress() throws Exception {
        LOG.info("testAddServerWithOperationInProgress starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        TestActorRef<MessageCollectorActor> leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        RaftActorContext follower2ActorContext = newFollowerContext(NEW_SERVER_ID2, followerActor);
        Follower newFollower2 = new Follower(follower2ActorContext);
        followerActor.underlyingActor().setBehavior(newFollower2);

        MockNewFollowerRaftActor newFollowerRaftActorInstance = newFollowerRaftActor.underlyingActor();
        newFollowerRaftActorInstance.setDropMessageOfType(InstallSnapshot.SERIALIZABLE_CLASS);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Wait for leader's install snapshot and capture it

        InstallSnapshot installSnapshot = expectFirstMatching(newFollowerCollectorActor, InstallSnapshot.class);

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

        expectMatching(leaderCollectorActor, ApplyState.class, 2);
        assertEquals("Leader journal last index", 1, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 1, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 1, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(),
                votingServer(LEADER_ID), votingServer(NEW_SERVER_ID), nonVotingServer(NEW_SERVER_ID2));

        // Verify ServerConfigurationPayload entry in the new follower

        expectMatching(newFollowerCollectorActor, ApplyState.class, 2);
        assertEquals("New follower peers", Sets.newHashSet(LEADER_ID, NEW_SERVER_ID2),
               newFollowerActorContext.getPeerIds());

        LOG.info("testAddServerWithOperationInProgress ending");
    }

    @Test
    public void testAddServerWithPriorSnapshotInProgress() throws Exception {
        LOG.info("testAddServerWithPriorSnapshotInProgress starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        TestActorRef<MessageCollectorActor> leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        // Drop commit message for now to delay snapshot completion
        leaderRaftActor.setDropMessageOfType(String.class);

        leaderActor.tell(new InitiateCaptureSnapshot(), leaderActor);

        String commitMsg = expectFirstMatching(leaderCollectorActor, String.class);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        leaderRaftActor.setDropMessageOfType(null);
        leaderActor.tell(commitMsg, leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().get());

        expectFirstMatching(newFollowerCollectorActor, ApplySnapshot.class);

        // Verify ServerConfigurationPayload entry in leader's log

        expectFirstMatching(leaderCollectorActor, ApplyState.class);
        assertEquals("Leader journal last index", 0, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 0, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 0, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(NEW_SERVER_ID));

        LOG.info("testAddServerWithPriorSnapshotInProgress ending");
    }

    @Test
    public void testAddServerWithPriorSnapshotCompleteTimeout() throws Exception {
        LOG.info("testAddServerWithPriorSnapshotCompleteTimeout starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(1);

        // Drop commit message so the snapshot doesn't complete.
        leaderRaftActor.setDropMessageOfType(String.class);

        leaderActor.tell(new InitiateCaptureSnapshot(), leaderActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.TIMEOUT, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());

        LOG.info("testAddServerWithPriorSnapshotCompleteTimeout ending");
    }

    @Test
    public void testAddServerWithLeaderChangeBeforePriorSnapshotComplete() throws Exception {
        LOG.info("testAddServerWithLeaderChangeBeforePriorSnapshotComplete starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(100);

        TestActorRef<MessageCollectorActor> leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

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

        leaderActor.tell(new RaftActorServerConfigurationSupport.ServerOperationTimeout(NEW_SERVER_ID), leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());
        assertEquals("isCapturing", false, leaderActorContext.getSnapshotManager().isCapturing());

        LOG.info("testAddServerWithLeaderChangeBeforePriorSnapshotComplete ending");
    }

    @Test
    public void testAddServerWithLeaderChangeDuringInstallSnapshot() throws Exception {
        LOG.info("testAddServerWithLeaderChangeDuringInstallSnapshot starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(8);

        TestActorRef<MessageCollectorActor> leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

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

        LOG.info("testAddServerWithLeaderChangeDuringInstallSnapshot ending");
    }

    @Test
    public void testAddServerWithInstallSnapshotTimeout() throws Exception {
        LOG.info("testAddServerWithInstallSnapshotTimeout starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

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

        LOG.info("testAddServerWithInstallSnapshotTimeout ending");
    }

    @Test
    public void testAddServerWithNoLeader() {
        LOG.info("testAddServerWithNoLeader starting");

        setupNewFollower();
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MockRaftActor> noLeaderActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(LEADER_ID).peerAddresses(ImmutableMap.of(FOLLOWER_ID,
                        followerActor.path().toString())).config(configParams).persistent(Optional.of(false)).
                        props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));
        noLeaderActor.underlyingActor().waitForInitializeBehaviorComplete();

        noLeaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());
        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());

        LOG.info("testAddServerWithNoLeader ending");
    }

    @Test
    public void testAddServerWithNoConsensusReached() {
        LOG.info("testAddServerWithNoConsensusReached starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.<String, String>of(),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        TestActorRef<MessageCollectorActor> leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        // Drop UnInitializedFollowerSnapshotReply initially
        leaderRaftActor.setDropMessageOfType(UnInitializedFollowerSnapshotReply.class);

        MockNewFollowerRaftActor newFollowerRaftActorInstance = newFollowerRaftActor.underlyingActor();
        TestActorRef<MessageCollectorActor> newFollowerCollectorActor =
                newCollectorActor(newFollowerRaftActorInstance, NEW_SERVER_ID);

        // Drop AppendEntries to the new follower so consensus isn't reached
        newFollowerRaftActorInstance.setDropMessageOfType(AppendEntries.class);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Capture the UnInitializedFollowerSnapshotReply
        Object snapshotReply = expectFirstMatching(leaderCollectorActor, UnInitializedFollowerSnapshotReply.class);

        // Send the UnInitializedFollowerSnapshotReply to resume the first request
        leaderRaftActor.setDropMessageOfType(null);
        leaderActor.tell(snapshotReply, leaderActor);

        expectFirstMatching(newFollowerCollectorActor, AppendEntries.class);

        // Send a second AddServer
        leaderActor.tell(new AddServer(NEW_SERVER_ID2, "", false), testKit.getRef());

        // The first AddServer should succeed with OK even though consensus wasn't reached
        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().get());

        // Verify ServerConfigurationPayload entry in leader's log
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(NEW_SERVER_ID));

        // The second AddServer should fail since consensus wasn't reached for the first
        addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.PRIOR_REQUEST_CONSENSUS_TIMEOUT, addServerReply.getStatus());

        // Re-send the second AddServer - should also fail
        leaderActor.tell(new AddServer(NEW_SERVER_ID2, "", false), testKit.getRef());
        addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.PRIOR_REQUEST_CONSENSUS_TIMEOUT, addServerReply.getStatus());

        LOG.info("testAddServerWithNoConsensusReached ending");
    }

    @Test
    public void testAddServerWithExistingServer() {
        LOG.info("testAddServerWithExistingServer starting");

        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, followerActor.path().toString()),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        leaderActor.tell(new AddServer(FOLLOWER_ID, followerActor.path().toString(), true), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.ALREADY_EXISTS, addServerReply.getStatus());

        LOG.info("testAddServerWithExistingServer ending");
    }

    @Test
    public void testAddServerForwardedToLeader() {
        LOG.info("testAddServerForwardedToLeader starting");

        setupNewFollower();
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MessageCollectorActor> leaderActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        TestActorRef<MockRaftActor> followerRaftActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(FOLLOWER_ID).peerAddresses(ImmutableMap.of(LEADER_ID,
                        leaderActor.path().toString())).config(configParams).persistent(Optional.of(false)).
                        props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(FOLLOWER_ID));
        followerRaftActor.underlyingActor().waitForInitializeBehaviorComplete();

        followerRaftActor.tell(new AppendEntries(1, LEADER_ID, 0, 1, Collections.<ReplicatedLogEntry>emptyList(),
                -1, -1, (short)0), leaderActor);

        followerRaftActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());
        expectFirstMatching(leaderActor, AddServer.class);

        LOG.info("testAddServerForwardedToLeader ending");
    }

    @Test
    public void testOnApplyState() {
        LOG.info("testOnApplyState starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        TestActorRef<MockRaftActor> noLeaderActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(LEADER_ID).peerAddresses(ImmutableMap.of(FOLLOWER_ID,
                        followerActor.path().toString())).config(configParams).persistent(Optional.of(false)).
                        props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        RaftActorServerConfigurationSupport support = new RaftActorServerConfigurationSupport(noLeaderActor.underlyingActor());

        ReplicatedLogEntry serverConfigEntry = new MockRaftActorContext.MockReplicatedLogEntry(1, 1,
                new ServerConfigurationPayload(Collections.<ServerInfo>emptyList()));
        boolean handled = support.handleMessage(new ApplyState(null, null, serverConfigEntry), ActorRef.noSender());
        assertEquals("Message handled", true, handled);

        ReplicatedLogEntry nonServerConfigEntry = new MockRaftActorContext.MockReplicatedLogEntry(1, 1,
                new MockRaftActorContext.MockPayload("1"));
        handled = support.handleMessage(new ApplyState(null, null, nonServerConfigEntry), ActorRef.noSender());
        assertEquals("Message handled", false, handled);

        LOG.info("testOnApplyState ending");
    }

    @Test
    public void testRemoveServerWithNoLeader() {
        LOG.info("testRemoveServerWithNoLeader starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MockRaftActor> leaderActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(LEADER_ID).peerAddresses(ImmutableMap.of(FOLLOWER_ID,
                        followerActor.path().toString())).config(configParams).persistent(Optional.of(false)).
                        props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));
        leaderActor.underlyingActor().waitForInitializeBehaviorComplete();

        leaderActor.tell(new RemoveServer(FOLLOWER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, removeServerReply.getStatus());

        LOG.info("testRemoveServerWithNoLeader ending");
    }

    @Test
    public void testRemoveServerNonExistentServer() {
        LOG.info("testRemoveServerNonExistentServer starting");

        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, followerActor.path().toString()),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        leaderActor.tell(new RemoveServer(NEW_SERVER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.DOES_NOT_EXIST, removeServerReply.getStatus());

        LOG.info("testRemoveServerNonExistentServer ending");
    }

    @Test
    public void testRemoveServerForwardToLeader() {
        LOG.info("testRemoveServerForwardToLeader starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MessageCollectorActor> leaderActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        TestActorRef<MockRaftActor> followerRaftActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(FOLLOWER_ID).peerAddresses(ImmutableMap.of(LEADER_ID,
                        leaderActor.path().toString())).config(configParams).persistent(Optional.of(false)).
                        props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(FOLLOWER_ID));
        followerRaftActor.underlyingActor().waitForInitializeBehaviorComplete();

        followerRaftActor.tell(new AppendEntries(1, LEADER_ID, 0, 1, Collections.<ReplicatedLogEntry>emptyList(),
                -1, -1, (short)0), leaderActor);

        followerRaftActor.tell(new RemoveServer(FOLLOWER_ID), testKit.getRef());
        expectFirstMatching(leaderActor, RemoveServer.class);

        LOG.info("testRemoveServerForwardToLeader ending");
    }

    @Test
    public void testRemoveServer() {
        LOG.info("testRemoveServer starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        configParams.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final String followerActorId = actorFactory.generateActorId(FOLLOWER_ID);
        final String followerActorPath = actorFactory.createTestActorPath(followerActorId);
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, followerActorPath),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        TestActorRef<MessageCollectorActor> leaderCollector = newLeaderCollectorActor(leaderActor.underlyingActor());

        TestActorRef<MessageCollectorActor> collector =
                actorFactory.createTestActor(MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> followerRaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(FOLLOWER_ID, ImmutableMap.of(LEADER_ID, leaderActor.path().toString()),
                        configParams, NO_PERSISTENCE, collector).withDispatcher(Dispatchers.DefaultDispatcherId()),
                followerActorId);

        leaderActor.tell(new RemoveServer(FOLLOWER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, removeServerReply.getStatus());

        final ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollector, ApplyState.class);
        assertEquals(0L, applyState.getReplicatedLogEntry().getIndex());
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(), votingServer(LEADER_ID));

        RaftActorBehavior currentBehavior = leaderActor.underlyingActor().getCurrentBehavior();
        assertTrue("Expected Leader", currentBehavior instanceof Leader);
        assertEquals("Follower ids size", 0, ((Leader)currentBehavior).getFollowerIds().size());

        MessageCollectorActor.expectFirstMatching(collector, ServerRemoved.class);

        LOG.info("testRemoveServer ending");
    }

    @Test
    public void testRemoveServerLeader() {
        LOG.info("testRemoveServerLeader starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        configParams.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final String followerActorId = actorFactory.generateActorId(FOLLOWER_ID);
        final String followerActorPath = actorFactory.createTestActorPath(followerActorId);
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, followerActorPath),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        TestActorRef<MessageCollectorActor> leaderCollector = newLeaderCollectorActor(leaderActor.underlyingActor());

        TestActorRef<MessageCollectorActor> followerCollector = actorFactory.createTestActor(MessageCollectorActor.props().
                withDispatcher(Dispatchers.DefaultDispatcherId()), actorFactory.generateActorId("collector"));
        actorFactory.createTestActor(
                CollectingMockRaftActor.props(FOLLOWER_ID, ImmutableMap.of(LEADER_ID, leaderActor.path().toString()),
                        configParams, NO_PERSISTENCE, followerCollector).withDispatcher(Dispatchers.DefaultDispatcherId()),
                followerActorId);

        leaderActor.tell(new RemoveServer(LEADER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, removeServerReply.getStatus());

        final ApplyState applyState = MessageCollectorActor.expectFirstMatching(followerCollector, ApplyState.class);
        assertEquals(0L, applyState.getReplicatedLogEntry().getIndex());
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(FOLLOWER_ID));

        MessageCollectorActor.expectFirstMatching(leaderCollector, ServerRemoved.class);

        LOG.info("testRemoveServerLeader ending");
    }

    @Test
    public void testRemoveServerLeaderWithNoFollowers() {
        LOG.info("testRemoveServerLeaderWithNoFollowers starting");

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(Collections.<String, String>emptyMap(),
                        new MockRaftActorContext()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        leaderActor.tell(new RemoveServer(LEADER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NOT_SUPPORTED, removeServerReply.getStatus());

        LOG.info("testRemoveServerLeaderWithNoFollowers ending");
    }

    @Test
    public void testChangeServersVotingStatus() {
        LOG.info("testChangeServersVotingStatus starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        configParams.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final String follower1ActorId = actorFactory.generateActorId(FOLLOWER_ID);
        final String follower1ActorPath = actorFactory.createTestActorPath(follower1ActorId);
        final String follower2ActorId = actorFactory.generateActorId(FOLLOWER_ID2);
        final String follower2ActorPath = actorFactory.createTestActorPath(follower2ActorId);

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, follower1ActorPath,
                        FOLLOWER_ID2, follower2ActorPath), new MockRaftActorContext()).
                        withDispatcher(Dispatchers.DefaultDispatcherId()), actorFactory.generateActorId(LEADER_ID));
        TestActorRef<MessageCollectorActor> leaderCollector = newLeaderCollectorActor(leaderActor.underlyingActor());

        TestActorRef<MessageCollectorActor> follower1Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> follower1RaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(FOLLOWER_ID, ImmutableMap.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID2, follower2ActorPath), configParams, NO_PERSISTENCE, follower1Collector).
                        withDispatcher(Dispatchers.DefaultDispatcherId()), follower1ActorId);

        TestActorRef<MessageCollectorActor> follower2Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> follower2RaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(FOLLOWER_ID2, ImmutableMap.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID, follower1ActorPath), configParams, NO_PERSISTENCE, follower2Collector).
                        withDispatcher(Dispatchers.DefaultDispatcherId()), follower2ActorId);

        // Send first ChangeServersVotingStatus message

        leaderActor.tell(new ChangeServersVotingStatus(ImmutableMap.of(FOLLOWER_ID, false, FOLLOWER_ID2, false)),
                testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        final ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollector, ApplyState.class);
        assertEquals(0L, applyState.getReplicatedLogEntry().getIndex());
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), nonVotingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower1Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower1RaftActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), nonVotingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower2Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower2RaftActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), nonVotingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.clearMessages(leaderCollector);
        MessageCollectorActor.clearMessages(follower1Collector);
        MessageCollectorActor.clearMessages(follower2Collector);

        // Send second ChangeServersVotingStatus message

        leaderActor.tell(new ChangeServersVotingStatus(ImmutableMap.of(FOLLOWER_ID, true)), testKit.getRef());
        reply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        MessageCollectorActor.expectFirstMatching(leaderCollector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), votingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower1Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower1RaftActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), votingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower2Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower2RaftActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), votingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        LOG.info("testChangeServersVotingStatus ending");
    }

    @Test
    public void testChangeLeaderToNonVoting() {
        LOG.info("testChangeLeaderToNonVoting starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(500, TimeUnit.MILLISECONDS));

        final String follower1ActorId = actorFactory.generateActorId(FOLLOWER_ID);
        final String follower1ActorPath = actorFactory.createTestActorPath(follower1ActorId);
        final String follower2ActorId = actorFactory.generateActorId(FOLLOWER_ID2);
        final String follower2ActorPath = actorFactory.createTestActorPath(follower2ActorId);

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(ImmutableMap.of(FOLLOWER_ID, follower1ActorPath,
                        FOLLOWER_ID2, follower2ActorPath), new MockRaftActorContext()).
                        withDispatcher(Dispatchers.DefaultDispatcherId()), actorFactory.generateActorId(LEADER_ID));
        TestActorRef<MessageCollectorActor> leaderCollector = newLeaderCollectorActor(leaderActor.underlyingActor());

        TestActorRef<MessageCollectorActor> follower1Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> follower1RaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(FOLLOWER_ID, ImmutableMap.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID2, follower2ActorPath), configParams, NO_PERSISTENCE, follower1Collector).
                        withDispatcher(Dispatchers.DefaultDispatcherId()), follower1ActorId);

        TestActorRef<MessageCollectorActor> follower2Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> follower2RaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(FOLLOWER_ID2, ImmutableMap.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID, follower1ActorPath), configParams, NO_PERSISTENCE, follower2Collector).
                        withDispatcher(Dispatchers.DefaultDispatcherId()), follower2ActorId);

        // Send ChangeServersVotingStatus message

        leaderActor.tell(new ChangeServersVotingStatus(ImmutableMap.of(LEADER_ID, false)), testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        MessageCollectorActor.expectFirstMatching(leaderCollector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                nonVotingServer(LEADER_ID), votingServer(FOLLOWER_ID), votingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower1Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower1RaftActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                nonVotingServer(LEADER_ID), votingServer(FOLLOWER_ID), votingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower2Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower2RaftActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                nonVotingServer(LEADER_ID), votingServer(FOLLOWER_ID), votingServer(FOLLOWER_ID2));

        verifyRaftState(RaftState.Leader, follower1RaftActor.underlyingActor(), follower2RaftActor.underlyingActor());
        verifyRaftState(RaftState.Follower, leaderActor.underlyingActor());

        MessageCollectorActor.expectMatching(leaderCollector, AppendEntries.class, 2);

        LOG.info("testChangeLeaderToNonVoting ending");
    }

    @Test
    public void testChangeToVotingWithNoLeader() {
        LOG.info("testChangeToVotingWithNoLeader starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));

        final String node1ID = "node1";
        final String node2ID = "node2";

        // Set up a persisted ServerConfigurationPayload. Initially node1 and node2 will come up as non-voting.
        // via the server config. The server config will also contain 2 voting peers that are down (ie no
        // actors created).

        ServerConfigurationPayload persistedServerConfig = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(node1ID, false), new ServerInfo(node2ID, false),
                new ServerInfo("downNode1", true), new ServerInfo("downNode2", true)));
        ReplicatedLogImplEntry persistedServerConfigEntry = new ReplicatedLogImplEntry(0, 1, persistedServerConfig);

        InMemoryJournal.addEntry(node1ID, 1, new UpdateElectionTerm(1, "downNode1"));
        InMemoryJournal.addEntry(node1ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 1, new UpdateElectionTerm(1, "downNode2"));
        InMemoryJournal.addEntry(node2ID, 2, persistedServerConfigEntry);

        TestActorRef<MessageCollectorActor> node1Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node1RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(node1ID, ImmutableMap.<String, String>of(), configParams,
                        PERSISTENT, node1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node1ID);
        CollectingMockRaftActor node1RaftActor = node1RaftActorRef.underlyingActor();

        TestActorRef<MessageCollectorActor> node2Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node2RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(node2ID, ImmutableMap.<String, String>of(), configParams,
                        PERSISTENT, node2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node2ID);
        CollectingMockRaftActor node2RaftActor = node2RaftActorRef.underlyingActor();

        // Wait for snapshot after recovery
        MessageCollectorActor.expectFirstMatching(node1Collector, SnapshotComplete.class);

        // Verify the intended server config was loaded and applied.
        verifyServerConfigurationPayloadEntry(node1RaftActor.getRaftActorContext().getReplicatedLog(),
                nonVotingServer(node1ID), nonVotingServer(node2ID), votingServer("downNode1"),
                votingServer("downNode2"));
        assertEquals("isVotingMember", false, node1RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Follower, node1RaftActor.getRaftState());
        assertEquals("getLeaderId", null, node1RaftActor.getLeaderId());

        MessageCollectorActor.expectFirstMatching(node2Collector, SnapshotComplete.class);
        assertEquals("isVotingMember", false, node2RaftActor.getRaftActorContext().isVotingMember());

        // For the test, we send a ChangeServersVotingStatus message to node1 to flip the voting states for
        // each server, ie node1 and node2 to voting and the 2 down nodes to non-voting. This should cause
        // node1 to try to elect itself as leader in order to apply the new server config. Since the 2
        // down nodes are switched to non-voting, node1 should only need a vote from node2.

        // First send the message such that node1 has no peer address for node2 - should fail.

        ChangeServersVotingStatus changeServers = new ChangeServersVotingStatus(ImmutableMap.of(node1ID, true,
                node2ID, true, "downNode1", false, "downNode2", false));
        node1RaftActorRef.tell(changeServers, testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, reply.getStatus());

        // Update node2's peer address and send the message again

        node1RaftActor.setPeerAddress(node2ID, node2RaftActorRef.path().toString());

        node1RaftActorRef.tell(changeServers, testKit.getRef());
        reply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        ApplyJournalEntries apply = MessageCollectorActor.expectFirstMatching(node1Collector, ApplyJournalEntries.class);
        assertEquals("getToIndex", 1, apply.getToIndex());
        verifyServerConfigurationPayloadEntry(node1RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID), nonVotingServer("downNode1"),
                nonVotingServer("downNode2"));
        assertEquals("isVotingMember", true, node1RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Leader, node1RaftActor.getRaftState());

        apply = MessageCollectorActor.expectFirstMatching(node2Collector, ApplyJournalEntries.class);
        assertEquals("getToIndex", 1, apply.getToIndex());
        verifyServerConfigurationPayloadEntry(node2RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID), nonVotingServer("downNode1"),
                nonVotingServer("downNode2"));
        assertEquals("isVotingMember", true, node2RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Follower, node2RaftActor.getRaftState());

        LOG.info("testChangeToVotingWithNoLeader ending");
    }

    @Test
    public void testChangeToVotingWithNoLeaderAndElectionTimeout() {
        LOG.info("testChangeToVotingWithNoLeaderAndElectionTimeout starting");

        final String node1ID = "node1";
        final String node2ID = "node2";

        PeerAddressResolver peerAddressResolver = new PeerAddressResolver() {
            @Override
            public String resolve(String peerId) {
                return peerId.equals(node1ID) ? actorFactory.createTestActorPath(node1ID) :
                    peerId.equals(node2ID) ? actorFactory.createTestActorPath(node2ID) : null;
            }
        };

        ServerConfigurationPayload persistedServerConfig = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(node1ID, false), new ServerInfo(node2ID, true)));
        ReplicatedLogImplEntry persistedServerConfigEntry = new ReplicatedLogImplEntry(0, 1, persistedServerConfig);

        InMemoryJournal.addEntry(node1ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node1ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node2ID, 2, persistedServerConfigEntry);

        DefaultConfigParamsImpl configParams1 = new DefaultConfigParamsImpl();
        configParams1.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams1.setElectionTimeoutFactor(1);
        configParams1.setPeerAddressResolver(peerAddressResolver);
        TestActorRef<MessageCollectorActor> node1Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node1RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(node1ID, ImmutableMap.<String, String>of(), configParams1,
                        PERSISTENT, node1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node1ID);
        CollectingMockRaftActor node1RaftActor = node1RaftActorRef.underlyingActor();

        DefaultConfigParamsImpl configParams2 = new DefaultConfigParamsImpl();
        configParams2.setElectionTimeoutFactor(1000000);
        configParams2.setPeerAddressResolver(peerAddressResolver);
        TestActorRef<MessageCollectorActor> node2Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node2RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(node2ID, ImmutableMap.<String, String>of(), configParams2,
                        PERSISTENT, node2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node2ID);
        CollectingMockRaftActor node2RaftActor = node2RaftActorRef.underlyingActor();

        // Wait for snapshot after recovery
        MessageCollectorActor.expectFirstMatching(node1Collector, SnapshotComplete.class);

        // Send a ChangeServersVotingStatus message to node1 to change mode1 to voting. This should cause
        // node1 to try to elect itself as leader in order to apply the new server config. But we'll drop
        // RequestVote messages in node2 which should cause node1 to time out and revert back to the previous
        // server config and fail with NO_LEADER. Note that node1 shouldn't forward the request to node2 b/c
        // node2 was previously voting.

        node2RaftActor.setDropMessageOfType(RequestVote.class);

        ChangeServersVotingStatus changeServers = new ChangeServersVotingStatus(ImmutableMap.of(node1ID, true));
        node1RaftActorRef.tell(changeServers, testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, reply.getStatus());

        assertEquals("Server config", Sets.newHashSet(nonVotingServer(node1ID), votingServer(node2ID)),
                Sets.newHashSet(node1RaftActor.getRaftActorContext().getPeerServerInfo(true).getServerConfig()));
        assertEquals("getRaftState", RaftState.Follower, node1RaftActor.getRaftState());

        LOG.info("testChangeToVotingWithNoLeaderAndElectionTimeout ending");
    }

    @Test
    public void testChangeToVotingWithNoLeaderAndForwardedToOtherNodeAfterElectionTimeout() {
        LOG.info("testChangeToVotingWithNoLeaderAndForwardedToOtherNodeAfterElectionTimeout starting");

        final String node1ID = "node1";
        final String node2ID = "node2";

        PeerAddressResolver peerAddressResolver = new PeerAddressResolver() {
            @Override
            public String resolve(String peerId) {
                return peerId.equals(node1ID) ? actorFactory.createTestActorPath(node1ID) :
                    peerId.equals(node2ID) ? actorFactory.createTestActorPath(node2ID) : null;
            }
        };

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(3);
        configParams.setPeerAddressResolver(peerAddressResolver);

        ServerConfigurationPayload persistedServerConfig = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(node1ID, false), new ServerInfo(node2ID, false)));
        ReplicatedLogImplEntry persistedServerConfigEntry = new ReplicatedLogImplEntry(0, 1, persistedServerConfig);

        InMemoryJournal.addEntry(node1ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node1ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node2ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 3, new ReplicatedLogImplEntry(1, 1,
                new MockRaftActorContext.MockPayload("2")));

        TestActorRef<MessageCollectorActor> node1Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node1RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(node1ID, ImmutableMap.<String, String>of(), configParams,
                        PERSISTENT, node1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node1ID);
        CollectingMockRaftActor node1RaftActor = node1RaftActorRef.underlyingActor();

        TestActorRef<MessageCollectorActor> node2Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node2RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(node2ID, ImmutableMap.<String, String>of(), configParams,
                        PERSISTENT, node2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node2ID);
        CollectingMockRaftActor node2RaftActor = node2RaftActorRef.underlyingActor();

        // Wait for snapshot after recovery
        MessageCollectorActor.expectFirstMatching(node1Collector, SnapshotComplete.class);

        // Send a ChangeServersVotingStatus message to node1 to change mode1 to voting. This should cause
        // node1 to try to elect itself as leader in order to apply the new server config. However node1's log
        // is behind node2's so node2 should not grant node1's vote. This should cause node1 to time out and
        // forward the request to node2.

        ChangeServersVotingStatus changeServers = new ChangeServersVotingStatus(
                ImmutableMap.of(node1ID, true, node2ID, true));
        node1RaftActorRef.tell(changeServers, testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        MessageCollectorActor.expectFirstMatching(node2Collector, ApplyJournalEntries.class);
        verifyServerConfigurationPayloadEntry(node2RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID));
        assertEquals("getRaftState", RaftState.Leader, node2RaftActor.getRaftState());

        MessageCollectorActor.expectFirstMatching(node1Collector, ApplyJournalEntries.class);
        verifyServerConfigurationPayloadEntry(node1RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID));
        assertEquals("isVotingMember", true, node1RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Follower, node1RaftActor.getRaftState());

        LOG.info("testChangeToVotingWithNoLeaderAndForwardedToOtherNodeAfterElectionTimeout ending");
    }

    @Test
    public void testChangeToVotingWithNoLeaderAndOtherLeaderElected() {
        LOG.info("testChangeToVotingWithNoLeaderAndOtherLeaderElected starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);

        final String node1ID = "node1";
        final String node2ID = "node2";

        configParams.setPeerAddressResolver(new PeerAddressResolver() {
            @Override
            public String resolve(String peerId) {
                return peerId.equals(node1ID) ? actorFactory.createTestActorPath(node1ID) :
                    peerId.equals(node2ID) ? actorFactory.createTestActorPath(node2ID) : null;
            }
        });

        ServerConfigurationPayload persistedServerConfig = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(node1ID, false), new ServerInfo(node2ID, true)));
        ReplicatedLogImplEntry persistedServerConfigEntry = new ReplicatedLogImplEntry(0, 1, persistedServerConfig);

        InMemoryJournal.addEntry(node1ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node1ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node2ID, 2, persistedServerConfigEntry);

        TestActorRef<MessageCollectorActor> node1Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node1RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(node1ID, ImmutableMap.<String, String>of(), configParams,
                        PERSISTENT, node1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node1ID);
        CollectingMockRaftActor node1RaftActor = node1RaftActorRef.underlyingActor();

        TestActorRef<MessageCollectorActor> node2Collector = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node2RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(node2ID, ImmutableMap.<String, String>of(), configParams,
                        PERSISTENT, node2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node2ID);
        CollectingMockRaftActor node2RaftActor = node2RaftActorRef.underlyingActor();

        // Wait for snapshot after recovery
        MessageCollectorActor.expectFirstMatching(node1Collector, SnapshotComplete.class);

        // Send a ChangeServersVotingStatus message to node1 to change node1 to voting. This should cause
        // node1 to try to elect itself as leader in order to apply the new server config. But we'll drop
        // RequestVote messages in node2 and make it the leader so node1 should forward the server change
        // request to node2 when node2 is elected.

        node2RaftActor.setDropMessageOfType(RequestVote.class);

        ChangeServersVotingStatus changeServers = new ChangeServersVotingStatus(ImmutableMap.of(node1ID, true,
                node2ID, true));
        node1RaftActorRef.tell(changeServers, testKit.getRef());

        MessageCollectorActor.expectFirstMatching(node2Collector, RequestVote.class);

        node2RaftActorRef.tell(ElectionTimeout.INSTANCE, ActorRef.noSender());

        ServerChangeReply reply = testKit.expectMsgClass(JavaTestKit.duration("5 seconds"), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        MessageCollectorActor.expectFirstMatching(node1Collector, ApplyJournalEntries.class);
        verifyServerConfigurationPayloadEntry(node1RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID));
        assertEquals("isVotingMember", true, node1RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Follower, node1RaftActor.getRaftState());

        MessageCollectorActor.expectFirstMatching(node2Collector, ApplyJournalEntries.class);
        verifyServerConfigurationPayloadEntry(node2RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID));
        assertEquals("getRaftState", RaftState.Leader, node2RaftActor.getRaftState());

        LOG.info("testChangeToVotingWithNoLeaderAndOtherLeaderElected ending");
    }

    private void verifyRaftState(RaftState expState, RaftActor... raftActors) {
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 5) {
            for(RaftActor raftActor: raftActors) {
                if(raftActor.getRaftState() == expState) {
                    return;
                }
            }
        }

        fail("None of the RaftActors have state " + expState);
    }

    private static ServerInfo votingServer(String id) {
        return new ServerInfo(id, true);
    }

    private static ServerInfo nonVotingServer(String id) {
        return new ServerInfo(id, false);
    }

    private TestActorRef<MessageCollectorActor> newLeaderCollectorActor(MockLeaderRaftActor leaderRaftActor) {
        return newCollectorActor(leaderRaftActor, LEADER_ID);
    }

    private TestActorRef<MessageCollectorActor> newCollectorActor(AbstractMockRaftActor raftActor, String id) {
        TestActorRef<MessageCollectorActor> collectorActor = actorFactory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(id + "Collector"));
        raftActor.setCollectorActor(collectorActor);
        return collectorActor;
    }

    private static void verifyServerConfigurationPayloadEntry(ReplicatedLog log, ServerInfo... expected) {
        ReplicatedLogEntry logEntry = log.get(log.lastIndex());
        assertEquals("Last log entry payload class", ServerConfigurationPayload.class, logEntry.getData().getClass());
        ServerConfigurationPayload payload = (ServerConfigurationPayload)logEntry.getData();
        assertEquals("Server config", Sets.newHashSet(expected), Sets.newHashSet(payload.getServerConfig()));
    }

    private static RaftActorContextImpl newFollowerContext(String id, TestActorRef<? extends UntypedActor> actor) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        NonPersistentDataProvider noPersistence = new NonPersistentDataProvider();
        ElectionTermImpl termInfo = new ElectionTermImpl(noPersistence, id, LOG);
        termInfo.update(1, LEADER_ID);
        return new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
                id, termInfo, -1, -1, ImmutableMap.of(LEADER_ID, ""), configParams, noPersistence, LOG);
    }

    static abstract class AbstractMockRaftActor extends MockRaftActor {
        private volatile TestActorRef<MessageCollectorActor> collectorActor;
        private volatile Class<?> dropMessageOfType;

        AbstractMockRaftActor(String id, Map<String, String> peerAddresses, Optional<ConfigParams> config,
                boolean persistent, TestActorRef<MessageCollectorActor> collectorActor) {
            super(builder().id(id).peerAddresses(peerAddresses).config(config.get()).
                    persistent(Optional.of(persistent)));
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

    public static class CollectingMockRaftActor extends AbstractMockRaftActor {

        CollectingMockRaftActor(String id, Map<String, String> peerAddresses, Optional<ConfigParams> config,
                boolean persistent, TestActorRef<MessageCollectorActor> collectorActor) {
            super(id, peerAddresses, config, persistent, collectorActor);
            snapshotCohortDelegate = new RaftActorSnapshotCohort() {
                @Override
                public void createSnapshot(ActorRef actorRef) {
                    actorRef.tell(new CaptureSnapshotReply(new byte[0]), actorRef);
                }

                @Override
                public void applySnapshot(byte[] snapshotBytes) {
                }
            };
        }

        public static Props props(final String id, final Map<String, String> peerAddresses,
                ConfigParams config, boolean persistent, TestActorRef<MessageCollectorActor> collectorActor){

            return Props.create(CollectingMockRaftActor.class, id, peerAddresses, Optional.of(config),
                    persistent, collectorActor);
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
            super(NEW_SERVER_ID, Maps.<String, String>newHashMap(), Optional.of(config), NO_PERSISTENCE, collectorActor);
            setPersistence(false);
        }

        static Props props(ConfigParams config, TestActorRef<MessageCollectorActor> collectorActor) {
            return Props.create(MockNewFollowerRaftActor.class, config, collectorActor);
        }
    }
}
