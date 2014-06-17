package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.*;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import static org.junit.Assert.*;

public class ShardTest extends AbstractActorTest{
  @Test
  public void testOnReceiveCreateTransaction() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = Props.create(Shard.class);
      final ActorRef subject = getSystem().actorOf(props, "testCreateTransaction");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new CreateTransactionChain(), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof CreateTransactionChainReply) {
                CreateTransactionChainReply reply = (CreateTransactionChainReply) in;
                return reply.getTransactionChainPath().toString();
              } else {
                throw noMatch();
              }
            }
          }.get(); // this extracts the received message

          assertTrue(out.matches("akka:\\/\\/test\\/user\\/testCreateTransaction\\/\\$.*"));
          // Will wait for the rest of the 3 seconds
          expectNoMsg();
        }


      };
    }};
  }

  @Test
  public void testOnReceiveRegisterListener() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = Props.create(Shard.class);
      final ActorRef subject = getSystem().actorOf(props, "testRegisterChangeListener");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new RegisterChangeListener(InstanceIdentifier.builder().build(), noOpDataChangeListener() , AsyncDataBroker.DataChangeScope.BASE), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof RegisterChangeListenerReply) {
                RegisterChangeListenerReply reply = (RegisterChangeListenerReply) in;
                return reply.getListenerRegistrationPath().toString();
              } else {
                throw noMatch();
              }
            }
          }.get(); // this extracts the received message

          assertTrue(out.matches("akka:\\/\\/test\\/user\\/testRegisterChangeListener\\/\\$.*"));
          // Will wait for the rest of the 3 seconds
          expectNoMsg();
        }


      };
    }};
  }

  private  AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> noOpDataChangeListener(){
    return new AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>() {
      @Override
      public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {

      }
    };
  }
}