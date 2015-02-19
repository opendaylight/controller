/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;


public class InMemoryDataStoreTest {

    private SchemaContext schemaContext;
    private InMemoryDOMDataStore domStore;

    @Before
    public void setupStore() {
        domStore = new InMemoryDOMDataStore("TEST", MoreExecutors.newDirectExecutorService());
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
         * Writes /test in writeTx
         */
        NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        writeTx.write(TestModel.TEST_PATH, testNode);

        /**
         * Reads /test from writeTx Read should return container.
         */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(TestModel.TEST_PATH);
        assertEquals("read: isPresent", true, writeTxContainer.get().isPresent());
        assertEquals("read: data", testNode, writeTxContainer.get().get());

        /**
         * Reads /test from readTx Read should return Absent.
         */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> readTxContainer = readTx.read(TestModel.TEST_PATH);
        assertEquals("read: isPresent", false, readTxContainer.get().isPresent());
    }

    @Test
    public void testTransactionCommit() throws InterruptedException, ExecutionException {

        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        assertNotNull(writeTx);

        /**
         * Writes /test in writeTx
         */
        NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        writeTx.write(TestModel.TEST_PATH, testNode);

        /**
         * Reads /test from writeTx Read should return container.
         */
        ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(TestModel.TEST_PATH);
        assertEquals("read: isPresent", true, writeTxContainer.get().isPresent());
        assertEquals("read: data", testNode, writeTxContainer.get().get());

        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

        assertThreePhaseCommit(cohort);

        Optional<NormalizedNode<?, ?>> afterCommitRead = domStore.newReadOnlyTransaction().read(TestModel.TEST_PATH)
                .get();
        assertEquals("After commit read: isPresent", true, afterCommitRead.isPresent());
        assertEquals("After commit read: data", testNode, afterCommitRead.get());
    }

    @Test
    public void testDelete() throws Exception {

        DOMStoreWriteTransaction writeTx = domStore.newWriteOnlyTransaction();
        assertNotNull( writeTx );

        // Write /test and commit

        writeTx.write( TestModel.TEST_PATH, ImmutableNodes.containerNode( TestModel.TEST_QNAME ) );

        assertThreePhaseCommit( writeTx.ready() );

        Optional<NormalizedNode<?, ?>> afterCommitRead = domStore.newReadOnlyTransaction().
                read(TestModel.TEST_PATH ).get();
        assertEquals( "After commit read: isPresent", true, afterCommitRead.isPresent() );

        // Delete /test and verify

        writeTx = domStore.newWriteOnlyTransaction();

        writeTx.delete( TestModel.TEST_PATH );

        assertThreePhaseCommit( writeTx.ready() );

        afterCommitRead = domStore.newReadOnlyTransaction().
                read(TestModel.TEST_PATH ).get();
        assertEquals( "After commit read: isPresent", false, afterCommitRead.isPresent() );
    }

    @Test
    public void testMerge() throws Exception {

        DOMStoreWriteTransaction writeTx = domStore.newWriteOnlyTransaction();
        assertNotNull( writeTx );

        ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier( new NodeIdentifier( TestModel.TEST_QNAME ) )
                .addChild( ImmutableNodes.mapNodeBuilder( TestModel.OUTER_LIST_QNAME )
                        .addChild( ImmutableNodes.mapEntry( TestModel.OUTER_LIST_QNAME,
                                                            TestModel.ID_QNAME, 1 ) ).build() ).build();

        writeTx.merge( TestModel.TEST_PATH, containerNode );

        assertThreePhaseCommit( writeTx.ready() );

        Optional<NormalizedNode<?, ?>> afterCommitRead = domStore.newReadOnlyTransaction().
                read(TestModel.TEST_PATH ).get();
        assertEquals( "After commit read: isPresent", true, afterCommitRead.isPresent() );
        assertEquals( "After commit read: data", containerNode, afterCommitRead.get() );

        // Merge a new list entry node

        writeTx = domStore.newWriteOnlyTransaction();
        assertNotNull( writeTx );

        containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier( new NodeIdentifier( TestModel.TEST_QNAME ) )
                .addChild( ImmutableNodes.mapNodeBuilder( TestModel.OUTER_LIST_QNAME )
                        .addChild( ImmutableNodes.mapEntry( TestModel.OUTER_LIST_QNAME,
                                                            TestModel.ID_QNAME, 1 ) )
                        .addChild( ImmutableNodes.mapEntry( TestModel.OUTER_LIST_QNAME,
                                                            TestModel.ID_QNAME, 2 ) ).build() ).build();

        writeTx.merge( TestModel.TEST_PATH, containerNode );

        assertThreePhaseCommit( writeTx.ready() );

        afterCommitRead = domStore.newReadOnlyTransaction().read(TestModel.TEST_PATH ).get();
        assertEquals( "After commit read: isPresent", true, afterCommitRead.isPresent() );
        assertEquals( "After commit read: data", containerNode, afterCommitRead.get() );
    }


    @Test
    public void testExistsForExistingData() throws Exception {

        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        assertNotNull( writeTx );

        ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
            .withNodeIdentifier( new NodeIdentifier( TestModel.TEST_QNAME ) )
            .addChild( ImmutableNodes.mapNodeBuilder( TestModel.OUTER_LIST_QNAME )
                .addChild( ImmutableNodes.mapEntry( TestModel.OUTER_LIST_QNAME,
                    TestModel.ID_QNAME, 1 ) ).build() ).build();

        writeTx.merge( TestModel.TEST_PATH, containerNode );

        CheckedFuture<Boolean, ReadFailedException> exists =
            writeTx.exists(TestModel.TEST_PATH);

        assertEquals(true, exists.checkedGet());

        DOMStoreThreePhaseCommitCohort ready = writeTx.ready();

        ready.preCommit().get();

        ready.commit().get();

        DOMStoreReadTransaction readTx = domStore.newReadOnlyTransaction();
        assertNotNull( readTx );

        exists =
            readTx.exists(TestModel.TEST_PATH);

        assertEquals(true, exists.checkedGet());
    }

    @Test
    public void testExistsForNonExistingData() throws Exception {

        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        assertNotNull( writeTx );

        CheckedFuture<Boolean, ReadFailedException> exists =
            writeTx.exists(TestModel.TEST_PATH);

        assertEquals(false, exists.checkedGet());

        DOMStoreReadTransaction readTx = domStore.newReadOnlyTransaction();
        assertNotNull( readTx );

        exists =
            readTx.exists(TestModel.TEST_PATH);

        assertEquals(false, exists.checkedGet());
    }

    @Test(expected=ReadFailedException.class)
    public void testExistsThrowsReadFailedException() throws Exception {

        DOMStoreReadTransaction readTx = domStore.newReadOnlyTransaction();
        assertNotNull( readTx );

        readTx.close();

        readTx.exists(TestModel.TEST_PATH).checkedGet();
    }



    @Test(expected=ReadFailedException.class)
    public void testReadWithReadOnlyTransactionClosed() throws Throwable {

        DOMStoreReadTransaction readTx = domStore.newReadOnlyTransaction();
        assertNotNull( readTx );

        readTx.close();

        doReadAndThrowEx( readTx );
    }

    @Test(expected=ReadFailedException.class)
    public void testReadWithReadOnlyTransactionFailure() throws Throwable {

        DataTreeSnapshot mockSnapshot = Mockito.mock( DataTreeSnapshot.class );
        Mockito.doThrow( new RuntimeException( "mock ex" ) ).when( mockSnapshot )
        .readNode( Mockito.any( YangInstanceIdentifier.class ) );

        DOMStoreReadTransaction readTx = new SnapshotBackedReadTransaction("1", true, mockSnapshot);

        doReadAndThrowEx( readTx );
    }

    @Test(expected=ReadFailedException.class)
    public void testReadWithReadWriteTransactionClosed() throws Throwable {

        DOMStoreReadTransaction readTx = domStore.newReadWriteTransaction();
        assertNotNull( readTx );

        readTx.close();

        doReadAndThrowEx( readTx );
    }

    @Test(expected=ReadFailedException.class)
    public void testReadWithReadWriteTransactionFailure() throws Throwable {

        DataTreeSnapshot mockSnapshot = Mockito.mock( DataTreeSnapshot.class );
        DataTreeModification mockModification = Mockito.mock( DataTreeModification.class );
        Mockito.doThrow( new RuntimeException( "mock ex" ) ).when( mockModification )
        .readNode( Mockito.any( YangInstanceIdentifier.class ) );
        Mockito.doReturn( mockModification ).when( mockSnapshot ).newModification();
        TransactionReadyPrototype mockReady = Mockito.mock( TransactionReadyPrototype.class );
        DOMStoreReadTransaction readTx = new SnapshotBackedReadWriteTransaction("1", false, mockSnapshot, mockReady);

        doReadAndThrowEx( readTx );
    }

    private void doReadAndThrowEx( final DOMStoreReadTransaction readTx ) throws Throwable {

        try {
            readTx.read(TestModel.TEST_PATH).get();
        } catch( ExecutionException e ) {
            throw e.getCause();
        }
    }

    @Test(expected=IllegalStateException.class)
    public void testWriteWithTransactionReady() throws Exception {

        DOMStoreWriteTransaction writeTx = domStore.newWriteOnlyTransaction();

        writeTx.ready();

        // Should throw ex
        writeTx.write( TestModel.TEST_PATH, ImmutableNodes.containerNode( TestModel.TEST_QNAME ) );
    }

    @Test(expected=IllegalStateException.class)
    public void testReadyWithTransactionAlreadyReady() throws Exception {

        DOMStoreWriteTransaction writeTx = domStore.newWriteOnlyTransaction();

        writeTx.ready();

        // Should throw ex
        writeTx.ready();
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

        Optional<NormalizedNode<?, ?>> afterCommitRead = domStore.newReadOnlyTransaction().read(TestModel.TEST_PATH)
                .get();
        assertFalse(afterCommitRead.isPresent());
    }

    @Test
    public void testTransactionChain() throws InterruptedException, ExecutionException {
        DOMStoreTransactionChain txChain = domStore.createTransactionChain();
        assertNotNull(txChain);

        /**
         * We alocate new read-write transaction and write /test
         *
         *
         */
        DOMStoreReadWriteTransaction firstTx = txChain.newReadWriteTransaction();
        assertTestContainerWrite(firstTx);

        /**
         * First transaction is marked as ready, we are able to allocate chained
         * transactions
         */
        DOMStoreThreePhaseCommitCohort firstWriteTxCohort = firstTx.ready();

        /**
         * We alocate chained transaction - read transaction, note first one is
         * still not commited to datastore.
         */
        DOMStoreReadTransaction secondReadTx = txChain.newReadOnlyTransaction();

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
        DOMStoreReadWriteTransaction thirdDeleteTx = txChain.newReadWriteTransaction();

        /**
         * We test existence of /test in third transaction container should
         * still be visible from first one (which is still uncommmited).
         *
         *
         */
        assertTestContainerExists(thirdDeleteTx);

        /**
         * We delete node in third transaction
         */
        thirdDeleteTx.delete(TestModel.TEST_PATH);

        /**
         * third transaction is sealed.
         */
        DOMStoreThreePhaseCommitCohort thirdDeleteTxCohort = thirdDeleteTx.ready();

        /**
         * We commit first transaction
         *
         */
        assertThreePhaseCommit(firstWriteTxCohort);

        // Alocates store transacion
        DOMStoreReadTransaction storeReadTx = domStore.newReadOnlyTransaction();
        /**
         * We verify transaction is commited to store, container should exists
         * in datastore.
         */
        assertTestContainerExists(storeReadTx);
        /**
         * We commit third transaction
         *
         */
        assertThreePhaseCommit(thirdDeleteTxCohort);
    }

    @Test
    @Ignore
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

    private static void assertThreePhaseCommit(final DOMStoreThreePhaseCommitCohort cohort)
            throws InterruptedException, ExecutionException {
        assertTrue(cohort.canCommit().get().booleanValue());
        cohort.preCommit().get();
        cohort.commit().get();
    }

    private static Optional<NormalizedNode<?, ?>> assertTestContainerWrite(final DOMStoreReadWriteTransaction writeTx)
            throws InterruptedException, ExecutionException {
        /**
         *
         * Writes /test in writeTx
         *
         */
        writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        return assertTestContainerExists(writeTx);
    }

    /**
     * Reads /test from readTx Read should return container.
     */
    private static Optional<NormalizedNode<?, ?>> assertTestContainerExists(final DOMStoreReadTransaction readTx)
            throws InterruptedException, ExecutionException {

        ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = readTx.read(TestModel.TEST_PATH);
        assertTrue(writeTxContainer.get().isPresent());
        return writeTxContainer.get();
    }

}
