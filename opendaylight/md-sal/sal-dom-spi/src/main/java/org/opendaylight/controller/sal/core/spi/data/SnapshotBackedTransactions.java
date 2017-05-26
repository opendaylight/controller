/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

/**
 * Public utility class for instantiating snapshot-backed transactions.
 */
@Beta
public final class SnapshotBackedTransactions {
    private SnapshotBackedTransactions() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a new read-only transaction.
     *
     * @param identifier Transaction Identifier
     * @param debug Enable transaction debugging
     * @param snapshot Snapshot which will be modified.
     */
    public static <T> SnapshotBackedReadTransaction<T> newReadTransaction(final T identifier, final boolean debug, final DataTreeSnapshot snapshot) {
        return new SnapshotBackedReadTransaction<>(identifier, debug, snapshot);
    }

    /**
     * Creates a new read-write transaction.
     *
     * @param identifier transaction Identifier
     * @param debug Enable transaction debugging
     * @param snapshot Snapshot which will be modified.
     * @param readyImpl Implementation of ready method.
     */
    public static <T> SnapshotBackedReadWriteTransaction<T> newReadWriteTransaction(final T identifier, final boolean debug,
            final DataTreeSnapshot snapshot, final TransactionReadyPrototype<T> readyImpl) {
        return new SnapshotBackedReadWriteTransaction<>(identifier, debug, snapshot, readyImpl);
    }

    /**
     * Creates a new write-only transaction.
     *
     * @param identifier transaction Identifier
     * @param debug Enable transaction debugging
     * @param snapshot Snapshot which will be modified.
     * @param readyImpl Implementation of ready method.
     */
    public static <T> SnapshotBackedWriteTransaction<T> newWriteTransaction(final T identifier, final boolean debug,
            final DataTreeSnapshot snapshot, final TransactionReadyPrototype<T> readyImpl) {
        return new SnapshotBackedWriteTransaction<>(identifier, debug, snapshot, readyImpl);
    }
}
