/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class DOMDataBrokerImpl implements DOMDataBroker, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DOMDataBrokerImpl.class);
    private static final Logger COORDINATOR_LOG = LoggerFactory.getLogger(CommitCoordination.class);
    private final ImmutableMap<LogicalDatastoreType, DOMStore> datastores;
    private final ListeningExecutorService executor;
    private final AtomicLong txNum = new AtomicLong();

    public DOMDataBrokerImpl(final ImmutableMap<LogicalDatastoreType, DOMStore> datastores,
            final ListeningExecutorService executor) {
        super();
        this.datastores = datastores;
        this.executor = executor;
    }

    private static final Function<Iterable<Boolean>, Boolean> AND_FUNCTION = new Function<Iterable<Boolean>, Boolean>() {

        @Override
        public Boolean apply(final Iterable<Boolean> input) {

            for (Boolean value : input) {
                if (value == false) {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        }
    };

    @Override
    public DOMDataReadTransaction newReadOnlyTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreReadTransaction> builder = ImmutableMap.builder();
        for (Entry<LogicalDatastoreType, DOMStore> store : datastores.entrySet()) {
            builder.put(store.getKey(), store.getValue().newReadOnlyTransaction());
        }
        return new ReadOnlyTransactionImpl(newTransactionIdentifier(), builder.build());
    }

    private Object newTransactionIdentifier() {
        return "DOM-" + txNum.getAndIncrement();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreReadWriteTransaction> builder = ImmutableMap.builder();
        for (Entry<LogicalDatastoreType, DOMStore> store : datastores.entrySet()) {
            builder.put(store.getKey(), store.getValue().newReadWriteTransaction());
        }
        return new ReadWriteTransactionImpl(newTransactionIdentifier(), builder.build(), this);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreWriteTransaction> builder = ImmutableMap.builder();
        for (Entry<LogicalDatastoreType, DOMStore> store : datastores.entrySet()) {
            builder.put(store.getKey(), store.getValue().newWriteOnlyTransaction());
        }
        return new WriteTransactionImpl<DOMStoreWriteTransaction>(newTransactionIdentifier(), builder.build(), this);
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final InstanceIdentifier path, final DOMDataChangeListener listener, final DataChangeScope triggeringScope) {

        DOMStore potentialStore = datastores.get(store);
        checkState(potentialStore != null, "Requested logical data store is not available.");
        return potentialStore.registerChangeListener(path, listener, triggeringScope);
    }

    private ListenableFuture<RpcResult<TransactionStatus>> submit(
            final WriteTransactionImpl<? extends DOMStoreWriteTransaction> transaction) {
        LOG.debug("Tx: {} is submitted for execution.", transaction.getIdentifier());
        return executor.submit(new CommitCoordination(transaction));
    }

    private abstract static class AbstractCompositeTransaction<K, T extends DOMStoreTransaction> implements
            AsyncTransaction<InstanceIdentifier, NormalizedNode<?, ?>> {

        private final ImmutableMap<K, T> backingTxs;
        private final Object identifier;

        protected AbstractCompositeTransaction(final Object identifier, final ImmutableMap<K, T> backingTxs) {
            this.identifier = checkNotNull(identifier, "Identifier should not be null");
            this.backingTxs = checkNotNull(backingTxs, "Backing transactions should not be null");
        }

        protected T getSubtransaction(final K key) {
            return backingTxs.get(key);
        }

        public Iterable<T> getSubtransactions() {
            return backingTxs.values();
        }

        @Override
        public Object getIdentifier() {
            return identifier;
        }

        @Override
        public void close() {
            try {
                for (T subtransaction : backingTxs.values()) {
                    subtransaction.close();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Uncaught exception occured during closing transaction.", e);
            }
        }

    }

    private static class ReadOnlyTransactionImpl extends
            AbstractCompositeTransaction<LogicalDatastoreType, DOMStoreReadTransaction> implements
            DOMDataReadTransaction {

        protected ReadOnlyTransactionImpl(final Object identifier,
                final ImmutableMap<LogicalDatastoreType, DOMStoreReadTransaction> backingTxs) {
            super(identifier, backingTxs);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
                final InstanceIdentifier path) {
            return getSubtransaction(store).read(path);
        }

    }

    private static class WriteTransactionImpl<T extends DOMStoreWriteTransaction> extends
            AbstractCompositeTransaction<LogicalDatastoreType, T> implements DOMDataWriteTransaction {

        private final DOMDataBrokerImpl broker;
        private ImmutableList<DOMStoreThreePhaseCommitCohort> cohorts;

        protected WriteTransactionImpl(final Object identifier, final ImmutableMap<LogicalDatastoreType, T> backingTxs,
                final DOMDataBrokerImpl broker) {
            super(identifier, backingTxs);
            this.broker = broker;
        }

        public synchronized Iterable<DOMStoreThreePhaseCommitCohort> ready() {
            checkState(cohorts == null, "Transaction was already marked as ready.");
            ImmutableList.Builder<DOMStoreThreePhaseCommitCohort> cohortsBuilder = ImmutableList.builder();
            for (DOMStoreWriteTransaction subTx : getSubtransactions()) {
                cohortsBuilder.add(subTx.ready());
            }
            cohorts = cohortsBuilder.build();
            return cohorts;
        }

        protected ImmutableList<DOMStoreThreePhaseCommitCohort> getCohorts() {
            return cohorts;
        }

        @Override
        public void put(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
            getSubtransaction(store).write(path, data);
        }

        @Override
        public void delete(final LogicalDatastoreType store, final InstanceIdentifier path) {
            getSubtransaction(store).delete(path);
        }

        @Override
        public void merge(final LogicalDatastoreType store, final InstanceIdentifier path,
                final NormalizedNode<?, ?> data) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Not implemented yet.");
        }

        @Override
        public void cancel() {
            // TODO Auto-generated method stub

        }

        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {

            ready();
            return broker.submit(this);
        }

    }

    private static class ReadWriteTransactionImpl extends WriteTransactionImpl<DOMStoreReadWriteTransaction> implements
            DOMDataReadWriteTransaction {

        protected ReadWriteTransactionImpl(final Object identifier,
                final ImmutableMap<LogicalDatastoreType, DOMStoreReadWriteTransaction> backingTxs,
                final DOMDataBrokerImpl broker) {
            // super(identifier, backingTxs);
            super(identifier, backingTxs, broker);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
                final InstanceIdentifier path) {
            return getSubtransaction(store).read(path);
        }

        @Override
        public void merge(final LogicalDatastoreType store, final InstanceIdentifier path,
                final NormalizedNode<?, ?> data) {

        }
    }

    private final class CommitCoordination implements Callable<RpcResult<TransactionStatus>> {

        private final WriteTransactionImpl<? extends DOMStoreWriteTransaction> transaction;

        public CommitCoordination(final WriteTransactionImpl<? extends DOMStoreWriteTransaction> transaction) {
            this.transaction = transaction;
        }

        @Override
        public RpcResult<TransactionStatus> call() throws Exception {

            try {
                Boolean canCommit = canCommit().get();

                if (canCommit) {
                    try {
                        preCommit().get();
                        try {
                            commit().get();
                            COORDINATOR_LOG.debug("Tx: {} Is commited.", transaction.getIdentifier());
                            return Rpcs.getRpcResult(true, TransactionStatus.COMMITED,
                                    Collections.<RpcError> emptySet());

                        } catch (InterruptedException | ExecutionException e) {
                            COORDINATOR_LOG.error("Tx: {} Error during commit", transaction.getIdentifier(), e);
                        }

                    } catch (InterruptedException | ExecutionException e) {
                        COORDINATOR_LOG.warn("Tx: {} Error during preCommit, starting Abort",
                                transaction.getIdentifier(), e);
                    }
                } else {
                    COORDINATOR_LOG.info("Tx: {} Did not pass canCommit phase.", transaction.getIdentifier());
                    abort().get();
                }
            } catch (InterruptedException | ExecutionException e) {
                COORDINATOR_LOG.warn("Tx: {} Error during canCommit, starting Abort", transaction.getIdentifier(), e);

            }
            try {
                abort().get();
            } catch (InterruptedException | ExecutionException e) {
                COORDINATOR_LOG.error("Tx: {} Error during abort", transaction.getIdentifier(), e);
            }
            return Rpcs.getRpcResult(false, TransactionStatus.FAILED, Collections.<RpcError> emptySet());
        }

        public ListenableFuture<Void> preCommit() {
            COORDINATOR_LOG.debug("Transaction {}: PreCommit Started ", transaction.getIdentifier());
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : transaction.getCohorts()) {
                ops.add(cohort.preCommit());
            }
            return (ListenableFuture) Futures.allAsList(ops.build());
        }

        public ListenableFuture<Void> commit() {
            COORDINATOR_LOG.debug("Transaction {}: Commit Started ", transaction.getIdentifier());
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : transaction.getCohorts()) {
                ops.add(cohort.commit());
            }
            return (ListenableFuture) Futures.allAsList(ops.build());
        }

        public ListenableFuture<Boolean> canCommit() {
            COORDINATOR_LOG.debug("Transaction {}: CanCommit Started ", transaction.getIdentifier());
            Builder<ListenableFuture<Boolean>> canCommitOperations = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : transaction.getCohorts()) {
                canCommitOperations.add(cohort.canCommit());
            }
            ListenableFuture<List<Boolean>> allCanCommits = Futures.allAsList(canCommitOperations.build());
            return Futures.transform(allCanCommits, AND_FUNCTION);
        }

        public ListenableFuture<Void> abort() {
            COORDINATOR_LOG.debug("Transaction {}: Abort Started ", transaction.getIdentifier());
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : transaction.getCohorts()) {
                ops.add(cohort.abort());
            }
            return (ListenableFuture) Futures.allAsList(ops.build());
        };

    }

    @Override
    public void close() throws Exception {

    }

}
