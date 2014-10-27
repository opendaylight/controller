package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.DeleteDataReply;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.MergeDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.READ_ONLY;
import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.READ_WRITE;
import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.WRITE_ONLY;

@SuppressWarnings("resource")
public class TransactionProxyTest {

    @SuppressWarnings("serial")
    static class TestException extends RuntimeException {
    }

    static interface Invoker {
        CheckedFuture<?, ReadFailedException> invoke(TransactionProxy proxy) throws Exception;
    }

    private static ActorSystem system;

    private final Configuration configuration = new MockConfiguration();

    @Mock
    private ActorContext mockActorContext;

    private SchemaContext schemaContext;

    @Mock
    private ClusterWrapper mockClusterWrapper;

    String memberName = "mock-member";

    @BeforeClass
    public static void setUpClass() throws IOException {

        Config config = ConfigFactory.parseMap(ImmutableMap.<String, Object>builder().
                put("akka.actor.default-dispatcher.type",
                        "akka.testkit.CallingThreadDispatcherConfigurator").build()).
                withFallback(ConfigFactory.load());
        system = ActorSystem.create("test", config);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

        schemaContext = TestModel.createTestContext();

        DatastoreContext dataStoreContext = DatastoreContext.newBuilder().build();

        doReturn(getSystem()).when(mockActorContext).getActorSystem();
        doReturn(memberName).when(mockActorContext).getCurrentMemberName();
        doReturn(schemaContext).when(mockActorContext).getSchemaContext();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(dataStoreContext).when(mockActorContext).getDatastoreContext();

        ShardStrategyFactory.setConfiguration(configuration);
    }

    private ActorSystem getSystem() {
        return system;
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
                return (argument instanceof DataExists) &&
                    ((DataExists)argument).getPath().equals(TestModel.TEST_PATH);
            }
        };

        return argThat(matcher);
    }

    private ReadData eqReadData() {
        ArgumentMatcher<ReadData> matcher = new ArgumentMatcher<ReadData>() {
            @Override
            public boolean matches(Object argument) {
                return (argument instanceof ReadData) &&
                    ((ReadData)argument).getPath().equals(TestModel.TEST_PATH);
            }
        };

        return argThat(matcher);
    }

    private WriteData eqWriteData(final NormalizedNode<?, ?> nodeToWrite) {
        ArgumentMatcher<WriteData> matcher = new ArgumentMatcher<WriteData>() {
            @Override
            public boolean matches(Object argument) {
                if(argument instanceof WriteData) {
                    WriteData obj = (WriteData) argument;
                    return obj.getPath().equals(TestModel.TEST_PATH) &&
                        obj.getData().equals(nodeToWrite);
                }
                return false;
            }
        };

        return argThat(matcher);
    }

    private MergeData eqMergeData(final NormalizedNode<?, ?> nodeToWrite) {
        ArgumentMatcher<MergeData> matcher = new ArgumentMatcher<MergeData>() {
            @Override
            public boolean matches(Object argument) {
                if(argument instanceof MergeData) {
                    MergeData obj = ((MergeData) argument);
                    return obj.getPath().equals(TestModel.TEST_PATH) &&
                        obj.getData().equals(nodeToWrite);
                }

               return false;
            }
        };

        return argThat(matcher);
    }

    private DeleteData eqDeleteData() {
        ArgumentMatcher<DeleteData> matcher = new ArgumentMatcher<DeleteData>() {
            @Override
            public boolean matches(Object argument) {
                return argument instanceof DeleteData &&
                    ((DeleteData)argument).getPath().equals(TestModel.TEST_PATH);
            }
        };

        return argThat(matcher);
    }

    private Future<Object> readyTxReply(String path) {
        return Futures.successful((Object)new ReadyTransactionReply(path));
    }

    private Future<ReadDataReply> readDataReply(NormalizedNode<?, ?> data) {
        return Futures.successful(new ReadDataReply(data));
    }

    private Future<DataExistsReply> dataExistsReply(boolean exists) {
        return Futures.successful(new DataExistsReply(exists));
    }

    private Future<WriteDataReply> writeDataReply() {
        return Futures.successful(new WriteDataReply());
    }

    private Future<MergeDataReply> mergeDataReply() {
        return Futures.successful(new MergeDataReply());
    }

    private Future<DeleteDataReply> deleteDataReply() {
        return Futures.successful(new DeleteDataReply());
    }

    private ActorSelection actorSelection(ActorRef actorRef) {
        return getSystem().actorSelection(actorRef.path());
    }

    private CreateTransactionReply createTransactionReply(ActorRef actorRef){
        return CreateTransactionReply.newBuilder()
            .setTransactionActorPath(actorRef.path().toString())
            .setTransactionId("txn-1").build();
    }

    private ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem, TransactionType type) {
        ActorRef actorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
        doReturn(actorSystem.actorSelection(actorRef.path())).
                when(mockActorContext).actorSelection(actorRef.path().toString());

        doReturn(Futures.successful(actorSystem.actorSelection(actorRef.path()))).
                when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        doReturn(Futures.successful(createTransactionReply(actorRef))).when(mockActorContext).
                executeOperationAsync(eq(actorSystem.actorSelection(actorRef.path())),
                        eqCreateTransaction(memberName, type));

        return actorRef;
    }

    private void propagateReadFailedExceptionCause(CheckedFuture<?, ReadFailedException> future)
            throws Throwable {

        try {
            future.checkedGet(5, TimeUnit.SECONDS);
            fail("Expected ReadFailedException");
        } catch(ReadFailedException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testRead() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData());

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", false, readOptional.isPresent());

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData());

        readOptional = transactionProxy.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());

        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadWithInvalidReplyMessageType() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY);

        transactionProxy.read(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testReadWithAsyncRemoteOperatonFailure() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY);

        propagateReadFailedExceptionCause(transactionProxy.read(TestModel.TEST_PATH));
    }

    private void testExceptionOnInitialCreateTransaction(Exception exToThrow, Invoker invoker)
            throws Throwable {
        ActorRef actorRef = getSystem().actorOf(Props.create(DoNothingActor.class));

        if (exToThrow instanceof PrimaryNotFoundException) {
            doReturn(Futures.failed(exToThrow)).when(mockActorContext).findPrimaryShardAsync(anyString());
        } else {
            doReturn(Futures.successful(getSystem().actorSelection(actorRef.path()))).
                    when(mockActorContext).findPrimaryShardAsync(anyString());
        }

        doReturn(Futures.failed(exToThrow)).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), any());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_ONLY);

        propagateReadFailedExceptionCause(invoker.invoke(transactionProxy));
    }

    private void testReadWithExceptionOnInitialCreateTransaction(Exception exToThrow) throws Throwable {
        testExceptionOnInitialCreateTransaction(exToThrow, new Invoker() {
            @Override
            public CheckedFuture<?, ReadFailedException> invoke(TransactionProxy proxy) throws Exception {
                return proxy.read(TestModel.TEST_PATH);
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

    @Test(expected = TestException.class)
    public void testReadWithPriorRecordingOperationFailure() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(writeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqWriteData(nodeToWrite));

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqDeleteData());

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.delete(TestModel.TEST_PATH);

        try {
            propagateReadFailedExceptionCause(transactionProxy.read(TestModel.TEST_PATH));
        } finally {
            verify(mockActorContext, times(0)).executeOperationAsync(
                    eq(actorSelection(actorRef)), eqReadData());
        }
    }

    @Test
    public void testReadWithPriorRecordingOperationSuccessful() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(writeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqWriteData(expectedNode));

        doReturn(readDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, expectedNode);

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());

        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());
    }

    @Test(expected=IllegalStateException.class)
    public void testReadPreConditionCheck() {

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        transactionProxy.read(TestModel.TEST_PATH);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidCreateTransactionReply() throws Throwable {
        ActorRef actorRef = getSystem().actorOf(Props.create(DoNothingActor.class));

        doReturn(getSystem().actorSelection(actorRef.path())).when(mockActorContext).
            actorSelection(actorRef.path().toString());

        doReturn(Futures.successful(getSystem().actorSelection(actorRef.path()))).
            when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        doReturn(Futures.successful(new Object())).when(mockActorContext).executeOperationAsync(
            eq(getSystem().actorSelection(actorRef.path())), eqCreateTransaction(memberName, READ_ONLY));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_ONLY);

        propagateReadFailedExceptionCause(transactionProxy.read(TestModel.TEST_PATH));
    }

    @Test
    public void testExists() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY);

        doReturn(dataExistsReply(false)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists());

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", false, exists);

        doReturn(dataExistsReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists());

        exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", true, exists);
    }

    @Test(expected = PrimaryNotFoundException.class)
    public void testExistsWhenAPrimaryNotFoundExceptionIsThrown() throws Throwable {
        testExceptionOnInitialCreateTransaction(new PrimaryNotFoundException("test"), new Invoker() {
            @Override
            public CheckedFuture<?, ReadFailedException> invoke(TransactionProxy proxy) throws Exception {
                return proxy.exists(TestModel.TEST_PATH);
            }
        });
    }

    @Test(expected = ReadFailedException.class)
    public void testExistsWithInvalidReplyMessageType() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY);

        transactionProxy.exists(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testExistsWithAsyncRemoteOperatonFailure() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY);

        propagateReadFailedExceptionCause(transactionProxy.exists(TestModel.TEST_PATH));
    }

    @Test(expected = TestException.class)
    public void testExistsWithPriorRecordingOperationFailure() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(writeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqWriteData(nodeToWrite));

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqDeleteData());

        doReturn(dataExistsReply(false)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.delete(TestModel.TEST_PATH);

        try {
            propagateReadFailedExceptionCause(transactionProxy.exists(TestModel.TEST_PATH));
        } finally {
            verify(mockActorContext, times(0)).executeOperationAsync(
                    eq(actorSelection(actorRef)), eqDataExists());
        }
    }

    @Test
    public void testExistsWithPriorRecordingOperationSuccessful() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(writeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqWriteData(nodeToWrite));

        doReturn(dataExistsReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", true, exists);
    }

    @Test(expected=IllegalStateException.class)
    public void testxistsPreConditionCheck() {

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        transactionProxy.exists(TestModel.TEST_PATH);
    }

    private void verifyRecordingOperationFutures(List<Future<Object>> futures,
            Class<?>... expResultTypes) throws Exception {
        assertEquals("getRecordingOperationFutures size", expResultTypes.length, futures.size());

        int i = 0;
        for( Future<Object> future: futures) {
            assertNotNull("Recording operation Future is null", future);

            Class<?> expResultType = expResultTypes[i++];
            if(Throwable.class.isAssignableFrom(expResultType)) {
                try {
                    Await.result(future, Duration.create(5, TimeUnit.SECONDS));
                    fail("Expected exception from recording operation Future");
                } catch(Exception e) {
                    // Expected
                }
            } else {
                assertEquals("Recording operation Future result type", expResultType,
                             Await.result(future, Duration.create(5, TimeUnit.SECONDS)).getClass());
            }
        }
    }

    @Test
    public void testWrite() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(writeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqWriteData(nodeToWrite));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqWriteData(nodeToWrite));

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                WriteDataReply.class);
    }

    @Test(expected=IllegalStateException.class)
    public void testWritePreConditionCheck() {

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY);

        transactionProxy.write(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME));
    }

    @Test(expected=IllegalStateException.class)
    public void testWriteAfterReadyPreConditionCheck() {

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        transactionProxy.ready();

        transactionProxy.write(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME));
    }

    @Test
    public void testMerge() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(mergeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqMergeData(nodeToWrite));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqMergeData(nodeToWrite));

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                MergeDataReply.class);
    }

    @Test
    public void testDelete() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        doReturn(deleteDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDeleteData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        transactionProxy.delete(TestModel.TEST_PATH);

        verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDeleteData());

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                DeleteDataReply.class);
    }

    private void verifyCohortFutures(ThreePhaseCommitCohortProxy proxy,
        Object... expReplies) throws Exception {
        assertEquals("getReadyOperationFutures size", expReplies.length,
                proxy.getCohortFutures().size());

        int i = 0;
        for( Future<ActorSelection> future: proxy.getCohortFutures()) {
            assertNotNull("Ready operation Future is null", future);

            Object expReply = expReplies[i++];
            if(expReply instanceof ActorSelection) {
                ActorSelection actual = Await.result(future, Duration.create(5, TimeUnit.SECONDS));
                assertEquals("Cohort actor path", expReply, actual);
            } else {
                // Expecting exception.
                try {
                    Await.result(future, Duration.create(5, TimeUnit.SECONDS));
                    fail("Expected exception from ready operation Future");
                } catch(Exception e) {
                    // Expected
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReady() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData());

        doReturn(writeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqWriteData(nodeToWrite));

        doReturn(readyTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(ReadyTransaction.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                WriteDataReply.class);

        verifyCohortFutures(proxy, getSystem().actorSelection(actorRef.path()));
    }

    @Test
    public void testReadyWithRecordingOperationFailure() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(mergeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqMergeData(nodeToWrite));

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqWriteData(nodeToWrite));

        doReturn(readyTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(ReadyTransaction.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyCohortFutures(proxy, TestException.class);

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                MergeDataReply.class, TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadyWithReplyFailure() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(mergeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqMergeData(nodeToWrite));

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), isA(ReadyTransaction.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                MergeDataReply.class);

        verifyCohortFutures(proxy, TestException.class);
    }

    @Test
    public void testReadyWithInitialCreateTransactionFailure() throws Exception {

        doReturn(Futures.failed(new PrimaryNotFoundException("mock"))).when(
                mockActorContext).findPrimaryShardAsync(anyString());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.delete(TestModel.TEST_PATH);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyCohortFutures(proxy, PrimaryNotFoundException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadyWithInvalidReplyMessageType() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(writeDataReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqWriteData(nodeToWrite));

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), isA(ReadyTransaction.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                WRITE_ONLY);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyCohortFutures(proxy, IllegalArgumentException.class);
    }

    @Test
    public void testGetIdentifier() {
        setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);
        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                TransactionProxy.TransactionType.READ_ONLY);

        Object id = transactionProxy.getIdentifier();
        assertNotNull("getIdentifier returned null", id);
        assertTrue("Invalid identifier: " + id, id.toString().startsWith(memberName));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testClose() throws Exception{
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.close();

        verify(mockActorContext).sendOperationAsync(
                eq(actorSelection(actorRef)), isA(CloseTransaction.SERIALIZABLE_CLASS));
    }
}
