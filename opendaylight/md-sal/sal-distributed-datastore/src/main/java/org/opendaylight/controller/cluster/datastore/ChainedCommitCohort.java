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
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChainedCommitCohort extends CommitCohort {
    private static final Logger LOG = LoggerFactory.getLogger(ChainedCommitCohort.class);

    private final @NonNull ReadWriteShardDataTreeTransaction transaction;

    @NonNullByDefault
    ChainedCommitCohort(final ReadWriteShardDataTreeTransaction transaction,
            final CompositeDataTreeCohort userCohorts) {
        super(transaction, userCohorts);
        this.transaction = requireNonNull(transaction);
    }

    @Override
    public void commit(final FutureCallback<UnsignedLong> callback) {
        final var parent = (ChainedTransactionParent) transaction.getParent();

        super.commit(new FutureCallback<>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                parent.clearTransaction(transaction);
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
