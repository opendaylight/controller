/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

public final class ReadWriteShardDataTreeTransaction extends AbstractShardDataTreeTransaction<DataTreeModification> {

    ReadWriteShardDataTreeTransaction(final ShardDataTreeTransactionParent parent, final TransactionIdentifier id,
        final DataTreeModification modification) {
        super(parent, id, modification);
    }

    ShardDataTreeCohort ready() {
        Preconditions.checkState(close(), "Transaction is already closed");
        return getParent().finishTransaction(this);
    }
}
