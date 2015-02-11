package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.SerializationUtils;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.IsolatedLeaderCheck;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader.FollowerToSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.protobuff.messages.cluster.raft.InstallSnapshotMessages;
import scala.concurrent.duration.FiniteDuration;

public class LeaderTest extends AbstractRaftActorBehaviorTest {

    static final String FOLLOWER_ID = "follower";

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private final TestActorRef<ForwardMessageToBehaviorActor> leaderActor = actorFactory.createTestActor(
            Props.create(ForwardMessageToBehaviorActor.class), actorFactory.generateActorId("leader"));

    private final TestActorRef<ForwardMessageToBehaviorActor> followerActor = actorFactory.createTestActor(
            Props.create(ForwardMessageToBehaviorActor.class), actorFactory.generateActorId("follower"));

    private Leader leader;

    @After
    public void tearDown() throws Exception {
        if(leader != null) {
            leader.close();
        }

        actorFactory.close();
    }

    @Test
    public void testHandleMessageForUnknownMessage() throws Exception {
        logStart("testHandleMessageForUnknownMessage");

        leader = new Leader(createActorContext());

        // handle message should return the Leader state when it receives an
        // unknown message
        RaftActorBehavior behavior = leader.handleMessage(followerActor, "foo");
        Assert.assertTrue(behavior instanceof Leader);
    }

    @Test
    public void testThatLeaderSendsAHeartbeatMessageToAllFollowers() throws Exception {
        logStart("testThatLeaderSendsAHeartbeatMessageToAllFollowers");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader should send an immediate heartbeat with no entries as follower is inactive.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getTerm", term, appendEntries.getTerm());
        assertEquals("getPrevLogIndex", -1, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", -1, appendEntries.getPrevLogTerm());
        assertEquals("Entries size", 0, appendEntries.getEntries().size());

        // The follower would normally reply - simulate that explicitly here.
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex - 1, term));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        // Sleep for the heartbeat interval so AppendEntries is sent.
        Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().
                getHeartBeatInterval().toMillis(), TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, new SendHeartBeat());

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getPrevLogIndex", lastIndex - 1, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", term, appendEntries.getPrevLogTerm());
        assertEquals("Entries size", 1, appendEntries.getEntries().size());
        assertEquals("Entry getIndex", lastIndex, appendEntries.getEntries().get(0).getIndex());
        assertEquals("Entry getTerm", term, appendEntries.getEntries().get(0).getTerm());
    }

    @Test
    public void testHandleReplicateMessageSendAppendEntriesToFollower() throws Exception {
        logStart("testHandleReplicateMessageSendAppendEntriesToFollower");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        long term = 1;
        actorContext.getTermInformation().update(term, "");

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // The follower would normally reply - simulate that explicitly here.
        long lastIndex = actorContext.getReplicatedLog().lastIndex();
        leader.handleMessage(followerActor, new AppendEntriesReply(
                FOLLOWER_ID, term, true, lastIndex, term));
        assertEquals("isFollowerActive", true, leader.getFollower(FOLLOWER_ID).isFollowerActive());

        followerActor.underlyingActor().clear();

        MockRaftActorContext.MockPayload payload = new MockRaftActorContext.MockPayload("foo");
        MockRaftActorContext.MockReplicatedLogEntry newEntry = new MockRaftActorContext.MockReplicatedLogEntry(
                1, lastIndex + 1, payload);
        actorContext.getReplicatedLog().append(newEntry);
        RaftActorBehavior raftBehavior = leader.handleMessage(leaderActor,
                new Replicate(null, null, newEntry));

        // State should not change
        assertTrue(raftBehavior instanceof Leader);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);
        assertEquals("getPrevLogIndex", lastIndex, appendEntries.getPrevLogIndex());
        assertEquals("getPrevLogTerm", term, appendEntries.getPrevLogTerm());
        assertEquals("Entries size", 1, appendEntries.getEntries().size());
        assertEquals("Entry getIndex", lastIndex + 1, appendEntries.getEntries().get(0).getIndex());
        assertEquals("Entry getTerm", term, appendEntries.getEntries().get(0).getTerm());
        assertEquals("Entry payload", payload, appendEntries.getEntries().get(0).getData());
    }

    @Test
    public void testHandleReplicateMessageWhenThereAreNoFollowers() throws Exception {
        logStart("testHandleReplicateMessageWhenThereAreNoFollowers");

        MockRaftActorContext actorContext = createActorContext();

        leader = new Leader(actorContext);

        actorContext.setLastApplied(0);

        long newLogIndex = actorContext.getReplicatedLog().lastIndex() + 1;
        long term = actorContext.getTermInformation().getCurrentTerm();
        MockRaftActorContext.MockReplicatedLogEntry newEntry = new MockRaftActorContext.MockReplicatedLogEntry(
                term, newLogIndex, new MockRaftActorContext.MockPayload("foo"));

        actorContext.getReplicatedLog().append(newEntry);

        RaftActorBehavior raftBehavior = leader.handleMessage(leaderActor,
                new Replicate(leaderActor, "state-id", newEntry));

        // State should not change
        assertTrue(raftBehavior instanceof Leader);

        assertEquals("getCommitIndex", newLogIndex, actorContext.getCommitIndex());

        // We should get 2 ApplyState messages - 1 for new log entry and 1 for the previous
        // one since lastApplied state is 0.
        List<ApplyState> applyStateList = MessageCollectorActor.getAllMatching(
                leaderActor, ApplyState.class);
        assertEquals("ApplyState count", newLogIndex, applyStateList.size());

        for(int i = 0; i <= newLogIndex - 1; i++ ) {
            ApplyState applyState = applyStateList.get(i);
            assertEquals("getIndex", i + 1, applyState.getReplicatedLogEntry().getIndex());
            assertEquals("getTerm", term, applyState.getReplicatedLogEntry().getTerm());
        }

        ApplyState last = applyStateList.get((int) newLogIndex - 1);
        assertEquals("getData", newEntry.getData(), last.getReplicatedLogEntry().getData());
        assertEquals("getIdentifier", "state-id", last.getIdentifier());
    }

    @Test
    public void testSendAppendEntriesOnAnInProgressInstallSnapshot() throws Exception {
        logStart("testSendAppendEntriesOnAnInProgressInstallSnapshot");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int newEntryIndex = 4;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.setCommitIndex(followersLastIndex);
        //set follower timeout to 2 mins, helps during debugging
        actorContext.setConfigParams(new MockConfigParamsImpl(120000L, 10));

        leader = new Leader(actorContext);

        // new entry
        ReplicatedLogImplEntry entry =
                new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                        new MockRaftActorContext.MockPayload("D"));

        //update follower timestamp
        leader.markFollowerActive(FOLLOWER_ID);

        ByteString bs = toByteString(leadersSnapshot);
        leader.setSnapshot(Optional.of(bs));
        FollowerToSnapshot fts = leader.new FollowerToSnapshot(bs);
        leader.setFollowerSnapshot(FOLLOWER_ID, fts);

        //send first chunk and no InstallSnapshotReply received yet
        fts.getNextChunk();
        fts.incrementChunkIndex();

        Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, new SendHeartBeat());

        AppendEntries aeproto = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        AppendEntries ae = (AppendEntries) SerializationUtils.fromSerializable(aeproto);

        assertTrue("AppendEntries should be sent with empty entries", ae.getEntries().isEmpty());

        //InstallSnapshotReply received
        fts.markSendStatus(true);

        leader.handleMessage(leaderActor, new SendHeartBeat());

        InstallSnapshotMessages.InstallSnapshot isproto = MessageCollectorActor.expectFirstMatching(followerActor,
                InstallSnapshot.SERIALIZABLE_CLASS);

        InstallSnapshot is = (InstallSnapshot) SerializationUtils.fromSerializable(isproto);

        assertEquals(snapshotIndex, is.getLastIncludedIndex());
    }

    @Test
    public void testSendAppendEntriesSnapshotScenario() throws Exception {
        logStart("testSendAppendEntriesSnapshotScenario");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int newEntryIndex = 4;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // new entry
        ReplicatedLogImplEntry entry =
                new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                        new MockRaftActorContext.MockPayload("D"));

        //update follower timestamp
        leader.markFollowerActive(FOLLOWER_ID);

        // this should invoke a sendinstallsnapshot as followersLastIndex < snapshotIndex
        RaftActorBehavior raftBehavior = leader.handleMessage(
                leaderActor, new Replicate(null, "state-id", entry));

        assertTrue(raftBehavior instanceof Leader);

        MessageCollectorActor.expectFirstMatching(leaderActor, InitiateInstallSnapshot.class);
    }

    @Test
    public void testInitiateInstallSnapshot() throws Exception {
        logStart("testInitiateInstallSnapshot");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int newEntryIndex = 4;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.setLastApplied(3);
        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        // Leader will send an immediate heartbeat - ignore it.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // set the snapshot as absent and check if capture-snapshot is invoked.
        leader.setSnapshot(Optional.<ByteString>absent());

        // new entry
        ReplicatedLogImplEntry entry =
                new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                        new MockRaftActorContext.MockPayload("D"));

        actorContext.getReplicatedLog().append(entry);

        // this should invoke a sendinstallsnapshot as followersLastIndex < snapshotIndex
        RaftActorBehavior raftBehavior = leader.handleMessage(
                leaderActor, new InitiateInstallSnapshot());

        CaptureSnapshot cs = MessageCollectorActor.expectFirstMatching(leaderActor, CaptureSnapshot.class);

        assertTrue(cs.isInstallSnapshotInitiated());
        assertEquals(3, cs.getLastAppliedIndex());
        assertEquals(1, cs.getLastAppliedTerm());
        assertEquals(4, cs.getLastIndex());
        assertEquals(2, cs.getLastTerm());

        // if an initiate is started again when first is in progress, it shouldn't initiate Capture
        raftBehavior = leader.handleMessage(leaderActor, new InitiateInstallSnapshot());

        List<CaptureSnapshot> captureSnapshots = MessageCollectorActor.getAllMatching(leaderActor, CaptureSnapshot.class);
        assertEquals("CaptureSnapshot should not get invoked when  initiate is in progress", 1, captureSnapshots.size());
    }

    @Test
    public void testInstallSnapshot() throws Exception {
        logStart("testInstallSnapshot");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());
        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        // Ignore initial heartbeat.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        RaftActorBehavior raftBehavior = leader.handleMessage(leaderActor,
                new SendInstallSnapshot(toByteString(leadersSnapshot)));

        assertTrue(raftBehavior instanceof Leader);

        // check if installsnapshot gets called with the correct values.

        InstallSnapshot installSnapshot = (InstallSnapshot) SerializationUtils.fromSerializable(
                MessageCollectorActor.expectFirstMatching(followerActor, InstallSnapshotMessages.InstallSnapshot.class));

        assertNotNull(installSnapshot.getData());
        assertEquals(snapshotIndex, installSnapshot.getLastIncludedIndex());
        assertEquals(snapshotTerm, installSnapshot.getLastIncludedTerm());

        assertEquals(currentTerm, installSnapshot.getTerm());
    }

    @Test
    public void testHandleInstallSnapshotReplyLastChunk() throws Exception {
        logStart("testHandleInstallSnapshotReplyLastChunk");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        // Ignore initial heartbeat.
        MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        // set the snapshot variables in replicatedlog

        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

        ByteString bs = toByteString(leadersSnapshot);
        leader.setSnapshot(Optional.of(bs));
        FollowerToSnapshot fts = leader.new FollowerToSnapshot(bs);
        leader.setFollowerSnapshot(FOLLOWER_ID, fts);
        while(!fts.isLastChunk(fts.getChunkIndex())) {
            fts.getNextChunk();
            fts.incrementChunkIndex();
        }

        //clears leaders log
        actorContext.getReplicatedLog().removeFrom(0);

        RaftActorBehavior raftBehavior = leader.handleMessage(followerActor,
                new InstallSnapshotReply(currentTerm, FOLLOWER_ID, fts.getChunkIndex(), true));

        assertTrue(raftBehavior instanceof Leader);

        assertEquals(0, leader.followerSnapshotSize());
        assertEquals(1, leader.followerLogSize());
        FollowerLogInformation fli = leader.getFollower(FOLLOWER_ID);
        assertNotNull(fli);
        assertEquals(snapshotIndex, fli.getMatchIndex());
        assertEquals(snapshotIndex, fli.getMatchIndex());
        assertEquals(snapshotIndex + 1, fli.getNextIndex());
    }

    @Test
    public void testSendSnapshotfromInstallSnapshotReply() throws Exception {
        logStart("testSendSnapshotfromInstallSnapshotReply");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl(){
            @Override
            public int getSnapshotChunkSize() {
                return 50;
            }
        };
        configParams.setHeartBeatInterval(new FiniteDuration(9, TimeUnit.SECONDS));
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(10, TimeUnit.SECONDS));

        actorContext.setConfigParams(configParams);
        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

        ByteString bs = toByteString(leadersSnapshot);
        leader.setSnapshot(Optional.of(bs));

        leader.handleMessage(leaderActor, new SendInstallSnapshot(bs));

        InstallSnapshotMessages.InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(
                followerActor, InstallSnapshotMessages.InstallSnapshot.class);

        assertEquals(1, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());

        followerActor.underlyingActor().clear();
        leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                FOLLOWER_ID, installSnapshot.getChunkIndex(), true));

        installSnapshot = MessageCollectorActor.expectFirstMatching(
                followerActor, InstallSnapshotMessages.InstallSnapshot.class);

        assertEquals(2, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());

        followerActor.underlyingActor().clear();
        leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                FOLLOWER_ID, installSnapshot.getChunkIndex(), true));

        installSnapshot = MessageCollectorActor.expectFirstMatching(
                followerActor, InstallSnapshotMessages.InstallSnapshot.class);

        // Send snapshot reply one more time and make sure that a new snapshot message should not be sent to follower
        followerActor.underlyingActor().clear();
        leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                FOLLOWER_ID, installSnapshot.getChunkIndex(), true));

        installSnapshot = MessageCollectorActor.getFirstMatching(
                followerActor, InstallSnapshotMessages.InstallSnapshot.class);

        Assert.assertNull(installSnapshot);
    }


    @Test
    public void testHandleInstallSnapshotReplyWithInvalidChunkIndex() throws Exception{
        logStart("testHandleInstallSnapshotReplyWithInvalidChunkIndex");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        actorContext.setConfigParams(new DefaultConfigParamsImpl(){
            @Override
            public int getSnapshotChunkSize() {
                return 50;
            }
        });

        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

        ByteString bs = toByteString(leadersSnapshot);
        leader.setSnapshot(Optional.of(bs));

        leader.handleMessage(leaderActor, new SendInstallSnapshot(bs));

        InstallSnapshotMessages.InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(
                followerActor, InstallSnapshotMessages.InstallSnapshot.class);

        assertEquals(1, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());

        followerActor.underlyingActor().clear();

        leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                FOLLOWER_ID, -1, false));

        Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, new SendHeartBeat());

        installSnapshot = MessageCollectorActor.expectFirstMatching(
                followerActor, InstallSnapshotMessages.InstallSnapshot.class);

        assertEquals(1, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());
    }

    @Test
    public void testHandleSnapshotSendsPreviousChunksHashCodeWhenSendingNextChunk() throws Exception {
        logStart("testHandleSnapshotSendsPreviousChunksHashCodeWhenSendingNextChunk");

        MockRaftActorContext actorContext = createActorContextWithFollower();

        final int followersLastIndex = 2;
        final int snapshotIndex = 3;
        final int snapshotTerm = 1;
        final int currentTerm = 2;

        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public int getSnapshotChunkSize() {
                return 50;
            }
        });

        actorContext.setCommitIndex(followersLastIndex);

        leader = new Leader(actorContext);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        // set the snapshot variables in replicatedlog
        actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
        actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
        actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

        ByteString bs = toByteString(leadersSnapshot);
        leader.setSnapshot(Optional.of(bs));

        leader.handleMessage(leaderActor, new SendInstallSnapshot(bs));

        InstallSnapshotMessages.InstallSnapshot installSnapshot = MessageCollectorActor.expectFirstMatching(
                followerActor, InstallSnapshotMessages.InstallSnapshot.class);

        assertEquals(1, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());
        assertEquals(AbstractLeader.INITIAL_LAST_CHUNK_HASH_CODE, installSnapshot.getLastChunkHashCode());

        int hashCode = installSnapshot.getData().hashCode();

        followerActor.underlyingActor().clear();

        leader.handleMessage(followerActor, new InstallSnapshotReply(installSnapshot.getTerm(),
                FOLLOWER_ID, 1, true));

        installSnapshot = MessageCollectorActor.expectFirstMatching(
                followerActor, InstallSnapshotMessages.InstallSnapshot.class);

        assertEquals(2, installSnapshot.getChunkIndex());
        assertEquals(3, installSnapshot.getTotalChunks());
        assertEquals(hashCode, installSnapshot.getLastChunkHashCode());
    }

    @Test
    public void testFollowerToSnapshotLogic() {
        logStart("testFollowerToSnapshotLogic");

        MockRaftActorContext actorContext = createActorContext();

        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public int getSnapshotChunkSize() {
                return 50;
            }
        });

        leader = new Leader(actorContext);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        ByteString bs = toByteString(leadersSnapshot);
        byte[] barray = bs.toByteArray();

        FollowerToSnapshot fts = leader.new FollowerToSnapshot(bs);
        leader.setFollowerSnapshot(FOLLOWER_ID, fts);

        assertEquals(bs.size(), barray.length);

        int chunkIndex=0;
        for (int i=0; i < barray.length; i = i + 50) {
            int j = i + 50;
            chunkIndex++;

            if (i + 50 > barray.length) {
                j = barray.length;
            }

            ByteString chunk = fts.getNextChunk();
            assertEquals("bytestring size not matching for chunk:"+ chunkIndex, j-i, chunk.size());
            assertEquals("chunkindex not matching", chunkIndex, fts.getChunkIndex());

            fts.markSendStatus(true);
            if (!fts.isLastChunk(chunkIndex)) {
                fts.incrementChunkIndex();
            }
        }

        assertEquals("totalChunks not matching", chunkIndex, fts.getTotalChunks());
    }

    @Override protected RaftActorBehavior createBehavior(
        RaftActorContext actorContext) {
        return new Leader(actorContext);
    }

    @Override
    protected MockRaftActorContext createActorContext() {
        return createActorContext(leaderActor);
    }

    @Override
    protected MockRaftActorContext createActorContext(ActorRef actorRef) {
        return createActorContext("leader", actorRef);
    }

    private MockRaftActorContext createActorContextWithFollower() {
        MockRaftActorContext actorContext = createActorContext();
        actorContext.setPeerAddresses(ImmutableMap.<String,String>builder().put(FOLLOWER_ID,
                followerActor.path().toString()).build());
        return actorContext;
    }

    private MockRaftActorContext createActorContext(String id, ActorRef actorRef) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(50, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        MockRaftActorContext context = new MockRaftActorContext(id, getSystem(), actorRef);
        context.setConfigParams(configParams);
        return context;
    }

    public static class ForwardMessageToBehaviorActor extends MessageCollectorActor {
        AbstractRaftActorBehavior behavior;

        @Override public void onReceive(Object message) throws Exception {
            if(behavior != null) {
                behavior.handleMessage(sender(), message);
            }

            super.onReceive(message);
        }

        public static Props props() {
            return Props.create(ForwardMessageToBehaviorActor.class);
        }
    }

    @Test
    public void testLeaderCreatedWithCommitIndexLessThanLastIndex() throws Exception {
        logStart("testLeaderCreatedWithCommitIndexLessThanLastIndex");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();

        MockRaftActorContext followerActorContext = createActorContext(FOLLOWER_ID, followerActor);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().behavior = follower;

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(FOLLOWER_ID, followerActor.path().toString());

        leaderActorContext.setPeerAddresses(peerAddresses);

        leaderActorContext.getReplicatedLog().removeFrom(0);

        //create 3 entries
        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        leaderActorContext.setCommitIndex(1);

        followerActorContext.getReplicatedLog().removeFrom(0);

        // follower too has the exact same log entries and has the same commit index
        followerActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        followerActorContext.setCommitIndex(1);

        leader = new Leader(leaderActorContext);

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals(1, appendEntries.getLeaderCommit());
        assertEquals(0, appendEntries.getEntries().size());
        assertEquals(0, appendEntries.getPrevLogIndex());

        AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(
                leaderActor, AppendEntriesReply.class);

        assertEquals(2, appendEntriesReply.getLogLastIndex());
        assertEquals(1, appendEntriesReply.getLogLastTerm());

        // follower returns its next index
        assertEquals(2, appendEntriesReply.getLogLastIndex());
        assertEquals(1, appendEntriesReply.getLogLastTerm());

        follower.close();
    }

    @Test
    public void testLeaderCreatedWithCommitIndexLessThanFollowersCommitIndex() throws Exception {
        logStart("testLeaderCreatedWithCommitIndexLessThanFollowersCommitIndex");

        MockRaftActorContext leaderActorContext = createActorContext();

        MockRaftActorContext followerActorContext = createActorContext(FOLLOWER_ID, followerActor);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().behavior = follower;

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(FOLLOWER_ID, followerActor.path().toString());

        leaderActorContext.setPeerAddresses(peerAddresses);

        leaderActorContext.getReplicatedLog().removeFrom(0);

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        leaderActorContext.setCommitIndex(1);

        followerActorContext.getReplicatedLog().removeFrom(0);

        followerActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        // follower has the same log entries but its commit index > leaders commit index
        followerActorContext.setCommitIndex(2);

        leader = new Leader(leaderActorContext);

        // Initial heartbeat
        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals(1, appendEntries.getLeaderCommit());
        assertEquals(0, appendEntries.getEntries().size());
        assertEquals(0, appendEntries.getPrevLogIndex());

        AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(
                leaderActor, AppendEntriesReply.class);

        assertEquals(2, appendEntriesReply.getLogLastIndex());
        assertEquals(1, appendEntriesReply.getLogLastTerm());

        leaderActor.underlyingActor().behavior = leader;
        leader.handleMessage(followerActor, appendEntriesReply);

        leaderActor.underlyingActor().clear();
        followerActor.underlyingActor().clear();

        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, new SendHeartBeat());

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        assertEquals(2, appendEntries.getLeaderCommit());
        assertEquals(0, appendEntries.getEntries().size());
        assertEquals(2, appendEntries.getPrevLogIndex());

        appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertEquals(2, appendEntriesReply.getLogLastIndex());
        assertEquals(1, appendEntriesReply.getLogLastTerm());

        assertEquals(2, followerActorContext.getCommitIndex());

        follower.close();
    }

    @Test
    public void testHandleAppendEntriesReplyFailure(){
        logStart("testHandleAppendEntriesReplyFailure");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();

        leader = new Leader(leaderActorContext);

        // Send initial heartbeat reply with last index.
        leader.handleAppendEntriesReply(followerActor, new AppendEntriesReply(FOLLOWER_ID, 1, true, 10, 1));

        FollowerLogInformation followerInfo = leader.getFollower(FOLLOWER_ID);
        assertEquals("getNextIndex", 11, followerInfo.getNextIndex());

        AppendEntriesReply reply = new AppendEntriesReply(FOLLOWER_ID, 1, false, 10, 1);

        RaftActorBehavior raftActorBehavior = leader.handleAppendEntriesReply(followerActor, reply);

        assertEquals(RaftState.Leader, raftActorBehavior.state());

        assertEquals("getNextIndex", 10, followerInfo.getNextIndex());
    }

    @Test
    public void testHandleAppendEntriesReplySuccess() throws Exception {
        logStart("testHandleAppendEntriesReplySuccess");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

        leaderActorContext.setCommitIndex(1);
        leaderActorContext.setLastApplied(1);
        leaderActorContext.getTermInformation().update(1, "leader");

        leader = new Leader(leaderActorContext);

        AppendEntriesReply reply = new AppendEntriesReply(FOLLOWER_ID, 1, true, 2, 1);

        RaftActorBehavior raftActorBehavior = leader.handleAppendEntriesReply(followerActor, reply);

        assertEquals(RaftState.Leader, raftActorBehavior.state());

        assertEquals(2, leaderActorContext.getCommitIndex());

        ApplyLogEntries applyLogEntries = MessageCollectorActor.expectFirstMatching(
                leaderActor, ApplyLogEntries.class);

        assertEquals(2, leaderActorContext.getLastApplied());

        assertEquals(2, applyLogEntries.getToIndex());

        List<ApplyState> applyStateList = MessageCollectorActor.getAllMatching(leaderActor,
                ApplyState.class);

        assertEquals(1,applyStateList.size());

        ApplyState applyState = applyStateList.get(0);

        assertEquals(2, applyState.getReplicatedLogEntry().getIndex());
    }

    @Test
    public void testHandleAppendEntriesReplyUnknownFollower(){
        logStart("testHandleAppendEntriesReplyUnknownFollower");

        MockRaftActorContext leaderActorContext = createActorContext();

        leader = new Leader(leaderActorContext);

        AppendEntriesReply reply = new AppendEntriesReply("unkown-follower", 1, false, 10, 1);

        RaftActorBehavior raftActorBehavior = leader.handleAppendEntriesReply(followerActor, reply);

        assertEquals(RaftState.Leader, raftActorBehavior.state());
    }

    @Test
    public void testHandleRequestVoteReply(){
        logStart("testHandleRequestVoteReply");

        MockRaftActorContext leaderActorContext = createActorContext();

        leader = new Leader(leaderActorContext);

        // Should be a no-op.
        RaftActorBehavior raftActorBehavior = leader.handleRequestVoteReply(followerActor,
                new RequestVoteReply(1, true));

        assertEquals(RaftState.Leader, raftActorBehavior.state());

        raftActorBehavior = leader.handleRequestVoteReply(followerActor, new RequestVoteReply(1, false));

        assertEquals(RaftState.Leader, raftActorBehavior.state());
    }

    @Test
    public void testIsolatedLeaderCheckNoFollowers() {
        logStart("testIsolatedLeaderCheckNoFollowers");

        MockRaftActorContext leaderActorContext = createActorContext();

        leader = new Leader(leaderActorContext);
        RaftActorBehavior behavior = leader.handleMessage(leaderActor, new IsolatedLeaderCheck());
        Assert.assertTrue(behavior instanceof Leader);
    }

    @Test
    public void testIsolatedLeaderCheckTwoFollowers() throws Exception {
        logStart("testIsolatedLeaderCheckTwoFollowers");

        new JavaTestKit(getSystem()) {{

            ActorRef followerActor1 = getTestActor();
            ActorRef followerActor2 = getTestActor();

            MockRaftActorContext leaderActorContext = createActorContext();

            Map<String, String> peerAddresses = new HashMap<>();
            peerAddresses.put("follower-1", followerActor1.path().toString());
            peerAddresses.put("follower-2", followerActor2.path().toString());

            leaderActorContext.setPeerAddresses(peerAddresses);

            leader = new Leader(leaderActorContext);
            leader.stopIsolatedLeaderCheckSchedule();

            leader.markFollowerActive("follower-1");
            leader.markFollowerActive("follower-2");
            RaftActorBehavior behavior = leader.handleMessage(leaderActor, new IsolatedLeaderCheck());
            Assert.assertTrue("Behavior not instance of Leader when all followers are active",
                behavior instanceof Leader);

            // kill 1 follower and verify if that got killed
            final JavaTestKit probe = new JavaTestKit(getSystem());
            probe.watch(followerActor1);
            followerActor1.tell(PoisonPill.getInstance(), ActorRef.noSender());
            final Terminated termMsg1 = probe.expectMsgClass(Terminated.class);
            assertEquals(termMsg1.getActor(), followerActor1);

            leader.markFollowerInActive("follower-1");
            leader.markFollowerActive("follower-2");
            behavior = leader.handleMessage(leaderActor, new IsolatedLeaderCheck());
            Assert.assertTrue("Behavior not instance of Leader when majority of followers are active",
                behavior instanceof Leader);

            // kill 2nd follower and leader should change to Isolated leader
            followerActor2.tell(PoisonPill.getInstance(), null);
            probe.watch(followerActor2);
            followerActor2.tell(PoisonPill.getInstance(), ActorRef.noSender());
            final Terminated termMsg2 = probe.expectMsgClass(Terminated.class);
            assertEquals(termMsg2.getActor(), followerActor2);

            leader.markFollowerInActive("follower-2");
            behavior = leader.handleMessage(leaderActor, new IsolatedLeaderCheck());
            Assert.assertTrue("Behavior not instance of IsolatedLeader when majority followers are inactive",
                behavior instanceof IsolatedLeader);
        }};
    }


    @Test
    public void testAppendEntryCallAtEndofAppendEntryReply() throws Exception {
        logStart("testAppendEntryCallAtEndofAppendEntryReply");

        MockRaftActorContext leaderActorContext = createActorContextWithFollower();

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        //configParams.setHeartBeatInterval(new FiniteDuration(9, TimeUnit.SECONDS));
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(10, TimeUnit.SECONDS));

        leaderActorContext.setConfigParams(configParams);

        MockRaftActorContext followerActorContext = createActorContext(FOLLOWER_ID, followerActor);

        followerActorContext.setConfigParams(configParams);

        Follower follower = new Follower(followerActorContext);
        followerActor.underlyingActor().behavior = follower;

        leaderActorContext.getReplicatedLog().removeFrom(0);
        leaderActorContext.setCommitIndex(-1);
        leaderActorContext.setLastApplied(-1);

        followerActorContext.getReplicatedLog().removeFrom(0);
        followerActorContext.setCommitIndex(-1);
        followerActorContext.setLastApplied(-1);

        leader = new Leader(leaderActorContext);

        AppendEntriesReply appendEntriesReply = MessageCollectorActor.expectFirstMatching(
                leaderActor, AppendEntriesReply.class);

        leader.handleMessage(followerActor, appendEntriesReply);

        // Clear initial heartbeat messages

        leaderActor.underlyingActor().clear();
        followerActor.underlyingActor().clear();

        // create 3 entries
        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());
        leaderActorContext.setCommitIndex(1);
        leaderActorContext.setLastApplied(1);

        Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

        leader.handleMessage(leaderActor, new SendHeartBeat());

        AppendEntries appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // Should send first log entry
        assertEquals(1, appendEntries.getLeaderCommit());
        assertEquals(0, appendEntries.getEntries().get(0).getIndex());
        assertEquals(-1, appendEntries.getPrevLogIndex());

        appendEntriesReply = MessageCollectorActor.expectFirstMatching(leaderActor, AppendEntriesReply.class);

        assertEquals(1, appendEntriesReply.getLogLastTerm());
        assertEquals(0, appendEntriesReply.getLogLastIndex());

        followerActor.underlyingActor().clear();

        leader.handleAppendEntriesReply(followerActor, appendEntriesReply);

        appendEntries = MessageCollectorActor.expectFirstMatching(followerActor, AppendEntries.class);

        // Should send second log entry
        assertEquals(1, appendEntries.getLeaderCommit());
        assertEquals(1, appendEntries.getEntries().get(0).getIndex());

        follower.close();
    }

    private class MockConfigParamsImpl extends DefaultConfigParamsImpl {

        private final long electionTimeOutIntervalMillis;
        private final int snapshotChunkSize;

        public MockConfigParamsImpl(long electionTimeOutIntervalMillis, int snapshotChunkSize) {
            super();
            this.electionTimeOutIntervalMillis = electionTimeOutIntervalMillis;
            this.snapshotChunkSize = snapshotChunkSize;
        }

        @Override
        public FiniteDuration getElectionTimeOutInterval() {
            return new FiniteDuration(electionTimeOutIntervalMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public int getSnapshotChunkSize() {
            return snapshotChunkSize;
        }
    }
}
