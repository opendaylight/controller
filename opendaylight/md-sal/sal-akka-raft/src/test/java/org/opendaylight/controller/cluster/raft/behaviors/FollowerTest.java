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

import com.google.common.base.Stopwatch;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.protobuf.ByteString;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActor;
import org.opendaylight.controller.cluster.raft.MockRaftActor.Builder;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.MockRaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.MockSnapshotState;
import org.opendaylight.controller.cluster.raft.NoopPeerAddressResolver;
import org.opendaylight.controller.cluster.raft.PeerAddressResolver;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
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
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class FollowerTest extends AbstractRaftActorBehaviorTest<Follower> {
    private final short ourPayloadVersion = 5;

    private final ActorRef followerActor = actorFactory.createActor(
            MessageCollectorActor.props(), actorFactory.generateActorId("follower"));
    private final ActorRef leaderActor = actorFactory.createActor(
            MessageCollectorActor.props(), actorFactory.generateActorId("leader"));

    private Follower follower;

    @Override
    @After
    public void tearDown() {
        if (follower != null) {
            follower.close();
        }

        super.tearDown();
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
        MockRaftActorContext context = new MockRaftActorContext("follower", getSystem(), actorRef, payloadVersion);
        ((DefaultConfigParamsImpl) context.getConfigParams()).setPeerAddressResolver(
            peerId -> leaderActor.path().toString());
        return context;
    }

    @Test
    public void testThatAnElectionTimeoutIsTriggered() {
        MockRaftActorContext actorContext = createActorContext();
        follower = new Follower(actorContext);

        MessageCollectorActor.expectFirstMatching(followerActor, TimeoutNow.class,
                actorContext.getConfigParams().getElectionTimeOutInterval().multipliedBy(6).toMillis());
    }

    @Test
    public void testHandleElectionTimeoutWhenNoLeaderMessageReceived() {
        logStart("testHandleElectionTimeoutWhenNoLeaderMessageReceived");

        MockRaftActorContext context = createActorContext();
        follower = new Follower(context);

        Uninterruptibles.sleepUninterruptibly(context.getConfigParams().getElectionTimeOutInterval().toMillis(),
                TimeUnit.MILLISECONDS);
        RaftActorBehavior raftBehavior = follower.handleMessage(leaderActor, ElectionTimeout.INSTANCE);

        assertTrue(raftBehavior instanceof Candidate);
    }

    @Test
    public void testHandleElectionTimeoutWhenLeaderMessageReceived() {
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
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNull() {
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
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNotTheSameAsCandidateId() {
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
    public void testHandleFirstAppendEntries() {
        logStart("testHandleFirstAppendEntries");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().clear(0,2);
        context.getReplicatedLog().append(newReplicatedLogEntry(1,100, "bar"));
        context.getReplicatedLog().setSnapshotIndex(99);

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

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
    public void testHandleFirstAppendEntriesWithPrevIndexMinusOne() {
        logStart("testHandleFirstAppendEntries");

        MockRaftActorContext context = createActorContext();

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

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
    public void testHandleFirstAppendEntriesWithPrevIndexMinusOneAndReplicatedToAllIndexPresentInLog() {
        logStart("testHandleFirstAppendEntriesWithPrevIndexMinusOneAndReplicatedToAllIndexPresentInLog");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().clear(0, 2);
        context.getReplicatedLog().append(newReplicatedLogEntry(1, 100, "bar"));
        context.getReplicatedLog().setSnapshotIndex(99);

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

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
    public void testHandleFirstAppendEntriesWithPrevIndexMinusOneAndReplicatedToAllIndexPresentInSnapshot() {
        logStart("testHandleFirstAppendEntriesWithPrevIndexMinusOneAndReplicatedToAllIndexPresentInSnapshot");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().clear(0, 2);
        context.getReplicatedLog().setSnapshotIndex(100);

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

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
    public void testFirstAppendEntriesWithNoPrevIndexAndReplToAllPresentInSnapshotButCalculatedPrevEntryMissing() {
        logStart(
               "testFirstAppendEntriesWithNoPrevIndexAndReplicatedToAllPresentInSnapshotButCalculatedPrevEntryMissing");

        MockRaftActorContext context = createActorContext();
        context.getReplicatedLog().clear(0, 2);
        context.getReplicatedLog().setSnapshotIndex(100);

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 105, "foo"));

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
    public void testHandleSyncUpAppendEntries() {
        logStart("testHandleSyncUpAppendEntries");

        MockRaftActorContext context = createActorContext();

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.initialSyncDone());

        // Clear all the messages
        MessageCollectorActor.clearMessages(followerActor);

        final var log = setLastLogEntry(context, 1, 101, new MockRaftActorContext.MockPayload(""));
        log.setLastApplied(101);
        log.setCommitIndex(101);

        entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

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
    public void testHandleAppendEntriesLeaderChangedBeforeSyncUpComplete() {
        logStart("testHandleAppendEntriesLeaderChangedBeforeSyncUpComplete");

        MockRaftActorContext context = createActorContext();

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

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
        setLastLogEntry(context, 1, 100, new MockRaftActorContext.MockPayload(""));

        entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

        // leader-2 is becoming the leader now and it says the commitIndex is 45
        appendEntries = new AppendEntries(2, "leader-2", 45, 1, entries, 46, 100, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        // We get a new message saying initial status is not done
        assertFalse(syncStatus.initialSyncDone());
    }

    @Test
    public void testHandleAppendEntriesLeaderChangedAfterSyncUpComplete() {
        logStart("testHandleAppendEntriesLeaderChangedAfterSyncUpComplete");

        MockRaftActorContext context = createActorContext();

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor,
                FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.initialSyncDone());

        // Clear all the messages
        MessageCollectorActor.clearMessages(followerActor);

        var log = setLastLogEntry(context, 1, 101, new MockRaftActorContext.MockPayload(""));
        log.setLastApplied(101);
        log.setCommitIndex(101);

        entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        appendEntries = new AppendEntries(2, "leader-1", 101, 1, entries, 102, 101, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertTrue(syncStatus.initialSyncDone());

        // Clear all the messages
        MessageCollectorActor.clearMessages(followerActor);

        log = setLastLogEntry(context, 1, 100, new MockRaftActorContext.MockPayload(""));
        log.setLastApplied(100);

        entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

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
    public void testHandleAppendEntriesWithNewerCommitIndex() {
        logStart("testHandleAppendEntriesWithNewerCommitIndex");

        final var context = createActorContext();

        setLastLogEntry(context, 1, 100, new MockRaftActorContext.MockPayload(""));
        final var log = context.getReplicatedLog();
        log.setLastApplied(100);
        log.setSnapshotIndex(99);

        final var entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

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
    public void testHandleAppendEntriesSenderPrevLogTermNotSameAsReceiverPrevLogTerm() {
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
    public void testHandleAppendEntriesSenderPrevLogIndexIsInTheSnapshot() {
        logStart("testHandleAppendEntriesSenderPrevLogIndexIsInTheSnapshot");

        MockRaftActorContext context = createActorContext();
        final var log = new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(5, 8, 3).build();
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
    public void testHandleAppendEntriesAddNewEntries() {
        logStart("testHandleAppendEntriesAddNewEntries");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.setTermInfo(new TermInfo(1, "test"));

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));
        log.append(newReplicatedLogEntry(1, 2, "two"));

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = List.of(
            newReplicatedLogEntry(1, 3, "three"), newReplicatedLogEntry(1, 4, "four"));

        // Send appendEntries with the same term as was set on the receiver
        // before the new behavior was created (1 in this case)
        // This will not work for a Candidate because as soon as a Candidate
        // is created it increments the term
        short leaderPayloadVersion = 10;
        String leaderId = "leader-1";
        AppendEntries appendEntries = new AppendEntries(1, leaderId, 2, 1, entries, 4, -1, leaderPayloadVersion);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        assertSame(follower, newBehavior);

        assertEquals("Next index", 4, log.lastIndex());
        assertEquals("Entry 3", entries.get(0), log.get(3));
        assertEquals("Entry 4", entries.get(1), log.get(4));

        assertEquals("getLeaderPayloadVersion", leaderPayloadVersion, newBehavior.getLeaderPayloadVersion());
        assertEquals("getLeaderId", leaderId, newBehavior.getLeaderId());

        expectAndVerifyAppendEntriesReply(1, true, "follower", 1, 4);
    }

    /**
     * This test verifies that when a new AppendEntries message is received with
     * new entries and the logs of the sender and receiver are out-of-sync that
     * the log is first corrected by removing the out of sync entries from the
     * log and then adding in the new entries sent with the AppendEntries message.
     */
    @Test
    public void testHandleAppendEntriesCorrectReceiverLogEntries() {
        logStart("testHandleAppendEntriesCorrectReceiverLogEntries");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.setTermInfo(new TermInfo(1, "test"));

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));
        log.append(newReplicatedLogEntry(1, 2, "two"));

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = List.of(
            newReplicatedLogEntry(2, 2, "two-1"), newReplicatedLogEntry(2, 3, "three"));

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

        assertEquals("Entry 1 data", "one", log.get(1).getData().toString());

        // Check that the entry at index 2 has the new data
        assertEquals("Entry 2", entries.get(0), log.get(2));

        assertEquals("Entry 3", entries.get(1), log.get(3));

        expectAndVerifyAppendEntriesReply(2, true, "follower", 2, 3);
    }

    @Test
    public void testHandleAppendEntriesWhenOutOfSyncLogDetectedRequestForceInstallSnapshot() {
        logStart("testHandleAppendEntriesWhenOutOfSyncLogDetectedRequestForceInstallSnapshot");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.setTermInfo(new TermInfo(1, "test"));

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));
        log.append(newReplicatedLogEntry(1, 2, "two"));

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = List.of(
            newReplicatedLogEntry(2, 2, "two-1"), newReplicatedLogEntry(2, 3, "three"));

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
    public void testHandleAppendEntriesPreviousLogEntryMissing() {
        logStart("testHandleAppendEntriesPreviousLogEntryMissing");

        final MockRaftActorContext context = createActorContext();

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));
        log.append(newReplicatedLogEntry(1, 2, "two"));

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(1, 4, "four"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", 3, 1, entries, 4, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        assertSame(follower, newBehavior);

        expectAndVerifyAppendEntriesReply(1, false, "follower", 1, 2);
    }

    @Test
    public void testHandleAppendEntriesWithExistingLogEntry() {
        logStart("testHandleAppendEntriesWithExistingLogEntry");

        MockRaftActorContext context = createActorContext();

        context.setTermInfo(new TermInfo(1, "test"));

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));

        context.resetReplicatedLog(log);

        // Send the last entry again.
        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(1, 1, "one"));

        follower = createBehavior(context);

        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", 0, 1, entries, 1, -1, (short)0));

        assertEquals("Next index", 1, log.lastIndex());
        assertEquals("Entry 1", entries.get(0), log.get(1));

        expectAndVerifyAppendEntriesReply(1, true, "follower", 1, 1);

        // Send the last entry again and also a new one.

        entries = List.of(newReplicatedLogEntry(1, 1, "one"), newReplicatedLogEntry(1, 2, "two"));

        MessageCollectorActor.clearMessages(leaderActor);
        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", 0, 1, entries, 2, -1, (short)0));

        assertEquals("Next index", 3, log.last().index() + 1);
        assertEquals("Entry 1", entries.get(0), log.get(1));
        assertEquals("Entry 2", entries.get(1), log.get(2));

        expectAndVerifyAppendEntriesReply(1, true, "follower", 1, 2);
    }

    @Test
    public void testHandleAppendEntriesAfterInstallingSnapshot() {
        logStart("testHandleAppendAfterInstallingSnapshot");

        MockRaftActorContext context = createActorContext();

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();

        // Set up a log as if it has been snapshotted
        log.setSnapshotIndex(3);
        log.setSnapshotTerm(1);

        context.resetReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(1, 4, "four"));

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
    public void testHandleInstallSnapshot() throws Exception {
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
        assertArrayEquals("getState", bsSnapshot.toByteArray(), applySnapshot.snapshot().read());
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
    public void testReceivingAppendEntriesDuringInstallSnapshot() {
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
                List.of(newReplicatedLogEntry(2, 1, "3")), 2, -1, (short)1);

        follower.handleMessage(leaderActor, appendEntries);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());
        assertEquals("getLogLastIndex", context.getReplicatedLog().lastIndex(), reply.getLogLastIndex());
        assertEquals("getLogLastTerm", context.getReplicatedLog().lastTerm(), reply.getLogLastTerm());
        assertEquals("getTerm", context.currentTerm(), reply.getTerm());

        assertNotNull(follower.getSnapshotTracker());
    }

    @Test
    public void testReceivingAppendEntriesDuringInstallSnapshotFromDifferentLeader() {
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
                List.of(newReplicatedLogEntry(2, 2, "3")), 2, -1, (short)1);

        follower.handleMessage(leaderActor, appendEntries);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());
        assertEquals("getLogLastIndex", 2, reply.getLogLastIndex());
        assertEquals("getLogLastTerm", 2, reply.getLogLastTerm());
        assertEquals("getTerm", 2, reply.getTerm());

        assertNull(follower.getSnapshotTracker());
    }

    @Test
    public void testInitialSyncUpWithHandleInstallSnapshotFollowedByAppendEntries() {
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

        final var log = setLastLogEntry(context, 1, 101, new MockRaftActorContext.MockPayload(""));
        log.setLastApplied(101);
        log.setCommitIndex(101);

        List<ReplicatedLogEntry> entries = List.of(newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader", 101, 1, entries, 102, 101, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertTrue(syncStatus.initialSyncDone());
    }

    @Test
    public void testHandleOutOfSequenceInstallSnapshot() {
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
    public void testFollowerSchedulesElectionTimeoutImmediatelyWhenItHasNoPeers() {
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
    public void testFollowerSchedulesElectionIfAutomaticElectionsAreDisabled() {
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
    public void testFollowerSchedulesElectionIfNonVoting() {
        MockRaftActorContext context = createActorContext();
        context.updatePeerIds(new ClusterConfig(new ServerInfo("follower", false)));
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
    public void testElectionScheduledWhenAnyRaftRPCReceived() {
        MockRaftActorContext context = createActorContext();
        follower = createBehavior(context);
        follower.handleMessage(leaderActor, new RequestVoteReply(100, false));
        verify(follower).scheduleElection(any());
    }

    @Test
    public void testElectionNotScheduledWhenNonRaftRPCMessageReceived() {
        MockRaftActorContext context = createActorContext();
        follower = createBehavior(context);
        follower.handleMessage(leaderActor, "non-raft-rpc");
        verify(follower, never()).scheduleElection(any());
    }

    @Test
    public void testCaptureSnapshotOnLastEntryInAppendEntries() {
        String id = "testCaptureSnapshotOnLastEntryInAppendEntries";
        logStart(id);

        InMemoryJournal.addEntry(id, 1, new UpdateElectionTerm(1, null));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setSnapshotBatchCount(2);
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final var followerRaftActor = new AtomicReference<MockRaftActor>();
        final var snapshotCohort = newRaftActorSnapshotCohort(followerRaftActor);
        Builder builder = MockRaftActor.builder().persistent(Optional.of(Boolean.TRUE)).id(id)
                .peerAddresses(Map.of("leader", "")).config(config).snapshotCohort(snapshotCohort);
        TestActorRef<MockRaftActor> followerActorRef = actorFactory.createTestActor(builder.props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        followerRaftActor.set(followerActorRef.underlyingActor());
        followerRaftActor.get().waitForInitializeBehaviorComplete();

        InMemorySnapshotStore.addSnapshotSavedLatch(id);
        InMemoryJournal.addDeleteMessagesCompleteLatch(id);
        InMemoryJournal.addWriteMessagesCompleteLatch(id, 1, ApplyJournalEntries.class);

        List<ReplicatedLogEntry> entries = List.of(
                newReplicatedLogEntry(1, 0, "one"), newReplicatedLogEntry(1, 1, "two"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", -1, -1, entries, 1, -1, (short)0);

        followerActorRef.tell(appendEntries, leaderActor);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());

        final Snapshot snapshot = InMemorySnapshotStore.waitForSavedSnapshot(id, Snapshot.class);

        InMemoryJournal.waitForDeleteMessagesComplete(id);
        InMemoryJournal.waitForWriteMessagesComplete(id);
        // We expect the ApplyJournalEntries for index 1 to remain in the persisted log b/c it's still queued for
        // persistence by the time we initiate capture so the last persisted journal sequence number doesn't include it.
        // This is OK - on recovery it will be a no-op since index 1 has already been applied.
        List<Object> journalEntries = InMemoryJournal.get(id, Object.class);
        assertEquals("Persisted journal entries size: " + journalEntries, 1, journalEntries.size());
        assertEquals("Persisted journal entry type", ApplyJournalEntries.class, journalEntries.get(0).getClass());
        assertEquals("ApplyJournalEntries index", 1, ((ApplyJournalEntries)journalEntries.get(0)).getToIndex());

        assertEquals("Snapshot unapplied size", 0, snapshot.getUnAppliedEntries().size());
        assertEquals("Snapshot getLastAppliedTerm", 1, snapshot.getLastAppliedTerm());
        assertEquals("Snapshot getLastAppliedIndex", 1, snapshot.getLastAppliedIndex());
        assertEquals("Snapshot getLastTerm", 1, snapshot.getLastTerm());
        assertEquals("Snapshot getLastIndex", 1, snapshot.getLastIndex());
        assertEquals("Snapshot state", List.of(entries.get(0).getData(), entries.get(1).getData()),
                MockRaftActor.fromState(snapshot.getState()));
    }

    @Test
    public void testCaptureSnapshotOnMiddleEntryInAppendEntries() {
        String id = "testCaptureSnapshotOnMiddleEntryInAppendEntries";
        logStart(id);

        InMemoryJournal.addEntry(id, 1, new UpdateElectionTerm(1, null));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setSnapshotBatchCount(2);
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final var followerRaftActor = new AtomicReference<MockRaftActor>();
        final var snapshotCohort = newRaftActorSnapshotCohort(followerRaftActor);
        Builder builder = MockRaftActor.builder().persistent(Optional.of(Boolean.TRUE)).id(id)
                .peerAddresses(Map.of("leader", "")).config(config).snapshotCohort(snapshotCohort);
        TestActorRef<MockRaftActor> followerActorRef = actorFactory.createTestActor(builder.props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        followerRaftActor.set(followerActorRef.underlyingActor());
        followerRaftActor.get().waitForInitializeBehaviorComplete();

        InMemorySnapshotStore.addSnapshotSavedLatch(id);
        InMemoryJournal.addDeleteMessagesCompleteLatch(id);
        InMemoryJournal.addWriteMessagesCompleteLatch(id, 1, ApplyJournalEntries.class);

        List<ReplicatedLogEntry> entries = List.of(
                newReplicatedLogEntry(1, 0, "one"), newReplicatedLogEntry(1, 1, "two"),
                newReplicatedLogEntry(1, 2, "three"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", -1, -1, entries, 2, -1, (short)0);

        followerActorRef.tell(appendEntries, leaderActor);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());

        final Snapshot snapshot = InMemorySnapshotStore.waitForSavedSnapshot(id, Snapshot.class);

        InMemoryJournal.waitForDeleteMessagesComplete(id);
        InMemoryJournal.waitForWriteMessagesComplete(id);
        // We expect the ApplyJournalEntries for index 2 to remain in the persisted log b/c it's still queued for
        // persistence by the time we initiate capture so the last persisted journal sequence number doesn't include it.
        // This is OK - on recovery it will be a no-op since index 2 has already been applied.
        final var journalEntries = InMemoryJournal.get(id, Object.class);
        assertEquals("Persisted journal entries size: " + journalEntries, 1, journalEntries.size());
        assertEquals("Persisted journal entry type", ApplyJournalEntries.class, journalEntries.get(0).getClass());
        assertEquals("ApplyJournalEntries index", 2, ((ApplyJournalEntries)journalEntries.get(0)).getToIndex());

        assertEquals("Snapshot unapplied size", 0, snapshot.getUnAppliedEntries().size());
        assertEquals("Snapshot getLastAppliedTerm", 1, snapshot.getLastAppliedTerm());
        assertEquals("Snapshot getLastAppliedIndex", 2, snapshot.getLastAppliedIndex());
        assertEquals("Snapshot getLastTerm", 1, snapshot.getLastTerm());
        assertEquals("Snapshot getLastIndex", 2, snapshot.getLastIndex());
        assertEquals("Snapshot state", List.of(entries.get(0).getData(), entries.get(1).getData(),
                entries.get(2).getData()), MockRaftActor.fromState(snapshot.getState()));

        assertEquals("Journal size", 0, followerRaftActor.get().getReplicatedLog().size());
        assertEquals("Snapshot index", 2, followerRaftActor.get().getReplicatedLog().getSnapshotIndex());

        // Reinstate the actor from persistence

        actorFactory.killActor(followerActorRef, new TestKit(getSystem()));

        followerActorRef = actorFactory.createTestActor(builder.props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        followerRaftActor.set(followerActorRef.underlyingActor());
        followerRaftActor.get().waitForInitializeBehaviorComplete();

        final var followerLog = followerRaftActor.get().getReplicatedLog();
        assertEquals("Journal size", 0, followerLog.size());
        assertEquals("Last index", 2, followerLog.lastIndex());
        assertEquals("Last applied index", 2, followerLog.getLastApplied());
        assertEquals("Commit index", 2, followerLog.getCommitIndex());
        assertEquals("State", List.of(entries.get(0).getData(), entries.get(1).getData(), entries.get(2).getData()),
            followerRaftActor.get().getState());
    }

    @Test
    public void testCaptureSnapshotOnAppendEntriesWithUnapplied() {
        String id = "testCaptureSnapshotOnAppendEntriesWithUnapplied";
        logStart(id);

        InMemoryJournal.addEntry(id, 1, new UpdateElectionTerm(1, null));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setSnapshotBatchCount(1);
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        final var followerRaftActor = new AtomicReference<MockRaftActor>();
        final var snapshotCohort = newRaftActorSnapshotCohort(followerRaftActor);
        Builder builder = MockRaftActor.builder().persistent(Optional.of(Boolean.TRUE)).id(id)
                .peerAddresses(Map.of("leader", "")).config(config).snapshotCohort(snapshotCohort);
        TestActorRef<MockRaftActor> followerActorRef = actorFactory.createTestActor(builder.props(stateDir())
                .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        followerRaftActor.set(followerActorRef.underlyingActor());
        followerRaftActor.get().waitForInitializeBehaviorComplete();

        InMemorySnapshotStore.addSnapshotSavedLatch(id);
        InMemoryJournal.addDeleteMessagesCompleteLatch(id);
        InMemoryJournal.addWriteMessagesCompleteLatch(id, 1, ApplyJournalEntries.class);

        List<ReplicatedLogEntry> entries = List.of(
                newReplicatedLogEntry(1, 0, "one"), newReplicatedLogEntry(1, 1, "two"),
                newReplicatedLogEntry(1, 2, "three"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", -1, -1, entries, 0, -1, (short)0);

        followerActorRef.tell(appendEntries, leaderActor);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());

        final Snapshot snapshot = InMemorySnapshotStore.waitForSavedSnapshot(id, Snapshot.class);

        InMemoryJournal.waitForDeleteMessagesComplete(id);
        InMemoryJournal.waitForWriteMessagesComplete(id);
        // We expect the ApplyJournalEntries for index 0 to remain in the persisted log b/c it's still queued for
        // persistence by the time we initiate capture so the last persisted journal sequence number doesn't include it.
        // This is OK - on recovery it will be a no-op since index 0 has already been applied.
        final var journalEntries = InMemoryJournal.get(id, Object.class);
        assertEquals("Persisted journal entries size: " + journalEntries, 1, journalEntries.size());
        assertEquals("Persisted journal entry type", ApplyJournalEntries.class, journalEntries.get(0).getClass());
        assertEquals("ApplyJournalEntries index", 0, ((ApplyJournalEntries)journalEntries.get(0)).getToIndex());

        assertEquals("Snapshot unapplied size", 2, snapshot.getUnAppliedEntries().size());
        assertEquals("Snapshot unapplied entry index", 1, snapshot.getUnAppliedEntries().get(0).index());
        assertEquals("Snapshot unapplied entry index", 2, snapshot.getUnAppliedEntries().get(1).index());
        assertEquals("Snapshot getLastAppliedTerm", 1, snapshot.getLastAppliedTerm());
        assertEquals("Snapshot getLastAppliedIndex", 0, snapshot.getLastAppliedIndex());
        assertEquals("Snapshot getLastTerm", 1, snapshot.getLastTerm());
        assertEquals("Snapshot getLastIndex", 2, snapshot.getLastIndex());
        assertEquals("Snapshot state", List.of(entries.get(0).getData()),
                MockRaftActor.fromState(snapshot.getState()));
    }

    @Test
    public void testNeedsLeaderAddress() {
        logStart("testNeedsLeaderAddress");

        MockRaftActorContext context = createActorContext();
        context.resetReplicatedLog(new MockRaftActorContext.SimpleReplicatedLog());
        context.addToPeers("leader", null, VotingState.VOTING);
        ((DefaultConfigParamsImpl)context.getConfigParams()).setPeerAddressResolver(NoopPeerAddressResolver.INSTANCE);

        follower = createBehavior(context);

        follower.handleMessage(leaderActor,
                new AppendEntries(1, "leader", -1, -1, List.of(), -1, -1, (short)0));

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
        final var snapshotCohort = new MockRaftActorSnapshotCohort() {
            @Override
            public MockSnapshotState takeSnapshot() {
                return new MockSnapshotState(followerRaftActor.get().getState());
            }

            @Override
            public void createSnapshot(final ActorRef actorRef, final OutputStream installSnapshotStream) {
                actorRef.tell(new CaptureSnapshotReply(takeSnapshot(), installSnapshotStream), actorRef);
            }

            @Override
            public void applySnapshot(final MockSnapshotState snapshotState) {
                // No-op
            }

            @Override
            public MockSnapshotState deserializeSnapshot(final ByteSource snapshotBytes) {
                throw new UnsupportedOperationException();
            }
        };
        return snapshotCohort;
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


    private static ReplicatedLogEntry newReplicatedLogEntry(final long term, final long index, final String data) {
        return new SimpleReplicatedLogEntry(index, term, new MockRaftActorContext.MockPayload(data));
    }

    private ByteString createSnapshot() {
        return toByteString(Map.of("1", "A", "2", "B", "3", "C"));
    }

    @Override
    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(final MockRaftActorContext actorContext,
            final ActorRef actorRef, final RaftRPC rpc) {
        super.assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, actorRef, rpc);

        String expVotedFor = rpc instanceof RequestVote ? ((RequestVote)rpc).getCandidateId() : null;
        assertEquals("New votedFor", expVotedFor, actorContext.termInfo().votedFor());
    }

    @Override
    protected void handleAppendEntriesAddSameEntryToLogReply(final ActorRef replyActor) {
        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(replyActor, AppendEntriesReply.class);
        assertTrue("isSuccess", reply.isSuccess());
    }
}
