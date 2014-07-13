package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
     * This test verifies that when an AppendEntries RPC is received by a RaftActor
     * with a commitIndex that is greater than what has been applied to the
     * state machine of the RaftActor, the RaftActor applies the state and
     * sets it current applied state to the commitIndex of the sender.
     *
     * @throws Exception
     */
    @Test
    public void testHandleAppendEntriesWithNewerCommitIndex() throws Exception {
        new JavaTestKit(getSystem()) {{

            RaftActorContext context =
                createActorContext();

            context.setLastApplied(100);
            setLastLogEntry((MockRaftActorContext) context, 0, 0, "");

            // The new commitIndex is 101
            AppendEntries appendEntries =
                new AppendEntries(100, "leader-1", 0, 0, null, 101);

            RaftState raftState =
                createBehavior(context).handleMessage(getRef(), appendEntries);

            assertEquals(101L, context.getLastApplied());

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
                new AppendEntries(100, "leader-1", 0, 0, null, 101);

            RaftActorBehavior behavior = createBehavior(context);

            // Send an unknown message so that the state of the RaftActor remains unchanged
            RaftState expected = behavior.handleMessage(getRef(), "unknown");

            RaftState raftState =
                behavior.handleMessage(getRef(), appendEntries);

            assertEquals(expected, raftState);

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

    /**
     * This test verifies that when an AppendEntries is received a specific prevLogTerm
     * which does not match the term that is in RaftActors log entry at prevLogIndex
     * then the RaftActor does not change it's state and it returns a failure.
     *
     * @throws Exception
     */
    @Test
    public void testHandleAppendEntriesSenderPrevLogTermNotSameAsReceiverPrevLogTerm()
        throws Exception {
        new JavaTestKit(getSystem()) {{

            MockRaftActorContext context = (MockRaftActorContext)
                createActorContext();

            // First set the receivers term to lower number
            context.getTermInformation().update(95, "test");

            // Set the last log entry term for the receiver to be greater than
            // what we will be sending as the prevLogTerm in AppendEntries
            MockRaftActorContext.MockReplicatedLog mockReplicatedLog =
                setLastLogEntry(context, 20, 0, "");

            // Also set the entry at index 0 with term 20 which will be greater
            // than the prevLogTerm sent by the sender
            mockReplicatedLog.setReplicatedLogEntry(
                new MockRaftActorContext.MockReplicatedLogEntry(20, 0, ""));

            // AppendEntries is now sent with a bigger term
            // this will set the receivers term to be the same as the sender's term
            AppendEntries appendEntries =
                new AppendEntries(100, "leader-1", 0, 0, null, 101);

            RaftActorBehavior behavior = createBehavior(context);

            // Send an unknown message so that the state of the RaftActor remains unchanged
            RaftState expected = behavior.handleMessage(getRef(), "unknown");

            RaftState raftState =
                behavior.handleMessage(getRef(), appendEntries);

            assertEquals(expected, raftState);

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

    /**
     * This test verifies that when a new AppendEntries message is received with
     * new entries and the logs of the sender and receiver match that the new
     * entries get added to the log and the log is incremented by the number of
     * entries received in appendEntries
     *
     * @throws Exception
     */
    @Test
    public void testHandleAppendEntriesAddNewEntries() throws Exception {
        new JavaTestKit(getSystem()) {{

            MockRaftActorContext context = (MockRaftActorContext)
                createActorContext();

            // First set the receivers term to lower number
            context.getTermInformation().update(1, "test");

            // Prepare the receivers log
            MockRaftActorContext.SimpleReplicatedLog log =
                new MockRaftActorContext.SimpleReplicatedLog();
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 0, "zero"));
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 1, "one"));
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 2, "two"));

            context.setReplicatedLog(log);

            // Prepare the entries to be sent with AppendEntries
            List<ReplicatedLogEntry> entries = new ArrayList<>();
            entries.add(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 3, "three"));
            entries.add(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 4, "four"));

            // Send appendEntries with the same term as was set on the receiver
            // before the new behavior was created (1 in this case)
            // This will not work for a Candidate because as soon as a Candidate
            // is created it increments the term
            AppendEntries appendEntries =
                new AppendEntries(1, "leader-1", 2, 1, entries, 101);

            RaftActorBehavior behavior = createBehavior(context);

            if (AbstractRaftActorBehaviorTest.this instanceof CandidateTest) {
                // Resetting the Candidates term to make sure it will match
                // the term sent by AppendEntries. If this was not done then
                // the test will fail because the Candidate will assume that
                // the message was sent to it from a lower term peer and will
                // thus respond with a failure
                context.getTermInformation().update(1, "test");
            }

            // Send an unknown message so that the state of the RaftActor remains unchanged
            RaftState expected = behavior.handleMessage(getRef(), "unknown");

            RaftState raftState =
                behavior.handleMessage(getRef(), appendEntries);

            assertEquals(expected, raftState);
            assertEquals(5, log.last().getIndex() + 1);
            assertNotNull(log.get(3));
            assertNotNull(log.get(4));

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

            assertEquals(true, out);


        }};
    }

    /**
     * This test verifies that when a new AppendEntries message is received with
     * new entries and the logs of the sender and receiver are out-of-sync that
     * the log is first corrected by removing the out of sync entries from the
     * log and then adding in the new entries sent with the AppendEntries message
     *
     * @throws Exception
     */
    @Test
    public void testHandleAppendEntriesCorrectReceiverLogEntries()
        throws Exception {
        new JavaTestKit(getSystem()) {{

            MockRaftActorContext context = (MockRaftActorContext)
                createActorContext();

            // First set the receivers term to lower number
            context.getTermInformation().update(2, "test");

            // Prepare the receivers log
            MockRaftActorContext.SimpleReplicatedLog log =
                new MockRaftActorContext.SimpleReplicatedLog();
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 0, "zero"));
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 1, "one"));
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 2, "two"));

            context.setReplicatedLog(log);

            // Prepare the entries to be sent with AppendEntries
            List<ReplicatedLogEntry> entries = new ArrayList<>();
            entries.add(
                new MockRaftActorContext.MockReplicatedLogEntry(2, 2, "two-1"));
            entries.add(
                new MockRaftActorContext.MockReplicatedLogEntry(2, 3, "three"));

            // Send appendEntries with the same term as was set on the receiver
            // before the new behavior was created (1 in this case)
            // This will not work for a Candidate because as soon as a Candidate
            // is created it increments the term
            AppendEntries appendEntries =
                new AppendEntries(2, "leader-1", 1, 1, entries, 101);

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
            RaftState expected = behavior.handleMessage(getRef(), "unknown");

            RaftState raftState =
                behavior.handleMessage(getRef(), appendEntries);

            assertEquals(expected, raftState);

            // The entry at index 2 will be found out-of-sync with the leader
            // and will be removed
            // Then the two new entries will be added to the log
            // Thus making the log to have 4 entries
            assertEquals(4, log.last().getIndex() + 1);
            assertNotNull(log.get(2));

            // Check that the entry at index 2 has the new data
            assertEquals("two-1", log.get(2).getData());
            assertNotNull(log.get(3));

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

            assertEquals(true, out);


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

                    RaftActorBehavior follower = createBehavior(
                        createActorContext(behaviorActor));

                    follower.handleMessage(getTestActor(),
                        new RequestVote(1000, "test", 10000, 999));

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

                    MockRaftActorContext.MockReplicatedLog
                        log = new MockRaftActorContext.MockReplicatedLog();
                    log.setReplicatedLogEntry(
                        new MockRaftActorContext.MockReplicatedLogEntry(20000,
                            1000000, ""));
                    log.setLast(
                        new MockRaftActorContext.MockReplicatedLogEntry(20000,
                            1000000, "")
                    );

                    ((MockRaftActorContext) actorContext).setReplicatedLog(log);

                    RaftActorBehavior follower = createBehavior(actorContext);

                    follower.handleMessage(getTestActor(),
                        new RequestVote(1000, "test", 10000, 999));

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

    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(
        ActorRef actorRef, RaftRPC rpc) {

        RaftActorContext actorContext = createActorContext();
        setLastLogEntry(
            (MockRaftActorContext) actorContext, 0, 0, "");

        RaftState raftState = createBehavior(actorContext)
            .handleMessage(actorRef, rpc);

        assertEquals(RaftState.Follower, raftState);
    }

    protected MockRaftActorContext.MockReplicatedLog setLastLogEntry(
        MockRaftActorContext actorContext, long term, long index, Object data) {
        return setLastLogEntry(actorContext,
            new MockRaftActorContext.MockReplicatedLogEntry(term, index, data));
    }

    protected MockRaftActorContext.MockReplicatedLog setLastLogEntry(
        MockRaftActorContext actorContext, ReplicatedLogEntry logEntry) {
        MockRaftActorContext.MockReplicatedLog
            log = new MockRaftActorContext.MockReplicatedLog();
        // By default MockReplicateLog has last entry set to (1,1,"")
        log.setLast(logEntry);
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
        return new AppendEntries(100, "leader-1", 0, 0, null, 1);
    }

    protected AppendEntriesReply createAppendEntriesReplyWithNewerTerm() {
        return new AppendEntriesReply(100, false);
    }

    protected RequestVote createRequestVoteWithNewerTerm() {
        return new RequestVote(100, "candidate-1", 10, 100);
    }

    protected RequestVoteReply createRequestVoteReplyWithNewerTerm() {
        return new RequestVoteReply(100, false);
    }



}
