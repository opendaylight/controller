package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public class InMemoryDataStoreTest {

    private SchemaContext schemaContext;
    private InMemoryDOMDataStore domStore;


    @Before
    public void setupStore() {
        domStore = new InMemoryDOMDataStore("TEST", MoreExecutors.sameThreadExecutor());
        schemaContext = TestModel.createTestContext();
        domStore.onGlobalContextUpdated(schemaContext);

    }


    @Test
    public void testTransactionIsolation() throws InterruptedException, ExecutionException {

        assertNotNull(domStore);


        DOMStoreReadTransaction readTx = domStore.newReadOnlyTransaction();
        assertNotNull(readTx);

        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        assertNotNull(writeTx);
        /**
         *
         * Writes /test in writeTx
         *
         */
        writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        /**
         *
         * Reads /test from writeTx
         * Read should return container.
         *
         */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(TestModel.TEST_PATH);
        assertTrue(writeTxContainer.get().isPresent());

        /**
        *
        * Reads /test from readTx
        * Read should return Absent.
        *
        */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> readTxContainer = readTx.read(TestModel.TEST_PATH);
        assertFalse(readTxContainer.get().isPresent());
    }

    @Test
    public void testTransactionCommit() throws InterruptedException, ExecutionException {

        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        assertNotNull(writeTx);
        /**
         *
         * Writes /test in writeTx
         *
         */
        writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        /**
         *
         * Reads /test from writeTx
         * Read should return container.
         *
         */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(TestModel.TEST_PATH);
        assertTrue(writeTxContainer.get().isPresent());

        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

        assertThreePhaseCommit(cohort);

        Optional<NormalizedNode<?, ?>> afterCommitRead = domStore.newReadOnlyTransaction().read(TestModel.TEST_PATH).get();
        assertTrue(afterCommitRead.isPresent());
    }

    @Test
    public void testTransactionAbort() throws InterruptedException, ExecutionException {

        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        assertNotNull(writeTx);

        assertTestContainerWrite(writeTx);

        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

        assertTrue(cohort.canCommit().get().booleanValue());
        cohort.preCommit().get();
        cohort.abort().get();

        Optional<NormalizedNode<?, ?>> afterCommitRead = domStore.newReadOnlyTransaction().read(TestModel.TEST_PATH).get();
        assertFalse(afterCommitRead.isPresent());
    }

    @Test
    public void testTransactionConflict() throws InterruptedException, ExecutionException {
        DOMStoreReadWriteTransaction txOne = domStore.newReadWriteTransaction();
        DOMStoreReadWriteTransaction txTwo = domStore.newReadWriteTransaction();
        assertTestContainerWrite(txOne);
        assertTestContainerWrite(txTwo);

        /**
         * Commits transaction
         */
        assertThreePhaseCommit(txOne.ready());

        /**
         * Asserts that txTwo could not be commited
         */
        assertFalse(txTwo.ready().canCommit().get());
    }



    private static void assertThreePhaseCommit(final DOMStoreThreePhaseCommitCohort cohort) throws InterruptedException, ExecutionException {
        assertTrue(cohort.canCommit().get().booleanValue());
        cohort.preCommit().get();
        cohort.commit().get();
    }


    private static Optional<NormalizedNode<?, ?>> assertTestContainerWrite(final DOMStoreReadWriteTransaction writeTx) throws InterruptedException, ExecutionException {
        /**
        *
        * Writes /test in writeTx
        *
        */
       writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

       /**
        *
        * Reads /test from writeTx
        * Read should return container.
        *
        */
       ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(TestModel.TEST_PATH);
       assertTrue(writeTxContainer.get().isPresent());
       return writeTxContainer.get();
    }

}
