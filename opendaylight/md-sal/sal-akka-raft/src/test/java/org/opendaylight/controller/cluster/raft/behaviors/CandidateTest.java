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
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CandidateTest extends AbstractRaftActorBehaviorTest<Candidate> {
    static final Logger LOG = LoggerFactory.getLogger(CandidateTest.class);

    private final TestActorRef<MessageCollectorActor> candidateActor = actorFactory.createTestActor(
        MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId("candidate"));

    private ActorRef[] peerActors;
    private RaftActorBehavior candidate;

    @Override
    @After
    public void tearDown() {
        if (candidate != null) {
            candidate.close();
        }

        super.tearDown();
    }

    @Test
    public void testWhenACandidateIsCreatedItIncrementsTheCurrentTermAndVotesForItself() {
        RaftActorContext raftActorContext = createActorContext();
        long expectedTerm = raftActorContext.currentTerm();

        candidate = new Candidate(raftActorContext);

        assertEquals("getCurrentTerm", new TermInfo(expectedTerm + 1, "candidate"), raftActorContext.termInfo());
    }

    @Test
    public void testThatAnElectionTimeoutIsTriggered() {
        MockRaftActorContext actorContext = createActorContext();
        candidate = new Candidate(actorContext);

        MessageCollectorActor.expectFirstMatching(candidateActor, ElectionTimeout.class,
                actorContext.getConfigParams().getElectionTimeOutInterval().multipliedBy(6).toMillis());
    }

    @Test
    public void testHandleElectionTimeoutWhenThereAreZeroPeers() {
        RaftActorContext raftActorContext = createActorContext();
        candidate = new Candidate(raftActorContext);

        RaftActorBehavior newBehavior =
            candidate.handleMessage(candidateActor, ElectionTimeout.INSTANCE);

        assertEquals("Behavior", RaftState.Leader, newBehavior.state());
    }

    @Test
    public void testHandleElectionTimeoutWhenThereAreTwoNodeCluster() {
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setPeerAddresses(setupPeers(1));
        candidate = new Candidate(raftActorContext);

        candidate = candidate.handleMessage(candidateActor, ElectionTimeout.INSTANCE);

        assertEquals("Behavior", RaftState.Candidate, candidate.state());
    }

    @Test
    public void testBecomeLeaderOnReceivingMajorityVotesInThreeNodeCluster() {
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setLastApplied(raftActorContext.getReplicatedLog().lastIndex());
        raftActorContext.setPeerAddresses(setupPeers(2));
        candidate = new Candidate(raftActorContext);

        candidate = candidate.handleMessage(peerActors[0], new RequestVoteReply(1, true));

        assertEquals("Behavior", RaftState.Leader, candidate.state());
    }

    @Test
    public void testBecomePreLeaderOnReceivingMajorityVotesInThreeNodeCluster() {
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setLastApplied(-1);
        raftActorContext.setPeerAddresses(setupPeers(2));
        candidate = new Candidate(raftActorContext);

        candidate = candidate.handleMessage(peerActors[0], new RequestVoteReply(1, true));

        // LastApplied is -1 and behind the last index.
        assertEquals("Behavior", RaftState.PreLeader, candidate.state());
    }

    @Test
    public void testBecomeLeaderOnReceivingMajorityVotesInFiveNodeCluster() {
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setTermInfo(new TermInfo(2L, "other"));
        final var log = new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 5, 1).build();
        log.setCommitIndex(log.lastIndex());
        log.setLastApplied(log.lastIndex());
        raftActorContext.setReplicatedLog(log);
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
    public void testBecomeLeaderOnReceivingMajorityVotesWithNonVotingPeers() {
        final var raftActorContext = createActorContext();
        raftActorContext.setPeerAddresses(setupPeers(4));
        raftActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().build());
        raftActorContext.getPeerInfo("peer1").setVotingState(VotingState.NON_VOTING);
        raftActorContext.getPeerInfo("peer4").setVotingState(VotingState.NON_VOTING);
        candidate = new Candidate(raftActorContext);

        MessageCollectorActor.expectFirstMatching(peerActors[1], RequestVote.class);
        MessageCollectorActor.expectFirstMatching(peerActors[2], RequestVote.class);
        MessageCollectorActor.assertNoneMatching(peerActors[0], RequestVote.class, 300);
        MessageCollectorActor.assertNoneMatching(peerActors[3], RequestVote.class, 100);

        candidate = candidate.handleMessage(peerActors[1], new RequestVoteReply(1, false));

        assertEquals("Behavior", RaftState.Candidate, candidate.state());

        candidate = candidate.handleMessage(peerActors[2], new RequestVoteReply(1, true));

        assertEquals("Behavior", RaftState.Leader, candidate.state());
    }

    @Test
    public void testResponseToHandleAppendEntriesWithLowerTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        RaftActorBehavior newBehavior = candidate.handleMessage(peerActors[0], new AppendEntries(1, "test", 0, 0,
                List.of(), 0, -1, (short) 0));

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(peerActors[0], AppendEntriesReply.class);
        assertFalse("isSuccess", reply.isSuccess());
        assertEquals("getTerm", 2, reply.getTerm());
        assertTrue("New Behavior : " + newBehavior, newBehavior instanceof Candidate);
    }

    @Test
    public void testResponseToHandleAppendEntriesWithHigherTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        RaftActorBehavior newBehavior = candidate.handleMessage(peerActors[0], new AppendEntries(5, "test", 0, 0,
            List.of(), 0, -1, (short) 0));

        assertTrue("New Behavior : " + newBehavior, newBehavior instanceof Follower);
    }

    @Test
    public void testResponseToHandleAppendEntriesWithEqualTerm() {
        MockRaftActorContext actorContext = createActorContext();

        candidate = new Candidate(actorContext);

        setupPeers(1);
        RaftActorBehavior newBehavior = candidate.handleMessage(peerActors[0], new AppendEntries(2, "test", 0, 0,
            List.of(), 0, -1, (short) 0));

        assertTrue("New Behavior : " + newBehavior + " term = " + actorContext.currentTerm(),
                newBehavior instanceof Follower);
    }

    @Test
    public void testResponseToRequestVoteWithLowerTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        candidate.handleMessage(peerActors[0], new RequestVote(1, "test", 0, 0));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(peerActors[0], RequestVoteReply.class);
        assertFalse("isVoteGranted", reply.isVoteGranted());
        assertEquals("getTerm", 2, reply.getTerm());
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForMatches() {
        MockRaftActorContext context = createActorContext();
        context.setTermInfo(new TermInfo(1000, null));

        // Once a candidate is created it will immediately increment the current term so after
        // construction the currentTerm should be 1001
        candidate = new Candidate(context);

        setupPeers(1);
        candidate.handleMessage(peerActors[0], new RequestVote(1001, "candidate", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(peerActors[0], RequestVoteReply.class);
        assertTrue("isVoteGranted", reply.isVoteGranted());
        assertEquals("getTerm", 1001, reply.getTerm());
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForDoesNotMatch() {
        MockRaftActorContext context = createActorContext();
        context.setTermInfo(new TermInfo(1000, null));

        // Once a candidate is created it will immediately increment the current term so after
        // construction the currentTerm should be 1001
        candidate = new Candidate(context);

        setupPeers(1);

        // RequestVote candidate ID ("candidate2") does not match this candidate's votedFor
        // (it votes for itself)
        candidate.handleMessage(peerActors[0], new RequestVote(1001, "candidate2", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(peerActors[0], RequestVoteReply.class);
        assertFalse("isVoteGranted", reply.isVoteGranted());
        assertEquals("getTerm", 1001, reply.getTerm());
    }

    @Test
    public void testCandidateSchedulesElectionTimeoutImmediatelyWhenItHasNoPeers() {
        MockRaftActorContext context = createActorContext();

        Stopwatch stopwatch = Stopwatch.createStarted();

        candidate = createBehavior(context);

        MessageCollectorActor.expectFirstMatching(candidateActor, ElectionTimeout.class);

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertTrue(elapsed < context.getConfigParams().getElectionTimeOutInterval().toMillis());
    }

    @Test
    @Override
    public void testHandleAppendEntriesAddSameEntryToLog() {
        MockRaftActorContext context = createActorContext();

        context.setTermInfo(new TermInfo(2, "test"));

        // Prepare the receivers log
        MockRaftActorContext.MockPayload payload = new MockRaftActorContext.MockPayload("zero");
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

        RaftActorBehavior raftBehavior = behavior.handleMessage(candidateActor, appendEntries);

        assertEquals("Raft state", RaftState.Follower, raftBehavior.state());

        assertEquals("ReplicatedLog size", 1, context.getReplicatedLog().size());

        handleAppendEntriesAddSameEntryToLogReply(candidateActor);
    }

    @Override
    protected Candidate createBehavior(final RaftActorContext actorContext) {
        return new Candidate(actorContext);
    }

    @Override
    protected MockRaftActorContext createActorContext(final int payloadVersion) {
        return new MockRaftActorContext("candidate", getSystem(), candidateActor, payloadVersion);
    }

    private Map<String, String> setupPeers(final int count) {
        final var peerMap = new HashMap<String, String>();
        peerActors = new ActorRef[count];
        for (int i = 0; i < count; i++) {
            peerActors[i] = actorFactory.createActor(MessageCollectorActor.props(),
                    actorFactory.generateActorId("peer"));
            peerMap.put("peer" + (i + 1), peerActors[i].path().toString());
        }

        return peerMap;
    }

    @Override
    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(final MockRaftActorContext actorContext,
            final ActorRef actorRef, final RaftRPC rpc) {
        super.assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, actorRef, rpc);
        if (rpc instanceof RequestVote requestVote) {
            assertEquals("New votedFor", requestVote.getCandidateId(), actorContext.termInfo().votedFor());
        } else {
            assertEquals("New votedFor", null, actorContext.termInfo().votedFor());
        }
    }
}
