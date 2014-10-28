package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.dispatch.OnComplete;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.datastore.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.datastore.utils.MockDataChangeListener;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;


public class ShardTest extends AbstractActorTest {

    private static final SchemaContext SCHEMA_CONTEXT = TestModel.createTestContext();

    private static final AtomicInteger NEXT_SHARD_NUM = new AtomicInteger();

    private final ShardIdentifier shardID = ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("config" + NEXT_SHARD_NUM.getAndIncrement()).build();

    private DatastoreContext dataStoreContext = DatastoreContext.newBuilder().
            shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).
            shardHeartbeatIntervalInMillis(100).build();

    @Before
    public void setUp() {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
    }

    @After
    public void tearDown() {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
    }

    private Props newShardProps() {
        return Shard.props(shardID, Collections.<ShardIdentifier,String>emptyMap(),
                dataStoreContext, SCHEMA_CONTEXT);
    }

    @Test
    public void testRegisterChangeListener() throws Exception {
        new ShardTestKit(getSystem()) {{
            TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps(),  "testRegisterChangeListener");

            waitUntilLeader(shard);

            shard.tell(new UpdateSchemaContext(SchemaContextHelper.full()), ActorRef.noSender());

            MockDataChangeListener listener = new MockDataChangeListener(1);
            ActorRef dclActor = getSystem().actorOf(DataChangeListener.props(listener),
                    "testRegisterChangeListener-DataChangeListener");

            shard.tell(new RegisterChangeListener(TestModel.TEST_PATH,
                    dclActor.path(), AsyncDataBroker.DataChangeScope.BASE), getRef());

            RegisterChangeListenerReply reply = expectMsgClass(duration("3 seconds"),
                    RegisterChangeListenerReply.class);
            String replyPath = reply.getListenerRegistrationPath().toString();
            assertTrue("Incorrect reply path: " + replyPath, replyPath.matches(
                    "akka:\\/\\/test\\/user\\/testRegisterChangeListener\\/\\$.*"));

            YangInstanceIdentifier path = TestModel.TEST_PATH;
            writeToStore(shard, path, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            listener.waitForChangeEvents(path);

            dclActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @SuppressWarnings("serial")
    @Test
    public void testChangeListenerNotifiedWhenNotTheLeaderOnRegistration() throws Exception {
        // This test tests the timing window in which a change listener is registered before the
        // shard becomes the leader. We verify that the listener is registered and notified of the
        // existing data when the shard becomes the leader.
        new ShardTestKit(getSystem()) {{
            // For this test, we want to send the RegisterChangeListener message after the shard
            // has recovered from persistence and before it becomes the leader. So we subclass
            // Shard to override onReceiveCommand and, when the first ElectionTimeout is received,
            // we know that the shard has been initialized to a follower and has started the
            // election process. The following 2 CountDownLatches are used to coordinate the
            // ElectionTimeout with the sending of the RegisterChangeListener message.
            final CountDownLatch onFirstElectionTimeout = new CountDownLatch(1);
            final CountDownLatch onChangeListenerRegistered = new CountDownLatch(1);
            Creator<Shard> creator = new Creator<Shard>() {
                boolean firstElectionTimeout = true;

                @Override
                public Shard create() throws Exception {
                    return new Shard(shardID, Collections.<ShardIdentifier,String>emptyMap(),
                            dataStoreContext, SCHEMA_CONTEXT) {
                        @Override
                        public void onReceiveCommand(final Object message) throws Exception {
                            if(message instanceof ElectionTimeout && firstElectionTimeout) {
                                // Got the first ElectionTimeout. We don't forward it to the
                                // base Shard yet until we've sent the RegisterChangeListener
                                // message. So we signal the onFirstElectionTimeout latch to tell
                                // the main thread to send the RegisterChangeListener message and
                                // start a thread to wait on the onChangeListenerRegistered latch,
                                // which the main thread signals after it has sent the message.
                                // After the onChangeListenerRegistered is triggered, we send the
                                // original ElectionTimeout message to proceed with the election.
                                firstElectionTimeout = false;
                                final ActorRef self = getSelf();
                                new Thread() {
                                    @Override
                                    public void run() {
                                        Uninterruptibles.awaitUninterruptibly(
                                                onChangeListenerRegistered, 5, TimeUnit.SECONDS);
                                        self.tell(message, self);
                                    }
                                }.start();

                                onFirstElectionTimeout.countDown();
                            } else {
                                super.onReceiveCommand(message);
                            }
                        }
                    };
                }
            };

            MockDataChangeListener listener = new MockDataChangeListener(1);
            ActorRef dclActor = getSystem().actorOf(DataChangeListener.props(listener),
                    "testRegisterChangeListenerWhenNotLeaderInitially-DataChangeListener");

            TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    Props.create(new DelegatingShardCreator(creator)),
                    "testRegisterChangeListenerWhenNotLeaderInitially");

            // Write initial data into the in-memory store.
            YangInstanceIdentifier path = TestModel.TEST_PATH;
            writeToStore(shard, path, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            // Wait until the shard receives the first ElectionTimeout message.
            assertEquals("Got first ElectionTimeout", true,
                    onFirstElectionTimeout.await(5, TimeUnit.SECONDS));

            // Now send the RegisterChangeListener and wait for the reply.
            shard.tell(new RegisterChangeListener(path, dclActor.path(),
                    AsyncDataBroker.DataChangeScope.SUBTREE), getRef());

            RegisterChangeListenerReply reply = expectMsgClass(duration("5 seconds"),
                    RegisterChangeListenerReply.class);
            assertNotNull("getListenerRegistrationPath", reply.getListenerRegistrationPath());

            // Sanity check - verify the shard is not the leader yet.
            shard.tell(new FindLeader(), getRef());
            FindLeaderReply findLeadeReply =
                    expectMsgClass(duration("5 seconds"), FindLeaderReply.class);
            assertNull("Expected the shard not to be the leader", findLeadeReply.getLeaderActor());

            // Signal the onChangeListenerRegistered latch to tell the thread above to proceed
            // with the election process.
            onChangeListenerRegistered.countDown();

            // Wait for the shard to become the leader and notify our listener with the existing
            // data in the store.
            listener.waitForChangeEvents(path);

            dclActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCreateTransaction(){
        new ShardTestKit(getSystem()) {{
            ActorRef shard = getSystem().actorOf(newShardProps(), "testCreateTransaction");

            waitUntilLeader(shard);

            shard.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shard.tell(new CreateTransaction("txn-1",
                    TransactionProxy.TransactionType.READ_ONLY.ordinal() ).toSerializable(), getRef());

            CreateTransactionReply reply = expectMsgClass(duration("3 seconds"),
                    CreateTransactionReply.class);

            String path = reply.getTransactionActorPath().toString();
            assertTrue("Unexpected transaction path " + path,
                    path.contains("akka://test/user/testCreateTransaction/shard-txn-1"));

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCreateTransactionOnChain(){
        new ShardTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(newShardProps(), "testCreateTransactionOnChain");

            waitUntilLeader(shard);

            shard.tell(new CreateTransaction("txn-1",
                    TransactionProxy.TransactionType.READ_ONLY.ordinal() , "foobar").toSerializable(),
                    getRef());

            CreateTransactionReply reply = expectMsgClass(duration("3 seconds"),
                    CreateTransactionReply.class);

            String path = reply.getTransactionActorPath().toString();
            assertTrue("Unexpected transaction path " + path,
                    path.contains("akka://test/user/testCreateTransactionOnChain/shard-txn-1"));

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testPeerAddressResolved() throws Exception {
        new ShardTestKit(getSystem()) {{
            final CountDownLatch recoveryComplete = new CountDownLatch(1);
            class TestShard extends Shard {
                TestShard() {
                    super(shardID, Collections.<ShardIdentifier, String>singletonMap(shardID, null),
                            dataStoreContext, SCHEMA_CONTEXT);
                }

                Map<String, String> getPeerAddresses() {
                    return getRaftActorContext().getPeerAddresses();
                }

                @Override
                protected void onRecoveryComplete() {
                    try {
                        super.onRecoveryComplete();
                    } finally {
                        recoveryComplete.countDown();
                    }
                }
            }

            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    Props.create(new DelegatingShardCreator(new Creator<Shard>() {
                        @Override
                        public TestShard create() throws Exception {
                            return new TestShard();
                        }
                    })), "testPeerAddressResolved");

            //waitUntilLeader(shard);
            assertEquals("Recovery complete", true,
                    Uninterruptibles.awaitUninterruptibly(recoveryComplete, 5, TimeUnit.SECONDS));

            String address = "akka://foobar";
            shard.underlyingActor().onReceiveCommand(new PeerAddressResolved(shardID, address));

            assertEquals("getPeerAddresses", address,
                    ((TestShard)shard.underlyingActor()).getPeerAddresses().get(shardID.toString()));

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testApplySnapshot() throws Exception {
        TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(),
                "testApplySnapshot");

        NormalizedNodeToNodeCodec codec =
            new NormalizedNodeToNodeCodec(SCHEMA_CONTEXT);

        writeToStore(shard, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        YangInstanceIdentifier root = YangInstanceIdentifier.builder().build();
        NormalizedNode<?,?> expected = readStore(shard, root);

        NormalizedNodeMessages.Container encode = codec.encode(expected);

        ApplySnapshot applySnapshot = new ApplySnapshot(Snapshot.create(
                encode.getNormalizedNode().toByteString().toByteArray(),
                Collections.<ReplicatedLogEntry>emptyList(), 1, 2, 3, 4));

        shard.underlyingActor().onReceiveCommand(applySnapshot);

        NormalizedNode<?,?> actual = readStore(shard, root);

        assertEquals(expected, actual);

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    public void testApplyState() throws Exception {

        TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(), "testApplyState");

        NormalizedNode<?, ?> node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        MutableCompositeModification compMod = new MutableCompositeModification();
        compMod.addModification(new WriteModification(TestModel.TEST_PATH, node, SCHEMA_CONTEXT));
        Payload payload = new CompositeModificationPayload(compMod.toSerializable());
        ApplyState applyState = new ApplyState(null, "test",
                new ReplicatedLogImplEntry(1, 2, payload));

        shard.underlyingActor().onReceiveCommand(applyState);

        NormalizedNode<?,?> actual = readStore(shard, TestModel.TEST_PATH);
        assertEquals("Applied state", node, actual);

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @SuppressWarnings("serial")
    @Test
    public void testRecovery() throws Exception {

        // Set up the InMemorySnapshotStore.

        InMemoryDOMDataStore testStore = InMemoryDOMDataStoreFactory.create("Test", null, null);
        testStore.onGlobalContextUpdated(SCHEMA_CONTEXT);

        DOMStoreWriteTransaction writeTx = testStore.newWriteOnlyTransaction();
        writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        DOMStoreThreePhaseCommitCohort commitCohort = writeTx.ready();
        commitCohort.preCommit().get();
        commitCohort.commit().get();

        DOMStoreReadTransaction readTx = testStore.newReadOnlyTransaction();
        NormalizedNode<?, ?> root = readTx.read(YangInstanceIdentifier.builder().build()).get().get();

        InMemorySnapshotStore.addSnapshot(shardID.toString(), Snapshot.create(
                new NormalizedNodeToNodeCodec(SCHEMA_CONTEXT).encode(
                        root).
                                getNormalizedNode().toByteString().toByteArray(),
                                Collections.<ReplicatedLogEntry>emptyList(), 0, 1, -1, -1));

        // Set up the InMemoryJournal.

        InMemoryJournal.addEntry(shardID.toString(), 0, new ReplicatedLogImplEntry(0, 1, newPayload(
                  new WriteModification(TestModel.OUTER_LIST_PATH,
                          ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(),
                          SCHEMA_CONTEXT))));

        int nListEntries = 11;
        Set<Integer> listEntryKeys = new HashSet<>();
        for(int i = 1; i <= nListEntries; i++) {
            listEntryKeys.add(Integer.valueOf(i));
            YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i).build();
            Modification mod = new MergeModification(path,
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i),
                    SCHEMA_CONTEXT);
            InMemoryJournal.addEntry(shardID.toString(), i, new ReplicatedLogImplEntry(i, 1,
                    newPayload(mod)));
        }

        InMemoryJournal.addEntry(shardID.toString(), nListEntries + 1,
                new ApplyLogEntries(nListEntries));

        // Create the actor and wait for recovery complete.

        final CountDownLatch recoveryComplete = new CountDownLatch(1);

        Creator<Shard> creator = new Creator<Shard>() {
            @Override
            public Shard create() throws Exception {
                return new Shard(shardID, Collections.<ShardIdentifier,String>emptyMap(),
                        dataStoreContext, SCHEMA_CONTEXT) {
                    @Override
                    protected void onRecoveryComplete() {
                        try {
                            super.onRecoveryComplete();
                        } finally {
                            recoveryComplete.countDown();
                        }
                    }
                };
            }
        };

        TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                Props.create(new DelegatingShardCreator(creator)), "testRecovery");

        assertEquals("Recovery complete", true, recoveryComplete.await(5, TimeUnit.SECONDS));

        // Verify data in the data store.

        NormalizedNode<?, ?> outerList = readStore(shard, TestModel.OUTER_LIST_PATH);
        assertNotNull(TestModel.OUTER_LIST_QNAME.getLocalName() + " not found", outerList);
        assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " value is not Iterable",
                outerList.getValue() instanceof Iterable);
        for(Object entry: (Iterable<?>) outerList.getValue()) {
            assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " entry is not MapEntryNode",
                    entry instanceof MapEntryNode);
            MapEntryNode mapEntry = (MapEntryNode)entry;
            Optional<DataContainerChild<? extends PathArgument, ?>> idLeaf =
                    mapEntry.getChild(new YangInstanceIdentifier.NodeIdentifier(TestModel.ID_QNAME));
            assertTrue("Missing leaf " + TestModel.ID_QNAME.getLocalName(), idLeaf.isPresent());
            Object value = idLeaf.get().getValue();
            assertTrue("Unexpected value for leaf "+ TestModel.ID_QNAME.getLocalName() + ": " + value,
                    listEntryKeys.remove(value));
        }

        if(!listEntryKeys.isEmpty()) {
            fail("Missing " + TestModel.OUTER_LIST_QNAME.getLocalName() + " entries with keys: " +
                    listEntryKeys);
        }

        assertEquals("Last log index", nListEntries,
                shard.underlyingActor().getShardMBean().getLastLogIndex());
        assertEquals("Commit index", nListEntries,
                shard.underlyingActor().getShardMBean().getCommitIndex());
        assertEquals("Last applied", nListEntries,
                shard.underlyingActor().getShardMBean().getLastApplied());

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    private CompositeModificationPayload newPayload(Modification... mods) {
        MutableCompositeModification compMod = new MutableCompositeModification();
        for(Modification mod: mods) {
            compMod.addModification(mod);
        }

        return new CompositeModificationPayload(compMod.toSerializable());
    }

    private DOMStoreThreePhaseCommitCohort setupMockWriteTransaction(String cohortName,
            InMemoryDOMDataStore dataStore, YangInstanceIdentifier path, NormalizedNode data,
            MutableCompositeModification modification) {
        return setupMockWriteTransaction(cohortName, dataStore, path, data, modification, null);
    }

    private DOMStoreThreePhaseCommitCohort setupMockWriteTransaction(String cohortName,
            InMemoryDOMDataStore dataStore, YangInstanceIdentifier path, NormalizedNode data,
            MutableCompositeModification modification,
            final Function<DOMStoreThreePhaseCommitCohort,ListenableFuture<Void>> preCommit) {

        DOMStoreWriteTransaction tx = dataStore.newWriteOnlyTransaction();
        tx.write(path, data);
        final DOMStoreThreePhaseCommitCohort realCohort = tx.ready();
        DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class, cohortName);

        doAnswer(new Answer<ListenableFuture<Boolean>>() {
            @Override
            public ListenableFuture<Boolean> answer(InvocationOnMock invocation) {
                return realCohort.canCommit();
            }
        }).when(cohort).canCommit();

        doAnswer(new Answer<ListenableFuture<Void>>() {
            @Override
            public ListenableFuture<Void> answer(InvocationOnMock invocation) throws Throwable {
                if(preCommit != null) {
                    return preCommit.apply(realCohort);
                } else {
                    return realCohort.preCommit();
                }
            }
        }).when(cohort).preCommit();

        doAnswer(new Answer<ListenableFuture<Void>>() {
            @Override
            public ListenableFuture<Void> answer(InvocationOnMock invocation) throws Throwable {
                return realCohort.commit();
            }
        }).when(cohort).commit();

        doAnswer(new Answer<ListenableFuture<Void>>() {
            @Override
            public ListenableFuture<Void> answer(InvocationOnMock invocation) throws Throwable {
                return realCohort.abort();
            }
        }).when(cohort).abort();

        modification.addModification(new WriteModification(path, data, SCHEMA_CONTEXT));

        return cohort;
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testConcurrentThreePhaseCommits() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testConcurrentThreePhaseCommits");

            waitUntilLeader(shard);

            // Setup 3 simulated transactions with mock cohorts backed by real cohorts.

            InMemoryDOMDataStore dataStore = shard.underlyingActor().getDataStore();

            String transactionID1 = "tx1";
            MutableCompositeModification modification1 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort1 = setupMockWriteTransaction("cohort1", dataStore,
                    TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME), modification1);

            String transactionID2 = "tx2";
            MutableCompositeModification modification2 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort2 = setupMockWriteTransaction("cohort2", dataStore,
                    TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(),
                    modification2);

            String transactionID3 = "tx3";
            MutableCompositeModification modification3 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort3 = setupMockWriteTransaction("cohort3", dataStore,
                    YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                        .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1),
                    modification3);

            long timeoutSec = 5;
            final FiniteDuration duration = FiniteDuration.create(timeoutSec, TimeUnit.SECONDS);
            final Timeout timeout = new Timeout(duration);

            // Simulate the ForwardedReadyTransaction message for the first Tx that would be sent
            // by the ShardTransaction.

            shard.tell(new ForwardedReadyTransaction(transactionID1, cohort1, modification1, true), getRef());
            ReadyTransactionReply readyReply = ReadyTransactionReply.fromSerializable(
                    expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Cohort path", shard.path().toString(), readyReply.getCohortPath());

            // Send the CanCommitTransaction message for the first Tx.

            shard.tell(new CanCommitTransaction(transactionID1).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            // Send the ForwardedReadyTransaction for the next 2 Tx's.

            shard.tell(new ForwardedReadyTransaction(transactionID2, cohort2, modification2, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            shard.tell(new ForwardedReadyTransaction(transactionID3, cohort3, modification3, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            // Send the CanCommitTransaction message for the next 2 Tx's. These should get queued and
            // processed after the first Tx completes.

            Future<Object> canCommitFuture1 = Patterns.ask(shard,
                    new CanCommitTransaction(transactionID2).toSerializable(), timeout);

            Future<Object> canCommitFuture2 = Patterns.ask(shard,
                    new CanCommitTransaction(transactionID3).toSerializable(), timeout);

            // Send the CommitTransaction message for the first Tx. After it completes, it should
            // trigger the 2nd Tx to proceed which should in turn then trigger the 3rd.

            shard.tell(new CommitTransaction(transactionID1).toSerializable(), getRef());
            expectMsgClass(duration, CommitTransactionReply.SERIALIZABLE_CLASS);

            // Wait for the next 2 Tx's to complete.

            final AtomicReference<Throwable> caughtEx = new AtomicReference<>();
            final CountDownLatch commitLatch = new CountDownLatch(2);

            class OnFutureComplete extends OnComplete<Object> {
                private final Class<?> expRespType;

                OnFutureComplete(Class<?> expRespType) {
                    this.expRespType = expRespType;
                }

                @Override
                public void onComplete(Throwable error, Object resp) {
                    if(error != null) {
                        caughtEx.set(new AssertionError(getClass().getSimpleName() + " failure", error));
                    } else {
                        try {
                            assertEquals("Commit response type", expRespType, resp.getClass());
                            onSuccess(resp);
                        } catch (Exception e) {
                            caughtEx.set(e);
                        }
                    }
                }

                void onSuccess(Object resp) throws Exception {
                }
            }

            class OnCommitFutureComplete extends OnFutureComplete {
                OnCommitFutureComplete() {
                    super(CommitTransactionReply.SERIALIZABLE_CLASS);
                }

                @Override
                public void onComplete(Throwable error, Object resp) {
                    super.onComplete(error, resp);
                    commitLatch.countDown();
                }
            }

            class OnCanCommitFutureComplete extends OnFutureComplete {
                private final String transactionID;

                OnCanCommitFutureComplete(String transactionID) {
                    super(CanCommitTransactionReply.SERIALIZABLE_CLASS);
                    this.transactionID = transactionID;
                }

                @Override
                void onSuccess(Object resp) throws Exception {
                    CanCommitTransactionReply canCommitReply =
                            CanCommitTransactionReply.fromSerializable(resp);
                    assertEquals("Can commit", true, canCommitReply.getCanCommit());

                    Future<Object> commitFuture = Patterns.ask(shard,
                            new CommitTransaction(transactionID).toSerializable(), timeout);
                    commitFuture.onComplete(new OnCommitFutureComplete(), getSystem().dispatcher());
                }
            }

            canCommitFuture1.onComplete(new OnCanCommitFutureComplete(transactionID2),
                    getSystem().dispatcher());

            canCommitFuture2.onComplete(new OnCanCommitFutureComplete(transactionID3),
                    getSystem().dispatcher());

            boolean done = commitLatch.await(timeoutSec, TimeUnit.SECONDS);

            if(caughtEx.get() != null) {
                throw caughtEx.get();
            }

            assertEquals("Commits complete", true, done);

            InOrder inOrder = inOrder(cohort1, cohort2, cohort3);
            inOrder.verify(cohort1).canCommit();
            inOrder.verify(cohort1).preCommit();
            inOrder.verify(cohort1).commit();
            inOrder.verify(cohort2).canCommit();
            inOrder.verify(cohort2).preCommit();
            inOrder.verify(cohort2).commit();
            inOrder.verify(cohort3).canCommit();
            inOrder.verify(cohort3).preCommit();
            inOrder.verify(cohort3).commit();

            // Verify data in the data store.

            NormalizedNode<?, ?> outerList = readStore(shard, TestModel.OUTER_LIST_PATH);
            assertNotNull(TestModel.OUTER_LIST_QNAME.getLocalName() + " not found", outerList);
            assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " value is not Iterable",
                    outerList.getValue() instanceof Iterable);
            Object entry = ((Iterable<Object>)outerList.getValue()).iterator().next();
            assertTrue(TestModel.OUTER_LIST_QNAME.getLocalName() + " entry is not MapEntryNode",
                       entry instanceof MapEntryNode);
            MapEntryNode mapEntry = (MapEntryNode)entry;
            Optional<DataContainerChild<? extends PathArgument, ?>> idLeaf =
                    mapEntry.getChild(new YangInstanceIdentifier.NodeIdentifier(TestModel.ID_QNAME));
            assertTrue("Missing leaf " + TestModel.ID_QNAME.getLocalName(), idLeaf.isPresent());
            assertEquals(TestModel.ID_QNAME.getLocalName() + " value", 1, idLeaf.get().getValue());

            for(int i = 0; i < 20 * 5; i++) {
                long lastLogIndex = shard.underlyingActor().getShardMBean().getLastLogIndex();
                if(lastLogIndex == 2) {
                    break;
                }
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }

            assertEquals("Last log index", 2, shard.underlyingActor().getShardMBean().getLastLogIndex());

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCommitPhaseFailure() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testCommitPhaseFailure");

            waitUntilLeader(shard);

            // Setup 2 simulated transactions with mock cohorts. The first one fails in the
            // commit phase.

            String transactionID1 = "tx1";
            MutableCompositeModification modification1 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort1 = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort1).canCommit();
            doReturn(Futures.immediateFuture(null)).when(cohort1).preCommit();
            doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock"))).when(cohort1).commit();

            String transactionID2 = "tx2";
            MutableCompositeModification modification2 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort2 = mock(DOMStoreThreePhaseCommitCohort.class, "cohort2");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort2).canCommit();

            FiniteDuration duration = duration("5 seconds");
            final Timeout timeout = new Timeout(duration);

            // Simulate the ForwardedReadyTransaction messages that would be sent
            // by the ShardTransaction.

            shard.tell(new ForwardedReadyTransaction(transactionID1, cohort1, modification1, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            shard.tell(new ForwardedReadyTransaction(transactionID2, cohort2, modification2, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            // Send the CanCommitTransaction message for the first Tx.

            shard.tell(new CanCommitTransaction(transactionID1).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            // Send the CanCommitTransaction message for the 2nd Tx. This should get queued and
            // processed after the first Tx completes.

            Future<Object> canCommitFuture = Patterns.ask(shard,
                    new CanCommitTransaction(transactionID2).toSerializable(), timeout);

            // Send the CommitTransaction message for the first Tx. This should send back an error
            // and trigger the 2nd Tx to proceed.

            shard.tell(new CommitTransaction(transactionID1).toSerializable(), getRef());
            expectMsgClass(duration, akka.actor.Status.Failure.class);

            // Wait for the 2nd Tx to complete the canCommit phase.

            final CountDownLatch latch = new CountDownLatch(1);
            canCommitFuture.onComplete(new OnComplete<Object>() {
                @Override
                public void onComplete(Throwable t, Object resp) {
                    latch.countDown();
                }
            }, getSystem().dispatcher());

            assertEquals("2nd CanCommit complete", true, latch.await(5, TimeUnit.SECONDS));

            InOrder inOrder = inOrder(cohort1, cohort2);
            inOrder.verify(cohort1).canCommit();
            inOrder.verify(cohort1).preCommit();
            inOrder.verify(cohort1).commit();
            inOrder.verify(cohort2).canCommit();

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testPreCommitPhaseFailure() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testPreCommitPhaseFailure");

            waitUntilLeader(shard);

            String transactionID = "tx1";
            MutableCompositeModification modification = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort).canCommit();
            doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock"))).when(cohort).preCommit();

            FiniteDuration duration = duration("5 seconds");

            // Simulate the ForwardedReadyTransaction messages that would be sent
            // by the ShardTransaction.

            shard.tell(new ForwardedReadyTransaction(transactionID, cohort, modification, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            // Send the CanCommitTransaction message.

            shard.tell(new CanCommitTransaction(transactionID).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            // Send the CommitTransaction message. This should send back an error
            // for preCommit failure.

            shard.tell(new CommitTransaction(transactionID).toSerializable(), getRef());
            expectMsgClass(duration, akka.actor.Status.Failure.class);

            InOrder inOrder = inOrder(cohort);
            inOrder.verify(cohort).canCommit();
            inOrder.verify(cohort).preCommit();

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCanCommitPhaseFailure() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testCanCommitPhaseFailure");

            waitUntilLeader(shard);

            final FiniteDuration duration = duration("5 seconds");

            String transactionID = "tx1";
            MutableCompositeModification modification = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
            doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock"))).when(cohort).canCommit();

            // Simulate the ForwardedReadyTransaction messages that would be sent
            // by the ShardTransaction.

            shard.tell(new ForwardedReadyTransaction(transactionID, cohort, modification, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            // Send the CanCommitTransaction message.

            shard.tell(new CanCommitTransaction(transactionID).toSerializable(), getRef());
            expectMsgClass(duration, akka.actor.Status.Failure.class);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testAbortBeforeFinishCommit() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testAbortBeforeFinishCommit");

            waitUntilLeader(shard);

            final FiniteDuration duration = duration("5 seconds");
            final Timeout timeout = new Timeout(duration);

            InMemoryDOMDataStore dataStore = shard.underlyingActor().getDataStore();

            final String transactionID = "tx1";
            final CountDownLatch abortComplete = new CountDownLatch(1);
            Function<DOMStoreThreePhaseCommitCohort,ListenableFuture<Void>> preCommit =
                          new Function<DOMStoreThreePhaseCommitCohort,ListenableFuture<Void>>() {
                @Override
                public ListenableFuture<Void> apply(final DOMStoreThreePhaseCommitCohort cohort) {
                    ListenableFuture<Void> preCommitFuture = cohort.preCommit();

                    Future<Object> abortFuture = Patterns.ask(shard,
                            new AbortTransaction(transactionID).toSerializable(), timeout);
                    abortFuture.onComplete(new OnComplete<Object>() {
                        @Override
                        public void onComplete(Throwable e, Object resp) {
                            abortComplete.countDown();
                        }
                    }, getSystem().dispatcher());

                    return preCommitFuture;
                }
            };

            MutableCompositeModification modification = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort = setupMockWriteTransaction("cohort1", dataStore,
                    TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME),
                    modification, preCommit);

            shard.tell(new ForwardedReadyTransaction(transactionID, cohort, modification, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            shard.tell(new CanCommitTransaction(transactionID).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            Future<Object> commitFuture = Patterns.ask(shard,
                    new CommitTransaction(transactionID).toSerializable(), timeout);

            assertEquals("Abort complete", true, abortComplete.await(5, TimeUnit.SECONDS));

            Await.result(commitFuture, duration);

            NormalizedNode<?, ?> node = readStore(shard, TestModel.TEST_PATH);
            assertNotNull(TestModel.TEST_QNAME.getLocalName() + " not found", node);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testTransactionCommitTimeout() throws Throwable {
        dataStoreContext = DatastoreContext.newBuilder().shardTransactionCommitTimeoutInSeconds(1).build();

        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testTransactionCommitTimeout");

            waitUntilLeader(shard);

            final FiniteDuration duration = duration("5 seconds");

            InMemoryDOMDataStore dataStore = shard.underlyingActor().getDataStore();

            writeToStore(shard, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
            writeToStore(shard, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

            // Create 1st Tx - will timeout

            String transactionID1 = "tx1";
            MutableCompositeModification modification1 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort1 = setupMockWriteTransaction("cohort1", dataStore,
                    YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                        .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1),
                    modification1);

            // Create 2nd Tx

            String transactionID2 = "tx3";
            MutableCompositeModification modification2 = new MutableCompositeModification();
            YangInstanceIdentifier listNodePath = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2).build();
            DOMStoreThreePhaseCommitCohort cohort2 = setupMockWriteTransaction("cohort3", dataStore,
                    listNodePath,
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2),
                    modification2);

            // Ready the Tx's

            shard.tell(new ForwardedReadyTransaction(transactionID1, cohort1, modification1, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            shard.tell(new ForwardedReadyTransaction(transactionID2, cohort2, modification2, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            // canCommit 1st Tx. We don't send the commit so it should timeout.

            shard.tell(new CanCommitTransaction(transactionID1).toSerializable(), getRef());
            expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS);

            // canCommit the 2nd Tx - it should complete after the 1st Tx times out.

            shard.tell(new CanCommitTransaction(transactionID2).toSerializable(), getRef());
            expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS);

            // Commit the 2nd Tx.

            shard.tell(new CommitTransaction(transactionID2).toSerializable(), getRef());
            expectMsgClass(duration, CommitTransactionReply.SERIALIZABLE_CLASS);

            NormalizedNode<?, ?> node = readStore(shard, listNodePath);
            assertNotNull(listNodePath + " not found", node);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testTransactionCommitQueueCapacityExceeded() throws Throwable {
        dataStoreContext = DatastoreContext.newBuilder().shardTransactionCommitQueueCapacity(1).build();

        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testTransactionCommitQueueCapacityExceeded");

            waitUntilLeader(shard);

            final FiniteDuration duration = duration("5 seconds");

            InMemoryDOMDataStore dataStore = shard.underlyingActor().getDataStore();

            String transactionID1 = "tx1";
            MutableCompositeModification modification1 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort1 = setupMockWriteTransaction("cohort1", dataStore,
                    TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME), modification1);

            String transactionID2 = "tx2";
            MutableCompositeModification modification2 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort2 = setupMockWriteTransaction("cohort2", dataStore,
                    TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(),
                    modification2);

            String transactionID3 = "tx3";
            MutableCompositeModification modification3 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort3 = setupMockWriteTransaction("cohort3", dataStore,
                    TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME), modification3);

            // Ready the Tx's

            shard.tell(new ForwardedReadyTransaction(transactionID1, cohort1, modification1, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            shard.tell(new ForwardedReadyTransaction(transactionID2, cohort2, modification2, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            shard.tell(new ForwardedReadyTransaction(transactionID3, cohort3, modification3, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            // canCommit 1st Tx.

            shard.tell(new CanCommitTransaction(transactionID1).toSerializable(), getRef());
            expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS);

            // canCommit the 2nd Tx - it should get queued.

            shard.tell(new CanCommitTransaction(transactionID2).toSerializable(), getRef());

            // canCommit the 3rd Tx - should exceed queue capacity and fail.

            shard.tell(new CanCommitTransaction(transactionID3).toSerializable(), getRef());
            expectMsgClass(duration, akka.actor.Status.Failure.class);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCanCommitBeforeReadyFailure() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testCanCommitBeforeReadyFailure");

            shard.tell(new CanCommitTransaction("tx").toSerializable(), getRef());
            expectMsgClass(duration("5 seconds"), akka.actor.Status.Failure.class);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testAbortTransaction() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testAbortTransaction");

            waitUntilLeader(shard);

            // Setup 2 simulated transactions with mock cohorts. The first one will be aborted.

            String transactionID1 = "tx1";
            MutableCompositeModification modification1 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort1 = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort1).canCommit();
            doReturn(Futures.immediateFuture(null)).when(cohort1).abort();

            String transactionID2 = "tx2";
            MutableCompositeModification modification2 = new MutableCompositeModification();
            DOMStoreThreePhaseCommitCohort cohort2 = mock(DOMStoreThreePhaseCommitCohort.class, "cohort2");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort2).canCommit();

            FiniteDuration duration = duration("5 seconds");
            final Timeout timeout = new Timeout(duration);

            // Simulate the ForwardedReadyTransaction messages that would be sent
            // by the ShardTransaction.

            shard.tell(new ForwardedReadyTransaction(transactionID1, cohort1, modification1, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            shard.tell(new ForwardedReadyTransaction(transactionID2, cohort2, modification2, true), getRef());
            expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

            // Send the CanCommitTransaction message for the first Tx.

            shard.tell(new CanCommitTransaction(transactionID1).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            // Send the CanCommitTransaction message for the 2nd Tx. This should get queued and
            // processed after the first Tx completes.

            Future<Object> canCommitFuture = Patterns.ask(shard,
                    new CanCommitTransaction(transactionID2).toSerializable(), timeout);

            // Send the AbortTransaction message for the first Tx. This should trigger the 2nd
            // Tx to proceed.

            shard.tell(new AbortTransaction(transactionID1).toSerializable(), getRef());
            expectMsgClass(duration, AbortTransactionReply.SERIALIZABLE_CLASS);

            // Wait for the 2nd Tx to complete the canCommit phase.

            final CountDownLatch latch = new CountDownLatch(1);
            canCommitFuture.onComplete(new OnComplete<Object>() {
                @Override
                public void onComplete(Throwable t, Object resp) {
                    latch.countDown();
                }
            }, getSystem().dispatcher());

            assertEquals("2nd CanCommit complete", true, latch.await(5, TimeUnit.SECONDS));

            InOrder inOrder = inOrder(cohort1, cohort2);
            inOrder.verify(cohort1).canCommit();
            inOrder.verify(cohort2).canCommit();

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCreateSnapshot() throws IOException, InterruptedException {
            testCreateSnapshot(true, "testCreateSnapshot");
    }

    @Test
    public void testCreateSnapshotWithNonPersistentData() throws IOException, InterruptedException {
        testCreateSnapshot(false, "testCreateSnapshotWithNonPersistentData");
    }

    public void testCreateSnapshot(boolean persistent, final String shardActorName) throws IOException, InterruptedException {
        final DatastoreContext dataStoreContext = DatastoreContext.newBuilder().
                shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(persistent).build();

        new ShardTestKit(getSystem()) {{
            final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));
            Creator<Shard> creator = new Creator<Shard>() {
                @Override
                public Shard create() throws Exception {
                    return new Shard(shardID, Collections.<ShardIdentifier,String>emptyMap(),
                            dataStoreContext, SCHEMA_CONTEXT) {
                        @Override
                        protected void commitSnapshot(long sequenceNumber) {
                            super.commitSnapshot(sequenceNumber);
                            latch.get().countDown();
                        }
                    };
                }
            };

            TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    Props.create(new DelegatingShardCreator(creator)), shardActorName);

            waitUntilLeader(shard);

            shard.tell(new CaptureSnapshot(-1,-1,-1,-1), getRef());

            assertEquals("Snapshot saved", true, latch.get().await(5, TimeUnit.SECONDS));

            latch.set(new CountDownLatch(1));
            shard.tell(new CaptureSnapshot(-1,-1,-1,-1), getRef());

            assertEquals("Snapshot saved", true, latch.get().await(5, TimeUnit.SECONDS));

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    /**
     * This test simply verifies that the applySnapShot logic will work
     * @throws ReadFailedException
     */
    @Test
    public void testInMemoryDataStoreRestore() throws ReadFailedException {
        InMemoryDOMDataStore store = new InMemoryDOMDataStore("test", MoreExecutors.listeningDecorator(
            MoreExecutors.sameThreadExecutor()), MoreExecutors.sameThreadExecutor());

        store.onGlobalContextUpdated(SCHEMA_CONTEXT);

        DOMStoreWriteTransaction putTransaction = store.newWriteOnlyTransaction();
        putTransaction.write(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        commitTransaction(putTransaction);


        NormalizedNode expected = readStore(store);

        DOMStoreWriteTransaction writeTransaction = store.newWriteOnlyTransaction();

        writeTransaction.delete(YangInstanceIdentifier.builder().build());
        writeTransaction.write(YangInstanceIdentifier.builder().build(), expected);

        commitTransaction(writeTransaction);

        NormalizedNode actual = readStore(store);

        assertEquals(expected, actual);

    }

    @Test
    public void testRecoveryApplicable(){

        final DatastoreContext persistentContext = DatastoreContext.newBuilder().
                shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(true).build();

        final Props persistentProps = Shard.props(shardID, Collections.<ShardIdentifier, String>emptyMap(),
                persistentContext, SCHEMA_CONTEXT);

        final DatastoreContext nonPersistentContext = DatastoreContext.newBuilder().
                shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(false).build();

        final Props nonPersistentProps = Shard.props(shardID, Collections.<ShardIdentifier, String>emptyMap(),
                nonPersistentContext, SCHEMA_CONTEXT);

        new ShardTestKit(getSystem()) {{
            TestActorRef<Shard> shard1 = TestActorRef.create(getSystem(),
                    persistentProps, "testPersistence1");

            assertTrue("Recovery Applicable", shard1.underlyingActor().getDataPersistenceProvider().isRecoveryApplicable());

            shard1.tell(PoisonPill.getInstance(), ActorRef.noSender());

            TestActorRef<Shard> shard2 = TestActorRef.create(getSystem(),
                    nonPersistentProps, "testPersistence2");

            assertFalse("Recovery Not Applicable", shard2.underlyingActor().getDataPersistenceProvider().isRecoveryApplicable());

            shard2.tell(PoisonPill.getInstance(), ActorRef.noSender());

        }};

    }


    private NormalizedNode readStore(InMemoryDOMDataStore store) throws ReadFailedException {
        DOMStoreReadTransaction transaction = store.newReadOnlyTransaction();
        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
            transaction.read(YangInstanceIdentifier.builder().build());

        Optional<NormalizedNode<?, ?>> optional = read.checkedGet();

        NormalizedNode<?, ?> normalizedNode = optional.get();

        transaction.close();

        return normalizedNode;
    }

    private void commitTransaction(DOMStoreWriteTransaction transaction) {
        DOMStoreThreePhaseCommitCohort commitCohort = transaction.ready();
        ListenableFuture<Void> future =
            commitCohort.preCommit();
        try {
            future.get();
            future = commitCohort.commit();
            future.get();
        } catch (InterruptedException | ExecutionException e) {
        }
    }

    private AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> noOpDataChangeListener() {
        return new AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
            @Override
            public void onDataChanged(
                AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {

            }
        };
    }

    private NormalizedNode<?,?> readStore(TestActorRef<Shard> shard, YangInstanceIdentifier id)
            throws ExecutionException, InterruptedException {
        DOMStoreReadTransaction transaction = shard.underlyingActor().getDataStore().newReadOnlyTransaction();

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
            transaction.read(id);

        Optional<NormalizedNode<?, ?>> optional = future.get();
        NormalizedNode<?, ?> node = optional.isPresent()? optional.get() : null;

        transaction.close();

        return node;
    }

    private void writeToStore(TestActorRef<Shard> shard, YangInstanceIdentifier id, NormalizedNode<?,?> node)
        throws ExecutionException, InterruptedException {
        DOMStoreWriteTransaction transaction = shard.underlyingActor().getDataStore().newWriteOnlyTransaction();

        transaction.write(id, node);

        DOMStoreThreePhaseCommitCohort commitCohort = transaction.ready();
        commitCohort.preCommit().get();
        commitCohort.commit().get();
    }

    private static final class DelegatingShardCreator implements Creator<Shard> {
        private final Creator<Shard> delegate;

        DelegatingShardCreator(Creator<Shard> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Shard create() throws Exception {
            return delegate.create();
        }
    }
}
