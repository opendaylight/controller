
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
import org.opendaylight.controller.md.cluster.datastore.model.CompositeModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataChangeListenerProxyTest extends AbstractActorTest {

  private static class MockDataChangedEvent implements AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> {
    Map<InstanceIdentifier,NormalizedNode<?,?>> createdData = new HashMap();
    Map<InstanceIdentifier,NormalizedNode<?,?>> updatedData = new HashMap();
    Map<InstanceIdentifier,NormalizedNode<?,?>> originalData = new HashMap();



    @Override
    public Map<InstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
      createdData.put(InstanceIdentifier.builder().build(), CompositeModel.createDocumentOne(CompositeModel.createTestContext()));
      return createdData;
    }

    @Override
    public Map<InstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
      updatedData.put(InstanceIdentifier.builder().build(), CompositeModel.createTestContainer());
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
      originalData.put(InstanceIdentifier.builder().build(), CompositeModel.createFamily());
      return originalData;
    }

    @Override public NormalizedNode<?, ?> getOriginalSubtree() {
      return CompositeModel.createFamily() ;
    }

    @Override public NormalizedNode<?, ?> getUpdatedSubtree() {
      return CompositeModel.createTestContainer();
    }
  }


  @Test
    public void testOnDataChanged() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        DataChangeListenerProxy dataChangeListenerProxy =
            new DataChangeListenerProxy(TestModel.createTestContext(),
                getSystem().actorSelection(actorRef.path()));

        dataChangeListenerProxy.onDataChanged(new MockDataChangedEvent());

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

        Assert.assertTrue(listMessages.get(0).getClass().equals(DataChanged.SERIALIZABLE_CLASS));

    }
}
