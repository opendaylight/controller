package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class DOMBrokerTest {

    private SchemaContext schemaContext;
    private DOMDataBrokerImpl domBroker;

    @Before
    public void setupStore() {
        InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER", MoreExecutors.sameThreadExecutor());
        InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("CFG", MoreExecutors.sameThreadExecutor());
        schemaContext = TestModel.createTestContext();

        operStore.onGlobalContextUpdated(schemaContext);
        configStore.onGlobalContextUpdated(schemaContext);

        ImmutableMap<LogicalDatastoreType, DOMStore> stores = ImmutableMap.<LogicalDatastoreType, DOMStore> builder() //
                .put(CONFIGURATION, configStore) //
                .put(OPERATIONAL, operStore) //
                .build();

        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        domBroker = new DOMDataBrokerImpl(stores, executor);
    }

    @Test
    public void testTransactionIsolation() throws InterruptedException, ExecutionException {

        assertNotNull(domBroker);

        DOMDataReadTransaction readTx = domBroker.newReadOnlyTransaction();
        assertNotNull(readTx);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);
        /**
         *
         * Writes /test in writeTx
         *
         */
        writeTx.put(OPERATIONAL, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        /**
         *
         * Reads /test from writeTx Read should return container.
         *
         */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(OPERATIONAL,
                TestModel.TEST_PATH);
        assertTrue(writeTxContainer.get().isPresent());

        /**
         *
         * Reads /test from readTx Read should return Absent.
         *
         */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> readTxContainer = readTx
                .read(OPERATIONAL, TestModel.TEST_PATH);
        assertFalse(readTxContainer.get().isPresent());
    }

    @Test
    public void testTransactionCommit() throws InterruptedException, ExecutionException {

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);
        /**
         *
         * Writes /test in writeTx
         *
         */
        writeTx.put(OPERATIONAL, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        /**
         *
         * Reads /test from writeTx Read should return container.
         *
         */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(OPERATIONAL,
                TestModel.TEST_PATH);
        assertTrue(writeTxContainer.get().isPresent());

        writeTx.commit().get();

        Optional<NormalizedNode<?, ?>> afterCommitRead = domBroker.newReadOnlyTransaction()
                .read(OPERATIONAL, TestModel.TEST_PATH).get();
        assertTrue(afterCommitRead.isPresent());
    }



}
