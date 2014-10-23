package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.MockAkkaJournal;
import org.opendaylight.controller.cluster.raft.utils.MockSnapshotStore;
import scala.concurrent.duration.FiniteDuration;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;

public class RaftActorTest extends AbstractActorTest {


    @After
    public void tearDown() {
        MockAkkaJournal.clearJournal();
        MockSnapshotStore.setMockSnapshot(null);
    }

    public static class MockRaftActor extends RaftActor {

        public static final class MockRaftActorCreator implements Creator<MockRaftActor> {
            private final Map<String, String> peerAddresses;
            private final String id;
            private final Optional<ConfigParams> config;

            private MockRaftActorCreator(Map<String, String> peerAddresses, String id,
                    Optional<ConfigParams> config) {
                this.peerAddresses = peerAddresses;
                this.id = id;
                this.config = config;
            }

            @Override
            public MockRaftActor create() throws Exception {
                return new MockRaftActor(id, peerAddresses, config);
            }
        }

        private final CountDownLatch recoveryComplete = new CountDownLatch(1);
        private final List<Object> state;

        public MockRaftActor(String id, Map<String, String> peerAddresses, Optional<ConfigParams> config) {
            super(id, peerAddresses, config);
            state = new ArrayList<>();
        }

        public void waitForRecoveryComplete() {
            try {
                assertEquals("Recovery complete", true, recoveryComplete.await(5,  TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public List<Object> getState() {
            return state;
        }

        public static Props props(final String id, final Map<String, String> peerAddresses,
                Optional<ConfigParams> config){
            return Props.create(new MockRaftActorCreator(peerAddresses, id, config));
        }

        @Override protected void applyState(ActorRef clientActor, String identifier, Object data) {
        }

        @Override
        protected void startLogRecoveryBatch(int maxBatchSize) {
        }

        @Override
        protected void appendRecoveredLogEntry(Payload data) {
            state.add(data);
        }

        @Override
        protected void applyCurrentLogRecoveryBatch() {
        }

        @Override
        protected void onRecoveryComplete() {
            recoveryComplete.countDown();
        }

        @Override
        protected void applyRecoverySnapshot(ByteString snapshot) {
            try {
                Object data = toObject(snapshot);
                System.out.println("!!!!!applyRecoverySnapshot: "+data);
                if (data instanceof List) {
                    state.addAll((List) data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override protected void createSnapshot() {
            throw new UnsupportedOperationException("createSnapshot");
        }

        @Override protected void applySnapshot(ByteString snapshot) {
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

            raftActor = this.getSystem().actorOf(MockRaftActor.props(actorName,
                    Collections.EMPTY_MAP, Optional.<ConfigParams>absent()), actorName);

        }


        public boolean waitForStartup(){
            // Wait for a specific log message to show up
            return
                new JavaTestKit.EventFilter<Boolean>(Logging.Info.class
                ) {
                    @Override
                    protected Boolean run() {
                        return true;
                    }
                }.from(raftActor.path().toString())
                    .message("Switching from behavior Candidate to Leader")
                    .occurrences(1).exec();


        }

        public void findLeader(final String expectedLeader){
            raftActor.tell(new FindLeader(), getRef());

            FindLeaderReply reply = expectMsgClass(duration("5 seconds"), FindLeaderReply.class);
            assertEquals("getLeaderActor", expectedLeader, reply.getLeaderActor());
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
    public void testRaftActorRecovery() throws Exception {
        new JavaTestKit(getSystem()) {{
            String persistenceId = "follower10";

            DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
            // Set the heartbeat interval high to essentially disable election otherwise the test
            // may fail if the actor is switched to Leader and the commitIndex is set to the last
            // log entry.
            config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

            ActorRef followerActor = getSystem().actorOf(MockRaftActor.props(persistenceId,
                    Collections.EMPTY_MAP, Optional.<ConfigParams>of(config)), persistenceId);

            watch(followerActor);

            List<ReplicatedLogEntry> snapshotUnappliedEntries = new ArrayList<>();
            ReplicatedLogEntry entry1 = new MockRaftActorContext.MockReplicatedLogEntry(1, 4,
                    new MockRaftActorContext.MockPayload("E"));
            snapshotUnappliedEntries.add(entry1);

            int lastAppliedDuringSnapshotCapture = 3;
            int lastIndexDuringSnapshotCapture = 4;

                // 4 messages as part of snapshot, which are applied to state
            ByteString snapshotBytes  = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

            Snapshot snapshot = Snapshot.create(snapshotBytes.toByteArray(),
                    snapshotUnappliedEntries, lastIndexDuringSnapshotCapture, 1 ,
                    lastAppliedDuringSnapshotCapture, 1);
            MockSnapshotStore.setMockSnapshot(snapshot);
            MockSnapshotStore.setPersistenceId(persistenceId);

            // add more entries after snapshot is taken
            List<ReplicatedLogEntry> entries = new ArrayList<>();
            ReplicatedLogEntry entry2 = new MockRaftActorContext.MockReplicatedLogEntry(1, 5,
                    new MockRaftActorContext.MockPayload("F"));
            ReplicatedLogEntry entry3 = new MockRaftActorContext.MockReplicatedLogEntry(1, 6,
                    new MockRaftActorContext.MockPayload("G"));
            ReplicatedLogEntry entry4 = new MockRaftActorContext.MockReplicatedLogEntry(1, 7,
                    new MockRaftActorContext.MockPayload("H"));
            entries.add(entry2);
            entries.add(entry3);
            entries.add(entry4);

            int lastAppliedToState = 5;
            int lastIndex = 7;

            MockAkkaJournal.addToJournal(5, entry2);
            // 2 entries are applied to state besides the 4 entries in snapshot
            MockAkkaJournal.addToJournal(6, new ApplyLogEntries(lastAppliedToState));
            MockAkkaJournal.addToJournal(7, entry3);
            MockAkkaJournal.addToJournal(8, entry4);

            // kill the actor
            followerActor.tell(PoisonPill.getInstance(), null);
            expectMsgClass(duration("5 seconds"), Terminated.class);

            unwatch(followerActor);

            //reinstate the actor
            TestActorRef<MockRaftActor> ref = TestActorRef.create(getSystem(),
                    MockRaftActor.props(persistenceId, Collections.EMPTY_MAP,
                            Optional.<ConfigParams>of(config)));

            ref.underlyingActor().waitForRecoveryComplete();

            RaftActorContext context = ref.underlyingActor().getRaftActorContext();
            assertEquals("Journal log size", snapshotUnappliedEntries.size() + entries.size(),
                    context.getReplicatedLog().size());
            assertEquals("Last index", lastIndex, context.getReplicatedLog().lastIndex());
            assertEquals("Last applied", lastAppliedToState, context.getLastApplied());
            assertEquals("Commit index", lastAppliedToState, context.getCommitIndex());
            assertEquals("Recovered state size", 6, ref.underlyingActor().getState().size());
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
