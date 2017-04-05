/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

abstract class ShardDataTreeTransactionParent {

    abstract void abortFromTransactionActor(AbstractShardDataTreeTransaction<?> transaction);

    abstract void abortTransaction(AbstractShardDataTreeTransaction<?> transaction, Runnable callback);

    abstract ShardDataTreeCohort finishTransaction(ReadWriteShardDataTreeTransaction transaction);

    abstract ShardDataTreeCohort createReadyCohort(TransactionIdentifier txId, DataTreeModification mod);

    abstract ShardDataTreeCohort createFailedCohort(TransactionIdentifier txId, DataTreeModification mod,
            Exception failure);
}
