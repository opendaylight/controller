package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.internal.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

import static org.junit.Assert.assertEquals;

public class FollowerTest extends AbstractRaftActorBehaviorTest {

    private final ActorRef followerActor = getSystem().actorOf(Props.create(
        DoNothingActor.class));


    @Override protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Follower(actorContext);
    }

    @Override protected RaftActorContext createActorContext() {
        return new MockRaftActorContext("test", getSystem(), followerActor);
    }

    @Test
    public void testThatAnElectionTimeoutIsTriggered(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    Follower follower = new Follower(createActorContext(getTestActor()));

                    final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"), "ElectionTimeout") {
                        // do not put code outside this method, will run afterwards
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
    public void testHandleElectionTimeout(){
        RaftActorContext raftActorContext = createActorContext();
        Follower follower =
            new Follower(raftActorContext);

        RaftState raftState =
            follower.handleMessage(followerActor, new ElectionTimeout());

        Assert.assertEquals(RaftState.Candidate, raftState);
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNull(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    RaftActorContext context = createActorContext(getTestActor());

                    context.getTermInformation().update(1000, null);

                    RaftActorBehavior follower = createBehavior(context);

                    follower.handleMessage(getTestActor(), new RequestVote(1000, "test", 10000, 999));

                    final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"), "RequestVoteReply") {
                        // do not put code outside this method, will run afterwards
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
                protected void run() {

                    RaftActorContext context = createActorContext(getTestActor());

                    context.getTermInformation().update(1000, "test");

                    RaftActorBehavior follower = createBehavior(context);

                    follower.handleMessage(getTestActor(), new RequestVote(1000, "candidate", 10000, 999));

                    final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"), "RequestVoteReply") {
                        // do not put code outside this method, will run afterwards
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

}
