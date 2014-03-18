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

import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class DOMDataBrokerImpl implements DOMDataBroker {

    ImmutableMap<LogicalDatastoreType, DOMStore> datastores;
    ListeningExecutorService executor;


    @Override
    public DOMDataReadTransaction newReadOnlyTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreReadTransaction> builder = ImmutableMap.builder();
        for(Entry<LogicalDatastoreType, DOMStore> store : datastores.entrySet()) {
            builder.put(store.getKey(), store.getValue().newReadOnlyTransaction());
        }
        return new ReadOnlyTransactionImpl(newTransactionIdentifier(), builder.build());
    }

    private Object newTransactionIdentifier() {
        return new Object();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreReadWriteTransaction> builder = ImmutableMap.builder();
        for(Entry<LogicalDatastoreType, DOMStore> store : datastores.entrySet()) {
            builder.put(store.getKey(), store.getValue().newReadWriteTransaction());
        }
        return new ReadWriteTransactionImpl(newTransactionIdentifier(), builder.build(),this);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        ImmutableMap.Builder<LogicalDatastoreType, DOMStoreWriteTransaction> builder = ImmutableMap.builder();
        for(Entry<LogicalDatastoreType, DOMStore> store : datastores.entrySet()) {
            builder.put(store.getKey(), store.getValue().newWriteOnlyTransaction());
        }
        return null; //new WriteTransactionImpl<DOMStoreWriteTransaction>(newTransactionIdentifier(), builder.build());
    }



    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store,
            final InstanceIdentifier path, final DOMDataChangeListener listener, final DataChangeScope triggeringScope) {

        DOMStore potentialStore = datastores.get(store);
        checkState(potentialStore != null, "Requested logical data store is not available.");
        return potentialStore.registerChangeListener(path, listener, triggeringScope);
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
            return backingTxs.get(backingTxs);
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
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store, final InstanceIdentifier path) {
            return getSubtransaction(store).read(path);
        }

    }

    private static class WriteTransactionImpl<T extends DOMStoreWriteTransaction> extends
            AbstractCompositeTransaction<LogicalDatastoreType, T> implements DOMDataWriteTransaction {

        private final DOMDataBrokerImpl broker;

        protected WriteTransactionImpl(final Object identifier, final ImmutableMap<LogicalDatastoreType, T> backingTxs, final DOMDataBrokerImpl broker) {
            super(identifier, backingTxs);
            this.broker = broker;
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
        public void merge(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Not implemented yet.");
        }

        @Override
        public void cancel() {
            // TODO Auto-generated method stub

        }

        @Override
        public Future<RpcResult<TransactionStatus>> commit() {
            return null; //broker.submit(this);
        }

    }

    private static class ReadWriteTransactionImpl extends WriteTransactionImpl<DOMStoreReadWriteTransaction> implements
            DOMDataReadWriteTransaction {

        protected ReadWriteTransactionImpl(final Object identifier,
                final ImmutableMap<LogicalDatastoreType, DOMStoreReadWriteTransaction> backingTxs,final DOMDataBrokerImpl broker) {
            //super(identifier, backingTxs);
            super(identifier, backingTxs, broker);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store, final InstanceIdentifier path) {
            return getSubtransaction(store).read(path);
        }
    }

}
