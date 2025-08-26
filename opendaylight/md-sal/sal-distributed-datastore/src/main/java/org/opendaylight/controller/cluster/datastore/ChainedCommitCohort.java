/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.SortedSet;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChainedCommitCohort extends CommitCohort {
    private static final Logger LOG = LoggerFactory.getLogger(ChainedCommitCohort.class);

    private final ReadWriteShardDataTreeTransaction transaction;
    private final ChainedTransactionParent chain;

    ChainedCommitCohort(final ShardDataTree dataTree, final ChainedTransactionParent chain,
            final ReadWriteShardDataTreeTransaction transaction, final CompositeDataTreeCohort userCohorts,
            final @Nullable SortedSet<String> participatingShardNames) {
        super(dataTree, transaction, userCohorts, participatingShardNames);
        this.transaction = requireNonNull(transaction);
        this.chain = requireNonNull(chain);
    }

    @Override
    public void commit(final FutureCallback<UnsignedLong> callback) {
        super.commit(new FutureCallback<>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                chain.clearTransaction(transaction);
                LOG.debug("Committed transaction {}", transaction);
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.error("Transaction {} commit failed, cannot recover", transaction, failure);
                callback.onFailure(failure);
            }
        });
    }
}
