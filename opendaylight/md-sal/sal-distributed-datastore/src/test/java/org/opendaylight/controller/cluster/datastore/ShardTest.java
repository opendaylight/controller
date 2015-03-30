package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.datastore.DataStoreVersions.CURRENT_VERSION;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.dispatch.OnComplete;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.persistence.SaveSnapshotSuccess;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.mockito.InOrder;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.DelegatingPersistentDataProvider;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.ModificationPayload;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.datastore.utils.MockDataChangeListener;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class ShardTest extends AbstractShardTest {

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
                    dclActor, AsyncDataBroker.DataChangeScope.BASE), getRef());

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
                    // Use a non persistent provider because this test actually invokes persist on the journal
                    // this will cause all other messages to not be queued properly after that.
                    // The basic issue is that you cannot use TestActorRef with a persistent actor (at least when
                    // it does do a persist)
                    return new Shard(shardID, Collections.<String,String>emptyMap(),
                            dataStoreContextBuilder.persistent(false).build(), SCHEMA_CONTEXT) {
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
            shard.tell(new RegisterChangeListener(path, dclActor,
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

    @SuppressWarnings("serial")
    @Test
    public void testPeerAddressResolved() throws Exception {
        new ShardTestKit(getSystem()) {{
            final CountDownLatch recoveryComplete = new CountDownLatch(1);
            class TestShard extends Shard {
                TestShard() {
                    super(shardID, Collections.<String, String>singletonMap(shardID.toString(), null),
                            newDatastoreContext(), SCHEMA_CONTEXT);
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
            shard.underlyingActor().onReceiveCommand(new PeerAddressResolved(shardID.toString(), address));

            assertEquals("getPeerAddresses", address,
                    ((TestShard)shard.underlyingActor()).getPeerAddresses().get(shardID.toString()));

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testApplySnapshot() throws Exception {
        TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(),
                "testApplySnapshot");

        InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", MoreExecutors.sameThreadExecutor());
        store.onGlobalContextUpdated(SCHEMA_CONTEXT);

        writeToStore(store, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        YangInstanceIdentifier root = YangInstanceIdentifier.builder().build();
        NormalizedNode<?,?> expected = readStore(store, root);

        ApplySnapshot applySnapshot = new ApplySnapshot(Snapshot.create(
                SerializationUtils.serializeNormalizedNode(expected),
                Collections.<ReplicatedLogEntry>emptyList(), 1, 2, 3, 4));

        shard.underlyingActor().onReceiveCommand(applySnapshot);

        NormalizedNode<?,?> actual = readStore(shard, root);

        assertEquals("Root node", expected, actual);

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    public void testApplyState() throws Exception {

        TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(), "testApplyState");

        NormalizedNode<?, ?> node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        ApplyState applyState = new ApplyState(null, "test", new ReplicatedLogImplEntry(1, 2,
                newModificationPayload(new WriteModification(TestModel.TEST_PATH, node))));

        shard.underlyingActor().onReceiveCommand(applyState);

        NormalizedNode<?,?> actual = readStore(shard, TestModel.TEST_PATH);
        assertEquals("Applied state", node, actual);

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    public void testRecovery() throws Exception {

        // Set up the InMemorySnapshotStore.

        InMemoryDOMDataStore testStore = InMemoryDOMDataStoreFactory.create("Test", null, null);
        testStore.onGlobalContextUpdated(SCHEMA_CONTEXT);

        writeToStore(testStore, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        NormalizedNode<?, ?> root = readStore(testStore, YangInstanceIdentifier.builder().build());

        InMemorySnapshotStore.addSnapshot(shardID.toString(), Snapshot.create(
                SerializationUtils.serializeNormalizedNode(root),
                Collections.<ReplicatedLogEntry>emptyList(), 0, 1, -1, -1));

        // Set up the InMemoryJournal.

        InMemoryJournal.addEntry(shardID.toString(), 0, new ReplicatedLogImplEntry(0, 1, newModificationPayload(
                  new WriteModification(TestModel.OUTER_LIST_PATH,
                          ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build()))));

        int nListEntries = 16;
        Set<Integer> listEntryKeys = new HashSet<>();

        // Add some ModificationPayload entries
        for(int i = 1; i <= nListEntries; i++) {
            listEntryKeys.add(Integer.valueOf(i));
            YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i).build();
            Modification mod = new MergeModification(path,
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i));
            InMemoryJournal.addEntry(shardID.toString(), i, new ReplicatedLogImplEntry(i, 1,
                    newModificationPayload(mod)));
        }

        InMemoryJournal.addEntry(shardID.toString(), nListEntries + 1,
                new ApplyJournalEntries(nListEntries));

        testRecovery(listEntryKeys);
    }

    private ModificationPayload newModificationPayload(final Modification... mods) throws IOException {
        MutableCompositeModification compMod = new MutableCompositeModification();
        for(Modification mod: mods) {
            compMod.addModification(mod);
        }

        return new ModificationPayload(compMod);
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testConcurrentThreePhaseCommits() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testConcurrentThreePhaseCommits");

            waitUntilLeader(shard);

            final String transactionID1 = "tx1";
            final String transactionID2 = "tx2";
            final String transactionID3 = "tx3";

            final AtomicReference<DOMStoreThreePhaseCommitCohort> mockCohort1 = new AtomicReference<>();
            final AtomicReference<DOMStoreThreePhaseCommitCohort> mockCohort2 = new AtomicReference<>();
            final AtomicReference<DOMStoreThreePhaseCommitCohort> mockCohort3 = new AtomicReference<>();
            ShardCommitCoordinator.CohortDecorator cohortDecorator = new ShardCommitCoordinator.CohortDecorator() {
                @Override
                public DOMStoreThreePhaseCommitCohort decorate(String transactionID, DOMStoreThreePhaseCommitCohort actual) {
                    if(transactionID.equals(transactionID1)) {
                        mockCohort1.set(createDelegatingMockCohort("cohort1", actual));
                        return mockCohort1.get();
                    } else if(transactionID.equals(transactionID2)) {
                        mockCohort2.set(createDelegatingMockCohort("cohort2", actual));
                        return mockCohort2.get();
                    } else {
                        mockCohort3.set(createDelegatingMockCohort("cohort3", actual));
                        return mockCohort3.get();
                    }
                }
            };

            shard.underlyingActor().getCommitCoordinator().setCohortDecorator(cohortDecorator);

            long timeoutSec = 5;
            final FiniteDuration duration = FiniteDuration.create(timeoutSec, TimeUnit.SECONDS);
            final Timeout timeout = new Timeout(duration);

            // Send a BatchedModifications message for the first transaction.

            shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            BatchedModificationsReply batchedReply = expectMsgClass(duration, BatchedModificationsReply.class);
            assertEquals("getCohortPath", shard.path().toString(), batchedReply.getCohortPath());
            assertEquals("getNumBatched", 1, batchedReply.getNumBatched());

            // Send the CanCommitTransaction message for the first Tx.

            shard.tell(new CanCommitTransaction(transactionID1).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            // Send BatchedModifications for the next 2 Tx's.

            shard.tell(newBatchedModifications(transactionID2, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            shard.tell(newBatchedModifications(transactionID3, YangInstanceIdentifier.builder(
                    TestModel.OUTER_LIST_PATH).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

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

                OnFutureComplete(final Class<?> expRespType) {
                    this.expRespType = expRespType;
                }

                @Override
                public void onComplete(final Throwable error, final Object resp) {
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

                void onSuccess(final Object resp) throws Exception {
                }
            }

            class OnCommitFutureComplete extends OnFutureComplete {
                OnCommitFutureComplete() {
                    super(CommitTransactionReply.SERIALIZABLE_CLASS);
                }

                @Override
                public void onComplete(final Throwable error, final Object resp) {
                    super.onComplete(error, resp);
                    commitLatch.countDown();
                }
            }

            class OnCanCommitFutureComplete extends OnFutureComplete {
                private final String transactionID;

                OnCanCommitFutureComplete(final String transactionID) {
                    super(CanCommitTransactionReply.SERIALIZABLE_CLASS);
                    this.transactionID = transactionID;
                }

                @Override
                void onSuccess(final Object resp) throws Exception {
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

            InOrder inOrder = inOrder(mockCohort1.get(), mockCohort2.get(), mockCohort3.get());
            inOrder.verify(mockCohort1.get()).canCommit();
            inOrder.verify(mockCohort1.get()).preCommit();
            inOrder.verify(mockCohort1.get()).commit();
            inOrder.verify(mockCohort2.get()).canCommit();
            inOrder.verify(mockCohort2.get()).preCommit();
            inOrder.verify(mockCohort2.get()).commit();
            inOrder.verify(mockCohort3.get()).canCommit();
            inOrder.verify(mockCohort3.get()).preCommit();
            inOrder.verify(mockCohort3.get()).commit();

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

            verifyLastApplied(shard, 2);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    private BatchedModifications newBatchedModifications(String transactionID, YangInstanceIdentifier path,
            NormalizedNode<?, ?> data, boolean ready) {
        return newBatchedModifications(transactionID, null, path, data, ready);
    }

    private BatchedModifications newBatchedModifications(String transactionID, String transactionChainID,
            YangInstanceIdentifier path, NormalizedNode<?, ?> data, boolean ready) {
        BatchedModifications batched = new BatchedModifications(transactionID, CURRENT_VERSION, transactionChainID);
        batched.addModification(new WriteModification(path, data));
        batched.setReady(ready);
        return batched;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMultipleBatchedModifications() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testMultipleBatchedModifications");

            waitUntilLeader(shard);

            final String transactionID = "tx";
            FiniteDuration duration = duration("5 seconds");

            final AtomicReference<DOMStoreThreePhaseCommitCohort> mockCohort = new AtomicReference<>();
            ShardCommitCoordinator.CohortDecorator cohortDecorator = new ShardCommitCoordinator.CohortDecorator() {
                @Override
                public DOMStoreThreePhaseCommitCohort decorate(String txID, DOMStoreThreePhaseCommitCohort actual) {
                    if(mockCohort.get() == null) {
                        mockCohort.set(createDelegatingMockCohort("cohort", actual));
                    }

                    return mockCohort.get();
                }
            };

            shard.underlyingActor().getCommitCoordinator().setCohortDecorator(cohortDecorator);

            // Send a BatchedModifications to start a transaction.

            shard.tell(newBatchedModifications(transactionID, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), false), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            // Send a couple more BatchedModifications.

            shard.tell(newBatchedModifications(transactionID, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(), false), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            shard.tell(newBatchedModifications(transactionID, YangInstanceIdentifier.builder(
                    TestModel.OUTER_LIST_PATH).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            // Send the CanCommitTransaction message.

            shard.tell(new CanCommitTransaction(transactionID).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            // Send the CanCommitTransaction message.

            shard.tell(new CommitTransaction(transactionID).toSerializable(), getRef());
            expectMsgClass(duration, CommitTransactionReply.SERIALIZABLE_CLASS);

            InOrder inOrder = inOrder(mockCohort.get());
            inOrder.verify(mockCohort.get()).canCommit();
            inOrder.verify(mockCohort.get()).preCommit();
            inOrder.verify(mockCohort.get()).commit();

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

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testBatchedModificationsOnTransactionChain() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testBatchedModificationsOnTransactionChain");

            waitUntilLeader(shard);

            String transactionChainID = "txChain";
            String transactionID1 = "tx1";
            String transactionID2 = "tx2";

            FiniteDuration duration = duration("5 seconds");

            // Send a BatchedModifications to start a chained write transaction and ready it.

            ContainerNode containerNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            YangInstanceIdentifier path = TestModel.TEST_PATH;
            shard.tell(newBatchedModifications(transactionID1, transactionChainID, path,
                    containerNode, true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            // Create a read Tx on the same chain.

            shard.tell(new CreateTransaction(transactionID2, TransactionProxy.TransactionType.READ_ONLY.ordinal() ,
                    transactionChainID).toSerializable(), getRef());

            CreateTransactionReply createReply = expectMsgClass(duration("3 seconds"), CreateTransactionReply.class);

            getSystem().actorSelection(createReply.getTransactionActorPath()).tell(new ReadData(path), getRef());
            ReadDataReply readReply = expectMsgClass(duration("3 seconds"), ReadDataReply.class);
            assertEquals("Read node", containerNode, readReply.getNormalizedNode());

            // Commit the write transaction.

            shard.tell(new CanCommitTransaction(transactionID1).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            shard.tell(new CommitTransaction(transactionID1).toSerializable(), getRef());
            expectMsgClass(duration, CommitTransactionReply.SERIALIZABLE_CLASS);

            // Verify data in the data store.

            NormalizedNode<?, ?> actualNode = readStore(shard, path);
            assertEquals("Stored node", containerNode, actualNode);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testOnBatchedModificationsWhenNotLeader() {
        final AtomicBoolean overrideLeaderCalls = new AtomicBoolean();
        new ShardTestKit(getSystem()) {{
            Creator<Shard> creator = new Creator<Shard>() {
                @Override
                public Shard create() throws Exception {
                    return new Shard(shardID, Collections.<String,String>emptyMap(),
                            newDatastoreContext(), SCHEMA_CONTEXT) {
                        @Override
                        protected boolean isLeader() {
                            return overrideLeaderCalls.get() ? false : super.isLeader();
                        }

                        @Override
                        protected ActorSelection getLeader() {
                            return overrideLeaderCalls.get() ? getSystem().actorSelection(getRef().path()) :
                                super.getLeader();
                        }
                    };
                }
            };

            TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    Props.create(new DelegatingShardCreator(creator)), "testOnBatchedModificationsWhenNotLeader");

            waitUntilLeader(shard);

            overrideLeaderCalls.set(true);

            BatchedModifications batched = new BatchedModifications("tx", DataStoreVersions.CURRENT_VERSION, "");

            shard.tell(batched, ActorRef.noSender());

            expectMsgEquals(batched);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCommitWithPersistenceDisabled() throws Throwable {
        dataStoreContextBuilder.persistent(false);
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testCommitWithPersistenceDisabled");

            waitUntilLeader(shard);

            String transactionID = "tx";
            FiniteDuration duration = duration("5 seconds");

            // Send a BatchedModifications to start a transaction.

            NormalizedNode<?, ?> containerNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            shard.tell(newBatchedModifications(transactionID, TestModel.TEST_PATH, containerNode, true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            // Send the CanCommitTransaction message.

            shard.tell(new CanCommitTransaction(transactionID).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            // Send the CanCommitTransaction message.

            shard.tell(new CommitTransaction(transactionID).toSerializable(), getRef());
            expectMsgClass(duration, CommitTransactionReply.SERIALIZABLE_CLASS);

            NormalizedNode<?, ?> actualNode = readStore(shard, TestModel.TEST_PATH);
            assertEquals(TestModel.TEST_QNAME.getLocalName(), containerNode, actualNode);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCommitWhenTransactionHasNoModifications(){
        // Note that persistence is enabled which would normally result in the entry getting written to the journal
        // but here that need not happen
        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testCommitWhenTransactionHasNoModifications");

                waitUntilLeader(shard);

                String transactionID = "tx1";
                MutableCompositeModification modification = new MutableCompositeModification();
                DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
                doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort).canCommit();
                doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort).preCommit();
                doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort).commit();

                FiniteDuration duration = duration("5 seconds");

                // Simulate the ForwardedReadyTransaction messages that would be sent
                // by the ShardTransaction.

                shard.tell(new ForwardedReadyTransaction(transactionID, CURRENT_VERSION,
                        cohort, modification, true), getRef());
                expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

                // Send the CanCommitTransaction message.

                shard.tell(new CanCommitTransaction(transactionID).toSerializable(), getRef());
                CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                        expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
                assertEquals("Can commit", true, canCommitReply.getCanCommit());

                shard.tell(new CommitTransaction(transactionID).toSerializable(), getRef());
                expectMsgClass(duration, ThreePhaseCommitCohortMessages.CommitTransactionReply.class);

                InOrder inOrder = inOrder(cohort);
                inOrder.verify(cohort).canCommit();
                inOrder.verify(cohort).preCommit();
                inOrder.verify(cohort).commit();

                // Use MBean for verification
                // Committed transaction count should increase as usual
                assertEquals(1,shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());

                // Commit index should not advance because this does not go into the journal
                assertEquals(-1, shard.underlyingActor().getShardMBean().getCommitIndex());

                shard.tell(PoisonPill.getInstance(), ActorRef.noSender());

            }
        };
    }

    @Test
    public void testCommitWhenTransactionHasModifications(){
        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testCommitWhenTransactionHasModifications");

                waitUntilLeader(shard);

                String transactionID = "tx1";
                MutableCompositeModification modification = new MutableCompositeModification();
                modification.addModification(new DeleteModification(YangInstanceIdentifier.builder().build()));
                DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
                doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort).canCommit();
                doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort).preCommit();
                doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort).commit();

                FiniteDuration duration = duration("5 seconds");

                // Simulate the ForwardedReadyTransaction messages that would be sent
                // by the ShardTransaction.

                shard.tell(new ForwardedReadyTransaction(transactionID, CURRENT_VERSION,
                        cohort, modification, true), getRef());
                expectMsgClass(duration, ReadyTransactionReply.SERIALIZABLE_CLASS);

                // Send the CanCommitTransaction message.

                shard.tell(new CanCommitTransaction(transactionID).toSerializable(), getRef());
                CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                        expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
                assertEquals("Can commit", true, canCommitReply.getCanCommit());

                shard.tell(new CommitTransaction(transactionID).toSerializable(), getRef());
                expectMsgClass(duration, ThreePhaseCommitCohortMessages.CommitTransactionReply.class);

                InOrder inOrder = inOrder(cohort);
                inOrder.verify(cohort).canCommit();
                inOrder.verify(cohort).preCommit();
                inOrder.verify(cohort).commit();

                // Use MBean for verification
                // Committed transaction count should increase as usual
                assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());

                // Commit index should advance as we do not have an empty modification
                assertEquals(0, shard.underlyingActor().getShardMBean().getCommitIndex());

                shard.tell(PoisonPill.getInstance(), ActorRef.noSender());

            }
        };
    }

    @Test
    public void testCommitPhaseFailure() throws Throwable {
        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testCommitPhaseFailure");

            waitUntilLeader(shard);

            // Setup 2 mock cohorts. The first one fails in the commit phase.

            final String transactionID1 = "tx1";
            final DOMStoreThreePhaseCommitCohort cohort1 = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort1).canCommit();
            doReturn(Futures.immediateFuture(null)).when(cohort1).preCommit();
            doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock"))).when(cohort1).commit();

            final String transactionID2 = "tx2";
            final DOMStoreThreePhaseCommitCohort cohort2 = mock(DOMStoreThreePhaseCommitCohort.class, "cohort2");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort2).canCommit();

            ShardCommitCoordinator.CohortDecorator cohortDecorator = new ShardCommitCoordinator.CohortDecorator() {
                @Override
                public DOMStoreThreePhaseCommitCohort decorate(String transactionID,
                        DOMStoreThreePhaseCommitCohort actual) {
                    return transactionID1.equals(transactionID) ? cohort1 : cohort2;
                }
            };

            shard.underlyingActor().getCommitCoordinator().setCohortDecorator(cohortDecorator);

            FiniteDuration duration = duration("5 seconds");
            final Timeout timeout = new Timeout(duration);

            // Send BatchedModifications to start and ready each transaction.

            shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

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
                public void onComplete(final Throwable t, final Object resp) {
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
            final DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort).canCommit();
            doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock"))).when(cohort).preCommit();

            ShardCommitCoordinator.CohortDecorator cohortDecorator = new ShardCommitCoordinator.CohortDecorator() {
                @Override
                public DOMStoreThreePhaseCommitCohort decorate(String transactionID,
                        DOMStoreThreePhaseCommitCohort actual) {
                    return cohort;
                }
            };

            shard.underlyingActor().getCommitCoordinator().setCohortDecorator(cohortDecorator);

            FiniteDuration duration = duration("5 seconds");

            // Send BatchedModifications to start and ready a transaction.

            shard.tell(newBatchedModifications(transactionID, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

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
            final DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
            doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock"))).when(cohort).canCommit();

            ShardCommitCoordinator.CohortDecorator cohortDecorator = new ShardCommitCoordinator.CohortDecorator() {
                @Override
                public DOMStoreThreePhaseCommitCohort decorate(String transactionID,
                        DOMStoreThreePhaseCommitCohort actual) {
                    return cohort;
                }
            };

            shard.underlyingActor().getCommitCoordinator().setCohortDecorator(cohortDecorator);

            // Send BatchedModifications to start and ready a transaction.

            shard.tell(newBatchedModifications(transactionID, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

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
            InMemoryDOMDataStore dataStore = shard.underlyingActor().getDataStore();

            final String transactionID = "tx1";
            Function<DOMStoreThreePhaseCommitCohort,ListenableFuture<Void>> preCommit =
                          new Function<DOMStoreThreePhaseCommitCohort,ListenableFuture<Void>>() {
                @Override
                public ListenableFuture<Void> apply(final DOMStoreThreePhaseCommitCohort cohort) {
                    ListenableFuture<Void> preCommitFuture = cohort.preCommit();

                    // Simulate an AbortTransaction message occurring during replication, after
                    // persisting and before finishing the commit to the in-memory store.
                    // We have no followers so due to optimizations in the RaftActor, it does not
                    // attempt replication and thus we can't send an AbortTransaction message b/c
                    // it would be processed too late after CommitTransaction completes. So we'll
                    // simulate an AbortTransaction message occurring during replication by calling
                    // the shard directly.
                    //
                    shard.underlyingActor().doAbortTransaction(transactionID, null);

                    return preCommitFuture;
                }
            };

            shard.tell(newBatchedModifications(transactionID, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            shard.tell(new CanCommitTransaction(transactionID).toSerializable(), getRef());
            CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(
                    expectMsgClass(duration, CanCommitTransactionReply.SERIALIZABLE_CLASS));
            assertEquals("Can commit", true, canCommitReply.getCanCommit());

            shard.tell(new CommitTransaction(transactionID).toSerializable(), getRef());
            expectMsgClass(duration, CommitTransactionReply.SERIALIZABLE_CLASS);

            NormalizedNode<?, ?> node = readStore(shard, TestModel.TEST_PATH);

            // Since we're simulating an abort occurring during replication and before finish commit,
            // the data should still get written to the in-memory store since we've gotten past
            // canCommit and preCommit and persisted the data.
            assertNotNull(TestModel.TEST_QNAME.getLocalName() + " not found", node);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testTransactionCommitTimeout() throws Throwable {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);

        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testTransactionCommitTimeout");

            waitUntilLeader(shard);

            final FiniteDuration duration = duration("5 seconds");

            writeToStore(shard, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
            writeToStore(shard, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

            // Create and ready the 1st Tx - will timeout

            String transactionID1 = "tx1";
            shard.tell(newBatchedModifications(transactionID1, YangInstanceIdentifier.builder(
                    TestModel.OUTER_LIST_PATH).nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
                ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            // Create and ready the 2nd Tx

            String transactionID2 = "tx2";
            YangInstanceIdentifier listNodePath = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2).build();
            shard.tell(newBatchedModifications(transactionID2, listNodePath,
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

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
        dataStoreContextBuilder.shardTransactionCommitQueueCapacity(1);

        new ShardTestKit(getSystem()) {{
            final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                    "testTransactionCommitQueueCapacityExceeded");

            waitUntilLeader(shard);

            final FiniteDuration duration = duration("5 seconds");

            String transactionID1 = "tx1";
            String transactionID2 = "tx2";
            String transactionID3 = "tx3";

            // Send a BatchedModifications to start transactions and ready them.

            shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            shard.tell(newBatchedModifications(transactionID2,TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            shard.tell(newBatchedModifications(transactionID3, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

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

            final String transactionID1 = "tx1";
            final DOMStoreThreePhaseCommitCohort cohort1 = mock(DOMStoreThreePhaseCommitCohort.class, "cohort1");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort1).canCommit();
            doReturn(Futures.immediateFuture(null)).when(cohort1).abort();

            final String transactionID2 = "tx2";
            final DOMStoreThreePhaseCommitCohort cohort2 = mock(DOMStoreThreePhaseCommitCohort.class, "cohort2");
            doReturn(Futures.immediateFuture(Boolean.TRUE)).when(cohort2).canCommit();

            FiniteDuration duration = duration("5 seconds");
            final Timeout timeout = new Timeout(duration);

            ShardCommitCoordinator.CohortDecorator cohortDecorator = new ShardCommitCoordinator.CohortDecorator() {
                @Override
                public DOMStoreThreePhaseCommitCohort decorate(String transactionID,
                        DOMStoreThreePhaseCommitCohort actual) {
                    return transactionID1.equals(transactionID) ? cohort1 : cohort2;
                }
            };

            shard.underlyingActor().getCommitCoordinator().setCohortDecorator(cohortDecorator);

            // Send BatchedModifications to start and ready each transaction.

            shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

            shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), getRef());
            expectMsgClass(duration, BatchedModificationsReply.class);

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

            Await.ready(canCommitFuture, duration);

            InOrder inOrder = inOrder(cohort1, cohort2);
            inOrder.verify(cohort1).canCommit();
            inOrder.verify(cohort2).canCommit();

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testCreateSnapshot() throws Exception {
        testCreateSnapshot(true, "testCreateSnapshot");
    }

    @Test
    public void testCreateSnapshotWithNonPersistentData() throws Exception {
        testCreateSnapshot(false, "testCreateSnapshotWithNonPersistentData");
    }

    @SuppressWarnings("serial")
    public void testCreateSnapshot(final boolean persistent, final String shardActorName) throws Exception{

        final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));

        final AtomicReference<Object> savedSnapshot = new AtomicReference<>();
        class TestPersistentDataProvider extends DelegatingPersistentDataProvider {
            TestPersistentDataProvider(DataPersistenceProvider delegate) {
                super(delegate);
            }

            @Override
            public void saveSnapshot(Object o) {
                savedSnapshot.set(o);
                super.saveSnapshot(o);
            }
        }

        dataStoreContextBuilder.persistent(persistent);

        new ShardTestKit(getSystem()) {{
            class TestShard extends Shard {

                protected TestShard(ShardIdentifier name, Map<String, String> peerAddresses,
                                    DatastoreContext datastoreContext, SchemaContext schemaContext) {
                    super(name, peerAddresses, datastoreContext, schemaContext);
                    setPersistence(new TestPersistentDataProvider(super.persistence()));
                }

                @Override
                public void handleCommand(Object message) {
                    super.handleCommand(message);

                    if (message instanceof SaveSnapshotSuccess || message.equals("commit_snapshot")) {
                        latch.get().countDown();
                    }
                }

                @Override
                public RaftActorContext getRaftActorContext() {
                    return super.getRaftActorContext();
                }
            }

            Creator<Shard> creator = new Creator<Shard>() {
                @Override
                public Shard create() throws Exception {
                    return new TestShard(shardID, Collections.<String,String>emptyMap(),
                            newDatastoreContext(), SCHEMA_CONTEXT);
                }
            };

            TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                    Props.create(new DelegatingShardCreator(creator)), shardActorName);

            waitUntilLeader(shard);

            writeToStore(shard, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            NormalizedNode<?,?> expectedRoot = readStore(shard, YangInstanceIdentifier.builder().build());

            // Trigger creation of a snapshot by ensuring
            RaftActorContext raftActorContext = ((TestShard) shard.underlyingActor()).getRaftActorContext();
            raftActorContext.getSnapshotManager().capture(mock(ReplicatedLogEntry.class), -1);

            assertEquals("Snapshot saved", true, latch.get().await(5, TimeUnit.SECONDS));

            assertTrue("Invalid saved snapshot " + savedSnapshot.get(),
                    savedSnapshot.get() instanceof Snapshot);

            verifySnapshot((Snapshot)savedSnapshot.get(), expectedRoot);

            latch.set(new CountDownLatch(1));
            savedSnapshot.set(null);

            raftActorContext.getSnapshotManager().capture(mock(ReplicatedLogEntry.class), -1);

            assertEquals("Snapshot saved", true, latch.get().await(5, TimeUnit.SECONDS));

            assertTrue("Invalid saved snapshot " + savedSnapshot.get(),
                    savedSnapshot.get() instanceof Snapshot);

            verifySnapshot((Snapshot)savedSnapshot.get(), expectedRoot);

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }

        private void verifySnapshot(Snapshot snapshot, NormalizedNode<?,?> expectedRoot) {

            NormalizedNode<?, ?> actual = SerializationUtils.deserializeNormalizedNode(snapshot.getState());
            assertEquals("Root node", expectedRoot, actual);

        }};
    }

    /**
     * This test simply verifies that the applySnapShot logic will work
     * @throws ReadFailedException
     */
    @Test
    public void testInMemoryDataStoreRestore() throws ReadFailedException {
        InMemoryDOMDataStore store = new InMemoryDOMDataStore("test", MoreExecutors.sameThreadExecutor());

        store.onGlobalContextUpdated(SCHEMA_CONTEXT);

        DOMStoreWriteTransaction putTransaction = store.newWriteOnlyTransaction();
        putTransaction.write(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        commitTransaction(putTransaction);


        NormalizedNode<?, ?> expected = readStore(store);

        DOMStoreWriteTransaction writeTransaction = store.newWriteOnlyTransaction();

        writeTransaction.delete(YangInstanceIdentifier.builder().build());
        writeTransaction.write(YangInstanceIdentifier.builder().build(), expected);

        commitTransaction(writeTransaction);

        NormalizedNode<?, ?> actual = readStore(store);

        assertEquals(expected, actual);
    }

    @Test
    public void testRecoveryApplicable(){

        final DatastoreContext persistentContext = DatastoreContext.newBuilder().
                shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(true).build();

        final Props persistentProps = Shard.props(shardID, Collections.<String, String>emptyMap(),
                persistentContext, SCHEMA_CONTEXT);

        final DatastoreContext nonPersistentContext = DatastoreContext.newBuilder().
                shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(false).build();

        final Props nonPersistentProps = Shard.props(shardID, Collections.<String, String>emptyMap(),
                nonPersistentContext, SCHEMA_CONTEXT);

        new ShardTestKit(getSystem()) {{
            TestActorRef<Shard> shard1 = TestActorRef.create(getSystem(),
                    persistentProps, "testPersistence1");

            assertTrue("Recovery Applicable", shard1.underlyingActor().persistence().isRecoveryApplicable());

            shard1.tell(PoisonPill.getInstance(), ActorRef.noSender());

            TestActorRef<Shard> shard2 = TestActorRef.create(getSystem(),
                    nonPersistentProps, "testPersistence2");

            assertFalse("Recovery Not Applicable", shard2.underlyingActor().persistence().isRecoveryApplicable());

            shard2.tell(PoisonPill.getInstance(), ActorRef.noSender());

        }};

    }

    @Test
    public void testOnDatastoreContext() {
        new ShardTestKit(getSystem()) {{
            dataStoreContextBuilder.persistent(true);

            TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(), "testOnDatastoreContext");

            assertEquals("isRecoveryApplicable", true,
                    shard.underlyingActor().persistence().isRecoveryApplicable());

            waitUntilLeader(shard);

            shard.tell(dataStoreContextBuilder.persistent(false).build(), ActorRef.noSender());

            assertEquals("isRecoveryApplicable", false,
                    shard.underlyingActor().persistence().isRecoveryApplicable());

            shard.tell(dataStoreContextBuilder.persistent(true).build(), ActorRef.noSender());

            assertEquals("isRecoveryApplicable", true,
                    shard.underlyingActor().persistence().isRecoveryApplicable());

            shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }};
    }

    @Test
    public void testRegisterRoleChangeListener() throws Exception {
        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testRegisterRoleChangeListener");

                waitUntilLeader(shard);

                TestActorRef<MessageCollectorActor> listener =
                        TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class));

                shard.tell(new RegisterRoleChangeListener(), listener);

                // TODO: MessageCollectorActor exists as a test util in both the akka-raft and distributed-datastore
                // projects. Need to move it to commons as a regular utility and then we can get rid of this arbitrary
                // sleep.
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

                List<Object> allMatching = MessageCollectorActor.getAllMatching(listener, RegisterRoleChangeListenerReply.class);

                assertEquals(1, allMatching.size());
            }
        };
    }

    @Test
    public void testFollowerInitialSyncStatus() throws Exception {
        final TestActorRef<Shard> shard = TestActorRef.create(getSystem(),
                newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                "testFollowerInitialSyncStatus");

        shard.underlyingActor().onReceiveCommand(new FollowerInitialSyncUpStatus(false, "member-1-shard-inventory-operational"));

        assertEquals(false, shard.underlyingActor().getShardMBean().getFollowerInitialSyncStatus());

        shard.underlyingActor().onReceiveCommand(new FollowerInitialSyncUpStatus(true, "member-1-shard-inventory-operational"));

        assertEquals(true, shard.underlyingActor().getShardMBean().getFollowerInitialSyncStatus());

        shard.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    private void commitTransaction(final DOMStoreWriteTransaction transaction) {
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
}
