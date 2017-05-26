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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTreeChangePublisher;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedTransactions;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ExecutorServiceUtil;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager.Invoker;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
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
 * {@link org.opendaylight.controller.sal.core.spi.data.SnapshotBackedReadTransaction} and {@link ResolveDataChangeEventsTask}
 * to implement {@link DOMStore} contract.
 *
 */
public class InMemoryDOMDataStore extends TransactionReadyPrototype<String> implements DOMStore, Identifiable<String>, SchemaContextListener, AutoCloseable, DOMStoreTreeChangePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStore.class);

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

    private final DataTree dataTree;
    private final ListenerTree listenerTree = ListenerTree.create();
    private final AtomicLong txCounter = new AtomicLong(0);

    private final QueuedNotificationManager<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent> dataChangeListenerNotificationManager;
    private final InMemoryDOMStoreTreeChangePublisher changePublisher;
    private final ExecutorService dataChangeListenerExecutor;
    private final boolean debugTransactions;
    private final String name;

    private volatile AutoCloseable closeable;

    public InMemoryDOMDataStore(final String name, final ExecutorService dataChangeListenerExecutor) {
        this(name, LogicalDatastoreType.OPERATIONAL, dataChangeListenerExecutor,
            InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, false);
    }

    public InMemoryDOMDataStore(final String name, final LogicalDatastoreType type,
            final ExecutorService dataChangeListenerExecutor,
            final int maxDataChangeListenerQueueSize, final boolean debugTransactions) {
        this.name = Preconditions.checkNotNull(name);
        this.dataChangeListenerExecutor = Preconditions.checkNotNull(dataChangeListenerExecutor);
        this.debugTransactions = debugTransactions;

        dataChangeListenerNotificationManager =
                new QueuedNotificationManager<>(this.dataChangeListenerExecutor,
                        DCL_NOTIFICATION_MGR_INVOKER, maxDataChangeListenerQueueSize,
                        "DataChangeListenerQueueMgr");
        changePublisher = new InMemoryDOMStoreTreeChangePublisher(this.dataChangeListenerExecutor, maxDataChangeListenerQueueSize);

        switch (type) {
            case CONFIGURATION:
                dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION);
                break;
            case OPERATIONAL:
                dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
                break;
            default:
                throw new IllegalArgumentException("Data store " + type + " not supported");
        }
    }

    public void setCloseable(final AutoCloseable closeable) {
        this.closeable = closeable;
    }

    public QueuedNotificationManager<?, ?> getDataChangeListenerNotificationManager() {
        return dataChangeListenerNotificationManager;
    }

    @Override
    public final String getIdentifier() {
        return name;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return SnapshotBackedTransactions.newReadTransaction(nextIdentifier(), debugTransactions, dataTree.takeSnapshot());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return SnapshotBackedTransactions.newReadWriteTransaction(nextIdentifier(), debugTransactions, dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return SnapshotBackedTransactions.newWriteTransaction(nextIdentifier(), debugTransactions, dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        return new DOMStoreTransactionChainImpl(this);
    }

    @Override
    public synchronized void onGlobalContextUpdated(final SchemaContext ctx) {
        dataTree.setSchemaContext(ctx);
    }

    @Override
    public void close() {
        ExecutorServiceUtil.tryGracefulShutdown(dataChangeListenerExecutor, 30, TimeUnit.SECONDS);

        if(closeable != null) {
            try {
                closeable.close();
            } catch(Exception e) {
                LOG.debug("Error closing instance", e);
            }
        }
    }

    public final boolean getDebugTransactions() {
        return debugTransactions;
    }

    final DataTreeSnapshot takeSnapshot() {
        return dataTree.takeSnapshot();
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
    public synchronized <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(final YangInstanceIdentifier treeId, final L listener) {
        /*
         * Make sure commit is not occurring right now. Listener has to be
         * registered and its state capture enqueued at a consistent point.
         */
        return changePublisher.registerTreeChangeListener(treeId, listener, dataTree.takeSnapshot());
    }

    @Override
    protected void transactionAborted(final SnapshotBackedWriteTransaction<String> tx) {
        LOG.debug("Tx: {} is closed.", tx.getIdentifier());
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort transactionReady(final SnapshotBackedWriteTransaction<String> tx, final DataTreeModification modification) {
        LOG.debug("Tx: {} is submitted. Modifications: {}", tx.getIdentifier(), modification);
        return new InMemoryDOMStoreThreePhaseCommitCohort(this, tx, modification);
    }

    String nextIdentifier() {
        return name + "-" + txCounter.getAndIncrement();
    }

    void validate(final DataTreeModification modification) throws DataValidationFailedException {
        dataTree.validate(modification);
    }

    DataTreeCandidate prepare(final DataTreeModification modification) {
        return dataTree.prepare(modification);
    }

    synchronized void commit(final DataTreeCandidate candidate) {
        dataTree.commit(candidate);
        changePublisher.publishChange(candidate);
        ResolveDataChangeEventsTask.create(candidate, listenerTree).resolve(dataChangeListenerNotificationManager);
    }
}
