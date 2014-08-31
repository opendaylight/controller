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

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;

import org.junit.Test;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.MockSnapshotStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class RaftActorTest extends AbstractActorTest {


    public static class MockRaftActor extends RaftActor {

        public static final class MockRaftActorCreator implements Creator<MockRaftActor> {
            private final Map<String, String> peerAddresses;
            private final String id;

            private MockRaftActorCreator(Map<String, String> peerAddresses, String id) {
                this.peerAddresses = peerAddresses;
                this.id = id;
            }

            @Override public MockRaftActor create() throws Exception {
                return new MockRaftActor(id, peerAddresses);
            }
        }

        CountDownLatch applyRecoverySnapshotCalled = new CountDownLatch(1);

        public MockRaftActor(String id,
            Map<String, String> peerAddresses) {
            super(id, peerAddresses);
        }

        public RaftActorContext getRaftActorContext() {
            return context;
        }

        public void waitForApplySnapshotCalled() {
            Uninterruptibles.awaitUninterruptibly(applyRecoverySnapshotCalled, 5, TimeUnit.SECONDS);
        }

        public static Props props(final String id, final Map<String, String> peerAddresses){
            return Props.create(new MockRaftActorCreator(peerAddresses, id));
        }

        @Override protected void applyState(ActorRef clientActor,
            String identifier,
            Object data) {
        }

        @Override
        protected void startLogRecoveryBatch(int maxBatchSize) {
        }

        @Override
        protected void appendRecoveryLogEntry(Payload data) {
        }

        @Override
        protected void applyCurrentLogRecoveryBatch() {
        }

        @Override
        protected void onRecoveryComplete() {
        }

        @Override
        protected void applyRecoverySnapshot(ByteString snapshot) {
            applyRecoverySnapshotCalled.countDown();
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
                    @Override
                    protected Boolean run() {
                        return true;
                    }
                }.from(raftActor.path().toString())
                    .message("Switching from state Candidate to Leader")
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
    public void testActorRecovery() {
        new JavaTestKit(getSystem()) {{
            String persistenceId = "follower10";

            ActorRef followerActor = getSystem().actorOf(
                    MockRaftActor.props(persistenceId, Collections.EMPTY_MAP), persistenceId);

            watch(followerActor);

            List<ReplicatedLogEntry> entries = new ArrayList<>();
            ReplicatedLogEntry entry1 = new MockRaftActorContext.MockReplicatedLogEntry(1, 4,
                    new MockRaftActorContext.MockPayload("E"));
            ReplicatedLogEntry entry2 = new MockRaftActorContext.MockReplicatedLogEntry(1, 5,
                    new MockRaftActorContext.MockPayload("F"));
            entries.add(entry1);
            entries.add(entry2);

            int lastApplied = 3;
            int lastIndex = 5;
            byte[] state = "A B C D".getBytes();
            Snapshot snapshot = Snapshot.create(state, entries, lastIndex, 1 , lastApplied, 1);
            MockSnapshotStore.setMockSnapshot(snapshot);
            MockSnapshotStore.setPersistenceId(persistenceId);

            followerActor.tell(PoisonPill.getInstance(), null);

            expectMsgClass(duration("5 seconds"), Terminated.class);

            unwatch(followerActor);

            TestActorRef<MockRaftActor> ref = TestActorRef.create(getSystem(),
                    MockRaftActor.props(persistenceId, Collections.EMPTY_MAP));

            ref.underlyingActor().waitForApplySnapshotCalled();

            RaftActorContext context = ref.underlyingActor().getRaftActorContext();
            assertEquals("Journal log size", entries.size(), context.getReplicatedLog().size());
            assertEquals("getLastApplied", lastApplied, context.getLastApplied());
            assertEquals("getCommitIndex", lastApplied, context.getCommitIndex());
            assertArrayEquals("", state, context.getReplicatedLog().getSnapshot().toByteArray());
        }};
    }
}
