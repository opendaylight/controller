package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public abstract class AbstractRaftActorBehaviorTest extends AbstractActorTest{

    private final ActorRef behaviorActor = getSystem().actorOf(Props.create(
        DoNothingActor.class));

   @Test
    public void testHandlingOfRaftRPCWithNewerTerm() throws Exception {
        new JavaTestKit(getSystem()) {{

            assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(getTestActor(),
                createAppendEntriesWithNewerTerm());

            assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(getTestActor(),
                createAppendEntriesReplyWithNewerTerm());

            assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(getTestActor(),
                createRequestVoteWithNewerTerm());

            assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(getTestActor(),
                createRequestVoteReplyWithNewerTerm());


        }};
    }

    @Test
    public void testHandlingOfAppendEntriesWithNewerCommitIndex() throws Exception{
        new JavaTestKit(getSystem()) {{

            RaftActorContext context =
                createActorContext();

            ((MockRaftActorContext) context).setLastApplied(100);

            AppendEntries appendEntries =
                new AppendEntries(100, "leader-1", 0, 0, null, 101);

            RaftState raftState =
                createBehavior(context).handleMessage(getRef(), appendEntries);

            assertEquals(new AtomicLong(101).get(), context.getLastApplied());

        }};
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermGreaterThanCurrentTermAndSenderLogMoreUpToDate(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    RaftActorBehavior follower = createBehavior(
                        createActorContext(behaviorActor));

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
    public void testHandleRequestVoteWhenSenderTermGreaterThanCurrentTermButSenderLogLessUptoDate(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    RaftActorContext actorContext =
                        createActorContext(behaviorActor);

                    MockRaftActorContext.MockReplicatedLog log = new MockRaftActorContext.MockReplicatedLog();
                    log.setReplicatedLogEntry(new MockRaftActorContext.MockReplicatedLogEntry(20000, 1000000, ""));
                    log.setLast(
                        new MockRaftActorContext.MockReplicatedLogEntry(20000,
                            1000000, ""));

                    ((MockRaftActorContext) actorContext).setReplicatedLog(log);

                    RaftActorBehavior follower = createBehavior(actorContext);

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

                    assertEquals(false, out);
                }
            };
        }};
    }




    @Test
    public void testHandleRequestVoteWhenSenderTermLessThanCurrentTerm(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    RaftActorContext context = createActorContext(behaviorActor);

                    context.getTermInformation().update(1000, null);

                    RaftActorBehavior follower = createBehavior(context);

                    follower.handleMessage(getTestActor(), new RequestVote(999, "test", 10000, 999));

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

    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(
        ActorRef actorRef, RaftRPC rpc){
        RaftState raftState = createBehavior()
            .handleMessage(actorRef, rpc);

        assertEquals(RaftState.Follower, raftState);
    }

    protected abstract RaftActorBehavior createBehavior(RaftActorContext actorContext);

    protected RaftActorBehavior createBehavior(){
        return createBehavior(createActorContext());
    }

    protected RaftActorContext createActorContext(){
        return new MockRaftActorContext();
    }

    protected RaftActorContext createActorContext(ActorRef actor) {
        return new MockRaftActorContext("test", getSystem(), actor);
    }

    protected AppendEntries createAppendEntriesWithNewerTerm(){
        return new AppendEntries(100, "leader-1", 0, 0, null, 1);
    }

    protected AppendEntriesReply createAppendEntriesReplyWithNewerTerm(){
        return new AppendEntriesReply(100, false);
    }

    protected RequestVote createRequestVoteWithNewerTerm(){
        return new RequestVote(100, "candidate-1", 10, 100);
    }

    protected RequestVoteReply createRequestVoteReplyWithNewerTerm(){
        return new RequestVoteReply(100, false);
    }



}
