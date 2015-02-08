package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.READ_ONLY;
import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.READ_WRITE;
import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.WRITE_ONLY;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
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
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

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

    private final String memberName = "mock-member";

    private final Builder dataStoreContextBuilder = DatastoreContext.newBuilder().operationTimeoutInSeconds(2).
            shardBatchedModificationCount(1);

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

        doReturn(getSystem()).when(mockActorContext).getActorSystem();
        doReturn(memberName).when(mockActorContext).getCurrentMemberName();
        doReturn(schemaContext).when(mockActorContext).getSchemaContext();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(dataStoreContextBuilder.build()).when(mockActorContext).getDatastoreContext();
        doReturn(10).when(mockActorContext).getTransactionOutstandingOperationLimit();

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
                if(CreateTransaction.SERIALIZABLE_CLASS.equals(argument.getClass())) {
                    CreateTransaction obj = CreateTransaction.fromSerializable(argument);
                    return obj.getTransactionId().startsWith(memberName) &&
                            obj.getTransactionType() == type.ordinal();
                }

                return false;
            }
        };

        return argThat(matcher);
    }

    private DataExists eqSerializedDataExists() {
        ArgumentMatcher<DataExists> matcher = new ArgumentMatcher<DataExists>() {
            @Override
            public boolean matches(Object argument) {
                return DataExists.SERIALIZABLE_CLASS.equals(argument.getClass()) &&
                       DataExists.fromSerializable(argument).getPath().equals(TestModel.TEST_PATH);
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

    private ReadData eqSerializedReadData() {
        return eqSerializedReadData(TestModel.TEST_PATH);
    }

    private ReadData eqSerializedReadData(final YangInstanceIdentifier path) {
        ArgumentMatcher<ReadData> matcher = new ArgumentMatcher<ReadData>() {
            @Override
            public boolean matches(Object argument) {
                return ReadData.SERIALIZABLE_CLASS.equals(argument.getClass()) &&
                       ReadData.fromSerializable(argument).getPath().equals(path);
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

    private WriteData eqLegacyWriteData(final NormalizedNode<?, ?> nodeToWrite) {
        ArgumentMatcher<WriteData> matcher = new ArgumentMatcher<WriteData>() {
            @Override
            public boolean matches(Object argument) {
                if(ShardTransactionMessages.WriteData.class.equals(argument.getClass())) {
                    WriteData obj = WriteData.fromSerializable(argument);
                    return obj.getPath().equals(TestModel.TEST_PATH) && obj.getData().equals(nodeToWrite);
                }

                return false;
            }
        };

        return argThat(matcher);
    }

    private MergeData eqLegacyMergeData(final NormalizedNode<?, ?> nodeToWrite) {
        ArgumentMatcher<MergeData> matcher = new ArgumentMatcher<MergeData>() {
            @Override
            public boolean matches(Object argument) {
                if(ShardTransactionMessages.MergeData.class.equals(argument.getClass())) {
                    MergeData obj = MergeData.fromSerializable(argument);
                    return obj.getPath().equals(TestModel.TEST_PATH) && obj.getData().equals(nodeToWrite);
                }

                return false;
            }
        };

        return argThat(matcher);
    }

    private DeleteData eqLegacyDeleteData(final YangInstanceIdentifier expPath) {
        ArgumentMatcher<DeleteData> matcher = new ArgumentMatcher<DeleteData>() {
            @Override
            public boolean matches(Object argument) {
                return ShardTransactionMessages.DeleteData.class.equals(argument.getClass()) &&
                       DeleteData.fromSerializable(argument).getPath().equals(expPath);
            }
        };

        return argThat(matcher);
    }

    private Future<Object> readySerializedTxReply(String path) {
        return Futures.successful((Object)new ReadyTransactionReply(path).toSerializable());
    }

    private Future<Object> readyTxReply(String path) {
        return Futures.successful((Object)new ReadyTransactionReply(path));
    }

    private Future<Object> readSerializedDataReply(NormalizedNode<?, ?> data,
            short transactionVersion) {
        return Futures.successful(new ReadDataReply(data, transactionVersion).toSerializable());
    }

    private Future<Object> readSerializedDataReply(NormalizedNode<?, ?> data) {
        return readSerializedDataReply(data, DataStoreVersions.CURRENT_VERSION);
    }

    private Future<ReadDataReply> readDataReply(NormalizedNode<?, ?> data) {
        return Futures.successful(new ReadDataReply(data, DataStoreVersions.CURRENT_VERSION));
    }

    private Future<Object> dataExistsSerializedReply(boolean exists) {
        return Futures.successful(new DataExistsReply(exists).toSerializable());
    }

    private Future<DataExistsReply> dataExistsReply(boolean exists) {
        return Futures.successful(new DataExistsReply(exists));
    }

    private Future<BatchedModificationsReply> batchedModificationsReply() {
        return Futures.successful(BatchedModificationsReply.INSTANCE);
    }

    private Future<Object> incompleteFuture(){
        return mock(Future.class);
    }

    private ActorSelection actorSelection(ActorRef actorRef) {
        return getSystem().actorSelection(actorRef.path());
    }

    private void expectBatchedModifications(ActorRef actorRef) {
        doReturn(batchedModificationsReply()).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));
    }

    private void expectBatchedModifications() {
        doReturn(batchedModificationsReply()).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class));
    }

    private void expectIncompleteBatchedModifications() {
        doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class));
    }

    private void expectReadyTransaction(ActorRef actorRef) {
        doReturn(readySerializedTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(ReadyTransaction.SERIALIZABLE_CLASS));
    }

    private void expectFailedBatchedModifications(ActorRef actorRef) {
        doReturn(Futures.failed(new TestException())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));
    }

    private CreateTransactionReply createTransactionReply(ActorRef actorRef, int transactionVersion){
        return CreateTransactionReply.newBuilder()
            .setTransactionActorPath(actorRef.path().toString())
            .setTransactionId("txn-1")
            .setMessageVersion(transactionVersion)
            .build();
    }

    private ActorRef setupActorContextWithoutInitialCreateTransaction(ActorSystem actorSystem) {
        ActorRef actorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
        doReturn(actorSystem.actorSelection(actorRef.path())).
                when(mockActorContext).actorSelection(actorRef.path().toString());

        doReturn(Futures.successful(actorSystem.actorSelection(actorRef.path()))).
                when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        doReturn(false).when(mockActorContext).isPathLocal(actorRef.path().toString());

        doReturn(10).when(mockActorContext).getTransactionOutstandingOperationLimit();

        return actorRef;
    }

    private ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem,
            TransactionType type, int transactionVersion) {
        ActorRef actorRef = setupActorContextWithoutInitialCreateTransaction(actorSystem);

        doReturn(Futures.successful(createTransactionReply(actorRef, transactionVersion))).when(mockActorContext).
                executeOperationAsync(eq(actorSystem.actorSelection(actorRef.path())),
                        eqCreateTransaction(memberName, type));

        return actorRef;
    }

    private ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem, TransactionType type) {
        return setupActorContextWithInitialCreateTransaction(actorSystem, type, DataStoreVersions.CURRENT_VERSION);
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

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_ONLY);

        doReturn(readSerializedDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", false, readOptional.isPresent());

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readSerializedDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        readOptional = transactionProxy.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());

        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadWithInvalidReplyMessageType() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqSerializedReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_ONLY);

        transactionProxy.read(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testReadWithAsyncRemoteOperatonFailure() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqSerializedReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_ONLY);

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
        doReturn(dataStoreContextBuilder.shardBatchedModificationCount(2).build()).
                when(mockActorContext).getDatastoreContext();

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectFailedBatchedModifications(actorRef);

        doReturn(readSerializedDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.delete(TestModel.TEST_PATH);

        try {
            propagateReadFailedExceptionCause(transactionProxy.read(TestModel.TEST_PATH));
        } finally {
            verify(mockActorContext, times(0)).executeOperationAsync(
                    eq(actorSelection(actorRef)), eqSerializedReadData());
        }
    }

    @Test
    public void testReadWithPriorRecordingOperationSuccessful() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef);

        doReturn(readSerializedDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, expectedNode);

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());
        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());

        InOrder inOrder = Mockito.inOrder(mockActorContext);
        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());
    }

    @Test(expected=IllegalStateException.class)
    public void testReadPreConditionCheck() {
        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);
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

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_ONLY);

        doReturn(dataExistsSerializedReply(false)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedDataExists());

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", false, exists);

        doReturn(dataExistsSerializedReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedDataExists());

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
                executeOperationAsync(eq(actorSelection(actorRef)), eqSerializedDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_ONLY);

        transactionProxy.exists(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testExistsWithAsyncRemoteOperatonFailure() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqSerializedDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_ONLY);

        propagateReadFailedExceptionCause(transactionProxy.exists(TestModel.TEST_PATH));
    }

    @Test(expected = TestException.class)
    public void testExistsWithPriorRecordingOperationFailure() throws Throwable {
        doReturn(dataStoreContextBuilder.shardBatchedModificationCount(2).build()).
                when(mockActorContext).getDatastoreContext();

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectFailedBatchedModifications(actorRef);

        doReturn(dataExistsSerializedReply(false)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,
                READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.delete(TestModel.TEST_PATH);

        try {
            propagateReadFailedExceptionCause(transactionProxy.exists(TestModel.TEST_PATH));
        } finally {
            verify(mockActorContext, times(0)).executeOperationAsync(
                    eq(actorSelection(actorRef)), eqSerializedDataExists());
        }
    }

    @Test
    public void testExistsWithPriorRecordingOperationSuccessful() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef);

        doReturn(dataExistsSerializedReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", true, exists);

        InOrder inOrder = Mockito.inOrder(mockActorContext);
        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedDataExists());
    }

    @Test(expected=IllegalStateException.class)
    public void testExistsPreConditionCheck() {
        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);
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
                assertEquals(String.format("Recording operation %d Future result type", i +1 ), expResultType,
                             Await.result(future, Duration.create(5, TimeUnit.SECONDS)).getClass());
            }
        }
    }

    @Test
    public void testWrite() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef);
        expectReadyTransaction(actorRef);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        // This sends the batched modification.
        transactionProxy.ready();

        verifyOneBatchedModification(actorRef, new WriteModification(TestModel.TEST_PATH, nodeToWrite));

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class);
    }

    @Test
    public void testWriteAfterAsyncRead() throws Throwable {
        ActorRef actorRef = setupActorContextWithoutInitialCreateTransaction(getSystem());

        Promise<Object> createTxPromise = akka.dispatch.Futures.promise();
        doReturn(createTxPromise).when(mockActorContext).executeOperationAsync(
                eq(getSystem().actorSelection(actorRef.path())),
                eqCreateTransaction(memberName, READ_WRITE));

        doReturn(readSerializedDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        expectBatchedModifications(actorRef);
        expectReadyTransaction(actorRef);

        final NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        final TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        final CountDownLatch readComplete = new CountDownLatch(1);
        final AtomicReference<Throwable> caughtEx = new AtomicReference<>();
        com.google.common.util.concurrent.Futures.addCallback(transactionProxy.read(TestModel.TEST_PATH),
                new  FutureCallback<Optional<NormalizedNode<?, ?>>>() {
                    @Override
                    public void onSuccess(Optional<NormalizedNode<?, ?>> result) {
                        try {
                            transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);
                        } catch (Exception e) {
                            caughtEx.set(e);
                        } finally {
                            readComplete.countDown();
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        caughtEx.set(t);
                        readComplete.countDown();
                    }
                });

        createTxPromise.success(createTransactionReply(actorRef, DataStoreVersions.CURRENT_VERSION));

        Uninterruptibles.awaitUninterruptibly(readComplete, 5, TimeUnit.SECONDS);

        if(caughtEx.get() != null) {
            throw caughtEx.get();
        }

        // This sends the batched modification.
        transactionProxy.ready();

        verifyOneBatchedModification(actorRef, new WriteModification(TestModel.TEST_PATH, nodeToWrite));

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class);
    }

    @Test(expected=IllegalStateException.class)
    public void testWritePreConditionCheck() {
        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_ONLY);
        transactionProxy.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
    }

    @Test(expected=IllegalStateException.class)
    public void testWriteAfterReadyPreConditionCheck() {
        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        transactionProxy.ready();

        transactionProxy.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
    }

    @Test
    public void testMerge() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef);
        expectReadyTransaction(actorRef);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        // This sends the batched modification.
        transactionProxy.ready();

        verifyOneBatchedModification(actorRef, new MergeModification(TestModel.TEST_PATH, nodeToWrite));

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class);
    }

    @Test
    public void testDelete() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        expectBatchedModifications(actorRef);
        expectReadyTransaction(actorRef);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        transactionProxy.delete(TestModel.TEST_PATH);

        // This sends the batched modification.
        transactionProxy.ready();

        verifyOneBatchedModification(actorRef, new DeleteModification(TestModel.TEST_PATH));

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class);
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

    @Test
    public void testReady() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readSerializedDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        expectBatchedModifications(actorRef);
        expectReadyTransaction(actorRef);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class);

        verifyCohortFutures(proxy, getSystem().actorSelection(actorRef.path()));

        verify(mockActorContext).executeOperationAsync(eq(actorSelection(actorRef)),
                isA(BatchedModifications.class));
    }

    private ActorRef testCompatibilityWithHeliumVersion(short version) throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(),
                READ_WRITE, version);

        NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readSerializedDataReply(testNode, version)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        doReturn(Futures.successful(new WriteDataReply().toSerializable(version))).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqLegacyWriteData(testNode));

        doReturn(Futures.successful(new MergeDataReply().toSerializable(version))).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqLegacyMergeData(testNode));

        doReturn(Futures.successful(new DeleteDataReply().toSerializable(version))).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqLegacyDeleteData(TestModel.TEST_PATH));

        expectReadyTransaction(actorRef);

        doReturn(actorRef.path().toString()).when(mockActorContext).resolvePath(eq(actorRef.path().toString()),
                eq(actorRef.path().toString()));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(TestModel.TEST_PATH).
                get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());
        assertEquals("Response NormalizedNode", testNode, readOptional.get());

        transactionProxy.write(TestModel.TEST_PATH, testNode);

        transactionProxy.merge(TestModel.TEST_PATH, testNode);

        transactionProxy.delete(TestModel.TEST_PATH);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                ShardTransactionMessages.WriteDataReply.class, ShardTransactionMessages.MergeDataReply.class,
                ShardTransactionMessages.DeleteDataReply.class);

        verifyCohortFutures(proxy, getSystem().actorSelection(actorRef.path()));

        return actorRef;
    }

    @Test
    public void testCompatibilityWithBaseHeliumVersion() throws Exception {
        ActorRef actorRef = testCompatibilityWithHeliumVersion(DataStoreVersions.BASE_HELIUM_VERSION);

        verify(mockActorContext).resolvePath(eq(actorRef.path().toString()),
                eq(actorRef.path().toString()));
    }

    @Test
    public void testCompatibilityWithHeliumR1Version() throws Exception {
        ActorRef actorRef = testCompatibilityWithHeliumVersion(DataStoreVersions.HELIUM_1_VERSION);

        verify(mockActorContext, Mockito.never()).resolvePath(eq(actorRef.path().toString()),
                eq(actorRef.path().toString()));
    }

    @Test
    public void testReadyWithRecordingOperationFailure() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectFailedBatchedModifications(actorRef);

        expectReadyTransaction(actorRef);

        doReturn(false).when(mockActorContext).isPathLocal(actorRef.path().toString());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyCohortFutures(proxy, TestException.class);

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(), TestException.class);
    }

    @Test
    public void testReadyWithReplyFailure() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef);

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)),
                        isA(ReadyTransaction.SERIALIZABLE_CLASS));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class);

        verifyCohortFutures(proxy, TestException.class);
    }

    @Test
    public void testReadyWithInitialCreateTransactionFailure() throws Exception {

        doReturn(Futures.failed(new PrimaryNotFoundException("mock"))).when(
                mockActorContext).findPrimaryShardAsync(anyString());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.delete(TestModel.TEST_PATH);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyCohortFutures(proxy, PrimaryNotFoundException.class);
    }

    @Test
    public void testReadyWithInvalidReplyMessageType() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef);

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)),
                        isA(ReadyTransaction.SERIALIZABLE_CLASS));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

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

    @Test
    public void testClose() throws Exception{
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        doReturn(readSerializedDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.close();

        verify(mockActorContext).sendOperationAsync(
                eq(actorSelection(actorRef)), isA(CloseTransaction.SERIALIZABLE_CLASS));
    }


    /**
     * Method to test a local Tx actor. The Tx paths are matched to decide if the
     * Tx actor is local or not. This is done by mocking the Tx actor path
     * and the caller paths and ensuring that the paths have the remote-address format
     *
     * Note: Since the default akka provider for test is not a RemoteActorRefProvider,
     * the paths returned for the actors for all the tests are not qualified remote paths.
     * Hence are treated as non-local/remote actors. In short, all tests except
     * few below run for remote actors
     *
     * @throws Exception
     */
    @Test
    public void testLocalTxActorRead() throws Exception {
        ActorSystem actorSystem = getSystem();
        ActorRef shardActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));

        doReturn(actorSystem.actorSelection(shardActorRef.path())).
            when(mockActorContext).actorSelection(shardActorRef.path().toString());

        doReturn(Futures.successful(actorSystem.actorSelection(shardActorRef.path()))).
            when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        String actorPath = "akka.tcp://system@127.0.0.1:2550/user/tx-actor";
        CreateTransactionReply createTransactionReply = CreateTransactionReply.newBuilder()
            .setTransactionId("txn-1").setTransactionActorPath(actorPath).build();

        doReturn(Futures.successful(createTransactionReply)).when(mockActorContext).
            executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                eqCreateTransaction(memberName, READ_ONLY));

        doReturn(true).when(mockActorContext).isPathLocal(actorPath);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext,READ_ONLY);

        // negative test case with null as the reply
        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
            any(ActorSelection.class), eqReadData());

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
            TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", false, readOptional.isPresent());

        // test case with node as read data reply
        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
            any(ActorSelection.class), eqReadData());

        readOptional = transactionProxy.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());

        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());

        // test for local data exists
        doReturn(dataExistsReply(true)).when(mockActorContext).executeOperationAsync(
            any(ActorSelection.class), eqDataExists());

        boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", true, exists);
    }

    @Test
    public void testLocalTxActorReady() throws Exception {
        ActorSystem actorSystem = getSystem();
        ActorRef shardActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));

        doReturn(actorSystem.actorSelection(shardActorRef.path())).
            when(mockActorContext).actorSelection(shardActorRef.path().toString());

        doReturn(Futures.successful(actorSystem.actorSelection(shardActorRef.path()))).
            when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        String actorPath = "akka.tcp://system@127.0.0.1:2550/user/tx-actor";
        CreateTransactionReply createTransactionReply = CreateTransactionReply.newBuilder().
            setTransactionId("txn-1").setTransactionActorPath(actorPath).
            setMessageVersion(DataStoreVersions.CURRENT_VERSION).build();

        doReturn(Futures.successful(createTransactionReply)).when(mockActorContext).
            executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                eqCreateTransaction(memberName, WRITE_ONLY));

        doReturn(true).when(mockActorContext).isPathLocal(actorPath);

        doReturn(batchedModificationsReply()).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class);

        // testing ready
        doReturn(readyTxReply(shardActorRef.path().toString())).when(mockActorContext).executeOperationAsync(
            any(ActorSelection.class), isA(ReadyTransaction.class));

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyCohortFutures(proxy, getSystem().actorSelection(shardActorRef.path()));
    }

    private static interface TransactionProxyOperation {
        void run(TransactionProxy transactionProxy);
    }

    private void throttleOperation(TransactionProxyOperation operation) {
        throttleOperation(operation, 1, true);
    }

    private void throttleOperation(TransactionProxyOperation operation, int outstandingOpsLimit, boolean shardFound){
        ActorSystem actorSystem = getSystem();
        ActorRef shardActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));

        doReturn(outstandingOpsLimit).when(mockActorContext).getTransactionOutstandingOperationLimit();

        doReturn(actorSystem.actorSelection(shardActorRef.path())).
                when(mockActorContext).actorSelection(shardActorRef.path().toString());

        if(shardFound) {
            doReturn(Futures.successful(actorSystem.actorSelection(shardActorRef.path()))).
                    when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
        } else {
            doReturn(Futures.failed(new Exception("not found")))
                    .when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
        }

        String actorPath = "akka.tcp://system@127.0.0.1:2550/user/tx-actor";
        CreateTransactionReply createTransactionReply = CreateTransactionReply.newBuilder().
                setTransactionId("txn-1").setTransactionActorPath(actorPath).
                setMessageVersion(DataStoreVersions.CURRENT_VERSION).build();

        doReturn(Futures.successful(createTransactionReply)).when(mockActorContext).
                executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                        eqCreateTransaction(memberName, READ_WRITE));

        doReturn(true).when(mockActorContext).isPathLocal(actorPath);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        long start = System.nanoTime();

        operation.run(transactionProxy);

        long end = System.nanoTime();

        long expected = TimeUnit.SECONDS.toNanos(mockActorContext.getDatastoreContext().getOperationTimeoutInSeconds());
        Assert.assertTrue(String.format("Expected elapsed time: %s. Actual: %s",
                expected, (end-start)), (end - start) > expected);

    }

    private void completeOperation(TransactionProxyOperation operation){
        completeOperation(operation, true);
    }

    private void completeOperation(TransactionProxyOperation operation, boolean shardFound){
        ActorSystem actorSystem = getSystem();
        ActorRef shardActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));

        doReturn(1).when(mockActorContext).getTransactionOutstandingOperationLimit();

        doReturn(actorSystem.actorSelection(shardActorRef.path())).
                when(mockActorContext).actorSelection(shardActorRef.path().toString());

        if(shardFound) {
            doReturn(Futures.successful(actorSystem.actorSelection(shardActorRef.path()))).
                    when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
        } else {
            doReturn(Futures.failed(new Exception("not found")))
                    .when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
        }

        String actorPath = "akka.tcp://system@127.0.0.1:2550/user/tx-actor";
        CreateTransactionReply createTransactionReply = CreateTransactionReply.newBuilder().
                setTransactionId("txn-1").setTransactionActorPath(actorPath).
                setMessageVersion(DataStoreVersions.CURRENT_VERSION).build();

        doReturn(Futures.successful(createTransactionReply)).when(mockActorContext).
                executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                        eqCreateTransaction(memberName, READ_WRITE));

        doReturn(true).when(mockActorContext).isPathLocal(actorPath);

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        long start = System.nanoTime();

        operation.run(transactionProxy);

        long end = System.nanoTime();

        long expected = TimeUnit.SECONDS.toNanos(mockActorContext.getDatastoreContext().getOperationTimeoutInSeconds());
        Assert.assertTrue(String.format("Expected elapsed time: %s. Actual: %s",
                expected, (end-start)), (end - start) <= expected);
    }

    public void testWriteThrottling(boolean shardFound){

        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectBatchedModifications();

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);
            }
        }, 1, shardFound);
    }

    @Test
    public void testWriteThrottlingWhenShardFound(){
        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectIncompleteBatchedModifications();

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);
            }
        });
    }

    @Test
    public void testWriteThrottlingWhenShardNotFound(){
        // Confirm that there is no throttling when the Shard is not found
        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectBatchedModifications();

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);
            }
        }, false);

    }


    @Test
    public void testWriteCompletion(){
        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectBatchedModifications();

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);
            }
        });
    }

    @Test
    public void testMergeThrottlingWhenShardFound(){

        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToMerge = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectIncompleteBatchedModifications();

                transactionProxy.merge(TestModel.TEST_PATH, nodeToMerge);

                transactionProxy.merge(TestModel.TEST_PATH, nodeToMerge);
            }
        });
    }

    @Test
    public void testMergeThrottlingWhenShardNotFound(){

        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToMerge = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectBatchedModifications();

                transactionProxy.merge(TestModel.TEST_PATH, nodeToMerge);

                transactionProxy.merge(TestModel.TEST_PATH, nodeToMerge);
            }
        }, false);
    }

    @Test
    public void testMergeCompletion(){
        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToMerge = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectBatchedModifications();

                transactionProxy.merge(TestModel.TEST_PATH, nodeToMerge);

                transactionProxy.merge(TestModel.TEST_PATH, nodeToMerge);
            }
        });

    }

    @Test
    public void testDeleteThrottlingWhenShardFound(){

        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                expectIncompleteBatchedModifications();

                transactionProxy.delete(TestModel.TEST_PATH);

                transactionProxy.delete(TestModel.TEST_PATH);
            }
        });
    }


    @Test
    public void testDeleteThrottlingWhenShardNotFound(){

        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                expectBatchedModifications();

                transactionProxy.delete(TestModel.TEST_PATH);

                transactionProxy.delete(TestModel.TEST_PATH);
            }
        }, false);
    }

    @Test
    public void testDeleteCompletion(){
        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                expectBatchedModifications();

                transactionProxy.delete(TestModel.TEST_PATH);

                transactionProxy.delete(TestModel.TEST_PATH);
            }
        });

    }

    @Test
    public void testReadThrottlingWhenShardFound(){

        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                        any(ActorSelection.class), eqReadData());

                transactionProxy.read(TestModel.TEST_PATH);

                transactionProxy.read(TestModel.TEST_PATH);
            }
        });
    }

    @Test
    public void testReadThrottlingWhenShardNotFound(){

        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                        any(ActorSelection.class), eqReadData());

                transactionProxy.read(TestModel.TEST_PATH);

                transactionProxy.read(TestModel.TEST_PATH);
            }
        }, false);
    }


    @Test
    public void testReadCompletion(){
        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToRead = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                doReturn(readDataReply(nodeToRead)).when(mockActorContext).executeOperationAsync(
                        any(ActorSelection.class), eqReadData());

                transactionProxy.read(TestModel.TEST_PATH);

                transactionProxy.read(TestModel.TEST_PATH);
            }
        });

    }

    @Test
    public void testExistsThrottlingWhenShardFound(){

        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                        any(ActorSelection.class), eqDataExists());

                transactionProxy.exists(TestModel.TEST_PATH);

                transactionProxy.exists(TestModel.TEST_PATH);
            }
        });
    }

    @Test
    public void testExistsThrottlingWhenShardNotFound(){

        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                        any(ActorSelection.class), eqDataExists());

                transactionProxy.exists(TestModel.TEST_PATH);

                transactionProxy.exists(TestModel.TEST_PATH);
            }
        }, false);
    }


    @Test
    public void testExistsCompletion(){
        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                doReturn(dataExistsReply(true)).when(mockActorContext).executeOperationAsync(
                        any(ActorSelection.class), eqDataExists());

                transactionProxy.exists(TestModel.TEST_PATH);

                transactionProxy.exists(TestModel.TEST_PATH);
            }
        });

    }

    @Test
    public void testReadyThrottling(){

        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectBatchedModifications();

                doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                        any(ActorSelection.class), any(ReadyTransaction.class));

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

                transactionProxy.ready();
            }
        });
    }

    @Test
    public void testReadyThrottlingWithTwoTransactionContexts(){

        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
                NormalizedNode<?, ?> carsNode = ImmutableNodes.containerNode(CarsModel.BASE_QNAME);

                expectBatchedModifications();

                doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                        any(ActorSelection.class), any(ReadyTransaction.class));

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

                transactionProxy.write(TestModel.TEST_PATH, carsNode);

                transactionProxy.ready();
            }
        }, 2, true);
    }

    @Test
    public void testModificationOperationBatching() throws Throwable {
        doReturn(dataStoreContextBuilder.shardBatchedModificationCount(3).build()).
                when(mockActorContext).getDatastoreContext();

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        expectBatchedModifications(actorRef);

        expectReadyTransaction(actorRef);

        YangInstanceIdentifier writePath1 = TestModel.TEST_PATH;
        NormalizedNode<?, ?> writeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        YangInstanceIdentifier writePath2 = TestModel.OUTER_LIST_PATH;
        NormalizedNode<?, ?> writeNode2 = ImmutableNodes.containerNode(TestModel.OUTER_LIST_QNAME);

        YangInstanceIdentifier writePath3 = TestModel.INNER_LIST_PATH;
        NormalizedNode<?, ?> writeNode3 = ImmutableNodes.containerNode(TestModel.INNER_LIST_QNAME);

        YangInstanceIdentifier mergePath1 = TestModel.TEST_PATH;
        NormalizedNode<?, ?> mergeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        YangInstanceIdentifier mergePath2 = TestModel.OUTER_LIST_PATH;
        NormalizedNode<?, ?> mergeNode2 = ImmutableNodes.containerNode(TestModel.OUTER_LIST_QNAME);

        YangInstanceIdentifier mergePath3 = TestModel.INNER_LIST_PATH;
        NormalizedNode<?, ?> mergeNode3 = ImmutableNodes.containerNode(TestModel.INNER_LIST_QNAME);

        YangInstanceIdentifier deletePath1 = TestModel.TEST_PATH;
        YangInstanceIdentifier deletePath2 = TestModel.OUTER_LIST_PATH;

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        transactionProxy.write(writePath1, writeNode1);
        transactionProxy.write(writePath2, writeNode2);
        transactionProxy.delete(deletePath1);
        transactionProxy.merge(mergePath1, mergeNode1);
        transactionProxy.merge(mergePath2, mergeNode2);
        transactionProxy.write(writePath3, writeNode3);
        transactionProxy.merge(mergePath3, mergeNode3);
        transactionProxy.delete(deletePath2);

        // This sends the last batch.
        transactionProxy.ready();

        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 3, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), new WriteModification(writePath1, writeNode1),
                new WriteModification(writePath2, writeNode2), new DeleteModification(deletePath1));

        verifyBatchedModifications(batchedModifications.get(1), new MergeModification(mergePath1, mergeNode1),
                new MergeModification(mergePath2, mergeNode2), new WriteModification(writePath3, writeNode3));

        verifyBatchedModifications(batchedModifications.get(2), new MergeModification(mergePath3, mergeNode3),
                new DeleteModification(deletePath2));

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class, BatchedModificationsReply.class, BatchedModificationsReply.class);
    }

    @Test
    public void testModificationOperationBatchingWithInterleavedReads() throws Throwable {
        doReturn(dataStoreContextBuilder.shardBatchedModificationCount(10).build()).
                when(mockActorContext).getDatastoreContext();

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        expectBatchedModifications(actorRef);

        YangInstanceIdentifier writePath1 = TestModel.TEST_PATH;
        NormalizedNode<?, ?> writeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        YangInstanceIdentifier writePath2 = TestModel.OUTER_LIST_PATH;
        NormalizedNode<?, ?> writeNode2 = ImmutableNodes.containerNode(TestModel.OUTER_LIST_QNAME);

        YangInstanceIdentifier mergePath1 = TestModel.TEST_PATH;
        NormalizedNode<?, ?> mergeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        YangInstanceIdentifier mergePath2 = TestModel.INNER_LIST_PATH;
        NormalizedNode<?, ?> mergeNode2 = ImmutableNodes.containerNode(TestModel.INNER_LIST_QNAME);

        YangInstanceIdentifier deletePath = TestModel.OUTER_LIST_PATH;

        doReturn(readSerializedDataReply(writeNode2)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData(writePath2));

        doReturn(readSerializedDataReply(mergeNode2)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData(mergePath2));

        doReturn(dataExistsSerializedReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedDataExists());

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        transactionProxy.write(writePath1, writeNode1);
        transactionProxy.write(writePath2, writeNode2);

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(writePath2).
                get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());
        assertEquals("Response NormalizedNode", writeNode2, readOptional.get());

        transactionProxy.merge(mergePath1, mergeNode1);
        transactionProxy.merge(mergePath2, mergeNode2);

        readOptional = transactionProxy.read(mergePath2).get(5, TimeUnit.SECONDS);

        transactionProxy.delete(deletePath);

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();
        assertEquals("Exists response", true, exists);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());
        assertEquals("Response NormalizedNode", mergeNode2, readOptional.get());

        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 3, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), new WriteModification(writePath1, writeNode1),
                new WriteModification(writePath2, writeNode2));

        verifyBatchedModifications(batchedModifications.get(1), new MergeModification(mergePath1, mergeNode1),
                new MergeModification(mergePath2, mergeNode2));

        verifyBatchedModifications(batchedModifications.get(2), new DeleteModification(deletePath));

        InOrder inOrder = Mockito.inOrder(mockActorContext);
        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData(writePath2));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData(mergePath2));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedDataExists());

        verifyRecordingOperationFutures(transactionProxy.getRecordedOperationFutures(),
                BatchedModificationsReply.class, BatchedModificationsReply.class, BatchedModificationsReply.class);
    }

    private List<BatchedModifications> captureBatchedModifications(ActorRef actorRef) {
        ArgumentCaptor<BatchedModifications> batchedModificationsCaptor =
                ArgumentCaptor.forClass(BatchedModifications.class);
        verify(mockActorContext, Mockito.atLeastOnce()).executeOperationAsync(
                eq(actorSelection(actorRef)), batchedModificationsCaptor.capture());

        List<BatchedModifications> batchedModifications = filterCaptured(
                batchedModificationsCaptor, BatchedModifications.class);
        return batchedModifications;
    }

    private <T> List<T> filterCaptured(ArgumentCaptor<T> captor, Class<T> type) {
        List<T> captured = new ArrayList<>();
        for(T c: captor.getAllValues()) {
            if(type.isInstance(c)) {
                captured.add(c);
            }
        }

        return captured;
    }

    private void verifyOneBatchedModification(ActorRef actorRef, Modification expected) {
        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 1, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), expected);
    }

    private void verifyBatchedModifications(Object message, Modification... expected) {
        assertEquals("Message type", BatchedModifications.class, message.getClass());
        BatchedModifications batchedModifications = (BatchedModifications)message;
        assertEquals("BatchedModifications size", expected.length, batchedModifications.getModifications().size());
        for(int i = 0; i < batchedModifications.getModifications().size(); i++) {
            Modification actual = batchedModifications.getModifications().get(i);
            assertEquals("Modification type", expected[i].getClass(), actual.getClass());
            assertEquals("getPath", ((AbstractModification)expected[i]).getPath(),
                    ((AbstractModification)actual).getPath());
            if(actual instanceof WriteModification) {
                assertEquals("getData", ((WriteModification)expected[i]).getData(),
                        ((WriteModification)actual).getData());
            }
        }
    }
}
