package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShardTest extends AbstractActorTest {
    @Test
    public void testOnReceiveCreateTransactionChain() throws Exception {
        new JavaTestKit(getSystem()) {{
            final Props props = Shard.props("config");
            final ActorRef subject =
                getSystem().actorOf(props, "testCreateTransactionChain");

            new Within(duration("1 seconds")) {
                protected void run() {

                    subject.tell(new CreateTransactionChain(), getRef());

                    final String out = new ExpectMsg<String>("match hint") {
                        // do not put code outside this method, will run afterwards
                        protected String match(Object in) {
                            if (in instanceof CreateTransactionChainReply) {
                                CreateTransactionChainReply reply =
                                    (CreateTransactionChainReply) in;
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
            final Props props = Shard.props("config");
            final ActorRef subject =
                getSystem().actorOf(props, "testRegisterChangeListener");

            new Within(duration("1 seconds")) {
                protected void run() {

                    subject.tell(
                        new UpdateSchemaContext(TestModel.createTestContext()),
                        getRef());

                    subject.tell(new RegisterChangeListener(TestModel.TEST_PATH,
                        getRef().path(), AsyncDataBroker.DataChangeScope.BASE),
                        getRef());

                    final String out = new ExpectMsg<String>("match hint") {
                        // do not put code outside this method, will run afterwards
                        protected String match(Object in) {
                            if (in instanceof RegisterChangeListenerReply) {
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
                    // Will wait for the rest of the 3 seconds
                    expectNoMsg();
                }


            };
        }};
    }

    @Test
    public void testCreateTransaction(){
        new JavaTestKit(getSystem()) {{
            final Props props = Shard.props("config");
            final ActorRef subject =
                getSystem().actorOf(props, "testCreateTransaction");

            new Within(duration("1 seconds")) {
                protected void run() {

                    subject.tell(
                        new UpdateSchemaContext(TestModel.createTestContext()),
                        getRef());

                    subject.tell(new CreateTransaction("txn-1"),
                        getRef());

                    final String out = new ExpectMsg<String>("match hint") {
                        // do not put code outside this method, will run afterwards
                        protected String match(Object in) {
                            if (in instanceof CreateTransactionReply) {
                                CreateTransactionReply reply =
                                    (CreateTransactionReply) in;
                                return reply.getTransactionPath()
                                    .toString();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("Unexpected transaction path " + out,
                        "akka://test/user/testCreateTransaction/shard-txn-1",
                        out);
                    expectNoMsg();
                }


            };
        }};
    }



    private AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> noOpDataChangeListener() {
        return new AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>() {
            @Override
            public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {

            }
        };
    }
}
