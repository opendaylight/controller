
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.DataChanged;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.CompositeModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
public class DataChangeListenerProxyTest extends AbstractActorTest {

  private static class MockDataChangedEvent implements AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> {
    Map<YangInstanceIdentifier,NormalizedNode<?,?>> createdData = new HashMap<>();
    Map<YangInstanceIdentifier,NormalizedNode<?,?>> updatedData = new HashMap<>();
    Map<YangInstanceIdentifier,NormalizedNode<?,?>> originalData = new HashMap<>();



    @Override
    public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getCreatedData() {
      createdData.put(YangInstanceIdentifier.builder().build(), CompositeModel.createDocumentOne(CompositeModel.createTestContext()));
      return createdData;
    }

    @Override
    public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getUpdatedData() {
      updatedData.put(YangInstanceIdentifier.builder().build(), CompositeModel.createTestContainer());
      return updatedData;

    }

    @Override
    public Set<YangInstanceIdentifier> getRemovedPaths() {
      Set<YangInstanceIdentifier>ids = new HashSet<>();
      ids.add( CompositeModel.TEST_PATH);
      return ids;
    }

    @Override
    public Map<YangInstanceIdentifier, NormalizedNode<?, ?>> getOriginalData() {
      originalData.put(YangInstanceIdentifier.builder().build(), CompositeModel.createFamily());
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

        DataChangeListenerProxy dataChangeListenerProxy = new DataChangeListenerProxy(
                getSystem().actorSelection(actorRef.path()));

        dataChangeListenerProxy.onDataChanged(new MockDataChangedEvent());

        //Check if it was received by the remote actor
        ActorContext
            testContext = new ActorContext(getSystem(), getSystem().actorOf(Props.create(DoNothingActor.class)), new MockClusterWrapper(), new MockConfiguration());
        Object messages = testContext
            .executeOperation(actorRef, "messages");

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<?> listMessages = (List<?>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0).getClass().equals(DataChanged.class));

    }
}
