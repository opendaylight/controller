/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.ElectionTerm;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContextImpl;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.VotingState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CandidateTest extends AbstractRaftActorBehaviorTest<Candidate> {
    static final Logger LOG = LoggerFactory.getLogger(CandidateTest.class);

    private final TestActorRef<MessageCollectorActor> candidateActor = actorFactory.createTestActor(
            Props.create(MessageCollectorActor.class), actorFactory.generateActorId("candidate"));

    private TestActorRef<MessageCollectorActor>[] peerActors;

    private RaftActorBehavior candidate;

    @Before
    public void setUp(){
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if(candidate != null) {
            candidate.close();
        }

        super.tearDown();
    }

    @Test
    public void testWhenACandidateIsCreatedItIncrementsTheCurrentTermAndVotesForItself(){
        RaftActorContext raftActorContext = createActorContext();
        long expectedTerm = raftActorContext.getTermInformation().getCurrentTerm();

        candidate = new Candidate(raftActorContext);

        assertEquals("getCurrentTerm", expectedTerm+1, raftActorContext.getTermInformation().getCurrentTerm());
        assertEquals("getVotedFor", raftActorContext.getId(), raftActorContext.getTermInformation().getVotedFor());
    }

    @Test
    public void testThatAnElectionTimeoutIsTriggered(){
         MockRaftActorContext actorContext = createActorContext();
         candidate = new Candidate(actorContext);

         MessageCollectorActor.expectFirstMatching(candidateActor, ElectionTimeout.class,
                 actorContext.getConfigParams().getElectionTimeOutInterval().$times(6).toMillis());
    }

    @Test
    public void testHandleElectionTimeoutWhenThereAreZeroPeers(){
        RaftActorContext raftActorContext = createActorContext();
        candidate = new Candidate(raftActorContext);

        RaftActorBehavior newBehavior =
            candidate.handleMessage(candidateActor, ElectionTimeout.INSTANCE);

        assertEquals("Behavior", RaftState.Leader, newBehavior.state());
    }

    @Test
    public void testHandleElectionTimeoutWhenThereAreTwoNodeCluster(){
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setPeerAddresses(setupPeers(1));
        candidate = new Candidate(raftActorContext);

        candidate = candidate.handleMessage(candidateActor, ElectionTimeout.INSTANCE);

        assertEquals("Behavior", RaftState.Candidate, candidate.state());
    }

    @Test
    public void testBecomeLeaderOnReceivingMajorityVotesInThreeNodeCluster(){
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setPeerAddresses(setupPeers(2));
        candidate = new Candidate(raftActorContext);

        candidate = candidate.handleMessage(peerActors[0], new RequestVoteReply(1, true));

        assertEquals("Behavior", RaftState.Leader, candidate.state());
    }

    @Test
    public void testBecomeLeaderOnReceivingMajorityVotesInFiveNodeCluster(){
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.getTermInformation().update(2L, "other");
        raftActorContext.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().
                createEntries(0, 5, 1).build());
        raftActorContext.setPeerAddresses(setupPeers(4));
        candidate = new Candidate(raftActorContext);

        RequestVote requestVote = MessageCollectorActor.expectFirstMatching(peerActors[0], RequestVote.class);
        assertEquals("getTerm", 3L, requestVote.getTerm());
        assertEquals("getCandidateId", raftActorContext.getId(), requestVote.getCandidateId());
        assertEquals("getLastLogTerm", 1L, requestVote.getLastLogTerm());
        assertEquals("getLastLogIndex", 4L, requestVote.getLastLogIndex());

        MessageCollectorActor.expectFirstMatching(peerActors[1], RequestVote.class);
        MessageCollectorActor.expectFirstMatching(peerActors[2], RequestVote.class);
        MessageCollectorActor.expectFirstMatching(peerActors[3], RequestVote.class);

        // First peers denies the vote.
        candidate = candidate.handleMessage(peerActors[0], new RequestVoteReply(1, false));

        assertEquals("Behavior", RaftState.Candidate, candidate.state());

        candidate = candidate.handleMessage(peerActors[1], new RequestVoteReply(1, true));

        assertEquals("Behavior", RaftState.Candidate, candidate.state());

        candidate = candidate.handleMessage(peerActors[2], new RequestVoteReply(1, true));

        assertEquals("Behavior", RaftState.Leader, candidate.state());
    }

    @Test
    public void testBecomeLeaderOnReceivingMajorityVotesWithNonVotingPeers(){
        ElectionTerm mockElectionTerm = Mockito.mock(ElectionTerm.class);
        Mockito.doReturn(1L).when(mockElectionTerm).getCurrentTerm();
        RaftActorContext raftActorContext = new RaftActorContextImpl(candidateActor, candidateActor.actorContext(),
                "candidate", mockElectionTerm, -1, -1, setupPeers(4), new DefaultConfigParamsImpl(),
                new NonPersistentDataProvider(), LOG);
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
                Collections.<ReplicatedLogEntry>emptyList(), 0, -1, (short) 0));

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(
                peerActors[0], AppendEntriesReply.class);
        assertEquals("isSuccess", false, reply.isSuccess());
        assertEquals("getTerm", 2, reply.getTerm());
        assertTrue("New Behavior : " + newBehavior, newBehavior instanceof Candidate);
    }

    @Test
    public void testResponseToHandleAppendEntriesWithHigherTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        RaftActorBehavior newBehavior = candidate.handleMessage(peerActors[0], new AppendEntries(5, "test", 0, 0,
                Collections.<ReplicatedLogEntry>emptyList(), 0, -1, (short) 0));

        assertTrue("New Behavior : " + newBehavior, newBehavior instanceof Follower);
    }

    @Test
    public void testResponseToHandleAppendEntriesWithEqualTerm() {
        MockRaftActorContext actorContext = createActorContext();

        candidate = new Candidate(actorContext);

        setupPeers(1);
        RaftActorBehavior newBehavior = candidate.handleMessage(peerActors[0], new AppendEntries(2, "test", 0, 0,
                Collections.<ReplicatedLogEntry>emptyList(), 0, -1, (short) 0));

        assertTrue("New Behavior : " + newBehavior + " term = " + actorContext.getTermInformation().getCurrentTerm(),
                newBehavior instanceof Follower);
    }


    @Test
    public void testResponseToRequestVoteWithLowerTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        candidate.handleMessage(peerActors[0], new RequestVote(1, "test", 0, 0));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(
                peerActors[0], RequestVoteReply.class);
        assertEquals("isVoteGranted", false, reply.isVoteGranted());
        assertEquals("getTerm", 2, reply.getTerm());
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForMatches() {
        MockRaftActorContext context = createActorContext();
        context.getTermInformation().update(1000, null);

        // Once a candidate is created it will immediately increment the current term so after
        // construction the currentTerm should be 1001
        candidate = new Candidate(context);

        setupPeers(1);
        candidate.handleMessage(peerActors[0], new RequestVote(1001, context.getId(), 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(
                peerActors[0], RequestVoteReply.class);
        assertEquals("isVoteGranted", true, reply.isVoteGranted());
        assertEquals("getTerm", 1001, reply.getTerm());
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForDoesNotMatch() {
        MockRaftActorContext context = createActorContext();
        context.getTermInformation().update(1000, null);

        // Once a candidate is created it will immediately increment the current term so after
        // construction the currentTerm should be 1001
        candidate = new Candidate(context);

        setupPeers(1);

        // RequestVote candidate ID ("candidate2") does not match this candidate's votedFor
        // (it votes for itself)
        candidate.handleMessage(peerActors[0], new RequestVote(1001, "candidate2", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(
                peerActors[0], RequestVoteReply.class);
        assertEquals("isVoteGranted", false, reply.isVoteGranted());
        assertEquals("getTerm", 1001, reply.getTerm());
    }

    @Test
    public void testCandidateSchedulesElectionTimeoutImmediatelyWhenItHasNoPeers(){
        MockRaftActorContext context = createActorContext();

        Stopwatch stopwatch = Stopwatch.createStarted();

        candidate = createBehavior(context);

        MessageCollectorActor.expectFirstMatching(candidateActor, ElectionTimeout.class);

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertTrue(elapsed < context.getConfigParams().getElectionTimeOutInterval().toMillis());
    }

    @Test
    @Override
    public void testHandleAppendEntriesAddSameEntryToLog() throws Exception {
        MockRaftActorContext context = createActorContext();

        context.getTermInformation().update(2, "test");

        // Prepare the receivers log
        MockRaftActorContext.MockPayload payload = new MockRaftActorContext.MockPayload("zero");
        setLastLogEntry(context, 2, 0, payload);

        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(new MockRaftActorContext.MockReplicatedLogEntry(2, 0, payload));

        AppendEntries appendEntries = new AppendEntries(2, "leader-1", -1, -1, entries, 2, -1, (short)0);

        behavior = createBehavior(context);

        // Resetting the Candidates term to make sure it will match
        // the term sent by AppendEntries. If this was not done then
        // the test will fail because the Candidate will assume that
        // the message was sent to it from a lower term peer and will
        // thus respond with a failure
        context.getTermInformation().update(2, "test");

        // Send an unknown message so that the state of the RaftActor remains unchanged
        RaftActorBehavior expected = behavior.handleMessage(candidateActor, "unknown");

        RaftActorBehavior raftBehavior = behavior.handleMessage(candidateActor, appendEntries);

        assertEquals("Raft state", RaftState.Follower, raftBehavior.state());

        assertEquals("ReplicatedLog size", 1, context.getReplicatedLog().size());

        handleAppendEntriesAddSameEntryToLogReply(candidateActor);
    }

    @Override
    protected Candidate createBehavior(final RaftActorContext actorContext) {
        return new Candidate(actorContext);
    }

    @Override protected MockRaftActorContext createActorContext() {
        return new MockRaftActorContext("candidate", getSystem(), candidateActor);
    }

    private Map<String, String> setupPeers(final int count) {
        Map<String, String> peerMap = new HashMap<>();
        peerActors = new TestActorRef[count];
        for(int i = 0; i < count; i++) {
            peerActors[i] = actorFactory.createTestActor(Props.create(MessageCollectorActor.class),
                    actorFactory.generateActorId("peer"));
            peerMap.put("peer" + (i+1), peerActors[i].path().toString());
        }

        return peerMap;
    }

    @Override
    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(final MockRaftActorContext actorContext,
            final ActorRef actorRef, final RaftRPC rpc) throws Exception {
        super.assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, actorRef, rpc);
        assertEquals("New votedFor", null, actorContext.getTermInformation().getVotedFor());
    }
}
