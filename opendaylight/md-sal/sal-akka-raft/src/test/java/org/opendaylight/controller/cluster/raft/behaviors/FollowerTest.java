package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.protobuf.ByteString;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FollowerTest extends AbstractRaftActorBehaviorTest {

    private final ActorRef followerActor = getSystem().actorOf(Props.create(
        DoNothingActor.class));


    @Override protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Follower(actorContext);
    }

    @Override protected  RaftActorContext createActorContext() {
        return createActorContext(followerActor);
    }

    protected  RaftActorContext createActorContext(ActorRef actorRef){
        return new MockRaftActorContext("test", getSystem(), actorRef);
    }

    @Test
    public void testThatAnElectionTimeoutIsTriggered(){
        new JavaTestKit(getSystem()) {{

            new Within(DefaultConfigParamsImpl.HEART_BEAT_INTERVAL.$times(6)) {
                protected void run() {

                    Follower follower = new Follower(createActorContext(getTestActor()));

                    final Boolean out = new ExpectMsg<Boolean>(DefaultConfigParamsImpl.HEART_BEAT_INTERVAL.$times(6), "ElectionTimeout") {
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
            setLastLogEntry((MockRaftActorContext) context, 1, 100, new MockRaftActorContext.MockPayload(""));
            ((MockRaftActorContext) context).getReplicatedLog().setSnapshotIndex(99);

            List<ReplicatedLogEntry> entries =
                Arrays.asList(
                        (ReplicatedLogEntry) new MockRaftActorContext.MockReplicatedLogEntry(2, 101,
                                new MockRaftActorContext.MockPayload("foo"))
                );

            // The new commitIndex is 101
            AppendEntries appendEntries =
                new AppendEntries(2, "leader-1", 100, 1, entries, 101);

            RaftState raftState =
                createBehavior(context).handleMessage(getRef(), appendEntries);

            assertEquals(101L, context.getLastApplied());

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
            MockRaftActorContext.SimpleReplicatedLog mockReplicatedLog =
                setLastLogEntry(context, 20, 0, new MockRaftActorContext.MockPayload(""));

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
                new MockRaftActorContext.MockReplicatedLogEntry(1, 0, new MockRaftActorContext.MockPayload("zero")));
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 1, new MockRaftActorContext.MockPayload("one")));
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 2, new MockRaftActorContext.MockPayload("two")));

            context.setReplicatedLog(log);

            // Prepare the entries to be sent with AppendEntries
            List<ReplicatedLogEntry> entries = new ArrayList<>();
            entries.add(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 3, new MockRaftActorContext.MockPayload("three")));
            entries.add(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 4, new MockRaftActorContext.MockPayload("four")));

            // Send appendEntries with the same term as was set on the receiver
            // before the new behavior was created (1 in this case)
            // This will not work for a Candidate because as soon as a Candidate
            // is created it increments the term
            AppendEntries appendEntries =
                new AppendEntries(1, "leader-1", 2, 1, entries, 4);

            RaftActorBehavior behavior = createBehavior(context);

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
                new MockRaftActorContext.MockReplicatedLogEntry(1, 0, new MockRaftActorContext.MockPayload("zero")));
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 1, new MockRaftActorContext.MockPayload("one")));
            log.append(
                new MockRaftActorContext.MockReplicatedLogEntry(1, 2, new MockRaftActorContext.MockPayload("two")));

            context.setReplicatedLog(log);

            // Prepare the entries to be sent with AppendEntries
            List<ReplicatedLogEntry> entries = new ArrayList<>();
            entries.add(
                new MockRaftActorContext.MockReplicatedLogEntry(2, 2, new MockRaftActorContext.MockPayload("two-1")));
            entries.add(
                new MockRaftActorContext.MockReplicatedLogEntry(2, 3, new MockRaftActorContext.MockPayload("three")));

            // Send appendEntries with the same term as was set on the receiver
            // before the new behavior was created (1 in this case)
            // This will not work for a Candidate because as soon as a Candidate
            // is created it increments the term
            AppendEntries appendEntries =
                new AppendEntries(2, "leader-1", 1, 1, entries, 3);

            RaftActorBehavior behavior = createBehavior(context);

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

            assertEquals("one", log.get(1).getData().toString());

            // Check that the entry at index 2 has the new data
            assertEquals("two-1", log.get(2).getData().toString());

            assertEquals("three", log.get(3).getData().toString());

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
     * This test verifies that when InstallSnapshot is received by
     * the follower its applied correctly.
     *
     * @throws Exception
     */
    @Test
    public void testHandleInstallSnapshot() throws Exception {
        JavaTestKit javaTestKit = new JavaTestKit(getSystem()) {{

            ActorRef leaderActor = getTestActor();
            MockRaftActorContext context = (MockRaftActorContext)
                    createActorContext(getRef());

            RaftActorBehavior follower = createBehavior(context);

            HashMap<String, String> followerSnapshot = new HashMap<>();
            followerSnapshot.put("1", "A");
            followerSnapshot.put("2", "B");
            followerSnapshot.put("3", "C");

            MockFollower mockFollower = new MockFollower(context);

            ByteString bsSnapshot  = toByteString(followerSnapshot);

            ByteString lastChunkData = mockFollower.lastSnapshot(bsSnapshot);

            final InstallSnapshot installSnapshot = new InstallSnapshot(1, "leader-1", 3, 1, lastChunkData, 3, 3);

            follower.handleMessage(leaderActor, installSnapshot);
            String[] matches = new ReceiveWhile<String>(String.class, duration("2 seconds")) {
                @Override
                protected String match(Object o) throws Exception {
                    if (o instanceof ApplySnapshot) {
                        ApplySnapshot as = (ApplySnapshot)o;
                        if (as.getSnapshot().getLastIndex() != installSnapshot.getLastIncludedIndex()) {
                            return "applySnapshot-lastIndex-mismatch";
                        }
                        if (as.getSnapshot().getLastAppliedTerm() != installSnapshot.getLastIncludedTerm()) {
                            return "applySnapshot-lastAppliedTerm-mismatch";
                        }
                        if (as.getSnapshot().getLastAppliedIndex() != installSnapshot.getLastIncludedIndex()) {
                            return "applySnapshot-lastAppliedIndex-mismatch";
                        }
                        if (as.getSnapshot().getLastTerm() != installSnapshot.getLastIncludedTerm()) {
                            return "applySnapshot-lastTerm-mismatch";
                        }
                       return "applySnapshot";
                    } else
                    if (o instanceof InstallSnapshotReply) {
                        InstallSnapshotReply isr = (InstallSnapshotReply)o;
                        if (isr.getChunkIndex() != installSnapshot.getChunkIndex()) {
                            return "installSnapshotReply-chunkIndex-mismatch";
                        }
                       return "installSnapshotReply";
                    }
                    return "ignoreCase";
                }
            }.get();

            String applySnapshotMatch = "";
            String installSnapshotReplyMatch = "";
            for (String reply: matches) {
                if (reply.startsWith("applySnapshot")) {
                    applySnapshotMatch = reply;
                } else if (reply.startsWith("installSnapshotReply")) {
                    installSnapshotReplyMatch = reply;
               }
            }

            assertEquals("applySnapshot", applySnapshotMatch);
            assertEquals("installSnapshotReply", installSnapshotReplyMatch);
        }};
    }

    class MockFollower extends Follower {

        private int offset = 0;
        private int start;
        public ByteString snapshotChunksCollected = ByteString.EMPTY;

        public MockFollower(RaftActorContext context) {
            super(context);
        }

        public ByteString getNextChunk (ByteString bs){
            int snapshotLength = bs.size();
            int start = offset;
            int size = 50;
            if (50 > snapshotLength) {
                size = snapshotLength;
            } else {
                if ((start + 50) > snapshotLength) {
                    size = snapshotLength - start;
                }
            }
            offset = offset + 50;
            return bs.substring(start, start + size);
        }

        public ByteString lastSnapshot (ByteString bs) {
            int snapshotLength = bs.size();
            ByteString chunkData = ByteString.EMPTY;
            ByteString lastChunkData = ByteString.EMPTY;

            while ((offset+50) < snapshotLength ) {
                chunkData = getNextChunk(bs);
                snapshotChunksCollected = snapshotChunksCollected.concat(chunkData);
            }
            lastChunkData = getNextChunk(bs);
            return lastChunkData;
        }
    }

    private ByteString toByteString(Map<String, String> state) {
        ByteArrayOutputStream b = null;
        ObjectOutputStream o = null;
        try {
            try {
                b = new ByteArrayOutputStream();
                o = new ObjectOutputStream(b);
                o.writeObject(state);
                byte[] snapshotBytes = b.toByteArray();
                return ByteString.copyFrom(snapshotBytes);
            } finally {
                if (o != null) {
                    o.flush();
                    o.close();
                }
                if (b != null) {
                    b.close();
                }
            }
        } catch (IOException e) {
            org.junit.Assert.fail("IOException in converting Hashmap to Bytestring:" + e);
        }
        return null;
    }
}
