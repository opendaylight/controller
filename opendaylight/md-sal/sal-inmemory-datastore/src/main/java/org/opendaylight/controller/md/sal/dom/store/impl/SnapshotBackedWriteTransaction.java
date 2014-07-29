/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkState;


import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Implementation of Write transaction which is backed by
 * {@link DataTreeSnapshot} and executed according to
 * {@link org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction.TransactionReadyPrototype}.
 *
 */
class SnapshotBackedWriteTransaction extends AbstractDOMStoreTransaction implements DOMStoreWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotBackedWriteTransaction.class);
    private DataTreeModification mutableTree;
    private boolean ready = false;
    private TransactionReadyPrototype readyImpl;

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
    public SnapshotBackedWriteTransaction(final Object identifier, final DataTreeSnapshot snapshot,
            final TransactionReadyPrototype readyImpl) {
        super(identifier);
        mutableTree = snapshot.newModification();
        this.readyImpl = Preconditions.checkNotNull(readyImpl, "readyImpl must not be null.");
        LOG.debug("Write Tx: {} allocated with snapshot {}", identifier, snapshot);
    }

    @Override
    public void close() {
        LOG.debug("Store transaction: {} : Closed", getIdentifier());
        this.mutableTree = null;
        this.readyImpl = null;
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotReady();
        try {
            LOG.debug("Tx: {} Write: {}:{}", getIdentifier(), path, data);
            mutableTree.write(path, data);
            // FIXME: Add checked exception
        } catch (Exception e) {
            LOG.error("Tx: {}, failed to write {}:{} in {}", getIdentifier(), path, data, mutableTree, e);
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
        try {
            LOG.debug("Tx: {} Merge: {}:{}", getIdentifier(), path, data);
            mutableTree.merge(path, data);
            // FIXME: Add checked exception
        } catch (Exception e) {
            LOG.error("Tx: {}, failed to write {}:{} in {}", getIdentifier(), path, data, mutableTree, e);
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
        try {
            LOG.debug("Tx: {} Delete: {}", getIdentifier(), path);
            mutableTree.delete(path);
            // FIXME: Add checked exception
        } catch (Exception e) {
            LOG.error("Tx: {}, failed to delete {} in {}", getIdentifier(), path, mutableTree, e);
            // Rethrow original ones if they are subclasses of RuntimeException
            // or Error
            Throwables.propagateIfPossible(e);
            // FIXME: Introduce proper checked exception
            throw new IllegalArgumentException("Illegal path to delete.", e);
        }
    }

    protected final boolean isReady() {
        return ready;
    }

    protected final void checkNotReady() {
        checkState(!ready, "Transaction %s is ready. No further modifications allowed.", getIdentifier());
    }

    @Override
    public synchronized DOMStoreThreePhaseCommitCohort ready() {
        checkState(!ready, "Transaction %s is already ready.", getIdentifier());
        ready = true;
        LOG.debug("Store transaction: {} : Ready", getIdentifier());
        mutableTree.ready();
        return readyImpl.ready(this);
    }

    protected DataTreeModification getMutatedView() {
        return mutableTree;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("ready", isReady());
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
    public static interface TransactionReadyPrototype {

        /**
         * Returns a commit coordinator associated with supplied transactions.
         *
         * This call must not fail.
         *
         * @param tx
         *            Transaction on which ready was invoked.
         * @return DOMStoreThreePhaseCommitCohort associated with transaction
         */
        DOMStoreThreePhaseCommitCohort ready(SnapshotBackedWriteTransaction tx);
    }
}