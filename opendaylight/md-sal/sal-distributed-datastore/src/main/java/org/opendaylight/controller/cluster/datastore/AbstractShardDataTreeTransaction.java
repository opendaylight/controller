/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base for transactions running on SharrdDataTree.
 *
 * @param <T> Backing transaction type.
 */
@NotThreadSafe
abstract class AbstractShardDataTreeTransaction<T extends DataTreeSnapshot>
        implements Identifiable<TransactionIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractShardDataTreeTransaction.class);

    private final ShardDataTreeTransactionParent parent;
    private final TransactionIdentifier id;
    private final T snapshot;

    private boolean closed;

    AbstractShardDataTreeTransaction(final ShardDataTreeTransactionParent parent, final TransactionIdentifier id,
        final T snapshot) {
        this.parent = Preconditions.checkNotNull(parent);
        this.snapshot = Preconditions.checkNotNull(snapshot);
        this.id = Preconditions.checkNotNull(id);
    }

    @Override
    public final TransactionIdentifier getIdentifier() {
        return id;
    }

    final ShardDataTreeTransactionParent getParent() {
        return parent;
    }

    final T getSnapshot() {
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
        Preconditions.checkState(close(), "Transaction is already closed");
        parent.abortTransaction(this, callback);
    }

    final void purge(final Runnable callback) {
        if (!closed) {
            LOG.warn("Purging unclosed transaction {}", id);
        }
        parent.purgeTransaction(id, callback);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("id", id).add("closed", closed).add("snapshot", snapshot)
                .toString();
    }
}
