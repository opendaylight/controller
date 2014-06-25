package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistrationReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import static org.junit.Assert.assertEquals;

public class DataChangeListenerRegistrationTest extends AbstractActorTest {
  private static ListeningExecutorService storeExecutor = MoreExecutors.listeningDecorator(MoreExecutors.sameThreadExecutor());

  private static final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", storeExecutor);

  static {
    store.onGlobalContextUpdated(TestModel.createTestContext());
  }


  @Test
  public void testOnReceiveCloseListenerRegistration() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = DataChangeListenerRegistration.props(store
          .registerChangeListener(TestModel.TEST_PATH, noOpDataChangeListener(),
              AsyncDataBroker.DataChangeScope.BASE));
      final ActorRef subject = getSystem().actorOf(props, "testCloseListenerRegistration");

      new Within(duration("1 seconds")) {
        protected void run() {

          subject.tell(new CloseDataChangeListenerRegistration(), getRef());

          final String out = new ExpectMsg<String>("match hint") {
            // do not put code outside this method, will run afterwards
            protected String match(Object in) {
              if (in instanceof CloseDataChangeListenerRegistrationReply) {
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

  private  AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> noOpDataChangeListener(){
    return new AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>() {
      @Override
      public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {

      }
    };
  }

}
