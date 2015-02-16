/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Write transaction which is backed by
 * {@link DataTreeSnapshot} and executed according to
 * {@link org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction.TransactionReadyPrototype}.
 *
 */
class SnapshotBackedWriteTransaction extends AbstractDOMStoreTransaction implements DOMStoreWriteTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotBackedWriteTransaction.class);
    private static final AtomicReferenceFieldUpdater<SnapshotBackedWriteTransaction, TransactionReadyPrototype> READY_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(SnapshotBackedWriteTransaction.class, TransactionReadyPrototype.class, "readyImpl");
    private static final AtomicReferenceFieldUpdater<SnapshotBackedWriteTransaction, DataTreeModification> TREE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(SnapshotBackedWriteTransaction.class, DataTreeModification.class, "mutableTree");

    // non-null when not ready
    private volatile TransactionReadyPrototype readyImpl;
    // non-null when not committed/closed
    private volatile DataTreeModification mutableTree;

    /**
     * Creates new write-only transaction.
     *
     * @param identifier
     *            transaction Identifier
     * @param snapshot
     *            Snapshot which will be modified.
     * @param readyImpl
     *            Implementation of ready method.
     */
    public SnapshotBackedWriteTransaction(final Object identifier, final boolean debug,
            final DataTreeSnapshot snapshot, final TransactionReadyPrototype readyImpl) {
        super(identifier, debug);
        this.readyImpl = Preconditions.checkNotNull(readyImpl, "readyImpl must not be null.");
        mutableTree = snapshot.newModification();
        LOG.debug("Write Tx: {} allocated with snapshot {}", identifier, snapshot);
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotReady();

        final DataTreeModification tree = mutableTree;
        LOG.debug("Tx: {} Write: {}:{}", getIdentifier(), path, data);

        try {
            tree.write(path, data);
            // FIXME: Add checked exception
        } catch (Exception e) {
            LOG.error("Tx: {}, failed to write {}:{} in {}", getIdentifier(), path, data, tree, e);
            // Rethrow original ones if they are subclasses of RuntimeException
            // or Error
            Throwables.propagateIfPossible(e);
            // FIXME: Introduce proper checked exception
            throw new IllegalArgumentException("Illegal input data.", e);
        }
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotReady();

        final DataTreeModification tree = mutableTree;
        LOG.debug("Tx: {} Merge: {}:{}", getIdentifier(), path, data);

        try {
            tree.merge(path, data);
            // FIXME: Add checked exception
        } catch (Exception e) {
            LOG.error("Tx: {}, failed to write {}:{} in {}", getIdentifier(), path, data, tree, e);
            // Rethrow original ones if they are subclasses of RuntimeException
            // or Error
            Throwables.propagateIfPossible(e);
            // FIXME: Introduce proper checked exception
            throw new IllegalArgumentException("Illegal input data.", e);
        }
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        checkNotReady();

        final DataTreeModification tree = mutableTree;
        LOG.debug("Tx: {} Delete: {}", getIdentifier(), path);

        try {
            tree.delete(path);
            // FIXME: Add checked exception
        } catch (Exception e) {
            LOG.error("Tx: {}, failed to delete {} in {}", getIdentifier(), path, tree, e);
            // Rethrow original ones if they are subclasses of RuntimeException
            // or Error
            Throwables.propagateIfPossible(e);
            // FIXME: Introduce proper checked exception
            throw new IllegalArgumentException("Illegal path to delete.", e);
        }
    }

    /**
     * Exposed for {@link SnapshotBackedReadWriteTransaction}'s sake only. The contract does
     * not allow data access after the transaction has been closed or readied.
     *
     * @param path Path to read
     * @return null if the the transaction has been closed;
     */
    protected final Optional<NormalizedNode<?, ?>> readSnapshotNode(final YangInstanceIdentifier path) {
        return readyImpl == null ? null : mutableTree.readNode(path);
    }

    private final void checkNotReady() {
        checkState(readyImpl != null, "Transaction %s is no longer open. No further modifications allowed.", getIdentifier());
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        final TransactionReadyPrototype wasReady = READY_UPDATER.getAndSet(this, null);
        checkState(wasReady != null, "Transaction %s is no longer open", getIdentifier());

        LOG.debug("Store transaction: {} : Ready", getIdentifier());

        final DataTreeModification tree = mutableTree;
        TREE_UPDATER.lazySet(this, null);
        tree.ready();
        return wasReady.transactionReady(this, tree);
    }

    @Override
    public void close() {
        final TransactionReadyPrototype wasReady = READY_UPDATER.getAndSet(this, null);
        if (wasReady != null) {
            LOG.debug("Store transaction: {} : Closed", getIdentifier());
            TREE_UPDATER.lazySet(this, null);
            wasReady.transactionAborted(this);
        } else {
            LOG.debug("Store transaction: {} : Closed after submit", getIdentifier());
        }
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("ready", readyImpl == null);
    }

    /**
     * Prototype implementation of
     * {@link #ready(org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction)}
     *
     * This class is intended to be implemented by Transaction factories
     * responsible for allocation of {@link org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction} and
     * providing underlying logic for applying implementation.
     *
     */
    abstract static class TransactionReadyPrototype {
        /**
         * Called when a transaction is closed without being readied. This is not invoked for
         * transactions which are ready.
         *
         * @param tx Transaction which got aborted.
         */
        protected abstract void transactionAborted(final SnapshotBackedWriteTransaction tx);

        /**
         * Returns a commit coordinator associated with supplied transactions.
         *
         * This call must not fail.
         *
         * @param tx
         *            Transaction on which ready was invoked.
         * @param tree
         *            Modified data tree which has been constructed.
         * @return DOMStoreThreePhaseCommitCohort associated with transaction
         */
        protected abstract DOMStoreThreePhaseCommitCohort transactionReady(SnapshotBackedWriteTransaction tx, DataTreeModification tree);
    }
}