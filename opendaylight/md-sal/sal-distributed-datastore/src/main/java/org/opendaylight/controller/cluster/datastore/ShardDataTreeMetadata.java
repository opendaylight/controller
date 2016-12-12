/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Verify;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshotMetadata;

abstract class ShardDataTreeMetadata<T extends ShardDataTreeSnapshotMetadata<T>> {
    /**
     * Apply a recovered metadata snapshot.
     *
     * @param snapshot Metadata snapshot
     */
    final void applySnapshot(@Nonnull final ShardDataTreeSnapshotMetadata<?> snapshot) {
        Verify.verify(getSupportedType().isInstance(snapshot), "Snapshot %s misrouted to handler of %s", snapshot,
            getSupportedType());
        doApplySnapshot(getSupportedType().cast(snapshot));
    }

    /**
     * Reset metadata to empty state.
     */
    abstract void reset();

    /**
     * Apply a recovered metadata snapshot. This is not a public entrypoint, just an interface between the base class
     * and its subclasses.
     *
     * @param snapshot Metadata snapshot
     */
    abstract void doApplySnapshot(@Nonnull T snapshot);

    /**
     * Return the type of metadata snapshot this object supports.
     *
     * @return Metadata type
     */
    abstract @Nonnull Class<T> getSupportedType();

    /**
     * Take a snapshot of current metadata state.
     *
     * @return Metadata snapshot, or null if the metadata is empty.
     */
    abstract @Nullable T toSnapshot();

    // Lifecycle events

    abstract void onTransactionAborted(TransactionIdentifier txId);

    abstract void onTransactionCommitted(TransactionIdentifier txId);

    abstract void onTransactionPurged(TransactionIdentifier txId);

    abstract void onHistoryCreated(LocalHistoryIdentifier historyId);

    abstract void onHistoryClosed(LocalHistoryIdentifier historyId);

    abstract void onHistoryPurged(LocalHistoryIdentifier historyId);

}
