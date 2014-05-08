/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkState;

/**
 * In-memory DOM Data Store
 *
 * Implementation of {@link DOMStore} which uses {@link DataTree} and other
 * classes such as {@link SnapshotBackedWriteTransaction}.
 * {@link SnapshotBackedReadTransaction} and {@link ResolveDataChangeEventsTask}
 * to implement {@link DOMStore} contract.
 *
 */
public class InMemoryDOMDataStore implements DOMStore, Identifiable<String>, SchemaContextListener,
        TransactionReadyPrototype,AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStore.class);
    private final DataTree dataTree = InMemoryDataTreeFactory.getInstance().create();
    private final ListenerTree listenerTree = ListenerTree.create();
    private final AtomicLong txCounter = new AtomicLong(0);
    private final ListeningExecutorService executor;

    private final String name;

    public InMemoryDOMDataStore(final String name, final ListeningExecutorService executor) {
        this.name = Preconditions.checkNotNull(name);
        this.executor = Preconditions.checkNotNull(executor);
    }

    @Override
    public final String getIdentifier() {
        return name;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new SnapshotBackedReadTransaction(nextIdentifier(), dataTree.takeSnapshot());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new SnapshotBackedReadWriteTransaction(nextIdentifier(), dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new SnapshotBackedWriteTransaction(nextIdentifier(), dataTree.takeSnapshot(), this);
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
    public void close(){
        executor.shutdownNow();
    }
    @Override
    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
            final InstanceIdentifier path, final L listener, final DataChangeScope scope) {

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
                executor.submit(new ChangeListenerNotifyTask(Collections.singletonList(reg), event));
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
    public synchronized DOMStoreThreePhaseCommitCohort ready(final SnapshotBackedWriteTransaction writeTx) {
        LOG.debug("Tx: {} is submitted. Modifications: {}", writeTx.getIdentifier(), writeTx.getMutatedView());
        return new ThreePhaseCommitImpl(writeTx);
    }

    private Object nextIdentifier() {
        return name + "-" + txCounter.getAndIncrement();
    }

    private class DOMStoreTransactionChainImpl implements DOMStoreTransactionChain, TransactionReadyPrototype {

        @GuardedBy("this")
        private SnapshotBackedWriteTransaction latestOutstandingTx;

        private boolean chainFailed = false;

        private void checkFailed() {
            Preconditions.checkState(!chainFailed, "Transaction chain is failed.");
        }

        @Override
        public synchronized DOMStoreReadTransaction newReadOnlyTransaction() {
            final DataTreeSnapshot snapshot;
            checkFailed();
            if (latestOutstandingTx != null) {
                checkState(latestOutstandingTx.isReady(), "Previous transaction in chain must be ready.");
                snapshot = latestOutstandingTx.getMutatedView();
            } else {
                snapshot = dataTree.takeSnapshot();
            }
            return new SnapshotBackedReadTransaction(nextIdentifier(), snapshot);
        }

        @Override
        public synchronized DOMStoreReadWriteTransaction newReadWriteTransaction() {
            final DataTreeSnapshot snapshot;
            checkFailed();
            if (latestOutstandingTx != null) {
                checkState(latestOutstandingTx.isReady(), "Previous transaction in chain must be ready.");
                snapshot = latestOutstandingTx.getMutatedView();
            } else {
                snapshot = dataTree.takeSnapshot();
            }
            final SnapshotBackedReadWriteTransaction ret = new SnapshotBackedReadWriteTransaction(nextIdentifier(),
                    snapshot, this);
            latestOutstandingTx = ret;
            return ret;
        }

        @Override
        public synchronized DOMStoreWriteTransaction newWriteOnlyTransaction() {
            final DataTreeSnapshot snapshot;
            checkFailed();
            if (latestOutstandingTx != null) {
                checkState(latestOutstandingTx.isReady(), "Previous transaction in chain must be ready.");
                snapshot = latestOutstandingTx.getMutatedView();
            } else {
                snapshot = dataTree.takeSnapshot();
            }
            final SnapshotBackedWriteTransaction ret = new SnapshotBackedWriteTransaction(nextIdentifier(), snapshot,
                    this);
            latestOutstandingTx = ret;
            return ret;
        }

        @Override
        public DOMStoreThreePhaseCommitCohort ready(final SnapshotBackedWriteTransaction tx) {
            DOMStoreThreePhaseCommitCohort storeCohort = InMemoryDOMDataStore.this.ready(tx);
            return new ChainedTransactionCommitImpl(tx, storeCohort, this);
        }

        @Override
        public void close() {

             executor.shutdownNow();

        }

        protected synchronized void onTransactionFailed(final SnapshotBackedWriteTransaction transaction,
                final Throwable t) {
            chainFailed = true;

        }

        public synchronized void onTransactionCommited(final SnapshotBackedWriteTransaction transaction) {
            // If commited transaction is latestOutstandingTx we clear
            // latestOutstandingTx
            // field in order to base new transactions on Datastore Data Tree
            // directly.
            if (transaction.equals(latestOutstandingTx)) {
                latestOutstandingTx = null;
            }
        }

    }

    private static class ChainedTransactionCommitImpl implements DOMStoreThreePhaseCommitCohort {

        private final SnapshotBackedWriteTransaction transaction;
        private final DOMStoreThreePhaseCommitCohort delegate;

        private final DOMStoreTransactionChainImpl txChain;

        protected ChainedTransactionCommitImpl(final SnapshotBackedWriteTransaction transaction,
                final DOMStoreThreePhaseCommitCohort delegate, final DOMStoreTransactionChainImpl txChain) {
            super();
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

        public ThreePhaseCommitImpl(final SnapshotBackedWriteTransaction writeTransaction) {
            this.transaction = writeTransaction;
            this.modification = transaction.getMutatedView();
        }

        @Override
        public ListenableFuture<Boolean> canCommit() {
            return executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws TransactionCommitFailedException {
                    try {
                        dataTree.validate(modification);
                        LOG.debug("Store Transaction: {} can be committed", transaction.getIdentifier());
                        return true;
                    } catch (ConflictingModificationAppliedException e) {
                        LOG.warn("Store Tx: {} Conflicting modification for {}.", transaction.getIdentifier(),
                                e.getPath());
                        throw new OptimisticLockFailedException("Optimistic lock failed.",e);
                    } catch (DataValidationFailedException e) {
                        LOG.warn("Store Tx: {} Data Precondition failed for {}.", transaction.getIdentifier(),
                                e.getPath(), e);
                        throw new TransactionCommitFailedException("Data did not pass validation.",e);
                    }
                }
            });
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            return executor.submit(new Callable<Void>() {
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
            return Futures.immediateFuture(null);
        }

        @Override
        public ListenableFuture<Void> commit() {
            checkState(candidate != null, "Proposed subtree must be computed");

            /*
             * The commit has to occur atomically with regard to listener
             * registrations.
             */
            synchronized (this) {
                dataTree.commit(candidate);

                for (ChangeListenerNotifyTask task : listenerResolver.call()) {
                    LOG.trace("Scheduling invocation of listeners: {}", task);
                    executor.submit(task);
                }
            }

            return Futures.immediateFuture(null);
        }
    }
}
