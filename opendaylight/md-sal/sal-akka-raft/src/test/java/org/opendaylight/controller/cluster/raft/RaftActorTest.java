package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.utils.MockAkkaJournal;
import org.opendaylight.controller.cluster.raft.utils.MockSnapshotStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

public class RaftActorTest extends AbstractActorTest {


    @After
    public void tearDown() {
        MockAkkaJournal.clearJournal();
        MockSnapshotStore.setMockSnapshot(null);
    }

    public static class MockRaftActor extends RaftActor {

        private boolean applySnapshotCalled = false;
        private List<Object> state;

        public MockRaftActor(String id,
            Map<String, String> peerAddresses) {
            super(id, peerAddresses);
            state = new ArrayList<>();
        }

        public RaftActorContext getRaftActorContext() {
            return context;
        }

        public boolean isApplySnapshotCalled() {
            return applySnapshotCalled;
        }

        public List<Object> getState() {
            return state;
        }

        public static Props props(final String id, final Map<String, String> peerAddresses){
            return Props.create(new Creator<MockRaftActor>(){

                @Override public MockRaftActor create() throws Exception {
                    return new MockRaftActor(id, peerAddresses);
                }
            });
        }

        @Override protected void applyState(ActorRef clientActor, String identifier, Object data) {
            state.add(data);
        }

        @Override protected void createSnapshot() {
            throw new UnsupportedOperationException("createSnapshot");
        }

        @Override protected void applySnapshot(ByteString snapshot) {
            applySnapshotCalled = true;
            try {
                Object data = toObject(snapshot);
                if (data instanceof List) {
                    state.addAll((List) data);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override protected void onStateChanged() {
        }

        @Override public String persistenceId() {
            return this.getId();
        }

        private Object toObject(ByteString bs) throws ClassNotFoundException, IOException {
            Object obj = null;
            ByteArrayInputStream bis = null;
            ObjectInputStream ois = null;
            try {
                bis = new ByteArrayInputStream(bs.toByteArray());
                ois = new ObjectInputStream(bis);
                obj = ois.readObject();
            } finally {
                if (bis != null) {
                    bis.close();
                }
                if (ois != null) {
                    ois.close();
                }
            }
            return obj;
        }


    }


    private static class RaftActorTestKit extends JavaTestKit {
        private final ActorRef raftActor;

        public RaftActorTestKit(ActorSystem actorSystem, String actorName) {
            super(actorSystem);

            raftActor = this.getSystem()
                .actorOf(MockRaftActor.props(actorName,
                    Collections.EMPTY_MAP), actorName);

        }


        public boolean waitForStartup(){
            // Wait for a specific log message to show up
            return
                new JavaTestKit.EventFilter<Boolean>(Logging.Info.class
                ) {
                    protected Boolean run() {
                        return true;
                    }
                }.from(raftActor.path().toString())
                    .message("Switching from state Candidate to Leader")
                    .occurrences(1).exec();


        }

        public void findLeader(final String expectedLeader){


            new Within(duration("1 seconds")) {
                protected void run() {

                    raftActor.tell(new FindLeader(), getRef());

                    String s = new ExpectMsg<String>(duration("1 seconds"),
                        "findLeader") {
                        // do not put code outside this method, will run afterwards
                        protected String match(Object in) {
                            if (in instanceof FindLeaderReply) {
                                return ((FindLeaderReply) in).getLeaderActor();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get();// this extracts the received message

                    assertEquals(expectedLeader, s);

                }


            };
        }

        public ActorRef getRaftActor() {
            return raftActor;
        }

    }


    @Test
    public void testConstruction() {
        boolean started = new RaftActorTestKit(getSystem(), "testConstruction").waitForStartup();
        assertEquals(true, started);
    }

    @Test
    public void testFindLeaderWhenLeaderIsSelf(){
        RaftActorTestKit kit = new RaftActorTestKit(getSystem(), "testFindLeader");
        kit.waitForStartup();
        kit.findLeader(kit.getRaftActor().path().toString());
    }

    @Test
    public void testRaftActorRecovery() {
        new JavaTestKit(getSystem()) {{
            new Within(duration("1 seconds")) {
                protected void run() {

                    String persistenceId = "follower10";

                    ActorRef followerActor = getSystem().actorOf(
                        MockRaftActor.props(persistenceId, Collections.EMPTY_MAP), persistenceId);

                    List<ReplicatedLogEntry> snapshotEntries = new ArrayList<>();
                    ReplicatedLogEntry entry1 = new MockRaftActorContext.MockReplicatedLogEntry(1, 4, new MockRaftActorContext.MockPayload("E"));
                    snapshotEntries.add(entry1);

                    int lastAppliedDuringSnapshotCapture = 3;
                    int lastIndexDuringSnapshotCapture = 4;


                    ByteString snapshotBytes = null;
                    try {
                        // 4 messages as part of snapshot, which are applied to state
                        snapshotBytes  = fromObject(Arrays.asList(new MockRaftActorContext.MockPayload("A"),
                            new MockRaftActorContext.MockPayload("B"),
                            new MockRaftActorContext.MockPayload("C"),
                            new MockRaftActorContext.MockPayload("D")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Snapshot snapshot = Snapshot.create(snapshotBytes.toByteArray(),
                        snapshotEntries, lastIndexDuringSnapshotCapture, 1 ,
                        lastAppliedDuringSnapshotCapture, 1);
                    MockSnapshotStore.setMockSnapshot(snapshot);
                    MockSnapshotStore.setPersistenceId(persistenceId);

                    // add more entries after snapshot is taken
                    List<ReplicatedLogEntry> entries = new ArrayList<>();
                    ReplicatedLogEntry entry2 = new MockRaftActorContext.MockReplicatedLogEntry(1, 5, new MockRaftActorContext.MockPayload("F"));
                    ReplicatedLogEntry entry3 = new MockRaftActorContext.MockReplicatedLogEntry(1, 6, new MockRaftActorContext.MockPayload("G"));
                    ReplicatedLogEntry entry4 = new MockRaftActorContext.MockReplicatedLogEntry(1, 7, new MockRaftActorContext.MockPayload("H"));
                    entries.add(entry2);
                    entries.add(entry3);
                    entries.add(entry4);

                    MockAkkaJournal.addToJournal(4, entry1);
                    MockAkkaJournal.addToJournal(5, entry2);
                    // 2 entries are applied to state besides the 4 in snapshot
                    MockAkkaJournal.addToJournal(6, new ApplyLogEntries(5));
                    MockAkkaJournal.addToJournal(7, entry3);
                    MockAkkaJournal.addToJournal(8, entry4);

                    int lastAppliedToState = 5;
                    int lastIndex = 7;

                    // kill the actor
                    followerActor.tell(PoisonPill.getInstance(), null);

                    try {
                        // give some time for actor to die
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //reinstate the actor
                    TestActorRef<MockRaftActor> ref = TestActorRef.create(getSystem(),
                        MockRaftActor.props(persistenceId, Collections.EMPTY_MAP));

                    try {
                        //give some time for snapshot offer to get called.
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    RaftActorContext context = ref.underlyingActor().getRaftActorContext();
                    assertEquals(snapshotEntries.size() + entries.size(), context.getReplicatedLog().size());
                    assertEquals(lastIndex, context.getReplicatedLog().lastIndex());
                    assertEquals(lastAppliedToState, context.getLastApplied());
                    assertEquals(lastAppliedToState, context.getCommitIndex());
                    assertTrue(ref.underlyingActor().isApplySnapshotCalled());
                    assertEquals(6, ref.underlyingActor().getState().size());
                }
            };
        }};

    }

    private ByteString fromObject(Object snapshot) throws Exception {
        ByteArrayOutputStream b = null;
        ObjectOutputStream o = null;
        try {
            b = new ByteArrayOutputStream();
            o = new ObjectOutputStream(b);
            o.writeObject(snapshot);
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
    }
}
