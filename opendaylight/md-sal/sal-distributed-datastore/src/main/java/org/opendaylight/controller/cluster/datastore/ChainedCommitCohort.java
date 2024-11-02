/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.SortedSet;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChainedCommitCohort extends CommitCohort {
    private final class ChainedComminComplete implements FutureCallback<UnsignedLong> {
        private final FutureCallback<UnsignedLong> callback;

        ChainedComminComplete(final FutureCallback<UnsignedLong> callback) {
            this.callback = requireNonNull(callback);
        }

        @Override
        public void onSuccess(final UnsignedLong result) {
            ChainedCommitCohort.this.onSuccess(callback, result);
        }

        @Override
        public void onFailure(final Throwable failure) {
            ChainedCommitCohort.this.onFailure(callback, failure);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("id", transactionId()).add("delegate", callback).toString();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ChainedCommitCohort.class);

    private final ReadWriteShardDataTreeTransaction transaction;
    private final ShardDataTreeTransactionChain chain;

    ChainedCommitCohort(final ShardDataTree dataTree, final ShardDataTreeTransactionChain chain,
            final ReadWriteShardDataTreeTransaction transaction,
            final @Nullable SortedSet<String> participatingShardNames) {
        super(participatingShardNames);
        this.transaction = requireNonNull(transaction);
        this.chain = requireNonNull(chain);
    }

    @Override
    TransactionIdentifier transactionId() {
        return transaction.getIdentifier();
    }

    @Override
    FutureCallback<UnsignedLong> wrapCommitCallback(final FutureCallback<UnsignedLong> callback) {
        return new ChainedComminComplete(callback);
    }

    private void onSuccess(final FutureCallback<UnsignedLong> callback, final UnsignedLong result) {
        chain.clearTransaction(transaction);
        LOG.debug("Committed transaction {}", transaction);
        callback.onSuccess(result);
    }

    private void onFailure(final FutureCallback<UnsignedLong> callback, final Throwable failure) {
        LOG.error("Transaction {} commit failed, cannot recover", transaction, failure);
        callback.onFailure(failure);
    }
}
