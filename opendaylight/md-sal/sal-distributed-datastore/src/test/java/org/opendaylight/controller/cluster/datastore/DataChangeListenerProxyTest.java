
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataChangeListenerProxyTest extends AbstractActorTest {

    private static class MockDataChangeEvent implements
        AsyncDataChangeEvent<YangInstanceIdentifier,NormalizedNode<?,?>> {

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
            throw new UnsupportedOperationException("getCreatedData");
        }

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
            throw new UnsupportedOperationException("getUpdatedData");
        }

        @Override public Set<YangInstanceIdentifier> getRemovedPaths() {
            throw new UnsupportedOperationException("getRemovedPaths");
        }

        @Override
        public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getOriginalData() {
            throw new UnsupportedOperationException("getOriginalData");
        }

        @Override public NormalizedNode<?, ?> getOriginalSubtree() {
            throw new UnsupportedOperationException("getOriginalSubtree");
        }

        @Override public NormalizedNode<?, ?> getUpdatedSubtree() {
            throw new UnsupportedOperationException("getUpdatedSubtree");
        }
    }

    @Test
    public void testOnDataChanged() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        DataChangeListenerProxy dataChangeListenerProxy =
            new DataChangeListenerProxy(
                getSystem().actorSelection(actorRef.path()));

        dataChangeListenerProxy.onDataChanged(new MockDataChangeEvent());

        //Check if it was received by the remote actor
        ActorContext
            testContext = new ActorContext(getSystem(), getSystem().actorOf(Props.create(DoNothingActor.class)), new MockConfiguration());
        Object messages = testContext
            .executeLocalOperation(actorRef, "messages",
                ActorContext.ASK_DURATION);

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0) instanceof DataChanged);

    }
}
