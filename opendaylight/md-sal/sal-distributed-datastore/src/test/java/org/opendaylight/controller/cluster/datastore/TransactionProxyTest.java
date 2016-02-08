/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.TransactionType.READ_ONLY;
import static org.opendaylight.controller.cluster.datastore.TransactionType.READ_WRITE;
import static org.opendaylight.controller.cluster.datastore.TransactionType.WRITE_ONLY;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.NormalizedNodeAggregatorTest;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Promise;

@SuppressWarnings("resource")
public class TransactionProxyTest extends AbstractTransactionProxyTest {

    @SuppressWarnings("serial")
    static class TestException extends RuntimeException {
    }

    static interface Invoker {
        CheckedFuture<?, ReadFailedException> invoke(TransactionProxy proxy) throws Exception;
    }

    @Test
    public void testRead() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", false, readOptional.isPresent());

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        readOptional = transactionProxy.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());

        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadWithInvalidReplyMessageType() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        transactionProxy.read(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testReadWithAsyncRemoteOperatonFailure() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        propagateReadFailedExceptionCause(transactionProxy.read(TestModel.TEST_PATH));
    }

    private void testExceptionOnInitialCreateTransaction(Exception exToThrow, Invoker invoker)
            throws Throwable {
        ActorRef actorRef = getSystem().actorOf(Props.create(DoNothingActor.class));

        if (exToThrow instanceof PrimaryNotFoundException) {
            doReturn(Futures.failed(exToThrow)).when(mockActorContext).findPrimaryShardAsync(anyString());
        } else {
            doReturn(primaryShardInfoReply(getSystem(), actorRef)).
                    when(mockActorContext).findPrimaryShardAsync(anyString());
        }

        doReturn(Futures.failed(exToThrow)).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), any(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

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

    @Test
    public void testReadWithPriorRecordingOperationSuccessful() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef, 1);

        doReturn(readDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, expectedNode);

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());
        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());

        InOrder inOrder = Mockito.inOrder(mockActorContext);
        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));
    }

    @Test(expected=IllegalStateException.class)
    public void testReadPreConditionCheck() {
        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);
        transactionProxy.read(TestModel.TEST_PATH);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidCreateTransactionReply() throws Throwable {
        ActorRef actorRef = getSystem().actorOf(Props.create(DoNothingActor.class));

        doReturn(getSystem().actorSelection(actorRef.path())).when(mockActorContext).
            actorSelection(actorRef.path().toString());

        doReturn(primaryShardInfoReply(getSystem(), actorRef)).
            when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        doReturn(Futures.successful(new Object())).when(mockActorContext).executeOperationAsync(
            eq(getSystem().actorSelection(actorRef.path())), eqCreateTransaction(memberName, READ_ONLY),
            any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        propagateReadFailedExceptionCause(transactionProxy.read(TestModel.TEST_PATH));
    }

    @Test
    public void testExists() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        doReturn(dataExistsReply(false)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists(), any(Timeout.class));

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", false, exists);

        doReturn(dataExistsReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists(), any(Timeout.class));

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
                executeOperationAsync(eq(actorSelection(actorRef)), eqDataExists(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        transactionProxy.exists(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
    }

    @Test(expected = TestException.class)
    public void testExistsWithAsyncRemoteOperatonFailure() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);

        doReturn(Futures.failed(new TestException())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqDataExists(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        propagateReadFailedExceptionCause(transactionProxy.exists(TestModel.TEST_PATH));
    }

    @Test
    public void testExistsWithPriorRecordingOperationSuccessful() throws Throwable {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef, 1);

        doReturn(dataExistsReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet();

        assertEquals("Exists response", true, exists);

        InOrder inOrder = Mockito.inOrder(mockActorContext);
        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists(), any(Timeout.class));
    }

    @Test(expected=IllegalStateException.class)
    public void testExistsPreConditionCheck() {
        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);
        transactionProxy.exists(TestModel.TEST_PATH);
    }

    @Test
    public void testWrite() throws Exception {
        dataStoreContextBuilder.shardBatchedModificationCount(1);
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef, 1);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        verifyOneBatchedModification(actorRef, new WriteModification(TestModel.TEST_PATH, nodeToWrite), false);
    }

    @Test
    public void testWriteAfterAsyncRead() throws Throwable {
        ActorRef actorRef = setupActorContextWithoutInitialCreateTransaction(getSystem(), DefaultShardStrategy.DEFAULT_SHARD);

        Promise<Object> createTxPromise = akka.dispatch.Futures.promise();
        doReturn(createTxPromise).when(mockActorContext).executeOperationAsync(
                eq(getSystem().actorSelection(actorRef.path())),
                eqCreateTransaction(memberName, READ_WRITE), any(Timeout.class));

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        expectBatchedModificationsReady(actorRef);

        final NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        final TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

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

        verifyOneBatchedModification(actorRef, new WriteModification(TestModel.TEST_PATH, nodeToWrite), true);
    }

    @Test(expected=IllegalStateException.class)
    public void testWritePreConditionCheck() {
        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);
        transactionProxy.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
    }

    @Test(expected=IllegalStateException.class)
    public void testWriteAfterReadyPreConditionCheck() {
        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.ready();

        transactionProxy.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
    }

    @Test
    public void testMerge() throws Exception {
        dataStoreContextBuilder.shardBatchedModificationCount(1);
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModifications(actorRef, 1);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        verifyOneBatchedModification(actorRef, new MergeModification(TestModel.TEST_PATH, nodeToWrite), false);
    }

    @Test
    public void testDelete() throws Exception {
        dataStoreContextBuilder.shardBatchedModificationCount(1);
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        expectBatchedModifications(actorRef, 1);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.delete(TestModel.TEST_PATH);

        verifyOneBatchedModification(actorRef, new DeleteModification(TestModel.TEST_PATH), false);
    }

    @Test
    public void testReadWrite() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        expectBatchedModifications(actorRef, 1);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.read(TestModel.TEST_PATH);

        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 1, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), false,
                new WriteModification(TestModel.TEST_PATH, nodeToWrite));
    }

    @Test
    public void testReadyWithReadWrite() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        expectBatchedModificationsReady(actorRef, true);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof SingleCommitCohortProxy);

        verifyCohortFutures((SingleCommitCohortProxy)ready, new CommitTransactionReply().toSerializable());

        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 1, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), true, true,
                new WriteModification(TestModel.TEST_PATH, nodeToWrite));

        assertEquals("getTotalMessageCount", 1, batchedModifications.get(0).getTotalMessagesSent());
    }

    @Test
    public void testReadyWithNoModifications() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        expectBatchedModificationsReady(actorRef, true);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof SingleCommitCohortProxy);

        verifyCohortFutures((SingleCommitCohortProxy)ready, new CommitTransactionReply().toSerializable());

        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 1, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), true, true);
    }

    @Test
    public void testReadyWithMultipleShardWrites() throws Exception {
        ActorRef actorRef1 = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        ActorRef actorRef2 = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY, "junk");

        expectBatchedModificationsReady(actorRef1);
        expectBatchedModificationsReady(actorRef2);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.write(TestModel.JUNK_PATH, ImmutableNodes.containerNode(TestModel.JUNK_QNAME));
        transactionProxy.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        verifyCohortFutures((ThreePhaseCommitCohortProxy)ready, actorSelection(actorRef1),
                actorSelection(actorRef2));
    }

    @Test
    public void testReadyWithWriteOnlyAndLastBatchPending() throws Exception {
        dataStoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModificationsReady(actorRef, true);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof SingleCommitCohortProxy);

        verifyCohortFutures((SingleCommitCohortProxy)ready, new CommitTransactionReply().toSerializable());

        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 1, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), true, true,
                new WriteModification(TestModel.TEST_PATH, nodeToWrite));
    }

    @Test
    public void testReadyWithWriteOnlyAndLastBatchEmpty() throws Exception {
        dataStoreContextBuilder.shardBatchedModificationCount(1).writeOnlyTransactionOptimizationsEnabled(true);
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectBatchedModificationsReady(actorRef, true);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof SingleCommitCohortProxy);

        verifyCohortFutures((SingleCommitCohortProxy)ready, new CommitTransactionReply().toSerializable());

        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 2, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), false,
                new WriteModification(TestModel.TEST_PATH, nodeToWrite));

        verifyBatchedModifications(batchedModifications.get(1), true, true);
    }

    @Test
    public void testReadyWithReplyFailure() throws Exception {
        dataStoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        expectFailedBatchedModifications(actorRef);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof SingleCommitCohortProxy);

        verifyCohortFutures((SingleCommitCohortProxy)ready, TestException.class);
    }

    @Test
    public void testReadyWithDebugContextEnabled() throws Exception {
        dataStoreContextBuilder.transactionDebugContextEnabled(true);

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        expectBatchedModificationsReady(actorRef, true);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.merge(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof DebugThreePhaseCommitCohort);

        verifyCohortFutures((DebugThreePhaseCommitCohort)ready, new CommitTransactionReply().toSerializable());
    }

    @Test
    public void testReadyWithLocalTransaction() throws Exception {
        ActorRef shardActorRef = getSystem().actorOf(Props.create(DoNothingActor.class));

        doReturn(getSystem().actorSelection(shardActorRef.path())).
                when(mockActorContext).actorSelection(shardActorRef.path().toString());

        doReturn(Futures.successful(newPrimaryShardInfo(shardActorRef, createDataTree()))).
                when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        expectReadyLocalTransaction(shardActorRef, true);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();
        assertTrue(ready instanceof SingleCommitCohortProxy);
        verifyCohortFutures((SingleCommitCohortProxy)ready, new CommitTransactionReply().toSerializable());
    }

    @Test
    public void testReadyWithLocalTransactionWithFailure() throws Exception {
        ActorRef shardActorRef = getSystem().actorOf(Props.create(DoNothingActor.class));

        doReturn(getSystem().actorSelection(shardActorRef.path())).
                when(mockActorContext).actorSelection(shardActorRef.path().toString());

        Optional<DataTree> mockDataTree = createDataTree();
        DataTreeModification mockModification = mockDataTree.get().takeSnapshot().newModification();
        doThrow(new RuntimeException("mock")).when(mockModification).ready();

        doReturn(Futures.successful(newPrimaryShardInfo(shardActorRef, mockDataTree))).
                when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        expectReadyLocalTransaction(shardActorRef, true);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();
        assertTrue(ready instanceof SingleCommitCohortProxy);
        verifyCohortFutures((SingleCommitCohortProxy)ready, RuntimeException.class);
    }

    private void testWriteOnlyTxWithFindPrimaryShardFailure(Exception toThrow) throws Exception {
        doReturn(Futures.failed(toThrow)).when(mockActorContext).findPrimaryShardAsync(anyString());

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

        transactionProxy.delete(TestModel.TEST_PATH);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof SingleCommitCohortProxy);

        verifyCohortFutures((SingleCommitCohortProxy)ready, toThrow.getClass());
    }

    @Test
    public void testWriteOnlyTxWithPrimaryNotFoundException() throws Exception {
        testWriteOnlyTxWithFindPrimaryShardFailure(new PrimaryNotFoundException("mock"));
    }

    @Test
    public void testWriteOnlyTxWithNotInitializedException() throws Exception {
        testWriteOnlyTxWithFindPrimaryShardFailure(new NotInitializedException("mock"));
    }

    @Test
    public void testWriteOnlyTxWithNoShardLeaderException() throws Exception {
        testWriteOnlyTxWithFindPrimaryShardFailure(new NoShardLeaderException("mock"));
    }

    @Test
    public void testReadyWithInvalidReplyMessageType() throws Exception {
        dataStoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);
        ActorRef actorRef1 = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        ActorRef actorRef2 = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY, "junk");

        doReturn(Futures.successful(new Object())).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef1)), isA(BatchedModifications.class),
                        any(Timeout.class));

        expectBatchedModificationsReady(actorRef2);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, WRITE_ONLY);

        transactionProxy.write(TestModel.JUNK_PATH, ImmutableNodes.containerNode(TestModel.JUNK_QNAME));
        transactionProxy.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        verifyCohortFutures((ThreePhaseCommitCohortProxy)ready, actorSelection(actorRef2),
                IllegalArgumentException.class);
    }

    @Test
    public void testGetIdentifier() {
        setupActorContextWithInitialCreateTransaction(getSystem(), READ_ONLY);
        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        Object id = transactionProxy.getIdentifier();
        assertNotNull("getIdentifier returned null", id);
        assertTrue("Invalid identifier: " + id, id.toString().startsWith(memberName));
    }

    @Test
    public void testClose() throws Exception{
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.close();

        verify(mockActorContext).sendOperationAsync(
                eq(actorSelection(actorRef)), isA(CloseTransaction.class));
    }

    private static interface TransactionProxyOperation {
        void run(TransactionProxy transactionProxy);
    }

    private void throttleOperation(TransactionProxyOperation operation) {
        throttleOperation(operation, 1, true);
    }

    private void throttleOperation(TransactionProxyOperation operation, int outstandingOpsLimit, boolean shardFound){
        throttleOperation(operation, outstandingOpsLimit, shardFound, TimeUnit.MILLISECONDS.toNanos(
                mockActorContext.getDatastoreContext().getOperationTimeoutInMillis()));
    }

    private PrimaryShardInfo newPrimaryShardInfo(ActorRef actorRef){
        return new PrimaryShardInfo(getSystem().actorSelection(actorRef.path()), DataStoreVersions.CURRENT_VERSION,
                Optional.<DataTree>absent());
    }

    private PrimaryShardInfo newPrimaryShardInfo(ActorRef actorRef, Optional<DataTree> dataTreeOptional){
        return new PrimaryShardInfo(getSystem().actorSelection(actorRef.path()), DataStoreVersions.CURRENT_VERSION,
                dataTreeOptional);
    }


    private void throttleOperation(TransactionProxyOperation operation, int outstandingOpsLimit, boolean shardFound, long expectedCompletionTime){
        ActorSystem actorSystem = getSystem();
        ActorRef shardActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));

        // Note that we setting batchedModificationCount to one less than what we need because in TransactionProxy
        // we now allow one extra permit to be allowed for ready
        doReturn(dataStoreContextBuilder.operationTimeoutInSeconds(2).
                shardBatchedModificationCount(outstandingOpsLimit-1).build()).when(mockActorContext).getDatastoreContext();

        doReturn(actorSystem.actorSelection(shardActorRef.path())).
                when(mockActorContext).actorSelection(shardActorRef.path().toString());

        if(shardFound) {
            doReturn(Futures.successful(newPrimaryShardInfo(shardActorRef))).
                    when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
            doReturn(Futures.successful(newPrimaryShardInfo(shardActorRef))).
                    when(mockActorContext).findPrimaryShardAsync(eq("cars"));

        } else {
            doReturn(Futures.failed(new Exception("not found")))
                    .when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
        }

        String actorPath = "akka.tcp://system@127.0.0.1:2550/user/tx-actor";

        doReturn(incompleteFuture()).when(mockActorContext).
        executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                 eqCreateTransaction(memberName, READ_WRITE), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        long start = System.nanoTime();

        operation.run(transactionProxy);

        long end = System.nanoTime();

        Assert.assertTrue(String.format("Expected elapsed time: %s. Actual: %s",
                expectedCompletionTime, (end-start)),
                ((end - start) > expectedCompletionTime) && ((end - start) < expectedCompletionTime*2));

    }

    private void completeOperation(TransactionProxyOperation operation){
        completeOperation(operation, true);
    }

    private void completeOperation(TransactionProxyOperation operation, boolean shardFound){
        ActorSystem actorSystem = getSystem();
        ActorRef shardActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));

        doReturn(actorSystem.actorSelection(shardActorRef.path())).
                when(mockActorContext).actorSelection(shardActorRef.path().toString());

        if(shardFound) {
            doReturn(Futures.successful(newPrimaryShardInfo(shardActorRef))).
                    when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
        } else {
            doReturn(Futures.failed(new PrimaryNotFoundException("test")))
                    .when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
        }

        ActorRef txActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
        String actorPath = txActorRef.path().toString();
        CreateTransactionReply createTransactionReply = new CreateTransactionReply(actorPath, "txn-1",
                DataStoreVersions.CURRENT_VERSION);

        doReturn(actorSystem.actorSelection(actorPath)).when(mockActorContext).actorSelection(actorPath);

        doReturn(Futures.successful(createTransactionReply)).when(mockActorContext).
                executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                        eqCreateTransaction(memberName, READ_WRITE), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        long start = System.nanoTime();

        operation.run(transactionProxy);

        long end = System.nanoTime();

        long expected = TimeUnit.MILLISECONDS.toNanos(mockActorContext.getDatastoreContext().getOperationTimeoutInMillis());
        Assert.assertTrue(String.format("Expected elapsed time: %s. Actual: %s",
                expected, (end-start)), (end - start) <= expected);
    }

    private void completeOperationLocal(TransactionProxyOperation operation, Optional<DataTree> dataTreeOptional){
        ActorSystem actorSystem = getSystem();
        ActorRef shardActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));

        doReturn(actorSystem.actorSelection(shardActorRef.path())).
                when(mockActorContext).actorSelection(shardActorRef.path().toString());

        doReturn(Futures.successful(newPrimaryShardInfo(shardActorRef, dataTreeOptional))).
                when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        long start = System.nanoTime();

        operation.run(transactionProxy);

        long end = System.nanoTime();

        long expected = TimeUnit.MILLISECONDS.toNanos(mockActorContext.getDatastoreContext().getOperationTimeoutInMillis());
        Assert.assertTrue(String.format("Expected elapsed time: %s. Actual: %s",
                expected, (end-start)), (end - start) <= expected);
    }

    private static Optional<DataTree> createDataTree(){
        DataTree dataTree = mock(DataTree.class);
        Optional<DataTree> dataTreeOptional = Optional.of(dataTree);
        DataTreeSnapshot dataTreeSnapshot = mock(DataTreeSnapshot.class);
        DataTreeModification dataTreeModification = mock(DataTreeModification.class);

        doReturn(dataTreeSnapshot).when(dataTree).takeSnapshot();
        doReturn(dataTreeModification).when(dataTreeSnapshot).newModification();

        return dataTreeOptional;
    }

    private static Optional<DataTree> createDataTree(NormalizedNode<?, ?> readResponse){
        DataTree dataTree = mock(DataTree.class);
        Optional<DataTree> dataTreeOptional = Optional.of(dataTree);
        DataTreeSnapshot dataTreeSnapshot = mock(DataTreeSnapshot.class);
        DataTreeModification dataTreeModification = mock(DataTreeModification.class);

        doReturn(dataTreeSnapshot).when(dataTree).takeSnapshot();
        doReturn(dataTreeModification).when(dataTreeSnapshot).newModification();
        doReturn(Optional.of(readResponse)).when(dataTreeModification).readNode(any(YangInstanceIdentifier.class));

        return dataTreeOptional;
    }


    @Test
    public void testWriteCompletionForLocalShard(){
        completeOperationLocal(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

            }
        }, createDataTree());
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

                expectBatchedModifications(2);

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

                expectBatchedModifications(2);

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

                expectBatchedModifications(2);

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

                expectBatchedModifications(2);

                transactionProxy.merge(TestModel.TEST_PATH, nodeToMerge);

                transactionProxy.merge(TestModel.TEST_PATH, nodeToMerge);
            }
        });

    }

    @Test
    public void testMergeCompletionForLocalShard(){
        completeOperationLocal(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

                transactionProxy.merge(TestModel.TEST_PATH, nodeToWrite);

            }
        }, createDataTree());
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
                expectBatchedModifications(2);

                transactionProxy.delete(TestModel.TEST_PATH);

                transactionProxy.delete(TestModel.TEST_PATH);
            }
        }, false);
    }

    @Test
    public void testDeleteCompletionForLocalShard(){
        completeOperationLocal(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {

                transactionProxy.delete(TestModel.TEST_PATH);

                transactionProxy.delete(TestModel.TEST_PATH);
            }
        }, createDataTree());

    }

    @Test
    public void testDeleteCompletion(){
        completeOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                expectBatchedModifications(2);

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
                        any(ActorSelection.class), eqReadData(), any(Timeout.class));

                transactionProxy.read(TestModel.TEST_PATH);

                transactionProxy.read(TestModel.TEST_PATH);
            }
        });

    }

    @Test
    public void testReadCompletionForLocalShard(){
        final NormalizedNode<?, ?> nodeToRead = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        completeOperationLocal(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                transactionProxy.read(TestModel.TEST_PATH);

                transactionProxy.read(TestModel.TEST_PATH);
            }
        }, createDataTree(nodeToRead));

    }

    @Test
    public void testReadCompletionForLocalShardWhenExceptionOccurs(){
        completeOperationLocal(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                transactionProxy.read(TestModel.TEST_PATH);

                transactionProxy.read(TestModel.TEST_PATH);
            }
        }, createDataTree());

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
                        any(ActorSelection.class), eqDataExists(), any(Timeout.class));

                transactionProxy.exists(TestModel.TEST_PATH);

                transactionProxy.exists(TestModel.TEST_PATH);
            }
        });

    }

    @Test
    public void testExistsCompletionForLocalShard(){
        final NormalizedNode<?, ?> nodeToRead = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        completeOperationLocal(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                transactionProxy.exists(TestModel.TEST_PATH);

                transactionProxy.exists(TestModel.TEST_PATH);
            }
        }, createDataTree(nodeToRead));

    }

    @Test
    public void testExistsCompletionForLocalShardWhenExceptionOccurs(){
        completeOperationLocal(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                transactionProxy.exists(TestModel.TEST_PATH);

                transactionProxy.exists(TestModel.TEST_PATH);
            }
        }, createDataTree());

    }
    @Test
    public void testReadyThrottling(){

        throttleOperation(new TransactionProxyOperation() {
            @Override
            public void run(TransactionProxy transactionProxy) {
                NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                expectBatchedModifications(1);

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

                expectBatchedModifications(2);

                transactionProxy.write(TestModel.TEST_PATH, nodeToWrite);

                // Trying to write to Cars will cause another transaction context to get created
                transactionProxy.write(CarsModel.BASE_PATH, carsNode);

                // Now ready should block for both transaction contexts
                transactionProxy.ready();
            }
        }, 1, true, TimeUnit.MILLISECONDS.toNanos(mockActorContext.getDatastoreContext().getOperationTimeoutInMillis()) * 2);
    }

    private void testModificationOperationBatching(TransactionType type) throws Exception {
        int shardBatchedModificationCount = 3;
        dataStoreContextBuilder.shardBatchedModificationCount(shardBatchedModificationCount);

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), type);

        expectBatchedModifications(actorRef, shardBatchedModificationCount);

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

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, type);

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

        verifyBatchedModifications(batchedModifications.get(0), false, new WriteModification(writePath1, writeNode1),
                new WriteModification(writePath2, writeNode2), new DeleteModification(deletePath1));

        verifyBatchedModifications(batchedModifications.get(1), false, new MergeModification(mergePath1, mergeNode1),
                new MergeModification(mergePath2, mergeNode2), new WriteModification(writePath3, writeNode3));

        verifyBatchedModifications(batchedModifications.get(2), true, true,
                new MergeModification(mergePath3, mergeNode3), new DeleteModification(deletePath2));

        assertEquals("getTotalMessageCount", 3, batchedModifications.get(2).getTotalMessagesSent());
    }

    @Test
    public void testReadWriteModificationOperationBatching() throws Throwable {
        testModificationOperationBatching(READ_WRITE);
    }

    @Test
    public void testWriteOnlyModificationOperationBatching() throws Throwable {
        testModificationOperationBatching(WRITE_ONLY);
    }

    @Test
    public void testOptimizedWriteOnlyModificationOperationBatching() throws Throwable {
        dataStoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);
        testModificationOperationBatching(WRITE_ONLY);
    }

    @Test
    public void testModificationOperationBatchingWithInterleavedReads() throws Throwable {

        int shardBatchedModificationCount = 10;
        dataStoreContextBuilder.shardBatchedModificationCount(shardBatchedModificationCount);

        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        expectBatchedModifications(actorRef, shardBatchedModificationCount);

        YangInstanceIdentifier writePath1 = TestModel.TEST_PATH;
        NormalizedNode<?, ?> writeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        YangInstanceIdentifier writePath2 = TestModel.OUTER_LIST_PATH;
        NormalizedNode<?, ?> writeNode2 = ImmutableNodes.containerNode(TestModel.OUTER_LIST_QNAME);

        YangInstanceIdentifier mergePath1 = TestModel.TEST_PATH;
        NormalizedNode<?, ?> mergeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        YangInstanceIdentifier mergePath2 = TestModel.INNER_LIST_PATH;
        NormalizedNode<?, ?> mergeNode2 = ImmutableNodes.containerNode(TestModel.INNER_LIST_QNAME);

        YangInstanceIdentifier deletePath = TestModel.OUTER_LIST_PATH;

        doReturn(readDataReply(writeNode2)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(writePath2), any(Timeout.class));

        doReturn(readDataReply(mergeNode2)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(mergePath2), any(Timeout.class));

        doReturn(dataExistsReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

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

        verifyBatchedModifications(batchedModifications.get(0), false, new WriteModification(writePath1, writeNode1),
                new WriteModification(writePath2, writeNode2));

        verifyBatchedModifications(batchedModifications.get(1), false, new MergeModification(mergePath1, mergeNode1),
                new MergeModification(mergePath2, mergeNode2));

        verifyBatchedModifications(batchedModifications.get(2), false, new DeleteModification(deletePath));

        InOrder inOrder = Mockito.inOrder(mockActorContext);
        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(writePath2), any(Timeout.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqReadData(mergePath2), any(Timeout.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));

        inOrder.verify(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqDataExists(), any(Timeout.class));
    }

    @Test
    public void testReadRoot() throws ReadFailedException, InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

        SchemaContext schemaContext = SchemaContextHelper.full();
        Configuration configuration = mock(Configuration.class);
        doReturn(configuration).when(mockActorContext).getConfiguration();
        doReturn(schemaContext).when(mockActorContext).getSchemaContext();
        doReturn(Sets.newHashSet("test", "cars")).when(configuration).getAllShardNames();

        NormalizedNode<?, ?> expectedNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        NormalizedNode<?, ?> expectedNode2 = ImmutableNodes.containerNode(CarsModel.CARS_QNAME);

        setUpReadData("test", NormalizedNodeAggregatorTest.getRootNode(expectedNode1, schemaContext));
        setUpReadData("cars", NormalizedNodeAggregatorTest.getRootNode(expectedNode2, schemaContext));

        doReturn(memberName).when(mockActorContext).getCurrentMemberName();

        doReturn(getSystem().dispatchers().defaultGlobalDispatcher()).when(mockActorContext).getClientDispatcher();

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_ONLY);

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                YangInstanceIdentifier.builder().build()).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());

        NormalizedNode<?, ?> normalizedNode = readOptional.get();

        assertTrue("Expect value to be a Collection", normalizedNode.getValue() instanceof Collection);

        @SuppressWarnings("unchecked")
        Collection<NormalizedNode<?,?>> collection = (Collection<NormalizedNode<?,?>>) normalizedNode.getValue();

        for(NormalizedNode<?,?> node : collection){
            assertTrue("Expected " + node + " to be a ContainerNode", node instanceof ContainerNode);
        }

        assertTrue("Child with QName = " + TestModel.TEST_QNAME + " not found",
                NormalizedNodeAggregatorTest.findChildWithQName(collection, TestModel.TEST_QNAME) != null);

        assertEquals(expectedNode1, NormalizedNodeAggregatorTest.findChildWithQName(collection, TestModel.TEST_QNAME));

        assertTrue("Child with QName = " + CarsModel.BASE_QNAME + " not found",
                NormalizedNodeAggregatorTest.findChildWithQName(collection, CarsModel.BASE_QNAME) != null);

        assertEquals(expectedNode2, NormalizedNodeAggregatorTest.findChildWithQName(collection, CarsModel.BASE_QNAME));
    }


    private void setUpReadData(String shardName, NormalizedNode<?, ?> expectedNode) {
        ActorSystem actorSystem = getSystem();
        ActorRef shardActorRef = getSystem().actorOf(Props.create(DoNothingActor.class));

        doReturn(getSystem().actorSelection(shardActorRef.path())).
                when(mockActorContext).actorSelection(shardActorRef.path().toString());

        doReturn(primaryShardInfoReply(getSystem(), shardActorRef)).
                when(mockActorContext).findPrimaryShardAsync(eq(shardName));

        ActorRef txActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));

        doReturn(actorSystem.actorSelection(txActorRef.path())).
                when(mockActorContext).actorSelection(txActorRef.path().toString());

        doReturn(Futures.successful(createTransactionReply(txActorRef, DataStoreVersions.CURRENT_VERSION))).when(mockActorContext).
                executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                        eqCreateTransaction(memberName, TransactionType.READ_ONLY), any(Timeout.class));

        doReturn(readDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(txActorRef)), eqReadData(YangInstanceIdentifier.builder().build()), any(Timeout.class));
    }
}
