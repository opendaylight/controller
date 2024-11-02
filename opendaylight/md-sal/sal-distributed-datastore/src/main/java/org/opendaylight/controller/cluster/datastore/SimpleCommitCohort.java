/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.SortedSet;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

final class SimpleCommitCohort extends CommitCohort {
    SimpleCommitCohort(final ShardDataTree dataTree, final ReadWriteShardDataTreeTransaction transaction,
            final CompositeDataTreeCohort userCohorts, final @Nullable SortedSet<String> participatingShardNames) {
        super(dataTree, transaction, userCohorts, participatingShardNames);
    }

    SimpleCommitCohort(final ShardDataTree dataTree, final DataTreeModification modification,
            final TransactionIdentifier transactionId, final CompositeDataTreeCohort userCohorts,
            final @Nullable SortedSet<String> participatingShardNames) {
        super(dataTree, modification, transactionId, userCohorts, participatingShardNames);
    }

    SimpleCommitCohort(final ShardDataTree dataTree, final DataTreeModification modification,
            final TransactionIdentifier transactionId, final Exception nextFailure) {
        super(dataTree, modification, transactionId, nextFailure);
    }
}
