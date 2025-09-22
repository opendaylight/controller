/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.raft.RaftActorTestKit.awaitSnapshot;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.protobufv3.internal.ByteString;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MessageCollectorActor;
import org.opendaylight.controller.cluster.raft.MockCommand;
import org.opendaylight.controller.cluster.raft.MockRaftActor;
import org.opendaylight.controller.cluster.raft.MockRaftActor.Builder;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.MockRaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.MockSnapshotState;
import org.opendaylight.controller.cluster.raft.NoopPeerAddressResolver;
import org.opendaylight.controller.cluster.raft.PeerAddressResolver;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.PropertiesTermInfoStore;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.WellKnownRaftPolicy;

class FollowerTest extends AbstractRaftActorBehaviorTest<Follower> {
    private final short ourPayloadVersion = 5;

    private final ActorRef followerActor = actorFactory.createActor(
            MessageCollectorActor.props(), actorFactory.generateActorId("follower"));
    private final ActorRef leaderActor = actorFactory.createActor(
            MessageCollectorActor.props(), actorFactory.generateActorId("leader"));

    private Follower follower;

    @Override
    @AfterEach
    void afterEach() {
        if (follower != null) {
            follower.close();
        }

        super.afterEach();
    }

    @Override
    protected Follower createBehavior(final RaftActorContext actorContext) {
        return spy(new Follower(actorContext));
    }

    @Override
    protected MockRaftActorContext createActorContext() {
        return createActorContext(ourPayloadVersion);
    }

    @Override
    protected  MockRaftActorContext createActorContext(final int payloadVersion) {
        return createActorContext(followerActor, payloadVersion);
    }

    @Override
    protected  MockRaftActorContext createActorContext(final ActorRef actorRef, final int payloadVersion) {
        final var context = new MockRaftActorContext("follower", stateDir, getSystem(), actorRef, payloadVersion);
        ((DefaultConfigParamsImpl) context.getConfigParams()).setPeerAddressResolver(
            peerId -> leaderActor.path().toString());
        return context;
    }

    @Test
    void testThatAnElectionTimeoutIsTriggered() {
        MockRaftActorContext actorContext = createActorContext();
        follower = new Follower(actorContext);

        MessageCollectorActor.expectFirstMatching(followerActor, TimeoutNow.class,
                actorContext.getConfigParams().getElectionTimeOutInterval().multipliedBy(6).toMillis());
    }

    @Test
    void testHandleElectionTimeoutWhenNoLeaderMessageReceived() {
        logStart("testHandleElectionTimeoutWhenNoLeaderMessageReceived");

        MockRaftActorContext context = createActorContext();
        follower = new Follower(context);

        Uninterruptibles.sleepUninterruptibly(context.getConfigParams().getElectionTimeOutInterval().toMillis(),
                TimeUnit.MILLISECONDS);
        RaftActorBehavior raftBehavior = follower.handleMessage(leaderActor, ElectionTimeout.INSTANCE);

        assertTrue(raftBehavior instanceof Candidate);
    }

    @Test
    void testHandleElectionTimeoutWhenLeaderMessageReceived() {
        logStart("testHandleElectionTimeoutWhenLeaderMessageReceived");

        MockRaftActorContext context = createActorContext();
        ((DefaultConfigParamsImpl) context.getConfigParams()).setHeartBeatInterval(Duration.ofMillis(100));
        ((DefaultConfigParamsImpl) context.getConfigParams()).setElectionTimeoutFactor(4);

        follower = new Follower(context);
        context.setCurrentBehavior(follower);

        Uninterruptibles.sleepUninterruptibly(context.getConfigParams()
                .getElectionTimeOutInterval().toMillis() - 100, TimeUnit.MILLISECONDS);
        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", -1, -1, List.of(),
                -1, -1, (short) 1));

        Uninterruptibles.sleepUninterruptibly(130, TimeUnit.MILLISECONDS);
        RaftActorBehavior raftBehavior = follower.handleMessage(leaderActor, ElectionTimeout.INSTANCE);
        assertTrue(raftBehavior instanceof Follower);

        Uninterruptibles.sleepUninterruptibly(context.getConfigParams()
                .getElectionTimeOutInterval().toMillis() - 150, TimeUnit.MILLISECONDS);
        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", -1, -1, List.of(),
                -1, -1, (short) 1));

        Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        assertInstanceOf(Follower.class, follower.handleMessage(leaderActor, ElectionTimeout.INSTANCE));
    }

    @Test
    void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNull() {
        logStart("testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNull");

        MockRaftActorContext context = createActorContext();
        long term = 1000;
        context.setTermInfo(new TermInfo(term, null));

        follower = createBehavior(context);

        follower.handleMessage(leaderActor, new RequestVote(term, "test", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, RequestVoteReply.class);

        assertTrue("isVoteGranted", reply.isVoteGranted());
        assertEquals("getTerm", term, reply.getTerm());
        verify(follower).scheduleElection(any());
    }

    @Test
    void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNotTheSameAsCandidateId() {
        logStart("testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNotTheSameAsCandidateId");

        MockRaftActorContext context = createActorContext();
        long term = 1000;
        context.setTermInfo(new TermInfo(term, "test"));

        follower = createBehavior(context);

        follower.handleMessage(leaderActor, new RequestVote(term, "candidate", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, RequestVoteReply.class);

        assertFalse("isVoteGranted", reply.isVoteGranted());
        verify(follower, never()).scheduleElection(any());
    }

    @Test
    void testHandleFirstAppendEntries() {
        logStart("testHandleFirstAppendEntries");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().clear(0,2);
        context.getReplicatedLog().append(newLogEntry(1,100, "bar"));
        context.getReplicatedLog().setSnapshotIndex(99);

        final var entries = List.of(newLogEntry(2, 101, "foo"));

        assertEquals(1, context.getReplicatedLog().size());

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);
        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertFalse(syncStatus.initialSyncDone());
        assertTrue("append entries reply should be true", reply.isSuccess());
    }

    @Test
    void testHandleFirstAppendEntriesWithPrevIndexMinusOne() {
        logStart("testHandleFirstAppendEntries");

        MockRaftActorContext context = createActorContext();

        final var entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", -1, -1, entries, 101, 100, (short) 0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);
        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertFalse(syncStatus.initialSyncDone());
        assertFalse("append entries reply should be false", reply.isSuccess());
    }

    @Test
    void testHandleFirstAppendEntriesWithPrevIndexMinusOneAndReplicatedToAllIndexPresentInLog() {
        logStart("testHandleFirstAppendEntriesWithPrevIndexMinusOneAndReplicatedToAllIndexPresentInLog");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().clear(0, 2);
        context.getReplicatedLog().append(newLogEntry(1, 100, "bar"));
        context.getReplicatedLog().setSnapshotIndex(99);

        final var entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", -1, -1, entries, 101, 100, (short) 0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);
        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertFalse(syncStatus.initialSyncDone());
        assertTrue("append entries reply should be true", reply.isSuccess());
    }

    @Test
    void testHandleFirstAppendEntriesWithPrevIndexMinusOneAndReplicatedToAllIndexPresentInSnapshot() {
        logStart("testHandleFirstAppendEntriesWithPrevIndexMinusOneAndReplicatedToAllIndexPresentInSnapshot");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().clear(0, 2);
        context.getReplicatedLog().setSnapshotIndex(100);

        final var entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", -1, -1, entries, 101, 100, (short) 0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);
        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertFalse(syncStatus.initialSyncDone());
        assertTrue("append entries reply should be true", reply.isSuccess());
    }

    @Test
    void testFirstAppendEntriesWithNoPrevIndexAndReplToAllPresentInSnapshotButCalculatedPrevEntryMissing() {
        logStart(
               "testFirstAppendEntriesWithNoPrevIndexAndReplicatedToAllPresentInSnapshotButCalculatedPrevEntryMissing");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().clear(0, 2);
        context.getReplicatedLog().setSnapshotIndex(100);

        final var entries = List.of(newLogEntry(2, 105, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", -1, -1, entries, 105, 100, (short) 0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);
        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertFalse(syncStatus.initialSyncDone());
        assertFalse("append entries reply should be false", reply.isSuccess());
    }

    @Test
    void testHandleSyncUpAppendEntries() {
        logStart("testHandleSyncUpAppendEntries");

        MockRaftActorContext context = createActorContext();

        var entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.initialSyncDone());

        // Clear all the messages
        MessageCollectorActor.clearMessages(followerActor);

        final var log = setLastLogEntry(context, 1, 101, new MockCommand(""));
        log.setLastApplied(101);
        log.setCommitIndex(101);

        entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        appendEntries = new AppendEntries(2, "leader-1", 101, 1, entries, 102, 101, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertTrue(syncStatus.initialSyncDone());

        MessageCollectorActor.clearMessages(followerActor);

        // Sending the same message again should not generate another message

        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.getFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertNull(syncStatus);
    }

    @Test
    void testHandleAppendEntriesLeaderChangedBeforeSyncUpComplete() {
        logStart("testHandleAppendEntriesLeaderChangedBeforeSyncUpComplete");

        MockRaftActorContext context = createActorContext();

        var entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.initialSyncDone());

        // Clear all the messages
        MessageCollectorActor.clearMessages(followerActor);

        context.getReplicatedLog().setLastApplied(100);
        setLastLogEntry(context, 1, 100, new MockCommand(""));

        entries = List.of(newLogEntry(2, 101, "foo"));

        // leader-2 is becoming the leader now and it says the commitIndex is 45
        appendEntries = new AppendEntries(2, "leader-2", 45, 1, entries, 46, 100, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        // We get a new message saying initial status is not done
        assertFalse(syncStatus.initialSyncDone());
    }

    @Test
    void testHandleAppendEntriesLeaderChangedAfterSyncUpComplete() {
        logStart("testHandleAppendEntriesLeaderChangedAfterSyncUpComplete");

        MockRaftActorContext context = createActorContext();

        var entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.initialSyncDone());

        // Clear all the messages
        MessageCollectorActor.clearMessages(followerActor);

        var log = setLastLogEntry(context, 1, 101, new MockCommand(""));
        log.setLastApplied(101);
        log.setCommitIndex(101);

        entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        appendEntries = new AppendEntries(2, "leader-1", 101, 1, entries, 102, 101, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertTrue(syncStatus.initialSyncDone());

        // Clear all the messages
        MessageCollectorActor.clearMessages(followerActor);

        log = setLastLogEntry(context, 1, 100, new MockCommand(""));
        log.setLastApplied(100);

        entries = List.of(newLogEntry(2, 101, "foo"));

        // leader-2 is becoming the leader now and it says the commitIndex is 45
        appendEntries = new AppendEntries(2, "leader-2", 45, 1, entries, 46, 100, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        // We get a new message saying initial status is not done
        assertFalse(syncStatus.initialSyncDone());
    }

    /**
     * This test verifies that when an AppendEntries RPC is received by a RaftActor
     * with a commitIndex that is greater than what has been applied to the
     * state machine of the RaftActor, the RaftActor applies the state and
     * sets it current applied state to the commitIndex of the sender.
     */
    @Test
    void testHandleAppendEntriesWithNewerCommitIndex() {
        logStart("testHandleAppendEntriesWithNewerCommitIndex");

        final var context = createActorContext();

        setLastLogEntry(context, 1, 100, new MockCommand(""));
        final var log = context.getReplicatedLog();
        log.setLastApplied(100);
        log.setSnapshotIndex(99);

        final var entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        assertEquals("getLastApplied", 101L, log.getLastApplied());
    }

    /**
     * This test verifies that when an AppendEntries is received with a prevLogTerm
     * which does not match the term that is in RaftActors log entry at prevLogIndex
     * then the RaftActor does not change it's state and it returns a failure.
     */
    @Test
    void testHandleAppendEntriesSenderPrevLogTermNotSameAsReceiverPrevLogTerm() {
        logStart("testHandleAppendEntriesSenderPrevLogTermNotSameAsReceiverPrevLogTerm");

        MockRaftActorContext context = createActorContext();

        AppendEntries appendEntries = new AppendEntries(2, "leader", 0, 2, List.of(), 101, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        assertSame(follower, newBehavior);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        assertFalse("isSuccess", reply.isSuccess());
    }

    @Test
    void testHandleAppendEntriesSenderPrevLogIndexIsInTheSnapshot() {
        logStart("testHandleAppendEntriesSenderPrevLogIndexIsInTheSnapshot");

        MockRaftActorContext context = createActorContext();
        final var log = new MockRaftActorContext.Builder().createEntries(5, 8, 3).build();
        log.setSnapshotIndex(4);
        log.setSnapshotTerm(3);
        context.resetReplicatedLog(log);

        AppendEntries appendEntries = new AppendEntries(3, "leader", 1, 3, List.of(), 8, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        assertSame(follower, newBehavior);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertTrue("isSuccess", reply.isSuccess());
    }

    /**
     * This test verifies that when a new AppendEntries message is received with
     * new entries and the logs of the sender and receiver match that the new
     * entries get added to the log and the log is incremented by the number of
     * entries received in appendEntries.
     */
    @Test
    void testHandleAppendEntriesAddNewEntries() {
        logStart("testHandleAppendEntriesAddNewEntries");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.setTermInfo(new TermInfo(1, "test"));

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newLogEntry(1, 0, "zero"));
        log.append(newLogEntry(1, 1, "one"));
        log.append(newLogEntry(1, 2, "two"));

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        final var entries = List.of(newLogEntry(1, 3, "three"), newLogEntry(1, 4, "four"));

        // Send appendEntries with the same term as was set on the receiver
        // before the new behavior was created (1 in this case)
        // This will not work for a Candidate because as soon as a Candidate
        // is created it increments the term
        short leaderPayloadVersion = 10;
        String leaderId = "leader-1";
        AppendEntries appendEntries = new AppendEntries(1, leaderId, 2, 1, entries, 4, -1, leaderPayloadVersion);

        follower = createBehavior(context);

        assertSame(follower, follower.handleMessage(leaderActor, appendEntries));

        assertEquals(4, log.lastIndex());
        assertLogEntry(entries.get(0), log.lookup(3));
        assertLogEntry(entries.get(1), log.lookup(4));

        assertEquals(leaderPayloadVersion, follower.getLeaderPayloadVersion());
        assertEquals(leaderId, follower.getLeaderId());

        expectAndVerifyAppendEntriesReply(1, true, "follower", 1, 4);
    }

    /**
     * This test verifies that when a new AppendEntries message is received with
     * new entries and the logs of the sender and receiver are out-of-sync that
     * the log is first corrected by removing the out of sync entries from the
     * log and then adding in the new entries sent with the AppendEntries message.
     */
    @Test
    void testHandleAppendEntriesCorrectReceiverLogEntries() {
        logStart("testHandleAppendEntriesCorrectReceiverLogEntries");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.setTermInfo(new TermInfo(1, "test"));

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newLogEntry(1, 0, "zero"));
        log.append(newLogEntry(1, 1, "one"));
        log.append(newLogEntry(1, 2, "two"));

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        final var entries = List.of(newLogEntry(2, 2, "two-1"), newLogEntry(2, 3, "three"));

        // Send appendEntries with the same term as was set on the receiver
        // before the new behavior was created (1 in this case)
        // This will not work for a Candidate because as soon as a Candidate
        // is created it increments the term
        AppendEntries appendEntries = new AppendEntries(2, "leader", 1, 1, entries, 3, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        assertSame(follower, newBehavior);

        // The entry at index 2 will be found out-of-sync with the leader and will be removed
        // Then the two new entries will be added to the log
        // Thus making the log to have 4 entries
        assertEquals("Next index", 3, log.lastIndex());
        //assertEquals("Entry 2", entries.get(0), log.get(2));

        assertEquals("Entry 1 data", "one", log.lookup(1).command().toString());

        // Check that the entry at index 2 has the new data
        assertLogEntry(entries.get(0), log.lookup(2));

        assertLogEntry(entries.get(1), log.lookup(3));

        expectAndVerifyAppendEntriesReply(2, true, "follower", 2, 3);
    }

    @Test
    void testHandleAppendEntriesWhenOutOfSyncLogDetectedRequestForceInstallSnapshot() {
        logStart("testHandleAppendEntriesWhenOutOfSyncLogDetectedRequestForceInstallSnapshot");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.setTermInfo(new TermInfo(1, "test"));

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newLogEntry(1, 0, "zero"));
        log.append(newLogEntry(1, 1, "one"));
        log.append(newLogEntry(1, 2, "two"));

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        final var entries = List.of(newLogEntry(2, 2, "two-1"), newLogEntry(2, 3, "three"));

        // Send appendEntries with the same term as was set on the receiver
        // before the new behavior was created (1 in this case)
        // This will not work for a Candidate because as soon as a Candidate
        // is created it increments the term
        AppendEntries appendEntries = new AppendEntries(2, "leader", 1, 1, entries, 3, -1, (short)0);

        context.setRaftPolicy(createRaftPolicy(false, true));
        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        assertSame(follower, newBehavior);

        expectAndVerifyAppendEntriesReply(2, false, "follower", 1, 2, true);
    }

    @Test
    void testHandleAppendEntriesPreviousLogEntryMissing() {
        logStart("testHandleAppendEntriesPreviousLogEntryMissing");

        final MockRaftActorContext context = createActorContext();

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newLogEntry(1, 0, "zero"));
        log.append(newLogEntry(1, 1, "one"));
        log.append(newLogEntry(1, 2, "two"));

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        final var entries = List.of(newLogEntry(1, 4, "four"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", 3, 1, entries, 4, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        assertSame(follower, newBehavior);

        expectAndVerifyAppendEntriesReply(1, false, "follower", 1, 2);
    }

    @Test
    void testHandleAppendEntriesWithExistingLogEntry() {
        logStart("testHandleAppendEntriesWithExistingLogEntry");

        MockRaftActorContext context = createActorContext();

        context.setTermInfo(new TermInfo(1, "test"));

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newLogEntry(1, 0, "zero"));
        log.append(newLogEntry(1, 1, "one"));

        context.resetReplicatedLog(log);

        // Send the last entry again.
        var entries = List.of(newLogEntry(1, 1, "one"));

        follower = createBehavior(context);

        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", 0, 1, entries, 1, -1, (short)0));

        assertEquals(1, log.lastIndex());
        assertLogEntry(entries.getFirst(), log.lookup(1));

        expectAndVerifyAppendEntriesReply(1, true, "follower", 1, 1);

        // Send the last entry again and also a new one.

        entries = List.of(newLogEntry(1, 1, "one"), newLogEntry(1, 2, "two"));

        MessageCollectorActor.clearMessages(leaderActor);
        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", 0, 1, entries, 2, -1, (short)0));

        assertEquals(3, log.last().index() + 1);
        assertLogEntry(entries.get(0), log.lookup(1));
        assertLogEntry(entries.get(1), log.lookup(2));

        expectAndVerifyAppendEntriesReply(1, true, "follower", 1, 2);
    }

    @Test
    void testHandleAppendEntriesAfterInstallingSnapshot() {
        logStart("testHandleAppendAfterInstallingSnapshot");

        MockRaftActorContext context = createActorContext();

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();

        // Set up a log as if it has been snapshotted
        log.setSnapshotIndex(3);
        log.setSnapshotTerm(1);

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        final var entries = List.of(newLogEntry(1, 4, "four"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", 3, 1, entries, 4, 3, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        assertSame(follower, newBehavior);

        expectAndVerifyAppendEntriesReply(1, true, "follower", 1, 4);
    }

    /**
     * This test verifies that when InstallSnapshot is received by
     * the follower its applied correctly.
     */
    @Test
    void testHandleInstallSnapshot() throws Exception {
        logStart("testHandleInstallSnapshot");

        MockRaftActorContext context = createActorContext();
        context.setTermInfo(new TermInfo(1, "leader"));

        follower = createBehavior(context);

        ByteString bsSnapshot = createSnapshot();
        int offset = 0;
        int snapshotLength = bsSnapshot.size();
        int chunkSize = 50;
        int totalChunks = snapshotLength / chunkSize + (snapshotLength % chunkSize > 0 ? 1 : 0);
        int lastIncludedIndex = 1;
        int chunkIndex = 1;
        InstallSnapshot lastInstallSnapshot = null;

        for (int i = 0; i < totalChunks; i++) {
            byte[] chunkData = getNextChunk(bsSnapshot, offset, chunkSize);
            lastInstallSnapshot = new InstallSnapshot(1, "leader", lastIncludedIndex, 1,
                    chunkData, chunkIndex, totalChunks);
            follower.handleMessage(leaderActor, lastInstallSnapshot);
            offset = offset + 50;
            lastIncludedIndex++;
            chunkIndex++;
        }
        assertNotNull(lastInstallSnapshot);

        final var applySnapshot = MessageCollectorActor.expectFirstMatching(followerActor, ApplyLeaderSnapshot.class);

        assertEquals("getLastIndex", lastInstallSnapshot.getLastIncludedIndex(), applySnapshot.lastEntry().index());
        assertEquals("getLastIncludedTerm", lastInstallSnapshot.getLastIncludedTerm(),
            applySnapshot.lastEntry().term());
        assertEquals("getLastTerm", lastInstallSnapshot.getLastIncludedTerm(), lastInstallSnapshot.getTerm());
        assertArrayEquals("getState", bsSnapshot.toByteArray(),
            applySnapshot.snapshot().io().openStream().readAllBytes());
        applySnapshot.callback().onSuccess();

        final var replies = MessageCollectorActor.getAllMatching(leaderActor, InstallSnapshotReply.class);
        assertEquals("InstallSnapshotReply count", totalChunks, replies.size());

        chunkIndex = 1;
        for (var reply : replies) {
            assertEquals("getChunkIndex", chunkIndex++, reply.getChunkIndex());
            assertEquals("getTerm", 1, reply.getTerm());
            assertTrue("isSuccess", reply.isSuccess());
            assertEquals("getFollowerId", "follower", reply.getFollowerId());
        }

        assertNull("Expected null SnapshotTracker", follower.getSnapshotTracker());
    }

    /**
     * Verify that when an AppendEntries is sent to a follower during a snapshot install
     * the Follower short-circuits the processing of the AppendEntries message.
     */
    @Test
    void testReceivingAppendEntriesDuringInstallSnapshot() {
        logStart("testReceivingAppendEntriesDuringInstallSnapshot");

        MockRaftActorContext context = createActorContext();

        follower = createBehavior(context);

        ByteString bsSnapshot  = createSnapshot();
        int snapshotLength = bsSnapshot.size();
        int chunkSize = 50;
        int totalChunks = snapshotLength / chunkSize + (snapshotLength % chunkSize > 0 ? 1 : 0);
        int lastIncludedIndex = 1;

        // Check that snapshot installation is not in progress
        assertNull(follower.getSnapshotTracker());

        // Make sure that we have more than 1 chunk to send
        assertTrue(totalChunks > 1);

        // Send an install snapshot with the first chunk to start the process of installing a snapshot
        byte[] chunkData = getNextChunk(bsSnapshot, 0, chunkSize);
        follower.handleMessage(leaderActor, new InstallSnapshot(1, "leader", lastIncludedIndex, 1,
                chunkData, 1, totalChunks));

        // Check if snapshot installation is in progress now
        assertNotNull(follower.getSnapshotTracker());

        // Send an append entry
        AppendEntries appendEntries = new AppendEntries(1, "leader", 1, 1,
                List.of(newLogEntry(2, 1, "3")), 2, -1, (short)1);

        follower.handleMessage(leaderActor, appendEntries);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());
        assertEquals("getLogLastIndex", context.getReplicatedLog().lastIndex(), reply.getLogLastIndex());
        assertEquals("getLogLastTerm", context.getReplicatedLog().lastTerm(), reply.getLogLastTerm());
        assertEquals("getTerm", context.currentTerm(), reply.getTerm());

        assertNotNull(follower.getSnapshotTracker());
    }

    @Test
    void testReceivingAppendEntriesDuringInstallSnapshotFromDifferentLeader() {
        logStart("testReceivingAppendEntriesDuringInstallSnapshotFromDifferentLeader");

        MockRaftActorContext context = createActorContext();

        follower = createBehavior(context);

        ByteString bsSnapshot  = createSnapshot();
        int snapshotLength = bsSnapshot.size();
        int chunkSize = 50;
        int totalChunks = snapshotLength / chunkSize + (snapshotLength % chunkSize > 0 ? 1 : 0);
        int lastIncludedIndex = 1;

        // Check that snapshot installation is not in progress
        assertNull(follower.getSnapshotTracker());

        // Make sure that we have more than 1 chunk to send
        assertTrue(totalChunks > 1);

        // Send an install snapshot with the first chunk to start the process of installing a snapshot
        byte[] chunkData = getNextChunk(bsSnapshot, 0, chunkSize);
        follower.handleMessage(leaderActor, new InstallSnapshot(1, "leader", lastIncludedIndex, 1,
                chunkData, 1, totalChunks));

        // Check if snapshot installation is in progress now
        assertNotNull(follower.getSnapshotTracker());

        // Send appendEntries with a new term and leader.
        AppendEntries appendEntries = new AppendEntries(2, "new-leader", 1, 1,
                List.of(newLogEntry(2, 2, "3")), 2, -1, (short)1);

        follower.handleMessage(leaderActor, appendEntries);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());
        assertEquals("getLogLastIndex", 2, reply.getLogLastIndex());
        assertEquals("getLogLastTerm", 2, reply.getLogLastTerm());
        assertEquals("getTerm", 2, reply.getTerm());

        assertNull(follower.getSnapshotTracker());
    }

    @Test
    void testInitialSyncUpWithHandleInstallSnapshotFollowedByAppendEntries() {
        logStart("testInitialSyncUpWithHandleInstallSnapshot");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().setCommitIndex(-1);

        follower = createBehavior(context);

        ByteString bsSnapshot  = createSnapshot();
        int offset = 0;
        int snapshotLength = bsSnapshot.size();
        int chunkSize = 50;
        int totalChunks = snapshotLength / chunkSize + (snapshotLength % chunkSize > 0 ? 1 : 0);
        int lastIncludedIndex = 1;
        int chunkIndex = 1;
        InstallSnapshot lastInstallSnapshot = null;

        for (int i = 0; i < totalChunks; i++) {
            byte[] chunkData = getNextChunk(bsSnapshot, offset, chunkSize);
            lastInstallSnapshot = new InstallSnapshot(1, "leader", lastIncludedIndex, 1,
                    chunkData, chunkIndex, totalChunks);
            follower.handleMessage(leaderActor, lastInstallSnapshot);
            offset = offset + 50;
            lastIncludedIndex++;
            chunkIndex++;
        }

        FollowerInitialSyncUpStatus syncStatus =
                MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.initialSyncDone());

        // Clear all the messages
        MessageCollectorActor.clearMessages(followerActor);

        final var log = setLastLogEntry(context, 1, 101, new MockCommand(""));
        log.setLastApplied(101);
        log.setCommitIndex(101);

        final var entries = List.of(newLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader", 101, 1, entries, 102, 101, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertTrue(syncStatus.initialSyncDone());
    }

    @Test
    void testHandleOutOfSequenceInstallSnapshot() {
        logStart("testHandleOutOfSequenceInstallSnapshot");

        MockRaftActorContext context = createActorContext();

        follower = createBehavior(context);

        ByteString bsSnapshot = createSnapshot();

        InstallSnapshot installSnapshot = new InstallSnapshot(1, "leader", 3, 1,
                getNextChunk(bsSnapshot, 10, 50), 3, 3);
        follower.handleMessage(leaderActor, installSnapshot);

        InstallSnapshotReply reply = MessageCollectorActor.expectFirstMatching(leaderActor,
                InstallSnapshotReply.class);

        assertFalse("isSuccess", reply.isSuccess());
        assertEquals("getChunkIndex", -1, reply.getChunkIndex());
        assertEquals("getTerm", 1, reply.getTerm());
        assertEquals("getFollowerId", "follower", reply.getFollowerId());

        assertNull("Expected null SnapshotTracker", follower.getSnapshotTracker());
    }

    @Test
    void testFollowerSchedulesElectionTimeoutImmediatelyWhenItHasNoPeers() {
        MockRaftActorContext context = createActorContext();

        Stopwatch stopwatch = Stopwatch.createStarted();

        follower = createBehavior(context);

        TimeoutNow timeoutNow = MessageCollectorActor.expectFirstMatching(followerActor, TimeoutNow.class);

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertTrue(elapsed < context.getConfigParams().getElectionTimeOutInterval().toMillis());

        RaftActorBehavior newBehavior = follower.handleMessage(ActorRef.noSender(), timeoutNow);
        assertTrue("Expected Candidate", newBehavior instanceof Candidate);
    }

    @Test
    void testFollowerSchedulesElectionIfAutomaticElectionsAreDisabled() {
        MockRaftActorContext context = createActorContext();
        context.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public Duration getElectionTimeOutInterval() {
                return Duration.ofMillis(100);
            }
        });

        context.setRaftPolicy(createRaftPolicy(false, false));

        follower = createBehavior(context);

        TimeoutNow timeoutNow = MessageCollectorActor.expectFirstMatching(followerActor, TimeoutNow.class);
        RaftActorBehavior newBehavior = follower.handleMessage(ActorRef.noSender(), timeoutNow);
        assertSame("handleMessage result", follower, newBehavior);
    }

    @Test
    void testFollowerSchedulesElectionIfNonVoting() {
        MockRaftActorContext context = createActorContext();
        context.updateVotingConfig(new VotingConfig(new ServerInfo("follower", false)));
        ((DefaultConfigParamsImpl) context.getConfigParams()).setHeartBeatInterval(Duration.ofMillis(100));
        ((DefaultConfigParamsImpl) context.getConfigParams()).setElectionTimeoutFactor(1);

        follower = new Follower(context, "leader", (short)1);

        ElectionTimeout electionTimeout = MessageCollectorActor.expectFirstMatching(followerActor,
                ElectionTimeout.class);
        RaftActorBehavior newBehavior = follower.handleMessage(ActorRef.noSender(), electionTimeout);
        assertSame("handleMessage result", follower, newBehavior);
        assertNull("Expected null leaderId", follower.getLeaderId());
    }

    @Test
    // TODO: parameterized with all possible RaftRPCs
    void testElectionScheduledWhenAnyRaftRPCReceived() {
        MockRaftActorContext context = createActorContext();
        follower = createBehavior(context);
        follower.handleMessage(leaderActor, new RequestVoteReply(100, false));
        verify(follower).scheduleElection(any());
    }

    @Test
    void testElectionNotScheduledWhenNonRaftRPCMessageReceived() {
        MockRaftActorContext context = createActorContext();
        follower = createBehavior(context);
        follower.handleMessage(leaderActor, "non-raft-rpc");
        verify(follower, never()).scheduleElection(any());
    }

    @Test
    void testCaptureSnapshotOnLastEntryInAppendEntries() throws Exception {
        String id = "testCaptureSnapshotOnLastEntryInAppendEntries";
        logStart(id);

        new PropertiesTermInfoStore(id, stateDir.resolve(id)).storeAndSetTerm(new TermInfo(1));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setSnapshotBatchCount(2);
        config.setRaftPolicy(WellKnownRaftPolicy.DISABLE_ELECTIONS);

        final var followerRaftActorRef = new AtomicReference<MockRaftActor>();
        final var snapshotCohort = newRaftActorSnapshotCohort(followerRaftActorRef);
        Builder builder = MockRaftActor.builder().persistent(Optional.of(Boolean.TRUE)).id(id)
                .peerAddresses(Map.of("leader", "")).config(config).snapshotCohort(snapshotCohort);
        TestActorRef<MockRaftActor> followerActorRef = actorFactory.createTestActor(builder.props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), id);

        final var followerRaftActor = followerActorRef.underlyingActor();
        followerRaftActorRef.set(followerRaftActor);
        followerRaftActor.waitForInitializeBehaviorComplete();

        assertNull(followerRaftActor.lastSnapshot());

        final var entries = List.of(newLogEntry(1, 0, "one"), newLogEntry(1, 1, "two"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", -1, -1, entries, 1, -1, (short)0);

        followerActorRef.tell(appendEntries, leaderActor);

        final var snapshotFile = awaitSnapshot(followerRaftActor);

        final var raftSnapshot = snapshotFile.readRaftSnapshot(OBJECT_STREAMS);
        assertEquals(List.of(), raftSnapshot.unappliedEntries());
        assertEquals(EntryInfo.of(1, 1), snapshotFile.lastIncluded());
        assertEquals("Snapshot state", List.of(entries.get(0).command(), entries.get(1).command()),
            MockRaftActor.fromState(snapshotFile.readSnapshot(MockSnapshotState.SUPPORT.reader())));

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());

        // TODO: do we need a sync here?
        final var journal = followerRaftActor.entryJournal();
        assertNotNull(journal);
        // Apply including the second entry ...
        assertEquals(2, journal.applyToJournalIndex());
        // ... but the journal is empty, as everything is included in the snapshot
        try (var reader = journal.openReader()) {
            assertEquals(3, reader.nextJournalIndex());
            assertNull(reader.nextEntry());
        }
    }

    @Test
    void testCaptureSnapshotOnMiddleEntryInAppendEntries() throws Exception {
        String id = "testCaptureSnapshotOnMiddleEntryInAppendEntries";
        logStart(id);

        new PropertiesTermInfoStore(id, stateDir.resolve(id)).storeAndSetTerm(new TermInfo(1));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setSnapshotBatchCount(2);
        config.setRaftPolicy(WellKnownRaftPolicy.DISABLE_ELECTIONS);

        final var followerRaftActorRef = new AtomicReference<MockRaftActor>();
        final var snapshotCohort = newRaftActorSnapshotCohort(followerRaftActorRef);
        Builder builder = MockRaftActor.builder().persistent(Optional.of(Boolean.TRUE)).id(id)
                .peerAddresses(Map.of("leader", "")).config(config).snapshotCohort(snapshotCohort);
        TestActorRef<MockRaftActor> followerActorRef = actorFactory.createTestActor(builder.props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        final var followerRaftActor = followerActorRef.underlyingActor();
        followerRaftActorRef.set(followerRaftActor);
        followerRaftActor.waitForInitializeBehaviorComplete();

        assertNull(followerRaftActor.lastSnapshot());

        final var entries = List.of(newLogEntry(1, 0, "one"), newLogEntry(1, 1, "two"), newLogEntry(1, 2, "three"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", -1, -1, entries, 2, -1, (short)0);

        followerActorRef.tell(appendEntries, leaderActor);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());

        final var snapshotFile = awaitSnapshot(followerRaftActor);
        final var raftSnapshot = snapshotFile.readRaftSnapshot(OBJECT_STREAMS);
        assertEquals(List.of(), raftSnapshot.unappliedEntries());
        assertEquals(EntryInfo.of(2, 1), snapshotFile.lastIncluded());
        assertEquals(List.of(entries.get(0).command(), entries.get(1).command(), entries.get(2).command()),
            MockRaftActor.fromState(snapshotFile.readSnapshot(MockSnapshotState.SUPPORT.reader())));

        // TODO: do we need a sync here?
        final var journal = followerRaftActor.entryJournal();
        assertNotNull(journal);

        assertEquals(3, journal.applyToJournalIndex());
        try (var reader = journal.openReader()) {
            assertEquals(4, reader.nextJournalIndex());
            assertNull(reader.nextEntry());
        }

        var followerLog = followerRaftActorRef.get().getRaftActorContext().getReplicatedLog();
        assertEquals("Journal size", 0, followerLog.size());
        assertEquals("Snapshot index", 2, followerLog.getSnapshotIndex());

        // Reinstate the actor from persistence

        actorFactory.killActor(followerActorRef, new TestKit(getSystem()));

        followerActorRef = actorFactory.createTestActor(builder.props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        followerRaftActorRef.set(followerActorRef.underlyingActor());
        followerRaftActorRef.get().waitForInitializeBehaviorComplete();

        followerLog = followerRaftActorRef.get().getRaftActorContext().getReplicatedLog();
        assertEquals("Journal size", 0, followerLog.size());
        assertEquals("Last index", 2, followerLog.lastIndex());
        assertEquals("Last applied index", 2, followerLog.getLastApplied());
        assertEquals("Commit index", 2, followerLog.getCommitIndex());
        assertEquals("State", List.of(entries.get(0).command(), entries.get(1).command(), entries.get(2).command()),
            followerRaftActorRef.get().getState());
    }

    @Test
    void testCaptureSnapshotOnAppendEntriesWithUnapplied() throws Exception {
        String id = "testCaptureSnapshotOnAppendEntriesWithUnapplied";
        logStart(id);

        new PropertiesTermInfoStore(id, stateDir.resolve(id)).storeAndSetTerm(new TermInfo(1));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setSnapshotBatchCount(1);
        config.setRaftPolicy(WellKnownRaftPolicy.DISABLE_ELECTIONS);

        final var followerRaftActorRef = new AtomicReference<MockRaftActor>();
        final var snapshotCohort = newRaftActorSnapshotCohort(followerRaftActorRef);
        Builder builder = MockRaftActor.builder().persistent(Optional.of(Boolean.TRUE)).id(id)
                .peerAddresses(Map.of("leader", "")).config(config).snapshotCohort(snapshotCohort);
        TestActorRef<MockRaftActor> followerActorRef = actorFactory.createTestActor(builder.props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        final var followerRaftActor = followerActorRef.underlyingActor();
        followerRaftActorRef.set(followerRaftActor);
        followerRaftActor.waitForInitializeBehaviorComplete();

        final var entries = List.of(newLogEntry(1, 0, "one"), newLogEntry(1, 1, "two"), newLogEntry(1, 2, "three"));

        final var appendEntries = new AppendEntries(1, "leader", -1, -1, entries, 0, -1, (short)0);

        followerActorRef.tell(appendEntries, leaderActor);

        final var reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());

        final var snapshotFile = awaitSnapshot(followerRaftActor);
        final var raftSnapshot = snapshotFile.readRaftSnapshot(OBJECT_STREAMS);

        assertEquals(List.of(), raftSnapshot.unappliedEntries());
        assertEquals("Snapshot getLastAppliedTerm", EntryInfo.of(0, 1), snapshotFile.lastIncluded());
        assertEquals("Snapshot state", List.of(entries.getFirst().command()),
                MockRaftActor.fromState(snapshotFile.readSnapshot(MockSnapshotState.SUPPORT.reader())));

        // TODO: do we need a sync here?
        final var journal = followerActorRef.underlyingActor().entryJournal();
        assertNotNull(journal);
        assertEquals(1, journal.applyToJournalIndex());

        try (var reader = journal.openReader()) {
            assertEquals(2, reader.nextJournalIndex());
            final var first = reader.nextEntry();
            assertNotNull(first);
            assertEquals(1, first.index());
            assertEquals(1, first.term());

            assertEquals(3, reader.nextJournalIndex());
            final var second = reader.nextEntry();
            assertNotNull(second);
            assertEquals(2, second.index());
            assertEquals(1, second.term());

            assertEquals(4, reader.nextJournalIndex());
            assertNull(reader.nextEntry());
        }
    }

    @Test
    void testNeedsLeaderAddress() {
        logStart("testNeedsLeaderAddress");

        MockRaftActorContext context = createActorContext();
        context.resetReplicatedLog(new MockRaftActorContext.SimpleReplicatedLog());
        context.addToPeers("leader", null, VotingState.VOTING);
        ((DefaultConfigParamsImpl)context.getConfigParams()).setPeerAddressResolver(NoopPeerAddressResolver.INSTANCE);

        follower = createBehavior(context);

        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", -1, -1, List.of(), -1, -1, (short)0));

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue(reply.isNeedsLeaderAddress());
        MessageCollectorActor.clearMessages(leaderActor);

        PeerAddressResolver mockResolver = mock(PeerAddressResolver.class);
        ((DefaultConfigParamsImpl)context.getConfigParams()).setPeerAddressResolver(mockResolver);

        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", -1, -1, List.of(), -1, -1,
                (short)0, RaftVersions.CURRENT_VERSION, leaderActor.path().toString()));

        reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertFalse(reply.isNeedsLeaderAddress());

        verify(mockResolver).setResolved("leader", leaderActor.path().toString());
    }

    private static MockRaftActorSnapshotCohort newRaftActorSnapshotCohort(
            final AtomicReference<MockRaftActor> followerRaftActor) {
        return () -> new MockSnapshotState(followerRaftActor.get().getState());
    }

    public byte[] getNextChunk(final ByteString bs, final int offset, final int chunkSize) {
        int snapshotLength = bs.size();
        int start = offset;
        int size = chunkSize;
        if (chunkSize > snapshotLength) {
            size = snapshotLength;
        } else if (start + chunkSize > snapshotLength) {
            size = snapshotLength - start;
        }

        byte[] nextChunk = new byte[size];
        bs.copyTo(nextChunk, start, 0, size);
        return nextChunk;
    }

    private void expectAndVerifyAppendEntriesReply(final int expTerm, final boolean expSuccess,
            final String expFollowerId, final long expLogLastTerm, final long expLogLastIndex) {
        expectAndVerifyAppendEntriesReply(expTerm, expSuccess, expFollowerId, expLogLastTerm, expLogLastIndex, false);
    }

    private void expectAndVerifyAppendEntriesReply(final int expTerm, final boolean expSuccess,
            final String expFollowerId, final long expLogLastTerm, final long expLogLastIndex,
            final boolean expForceInstallSnapshot) {

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        assertEquals("isSuccess", expSuccess, reply.isSuccess());
        assertEquals("getTerm", expTerm, reply.getTerm());
        assertEquals("getFollowerId", expFollowerId, reply.getFollowerId());
        assertEquals("getLogLastTerm", expLogLastTerm, reply.getLogLastTerm());
        assertEquals("getLogLastIndex", expLogLastIndex, reply.getLogLastIndex());
        assertEquals("getPayloadVersion", ourPayloadVersion, reply.getPayloadVersion());
        assertEquals("isForceInstallSnapshot", expForceInstallSnapshot, reply.isForceInstallSnapshot());
        assertFalse("isNeedsLeaderAddress", reply.isNeedsLeaderAddress());
    }

    @NonNullByDefault
    private static LogEntry newLogEntry(final long term, final long index, final String data) {
        return new DefaultLogEntry(index, term, new MockCommand(data));
    }

    private ByteString createSnapshot() {
        return toByteString(Map.of("1", "A", "2", "B", "3", "C"));
    }

    @Override
    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(final MockRaftActorContext actorContext,
            final ActorRef actorRef, final RaftRPC rpc) {
        super.assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, actorRef, rpc);

        final var expVotedFor = rpc instanceof RequestVote rv ? rv.getCandidateId() : null;
        assertEquals("New votedFor", expVotedFor, actorContext.termInfo().votedFor());
    }

    @Override
    protected void handleAppendEntriesAddSameEntryToLogReply(final ActorRef replyActor) {
        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(replyActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());
    }
}
