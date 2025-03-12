/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.assertNoneMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectFirstMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectMatching;

import com.google.common.base.Stopwatch;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActor.MockSnapshotState;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
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
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.FailingTermInfoStore;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;
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
    private static final String LEADER_ID = "leader";
    private static final String FOLLOWER_ID = "follower";
    private static final String FOLLOWER_ID2 = "follower2";
    private static final String NEW_SERVER_ID = "new-server";
    private static final String NEW_SERVER_ID2 = "new-server2";
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorServerConfigurationSupportTest.class);
    private static final Class<?> COMMIT_MESSAGE_CLASS = RaftActorSnapshotMessageSupport.CommitSnapshot.class;
    private static final boolean NO_PERSISTENCE = false;
    private static final boolean PERSISTENT = true;

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private final TestActorRef<ForwardMessageToBehaviorActor> followerActor = actorFactory.createTestActor(
            Props.create(ForwardMessageToBehaviorActor.class).withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId(FOLLOWER_ID));

    private TestActorRef<MockNewFollowerRaftActor> newFollowerRaftActor;
    private ActorRef newFollowerCollectorActor;
    private RaftActorContext newFollowerActorContext;

    private final TestKit testKit = new TestKit(getSystem());

    @Before
    public void setup() throws Exception {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void setupNewFollower() {
        DefaultConfigParamsImpl configParams = newFollowerConfigParams();

        newFollowerCollectorActor = actorFactory.createActor(MessageCollectorActor.props(),
                actorFactory.generateActorId(NEW_SERVER_ID + "Collector"));
        newFollowerRaftActor = actorFactory.createTestActor(MockNewFollowerRaftActor.props(stateDir(),
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
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testAddServerWithExistingFollower() {
        LOG.info("testAddServerWithExistingFollower starting");
        setupNewFollower();
        RaftActorContextImpl followerActorContext = newFollowerContext(FOLLOWER_ID, followerActor);
        followerActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1)
            .build());
        followerActorContext.setCommitIndex(2);
        followerActorContext.setLastApplied(2);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);
        followerActorContext.setCurrentBehavior(follower);

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(FOLLOWER_ID, followerActor.path().toString()),
                        followerActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        // Expect initial heartbeat from the leader.
        expectFirstMatching(followerActor, AppendEntries.class);
        clearMessages(followerActor);

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        final ActorRef leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Leader should install snapshot - capture and verify ApplySnapshot contents

        final var applySnapshot = expectFirstMatching(newFollowerCollectorActor, ApplyLeaderSnapshot.class);
        assertEquals("leader", applySnapshot.leaderId());
        assertEquals(1, applySnapshot.term());
        assertEquals(ImmutableRaftEntryMeta.of(2, 1), applySnapshot.lastEntry());
        assertNull(applySnapshot.serverConfig());

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().orElseThrow());

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

        assertEquals("Follower peers", Set.of(LEADER_ID, NEW_SERVER_ID), followerActorContext.getPeerIds());

        assertEquals("New follower peers", Set.of(LEADER_ID, FOLLOWER_ID), newFollowerActorContext.getPeerIds());

        assertEquals("Follower commit index", 3, followerActorContext.getCommitIndex());
        assertEquals("Follower last applied index", 3, followerActorContext.getLastApplied());
        assertEquals("New follower commit index", 3, newFollowerActorContext.getCommitIndex());
        assertEquals("New follower last applied index", 3, newFollowerActorContext.getLastApplied());

        assertEquals("Leader persisted ReplicatedLogImplEntry entries", 0,
                InMemoryJournal.get(LEADER_ID, SimpleReplicatedLogEntry.class).size());
        assertEquals("Leader persisted ServerConfigurationPayload entries", 1,
                InMemoryJournal.get(LEADER_ID, ClusterConfig.class).size());

        assertEquals("New follower persisted ReplicatedLogImplEntry entries", 0,
                InMemoryJournal.get(NEW_SERVER_ID, SimpleReplicatedLogEntry.class).size());
        assertEquals("New follower persisted ServerConfigurationPayload entries", 1,
                InMemoryJournal.get(NEW_SERVER_ID, ClusterConfig.class).size());

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
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        final RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        final ActorRef leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Leader should install snapshot - capture and verify ApplySnapshot contents

        final var applySnapshot = expectFirstMatching(newFollowerCollectorActor, ApplyLeaderSnapshot.class);
        final MockSnapshotState state;
        try (var ois = new ObjectInputStream(applySnapshot.snapshot().openStream())) {
            state = (MockSnapshotState) ois.readObject();
        }

        List<Object> snapshotState = MockRaftActor.fromState(state);
        assertEquals("Snapshot state", snapshotState, leaderRaftActor.getState());

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().orElseThrow());

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

        assertEquals("New follower peers", Set.of(LEADER_ID), newFollowerActorContext.getPeerIds());

        LOG.info("testAddServerWithNoExistingFollower ending");
    }

    @Test
    public void testAddServersAsNonVoting() {
        LOG.info("testAddServersAsNonVoting starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        final RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        final ActorRef leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), false), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().orElseThrow());

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

        assertEquals("New follower peers", Set.of(LEADER_ID), newFollowerActorContext.getPeerIds());

        assertNoneMatching(newFollowerCollectorActor, InstallSnapshot.class, 500);

        // Add another non-voting server.

        clearMessages(leaderCollectorActor);

        RaftActorContext follower2ActorContext = newFollowerContext(NEW_SERVER_ID2, followerActor);
        Follower newFollower2 = new Follower(follower2ActorContext);
        followerActor.underlyingActor().setBehavior(newFollower2);

        leaderActor.tell(new AddServer(NEW_SERVER_ID2, followerActor.path().toString(), false), testKit.getRef());

        addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", Optional.of(LEADER_ID), addServerReply.getLeaderHint());

        expectFirstMatching(leaderCollectorActor, ApplyState.class);
        assertEquals("Leader journal last index", 1, leaderActorContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 1, leaderActorContext.getCommitIndex());
        assertEquals("Leader last applied index", 1, leaderActorContext.getLastApplied());
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(),
                votingServer(LEADER_ID), nonVotingServer(NEW_SERVER_ID), nonVotingServer(NEW_SERVER_ID2));

        LOG.info("testAddServersAsNonVoting ending");
    }

    @Test
    public void testAddServerWithOperationInProgress() {
        LOG.info("testAddServerWithOperationInProgress starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        final RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        final ActorRef leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        RaftActorContext follower2ActorContext = newFollowerContext(NEW_SERVER_ID2, followerActor);
        Follower newFollower2 = new Follower(follower2ActorContext);
        followerActor.underlyingActor().setBehavior(newFollower2);

        MockNewFollowerRaftActor newFollowerRaftActorInstance = newFollowerRaftActor.underlyingActor();
        newFollowerRaftActorInstance.setDropMessageOfType(InstallSnapshot.class);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        // Wait for leader's install snapshot and capture it

        InstallSnapshot installSnapshot = expectFirstMatching(newFollowerCollectorActor, InstallSnapshot.class);

        // Send a second AddServer - should get queued
        TestKit testKit2 = new TestKit(getSystem());
        leaderActor.tell(new AddServer(NEW_SERVER_ID2, followerActor.path().toString(), false), testKit2.getRef());

        // Continue the first AddServer
        newFollowerRaftActorInstance.setDropMessageOfType(null);
        newFollowerRaftActor.tell(installSnapshot, leaderActor);

        // Verify both complete successfully
        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());

        addServerReply = testKit2.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
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
        assertEquals("New follower peers", Set.of(LEADER_ID, NEW_SERVER_ID2), newFollowerActorContext.getPeerIds());

        LOG.info("testAddServerWithOperationInProgress ending");
    }

    @Test
    public void testAddServerWithPriorSnapshotInProgress() {
        LOG.info("testAddServerWithPriorSnapshotInProgress starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        final RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        ActorRef leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        // Drop commit message for now to delay snapshot completion
        leaderRaftActor.setDropMessageOfType(String.class);

        leaderActor.tell(new InitiateCaptureSnapshot(), leaderActor);

        Object commitMsg = expectFirstMatching(leaderCollectorActor, COMMIT_MESSAGE_CLASS);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        leaderRaftActor.setDropMessageOfType(null);
        leaderActor.tell(commitMsg, leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().orElseThrow());

        expectFirstMatching(newFollowerCollectorActor, ApplyLeaderSnapshot.class);

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
    public void testAddServerWithPriorSnapshotCompleteTimeout() {
        LOG.info("testAddServerWithPriorSnapshotCompleteTimeout starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(1);

        // Drop commit message so the snapshot doesn't complete.
        leaderRaftActor.setDropMessageOfType(COMMIT_MESSAGE_CLASS);

        leaderActor.tell(new InitiateCaptureSnapshot(), leaderActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.TIMEOUT, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());

        LOG.info("testAddServerWithPriorSnapshotCompleteTimeout ending");
    }

    @Test
    public void testAddServerWithLeaderChangeBeforePriorSnapshotComplete() {
        LOG.info("testAddServerWithLeaderChangeBeforePriorSnapshotComplete starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(100);

        final ActorRef leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        // Drop the commit message so the snapshot doesn't complete yet.
        leaderRaftActor.setDropMessageOfType(COMMIT_MESSAGE_CLASS);

        leaderActor.tell(new InitiateCaptureSnapshot(), leaderActor);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        Object commitMsg = expectFirstMatching(leaderCollectorActor, COMMIT_MESSAGE_CLASS);

        // Change the leader behavior to follower
        leaderActor.tell(new Follower(leaderActorContext), leaderActor);

        // Drop CaptureSnapshotReply in case install snapshot is incorrectly initiated after the prior
        // snapshot completes. This will prevent the invalid snapshot from completing and fail the
        // isCapturing assertion below.
        leaderRaftActor.setDropMessageOfType(CaptureSnapshotReply.class);

        // Complete the prior snapshot - this should be a no-op b/c it's no longer the leader
        leaderActor.tell(commitMsg, leaderActor);

        leaderActor.tell(new RaftActorServerConfigurationSupport.ServerOperationTimeout(NEW_SERVER_ID), leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());
        assertFalse("isCapturing", leaderActorContext.getSnapshotManager().isCapturing());

        LOG.info("testAddServerWithLeaderChangeBeforePriorSnapshotComplete ending");
    }

    @Test
    public void testAddServerWithLeaderChangeDuringInstallSnapshot() {
        LOG.info("testAddServerWithLeaderChangeDuringInstallSnapshot starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(8);

        ActorRef leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        // Drop the UnInitializedFollowerSnapshotReply to delay it.
        leaderRaftActor.setDropMessageOfType(UnInitializedFollowerSnapshotReply.class);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        final UnInitializedFollowerSnapshotReply snapshotReply = expectFirstMatching(leaderCollectorActor,
                UnInitializedFollowerSnapshotReply.class);

        // Prevent election timeout when the leader switches to follower
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(100);

        // Change the leader behavior to follower
        leaderActor.tell(new Follower(leaderActorContext), leaderActor);

        // Send the captured UnInitializedFollowerSnapshotReply - should be a no-op
        leaderRaftActor.setDropMessageOfType(null);
        leaderActor.tell(snapshotReply, leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());

        assertEquals("Leader peers size", 0, leaderActorContext.getPeerIds().size());

        LOG.info("testAddServerWithLeaderChangeDuringInstallSnapshot ending");
    }

    @Test
    public void testAddServerWithInstallSnapshotTimeout() {
        LOG.info("testAddServerWithInstallSnapshotTimeout starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(1);

        // Drop the InstallSnapshot message so it times out
        newFollowerRaftActor.underlyingActor().setDropMessageOfType(InstallSnapshot.class);

        leaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true), testKit.getRef());

        leaderActor.tell(new UnInitializedFollowerSnapshotReply("bogus"), leaderActor);

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
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
                MockRaftActor.builder().id(LEADER_ID).peerAddresses(Map.of(FOLLOWER_ID,
                        followerActor.path().toString())).config(configParams).persistent(Optional.of(Boolean.FALSE))
                        .props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));
        noLeaderActor.underlyingActor().waitForInitializeBehaviorComplete();

        noLeaderActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true),
                testKit.getRef());
        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, addServerReply.getStatus());

        LOG.info("testAddServerWithNoLeader ending");
    }

    @Test
    public void testAddServerWithNoConsensusReached() {
        LOG.info("testAddServerWithNoConsensusReached starting");

        setupNewFollower();
        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), initialActorContext)
                    .withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        MockLeaderRaftActor leaderRaftActor = leaderActor.underlyingActor();
        final RaftActorContext leaderActorContext = leaderRaftActor.getRaftActorContext();

        final ActorRef leaderCollectorActor = newLeaderCollectorActor(leaderRaftActor);

        // Drop UnInitializedFollowerSnapshotReply initially
        leaderRaftActor.setDropMessageOfType(UnInitializedFollowerSnapshotReply.class);

        MockNewFollowerRaftActor newFollowerRaftActorInstance = newFollowerRaftActor.underlyingActor();
        newFollowerCollectorActor = newCollectorActor(newFollowerRaftActorInstance, NEW_SERVER_ID);

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
        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, addServerReply.getStatus());
        assertEquals("getLeaderHint", LEADER_ID, addServerReply.getLeaderHint().orElseThrow());

        // Verify ServerConfigurationPayload entry in leader's log
        verifyServerConfigurationPayloadEntry(leaderActorContext.getReplicatedLog(), votingServer(LEADER_ID),
                votingServer(NEW_SERVER_ID));

        // The second AddServer should fail since consensus wasn't reached for the first
        addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.PRIOR_REQUEST_CONSENSUS_TIMEOUT, addServerReply.getStatus());

        // Re-send the second AddServer - should also fail
        leaderActor.tell(new AddServer(NEW_SERVER_ID2, "", false), testKit.getRef());
        addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.PRIOR_REQUEST_CONSENSUS_TIMEOUT, addServerReply.getStatus());

        LOG.info("testAddServerWithNoConsensusReached ending");
    }

    @Test
    public void testAddServerWithExistingServer() {
        LOG.info("testAddServerWithExistingServer starting");

        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(FOLLOWER_ID, followerActor.path().toString()),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        leaderActor.tell(new AddServer(FOLLOWER_ID, followerActor.path().toString(), true), testKit.getRef());

        AddServerReply addServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), AddServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.ALREADY_EXISTS, addServerReply.getStatus());

        LOG.info("testAddServerWithExistingServer ending");
    }

    @Test
    public void testAddServerForwardedToLeader() {
        LOG.info("testAddServerForwardedToLeader starting");

        setupNewFollower();
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        ActorRef leaderActor = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId(LEADER_ID));

        TestActorRef<MockRaftActor> followerRaftActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(FOLLOWER_ID).peerAddresses(Map.of(LEADER_ID,
                        leaderActor.path().toString())).config(configParams).persistent(Optional.of(Boolean.FALSE))
                        .props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(FOLLOWER_ID));
        followerRaftActor.underlyingActor().waitForInitializeBehaviorComplete();

        followerRaftActor.tell(new AppendEntries(1, LEADER_ID, 0, 1, List.of(), -1, -1, (short)0), leaderActor);

        followerRaftActor.tell(new AddServer(NEW_SERVER_ID, newFollowerRaftActor.path().toString(), true),
                testKit.getRef());
        expectFirstMatching(leaderActor, AddServer.class);

        LOG.info("testAddServerForwardedToLeader ending");
    }

    @Test
    public void testOnApplyState() {
        LOG.info("testOnApplyState starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
        TestActorRef<MockRaftActor> noLeaderActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(LEADER_ID).peerAddresses(Map.of(FOLLOWER_ID,
                        followerActor.path().toString())).config(configParams).persistent(Optional.of(Boolean.FALSE))
                        .props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        RaftActorServerConfigurationSupport support = new RaftActorServerConfigurationSupport(
                noLeaderActor.underlyingActor());

        ReplicatedLogEntry serverConfigEntry = new SimpleReplicatedLogEntry(1, 1, new ClusterConfig());
        boolean handled = support.handleMessage(new ApplyState(null, null, serverConfigEntry), ActorRef.noSender());
        assertTrue("Message handled", handled);

        ReplicatedLogEntry nonServerConfigEntry = new SimpleReplicatedLogEntry(1, 1,
                new MockRaftActorContext.MockPayload("1"));
        handled = support.handleMessage(new ApplyState(null, null, nonServerConfigEntry), ActorRef.noSender());
        assertFalse("Message handled", handled);

        LOG.info("testOnApplyState ending");
    }

    @Test
    public void testRemoveServerWithNoLeader() {
        LOG.info("testRemoveServerWithNoLeader starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        TestActorRef<MockRaftActor> leaderActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(LEADER_ID).peerAddresses(Map.of(FOLLOWER_ID,
                        followerActor.path().toString())).config(configParams).persistent(Optional.of(Boolean.FALSE))
                        .props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));
        leaderActor.underlyingActor().waitForInitializeBehaviorComplete();

        leaderActor.tell(new RemoveServer(FOLLOWER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, removeServerReply.getStatus());

        LOG.info("testRemoveServerWithNoLeader ending");
    }

    @Test
    public void testRemoveServerNonExistentServer() {
        LOG.info("testRemoveServerNonExistentServer starting");

        RaftActorContext initialActorContext = new MockRaftActorContext();

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(FOLLOWER_ID, followerActor.path().toString()),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        leaderActor.tell(new RemoveServer(NEW_SERVER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.DOES_NOT_EXIST, removeServerReply.getStatus());

        LOG.info("testRemoveServerNonExistentServer ending");
    }

    @Test
    public void testRemoveServerForwardToLeader() {
        LOG.info("testRemoveServerForwardToLeader starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

        ActorRef leaderActor = actorFactory.createTestActor(
                MessageCollectorActor.props(), actorFactory.generateActorId(LEADER_ID));

        TestActorRef<MockRaftActor> followerRaftActor = actorFactory.createTestActor(
                MockRaftActor.builder().id(FOLLOWER_ID).peerAddresses(Map.of(LEADER_ID,
                        leaderActor.path().toString())).config(configParams).persistent(Optional.of(Boolean.FALSE))
                        .props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(FOLLOWER_ID));
        followerRaftActor.underlyingActor().waitForInitializeBehaviorComplete();

        followerRaftActor.tell(new AppendEntries(1, LEADER_ID, 0, 1, List.of(), -1, -1, (short)0), leaderActor);

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

        final String follower1ActorId = actorFactory.generateActorId(FOLLOWER_ID);
        final String follower1ActorPath = actorFactory.createTestActorPath(follower1ActorId);
        final String follower2ActorId = actorFactory.generateActorId(FOLLOWER_ID2);
        final String follower2ActorPath = actorFactory.createTestActorPath(follower2ActorId);
        RaftActorContext initialActorContext = new MockRaftActorContext();

        final String downNodeId = "downNode";
        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
            MockLeaderRaftActor.props(stateDir(),
                Map.of(FOLLOWER_ID, follower1ActorPath, FOLLOWER_ID2, follower2ActorPath, downNodeId, ""),
                initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        final ActorRef leaderCollector = newLeaderCollectorActor(leaderActor.underlyingActor());

        ActorRef follower1Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        final TestActorRef<CollectingMockRaftActor> follower1Actor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), FOLLOWER_ID, Map.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID2, follower2ActorPath, downNodeId, ""), configParams, NO_PERSISTENCE,
                        follower1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), follower1ActorId);

        ActorRef follower2Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        final TestActorRef<CollectingMockRaftActor> follower2Actor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), FOLLOWER_ID2, Map.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID, follower1ActorPath, downNodeId, ""), configParams, NO_PERSISTENCE,
                        follower2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), follower2ActorId);

        leaderActor.underlyingActor().waitForInitializeBehaviorComplete();
        follower1Actor.underlyingActor().waitForInitializeBehaviorComplete();
        follower2Actor.underlyingActor().waitForInitializeBehaviorComplete();

        leaderActor.tell(new RemoveServer(FOLLOWER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, removeServerReply.getStatus());

        ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollector, ApplyState.class);
        assertEquals(0L, applyState.getReplicatedLogEntry().index());
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), votingServer(FOLLOWER_ID2), votingServer(downNodeId));

        applyState = MessageCollectorActor.expectFirstMatching(follower2Collector, ApplyState.class);
        assertEquals(0L, applyState.getReplicatedLogEntry().index());
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), votingServer(FOLLOWER_ID2), votingServer(downNodeId));

        RaftActorBehavior currentBehavior = leaderActor.underlyingActor().getCurrentBehavior();
        assertTrue("Expected Leader", currentBehavior instanceof Leader);
        assertEquals("Follower ids size", 2, ((Leader)currentBehavior).getFollowerIds().size());

        MessageCollectorActor.expectFirstMatching(follower1Collector, ServerRemoved.class);

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
                MockLeaderRaftActor.props(stateDir(), Map.of(FOLLOWER_ID, followerActorPath),
                        initialActorContext).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        final ActorRef leaderCollector = newLeaderCollectorActor(leaderActor.underlyingActor());

        final ActorRef followerCollector =
                actorFactory.createActor(MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), FOLLOWER_ID, Map.of(LEADER_ID, leaderActor.path().toString()),
                        configParams, NO_PERSISTENCE, followerCollector)
                        .withDispatcher(Dispatchers.DefaultDispatcherId()),
                followerActorId);

        leaderActor.tell(new RemoveServer(LEADER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), RemoveServerReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, removeServerReply.getStatus());

        final ApplyState applyState = MessageCollectorActor.expectFirstMatching(followerCollector, ApplyState.class);
        assertEquals(0L, applyState.getReplicatedLogEntry().index());
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(FOLLOWER_ID));

        MessageCollectorActor.expectFirstMatching(leaderCollector, ServerRemoved.class);

        LOG.info("testRemoveServerLeader ending");
    }

    @Test
    public void testRemoveServerLeaderWithNoFollowers() {
        LOG.info("testRemoveServerLeaderWithNoFollowers starting");

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(),
                        new MockRaftActorContext()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                actorFactory.generateActorId(LEADER_ID));

        leaderActor.tell(new RemoveServer(LEADER_ID), testKit.getRef());
        RemoveServerReply removeServerReply = testKit.expectMsgClass(Duration.ofSeconds(5), RemoveServerReply.class);
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
                MockLeaderRaftActor.props(stateDir(), Map.of(FOLLOWER_ID, follower1ActorPath,
                        FOLLOWER_ID2, follower2ActorPath), new MockRaftActorContext())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()), actorFactory.generateActorId(LEADER_ID));
        ActorRef leaderCollector = newLeaderCollectorActor(leaderActor.underlyingActor());

        ActorRef follower1Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        final TestActorRef<CollectingMockRaftActor> follower1RaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), FOLLOWER_ID, Map.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID2, follower2ActorPath), configParams, NO_PERSISTENCE, follower1Collector)
                        .withDispatcher(Dispatchers.DefaultDispatcherId()), follower1ActorId);

        ActorRef follower2Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        final TestActorRef<CollectingMockRaftActor> follower2RaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), FOLLOWER_ID2, Map.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID, follower1ActorPath), configParams, NO_PERSISTENCE, follower2Collector)
                        .withDispatcher(Dispatchers.DefaultDispatcherId()), follower2ActorId);

        // Send first ChangeServersVotingStatus message

        leaderActor.tell(new ChangeServersVotingStatus(Map.of(FOLLOWER_ID, false, FOLLOWER_ID2, false)),
                testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        final ApplyState applyState = MessageCollectorActor.expectFirstMatching(leaderCollector, ApplyState.class);
        assertEquals(0L, applyState.getReplicatedLogEntry().index());
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), nonVotingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower1Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower1RaftActor.underlyingActor().getRaftActorContext()
                .getReplicatedLog(), votingServer(LEADER_ID), nonVotingServer(FOLLOWER_ID),
                nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower2Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower2RaftActor.underlyingActor().getRaftActorContext()
                .getReplicatedLog(), votingServer(LEADER_ID), nonVotingServer(FOLLOWER_ID),
                nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.clearMessages(leaderCollector);
        MessageCollectorActor.clearMessages(follower1Collector);
        MessageCollectorActor.clearMessages(follower2Collector);

        // Send second ChangeServersVotingStatus message

        leaderActor.tell(new ChangeServersVotingStatus(Map.of(FOLLOWER_ID, true)), testKit.getRef());
        reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        MessageCollectorActor.expectFirstMatching(leaderCollector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                votingServer(LEADER_ID), votingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower1Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower1RaftActor.underlyingActor().getRaftActorContext()
                .getReplicatedLog(), votingServer(LEADER_ID), votingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower2Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower2RaftActor.underlyingActor().getRaftActorContext()
                .getReplicatedLog(), votingServer(LEADER_ID), votingServer(FOLLOWER_ID), nonVotingServer(FOLLOWER_ID2));

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
                MockLeaderRaftActor.props(stateDir(), Map.of(FOLLOWER_ID, follower1ActorPath,
                        FOLLOWER_ID2, follower2ActorPath), new MockRaftActorContext())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()), actorFactory.generateActorId(LEADER_ID));
        ActorRef leaderCollector = newLeaderCollectorActor(leaderActor.underlyingActor());

        ActorRef follower1Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        final TestActorRef<CollectingMockRaftActor> follower1RaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), FOLLOWER_ID, Map.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID2, follower2ActorPath), configParams, NO_PERSISTENCE, follower1Collector)
                        .withDispatcher(Dispatchers.DefaultDispatcherId()), follower1ActorId);

        ActorRef follower2Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        final TestActorRef<CollectingMockRaftActor> follower2RaftActor = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), FOLLOWER_ID2, Map.of(LEADER_ID, leaderActor.path().toString(),
                        FOLLOWER_ID, follower1ActorPath), configParams, NO_PERSISTENCE, follower2Collector)
                        .withDispatcher(Dispatchers.DefaultDispatcherId()), follower2ActorId);

        // Send ChangeServersVotingStatus message

        leaderActor.tell(new ChangeServersVotingStatus(Map.of(LEADER_ID, false)), testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        MessageCollectorActor.expectFirstMatching(leaderCollector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(leaderActor.underlyingActor().getRaftActorContext().getReplicatedLog(),
                nonVotingServer(LEADER_ID), votingServer(FOLLOWER_ID), votingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower1Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower1RaftActor.underlyingActor().getRaftActorContext()
                .getReplicatedLog(), nonVotingServer(LEADER_ID), votingServer(FOLLOWER_ID), votingServer(FOLLOWER_ID2));

        MessageCollectorActor.expectFirstMatching(follower2Collector, ApplyState.class);
        verifyServerConfigurationPayloadEntry(follower2RaftActor.underlyingActor().getRaftActorContext()
                .getReplicatedLog(), nonVotingServer(LEADER_ID), votingServer(FOLLOWER_ID), votingServer(FOLLOWER_ID2));

        verifyRaftState(RaftState.Leader, follower1RaftActor.underlyingActor(), follower2RaftActor.underlyingActor());
        verifyRaftState(RaftState.Follower, leaderActor.underlyingActor());

        MessageCollectorActor.expectMatching(leaderCollector, AppendEntries.class, 2);

        LOG.info("testChangeLeaderToNonVoting ending");
    }

    @Test
    public void testChangeLeaderToNonVotingInSingleNode() {
        LOG.info("testChangeLeaderToNonVotingInSingleNode starting");

        TestActorRef<MockLeaderRaftActor> leaderActor = actorFactory.createTestActor(
                MockLeaderRaftActor.props(stateDir(), Map.of(), new MockRaftActorContext())
                        .withDispatcher(Dispatchers.DefaultDispatcherId()), actorFactory.generateActorId(LEADER_ID));

        leaderActor.tell(new ChangeServersVotingStatus(Map.of(LEADER_ID, false)), testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.INVALID_REQUEST, reply.getStatus());

        LOG.info("testChangeLeaderToNonVotingInSingleNode ending");
    }

    @Test
    public void testChangeToVotingWithNoLeader() {
        LOG.info("testChangeToVotingWithNoLeader starting");

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(5);

        final String node1ID = "node1";
        final String node2ID = "node2";

        // Set up a persisted ServerConfigurationPayload. Initially node1 and node2 will come up as non-voting.
        // via the server config. The server config will also contain 2 voting peers that are down (ie no
        // actors created).

        final var persistedServerConfig = new ClusterConfig(
                new ServerInfo(node1ID, false), new ServerInfo(node2ID, false),
                new ServerInfo("downNode1", true), new ServerInfo("downNode2", true));
        SimpleReplicatedLogEntry persistedServerConfigEntry = new SimpleReplicatedLogEntry(0, 1, persistedServerConfig);

        InMemoryJournal.addEntry(node1ID, 1, new UpdateElectionTerm(1, "downNode1"));
        InMemoryJournal.addEntry(node1ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node1ID, 3, new ApplyJournalEntries(0));
        InMemoryJournal.addEntry(node2ID, 1, new UpdateElectionTerm(1, "downNode2"));
        InMemoryJournal.addEntry(node2ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 3, new ApplyJournalEntries(0));

        ActorRef node1Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node1RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), node1ID, Map.of(), configParams,
                        PERSISTENT, node1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node1ID);
        CollectingMockRaftActor node1RaftActor = node1RaftActorRef.underlyingActor();

        ActorRef node2Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node2RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), node2ID, Map.of(), configParams,
                        PERSISTENT, node2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node2ID);
        CollectingMockRaftActor node2RaftActor = node2RaftActorRef.underlyingActor();

        node1RaftActor.waitForInitializeBehaviorComplete();
        node2RaftActor.waitForInitializeBehaviorComplete();

        // Verify the intended server config was loaded and applied.
        verifyServerConfigurationPayloadEntry(node1RaftActor.getRaftActorContext().getReplicatedLog(),
                nonVotingServer(node1ID), nonVotingServer(node2ID), votingServer("downNode1"),
                votingServer("downNode2"));
        assertFalse("isVotingMember", node1RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Follower, node1RaftActor.getRaftState());
        assertEquals("getLeaderId", null, node1RaftActor.getLeaderId());

        verifyServerConfigurationPayloadEntry(node2RaftActor.getRaftActorContext().getReplicatedLog(),
                nonVotingServer(node1ID), nonVotingServer(node2ID), votingServer("downNode1"),
                votingServer("downNode2"));
        assertFalse("isVotingMember", node2RaftActor.getRaftActorContext().isVotingMember());

        // For the test, we send a ChangeServersVotingStatus message to node1 to flip the voting states for
        // each server, ie node1 and node2 to voting and the 2 down nodes to non-voting. This should cause
        // node1 to try to elect itself as leader in order to apply the new server config. Since the 2
        // down nodes are switched to non-voting, node1 should only need a vote from node2.

        // First send the message such that node1 has no peer address for node2 - should fail.

        ChangeServersVotingStatus changeServers = new ChangeServersVotingStatus(Map.of(node1ID, true,
                node2ID, true, "downNode1", false, "downNode2", false));
        node1RaftActorRef.tell(changeServers, testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, reply.getStatus());
        assertEquals("getRaftState", RaftState.Follower, node1RaftActor.getRaftState());

        // Send an AppendEntries so node1 has a leaderId

        long term = node1RaftActor.getRaftActorContext().currentTerm();
        node1RaftActorRef.tell(new AppendEntries(term, "downNode1", -1L, -1L,
                List.of(), 0, -1, (short)1), ActorRef.noSender());

        // Wait for the ElectionTimeout to clear the leaderId. The leaderId must be null so on the next
        // ChangeServersVotingStatus message, it will try to elect a leader.

        AbstractRaftActorIntegrationTest.verifyRaftState(node1RaftActorRef,
            rs -> assertNull("getLeader", rs.getLeader()));

        // Update node2's peer address and send the message again

        node1RaftActor.setPeerAddress(node2ID, node2RaftActorRef.path().toString());

        node1RaftActorRef.tell(changeServers, testKit.getRef());
        reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        ApplyJournalEntries apply = MessageCollectorActor.expectFirstMatching(node1Collector,
                ApplyJournalEntries.class);
        assertEquals("getToIndex", 1, apply.getToIndex());
        verifyServerConfigurationPayloadEntry(node1RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID), nonVotingServer("downNode1"),
                nonVotingServer("downNode2"));
        assertTrue("isVotingMember", node1RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Leader, node1RaftActor.getRaftState());

        apply = MessageCollectorActor.expectFirstMatching(node2Collector, ApplyJournalEntries.class);
        assertEquals("getToIndex", 1, apply.getToIndex());
        verifyServerConfigurationPayloadEntry(node2RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID), nonVotingServer("downNode1"),
                nonVotingServer("downNode2"));
        assertTrue("isVotingMember", node2RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Follower, node2RaftActor.getRaftState());

        LOG.info("testChangeToVotingWithNoLeader ending");
    }

    @Test
    public void testChangeToVotingWithNoLeaderAndElectionTimeout() {
        LOG.info("testChangeToVotingWithNoLeaderAndElectionTimeout starting");

        final String node1ID = "node1";
        final String node2ID = "node2";

        final PeerAddressResolver peerAddressResolver = peerId -> peerId.equals(node1ID)
                ? actorFactory.createTestActorPath(node1ID) : peerId.equals(node2ID)
                        ? actorFactory.createTestActorPath(node2ID) : null;

        final var persistedServerConfig = new ClusterConfig(
                new ServerInfo(node1ID, false), new ServerInfo(node2ID, true));
        SimpleReplicatedLogEntry persistedServerConfigEntry = new SimpleReplicatedLogEntry(0, 1, persistedServerConfig);

        InMemoryJournal.addEntry(node1ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node1ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node2ID, 2, persistedServerConfigEntry);

        DefaultConfigParamsImpl configParams1 = new DefaultConfigParamsImpl();
        configParams1.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams1.setElectionTimeoutFactor(1);
        configParams1.setPeerAddressResolver(peerAddressResolver);
        ActorRef node1Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node1RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), node1ID, Map.of(), configParams1,
                        PERSISTENT, node1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node1ID);
        final CollectingMockRaftActor node1RaftActor = node1RaftActorRef.underlyingActor();

        DefaultConfigParamsImpl configParams2 = new DefaultConfigParamsImpl();
        configParams2.setElectionTimeoutFactor(1000000);
        configParams2.setPeerAddressResolver(peerAddressResolver);
        ActorRef node2Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node2RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), node2ID, Map.of(), configParams2,
                        PERSISTENT, node2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node2ID);
        CollectingMockRaftActor node2RaftActor = node2RaftActorRef.underlyingActor();

        // Send a ChangeServersVotingStatus message to node1 to change mode1 to voting. This should cause
        // node1 to try to elect itself as leader in order to apply the new server config. But we'll drop
        // RequestVote messages in node2 which should cause node1 to time out and revert back to the previous
        // server config and fail with NO_LEADER. Note that node1 shouldn't forward the request to node2 b/c
        // node2 was previously voting.

        node2RaftActor.setDropMessageOfType(RequestVote.class);

        ChangeServersVotingStatus changeServers = new ChangeServersVotingStatus(Map.of(node1ID, true));
        node1RaftActorRef.tell(changeServers, testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.NO_LEADER, reply.getStatus());

        assertEquals("Server config", Set.of(nonVotingServer(node1ID), votingServer(node2ID)),
            Set.copyOf(node1RaftActor.getRaftActorContext().getPeerServerInfo(true).serverInfo()));
        assertEquals("getRaftState", RaftState.Follower, node1RaftActor.getRaftState());

        LOG.info("testChangeToVotingWithNoLeaderAndElectionTimeout ending");
    }

    @Test
    public void testChangeToVotingWithNoLeaderAndForwardedToOtherNodeAfterElectionTimeout() {
        LOG.info("testChangeToVotingWithNoLeaderAndForwardedToOtherNodeAfterElectionTimeout starting");

        final String node1ID = "node1";
        final String node2ID = "node2";

        final PeerAddressResolver peerAddressResolver = peerId -> peerId.equals(node1ID)
                ? actorFactory.createTestActorPath(node1ID) : peerId.equals(node2ID)
                        ? actorFactory.createTestActorPath(node2ID) : null;

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(3);
        configParams.setPeerAddressResolver(peerAddressResolver);

        final var persistedServerConfig = new ClusterConfig(
                new ServerInfo(node1ID, false), new ServerInfo(node2ID, false));
        SimpleReplicatedLogEntry persistedServerConfigEntry = new SimpleReplicatedLogEntry(0, 1, persistedServerConfig);

        InMemoryJournal.addEntry(node1ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node1ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node2ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 3, new SimpleReplicatedLogEntry(1, 1,
                new MockRaftActorContext.MockPayload("2")));
        InMemoryJournal.addEntry(node2ID, 4, new ApplyJournalEntries(1));

        ActorRef node1Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node1RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), node1ID, Map.of(), configParams,
                        PERSISTENT, node1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node1ID);
        final CollectingMockRaftActor node1RaftActor = node1RaftActorRef.underlyingActor();

        ActorRef node2Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node2RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), node2ID, Map.of(), configParams,
                        PERSISTENT, node2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node2ID);
        final CollectingMockRaftActor node2RaftActor = node2RaftActorRef.underlyingActor();

        // Send a ChangeServersVotingStatus message to node1 to change mode1 to voting. This should cause
        // node1 to try to elect itself as leader in order to apply the new server config. However node1's log
        // is behind node2's so node2 should not grant node1's vote. This should cause node1 to time out and
        // forward the request to node2.

        ChangeServersVotingStatus changeServers = new ChangeServersVotingStatus(
                Map.of(node1ID, true, node2ID, true));
        node1RaftActorRef.tell(changeServers, testKit.getRef());
        ServerChangeReply reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        MessageCollectorActor.expectFirstMatching(node2Collector, ApplyJournalEntries.class);
        verifyServerConfigurationPayloadEntry(node2RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID));
        assertEquals("getRaftState", RaftState.Leader, node2RaftActor.getRaftState());

        MessageCollectorActor.expectFirstMatching(node1Collector, ApplyJournalEntries.class);
        verifyServerConfigurationPayloadEntry(node1RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID));
        assertTrue("isVotingMember", node1RaftActor.getRaftActorContext().isVotingMember());
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

        configParams.setPeerAddressResolver(peerId -> peerId.equals(node1ID)
                ? actorFactory.createTestActorPath(node1ID) : peerId.equals(node2ID)
                        ? actorFactory.createTestActorPath(node2ID) : null);

        final var persistedServerConfig = new ClusterConfig(
                new ServerInfo(node1ID, false), new ServerInfo(node2ID, true));
        SimpleReplicatedLogEntry persistedServerConfigEntry = new SimpleReplicatedLogEntry(0, 1, persistedServerConfig);

        InMemoryJournal.addEntry(node1ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node1ID, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(node2ID, 1, new UpdateElectionTerm(1, "node1"));
        InMemoryJournal.addEntry(node2ID, 2, persistedServerConfigEntry);

        ActorRef node1Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node1RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), node1ID, Map.of(), configParams,
                        PERSISTENT, node1Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node1ID);
        final CollectingMockRaftActor node1RaftActor = node1RaftActorRef.underlyingActor();

        ActorRef node2Collector = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId("collector"));
        TestActorRef<CollectingMockRaftActor> node2RaftActorRef = actorFactory.createTestActor(
                CollectingMockRaftActor.props(stateDir(), node2ID, Map.of(), configParams,
                        PERSISTENT, node2Collector).withDispatcher(Dispatchers.DefaultDispatcherId()), node2ID);
        CollectingMockRaftActor node2RaftActor = node2RaftActorRef.underlyingActor();

        // Send a ChangeServersVotingStatus message to node1 to change node1 to voting. This should cause
        // node1 to try to elect itself as leader in order to apply the new server config. But we'll drop
        // RequestVote messages in node2 and make it the leader so node1 should forward the server change
        // request to node2 when node2 is elected.

        node2RaftActor.setDropMessageOfType(RequestVote.class);

        ChangeServersVotingStatus changeServers = new ChangeServersVotingStatus(Map.of(node1ID, true, node2ID, true));
        node1RaftActorRef.tell(changeServers, testKit.getRef());

        MessageCollectorActor.expectFirstMatching(node2Collector, RequestVote.class);

        node2RaftActorRef.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        ServerChangeReply reply = testKit.expectMsgClass(Duration.ofSeconds(5), ServerChangeReply.class);
        assertEquals("getStatus", ServerChangeStatus.OK, reply.getStatus());

        MessageCollectorActor.expectFirstMatching(node1Collector, ApplyJournalEntries.class);
        verifyServerConfigurationPayloadEntry(node1RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID));
        assertTrue("isVotingMember", node1RaftActor.getRaftActorContext().isVotingMember());
        assertEquals("getRaftState", RaftState.Follower, node1RaftActor.getRaftState());

        MessageCollectorActor.expectFirstMatching(node2Collector, ApplyJournalEntries.class);
        verifyServerConfigurationPayloadEntry(node2RaftActor.getRaftActorContext().getReplicatedLog(),
                votingServer(node1ID), votingServer(node2ID));
        assertEquals("getRaftState", RaftState.Leader, node2RaftActor.getRaftState());

        LOG.info("testChangeToVotingWithNoLeaderAndOtherLeaderElected ending");
    }

    private static void verifyRaftState(final RaftState expState, final RaftActor... raftActors) {
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            for (RaftActor raftActor : raftActors) {
                if (raftActor.getRaftState() == expState) {
                    return;
                }
            }
        }

        fail("None of the RaftActors have state " + expState);
    }

    private static ServerInfo votingServer(final String id) {
        return new ServerInfo(id, true);
    }

    private static ServerInfo nonVotingServer(final String id) {
        return new ServerInfo(id, false);
    }

    private ActorRef newLeaderCollectorActor(final MockLeaderRaftActor leaderRaftActor) {
        return newCollectorActor(leaderRaftActor, LEADER_ID);
    }

    private ActorRef newCollectorActor(final AbstractMockRaftActor raftActor, final String id) {
        ActorRef collectorActor = actorFactory.createTestActor(
                MessageCollectorActor.props(), actorFactory.generateActorId(id + "Collector"));
        raftActor.setCollectorActor(collectorActor);
        return collectorActor;
    }

    private static void verifyServerConfigurationPayloadEntry(final ReplicatedLog log, final ServerInfo... expected) {
        ReplicatedLogEntry logEntry = log.get(log.lastIndex());
        final var payload = assertInstanceOf(ClusterConfig.class, logEntry.getData());
        assertEquals("Server config", Set.of(expected), Set.copyOf(payload.serverInfo()));
    }

    private static RaftActorContextImpl newFollowerContext(final String id,
            final TestActorRef<? extends AbstractActor> actor) {
        final var configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);

        return new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
            new LocalAccess(id, new FailingTermInfoStore(1, LEADER_ID)),
            -1, -1, Map.of(LEADER_ID, ""), configParams, (short) 0,  new NonPersistentDataProvider(Runnable::run),
            applyState -> actor.tell(applyState, actor), MoreExecutors.directExecutor());
    }

    abstract static class AbstractMockRaftActor extends MockRaftActor {
        private volatile ActorRef collectorActor;
        private volatile Class<?> dropMessageOfType;

        AbstractMockRaftActor(final Path stateDir, final String id, final Map<String, String> peerAddresses,
                final Optional<ConfigParams> config, final boolean persistent, final ActorRef collectorActor) {
            super(stateDir, builder().id(id).peerAddresses(peerAddresses).config(config.orElseThrow())
                .persistent(Optional.of(persistent)));
            this.collectorActor = collectorActor;
        }

        void setDropMessageOfType(final Class<?> dropMessageOfType) {
            this.dropMessageOfType = dropMessageOfType;
        }

        void setCollectorActor(final ActorRef collectorActor) {
            this.collectorActor = collectorActor;
        }

        @Override
        public void handleCommand(final Object message) {
            if (dropMessageOfType == null || !dropMessageOfType.equals(message.getClass())) {
                super.handleCommand(message);
            }

            if (collectorActor != null) {
                collectorActor.tell(message, getSender());
            }
        }
    }

    public static class CollectingMockRaftActor extends AbstractMockRaftActor {

        CollectingMockRaftActor(final Path stateDir, final String id, final Map<String, String> peerAddresses,
                final Optional<ConfigParams> config, final boolean persistent, final ActorRef collectorActor) {
            super(stateDir, id, peerAddresses, config, persistent, collectorActor);
            snapshotCohortDelegate = new RaftActorSnapshotCohort() {
                @Override
                public void createSnapshot(final ActorRef actorRef, final OutputStream installSnapshotStream) {
                    actorRef.tell(new CaptureSnapshotReply(ByteState.empty(), installSnapshotStream), actorRef);
                }

                @Override
                public void applySnapshot(
                        final org.opendaylight.controller.cluster.raft.persisted.Snapshot.State snapshotState) {
                }

                @Override
                public org.opendaylight.controller.cluster.raft.persisted.Snapshot.State deserializeSnapshot(
                        final ByteSource snapshotBytes) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public static Props props(final Path stateDir, final String id, final Map<String, String> peerAddresses,
                final ConfigParams config, final boolean persistent, final ActorRef collectorActor) {
            return Props.create(CollectingMockRaftActor.class, stateDir, id, peerAddresses, Optional.of(config),
                    persistent, collectorActor);
        }

    }

    public static class MockLeaderRaftActor extends AbstractMockRaftActor {
        public MockLeaderRaftActor(final Path stateDir, final Map<String, String> peerAddresses,
                final ConfigParams config, final RaftActorContext fromContext) {
            super(stateDir, LEADER_ID, peerAddresses, Optional.of(config), NO_PERSISTENCE, null);
            setPersistence(false);

            final var context = getRaftActorContext();
            for (int i = 0; i < fromContext.getReplicatedLog().size(); i++) {
                final var entry = fromContext.getReplicatedLog().get(i);
                getState().add(entry.getData());
                context.getReplicatedLog().append(entry);
            }

            context.setCommitIndex(fromContext.getCommitIndex());
            context.setLastApplied(fromContext.getLastApplied());
            context.setTermInfo(fromContext.termInfo());
        }

        @Override
        protected void initializeBehavior() {
            changeCurrentBehavior(new Leader(getRaftActorContext()));
            initializeBehaviorComplete.countDown();
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void createSnapshot(final ActorRef actorRef, final OutputStream installSnapshotStream) {
            MockSnapshotState snapshotState = new MockSnapshotState(List.copyOf(getState()));
            if (installSnapshotStream != null) {
                SerializationUtils.serialize(snapshotState, installSnapshotStream);
            }

            actorRef.tell(new CaptureSnapshotReply(snapshotState, installSnapshotStream), actorRef);
        }

        static Props props(final Path stateDir, final Map<String, String> peerAddresses,
                final RaftActorContext fromContext) {
            final var configParams = new DefaultConfigParamsImpl();
            configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
            configParams.setElectionTimeoutFactor(10);
            return Props.create(MockLeaderRaftActor.class, stateDir, peerAddresses, configParams, fromContext);
        }
    }

    public static class MockNewFollowerRaftActor extends AbstractMockRaftActor {
        public MockNewFollowerRaftActor(final Path stateDir, final ConfigParams config, final ActorRef collectorActor) {
            super(stateDir, NEW_SERVER_ID, Map.of(), Optional.of(config), NO_PERSISTENCE, collectorActor);
            setPersistence(false);
        }

        static Props props(final Path stateDir, final ConfigParams config, final ActorRef collectorActor) {
            return Props.create(MockNewFollowerRaftActor.class, stateDir, config, collectorActor);
        }
    }
}
