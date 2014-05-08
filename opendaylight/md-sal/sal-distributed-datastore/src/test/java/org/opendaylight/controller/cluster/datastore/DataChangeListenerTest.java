package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataChangedReply;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class DataChangeListenerTest extends AbstractActorTest {

    private static class MockDataChangedEvent implements AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> {

        @Override
        public Map<InstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
            throw new UnsupportedOperationException("getCreatedData");
        }

        @Override
        public Map<InstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
            throw new UnsupportedOperationException("getUpdatedData");
        }

        @Override public Set<InstanceIdentifier> getRemovedPaths() {
            throw new UnsupportedOperationException("getRemovedPaths");
        }

        @Override
        public Map<InstanceIdentifier, ? extends NormalizedNode<?, ?>> getOriginalData() {
            throw new UnsupportedOperationException("getOriginalData");
        }

        @Override public NormalizedNode<?, ?> getOriginalSubtree() {
            throw new UnsupportedOperationException("getOriginalSubtree");
        }

        @Override public NormalizedNode<?, ?> getUpdatedSubtree() {
            throw new UnsupportedOperationException("getUpdatedSubtree");
        }
    }

    private class MockDataChangeListener implements AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> {
        private boolean gotIt = false;

        @Override public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {
            gotIt = true;
        }

        public boolean gotIt() {
            return gotIt;
        }
    }

    @Test
    public void testDataChanged(){
        new JavaTestKit(getSystem()) {{
            final MockDataChangeListener listener = new MockDataChangeListener();
            final Props props = DataChangeListener.props(listener);
            final ActorRef subject =
                getSystem().actorOf(props, "testDataChanged");

            new Within(duration("1 seconds")) {
                protected void run() {

                    subject.tell(
                        new DataChanged(new MockDataChangedEvent()),
                        getRef());

                    final Boolean out = new ExpectMsg<Boolean>("dataChanged") {
                        // do not put code outside this method, will run afterwards
                        protected Boolean match(Object in) {
                            if (in instanceof DataChangedReply) {
                                DataChangedReply reply =
                                    (DataChangedReply) in;
                                return true;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue(out);
                    assertTrue(listener.gotIt());
                    // Will wait for the rest of the 3 seconds
                    expectNoMsg();
                }


            };
        }};
    }
}
