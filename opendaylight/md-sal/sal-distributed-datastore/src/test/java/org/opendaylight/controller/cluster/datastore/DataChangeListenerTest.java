package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataChangedReply;
import org.opendaylight.controller.md.cluster.datastore.model.CompositeModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DataChangeListenerTest extends AbstractActorTest {

    private static class MockDataChangedEvent implements AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> {
       Map<InstanceIdentifier,NormalizedNode<?,?>> createdData = new HashMap();
       Map<InstanceIdentifier,NormalizedNode<?,?>> updatedData = new HashMap();
       Map<InstanceIdentifier,NormalizedNode<?,?>> originalData = new HashMap();



        @Override
        public Map<InstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
            createdData.put(CompositeModel.FAMILY_PATH, CompositeModel.createFamily());
            return createdData;
        }

        @Override
        public Map<InstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
            updatedData.put(CompositeModel.FAMILY_PATH, CompositeModel.createFamily());
            return updatedData;

        }

        @Override
        public Set<InstanceIdentifier> getRemovedPaths() {
               Set<InstanceIdentifier>ids = new HashSet();
               ids.add( CompositeModel.TEST_PATH);
              return ids;
        }

        @Override
        public Map<InstanceIdentifier, NormalizedNode<?, ?>> getOriginalData() {
          originalData.put(CompositeModel.FAMILY_PATH, CompositeModel.createFamily());
          return originalData;
        }

        @Override public NormalizedNode<?, ?> getOriginalSubtree() {

          //fixme: need to have some valid data here
          return null;
        }

        @Override public NormalizedNode<?, ?> getUpdatedSubtree() {

          //fixme: need to have some valid data here
          return null;
        }
    }

    private class MockDataChangeListener implements AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> {
        private boolean gotIt = false;
        private   AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change;

        @Override public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {
            gotIt = true;this.change=change;
        }

        public boolean gotIt() {
            return gotIt;
        }
        public  AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> getChange(){
          return change;
        }
    }

    @Test
    public void testDataChanged(){
        new JavaTestKit(getSystem()) {{
            final MockDataChangeListener listener = new MockDataChangeListener();
            final Props props = DataChangeListener.props(CompositeModel.createTestContext(),listener);
            final ActorRef subject =
                getSystem().actorOf(props, "testDataChanged");

            new Within(duration("1 seconds")) {
                protected void run() {

                    subject.tell(
                        new DataChanged(CompositeModel.createTestContext(),new MockDataChangedEvent()).toSerializable(),
                        getRef());

                    final Boolean out = new ExpectMsg<Boolean>("dataChanged") {
                        // do not put code outside this method, will run afterwards
                        protected Boolean match(Object in) {
                            if (in.getClass().equals(DataChangedReply.SERIALIZABLE_CLASS)) {

                                return true;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue(out);
                    assertTrue(listener.gotIt());
                    assertNotNull(listener.getChange().getCreatedData());
                    // Will wait for the rest of the 3 seconds
                    expectNoMsg();
                }


            };
        }};
    }
}
