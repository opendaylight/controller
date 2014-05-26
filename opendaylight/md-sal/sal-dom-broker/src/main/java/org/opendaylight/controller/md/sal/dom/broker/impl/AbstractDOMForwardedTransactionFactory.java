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

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractDOMForwardedTransactionFactory<T extends DOMStoreTransactionFactory> {

    private final ImmutableMap<LogicalDatastoreType, T> storeTxFactories;

    public AbstractDOMForwardedTransactionFactory(
            final Map<LogicalDatastoreType, ? extends T> txFactories) {
        this.storeTxFactories = ImmutableMap.copyOf(txFactories);
    }

    /**
     * Implementations must return unique identifier for each and every call of
     * this method;
     *
     * @return
     */
    abstract Object newTransactionIdentifier();

    public DOMDataReadTransaction newReadOnlyTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreReadTransaction> builder = ImmutableMap.builder();
        for (Entry<LogicalDatastoreType, T> store : storeTxFactories.entrySet()) {
            builder.put(store.getKey(), store.getValue().newReadOnlyTransaction());
        }
        return new DOMForwardedReadOnlyTransaction(newTransactionIdentifier(), builder.build());
    }

    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreReadWriteTransaction> builder = ImmutableMap.builder();
        for (Entry<LogicalDatastoreType, T> store : storeTxFactories.entrySet()) {
            builder.put(store.getKey(), store.getValue().newReadWriteTransaction());
        }
        return new DOMForwardedReadWriteTransaction(newTransactionIdentifier(), builder.build(), this);
    }

    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreWriteTransaction> builder = ImmutableMap.builder();
        for (Entry<LogicalDatastoreType, T> store : storeTxFactories.entrySet()) {
            builder.put(store.getKey(), store.getValue().newWriteOnlyTransaction());
        }
        return new DOMForwardedWriteTransaction<DOMStoreWriteTransaction>(newTransactionIdentifier(), builder.build(),
                this);
    }

    protected final Map<LogicalDatastoreType,T> getTxFactories() {
        return storeTxFactories;
    }

    abstract ListenableFuture<RpcResult<TransactionStatus>> submit(final DOMDataWriteTransaction transaction,
            final Iterable<DOMStoreThreePhaseCommitCohort> cohorts);

    static abstract class AbstractDOMForwardedCompositeTransaction<K, T extends DOMStoreTransaction> implements
            AsyncTransaction<InstanceIdentifier, NormalizedNode<?, ?>> {

        private final ImmutableMap<K, T> backingTxs;
        private final Object identifier;

        protected AbstractDOMForwardedCompositeTransaction(final Object identifier, final ImmutableMap<K, T> backingTxs) {
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

    private static class DOMForwardedReadOnlyTransaction extends
            AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, DOMStoreReadTransaction> implements
            DOMDataReadTransaction {

        protected DOMForwardedReadOnlyTransaction(final Object identifier,
                final ImmutableMap<LogicalDatastoreType, DOMStoreReadTransaction> backingTxs) {
            super(identifier, backingTxs);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
                final InstanceIdentifier path) {
            return getSubtransaction(store).read(path);
        }

    }

    private class DOMForwardedWriteTransaction<T extends DOMStoreWriteTransaction> extends
            AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, T> implements DOMDataWriteTransaction {

        private final AbstractDOMForwardedTransactionFactory factory;
        private ImmutableList<DOMStoreThreePhaseCommitCohort> cohorts;

        protected DOMForwardedWriteTransaction(final Object identifier,
                final ImmutableMap<LogicalDatastoreType, T> backingTxs,
                final AbstractDOMForwardedTransactionFactory broker) {
            super(identifier, backingTxs);
            this.factory = broker;
        }

        /**
         * Seals all backing {@link DOMStoreWriteTransaction} transactions to
         * ready.
         *
         */
        private synchronized Iterable<DOMStoreThreePhaseCommitCohort> ready() {
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
            getSubtransaction(store).merge(path, data);
        }

        @Override
        public void cancel() {
            // TODO Auto-generated method stub

        }

        @Override
        public synchronized ListenableFuture<RpcResult<TransactionStatus>> commit() {
            ready();
            return factory.submit(this, getCohorts());
        }
    }

    private class DOMForwardedReadWriteTransaction extends DOMForwardedWriteTransaction<DOMStoreReadWriteTransaction>
            implements DOMDataReadWriteTransaction {

        protected DOMForwardedReadWriteTransaction(final Object identifier,
                final ImmutableMap<LogicalDatastoreType, DOMStoreReadWriteTransaction> backingTxs,
                final AbstractDOMForwardedTransactionFactory broker) {
            super(identifier, backingTxs, broker);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
                final InstanceIdentifier path) {
            return getSubtransaction(store).read(path);
        }
    }

}
