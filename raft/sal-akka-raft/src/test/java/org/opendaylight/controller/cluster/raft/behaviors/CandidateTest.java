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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.MessageCollector;
import org.opendaylight.controller.cluster.raft.MessageCollectorActor;
import org.opendaylight.controller.cluster.raft.MockCommand;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.raft.api.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CandidateTest extends AbstractRaftActorBehaviorTest<Candidate> {
    static final Logger LOG = LoggerFactory.getLogger(CandidateTest.class);

    private final TestActorRef<MessageCollectorActor> candidateActor = actorFactory.createTestActor(
        MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId("candidate"));

    private MessageCollector[] peerActors;
    private RaftActorBehavior candidate;

    @Override
    @AfterEach
    void afterEach() {
        if (candidate != null) {
            candidate.close();
        }

        super.afterEach();
    }

    @Test
    void testWhenACandidateIsCreatedItIncrementsTheCurrentTermAndVotesForItself() {
        RaftActorContext raftActorContext = createActorContext();
        long expectedTerm = raftActorContext.currentTerm();

        candidate = new Candidate(raftActorContext);

        assertEquals("getCurrentTerm", new TermInfo(expectedTerm + 1, "candidate"), raftActorContext.termInfo());
    }

    @Test
    void testThatAnElectionTimeoutIsTriggered() {
        MockRaftActorContext actorContext = createActorContext();
        candidate = new Candidate(actorContext);

        MessageCollectorActor.expectFirstMatching(candidateActor, ElectionTimeout.class,
                actorContext.getConfigParams().getElectionTimeOutInterval().multipliedBy(6).toMillis());
    }

    @Test
    void testHandleElectionTimeoutWhenThereAreZeroPeers() {
        RaftActorContext raftActorContext = createActorContext();
        candidate = new Candidate(raftActorContext);

        candidate = assertInstanceOf(Leader.class, candidate.handleMessage(candidateActor, ElectionTimeout.INSTANCE));
    }

    @Test
    void testHandleElectionTimeoutWhenThereAreTwoNodeCluster() {
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setPeerAddresses(setupPeers(1));
        candidate = new Candidate(raftActorContext);

        assertSame(candidate, candidate.handleMessage(candidateActor, ElectionTimeout.INSTANCE));
    }

    @Test
    void testBecomeLeaderOnReceivingMajorityVotesInThreeNodeCluster() {
        MockRaftActorContext raftActorContext = createActorContext();
        final var log = raftActorContext.getReplicatedLog();
        log.setLastApplied(log.lastIndex());
        raftActorContext.setPeerAddresses(setupPeers(2));
        candidate = new Candidate(raftActorContext);

        candidate = assertInstanceOf(Leader.class,
            candidate.handleMessage(peerActors[0], new RequestVoteReply(1, true)));
    }

    @Test
    void testBecomePreLeaderOnReceivingMajorityVotesInThreeNodeCluster() {
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.getReplicatedLog().setLastApplied(-1);
        raftActorContext.setPeerAddresses(setupPeers(2));
        candidate = new Candidate(raftActorContext);

        // LastApplied is -1 and behind the last index.
        candidate = assertInstanceOf(PreLeader.class,
            candidate.handleMessage(peerActors[0], new RequestVoteReply(1, true)));
    }

    @Test
    void testBecomeLeaderOnReceivingMajorityVotesInFiveNodeCluster() {
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setTermInfo(new TermInfo(2L, "other"));
        final var log = new MockRaftActorContext.Builder().createEntries(0, 5, 1).build();
        log.setCommitIndex(log.lastIndex());
        log.setLastApplied(log.lastIndex());
        raftActorContext.resetReplicatedLog(log);
        raftActorContext.setPeerAddresses(setupPeers(4));
        candidate = new Candidate(raftActorContext);

        RequestVote requestVote = MessageCollectorActor.expectFirstMatching(peerActors[0], RequestVote.class);
        assertEquals("getTerm", 3L, requestVote.getTerm());
        assertEquals("getCandidateId", "candidate", requestVote.getCandidateId());
        assertEquals("getLastLogTerm", 1L, requestVote.getLastLogTerm());
        assertEquals("getLastLogIndex", 4L, requestVote.getLastLogIndex());

        MessageCollectorActor.expectFirstMatching(peerActors[1], RequestVote.class);
        MessageCollectorActor.expectFirstMatching(peerActors[2], RequestVote.class);
        MessageCollectorActor.expectFirstMatching(peerActors[3], RequestVote.class);

        // First peer denies the vote, hence two first two messages do not result in quorum
        assertSame(candidate, candidate.handleMessage(peerActors[0], new RequestVoteReply(1, false)));
        assertSame(candidate, candidate.handleMessage(peerActors[1], new RequestVoteReply(1, true)));

        // The third response results in quorum, becoming a leader
        assertInstanceOf(Leader.class, candidate.handleMessage(peerActors[2], new RequestVoteReply(1, true)));
    }

    @Test
    void testBecomeLeaderOnReceivingMajorityVotesWithNonVotingPeers() {
        final var raftActorContext = createActorContext();
        raftActorContext.setPeerAddresses(setupPeers(4));
        raftActorContext.resetReplicatedLog(new MockRaftActorContext.Builder().build());
        raftActorContext.getPeerInfo("peer1").setVotingState(VotingState.NON_VOTING);
        raftActorContext.getPeerInfo("peer4").setVotingState(VotingState.NON_VOTING);
        candidate = new Candidate(raftActorContext);

        peerActors[1].expectFirstMatching(RequestVote.class);
        peerActors[2].expectFirstMatching(RequestVote.class);
        peerActors[0].assertNoneMatching(RequestVote.class, 300);
        peerActors[3].assertNoneMatching(RequestVote.class, 100);

        assertSame(candidate, candidate.handleMessage(peerActors[1].actor(), new RequestVoteReply(1, false)));

        candidate = assertInstanceOf(Leader.class,
            candidate.handleMessage(peerActors[2].actor(), new RequestVoteReply(1, true)));
    }

    @Test
    void testResponseToHandleAppendEntriesWithLowerTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        RaftActorBehavior newBehavior = candidate.handleMessage(peerActors[0].actor(),
            new AppendEntries(1, "test", 0, 0, List.of(), 0, -1, (short) 0));

        AppendEntriesReply reply = peerActors[0].expectFirstMatching(AppendEntriesReply.class);
        assertFalse("isSuccess", reply.isSuccess());
        assertEquals("getTerm", 2, reply.getTerm());
        assertTrue("New Behavior : " + newBehavior, newBehavior instanceof Candidate);
    }

    @Test
    void testResponseToHandleAppendEntriesWithHigherTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        RaftActorBehavior newBehavior = candidate.handleMessage(peerActors[0].actor(),
            new AppendEntries(5, "test", 0, 0, List.of(), 0, -1, (short) 0));

        assertTrue("New Behavior : " + newBehavior, newBehavior instanceof Follower);
    }

    @Test
    void testResponseToHandleAppendEntriesWithEqualTerm() {
        MockRaftActorContext actorContext = createActorContext();

        candidate = new Candidate(actorContext);

        setupPeers(1);
        RaftActorBehavior newBehavior = candidate.handleMessage(peerActors[0].actor(),
            new AppendEntries(2, "test", 0, 0, List.of(), 0, -1, (short) 0));

        assertTrue("New Behavior : " + newBehavior + " term = " + actorContext.currentTerm(),
                newBehavior instanceof Follower);
    }

    @Test
    void testResponseToRequestVoteWithLowerTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        candidate.handleMessage(peerActors[0].actor(), new RequestVote(1, "test", 0, 0));

        RequestVoteReply reply = peerActors[0].expectFirstMatching(RequestVoteReply.class);
        assertFalse("isVoteGranted", reply.isVoteGranted());
        assertEquals("getTerm", 2, reply.getTerm());
    }

    @Test
    void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForMatches() {
        MockRaftActorContext context = createActorContext();
        context.setTermInfo(new TermInfo(1000, null));

        // Once a candidate is created it will immediately increment the current term so after
        // construction the currentTerm should be 1001
        candidate = new Candidate(context);

        setupPeers(1);
        candidate.handleMessage(peerActors[0].actor(), new RequestVote(1001, "candidate", 10000, 999));

        RequestVoteReply reply = peerActors[0].expectFirstMatching(RequestVoteReply.class);
        assertTrue("isVoteGranted", reply.isVoteGranted());
        assertEquals("getTerm", 1001, reply.getTerm());
    }

    @Test
    void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForDoesNotMatch() {
        MockRaftActorContext context = createActorContext();
        context.setTermInfo(new TermInfo(1000, null));

        // Once a candidate is created it will immediately increment the current term so after
        // construction the currentTerm should be 1001
        candidate = new Candidate(context);

        setupPeers(1);

        // RequestVote candidate ID ("candidate2") does not match this candidate's votedFor
        // (it votes for itself)
        candidate.handleMessage(peerActors[0].actor(), new RequestVote(1001, "candidate2", 10000, 999));

        RequestVoteReply reply = peerActors[0].expectFirstMatching(RequestVoteReply.class);
        assertFalse("isVoteGranted", reply.isVoteGranted());
        assertEquals("getTerm", 1001, reply.getTerm());
    }

    @Test
    void testCandidateSchedulesElectionTimeoutImmediatelyWhenItHasNoPeers() {
        MockRaftActorContext context = createActorContext();

        Stopwatch stopwatch = Stopwatch.createStarted();

        candidate = createBehavior(context);

        MessageCollectorActor.expectFirstMatching(candidateActor, ElectionTimeout.class);

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertTrue(elapsed < context.getConfigParams().getElectionTimeOutInterval().toMillis());
    }

    @Test
    @Override
    void testHandleAppendEntriesAddSameEntryToLog() {
        MockRaftActorContext context = createActorContext();

        context.setTermInfo(new TermInfo(2, "test"));

        // Prepare the receivers log
        MockCommand payload = new MockCommand("zero");
        setLastLogEntry(context, 2, 0, payload);

        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(new SimpleReplicatedLogEntry(0, 2, payload));

        final AppendEntries appendEntries = new AppendEntries(2, "leader-1", -1, -1, entries, 2, -1, (short)0);

        behavior = createBehavior(context);

        // Resetting the Candidates term to make sure it will match
        // the term sent by AppendEntries. If this was not done then
        // the test will fail because the Candidate will assume that
        // the message was sent to it from a lower term peer and will
        // thus respond with a failure
        context.setTermInfo(new TermInfo(2, "test"));

        // Send an unknown message so that the state of the RaftActor remains unchanged
        behavior.handleMessage(candidateActor, "unknown");

        final var raftBehavior = assertInstanceOf(Follower.class,
            behavior.handleMessage(candidateActor, appendEntries));

        assertEquals(1, context.getReplicatedLog().size());

        handleAppendEntriesAddSameEntryToLogReply(candidateActor);
    }

    @Override
    protected Candidate createBehavior(final RaftActorContext actorContext) {
        return new Candidate(actorContext);
    }

    @Override
    protected MockRaftActorContext createActorContext(final int payloadVersion) {
        return new MockRaftActorContext("candidate", stateDir, getSystem(), candidateActor, payloadVersion);
    }

    private Map<String, String> setupPeers(final int count) {
        final var peerMap = new HashMap<String, String>();
        peerActors = new MessageCollector[count];
        for (int i = 0; i < count; i++) {
            peerActors[i] = MessageCollector.ofPrefix(actorFactory, "peer");
            peerMap.put("peer" + (i + 1), peerActors[i].actor().path().toString());
        }

        return peerMap;
    }

    @Override
    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(final MockRaftActorContext actorContext,
            final MessageCollector actorRef, final RaftRPC rpc) {
        super.assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, actorRef, rpc);
        if (rpc instanceof RequestVote requestVote) {
            assertEquals("New votedFor", requestVote.getCandidateId(), actorContext.termInfo().votedFor());
        } else {
            assertEquals("New votedFor", null, actorContext.termInfo().votedFor());
        }
    }
}
