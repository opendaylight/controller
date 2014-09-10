package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShardTest extends AbstractActorTest {

    private static final DatastoreContext DATA_STORE_CONTEXT = new DatastoreContext();

    @Test
    public void testOnReceiveRegisterListener() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, DATA_STORE_CONTEXT, TestModel.createTestContext());
            final ActorRef subject =
                getSystem().actorOf(props, "testRegisterChangeListener");

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new UpdateSchemaContext(SchemaContextHelper.full()),
                        getRef());

                    subject.tell(new RegisterChangeListener(TestModel.TEST_PATH,
                        getRef().path(), AsyncDataBroker.DataChangeScope.BASE),
                        getRef());

                    final Boolean notificationEnabled = new ExpectMsg<Boolean>(
                                                   duration("3 seconds"), "enable notification") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected Boolean match(Object in) {
                            if(in instanceof EnableNotification){
                                return ((EnableNotification) in).isEnabled();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertFalse(notificationEnabled);

                    final String out = new ExpectMsg<String>(duration("3 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(RegisterChangeListenerReply.class)) {
                                RegisterChangeListenerReply reply =
                                    (RegisterChangeListenerReply) in;
                                return reply.getListenerRegistrationPath()
                                    .toString();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue(out.matches(
                        "akka:\\/\\/test\\/user\\/testRegisterChangeListener\\/\\$.*"));
                }


            };
        }};
    }

    @Test
    public void testCreateTransaction(){
        new JavaTestKit(getSystem()) {{
            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, DATA_STORE_CONTEXT, TestModel.createTestContext());
            final ActorRef subject =
                getSystem().actorOf(props, "testCreateTransaction");

            // Wait for a specific log message to show up
            final boolean result =
                new JavaTestKit.EventFilter<Boolean>(Logging.Info.class
                ) {
                    @Override
                    protected Boolean run() {
                        return true;
                    }
                }.from(subject.path().toString())
                    .message("Switching from state Candidate to Leader")
                    .occurrences(1).exec();

            Assert.assertEquals(true, result);

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new UpdateSchemaContext(TestModel.createTestContext()),
                        getRef());

                    subject.tell(new CreateTransaction("txn-1", TransactionProxy.TransactionType.READ_ONLY.ordinal() ).toSerializable(),
                        getRef());

                    final String out = new ExpectMsg<String>(duration("3 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in instanceof CreateTransactionReply) {
                                CreateTransactionReply reply =
                                    (CreateTransactionReply) in;
                                return reply.getTransactionActorPath()
                                    .toString();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue("Unexpected transaction path " + out,
                        out.contains("akka://test/user/testCreateTransaction/shard-txn-1"));
                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testCreateTransactionOnChain(){
        new JavaTestKit(getSystem()) {{
            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, DATA_STORE_CONTEXT, TestModel.createTestContext());
            final ActorRef subject =
                getSystem().actorOf(props, "testCreateTransactionOnChain");

            // Wait for a specific log message to show up
            final boolean result =
                new JavaTestKit.EventFilter<Boolean>(Logging.Info.class
                ) {
                    @Override
                    protected Boolean run() {
                        return true;
                    }
                }.from(subject.path().toString())
                    .message("Switching from state Candidate to Leader")
                    .occurrences(1).exec();

            Assert.assertEquals(true, result);

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new UpdateSchemaContext(TestModel.createTestContext()),
                        getRef());

                    subject.tell(new CreateTransaction("txn-1", TransactionProxy.TransactionType.READ_ONLY.ordinal() , "foobar").toSerializable(),
                        getRef());

                    final String out = new ExpectMsg<String>(duration("3 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in instanceof CreateTransactionReply) {
                                CreateTransactionReply reply =
                                    (CreateTransactionReply) in;
                                return reply.getTransactionActorPath()
                                    .toString();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue("Unexpected transaction path " + out,
                        out.contains("akka://test/user/testCreateTransactionOnChain/shard-txn-1"));
                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testPeerAddressResolved(){
        new JavaTestKit(getSystem()) {{
            Map<ShardIdentifier, String> peerAddresses = new HashMap<>();

            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            peerAddresses.put(identifier, null);
            final Props props = Shard.props(identifier, peerAddresses, DATA_STORE_CONTEXT, TestModel.createTestContext());
            final ActorRef subject =
                getSystem().actorOf(props, "testPeerAddressResolved");

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
        Map<ShardIdentifier, String> peerAddresses = new HashMap<>();

        final ShardIdentifier identifier =
            ShardIdentifier.builder().memberName("member-1")
                .shardName("inventory").type("config").build();

        peerAddresses.put(identifier, null);
        final Props props = Shard.props(identifier, peerAddresses, DATA_STORE_CONTEXT, TestModel.createTestContext());

        TestActorRef<Shard> ref = TestActorRef.create(getSystem(), props);

        ref.underlyingActor().updateSchemaContext(TestModel.createTestContext());

        NormalizedNodeToNodeCodec codec =
            new NormalizedNodeToNodeCodec(TestModel.createTestContext());

        ref.underlyingActor().writeToStore(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        NormalizedNode expected = ref.underlyingActor().readStore();

        NormalizedNodeMessages.Container encode = codec
            .encode(YangInstanceIdentifier.builder().build(), expected);


        ref.underlyingActor().applySnapshot(encode.getNormalizedNode().toByteString());

        NormalizedNode actual = ref.underlyingActor().readStore();

        assertEquals(expected, actual);
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

    }

    @Test
    public void testCreateSnapshot() throws IOException, InterruptedException {
        new ShardTestKit(getSystem()) {{
            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, DATA_STORE_CONTEXT, TestModel.createTestContext());
            final ActorRef subject =
                getSystem().actorOf(props, "testCreateSnapshot");

            // Wait for a specific log message to show up
            this.waitForLogMessage(Logging.Info.class, subject, "Switching from state Candidate to Leader");


            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new UpdateSchemaContext(TestModel.createTestContext()),
                        getRef());

                    subject.tell(new CaptureSnapshot(-1,-1,-1,-1),
                        getRef());

                    waitForLogMessage(Logging.Debug.class, subject, "CaptureSnapshotReply received by actor");
                }
            };

            Thread.sleep(2000);
            deletePersistenceFiles();
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

        store.onGlobalContextUpdated(TestModel.createTestContext());

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
}
