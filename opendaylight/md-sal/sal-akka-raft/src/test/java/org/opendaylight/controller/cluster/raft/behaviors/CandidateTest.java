package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.slf4j.impl.SimpleLogger;

public class CandidateTest extends AbstractRaftActorBehaviorTest {

    static {
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + MockRaftActorContext.class.getName(), "trace");
    }

    private final ActorRef candidateActor = getSystem().actorOf(Props.create(
        DoNothingActor.class));

    private final ActorRef peerActor1 = getSystem().actorOf(Props.create(
        DoNothingActor.class));

    private final ActorRef peerActor2 = getSystem().actorOf(Props.create(
        DoNothingActor.class));

    private final ActorRef peerActor3 = getSystem().actorOf(Props.create(
        DoNothingActor.class));

    private final ActorRef peerActor4 = getSystem().actorOf(Props.create(
        DoNothingActor.class));

    private final Map<String, String> onePeer = new HashMap<>();
    private final Map<String, String> twoPeers = new HashMap<>();
    private final Map<String, String> fourPeers = new HashMap<>();

    @Before
    public void setUp(){
        onePeer.put(peerActor1.path().toString(),
            peerActor1.path().toString());

        twoPeers.put(peerActor1.path().toString(),
            peerActor1.path().toString());
        twoPeers.put(peerActor2.path().toString(),
            peerActor2.path().toString());

        fourPeers.put(peerActor1.path().toString(),
            peerActor1.path().toString());
        fourPeers.put(peerActor2.path().toString(),
            peerActor2.path().toString());
        fourPeers.put(peerActor3.path().toString(),
            peerActor3.path().toString());
        fourPeers.put(peerActor4.path().toString(),
            peerActor3.path().toString());


    }

    @Test
    public void testWhenACandidateIsCreatedItIncrementsTheCurrentTermAndVotesForItself(){
        RaftActorContext raftActorContext = createActorContext();
        long expectedTerm = raftActorContext.getTermInformation().getCurrentTerm();

        new Candidate(raftActorContext);

        assertEquals(expectedTerm+1, raftActorContext.getTermInformation().getCurrentTerm());
        assertEquals(raftActorContext.getId(), raftActorContext.getTermInformation().getVotedFor());
    }

    @Test
    public void testThatAnElectionTimeoutIsTriggered(){
        new JavaTestKit(getSystem()) {{

            new Within(DefaultConfigParamsImpl.HEART_BEAT_INTERVAL.$times(6)) {
                @Override
                protected void run() {

                    Candidate candidate = new Candidate(createActorContext(getTestActor()));

                    final Boolean out = new ExpectMsg<Boolean>(DefaultConfigParamsImpl.HEART_BEAT_INTERVAL.$times(6), "ElectionTimeout") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected Boolean match(Object in) {
                            if (in instanceof ElectionTimeout) {
                                 return true;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get();

                    assertEquals(true, out);
                }
            };
        }};
    }

    @Test
    public void testHandleElectionTimeoutWhenThereAreZeroPeers(){
        RaftActorContext raftActorContext = createActorContext();
        Candidate candidate =
            new Candidate(raftActorContext);

        RaftActorBehavior raftBehavior =
            candidate.handleMessage(candidateActor, new ElectionTimeout());

        Assert.assertTrue(raftBehavior instanceof Leader);
    }

    @Test
    public void testHandleElectionTimeoutWhenThereAreTwoNodesInCluster(){
        MockRaftActorContext raftActorContext =
            (MockRaftActorContext) createActorContext();
        raftActorContext.setPeerAddresses(onePeer);
        Candidate candidate =
            new Candidate(raftActorContext);

        RaftActorBehavior raftBehavior =
            candidate.handleMessage(candidateActor, new ElectionTimeout());

        Assert.assertTrue(raftBehavior instanceof Candidate);
    }

    @Test
    public void testBecomeLeaderOnReceivingMajorityVotesInThreeNodesInCluster(){
        MockRaftActorContext raftActorContext =
            (MockRaftActorContext) createActorContext();
        raftActorContext.setPeerAddresses(twoPeers);
        Candidate candidate =
            new Candidate(raftActorContext);

        RaftActorBehavior behaviorOnFirstVote = candidate.handleMessage(peerActor1, new RequestVoteReply(0, true));

        Assert.assertTrue(behaviorOnFirstVote instanceof Leader);

    }

    @Test
    public void testBecomeLeaderOnReceivingMajorityVotesInFiveNodesInCluster(){
        MockRaftActorContext raftActorContext =
            (MockRaftActorContext) createActorContext();
        raftActorContext.setPeerAddresses(fourPeers);
        Candidate candidate =
            new Candidate(raftActorContext);

        RaftActorBehavior behaviorOnFirstVote = candidate.handleMessage(peerActor1, new RequestVoteReply(0, true));

        RaftActorBehavior behaviorOnSecondVote = candidate.handleMessage(peerActor2, new RequestVoteReply(0, true));

        Assert.assertTrue(behaviorOnFirstVote instanceof Candidate);
        Assert.assertTrue(behaviorOnSecondVote instanceof Leader);

    }

    @Test
    public void testResponseToAppendEntriesWithLowerTerm(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    Candidate candidate = new Candidate(createActorContext(getTestActor()));

                    candidate.handleMessage(getTestActor(), new AppendEntries(0, "test", 0,0,Collections.<ReplicatedLogEntry>emptyList(), 0, -1));

                    final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"), "AppendEntriesResponse") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected Boolean match(Object in) {
                            if (in instanceof AppendEntriesReply) {
                                AppendEntriesReply reply = (AppendEntriesReply) in;
                                return reply.isSuccess();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get();

                    assertEquals(false, out);
                }
            };
        }};
    }

    @Test
    public void testResponseToRequestVoteWithLowerTerm(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    Candidate candidate = new Candidate(createActorContext(getTestActor()));

                    candidate.handleMessage(getTestActor(), new RequestVote(0, "test", 0, 0));

                    final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"), "AppendEntriesResponse") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected Boolean match(Object in) {
                            if (in instanceof RequestVoteReply) {
                                RequestVoteReply reply = (RequestVoteReply) in;
                                return reply.isVoteGranted();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get();

                    assertEquals(false, out);
                }
            };
        }};
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNull(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    RaftActorContext context = createActorContext(getTestActor());

                    context.getTermInformation().update(1000, null);

                    // Once a candidate is created it will immediately increment the current term so after
                    // construction the currentTerm should be 1001
                    RaftActorBehavior follower = createBehavior(context);

                    follower.handleMessage(getTestActor(), new RequestVote(1001, "test", 10000, 999));

                    final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"), "RequestVoteReply") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected Boolean match(Object in) {
                            if (in instanceof RequestVoteReply) {
                                RequestVoteReply reply = (RequestVoteReply) in;
                                return reply.isVoteGranted();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get();

                    assertEquals(true, out);
                }
            };
        }};
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNotTheSameAsCandidateId(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    RaftActorContext context = createActorContext(getTestActor());

                    context.getTermInformation().update(1000, "test");

                    RaftActorBehavior follower = createBehavior(context);

                    follower.handleMessage(getTestActor(), new RequestVote(1001, "candidate", 10000, 999));

                    final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"), "RequestVoteReply") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected Boolean match(Object in) {
                            if (in instanceof RequestVoteReply) {
                                RequestVoteReply reply = (RequestVoteReply) in;
                                return reply.isVoteGranted();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get();

                    assertEquals(false, out);
                }
            };
        }};
    }



    @Override protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Candidate(actorContext);
    }

    @Override protected RaftActorContext createActorContext() {
        return new MockRaftActorContext("test", getSystem(), candidateActor);
    }


}
