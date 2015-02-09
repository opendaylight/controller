package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
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
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.impl.SimpleLogger;

public class FollowerTest extends AbstractRaftActorBehaviorTest {

    static {
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + MockRaftActorContext.class.getName(), "trace");
    }

    private final ActorRef followerActor = getSystem().actorOf(Props.create(
        DoNothingActor.class));


    @Override protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Follower(actorContext);
    }

    @Override protected  RaftActorContext createActorContext() {
        return createActorContext(followerActor);
    }

    @Override
    protected  RaftActorContext createActorContext(ActorRef actorRef){
        return new MockRaftActorContext("test", getSystem(), actorRef);
    }

    @Test
    public void testThatAnElectionTimeoutIsTriggered(){
        new JavaTestKit(getSystem()) {{

            new Within(DefaultConfigParamsImpl.HEART_BEAT_INTERVAL.$times(6)) {
                @Override
                protected void run() {

                    Follower follower = new Follower(createActorContext(getTestActor()));

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
    public void testHandleElectionTimeout(){
        RaftActorContext raftActorContext = createActorContext();
        Follower follower =
            new Follower(raftActorContext);

        RaftActorBehavior raftBehavior =
            follower.handleMessage(followerActor, new ElectionTimeout());

        assertTrue(raftBehavior instanceof Candidate);
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNull(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    RaftActorContext context = createActorContext(getTestActor());

                    context.getTermInformation().update(1000, null);

                    RaftActorBehavior follower = createBehavior(context);

                    follower.handleMessage(getTestActor(), new RequestVote(1000, "test", 10000, 999));

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

                    follower.handleMessage(getTestActor(), new RequestVote(1000, "candidate", 10000, 999));

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
            setLastLogEntry((MockRaftActorContext) context, 1, 100,
                new MockRaftActorContext.MockPayload(""));
            ((MockRaftActorContext) context).getReplicatedLog().setSnapshotIndex(99);

            List<ReplicatedLogEntry> entries =
                Arrays.asList(
                        (ReplicatedLogEntry) new MockRaftActorContext.MockReplicatedLogEntry(2, 101,
                                new MockRaftActorContext.MockPayload("foo"))
                );

            // The new commitIndex is 101
            AppendEntries appendEntries =
                new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100);

            RaftActorBehavior raftBehavior =
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
                new AppendEntries(1, "leader-1", 2, 1, entries, 4, -1);

            RaftActorBehavior behavior = createBehavior(context);

            // Send an unknown message so that the state of the RaftActor remains unchanged
            RaftActorBehavior expected = behavior.handleMessage(getRef(), "unknown");

            RaftActorBehavior raftBehavior =
                behavior.handleMessage(getRef(), appendEntries);

            assertEquals(expected, raftBehavior);
            assertEquals(5, log.last().getIndex() + 1);
            assertNotNull(log.get(3));
            assertNotNull(log.get(4));

            // Also expect an AppendEntriesReply to be sent where success is false
            final Boolean out = new ExpectMsg<Boolean>(duration("1 seconds"),
                "AppendEntriesReply") {
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
                new AppendEntries(2, "leader-1", 1, 1, entries, 3, -1);

            RaftActorBehavior behavior = createBehavior(context);

            // Send an unknown message so that the state of the RaftActor remains unchanged
            RaftActorBehavior expected = behavior.handleMessage(getRef(), "unknown");

            RaftActorBehavior raftBehavior =
                behavior.handleMessage(getRef(), appendEntries);

            assertEquals(expected, raftBehavior);

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

            assertEquals(true, out);


        }};
    }

    @Test
    public void testHandleAppendEntriesPreviousLogEntryMissing(){
        new JavaTestKit(getSystem()) {{

            MockRaftActorContext context = (MockRaftActorContext)
                    createActorContext();

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
                    new MockRaftActorContext.MockReplicatedLogEntry(1, 4, new MockRaftActorContext.MockPayload("two-1")));

            AppendEntries appendEntries =
                    new AppendEntries(1, "leader-1", 3, 1, entries, 4, -1);

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

        }};

    }

    @Test
    public void testHandleAppendAfterInstallingSnapshot(){
        new JavaTestKit(getSystem()) {{

            MockRaftActorContext context = (MockRaftActorContext)
                    createActorContext();


            // Prepare the receivers log
            MockRaftActorContext.SimpleReplicatedLog log =
                    new MockRaftActorContext.SimpleReplicatedLog();

            // Set up a log as if it has been snapshotted
            log.setSnapshotIndex(3);
            log.setSnapshotTerm(1);

            context.setReplicatedLog(log);

            // Prepare the entries to be sent with AppendEntries
            List<ReplicatedLogEntry> entries = new ArrayList<>();
            entries.add(
                    new MockRaftActorContext.MockReplicatedLogEntry(1, 4, new MockRaftActorContext.MockPayload("two-1")));

            AppendEntries appendEntries =
                    new AppendEntries(1, "leader-1", 3, 1, entries, 4, 3);

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

            ActorRef leaderActor = getSystem().actorOf(Props.create(
                MessageCollectorActor.class));

            MockRaftActorContext context = (MockRaftActorContext)
                createActorContext(getRef());

            Follower follower = (Follower)createBehavior(context);

            HashMap<String, String> followerSnapshot = new HashMap<>();
            followerSnapshot.put("1", "A");
            followerSnapshot.put("2", "B");
            followerSnapshot.put("3", "C");

            ByteString bsSnapshot  = toByteString(followerSnapshot);
            ByteString chunkData = ByteString.EMPTY;
            int offset = 0;
            int snapshotLength = bsSnapshot.size();
            int i = 1;
            int chunkIndex = 1;

            do {
                chunkData = getNextChunk(bsSnapshot, offset);
                final InstallSnapshot installSnapshot =
                    new InstallSnapshot(1, "leader-1", i, 1,
                        chunkData, chunkIndex, 3);
                follower.handleMessage(leaderActor, installSnapshot);
                offset = offset + 50;
                i++;
                chunkIndex++;
            } while ((offset+50) < snapshotLength);

            final InstallSnapshot installSnapshot3 = new InstallSnapshot(1, "leader-1", 3, 1, chunkData, chunkIndex, 3);
            follower.handleMessage(leaderActor, installSnapshot3);

            String[] matches = new ReceiveWhile<String>(String.class, duration("2 seconds")) {
                @Override
                protected String match(Object o) throws Exception {
                    if (o instanceof ApplySnapshot) {
                        ApplySnapshot as = (ApplySnapshot)o;
                        if (as.getSnapshot().getLastIndex() != installSnapshot3.getLastIncludedIndex()) {
                            return "applySnapshot-lastIndex-mismatch";
                        }
                        if (as.getSnapshot().getLastAppliedTerm() != installSnapshot3.getLastIncludedTerm()) {
                            return "applySnapshot-lastAppliedTerm-mismatch";
                        }
                        if (as.getSnapshot().getLastAppliedIndex() != installSnapshot3.getLastIncludedIndex()) {
                            return "applySnapshot-lastAppliedIndex-mismatch";
                        }
                        if (as.getSnapshot().getLastTerm() != installSnapshot3.getLastIncludedTerm()) {
                            return "applySnapshot-lastTerm-mismatch";
                        }
                        return "applySnapshot";
                    }

                    return "ignoreCase";
                }
            }.get();

            // Verify that after a snapshot is successfully applied the collected snapshot chunks is reset to empty
            assertEquals(ByteString.EMPTY, follower.getSnapshotChunksCollected());

            String applySnapshotMatch = "";
            for (String reply: matches) {
                if (reply.startsWith("applySnapshot")) {
                    applySnapshotMatch = reply;
                }
            }

            assertEquals("applySnapshot", applySnapshotMatch);

            Object messages = executeLocalOperation(leaderActor, "get-all-messages");

            assertNotNull(messages);
            assertTrue(messages instanceof List);
            List<Object> listMessages = (List<Object>) messages;

            int installSnapshotReplyReceivedCount = 0;
            for (Object message: listMessages) {
                if (message instanceof InstallSnapshotReply) {
                    ++installSnapshotReplyReceivedCount;
                }
            }

            assertEquals(3, installSnapshotReplyReceivedCount);

        }};
    }

    @Test
    public void testHandleOutOfSequenceInstallSnapshot() throws Exception {
        JavaTestKit javaTestKit = new JavaTestKit(getSystem()) {
            {

                ActorRef leaderActor = getSystem().actorOf(Props.create(
                        MessageCollectorActor.class));

                MockRaftActorContext context = (MockRaftActorContext)
                        createActorContext(getRef());

                Follower follower = (Follower) createBehavior(context);

                HashMap<String, String> followerSnapshot = new HashMap<>();
                followerSnapshot.put("1", "A");
                followerSnapshot.put("2", "B");
                followerSnapshot.put("3", "C");

                ByteString bsSnapshot = toByteString(followerSnapshot);

                final InstallSnapshot installSnapshot = new InstallSnapshot(1, "leader-1", 3, 1, getNextChunk(bsSnapshot, 10), 3, 3);
                follower.handleMessage(leaderActor, installSnapshot);

                Object messages = executeLocalOperation(leaderActor, "get-all-messages");

                assertNotNull(messages);
                assertTrue(messages instanceof List);
                List<Object> listMessages = (List<Object>) messages;

                int installSnapshotReplyReceivedCount = 0;
                for (Object message: listMessages) {
                    if (message instanceof InstallSnapshotReply) {
                        ++installSnapshotReplyReceivedCount;
                    }
                }

                assertEquals(1, installSnapshotReplyReceivedCount);
                InstallSnapshotReply reply = (InstallSnapshotReply) listMessages.get(0);
                assertEquals(false, reply.isSuccess());
                assertEquals(-1, reply.getChunkIndex());
                assertEquals(ByteString.EMPTY, follower.getSnapshotChunksCollected());


            }};
    }

    public Object executeLocalOperation(ActorRef actor, Object message) throws Exception {
        return MessageCollectorActor.getAllMessages(actor);
    }

    public ByteString getNextChunk (ByteString bs, int offset){
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
        return bs.substring(start, start + size);
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
