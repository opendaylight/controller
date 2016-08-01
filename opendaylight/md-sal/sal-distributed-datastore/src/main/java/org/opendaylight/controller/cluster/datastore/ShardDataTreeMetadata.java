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
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshotMetadata;

abstract class ShardDataTreeMetadata<T extends ShardDataTreeSnapshotMetadata<T>> {
    final void applySnapshot(@Nonnull final ShardDataTreeSnapshotMetadata<?> snapshot) {
        Verify.verify(getSupportedType().isInstance(snapshot), "Snapshot %s misrouted to handler of %s", snapshot,
            getSupportedType());
        doApplySnapshot(getSupportedType().cast(snapshot));
    }

    abstract void reset();

    abstract void doApplySnapshot(@Nonnull T snapshot);

    abstract @Nonnull Class<T> getSupportedType();

    abstract @Nullable T toStapshot();

    // Lifecycle events
    abstract void transactionCommitted(TransactionIdentifier txId);
}
