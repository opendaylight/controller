package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.md.cluster.datastore.model.CompositeModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;

public class DataChangeListenerTest extends AbstractActorTest {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataChangedWhenNotificationsAreEnabled(){
        new JavaTestKit(getSystem()) {{
            final AsyncDataChangeEvent mockChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
            final AsyncDataChangeListener mockListener = Mockito.mock(AsyncDataChangeListener.class);
            final Props props = DataChangeListener.props(mockListener);
            final ActorRef subject = getSystem().actorOf(props, "testDataChangedNotificationsEnabled");

            // Let the DataChangeListener know that notifications should be enabled
            subject.tell(new EnableNotification(true), getRef());

            subject.tell(new DataChanged(CompositeModel.createTestContext(), mockChangeEvent),
                    getRef());

            expectMsgClass(DataChangedReply.class);

            Mockito.verify(mockListener).onDataChanged(mockChangeEvent);
        }};
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataChangedWhenNotificationsAreDisabled(){
        new JavaTestKit(getSystem()) {{
            final AsyncDataChangeEvent mockChangeEvent = Mockito.mock(AsyncDataChangeEvent.class);
            final AsyncDataChangeListener mockListener = Mockito.mock(AsyncDataChangeListener.class);
            final Props props = DataChangeListener.props(mockListener);
            final ActorRef subject =
                getSystem().actorOf(props, "testDataChangedNotificationsDisabled");

            subject.tell(new DataChanged(CompositeModel.createTestContext(), mockChangeEvent),
                    getRef());

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {
                    expectNoMsg();

                    Mockito.verify(mockListener, Mockito.never()).onDataChanged(
                            Mockito.any(AsyncDataChangeEvent.class));
                }
            };
        }};
    }
}
