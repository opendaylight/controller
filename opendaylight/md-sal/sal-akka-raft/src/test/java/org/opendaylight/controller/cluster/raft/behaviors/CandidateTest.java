package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import com.google.common.base.Stopwatch;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class CandidateTest extends AbstractRaftActorBehaviorTest {

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
            candidate.handleMessage(candidateActor, new ElectionTimeout());

        assertEquals("Behavior", RaftState.Leader, newBehavior.state());
    }

    @Test
    public void testHandleElectionTimeoutWhenThereAreTwoNodeCluster(){
        MockRaftActorContext raftActorContext = createActorContext();
        raftActorContext.setPeerAddresses(setupPeers(1));
        candidate = new Candidate(raftActorContext);

        candidate = candidate.handleMessage(candidateActor, new ElectionTimeout());

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
        raftActorContext.setPeerAddresses(setupPeers(4));
        candidate = new Candidate(raftActorContext);

        // First peers denies the vote.
        candidate = candidate.handleMessage(peerActors[0], new RequestVoteReply(1, false));

        assertEquals("Behavior", RaftState.Candidate, candidate.state());

        candidate = candidate.handleMessage(peerActors[1], new RequestVoteReply(1, true));

        assertEquals("Behavior", RaftState.Candidate, candidate.state());

        candidate = candidate.handleMessage(peerActors[2], new RequestVoteReply(1, true));

        assertEquals("Behavior", RaftState.Leader, candidate.state());
    }

    @Test
    public void testResponseToHandleAppendEntriesWithLowerTerm() {
        candidate = new Candidate(createActorContext());

        setupPeers(1);
        candidate.handleMessage(peerActors[0], new AppendEntries(1, "test", 0, 0,
                Collections.<ReplicatedLogEntry>emptyList(), 0, -1));

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(
                peerActors[0], AppendEntriesReply.class);
        assertEquals("isSuccess", false, reply.isSuccess());
        assertEquals("getTerm", 2, reply.getTerm());
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


    @Override
    protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Candidate(actorContext);
    }

    @Override protected MockRaftActorContext createActorContext() {
        return new MockRaftActorContext("candidate", getSystem(), candidateActor);
    }

    private Map<String, String> setupPeers(int count) {
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
    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(RaftActorContext actorContext,
            ActorRef actorRef, RaftRPC rpc) throws Exception {
        super.assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, actorRef, rpc);
        assertEquals("New votedFor", null, actorContext.getTermInformation().getVotedFor());
    }
}
