package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionReply;
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
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import static org.junit.Assert.assertEquals;

public class ShardTransactionTest extends AbstractActorTest {
  private static ListeningExecutorService storeExecutor = MoreExecutors.listeningDecorator(MoreExecutors.sameThreadExecutor());

  private static final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", storeExecutor);

  static {
    store.onGlobalContextUpdated(TestModel.createTestContext());
  }

  @Test
  public void testOnReceiveReadData() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = ShardTransaction.props(store.newReadWriteTransaction());
      final ActorRef subject = getSystem().actorOf(props, "testReadData");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new ReadData(InstanceIdentifier.builder().build()), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof ReadDataReply) {
                if (((ReadDataReply) in).getNormalizedNode() != null) {
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
  public void testOnReceiveWriteData() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = ShardTransaction.props(store.newReadWriteTransaction());
      final ActorRef subject = getSystem().actorOf(props, "testWriteData");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new WriteData(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME)), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof WriteDataReply) {
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
  public void testOnReceiveMergeData() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = ShardTransaction.props(store.newReadWriteTransaction());
      final ActorRef subject = getSystem().actorOf(props, "testMergeData");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new MergeData(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME)), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof MergeDataReply) {
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
  public void testOnReceiveDeleteData() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = ShardTransaction.props(store.newReadWriteTransaction());
      final ActorRef subject = getSystem().actorOf(props, "testDeleteData");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new DeleteData(TestModel.TEST_PATH), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof DeleteDataReply) {
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
  public void testOnReceiveReadyTransaction() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = ShardTransaction.props(store.newReadWriteTransaction());
      final ActorRef subject = getSystem().actorOf(props, "testReadyTransaction");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new ReadyTransaction(), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof ReadyTransactionReply) {
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
      final Props props = ShardTransaction.props(store.newReadWriteTransaction());
      final ActorRef subject = getSystem().actorOf(props, "testCloseTransaction");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new CloseTransaction(), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof CloseTransactionReply) {
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


}