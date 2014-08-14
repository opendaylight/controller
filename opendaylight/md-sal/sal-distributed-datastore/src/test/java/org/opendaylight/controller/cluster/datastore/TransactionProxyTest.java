package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.exceptions.OperationTimeoutException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteDataReply;
import org.opendaylight.controller.cluster.datastore.messages.MergeDataReply;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.datastore.utils.MockActorContext;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.Executors;

import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionProxyTest extends AbstractActorTest {

    private final Configuration configuration = new MockConfiguration();

    private final ActorContext testContext =
        new ActorContext(getSystem(),
            getSystem().actorOf(Props.create(DoNothingActor.class)),
            new MockClusterWrapper(), configuration);

    private final ListeningExecutorService transactionExecutor =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    @Before
    public void setUp() {
        ShardStrategyFactory.setConfiguration(configuration);
    }

    @After
    public void tearDown() {
        transactionExecutor.shutdownNow();
    }

    @Test
    public void testRead() throws Exception {
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext
            .setExecuteLocalOperationResponse(createPrimaryFound(actorRef));
        actorContext
            .setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");


        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());


        actorContext.setExecuteRemoteOperationResponse(
            new ReadDataReply(TestModel.createTestContext(), null)
                .toSerializable());

        ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
            transactionProxy.read(TestModel.TEST_PATH);

        Optional<NormalizedNode<?, ?>> normalizedNodeOptional = read.get();

        Assert.assertFalse(normalizedNodeOptional.isPresent());

        actorContext.setExecuteRemoteOperationResponse(new ReadDataReply(
            TestModel.createTestContext(),
            ImmutableNodes.containerNode(TestModel.TEST_QNAME))
            .toSerializable());

        read = transactionProxy.read(TestModel.TEST_PATH);

        normalizedNodeOptional = read.get();

        Assert.assertTrue(normalizedNodeOptional.isPresent());
    }

    @Test
    public void testExists() throws Exception {
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext
            .setExecuteLocalOperationResponse(createPrimaryFound(actorRef));
        actorContext
            .setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");


        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());


        actorContext.setExecuteRemoteOperationResponse(
            new DataExistsReply(false).toSerializable());

        CheckedFuture<Boolean, ReadFailedException> exists =
            transactionProxy.exists(TestModel.TEST_PATH);

        Assert.assertFalse(exists.checkedGet());

        actorContext.setExecuteRemoteOperationResponse(
            new DataExistsReply(true).toSerializable());

        exists = transactionProxy.exists(TestModel.TEST_PATH);

        Assert.assertTrue(exists.checkedGet());

        actorContext.setExecuteRemoteOperationResponse("bad message");

        exists = transactionProxy.exists(TestModel.TEST_PATH);

        try {
            exists.checkedGet();
            fail();
        } catch (ReadFailedException e) {
        }

    }

    @Test(expected = ReadFailedException.class)
    public void testReadWhenAnInvalidMessageIsSentInReply() throws Exception {
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext
            .setExecuteLocalOperationResponse(createPrimaryFound(actorRef));
        actorContext
            .setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());



        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>
            read = transactionProxy.read(TestModel.TEST_PATH);

        read.checkedGet();
    }

    @Test
    public void testReadWhenAPrimaryNotFoundExceptionIsThrown()
        throws Exception {
        final ActorContext actorContext = mock(ActorContext.class);

        when(actorContext.executeShardOperation(anyString(), any(), any(
            FiniteDuration.class)))
            .thenThrow(new PrimaryNotFoundException("test"));

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());


        ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
            transactionProxy.read(TestModel.TEST_PATH);

        Assert.assertFalse(read.get().isPresent());

    }


    @Test
    public void testReadWhenATimeoutExceptionIsThrown() throws Exception {
        final ActorContext actorContext = mock(ActorContext.class);

        when(actorContext.executeShardOperation(anyString(), any(), any(
            FiniteDuration.class)))
            .thenThrow(new OperationTimeoutException("test", new Exception("reason")));

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());


        ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
            transactionProxy.read(TestModel.TEST_PATH);

        Assert.assertFalse(read.get().isPresent());

    }

    @Test
    public void testReadWhenAAnyOtherExceptionIsThrown() throws Exception {
        final ActorContext actorContext = mock(ActorContext.class);

        when(actorContext.executeShardOperation(anyString(), any(), any(
            FiniteDuration.class))).thenThrow(new NullPointerException());

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());


        try {
            ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
                transactionProxy.read(TestModel.TEST_PATH);
            fail("A null pointer exception was expected");
        } catch (NullPointerException e) {

        }
    }



    @Test
    public void testWrite() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext
            .setExecuteLocalOperationResponse(createPrimaryFound(actorRef));
        actorContext
            .setExecuteShardOperationResponse(createTransactionReply(actorRef));
        Object obj = new WriteDataReply().toSerializable();
        actorContext.setExecuteRemoteOperationResponse(obj);

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.WRITE_ONLY,
                transactionExecutor, TestModel.createTestContext());

        transactionProxy.write(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));

        Object messages = testContext
            .executeLocalOperation(actorRef, "messages",
                ActorContext.ASK_DURATION);

        Assert.assertEquals(obj,
            actorContext.getRemoteOperationResponseOnExecution());
    }

    private Object createPrimaryFound(ActorRef actorRef) {
        return new PrimaryFound(actorRef.path().toString()).toSerializable();
    }

    @Test
    public void testMerge() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext
            .setExecuteLocalOperationResponse(createPrimaryFound(actorRef));
        actorContext
            .setExecuteShardOperationResponse(createTransactionReply(actorRef));
        Object obj = new MergeDataReply().toSerializable();
        actorContext.setExecuteRemoteOperationResponse(obj);

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.WRITE_ONLY,
                transactionExecutor, TestModel.createTestContext());

        transactionProxy.merge(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));

        Assert.assertEquals(obj,
            actorContext.getRemoteOperationResponseOnExecution());
    }

    @Test
    public void testDelete() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext
            .setExecuteLocalOperationResponse(createPrimaryFound(actorRef));
        actorContext
            .setExecuteShardOperationResponse(createTransactionReply(actorRef));
        Object obj = new DeleteDataReply().toSerializable();
        actorContext.setExecuteRemoteOperationResponse(obj);

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_WRITE,
                transactionExecutor, TestModel.createTestContext());

        transactionProxy.delete(TestModel.TEST_PATH);

        Assert.assertEquals(obj,
            actorContext.getRemoteOperationResponseOnExecution());


    }

    @Test
    public void testReady() throws Exception {
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef doNothingActorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext.setExecuteLocalOperationResponse(
            createPrimaryFound(doNothingActorRef));
        actorContext.setExecuteShardOperationResponse(
            createTransactionReply(doNothingActorRef));
        actorContext.setExecuteRemoteOperationResponse(
            new ReadyTransactionReply(doNothingActorRef.path())
                .toSerializable());

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());


        transactionProxy.read(TestModel.TEST_PATH);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        Assert.assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        Assert.assertTrue("No cohort paths returned",
            proxy.getCohortPaths().size() > 0);

    }


    @Test
    public void testGetIdentifier() {
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef doNothingActorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse(
            createTransactionReply(doNothingActorRef));

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());

        Assert.assertNotNull(transactionProxy.getIdentifier());
    }

    @Test
    public void testClose() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext =
            new MockActorContext(this.getSystem());
        actorContext
            .setExecuteLocalOperationResponse(createPrimaryFound(actorRef));
        actorContext
            .setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.close();

        Object messages = testContext
            .executeLocalOperation(actorRef, "messages",
                ActorContext.ASK_DURATION);

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0).getClass()
            .equals(CloseTransaction.SERIALIZABLE_CLASS));
    }

    private CreateTransactionReply createTransactionReply(ActorRef actorRef) {
        return CreateTransactionReply.newBuilder()
            .setTransactionActorPath(actorRef.path().toString())
            .setTransactionId("txn-1")
            .build();
    }
}
