package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistrationReply;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataChangeListenerRegistrationTest extends AbstractActorTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testOnReceiveCloseListenerRegistration() throws Exception {
        new JavaTestKit(getSystem()) {{
            ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
            registration = Mockito.mock(ListenerRegistration.class);

            final Props props = DataChangeListenerRegistration.props(registration);
            final ActorRef subject = getSystem().actorOf(props, "testOnReceiveCloseListenerRegistration");

            watch(subject);

            subject.tell(CloseDataChangeListenerRegistration.INSTANCE, getRef());

            expectMsgClass(duration("5 seconds"), CloseDataChangeListenerRegistrationReply.class);
            expectMsgClass(duration("5 seconds"), Terminated.class);

            Mockito.verify(registration).close();
        }};
    }
}
