package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import junit.framework.Assert;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MockActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DistributedDataStoreTest extends AbstractActorTest{

    private DistributedDataStore distributedDataStore;
    private MockActorContext mockActorContext;
    private ActorRef doNothingActorRef;

    @org.junit.Before
    public void setUp() throws Exception {
        final Props props = Props.create(DoNothingActor.class);

        doNothingActorRef = getSystem().actorOf(props);

        mockActorContext = new MockActorContext(getSystem(), doNothingActorRef);
        distributedDataStore = new DistributedDataStore(mockActorContext, "config");
        distributedDataStore.onGlobalContextUpdated(
            TestModel.createTestContext());

        // Make CreateTransactionReply as the default response. Will need to be
        // tuned if a specific test requires some other response
        mockActorContext.setExecuteShardOperationResponse(
            new CreateTransactionReply(doNothingActorRef.path(), "txn-1 "));
    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @org.junit.Test
    public void testRegisterChangeListener() throws Exception {
        mockActorContext.setExecuteShardOperationResponse(new RegisterChangeListenerReply(doNothingActorRef.path()));
        ListenerRegistration registration =
                distributedDataStore.registerChangeListener(TestModel.TEST_PATH, new AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>() {
            @Override
            public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {
                throw new UnsupportedOperationException("onDataChanged");
            }
        }, AsyncDataBroker.DataChangeScope.BASE);

        Assert.assertNotNull(registration);
    }

    @org.junit.Test
    public void testCreateTransactionChain() throws Exception {
        final DOMStoreTransactionChain transactionChain = distributedDataStore.createTransactionChain();
        Assert.assertNotNull(transactionChain);
    }

    @org.junit.Test
    public void testNewReadOnlyTransaction() throws Exception {
        final DOMStoreReadTransaction transaction = distributedDataStore.newReadOnlyTransaction();
        Assert.assertNotNull(transaction);
    }

    @org.junit.Test
    public void testNewWriteOnlyTransaction() throws Exception {
        final DOMStoreWriteTransaction transaction = distributedDataStore.newWriteOnlyTransaction();
        Assert.assertNotNull(transaction);
    }

    @org.junit.Test
    public void testNewReadWriteTransaction() throws Exception {
        final DOMStoreReadWriteTransaction transaction = distributedDataStore.newReadWriteTransaction();
        Assert.assertNotNull(transaction);
    }
}
