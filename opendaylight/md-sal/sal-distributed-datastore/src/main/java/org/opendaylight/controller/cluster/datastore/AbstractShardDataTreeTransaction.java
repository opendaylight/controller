/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

/**
 * Abstract base for transactions running on SharrdDataTree.
 *
 * @param <T> Backing transaction type.
 */
@NotThreadSafe
abstract class AbstractShardDataTreeTransaction<T extends DataTreeSnapshot> {
    private final T snapshot;
    private boolean closed;

    protected AbstractShardDataTreeTransaction(final T snapshot) {
        this.snapshot = Preconditions.checkNotNull(snapshot);
    }

    final T getSnapshot() {
        Preconditions.checkState(!closed, "Transaction is already closed");
        return snapshot;
    }

    /**
     * Close this transaction and mark it as closed, allowing idempotent invocations.
     *
     * @return True if the transaction got closed by this method invocation.
     */
    protected final boolean close() {
        if (!closed) {
            closed = true;
            return true;
        } else {
            return false;
        }
    }

    abstract void abort();
}
