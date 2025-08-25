/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;

/**
 * Abstract base for transactions running on SharrdDataTree. This class is NOT thread-safe.
 *
 * @param <T> Backing transaction type.
 */
abstract class AbstractShardDataTreeTransaction<T extends DataTreeSnapshot>
        implements Identifiable<TransactionIdentifier> {
    private final @NonNull TransactionParent parent;
    private final @NonNull TransactionIdentifier id;
    private final @NonNull T snapshot;

    private boolean closed;

    AbstractShardDataTreeTransaction(final TransactionParent parent, final TransactionIdentifier id,
        final T snapshot) {
        this.parent = requireNonNull(parent);
        this.snapshot = requireNonNull(snapshot);
        this.id = requireNonNull(id);
    }

    @Override
    public final TransactionIdentifier getIdentifier() {
        return id;
    }

    final @NonNull TransactionParent getParent() {
        return parent;
    }

    final @NonNull T getSnapshot() {
        return snapshot;
    }

    final boolean isClosed() {
        return closed;
    }

    /**
     * Close this transaction and mark it as closed, allowing idempotent invocations.
     *
     * @return True if the transaction got closed by this method invocation.
     */
    protected final boolean close() {
        if (closed) {
            return false;
        }

        closed = true;
        return true;
    }

    final void abort(final Runnable callback) {
        checkState(close(), "Transaction is already closed");
        parent.abortTransaction(this, callback);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("id", id).add("closed", closed).add("snapshot", snapshot)
                .toString();
    }
}
