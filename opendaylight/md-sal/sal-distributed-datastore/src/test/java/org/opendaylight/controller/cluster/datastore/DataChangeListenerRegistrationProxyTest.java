package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.List;

public class DataChangeListenerRegistrationProxyTest extends AbstractActorTest{

    private ActorRef dataChangeListenerActor = getSystem().actorOf(Props.create(DoNothingActor.class));

    private static class MockDataChangeListener implements
        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> {

        @Override public void onDataChanged(
            AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
            throw new UnsupportedOperationException("onDataChanged");
        }
    }

    @Test
    public void testGetInstance() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        MockDataChangeListener listener =
            new MockDataChangeListener();
        DataChangeListenerRegistrationProxy proxy =
            new DataChangeListenerRegistrationProxy(
                getSystem().actorSelection(actorRef.path()),
                listener, dataChangeListenerActor);

        Assert.assertEquals(listener, proxy.getInstance());

    }

    @Test
    public void testClose() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        DataChangeListenerRegistrationProxy proxy =
            new DataChangeListenerRegistrationProxy(
                getSystem().actorSelection(actorRef.path()),
                new MockDataChangeListener(), dataChangeListenerActor);

        proxy.close();

        //Check if it was received by the remote actor
        ActorContext
            testContext = new ActorContext(getSystem(), getSystem().actorOf(Props.create(DoNothingActor.class)),new MockClusterWrapper(), new MockConfiguration());
        Object messages = testContext
            .executeLocalOperation(actorRef, "messages",
                ActorContext.ASK_DURATION);

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0).getClass().equals(CloseDataChangeListenerRegistration.SERIALIZABLE_CLASS));
    }
}
