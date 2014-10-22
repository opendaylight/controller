package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.exceptions.UnknownMessageException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.DeleteDataReply;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.MergeDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.cluster.datastore.modification.CompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.Duration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ShardTransactionTest extends AbstractActorTest {
    private static ListeningExecutorService storeExecutor =
        MoreExecutors.listeningDecorator(MoreExecutors.sameThreadExecutor());

    private static final InMemoryDOMDataStore store =
        new InMemoryDOMDataStore("OPER", storeExecutor, MoreExecutors.sameThreadExecutor());

    private static final SchemaContext testSchemaContext = TestModel.createTestContext();

    private static final ShardIdentifier SHARD_IDENTIFIER =
        ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("config").build();

    private DatastoreContext datastoreContext = DatastoreContext.newBuilder().build();

    private final ShardStats shardStats = new ShardStats(SHARD_IDENTIFIER.toString(), "DataStore");

    @BeforeClass
    public static void staticSetup() {
        store.onGlobalContextUpdated(testSchemaContext);
    }

    private ActorRef createShard(){
        return getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
            Collections.EMPTY_MAP, datastoreContext, TestModel.createTestContext()));
    }

    @Test
    public void testOnReceiveReadData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject = getSystem().actorOf(props, "testReadData");

            //serialized read
            subject.tell(new ReadData(YangInstanceIdentifier.builder().build()).toSerializable(),
                getRef());

            ShardTransactionMessages.ReadDataReply replySerialized =
                expectMsgClass(duration("5 seconds"), ReadDataReply.SERIALIZABLE_CLASS);

            assertNotNull(ReadDataReply.fromSerializable(
                testSchemaContext,YangInstanceIdentifier.builder().build(), replySerialized)
                .getNormalizedNode());

            // unserialized read
            subject.tell(new ReadData(YangInstanceIdentifier.builder().build()),getRef());

            ReadDataReply reply = expectMsgClass(duration("5 seconds"), ReadDataReply.class);

            assertNotNull(reply.getNormalizedNode());

            expectNoMsg();

        }};
    }

    @Test
    public void testOnReceiveReadDataWhenDataNotFound() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props( store.newReadOnlyTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject = getSystem().actorOf(props, "testReadDataWhenDataNotFound");

            subject.tell(
                new ReadData(TestModel.TEST_PATH).toSerializable(),
                getRef());

            ShardTransactionMessages.ReadDataReply replySerialized =
                expectMsgClass(duration("5 seconds"), ReadDataReply.SERIALIZABLE_CLASS);

            assertTrue(ReadDataReply.fromSerializable(
                testSchemaContext, TestModel.TEST_PATH, replySerialized)
                .getNormalizedNode() == null);

            // unserialized read
            subject.tell(new ReadData(TestModel.TEST_PATH),getRef());

            ReadDataReply reply = expectMsgClass(duration("5 seconds"), ReadDataReply.class);

            assertTrue(reply.getNormalizedNode() == null);

            expectNoMsg();
        }};
    }

    @Test
    public void testOnReceiveDataExistsPositive() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject = getSystem().actorOf(props, "testDataExistsPositive");

            subject.tell(
                new DataExists(YangInstanceIdentifier.builder().build()).toSerializable(),
                getRef());

            ShardTransactionMessages.DataExistsReply replySerialized =
                expectMsgClass(duration("5 seconds"), ShardTransactionMessages.DataExistsReply.class);

            assertTrue(DataExistsReply.fromSerializable(replySerialized).exists());

            // unserialized read
            subject.tell(new DataExists(YangInstanceIdentifier.builder().build()),getRef());

            DataExistsReply reply = expectMsgClass(duration("5 seconds"), DataExistsReply.class);

            assertTrue(reply.exists());

            expectNoMsg();
        }};
    }

    @Test
    public void testOnReceiveDataExistsNegative() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject = getSystem().actorOf(props, "testDataExistsNegative");

            subject.tell(
                new DataExists(TestModel.TEST_PATH).toSerializable(),
                getRef());

            ShardTransactionMessages.DataExistsReply replySerialized =
                expectMsgClass(duration("5 seconds"), ShardTransactionMessages.DataExistsReply.class);

            assertFalse(DataExistsReply.fromSerializable(replySerialized).exists());

            // unserialized read
            subject.tell(new DataExists(TestModel.TEST_PATH),getRef());

            DataExistsReply reply = expectMsgClass(duration("5 seconds"), DataExistsReply.class);

            assertFalse(reply.exists());

            expectNoMsg();

        }};
    }

    private void assertModification(final ActorRef subject,
        final Class<? extends Modification> modificationType) {
        new JavaTestKit(getSystem()) {{
            new Within(duration("3 seconds")) {
                @Override
                protected void run() {
                    subject
                        .tell(new ShardWriteTransaction.GetCompositedModification(),
                            getRef());

                    final CompositeModification compositeModification =
                        new ExpectMsg<CompositeModification>(duration("3 seconds"), "match hint") {
                            // do not put code outside this method, will run afterwards
                            @Override
                            protected CompositeModification match(Object in) {
                                if (in instanceof ShardWriteTransaction.GetCompositeModificationReply) {
                                    return ((ShardWriteTransaction.GetCompositeModificationReply) in)
                                        .getModification();
                                } else {
                                    throw noMatch();
                                }
                            }
                        }.get(); // this extracts the received message

                    assertTrue(
                        compositeModification.getModifications().size() == 1);
                    assertEquals(modificationType,
                        compositeModification.getModifications().get(0)
                            .getClass());

                }
            };
        }};
    }

    @Test
    public void testOnReceiveWriteData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props(store.newWriteOnlyTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject =
                getSystem().actorOf(props, "testWriteData");

            subject.tell(new WriteData(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), TestModel.createTestContext()).toSerializable(),
                getRef());

            ShardTransactionMessages.WriteDataReply replySerialized =
                expectMsgClass(duration("5 seconds"), ShardTransactionMessages.WriteDataReply.class);

            assertModification(subject, WriteModification.class);

            //unserialized write
            subject.tell(new WriteData(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME),
                TestModel.createTestContext()),
                getRef());

            expectMsgClass(duration("5 seconds"), WriteDataReply.class);

            expectNoMsg();
        }};
    }

    @Test
    public void testOnReceiveMergeData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject =
                getSystem().actorOf(props, "testMergeData");

            subject.tell(new MergeData(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), testSchemaContext).toSerializable(),
                getRef());

            ShardTransactionMessages.MergeDataReply replySerialized =
                expectMsgClass(duration("5 seconds"), ShardTransactionMessages.MergeDataReply.class);

            assertModification(subject, MergeModification.class);

            //unserialized merge
            subject.tell(new MergeData(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), testSchemaContext),
                getRef());

            expectMsgClass(duration("5 seconds"), MergeDataReply.class);

            expectNoMsg();
        }};
    }

    @Test
    public void testOnReceiveDeleteData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props( store.newWriteOnlyTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject =
                getSystem().actorOf(props, "testDeleteData");

            subject.tell(new DeleteData(TestModel.TEST_PATH).toSerializable(), getRef());

            ShardTransactionMessages.DeleteDataReply replySerialized =
                expectMsgClass(duration("5 seconds"), ShardTransactionMessages.DeleteDataReply.class);

            assertModification(subject, DeleteModification.class);

            //unserialized merge
            subject.tell(new DeleteData(TestModel.TEST_PATH), getRef());

            expectMsgClass(duration("5 seconds"), DeleteDataReply.class);

            expectNoMsg();

        }};
    }


    @Test
    public void testOnReceiveReadyTransaction() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props( store.newReadWriteTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject =
                getSystem().actorOf(props, "testReadyTransaction");

            subject.tell(new ReadyTransaction().toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), ReadyTransactionReply.SERIALIZABLE_CLASS);

            expectNoMsg();


        }};

        // test
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props( store.newReadWriteTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject =
                getSystem().actorOf(props, "testReadyTransaction2");

            subject.tell(new ReadyTransaction(), getRef());

            expectMsgClass(duration("5 seconds"), ReadyTransactionReply.class);

            expectNoMsg();
        }};

    }

    @Test
    public void testOnReceiveCloseTransaction() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject =
                getSystem().actorOf(props, "testCloseTransaction");

            watch(subject);

            new Within(duration("6 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new CloseTransaction().toSerializable(), getRef());

                    final String out = new ExpectMsg<String>(duration("3 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            System.out.println("!!!IN match 1: "+(in!=null?in.getClass():"NULL"));
                            if (in.getClass().equals(CloseTransactionReply.SERIALIZABLE_CLASS)) {
                                return "match";
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    final String termination = new ExpectMsg<String>(duration("3 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            System.out.println("!!!IN match 2: "+(in!=null?in.getClass():"NULL"));
                            if (in instanceof Terminated) {
                                return "match";
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", termination);
                }
            };
        }};
    }

    @Test(expected=UnknownMessageException.class)
    public void testNegativePerformingWriteOperationOnReadTransaction() throws Exception {
        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn");
        final TestActorRef subject = TestActorRef.apply(props,getSystem());

        subject.receive(new DeleteData(TestModel.TEST_PATH).toSerializable(), ActorRef.noSender());
    }

    @Test
    public void testShardTransactionInactivity() {

        datastoreContext = DatastoreContext.newBuilder().shardTransactionIdleTimeout(
                Duration.create(500, TimeUnit.MILLISECONDS)).build();

        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();
            final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                    testSchemaContext, datastoreContext, shardStats, "txn");
            final ActorRef subject =
                getSystem().actorOf(props, "testShardTransactionInactivity");

            watch(subject);

            // The shard Tx actor should receive a ReceiveTimeout message and self-destruct.

            final String termination = new ExpectMsg<String>(duration("3 seconds"), "match hint") {
                // do not put code outside this method, will run afterwards
                @Override
                protected String match(Object in) {
                    if (in instanceof Terminated) {
                        return "match";
                    } else {
                        throw noMatch();
                    }
                }
            }.get(); // this extracts the received message

            assertEquals("match", termination);
        }};
    }
}
