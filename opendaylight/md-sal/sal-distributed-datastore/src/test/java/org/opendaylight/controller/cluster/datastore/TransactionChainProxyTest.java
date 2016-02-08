/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.TransactionType.READ_WRITE;
import static org.opendaylight.controller.cluster.datastore.TransactionType.WRITE_ONLY;
import akka.actor.ActorRef;
import akka.util.Timeout;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import scala.concurrent.Promise;

public class TransactionChainProxyTest extends AbstractTransactionProxyTest {

    @SuppressWarnings("resource")
    @Test
    public void testNewReadOnlyTransaction() throws Exception {

        DOMStoreTransaction dst = new TransactionChainProxy(mockComponentFactory).newReadOnlyTransaction();
        Assert.assertTrue(dst instanceof DOMStoreReadTransaction);

    }

    @SuppressWarnings("resource")
    @Test
    public void testNewReadWriteTransaction() throws Exception {
        DOMStoreTransaction dst = new TransactionChainProxy(mockComponentFactory).newReadWriteTransaction();
        Assert.assertTrue(dst instanceof DOMStoreReadWriteTransaction);

    }

    @SuppressWarnings("resource")
    @Test
    public void testNewWriteOnlyTransaction() throws Exception {
        DOMStoreTransaction dst = new TransactionChainProxy(mockComponentFactory).newWriteOnlyTransaction();
        Assert.assertTrue(dst instanceof DOMStoreWriteTransaction);

    }

    @Test
    public void testClose() throws Exception {
        new TransactionChainProxy(mockComponentFactory).close();

        verify(mockActorContext, times(1)).broadcast(any(Function.class));
    }

    @Test
    public void testTransactionChainsHaveUniqueId() {
        try (TransactionChainProxy one = new TransactionChainProxy(mockComponentFactory)) {
            try (TransactionChainProxy two = new TransactionChainProxy(mockComponentFactory)) {

                Assert.assertNotEquals(one.getTransactionChainId(), two.getTransactionChainId());
            }
        }
    }

    @Test
    public void testRateLimitingUsedInReadWriteTxCreation() {
        try (TransactionChainProxy txChainProxy = new TransactionChainProxy(mockComponentFactory)) {

            txChainProxy.newReadWriteTransaction();

            verify(mockActorContext, times(1)).acquireTxCreationPermit();
        }
    }

    @Test
    public void testRateLimitingUsedInWriteOnlyTxCreation() {
        try (TransactionChainProxy txChainProxy = new TransactionChainProxy(mockComponentFactory)) {

            txChainProxy.newWriteOnlyTransaction();

            verify(mockActorContext, times(1)).acquireTxCreationPermit();
        }
    }

    @Test
    public void testRateLimitingNotUsedInReadOnlyTxCreation() {
        try (TransactionChainProxy txChainProxy = new TransactionChainProxy(mockComponentFactory)) {

            txChainProxy.newReadOnlyTransaction();

            verify(mockActorContext, times(0)).acquireTxCreationPermit();
        }
    }

    /**
     * Tests 2 successive chained write-only transactions and verifies the second transaction isn't
     * initiated until the first one completes its read future.
     */
    @Test
    public void testChainedWriteOnlyTransactions() throws Exception {
        dataStoreContextBuilder.writeOnlyTransactionOptimizationsEnabled(true);

        try (TransactionChainProxy txChainProxy = new TransactionChainProxy(mockComponentFactory)) {

            ActorRef txActorRef1 = setupActorContextWithoutInitialCreateTransaction(getSystem());

            Promise<Object> batchedReplyPromise1 = akka.dispatch.Futures.promise();
            doReturn(batchedReplyPromise1.future()).when(mockActorContext).executeOperationAsync(
                    eq(actorSelection(txActorRef1)), isA(BatchedModifications.class), any(Timeout.class));

            DOMStoreWriteTransaction writeTx1 = txChainProxy.newWriteOnlyTransaction();

            NormalizedNode<?, ?> writeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            writeTx1.write(TestModel.TEST_PATH, writeNode1);

            writeTx1.ready();

            verify(mockActorContext, times(1)).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

            verifyOneBatchedModification(txActorRef1, new WriteModification(TestModel.TEST_PATH, writeNode1), true);

            ActorRef txActorRef2 = setupActorContextWithoutInitialCreateTransaction(getSystem());

            expectBatchedModifications(txActorRef2, 1);

            final NormalizedNode<?, ?> writeNode2 = ImmutableNodes.containerNode(TestModel.OUTER_LIST_QNAME);

            final DOMStoreWriteTransaction writeTx2 = txChainProxy.newWriteOnlyTransaction();

            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch write2Complete = new CountDownLatch(1);
            new Thread() {
                @Override
                public void run() {
                    try {
                        writeTx2.write(TestModel.OUTER_LIST_PATH, writeNode2);
                    } catch (Exception e) {
                        caughtEx.set(e);
                    } finally {
                        write2Complete.countDown();
                    }
                }
            }.start();

            assertEquals("Tx 2 write should've completed", true, write2Complete.await(5, TimeUnit.SECONDS));

            if (caughtEx.get() != null) {
                throw caughtEx.get();
            }

            try {
                verify(mockActorContext, times(1)).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));
            } catch (AssertionError e) {
                fail("Tx 2 should not have initiated until the Tx 1's ready future completed");
            }

            batchedReplyPromise1.success(readyTxReply(txActorRef1.path().toString()).value().get().get());

            // Tx 2 should've proceeded to find the primary shard.
            verify(mockActorContext, timeout(5000).times(2)).findPrimaryShardAsync(
                    eq(DefaultShardStrategy.DEFAULT_SHARD));
        }
    }

    /**
     * Tests 2 successive chained read-write transactions and verifies the second transaction isn't
     * initiated until the first one completes its read future.
     */
    @Test
    public void testChainedReadWriteTransactions() throws Exception {
        try (TransactionChainProxy txChainProxy = new TransactionChainProxy(mockComponentFactory)) {

            ActorRef txActorRef1 = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

            expectBatchedModifications(txActorRef1, 1);

            Promise<Object> readyReplyPromise1 = akka.dispatch.Futures.promise();
            doReturn(readyReplyPromise1.future()).when(mockActorContext).executeOperationAsync(
                    eq(actorSelection(txActorRef1)), isA(BatchedModifications.class), any(Timeout.class));

            DOMStoreWriteTransaction writeTx1 = txChainProxy.newReadWriteTransaction();

            NormalizedNode<?, ?> writeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            writeTx1.write(TestModel.TEST_PATH, writeNode1);

            writeTx1.ready();

            verifyOneBatchedModification(txActorRef1, new WriteModification(TestModel.TEST_PATH, writeNode1), true);

            String tx2MemberName = "mock-member";
            ActorRef shardActorRef2 = setupActorContextWithoutInitialCreateTransaction(getSystem());
            ActorRef txActorRef2 = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE,
                    DataStoreVersions.CURRENT_VERSION, tx2MemberName, shardActorRef2);

            expectBatchedModifications(txActorRef2, 1);

            final NormalizedNode<?, ?> writeNode2 = ImmutableNodes.containerNode(TestModel.OUTER_LIST_QNAME);

            final DOMStoreWriteTransaction writeTx2 = txChainProxy.newReadWriteTransaction();

            final AtomicReference<Exception> caughtEx = new AtomicReference<>();
            final CountDownLatch write2Complete = new CountDownLatch(1);
            new Thread() {
                @Override
                public void run() {
                    try {
                        writeTx2.write(TestModel.OUTER_LIST_PATH, writeNode2);
                    } catch (Exception e) {
                        caughtEx.set(e);
                    } finally {
                        write2Complete.countDown();
                    }
                }
            }.start();

            assertEquals("Tx 2 write should've completed", true, write2Complete.await(5, TimeUnit.SECONDS));

            if (caughtEx.get() != null) {
                throw caughtEx.get();
            }

            try {
                verify(mockActorContext, never()).executeOperationAsync(
                        eq(getSystem().actorSelection(shardActorRef2.path())),
                        eqCreateTransaction(tx2MemberName, READ_WRITE));
            } catch (AssertionError e) {
                fail("Tx 2 should not have initiated until the Tx 1's ready future completed");
            }

            readyReplyPromise1.success(readyTxReply(txActorRef1.path().toString()).value().get().get());

            verify(mockActorContext, timeout(5000)).executeOperationAsync(
                    eq(getSystem().actorSelection(shardActorRef2.path())),
                    eqCreateTransaction(tx2MemberName, READ_WRITE), any(Timeout.class));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testChainedWriteTransactionsWithPreviousTxNotReady() throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY);

        expectBatchedModifications(actorRef, 1);

        try (TransactionChainProxy txChainProxy = new TransactionChainProxy(mockComponentFactory)) {

            DOMStoreWriteTransaction writeTx1 = txChainProxy.newWriteOnlyTransaction();

            NormalizedNode<?, ?> writeNode1 = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            writeTx1.write(TestModel.TEST_PATH, writeNode1);

            NormalizedNode<?, ?> writeNode2 = ImmutableNodes.containerNode(TestModel.OUTER_LIST_QNAME);

            txChainProxy.newWriteOnlyTransaction();
        }
    }
}
