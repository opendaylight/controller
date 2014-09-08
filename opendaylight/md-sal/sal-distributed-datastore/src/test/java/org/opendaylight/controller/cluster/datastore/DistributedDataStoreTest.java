package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import akka.util.Timeout;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
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
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            new DatastoreContext());

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
        ActorContext actorContext = mock(ActorContext.class);

        distributedDataStore = new DistributedDataStore(actorContext);
        distributedDataStore.onGlobalContextUpdated(TestModel.createTestContext());

        Future future = mock(Future.class);
        when(actorContext.getActorSystem()).thenReturn(getSystem());
        when(actorContext
            .executeLocalShardOperationAsync(anyString(), anyObject(), any(Timeout.class))).thenReturn(future);

        ListenerRegistration registration =
            distributedDataStore.registerChangeListener(TestModel.TEST_PATH,
                mock(AsyncDataChangeListener.class),
                AsyncDataBroker.DataChangeScope.BASE);

        assertNotNull(registration);

        assertEquals(DataChangeListenerRegistrationProxy.class, registration.getClass());
    }

    @Test
    public void testRegisterChangeListenerWhenSuccessfulReplyReceived() throws Exception {
        ActorContext actorContext = mock(ActorContext.class);

        distributedDataStore = new DistributedDataStore(actorContext);
        distributedDataStore.onGlobalContextUpdated(
            TestModel.createTestContext());

        ExecutionContextExecutor executor = ExecutionContexts.fromExecutor(MoreExecutors.sameThreadExecutor());

        // Make Future successful
        Future f = Futures.successful(new RegisterChangeListenerReply(doNothingActorRef.path()));

        // Setup the mocks
        ActorSystem actorSystem = mock(ActorSystem.class);
        ActorSelection actorSelection = mock(ActorSelection.class);

        when(actorSystem.dispatcher()).thenReturn(executor);
        when(actorSystem.actorOf(any(Props.class))).thenReturn(doNothingActorRef);
        when(actorContext.getActorSystem()).thenReturn(actorSystem);
        when(actorContext
            .executeLocalShardOperationAsync(anyString(), anyObject(), any(Timeout.class))).thenReturn(f);
        when(actorContext.actorSelection(any(ActorPath.class))).thenReturn(actorSelection);

        ListenerRegistration registration =
            distributedDataStore.registerChangeListener(TestModel.TEST_PATH,
                mock(AsyncDataChangeListener.class),
                AsyncDataBroker.DataChangeScope.BASE);

        assertNotNull(registration);

        assertEquals(DataChangeListenerRegistrationProxy.class, registration.getClass());

        ActorSelection listenerRegistrationActor =
            ((DataChangeListenerRegistrationProxy) registration).getListenerRegistrationActor();

        assertNotNull(listenerRegistrationActor);

        assertEquals(actorSelection, listenerRegistrationActor);
    }

    @Test
    public void testRegisterChangeListenerWhenSuccessfulReplyFailed() throws Exception {
        ActorContext actorContext = mock(ActorContext.class);

        distributedDataStore = new DistributedDataStore(actorContext);
        distributedDataStore.onGlobalContextUpdated(
            TestModel.createTestContext());

        ExecutionContextExecutor executor = ExecutionContexts.fromExecutor(MoreExecutors.sameThreadExecutor());

        // Make Future fail
        Future f = Futures.failed(new IllegalArgumentException());

        // Setup the mocks
        ActorSystem actorSystem = mock(ActorSystem.class);
        ActorSelection actorSelection = mock(ActorSelection.class);

        when(actorSystem.dispatcher()).thenReturn(executor);
        when(actorSystem.actorOf(any(Props.class))).thenReturn(doNothingActorRef);
        when(actorContext.getActorSystem()).thenReturn(actorSystem);
        when(actorContext
            .executeLocalShardOperationAsync(anyString(), anyObject(), any(Timeout.class))).thenReturn(f);
        when(actorContext.actorSelection(any(ActorPath.class))).thenReturn(actorSelection);

        ListenerRegistration registration =
            distributedDataStore.registerChangeListener(TestModel.TEST_PATH,
                mock(AsyncDataChangeListener.class),
                AsyncDataBroker.DataChangeScope.BASE);

        assertNotNull(registration);

        assertEquals(DataChangeListenerRegistrationProxy.class, registration.getClass());

        ActorSelection listenerRegistrationActor =
            ((DataChangeListenerRegistrationProxy) registration).getListenerRegistrationActor();

        assertNull(listenerRegistrationActor);

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
