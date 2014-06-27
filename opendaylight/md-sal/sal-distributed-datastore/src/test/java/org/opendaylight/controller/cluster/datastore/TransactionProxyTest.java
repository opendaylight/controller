package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.datastore.utils.MockActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionProxyTest extends AbstractActorTest {

    private ExecutorService transactionExecutor =
        Executors.newSingleThreadExecutor();

    @Test
    public void testRead() throws Exception {
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);


        ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
            transactionProxy.read(TestModel.TEST_PATH);

        Optional<NormalizedNode<?, ?>> normalizedNodeOptional = read.get();

        Assert.assertFalse(normalizedNodeOptional.isPresent());

        actorContext.setExecuteRemoteOperationResponse(new ReadDataReply(
            ImmutableNodes.containerNode(TestModel.TEST_QNAME)));

        read = transactionProxy.read(TestModel.TEST_PATH);

        normalizedNodeOptional = read.get();

        Assert.assertTrue(normalizedNodeOptional.isPresent());
    }

    @Test
    public void testReadWhenANullIsReturned() throws Exception {
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);


        ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
            transactionProxy.read(TestModel.TEST_PATH);

        Optional<NormalizedNode<?, ?>> normalizedNodeOptional = read.get();

        Assert.assertFalse(normalizedNodeOptional.isPresent());

        actorContext.setExecuteRemoteOperationResponse(new ReadDataReply(
            null));

        read = transactionProxy.read(TestModel.TEST_PATH);

        normalizedNodeOptional = read.get();

        Assert.assertFalse(normalizedNodeOptional.isPresent());
    }

    @Test
    public void testWrite() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);

        transactionProxy.write(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));

        ActorContext testContext = new ActorContext(getSystem(), getSystem().actorOf(Props.create(DoNothingActor.class)));
        Object messages = testContext
            .executeLocalOperation(actorRef, "messages",
                ActorContext.ASK_DURATION);

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0) instanceof WriteData);
    }

    @Test
    public void testMerge() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);

        transactionProxy.merge(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));

        ActorContext testContext = new ActorContext(getSystem(), getSystem().actorOf(Props.create(DoNothingActor.class)));
        Object messages = testContext
            .executeLocalOperation(actorRef, "messages",
                ActorContext.ASK_DURATION);

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0) instanceof MergeData);
    }

    @Test
    public void testDelete() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);

        transactionProxy.delete(TestModel.TEST_PATH);

        ActorContext testContext = new ActorContext(getSystem(), getSystem().actorOf(Props.create(DoNothingActor.class)));
        Object messages = testContext
            .executeLocalOperation(actorRef, "messages",
                ActorContext.ASK_DURATION);

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0) instanceof DeleteData);
    }

    @Test
    public void testReady() throws Exception {
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef doNothingActorRef = getSystem().actorOf(props);

        final MockActorContext actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse(createTransactionReply(doNothingActorRef));
        actorContext.setExecuteRemoteOperationResponse(new ReadyTransactionReply(doNothingActorRef.path()));

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);


        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        Assert.assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        Assert.assertTrue("No cohort paths returned", proxy.getCohortPaths().size() > 0);

    }

    @Test
    public void testGetIdentifier(){
        final Props props = Props.create(DoNothingActor.class);
        final ActorRef doNothingActorRef = getSystem().actorOf(props);

        final MockActorContext actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse( createTransactionReply(doNothingActorRef) );

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);

        Assert.assertNotNull(transactionProxy.getIdentifier());
    }

    @Test
    public void testClose(){
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        final MockActorContext actorContext = new MockActorContext(this.getSystem());
        actorContext.setExecuteShardOperationResponse(createTransactionReply(actorRef));
        actorContext.setExecuteRemoteOperationResponse("message");

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor);

        transactionProxy.close();

        ActorContext testContext = new ActorContext(getSystem(), getSystem().actorOf(Props.create(DoNothingActor.class)));
        Object messages = testContext
            .executeLocalOperation(actorRef, "messages",
                ActorContext.ASK_DURATION);

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<Object> listMessages = (List<Object>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0) instanceof CloseTransaction);
    }

    private CreateTransactionReply createTransactionReply(ActorRef actorRef){
        return new CreateTransactionReply(actorRef.path(), "txn-1");
    }
}
