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
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.SerializationUtils;
import org.opendaylight.controller.cluster.raft.SnapshotSupport;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateInstallSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.IsolatedLeaderCheck;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;
import org.opendaylight.controller.protobuff.messages.cluster.raft.InstallSnapshotMessages;
import scala.concurrent.duration.FiniteDuration;

public class LeaderTest extends AbstractRaftActorBehaviorTest {

    private final ActorRef leaderActor =
        getSystem().actorOf(Props.create(DoNothingActor.class));
    private final ActorRef senderActor =
        getSystem().actorOf(Props.create(DoNothingActor.class));

    @Test
    public void testHandleMessageForUnknownMessage() throws Exception {
        new JavaTestKit(getSystem()) {{
            Leader leader =
                new Leader(createActorContext());

            // handle message should return the Leader state when it receives an
            // unknown message
            RaftActorBehavior behavior = leader.handleMessage(senderActor, "foo");
            Assert.assertTrue(behavior instanceof Leader);
        }};
    }


    @Test
    public void testThatLeaderSendsAHeartbeatMessageToAllFollowers() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    ActorRef followerActor = getTestActor();

                    MockRaftActorContext actorContext = (MockRaftActorContext) createActorContext();

                    Map<String, String> peerAddresses = new HashMap();

                    String followerId = "follower";
                    peerAddresses.put(followerId, followerActor.path().toString());

                    actorContext.setPeerAddresses(peerAddresses);

                    long term = 1;
                    actorContext.getTermInformation().update(term, "");

                    Leader leader = new Leader(actorContext);

                    // Leader should send an immediate heartbeat with no entries as follower is inactive.
                    long lastIndex = actorContext.getReplicatedLog().lastIndex();
                    AppendEntries appendEntries = AppendEntries.fromSerializable(
                            expectMsgClass(duration("5 seconds"), AppendEntriesMessages.AppendEntries.class));
                    assertEquals("getTerm", term, appendEntries.getTerm());
                    assertEquals("getPrevLogIndex", -1, appendEntries.getPrevLogIndex());
                    assertEquals("getPrevLogTerm", -1, appendEntries.getPrevLogTerm());
                    assertEquals("Entries size", 0, appendEntries.getEntries().size());

                    // The follower would normally reply - simulate that explicitly here.
                    leader.handleMessage(followerActor, new AppendEntriesReply(
                            followerId, term, true, lastIndex - 1, term));
                    assertEquals("isFollowerActive", true, leader.getFollower(followerId).isFollowerActive());

                    // Sleep for the heartbeat interval so AppendEntries is sent.
                    Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().
                            getHeartBeatInterval().toMillis(), TimeUnit.MILLISECONDS);

                    leader.handleMessage(senderActor, new SendHeartBeat());

                    appendEntries = AppendEntries.fromSerializable(expectMsgClass(duration("5 seconds"),
                            AppendEntriesMessages.AppendEntries.class));
                    assertEquals("getPrevLogIndex", lastIndex - 1, appendEntries.getPrevLogIndex());
                    assertEquals("getPrevLogTerm", term, appendEntries.getPrevLogTerm());
                    assertEquals("Entries size", 1, appendEntries.getEntries().size());
                    assertEquals("Entry getIndex", lastIndex, appendEntries.getEntries().get(0).getIndex());
                    assertEquals("Entry getTerm", term, appendEntries.getEntries().get(0).getTerm());
                }
            };
        }};
    }

    @Test
    public void testHandleReplicateMessageSendAppendEntriesToFollower() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    ActorRef followerActor = getTestActor();

                    MockRaftActorContext actorContext =
                        (MockRaftActorContext) createActorContext();

                    Map<String, String> peerAddresses = new HashMap();

                    String followerId = "follower";
                    peerAddresses.put(followerId, followerActor.path().toString());

                    actorContext.setPeerAddresses(peerAddresses);

                    long term = 1;
                    actorContext.getTermInformation().update(term, "");

                    Leader leader = new Leader(actorContext);

                    // Leader will send an immediate heartbeat - ignore it.
                    expectMsgClass(duration("5 seconds"), AppendEntriesMessages.AppendEntries.class);

                    // The follower would normally reply - simulate that explicitly here.
                    long lastIndex = actorContext.getReplicatedLog().lastIndex();
                    leader.handleMessage(followerActor, new AppendEntriesReply(
                            followerId, term, true, lastIndex, term));
                    assertEquals("isFollowerActive", true, leader.getFollower(followerId).isFollowerActive());

                    MockRaftActorContext.MockPayload payload = new MockRaftActorContext.MockPayload("foo");
                    MockRaftActorContext.MockReplicatedLogEntry newEntry = new MockRaftActorContext.MockReplicatedLogEntry(
                            1, lastIndex + 1, payload);
                    actorContext.getReplicatedLog().append(newEntry);
                    RaftActorBehavior raftBehavior = leader.handleMessage(senderActor,
                            new Replicate(null, null, newEntry));

                    // State should not change
                    assertTrue(raftBehavior instanceof Leader);

                    AppendEntries appendEntries = AppendEntries.fromSerializable(
                            expectMsgClass(duration("5 seconds"), AppendEntriesMessages.AppendEntries.class));
                    assertEquals("getPrevLogIndex", lastIndex, appendEntries.getPrevLogIndex());
                    assertEquals("getPrevLogTerm", term, appendEntries.getPrevLogTerm());
                    assertEquals("Entries size", 1, appendEntries.getEntries().size());
                    assertEquals("Entry getIndex", lastIndex + 1, appendEntries.getEntries().get(0).getIndex());
                    assertEquals("Entry getTerm", term, appendEntries.getEntries().get(0).getTerm());
                    assertEquals("Entry payload", payload, appendEntries.getEntries().get(0).getData());
                }
            };
        }};
    }

    @Test
    public void testHandleReplicateMessageWhenThereAreNoFollowers() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    ActorRef raftActor = getTestActor();

                    MockRaftActorContext actorContext =
                        new MockRaftActorContext("test", getSystem(), raftActor);

                    actorContext.getReplicatedLog().removeFrom(0);

                    actorContext.setReplicatedLog(
                        new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 2, 1)
                            .build());

                    Leader leader = new Leader(actorContext);
                    RaftActorBehavior raftBehavior = leader
                        .handleMessage(senderActor, new Replicate(null, "state-id",actorContext.getReplicatedLog().get(1)));

                    // State should not change
                    assertTrue(raftBehavior instanceof Leader);

                    assertEquals(1, actorContext.getCommitIndex());

                    final String out =
                        new ExpectMsg<String>(duration("1 seconds"),
                            "match hint") {
                            // do not put code outside this method, will run afterwards
                            @Override
                            protected String match(Object in) {
                                if (in instanceof ApplyState) {
                                    if (((ApplyState) in).getIdentifier().equals("state-id")) {
                                        return "match";
                                    }
                                    return null;
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertEquals("match", out);

                }
            };
        }};
    }

    @Test
    public void testSendAppendEntriesOnAnInProgressInstallSnapshot() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef followerActor = getSystem().actorOf(Props.create(MessageCollectorActor.class));

            Map<String, String> peerAddresses = new HashMap();
            peerAddresses.put(followerActor.path().toString(),
                followerActor.path().toString());

            MockRaftActorContext actorContext =
                (MockRaftActorContext) createActorContext(leaderActor);
            actorContext.setPeerAddresses(peerAddresses);

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

            MockLeader leader = new MockLeader(actorContext);

            // new entry
            ReplicatedLogImplEntry entry =
                new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                    new MockRaftActorContext.MockPayload("D"));

            //update follower timestamp
            leader.markFollowerActive(followerActor.path().toString());

            ByteString bs = toByteString(leadersSnapshot);
            leader.setSnapshot(Optional.of(bs));
            leader.createFollowerToSnapshot(followerActor.path().toString(), bs);

            //send first chunk and no InstallSnapshotReply received yet
            leader.getFollowerToSnapshot().getNextChunk();
            leader.getFollowerToSnapshot().incrementChunkIndex();

            Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

            leader.handleMessage(leaderActor, new SendHeartBeat());

            AppendEntriesMessages.AppendEntries aeproto = (AppendEntriesMessages.AppendEntries)MessageCollectorActor.getFirstMatching(
                followerActor, AppendEntries.SERIALIZABLE_CLASS);

            assertNotNull("AppendEntries should be sent even if InstallSnapshotReply is not " +
                "received", aeproto);

            AppendEntries ae = (AppendEntries) SerializationUtils.fromSerializable(aeproto);

            assertTrue("AppendEntries should be sent with empty entries", ae.getEntries().isEmpty());

            //InstallSnapshotReply received
            leader.getFollowerToSnapshot().markSendStatus(true);

            leader.handleMessage(senderActor, new SendHeartBeat());

            InstallSnapshotMessages.InstallSnapshot isproto = (InstallSnapshotMessages.InstallSnapshot)
                MessageCollectorActor.getFirstMatching(followerActor,
                    InstallSnapshot.SERIALIZABLE_CLASS);

            assertNotNull("Installsnapshot should get called for sending the next chunk of snapshot",
                isproto);

            InstallSnapshot is = (InstallSnapshot) SerializationUtils.fromSerializable(isproto);

            assertEquals(snapshotIndex, is.getLastIncludedIndex());

        }};
    }

    @Test
    public void testSendAppendEntriesSnapshotScenario() {
        new JavaTestKit(getSystem()) {{

            ActorRef followerActor = getTestActor();

            Map<String, String> peerAddresses = new HashMap();
            peerAddresses.put(followerActor.path().toString(),
                followerActor.path().toString());

            MockRaftActorContext actorContext =
                (MockRaftActorContext) createActorContext(getRef());
            actorContext.setPeerAddresses(peerAddresses);

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

            Leader leader = new Leader(actorContext);

            // new entry
            ReplicatedLogImplEntry entry =
                new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                    new MockRaftActorContext.MockPayload("D"));

            //update follower timestamp
            leader.markFollowerActive(followerActor.path().toString());

            Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

            // this should invoke a sendinstallsnapshot as followersLastIndex < snapshotIndex
            RaftActorBehavior raftBehavior = leader.handleMessage(
                senderActor, new Replicate(null, "state-id", entry));

            assertTrue(raftBehavior instanceof Leader);

            // we might receive some heartbeat messages, so wait till we InitiateInstallSnapshot
            Boolean[] matches = new ReceiveWhile<Boolean>(Boolean.class, duration("2 seconds")) {
                @Override
                protected Boolean match(Object o) throws Exception {
                    if (o instanceof InitiateInstallSnapshot) {
                        return true;
                    }
                    return false;
                }
            }.get();

            boolean initiateInitiateInstallSnapshot = false;
            for (Boolean b: matches) {
                initiateInitiateInstallSnapshot = b | initiateInitiateInstallSnapshot;
            }

            assertTrue(initiateInitiateInstallSnapshot);
        }};
    }

    @Test
    public void testInitiateInstallSnapshot() throws Exception {
        new JavaTestKit(getSystem()) {{

            ActorRef leaderActor = getSystem().actorOf(Props.create(MessageCollectorActor.class));

            ActorRef followerActor = getTestActor();

            Map<String, String> peerAddresses = new HashMap();
            peerAddresses.put(followerActor.path().toString(),
                followerActor.path().toString());


            MockRaftActorContext actorContext =
                (MockRaftActorContext) createActorContext(leaderActor);
            actorContext.setPeerAddresses(peerAddresses);

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

            SnapshotSupport mockSnapshotSupport = Mockito.mock(SnapshotSupport.class);
            actorContext.setSnapshotSupport(mockSnapshotSupport);

            Leader leader = new Leader(actorContext);
            // set the snapshot as absent and check if capture-snapshot is invoked.
            leader.setSnapshot(Optional.<ByteString>absent());

            // new entry
            ReplicatedLogImplEntry entry =
                new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                    new MockRaftActorContext.MockPayload("D"));

            actorContext.getReplicatedLog().append(entry);

            // this should invoke a sendinstallsnapshot as followersLastIndex < snapshotIndex
            leader.handleMessage(leaderActor, new InitiateInstallSnapshot());

            Mockito.verify(mockSnapshotSupport).capture(1, 3, -1, true);

            Mockito.reset(mockSnapshotSupport);

            actorContext.setSnapshotCaptureInitiated(true);

            // if an initiate is started again when first is in progress, it shouldnt initiate Capture
            leader.handleMessage(leaderActor, new InitiateInstallSnapshot());

            Mockito.verify(mockSnapshotSupport, Mockito.never()).capture(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyLong(), Mockito.anyBoolean());
        }};
    }

    @Test
    public void testInstallSnapshot() {
        new JavaTestKit(getSystem()) {{

            ActorRef followerActor = getTestActor();

            Map<String, String> peerAddresses = new HashMap();
            peerAddresses.put(followerActor.path().toString(),
                followerActor.path().toString());

            MockRaftActorContext actorContext =
                (MockRaftActorContext) createActorContext();
            actorContext.setPeerAddresses(peerAddresses);


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
            actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());
            actorContext.setCommitIndex(followersLastIndex);

            Leader leader = new Leader(actorContext);

            // Ignore initial heartbeat.
            expectMsgClass(duration("5 seconds"), AppendEntriesMessages.AppendEntries.class);

            // new entry
            ReplicatedLogImplEntry entry =
                new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                    new MockRaftActorContext.MockPayload("D"));

            RaftActorBehavior raftBehavior = leader.handleMessage(senderActor,
                new SendInstallSnapshot(toByteString(leadersSnapshot)));

            assertTrue(raftBehavior instanceof Leader);

            // check if installsnapshot gets called with the correct values.
            final String out =
                new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                    // do not put code outside this method, will run afterwards
                    @Override
                    protected String match(Object in) {
                        if (in instanceof InstallSnapshotMessages.InstallSnapshot) {
                            InstallSnapshot is = (InstallSnapshot)
                                SerializationUtils.fromSerializable(in);
                            if (is.getData() == null) {
                                return "InstallSnapshot data is null";
                            }
                            if (is.getLastIncludedIndex() != snapshotIndex) {
                                return is.getLastIncludedIndex() + "!=" + snapshotIndex;
                            }
                            if (is.getLastIncludedTerm() != snapshotTerm) {
                                return is.getLastIncludedTerm() + "!=" + snapshotTerm;
                            }
                            if (is.getTerm() == currentTerm) {
                                return is.getTerm() + "!=" + currentTerm;
                            }

                            return "match";

                        } else {
                            return "message mismatch:" + in.getClass();
                        }
                    }
                }.get(); // this extracts the received message

            assertEquals("match", out);
        }};
    }

    @Test
    public void testHandleInstallSnapshotReplyLastChunk() {
        new JavaTestKit(getSystem()) {{

            ActorRef followerActor = getTestActor();

            Map<String, String> peerAddresses = new HashMap();
            peerAddresses.put(followerActor.path().toString(),
                followerActor.path().toString());

            final int followersLastIndex = 2;
            final int snapshotIndex = 3;
            final int newEntryIndex = 4;
            final int snapshotTerm = 1;
            final int currentTerm = 2;

            MockRaftActorContext actorContext =
                (MockRaftActorContext) createActorContext();
            actorContext.setPeerAddresses(peerAddresses);
            actorContext.setCommitIndex(followersLastIndex);

            MockLeader leader = new MockLeader(actorContext);

            // Ignore initial heartbeat.
            expectMsgClass(duration("5 seconds"), AppendEntriesMessages.AppendEntries.class);

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
            leader.createFollowerToSnapshot(followerActor.path().toString(), bs);
            while(!leader.getFollowerToSnapshot().isLastChunk(leader.getFollowerToSnapshot().getChunkIndex())) {
                leader.getFollowerToSnapshot().getNextChunk();
                leader.getFollowerToSnapshot().incrementChunkIndex();
            }

            //clears leaders log
            actorContext.getReplicatedLog().removeFrom(0);

            RaftActorBehavior raftBehavior = leader.handleMessage(senderActor,
                new InstallSnapshotReply(currentTerm, followerActor.path().toString(),
                    leader.getFollowerToSnapshot().getChunkIndex(), true));

            assertTrue(raftBehavior instanceof Leader);

            assertEquals(leader.mapFollowerToSnapshot.size(), 0);
            assertEquals(leader.followerToLog.size(), 1);
            assertNotNull(leader.followerToLog.get(followerActor.path().toString()));
            FollowerLogInformation fli = leader.followerToLog.get(followerActor.path().toString());
            assertEquals(snapshotIndex, fli.getMatchIndex().get());
            assertEquals(snapshotIndex, fli.getMatchIndex().get());
            assertEquals(snapshotIndex + 1, fli.getNextIndex().get());
        }};
    }

    @Test
    public void testHandleInstallSnapshotReplyWithInvalidChunkIndex() throws Exception{
        new JavaTestKit(getSystem()) {{

            TestActorRef<MessageCollectorActor> followerActor =
                    TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class), "follower");

            Map<String, String> peerAddresses = new HashMap<>();
            peerAddresses.put(followerActor.path().toString(),
                    followerActor.path().toString());

            final int followersLastIndex = 2;
            final int snapshotIndex = 3;
            final int snapshotTerm = 1;
            final int currentTerm = 2;

            MockRaftActorContext actorContext =
                    (MockRaftActorContext) createActorContext();

            actorContext.setConfigParams(new DefaultConfigParamsImpl(){
                @Override
                public int getSnapshotChunkSize() {
                    return 50;
                }
            });
            actorContext.setPeerAddresses(peerAddresses);
            actorContext.setCommitIndex(followersLastIndex);

            MockLeader leader = new MockLeader(actorContext);

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


            MessageCollectorActor.getAllMatching(followerActor,
                    InstallSnapshotMessages.InstallSnapshot.class);

            InstallSnapshotMessages.InstallSnapshot installSnapshot = MessageCollectorActor.getFirstMatching(
                    followerActor, InstallSnapshotMessages.InstallSnapshot.class);
            assertNotNull(installSnapshot);

            assertEquals(1, installSnapshot.getChunkIndex());
            assertEquals(3, installSnapshot.getTotalChunks());

            followerActor.underlyingActor().clear();

            leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                followerActor.path().toString(), -1, false));

            Uninterruptibles.sleepUninterruptibly(actorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                TimeUnit.MILLISECONDS);

            leader.handleMessage(leaderActor, new SendHeartBeat());

            installSnapshot = MessageCollectorActor.getFirstMatching(
                    followerActor, InstallSnapshotMessages.InstallSnapshot.class);
            assertNotNull(installSnapshot);

            assertEquals(1, installSnapshot.getChunkIndex());
            assertEquals(3, installSnapshot.getTotalChunks());

            followerActor.tell(PoisonPill.getInstance(), getRef());
        }};
    }

    @Test
    public void testHandleSnapshotSendsPreviousChunksHashCodeWhenSendingNextChunk() throws Exception {
        new JavaTestKit(getSystem()) {
            {

                TestActorRef<MessageCollectorActor> followerActor =
                        TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class), "follower-chunk");

                Map<String, String> peerAddresses = new HashMap<>();
                peerAddresses.put(followerActor.path().toString(),
                        followerActor.path().toString());

                final int followersLastIndex = 2;
                final int snapshotIndex = 3;
                final int snapshotTerm = 1;
                final int currentTerm = 2;

                MockRaftActorContext actorContext =
                        (MockRaftActorContext) createActorContext();

                actorContext.setConfigParams(new DefaultConfigParamsImpl() {
                    @Override
                    public int getSnapshotChunkSize() {
                        return 50;
                    }
                });
                actorContext.setPeerAddresses(peerAddresses);
                actorContext.setCommitIndex(followersLastIndex);

                MockLeader leader = new MockLeader(actorContext);

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

                InstallSnapshotMessages.InstallSnapshot installSnapshot = MessageCollectorActor.getFirstMatching(
                        followerActor, InstallSnapshotMessages.InstallSnapshot.class);
                assertNotNull(installSnapshot);

                assertEquals(1, installSnapshot.getChunkIndex());
                assertEquals(3, installSnapshot.getTotalChunks());
                assertEquals(AbstractLeader.INITIAL_LAST_CHUNK_HASH_CODE, installSnapshot.getLastChunkHashCode());

                int hashCode = installSnapshot.getData().hashCode();

                followerActor.underlyingActor().clear();

                leader.handleMessage(followerActor, new InstallSnapshotReply(installSnapshot.getTerm(),followerActor.path().toString(),1,true ));

                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                leader.handleMessage(leaderActor, new SendHeartBeat());

                installSnapshot = MessageCollectorActor.getFirstMatching(
                        followerActor, InstallSnapshotMessages.InstallSnapshot.class);
                assertNotNull(installSnapshot);
                assertEquals(2, installSnapshot.getChunkIndex());
                assertEquals(3, installSnapshot.getTotalChunks());
                assertEquals(hashCode, installSnapshot.getLastChunkHashCode());

                followerActor.tell(PoisonPill.getInstance(), getRef());
            }};
    }

    @Test
    public void testSendSnapshotfromInstallSnapshotReply() throws Exception {
        new JavaTestKit(getSystem()) {{

            TestActorRef<MessageCollectorActor> followerActor =
                TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class), "follower-reply");

            Map<String, String> peerAddresses = new HashMap<>();
            peerAddresses.put("follower-reply",
                followerActor.path().toString());

            final int followersLastIndex = 2;
            final int snapshotIndex = 3;
            final int snapshotTerm = 1;
            final int currentTerm = 2;

            MockRaftActorContext actorContext =
                (MockRaftActorContext) createActorContext();
            DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl(){
                @Override
                public int getSnapshotChunkSize() {
                    return 50;
                }
            };
            configParams.setHeartBeatInterval(new FiniteDuration(9, TimeUnit.SECONDS));
            configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(10, TimeUnit.SECONDS));

            actorContext.setConfigParams(configParams);
            actorContext.setPeerAddresses(peerAddresses);
            actorContext.setCommitIndex(followersLastIndex);

            MockLeader leader = new MockLeader(actorContext);

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

            List<Object> objectList = MessageCollectorActor.getAllMatching(followerActor,
                InstallSnapshotMessages.InstallSnapshot.class);

            assertEquals(1, objectList.size());

            Object o = objectList.get(0);
            assertTrue(o instanceof InstallSnapshotMessages.InstallSnapshot);

            InstallSnapshotMessages.InstallSnapshot installSnapshot = (InstallSnapshotMessages.InstallSnapshot) o;

            assertEquals(1, installSnapshot.getChunkIndex());
            assertEquals(3, installSnapshot.getTotalChunks());

            leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                "follower-reply", installSnapshot.getChunkIndex(), true));

            objectList = MessageCollectorActor.getAllMatching(followerActor,
                InstallSnapshotMessages.InstallSnapshot.class);

            assertEquals(2, objectList.size());

            installSnapshot = (InstallSnapshotMessages.InstallSnapshot) objectList.get(1);

            leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                "follower-reply", installSnapshot.getChunkIndex(), true));

            objectList = MessageCollectorActor.getAllMatching(followerActor,
                InstallSnapshotMessages.InstallSnapshot.class);

            assertEquals(3, objectList.size());

            installSnapshot = (InstallSnapshotMessages.InstallSnapshot) objectList.get(2);

            // Send snapshot reply one more time and make sure that a new snapshot message should not be sent to follower
            leader.handleMessage(followerActor, new InstallSnapshotReply(actorContext.getTermInformation().getCurrentTerm(),
                "follower-reply", installSnapshot.getChunkIndex(), true));

            objectList = MessageCollectorActor.getAllMatching(followerActor,
                InstallSnapshotMessages.InstallSnapshot.class);

            // Count should still stay at 3
            assertEquals(3, objectList.size());
        }};
    }


    @Test
    public void testFollowerToSnapshotLogic() {

        MockRaftActorContext actorContext = (MockRaftActorContext) createActorContext();

        actorContext.setConfigParams(new DefaultConfigParamsImpl() {
            @Override
            public int getSnapshotChunkSize() {
                return 50;
            }
        });

        MockLeader leader = new MockLeader(actorContext);

        Map<String, String> leadersSnapshot = new HashMap<>();
        leadersSnapshot.put("1", "A");
        leadersSnapshot.put("2", "B");
        leadersSnapshot.put("3", "C");

        ByteString bs = toByteString(leadersSnapshot);
        byte[] barray = bs.toByteArray();

        leader.createFollowerToSnapshot("followerId", bs);
        assertEquals(bs.size(), barray.length);

        int chunkIndex=0;
        for (int i=0; i < barray.length; i = i + 50) {
            int j = i + 50;
            chunkIndex++;

            if (i + 50 > barray.length) {
                j = barray.length;
            }

            ByteString chunk = leader.getFollowerToSnapshot().getNextChunk();
            assertEquals("bytestring size not matching for chunk:"+ chunkIndex, j-i, chunk.size());
            assertEquals("chunkindex not matching", chunkIndex, leader.getFollowerToSnapshot().getChunkIndex());

            leader.getFollowerToSnapshot().markSendStatus(true);
            if (!leader.getFollowerToSnapshot().isLastChunk(chunkIndex)) {
                leader.getFollowerToSnapshot().incrementChunkIndex();
            }
        }

        assertEquals("totalChunks not matching", chunkIndex, leader.getFollowerToSnapshot().getTotalChunks());
    }


    @Override protected RaftActorBehavior createBehavior(
        RaftActorContext actorContext) {
        return new Leader(actorContext);
    }

    @Override protected RaftActorContext createActorContext() {
        return createActorContext(leaderActor);
    }

    @Override
    protected RaftActorContext createActorContext(ActorRef actorRef) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(50, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        MockRaftActorContext context = new MockRaftActorContext("test", getSystem(), actorRef);
        context.setConfigParams(configParams);
        return context;
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
            Assert.fail("IOException in converting Hashmap to Bytestring:" + e);
        }
        return null;
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
        new JavaTestKit(getSystem()) {{

            TestActorRef<ForwardMessageToBehaviorActor> leaderActor = TestActorRef.create(getSystem(),
                    Props.create(ForwardMessageToBehaviorActor.class));

            MockRaftActorContext leaderActorContext =
                    new MockRaftActorContext("leader", getSystem(), leaderActor);

            TestActorRef<ForwardMessageToBehaviorActor> followerActor = TestActorRef.create(getSystem(),
                    ForwardMessageToBehaviorActor.props());

            MockRaftActorContext followerActorContext =
                    new MockRaftActorContext("follower", getSystem(), followerActor);

            Follower follower = new Follower(followerActorContext);
            followerActor.underlyingActor().behavior = follower;

            Map<String, String> peerAddresses = new HashMap<>();
            peerAddresses.put("follower", followerActor.path().toString());

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

            Leader leader = new Leader(leaderActorContext);

            AppendEntries appendEntries = AppendEntries.fromSerializable(
                    MessageCollectorActor.expectFirstMatching(followerActor, AppendEntriesMessages.AppendEntries.class));
            assertNotNull(appendEntries);

            assertEquals(1, appendEntries.getLeaderCommit());
            assertEquals(0, appendEntries.getEntries().size());
            assertEquals(0, appendEntries.getPrevLogIndex());

            AppendEntriesReply appendEntriesReply = MessageCollectorActor.getFirstMatching(
                    leaderActor, AppendEntriesReply.class);
            assertNotNull(appendEntriesReply);

            assertEquals(2, appendEntriesReply.getLogLastIndex());
            assertEquals(1, appendEntriesReply.getLogLastTerm());

            // follower returns its next index
            assertEquals(2, appendEntriesReply.getLogLastIndex());
            assertEquals(1, appendEntriesReply.getLogLastTerm());
        }};
    }


    @Test
    public void testLeaderCreatedWithCommitIndexLessThanFollowersCommitIndex() throws Exception {
        new JavaTestKit(getSystem()) {{

            TestActorRef<ForwardMessageToBehaviorActor> leaderActor = TestActorRef.create(getSystem(),
                    Props.create(ForwardMessageToBehaviorActor.class));

            MockRaftActorContext leaderActorContext =
                    new MockRaftActorContext("leader", getSystem(), leaderActor);

            TestActorRef<ForwardMessageToBehaviorActor> followerActor = TestActorRef.create(getSystem(),
                    ForwardMessageToBehaviorActor.props());

            MockRaftActorContext followerActorContext =
                    new MockRaftActorContext("follower", getSystem(), followerActor);

            Follower follower = new Follower(followerActorContext);
            followerActor.underlyingActor().behavior = follower;

            Map<String, String> peerAddresses = new HashMap<>();
            peerAddresses.put("follower", followerActor.path().toString());

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

            Leader leader = new Leader(leaderActorContext);

            // Initial heartbeat
            AppendEntries appendEntries = AppendEntries.fromSerializable(
                    MessageCollectorActor.expectFirstMatching(followerActor, AppendEntriesMessages.AppendEntries.class));
            assertNotNull(appendEntries);

            assertEquals(1, appendEntries.getLeaderCommit());
            assertEquals(0, appendEntries.getEntries().size());
            assertEquals(0, appendEntries.getPrevLogIndex());

            AppendEntriesReply appendEntriesReply = MessageCollectorActor.getFirstMatching(
                    leaderActor, AppendEntriesReply.class);
            assertNotNull(appendEntriesReply);

            assertEquals(2, appendEntriesReply.getLogLastIndex());
            assertEquals(1, appendEntriesReply.getLogLastTerm());

            leaderActor.underlyingActor().behavior = leader;
            leader.handleMessage(followerActor, appendEntriesReply);

            leaderActor.underlyingActor().clear();
            followerActor.underlyingActor().clear();

            Uninterruptibles.sleepUninterruptibly(leaderActorContext.getConfigParams().getHeartBeatInterval().toMillis(),
                    TimeUnit.MILLISECONDS);

            leader.handleMessage(leaderActor, new SendHeartBeat());

            appendEntries = AppendEntries.fromSerializable(
                    MessageCollectorActor.getFirstMatching(followerActor, AppendEntriesMessages.AppendEntries.class));
            assertNotNull(appendEntries);

            assertEquals(1, appendEntries.getLeaderCommit());
            assertEquals(0, appendEntries.getEntries().size());
            assertEquals(2, appendEntries.getPrevLogIndex());

            appendEntriesReply = MessageCollectorActor.getFirstMatching(leaderActor, AppendEntriesReply.class);
            assertNotNull(appendEntriesReply);

            assertEquals(2, appendEntriesReply.getLogLastIndex());
            assertEquals(1, appendEntriesReply.getLogLastTerm());

            assertEquals(1, followerActorContext.getCommitIndex());
        }};
    }

    @Test
    public void testHandleAppendEntriesReplyFailure(){
        new JavaTestKit(getSystem()) {
            {

                ActorRef leaderActor =
                    getSystem().actorOf(Props.create(MessageCollectorActor.class));

                ActorRef followerActor =
                    getSystem().actorOf(Props.create(MessageCollectorActor.class));


                MockRaftActorContext leaderActorContext =
                    new MockRaftActorContext("leader", getSystem(), leaderActor);

                Map<String, String> peerAddresses = new HashMap();
                peerAddresses.put("follower-1",
                    followerActor.path().toString());

                leaderActorContext.setPeerAddresses(peerAddresses);

                Leader leader = new Leader(leaderActorContext);

                AppendEntriesReply reply = new AppendEntriesReply("follower-1", 1, false, 10, 1);

                RaftActorBehavior raftActorBehavior = leader.handleAppendEntriesReply(followerActor, reply);

                assertEquals(RaftState.Leader, raftActorBehavior.state());

            }};
    }

    @Test
    public void testHandleAppendEntriesReplySuccess() throws Exception {
        new JavaTestKit(getSystem()) {
            {

                ActorRef leaderActor =
                    getSystem().actorOf(Props.create(MessageCollectorActor.class));

                ActorRef followerActor =
                    getSystem().actorOf(Props.create(MessageCollectorActor.class));


                MockRaftActorContext leaderActorContext =
                    new MockRaftActorContext("leader", getSystem(), leaderActor);

                leaderActorContext.setReplicatedLog(
                    new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(0, 3, 1).build());

                Map<String, String> peerAddresses = new HashMap();
                peerAddresses.put("follower-1",
                    followerActor.path().toString());

                leaderActorContext.setPeerAddresses(peerAddresses);
                leaderActorContext.setCommitIndex(1);
                leaderActorContext.setLastApplied(1);
                leaderActorContext.getTermInformation().update(1, "leader");

                Leader leader = new Leader(leaderActorContext);

                AppendEntriesReply reply = new AppendEntriesReply("follower-1", 1, true, 2, 1);

                RaftActorBehavior raftActorBehavior = leader.handleAppendEntriesReply(followerActor, reply);

                assertEquals(RaftState.Leader, raftActorBehavior.state());

                assertEquals(2, leaderActorContext.getCommitIndex());

                ApplyLogEntries applyLogEntries =
                    MessageCollectorActor.getFirstMatching(leaderActor,
                    ApplyLogEntries.class);

                assertNotNull(applyLogEntries);

                assertEquals(2, leaderActorContext.getLastApplied());

                assertEquals(2, applyLogEntries.getToIndex());

                List<Object> applyStateList = MessageCollectorActor.getAllMatching(leaderActor,
                    ApplyState.class);

                assertEquals(1,applyStateList.size());

                ApplyState applyState = (ApplyState) applyStateList.get(0);

                assertEquals(2, applyState.getReplicatedLogEntry().getIndex());

            }};
    }

    @Test
    public void testHandleAppendEntriesReplyUnknownFollower(){
        new JavaTestKit(getSystem()) {
            {

                ActorRef leaderActor =
                    getSystem().actorOf(Props.create(MessageCollectorActor.class));

                MockRaftActorContext leaderActorContext =
                    new MockRaftActorContext("leader", getSystem(), leaderActor);

                Leader leader = new Leader(leaderActorContext);

                AppendEntriesReply reply = new AppendEntriesReply("follower-1", 1, false, 10, 1);

                RaftActorBehavior raftActorBehavior = leader.handleAppendEntriesReply(getRef(), reply);

                assertEquals(RaftState.Leader, raftActorBehavior.state());

            }};
    }

    @Test
    public void testHandleRequestVoteReply(){
        new JavaTestKit(getSystem()) {
            {

                ActorRef leaderActor =
                    getSystem().actorOf(Props.create(MessageCollectorActor.class));

                MockRaftActorContext leaderActorContext =
                    new MockRaftActorContext("leader", getSystem(), leaderActor);

                Leader leader = new Leader(leaderActorContext);

                RaftActorBehavior raftActorBehavior = leader.handleRequestVoteReply(getRef(), new RequestVoteReply(1, true));

                assertEquals(RaftState.Leader, raftActorBehavior.state());

                raftActorBehavior = leader.handleRequestVoteReply(getRef(), new RequestVoteReply(1, false));

                assertEquals(RaftState.Leader, raftActorBehavior.state());
            }};
    }

    @Test
    public void testIsolatedLeaderCheckNoFollowers() {
        new JavaTestKit(getSystem()) {{
            ActorRef leaderActor = getTestActor();

            MockRaftActorContext leaderActorContext =
                new MockRaftActorContext("leader", getSystem(), leaderActor);

            Map<String, String> peerAddresses = new HashMap<>();
            leaderActorContext.setPeerAddresses(peerAddresses);

            Leader leader = new Leader(leaderActorContext);
            RaftActorBehavior behavior = leader.handleMessage(leaderActor, new IsolatedLeaderCheck());
            Assert.assertTrue(behavior instanceof Leader);
        }};
    }

    @Test
    public void testIsolatedLeaderCheckTwoFollowers() throws Exception {
        new JavaTestKit(getSystem()) {{

            ActorRef followerActor1 = getTestActor();
            ActorRef followerActor2 = getTestActor();

            MockRaftActorContext leaderActorContext = (MockRaftActorContext) createActorContext();

            Map<String, String> peerAddresses = new HashMap<>();
            peerAddresses.put("follower-1", followerActor1.path().toString());
            peerAddresses.put("follower-2", followerActor2.path().toString());

            leaderActorContext.setPeerAddresses(peerAddresses);

            Leader leader = new Leader(leaderActorContext);
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
        new JavaTestKit(getSystem()) {{

            TestActorRef<MessageCollectorActor> leaderActor = TestActorRef.create(getSystem(),
                    Props.create(MessageCollectorActor.class));

            MockRaftActorContext leaderActorContext =
                    new MockRaftActorContext("leader", getSystem(), leaderActor);

            DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
            //configParams.setHeartBeatInterval(new FiniteDuration(9, TimeUnit.SECONDS));
            configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(10, TimeUnit.SECONDS));

            leaderActorContext.setConfigParams(configParams);

            TestActorRef<ForwardMessageToBehaviorActor> followerActor = TestActorRef.create(getSystem(),
                    ForwardMessageToBehaviorActor.props());

            MockRaftActorContext followerActorContext =
                    new MockRaftActorContext("follower-reply", getSystem(), followerActor);

            followerActorContext.setConfigParams(configParams);

            Follower follower = new Follower(followerActorContext);
            followerActor.underlyingActor().behavior = follower;

            Map<String, String> peerAddresses = new HashMap<>();
            peerAddresses.put("follower-reply",
                    followerActor.path().toString());

            leaderActorContext.setPeerAddresses(peerAddresses);

            leaderActorContext.getReplicatedLog().removeFrom(0);
            leaderActorContext.setCommitIndex(-1);
            leaderActorContext.setLastApplied(-1);

            followerActorContext.getReplicatedLog().removeFrom(0);
            followerActorContext.setCommitIndex(-1);
            followerActorContext.setLastApplied(-1);

            Leader leader = new Leader(leaderActorContext);

            AppendEntriesReply appendEntriesReply = MessageCollectorActor.getFirstMatching(
                    leaderActor, AppendEntriesReply.class);
            assertNotNull(appendEntriesReply);
            System.out.println("appendEntriesReply: "+appendEntriesReply);
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

            AppendEntries appendEntries = AppendEntries.fromSerializable(MessageCollectorActor.getFirstMatching(
                    followerActor, AppendEntriesMessages.AppendEntries.class));
            assertNotNull(appendEntries);

            // Should send first log entry
            assertEquals(1, appendEntries.getLeaderCommit());
            assertEquals(0, appendEntries.getEntries().get(0).getIndex());
            assertEquals(-1, appendEntries.getPrevLogIndex());

            appendEntriesReply = MessageCollectorActor.getFirstMatching(leaderActor, AppendEntriesReply.class);
            assertNotNull(appendEntriesReply);

            assertEquals(1, appendEntriesReply.getLogLastTerm());
            assertEquals(0, appendEntriesReply.getLogLastIndex());

            followerActor.underlyingActor().clear();

            leader.handleAppendEntriesReply(followerActor, appendEntriesReply);

            appendEntries = AppendEntries.fromSerializable(MessageCollectorActor.getFirstMatching(
                    followerActor, AppendEntriesMessages.AppendEntries.class));
            assertNotNull(appendEntries);

            // Should send second log entry
            assertEquals(1, appendEntries.getLeaderCommit());
            assertEquals(1, appendEntries.getEntries().get(0).getIndex());
        }};
    }

    class MockLeader extends Leader {

        FollowerToSnapshot fts;

        public MockLeader(RaftActorContext context){
            super(context);
        }

        public FollowerToSnapshot getFollowerToSnapshot() {
            return fts;
        }

        public void createFollowerToSnapshot(String followerId, ByteString bs ) {
            fts = new FollowerToSnapshot(bs);
            mapFollowerToSnapshot.put(followerId, fts);

        }
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
