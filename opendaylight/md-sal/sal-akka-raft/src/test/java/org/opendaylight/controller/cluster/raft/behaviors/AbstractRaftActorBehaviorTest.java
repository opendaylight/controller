package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.SerializationUtils;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractRaftActorBehaviorTest extends AbstractActorTest {

    private final ActorRef behaviorActor = getSystem().actorOf(Props.create(
        DoNothingActor.class));

    /**
     * This test checks that when a new Raft RPC message is received with a newer
     * term the RaftActor gets into the Follower state.
     *
     * @throws Exception
     */
    @Test
    public void testHandleRaftRPCWithNewerTerm() throws Exception {
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


    /**
     * This test verifies that when an AppendEntries is received with a term that
     * is less that the currentTerm of the RaftActor then the RaftActor does not
     * change it's state and it responds back with a failure
     *
     * @throws Exception
     */
    @Test
    public void testHandleAppendEntriesSenderTermLessThanReceiverTerm()
        throws Exception {
        new JavaTestKit(getSystem()) {{

            MockRaftActorContext context = (MockRaftActorContext)
                createActorContext();

            // First set the receivers term to a high number (1000)
            context.getTermInformation().update(1000, "test");

            AppendEntries appendEntries =
                new AppendEntries(100, "leader-1", 0, 0, null, 101, -1);

            RaftActorBehavior behavior = createBehavior(context);

            // Send an unknown message so that the state of the RaftActor remains unchanged
            RaftActorBehavior expected = behavior.handleMessage(getRef(), "unknown");

            RaftActorBehavior raftBehavior =
                behavior.handleMessage(getRef(), appendEntries);

            assertEquals(expected, raftBehavior);

            // Also expect an AppendEntriesReply to be sent where success is false
            final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"),
                "AppendEntriesReply") {
                // do not put code outside this method, will run afterwards
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


        }};
    }


    @Test
    public void testHandleAppendEntriesAddSameEntryToLog(){
        new JavaTestKit(getSystem()) {
            {

                MockRaftActorContext context = (MockRaftActorContext)
                    createActorContext();

                // First set the receivers term to lower number
                context.getTermInformation().update(2, "test");

                // Prepare the receivers log
                MockRaftActorContext.SimpleReplicatedLog log =
                    new MockRaftActorContext.SimpleReplicatedLog();
                log.append(
                    new MockRaftActorContext.MockReplicatedLogEntry(1, 0, new MockRaftActorContext.MockPayload("zero")));

                context.setReplicatedLog(log);

                List<ReplicatedLogEntry> entries = new ArrayList<>();
                entries.add(
                    new MockRaftActorContext.MockReplicatedLogEntry(1, 0, new MockRaftActorContext.MockPayload("zero")));

                AppendEntries appendEntries =
                    new AppendEntries(2, "leader-1", -1, 1, entries, 0, -1);

                RaftActorBehavior behavior = createBehavior(context);

                if (AbstractRaftActorBehaviorTest.this instanceof CandidateTest) {
                    // Resetting the Candidates term to make sure it will match
                    // the term sent by AppendEntries. If this was not done then
                    // the test will fail because the Candidate will assume that
                    // the message was sent to it from a lower term peer and will
                    // thus respond with a failure
                    context.getTermInformation().update(2, "test");
                }

                // Send an unknown message so that the state of the RaftActor remains unchanged
                RaftActorBehavior expected = behavior.handleMessage(getRef(), "unknown");

                RaftActorBehavior raftBehavior =
                    behavior.handleMessage(getRef(), appendEntries);

                assertEquals(expected, raftBehavior);

                assertEquals(1, log.size());


            }};
    }

    /**
     * This test verifies that when a RequestVote is received by the RaftActor
     * with a term which is greater than the RaftActors' currentTerm and the
     * senders' log is more upto date than the receiver that the receiver grants
     * the vote to the sender
     */
    @Test
    public void testHandleRequestVoteWhenSenderTermGreaterThanCurrentTermAndSenderLogMoreUpToDate() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    RaftActorBehavior behavior = createBehavior(
                        createActorContext(behaviorActor));

                    RaftActorBehavior raftBehavior = behavior.handleMessage(getTestActor(),
                        new RequestVote(1000, "test", 10000, 999));

                    if(!(behavior instanceof Follower)){
                        assertTrue(raftBehavior instanceof Follower);
                    } else {

                        final Boolean out =
                            new ExpectMsg<Boolean>(duration("1 seconds"),
                                "RequestVoteReply") {
                                // do not put code outside this method, will run afterwards
                                protected Boolean match(Object in) {
                                    if (in instanceof RequestVoteReply) {
                                        RequestVoteReply reply =
                                            (RequestVoteReply) in;
                                        return reply.isVoteGranted();
                                    } else {
                                        throw noMatch();
                                    }
                                }
                            }.get();

                        assertEquals(true, out);
                    }
                }
            };
        }};
    }

    /**
     * This test verifies that when a RaftActor receives a RequestVote message
     * with a term that is greater than it's currentTerm but a less up-to-date
     * log then the receiving RaftActor will not grant the vote to the sender
     */
    @Test
    public void testHandleRequestVoteWhenSenderTermGreaterThanCurrentTermButSenderLogLessUptoDate() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    RaftActorContext actorContext =
                        createActorContext(behaviorActor);

                    MockRaftActorContext.SimpleReplicatedLog
                        log = new MockRaftActorContext.SimpleReplicatedLog();
                    log.append(
                        new MockRaftActorContext.MockReplicatedLogEntry(20000,
                            1000000, new MockRaftActorContext.MockPayload("")));

                    ((MockRaftActorContext) actorContext).setReplicatedLog(log);

                    RaftActorBehavior behavior = createBehavior(actorContext);

                    RaftActorBehavior raftBehavior = behavior.handleMessage(getTestActor(),
                        new RequestVote(1000, "test", 10000, 999));

                    if(!(behavior instanceof Follower)){
                        assertTrue(raftBehavior instanceof Follower);
                    } else {
                        final Boolean out =
                            new ExpectMsg<Boolean>(duration("1 seconds"),
                                "RequestVoteReply") {
                                // do not put code outside this method, will run afterwards
                                protected Boolean match(Object in) {
                                    if (in instanceof RequestVoteReply) {
                                        RequestVoteReply reply =
                                            (RequestVoteReply) in;
                                        return reply.isVoteGranted();
                                    } else {
                                        throw noMatch();
                                    }
                                }
                            }.get();

                        assertEquals(false, out);
                    }
                }
            };
        }};
    }



    /**
     * This test verifies that the receiving RaftActor will not grant a vote
     * to a sender if the sender's term is lesser than the currentTerm of the
     * recipient RaftActor
     */
    @Test
    public void testHandleRequestVoteWhenSenderTermLessThanCurrentTerm() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    RaftActorContext context =
                        createActorContext(behaviorActor);

                    context.getTermInformation().update(1000, null);

                    RaftActorBehavior follower = createBehavior(context);

                    follower.handleMessage(getTestActor(),
                        new RequestVote(999, "test", 10000, 999));

                    final Boolean out =
                        new ExpectMsg<Boolean>(duration("1 seconds"),
                            "RequestVoteReply") {
                            // do not put code outside this method, will run afterwards
                            protected Boolean match(Object in) {
                                if (in instanceof RequestVoteReply) {
                                    RequestVoteReply reply =
                                        (RequestVoteReply) in;
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
    public void testFakeSnapshots() {
        MockRaftActorContext context = new MockRaftActorContext("test", getSystem(), behaviorActor);
        AbstractRaftActorBehavior behavior = new Leader(context);
        context.getTermInformation().update(1, "leader");

        //entry with 1 index=0 entry with replicatedToAllIndex = 0, does not do anything, returns the
        context.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 1, 1).build());
        context.setLastApplied(0);
        assertEquals(-1, behavior.fakeSnapshot(0, -1));
        assertEquals(1, context.getReplicatedLog().size());

        //2 entries, lastApplied still 0, no purging.
        context.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0,2,1).build());
        context.setLastApplied(0);
        assertEquals(-1, behavior.fakeSnapshot(0, -1));
        assertEquals(2, context.getReplicatedLog().size());

        //2 entries, lastApplied still 0, no purging.
        context.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0,2,1).build());
        context.setLastApplied(1);
        assertEquals(0, behavior.fakeSnapshot(0, -1));
        assertEquals(1, context.getReplicatedLog().size());

        //5 entries, lastApplied =2 and replicatedIndex = 3, but since we want to keep the lastapplied, indices 0 and 1 will only get purged
        context.setReplicatedLog(new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0,5,1).build());
        context.setLastApplied(2);
        assertEquals(1, behavior.fakeSnapshot(3, 1));
        assertEquals(3, context.getReplicatedLog().size());


    }

    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(
        ActorRef actorRef, RaftRPC rpc) {

        RaftActorContext actorContext = createActorContext();
        Payload p = new MockRaftActorContext.MockPayload("");
        setLastLogEntry(
            (MockRaftActorContext) actorContext, 0, 0, p);

        RaftActorBehavior raftBehavior = createBehavior(actorContext)
            .handleMessage(actorRef, rpc);

        assertTrue(raftBehavior instanceof Follower);
    }

    protected MockRaftActorContext.SimpleReplicatedLog setLastLogEntry(
        MockRaftActorContext actorContext, long term, long index, Payload data) {
        return setLastLogEntry(actorContext,
            new MockRaftActorContext.MockReplicatedLogEntry(term, index, data));
    }

    protected MockRaftActorContext.SimpleReplicatedLog setLastLogEntry(
        MockRaftActorContext actorContext, ReplicatedLogEntry logEntry) {
        MockRaftActorContext.SimpleReplicatedLog
            log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(logEntry);
        actorContext.setReplicatedLog(log);

        return log;
    }

    protected abstract RaftActorBehavior createBehavior(
        RaftActorContext actorContext);

    protected RaftActorBehavior createBehavior() {
        return createBehavior(createActorContext());
    }

    protected RaftActorContext createActorContext() {
        return new MockRaftActorContext();
    }

    protected RaftActorContext createActorContext(ActorRef actor) {
        return new MockRaftActorContext("test", getSystem(), actor);
    }

    protected AppendEntries createAppendEntriesWithNewerTerm() {
        return new AppendEntries(100, "leader-1", 0, 0, null, 1, -1);
    }

    protected AppendEntriesReply createAppendEntriesReplyWithNewerTerm() {
        return new AppendEntriesReply("follower-1", 100, false, 100, 100);
    }

    protected RequestVote createRequestVoteWithNewerTerm() {
        return new RequestVote(100, "candidate-1", 10, 100);
    }

    protected RequestVoteReply createRequestVoteReplyWithNewerTerm() {
        return new RequestVoteReply(100, false);
    }

    protected Object fromSerializableMessage(Object serializable){
        return SerializationUtils.fromSerializable(serializable);
    }
}
