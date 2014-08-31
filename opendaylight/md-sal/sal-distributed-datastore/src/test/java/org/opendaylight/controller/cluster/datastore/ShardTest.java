package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.Snapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
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

import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class ShardTest extends AbstractActorTest {

    private static final DatastoreContext DATA_STORE_CONTEXT =
            new DatastoreContext("", null, Duration.create(10, TimeUnit.MINUTES), 5, 3, 5000, 500);

    private static final SchemaContext SCHEMA_CONTEXT = TestModel.createTestContext();

    @Before
    public void setUp() {
        System.setProperty("shard.persistent", "false");
    }

    private Props newShardProps() {
        ShardIdentifier identifier = ShardIdentifier.builder().memberName("member-1")
                .shardName("inventory").type("config").build();

        return Shard.props(identifier, Collections.<ShardIdentifier,String>emptyMap(),
                DATA_STORE_CONTEXT, SCHEMA_CONTEXT);
    }

    @Test
    public void testOnReceiveRegisterListener() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef subject = getSystem().actorOf(newShardProps(), "testRegisterChangeListener");

            subject.tell(new UpdateSchemaContext(SchemaContextHelper.full()), getRef());

            subject.tell(new RegisterChangeListener(TestModel.TEST_PATH,
                    getRef().path(), AsyncDataBroker.DataChangeScope.BASE), getRef());

            EnableNotification enable = expectMsgClass(duration("3 seconds"), EnableNotification.class);
            assertEquals("isEnabled", false, enable.isEnabled());

            RegisterChangeListenerReply reply = expectMsgClass(duration("3 seconds"),
                    RegisterChangeListenerReply.class);
            assertTrue(reply.getListenerRegistrationPath().toString().matches(
                    "akka:\\/\\/test\\/user\\/testRegisterChangeListener\\/\\$.*"));
        }};
    }

    @Test
    public void testCreateTransaction(){
        new ShardTestKit(getSystem()) {{
            ActorRef subject = getSystem().actorOf(newShardProps(), "testCreateTransaction");

            waitUntilLeader(subject);

            subject.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            subject.tell(new CreateTransaction("txn-1",
                    TransactionProxy.TransactionType.READ_ONLY.ordinal() ).toSerializable(), getRef());

            CreateTransactionReply reply = expectMsgClass(duration("3 seconds"),
                    CreateTransactionReply.class);

            String path = reply.getTransactionActorPath().toString();
            assertTrue("Unexpected transaction path " + path,
                    path.contains("akka://test/user/testCreateTransaction/shard-txn-1"));
            expectNoMsg();
        }};
    }

    @Test
    public void testCreateTransactionOnChain(){
        new ShardTestKit(getSystem()) {{
            final ActorRef subject = getSystem().actorOf(newShardProps(), "testCreateTransactionOnChain");

            waitUntilLeader(subject);

            subject.tell(new CreateTransaction("txn-1",
                    TransactionProxy.TransactionType.READ_ONLY.ordinal() , "foobar").toSerializable(),
                    getRef());

            CreateTransactionReply reply = expectMsgClass(duration("3 seconds"),
                    CreateTransactionReply.class);

            String path = reply.getTransactionActorPath().toString();
            assertTrue("Unexpected transaction path " + path,
                    path.contains("akka://test/user/testCreateTransactionOnChain/shard-txn-1"));
            expectNoMsg();
        }};
    }

    @Test
    public void testPeerAddressResolved(){
        new JavaTestKit(getSystem()) {{
            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            Props props = Shard.props(identifier,
                    Collections.<ShardIdentifier, String>singletonMap(identifier, null),
                    DATA_STORE_CONTEXT, SCHEMA_CONTEXT);
            final ActorRef subject = getSystem().actorOf(props, "testPeerAddressResolved");

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new PeerAddressResolved(identifier, "akka://foobar"),
                        getRef());

                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testApplySnapshot() throws ExecutionException, InterruptedException {
        TestActorRef<Shard> ref = TestActorRef.create(getSystem(), newShardProps());

        NormalizedNodeToNodeCodec codec =
            new NormalizedNodeToNodeCodec(SCHEMA_CONTEXT);

        ref.underlyingActor().writeToStore(TestModel.TEST_PATH, ImmutableNodes.containerNode(
                TestModel.TEST_QNAME));

        YangInstanceIdentifier root = YangInstanceIdentifier.builder().build();
        NormalizedNode<?,?> expected = ref.underlyingActor().readStore(root);

        NormalizedNodeMessages.Container encode = codec.encode(root, expected);

        ApplySnapshot applySnapshot = new ApplySnapshot(Snapshot.create(
                encode.getNormalizedNode().toByteString().toByteArray(),
                Collections.<ReplicatedLogEntry>emptyList(), 1, 2, 3, 4));

        ref.underlyingActor().onReceiveCommand(applySnapshot);

        NormalizedNode<?,?> actual = ref.underlyingActor().readStore(root);

        assertEquals(expected, actual);
    }

    @Test
    public void testApplyState() throws Exception {

        TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps());

        NormalizedNode<?, ?> node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        MutableCompositeModification compMod = new MutableCompositeModification();
        compMod.addModification(new WriteModification(TestModel.TEST_PATH, node, SCHEMA_CONTEXT));
        Payload payload = new CompositeModificationPayload(compMod.toSerializable());
        ApplyState applyState = new ApplyState(null, "test",
                new ReplicatedLogImplEntry(1, 2, payload));

        shard.underlyingActor().onReceiveCommand(applyState);

        NormalizedNode<?,?> actual = shard.underlyingActor().readStore(TestModel.TEST_PATH);
        assertEquals("Applied state", node, actual);
    }

    @Test
    public void testRecovery() throws Exception {

        TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps(), "testRecovery");

        shard.underlyingActor().writeToStore(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        NormalizedNode<?, ?> root = shard.underlyingActor().readStore(
                YangInstanceIdentifier.builder().build());

        Snapshot snapshot = Snapshot.create(new NormalizedNodeToNodeCodec(SCHEMA_CONTEXT).encode(
                YangInstanceIdentifier.builder().build(), root).getNormalizedNode().toByteString().toByteArray(),
                Collections.<ReplicatedLogEntry>emptyList(), 0, 1, -1, -1);
        shard.underlyingActor().onReceiveRecover(new SnapshotOffer(null, snapshot));

        shard.underlyingActor().onReceiveRecover(new ReplicatedLogImplEntry(0, 1, newPayload(
                  new WriteModification(TestModel.OUTER_LIST_PATH,
                          ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(),
                          SCHEMA_CONTEXT))));

        int nListEntries = 10;
        Set<Integer> listEntryKeys = new HashSet<>();
        for(int i = 1; i <= nListEntries; i++) {
            listEntryKeys.add(Integer.valueOf(i));
            YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i).build();
            Modification mod = new MergeModification(path,
                    ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i),
                    SCHEMA_CONTEXT);
            shard.underlyingActor().onReceiveRecover(new ReplicatedLogImplEntry(i, 1,
                    newPayload(mod )));
        }

        shard.underlyingActor().onReceiveRecover(RecoveryCompleted.getInstance());

        NormalizedNode<?, ?> outerList = shard.underlyingActor().readStore(TestModel.OUTER_LIST_PATH);
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
    }

    private CompositeModificationPayload newPayload(Modification... mods) {
        MutableCompositeModification compMod = new MutableCompositeModification();
        for(Modification mod: mods) {
            compMod.addModification(mod);
        }

        return new CompositeModificationPayload(compMod.toSerializable());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForwardedCommitTransactionWithPersistence() throws IOException {
        System.setProperty("shard.persistent", "true");

        new ShardTestKit(getSystem()) {{
            TestActorRef<Shard> shard = TestActorRef.create(getSystem(), newShardProps());

            waitUntilLeader(shard);

            NormalizedNode<?, ?> node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

            DOMStoreThreePhaseCommitCohort cohort = mock(DOMStoreThreePhaseCommitCohort.class);
            doReturn(Futures.immediateFuture(null)).when(cohort).commit();

            MutableCompositeModification modification = new MutableCompositeModification();
            modification.addModification(new WriteModification(TestModel.TEST_PATH, node,
                    SCHEMA_CONTEXT));

            shard.tell(new ForwardedCommitTransaction(cohort, modification), getRef());

            expectMsgClass(duration("5 seconds"), CommitTransactionReply.SERIALIZABLE_CLASS);

            verify(cohort).commit();

            assertEquals("Last log index", 0, shard.underlyingActor().getShardMBean().getLastLogIndex());
        }};
    }

    @Test
    public void testCreateSnapshot() throws IOException, InterruptedException {
        new ShardTestKit(getSystem()) {{
            final ActorRef subject = getSystem().actorOf(newShardProps(), "testCreateSnapshot");

            waitUntilLeader(subject);

            subject.tell(new CaptureSnapshot(-1,-1,-1,-1), getRef());

            waitForLogMessage(Logging.Info.class, subject, "CaptureSnapshotReply received by actor");

            subject.tell(new CaptureSnapshot(-1,-1,-1,-1), getRef());

            waitForLogMessage(Logging.Info.class, subject, "CaptureSnapshotReply received by actor");
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

    private static class ShardTestKit extends JavaTestKit {

        private ShardTestKit(ActorSystem actorSystem) {
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

        protected void waitUntilLeader(ActorRef subject) {
            waitForLogMessage(Logging.Info.class, subject,
                    "Switching from state Candidate to Leader");
        }
    }
}
