package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.FollowerLogInformation;
import org.opendaylight.controller.cluster.raft.FollowerLogInformationImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.SerializationUtils;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.base.messages.SendInstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.protobuff.messages.InstallSnapshotMessages;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LeaderTest extends AbstractRaftActorBehaviorTest {

    private ActorRef leaderActor =
        getSystem().actorOf(Props.create(DoNothingActor.class));
    private ActorRef senderActor =
        getSystem().actorOf(Props.create(DoNothingActor.class));

    @Test
    public void testHandleMessageForUnknownMessage() throws Exception {
        new JavaTestKit(getSystem()) {{
            Leader leader =
                new Leader(createActorContext());

            // handle message should return the Leader state when it receives an
            // unknown message
            RaftState state = leader.handleMessage(senderActor, "foo");
            Assert.assertEquals(RaftState.Leader, state);
        }};
    }


    @Test
    public void testThatLeaderSendsAHeartbeatMessageToAllFollowers() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef followerActor = getTestActor();

                    MockRaftActorContext actorContext =
                        (MockRaftActorContext) createActorContext();

                    Map<String, String> peerAddresses = new HashMap();

                    peerAddresses.put(followerActor.path().toString(),
                        followerActor.path().toString());

                    actorContext.setPeerAddresses(peerAddresses);

                    Leader leader = new Leader(actorContext);
                    leader.handleMessage(senderActor, new SendHeartBeat());

                    final String out =
                        new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                            // do not put code outside this method, will run afterwards
                            protected String match(Object in) {
                                Object msg = fromSerializableMessage(in);
                                if (msg instanceof AppendEntries) {
                                    if (((AppendEntries)msg).getTerm() == 0) {
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
    public void testHandleReplicateMessageSendAppendEntriesToFollower() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef followerActor = getTestActor();

                    MockRaftActorContext actorContext =
                        (MockRaftActorContext) createActorContext();

                    Map<String, String> peerAddresses = new HashMap();

                    peerAddresses.put(followerActor.path().toString(),
                        followerActor.path().toString());

                    actorContext.setPeerAddresses(peerAddresses);

                    Leader leader = new Leader(actorContext);
                    RaftState raftState = leader
                        .handleMessage(senderActor, new Replicate(null, null,
                            new MockRaftActorContext.MockReplicatedLogEntry(1,
                                100,
                                new MockRaftActorContext.MockPayload("foo"))
                        ));

                    // State should not change
                    assertEquals(RaftState.Leader, raftState);

                    final String out =
                        new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                            // do not put code outside this method, will run afterwards
                            protected String match(Object in) {
                                Object msg = fromSerializableMessage(in);
                                if (msg instanceof AppendEntries) {
                                    if (((AppendEntries)msg).getTerm() == 0) {
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
    public void testHandleReplicateMessageWhenThereAreNoFollowers() {
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef raftActor = getTestActor();

                    MockRaftActorContext actorContext =
                        new MockRaftActorContext("test", getSystem(), raftActor);

                    actorContext.getReplicatedLog().removeFrom(0);

                    actorContext.getReplicatedLog().append(new ReplicatedLogImplEntry(0, 1,
                        new MockRaftActorContext.MockPayload("foo")));

                    ReplicatedLogImplEntry entry =
                        new ReplicatedLogImplEntry(1, 1,
                            new MockRaftActorContext.MockPayload("foo"));

                    actorContext.getReplicatedLog().append(entry);

                    Leader leader = new Leader(actorContext);
                    RaftState raftState = leader
                        .handleMessage(senderActor, new Replicate(null, "state-id",entry));

                    // State should not change
                    assertEquals(RaftState.Leader, raftState);

                    assertEquals(1, actorContext.getCommitIndex());

                    final String out =
                        new ExpectMsg<String>(duration("1 seconds"),
                            "match hint") {
                            // do not put code outside this method, will run afterwards
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
    public void testSendInstallSnapshot() {
        new LeaderTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {
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
                    actorContext.getReplicatedLog().setSnapshot(
                        toByteString(leadersSnapshot));
                    actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
                    actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);

                    MockLeader leader = new MockLeader(actorContext);
                    // set the follower info in leader
                    leader.addToFollowerToLog(followerActor.path().toString(), followersLastIndex, -1);

                    // new entry
                    ReplicatedLogImplEntry entry =
                        new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                            new MockRaftActorContext.MockPayload("D"));

                    // this should invoke a sendinstallsnapshot as followersLastIndex < snapshotIndex
                    RaftState raftState = leader.handleMessage(
                        senderActor, new Replicate(null, "state-id", entry));

                    assertEquals(RaftState.Leader, raftState);

                    // we might receive some heartbeat messages, so wait till we SendInstallSnapshot
                    Boolean[] matches = new ReceiveWhile<Boolean>(Boolean.class, duration("2 seconds")) {
                        @Override
                        protected Boolean match(Object o) throws Exception {
                            if (o instanceof SendInstallSnapshot) {
                                return true;
                            }
                            return false;
                        }
                    }.get();

                    boolean sendInstallSnapshotReceived = false;
                    for (Boolean b: matches) {
                        sendInstallSnapshotReceived = b | sendInstallSnapshotReceived;
                    }

                    assertTrue(sendInstallSnapshotReceived);

                }
            };
        }};
    }

    @Test
    public void testInstallSnapshot() {
        new LeaderTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {
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
                    actorContext.getReplicatedLog().setSnapshot(toByteString(leadersSnapshot));
                    actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
                    actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);

                    actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

                    MockLeader leader = new MockLeader(actorContext);
                    // set the follower info in leader
                    leader.addToFollowerToLog(followerActor.path().toString(), followersLastIndex, -1);

                    // new entry
                    ReplicatedLogImplEntry entry =
                        new ReplicatedLogImplEntry(newEntryIndex, currentTerm,
                            new MockRaftActorContext.MockPayload("D"));


                    RaftState raftState = leader.handleMessage(senderActor, new SendInstallSnapshot());

                    assertEquals(RaftState.Leader, raftState);

                    // check if installsnapshot gets called with the correct values.
                    final String out =
                        new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                            // do not put code outside this method, will run afterwards
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
                }
            };
        }};
    }

    @Test
    public void testHandleInstallSnapshotReplyLastChunk() {
        new LeaderTestKit(getSystem()) {{
            new Within(duration("1 seconds")) {
                protected void run() {
                    ActorRef followerActor = getTestActor();

                    Map<String, String> peerAddresses = new HashMap();
                    peerAddresses.put(followerActor.path().toString(),
                        followerActor.path().toString());

                    MockRaftActorContext actorContext =
                        (MockRaftActorContext) createActorContext();
                    actorContext.setPeerAddresses(peerAddresses);

                    final int followersLastIndex = 2;
                    final int snapshotIndex = 3;
                    final int newEntryIndex = 4;
                    final int snapshotTerm = 1;
                    final int currentTerm = 2;

                    MockLeader leader = new MockLeader(actorContext);
                    // set the follower info in leader
                    leader.addToFollowerToLog(followerActor.path().toString(), followersLastIndex, -1);

                    Map<String, String> leadersSnapshot = new HashMap<>();
                    leadersSnapshot.put("1", "A");
                    leadersSnapshot.put("2", "B");
                    leadersSnapshot.put("3", "C");

                    // set the snapshot variables in replicatedlog
                    actorContext.getReplicatedLog().setSnapshot(
                        toByteString(leadersSnapshot));
                    actorContext.getReplicatedLog().setSnapshotIndex(snapshotIndex);
                    actorContext.getReplicatedLog().setSnapshotTerm(snapshotTerm);
                    actorContext.getTermInformation().update(currentTerm, leaderActor.path().toString());

                    ByteString bs = toByteString(leadersSnapshot);
                    leader.createFollowerToSnapshot(followerActor.path().toString(), bs);
                    while(!leader.getFollowerToSnapshot().isLastChunk(leader.getFollowerToSnapshot().getChunkIndex())) {
                        leader.getFollowerToSnapshot().getNextChunk();
                        leader.getFollowerToSnapshot().incrementChunkIndex();
                    }

                    //clears leaders log
                    actorContext.getReplicatedLog().removeFrom(0);

                    RaftState raftState = leader.handleMessage(senderActor,
                        new InstallSnapshotReply(currentTerm, followerActor.path().toString(),
                            leader.getFollowerToSnapshot().getChunkIndex(), true));

                    assertEquals(RaftState.Leader, raftState);

                    assertEquals(leader.mapFollowerToSnapshot.size(), 0);
                    assertEquals(leader.followerToLog.size(), 1);
                    assertNotNull(leader.followerToLog.get(followerActor.path().toString()));
                    FollowerLogInformation fli = leader.followerToLog.get(followerActor.path().toString());
                    assertEquals(snapshotIndex, fli.getMatchIndex().get());
                    assertEquals(snapshotIndex, fli.getMatchIndex().get());
                    assertEquals(snapshotIndex + 1, fli.getNextIndex().get());
                }
            };
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

    protected RaftActorContext createActorContext(ActorRef actorRef) {
        return new MockRaftActorContext("test", getSystem(), actorRef);
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

    private static class LeaderTestKit extends JavaTestKit {

        private LeaderTestKit(ActorSystem actorSystem) {
            super(actorSystem);
        }

        protected void waitForLogMessage(final Class logLevel, ActorRef subject, String logMessage){
            // Wait for a specific log message to show up
            final boolean result =
            new JavaTestKit.EventFilter<Boolean>(logLevel
            ) {
                @Override
                protected Boolean run() {
                    return true;
                }
            }.from(subject.path().toString())
                .message(logMessage)
                .occurrences(1).exec();

            Assert.assertEquals(true, result);

        }
    }

    class MockLeader extends Leader {

        FollowerToSnapshot fts;

        public MockLeader(RaftActorContext context){
            super(context);
        }

        public void addToFollowerToLog(String followerId, long nextIndex, long matchIndex) {
            FollowerLogInformation followerLogInformation =
                new FollowerLogInformationImpl(followerId,
                    new AtomicLong(nextIndex),
                    new AtomicLong(matchIndex));
            followerToLog.put(followerId, followerLogInformation);
        }

        public FollowerToSnapshot getFollowerToSnapshot() {
            return fts;
        }

        public void createFollowerToSnapshot(String followerId, ByteString bs ) {
            fts = new FollowerToSnapshot(bs);
            mapFollowerToSnapshot.put(followerId, fts);

        }
    }
}
