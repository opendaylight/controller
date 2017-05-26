/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DOMTransactionChainTest {

    private SchemaContext schemaContext;
    private AbstractDOMDataBroker domBroker;

    @Before
    public void setupStore() {
        InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER", MoreExecutors.newDirectExecutorService());
        InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("CFG", MoreExecutors.newDirectExecutorService());
        schemaContext = TestModel.createTestContext();

        operStore.onGlobalContextUpdated(schemaContext);
        configStore.onGlobalContextUpdated(schemaContext);

        ImmutableMap<LogicalDatastoreType, DOMStore> stores = ImmutableMap.<LogicalDatastoreType, DOMStore> builder() //
                .put(CONFIGURATION, configStore) //
                .put(OPERATIONAL, operStore) //
                .build();

        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        domBroker = new SerializedDOMDataBroker(stores, executor);
    }

    @Test
    public void testTransactionChainNoConflict() throws InterruptedException, ExecutionException, TimeoutException {
        BlockingTransactionChainListener listener = new BlockingTransactionChainListener();
        DOMTransactionChain txChain = domBroker.createTransactionChain(listener);
        assertNotNull(txChain);

        /**
         * We alocate new read-write transaction and write /test
         *
         *
         */
        DOMDataReadWriteTransaction firstTx = allocateAndWrite(txChain);

        /**
         * First transaction is marked as ready, we are able to allocate chained
         * transactions
         */
        ListenableFuture<Void> firstWriteTxFuture = firstTx.submit();

        /**
         * We alocate chained transaction - read transaction.
         */
        DOMDataReadTransaction secondReadTx = txChain.newReadOnlyTransaction();

        /**
         *
         * We test if we are able to read data from tx, read should not fail
         * since we are using chained transaction.
         *
         *
         */
        assertTestContainerExists(secondReadTx);

        /**
         *
         * We alocate next transaction, which is still based on first one, but
         * is read-write.
         *
         */
        DOMDataReadWriteTransaction thirdDeleteTx = allocateAndDelete(txChain);

        /**
         * We commit first transaction
         *
         */
        assertCommitSuccessful(firstWriteTxFuture);

        /**
         *
         * Allocates transaction from data store.
         *
         */
        DOMDataReadTransaction storeReadTx = domBroker.newReadOnlyTransaction();

        /**
         * We verify transaction is commited to store, container should exists
         * in datastore.
         */
        assertTestContainerExists(storeReadTx);

        /**
         * third transaction is sealed and commited
         */
        ListenableFuture<Void> thirdDeleteTxFuture = thirdDeleteTx.submit();
        assertCommitSuccessful(thirdDeleteTxFuture);

        /**
         * We close transaction chain.
         */
        txChain.close();

        listener.getSuccessFuture().get(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testTransactionChainNotSealed() throws InterruptedException, ExecutionException, TimeoutException {
        BlockingTransactionChainListener listener = new BlockingTransactionChainListener();
        DOMTransactionChain txChain = domBroker.createTransactionChain(listener);
        assertNotNull(txChain);

        /**
         * We alocate new read-write transaction and write /test
         *
         *
         */
        allocateAndWrite(txChain);

        /**
         * We alocate chained transaction - read transaction, note first one is
         * still not commited to datastore, so this allocation should fail with
         * IllegalStateException.
         */
        try {
            txChain.newReadOnlyTransaction();
            fail("Allocation of secondReadTx should fail with IllegalStateException");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }

    private static DOMDataReadWriteTransaction allocateAndDelete(final DOMTransactionChain txChain)
            throws InterruptedException, ExecutionException {
        DOMDataReadWriteTransaction tx = txChain.newReadWriteTransaction();

        /**
         * We test existence of /test in third transaction container should
         * still be visible from first one (which is still uncommmited).
         *
         */
        assertTestContainerExists(tx);

        /**
         * We delete node in third transaction
         */
        tx.delete(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH);
        return tx;
    }

    private static DOMDataReadWriteTransaction allocateAndWrite(final DOMTransactionChain txChain)
            throws InterruptedException, ExecutionException {
        DOMDataReadWriteTransaction tx = txChain.newReadWriteTransaction();
        assertTestContainerWrite(tx);
        return tx;
    }

    private static void assertCommitSuccessful(final ListenableFuture<Void> future)
            throws InterruptedException, ExecutionException {
        future.get();
    }

    private static void assertTestContainerExists(final DOMDataReadTransaction readTx) throws InterruptedException,
            ExecutionException {
        ListenableFuture<Optional<NormalizedNode<?, ?>>> readFuture = readTx.read(OPERATIONAL, TestModel.TEST_PATH);
        Optional<NormalizedNode<?, ?>> readedData = readFuture.get();
        assertTrue(readedData.isPresent());
    }

    private static void assertTestContainerWrite(final DOMDataReadWriteTransaction tx) throws InterruptedException,
            ExecutionException {
        tx.put(OPERATIONAL, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        assertTestContainerExists(tx);
    }
}
