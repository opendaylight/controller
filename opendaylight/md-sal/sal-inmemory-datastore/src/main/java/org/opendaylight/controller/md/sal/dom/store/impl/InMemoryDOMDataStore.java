/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.sal.core.compat.ListenerTree;
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
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ExecutorServiceUtil;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
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
 * <p>
 * Implementation of {@link DOMStore} which uses {@link DataTree} and other
 * classes such as {@link SnapshotBackedWriteTransaction}.
 * {@link org.opendaylight.controller.sal.core.spi.data.SnapshotBackedReadTransaction} and
 * {@link ResolveDataChangeEventsTask}
 * to implement {@link DOMStore} contract.
 *
 */
public class InMemoryDOMDataStore extends TransactionReadyPrototype<String>
        implements DOMStore, Identifiable<String>, SchemaContextListener, AutoCloseable, DOMStoreTreeChangePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStore.class);

    private final DataTree dataTree;
    private final ListenerTree listenerTree = ListenerTree.create();
    private final AtomicLong txCounter = new AtomicLong(0);

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

        changePublisher = new InMemoryDOMStoreTreeChangePublisher(this.dataChangeListenerExecutor,
                maxDataChangeListenerQueueSize);

        switch (type) {
            case CONFIGURATION:
                dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION);
                break;
            case OPERATIONAL:
                dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL);
                break;
            default:
                throw new IllegalArgumentException("Data store " + type + " not supported");
        }
    }

    public void setCloseable(final AutoCloseable closeable) {
        this.closeable = closeable;
    }

    public QueuedNotificationManager<?, ?> getDataChangeListenerNotificationManager() {
        return changePublisher.getNotificationManager();
    }

    @Override
    public final String getIdentifier() {
        return name;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return SnapshotBackedTransactions.newReadTransaction(nextIdentifier(), debugTransactions,
                dataTree.takeSnapshot());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return SnapshotBackedTransactions.newReadWriteTransaction(nextIdentifier(), debugTransactions,
                dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return SnapshotBackedTransactions.newWriteTransaction(nextIdentifier(), debugTransactions,
                dataTree.takeSnapshot(), this);
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
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void close() {
        ExecutorServiceUtil.tryGracefulShutdown(dataChangeListenerExecutor, 30, TimeUnit.SECONDS);

        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
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
    public synchronized <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
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
    protected DOMStoreThreePhaseCommitCohort transactionReady(final SnapshotBackedWriteTransaction<String> tx,
                                                              final DataTreeModification modification,
                                                              final Exception readyError) {
        LOG.debug("Tx: {} is submitted. Modifications: {}", tx.getIdentifier(), modification);
        return new InMemoryDOMStoreThreePhaseCommitCohort(this, tx, modification, readyError);
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
    }
}
