package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MockActorContext;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DistributedDataStoreTest extends AbstractActorTest{

    private DistributedDataStore distributedDataStore;
    private MockActorContext mockActorContext;
    private ActorRef doNothingActorRef;

    @Before
    public void setUp() throws Exception {
        ShardStrategyFactory.setConfiguration(new MockConfiguration());
        final Props props = Props.create(DoNothingActor.class);

        doNothingActorRef = getSystem().actorOf(props);

        mockActorContext = new MockActorContext(getSystem(), doNothingActorRef);
        distributedDataStore = new DistributedDataStore(mockActorContext);
        distributedDataStore.onGlobalContextUpdated(
            TestModel.createTestContext());

        // Make CreateTransactionReply as the default response. Will need to be
        // tuned if a specific test requires some other response
        mockActorContext.setExecuteShardOperationResponse(
            CreateTransactionReply.newBuilder()
                .setTransactionActorPath(doNothingActorRef.path().toString())
                .setTransactionId("txn-1 ")
                .build());
    }

    @After
    public void tearDown() throws Exception {

    }

    @SuppressWarnings("resource")
    @Test
    public void testConstructor(){
        ActorSystem actorSystem = mock(ActorSystem.class);

        new DistributedDataStore(actorSystem, "config",
            mock(ClusterWrapper.class), mock(Configuration.class),
            new DistributedDataStoreProperties());

        verify(actorSystem).actorOf(any(Props.class), eq("shardmanager-config"));
    }

    @Test
    public void testRegisterChangeListenerWhenShardIsNotLocal() throws Exception {

        ListenerRegistration registration =
                distributedDataStore.registerChangeListener(TestModel.TEST_PATH, new AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
            @Override
            public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
                throw new UnsupportedOperationException("onDataChanged");
            }
        }, AsyncDataBroker.DataChangeScope.BASE);

        // Since we do not expect the shard to be local registration will return a NoOpRegistration
        assertTrue(registration instanceof NoOpDataChangeListenerRegistration);

        assertNotNull(registration);
    }

    @Test
    public void testRegisterChangeListenerWhenShardIsLocal() throws Exception {

        mockActorContext.setExecuteLocalShardOperationResponse(new RegisterChangeListenerReply(doNothingActorRef.path()));

        ListenerRegistration registration =
            distributedDataStore.registerChangeListener(TestModel.TEST_PATH, new AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
                @Override
                public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
                    throw new UnsupportedOperationException("onDataChanged");
                }
            }, AsyncDataBroker.DataChangeScope.BASE);

        assertTrue(registration instanceof DataChangeListenerRegistrationProxy);

        assertNotNull(registration);
    }


    @Test
    public void testCreateTransactionChain() throws Exception {
        final DOMStoreTransactionChain transactionChain = distributedDataStore.createTransactionChain();
        assertNotNull(transactionChain);
    }

    @Test
    public void testNewReadOnlyTransaction() throws Exception {
        final DOMStoreReadTransaction transaction = distributedDataStore.newReadOnlyTransaction();
        assertNotNull(transaction);
    }

    @Test
    public void testNewWriteOnlyTransaction() throws Exception {
        final DOMStoreWriteTransaction transaction = distributedDataStore.newWriteOnlyTransaction();
        assertNotNull(transaction);
    }

    @Test
    public void testNewReadWriteTransaction() throws Exception {
        final DOMStoreReadWriteTransaction transaction = distributedDataStore.newReadWriteTransaction();
        assertNotNull(transaction);
    }
}
