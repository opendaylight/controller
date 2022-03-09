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
import java.util.Optional;
import java.util.SortedSet;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChainedCommitCohort extends ShardDataTreeCohort {
    private static final Logger LOG = LoggerFactory.getLogger(ChainedCommitCohort.class);
    private final ReadWriteShardDataTreeTransaction transaction;
    private final ShardDataTreeTransactionChain chain;
    private final ShardDataTreeCohort delegate;

    ChainedCommitCohort(final ShardDataTreeTransactionChain chain, final ReadWriteShardDataTreeTransaction transaction,
            final ShardDataTreeCohort delegate) {
        this.transaction = requireNonNull(transaction);
        this.delegate = requireNonNull(delegate);
        this.chain = requireNonNull(chain);
    }

    @Override
    public void commit(final FutureCallback<UnsignedLong> callback) {
        delegate.commit(new FutureCallback<UnsignedLong>() {
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

    @Override
    public TransactionIdentifier getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void canCommit(final FutureCallback<Empty> callback) {
        delegate.canCommit(callback);
    }

    @Override
    public void preCommit(final FutureCallback<DataTreeCandidate> callback) {
        delegate.preCommit(callback);
    }

    @Override
    public void abort(final FutureCallback<Empty> callback) {
        delegate.abort(callback);
    }

    @Override
    DataTreeCandidateTip getCandidate() {
        return delegate.getCandidate();
    }

    @Override
    DataTreeModification getDataTreeModification() {
        return delegate.getDataTreeModification();
    }

    @Override
    public boolean isFailed() {
        return delegate.isFailed();
    }

    @Override
    public State getState() {
        return delegate.getState();
    }

    @Override
    Optional<SortedSet<String>> getParticipatingShardNames() {
        return delegate.getParticipatingShardNames();
    }
}
