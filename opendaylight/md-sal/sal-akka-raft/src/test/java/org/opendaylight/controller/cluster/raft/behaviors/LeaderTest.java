/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.protobuf.ByteString;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.messaging.MessageSlice;
import org.opendaylight.controller.cluster.messaging.MessageSliceReply;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorLeadershipTransferCohort;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader.SnapshotHolder;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.policy.DefaultRaftPolicy;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.ForwardMessageToBehaviorActor;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.yangtools.concepts.Identifier;
import scala.concurrent.duration.FiniteDuration;

public class LeaderTest extends AbstractLeaderTest<Leader> {

    static final String FOLLOWER_ID = "follower";
    public static final String LEADER_ID = "leader";

    private final TestActorRef<ForwardMessageToBehaviorActor> leaderActor = actorFactory.createTestActor(
            Props.create(ForwardMessageToBehaviorActor.class), actorFactory.generateActorId("leader"));

    private final TestActorRef<ForwardMessageToBehaviorActor> followerActor = actorFactory.createTestActor(
            Props.create(ForwardMessageToBehaviorActor.class), actorFactory.generateActorId("follower"));

    private Leader leader;
    private final short payloadVersion = 5;

    @Override
    @After
    public void tearDown() {
        if (leader != null) {
            leader.close();
        }

        super.tearDown();
    }

    @Test
    public void testHandleMessageForUnknownMessage() {
        logStart("testHandleMessageForUnknownMessage");

        leader = new Leader(createActorContext());

        // handle message should null when it receives an unknown message
        assertNull(leader.handleMessage(followerActor, "foo"));
    }

    @Test
    public void testThatLeaderSendsAHeartbeatMessageToAllFollowers() {
        logStart("testThatLeaderSendsAHeartbeatMessageToAllFollowers");

        MockRaftActorContext actorContext = createActorContextWithFollower();
        actorContext.setCommitIndex(-1);
        actorContext.setPayloadVersion(payloadVersion);

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);
        actorContext.setCurrentBehavior(leader);

        // Leader should send an immediate heartbeat with no entries as follower is inactive.
        final long lastIndex = actorContext.getReplicatedLog().lastIndex();
        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getTerm", term, appendEntries.getTerm());
        assertEquals("getPrevLogIndex", -1, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", -1, appendEntries.getPrevLogTerm());
        assertEquals("Entries size", 0, appendEntries.getEntries().size());
        assertEquals("getPayloadVersion", payloadVersion, appendEntries.getPayloadVersion());

        // The follower would normally reply - simulate that explicitly here.
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex - 1, term, (short)0));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        // Sleep for the heartbeat interval so AppendEntries is sent.
        Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams()
                .getHeartBeatInterval().toMillis(), TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getPrevLogIndex", lastIndex - 1, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", term, appendEntries.getPrevLogTerm());
        assertEquals("Entries size", 1, appendEntries.getEntries().size());
        assertEquals("Entry getIndex", lastIndex, appendEntries.getEntries().get(0).getIndex());
        assertEquals("Entry getTerm", term, appendEntries.getEntries().get(0).getTerm());
        assertEquals("getPayloadVersion", payloadVersion, appendEntries.getPayloadVersion());
    }


    private RaftActorBehavior sendReplicate(final MockRaftActorContext actorContext, final long index) {
        return sendReplicate(actorContext, 1, index);
    }

    private RaftActorBehavior sendReplicate(final MockRaftActorContext actorContext, final long term,
            final long index) {
        return sendReplicate(actorContext, term, index, new MockRaftActorContext.MockPayload("foo"));
    }

    private RaftActorBehavior sendReplicate(final MockRaftActorContext actorContext, final long term, final long index,
            final Payload payload) {
        SimpleReplicatedLogEntry newEntry = new SimpleReplicatedLogEntry(index, term, payload);
        actorContext.getReplicatedLog().append(newEntry);
        return leader.handleMessage(leaderActor, new Replicate(null, null, newEntry, true));
    }

    @Test
    public void testHandleReplicateMessageSendAppendEntriesToFollower() {
        logStart("testHandleReplicateMessageSendAppendEntriesToFollower");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower would normally reply - simulate that explicitly here.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex, term, (short)0));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        RaftActorBehavior raftBehavior = sendReplicate(actorContext, lastIndex + 1);

        // State should not change
        assertTrue(raftBehavior instanceof Leader);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getPrevLogIndex", lastIndex, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", term, appendEntries.getPrevLogTerm());
        assertEquals("Entries size", 1, appendEntries.getEntries().size());
        assertEquals("Entry getIndex", lastIndex + 1, appendEntries.getEntries().get(0).getIndex());
        assertEquals("Entry getTerm", term, appendEntries.getEntries().get(0).getTerm());
        assertEquals("Entry payload", "foo", appendEntries.getEntries().get(0).getData().toString());
        assertEquals("Commit Index", lastIndex, actorContext.getCommitIndex());
    }

    @Test
    public void testHandleReplicateMessageWithHigherTermThanPreviousEntry() {
        logStart("testHandleReplicateMessageWithHigherTermThanPreviousEntry");

        MockRaftActorContext actorContext = createActorContextWithFollower();
        actorContext.setCommitIndex(-1);
        actorContext.setLastApplied(-1);

        // The raft context is initialized with a couple log entries. However the commitIndex
        // is -1, simulating that the leader previously didn't get consensus and thus the log entries weren't
        // committed and applied. Now it regains leadership with a higher term (2).
        long prevTerm = actorContext.getTermInformation().getCurrentTerm();
        long newTerm = prevTerm + 1;
        actorContext.getTermInformation().update(newTerm, "");

        leader = new Leader(actorContext);
        actorContext.setCurrentBehavior(leader);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower replies with the leader's current last index and term, simulating that it is
        // up to date with the leader.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, newTerm, true, lastIndex, prevTerm, (short)0));

        // The commit index should not get updated even though consensus was reached. This is b/c the
        // last entry's term does match the current term. As per §5.4.1, "Raft never commits log entries
        // from previous terms by counting replicas".
        assertEquals("Commit Index", -1, actorContext.getCommitIndex());

        followerActor.underlyingActor().clear();

        // Now replicate a new entry with the new term 2.
        long newIndex = lastIndex + 1;
        sendReplicate(actorContext, newTerm, newIndex);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getPrevLogIndex", lastIndex, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", prevTerm, appendEntries.getPrevLogTerm());
        assertEquals("Entries size", 1, appendEntries.getEntries().size());
        assertEquals("Entry getIndex", newIndex, appendEntries.getEntries().get(0).getIndex());
        assertEquals("Entry getTerm", newTerm, appendEntries.getEntries().get(0).getTerm());
        assertEquals("Entry payload", "foo", appendEntries.getEntries().get(0).getData().toString());

        // The follower replies with success. The leader should now update the commit index to the new index
        // as per §5.4.1 "once an entry from the current term is committed by counting replicas, then all
        // prior entries are committed indirectly".
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, newTerm, true, newIndex, newTerm, (short)0));

        assertEquals("Commit Index", newIndex, actorContext.getCommitIndex());
    }

    @Test
    public void testHandleReplicateMessageCommitIndexIncrementedBeforeConsensus() {
        logStart("testHandleReplicateMessageCommitIndexIncrementedBeforeConsensus");

        MockRaftActorContext actorContext = createActorContextWithFollower();
        actorContext.setRaftPolicy(createRaftPolicy(true, true));

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower would normally reply - simulate that explicitly here.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex, term, (short) 0));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        RaftActorBehavior raftBehavior = sendReplicate(actorContext, lastIndex + 1);

        // State should not change
        assertTrue(raftBehavior instanceof Leader);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getPrevLogIndex", lastIndex, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", term, appendEntries.getPrevLogTerm());
        assertEquals("Entries size", 1, appendEntries.getEntries().size());
        assertEquals("Entry getIndex", lastIndex + 1, appendEntries.getEntries().get(0).getIndex());
        assertEquals("Entry getTerm", term, appendEntries.getEntries().get(0).getTerm());
        assertEquals("Entry payload", "foo", appendEntries.getEntries().get(0).getData().toString());
        assertEquals("Commit Index", lastIndex + 1, actorContext.getCommitIndex());
    }

    @Test
    public void testMultipleReplicateShouldNotCauseDuplicateAppendEntriesToBeSent() {
        logStart("testHandleReplicateMessageSendAppendEntriesToFollower");

        MockRaftActorContext actorContext = createActorContextWithFollower();
        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public FiniteDuration getHeartBeatInterval() {
                return FiniteDuration.apply(5, TimeUnit.SECONDS);
            }
        });

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower would normally reply - simulate that explicitly here.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex, term, (short)0));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        for (int i = 0; i < 5; i++) {
            sendReplicate(actorContext, lastIndex + i + 1);
        }

        List<AppendEntries> allMessages = MessageCollectorActor.getAllMatching(followerActor, AppendEntries.class);
        // We expect only 1 message to be sent because of two reasons,
        // - an append entries reply was not received
        // - the heartbeat interval has not expired
        // In this scenario if multiple messages are sent they would likely be duplicates
        assertEquals("The number of append entries collected should be 1", 1, allMessages.size());
    }

    @Test
    public void testMultipleReplicateWithReplyShouldResultInAppendEntries() {
        logStart("testMultipleReplicateWithReplyShouldResultInAppendEntries");

        MockRaftActorContext actorContext = createActorContextWithFollower();
        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public FiniteDuration getHeartBeatInterval() {
                return FiniteDuration.apply(5, TimeUnit.SECONDS);
            }
        });

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower would normally reply - simulate that explicitly here.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex, term, (short)0));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        for (int i = 0; i < 3; i++) {
            sendReplicate(actorContext, lastIndex + i + 1);
            leader.handleMessage(followerActor, new AppendEntriesReply(
                    FOLLOWER_ID, term, true, lastIndex + i + 1, term, (short)0));
        }

        // We are expecting six messages here -- a request to replicate and a consensus-reached message
        List<AppendEntries> allMessages = MessageCollectorActor.getAllMatching(followerActor, AppendEntries.class);
        assertEquals("The number of request/consensus appends collected", 6, allMessages.size());
        for (int i = 0; i < 3; i++) {
            assertRequestEntry(lastIndex, allMessages, i);
            assertCommitEntry(lastIndex, allMessages, i);
        }

        // Now perform another commit, eliciting a request to persist
        sendReplicate(actorContext, lastIndex + 3 + 1);
        allMessages = MessageCollectorActor.getAllMatching(followerActor, AppendEntries.class);
        // This elicits another message for request to replicate
        assertEquals("The number of request entries collected", 7, allMessages.size());
        assertRequestEntry(lastIndex, allMessages, 3);

        sendReplicate(actorContext, lastIndex + 4 + 1);
        allMessages = MessageCollectorActor.getAllMatching(followerActor, AppendEntries.class);
        assertEquals("The number of request entries collected", 7, allMessages.size());
    }

    private static void assertCommitEntry(final long lastIndex, final List<AppendEntries> allMessages,
            final int messageNr) {
        final AppendEntries commitReq = allMessages.get(2 * messageNr + 1);
        assertEquals(lastIndex + messageNr + 1, commitReq.getLeaderCommit());
        assertEquals(List.of(), commitReq.getEntries());
    }

    private static void assertRequestEntry(final long lastIndex, final List<AppendEntries> allMessages,
            final int messageNr) {
        final AppendEntries req = allMessages.get(2 * messageNr);
        assertEquals(lastIndex + messageNr, req.getLeaderCommit());

        final List<ReplicatedLogEntry> entries = req.getEntries();
        assertEquals(1, entries.size());
        assertEquals(messageNr + 2, entries.get(0).getIndex());
    }

    @Test
    public void testDuplicateAppendEntriesWillBeSentOnHeartBeat() {
        logStart("testDuplicateAppendEntriesWillBeSentOnHeartBeat");

        MockRaftActorContext actorContext = createActorContextWithFollower();
        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public FiniteDuration getHeartBeatInterval() {
                return FiniteDuration.apply(500, TimeUnit.MILLISECONDS);
            }
        });

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower would normally reply - simulate that explicitly here.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex, term, (short)0));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        sendReplicate(actorContext, lastIndex + 1);

        // Wait slightly longer than heartbeat duration
        Uninterruptibles.sleepUninterruptibly(750, TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        List<AppendEntries> allMessages = MessageCollectorActor.getAllMatching(followerActor, AppendEntries.class);
        assertEquals("The number of append entries collected should be 2", 2, allMessages.size());

        assertEquals(1, allMessages.get(0).getEntries().size());
        assertEquals(lastIndex + 1, allMessages.get(0).getEntries().get(0).getIndex());
        assertEquals(1, allMessages.get(1).getEntries().size());
        assertEquals(lastIndex + 1, allMessages.get(0).getEntries().get(0).getIndex());

    }

    @Test
    public void testHeartbeatsAreAlwaysSentIfTheHeartbeatIntervalHasElapsed() {
        logStart("testHeartbeatsAreAlwaysSentIfTheHeartbeatIntervalHasElapsed");

        MockRaftActorContext actorContext = createActorContextWithFollower();
        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public FiniteDuration getHeartBeatInterval() {
                return FiniteDuration.apply(100, TimeUnit.MILLISECONDS);
            }
        });

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower would normally reply - simulate that explicitly here.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex, term, (short)0));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        for (int i = 0; i < 3; i++) {
            Uninterruptibles.sleepUninterruptibly(150, TimeUnit.MILLISECONDS);
            leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);
        }

        List<AppendEntries> allMessages = MessageCollectorActor.getAllMatching(followerActor, AppendEntries.class);
        assertEquals("The number of append entries collected should be 3", 3, allMessages.size());
    }

    @Test
    public void testSendingReplicateImmediatelyAfterHeartbeatDoesReplicate() {
        logStart("testSendingReplicateImmediatelyAfterHeartbeatDoesReplicate");

        MockRaftActorContext actorContext = createActorContextWithFollower();
        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public FiniteDuration getHeartBeatInterval() {
                return FiniteDuration.apply(100, TimeUnit.MILLISECONDS);
            }
        });

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower would normally reply - simulate that explicitly here.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex, term, (short)0));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        Uninterruptibles.sleepUninterruptibly(150, TimeUnit.MILLISECONDS);
        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);
        sendReplicate(actorContext, lastIndex + 1);

        List<AppendEntries> allMessages = MessageCollectorActor.getAllMatching(followerActor, AppendEntries.class);
        assertEquals("The number of append entries collected should be 2", 2, allMessages.size());

        assertEquals(0, allMessages.get(0).getEntries().size());
        assertEquals(1, allMessages.get(1).getEntries().size());
    }


    @Test
    public void testHandleReplicateMessageWhenThereAreNoFollowers() {
        logStart("testHandleReplicateMessageWhenThereAreNoFollowers");

        MockRaftActorContext actorContext = createActorContext();

        leader = new Leader(actorContext);

        actorContext.setLastApplied(0);

        long newLogIndex = actorContext.getReplicatedLog().lastIndex() + 1;
        long term = actorContext.getTermInformation().getCurrentTerm();
        ReplicatedLogEntry newEntry = new SimpleReplicatedLogEntry(
                newLogIndex, term, new MockRaftActorContext.MockPayload("foo"));

        actorContext.getReplicatedLog().append(newEntry);

        final Identifier id = new MockIdentifier("state-id");
        RaftActorBehavior raftBehavior = leader.handleMessage(leaderActor,
                new Replicate(leaderActor, id, newEntry, true));

        // State should not change
        assertTrue(raftBehavior instanceof Leader);

        assertEquals("getCommitIndex", newLogIndex, actorContext.getCommitIndex());

        // We should get 2 ApplyState messages - 1 for new log entry and 1 for the previous
        // one since lastApplied state is 0.
        List<ApplyState> applyStateList = MessageCollectorActor.getAllMatching(
                leaderActor, ApplyState.class);
        assertEquals("ApplyState count", newLogIndex, applyStateList.size());

        for (int i = 0; i <= newLogIndex - 1; i++) {
            ApplyState applyState = applyStateList.get(i);
            assertEquals("getIndex", i + 1, applyState.getReplicatedLogEntry().getIndex());
            assertEquals("getTerm", term, applyState.getReplicatedLogEntry().getTerm());
        }

        ApplyState last = applyStateList.get((int) newLogIndex - 1);
        assertEquals("getData", newEntry.getData(), last.getReplicatedLogEntry().getData());
        assertEquals("getIdentifier", id, last.getIdentifier());
    }

    @Test
    public void testSendAppendEntriesOnAnInProgressInstallSnapshot() throws Exception {
        logStart("testSendAppendEntriesOnAnInProgressInstallSnapshot");

        final MockRaftActorContext actorContext = createActorContextWithFollower();

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        final int commitIndex = 3;
        final int snapshotIndex = 2;
        final int snapshotTerm = 1;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.setCommitIndex(commitIndex);
        //set follower timeout to 2 mins, helps during debugging
        actorContext.setConfigParams(new MockConfigParamsImpl(120000L, 10));

        leader = new Leader(actorContext);

        leader.getFollower(FOLLOWER_ID).setMatchIndex(-1);
        leader.getFollower(FOLLOWER_ID).setNextIndex(0);

        //update follower timestamp
        leader.markFollowerActive(FOLLOWER_ID);

        ByteString bs = toByteString(Map.of("1", "A", "2", "B", "3", "C"));
        leader.setSnapshotHolder(new SnapshotHolder(Snapshot.create(ByteState.of(bs.toByteArray()),
                List.of(), commitIndex, snapshotTerm, commitIndex, snapshotTerm,
                -1, null, null), ByteSource.wrap(bs.toByteArray())));
        LeaderInstallSnapshotState fts = new LeaderInstallSnapshotState(
                actorContext.getConfigParams().getSnapshotChunkSize(), leader.logName());
        fts.setSnapshotBytes(ByteSource.wrap(bs.toByteArray()));
        leader.getFollower(FOLLOWER_ID).setLeaderInstallSnapshotState(fts);

        //send first chunk and no InstallSnapshotReply received yet
        fts.getNextChunk();
        fts.incrementChunkIndex();

        Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        AppendEntries ae = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertTrue("AppendEntries should be sent with empty entries", ae.getEntries().isEmpty());

        //InstallSnapshotReply received
        fts.markSendStatus(true);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        InstallSnapshot is = MessageCollectorActor.expectFirstMatching(followerActor, InstallSnapshot.class);

        assertEquals(commitIndex, is.getLastIncludedIndex());
    }

    @Test
    public void testSendAppendEntriesSnapshotScenario() {
        logStart("testSendAppendEntriesSnapshotScenario");

        final MockRaftActorContext actorContext = createActorContextWithFollower();

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int newEntryIndex = 4;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // new entry
        SimpleReplicatedLogEntry entry =
                new SimpleReplicatedLogEntry(newEntryIndex, currentTerm,
                        new MockRaftActorContext.MockPayload("D"));

        actorContext.getReplicatedLog().append(entry);

        //update follower timestamp
        leader.markFollowerActive(FOLLOWER_ID);

        // this should invoke a sendinstallsnapshot as followersLastIndex < snapshotIndex
        RaftActorBehavior raftBehavior = leader.handleMessage(
                leaderActor, new Replicate(null, new MockIdentifier("state-id"), entry, true));

        assertTrue(raftBehavior instanceof Leader);

        assertEquals("isCapturing", true, actorContext.getSnapshotManager().isCapturing());
    }

    @Test
    public void testInitiateInstallSnapshot() {
        logStart("testInitiateInstallSnapshot");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int newEntryIndex = 4;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.setLastApplied(3);
        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // set the snapshot as absent and check if capture-snapshot is invoked.
        leader.setSnapshotHolder(null);

        // new entry
        SimpleReplicatedLogEntry entry = new SimpleReplicatedLogEntry(newEntryIndex, currentTerm,
                new MockRaftActorContext.MockPayload("D"));

        actorContext.getReplicatedLog().append(entry);

        //update follower timestamp
        leader.markFollowerActive(FOLLOWER_ID);

        leader.handleMessage(leaderActor, new Replicate(null, new MockIdentifier("state-id"), entry, true));

        assertEquals("isCapturing", true, actorContext.getSnapshotManager().isCapturing());

        CaptureSnapshot cs = actorContext.getSnapshotManager().getCaptureSnapshot();

        assertEquals(3, cs.getLastAppliedIndex());
        assertEquals(1, cs.getLastAppliedTerm());
        assertEquals(4, cs.getLastIndex());
        assertEquals(2, cs.getLastTerm());

        // if an initiate is started again when first is in progress, it shouldnt initiate Capture
        leader.handleMessage(leaderActor, new Replicate(null, new MockIdentifier("state-id"), entry, true));

        assertSame("CaptureSnapshot instance", cs, actorContext.getSnapshotManager().getCaptureSnapshot());
    }

    @Test
    public void testInitiateForceInstallSnapshot() throws Exception {
        logStart("testInitiateForceInstallSnapshot");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int followersLastIndex = 2;
        final int snapshotIndex = -1;
        final int newEntryIndex = 4;
        final int snapshotTerm = -1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.setLastApplied(3);
        actorContext.setCommitIndex(followersLastIndex);

        actorContext.getReplicatedLog().removeFrom(0);

        AtomicReference<Optional<OutputStream>> installSnapshotStream = new AtomicReference<>();
        actorContext.setCreateSnapshotProcedure(installSnapshotStream::set);

        leader = new Leader(actorContext);
        actorContext.setCurrentBehavior(leader);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // set the snapshot as absent and check if capture-snapshot is invoked.
        leader.setSnapshotHolder(null);

        for (int i = 0; i < 4; i++) {
            actorContext.getReplicatedLog().append(new SimpleReplicatedLogEntry(i, 1,
                    new MockRaftActorContext.MockPayload("X" + i)));
        }

        // new entry
        SimpleReplicatedLogEntry entry = new SimpleReplicatedLogEntry(newEntryIndex, currentTerm,
                new MockRaftActorContext.MockPayload("D"));

        actorContext.getReplicatedLog().append(entry);

        //update follower timestamp
        leader.markFollowerActive(FOLLOWER_ID);

        // Sending this AppendEntriesReply forces the Leader to capture a snapshot, which subsequently gets
        // installed with a SendInstallSnapshot
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, false, 1, 1, (short) 1, true, false,
                RaftVersions.CURRENT_VERSION));

        assertEquals("isCapturing", true, actorContext.getSnapshotManager().isCapturing());

        CaptureSnapshot cs = actorContext.getSnapshotManager().getCaptureSnapshot();
        assertEquals(3, cs.getLastAppliedIndex());
        assertEquals(1, cs.getLastAppliedTerm());
        assertEquals(4, cs.getLastIndex());
        assertEquals(2, cs.getLastTerm());

        assertNotNull("Create snapshot procedure not invoked", installSnapshotStream.get());
        assertTrue("Install snapshot stream present", installSnapshotStream.get().isPresent());

        MessageCollectorActor.clearMessages(followerActor);

        // Sending Replicate message should not initiate another capture since the first is in progress.
        leader.handleMessage(leaderActor, new Replicate(null, new MockIdentifier("state-id"), entry, true));
        assertSame("CaptureSnapshot instance", cs, actorContext.getSnapshotManager().getCaptureSnapshot());

        // Similarly sending another AppendEntriesReply to force a snapshot should not initiate another capture.
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, false, 1, 1, (short) 1, true, false,
                RaftVersions.CURRENT_VERSION));
        assertSame("CaptureSnapshot instance", cs, actorContext.getSnapshotManager().getCaptureSnapshot());

        // Now simulate the CaptureSnapshotReply to initiate snapshot install - the first chunk should be sent.
        final byte[] bytes = new byte[]{1, 2, 3};
        installSnapshotStream.get().get().write(bytes);
        actorContext.getSnapshotManager().persist(ByteState.of(bytes), installSnapshotStream.get(),
                Runtime.getRuntime().totalMemory());
        MessageCollectorActor.expectFirstMatching(followerActor, InstallSnapshot.class);

        // Sending another AppendEntriesReply to force a snapshot should be a no-op and not try to re-send the chunk.
        MessageCollectorActor.clearMessages(followerActor);
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, false, 1, 1, (short) 1, true, false,
                RaftVersions.CURRENT_VERSION));
        MessageCollectorActor.assertNoneMatching(followerActor, InstallSnapshot.class, 200);
    }


    @Test
    public void testInstallSnapshot() {
        logStart("testInstallSnapshot");

        final MockRaftActorContext actorContext = createActorContextWithFollower();

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        final int lastAppliedIndex = 3;
        final int snapshotIndex = 2;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());
        actorContext.setCommitIndex(lastAppliedIndex);
        actorContext.setLastApplied(lastAppliedIndex);

        leader = new Leader(actorContext);

        // Initial heartbeat.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        leader.getFollower(FOLLOWER_ID).setMatchIndex(-1);
        leader.getFollower(FOLLOWER_ID).setNextIndex(0);

        byte[] bytes = toByteString(leadersSnapshot).toByteArray();
        Snapshot snapshot = Snapshot.create(ByteState.of(bytes), List.of(),
                lastAppliedIndex, snapshotTerm, lastAppliedIndex, snapshotTerm, -1, null, null);

        RaftActorBehavior raftBehavior = leader.handleMessage(leaderActor,
                new SendInstallSnapshot(snapshot, ByteSource.wrap(bytes)));

        assertTrue(raftBehavior instanceof Leader);

        // check if installsnapshot gets called with the correct values.

        InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor,
                InstallSnapshot.class);

        assertNotNull(installSnapshot.getData());
        assertEquals(lastAppliedIndex, installSnapshot.getLastIncludedIndex());
        assertEquals(snapshotTerm, installSnapshot.getLastIncludedTerm());

        assertEquals(currentTerm, installSnapshot.getTerm());
    }

    @Test
    public void testForceInstallSnapshot() {
        logStart("testForceInstallSnapshot");

        final MockRaftActorContext actorContext = createActorContextWithFollower();

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        final int lastAppliedIndex = 3;
        final int snapshotIndex = -1;
        final int snapshotTerm = -1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());
        actorContext.setCommitIndex(lastAppliedIndex);
        actorContext.setLastApplied(lastAppliedIndex);

        leader = new Leader(actorContext);

        // Initial heartbeat.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        leader.getFollower(FOLLOWER_ID).setMatchIndex(-1);
        leader.getFollower(FOLLOWER_ID).setNextIndex(-1);

        byte[] bytes = toByteString(leadersSnapshot).toByteArray();
        Snapshot snapshot = Snapshot.create(ByteState.of(bytes), List.of(),
                lastAppliedIndex, snapshotTerm, lastAppliedIndex, snapshotTerm, -1, null, null);

        RaftActorBehavior raftBehavior = leader.handleMessage(leaderActor,
                new SendInstallSnapshot(snapshot, ByteSource.wrap(bytes)));

        assertTrue(raftBehavior instanceof Leader);

        // check if installsnapshot gets called with the correct values.

        InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor,
                InstallSnapshot.class);

        assertNotNull(installSnapshot.getData());
        assertEquals(lastAppliedIndex, installSnapshot.getLastIncludedIndex());
        assertEquals(snapshotTerm, installSnapshot.getLastIncludedTerm());

        assertEquals(currentTerm, installSnapshot.getTerm());
    }

    @Test
    public void testHandleInstallSnapshotReplyLastChunk() throws Exception {
        logStart("testHandleInstallSnapshotReplyLastChunk");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int commitIndex = 3;
        final int snapshotIndex = 2;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        actorContext.setCommitIndex(commitIndex);

        leader = new Leader(actorContext);
        actorContext.setCurrentBehavior(leader);

        leader.getFollower(FOLLOWER_ID).setMatchIndex(-1);
        leader.getFollower(FOLLOWER_ID).setNextIndex(0);

        // Ignore initial heartbeat.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        // set the snapshot variables in replicatedlog

        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

        ByteString bs = toByteString(leadersSnapshot);
        leader.setSnapshotHolder(new SnapshotHolder(Snapshot.create(ByteState.of(bs.toByteArray()),
                List.of(), commitIndex, snapshotTerm, commitIndex, snapshotTerm,
                -1, null, null), ByteSource.wrap(bs.toByteArray())));
        LeaderInstallSnapshotState fts = new LeaderInstallSnapshotState(
                actorContext.getConfigParams().getSnapshotChunkSize(), leader.logName());
        fts.setSnapshotBytes(ByteSource.wrap(bs.toByteArray()));
        leader.getFollower(FOLLOWER_ID).setLeaderInstallSnapshotState(fts);
        while (!fts.isLastChunk(fts.getChunkIndex())) {
            fts.getNextChunk();
            fts.incrementChunkIndex();
        }

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        RaftActorBehavior raftBehavior = leader.handleMessage(followerActor,
                new InstallSnapshotReply(currentTerm, FOLLOWER_ID, fts.getChunkIndex(), true));

        assertTrue(raftBehavior instanceof Leader);

        assertEquals(1, leader.followerLogSize());
        FollowerLogInformation fli = leader.getFollower(FOLLOWER_ID);
        assertNotNull(fli);
        assertNull(fli.getInstallSnapshotState());
        assertEquals(commitIndex, fli.getMatchIndex());
        assertEquals(commitIndex + 1, fli.getNextIndex());
        assertFalse(leader.hasSnapshot());
    }

    @Test
    public void testSendSnapshotfromInstallSnapshotReply() {
        logStart("testSendSnapshotfromInstallSnapshotReply");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int commitIndex = 3;
        final int snapshotIndex = 2;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl() {
            @Override
            public int getSnapshotChunkSize() {
                return 50;
            }
        };
        configParams.setHeartBeatInterval(new FiniteDuration(9, TimeUnit.SECONDS));
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(10, TimeUnit.SECONDS));

        actorContext.setConfigParams(configParams);
        actorContext.setCommitIndex(commitIndex);

        leader = new Leader(actorContext);
        actorContext.setCurrentBehavior(leader);

        leader.getFollower(FOLLOWER_ID).setMatchIndex(-1);
        leader.getFollower(FOLLOWER_ID).setNextIndex(0);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

        ByteString bs = toByteString(leadersSnapshot);
        Snapshot snapshot = Snapshot.create(ByteState.of(bs.toByteArray()),
                List.of(), commitIndex, snapshotTerm, commitIndex, snapshotTerm, -1, null, null);

        leader.handleMessage(leaderActor, new SendInstallSnapshot(snapshot, ByteSource.wrap(bs.toByteArray())));

        InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor,
                InstallSnapshot.class);

        assertEquals(1, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());

        followerActor.underlyingActor().clear();
        leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                FOLLOWER_ID, installSnapshot.getChunkIndex(), true));

        installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor, InstallSnapshot.class);

        assertEquals(2, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());

        followerActor.underlyingActor().clear();
        leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                FOLLOWER_ID, installSnapshot.getChunkIndex(), true));

        installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor, InstallSnapshot.class);

        // Send snapshot reply one more time and make sure that a new snapshot message should not be sent to follower
        followerActor.underlyingActor().clear();
        leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                FOLLOWER_ID, installSnapshot.getChunkIndex(), true));

        installSnapshot = MessageCollectorActor.getFirstMatching(followerActor, InstallSnapshot.class);

        assertNull(installSnapshot);
    }


    @Test
    public void testHandleInstallSnapshotReplyWithInvalidChunkIndex() {
        logStart("testHandleInstallSnapshotReplyWithInvalidChunkIndex");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int commitIndex = 3;
        final int snapshotIndex = 2;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public int getSnapshotChunkSize() {
                return 50;
            }
        });

        actorContext.setCommitIndex(commitIndex);

        leader = new Leader(actorContext);

        leader.getFollower(FOLLOWER_ID).setMatchIndex(-1);
        leader.getFollower(FOLLOWER_ID).setNextIndex(0);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

        ByteString bs = toByteString(leadersSnapshot);
        Snapshot snapshot = Snapshot.create(ByteState.of(bs.toByteArray()),
                List.of(), commitIndex, snapshotTerm, commitIndex, snapshotTerm, -1, null, null);

        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        leader.handleMessage(leaderActor, new SendInstallSnapshot(snapshot, ByteSource.wrap(bs.toByteArray())));

        InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor,
                InstallSnapshot.class);

        assertEquals(1, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());

        followerActor.underlyingActor().clear();

        leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                FOLLOWER_ID, -1, false));

        Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor, InstallSnapshot.class);

        assertEquals(1, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());
    }

    @Test
    public void testHandleSnapshotSendsPreviousChunksHashCodeWhenSendingNextChunk() {
        logStart("testHandleSnapshotSendsPreviousChunksHashCodeWhenSendingNextChunk");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int commitIndex = 3;
        final int snapshotIndex = 2;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public int getSnapshotChunkSize() {
                return 50;
            }
        });

        actorContext.setCommitIndex(commitIndex);

        leader = new Leader(actorContext);

        leader.getFollower(FOLLOWER_ID).setMatchIndex(-1);
        leader.getFollower(FOLLOWER_ID).setNextIndex(0);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

        ByteString bs = toByteString(leadersSnapshot);
        Snapshot snapshot = Snapshot.create(ByteState.of(bs.toByteArray()),
                List.of(), commitIndex, snapshotTerm, commitIndex, snapshotTerm, -1, null, null);

        leader.handleMessage(leaderActor, new SendInstallSnapshot(snapshot, ByteSource.wrap(bs.toByteArray())));

        InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor,
                InstallSnapshot.class);

        assertEquals(1, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());
        assertEquals(LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE,
                installSnapshot.getLastChunkHashCode().getAsInt());

        final int hashCode = Arrays.hashCode(installSnapshot.getData());

        followerActor.underlyingActor().clear();

        leader.handleMessage(followerActor, new InstallSnapshotReply(installSnapshot.getTerm(),
                FOLLOWER_ID, 1, true));

        installSnapshot = MessageCollectorActor.expectFirstMatching(followerActor, InstallSnapshot.class);

        assertEquals(2, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());
        assertEquals(hashCode, installSnapshot.getLastChunkHashCode().getAsInt());
    }

    @Test
    public void testLeaderInstallSnapshotState() throws IOException {
        logStart("testLeaderInstallSnapshotState");

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        ByteString bs = toByteString(leadersSnapshot);
        byte[] barray = bs.toByteArray();

        LeaderInstallSnapshotState fts = new LeaderInstallSnapshotState(50, "test");
        fts.setSnapshotBytes(ByteSource.wrap(barray));

        assertEquals(bs.size(), barray.length);

        int chunkIndex = 0;
        for (int i = 0; i < barray.length; i = i + 50) {
            int length = i + 50;
            chunkIndex++;

            if (i + 50 > barray.length) {
                length = barray.length;
            }

            byte[] chunk = fts.getNextChunk();
            assertEquals("bytestring size not matching for chunk:" + chunkIndex, length - i, chunk.length);
            assertEquals("chunkindex not matching", chunkIndex, fts.getChunkIndex());

            fts.markSendStatus(true);
            if (!fts.isLastChunk(chunkIndex)) {
                fts.incrementChunkIndex();
            }
        }

        assertEquals("totalChunks not matching", chunkIndex, fts.getTotalChunks());
        fts.close();
    }

    @Override
    protected Leader createBehavior(final RaftActorContext actorContext) {
        return new Leader(actorContext);
    }

    @Override
    protected MockRaftActorContext createActorContext() {
        return createActorContext(leaderActor);
    }

    @Override
    protected MockRaftActorContext createActorContext(final ActorRef actorRef) {
        return createActorContext(LEADER_ID, actorRef);
    }

    private MockRaftActorContext createActorContext(final String id, final ActorRef actorRef) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(50, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        MockRaftActorContext context = new MockRaftActorContext(id, getSystem(), actorRef);
        context.setConfigParams(configParams);
        context.setPayloadVersion(payloadVersion);
        return context;
    }

    private MockRaftActorContext createActorContextWithFollower() {
        MockRaftActorContext actorContext = createActorContext();
        actorContext.setPeerAddresses(Map.of(FOLLOWER_ID, followerActor.path().toString()));
        return actorContext;
    }

    private MockRaftActorContext createFollowerActorContextWithLeader() {
        MockRaftActorContext followerActorContext = createActorContext(FOLLOWER_ID, followerActor);
        DefaultConfigParamsImpl followerConfig = new DefaultConfigParamsImpl();
        followerConfig.setElectionTimeoutFactor(10000);
        followerActorContext.setConfigParams(followerConfig);
        followerActorContext.setPeerAddresses(Map.of(LEADER_ID, leaderActor.path().toString()));
        return followerActorContext;
    }

    @Test
    public void testLeaderCreatedWithCommitIndexLessThanLastIndex() {
        logStart("testLeaderCreatedWithCommitIndexLessThanLastIndex");

        final MockRaftActorContext leaderActorContext = createActorContextWithFollower();

        MockRaftActorContext followerActorContext = createActorContext(FOLLOWER_ID, followerActor);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);
        followerActorContext.setCurrentBehavior(follower);

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(FOLLOWER_ID, followerActor.path().toString());

        leaderActorContext.setPeerAddresses(peerAddresses);

        leaderActorContext.getReplicatedLog().removeFrom(0);

        //create 3 entries
        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        leaderActorContext.setCommitIndex(1);

        followerActorContext.getReplicatedLog().removeFrom(0);

        // follower too has the exact same log entries and has the same commit index
        followerActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        followerActorContext.setCommitIndex(1);

        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals(-1, appendEntries.getLeaderCommit());
        assertEquals(0, appendEntries.getEntries().size());
        assertEquals(0, appendEntries.getPrevLogIndex());

        AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(
                leaderActor, AppendEntriesReply.class);

        assertEquals(2, appendEntriesReply.getLogLastIndex());
        assertEquals(1, appendEntriesReply.getLogLastTerm());

        // follower returns its next index
        assertEquals(2, appendEntriesReply.getLogLastIndex());
        assertEquals(1, appendEntriesReply.getLogLastTerm());

        follower.close();
    }

    @Test
    public void testLeaderCreatedWithCommitIndexLessThanFollowersCommitIndex() {
        logStart("testLeaderCreatedWithCommitIndexLessThanFollowersCommitIndex");

        final MockRaftActorContext leaderActorContext = createActorContext();

        MockRaftActorContext followerActorContext = createActorContext(FOLLOWER_ID, followerActor);
        followerActorContext.setPeerAddresses(Map.of(LEADER_ID, leaderActor.path().toString()));

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);
        followerActorContext.setCurrentBehavior(follower);

        Map<String, String> leaderPeerAddresses = new HashMap<>();
        leaderPeerAddresses.put(FOLLOWER_ID, followerActor.path().toString());

        leaderActorContext.setPeerAddresses(leaderPeerAddresses);

        leaderActorContext.getReplicatedLog().removeFrom(0);

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        leaderActorContext.setCommitIndex(1);

        followerActorContext.getReplicatedLog().removeFrom(0);

        followerActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        // follower has the same log entries but its commit index > leaders commit index
        followerActorContext.setCommitIndex(2);

        leader = new Leader(leaderActorContext);

        // Initial heartbeat
        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals(-1, appendEntries.getLeaderCommit());
        assertEquals(0, appendEntries.getEntries().size());
        assertEquals(0, appendEntries.getPrevLogIndex());

        AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(
                leaderActor, AppendEntriesReply.class);

        assertEquals(2, appendEntriesReply.getLogLastIndex());
        assertEquals(1, appendEntriesReply.getLogLastTerm());

        leaderActor.underlyingActor().setBehavior(follower);
        leader.handleMessage(followerActor, appendEntriesReply);

        leaderActor.underlyingActor().clear();
        followerActor.underlyingActor().clear();

        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals(2, appendEntries.getLeaderCommit());
        assertEquals(0, appendEntries.getEntries().size());
        assertEquals(2, appendEntries.getPrevLogIndex());

        appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertEquals(2, appendEntriesReply.getLogLastIndex());
        assertEquals(1, appendEntriesReply.getLogLastTerm());

        assertEquals(2, followerActorContext.getCommitIndex());

        follower.close();
    }

    @Test
    public void testHandleAppendEntriesReplyFailureWithFollowersLogBehindTheLeader() {
        logStart("testHandleAppendEntriesReplyFailureWithFollowersLogBehindTheLeader");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(1000, TimeUnit.SECONDS));

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());
        long leaderCommitIndex = 2;
        leaderActorContext.setCommitIndex(leaderCommitIndex);
        leaderActorContext.setLastApplied(leaderCommitIndex);

        final ReplicatedLogEntry leadersSecondLogEntry = leaderActorContext.getReplicatedLog().get(1);
        final ReplicatedLogEntry leadersThirdLogEntry = leaderActorContext.getReplicatedLog().get(2);

        MockRaftActorContext followerActorContext = createFollowerActorContextWithLeader();

        followerActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 1, 1).build());
        followerActorContext.setCommitIndex(0);
        followerActorContext.setLastApplied(0);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);

        leader = new Leader(leaderActorContext);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        final AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        MessageCollectorActor.clearMessages(followerActor);
        MessageCollectorActor.clearMessages(leaderActor);

        // Verify initial AppendEntries sent.
        assertEquals("getLeaderCommit", -1, appendEntries.getLeaderCommit());
        assertEquals("Log entries size", 0, appendEntries.getEntries().size());
        assertEquals("getPrevLogIndex", 1, appendEntries.getPrevLogIndex());

        leaderActor.underlyingActor().setBehavior(leader);

        leader.handleMessage(followerActor, appendEntriesReply);

        MessageCollectorActor.expectMatching(leaderActor, AppendEntriesReply.class, 1);
        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals("getLeaderCommit", leaderCommitIndex, appendEntries.getLeaderCommit());
        assertEquals("getPrevLogIndex", 0, appendEntries.getPrevLogIndex());
        assertEquals("Log entries size", 2, appendEntries.getEntries().size());

        assertEquals("First entry index", 1, appendEntries.getEntries().get(0).getIndex());
        assertEquals("First entry data", leadersSecondLogEntry.getData(),
                appendEntries.getEntries().get(0).getData());
        assertEquals("Second entry index", 2, appendEntries.getEntries().get(1).getIndex());
        assertEquals("Second entry data", leadersThirdLogEntry.getData(),
                appendEntries.getEntries().get(1).getData());

        FollowerLogInformation followerInfo = leader.getFollower(FOLLOWER_ID);
        assertEquals("getNextIndex", 3, followerInfo.getNextIndex());

        List<ApplyState> applyStateList = MessageCollectorActor.expectMatching(followerActor, ApplyState.class, 2);

        ApplyState applyState = applyStateList.get(0);
        assertEquals("Follower's first ApplyState index", 1, applyState.getReplicatedLogEntry().getIndex());
        assertEquals("Follower's first ApplyState term", 1, applyState.getReplicatedLogEntry().getTerm());
        assertEquals("Follower's first ApplyState data", leadersSecondLogEntry.getData(),
                applyState.getReplicatedLogEntry().getData());

        applyState = applyStateList.get(1);
        assertEquals("Follower's second ApplyState index", 2, applyState.getReplicatedLogEntry().getIndex());
        assertEquals("Follower's second ApplyState term", 1, applyState.getReplicatedLogEntry().getTerm());
        assertEquals("Follower's second ApplyState data", leadersThirdLogEntry.getData(),
                applyState.getReplicatedLogEntry().getData());

        assertEquals("Follower's commit index", 2, followerActorContext.getCommitIndex());
        assertEquals("Follower's lastIndex", 2, followerActorContext.getReplicatedLog().lastIndex());
    }

    @Test
    public void testHandleAppendEntriesReplyFailureWithFollowersLogEmpty() {
        logStart("testHandleAppendEntriesReplyFailureWithFollowersLogEmpty");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(1000, TimeUnit.SECONDS));

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 2, 1).build());
        long leaderCommitIndex = 1;
        leaderActorContext.setCommitIndex(leaderCommitIndex);
        leaderActorContext.setLastApplied(leaderCommitIndex);

        final ReplicatedLogEntry leadersFirstLogEntry = leaderActorContext.getReplicatedLog().get(0);
        final ReplicatedLogEntry leadersSecondLogEntry = leaderActorContext.getReplicatedLog().get(1);

        MockRaftActorContext followerActorContext = createFollowerActorContextWithLeader();

        followerActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());
        followerActorContext.setCommitIndex(-1);
        followerActorContext.setLastApplied(-1);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);
        followerActorContext.setCurrentBehavior(follower);

        leader = new Leader(leaderActorContext);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        final AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        MessageCollectorActor.clearMessages(followerActor);
        MessageCollectorActor.clearMessages(leaderActor);

        // Verify initial AppendEntries sent with the leader's current commit index.
        assertEquals("getLeaderCommit", -1, appendEntries.getLeaderCommit());
        assertEquals("Log entries size", 0, appendEntries.getEntries().size());
        assertEquals("getPrevLogIndex", 0, appendEntries.getPrevLogIndex());

        leaderActor.underlyingActor().setBehavior(leader);
        leaderActorContext.setCurrentBehavior(leader);

        leader.handleMessage(followerActor, appendEntriesReply);

        MessageCollectorActor.expectMatching(leaderActor, AppendEntriesReply.class, 1);
        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals("getLeaderCommit", leaderCommitIndex, appendEntries.getLeaderCommit());
        assertEquals("getPrevLogIndex", -1, appendEntries.getPrevLogIndex());
        assertEquals("Log entries size", 2, appendEntries.getEntries().size());

        assertEquals("First entry index", 0, appendEntries.getEntries().get(0).getIndex());
        assertEquals("First entry data", leadersFirstLogEntry.getData(),
                appendEntries.getEntries().get(0).getData());
        assertEquals("Second entry index", 1, appendEntries.getEntries().get(1).getIndex());
        assertEquals("Second entry data", leadersSecondLogEntry.getData(),
                appendEntries.getEntries().get(1).getData());

        FollowerLogInformation followerInfo = leader.getFollower(FOLLOWER_ID);
        assertEquals("getNextIndex", 2, followerInfo.getNextIndex());

        List<ApplyState> applyStateList = MessageCollectorActor.expectMatching(followerActor, ApplyState.class, 2);

        ApplyState applyState = applyStateList.get(0);
        assertEquals("Follower's first ApplyState index", 0, applyState.getReplicatedLogEntry().getIndex());
        assertEquals("Follower's first ApplyState term", 1, applyState.getReplicatedLogEntry().getTerm());
        assertEquals("Follower's first ApplyState data", leadersFirstLogEntry.getData(),
                applyState.getReplicatedLogEntry().getData());

        applyState = applyStateList.get(1);
        assertEquals("Follower's second ApplyState index", 1, applyState.getReplicatedLogEntry().getIndex());
        assertEquals("Follower's second ApplyState term", 1, applyState.getReplicatedLogEntry().getTerm());
        assertEquals("Follower's second ApplyState data", leadersSecondLogEntry.getData(),
                applyState.getReplicatedLogEntry().getData());

        assertEquals("Follower's commit index", 1, followerActorContext.getCommitIndex());
        assertEquals("Follower's lastIndex", 1, followerActorContext.getReplicatedLog().lastIndex());
    }

    @Test
    public void testHandleAppendEntriesReplyFailureWithFollowersLogTermDifferent() {
        logStart("testHandleAppendEntriesReplyFailureWithFollowersLogTermDifferent");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(1000, TimeUnit.SECONDS));

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 2, 2).build());
        long leaderCommitIndex = 1;
        leaderActorContext.setCommitIndex(leaderCommitIndex);
        leaderActorContext.setLastApplied(leaderCommitIndex);

        final ReplicatedLogEntry leadersFirstLogEntry = leaderActorContext.getReplicatedLog().get(0);
        final ReplicatedLogEntry leadersSecondLogEntry = leaderActorContext.getReplicatedLog().get(1);

        MockRaftActorContext followerActorContext = createFollowerActorContextWithLeader();

        followerActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 1, 1).build());
        followerActorContext.setCommitIndex(-1);
        followerActorContext.setLastApplied(-1);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);
        followerActorContext.setCurrentBehavior(follower);

        leader = new Leader(leaderActorContext);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        final AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        MessageCollectorActor.clearMessages(followerActor);
        MessageCollectorActor.clearMessages(leaderActor);

        // Verify initial AppendEntries sent with the leader's current commit index.
        assertEquals("getLeaderCommit", -1, appendEntries.getLeaderCommit());
        assertEquals("Log entries size", 0, appendEntries.getEntries().size());
        assertEquals("getPrevLogIndex", 0, appendEntries.getPrevLogIndex());

        leaderActor.underlyingActor().setBehavior(leader);
        leaderActorContext.setCurrentBehavior(leader);

        leader.handleMessage(followerActor, appendEntriesReply);

        MessageCollectorActor.expectMatching(leaderActor, AppendEntriesReply.class, 1);
        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals("getLeaderCommit", leaderCommitIndex, appendEntries.getLeaderCommit());
        assertEquals("getPrevLogIndex", -1, appendEntries.getPrevLogIndex());
        assertEquals("Log entries size", 2, appendEntries.getEntries().size());

        assertEquals("First entry index", 0, appendEntries.getEntries().get(0).getIndex());
        assertEquals("First entry term", 2, appendEntries.getEntries().get(0).getTerm());
        assertEquals("First entry data", leadersFirstLogEntry.getData(),
                appendEntries.getEntries().get(0).getData());
        assertEquals("Second entry index", 1, appendEntries.getEntries().get(1).getIndex());
        assertEquals("Second entry term", 2, appendEntries.getEntries().get(1).getTerm());
        assertEquals("Second entry data", leadersSecondLogEntry.getData(),
                appendEntries.getEntries().get(1).getData());

        FollowerLogInformation followerInfo = leader.getFollower(FOLLOWER_ID);
        assertEquals("getNextIndex", 2, followerInfo.getNextIndex());

        List<ApplyState> applyStateList = MessageCollectorActor.expectMatching(followerActor, ApplyState.class, 2);

        ApplyState applyState = applyStateList.get(0);
        assertEquals("Follower's first ApplyState index", 0, applyState.getReplicatedLogEntry().getIndex());
        assertEquals("Follower's first ApplyState term", 2, applyState.getReplicatedLogEntry().getTerm());
        assertEquals("Follower's first ApplyState data", leadersFirstLogEntry.getData(),
                applyState.getReplicatedLogEntry().getData());

        applyState = applyStateList.get(1);
        assertEquals("Follower's second ApplyState index", 1, applyState.getReplicatedLogEntry().getIndex());
        assertEquals("Follower's second ApplyState term", 2, applyState.getReplicatedLogEntry().getTerm());
        assertEquals("Follower's second ApplyState data", leadersSecondLogEntry.getData(),
                applyState.getReplicatedLogEntry().getData());

        assertEquals("Follower's commit index", 1, followerActorContext.getCommitIndex());
        assertEquals("Follower's lastIndex", 1, followerActorContext.getReplicatedLog().lastIndex());
        assertEquals("Follower's lastTerm", 2, followerActorContext.getReplicatedLog().lastTerm());
    }

    @Test
    public void testHandleAppendEntriesReplyWithNewerTerm() {
        logStart("testHandleAppendEntriesReplyWithNewerTerm");

        MockRaftActorContext leaderActorContext = createActorContext();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(10000, TimeUnit.SECONDS));

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 2, 2).build());

        leader = new Leader(leaderActorContext);
        leaderActor.underlyingActor().setBehavior(leader);
        leaderActor.tell(new AppendEntriesReply("foo", 20, false, 1000, 10, (short) 1), ActorRef.noSender());

        AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        assertEquals(false, appendEntriesReply.isSuccess());
        assertEquals(RaftState.Follower, leaderActor.underlyingActor().getFirstBehaviorChange().state());

        MessageCollectorActor.clearMessages(leaderActor);
    }

    @Test
    public void testHandleAppendEntriesReplyWithNewerTermWhenElectionsAreDisabled() {
        logStart("testHandleAppendEntriesReplyWithNewerTermWhenElectionsAreDisabled");

        MockRaftActorContext leaderActorContext = createActorContext();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(10000, TimeUnit.SECONDS));

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 2, 2).build());
        leaderActorContext.setRaftPolicy(createRaftPolicy(false, false));

        leader = new Leader(leaderActorContext);
        leaderActor.underlyingActor().setBehavior(leader);
        leaderActor.tell(new AppendEntriesReply("foo", 20, false, 1000, 10, (short) 1), ActorRef.noSender());

        AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        assertEquals(false, appendEntriesReply.isSuccess());
        assertEquals(RaftState.Leader, leaderActor.underlyingActor().getFirstBehaviorChange().state());

        MessageCollectorActor.clearMessages(leaderActor);
    }

    @Test
    public void testHandleAppendEntriesReplySuccess() {
        logStart("testHandleAppendEntriesReplySuccess");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        leaderActorContext.setCommitIndex(1);
        leaderActorContext.setLastApplied(1);
        leaderActorContext.getTermInformation().update(1, "leader");

        leader = new Leader(leaderActorContext);

        FollowerLogInformation followerInfo = leader.getFollower(FOLLOWER_ID);

        assertEquals(payloadVersion, leader.getLeaderPayloadVersion());
        assertEquals(RaftVersions.HELIUM_VERSION, followerInfo.getRaftVersion());

        AppendEntriesReply reply = new AppendEntriesReply(FOLLOWER_ID, 1, true, 2, 1, payloadVersion);

        RaftActorBehavior raftActorBehavior = leader.handleAppendEntriesReply(followerActor, reply);

        assertEquals(RaftState.Leader, raftActorBehavior.state());

        assertEquals(2, leaderActorContext.getCommitIndex());

        ApplyJournalEntries applyJournalEntries = MessageCollectorActor.expectFirstMatching(
                leaderActor, ApplyJournalEntries.class);

        assertEquals(2, leaderActorContext.getLastApplied());

        assertEquals(2, applyJournalEntries.getToIndex());

        List<ApplyState> applyStateList = MessageCollectorActor.getAllMatching(leaderActor,
                ApplyState.class);

        assertEquals(1,applyStateList.size());

        ApplyState applyState = applyStateList.get(0);

        assertEquals(2, applyState.getReplicatedLogEntry().getIndex());

        assertEquals(2, followerInfo.getMatchIndex());
        assertEquals(3, followerInfo.getNextIndex());
        assertEquals(payloadVersion, followerInfo.getPayloadVersion());
        assertEquals(RaftVersions.CURRENT_VERSION, followerInfo.getRaftVersion());
    }

    @Test
    public void testHandleAppendEntriesReplyUnknownFollower() {
        logStart("testHandleAppendEntriesReplyUnknownFollower");

        MockRaftActorContext leaderActorContext = createActorContext();

        leader = new Leader(leaderActorContext);

        AppendEntriesReply reply = new AppendEntriesReply("unkown-follower", 1, false, 10, 1, (short)0);

        RaftActorBehavior raftActorBehavior = leader.handleAppendEntriesReply(followerActor, reply);

        assertEquals(RaftState.Leader, raftActorBehavior.state());
    }

    @Test
    public void testFollowerCatchUpWithAppendEntriesMaxDataSizeExceeded() {
        logStart("testFollowerCatchUpWithAppendEntriesMaxDataSizeExceeded");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(1000, TimeUnit.SECONDS));
        // Note: the size here depends on estimate
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setSnapshotChunkSize(246);

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 4, 1).build());
        long leaderCommitIndex = 3;
        leaderActorContext.setCommitIndex(leaderCommitIndex);
        leaderActorContext.setLastApplied(leaderCommitIndex);

        final ReplicatedLogEntry leadersFirstLogEntry = leaderActorContext.getReplicatedLog().get(0);
        final ReplicatedLogEntry leadersSecondLogEntry = leaderActorContext.getReplicatedLog().get(1);
        final ReplicatedLogEntry leadersThirdLogEntry = leaderActorContext.getReplicatedLog().get(2);
        final ReplicatedLogEntry leadersFourthLogEntry = leaderActorContext.getReplicatedLog().get(3);

        MockRaftActorContext followerActorContext = createFollowerActorContextWithLeader();

        followerActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());
        followerActorContext.setCommitIndex(-1);
        followerActorContext.setLastApplied(-1);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);
        followerActorContext.setCurrentBehavior(follower);

        leader = new Leader(leaderActorContext);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        final AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        MessageCollectorActor.clearMessages(followerActor);
        MessageCollectorActor.clearMessages(leaderActor);

        // Verify initial AppendEntries sent with the leader's current commit index.
        assertEquals("getLeaderCommit", -1, appendEntries.getLeaderCommit());
        assertEquals("Log entries size", 0, appendEntries.getEntries().size());
        assertEquals("getPrevLogIndex", 2, appendEntries.getPrevLogIndex());

        leaderActor.underlyingActor().setBehavior(leader);
        leaderActorContext.setCurrentBehavior(leader);

        leader.handleMessage(followerActor, appendEntriesReply);

        List<AppendEntries> appendEntriesList = MessageCollectorActor.expectMatching(followerActor,
                AppendEntries.class, 2);
        MessageCollectorActor.expectMatching(leaderActor, AppendEntriesReply.class, 2);

        appendEntries = appendEntriesList.get(0);
        assertEquals("getLeaderCommit", leaderCommitIndex, appendEntries.getLeaderCommit());
        assertEquals("getPrevLogIndex", -1, appendEntries.getPrevLogIndex());
        assertEquals("Log entries size", 2, appendEntries.getEntries().size());

        assertEquals("First entry index", 0, appendEntries.getEntries().get(0).getIndex());
        assertEquals("First entry data", leadersFirstLogEntry.getData(),
                appendEntries.getEntries().get(0).getData());
        assertEquals("Second entry index", 1, appendEntries.getEntries().get(1).getIndex());
        assertEquals("Second entry data", leadersSecondLogEntry.getData(),
                appendEntries.getEntries().get(1).getData());

        appendEntries = appendEntriesList.get(1);
        assertEquals("getLeaderCommit", leaderCommitIndex, appendEntries.getLeaderCommit());
        assertEquals("getPrevLogIndex", 1, appendEntries.getPrevLogIndex());
        assertEquals("Log entries size", 2, appendEntries.getEntries().size());

        assertEquals("First entry index", 2, appendEntries.getEntries().get(0).getIndex());
        assertEquals("First entry data", leadersThirdLogEntry.getData(),
                appendEntries.getEntries().get(0).getData());
        assertEquals("Second entry index", 3, appendEntries.getEntries().get(1).getIndex());
        assertEquals("Second entry data", leadersFourthLogEntry.getData(),
                appendEntries.getEntries().get(1).getData());

        FollowerLogInformation followerInfo = leader.getFollower(FOLLOWER_ID);
        assertEquals("getNextIndex", 4, followerInfo.getNextIndex());

        MessageCollectorActor.expectMatching(followerActor, ApplyState.class, 4);

        assertEquals("Follower's commit index", 3, followerActorContext.getCommitIndex());
        assertEquals("Follower's lastIndex", 3, followerActorContext.getReplicatedLog().lastIndex());
    }

    @Test
    public void testHandleRequestVoteReply() {
        logStart("testHandleRequestVoteReply");

        MockRaftActorContext leaderActorContext = createActorContext();

        leader = new Leader(leaderActorContext);

        // Should be a no-op.
        RaftActorBehavior raftActorBehavior = leader.handleRequestVoteReply(followerActor,
                new RequestVoteReply(1, true));

        assertEquals(RaftState.Leader, raftActorBehavior.state());

        raftActorBehavior = leader.handleRequestVoteReply(followerActor, new RequestVoteReply(1, false));

        assertEquals(RaftState.Leader, raftActorBehavior.state());
    }

    @Test
    public void testIsolatedLeaderCheckNoFollowers() {
        logStart("testIsolatedLeaderCheckNoFollowers");

        MockRaftActorContext leaderActorContext = createActorContext();

        leader = new Leader(leaderActorContext);
        RaftActorBehavior newBehavior = leader.handleMessage(leaderActor, Leader.ISOLATED_LEADER_CHECK);
        assertTrue(newBehavior instanceof Leader);
    }

    @Test
    public void testIsolatedLeaderCheckNoVotingFollowers() {
        logStart("testIsolatedLeaderCheckNoVotingFollowers");

        MockRaftActorContext followerActorContext = createFollowerActorContextWithLeader();
        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().setBehavior(follower);

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(1000, TimeUnit.SECONDS));
        leaderActorContext.getPeerInfo(FOLLOWER_ID).setVotingState(VotingState.NON_VOTING);

        leader = new Leader(leaderActorContext);
        leader.getFollower(FOLLOWER_ID).markFollowerActive();
        RaftActorBehavior newBehavior = leader.handleMessage(leaderActor, Leader.ISOLATED_LEADER_CHECK);
        assertTrue("Expected Leader", newBehavior instanceof Leader);
    }

    private RaftActorBehavior setupIsolatedLeaderCheckTestWithTwoFollowers(final RaftPolicy raftPolicy) {
        ActorRef followerActor1 = getSystem().actorOf(MessageCollectorActor.props(), "follower-1");
        ActorRef followerActor2 = getSystem().actorOf(MessageCollectorActor.props(), "follower-2");

        MockRaftActorContext leaderActorContext = createActorContext();

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put("follower-1", followerActor1.path().toString());
        peerAddresses.put("follower-2", followerActor2.path().toString());

        leaderActorContext.setPeerAddresses(peerAddresses);
        leaderActorContext.setRaftPolicy(raftPolicy);

        leader = new Leader(leaderActorContext);

        leader.markFollowerActive("follower-1");
        leader.markFollowerActive("follower-2");
        RaftActorBehavior newBehavior = leader.handleMessage(leaderActor, Leader.ISOLATED_LEADER_CHECK);
        assertTrue("Behavior not instance of Leader when all followers are active", newBehavior instanceof Leader);

        // kill 1 follower and verify if that got killed
        final TestKit probe = new TestKit(getSystem());
        probe.watch(followerActor1);
        followerActor1.tell(PoisonPill.getInstance(), ActorRef.noSender());
        final Terminated termMsg1 = probe.expectMsgClass(Terminated.class);
        assertEquals(termMsg1.getActor(), followerActor1);

        leader.markFollowerInActive("follower-1");
        leader.markFollowerActive("follower-2");
        newBehavior = leader.handleMessage(leaderActor, Leader.ISOLATED_LEADER_CHECK);
        assertTrue("Behavior not instance of Leader when majority of followers are active",
                newBehavior instanceof Leader);

        // kill 2nd follower and leader should change to Isolated leader
        followerActor2.tell(PoisonPill.getInstance(), null);
        probe.watch(followerActor2);
        followerActor2.tell(PoisonPill.getInstance(), ActorRef.noSender());
        final Terminated termMsg2 = probe.expectMsgClass(Terminated.class);
        assertEquals(termMsg2.getActor(), followerActor2);

        leader.markFollowerInActive("follower-2");
        return leader.handleMessage(leaderActor, Leader.ISOLATED_LEADER_CHECK);
    }

    @Test
    public void testIsolatedLeaderCheckTwoFollowers() {
        logStart("testIsolatedLeaderCheckTwoFollowers");

        RaftActorBehavior newBehavior = setupIsolatedLeaderCheckTestWithTwoFollowers(DefaultRaftPolicy.INSTANCE);

        assertTrue("Behavior not instance of IsolatedLeader when majority followers are inactive",
            newBehavior instanceof IsolatedLeader);
    }

    @Test
    public void testIsolatedLeaderCheckTwoFollowersWhenElectionsAreDisabled() {
        logStart("testIsolatedLeaderCheckTwoFollowersWhenElectionsAreDisabled");

        RaftActorBehavior newBehavior = setupIsolatedLeaderCheckTestWithTwoFollowers(createRaftPolicy(false, true));

        assertTrue("Behavior should not switch to IsolatedLeader because elections are disabled",
                newBehavior instanceof Leader);
    }

    @Test
    public void testLaggingFollowerStarvation() {
        logStart("testLaggingFollowerStarvation");

        String leaderActorId = actorFactory.generateActorId("leader");
        String follower1ActorId = actorFactory.generateActorId("follower");
        String follower2ActorId = actorFactory.generateActorId("follower");

        final ActorRef follower1Actor = actorFactory.createActor(MessageCollectorActor.props(), follower1ActorId);
        final ActorRef follower2Actor = actorFactory.createActor(MessageCollectorActor.props(), follower2ActorId);

        MockRaftActorContext leaderActorContext =
                new MockRaftActorContext(leaderActorId, getSystem(), leaderActor);

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(200, TimeUnit.MILLISECONDS));
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(10, TimeUnit.SECONDS));

        leaderActorContext.setConfigParams(configParams);

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(1,5,1).build());

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(follower1ActorId,
                follower1Actor.path().toString());
        peerAddresses.put(follower2ActorId,
                follower2Actor.path().toString());

        leaderActorContext.setPeerAddresses(peerAddresses);
        leaderActorContext.getTermInformation().update(1, leaderActorId);

        leader = createBehavior(leaderActorContext);

        leaderActor.underlyingActor().setBehavior(leader);

        for (int i = 1; i < 6; i++) {
            // Each AppendEntriesReply could end up rescheduling the heartbeat (without the fix for bug 2733)
            RaftActorBehavior newBehavior = leader.handleMessage(follower1Actor,
                    new AppendEntriesReply(follower1ActorId, 1, true, i, 1, (short)0));
            assertTrue(newBehavior == leader);
            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }

        // Check if the leader has been receiving SendHeartbeat messages despite getting AppendEntriesReply
        List<SendHeartBeat> heartbeats = MessageCollectorActor.getAllMatching(leaderActor, SendHeartBeat.class);

        assertTrue(String.format("%s heartbeat(s) is less than expected", heartbeats.size()),
                heartbeats.size() > 1);

        // Check if follower-2 got AppendEntries during this time and was not starved
        List<AppendEntries> appendEntries = MessageCollectorActor.getAllMatching(follower2Actor, AppendEntries.class);

        assertTrue(String.format("%s append entries is less than expected", appendEntries.size()),
                appendEntries.size() > 1);
    }

    @Test
    public void testReplicationConsensusWithNonVotingFollower() {
        logStart("testReplicationConsensusWithNonVotingFollower");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(1000, TimeUnit.SECONDS));

        leaderActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());
        leaderActorContext.setCommitIndex(-1);
        leaderActorContext.setLastApplied(-1);

        String nonVotingFollowerId = "nonvoting-follower";
        ActorRef nonVotingFollowerActor = actorFactory.createActor(
                MessageCollectorActor.props(), actorFactory.generateActorId(nonVotingFollowerId));

        leaderActorContext.addToPeers(nonVotingFollowerId, nonVotingFollowerActor.path().toString(),
                VotingState.NON_VOTING);

        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        // Ignore initial heartbeats
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        MessageCollectorActor.expectFirstMatching(nonVotingFollowerActor, AppendEntries.class);

        MessageCollectorActor.clearMessages(followerActor);
        MessageCollectorActor.clearMessages(nonVotingFollowerActor);
        MessageCollectorActor.clearMessages(leaderActor);

        // Send a Replicate message and wait for AppendEntries.
        sendReplicate(leaderActorContext, 0);

        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        MessageCollectorActor.expectFirstMatching(nonVotingFollowerActor, AppendEntries.class);

        // Send reply only from the voting follower and verify consensus via ApplyState.
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, 0, 1, (short)0));

        MessageCollectorActor.expectFirstMatching(leaderActor, ApplyState.class);

        leader.handleMessage(leaderActor, new AppendEntriesReply(nonVotingFollowerId, 1, true, 0, 1, (short)0));

        MessageCollectorActor.clearMessages(followerActor);
        MessageCollectorActor.clearMessages(nonVotingFollowerActor);
        MessageCollectorActor.clearMessages(leaderActor);

        // Send another Replicate message
        sendReplicate(leaderActorContext, 1);

        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(nonVotingFollowerActor,
                AppendEntries.class);
        assertEquals("Log entries size", 1, appendEntries.getEntries().size());
        assertEquals("Log entry index", 1, appendEntries.getEntries().get(0).getIndex());

        // Send reply only from the non-voting follower and verify no consensus via no ApplyState.
        leader.handleMessage(leaderActor, new AppendEntriesReply(nonVotingFollowerId, 1, true, 1, 1, (short)0));

        MessageCollectorActor.assertNoneMatching(leaderActor, ApplyState.class, 500);

        // Send reply from the voting follower and verify consensus.
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, 1, 1, (short)0));

        MessageCollectorActor.expectFirstMatching(leaderActor, ApplyState.class);
    }

    @Test
    public void testTransferLeadershipWithFollowerInSync() {
        logStart("testTransferLeadershipWithFollowerInSync");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        leaderActorContext.setLastApplied(-1);
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(1000, TimeUnit.SECONDS));
        leaderActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());

        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        // Initial heartbeat
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, -1, -1, (short)0));
        MessageCollectorActor.clearMessages(followerActor);

        sendReplicate(leaderActorContext, 0);
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, 0, 1, (short)0));
        MessageCollectorActor.expectFirstMatching(leaderActor, ApplyState.class);
        MessageCollectorActor.clearMessages(followerActor);

        RaftActorLeadershipTransferCohort mockTransferCohort = mock(RaftActorLeadershipTransferCohort.class);
        leader.transferLeadership(mockTransferCohort);

        verify(mockTransferCohort, never()).transferComplete();
        doReturn(Optional.empty()).when(mockTransferCohort).getRequestedFollowerId();
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, 0, 1, (short)0));

        // Expect a final AppendEntries to ensure the follower's lastApplied index is up-to-date
        MessageCollectorActor.expectMatching(followerActor, AppendEntries.class, 2);

        // Leader should force an election timeout
        MessageCollectorActor.expectFirstMatching(followerActor, TimeoutNow.class);

        verify(mockTransferCohort).transferComplete();
    }

    @Test
    public void testTransferLeadershipWithEmptyLog() {
        logStart("testTransferLeadershipWithEmptyLog");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(1000, TimeUnit.SECONDS));
        leaderActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());

        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        // Initial heartbeat
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, -1, -1, (short)0));
        MessageCollectorActor.clearMessages(followerActor);

        RaftActorLeadershipTransferCohort mockTransferCohort = mock(RaftActorLeadershipTransferCohort.class);
        doReturn(Optional.empty()).when(mockTransferCohort).getRequestedFollowerId();
        leader.transferLeadership(mockTransferCohort);

        verify(mockTransferCohort, never()).transferComplete();
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, -1, -1, (short)0));

        // Expect a final AppendEntries to ensure the follower's lastApplied index is up-to-date
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // Leader should force an election timeout
        MessageCollectorActor.expectFirstMatching(followerActor, TimeoutNow.class);

        verify(mockTransferCohort).transferComplete();
    }

    @Test
    public void testTransferLeadershipWithFollowerInitiallyOutOfSync() {
        logStart("testTransferLeadershipWithFollowerInitiallyOutOfSync");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(200, TimeUnit.MILLISECONDS));

        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        // Initial heartbeat
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        MessageCollectorActor.clearMessages(followerActor);

        RaftActorLeadershipTransferCohort mockTransferCohort = mock(RaftActorLeadershipTransferCohort.class);
        doReturn(Optional.empty()).when(mockTransferCohort).getRequestedFollowerId();
        leader.transferLeadership(mockTransferCohort);

        verify(mockTransferCohort, never()).transferComplete();

        // Sync up the follower.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, -1, -1, (short)0));
        MessageCollectorActor.clearMessages(followerActor);

        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams()
                .getHeartBeatInterval().toMillis() + 1, TimeUnit.MILLISECONDS);
        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, 1, 1, (short)0));

        // Leader should force an election timeout
        MessageCollectorActor.expectFirstMatching(followerActor, TimeoutNow.class);

        verify(mockTransferCohort).transferComplete();
    }

    @Test
    public void testTransferLeadershipWithFollowerSyncTimeout() {
        logStart("testTransferLeadershipWithFollowerSyncTimeout");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(200, TimeUnit.MILLISECONDS));
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(2);
        leaderActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());

        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        // Initial heartbeat
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, -1, -1, (short)0));
        MessageCollectorActor.clearMessages(followerActor);

        sendReplicate(leaderActorContext, 0);
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        MessageCollectorActor.clearMessages(followerActor);

        RaftActorLeadershipTransferCohort mockTransferCohort = mock(RaftActorLeadershipTransferCohort.class);
        leader.transferLeadership(mockTransferCohort);

        verify(mockTransferCohort, never()).transferComplete();

        // Send heartbeats to time out the transfer.
        for (int i = 0; i < leaderActorContext.getConfigParams().getElectionTimeoutFactor(); i++) {
            Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams()
                    .getHeartBeatInterval().toMillis() + 1, TimeUnit.MILLISECONDS);
            leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);
        }

        verify(mockTransferCohort).abortTransfer();
        verify(mockTransferCohort, never()).transferComplete();
        MessageCollectorActor.assertNoneMatching(followerActor, ElectionTimeout.class, 100);
    }

    @Test
    public void testReplicationWithPayloadSizeThatExceedsThreshold() {
        logStart("testReplicationWithPayloadSizeThatExceedsThreshold");

        final int serializedSize = SerializationUtils.serialize(new AppendEntries(1, LEADER_ID, -1, -1,
                List.of(new SimpleReplicatedLogEntry(0, 1,
                        new MockRaftActorContext.MockPayload("large"))), 0, -1, (short)0)).length;
        final MockRaftActorContext.MockPayload largePayload =
                new MockRaftActorContext.MockPayload("large", serializedSize);

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(300, TimeUnit.MILLISECONDS));
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setSnapshotChunkSize(serializedSize - 50);
        leaderActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());
        leaderActorContext.setCommitIndex(-1);
        leaderActorContext.setLastApplied(-1);

        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        // Send initial heartbeat reply so follower is marked active
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(followerActor, new AppendEntriesReply(FOLLOWER_ID, -1, true, -1, -1, (short)0));
        MessageCollectorActor.clearMessages(followerActor);

        // Send normal payload first to prime commit index.
        final long term = leaderActorContext.getTermInformation().getCurrentTerm();
        sendReplicate(leaderActorContext, term, 0);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("Entries size", 1, appendEntries.getEntries().size());
        assertEquals("Entry getIndex", 0, appendEntries.getEntries().get(0).getIndex());

        leader.handleMessage(followerActor, new AppendEntriesReply(FOLLOWER_ID, term, true, 0, term, (short)0));
        assertEquals("getCommitIndex", 0, leaderActorContext.getCommitIndex());
        MessageCollectorActor.clearMessages(followerActor);

        // Now send a large payload that exceeds the maximum size for a single AppendEntries - it should be sliced.
        sendReplicate(leaderActorContext, term, 1, largePayload);

        MessageSlice messageSlice = MessageCollectorActor.expectFirstMatching(followerActor, MessageSlice.class);
        assertEquals("getSliceIndex", 1, messageSlice.getSliceIndex());
        assertEquals("getTotalSlices", 2, messageSlice.getTotalSlices());

        final Identifier slicingId = messageSlice.getIdentifier();

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getPrevLogIndex", 0, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", term, appendEntries.getPrevLogTerm());
        assertEquals("getLeaderCommit", -1, appendEntries.getLeaderCommit());
        assertEquals("Entries size", 0, appendEntries.getEntries().size());
        MessageCollectorActor.clearMessages(followerActor);

        // Initiate a heartbeat - it should send an empty AppendEntries since slicing is in progress.

        // Sleep for the heartbeat interval so AppendEntries is sent.
        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams()
                .getHeartBeatInterval().toMillis(), TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getLeaderCommit", -1, appendEntries.getLeaderCommit());
        assertEquals("Entries size", 0, appendEntries.getEntries().size());
        MessageCollectorActor.clearMessages(followerActor);

        // Simulate the MessageSliceReply's and AppendEntriesReply from the follower.

        leader.handleMessage(followerActor, MessageSliceReply.success(slicingId, 1, followerActor));
        messageSlice = MessageCollectorActor.expectFirstMatching(followerActor, MessageSlice.class);
        assertEquals("getSliceIndex", 2, messageSlice.getSliceIndex());

        leader.handleMessage(followerActor, MessageSliceReply.success(slicingId, 2, followerActor));

        leader.handleMessage(followerActor, new AppendEntriesReply(FOLLOWER_ID, term, true, 1, term, (short)0));

        MessageCollectorActor.clearMessages(followerActor);

        // Send another normal payload.

        sendReplicate(leaderActorContext, term, 2);

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("Entries size", 1, appendEntries.getEntries().size());
        assertEquals("Entry getIndex", 2, appendEntries.getEntries().get(0).getIndex());
        assertEquals("getLeaderCommit", 1, appendEntries.getLeaderCommit());
    }

    @Test
    public void testLargePayloadSlicingExpiration() {
        logStart("testLargePayloadSlicingExpiration");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                new FiniteDuration(100, TimeUnit.MILLISECONDS));
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setElectionTimeoutFactor(1);
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setSnapshotChunkSize(10);
        leaderActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());
        leaderActorContext.setCommitIndex(-1);
        leaderActorContext.setLastApplied(-1);

        final long term = leaderActorContext.getTermInformation().getCurrentTerm();
        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        // Send initial heartbeat reply so follower is marked active
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        leader.handleMessage(followerActor, new AppendEntriesReply(FOLLOWER_ID, -1, true, -1, -1, (short)0));
        MessageCollectorActor.clearMessages(followerActor);

        sendReplicate(leaderActorContext, term, 0, new MockRaftActorContext.MockPayload("large",
                leaderActorContext.getConfigParams().getSnapshotChunkSize() + 1));
        MessageCollectorActor.expectFirstMatching(followerActor, MessageSlice.class);

        // Sleep for at least 3 * election timeout so the slicing state expires.
        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams()
                .getElectionTimeOutInterval().toMillis() * 3  + 50, TimeUnit.MILLISECONDS);
        MessageCollectorActor.clearMessages(followerActor);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getLeaderCommit", -1, appendEntries.getLeaderCommit());
        assertEquals("Entries size", 0, appendEntries.getEntries().size());

        MessageCollectorActor.assertNoneMatching(followerActor, MessageSlice.class, 300);
        MessageCollectorActor.clearMessages(followerActor);

        // Send an AppendEntriesReply - this should restart the slicing.

        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams()
                .getHeartBeatInterval().toMillis() + 50, TimeUnit.MILLISECONDS);

        leader.handleMessage(followerActor, new AppendEntriesReply(FOLLOWER_ID, term, true, -1, term, (short)0));

        MessageCollectorActor.expectFirstMatching(followerActor, MessageSlice.class);
    }

    @Test
    public void testLeaderAddressInAppendEntries() {
        logStart("testLeaderAddressInAppendEntries");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();
        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setHeartBeatInterval(
                FiniteDuration.create(50, TimeUnit.MILLISECONDS));
        leaderActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());
        leaderActorContext.setCommitIndex(-1);
        leaderActorContext.setLastApplied(-1);

        ((DefaultConfigParamsImpl)leaderActorContext.getConfigParams()).setPeerAddressResolver(
            peerId -> leaderActor.path().toString());

        leader = new Leader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(leader);

        // Initial heartbeat shouldn't have the leader address

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertFalse(appendEntries.getLeaderAddress().isPresent());
        MessageCollectorActor.clearMessages(followerActor);

        // Send AppendEntriesReply indicating the follower needs the leader address

        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, -1, -1, (short)0, false, true,
                RaftVersions.CURRENT_VERSION));

        // Sleep for the heartbeat interval so AppendEntries is sent.
        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams()
                .getHeartBeatInterval().toMillis(), TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertTrue(appendEntries.getLeaderAddress().isPresent());
        assertEquals(leaderActor.path().toString(), appendEntries.getLeaderAddress().get());
        MessageCollectorActor.clearMessages(followerActor);

        // Send AppendEntriesReply indicating the follower does not need the leader address

        leader.handleMessage(leaderActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, -1, -1, (short)0, false, false,
                RaftVersions.CURRENT_VERSION));

        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams()
                .getHeartBeatInterval().toMillis(), TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, SendHeartBeat.INSTANCE);

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertFalse(appendEntries.getLeaderAddress().isPresent());
    }

    @Override
    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(final MockRaftActorContext actorContext,
            final ActorRef actorRef, final RaftRPC rpc) {
        super.assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, actorRef, rpc);
        assertEquals("New votedFor", null, actorContext.getTermInformation().getVotedFor());
    }

    private static class MockConfigParamsImpl extends DefaultConfigParamsImpl {

        private final long electionTimeOutIntervalMillis;
        private final int snapshotChunkSize;

        MockConfigParamsImpl(final long electionTimeOutIntervalMillis, final int snapshotChunkSize) {
            this.electionTimeOutIntervalMillis = electionTimeOutIntervalMillis;
            this.snapshotChunkSize = snapshotChunkSize;
        }

        @Override
        public FiniteDuration getElectionTimeOutInterval() {
            return new FiniteDuration(electionTimeOutIntervalMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public int getSnapshotChunkSize() {
            return snapshotChunkSize;
        }
    }
}
