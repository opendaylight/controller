package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ForwardingExecutorService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitDeadlockException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.util.concurrent.DeadlockDetectingListeningExecutorService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DOMBrokerTest {

    private SchemaContext schemaContext;
    private AbstractDOMDataBroker domBroker;
    private ListeningExecutorService executor;
    private ExecutorService futureExecutor;
    private CommitExecutorService commitExecutor;

    @Before
    public void setupStore() {

        InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER",
                MoreExecutors.newDirectExecutorService());
        InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("CFG",
                MoreExecutors.newDirectExecutorService());
        schemaContext = TestModel.createTestContext();

        operStore.onGlobalContextUpdated(schemaContext);
        configStore.onGlobalContextUpdated(schemaContext);

        ImmutableMap<LogicalDatastoreType, DOMStore> stores = ImmutableMap.<LogicalDatastoreType, DOMStore> builder() //
                .put(CONFIGURATION, configStore) //
                .put(OPERATIONAL, operStore) //
                .build();

        commitExecutor = new CommitExecutorService(Executors.newSingleThreadExecutor());
        futureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(1, 5, "FCB");
        executor = new DeadlockDetectingListeningExecutorService(commitExecutor,
                TransactionCommitDeadlockException.DEADLOCK_EXCEPTION_SUPPLIER, futureExecutor);
        domBroker = new SerializedDOMDataBroker(stores, executor);
    }

    @After
    public void tearDown() {
        if( executor != null ) {
            executor.shutdownNow();
        }

        if(futureExecutor != null) {
            futureExecutor.shutdownNow();
        }
    }

    @Test(timeout=10000)
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

    @Test(timeout=10000)
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

        writeTx.submit().get();

        Optional<NormalizedNode<?, ?>> afterCommitRead = domBroker.newReadOnlyTransaction()
                .read(OPERATIONAL, TestModel.TEST_PATH).get();
        assertTrue(afterCommitRead.isPresent());
    }

    @Test(expected=TransactionCommitFailedException.class)
    public void testRejectedCommit() throws Exception {

        commitExecutor.delegate = Mockito.mock( ExecutorService.class );
        Mockito.doThrow( new RejectedExecutionException( "mock" ) )
            .when( commitExecutor.delegate ).execute( Mockito.any( Runnable.class ) );
        Mockito.doNothing().when( commitExecutor.delegate ).shutdown();
        Mockito.doReturn( Collections.emptyList() ).when( commitExecutor.delegate ).shutdownNow();
        Mockito.doReturn( "" ).when( commitExecutor.delegate ).toString();
        Mockito.doReturn( true ).when( commitExecutor.delegate )
            .awaitTermination( Mockito.anyLong(), Mockito.any( TimeUnit.class ) );

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        writeTx.put( OPERATIONAL, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME) );

        writeTx.submit().checkedGet( 5, TimeUnit.SECONDS );
    }

    /**
     * Tests a simple DataChangeListener notification after a write.
     */
    @Test
    public void testDataChangeListener() throws Throwable {

        final NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode( TestModel.TEST_QNAME );

        TestDOMDataChangeListener dcListener = new TestDOMDataChangeListener();

        domBroker.registerDataChangeListener( OPERATIONAL, TestModel.TEST_PATH,
                                              dcListener, DataChangeScope.BASE );

        final DOMDataWriteTransaction writeTx = domBroker.newWriteOnlyTransaction();
        assertNotNull( writeTx );

        writeTx.put( OPERATIONAL, TestModel.TEST_PATH, testNode );

        AtomicReference<Throwable> caughtEx = submitTxAsync( writeTx );

        dcListener.waitForChange();

        if( caughtEx.get() != null ) {
            throw caughtEx.get();
        }

        NormalizedNode<?, ?> actualNode = dcListener.change.getCreatedData().get( TestModel.TEST_PATH );
        assertEquals( "Created node", testNode, actualNode );
    }

    /**
     * Tests a DataChangeListener that does an async submit of a write Tx in its onDataChanged method.
     * This should succeed without deadlock.
     */
    @Test
    public void testDataChangeListenerDoingAsyncWriteTxSubmit() throws Throwable {

        final AtomicReference<Throwable> caughtCommitEx = new AtomicReference<>();
        final CountDownLatch commitCompletedLatch = new CountDownLatch( 1 );

        TestDOMDataChangeListener dcListener = new TestDOMDataChangeListener() {
            @Override
            public void onDataChanged( final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change ) {

                DOMDataWriteTransaction writeTx = domBroker.newWriteOnlyTransaction();
                writeTx.put( OPERATIONAL, TestModel.TEST2_PATH,
                             ImmutableNodes.containerNode( TestModel.TEST2_QNAME ) );
                Futures.addCallback( writeTx.submit(), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess( final Void result ) {
                        commitCompletedLatch.countDown();
                    }

                    @Override
                    public void onFailure( final Throwable t ) {
                        caughtCommitEx.set( t );
                        commitCompletedLatch.countDown();
                    }
                } );

                super.onDataChanged( change );
            }
        };

        domBroker.registerDataChangeListener( OPERATIONAL, TestModel.TEST_PATH,
                                              dcListener, DataChangeScope.BASE );

        final DOMDataWriteTransaction writeTx = domBroker.newWriteOnlyTransaction();
        assertNotNull( writeTx );

        writeTx.put( OPERATIONAL, TestModel.TEST_PATH, ImmutableNodes.containerNode( TestModel.TEST_QNAME ) );

        AtomicReference<Throwable> caughtEx = submitTxAsync( writeTx );

        dcListener.waitForChange();

        if( caughtEx.get() != null ) {
            throw caughtEx.get();
        }

        assertTrue( "Commit Future was not invoked", commitCompletedLatch.await( 5, TimeUnit.SECONDS ) );

        if( caughtCommitEx.get() != null ) {
            throw caughtCommitEx.get();
        }
    }

    /**
     * Tests a DataChangeListener that does a blocking submit of a write Tx in its onDataChanged method.
     * This should throw an exception and not deadlock.
     */
    @Test(expected=TransactionCommitDeadlockException.class)
    public void testDataChangeListenerDoingBlockingWriteTxSubmit() throws Throwable {

        final AtomicReference<Throwable> caughtCommitEx = new AtomicReference<>();

        TestDOMDataChangeListener dcListener = new TestDOMDataChangeListener() {
            @Override
            public void onDataChanged( final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change ) {
                DOMDataWriteTransaction writeTx = domBroker.newWriteOnlyTransaction();
                writeTx.put( OPERATIONAL, TestModel.TEST2_PATH,
                             ImmutableNodes.containerNode( TestModel.TEST2_QNAME ) );
                try {
                    writeTx.submit().get();
                } catch( ExecutionException e ) {
                    caughtCommitEx.set( e.getCause() );
                } catch( Exception e ) {
                    caughtCommitEx.set( e );
                }
                finally {
                    super.onDataChanged( change );
                }
            }
        };

        domBroker.registerDataChangeListener( OPERATIONAL, TestModel.TEST_PATH,
                                              dcListener, DataChangeScope.BASE );

        final DOMDataWriteTransaction writeTx = domBroker.newWriteOnlyTransaction();
        assertNotNull( writeTx );

        writeTx.put( OPERATIONAL, TestModel.TEST_PATH, ImmutableNodes.containerNode( TestModel.TEST_QNAME ) );

        AtomicReference<Throwable> caughtEx = submitTxAsync( writeTx );

        dcListener.waitForChange();

        if( caughtEx.get() != null ) {
            throw caughtEx.get();
        }

        if( caughtCommitEx.get() != null ) {
            throw caughtCommitEx.get();
        }
    }

    AtomicReference<Throwable> submitTxAsync( final DOMDataWriteTransaction writeTx ) {
        final AtomicReference<Throwable> caughtEx = new AtomicReference<>();
        new Thread() {
            @Override
            public void run() {

                try {
                    writeTx.submit();
                } catch( Throwable e ) {
                    caughtEx.set( e );
                }
            }

        }.start();

        return caughtEx;
    }

    static class TestDOMDataChangeListener implements DOMDataChangeListener {

        volatile AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change;
        private final CountDownLatch latch = new CountDownLatch( 1 );

        @Override
        public void onDataChanged( final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change ) {
            this.change = change;
            latch.countDown();
        }

        void waitForChange() throws InterruptedException {
            assertTrue( "onDataChanged was not called", latch.await( 5, TimeUnit.SECONDS ) );
        }
    }

    static class CommitExecutorService extends ForwardingExecutorService {

        ExecutorService delegate;

        public CommitExecutorService( final ExecutorService delegate ) {
            this.delegate = delegate;
        }

        @Override
        protected ExecutorService delegate() {
            return delegate;
        }
    }
}
