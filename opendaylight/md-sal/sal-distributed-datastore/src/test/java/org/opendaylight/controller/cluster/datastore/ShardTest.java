package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.testkit.JavaTestKit;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShardTest extends AbstractActorTest {

    private static final ShardContext shardContext = new ShardContext();

    @Test
    public void testOnReceiveCreateTransactionChain() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, shardContext);
            final ActorRef subject =
                getSystem().actorOf(props, "testCreateTransactionChain");


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

                    subject.tell(new CreateTransactionChain().toSerializable(), getRef());

                    final String out = new ExpectMsg<String>(duration("3 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(CreateTransactionChainReply.SERIALIZABLE_CLASS)){
                                CreateTransactionChainReply reply =
                                    CreateTransactionChainReply.fromSerializable(getSystem(),in);
                                return reply.getTransactionChainPath()
                                    .toString();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("Unexpected transaction path " + out,
                        "akka://test/user/testCreateTransactionChain/$a",
                        out);

                    expectNoMsg();
                }


            };
        }};
    }

    @Test
    public void testOnReceiveRegisterListener() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, shardContext);
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

            final Props props = Shard.props(identifier, Collections.EMPTY_MAP, shardContext);
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
    public void testPeerAddressResolved(){
        new JavaTestKit(getSystem()) {{
            Map<ShardIdentifier, String> peerAddresses = new HashMap<>();

            final ShardIdentifier identifier =
                ShardIdentifier.builder().memberName("member-1")
                    .shardName("inventory").type("config").build();

            peerAddresses.put(identifier, null);
            final Props props = Shard.props(identifier, peerAddresses, shardContext);
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

    private AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> noOpDataChangeListener() {
        return new AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
            @Override
            public void onDataChanged(
                AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {

            }
        };
    }
}
