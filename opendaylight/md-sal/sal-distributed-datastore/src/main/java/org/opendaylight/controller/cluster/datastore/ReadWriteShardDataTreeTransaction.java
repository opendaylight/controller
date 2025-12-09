/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

public final class ReadWriteShardDataTreeTransaction extends AbstractShardDataTreeTransaction<DataTreeModification> {
    @NonNullByDefault
    ReadWriteShardDataTreeTransaction(final TransactionParent parent, final TransactionIdentifier id,
            final DataTreeModification modification) {
        super(parent, id, modification);
    }

    @NonNullByDefault
    static ReadWriteShardDataTreeTransaction closedOf(final TransactionParent parent,
            final TransactionIdentifier id, final DataTreeModification modification) {
        final var ret = new ReadWriteShardDataTreeTransaction(parent, id, modification);
        ret.close();
        return ret;
    }

    CommitCohort ready() {
        checkState(close(), "Transaction is already closed");
        return getParent().finishTransaction(this);
    }
}
