package org.opendaylight.controller.cluster.raft;

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
import com.google.common.collect.ImmutableMap;
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
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.datastore.DataPersistenceProviderMonitor;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.RaftActor.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class RaftActorTest extends AbstractActorTest {

    private TestActorFactory factory;

    @Before
    public void setUp(){
        factory = new TestActorFactory(getSystem());
    }

    @After
    public void tearDown() throws Exception {
        factory.close();
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    public static class MockRaftActor extends RaftActor implements RaftActorRecoveryCohort, RaftActorSnapshotCohort {

        private final RaftActor actorDelegate;
        private final RaftActorRecoveryCohort recoveryCohortDelegate;
        private final RaftActorSnapshotCohort snapshotCohortDelegate;
        private final CountDownLatch recoveryComplete = new CountDownLatch(1);
        private final List<Object> state;
        private ActorRef roleChangeNotifier;
        private final CountDownLatch initializeBehaviorComplete = new CountDownLatch(1);

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

        public MockRaftActor(String id, Map<String, String> peerAddresses, Optional<ConfigParams> config,
                             DataPersistenceProvider dataPersistenceProvider) {
            super(id, peerAddresses, config);
            state = new ArrayList<>();
            this.actorDelegate = mock(RaftActor.class);
            this.recoveryCohortDelegate = mock(RaftActorRecoveryCohort.class);
            this.snapshotCohortDelegate = mock(RaftActorSnapshotCohort.class);
            if(dataPersistenceProvider == null){
                setPersistence(true);
            } else {
                setPersistence(dataPersistenceProvider);
            }
        }

        public void waitForRecoveryComplete() {
            try {
                assertEquals("Recovery complete", true, recoveryComplete.await(5,  TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void waitForInitializeBehaviorComplete() {
            try {
                assertEquals("Behavior initialized", true, initializeBehaviorComplete.await(5,  TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        public void waitUntilLeader(){
            for(int i = 0;i < 10; i++){
                if(isLeader()){
                    break;
                }
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
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

        public static Props props(final String id, final Map<String, String> peerAddresses,
                                  Optional<ConfigParams> config, ActorRef roleChangeNotifier,
                                  DataPersistenceProvider dataPersistenceProvider){
            return Props.create(new MockRaftActorCreator(peerAddresses, id, config, dataPersistenceProvider, roleChangeNotifier));
        }


        @Override protected void applyState(ActorRef clientActor, String identifier, Object data) {
            actorDelegate.applyState(clientActor, identifier, data);
            LOG.info("{}: applyState called", persistenceId());
        }

        @Override
        @Nonnull
        protected RaftActorRecoveryCohort getRaftActorRecoveryCohort() {
            return this;
        }

        @Override
        protected RaftActorSnapshotCohort getRaftActorSnapshotCohort() {
            return this;
        }

        @Override
        public void startLogRecoveryBatch(int maxBatchSize) {
        }

        @Override
        public void appendRecoveredLogEntry(Payload data) {
            state.add(data);
        }

        @Override
        public void applyCurrentLogRecoveryBatch() {
        }

        @Override
        protected void onRecoveryComplete() {
            actorDelegate.onRecoveryComplete();
            recoveryComplete.countDown();
        }

        @Override
        protected void initializeBehavior() {
            super.initializeBehavior();
            initializeBehaviorComplete.countDown();
        }

        @Override
        public void applyRecoverySnapshot(byte[] bytes) {
            recoveryCohortDelegate.applyRecoverySnapshot(bytes);
            try {
                Object data = toObject(bytes);
                if (data instanceof List) {
                    state.addAll((List<?>) data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void createSnapshot(ActorRef actorRef) {
            LOG.info("{}: createSnapshot called", persistenceId());
            snapshotCohortDelegate.createSnapshot(actorRef);
        }

        @Override
        public void applySnapshot(byte [] snapshot) {
            LOG.info("{}: applySnapshot called", persistenceId());
            snapshotCohortDelegate.applySnapshot(snapshot);
        }

        @Override
        protected void onStateChanged() {
            actorDelegate.onStateChanged();
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


    public static class RaftActorTestKit extends JavaTestKit {
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

        public static void waitUntilLeader(ActorRef actorRef) {
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
            String persistenceId = factory.generateActorId("follower-");

            DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
            // Set the heartbeat interval high to essentially disable election otherwise the test
            // may fail if the actor is switched to Leader and the commitIndex is set to the last
            // log entry.
            config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

            ActorRef followerActor = factory.createActor(MockRaftActor.props(persistenceId,
                    Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config)), persistenceId);

            watch(followerActor);

            List<ReplicatedLogEntry> snapshotUnappliedEntries = new ArrayList<>();
            ReplicatedLogEntry entry1 = new MockRaftActorContext.MockReplicatedLogEntry(1, 4,
                    new MockRaftActorContext.MockPayload("E"));
            snapshotUnappliedEntries.add(entry1);

            int lastAppliedDuringSnapshotCapture = 3;
            int lastIndexDuringSnapshotCapture = 4;

            // 4 messages as part of snapshot, which are applied to state
            ByteString snapshotBytes = fromObject(Arrays.asList(
                    new MockRaftActorContext.MockPayload("A"),
                    new MockRaftActorContext.MockPayload("B"),
                    new MockRaftActorContext.MockPayload("C"),
                    new MockRaftActorContext.MockPayload("D")));

            Snapshot snapshot = Snapshot.create(snapshotBytes.toByteArray(),
                    snapshotUnappliedEntries, lastIndexDuringSnapshotCapture, 1,
                    lastAppliedDuringSnapshotCapture, 1);
            InMemorySnapshotStore.addSnapshot(persistenceId, snapshot);

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

            InMemoryJournal.addEntry(persistenceId, 5, entry2);
            // 2 entries are applied to state besides the 4 entries in snapshot
            InMemoryJournal.addEntry(persistenceId, 6, new ApplyJournalEntries(lastAppliedToState));
            InMemoryJournal.addEntry(persistenceId, 7, entry3);
            InMemoryJournal.addEntry(persistenceId, 8, entry4);

            // kill the actor
            followerActor.tell(PoisonPill.getInstance(), null);
            expectMsgClass(duration("5 seconds"), Terminated.class);

            unwatch(followerActor);

            //reinstate the actor
            TestActorRef<MockRaftActor> ref = factory.createTestActor(
                    MockRaftActor.props(persistenceId, Collections.<String, String>emptyMap(),
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

    @Test
    public void testRaftActorRecoveryWithPreLithuimApplyLogEntries() throws Exception {
        new JavaTestKit(getSystem()) {{
            String persistenceId = factory.generateActorId("leader-");

            DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
            config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

            // Setup the persisted journal with some entries
            ReplicatedLogEntry entry0 = new MockRaftActorContext.MockReplicatedLogEntry(1, 0,
                    new MockRaftActorContext.MockPayload("zero"));
            ReplicatedLogEntry entry1 = new MockRaftActorContext.MockReplicatedLogEntry(1, 1,
                    new MockRaftActorContext.MockPayload("oen"));
            ReplicatedLogEntry entry2 = new MockRaftActorContext.MockReplicatedLogEntry(1, 2,
                    new MockRaftActorContext.MockPayload("two"));

            long seqNr = 1;
            InMemoryJournal.addEntry(persistenceId, seqNr++, entry0);
            InMemoryJournal.addEntry(persistenceId, seqNr++, entry1);
            InMemoryJournal.addEntry(persistenceId, seqNr++, new ApplyLogEntries(1));
            InMemoryJournal.addEntry(persistenceId, seqNr++, entry2);

            int lastAppliedToState = 1;
            int lastIndex = 2;

            //reinstate the actor
            TestActorRef<MockRaftActor> leaderActor = factory.createTestActor(
                    MockRaftActor.props(persistenceId, Collections.<String, String>emptyMap(),
                            Optional.<ConfigParams>of(config)));

            leaderActor.underlyingActor().waitForRecoveryComplete();

            RaftActorContext context = leaderActor.underlyingActor().getRaftActorContext();
            assertEquals("Journal log size", 3, context.getReplicatedLog().size());
            assertEquals("Last index", lastIndex, context.getReplicatedLog().lastIndex());
            assertEquals("Last applied", lastAppliedToState, context.getLastApplied());
            assertEquals("Commit index", lastAppliedToState, context.getCommitIndex());
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
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config)), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                // Wait for akka's recovery to complete so it doesn't interfere.
                mockRaftActor.waitForRecoveryComplete();

                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                Snapshot snapshot = Snapshot.create(snapshotBytes.toByteArray(),
                        Lists.<ReplicatedLogEntry>newArrayList(), 3, 1, 3, 1);

                mockRaftActor.onReceiveRecover(new SnapshotOffer(new SnapshotMetadata(persistenceId, 100, 100), snapshot));

                verify(mockRaftActor.recoveryCohortDelegate).applyRecoverySnapshot(eq(snapshotBytes.toByteArray()));

                mockRaftActor.onReceiveRecover(new ReplicatedLogImplEntry(0, 1, new MockRaftActorContext.MockPayload("A")));

                ReplicatedLog replicatedLog = mockRaftActor.getReplicatedLog();

                assertEquals("add replicated log entry", 1, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new ReplicatedLogImplEntry(1, 1, new MockRaftActorContext.MockPayload("A")));

                assertEquals("add replicated log entry", 2, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new ApplyJournalEntries(1));

                assertEquals("commit index 1", 1, mockRaftActor.getRaftActorContext().getCommitIndex());

                // The snapshot had 4 items + we added 2 more items during the test
                // We start removing from 5 and we should get 1 item in the replicated log
                mockRaftActor.onReceiveRecover(new RaftActor.DeleteEntries(5));

                assertEquals("remove log entries", 1, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new UpdateElectionTerm(10, "foobar"));

                assertEquals("election term", 10, mockRaftActor.getRaftActorContext().getTermInformation().getCurrentTerm());
                assertEquals("voted for", "foobar", mockRaftActor.getRaftActorContext().getTermInformation().getVotedFor());

                mockRaftActor.onReceiveRecover(mock(RecoveryCompleted.class));

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
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), new DataPersistenceProviderMonitor()), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                // Wait for akka's recovery to complete so it doesn't interfere.
                mockRaftActor.waitForRecoveryComplete();

                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                Snapshot snapshot = Snapshot.create(snapshotBytes.toByteArray(),
                        Lists.<ReplicatedLogEntry>newArrayList(), 3, 1, 3, 1);

                mockRaftActor.onReceiveRecover(new SnapshotOffer(new SnapshotMetadata(persistenceId, 100, 100), snapshot));

                verify(mockRaftActor.recoveryCohortDelegate, times(0)).applyRecoverySnapshot(any(byte[].class));

                mockRaftActor.onReceiveRecover(new ReplicatedLogImplEntry(0, 1, new MockRaftActorContext.MockPayload("A")));

                ReplicatedLog replicatedLog = mockRaftActor.getReplicatedLog();

                assertEquals("add replicated log entry", 0, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new ReplicatedLogImplEntry(1, 1, new MockRaftActorContext.MockPayload("A")));

                assertEquals("add replicated log entry", 0, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new ApplyJournalEntries(1));

                assertEquals("commit index -1", -1, mockRaftActor.getRaftActorContext().getCommitIndex());

                mockRaftActor.onReceiveRecover(new RaftActor.DeleteEntries(2));

                assertEquals("remove log entries", 0, replicatedLog.size());

                mockRaftActor.onReceiveRecover(new UpdateElectionTerm(10, "foobar"));

                assertNotEquals("election term", 10, mockRaftActor.getRaftActorContext().getTermInformation().getCurrentTerm());
                assertNotEquals("voted for", "foobar", mockRaftActor.getRaftActorContext().getTermInformation().getVotedFor());

                mockRaftActor.onReceiveRecover(mock(RecoveryCompleted.class));
            }};
    }


    @Test
    public void testUpdatingElectionTermCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                CountDownLatch persistLatch = new CountDownLatch(1);
                DataPersistenceProviderMonitor dataPersistenceProviderMonitor = new DataPersistenceProviderMonitor();
                dataPersistenceProviderMonitor.setPersistLatch(persistLatch);

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProviderMonitor), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();

                mockRaftActor.getRaftActorContext().getTermInformation().updateAndPersist(10, "foobar");

                assertEquals("Persist called", true, persistLatch.await(5, TimeUnit.SECONDS));
            }
        };
    }

    @Test
    public void testAddingReplicatedLogEntryCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();

                MockRaftActorContext.MockReplicatedLogEntry logEntry = new MockRaftActorContext.MockReplicatedLogEntry(10, 10, mock(Payload.class));

                mockRaftActor.getRaftActorContext().getReplicatedLog().appendAndPersist(logEntry);

                verify(dataPersistenceProvider).persist(eq(logEntry), any(Procedure.class));
            }
        };
    }

    @Test
    public void testRemovingReplicatedLogEntryCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();

                mockRaftActor.waitUntilLeader();

                mockRaftActor.getReplicatedLog().appendAndPersist(new MockRaftActorContext.MockReplicatedLogEntry(1, 0, mock(Payload.class)));

                mockRaftActor.getRaftActorContext().getReplicatedLog().removeFromAndPersist(0);

                verify(dataPersistenceProvider, times(3)).persist(anyObject(), any(Procedure.class));
            }
        };
    }

    @Test
    public void testApplyJournalEntriesCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();

                mockRaftActor.waitUntilLeader();

                mockRaftActor.onReceiveCommand(new ApplyJournalEntries(10));

                verify(dataPersistenceProvider, times(2)).persist(anyObject(), any(Procedure.class));

            }

        };
    }

    @Test
    public void testCaptureSnapshotReplyCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                        MockRaftActor.props(persistenceId, Collections.<String, String>emptyMap(),
                                Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();

                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                RaftActorContext raftActorContext = mockRaftActor.getRaftActorContext();

                raftActorContext.getSnapshotManager().capture(
                        new MockRaftActorContext.MockReplicatedLogEntry(1, -1,
                                new MockRaftActorContext.MockPayload("D")), -1);

                mockRaftActor.setCurrentBehavior(new Leader(raftActorContext));

                mockRaftActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));

                verify(dataPersistenceProvider).saveSnapshot(anyObject());

            }
        };
    }

    @Test
    public void testSaveSnapshotSuccessCallsDataPersistence() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        ImmutableMap.of("leader", "fake/path"), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();
                MockRaftActorContext.MockReplicatedLogEntry lastEntry = new MockRaftActorContext.MockReplicatedLogEntry(1, 4, mock(Payload.class));

                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1, 0, mock(Payload.class)));
                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1, 1, mock(Payload.class)));
                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1, 2, mock(Payload.class)));
                mockRaftActor.getReplicatedLog().append(new MockRaftActorContext.MockReplicatedLogEntry(1, 3, mock(Payload.class)));
                mockRaftActor.getReplicatedLog().append(lastEntry);

                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                RaftActorContext raftActorContext = mockRaftActor.getRaftActorContext();
                mockRaftActor.setCurrentBehavior(new Follower(raftActorContext));

                long replicatedToAllIndex = 1;

                mockRaftActor.getRaftActorContext().getSnapshotManager().capture(lastEntry, replicatedToAllIndex);

                verify(mockRaftActor.snapshotCohortDelegate).createSnapshot(any(ActorRef.class));

                mockRaftActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));

                mockRaftActor.onReceiveCommand(new SaveSnapshotSuccess(new SnapshotMetadata("foo", 100, 100)));

                verify(dataPersistenceProvider).deleteSnapshots(any(SnapshotSelectionCriteria.class));

                verify(dataPersistenceProvider).deleteMessages(100);

                assertEquals(3, mockRaftActor.getReplicatedLog().size());
                assertEquals(1, mockRaftActor.getCurrentBehavior().getReplicatedToAllIndex());

                assertNotNull(mockRaftActor.getReplicatedLog().get(2));
                assertNotNull(mockRaftActor.getReplicatedLog().get(3));
                assertNotNull(mockRaftActor.getReplicatedLog().get(4));

                // Index 2 will not be in the log because it was removed due to snapshotting
                assertNull(mockRaftActor.getReplicatedLog().get(1));
                assertNull(mockRaftActor.getReplicatedLog().get(0));

            }
        };
    }

    @Test
    public void testApplyState() throws Exception {

        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();

                ReplicatedLogEntry entry = new MockRaftActorContext.MockReplicatedLogEntry(1, 5,
                        new MockRaftActorContext.MockPayload("F"));

                mockRaftActor.onReceiveCommand(new ApplyState(mockActorRef, "apply-state", entry));

                verify(mockRaftActor.actorDelegate).applyState(eq(mockActorRef), eq("apply-state"), anyObject());

            }
        };
    }

    @Test
    public void testApplySnapshot() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProviderMonitor dataPersistenceProviderMonitor = new DataPersistenceProviderMonitor();

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProviderMonitor), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();

                ReplicatedLog oldReplicatedLog = mockRaftActor.getReplicatedLog();

                oldReplicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1, 0, mock(Payload.class)));
                oldReplicatedLog.append(new MockRaftActorContext.MockReplicatedLogEntry(1, 1, mock(Payload.class)));
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

                verify(mockRaftActor.snapshotCohortDelegate).applySnapshot(eq(snapshot.getState()));

                assertTrue("The replicatedLog should have changed",
                        oldReplicatedLog != mockRaftActor.getReplicatedLog());

                assertEquals("lastApplied should be same as in the snapshot",
                        (Long) 3L, mockRaftActor.getLastApplied());

                assertEquals(0, mockRaftActor.getReplicatedLog().size());

            }
        };
    }

    @Test
    public void testSaveSnapshotFailure() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();

                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProviderMonitor dataPersistenceProviderMonitor = new DataPersistenceProviderMonitor();

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                        Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), dataPersistenceProviderMonitor), persistenceId);

                MockRaftActor mockRaftActor = mockActorRef.underlyingActor();

                mockRaftActor.waitForInitializeBehaviorComplete();

                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("A"),
                        new MockRaftActorContext.MockPayload("B"),
                        new MockRaftActorContext.MockPayload("C"),
                        new MockRaftActorContext.MockPayload("D")));

                RaftActorContext raftActorContext = mockRaftActor.getRaftActorContext();

                mockRaftActor.setCurrentBehavior(new Leader(raftActorContext));

                raftActorContext.getSnapshotManager().capture(
                        new MockRaftActorContext.MockReplicatedLogEntry(1, 1,
                                new MockRaftActorContext.MockPayload("D")), 1);

                mockRaftActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));

                mockRaftActor.onReceiveCommand(new SaveSnapshotFailure(new SnapshotMetadata("foobar", 10L, 1234L),
                        new Exception()));

                assertEquals("Snapshot index should not have advanced because save snapshot failed", -1,
                        mockRaftActor.getReplicatedLog().getSnapshotIndex());

            }
        };
    }

    @Test
    public void testRaftRoleChangeNotifierWhenRaftActorHasNoPeers() throws Exception {
        new JavaTestKit(getSystem()) {{
            TestActorRef<MessageCollectorActor> notifierActor = factory.createTestActor(
                    Props.create(MessageCollectorActor.class));
            MessageCollectorActor.waitUntilReady(notifierActor);

            DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
            long heartBeatInterval = 100;
            config.setHeartBeatInterval(FiniteDuration.create(heartBeatInterval, TimeUnit.MILLISECONDS));
            config.setElectionTimeoutFactor(20);

            String persistenceId = factory.generateActorId("notifier-");

            TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.props(persistenceId,
                    Collections.<String, String>emptyMap(), Optional.<ConfigParams>of(config), notifierActor,
                    new NonPersistentDataProvider()), persistenceId);

            List<RoleChanged> matches =  MessageCollectorActor.expectMatching(notifierActor, RoleChanged.class, 3);


            // check if the notifier got a role change from null to Follower
            RoleChanged raftRoleChanged = matches.get(0);
            assertEquals(persistenceId, raftRoleChanged.getMemberId());
            assertNull(raftRoleChanged.getOldRole());
            assertEquals(RaftState.Follower.name(), raftRoleChanged.getNewRole());

            // check if the notifier got a role change from Follower to Candidate
            raftRoleChanged = matches.get(1);
            assertEquals(persistenceId, raftRoleChanged.getMemberId());
            assertEquals(RaftState.Follower.name(), raftRoleChanged.getOldRole());
            assertEquals(RaftState.Candidate.name(), raftRoleChanged.getNewRole());

            // check if the notifier got a role change from Candidate to Leader
            raftRoleChanged = matches.get(2);
            assertEquals(persistenceId, raftRoleChanged.getMemberId());
            assertEquals(RaftState.Candidate.name(), raftRoleChanged.getOldRole());
            assertEquals(RaftState.Leader.name(), raftRoleChanged.getNewRole());

            LeaderStateChanged leaderStateChange = MessageCollectorActor.expectFirstMatching(
                    notifierActor, LeaderStateChanged.class);

            assertEquals(raftRoleChanged.getMemberId(), leaderStateChange.getLeaderId());

            notifierActor.underlyingActor().clear();

            MockRaftActor raftActor = raftActorRef.underlyingActor();
            final String newLeaderId = "new-leader";
            Follower follower = new Follower(raftActor.getRaftActorContext()) {
                @Override
                public RaftActorBehavior handleMessage(ActorRef sender, Object message) {
                    leaderId = newLeaderId;
                    return this;
                }
            };

            raftActor.changeCurrentBehavior(follower);

            leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
            assertEquals(persistenceId, leaderStateChange.getMemberId());
            assertEquals(null, leaderStateChange.getLeaderId());

            raftRoleChanged = MessageCollectorActor.expectFirstMatching(notifierActor, RoleChanged.class);
            assertEquals(RaftState.Leader.name(), raftRoleChanged.getOldRole());
            assertEquals(RaftState.Follower.name(), raftRoleChanged.getNewRole());

            notifierActor.underlyingActor().clear();

            raftActor.handleCommand("any");

            leaderStateChange = MessageCollectorActor.expectFirstMatching(notifierActor, LeaderStateChanged.class);
            assertEquals(persistenceId, leaderStateChange.getMemberId());
            assertEquals(newLeaderId, leaderStateChange.getLeaderId());
        }};
    }

    @Test
    public void testRaftRoleChangeNotifierWhenRaftActorHasPeers() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef notifierActor = factory.createActor(Props.create(MessageCollectorActor.class));
            MessageCollectorActor.waitUntilReady(notifierActor);

            DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
            long heartBeatInterval = 100;
            config.setHeartBeatInterval(FiniteDuration.create(heartBeatInterval, TimeUnit.MILLISECONDS));
            config.setElectionTimeoutFactor(1);

            String persistenceId = factory.generateActorId("notifier-");

            factory.createActor(MockRaftActor.props(persistenceId,
                    ImmutableMap.of("leader", "fake/path"), Optional.<ConfigParams>of(config), notifierActor), persistenceId);

            List<RoleChanged> matches =  null;
            for(int i = 0; i < 5000 / heartBeatInterval; i++) {
                matches = MessageCollectorActor.getAllMatching(notifierActor, RoleChanged.class);
                assertNotNull(matches);
                if(matches.size() == 3) {
                    break;
                }
                Uninterruptibles.sleepUninterruptibly(heartBeatInterval, TimeUnit.MILLISECONDS);
            }

            assertEquals(2, matches.size());

            // check if the notifier got a role change from null to Follower
            RoleChanged raftRoleChanged = matches.get(0);
            assertEquals(persistenceId, raftRoleChanged.getMemberId());
            assertNull(raftRoleChanged.getOldRole());
            assertEquals(RaftState.Follower.name(), raftRoleChanged.getNewRole());

            // check if the notifier got a role change from Follower to Candidate
            raftRoleChanged = matches.get(1);
            assertEquals(persistenceId, raftRoleChanged.getMemberId());
            assertEquals(RaftState.Follower.name(), raftRoleChanged.getOldRole());
            assertEquals(RaftState.Candidate.name(), raftRoleChanged.getNewRole());

        }};
    }

    @Test
    public void testFakeSnapshotsForLeaderWithInRealSnapshots() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");
                String follower1Id = factory.generateActorId("follower-");

                ActorRef followerActor1 =
                        factory.createActor(Props.create(MessageCollectorActor.class));

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
                config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                Map<String, String> peerAddresses = new HashMap<>();
                peerAddresses.put(follower1Id, followerActor1.path().toString());

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                        MockRaftActor.props(persistenceId, peerAddresses,
                                Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor leaderActor = mockActorRef.underlyingActor();

                leaderActor.getRaftActorContext().setCommitIndex(4);
                leaderActor.getRaftActorContext().setLastApplied(4);
                leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

                leaderActor.waitForInitializeBehaviorComplete();

                // create 8 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot

                Leader leader = new Leader(leaderActor.getRaftActorContext());
                leaderActor.setCurrentBehavior(leader);
                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

                MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
                leaderActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(0, 8, 1).build());

                assertEquals(8, leaderActor.getReplicatedLog().size());

                leaderActor.getRaftActorContext().getSnapshotManager()
                        .capture(new MockRaftActorContext.MockReplicatedLogEntry(1, 6,
                                new MockRaftActorContext.MockPayload("x")), 4);

                verify(leaderActor.snapshotCohortDelegate).createSnapshot(any(ActorRef.class));

                assertEquals(8, leaderActor.getReplicatedLog().size());

                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
                //fake snapshot on index 5
                leaderActor.onReceiveCommand(new AppendEntriesReply(follower1Id, 1, true, 5, 1));

                assertEquals(8, leaderActor.getReplicatedLog().size());

                //fake snapshot on index 6
                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());
                leaderActor.onReceiveCommand(new AppendEntriesReply(follower1Id, 1, true, 6, 1));
                assertEquals(8, leaderActor.getReplicatedLog().size());

                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

                assertEquals(8, leaderActor.getReplicatedLog().size());

                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("foo-0"),
                        new MockRaftActorContext.MockPayload("foo-1"),
                        new MockRaftActorContext.MockPayload("foo-2"),
                        new MockRaftActorContext.MockPayload("foo-3"),
                        new MockRaftActorContext.MockPayload("foo-4")));

                leaderActor.getRaftActorContext().getSnapshotManager().persist(new NonPersistentDataProvider()
                        , snapshotBytes.toByteArray(), leader, Runtime.getRuntime().totalMemory());

                assertFalse(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());

                // The commit is needed to complete the snapshot creation process
                leaderActor.getRaftActorContext().getSnapshotManager().commit(new NonPersistentDataProvider(), -1);

                // capture snapshot reply should remove the snapshotted entries only
                assertEquals(3, leaderActor.getReplicatedLog().size());
                assertEquals(7, leaderActor.getReplicatedLog().lastIndex());

                // add another non-replicated entry
                leaderActor.getReplicatedLog().append(
                        new ReplicatedLogImplEntry(8, 1, new MockRaftActorContext.MockPayload("foo-8")));

                //fake snapshot on index 7, since lastApplied = 7 , we would keep the last applied
                leaderActor.onReceiveCommand(new AppendEntriesReply(follower1Id, 1, true, 7, 1));
                assertEquals(2, leaderActor.getReplicatedLog().size());
                assertEquals(8, leaderActor.getReplicatedLog().lastIndex());

            }
        };
    }

    @Test
    public void testFakeSnapshotsForFollowerWithInRealSnapshots() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("follower-");
                String leaderId = factory.generateActorId("leader-");


                ActorRef leaderActor1 =
                        factory.createActor(Props.create(MessageCollectorActor.class));

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
                config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                Map<String, String> peerAddresses = new HashMap<>();
                peerAddresses.put(leaderId, leaderActor1.path().toString());

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                        MockRaftActor.props(persistenceId, peerAddresses,
                                Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor followerActor = mockActorRef.underlyingActor();
                followerActor.getRaftActorContext().setCommitIndex(4);
                followerActor.getRaftActorContext().setLastApplied(4);
                followerActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

                followerActor.waitForInitializeBehaviorComplete();


                Follower follower = new Follower(followerActor.getRaftActorContext());
                followerActor.setCurrentBehavior(follower);
                assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());

                // create 6 entries in the log - 0 to 4 are applied and will get picked up as part of the capture snapshot
                MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
                followerActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(0, 6, 1).build());

                // log has indices 0-5
                assertEquals(6, followerActor.getReplicatedLog().size());

                //snapshot on 4
                followerActor.getRaftActorContext().getSnapshotManager().capture(
                        new MockRaftActorContext.MockReplicatedLogEntry(1, 5,
                                new MockRaftActorContext.MockPayload("D")), 4);

                verify(followerActor.snapshotCohortDelegate).createSnapshot(any(ActorRef.class));

                assertEquals(6, followerActor.getReplicatedLog().size());

                //fake snapshot on index 6
                List<ReplicatedLogEntry> entries =
                        Arrays.asList(
                                (ReplicatedLogEntry) new MockRaftActorContext.MockReplicatedLogEntry(1, 6,
                                        new MockRaftActorContext.MockPayload("foo-6"))
                        );
                followerActor.onReceiveCommand(new AppendEntries(1, leaderId, 5, 1, entries, 5, 5));
                assertEquals(7, followerActor.getReplicatedLog().size());

                //fake snapshot on index 7
                assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());

                entries =
                        Arrays.asList(
                                (ReplicatedLogEntry) new MockRaftActorContext.MockReplicatedLogEntry(1, 7,
                                        new MockRaftActorContext.MockPayload("foo-7"))
                        );
                followerActor.onReceiveCommand(new AppendEntries(1, leaderId, 6, 1, entries, 6, 6));
                assertEquals(8, followerActor.getReplicatedLog().size());

                assertEquals(RaftState.Follower, followerActor.getCurrentBehavior().state());


                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("foo-0"),
                        new MockRaftActorContext.MockPayload("foo-1"),
                        new MockRaftActorContext.MockPayload("foo-2"),
                        new MockRaftActorContext.MockPayload("foo-3"),
                        new MockRaftActorContext.MockPayload("foo-4")));
                followerActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));
                assertFalse(followerActor.getRaftActorContext().getSnapshotManager().isCapturing());

                // The commit is needed to complete the snapshot creation process
                followerActor.getRaftActorContext().getSnapshotManager().commit(new NonPersistentDataProvider(), -1);

                // capture snapshot reply should remove the snapshotted entries only till replicatedToAllIndex
                assertEquals(3, followerActor.getReplicatedLog().size()); //indexes 5,6,7 left in the log
                assertEquals(7, followerActor.getReplicatedLog().lastIndex());

                entries =
                        Arrays.asList(
                                (ReplicatedLogEntry) new MockRaftActorContext.MockReplicatedLogEntry(1, 8,
                                        new MockRaftActorContext.MockPayload("foo-7"))
                        );
                // send an additional entry 8 with leaderCommit = 7
                followerActor.onReceiveCommand(new AppendEntries(1, leaderId, 7, 1, entries, 7, 7));

                // 7 and 8, as lastapplied is 7
                assertEquals(2, followerActor.getReplicatedLog().size());

            }
        };
    }

    @Test
    public void testFakeSnapshotsForLeaderWithInInitiateSnapshots() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String persistenceId = factory.generateActorId("leader-");
                String follower1Id = factory.generateActorId("follower-");
                String follower2Id = factory.generateActorId("follower-");

                ActorRef followerActor1 =
                        factory.createActor(Props.create(MessageCollectorActor.class), follower1Id);
                ActorRef followerActor2 =
                        factory.createActor(Props.create(MessageCollectorActor.class), follower2Id);

                DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
                config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
                config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));

                DataPersistenceProvider dataPersistenceProvider = mock(DataPersistenceProvider.class);

                Map<String, String> peerAddresses = new HashMap<>();
                peerAddresses.put(follower1Id, followerActor1.path().toString());
                peerAddresses.put(follower2Id, followerActor2.path().toString());

                TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                        MockRaftActor.props(persistenceId, peerAddresses,
                                Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

                MockRaftActor leaderActor = mockActorRef.underlyingActor();
                leaderActor.getRaftActorContext().setCommitIndex(9);
                leaderActor.getRaftActorContext().setLastApplied(9);
                leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

                leaderActor.waitForInitializeBehaviorComplete();

                Leader leader = new Leader(leaderActor.getRaftActorContext());
                leaderActor.setCurrentBehavior(leader);
                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

                // create 5 entries in the log
                MockRaftActorContext.MockReplicatedLogBuilder logBuilder = new MockRaftActorContext.MockReplicatedLogBuilder();
                leaderActor.getRaftActorContext().setReplicatedLog(logBuilder.createEntries(5, 10, 1).build());

                //set the snapshot index to 4 , 0 to 4 are snapshotted
                leaderActor.getRaftActorContext().getReplicatedLog().setSnapshotIndex(4);
                //setting replicatedToAllIndex = 9, for the log to clear
                leader.setReplicatedToAllIndex(9);
                assertEquals(5, leaderActor.getReplicatedLog().size());
                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

                leaderActor.onReceiveCommand(new AppendEntriesReply(follower1Id, 1, true, 9, 1));
                assertEquals(5, leaderActor.getReplicatedLog().size());
                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

                // set the 2nd follower nextIndex to 1 which has been snapshotted
                leaderActor.onReceiveCommand(new AppendEntriesReply(follower2Id, 1, true, 0, 1));
                assertEquals(5, leaderActor.getReplicatedLog().size());
                assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

                // simulate a real snapshot
                leaderActor.onReceiveCommand(new SendHeartBeat());
                assertEquals(5, leaderActor.getReplicatedLog().size());
                assertEquals(String.format("expected to be Leader but was %s. Current Leader = %s ",
                        leaderActor.getCurrentBehavior().state(), leaderActor.getLeaderId())
                        , RaftState.Leader, leaderActor.getCurrentBehavior().state());


                //reply from a slow follower does not initiate a fake snapshot
                leaderActor.onReceiveCommand(new AppendEntriesReply(follower2Id, 1, true, 9, 1));
                assertEquals("Fake snapshot should not happen when Initiate is in progress", 5, leaderActor.getReplicatedLog().size());

                ByteString snapshotBytes = fromObject(Arrays.asList(
                        new MockRaftActorContext.MockPayload("foo-0"),
                        new MockRaftActorContext.MockPayload("foo-1"),
                        new MockRaftActorContext.MockPayload("foo-2"),
                        new MockRaftActorContext.MockPayload("foo-3"),
                        new MockRaftActorContext.MockPayload("foo-4")));
                leaderActor.onReceiveCommand(new CaptureSnapshotReply(snapshotBytes.toByteArray()));
                assertFalse(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());

                assertEquals("Real snapshot didn't clear the log till replicatedToAllIndex", 0, leaderActor.getReplicatedLog().size());

                //reply from a slow follower after should not raise errors
                leaderActor.onReceiveCommand(new AppendEntriesReply(follower2Id, 1, true, 5, 1));
                assertEquals(0, leaderActor.getReplicatedLog().size());
            }
        };
    }

    @Test
    public void testRealSnapshotWhenReplicatedToAllIndexMinusOne() throws Exception {
        new JavaTestKit(getSystem()) {{
            String persistenceId = factory.generateActorId("leader-");
            DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
            config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
            config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
            config.setSnapshotBatchCount(5);

            DataPersistenceProvider dataPersistenceProvider = new NonPersistentDataProvider();

            Map<String, String> peerAddresses = new HashMap<>();

            TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                    MockRaftActor.props(persistenceId, peerAddresses,
                            Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

            MockRaftActor leaderActor = mockActorRef.underlyingActor();
            leaderActor.getRaftActorContext().setCommitIndex(3);
            leaderActor.getRaftActorContext().setLastApplied(3);
            leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);

            leaderActor.waitForInitializeBehaviorComplete();
            for(int i=0;i< 4;i++) {
                leaderActor.getReplicatedLog()
                        .append(new MockRaftActorContext.MockReplicatedLogEntry(1, i,
                                new MockRaftActorContext.MockPayload("A")));
            }

            Leader leader = new Leader(leaderActor.getRaftActorContext());
            leaderActor.setCurrentBehavior(leader);
            assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

            // Persist another entry (this will cause a CaptureSnapshot to be triggered
            leaderActor.persistData(mockActorRef, "x", new MockRaftActorContext.MockPayload("duh"));

            // Now send a CaptureSnapshotReply
            mockActorRef.tell(new CaptureSnapshotReply(fromObject("foo").toByteArray()), mockActorRef);

            // Trimming log in this scenario is a no-op
            assertEquals(-1, leaderActor.getReplicatedLog().getSnapshotIndex());
            assertFalse(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());
            assertEquals(-1, leader.getReplicatedToAllIndex());

        }};
    }

    @Test
    public void testRealSnapshotWhenReplicatedToAllIndexNotInReplicatedLog() throws Exception {
        new JavaTestKit(getSystem()) {{
            String persistenceId = factory.generateActorId("leader-");
            DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
            config.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
            config.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
            config.setSnapshotBatchCount(5);

            DataPersistenceProvider dataPersistenceProvider = new NonPersistentDataProvider();

            Map<String, String> peerAddresses = new HashMap<>();

            TestActorRef<MockRaftActor> mockActorRef = factory.createTestActor(
                    MockRaftActor.props(persistenceId, peerAddresses,
                            Optional.<ConfigParams>of(config), dataPersistenceProvider), persistenceId);

            MockRaftActor leaderActor = mockActorRef.underlyingActor();
            leaderActor.getRaftActorContext().setCommitIndex(3);
            leaderActor.getRaftActorContext().setLastApplied(3);
            leaderActor.getRaftActorContext().getTermInformation().update(1, persistenceId);
            leaderActor.getReplicatedLog().setSnapshotIndex(3);

            leaderActor.waitForInitializeBehaviorComplete();
            Leader leader = new Leader(leaderActor.getRaftActorContext());
            leaderActor.setCurrentBehavior(leader);
            leader.setReplicatedToAllIndex(3);
            assertEquals(RaftState.Leader, leaderActor.getCurrentBehavior().state());

            // Persist another entry (this will cause a CaptureSnapshot to be triggered
            leaderActor.persistData(mockActorRef, "x", new MockRaftActorContext.MockPayload("duh"));

            // Now send a CaptureSnapshotReply
            mockActorRef.tell(new CaptureSnapshotReply(fromObject("foo").toByteArray()), mockActorRef);

            // Trimming log in this scenario is a no-op
            assertEquals(3, leaderActor.getReplicatedLog().getSnapshotIndex());
            assertFalse(leaderActor.getRaftActorContext().getSnapshotManager().isCapturing());
            assertEquals(3, leader.getReplicatedToAllIndex());

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
