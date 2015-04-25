package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import com.google.common.base.Stopwatch;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class FollowerTest extends AbstractRaftActorBehaviorTest {

    private final TestActorRef<MessageCollectorActor> followerActor = actorFactory.createTestActor(
            Props.create(MessageCollectorActor.class), actorFactory.generateActorId("follower"));

    private final TestActorRef<MessageCollectorActor> leaderActor = actorFactory.createTestActor(
            Props.create(MessageCollectorActor.class), actorFactory.generateActorId("leader"));

    private RaftActorBehavior follower;

    private final short payloadVersion = 5;

    @Override
    @After
    public void tearDown() throws Exception {
        if(follower != null) {
            follower.close();
        }

        super.tearDown();
    }

    @Override
    protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Follower(actorContext);
    }

    @Override
    protected  MockRaftActorContext createActorContext() {
        return createActorContext(followerActor);
    }

    @Override
    protected  MockRaftActorContext createActorContext(ActorRef actorRef){
        MockRaftActorContext context = new MockRaftActorContext("follower", getSystem(), actorRef);
        context.setPayloadVersion(payloadVersion );
        return context;
    }

    @Test
    public void testThatAnElectionTimeoutIsTriggered(){
        MockRaftActorContext actorContext = createActorContext();
        follower = new Follower(actorContext);

        MessageCollectorActor.expectFirstMatching(followerActor, ElectionTimeout.class,
                actorContext.getConfigParams().getElectionTimeOutInterval().$times(6).toMillis());
    }

    @Test
    public void testHandleElectionTimeout(){
        logStart("testHandleElectionTimeout");

        follower = new Follower(createActorContext());

        RaftActorBehavior raftBehavior = follower.handleMessage(followerActor, new ElectionTimeout());

        assertTrue(raftBehavior instanceof Candidate);
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNull(){
        logStart("testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNull");

        RaftActorContext context = createActorContext();
        long term = 1000;
        context.getTermInformation().update(term, null);

        follower = createBehavior(context);

        follower.handleMessage(leaderActor, new RequestVote(term, "test", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, RequestVoteReply.class);

        assertEquals("isVoteGranted", true, reply.isVoteGranted());
        assertEquals("getTerm", term, reply.getTerm());
    }

    @Test
    public void testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNotTheSameAsCandidateId(){
        logStart("testHandleRequestVoteWhenSenderTermEqualToCurrentTermAndVotedForIsNotTheSameAsCandidateId");

        RaftActorContext context = createActorContext();
        long term = 1000;
        context.getTermInformation().update(term, "test");

        follower = createBehavior(context);

        follower.handleMessage(leaderActor, new RequestVote(term, "candidate", 10000, 999));

        RequestVoteReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, RequestVoteReply.class);

        assertEquals("isVoteGranted", false, reply.isVoteGranted());
    }


    @Test
    public void testHandleFirstAppendEntries() throws Exception {
        logStart("testHandleFirstAppendEntries");

        MockRaftActorContext context = createActorContext();

        List<ReplicatedLogEntry> entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.isInitialSyncDone());
    }

    @Test
    public void testHandleSyncUpAppendEntries() throws Exception {
        logStart("testHandleSyncUpAppendEntries");

        MockRaftActorContext context = createActorContext();

        List<ReplicatedLogEntry> entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.isInitialSyncDone());

        // Clear all the messages
        followerActor.underlyingActor().clear();

        context.setLastApplied(101);
        context.setCommitIndex(101);
        setLastLogEntry(context, 1, 101,
                new MockRaftActorContext.MockPayload(""));

        entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        appendEntries = new AppendEntries(2, "leader-1", 101, 1, entries, 102, 101, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertTrue(syncStatus.isInitialSyncDone());

        followerActor.underlyingActor().clear();

        // Sending the same message again should not generate another message

        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.getFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertNull(syncStatus);

    }

    @Test
    public void testHandleAppendEntriesLeaderChangedBeforeSyncUpComplete() throws Exception {
        logStart("testHandleAppendEntriesLeaderChangedBeforeSyncUpComplete");

        MockRaftActorContext context = createActorContext();

        List<ReplicatedLogEntry> entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.isInitialSyncDone());

        // Clear all the messages
        followerActor.underlyingActor().clear();

        context.setLastApplied(100);
        setLastLogEntry(context, 1, 100,
                new MockRaftActorContext.MockPayload(""));

        entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // leader-2 is becoming the leader now and it says the commitIndex is 45
        appendEntries = new AppendEntries(2, "leader-2", 45, 1, entries, 46, 100, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        // We get a new message saying initial status is not done
        assertFalse(syncStatus.isInitialSyncDone());

    }


    @Test
    public void testHandleAppendEntriesLeaderChangedAfterSyncUpComplete() throws Exception {
        logStart("testHandleAppendEntriesLeaderChangedAfterSyncUpComplete");

        MockRaftActorContext context = createActorContext();

        List<ReplicatedLogEntry> entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        FollowerInitialSyncUpStatus syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.isInitialSyncDone());

        // Clear all the messages
        followerActor.underlyingActor().clear();

        context.setLastApplied(101);
        context.setCommitIndex(101);
        setLastLogEntry(context, 1, 101,
                new MockRaftActorContext.MockPayload(""));

        entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        appendEntries = new AppendEntries(2, "leader-1", 101, 1, entries, 102, 101, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertTrue(syncStatus.isInitialSyncDone());

        // Clear all the messages
        followerActor.underlyingActor().clear();

        context.setLastApplied(100);
        setLastLogEntry(context, 1, 100,
                new MockRaftActorContext.MockPayload(""));

        entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // leader-2 is becoming the leader now and it says the commitIndex is 45
        appendEntries = new AppendEntries(2, "leader-2", 45, 1, entries, 46, 100, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        // We get a new message saying initial status is not done
        assertFalse(syncStatus.isInitialSyncDone());

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
        logStart("testHandleAppendEntriesWithNewerCommitIndex");

        MockRaftActorContext context = createActorContext();

        context.setLastApplied(100);
        setLastLogEntry(context, 1, 100,
                new MockRaftActorContext.MockPayload(""));
        context.getReplicatedLog().setSnapshotIndex(99);

        List<ReplicatedLogEntry> entries = Arrays.<ReplicatedLogEntry>asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader-1", 100, 1, entries, 101, 100, (short)0);

        follower = createBehavior(context);
        follower.handleMessage(leaderActor, appendEntries);

        assertEquals("getLastApplied", 101L, context.getLastApplied());
    }

    /**
     * This test verifies that when an AppendEntries is received a specific prevLogTerm
     * which does not match the term that is in RaftActors log entry at prevLogIndex
     * then the RaftActor does not change it's state and it returns a failure.
     *
     * @throws Exception
     */
    @Test
    public void testHandleAppendEntriesSenderPrevLogTermNotSameAsReceiverPrevLogTerm() {
        logStart("testHandleAppendEntriesSenderPrevLogTermNotSameAsReceiverPrevLogTerm");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.getTermInformation().update(95, "test");

        // AppendEntries is now sent with a bigger term
        // this will set the receivers term to be the same as the sender's term
        AppendEntries appendEntries = new AppendEntries(100, "leader", 0, 0, null, 101, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        Assert.assertSame(follower, newBehavior);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        assertEquals("isSuccess", false, reply.isSuccess());
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
    public void testHandleAppendEntriesAddNewEntries() {
        logStart("testHandleAppendEntriesAddNewEntries");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.getTermInformation().update(1, "test");

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));
        log.append(newReplicatedLogEntry(1, 2, "two"));

        context.setReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(newReplicatedLogEntry(1, 3, "three"));
        entries.add(newReplicatedLogEntry(1, 4, "four"));

        // Send appendEntries with the same term as was set on the receiver
        // before the new behavior was created (1 in this case)
        // This will not work for a Candidate because as soon as a Candidate
        // is created it increments the term
        AppendEntries appendEntries = new AppendEntries(1, "leader-1", 2, 1, entries, 4, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        Assert.assertSame(follower, newBehavior);

        assertEquals("Next index", 5, log.last().getIndex() + 1);
        assertEquals("Entry 3", entries.get(0), log.get(3));
        assertEquals("Entry 4", entries.get(1), log.get(4));

        expectAndVerifyAppendEntriesReply(1, true, context.getId(), 1, 4);
    }

    /**
     * This test verifies that when a new AppendEntries message is received with
     * new entries and the logs of the sender and receiver are out-of-sync that
     * the log is first corrected by removing the out of sync entries from the
     * log and then adding in the new entries sent with the AppendEntries message
     */
    @Test
    public void testHandleAppendEntriesCorrectReceiverLogEntries() {
        logStart("testHandleAppendEntriesCorrectReceiverLogEntries");

        MockRaftActorContext context = createActorContext();

        // First set the receivers term to lower number
        context.getTermInformation().update(1, "test");

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));
        log.append(newReplicatedLogEntry(1, 2, "two"));

        context.setReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(newReplicatedLogEntry(2, 2, "two-1"));
        entries.add(newReplicatedLogEntry(2, 3, "three"));

        // Send appendEntries with the same term as was set on the receiver
        // before the new behavior was created (1 in this case)
        // This will not work for a Candidate because as soon as a Candidate
        // is created it increments the term
        AppendEntries appendEntries = new AppendEntries(2, "leader", 1, 1, entries, 3, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        Assert.assertSame(follower, newBehavior);

        // The entry at index 2 will be found out-of-sync with the leader
        // and will be removed
        // Then the two new entries will be added to the log
        // Thus making the log to have 4 entries
        assertEquals("Next index", 4, log.last().getIndex() + 1);
        //assertEquals("Entry 2", entries.get(0), log.get(2));

        assertEquals("Entry 1 data", "one", log.get(1).getData().toString());

        // Check that the entry at index 2 has the new data
        assertEquals("Entry 2", entries.get(0), log.get(2));

        assertEquals("Entry 3", entries.get(1), log.get(3));

        expectAndVerifyAppendEntriesReply(2, true, context.getId(), 2, 3);
    }

    @Test
    public void testHandleAppendEntriesPreviousLogEntryMissing(){
        logStart("testHandleAppendEntriesPreviousLogEntryMissing");

        MockRaftActorContext context = createActorContext();

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));
        log.append(newReplicatedLogEntry(1, 2, "two"));

        context.setReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(newReplicatedLogEntry(1, 4, "four"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", 3, 1, entries, 4, -1, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        Assert.assertSame(follower, newBehavior);

        expectAndVerifyAppendEntriesReply(1, false, context.getId(), 1, 2);
    }

    @Test
    public void testHandleAppendEntriesWithExistingLogEntry() {
        logStart("testHandleAppendEntriesWithExistingLogEntry");

        MockRaftActorContext context = createActorContext();

        context.getTermInformation().update(1, "test");

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();
        log.append(newReplicatedLogEntry(1, 0, "zero"));
        log.append(newReplicatedLogEntry(1, 1, "one"));

        context.setReplicatedLog(log);

        // Send the last entry again.
        List<ReplicatedLogEntry> entries = Arrays.asList(newReplicatedLogEntry(1, 1, "one"));

        follower = createBehavior(context);

        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", 0, 1, entries, 1, -1, (short)0));

        assertEquals("Next index", 2, log.last().getIndex() + 1);
        assertEquals("Entry 1", entries.get(0), log.get(1));

        expectAndVerifyAppendEntriesReply(1, true, context.getId(), 1, 1);

        // Send the last entry again and also a new one.

        entries = Arrays.asList(newReplicatedLogEntry(1, 1, "one"), newReplicatedLogEntry(1, 2, "two"));

        leaderActor.underlyingActor().clear();
        follower.handleMessage(leaderActor, new AppendEntries(1, "leader", 0, 1, entries, 2, -1, (short)0));

        assertEquals("Next index", 3, log.last().getIndex() + 1);
        assertEquals("Entry 1", entries.get(0), log.get(1));
        assertEquals("Entry 2", entries.get(1), log.get(2));

        expectAndVerifyAppendEntriesReply(1, true, context.getId(), 1, 2);
    }

    @Test
    public void testHandleAppendEntriesAfterInstallingSnapshot(){
        logStart("testHandleAppendAfterInstallingSnapshot");

        MockRaftActorContext context = createActorContext();

        // Prepare the receivers log
        MockRaftActorContext.SimpleReplicatedLog log = new MockRaftActorContext.SimpleReplicatedLog();

        // Set up a log as if it has been snapshotted
        log.setSnapshotIndex(3);
        log.setSnapshotTerm(1);

        context.setReplicatedLog(log);

        // Prepare the entries to be sent with AppendEntries
        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(newReplicatedLogEntry(1, 4, "four"));

        AppendEntries appendEntries = new AppendEntries(1, "leader", 3, 1, entries, 4, 3, (short)0);

        follower = createBehavior(context);

        RaftActorBehavior newBehavior = follower.handleMessage(leaderActor, appendEntries);

        Assert.assertSame(follower, newBehavior);

        expectAndVerifyAppendEntriesReply(1, true, context.getId(), 1, 4);
    }


    /**
     * This test verifies that when InstallSnapshot is received by
     * the follower its applied correctly.
     *
     * @throws Exception
     */
    @Test
    public void testHandleInstallSnapshot() throws Exception {
        logStart("testHandleInstallSnapshot");

        MockRaftActorContext context = createActorContext();

        follower = createBehavior(context);

        ByteString bsSnapshot  = createSnapshot();
        int offset = 0;
        int snapshotLength = bsSnapshot.size();
        int chunkSize = 50;
        int totalChunks = (snapshotLength / chunkSize) + ((snapshotLength % chunkSize) > 0 ? 1 : 0);
        int lastIncludedIndex = 1;
        int chunkIndex = 1;
        InstallSnapshot lastInstallSnapshot = null;

        for(int i = 0; i < totalChunks; i++) {
            ByteString chunkData = getNextChunk(bsSnapshot, offset, chunkSize);
            lastInstallSnapshot = new InstallSnapshot(1, "leader", lastIncludedIndex, 1,
                    chunkData, chunkIndex, totalChunks);
            follower.handleMessage(leaderActor, lastInstallSnapshot);
            offset = offset + 50;
            lastIncludedIndex++;
            chunkIndex++;
        }

        ApplySnapshot applySnapshot = MessageCollectorActor.expectFirstMatching(followerActor,
                ApplySnapshot.class);
        Snapshot snapshot = applySnapshot.getSnapshot();
        assertNotNull(lastInstallSnapshot);
        assertEquals("getLastIndex", lastInstallSnapshot.getLastIncludedIndex(), snapshot.getLastIndex());
        assertEquals("getLastIncludedTerm", lastInstallSnapshot.getLastIncludedTerm(),
                snapshot.getLastAppliedTerm());
        assertEquals("getLastAppliedIndex", lastInstallSnapshot.getLastIncludedIndex(),
                snapshot.getLastAppliedIndex());
        assertEquals("getLastTerm", lastInstallSnapshot.getLastIncludedTerm(), snapshot.getLastTerm());
        Assert.assertArrayEquals("getState", bsSnapshot.toByteArray(), snapshot.getState());

        List<InstallSnapshotReply> replies = MessageCollectorActor.getAllMatching(
                leaderActor, InstallSnapshotReply.class);
        assertEquals("InstallSnapshotReply count", totalChunks, replies.size());

        chunkIndex = 1;
        for(InstallSnapshotReply reply: replies) {
            assertEquals("getChunkIndex", chunkIndex++, reply.getChunkIndex());
            assertEquals("getTerm", 1, reply.getTerm());
            assertEquals("isSuccess", true, reply.isSuccess());
            assertEquals("getFollowerId", context.getId(), reply.getFollowerId());
        }

        assertNull("Expected null SnapshotTracker", ((Follower) follower).getSnapshotTracker());
    }


    /**
     * Verify that when an AppendEntries is sent to a follower during a snapshot install
     * the Follower short-circuits the processing of the AppendEntries message.
     *
     * @throws Exception
     */
    @Test
    public void testReceivingAppendEntriesDuringInstallSnapshot() throws Exception {
        logStart("testReceivingAppendEntriesDuringInstallSnapshot");

        MockRaftActorContext context = createActorContext();

        follower = createBehavior(context);

        ByteString bsSnapshot  = createSnapshot();
        int snapshotLength = bsSnapshot.size();
        int chunkSize = 50;
        int totalChunks = (snapshotLength / chunkSize) + ((snapshotLength % chunkSize) > 0 ? 1 : 0);
        int lastIncludedIndex = 1;

        // Check that snapshot installation is not in progress
        assertNull(((Follower) follower).getSnapshotTracker());

        // Make sure that we have more than 1 chunk to send
        assertTrue(totalChunks > 1);

        // Send an install snapshot with the first chunk to start the process of installing a snapshot
        ByteString chunkData = getNextChunk(bsSnapshot, 0, chunkSize);
        follower.handleMessage(leaderActor, new InstallSnapshot(1, "leader", lastIncludedIndex, 1,
                chunkData, 1, totalChunks));

        // Check if snapshot installation is in progress now
        assertNotNull(((Follower) follower).getSnapshotTracker());

        // Send an append entry
        AppendEntries appendEntries = mock(AppendEntries.class);
        doReturn(context.getTermInformation().getCurrentTerm()).when(appendEntries).getTerm();

        follower.handleMessage(leaderActor, appendEntries);

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);
        assertEquals(context.getReplicatedLog().lastIndex(), reply.getLogLastIndex());
        assertEquals(context.getReplicatedLog().lastTerm(), reply.getLogLastTerm());
        assertEquals(context.getTermInformation().getCurrentTerm(), reply.getTerm());

        // We should not hit the code that needs to look at prevLogIndex because we are short circuiting
        verify(appendEntries, never()).getPrevLogIndex();

    }

    @Test
    public void testInitialSyncUpWithHandleInstallSnapshotFollowedByAppendEntries() throws Exception {
        logStart("testInitialSyncUpWithHandleInstallSnapshot");

        MockRaftActorContext context = createActorContext();

        follower = createBehavior(context);

        ByteString bsSnapshot  = createSnapshot();
        int offset = 0;
        int snapshotLength = bsSnapshot.size();
        int chunkSize = 50;
        int totalChunks = (snapshotLength / chunkSize) + ((snapshotLength % chunkSize) > 0 ? 1 : 0);
        int lastIncludedIndex = 1;
        int chunkIndex = 1;
        InstallSnapshot lastInstallSnapshot = null;

        for(int i = 0; i < totalChunks; i++) {
            ByteString chunkData = getNextChunk(bsSnapshot, offset, chunkSize);
            lastInstallSnapshot = new InstallSnapshot(1, "leader", lastIncludedIndex, 1,
                    chunkData, chunkIndex, totalChunks);
            follower.handleMessage(leaderActor, lastInstallSnapshot);
            offset = offset + 50;
            lastIncludedIndex++;
            chunkIndex++;
        }

        FollowerInitialSyncUpStatus syncStatus =
                MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertFalse(syncStatus.isInitialSyncDone());

        // Clear all the messages
        followerActor.underlyingActor().clear();

        context.setLastApplied(101);
        context.setCommitIndex(101);
        setLastLogEntry(context, 1, 101,
                new MockRaftActorContext.MockPayload(""));

        List<ReplicatedLogEntry> entries = Arrays.asList(
                newReplicatedLogEntry(2, 101, "foo"));

        // The new commitIndex is 101
        AppendEntries appendEntries = new AppendEntries(2, "leader", 101, 1, entries, 102, 101, (short)0);
        follower.handleMessage(leaderActor, appendEntries);

        syncStatus = MessageCollectorActor.expectFirstMatching(followerActor, FollowerInitialSyncUpStatus.class);

        assertTrue(syncStatus.isInitialSyncDone());
    }

    @Test
    public void testHandleOutOfSequenceInstallSnapshot() throws Exception {
        logStart("testHandleOutOfSequenceInstallSnapshot");

        MockRaftActorContext context = createActorContext();

        follower = createBehavior(context);

        ByteString bsSnapshot = createSnapshot();

        InstallSnapshot installSnapshot = new InstallSnapshot(1, "leader", 3, 1,
                getNextChunk(bsSnapshot, 10, 50), 3, 3);
        follower.handleMessage(leaderActor, installSnapshot);

        InstallSnapshotReply reply = MessageCollectorActor.expectFirstMatching(leaderActor,
                InstallSnapshotReply.class);

        assertEquals("isSuccess", false, reply.isSuccess());
        assertEquals("getChunkIndex", -1, reply.getChunkIndex());
        assertEquals("getTerm", 1, reply.getTerm());
        assertEquals("getFollowerId", context.getId(), reply.getFollowerId());

        assertNull("Expected null SnapshotTracker", ((Follower) follower).getSnapshotTracker());
    }

    @Test
    public void testFollowerSchedulesElectionTimeoutImmediatelyWhenItHasNoPeers(){
        MockRaftActorContext context = createActorContext();

        Stopwatch stopwatch = Stopwatch.createStarted();

        follower = createBehavior(context);

        MessageCollectorActor.expectFirstMatching(followerActor, ElectionTimeout.class);

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        assertTrue(elapsed < context.getConfigParams().getElectionTimeOutInterval().toMillis());
    }

    public ByteString getNextChunk (ByteString bs, int offset, int chunkSize){
        int snapshotLength = bs.size();
        int start = offset;
        int size = chunkSize;
        if (chunkSize > snapshotLength) {
            size = snapshotLength;
        } else {
            if ((start + chunkSize) > snapshotLength) {
                size = snapshotLength - start;
            }
        }
        return bs.substring(start, start + size);
    }

    private void expectAndVerifyAppendEntriesReply(int expTerm, boolean expSuccess,
            String expFollowerId, long expLogLastTerm, long expLogLastIndex) {

        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(leaderActor,
                AppendEntriesReply.class);

        assertEquals("isSuccess", expSuccess, reply.isSuccess());
        assertEquals("getTerm", expTerm, reply.getTerm());
        assertEquals("getFollowerId", expFollowerId, reply.getFollowerId());
        assertEquals("getLogLastTerm", expLogLastTerm, reply.getLogLastTerm());
        assertEquals("getLogLastIndex", expLogLastIndex, reply.getLogLastIndex());
        assertEquals("getPayloadVersion", payloadVersion, reply.getPayloadVersion());
    }

    private ReplicatedLogEntry newReplicatedLogEntry(long term, long index, String data) {
        return new MockRaftActorContext.MockReplicatedLogEntry(term, index,
                new MockRaftActorContext.MockPayload(data));
    }

    private ByteString createSnapshot(){
        HashMap<String, String> followerSnapshot = new HashMap<>();
        followerSnapshot.put("1", "A");
        followerSnapshot.put("2", "B");
        followerSnapshot.put("3", "C");

        return toByteString(followerSnapshot);
    }

    @Override
    protected void assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(RaftActorContext actorContext,
            ActorRef actorRef, RaftRPC rpc) throws Exception {
        super.assertStateChangesToFollowerWhenRaftRPCHasNewerTerm(actorContext, actorRef, rpc);

        String expVotedFor = RequestVote.class.isInstance(rpc) ? ((RequestVote)rpc).getCandidateId() : null;
        assertEquals("New votedFor", expVotedFor, actorContext.getTermInformation().getVotedFor());
    }

    @Override
    protected void handleAppendEntriesAddSameEntryToLogReply(TestActorRef<MessageCollectorActor> replyActor)
            throws Exception {
        AppendEntriesReply reply = MessageCollectorActor.expectFirstMatching(replyActor, AppendEntriesReply.class);
        assertEquals("isSuccess", true, reply.isSuccess());
    }
}
