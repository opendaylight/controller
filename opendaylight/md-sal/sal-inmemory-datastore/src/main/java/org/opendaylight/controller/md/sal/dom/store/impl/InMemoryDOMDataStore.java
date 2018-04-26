/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTreeChangePublisher;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

/**
 * In-memory DOM Data Store
 *
 * <p>
 * Implementation of {@link DOMStore} which uses {@link DataTree} and other
 * classes such as {@link SnapshotBackedWriteTransaction}.
 * {@link org.opendaylight.controller.sal.core.spi.data.SnapshotBackedReadTransaction} and
 * {@link ResolveDataChangeEventsTask}
 * to implement {@link DOMStore} contract.
 *
 */
public class InMemoryDOMDataStore implements DOMStore, Identifiable<String>, SchemaContextListener, AutoCloseable,
        DOMStoreTreeChangePublisher {

    private final org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore delegate;

    public InMemoryDOMDataStore(final String name, final ExecutorService dataChangeListenerExecutor) {
        this(name, LogicalDatastoreType.OPERATIONAL, dataChangeListenerExecutor,
            InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, false);
    }

    public InMemoryDOMDataStore(final String name, final LogicalDatastoreType type,
            final ExecutorService dataChangeListenerExecutor,
            final int maxDataChangeListenerQueueSize, final boolean debugTransactions) {
        delegate = new org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore(name, dataChangeListenerExecutor,
            maxDataChangeListenerQueueSize, debugTransactions);
    }

    public void setCloseable(final AutoCloseable closeable) {
        delegate.setCloseable(closeable);
    }

    public QueuedNotificationManager<?, ?> getDataChangeListenerNotificationManager() {
        return delegate.getDataChangeListenerNotificationManager();
    }

    @Override
    public final String getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return adaptTransaction(delegate.newReadOnlyTransaction());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return adaptTransaction(delegate.newReadWriteTransaction());
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return adaptTransaction(delegate.newWriteOnlyTransaction());
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        final org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain delegateChain =
                delegate.createTransactionChain();

        return new DOMStoreTransactionChain() {
            @Override
            public DOMStoreWriteTransaction newWriteOnlyTransaction() {
                return adaptTransaction(delegateChain.newWriteOnlyTransaction());
            }

            @Override
            public DOMStoreReadWriteTransaction newReadWriteTransaction() {
                return adaptTransaction(delegateChain.newReadWriteTransaction());
            }

            @Override
            public DOMStoreReadTransaction newReadOnlyTransaction() {
                return adaptTransaction(delegateChain.newReadOnlyTransaction());
            }

            @Override
            public void close() {
                delegateChain.close();
            }
        };
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext ctx) {
        delegate.onGlobalContextUpdated(ctx);
    }

    @Override
    public void close() {
        delegate.close();
    }

    public final boolean getDebugTransactions() {
        return delegate.getDebugTransactions();
    }

    @Override
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L>
            registerChangeListener(final YangInstanceIdentifier path, final L listener, final DataChangeScope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        final ListenerRegistration<?> reg = delegate.registerTreeChangeListener(treeId, listener::onDataTreeChanged);
        return new AbstractListenerRegistration<L>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        };
    }

    static CheckedFuture<Boolean, ReadFailedException> adaptExists(
            final CheckedFuture<Boolean, org.opendaylight.mdsal.common.api.ReadFailedException> delegate) {
        // TODO Auto-generated method stub
        return null;
    }

    static CheckedFuture<com.google.common.base.Optional<NormalizedNode<?, ?>>, ReadFailedException> adaptRead(
            final CheckedFuture<com.google.common.base.Optional<NormalizedNode<?, ?>>,
                org.opendaylight.mdsal.common.api.ReadFailedException> delegate) {
        // TODO Auto-generated method stub
        return null;
    }

    static DOMStoreThreePhaseCommitCohort adaptReady(
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort delegate) {
        return new DOMStoreThreePhaseCommitCohort() {
            @Override
            public ListenableFuture<Void> preCommit() {
                return delegate.preCommit();
            }

            @Override
            public ListenableFuture<Void> commit() {
                return delegate.commit();
            }

            @Override
            public ListenableFuture<Boolean> canCommit() {
                return delegate.canCommit();
            }

            @Override
            public ListenableFuture<Void> abort() {
                return delegate.abort();
            }
        };
    }

    static DOMStoreReadTransaction adaptTransaction(
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction delegate) {
        return new DOMStoreReadTransaction() {
            @Override
            public Object getIdentifier() {
                return delegate.getIdentifier();
            }

            @Override
            public void close() {
                delegate.close();
            }

            @Override
            public CheckedFuture<com.google.common.base.Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                    final YangInstanceIdentifier path) {
                return adaptRead(delegate.read(path));
            }

            @Override
            public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
                return adaptExists(delegate.exists(path));
            }
        };
    }

    static DOMStoreReadWriteTransaction adaptTransaction(
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction delegate) {
        return new DOMStoreReadWriteTransaction() {

            @Override
            public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
                delegate.write(path, data);
            }

            @Override
            public DOMStoreThreePhaseCommitCohort ready() {
                return adaptReady(delegate.ready());
            }

            @Override
            public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
                delegate.merge(path, data);
            }

            @Override
            public void delete(final YangInstanceIdentifier path) {
                delegate.delete(path);
            }

            @Override
            public Object getIdentifier() {
                return delegate.getIdentifier();
            }

            @Override
            public void close() {
                delegate.close();
            }

            @Override
            public CheckedFuture<com.google.common.base.Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                    final YangInstanceIdentifier path) {
                return adaptRead(delegate.read(path));

            }

            @Override
            public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
                return adaptExists(delegate.exists(path));
            }
        };
    }

    static DOMStoreWriteTransaction adaptTransaction(
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction delegate) {
        return new DOMStoreWriteTransaction() {
            @Override
            public Object getIdentifier() {
                return delegate.getIdentifier();
            }

            @Override
            public void close() {
                delegate.close();
            }

            @Override
            public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
                delegate.write(path, data);
            }

            @Override
            public DOMStoreThreePhaseCommitCohort ready() {
                return adaptReady(delegate.ready());
            }

            @Override
            public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
                delegate.merge(path, data);
            }

            @Override
            public void delete(final YangInstanceIdentifier path) {
                delegate.delete(path);
            }
        };
    }
}
