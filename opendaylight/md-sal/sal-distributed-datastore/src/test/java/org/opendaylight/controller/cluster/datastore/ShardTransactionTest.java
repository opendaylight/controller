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
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import scala.concurrent.duration.Duration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
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

    private ShardContext shardContext = new ShardContext();

    @BeforeClass
    public static void staticSetup() {
        store.onGlobalContextUpdated(testSchemaContext);
    }

    @Test
    public void testOnReceiveReadData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                    testSchemaContext, shardContext);
            final ActorRef subject = getSystem().actorOf(props, "testReadData");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new ReadData(YangInstanceIdentifier.builder().build()).toSerializable(),
                        getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(ReadDataReply.SERIALIZABLE_CLASS)) {
                              if (ReadDataReply.fromSerializable(testSchemaContext,YangInstanceIdentifier.builder().build(), in)
                                  .getNormalizedNode()!= null) {
                                    return "match";
                                }
                                return null;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    expectNoMsg();
                }


            };
        }};
    }

    @Test
    public void testOnReceiveReadDataWhenDataNotFound() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props( store.newReadOnlyTransaction(), shard,
                    testSchemaContext, shardContext);
            final ActorRef subject = getSystem().actorOf(props, "testReadDataWhenDataNotFound");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new ReadData(TestModel.TEST_PATH).toSerializable(),
                        getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(ReadDataReply.SERIALIZABLE_CLASS)) {
                                if (ReadDataReply.fromSerializable(testSchemaContext,TestModel.TEST_PATH, in)
                                    .getNormalizedNode()
                                    == null) {
                                    return "match";
                                }
                                return null;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    expectNoMsg();
                }


            };
        }};
    }

    @Test
    public void testOnReceiveDataExistsPositive() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                    testSchemaContext, shardContext);
            final ActorRef subject = getSystem().actorOf(props, "testDataExistsPositive");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new DataExists(YangInstanceIdentifier.builder().build()).toSerializable(),
                        getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(DataExistsReply.SERIALIZABLE_CLASS)) {
                                if (DataExistsReply.fromSerializable(in)
                                    .exists()) {
                                    return "match";
                                }
                                return null;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    expectNoMsg();
                }


            };
        }};
    }

    @Test
    public void testOnReceiveDataExistsNegative() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                    testSchemaContext, shardContext);
            final ActorRef subject = getSystem().actorOf(props, "testDataExistsNegative");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new DataExists(TestModel.TEST_PATH).toSerializable(),
                        getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(DataExistsReply.SERIALIZABLE_CLASS)) {
                                if (!DataExistsReply.fromSerializable(in)
                                    .exists()) {
                                    return "match";
                                }
                                return null;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    expectNoMsg();
                }


            };
        }};
    }

    private void assertModification(final ActorRef subject,
        final Class<? extends Modification> modificationType) {
        new JavaTestKit(getSystem()) {{
            new Within(duration("1 seconds")) {
                @Override
                protected void run() {
                    subject
                        .tell(new ShardTransaction.GetCompositedModification(),
                            getRef());

                    final CompositeModification compositeModification =
                        new ExpectMsg<CompositeModification>(duration("1 seconds"), "match hint") {
                            // do not put code outside this method, will run afterwards
                            @Override
                            protected CompositeModification match(Object in) {
                                if (in instanceof ShardTransaction.GetCompositeModificationReply) {
                                    return ((ShardTransaction.GetCompositeModificationReply) in)
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
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props(store.newWriteOnlyTransaction(), shard,
                    testSchemaContext, shardContext);
            final ActorRef subject =
                getSystem().actorOf(props, "testWriteData");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new WriteData(TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME), TestModel.createTestContext()).toSerializable(),
                        getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(WriteDataReply.SERIALIZABLE_CLASS)) {
                                return "match";
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    assertModification(subject, WriteModification.class);
                    expectNoMsg();
                }


            };
        }};
    }

    @Test
    public void testOnReceiveMergeData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                    testSchemaContext, shardContext);
            final ActorRef subject =
                getSystem().actorOf(props, "testMergeData");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new MergeData(TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME), testSchemaContext).toSerializable(),
                        getRef());

                    final String out = new ExpectMsg<String>(duration("500 milliseconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(MergeDataReply.SERIALIZABLE_CLASS)) {
                                return "match";
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    assertModification(subject, MergeModification.class);

                    expectNoMsg();
                }


            };
        }};
    }

    @Test
    public void testOnReceiveDeleteData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props( store.newWriteOnlyTransaction(), shard,
                    testSchemaContext, shardContext);
            final ActorRef subject =
                getSystem().actorOf(props, "testDeleteData");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new DeleteData(TestModel.TEST_PATH).toSerializable(), getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(DeleteDataReply.SERIALIZABLE_CLASS)) {
                                return "match";
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    assertModification(subject, DeleteModification.class);
                    expectNoMsg();
                }


            };
        }};
    }


    @Test
    public void testOnReceiveReadyTransaction() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props( store.newReadWriteTransaction(), shard,
                    testSchemaContext, shardContext);
            final ActorRef subject =
                getSystem().actorOf(props, "testReadyTransaction");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new ReadyTransaction().toSerializable(), getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(ReadyTransactionReply.SERIALIZABLE_CLASS)) {
                                return "match";
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    expectNoMsg();
                }


            };
        }};

    }

    @Test
    public void testOnReceiveCloseTransaction() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                    testSchemaContext, shardContext);
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
        final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                Collections.EMPTY_MAP, new ShardContext()));
        final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                testSchemaContext, shardContext);
        final TestActorRef subject = TestActorRef.apply(props,getSystem());

        subject.receive(new DeleteData(TestModel.TEST_PATH).toSerializable(), ActorRef.noSender());
    }

    @Test
    public void testShardTransactionInactivity() {

        shardContext = new ShardContext(InMemoryDOMDataStoreConfigProperties.getDefault(),
                Duration.create(500, TimeUnit.MILLISECONDS));

        new JavaTestKit(getSystem()) {{
            final ActorRef shard = getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
                    Collections.EMPTY_MAP, new ShardContext()));
            final Props props = ShardTransaction.props(store.newReadWriteTransaction(), shard,
                    testSchemaContext, shardContext);
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
