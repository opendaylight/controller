/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
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
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.RemoteOperationException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
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

import java.util.concurrent.Executors;

import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionProxyFailedTest extends AbstractActorTest {

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
            .thenThrow(new TimeoutException("test", new Exception("reason")));

        TransactionProxy transactionProxy =
            new TransactionProxy(actorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());


        ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
            transactionProxy.read(TestModel.TEST_PATH);

        Assert.assertFalse(read.get().isPresent());

    }

    @Test(expected = IllegalStateException.class)
    public void testReadWhenAnyRuntimeExceptionThrown() throws Exception {



        ActorContext mockActorContext = Mockito.mock(ActorContext.class);


        doThrow(new RemoteOperationException()).when(mockActorContext)
            .executeShardOperation(any(String.class), any(),
                any(FiniteDuration.class));

        TransactionProxy transactionProxy =
            new TransactionProxy(mockActorContext,
                TransactionProxy.TransactionType.READ_ONLY, transactionExecutor,
                TestModel.createTestContext());


        ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
            transactionProxy.read(TestModel.TEST_PATH);
        fail("An IllegalStateException was expected");

    }



    @Test(expected = IllegalStateException.class)
    public void testWriteWhenAnyRuntimeExceptionThrown() throws Exception {



        ActorContext mockActorContext = Mockito.mock(ActorContext.class);


        doThrow(new RemoteOperationException()).when(mockActorContext)
            .executeShardOperation(any(String.class), any(),
                any(FiniteDuration.class));

        TransactionProxy transactionProxy =
            new TransactionProxy(mockActorContext,
                TransactionProxy.TransactionType.READ_WRITE,
                transactionExecutor, TestModel.createTestContext());


        transactionProxy.write(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));
        fail("An IllegalStateException was expected");


    }

    private Object createPrimaryFound(ActorRef actorRef) {
        return new PrimaryFound(actorRef.path().toString()).toSerializable();
    }

    @Test(expected = IllegalStateException.class)
    public void testMergeWhenAnyRuntimeExceptionThrown() throws Exception {
        ActorContext mockActorContext = Mockito.mock(ActorContext.class);


        doThrow(new RemoteOperationException()).when(mockActorContext)
            .executeShardOperation(any(String.class), any(),
                any(FiniteDuration.class));

        TransactionProxy transactionProxy =
            new TransactionProxy(mockActorContext,
                TransactionProxy.TransactionType.WRITE_ONLY,
                transactionExecutor, TestModel.createTestContext());


        transactionProxy.merge(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));
        fail("An IllegalStateException was expected");

    }

    @Test(expected = IllegalStateException.class)
    public void testDeleteWhenAnyRuntimeExceptionThrown() throws Exception {
        ActorContext mockActorContext = Mockito.mock(ActorContext.class);


        doThrow(new RemoteOperationException()).when(mockActorContext)
            .executeShardOperation(any(String.class), any(),
                any(FiniteDuration.class));

        TransactionProxy transactionProxy =
            new TransactionProxy(mockActorContext,
                TransactionProxy.TransactionType.WRITE_ONLY,
                transactionExecutor, TestModel.createTestContext());



        transactionProxy.delete(TestModel.TEST_PATH);

        fail("An IllegalStateException was expected");


    }

    @Test(expected = IllegalStateException.class)
    public void testReadyWhenRuntimeExceptionThrown() throws Exception {

        final Props props = Props.create(DoNothingActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);
        ActorContext mockActorContext = Mockito.mock(ActorContext.class);

        when(mockActorContext.executeShardOperation(any(String.class), any(),
            any(FiniteDuration.class))).thenReturn(
            createTransactionReply(actorRef));

        TransactionProxy transactionProxy =
            new TransactionProxy(mockActorContext,
                TransactionProxy.TransactionType.WRITE_ONLY,
                transactionExecutor, TestModel.createTestContext());

        transactionProxy.write(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.NAME_QNAME));


        doThrow(new RemoteOperationException()).when(mockActorContext)
            .executeRemoteOperation(
                any(ActorSelection.class), any(), any(FiniteDuration.class));

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();
        fail("An IllegalStateException was expected");


    }



    private CreateTransactionReply createTransactionReply(ActorRef actorRef) {
        return CreateTransactionReply.newBuilder()
            .setTransactionActorPath(actorRef.path().toString())
            .setTransactionId("txn-1")
            .build();
    }
}
