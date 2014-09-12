/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ExecutorServiceUtil;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager.Invoker;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory DOM Data Store
 *
 * Implementation of {@link DOMStore} which uses {@link DataTree} and other
 * classes such as {@link SnapshotBackedWriteTransaction}.
 * {@link SnapshotBackedReadTransaction} and {@link ResolveDataChangeEventsTask}
 * to implement {@link DOMStore} contract.
 *
 */
public class InMemoryDOMDataStore extends TransactionReadyPrototype implements DOMStore, Identifiable<String>, SchemaContextListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStore.class);
    private static final ListenableFuture<Void> SUCCESSFUL_FUTURE = Futures.immediateFuture(null);

    private static final Invoker<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent> DCL_NOTIFICATION_MGR_INVOKER =
            new Invoker<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent>() {
                @Override
                public void invokeListener(final DataChangeListenerRegistration<?> listener,
                                           final DOMImmutableDataChangeEvent notification ) {
                    final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> inst = listener.getInstance();
                    if (inst != null) {
                        inst.onDataChanged(notification);
                    }
                }
            };

    private final DataTree dataTree = InMemoryDataTreeFactory.getInstance().create();
    private final ListenerTree listenerTree = ListenerTree.create();
    private final AtomicLong txCounter = new AtomicLong(0);
    private final ListeningExecutorService listeningExecutor;

    private final QueuedNotificationManager<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent> dataChangeListenerNotificationManager;
    private final ExecutorService dataChangeListenerExecutor;

    private final ExecutorService domStoreExecutor;
    private final boolean debugTransactions;
    private final String name;

    private volatile AutoCloseable closeable;

    public InMemoryDOMDataStore(final String name, final ExecutorService domStoreExecutor,
            final ExecutorService dataChangeListenerExecutor) {
        this(name, domStoreExecutor, dataChangeListenerExecutor,
             InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, false);
    }

    public InMemoryDOMDataStore(final String name, final ExecutorService domStoreExecutor,
            final ExecutorService dataChangeListenerExecutor, final int maxDataChangeListenerQueueSize,
            final boolean debugTransactions) {
        this.name = Preconditions.checkNotNull(name);
        this.domStoreExecutor = Preconditions.checkNotNull(domStoreExecutor);
        this.listeningExecutor = MoreExecutors.listeningDecorator(this.domStoreExecutor);
        this.dataChangeListenerExecutor = Preconditions.checkNotNull(dataChangeListenerExecutor);
        this.debugTransactions = debugTransactions;

        dataChangeListenerNotificationManager =
                new QueuedNotificationManager<>(this.dataChangeListenerExecutor,
                        DCL_NOTIFICATION_MGR_INVOKER, maxDataChangeListenerQueueSize,
                        "DataChangeListenerQueueMgr");
    }

    public void setCloseable(final AutoCloseable closeable) {
        this.closeable = closeable;
    }

    public QueuedNotificationManager<?, ?> getDataChangeListenerNotificationManager() {
        return dataChangeListenerNotificationManager;
    }

    public ExecutorService getDomStoreExecutor() {
        return domStoreExecutor;
    }

    @Override
    public final String getIdentifier() {
        return name;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new SnapshotBackedReadTransaction(nextIdentifier(), debugTransactions, dataTree.takeSnapshot());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new SnapshotBackedReadWriteTransaction(nextIdentifier(), debugTransactions, dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new SnapshotBackedWriteTransaction(nextIdentifier(), debugTransactions, dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return new DOMStoreTransactionChainImpl();
    }

    @Override
    public synchronized void onGlobalContextUpdated(final SchemaContext ctx) {
        dataTree.setSchemaContext(ctx);
    }

    @Override
    public void close() {
        ExecutorServiceUtil.tryGracefulShutdown(listeningExecutor, 30, TimeUnit.SECONDS);
        ExecutorServiceUtil.tryGracefulShutdown(dataChangeListenerExecutor, 30, TimeUnit.SECONDS);

        if(closeable != null) {
            try {
                closeable.close();
            } catch(Exception e) {
                LOG.debug("Error closing instance", e);
            }
        }
    }

    boolean getDebugTransactions() {
        return debugTransactions;
    }

    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
            final YangInstanceIdentifier path, final L listener, final DataChangeScope scope) {

        /*
         * Make sure commit is not occurring right now. Listener has to be
         * registered and its state capture enqueued at a consistent point.
         *
         * FIXME: improve this to read-write lock, such that multiple listener
         * registrations can occur simultaneously
         */
        final DataChangeListenerRegistration<L> reg;
        synchronized (this) {
            LOG.debug("{}: Registering data change listener {} for {}", name, listener, path);

            reg = listenerTree.registerDataChangeListener(path, listener, scope);

            Optional<NormalizedNode<?, ?>> currentState = dataTree.takeSnapshot().readNode(path);
            if (currentState.isPresent()) {
                final NormalizedNode<?, ?> data = currentState.get();

                final DOMImmutableDataChangeEvent event = DOMImmutableDataChangeEvent.builder(DataChangeScope.BASE) //
                        .setAfter(data) //
                        .addCreated(path, data) //
                        .build();

                dataChangeListenerNotificationManager.submitNotification(reg, event);
            }
        }

        return new AbstractListenerRegistration<L>(listener) {
            @Override
            protected void removeRegistration() {
                synchronized (InMemoryDOMDataStore.this) {
                    reg.close();
                }
            }
        };
    }

    @Override
    protected void transactionAborted(final SnapshotBackedWriteTransaction tx) {
        LOG.debug("Tx: {} is closed.", tx.getIdentifier());
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort transactionReady(final SnapshotBackedWriteTransaction tx, final DataTreeModification tree) {
        LOG.debug("Tx: {} is submitted. Modifications: {}", tx.getIdentifier(), tree);
        return new ThreePhaseCommitImpl(tx, tree);
    }

    private Object nextIdentifier() {
        return name + "-" + txCounter.getAndIncrement();
    }

    private class DOMStoreTransactionChainImpl extends TransactionReadyPrototype implements DOMStoreTransactionChain {
        @GuardedBy("this")
        private SnapshotBackedWriteTransaction allocatedTransaction;
        @GuardedBy("this")
        private DataTreeSnapshot readySnapshot;
        @GuardedBy("this")
        private boolean chainFailed = false;

        @GuardedBy("this")
        private void checkFailed() {
            Preconditions.checkState(!chainFailed, "Transaction chain is failed.");
        }

        @GuardedBy("this")
        private DataTreeSnapshot getSnapshot() {
            checkFailed();

            if (allocatedTransaction != null) {
                Preconditions.checkState(readySnapshot != null, "Previous transaction %s is not ready yet", allocatedTransaction.getIdentifier());
                return readySnapshot;
            } else {
                return dataTree.takeSnapshot();
            }
        }

        @GuardedBy("this")
        private <T extends SnapshotBackedWriteTransaction> T recordTransaction(final T transaction) {
            allocatedTransaction = transaction;
            readySnapshot = null;
            return transaction;
        }

        @Override
        public synchronized DOMStoreReadTransaction newReadOnlyTransaction() {
            final DataTreeSnapshot snapshot = getSnapshot();
            return new SnapshotBackedReadTransaction(nextIdentifier(), getDebugTransactions(), snapshot);
        }

        @Override
        public synchronized DOMStoreReadWriteTransaction newReadWriteTransaction() {
            final DataTreeSnapshot snapshot = getSnapshot();
            return recordTransaction(new SnapshotBackedReadWriteTransaction(nextIdentifier(),
                    getDebugTransactions(), snapshot, this));
        }

        @Override
        public synchronized DOMStoreWriteTransaction newWriteOnlyTransaction() {
            final DataTreeSnapshot snapshot = getSnapshot();
            return recordTransaction(new SnapshotBackedWriteTransaction(nextIdentifier(),
                    getDebugTransactions(), snapshot, this));
        }

        @Override
        protected synchronized void transactionAborted(final SnapshotBackedWriteTransaction tx) {
            if (tx.equals(allocatedTransaction)) {
                Preconditions.checkState(readySnapshot == null, "Unexpected abort of transaction %s with ready snapshot %s", tx, readySnapshot);
                allocatedTransaction = null;
            }
        }

        @Override
        protected synchronized DOMStoreThreePhaseCommitCohort transactionReady(final SnapshotBackedWriteTransaction tx, final DataTreeModification tree) {
            Preconditions.checkState(tx.equals(allocatedTransaction), "Mis-ordered ready transaction %s last allocated was %s", tx, allocatedTransaction);
            if (readySnapshot != null) {
                // The snapshot should have been cleared
                LOG.warn("Uncleared snapshot {} encountered, overwritten with transaction {} snapshot {}", readySnapshot, tx, tree);
            }

            final DOMStoreThreePhaseCommitCohort cohort = InMemoryDOMDataStore.this.transactionReady(tx, tree);
            readySnapshot = tree;
            return new ChainedTransactionCommitImpl(tx, cohort, this);
        }

        @Override
        public void close() {
            // FIXME: this call doesn't look right here - listeningExecutor is shared and owned
            // by the outer class.
            //listeningExecutor.shutdownNow();
        }

        protected synchronized void onTransactionFailed(final SnapshotBackedWriteTransaction transaction,
                final Throwable t) {
            chainFailed = true;
        }

        public synchronized void onTransactionCommited(final SnapshotBackedWriteTransaction transaction) {
            // If the committed transaction was the one we allocated last,
            // we clear it and the ready snapshot, so the next transaction
            // allocated refers to the data tree directly.
            if (transaction.equals(allocatedTransaction)) {
                if (readySnapshot == null) {
                    LOG.warn("Transaction {} committed while no ready snapshot present", transaction);
                }

                allocatedTransaction = null;
                readySnapshot = null;
            }
        }
    }

    private static class ChainedTransactionCommitImpl implements DOMStoreThreePhaseCommitCohort {
        private final SnapshotBackedWriteTransaction transaction;
        private final DOMStoreThreePhaseCommitCohort delegate;
        private final DOMStoreTransactionChainImpl txChain;

        protected ChainedTransactionCommitImpl(final SnapshotBackedWriteTransaction transaction,
                final DOMStoreThreePhaseCommitCohort delegate, final DOMStoreTransactionChainImpl txChain) {
            this.transaction = transaction;
            this.delegate = delegate;
            this.txChain = txChain;
        }

        @Override
        public ListenableFuture<Boolean> canCommit() {
            return delegate.canCommit();
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            return delegate.preCommit();
        }

        @Override
        public ListenableFuture<Void> abort() {
            return delegate.abort();
        }

        @Override
        public ListenableFuture<Void> commit() {
            ListenableFuture<Void> commitFuture = delegate.commit();
            Futures.addCallback(commitFuture, new FutureCallback<Void>() {
                @Override
                public void onFailure(final Throwable t) {
                    txChain.onTransactionFailed(transaction, t);
                }

                @Override
                public void onSuccess(final Void result) {
                    txChain.onTransactionCommited(transaction);
                }
            });
            return commitFuture;
        }
    }

    private class ThreePhaseCommitImpl implements DOMStoreThreePhaseCommitCohort {
        private final SnapshotBackedWriteTransaction transaction;
        private final DataTreeModification modification;

        private ResolveDataChangeEventsTask listenerResolver;
        private DataTreeCandidate candidate;

        public ThreePhaseCommitImpl(final SnapshotBackedWriteTransaction writeTransaction, final DataTreeModification modification) {
            this.transaction = writeTransaction;
            this.modification = modification;
        }

        @Override
        public ListenableFuture<Boolean> canCommit() {
            return listeningExecutor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws TransactionCommitFailedException {
                    try {
                        dataTree.validate(modification);
                        LOG.debug("Store Transaction: {} can be committed", transaction.getIdentifier());
                        return true;
                    } catch (ConflictingModificationAppliedException e) {
                        LOG.warn("Store Tx: {} Conflicting modification for {}.", transaction.getIdentifier(),
                                e.getPath());
                        transaction.warnDebugContext(LOG);
                        throw new OptimisticLockFailedException("Optimistic lock failed.",e);
                    } catch (DataValidationFailedException e) {
                        LOG.warn("Store Tx: {} Data Precondition failed for {}.", transaction.getIdentifier(),
                                e.getPath(), e);
                        transaction.warnDebugContext(LOG);
                        throw new TransactionCommitFailedException("Data did not pass validation.",e);
                    }
                }
            });
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            return listeningExecutor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    candidate = dataTree.prepare(modification);
                    listenerResolver = ResolveDataChangeEventsTask.create(candidate, listenerTree);
                    return null;
                }
            });
        }

        @Override
        public ListenableFuture<Void> abort() {
            candidate = null;
            return SUCCESSFUL_FUTURE;
        }

        @Override
        public ListenableFuture<Void> commit() {
            checkState(candidate != null, "Proposed subtree must be computed");

            /*
             * The commit has to occur atomically with regard to listener
             * registrations.
             */
            synchronized (InMemoryDOMDataStore.this) {
                dataTree.commit(candidate);
                listenerResolver.resolve(dataChangeListenerNotificationManager);
            }

            return SUCCESSFUL_FUTURE;
        }
    }
}
