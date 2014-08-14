package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.dispatch.Futures;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.READ_ONLY;

import org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.datastore.utils.MockActorContext;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;

public class TransactionProxyTest extends AbstractActorTest {

    @SuppressWarnings("serial")
    static class TestException extends RuntimeException {
    }

    static interface Invoker {
        void invoke(TransactionProxy proxy) throws Exception;
    }

    private final Configuration configuration = new MockConfiguration();

    private MockActorContext actorContext;

    @Mock
    private ActorContext mockActorContext;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

        doReturn(getSystem()).when(mockActorContext).getActorSystem();

        ShardStrategyFactory.setConfiguration(configuration);
    }

    private CreateTransaction eqCreateTransaction(final String memberName,
            final TransactionType type) {
        ArgumentMatcher<CreateTransaction> matcher = new ArgumentMatcher<CreateTransaction>() {
            @Override
            public boolean matches(Object argument) {
                CreateTransaction obj = CreateTransaction.fromSerializable(argument);
                return obj.getTransactionId().startsWith(memberName) &&
                       obj.getTransactionType() == type.ordinal();
            }
        };

        return argThat(matcher);
    }

    private DataExists eqDataExists() {
        ArgumentMatcher<DataExists> matcher = new ArgumentMatcher<DataExists>() {
            @Override
            public boolean matches(Object argument) {
                DataExists obj = DataExists.fromSerializable(argument);
                return obj.getPath().equals(TestModel.TEST_PATH);
            }
        };

        return argThat(matcher);
    }

    private ReadData eqReadData() {
        ArgumentMatcher<ReadData> matcher = new ArgumentMatcher<ReadData>() {
            @Override
            public boolean matches(Object argument) {
                ReadData obj = ReadData.fromSerializable(argument);
                return obj.getPath().equals(TestModel.TEST_PATH);
            }
        };

        return argThat(matcher);
    }

    private ActorRef setupInitialMockActorContext(Class<? extends Actor> actorClass) {
        final Props props = Props.create(actorClass);
        final ActorRef actorRef = getSystem().actorOf(props);

        actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteLocalOperationResponse(createPrimaryFound(actorRef));
        actorContext.setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");
        return actorRef;
    }

    private ActorRef setupActorContextWithInitialCreateTransaction(TransactionType type) {
        ActorRef actorRef = getSystem().actorOf(Props.create(DoNothingActor.class));
        String memberName = "mock-member";
        doReturn(getSystem().actorSelection(actorRef.path())).
                when(mockActorContext).actorSelection(actorRef.path().toString());
        doReturn(memberName).when(mockActorContext).getCurrentMemberName();
        doReturn(createTransactionReply(actorRef)).when(mockActorContext).
                executeShardOperation(eq(DefaultShardStrategy.DEFAULT_SHARD),
                        eqCreateTransaction(memberName, type), anyDuration());
        return actorRef;
    }

    private Future<Object> readDataReply(NormalizedNode<?, ?> data) {
        return Futures.successful(new ReadDataReply(TestModel.createTestContext(), data)
                .toSerializable());
    }

    private Future<Object> dataExistsReply(boolean exists) {
        return Futures.successful(new DataExistsReply(exists).toSerializable());
    }

    private ActorSelection actorSelection(ActorRef actorRef) {
        return getSystem().actorSelection(actorRef.path());
    }

    private FiniteDuration anyDuration() {
        return any(FiniteDuration.class);
    }

    @Test
    public void testRead() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(READ_ONLY);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY, TestModel.createTestContext());

        doReturn(readDataReply(null)).when(mockActorContext).executeRemoteOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), anyDuration());

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", false, readOptional.isPresent());

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readDataReply(expectedNode)).when(mockActorContext).executeRemoteOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), anyDuration());

        readOptional = transactionProxy.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());

        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadWhenAnInvalidMessageIsSentInReply() throws Exception {
        setupActorContextWithInitialCreateTransaction(READ_ONLY);

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeRemoteOperationAsync(any(ActorSelection.class), any(), anyDuration());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY, TestModel.createTestContext());

        transactionProxy.read(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testReadWithAsyncRemoteOperatonFailure() throws Throwable {
        setupActorContextWithInitialCreateTransaction(READ_ONLY);

        doThrow(new TestException()).when(mockActorContext).
                executeRemoteOperationAsync(any(ActorSelection.class), any(), anyDuration());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY, TestModel.createTestContext());

        try {
            transactionProxy.read(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
            fail("Expected ReadFailedException");
        } catch(ReadFailedException e) {
            // Expected - throw cause - expects TestException.
            throw e.getCause();
        }
    }

    private void testExceptionOnInitialCreateTransaction(Exception exToThrow, Invoker invoker)
            throws Throwable {

        doThrow(exToThrow).when(mockActorContext).executeShardOperation(
                anyString(), any(), anyDuration());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY, TestModel.createTestContext());

        try {
            invoker.invoke(transactionProxy);;
            fail("Expected ReadFailedException");
        } catch(ReadFailedException e) {
            // Expected - throw cause - expects TestException.
            throw e.getCause();
        }
    }

    private void testReadWithExceptionOnInitialCreateTransaction(Exception exToThrow) throws Throwable {
        testExceptionOnInitialCreateTransaction(exToThrow, new Invoker() {
            @Override
            public void invoke(TransactionProxy proxy) throws Exception {
                proxy.read(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
            }
        });
    }

    @Test(expected = PrimaryNotFoundException.class)
    public void testReadWhenAPrimaryNotFoundExceptionIsThrown() throws Throwable {
        testReadWithExceptionOnInitialCreateTransaction(new PrimaryNotFoundException("test"));
    }

    @Test(expected = TimeoutException.class)
    public void testReadWhenATimeoutExceptionIsThrown() throws Throwable {
        testReadWithExceptionOnInitialCreateTransaction(new TimeoutException("test",
                new Exception("reason")));
    }

    @Test(expected = TestException.class)
    public void testReadWhenAnyOtherExceptionIsThrown() throws Throwable {
        testReadWithExceptionOnInitialCreateTransaction(new TestException());
    }

    @Test
    public void testExists() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(READ_ONLY);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY, TestModel.createTestContext());

        doReturn(dataExistsReply(false)).when(mockActorContext).executeRemoteOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists(), anyDuration());

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", false, exists);

        doReturn(dataExistsReply(true)).when(mockActorContext).executeRemoteOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists(), anyDuration());

        exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", true, exists);
    }

    @Test(expected = PrimaryNotFoundException.class)
    public void testExistsWhenAPrimaryNotFoundExceptionIsThrown() throws Throwable {
        testExceptionOnInitialCreateTransaction(new PrimaryNotFoundException("test"), new Invoker() {
            @Override
            public void invoke(TransactionProxy proxy) throws Exception {
                proxy.exists(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
            }
        });
    }

    @Test(expected = ReadFailedException.class)
    public void testExistsWhenAnInvalidMessageIsSentInReply() throws Exception {
        setupActorContextWithInitialCreateTransaction(READ_ONLY);

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeRemoteOperationAsync(any(ActorSelection.class), any(), anyDuration());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY, TestModel.createTestContext());

        transactionProxy.exists(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testExistsWithAsyncRemoteOperatonFailure() throws Throwable {
        setupActorContextWithInitialCreateTransaction(READ_ONLY);

        doThrow(new TestException()).when(mockActorContext).
                executeRemoteOperationAsync(any(ActorSelection.class), any(), anyDuration());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY, TestModel.createTestContext());

        try {
            transactionProxy.exists(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
            fail("Expected ReadFailedException");
        } catch(ReadFailedException e) {
            // Expected - throw cause - expects TestException.
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    private void queryMessagesAndVerify(ActorRef actorRef, Class<?> expMessageType) throws Exception {
        Object messages = Await.result(ask(actorRef, "messages", 5000L),
                ActorContext.AWAIT_DURATION);

        assertNotNull("Returned message is null", messages);
        assertTrue("Expected returned message of type List", messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;
        assertEquals("Returned message List size: " + listMessages, 1, listMessages.size());
        assertEquals("Returned message type", expMessageType, listMessages.get(0).getClass());
    }

    @Test
    public void testWrite() throws Exception {
        final ActorRef actorRef = setupInitialMockActorContext(MessageCollectorActor.class);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.WRITE_ONLY, TestModel.createTestContext());

        transactionProxy.write(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));

        queryMessagesAndVerify(actorRef, WriteData.SERIALIZABLE_CLASS);
    }

    private Object createPrimaryFound(ActorRef actorRef) {
        return new PrimaryFound(actorRef.path().toString()).toSerializable();
    }

    @Test
    public void testMerge() throws Exception {
        final ActorRef actorRef = setupInitialMockActorContext(MessageCollectorActor.class);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.WRITE_ONLY, TestModel.createTestContext());

        transactionProxy.merge(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));

        queryMessagesAndVerify(actorRef, MergeData.SERIALIZABLE_CLASS);
    }

    @Test
    public void testDelete() throws Exception {
        final ActorRef actorRef = setupInitialMockActorContext(MessageCollectorActor.class);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.WRITE_ONLY, TestModel.createTestContext());

        transactionProxy.delete(TestModel.TEST_PATH);

        queryMessagesAndVerify(actorRef, DeleteData.SERIALIZABLE_CLASS);
    }

    @Test
    public void testReady() throws Exception {
        final ActorRef actorRef = setupInitialMockActorContext(DoNothingActor.class);

        actorContext.setExecuteRemoteOperationResponse(new ReadyTransactionReply(
                actorRef.path()).toSerializable());

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_WRITE, TestModel.createTestContext());

        transactionProxy.read(TestModel.TEST_PATH);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        assertTrue("No cohort paths returned", proxy.getCohortPaths().size() > 0);
    }

    @Test
    public void testGetIdentifier(){
        final ActorRef actorRef = setupInitialMockActorContext(DoNothingActor.class);
        actorContext.setExecuteShardOperationResponse( createTransactionReply(actorRef) );

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        assertNotNull(transactionProxy.getIdentifier());
    }

    @Test
    public void testClose() throws Exception{
        final ActorRef actorRef = setupInitialMockActorContext(MessageCollectorActor.class);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.close();

        queryMessagesAndVerify(actorRef, CloseTransaction.SERIALIZABLE_CLASS);
    }

    private CreateTransactionReply createTransactionReply(ActorRef actorRef){
        return CreateTransactionReply.newBuilder()
            .setTransactionActorPath(actorRef.path().toString())
            .setTransactionId("txn-1")
            .build();
    }
}
