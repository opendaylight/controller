package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
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
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionProxyTest extends AbstractActorTest {

    private final Configuration configuration = new MockConfiguration();

    private MockActorContext actorContext;

    @Before
    public void setUp(){
        ShardStrategyFactory.setConfiguration(configuration);
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

    @Test
    public void testRead() throws Exception {
        setupInitialMockActorContext(DoNothingActor.class);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        actorContext.setExecuteRemoteOperationResponse(
            new ReadDataReply(TestModel.createTestContext(), null).toSerializable());

        ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
            transactionProxy.read(TestModel.TEST_PATH);

        Optional<NormalizedNode<?, ?>> normalizedNodeOptional = read.get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", false, normalizedNodeOptional.isPresent());

        ReadData input = actorContext.getInputMessage(ReadData.class);
        assertEquals("", TestModel.TEST_PATH, input.getPath());

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        actorContext.setExecuteRemoteOperationResponse(new ReadDataReply(
            TestModel.createTestContext(), expectedNode).toSerializable());

        read = transactionProxy.read(TestModel.TEST_PATH);

        normalizedNodeOptional = read.get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, normalizedNodeOptional.isPresent());

        assertEquals("Response NormalizedNode", expectedNode, normalizedNodeOptional.get());

        input = actorContext.getInputMessage(ReadData.class);
        assertEquals("", TestModel.TEST_PATH, input.getPath());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadWhenAnInvalidMessageIsSentInReply() throws Exception {
        setupInitialMockActorContext(DoNothingActor.class);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>
            read = transactionProxy.read(TestModel.TEST_PATH);

        read.checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = ReadFailedException.class)
    public void testReadWithAsyncRemoteOperatonFailure() throws Exception {
        setupInitialMockActorContext(DoNothingActor.class);

        actorContext.setExecuteRemoteOperationFailure(new Exception("mock"));
        actorContext.setExecuteRemoteOperationResponse(null);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>
            read = transactionProxy.read(TestModel.TEST_PATH);

        read.checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = ReadFailedException.class)
    public void testReadWhenAPrimaryNotFoundExceptionIsThrown() throws Exception {
        final ActorContext actorContext = mock(ActorContext.class);

        when(actorContext.executeShardOperation(anyString(), any(), any(
            FiniteDuration.class))).thenThrow(new PrimaryNotFoundException("test"));

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
                transactionProxy.read(TestModel.TEST_PATH);

        read.checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = ReadFailedException.class)
    public void testReadWhenATimeoutExceptionIsThrown() throws Exception {
        final ActorContext actorContext = mock(ActorContext.class);

        when(actorContext.executeShardOperation(anyString(), any(), any(
            FiniteDuration.class))).thenThrow(new TimeoutException("test", new Exception("reason")));

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
            transactionProxy.read(TestModel.TEST_PATH);

        read.checkedGet(5, TimeUnit.SECONDS);
    }

    @Test
    public void testReadWhenAAnyOtherExceptionIsThrown() throws Exception {
        final ActorContext actorContext = mock(ActorContext.class);

        when(actorContext.executeShardOperation(anyString(), any(), any(
            FiniteDuration.class))).thenThrow(new NullPointerException());

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        try {
            ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
                transactionProxy.read(TestModel.TEST_PATH);
            fail("A null pointer exception was expected");
        } catch(NullPointerException e){
            // Expected
        }
    }

    @Test
    public void testExists() throws Exception {
        setupInitialMockActorContext(DoNothingActor.class);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        actorContext.setExecuteRemoteOperationResponse(new DataExistsReply(false).toSerializable());

        CheckedFuture<Boolean, ReadFailedException> exists =
                transactionProxy.exists(TestModel.TEST_PATH);

        assertEquals("Exists response", false, exists.checkedGet());

        DataExists input = actorContext.getInputMessage(DataExists.class);
        assertEquals("", TestModel.TEST_PATH, input.getPath());

        actorContext.setExecuteRemoteOperationResponse(new DataExistsReply(true).toSerializable());

        exists = transactionProxy.exists(TestModel.TEST_PATH);

        assertEquals("Exists response", true, exists.checkedGet(5, TimeUnit.SECONDS));

        input = actorContext.getInputMessage(DataExists.class);
        assertEquals("", TestModel.TEST_PATH, input.getPath());
    }

    @Test(expected = ReadFailedException.class)
    public void testExistsWhenAPrimaryNotFoundExceptionIsThrown() throws Exception {
        final ActorContext actorContext = mock(ActorContext.class);

        when(actorContext.executeShardOperation(anyString(), any(), any(
            FiniteDuration.class))).thenThrow(new PrimaryNotFoundException("test"));

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        CheckedFuture<Boolean, ReadFailedException> exists =
                transactionProxy.exists(TestModel.TEST_PATH);

        exists.checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = ReadFailedException.class)
    public void testExistsWhenAnInvalidMessageIsSentInReply() throws Exception {
        setupInitialMockActorContext(DoNothingActor.class);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        CheckedFuture<Boolean, ReadFailedException> exists =
                transactionProxy.exists(TestModel.TEST_PATH);

        exists.checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = ReadFailedException.class)
    public void testExistsWithAsyncRemoteOperatonFailure() throws Exception {
        setupInitialMockActorContext(DoNothingActor.class);

        actorContext.setExecuteRemoteOperationFailure(new Exception("mock"));
        actorContext.setExecuteRemoteOperationResponse(null);

        TransactionProxy transactionProxy = new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, TestModel.createTestContext());

        CheckedFuture<Boolean, ReadFailedException> exists =
                transactionProxy.exists(TestModel.TEST_PATH);

        exists.checkedGet(5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private void queryMessagesAndVerify(ActorRef actorRef, Class<?> expMessageType) throws Exception {
        Object messages = Await.result(ask(actorRef, "messages", 5000L),
                ActorContext.AWAIT_DURATION);

        assertNotNull("Returned message is null", messages);
        assertTrue("Expected returned message of type List", messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;
        assertEquals("Returned message List size", 1, listMessages.size());
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
