package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.md.cluster.datastore.model.CompositeModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DataChangeListenerTest extends AbstractActorTest {

    private static class MockDataChangedEvent implements AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> {
       Map<YangInstanceIdentifier,NormalizedNode<?,?>> createdData = new HashMap<>();
       Map<YangInstanceIdentifier,NormalizedNode<?,?>> updatedData = new HashMap<>();
       Map<YangInstanceIdentifier,NormalizedNode<?,?>> originalData = new HashMap<>();



        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
            createdData.put(CompositeModel.FAMILY_PATH, CompositeModel.createFamily());
            return createdData;
        }

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
            updatedData.put(CompositeModel.FAMILY_PATH, CompositeModel.createFamily());
            return updatedData;

        }

        @Override
        public Set<YangInstanceIdentifier> getRemovedPaths() {
               Set<YangInstanceIdentifier>ids = new HashSet();
               ids.add( CompositeModel.TEST_PATH);
              return ids;
        }

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getOriginalData() {
          originalData.put(CompositeModel.FAMILY_PATH, CompositeModel.createFamily());
          return originalData;
        }

        @Override public NormalizedNode<?, ?> getOriginalSubtree() {


          return originalData.put(CompositeModel.FAMILY_PATH, CompositeModel.createFamily());
        }

        @Override public NormalizedNode<?, ?> getUpdatedSubtree() {

          //fixme: need to have some valid data here
          return originalData.put(CompositeModel.FAMILY_PATH, CompositeModel.createFamily());
        }
    }

    private class MockDataChangeListener implements AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> {
        private boolean gotIt = false;
        private   AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change;

        @Override public void onDataChanged(
            AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
            gotIt = true;this.change=change;
        }

        public boolean gotIt() {
            return gotIt;
        }
        public  AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> getChange(){
          return change;
        }
    }

    @Test
    public void testDataChangedWhenNotificationsAreEnabled(){
        new JavaTestKit(getSystem()) {{
            final MockDataChangeListener listener = new MockDataChangeListener();
            final Props props = DataChangeListener.props(listener);
            final ActorRef subject =
                getSystem().actorOf(props, "testDataChangedNotificationsEnabled");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    // Let the DataChangeListener know that notifications should
                    // be enabled
                    subject.tell(new EnableNotification(true), getRef());

                    subject.tell(
                        new DataChanged(CompositeModel.createTestContext(),new MockDataChangedEvent()),
                        getRef());

                    final Boolean out = new ExpectMsg<Boolean>(duration("800 millis"), "dataChanged") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected Boolean match(Object in) {
                            if (in != null && in.getClass().equals(DataChangedReply.class)) {

                                return true;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue(out);
                    assertTrue(listener.gotIt());
                    assertNotNull(listener.getChange().getCreatedData());

                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testDataChangedWhenNotificationsAreDisabled(){
        new JavaTestKit(getSystem()) {{
            final MockDataChangeListener listener = new MockDataChangeListener();
            final Props props = DataChangeListener.props(listener);
            final ActorRef subject =
                getSystem().actorOf(props, "testDataChangedNotificationsDisabled");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(
                        new DataChanged(CompositeModel.createTestContext(),new MockDataChangedEvent()),
                        getRef());

                    expectNoMsg();
                }
            };
        }};
    }
}
