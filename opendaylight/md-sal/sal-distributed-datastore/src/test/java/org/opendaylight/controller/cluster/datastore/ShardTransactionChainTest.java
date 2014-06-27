package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;

import static org.junit.Assert.assertEquals;

public class ShardTransactionChainTest extends AbstractActorTest {

  private static ListeningExecutorService storeExecutor = MoreExecutors.listeningDecorator(MoreExecutors.sameThreadExecutor());

  private static final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", storeExecutor);

  static {
    store.onGlobalContextUpdated(TestModel.createTestContext());
  }
  @Test
  public void testOnReceiveCreateTransaction() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = ShardTransactionChain.props(store.createTransactionChain());
      final ActorRef subject = getSystem().actorOf(props, "testCreateTransaction");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new CreateTransaction("txn-1"), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof CreateTransactionReply) {
                return ((CreateTransactionReply) in).getTransactionPath().toString();
              } else {
                throw noMatch();
              }
            }
          }.get(); // this extracts the received message

            assertEquals("Unexpected transaction path " + out,
                "akka://test/user/testCreateTransaction/shard-txn-1",
                out);

            // Will wait for the rest of the 3 seconds
          expectNoMsg();
        }


      };
    }};
  }

  @Test
  public void testOnReceiveCloseTransactionChain() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = ShardTransactionChain.props(store.createTransactionChain());
      final ActorRef subject = getSystem().actorOf(props, "testCloseTransactionChain");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new CloseTransactionChain(), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof CloseTransactionChainReply) {
                return "match";
              } else {
                throw noMatch();
              }
            }
          }.get(); // this extracts the received message

          assertEquals("match", out);
          // Will wait for the rest of the 3 seconds
          expectNoMsg();
        }


      };
    }};
  }
}
