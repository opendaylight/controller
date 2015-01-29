package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.japi.Creator;
import akka.japi.Procedure;
import akka.pattern.Patterns;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotSelectionCriteria;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.datastore.DataPersistenceProviderMonitor;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.raft.utils.MockAkkaJournal;
import org.opendaylight.controller.cluster.raft.utils.MockSnapshotStore;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RaftActorTest extends AbstractActorTest {


    @After
    public void tearDown() {
        MockAkkaJournal.clearJournal();
        MockSnapshotStore.setMockSnapshot(null);
    }

    public static class MockRaftActor extends RaftActor {

        private final DataPersistenceProvider dataPersistenceProvider;
        private final RaftActor delegate;
        private final CountDownLatch recoveryComplete = new CountDownLatch(1);
        private final List<Object> state;
        private ActorRef roleChangeNotifier;

        public static final class MockRaftActorCreator implements Creator<MockRaftActor> {
            private static final long serialVersionUID = 1L;
            private final Map<String, String> peerAddresses;
            private final String id;
            private final Optional<ConfigParams> config;
            private final DataPersistenceProvider dataPersistenceProvider;
            private final ActorRef roleChangeNotifier;

            private MockRaftActorCreator(Map<String, String> peerAddresses, String id,
                Optional<ConfigParams> config, DataPersistenceProvider dataPersistenceProvider,
                ActorRef roleChangeNotifier) {
                this.peerAddresses = peerAddresses;
                this.id = id;
                this.config = config;
                this.dataPersistenceProvider = dataPersistenceProvider;
                this.roleChangeNotifier = roleChangeNotifier;
            }

            @Override
            public MockRaftActor create() throws Exception {
                MockRaftActor mockRaftActor = new MockRaftActor(id, peerAddresses, config,
                    dataPersistenceProvider);
                mockRaftActor.roleChangeNotifier = this.roleChangeNotifier;
                return mockRaftActor;
            }
        }

        public MockRaftActor(String id, Map<String, String> peerAddresses, Optional<ConfigParams> config, DataPersistenceProvider dataPersistenceProvider) {
            super(id, peerAddresses, config);
            state = new ArrayList<>();
            this.delegate = mock(RaftActor.class);
            if(dataPersistenceProvider == null){
                this.dataPersistenceProvider = new PersistentDataProvider();
            } else {
                this.dataPersistenceProvider = dataPersistenceProvider;
            }
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
            return Props.create(new MockRaftActorCreator(peerAddresses, id, config, null, null));
        }

        public static Props props(final String id, final Map<String, String> peerAddresses,
                                  Optional<ConfigParams> config, DataPersistenceProvider dataPersistenceProvider){
            return Props.create(new MockRaftActorCreator(peerAddresses, id, config, dataPersistenceProvider, null));
        }

        public static Props props(final String id, final Map<String, String> peerAddresses,
            Optional<ConfigParams> config, ActorRef roleChangeNotifier){
            return Props.create(new MockRaftActorCreator(peerAddresses, id, config, null, roleChangeNotifier));
        }

        @Override protected void applyState(ActorRef clientActor, String identifier, Object data) {
            delegate.applyState(clientActor, identifier, data);
            LOG.info("applyState called");
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
            delegate.onRecoveryComplete();
            recoveryComplete.countDown();
        }

        @Override
        protected void applyRecoverySnapshot(byte[] bytes) {
            delegate.applyRecoverySnapshot(bytes);
            try {
                Object data = toObject(bytes);
                if (data instanceof List) {
                    state.addAll((List<?>) data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override protected void createSnapshot() {
            delegate.createSnapshot();
        }

        @Override protected void applySnapshot(byte [] snapshot) {
            delegate.applySnapshot(snapshot);
        }

        @Override protected void onStateChanged() {
            delegate.onStateChanged();
        }

        @Override
        protected DataPersistenceProvider persistence() {
            return this.dataPersistenceProvider;
        }

        @Override
        protected Optional<ActorRef> getRoleChangeNotifier() {
            return Optional.fromNullable(roleChangeNotifier);
        }

        @Override public String persistenceId() {
            return this.getId();
        }

        private Object toObject(byte[] bs) throws ClassNotFoundException, IOException {
            Object obj = null;
            ByteArrayInputStream bis = null;
            ObjectInputStream ois = null;
            try {
                bis = new ByteArrayInputStream(bs);
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

        public ReplicatedLog getReplicatedLog(){
            return this.getRaftActorContext().getReplicatedLog();
        }

    }


    private static class RaftActorTestKit extends JavaTestKit {
        private final ActorRef raftActor;

        public RaftActorTestKit(ActorSystem actorSystem, String actorName) {
            super(actorSystem);

            raftActor = this.getSystem().actorOf(MockRaftActor.props(actorName,
                    Collections.<String,String>emptyMap(), Optional.<ConfigParams>absent()), actorName);

        }


        public ActorRef getRaftActor() {
            return raftActor;
        }

        public boolean waitForLogMessage(final Class<?> logEventClass, String message){
            // Wait for a specific log message to show up
            return
                new JavaTestKit.EventFilter<Boolean>(logEventClass
                ) {
                    @Override
                    protected Boolean run() {
                        return true;
                    }
                }.from(raftActor.path().toString())
                    .message(message)
                    .occurrences(1).exec();


        }

        protected void waitUntilLeader(){
            waitUntilLeader(raftActor);
        }

        protected void waitUntilLeader(ActorRef actorRef) {
            FiniteDuration duration = Duration.create(100, TimeUnit.MILLISECONDS);
            for(int i = 0; i < 20 * 5; i++) {
                Future<Object> future = Patterns.ask(actorRef, new FindLeader(), new Timeout(duration));
                try {
                    FindLeaderReply resp = (FindLeaderReply) Await.result(future, duration);
                    if(resp.getLeaderActor() != null) {
                        return;
                    }
                } catch(TimeoutException e) {
                } catch(Exception e) {
                    System.err.println("FindLeader threw ex");
                    e.printStackTrace();
                }


                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }

            Assert.fail("Leader not found for actorRef " + actorRef.path());
        }

    }


    @Test
    public void testConstruction() {
        new RaftActorTestKit(getSystem(), "testConstruction").waitUntilLeader();
    }

    @Test
    public void testFindLeaderWhenLeaderIsSelf(){
        RaftActorTestKit kit = new RaftActorTestKit(getSystem(), "testFindLeader");
        kit.waitUntilLeader();
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
                    Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config)), persistenceId);

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
                    MockRaftActor.props(persistenceId, Collections.<String,String>emptyMap(),
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

    /**
     * This test verifies that when recovery is applicable (typically when persistence is true) the RaftActor does
     * process recovery messages
     *
     * @throws Exception
     */

    @Test
    public void testHandleRecoveryWhenDataPersistenceRecoveryApplicable() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testHandleRecoveryWhenDataPersistenceRecoveryApplicable";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config)), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                // Wait for akka's recovery to complete so it doesn't interfere.
                mockRaftActor.waitForRecoveryComplete();

                ByteString snapshotBytes  = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                Snapshot snapshot = Snapshot.create(snapshotBytes.toByteArray(),
                        Lists.<ReplicatedLogEntry>newArrayList(), 3, 1 ,3, 1);

                mockRaftActor.onReceiveRecover(new SnapshotOffer(new SnapshotMetadata(persistenceId, 100, 100), snapshot));

                verify(mockRaftActor.delegate).applyRecoverySnapshot(eq(snapshotBytes.toByteArray()));

                mockRaftActor.onReceiveRecover(new ReplicatedLogImplEntry(0, 1, new MockRaftActorContext.MockPayload("A")));

                ReplicatedLog replicatedLog = mockRaftActor.getReplicatedLog();

                assertEquals("add replicated log entry", 1, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new ReplicatedLogImplEntry(1, 1, new MockRaftActorContext.MockPayload("A")));

                assertEquals("add replicated log entry", 2, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new ApplyLogEntries(1));

                assertEquals("commit index 1", 1, mockRaftActor.getRaftActorContext().getCommitIndex());

                // The snapshot had 4 items + we added 2 more items during the test
                // We start removing from 5 and we should get 1 item in the replicated log
                mockRaftActor.onReceiveRecover(new RaftActor.DeleteEntries(5));

                assertEquals("remove log entries", 1, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new RaftActor.UpdateElectionTerm(10, "foobar"));

                assertEquals("election term", 10, mockRaftActor.getRaftActorContext().getTermInformation().getCurrentTerm());
                assertEquals("voted for", "foobar", mockRaftActor.getRaftActorContext().getTermInformation().getVotedFor());

                mockRaftActor.onReceiveRecover(mock(RecoveryCompleted.class));

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }};
    }

    /**
     * This test verifies that when recovery is not applicable (typically when persistence is false) the RaftActor does
     * not process recovery messages
     *
     * @throws Exception
     */
    @Test
    public void testHandleRecoveryWhenDataPersistenceRecoveryNotApplicable() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testHandleRecoveryWhenDataPersistenceRecoveryNotApplicable";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), new DataPersistenceProviderMonitor()), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                // Wait for akka's recovery to complete so it doesn't interfere.
                mockRaftActor.waitForRecoveryComplete();

                ByteString snapshotBytes  = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                Snapshot snapshot = Snapshot.create(snapshotBytes.toByteArray(),
                        Lists.<ReplicatedLogEntry>newArrayList(), 3, 1 ,3, 1);

                mockRaftActor.onReceiveRecover(new SnapshotOffer(new SnapshotMetadata(persistenceId, 100, 100), snapshot));

                verify(mockRaftActor.delegate, times(0)).applyRecoverySnapshot(any(byte[].class));

                mockRaftActor.onReceiveRecover(new ReplicatedLogImplEntry(0, 1, new MockRaftActorContext.MockPayload("A")));

                ReplicatedLog replicatedLog = mockRaftActor.getReplicatedLog();

                assertEquals("add replicated log entry", 0, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new ReplicatedLogImplEntry(1, 1, new MockRaftActorContext.MockPayload("A")));

                assertEquals("add replicated log entry", 0, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new ApplyLogEntries(1));

                assertEquals("commit index -1", -1, mockRaftActor.getRaftActorContext().getCommitIndex());

                mockRaftActor.onReceiveRecover(new RaftActor.DeleteEntries(2));

                assertEquals("remove log entries", 0, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new RaftActor.UpdateElectionTerm(10, "foobar"));

                assertNotEquals("election term", 10, mockRaftActor.getRaftActorContext().getTermInformation().getCurrentTerm());
                assertNotEquals("voted for", "foobar", mockRaftActor.getRaftActorContext().getTermInformation().getVotedFor());

                mockRaftActor.onReceiveRecover(mock(RecoveryCompleted.class));

                mockActorRef.tell(PoisonPill.getInstance(), getRef());
            }};
    }


    @Test
    public void testUpdatingElectionTermCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testUpdatingElectionTermCallsDataPersistence";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                CountDownLatch persistLatch = new CountDownLatch(1);
                DataPersistenceProviderMonitor dataPersistenceProviderMonitor = new DataPersistenceProviderMonitor();
                dataPersistenceProviderMonitor.setPersistLatch(persistLatch);

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProviderMonitor), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.getRaftActorContext().getTermInformation().updateAndPersist(10, "foobar");

                assertEquals("Persist called", true, persistLatch.await(5, TimeUnit.SECONDS));

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testAddingReplicatedLogEntryCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testAddingReplicatedLogEntryCallsDataPersistence";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                MockRaftActorContext.MockReplicatedLogEntry logEntry = new MockRaftActorContext.MockReplicatedLogEntry(10, 10, mock(Payload.class));

                mockRaftActor.getRaftActorContext().getReplicatedLog().appendAndPersist(logEntry);

                verify(dataPersistenceProvider).persist(eq(logEntry), any(Procedure.class));

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testRemovingReplicatedLogEntryCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testRemovingReplicatedLogEntryCallsDataPersistence";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.getReplicatedLog().appendAndPersist(new MockRaftActorContext.MockReplicatedLogEntry(1, 0, mock(Payload.class)));

                mockRaftActor.getRaftActorContext().getReplicatedLog().removeFromAndPersist(0);

                verify(dataPersistenceProvider, times(2)).persist(anyObject(), any(Procedure.class));

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testApplyLogEntriesCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testApplyLogEntriesCallsDataPersistence";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.onReceiveCommand(new ApplyLogEntries(10));

                verify(dataPersistenceProvider, times(1)).persist(anyObject(), any(Procedure.class));

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testCaptureSnapshotReplyCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testCaptureSnapshotReplyCallsDataPersistence";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(),
                    MockRaftActor.props(persistenceId,Collections.<String,String>emptyMap(),
                        Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                ByteString snapshotBytes  = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                mockRaftActor.onReceiveCommand(new CaptureSnapshot(-1,1,-1,1));

                RaftActorContext raftActorContext = mockRaftActor.getRaftActorContext();

                mockRaftActor.setCurrentBehavior(new Leader(raftActorContext));

                mockRaftActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));

                verify(dataPersistenceProvider).saveSnapshot(anyObject());

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testSaveSnapshotSuccessCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testSaveSnapshotSuccessCallsDataPersistence";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1,0, mock(Payload.class)));
                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1,1, mock(Payload.class)));
                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1,2, mock(Payload.class)));
                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1,3, mock(Payload.class)));
                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1,4, mock(Payload.class)));

                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                RaftActorContext raftActorContext = mockRaftActor.getRaftActorContext();
                mockRaftActor.setCurrentBehavior(new Follower(raftActorContext));

                mockRaftActor.onReceiveCommand(new CaptureSnapshot(-1, 1, 2, 1));

                verify(mockRaftActor.delegate).createSnapshot();

                mockRaftActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));

                mockRaftActor.onReceiveCommand(new SaveSnapshotSuccess(new SnapshotMetadata("foo", 100, 100)));

                verify(dataPersistenceProvider).deleteSnapshots(any(SnapshotSelectionCriteria.class));

                verify(dataPersistenceProvider).deleteMessages(100);

                assertEquals(2, mockRaftActor.getReplicatedLog().size());

                assertNotNull(mockRaftActor.getReplicatedLog().get(3));
                assertNotNull(mockRaftActor.getReplicatedLog().get(4));

                // Index 2 will not be in the log because it was removed due to snapshotting
                assertNull(mockRaftActor.getReplicatedLog().get(2));

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testApplyState() throws Exception {

        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testApplyState";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                ReplicatedLogEntry entry = new MockRaftActorContext.MockReplicatedLogEntry(1, 5,
                        new MockRaftActorContext.MockPayload("F"));

                mockRaftActor.onReceiveCommand(new ApplyState(mockActorRef, "apply-state", entry));

                verify(mockRaftActor.delegate).applyState(eq(mockActorRef), eq("apply-state"), anyObject());

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testApplySnapshot() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testApplySnapshot";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProviderMonitor dataPersistenceProviderMonitor = new DataPersistenceProviderMonitor();

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProviderMonitor), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                ReplicatedLog oldReplicatedLog = mockRaftActor.getReplicatedLog();

                oldReplicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,0,mock(Payload.class)));
                oldReplicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1,1,mock(Payload.class)));
                oldReplicatedLog.append(
                    new MockRaftActorContext.MockReplicatedLogEntry(1, 2,
                        mock(Payload.class)));

                ByteString snapshotBytes = fromObject(Arrays.asList(
                    new MockRaftActorContext.MockPayload("A"),
                    new MockRaftActorContext.MockPayload("B"),
                    new MockRaftActorContext.MockPayload("C"),
                    new MockRaftActorContext.MockPayload("D")));

                Snapshot snapshot = mock(Snapshot.class);

                doReturn(snapshotBytes.toByteArray()).when(snapshot).getState();

                doReturn(3L).when(snapshot).getLastAppliedIndex();

                mockRaftActor.onReceiveCommand(new ApplySnapshot(snapshot));

                verify(mockRaftActor.delegate).applySnapshot(eq(snapshot.getState()));

                assertTrue("The replicatedLog should have changed",
                    oldReplicatedLog != mockRaftActor.getReplicatedLog());

                assertEquals("lastApplied should be same as in the snapshot",
                    (Long) 3L, mockRaftActor.getLastApplied());

                assertEquals(0, mockRaftActor.getReplicatedLog().size());

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testSaveSnapshotFailure() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "testSaveSnapshotFailure";

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProviderMonitor dataPersistenceProviderMonitor = new DataPersistenceProviderMonitor();

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(persistenceId,
                        Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProviderMonitor), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                ByteString snapshotBytes  = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                RaftActorContext raftActorContext = mockRaftActor.getRaftActorContext();

                mockRaftActor.setCurrentBehavior(new Leader(raftActorContext));

                mockRaftActor.onReceiveCommand(new CaptureSnapshot(-1,1,-1,1));

                mockRaftActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));

                mockRaftActor.onReceiveCommand(new SaveSnapshotFailure(new SnapshotMetadata("foobar", 10L, 1234L),
                        new Exception()));

                assertEquals("Snapshot index should not have advanced because save snapshot failed", -1,
                        mockRaftActor.getReplicatedLog().getSnapshotIndex());

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testRaftRoleChangeNotifier() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef notifierActor = getSystem().actorOf(Props.create(MessageCollectorActor.class));
            DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
            String id = "testRaftRoleChangeNotifier";

            TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(), MockRaftActor.props(id,
                Collections.<String,String>emptyMap(), Optional.<ConfigParams>of(config), notifierActor), id);

            // sleeping for a minimum of 2 seconds, if it spans more its fine.
            Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

            List<Object> matches =  MessageCollectorActor.getAllMatching(notifierActor, RoleChanged.class);
            assertNotNull(matches);
            assertEquals(3, matches.size());

            // check if the notifier got a role change from null to Follower
            RoleChanged raftRoleChanged = (RoleChanged) matches.get(0);
            assertEquals(id, raftRoleChanged.getMemberId());
            assertNull(raftRoleChanged.getOldRole());
            assertEquals(RaftState.Follower.name(), raftRoleChanged.getNewRole());

            // check if the notifier got a role change from Follower to Candidate
            raftRoleChanged = (RoleChanged) matches.get(1);
            assertEquals(id, raftRoleChanged.getMemberId());
            assertEquals(RaftState.Follower.name(), raftRoleChanged.getOldRole());
            assertEquals(RaftState.Candidate.name(), raftRoleChanged.getNewRole());

            // check if the notifier got a role change from Candidate to Leader
            raftRoleChanged = (RoleChanged) matches.get(2);
            assertEquals(id, raftRoleChanged.getMemberId());
            assertEquals(RaftState.Candidate.name(), raftRoleChanged.getOldRole());
            assertEquals(RaftState.Leader.name(), raftRoleChanged.getNewRole());
        }};
    }

    @Test
    public void testFakeSnapshotsForLeaderWithInRealSnapshots() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "leader1";

                ActorRef followerActor1 =
                        getSystem().actorOf(Props.create(MessageCollectorActor.class));

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
                config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                Map<String, String> peerAddresses = new HashMap<>();
                peerAddresses.put("follower-1", followerActor1.path().toString());

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(),
                        MockRaftActor.props(persistenceId, peerAddresses,
                                Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor leaderActor = mockActorRef.underlyingActor();
                leaderActor.getRaftActorContext().setCommitIndex(4);
                leaderActor.getRaftActorContext().setLastApplied(4);
                leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

                //sleep for recovery to get completed and for the behaviour to be set as Follower
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

                // create 8 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot

                Leader leader = new Leader(leaderActor.getRaftActorContext());
                leaderActor.setCurrentBehavior(leader);
                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

                MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
                leaderActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(0, 8, 1).build());

                assertEquals(8, leaderActor.getReplicatedLog().size());

                leaderActor.onReceiveCommand(new CaptureSnapshot(6,1,4,1));
                leaderActor.getRaftActorContext().setSnapshotCaptureInitiated(true);
                verify(leaderActor.delegate).createSnapshot();

                assertEquals(8, leaderActor.getReplicatedLog().size());

                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
                //fake snapshot on index 5
                leaderActor.onReceiveCommand(new AppendEntriesReply("follower-1", 1, true, 5, 1));

                assertEquals(8, leaderActor.getReplicatedLog().size());

                //fake snapshot on index 6
                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
                leaderActor.onReceiveCommand(new AppendEntriesReply("follower-1", 1, true, 6, 1));
                assertEquals(8, leaderActor.getReplicatedLog().size());

                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

                assertEquals(8, leaderActor.getReplicatedLog().size());

                ByteString snapshotBytes  = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("foo-0"),
                        new MockRaftActorContext.MockPayload("foo-1"),
                        new MockRaftActorContext.MockPayload("foo-2"),
                        new MockRaftActorContext.MockPayload("foo-3"),
                        new MockRaftActorContext.MockPayload("foo-4")));
                leaderActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));
                assertFalse(leaderActor.getRaftActorContext().isSnapshotCaptureInitiated());

                // capture snapshot reply should remove the snapshotted entries only
                assertEquals(3, leaderActor.getReplicatedLog().size());
                assertEquals(7, leaderActor.getReplicatedLog().lastIndex());

                // add another non-replicated entry
                leaderActor.getReplicatedLog().append(
                        new ReplicatedLogImplEntry(8, 1, new MockRaftActorContext.MockPayload("foo-8")));

                //fake snapshot on index 7, since lastApplied = 7 , we would keep the last applied
                leaderActor.onReceiveCommand(new AppendEntriesReply("follower-1", 1, true, 7, 1));
                assertEquals(2, leaderActor.getReplicatedLog().size());
                assertEquals(8, leaderActor.getReplicatedLog().lastIndex());

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
    }

    @Test
    public void testFakeSnapshotsForFollowerWithInRealSnapshots() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = "follower1";

                ActorRef leaderActor1 =
                        getSystem().actorOf(Props.create(MessageCollectorActor.class));

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
                config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                Map<String, String> peerAddresses = new HashMap<>();
                peerAddresses.put("leader", leaderActor1.path().toString());

                TestActorRef<MockRaftActor> mockActorRef = TestActorRef.create(getSystem(),
                        MockRaftActor.props(persistenceId, peerAddresses,
                                Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor followerActor = mockActorRef.underlyingActor();
                followerActor.getRaftActorContext().setCommitIndex(4);
                followerActor.getRaftActorContext().setLastApplied(4);
                followerActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

                //sleep for recovery to get completed and for the behaviour to be set as Follower
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

                // create 8 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot
                Follower follower = new Follower(followerActor.getRaftActorContext());
                followerActor.setCurrentBehavior(follower);
                assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());

                MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
                followerActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(0, 6, 1).build());

                // log as indices 0-5
                assertEquals(6, followerActor.getReplicatedLog().size());

                //snapshot on 4
                followerActor.onReceiveCommand(new CaptureSnapshot(5,1,4,1));
                followerActor.getRaftActorContext().setSnapshotCaptureInitiated(true);
                verify(followerActor.delegate).createSnapshot();

                assertEquals(6, followerActor.getReplicatedLog().size());

                //fake snapshot on index 6
                List<ReplicatedLogEntry> entries =
                        Arrays.asList(
                                (ReplicatedLogEntry) new MockRaftActorContext.MockReplicatedLogEntry(1, 6,
                                        new MockRaftActorContext.MockPayload("foo-6"))
                        );
                followerActor.onReceiveCommand(new AppendEntries(1, "leader", 5, 1, entries , 5, 5));
                assertEquals(7, followerActor.getReplicatedLog().size());

                //fake snapshot on index 7
                assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());

                entries =
                        Arrays.asList(
                                (ReplicatedLogEntry) new MockRaftActorContext.MockReplicatedLogEntry(1, 7,
                                        new MockRaftActorContext.MockPayload("foo-7"))
                        );
                followerActor.onReceiveCommand(new AppendEntries(1, "leader", 6, 1, entries, 6, 6));
                assertEquals(8, followerActor.getReplicatedLog().size());

                assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());


                ByteString snapshotBytes  = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("foo-0"),
                        new MockRaftActorContext.MockPayload("foo-1"),
                        new MockRaftActorContext.MockPayload("foo-2"),
                        new MockRaftActorContext.MockPayload("foo-3"),
                        new MockRaftActorContext.MockPayload("foo-4")));
                followerActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));
                assertFalse(followerActor.getRaftActorContext().isSnapshotCaptureInitiated());

                // capture snapshot reply should remove the snapshotted entries only
                assertEquals(3, followerActor.getReplicatedLog().size()); //indexes 5,6,7 left in the log
                assertEquals(7, followerActor.getReplicatedLog().lastIndex());

                entries =
                        Arrays.asList(
                                (ReplicatedLogEntry) new MockRaftActorContext.MockReplicatedLogEntry(1, 8,
                                        new MockRaftActorContext.MockPayload("foo-7"))
                        );
                // send an additional entry 8 with leaderCommit = 7
                followerActor.onReceiveCommand(new AppendEntries(1, "leader", 7, 1, entries , 7, 7));

                // 7 and 8, as lastapplied is 7
                assertEquals(2, followerActor.getReplicatedLog().size());

                mockActorRef.tell(PoisonPill.getInstance(), getRef());

            }
        };
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
